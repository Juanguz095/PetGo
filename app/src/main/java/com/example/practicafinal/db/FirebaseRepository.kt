package com.example.practicafinal.db

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Location
import android.net.Uri
import android.util.Base64
import com.bumptech.glide.Glide
import com.example.practicafinal.modelo.Albergue
import com.example.practicafinal.modelo.Avistamiento
import com.example.practicafinal.modelo.Denuncia
import com.example.practicafinal.modelo.Publicacion
import com.example.practicafinal.modelo.ResultadoRegistro
import com.example.practicafinal.modelo.Usuario
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.AggregateSource
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import java.util.UUID
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt
import kotlin.math.round

class FirebaseRepository(private val context: Context) {

    companion object {
        private const val MAX_INLINE_PHOTO_BYTES = 80 * 1024
        private const val MAX_INLINE_PHOTO_SIDE = 480
        private const val USERS = "usuarios"
        private const val PUBLICATIONS = "publicaciones"
        private const val SIGHTINGS = "avistamientos"
        private const val SHELTERS = "albergues"
        private const val REPORTS = "denuncias"
        private const val NOTIFICATIONS = "notificaciones"
    }

    private val db: FirebaseFirestore
        get() = FirebaseFirestore.getInstance()
    private val auth: FirebaseAuth
        get() = FirebaseAuth.getInstance()

    private fun currentUid(): String = checkNotNull(com.example.practicafinal.session.SesionManager.currentFirebaseUid(context)) { "Inicia sesión para continuar" }
    private fun <T> await(task: Task<T>): T = Tasks.await(task, 30, TimeUnit.SECONDS)

    fun registrar(nombre: String, correo: String, contrasena: String, tipo: String = "Ciudadano", ciudad: String = ""): ResultadoRegistro {
        val normalizedEmail = correo.trim().lowercase(java.util.Locale.ROOT)
        val firebaseUser = checkNotNull(await(auth.createUserWithEmailAndPassword(normalizedEmail, contrasena)).user)
        val id = legacyId(firebaseUser.uid)
        val user = Usuario(id, nombre.trim(), normalizedEmail, tipo, ciudad.trim())
        val sesion = com.example.practicafinal.session.SesionManager
        sesion.guardarRegistroPendiente(context, firebaseUser.uid, user)
        return try {
            await(db.collection(USERS).document(firebaseUser.uid).set(user.toFirestoreMap(firebaseUser.uid)))
            sesion.limpiarRegistroPendiente(context)
            ResultadoRegistro.Completo(user)
        } catch (error: Exception) {
            android.util.Log.e("PetGo", "La cuenta existe, pero falta guardar el perfil", error)
            ResultadoRegistro.PerfilPendiente(user, error)
        }
    }

    fun reintentarGuardarPerfilPendiente(): Usuario? {
        val pendiente = checkNotNull(
            com.example.practicafinal.session.SesionManager.obtenerRegistroPendiente(context)
        ) { "No hay un perfil pendiente para esta cuenta" }
        check(pendiente.firebaseUid == currentUid()) { "La cuenta activa no coincide con el registro pendiente" }
        await(
            db.collection(USERS).document(pendiente.firebaseUid)
                .set(pendiente.usuario.toFirestoreMap(pendiente.firebaseUid))
        )
        com.example.practicafinal.session.SesionManager.limpiarRegistroPendiente(context)
        return pendiente.usuario
    }

    fun validarLogin(correo: String, contrasena: String): Usuario? {
        val normalizedEmail = correo.trim().lowercase(java.util.Locale.ROOT)
        val firebaseUser = checkNotNull(await(auth.signInWithEmailAndPassword(normalizedEmail, contrasena)).user)
        val snapshot = await(db.collection(USERS).document(firebaseUser.uid).get())
        val user = if (snapshot.exists()) snapshot.toUsuario(firebaseUser.uid) else {
            Usuario(legacyId(firebaseUser.uid), normalizedEmail.substringBefore("@"), normalizedEmail, "Ciudadano")
                .also { await(db.collection(USERS).document(firebaseUser.uid).set(it.toFirestoreMap(firebaseUser.uid))) }
        }
        com.example.practicafinal.session.SesionManager.guardarSesion(context, user.id, firebaseUser.uid)
        return user
    }

    fun obtenerPorId(id: Long): Usuario? {
        val uid = currentUid()
        val snapshot = await(db.collection(USERS).document(uid).get())
        return snapshot.takeIf { it.exists() }?.toUsuario(uid)?.takeIf { it.id == id }
    }

    fun pushHabilitado(): Boolean {
        val uid = currentUid()
        val snapshot = await(db.collection(USERS).document(uid)
            .collection("preferencias").document("notificaciones").get())
        return snapshot.getBoolean("habilitadas") == true
    }

