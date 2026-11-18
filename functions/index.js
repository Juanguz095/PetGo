const { initializeApp } = require("firebase-admin/app");
const { getFirestore } = require("firebase-admin/firestore");
const { getMessaging } = require("firebase-admin/messaging");
const { onDocumentCreated, onDocumentUpdated } = require("firebase-functions/v2/firestore");

initializeApp();

const db = getFirestore();

exports.enviarPushAvistamiento = onDocumentCreated(
  "notificaciones/{notificationId}",
  async (event) => {
    const notification = event.data?.data();
    if (!notification || ![
      "avistamiento",
      "caso_resuelto",
      "alerta_cercana",
      "revision_avistamiento",
      "solicitud_adopcion",
      "denuncia_estado",
    ].includes(notification.tipo)) return;

    const userId = notification.usuarioDestinoId;
    const publicationId = notification.publicacionId;
    if (typeof userId !== "string" || typeof publicationId !== "string") return;
    if (userId === notification.usuarioOrigenId) return;

    const [preferencesSnapshot, devicesSnapshot] = await Promise.all([
      db.doc(`usuarios/${userId}/preferencias/notificaciones`).get(),
      db.collection(`usuarios/${userId}/dispositivos`).get(),
    ]);

    const preferenceVersion = preferencesSnapshot.get("version");
    if (
      preferencesSnapshot.get("habilitadas") !== true ||
      typeof preferenceVersion !== "string"
    ) return;

    const devices = devicesSnapshot.docs
      .map((document) => ({
        reference: document.ref,
        token: document.get("token"),
        preferenceVersion: document.get("preferenciaVersion"),
      }))
      .filter(
        (device) =>
          typeof device.token === "string" &&
          device.token.length > 0 &&
          device.preferenceVersion === preferenceVersion
      );

    if (devices.length === 0) return;

    const title = notification.tipo === "caso_resuelto"
      ? "Mascota encontrada"
      : notification.tipo === "alerta_cercana"
        ? "Mascota perdida cerca"
        : notification.tipo === "revision_avistamiento"
          ? "Actualización de avistamiento"
          : notification.tipo === "solicitud_adopcion"
            ? "Solicitud de adopción"
            : notification.tipo === "denuncia_estado"
              ? "Actualización de denuncia"
              : "Nuevo avistamiento";
    const body =
      typeof notification.mensaje === "string" && notification.mensaje.length > 0
        ? notification.mensaje
        : "Hay novedades sobre una mascota perdida.";
    const data = {
      type: notification.tipo,
      notificationId: event.params.notificationId,
      usuarioDestinoId: userId,
      publicacionId,
      titulo: title,
      mensaje: body,
      ...(typeof notification.latitud === "number" && typeof notification.longitud === "number"
        ? {
          latitud: String(notification.latitud),
          longitud: String(notification.longitud),
        }
        : {}),
    };

    for (let index = 0; index < devices.length; index += 500) {
      const group = devices.slice(index, index + 500);
      const response = await getMessaging().sendEachForMulticast({
        tokens: group.map((device) => device.token),
        data,
        android: { priority: "high" },
      });

      const removals = [];
      response.responses.forEach((result, resultIndex) => {
        const code = result.error?.code;
        if (
          !result.success &&
          (code === "messaging/registration-token-not-registered" ||
            code === "messaging/invalid-registration-token")
        ) {
          removals.push(group[resultIndex].reference.delete());
        }
      });
      await Promise.all(removals);
    }
  }
);

exports.notificarCasoResuelto = onDocumentUpdated(
  "publicaciones/{publicationId}",
  async (event) => {
    const before = event.data?.before?.data();
    const after = event.data?.after?.data();
    if (
      !before || !after ||
      before.estado !== "Activa" || after.estado !== "Resuelta" ||
      after.tipo !== "Perdida"
    ) return;

    const ownerId = after.usuarioId;
    const publicationId = event.params.publicationId;
    if (typeof ownerId !== "string") return;

    const reports = await db.collection("avistamientos")
      .where("publicacionId", "==", publicationId)
      .get();
    const reporters = new Set(
      reports.docs
        .map((report) => report.get("usuarioId"))
        .filter((userId) => typeof userId === "string" && userId !== ownerId)
    );
    const recipients = [...reporters];
    for (let index = 0; index < recipients.length; index += 450) {
      const batch = db.batch();
      for (const reporterId of recipients.slice(index, index + 450)) {
        const notificationId = `${publicationId}_resuelta_${reporterId}`;
        const reference = db.collection("notificaciones").doc(notificationId);
        batch.set(reference, {
          id: notificationId,
          usuarioDestinoId: reporterId,
          usuarioOrigenId: ownerId,
          tipo: "caso_resuelto",
          publicacionId,
          avistamientoId: "",
          mensaje: `${after.nombre || "La mascota"} fue marcada como encontrada. Gracias por ayudar.`,
          fecha: Date.now(),
          leida: false,
        });
      }
      await batch.commit();
    }
  }
);

