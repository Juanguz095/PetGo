const { createRequire } = require('node:module');
const { resolve } = require('node:path');
const { readFileSync } = require('node:fs');
const { strict: assert } = require('node:assert');
const localRequire = createRequire(resolve('.review/package.json'));
const { initializeTestEnvironment, assertFails, assertSucceeds } = localRequire('@firebase/rules-unit-testing');
const { doc, setDoc, getDoc, updateDoc, writeBatch } = localRequire('firebase/firestore');
const { ref, uploadBytes, getBytes } = localRequire('firebase/storage');

(async () => {
  const env = await initializeTestEnvironment({
    projectId: 'demo-petgo-review',
    firestore: { host: '127.0.0.1', port: 8180, rules: readFileSync('firestore.rules', 'utf8') },
    storage: { host: '127.0.0.1', port: 9299, rules: readFileSync('storage.rules', 'utf8') },
  });
  await env.clearFirestore();
  try {
    const owner = env.authenticatedContext('owner');
    const other = env.authenticatedContext('other');
    const stranger = env.authenticatedContext('stranger');
    const moderator = env.authenticatedContext('moderator', { moderator: true });
    const guest = env.unauthenticatedContext();
    await assertSucceeds(setDoc(doc(owner.firestore(), 'usuarios/owner'), {
      legacyId: 1, nombre: 'Ana', correo: 'ana@example.com', tipo: 'Ciudadano', ciudad: 'Lima',
    }));
    const publication = {
      usuarioId: 'owner', usuarioLegacyId: 1, usuarioNombre: 'Ana', tipo: 'Perdida', estado: 'Activa',
      nombre: 'Luna', descripcion: 'Collar rojo', latitud: -12, longitud: -77,
      fechaCreacion: Date.now(),
    };
    await assertSucceeds(setDoc(doc(owner.firestore(), 'publicaciones/pet'), publication));
    const adoptionReference = doc(owner.firestore(), 'publicaciones/adoption');
    await assertSucceeds(setDoc(adoptionReference, {
      ...publication,
      tipo: 'Adopcion',
      edadAproximada: '2 años',
      caracter: 'Juguetona',
      cuidados: 'Vacunas al día',
    }));
    await assertSucceeds(updateDoc(adoptionReference, {
      edadAproximada: '3 años',
      caracter: 'Tranquila',
      cuidados: 'Alimentación especial',
    }));
    await assertFails(updateDoc(adoptionReference, { edadAproximada: 'x'.repeat(41) }));
    await assertFails(updateDoc(adoptionReference, { caracter: 'x'.repeat(81) }));
    await assertFails(updateDoc(adoptionReference, { cuidados: 'x'.repeat(181) }));
    await assertFails(setDoc(doc(owner.firestore(), 'publicaciones/autor-falso'), {
      ...publication, usuarioNombre: 'Beatriz',
    }));
    await assertFails(getDoc(doc(guest.firestore(), 'publicaciones/pet')));
    await assertFails(setDoc(doc(other.firestore(), 'publicaciones/spoof'), publication));
    await assertFails(updateDoc(doc(other.firestore(), 'publicaciones/pet'), { estado: 'Resuelta' }));
    await assertFails(updateDoc(doc(owner.firestore(), 'publicaciones/pet'), { usuarioId: 'other' }));
    const sighting = {
      usuarioId: 'other', usuarioLegacyId: 2, publicacionId: 'pet',
      latitud: -12.1, longitud: -77.1, descripcion: 'La vi en el parque', fecha: Date.now(),
    };
    await assertFails(setDoc(doc(other.firestore(), 'avistamientos/invalid'), {
      ...sighting, latitud: -91,
    }));
    await assertFails(setDoc(doc(other.firestore(), 'avistamientos/missing-fields'), {
      usuarioId: 'other', publicacionId: 'pet',
    }));
    await assertFails(setDoc(doc(other.firestore(), 'avistamientos/future'), {
      ...sighting, fecha: Date.now() + 60_000,
    }));
    await assertFails(setDoc(doc(other.firestore(), 'avistamientos/old'), {
      ...sighting, fecha: Date.now() - 31 * 24 * 60 * 60 * 1000,
    }));
    await assertSucceeds(setDoc(doc(owner.firestore(), 'usuarios/owner/favoritos/pet'), { publicacionId: 'pet' }));
    await assertFails(getDoc(doc(other.firestore(), 'usuarios/owner/favoritos/pet')));
    const report = {
      usuarioId: 'owner', motivo: 'Maltrato', descripcion: 'Necesita ayuda',
      latitud: -12, longitud: -77, fecha: Date.now(), estado: 'Recibida',
    };
    await assertSucceeds(setDoc(doc(owner.firestore(), 'denuncias/report'), report));
    await assertFails(getDoc(doc(other.firestore(), 'denuncias/report')));
    await assertFails(setDoc(doc(owner.firestore(), 'denuncias/empty'), { ...report, descripcion: '' }));
    await assertFails(updateDoc(doc(owner.firestore(), 'denuncias/report'), { estado: 'Atendida' }));
    await assertSucceeds(getDoc(doc(moderator.firestore(), 'denuncias/report')));
    await assertSucceeds(updateDoc(doc(moderator.firestore(), 'denuncias/report'), { estado: 'En revisión' }));
    const reportNotification = {
      id: 'report_denuncia_atendida', usuarioOrigenId: 'moderator', usuarioDestinoId: 'owner',
      publicacionId: 'report', avistamientoId: 'report', tipo: 'denuncia_estado',
      mensaje: 'Tu denuncia fue atendida', latitud: -12, longitud: -77,
      fecha: Date.now(), leida: false,
    };
    const moderationBatch = writeBatch(moderator.firestore());
    moderationBatch.update(doc(moderator.firestore(), 'denuncias/report'), { estado: 'Atendida' });
    moderationBatch.set(doc(moderator.firestore(), 'notificaciones/report_denuncia_atendida'), reportNotification);
    await assertSucceeds(moderationBatch.commit());
    await assertSucceeds(getDoc(doc(owner.firestore(), 'notificaciones/report_denuncia_atendida')));
    console.log('Denuncia y claim de moderación: OK');
    const notification = {
      id: 'seen', usuarioOrigenId: 'other', usuarioDestinoId: 'owner',
      publicacionId: 'pet', avistamientoId: 'seen', tipo: 'avistamiento',
      mensaje: 'Vieron a Luna', latitud: -12.1, longitud: -77.1,
      fecha: Date.now(), leida: false,
    };
    await assertFails(setDoc(doc(other.firestore(), 'notificaciones/fake'), {
      ...notification, id: 'fake', avistamientoId: 'missing',
    }));
    console.log('Notificación falsa rechazada: OK');
    const batch = writeBatch(other.firestore());
    batch.set(doc(other.firestore(), 'avistamientos/seen'), sighting);
    batch.set(doc(other.firestore(), 'notificaciones/seen'), notification);
    await assertSucceeds(batch.commit());
    console.log('Notificación inicial de avistamiento: OK');
    const revisionNotification = {
      id: 'seen_revision_coincide', usuarioOrigenId: 'owner', usuarioDestinoId: 'other',
      publicacionId: 'pet', avistamientoId: 'seen', tipo: 'revision_avistamiento',
      mensaje: 'El dueño confirmó la coincidencia', latitud: -12.1, longitud: -77.1,
      fecha: Date.now(), leida: false,
    };
    const reviewBatch = writeBatch(owner.firestore());
    reviewBatch.update(doc(owner.firestore(), 'avistamientos/seen'), { estadoRevision: 'Coincide' });
    reviewBatch.set(doc(owner.firestore(), 'notificaciones/seen_revision_coincide'), revisionNotification);
    await assertSucceeds(reviewBatch.commit());
    console.log('Notificación de revisión: OK');
    await assertSucceeds(getDoc(doc(other.firestore(), 'notificaciones/seen_revision_coincide')));
    await assertFails(getDoc(doc(owner.firestore(), 'notificaciones/seen_revision_coincide')));

    const unlinked = { ...sighting, publicacionId: '0' };
    await assertSucceeds(setDoc(doc(other.firestore(), 'avistamientos/unlinked'), unlinked));
    const linkBatch = writeBatch(owner.firestore());
    linkBatch.update(doc(owner.firestore(), 'avistamientos/unlinked'), {
      publicacionId: 'pet', estadoRevision: 'Pendiente',
    });
    linkBatch.set(doc(owner.firestore(), 'notificaciones/unlinked_vinculado'), {
      id: 'unlinked_vinculado', usuarioOrigenId: 'owner', usuarioDestinoId: 'other',
      publicacionId: 'pet', avistamientoId: 'unlinked', tipo: 'revision_avistamiento',
      mensaje: 'El dueño vinculó tu reporte', latitud: -12.1, longitud: -77.1,
      fecha: Date.now(), leida: false,
    });
    await assertSucceeds(linkBatch.commit());
    console.log('Notificación al vincular avistamiento: OK');

    const adoption = { ...publication, tipo: 'Adopcion', estado: 'Activa', nombre: 'Mila' };
    await assertSucceeds(setDoc(doc(owner.firestore(), 'publicaciones/adoption'), adoption));
    const applicationId = 'adoption_other';
    const application = {
      id: applicationId, publicacionId: 'adoption', usuarioInteresadoId: 'other',
      usuarioPropietarioId: 'owner', mensaje: 'Puedo darle un hogar',
      contactoWhatsapp: '999123456', estado: 'Pendiente', fecha: Date.now(),
    };
    const applicationNotification = {
      id: `${applicationId}_nueva`, usuarioOrigenId: 'other', usuarioDestinoId: 'owner',
      publicacionId: 'adoption', avistamientoId: applicationId, tipo: 'solicitud_adopcion',
      mensaje: 'Nueva solicitud', latitud: 0, longitud: 0, fecha: Date.now(), leida: false,
    };
    const applicationBatch = writeBatch(other.firestore());
    applicationBatch.set(doc(other.firestore(), `solicitudes_adopcion/${applicationId}`), application);
    applicationBatch.set(doc(other.firestore(), `notificaciones/${applicationId}_nueva`), applicationNotification);
    await assertSucceeds(applicationBatch.commit());
    console.log('Solicitud de adopción y notificación: OK');
    await assertSucceeds(getDoc(doc(owner.firestore(), `solicitudes_adopcion/${applicationId}`)));
    await assertFails(getDoc(doc(stranger.firestore(), `solicitudes_adopcion/${applicationId}`)));
    const statusNotification = {
      id: `${applicationId}_estado_en_conversación`, usuarioOrigenId: 'owner', usuarioDestinoId: 'other',
      publicacionId: 'adoption', avistamientoId: applicationId, tipo: 'solicitud_adopcion',
      mensaje: 'El dueño quiere conversar', latitud: 0, longitud: 0, fecha: Date.now(), leida: false,
    };
    const statusBatch = writeBatch(owner.firestore());
    statusBatch.update(doc(owner.firestore(), `solicitudes_adopcion/${applicationId}`), { estado: 'En conversación' });
    statusBatch.set(doc(owner.firestore(), `notificaciones/${applicationId}_estado_en_conversación`), statusNotification);
    await assertSucceeds(statusBatch.commit());
    await assertFails(updateDoc(doc(other.firestore(), `solicitudes_adopcion/${applicationId}`), { estado: 'No seleccionado' }));
    await assertSucceeds(updateDoc(doc(owner.firestore(), 'publicaciones/adoption'), { estado: 'Adoptada' }));
    await assertSucceeds(updateDoc(doc(owner.firestore(), `solicitudes_adopcion/${applicationId}`), { estado: 'Adopción completada' }));
    await assertFails(updateDoc(doc(owner.firestore(), 'avistamientos/seen'), { latitud: -12.3 }));
    await assertSucceeds(updateDoc(doc(other.firestore(), 'avistamientos/seen'), { latitud: -12.2 }));
    await assertFails(getDoc(doc(other.firestore(), 'notificaciones/seen')));
    await assertFails(setDoc(doc(other.firestore(), 'notificaciones/replay'), {
      ...notification, id: 'replay', avistamientoId: 'seen',
    }));
    await assertSucceeds(updateDoc(doc(owner.firestore(), 'notificaciones/seen'), { leida: true }));
    await assertFails(updateDoc(doc(owner.firestore(), 'notificaciones/seen'), { usuarioDestinoId: 'other' }));
    await assertFails(setDoc(doc(other.firestore(), 'notificaciones/spoof'), {
      ...notification, id: 'spoof', usuarioDestinoId: 'other',
    }));
    await assertSucceeds(uploadBytes(ref(owner.storage(), 'usuarios/owner/photo.jpg'), new Uint8Array([1, 2]), { contentType: 'image/jpeg' }));
    await assertFails(uploadBytes(ref(other.storage(), 'usuarios/owner/other.jpg'), new Uint8Array([1]), { contentType: 'image/jpeg' }));
    await assertFails(uploadBytes(ref(owner.storage(), 'usuarios/owner/file.txt'), new Uint8Array([1]), { contentType: 'text/plain' }));
    await assertFails(getBytes(ref(guest.storage(), 'usuarios/owner/photo.jpg')));
    assert(true);
    console.log('Comprobaciones de reglas Firestore/Storage completadas.');
  } finally { await env.cleanup(); }
})().catch(error => { console.error(error); process.exitCode = 1; });