    fun actualizarPushHabilitado(habilitadas: Boolean) {
        val uid = currentUid()
        val preferencias = db.collection(USERS).document(uid)
            .collection("preferencias").document("notificaciones")
        val version = UUID.randomUUID().toString()
        if (habilitadas) {
            val token = await(FirebaseMessaging.getInstance().token)
            val batch = db.batch()
            batch.set(preferencias, mapOf(
                "habilitadas" to true,
                "version" to version,
                "actualizado" to System.currentTimeMillis()
            ))
            batch.set(db.collection(USERS).document(uid).collection("dispositivos")
                .document(hashToken(token)), mapOf(
                "token" to token,
                "plataforma" to "android",
                "preferenciaVersion" to version,
                "actualizado" to System.currentTimeMillis()
            ))
            await(batch.commit())
        } else {
            await(preferencias.set(mapOf(
                "habilitadas" to false,
                "version" to version,
                "actualizado" to System.currentTimeMillis()
            )))
            try {
                val dispositivos = db.collection(USERS).document(uid).collection("dispositivos")
                val snapshot = await(dispositivos.get())
                snapshot.documents.chunked(500).forEach { documents ->
                    val batch = db.batch()
                    documents.forEach { batch.delete(it.reference) }
                    await(batch.commit())
                }
            } catch (error: Exception) {
                android.util.Log.w("PetGo", "Los avisos quedaron desactivados; no se pudieron limpiar todos los dispositivos", error)
            }
        }
    }

    fun quitarDispositivoPushActual() {
        val uid = currentUid()
        val token = await(FirebaseMessaging.getInstance().token)
        await(db.collection(USERS).document(uid).collection("dispositivos")
            .document(hashToken(token)).delete())
    }

    fun registrarTokenPushSiHabilitado(token: String) {
        val uid = auth.currentUser?.uid ?: return
        val preferencias = db.collection(USERS).document(uid)
            .collection("preferencias").document("notificaciones")
        preferencias.get().addOnSuccessListener { snapshot ->
            if (snapshot.getBoolean("habilitadas") == true) {
                val version = snapshot.getString("version") ?: return@addOnSuccessListener
                db.collection(USERS).document(uid).collection("dispositivos")
                    .document(hashToken(token))
                    .set(mapOf(
                        "token" to token,
                        "plataforma" to "android",
                        "preferenciaVersion" to version,
                        "actualizado" to System.currentTimeMillis()
                    ))
            }
        }.addOnFailureListener { error ->
            android.util.Log.w("PetGo", "No se pudo comprobar la preferencia de notificaciones", error)
        }
    }

    private fun hashToken(token: String): String = java.security.MessageDigest
        .getInstance("SHA-256")
        .digest(token.toByteArray(Charsets.UTF_8))
        .joinToString("") { byte -> "%02x".format(byte) }

    fun insertarPublicacion(
        usuarioId: Long?,
        tipo: String,
        nombre: String,
        descripcion: String,
        foto: String?,
        ultimoLugar: String?,
        especie: String?,
        latitud: Double,
        longitud: Double,
        contactoWhatsapp: String? = null,
        fechaPerdida: Long? = null,
        recompensa: String? = null,
        color: String? = null,
        tamano: String? = null,
        senasParticulares: String? = null
    ): Long {
        val uid = currentUid()
        val id = newId()
        val fotoFinal = foto?.let(::procesarFoto)
        val publication = Publicacion(
            id = id,
            usuarioId = usuarioId ?: legacyId(uid),
            tipo = tipo,
            nombre = nombre,
            descripcion = descripcion,
            foto = fotoFinal,
            ultimoLugar = ultimoLugar,
            especie = especie,
            latitud = latitud,
            longitud = longitud,
            fechaCreacion = System.currentTimeMillis(),
            estado = "Activa",
            contactoWhatsapp = contactoWhatsapp,
            fechaPerdida = fechaPerdida,
            recompensa = recompensa,
            ultimaConfirmacion = if (tipo == "Perdida") System.currentTimeMillis() else null,
            color = color?.trim()?.ifBlank { null },
            tamano = tamano?.trim()?.ifBlank { null },
            senasParticulares = senasParticulares?.trim()?.ifBlank { null }
        )
        val publicData = publication.copy(
            latitud = aproximarCoordenada(latitud),
            longitud = aproximarCoordenada(longitud)
        ).toFirestoreMap(uid)
        val batch = db.batch()
        batch.set(db.collection(PUBLICATIONS).document(id.toString()), publicData)
        batch.set(ubicacionPrivadaPublicacion(uid, id), mapOf("latitud" to latitud, "longitud" to longitud))
        await(batch.commit())
        return id
    }