exports.notificarAvistamientoVinculado = onDocumentUpdated(
  "avistamientos/{sightingId}",
  async (event) => {
    const before = event.data?.before?.data();
    const after = event.data?.after?.data();
    if (
      !before || !after || before.publicacionId !== "0" ||
      typeof after.publicacionId !== "string" || after.publicacionId === "0"
    ) return;

    const publicationSnapshot = await db.doc(`publicaciones/${after.publicacionId}`).get();
    if (!publicationSnapshot.exists) return;
    const publication = publicationSnapshot.data();
    const privateLocation = await db.doc(
      `avistamientos/${event.params.sightingId}/ubicacion_privada/coordenadas`
    ).get();
    const latitude = privateLocation.get("latitud") ?? after.latitud;
    const longitude = privateLocation.get("longitud") ?? after.longitud;
    const ownerId = publication.usuarioId;
    const reporterId = after.usuarioId;
    if (typeof ownerId !== "string" || typeof reporterId !== "string" || ownerId === reporterId) return;

    const notificationId = `${event.params.sightingId}_vinculado`;
    await db.collection("notificaciones").doc(notificationId).set({
      id: notificationId,
      usuarioDestinoId: reporterId,
      usuarioOrigenId: ownerId,
      tipo: "revision_avistamiento",
      publicacionId: after.publicacionId,
      avistamientoId: event.params.sightingId,
      mensaje: `El dueño relacionó tu reporte con ${publication.nombre || "una alerta"}. Está pendiente de revisión.`,
      latitud: latitude,
      longitud: longitude,
      fecha: Date.now(),
      leida: false,
    });
  }
);

function distanciaKm(lat1, lng1, lat2, lng2) {
  const rad = Math.PI / 180;
  const dLat = (lat2 - lat1) * rad;
  const dLng = (lng2 - lng1) * rad;
  const a = Math.sin(dLat / 2) ** 2 +
    Math.cos(lat1 * rad) * Math.cos(lat2 * rad) * Math.sin(dLng / 2) ** 2;
  return 6371 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
}

exports.notificarPerdidaCercana = onDocumentCreated(
  "publicaciones/{publicationId}",
  async (event) => {
    const pub = event.data?.data();
    if (!pub || pub.tipo !== "Perdida" || pub.estado !== "Activa") return;

    const latitud = pub.latitud;
    const longitud = pub.longitud;
    if (typeof latitud !== "number" || typeof longitud !== "number") return;

    const ownerId = pub.usuarioId;
    const snapshot = await db.collection("usuarios")
      .where("alertaCercanaHabilitada", "==", true)
      .get();

    const petName = pub.nombre || "Una mascota";
    for (const doc of snapshot.docs) {
      const userId = doc.id;
      if (typeof ownerId === "string" && userId === ownerId) continue;

      const uLat = doc.get("alertaCercanaLatitud");
      const uLng = doc.get("alertaCercanaLongitud");
      if (typeof uLat !== "number" || typeof uLng !== "number") continue;

      const radio = typeof doc.get("alertaCercanaRadioKm") === "number"
        ? doc.get("alertaCercanaRadioKm")
        : 10;

      const distancia = distanciaKm(latitud, longitud, uLat, uLng);
      if (distancia > radio) continue;

      const notificationId = `${event.params.publicationId}_cercana_${userId}`;
      const kmTexto = distancia < 1 ? "a menos de 1 km" : `a unos ${Math.round(distancia)} km`;
      await db.collection("notificaciones").doc(notificationId).set({
        id: notificationId,
        usuarioDestinoId: userId,
        usuarioOrigenId: typeof ownerId === "string" ? ownerId : "",
        tipo: "alerta_cercana",
        publicacionId: event.params.publicationId,
        avistamientoId: "",
        mensaje: `${petName} se perdió ${kmTexto} de tu zona.`,
        latitud,
        longitud,
        fecha: Date.now(),
        leida: false,
      });
    }
  }
);

