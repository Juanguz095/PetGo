package com.example.practicafinal.db

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import com.bumptech.glide.Glide
import android.util.Base64
import com.example.practicafinal.modelo.Albergue
import com.example.practicafinal.modelo.Avistamiento
import com.example.practicafinal.modelo.Denuncia
import com.example.practicafinal.modelo.Publicacion
import com.example.practicafinal.modelo.Usuario
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.util.UUID
import java.util.concurrent.TimeUnit

class FirebaseRepository(private val context: Context) {

    companion object {
        const val MAX_IMAGE_BYTES = 5L * 1024L * 1024L
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

    fun registrar(nombre: String, correo: String, contrasena: String, tipo: String = "Ciudadano", ciudad: String = ""): Long {
        val normalizedEmail = correo.trim().lowercase(java.util.Locale.ROOT)
        val firebaseUser = checkNotNull(await(auth.createUserWithEmailAndPassword(normalizedEmail, contrasena)).user)
        val id = legacyId(firebaseUser.uid)
        val user = Usuario(id, nombre.trim(), normalizedEmail, tipo, ciudad.trim())
        await(db.collection(USERS).document(firebaseUser.uid).set(user.toFirestoreMap(firebaseUser.uid)))
        com.example.practicafinal.session.SesionManager.guardarSesion(context, id, firebaseUser.uid)
        return id
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

    fun insertarPublicacion(
        usuarioId: Long?,
        tipo: String,
        nombre: String,
        descripcion: String,
        foto: String?,
        ultimoLugar: String?,
        especie: String?,
        latitud: Double,
        longitud: Double
    ): Long {
        val uid = currentUid()
        val id = newId()
        val fotoFinal = foto?.let { procesarFoto(it) }
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
            estado = "Activa"
        )
        await(db.collection(PUBLICATIONS).document(id.toString()).set(publication.toFirestoreMap(uid)))
        return id
    }

    private fun procesarFoto(value: String): String {
        val uri = Uri.parse(value)
        if (uri.scheme in listOf("http", "https", "android.resource")) return value
        val target = Glide.with(context).asBitmap().load(uri).submit(600, 600)
        try {
            val bitmap = target.get(30, TimeUnit.SECONDS)
            val scale = minOf(1f, 600f / maxOf(bitmap.width, bitmap.height))
            val resized = if (scale < 1f) Bitmap.createScaledBitmap(
                bitmap, maxOf(1, (bitmap.width * scale).toInt()), maxOf(1, (bitmap.height * scale).toInt()), true
            ) else bitmap
            try {
                val output = java.io.ByteArrayOutputStream()
                check(resized.compress(Bitmap.CompressFormat.JPEG, 60, output)) { "No se pudo comprimir la foto" }
                val bytes = output.toByteArray()
                check(bytes.size <= 700 * 1024) { "La foto supera el tamaño permitido" }
                return "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
            } finally {
                if (resized !== bitmap) resized.recycle()
            }
        } catch (error: Exception) {
            throw IllegalArgumentException("No se pudo leer la foto. Selecciónala de nuevo.", error)
        } finally {
            Glide.with(context).clear(target)
        }
    }

    fun obtenerPublicaciones(): List<Publicacion> =
        await(db.collection(PUBLICATIONS).orderBy("fechaCreacion").get()).documents
            .asReversed().map { it.toPublicacion() }

    fun obtenerPropias(): List<Publicacion> =
        await(db.collection(PUBLICATIONS).whereEqualTo("usuarioId", currentUid()).get()).documents
            .map { it.toPublicacion() }.sortedByDescending { it.fechaCreacion }

    fun obtenerPerdidas(): List<Publicacion> =
        obtenerPublicaciones().filter { it.tipo == "Perdida" }

    fun obtenerAdopciones(): List<Publicacion> =
        obtenerPublicaciones().filter { it.tipo == "Adopcion" && it.estado == "Activa" }

    fun obtenerPorIdPublicacion(id: Long): Publicacion? =
        await(db.collection(PUBLICATIONS).document(id.toString()).get()).takeIf { it.exists() }?.toPublicacion()

    fun insertarAvistamiento(
        publicacionId: Long,
        usuarioId: Long?,
        latitud: Double,
        longitud: Double,
        descripcion: String,
        foto: String?
    ): Long {
        val uid = currentUid()
        val id = newId()
        val fotoFinal = foto?.let { procesarFoto(it) }
        val sighting = Avistamiento(
            id,
            publicacionId,
            usuarioId ?: legacyId(currentUid()),
            latitud,
            longitud,
            descripcion,
            fotoFinal,
            System.currentTimeMillis()
        )
        val batch = db.batch()
        val pubSnapshot = await(db.collection(PUBLICATIONS).document(publicacionId.toString()).get())
        check(pubSnapshot.exists() && pubSnapshot.getString("tipo") == "Perdida" && pubSnapshot.getString("estado") == "Activa") {
            "Esta alerta ya no admite avistamientos"
        }
        batch.set(db.collection(SIGHTINGS).document(id.toString()), sighting.toFirestoreMap(uid))
        val ownerUid = pubSnapshot.getString("usuarioId")
        val petName = pubSnapshot.getString("nombre") ?: "la mascota"
        if (!ownerUid.isNullOrEmpty() && ownerUid != uid) {
            val notifId = newId()
            val notif = hashMapOf(
                "id" to notifId.toString(),
                "usuarioDestinoId" to ownerUid,
                "usuarioOrigenId" to uid,
                "tipo" to "avistamiento",
                "publicacionId" to publicacionId.toString(),
                "mensaje" to "Vieron a $petName",
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
        await(ref.update(mapOf("latitud" to latitud, "longitud" to longitud)))
    }

    fun insertarAlbergue(
        nombre: String,
        descripcion: String,
        direccion: String,
        telefono: String,
        foto: String?,
        latitud: Double,
        longitud: Double
    ): Long {
        val id = newId()
        val shelter = Albergue(id, nombre, descripcion, direccion, telefono, foto, latitud, longitud)
        await(db.collection(SHELTERS).document(id.toString()).set(shelter.toFirestoreMap() + mapOf("usuarioId" to currentUid())))
        return id
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
        val fotoFinal = foto?.let { procesarFoto(it) }
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

    fun seedIfNeeded() {
        val prefs = context.getSharedPreferences("seed_status", android.content.Context.MODE_PRIVATE)
        if (prefs.getBoolean("seeded", false)) return
        val publications = db.collection(PUBLICATIONS)
        if (await(publications.limit(1).get()).isEmpty) {
            val now = System.currentTimeMillis()
            val seeds = listOf(
                Publicacion(1000000001L, legacyId(currentUid()), "Perdida", "Max", "Se perdió cerca del Centro de Lima", null, "Centro de Lima", "Perro", -12.0464, -77.0428, now, "Activa"),
                Publicacion(1000000002L, legacyId(currentUid()), "Perdida", "Michi", "Se perdió en La Victoria", null, "La Victoria", "Gato", -12.0670, -77.0337, now - 1, "Activa"),
                Publicacion(1000000003L, legacyId(currentUid()), "Encontrada", "Luna", "Encontrada en Lince, busca dueño", null, "Lince", "Perro", -12.0911, -77.0359, now - 2, "Activa"),
                Publicacion(1000000004L, legacyId(currentUid()), "Adopcion", "Bella", "Perrita cariñosa en busca de hogar", "android.resource://com.example.practicafinal/drawable/perro_adopcion", null, "Perro", -12.0580, -77.0360, now - 3, "Activa"),
                Publicacion(1000000005L, legacyId(currentUid()), "Adopcion", "Simba", "Gatito juguetón esperando adopción", "android.resource://com.example.practicafinal/drawable/gato_adopcion", null, "Gato", -12.0700, -77.0480, now - 4, "Activa")
            )
            seeds.forEach { p ->
                await(publications.document(p.id.toString()).set(p.toFirestoreMap(currentUid())))
            }
        }
        val shelters = db.collection(SHELTERS)
        if (await(shelters.limit(1).get()).isEmpty) {
            val seeds = listOf(
                Albergue(2000000001L, "Albergue Patitas", "Refugio de mascotas", "Av. Universitaria 123", "999888777", "android.resource://com.example.practicafinal/drawable/albergue_patitas", -12.0850, -77.0050),
                Albergue(2000000002L, "Refugio Huellitas", "Hogar temporal", "Jr. Las Flores 456", "987654321", "android.resource://com.example.practicafinal/drawable/albergue_huellitas", -12.0200, -77.0800),
                Albergue(2000000003L, "Hogar Peludo", "Adopción responsable", "Calle Los Olivos 789", "912345678", "android.resource://com.example.practicafinal/drawable/albergue_peludo", -12.0760, -77.0620)
            )
            seeds.forEach { a -> await(shelters.document(a.id.toString()).set(a.toFirestoreMap() + mapOf("usuarioId" to currentUid()))) }
        }
        prefs.edit().putBoolean("seeded", true).apply()
    }

    fun observarPublicaciones(
        onChanged: (List<Publicacion>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration = db.collection(PUBLICATIONS).orderBy("fechaCreacion")
        .addSnapshotListener { snapshot, error ->
            if (error != null) onError(error)
            else onChanged(snapshot?.documents.orEmpty().asReversed().map { it.toPublicacion() })
        }

    fun observarAvistamientos(
        onChanged: (List<Avistamiento>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration = db.collection(SIGHTINGS).orderBy("fecha")
        .addSnapshotListener { snapshot, error ->
            if (error != null) onError(error)
            else onChanged(snapshot?.documents.orEmpty().asReversed().map { it.toAvistamiento() })
        }

    fun observarAlbergues(
        onChanged: (List<Albergue>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration = db.collection(SHELTERS)
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
    private fun legacyId(uid: String): Long = (uid.hashCode().toLong() and Long.MAX_VALUE).let { if (it == 0L) 1L else it }

    private fun DocumentSnapshot.toUsuario(uid: String?): Usuario = Usuario(
        (get("legacyId") as? Number)?.toLong() ?: uid?.let(::legacyId) ?: id.toLongOrNull() ?: 0L,
        getString("nombre").orEmpty(),
        getString("correo").orEmpty(),
        getString("tipo") ?: "Ciudadano",
        getString("ciudad").orEmpty()
    )

    private fun DocumentSnapshot.toPublicacion(): Publicacion = Publicacion(
        id.toLongOrNull() ?: 0L,
        (get("usuarioLegacyId") as? Number)?.toLong()
            ?: getString("usuarioId")?.let(::legacyId),
        getString("tipo").orEmpty(),
        getString("nombre").orEmpty(),
        getString("descripcion").orEmpty(),
        getString("foto"),
        getString("ultimoLugar"),
        getString("especie"),
        (get("latitud") as? Number)?.toDouble() ?: 0.0,
        (get("longitud") as? Number)?.toDouble() ?: 0.0,
        (get("fechaCreacion") as? Number)?.toLong() ?: 0L,
        getString("estado") ?: "Activa"
    )

    private fun DocumentSnapshot.toAvistamiento(): Avistamiento = Avistamiento(
        id.toLongOrNull() ?: 0L,
        getString("publicacionId")?.toLongOrNull()
            ?: (get("publicacionId") as? Number)?.toLong() ?: 0L,
        (get("usuarioLegacyId") as? Number)?.toLong()
            ?: getString("usuarioId")?.let(::legacyId),
        (get("latitud") as? Number)?.toDouble() ?: 0.0,
        (get("longitud") as? Number)?.toDouble() ?: 0.0,
        getString("descripcion").orEmpty(),
        getString("foto"),
        (get("fecha") as? Number)?.toLong() ?: 0L
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