    fun actualizarPublicacion(
        id: Long, tipo: String, nombre: String, descripcion: String, foto: String?,
        ultimoLugar: String?, especie: String?, latitud: Double, longitud: Double,
        contactoWhatsapp: String?, fechaPerdida: Long? = null, recompensa: String? = null,
        color: String? = null, tamano: String? = null, senasParticulares: String? = null
    ) {
        val uid = currentUid()
        val ref = db.collection(PUBLICATIONS).document(id.toString())
        val snapshot = await(ref.get())
        check(snapshot.exists() && snapshot.getString("usuarioId") == uid) {
            "Solo puedes editar tus propias publicaciones"
        }
        val actual = snapshot.toPublicacion()
        check(actual.estado == "Activa") { "Solo puedes editar publicaciones activas" }
        check(tipo in listOf("Perdida", "Encontrada", "Adopcion")) { "Tipo de publicación no válido" }
        val fotoFinal = when {
            foto == actual.foto -> actual.foto
            foto.isNullOrBlank() -> null
            else -> procesarFoto(foto)
        }
        val actualizada = actual.copy(
            tipo = tipo,
            nombre = nombre.trim(),
            descripcion = descripcion.trim(),
            foto = fotoFinal,
            ultimoLugar = ultimoLugar?.trim()?.ifBlank { null },
            especie = especie,
            latitud = latitud,
            longitud = longitud,
            contactoWhatsapp = contactoWhatsapp?.filter(Char::isDigit)?.ifBlank { null },
            fechaPerdida = fechaPerdida,
            recompensa = recompensa?.trim()?.ifBlank { null },
            ultimaConfirmacion = if (tipo == "Perdida") System.currentTimeMillis() else actual.ultimaConfirmacion,
            color = color?.trim()?.ifBlank { null },
            tamano = tamano?.trim()?.ifBlank { null },
            senasParticulares = senasParticulares?.trim()?.ifBlank { null }
        )
        val publicData = actualizada.copy(
            latitud = aproximarCoordenada(latitud),
            longitud = aproximarCoordenada(longitud)
        ).toFirestoreMap(uid)
        val batch = db.batch()
        batch.set(ref, publicData)
        batch.set(ubicacionPrivadaPublicacion(uid, id), mapOf("latitud" to latitud, "longitud" to longitud))
        await(batch.commit())
    }

    fun obtenerCoordenadasPublicacion(id: Long): Pair<Double, Double>? {
        val uid = currentUid()
        val location = await(ubicacionPrivadaPublicacion(uid, id).get())
        val latitude = (location.get("latitud") as? Number)?.toDouble()
        val longitude = (location.get("longitud") as? Number)?.toDouble()
        if (latitude != null && longitude != null) return latitude to longitude

        val publication = obtenerPorIdPublicacion(id) ?: return null
        return publication.latitud to publication.longitud
    }

    /** Oculta la publicación sin borrar los avistamientos vinculados. */
    fun eliminarPublicacion(id: Long) {
        val uid = currentUid()
        val ref = db.collection(PUBLICATIONS).document(id.toString())
        val snapshot = await(ref.get())
        check(snapshot.exists() && snapshot.getString("usuarioId") == uid) {
            "Solo puedes retirar tus propias publicaciones"
        }
        check(snapshot.getString("estado") == "Activa") {
            "Solo puedes retirar publicaciones activas"
        }
        val batch = db.batch()
        batch.update(ref, "estado", "Eliminada")
        batch.delete(ubicacionPrivadaPublicacion(uid, id))
        await(batch.commit())
    }