exports.notificarRevisionAvistamiento = onDocumentUpdated(
  "avistamientos/{sightingId}",
  async (event) => {
    const before = event.data?.before?.data();
    const after = event.data?.after?.data();
    const estadosNotificables = new Set(["Coincide", "No coincide", "Duplicado"]);
    if (
      !before || !after ||
      before.publicacionId !== after.publicacionId ||
      after.publicacionId === "0" ||
      before.estadoRevision === after.estadoRevision ||
      !estadosNotificables.has(after.estadoRevision)
    ) return;

    const publicationSnapshot = await db.doc(`publicaciones/${after.publicacionId}`).get();
    if (!publicationSnapshot.exists) return;
    const publication = publicationSnapshot.data();
    const ownerId = publication.usuarioId;
    const reporterId = after.usuarioId;
    if (typeof ownerId !== "string" || typeof reporterId !== "string" || ownerId === reporterId) return;

    const privateLocation = await db.doc(
      `avistamientos/${event.params.sightingId}/ubicacion_privada/coordenadas`
    ).get();
    const latitude = privateLocation.get("latitud") ?? after.latitud;
    const longitude = privateLocation.get("longitud") ?? after.longitud;
    const statusKey = after.estadoRevision.toLowerCase().replaceAll(" ", "_");
    const notificationId = `${event.params.sightingId}_revision_${statusKey}`;
    const message = after.estadoRevision === "Coincide"
      ? `El dueño confirmó que tu avistamiento coincide con ${publication.nombre || "la mascota"}.`
      : after.estadoRevision === "No coincide"
        ? `El dueño revisó tu avistamiento y señaló que no coincide con ${publication.nombre || "la mascota"}.`
        : `El dueño marcó tu avistamiento como duplicado.`;

    await db.collection("notificaciones").doc(notificationId).set({
      id: notificationId,
      usuarioDestinoId: reporterId,
      usuarioOrigenId: ownerId,
      tipo: "revision_avistamiento",
      publicacionId: after.publicacionId,
      avistamientoId: event.params.sightingId,
      mensaje: message,
      latitud: latitude,
      longitud: longitude,
      fecha: Date.now(),
      leida: false,
    });
  }
);

async function notificarSolicitudAdopcion(
  solicitud,
  notificationId,
  destinationId,
  originId,
  message
) {
  if (typeof destinationId !== "string" || typeof originId !== "string" || destinationId === originId) return;
  await db.collection("notificaciones").doc(notificationId).set({
    id: notificationId,
    usuarioDestinoId: destinationId,
    usuarioOrigenId: originId,
    tipo: "solicitud_adopcion",
    publicacionId: solicitud.publicacionId,
    avistamientoId: "",
    mensaje: message,
    latitud: 0,
    longitud: 0,
    fecha: Date.now(),
    leida: false,
  });
}

exports.notificarNuevaSolicitudAdopcion = onDocumentCreated(
  "solicitudes_adopcion/{solicitudId}",
  async (event) => {
    const solicitud = event.data?.data();
    if (!solicitud || solicitud.estado !== "Pendiente") return;
    const publicationSnapshot = await db.doc(`publicaciones/${solicitud.publicacionId}`).get();
    if (!publicationSnapshot.exists) return;
    const nombre = publicationSnapshot.get("nombre") || "tu publicación";
    const message = `Una persona quiere conocer más sobre ${nombre}.`;
    await notificarSolicitudAdopcion(
      solicitud,
      `${event.params.solicitudId}_nueva`,
      solicitud.usuarioPropietarioId,
      solicitud.usuarioInteresadoId,
      message
    );
  }
);

exports.notificarEstadoSolicitudAdopcion = onDocumentUpdated(
  "solicitudes_adopcion/{solicitudId}",
  async (event) => {
    const before = event.data?.before?.data();
    const after = event.data?.after?.data();
    if (!before || !after || before.estado === after.estado) return;
    const publicationSnapshot = await db.doc(`publicaciones/${after.publicacionId}`).get();
    if (!publicationSnapshot.exists) return;
    const nombre = publicationSnapshot.get("nombre") || "la mascota";
    const message = after.estado === "En conversación"
      ? `El dueño de ${nombre} quiere conversar contigo sobre la adopción.`
      : after.estado === "No seleccionado"
        ? `El dueño cerró tu solicitud para ${nombre}. Gracias por tu interés.`
        : `La solicitud para ${nombre} se cerró porque la adopción ya se concretó.`;
    const stateKey = after.estado === "En conversación"
      ? "en_conversación"
      : after.estado === "Adopción completada"
        ? "adopcion_completada"
        : after.estado.toLowerCase().replaceAll(" ", "_");
    const notificationId = `${event.params.solicitudId}_estado_${stateKey}`;
    await notificarSolicitudAdopcion(
      after,
      notificationId,
      after.usuarioInteresadoId,
      after.usuarioPropietarioId,
      message
    );
  }
);