    /** Guarda una versión pequeña en Firestore para que publicar fotos no dependa de Storage. */
    private fun procesarFoto(value: String): String? {
        val uri = Uri.parse(value)
        if (uri.scheme == "http" || uri.scheme == "https") return value

        return try {
            if (value.startsWith("data:image/")) {
                val contenido = value.substringAfter("base64,", "")
                val bytes = Base64.decode(contenido, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
                try {
                    codificarFotoCompacta(bitmap)
                } finally {
                    bitmap.recycle()
                }
            } else {
                val target = Glide.with(context).asBitmap()
                    .load(uri)
                    .submit(MAX_INLINE_PHOTO_SIDE, MAX_INLINE_PHOTO_SIDE)
                try {
                    val bitmap = target.get(30, TimeUnit.SECONDS)
                    codificarFotoCompacta(bitmap)
                } finally {
                    Glide.with(context).clear(target)
                }
            }
        } catch (error: Exception) {
            android.util.Log.w("PetGo", "Se guardará el aviso sin la foto seleccionada", error)
            null
        }
    }

    private fun codificarFotoCompacta(bitmap: Bitmap): String? {
        val ladoOriginal = maxOf(bitmap.width, bitmap.height)
        if (ladoOriginal <= 0) return null
        var ladoDestino = minOf(MAX_INLINE_PHOTO_SIDE, ladoOriginal)

        while (ladoDestino >= 200) {
            val escala = minOf(1f, ladoDestino.toFloat() / ladoOriginal)
            val ancho = maxOf(1, (bitmap.width * escala).toInt())
            val alto = maxOf(1, (bitmap.height * escala).toInt())
            val reducido = if (ancho == bitmap.width && alto == bitmap.height) {
                bitmap
            } else {
                Bitmap.createScaledBitmap(bitmap, ancho, alto, true)
            }

            try {
                for (calidad in intArrayOf(68, 58, 48, 38, 30)) {
                    val salida = java.io.ByteArrayOutputStream()
                    if (!reducido.compress(Bitmap.CompressFormat.JPEG, calidad, salida)) continue
                    val bytes = salida.toByteArray()
                    if (bytes.size <= MAX_INLINE_PHOTO_BYTES) {
                        return "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
                    }
                }
            } finally {
                if (reducido !== bitmap) reducido.recycle()
            }
            ladoDestino = (ladoDestino * 0.75f).toInt()
        }
        return null
    }

    fun obtenerPublicaciones(): List<Publicacion> =
        await(db.collection(PUBLICATIONS).orderBy("fechaCreacion").get()).documents
            .asReversed().map { it.toPublicacion() }

    fun obtenerPaginaPublicaciones(
        tipo: String?, after: DocumentSnapshot?, pageSize: Int = 24
    ): PaginaFirestore<Publicacion> {
        require(pageSize > 0)
        return cargarPaginaPublicaciones(tipo, null, after, pageSize)
    }

    fun obtenerPaginaPropias(after: DocumentSnapshot?, pageSize: Int = 24): PaginaFirestore<Publicacion> {
        require(pageSize > 0)
        return cargarPaginaPublicaciones(null, currentUid(), after, pageSize, excluirEliminadas = true)
    }

    private fun cargarPaginaPublicaciones(
        tipo: String?, uid: String?, after: DocumentSnapshot?, pageSize: Int,
        excluirEliminadas: Boolean = false
    ): PaginaFirestore<Publicacion> {
        val collection = db.collection(PUBLICATIONS)
        val seleccionadas = mutableListOf<DocumentSnapshot>()
        var cursor = after
        var ultimoEscaneado = after
        var agotado = false
        var hayMas = false

        while (!agotado && !hayMas) {
            var query: Query = collection.orderBy("fechaCreacion", Query.Direction.DESCENDING)
                .limit(pageSize.toLong() + 1)
            cursor?.let { query = query.startAfter(it) }
            val snapshot = await(query.get())
            if (snapshot.isEmpty) {
                agotado = true
                break
            }

            for (document in snapshot.documents) {
                ultimoEscaneado = document
                val coincideTipo = tipo == null || document.getString("tipo") == tipo
                val coincideUsuario = uid == null || perteneceAUsuario(document, uid)
                val visible = !excluirEliminadas || document.getString("estado") != "Eliminada"
                if (coincideTipo && coincideUsuario && visible) seleccionadas += document
                if (seleccionadas.size > pageSize) {
                    hayMas = true
                    break
                }
            }

            if (!hayMas) {
                agotado = snapshot.size() < pageSize + 1
                cursor = snapshot.documents.lastOrNull() ?: cursor
            }
        }

        val documentosPagina = seleccionadas.take(pageSize)
        val siguienteCursor = if (hayMas) documentosPagina.lastOrNull() else ultimoEscaneado
        return PaginaFirestore(
            documentosPagina.map { it.toPublicacion() },
            siguienteCursor,
            hayMas
        )
    }

    private fun perteneceAUsuario(document: DocumentSnapshot, uid: String): Boolean {
        val idLegacy = legacyId(uid)
        return document.getString("usuarioId") == uid ||
            (document.get("usuarioId") as? Number)?.toLong() == idLegacy ||
            (document.get("usuarioLegacyId") as? Number)?.toLong() == idLegacy
    }

    fun obtenerPaginaAlbergues(after: DocumentSnapshot?, pageSize: Int = 24): PaginaFirestore<Albergue> {
        require(pageSize > 0)
        var query: Query = db.collection(SHELTERS).orderBy("nombre").limit(pageSize.toLong() + 1)
        if (after != null) query = query.startAfter(after)
        val snapshot = await(query.get())
        val documents = snapshot.documents.take(pageSize)
        return PaginaFirestore(
            documents.map { it.toAlbergue() },
            documents.lastOrNull(),
            snapshot.size() > pageSize
        )
    }

    fun obtenerPropias(): List<Publicacion> =
        await(db.collection(PUBLICATIONS).whereEqualTo("usuarioId", currentUid()).get()).documents
            .map { it.toPublicacion() }.filter { it.estado != "Eliminada" }
            .sortedByDescending { it.fechaCreacion }

    fun contarPropias(): Long = try {
        val uid = currentUid()
        val total = await(
            db.collection(PUBLICATIONS).whereEqualTo("usuarioId", uid)
                .count().get(AggregateSource.SERVER)
        ).count
        val retiradas = await(
            db.collection(PUBLICATIONS)
                .whereEqualTo("usuarioId", uid)
                .whereEqualTo("estado", "Eliminada")
                .count().get(AggregateSource.SERVER)
        ).count
        total - retiradas
    } catch (_: Exception) {
        obtenerPropias().size.toLong()
    }

    fun obtenerPerdidas(): List<Publicacion> =
        obtenerPublicaciones().filter { it.tipo == "Perdida" && it.estado == "Activa" }

    fun obtenerEncontradas(): List<Publicacion> =
        obtenerPublicaciones().filter { it.tipo == "Encontrada" && it.estado == "Activa" }

    fun obtenerAdopciones(): List<Publicacion> =
        obtenerPublicaciones().filter { it.tipo == "Adopcion" && it.estado == "Activa" }

    fun obtenerPorIdPublicacion(id: Long): Publicacion? =
        await(db.collection(PUBLICATIONS).document(id.toString()).get()).takeIf { it.exists() }?.toPublicacion()

    private fun ubicacionPrivadaPublicacion(uid: String, id: Long) = db.collection(USERS)
        .document(uid).collection("ubicaciones_publicaciones").document(id.toString())

    fun insertarAvistamiento(
        publicacionId: Long?,
        usuarioId: Long?,
        latitud: Double,
        longitud: Double,
        descripcion: String,
        foto: String?,
        fechaAvistamiento: Long = System.currentTimeMillis(),
        especie: String? = null
    ): Long {
        val uid = currentUid()
        val id = newId()
        val fotoFinal = foto?.let(::procesarFoto)
        val latitudAproximada = aproximarCoordenada(latitud)
        val longitudAproximada = aproximarCoordenada(longitud)
        val sighting = Avistamiento(
            id,
            publicacionId ?: 0L,
            usuarioId ?: legacyId(currentUid()),
            latitudAproximada,
            longitudAproximada,
            descripcion,
            fotoFinal,
            fechaAvistamiento.coerceIn(
                System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000,
                System.currentTimeMillis()
            ),
            especie = especie
        )
        val batch = db.batch()
        val pubSnapshot = publicacionId?.let {
            await(db.collection(PUBLICATIONS).document(it.toString()).get())
        }
        if (publicacionId != null) {
            check(pubSnapshot?.exists() == true && pubSnapshot.getString("tipo") == "Perdida" && pubSnapshot.getString("estado") == "Activa") {
                "Esta alerta ya no admite avistamientos"
            }
        }
        batch.set(db.collection(SIGHTINGS).document(id.toString()), sighting.toFirestoreMap(uid))
        batch.set(
            db.collection(SIGHTINGS).document(id.toString())
                .collection("ubicacion_privada").document("coordenadas"),
            mapOf("latitud" to latitud, "longitud" to longitud)
        )
        val ownerUid = pubSnapshot?.getString("usuarioId")
        val petName = pubSnapshot?.getString("nombre") ?: "la mascota"
        if (!ownerUid.isNullOrEmpty() && ownerUid != uid) {
            val distancia = FloatArray(1)
            Location.distanceBetween(
                pubSnapshot.getDouble("latitud") ?: latitud,
                pubSnapshot.getDouble("longitud") ?: longitud,
                latitud,
                longitud,
                distancia
            )
            val distanciaTexto = if (distancia[0] < 1000f) {
                "~${distancia[0].roundToInt()} m del último punto"
            } else {
                "~${String.format(Locale.getDefault(), "%.1f", distancia[0] / 1000f)} km del último punto"
            }
            val notifId = id
            val notif = hashMapOf(
                "id" to notifId.toString(),
                "usuarioDestinoId" to ownerUid,
                "usuarioOrigenId" to uid,
                "tipo" to "avistamiento",
                "publicacionId" to sighting.publicacionId.toString(),
                "avistamientoId" to id.toString(),
                "mensaje" to "Vieron a $petName a $distanciaTexto · ${com.example.practicafinal.util.fechaRelativa(sighting.fecha)}",
                "latitud" to latitud,
                "longitud" to longitud,
                "fecha" to System.currentTimeMillis(),
                "leida" to false
            )
            batch.set(db.collection(NOTIFICATIONS).document(notifId.toString()), notif)
        }
        await(batch.commit())
        return id
    }

    fun obtenerAvistamientos(): List<Avistamiento> =
        await(db.collection(SIGHTINGS).orderBy("fecha").get()).documents
            .asReversed().map { it.toAvistamiento() }

    fun obtenerAvistamientosPorPublicacion(publicacionId: Long): List<Avistamiento> =
        await(db.collection(SIGHTINGS).whereEqualTo("publicacionId", publicacionId.toString()).get())
            .documents.map { it.toAvistamiento() }

    fun marcarResuelta(id: Long) {
        val uid = currentUid()
        val ref = db.collection(PUBLICATIONS).document(id.toString())
        val snapshot = await(ref.get())
        check(snapshot.getString("usuarioId") == currentUid()) {
            "Solo el dueño puede marcar la publicación como resuelta"
        }
        await(ref.update("estado", "Resuelta"))
    }

    fun confirmarAlertaPerdida(id: Long) {
        val uid = currentUid()
        val ref = db.collection(PUBLICATIONS).document(id.toString())
        val snapshot = await(ref.get())
        check(snapshot.exists() && snapshot.getString("usuarioId") == uid &&
            snapshot.getString("tipo") == "Perdida" && snapshot.getString("estado") == "Activa") {
            "Solo puedes confirmar tus alertas perdidas activas"
        }
        await(ref.update("ultimaConfirmacion", System.currentTimeMillis()))
    }

    fun actualizarRevisionAvistamiento(id: Long, estado: String) {
        require(estado in setOf("Pendiente", "Coincide", "No coincide", "Duplicado"))
        val ref = db.collection(SIGHTINGS).document(id.toString())
        val snapshot = await(ref.get())
        val publicationId = snapshot.getString("publicacionId")?.toLongOrNull()
            ?: (snapshot.get("publicacionId") as? Number)?.toLong()
            ?: error("No se encontró la alerta vinculada")
        check(publicationId > 0L) { "Vincula el avistamiento con una alerta antes de revisarlo" }
        val publication = await(db.collection(PUBLICATIONS).document(publicationId.toString()).get())
        check(publication.getString("usuarioId") == currentUid()) {
            "Solo quien publicó la alerta puede revisar este avistamiento"
        }
        await(ref.update("estadoRevision", estado))
    }

    fun vincularAvistamiento(id: Long, publicacionId: Long) {
        val uid = currentUid()
        val publication = await(db.collection(PUBLICATIONS).document(publicacionId.toString()).get())
        check(publication.exists() && publication.getString("usuarioId") == uid &&
            publication.getString("tipo") == "Perdida" && publication.getString("estado") == "Activa") {
            "Solo puedes vincularlo a tu alerta perdida activa"
        }
        val ref = db.collection(SIGHTINGS).document(id.toString())
        val sighting = await(ref.get())
        check(sighting.exists() && sighting.getString("publicacionId") == "0") {
            "Este reporte ya está vinculado a una alerta"
        }
        val especieSighting = sighting.getString("especie")
        val especiePublication = publication.getString("especie")
        check(especieSighting.isNullOrBlank() || especiePublication.isNullOrBlank() ||
            especieSighting.equals(especiePublication, ignoreCase = true)) {
            "La especie del reporte no coincide con tu alerta"
        }
        await(ref.update(mapOf(
            "publicacionId" to publicacionId.toString(),
            "estadoRevision" to "Pendiente"
        )))
    }

    fun marcarAdoptada(id: Long) {
        val uid = currentUid()
        val ref = db.collection(PUBLICATIONS).document(id.toString())
        val snapshot = await(ref.get())
        check(snapshot.getString("usuarioId") == currentUid()) {
            "Solo el dueño puede marcar la publicación como adoptada"
        }
        await(ref.update("estado", "Adoptada"))
    }

    fun actualizarAvistamiento(id: Long, latitud: Double, longitud: Double) {
        val uid = currentUid()
        val ref = db.collection(SIGHTINGS).document(id.toString())
        val snapshot = await(ref.get())
        check(snapshot.getString("usuarioId") == uid) {
            "Solo quien reportó el avistamiento puede actualizarlo"
        }
        val batch = db.batch()
        batch.update(ref, mapOf(
            "latitud" to aproximarCoordenada(latitud),
            "longitud" to aproximarCoordenada(longitud)
        ))
        batch.set(
            ref.collection("ubicacion_privada").document("coordenadas"),
            mapOf("latitud" to latitud, "longitud" to longitud)
        )
        await(batch.commit())
    }

    fun obtenerCoordenadasAvistamiento(id: Long): Pair<Double, Double>? {
        val snapshot = await(db.collection(SIGHTINGS).document(id.toString())
            .collection("ubicacion_privada").document("coordenadas").get())
        if (!snapshot.exists()) return null
        val latitud = (snapshot.get("latitud") as? Number)?.toDouble() ?: return null
        val longitud = (snapshot.get("longitud") as? Number)?.toDouble() ?: return null
        return latitud to longitud
    }

    fun obtenerAlbergues(): List<Albergue> =
        await(db.collection(SHELTERS).get()).documents.map { it.toAlbergue() }

    fun insertarDenuncia(
        motivo: String,
        descripcion: String,
        foto: String?,
        latitud: Double,
        longitud: Double
    ): Long {
        val uid = currentUid()
        val id = newId()
        val fotoFinal = foto?.let(::procesarFoto)
        val report = Denuncia(id, motivo, descripcion, fotoFinal, latitud, longitud, System.currentTimeMillis())
        await(db.collection(REPORTS).document(id.toString()).set(report.toFirestoreMap(uid)))
        return id
    }

    fun obtenerDenuncias(): List<Denuncia> {
        val uid = currentUid()
        if (uid.isEmpty()) return emptyList()
        return await(db.collection(REPORTS).whereEqualTo("usuarioId", uid).get()).documents
            .map { it.toDenuncia() }.sortedByDescending { it.fecha }
    }

    fun contarDenuncias(): Long = try {
        await(
            db.collection(REPORTS)
                .whereEqualTo("usuarioId", currentUid())
                .count()
                .get(AggregateSource.SERVER)
        ).count
    } catch (_: Exception) {
        obtenerDenuncias().size.toLong()
    }

    fun obtenerNotificaciones(): List<Map<String, Any>> {
        val uid = currentUid()
        if (uid.isEmpty()) return emptyList()
        return await(db.collection(NOTIFICATIONS)
            .whereEqualTo("usuarioDestinoId", uid).get()).documents.mapNotNull { doc ->
            val map = mutableMapOf<String, Any>()
            map["id"] = doc.id
            map["mensaje"] = doc.getString("mensaje").orEmpty()
            map["tipo"] = doc.getString("tipo").orEmpty()
            map["publicacionId"] = doc.getString("publicacionId").orEmpty()
            map["fecha"] = (doc.get("fecha") as? Number)?.toLong() ?: 0L
            map["leida"] = doc.getBoolean("leida") ?: false
            map["latitud"] = (doc.get("latitud") as? Number)?.toDouble() ?: 0.0
            map["longitud"] = (doc.get("longitud") as? Number)?.toDouble() ?: 0.0
            map["usuarioOrigenId"] = doc.getString("usuarioOrigenId").orEmpty()
            map
        }.sortedByDescending { it["fecha"] as Long }
    }

    fun contarNoLeidas(): Int {
        val uid = currentUid()
        if (uid.isEmpty()) return 0
        return await(db.collection(NOTIFICATIONS)
            .whereEqualTo("usuarioDestinoId", uid)
            .whereEqualTo("leida", false)
            .get()).size()
    }

    fun marcarNotificacionLeida(notifId: String) {
        await(db.collection(NOTIFICATIONS).document(notifId).update("leida", true))
    }

    fun marcarTodasLeidas() {
        val uid = currentUid()
        if (uid.isEmpty()) return
        val snapshot = await(db.collection(NOTIFICATIONS)
            .whereEqualTo("usuarioDestinoId", uid)
            .whereEqualTo("leida", false)
            .get())
        snapshot.documents.chunked(500).forEach { documents ->
            val batch = db.batch()
            documents.forEach { batch.update(it.reference, "leida", true) }
            await(batch.commit())
        }
    }

    fun observarPublicaciones(
        onChanged: (List<Publicacion>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration = db.collection(PUBLICATIONS)
        .orderBy("fechaCreacion", Query.Direction.DESCENDING)
        .limit(500)
        .addSnapshotListener { snapshot, error ->
            if (error != null) onError(error)
            else onChanged(snapshot?.documents.orEmpty().map { it.toPublicacion() }.filter { it.estado == "Activa" })
        }

    fun observarAvistamientos(
        onChanged: (List<Avistamiento>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration = db.collection(SIGHTINGS)
        .orderBy("fecha", Query.Direction.DESCENDING)
        .limit(500)
        .addSnapshotListener { snapshot, error ->
            if (error != null) onError(error)
            else onChanged(snapshot?.documents.orEmpty().map { it.toAvistamiento() })
        }

    fun observarAlbergues(
        onChanged: (List<Albergue>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration = db.collection(SHELTERS)
        .orderBy("nombre")
        .limit(300)
        .addSnapshotListener { snapshot, error ->
            if (error != null) onError(error)
            else onChanged(snapshot?.documents.orEmpty().map { it.toAlbergue() })
        }

    fun observarDenuncias(
        onChanged: (List<Denuncia>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration = db.collection(REPORTS).whereEqualTo("usuarioId", currentUid())
        .addSnapshotListener { snapshot, error ->
            if (error != null) onError(error)
            else onChanged(snapshot?.documents.orEmpty().map { it.toDenuncia() })
        }

    private fun newId(): Long = (UUID.randomUUID().mostSignificantBits and Long.MAX_VALUE).let { if (it == 0L) 1L else it }
    private fun aproximarCoordenada(coordenada: Double): Double = round(coordenada * 1000.0) / 1000.0
    private fun legacyId(uid: String): Long = (uid.hashCode().toLong() and Long.MAX_VALUE).let { if (it == 0L) 1L else it }

    private fun DocumentSnapshot.toUsuario(uid: String?): Usuario = Usuario(
        (get("legacyId") as? Number)?.toLong() ?: uid?.let(::legacyId) ?: id.toLongOrNull() ?: 0L,
        getString("nombre").orEmpty(),
        getString("correo").orEmpty(),
        getString("tipo") ?: "Ciudadano",
        getString("ciudad").orEmpty()
    )

    private fun DocumentSnapshot.toPublicacion(): Publicacion {
        val nombre = getString("nombre").orEmpty().trim().let {
            if (it.isBlank() || it.equals("desconocido", ignoreCase = true)) "Mascota sin nombre" else it
        }
        val latitude = (get("latitud") as? Number)?.toDouble() ?: 0.0
        val longitude = (get("longitud") as? Number)?.toDouble() ?: 0.0
        val esPropietario = getString("usuarioId") == FirebaseAuth.getInstance().currentUser?.uid
        return Publicacion(
            id.toLongOrNull() ?: 0L,
            (get("usuarioLegacyId") as? Number)?.toLong()
                ?: getString("usuarioId")?.let(::legacyId),
            getString("tipo").orEmpty(),
            nombre,
            getString("descripcion").orEmpty(),
            getString("foto"),
            getString("ultimoLugar"),
            getString("especie"),
            if (esPropietario) latitude else aproximarCoordenada(latitude),
            if (esPropietario) longitude else aproximarCoordenada(longitude),
            (get("fechaCreacion") as? Number)?.toLong() ?: 0L,
            getString("estado") ?: "Activa",
            getString("contactoWhatsapp"),
            (get("fechaPerdida") as? Number)?.toLong(),
            getString("recompensa"),
            (get("ultimaConfirmacion") as? Number)?.toLong(),
            getString("color"),
            getString("tamano"),
            getString("senasParticulares")
        )
    }

    private fun DocumentSnapshot.toAvistamiento(): Avistamiento = Avistamiento(
        id.toLongOrNull() ?: 0L,
        getString("publicacionId")?.toLongOrNull()
            ?: (get("publicacionId") as? Number)?.toLong() ?: 0L,
        (get("usuarioLegacyId") as? Number)?.toLong()
            ?: getString("usuarioId")?.let(::legacyId),
        ((get("latitud") as? Number)?.toDouble() ?: 0.0).let(::aproximarCoordenada),
        ((get("longitud") as? Number)?.toDouble() ?: 0.0).let(::aproximarCoordenada),
        getString("descripcion").orEmpty(),
        getString("foto"),
        (get("fecha") as? Number)?.toLong() ?: 0L,
        getString("especie"),
        getString("estadoRevision") ?: "Pendiente"
    )

    private fun DocumentSnapshot.toAlbergue(): Albergue = Albergue(
        id.toLongOrNull() ?: 0L,
        getString("nombre").orEmpty(),
        getString("descripcion").orEmpty(),
        getString("direccion").orEmpty(),
        getString("telefono").orEmpty(),
        getString("foto"),
        (get("latitud") as? Number)?.toDouble() ?: 0.0,
        (get("longitud") as? Number)?.toDouble() ?: 0.0
    )

    private fun DocumentSnapshot.toDenuncia(): Denuncia = Denuncia(
        id.toLongOrNull() ?: 0L,
        getString("motivo").orEmpty(),
        getString("descripcion").orEmpty(),
        getString("foto"),
        (get("latitud") as? Number)?.toDouble() ?: 0.0,
        (get("longitud") as? Number)?.toDouble() ?: 0.0,
        (get("fecha") as? Number)?.toLong() ?: 0L
    )
}
