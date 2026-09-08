package com.example.practicafinal.db

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
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
import com.google.firebase.storage.FirebaseStorage
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.math.abs

class FirebaseRepository(private val context: Context) {

    companion object {
        const val MAX_IMAGE_BYTES = 5L * 1024L * 1024L
        private const val USERS = "usuarios"
        private const val PUBLICATIONS = "publicaciones"
        private const val SIGHTINGS = "avistamientos"
        private const val SHELTERS = "albergues"
        private const val REPORTS = "denuncias"
    }

    private val db: FirebaseFirestore
        get() = FirebaseFirestore.getInstance()
    private val auth: FirebaseAuth
        get() = FirebaseAuth.getInstance()
    private val storage: FirebaseStorage
        get() = FirebaseStorage.getInstance()

    private fun currentUid(): String = com.example.practicafinal.session.SesionManager.currentFirebaseUid(context) ?: ""
    private fun <T> await(task: Task<T>): T = Tasks.await(task, 30, TimeUnit.SECONDS)

    private fun restApiKey(): String = "AIzaSyB0wKntYPlNf-0IqgXowwkmCSdviVJIejg"

    private fun restSignUp(email: String, password: String): Pair<String, String> {
        val key = restApiKey()
        val url = URL("https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=$key")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            doOutput = true
        }
        val body = JSONObject().apply {
            put("email", email)
            put("password", password)
            put("returnSecureToken", true)
        }
        OutputStreamWriter(conn.outputStream).use { it.write(body.toString()) }
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val response = stream.bufferedReader().use { it.readText() }
        val json = JSONObject(response)
        if (code !in 200..299) {
            val msg = json.optJSONObject("error")?.optString("message") ?: "Error desconocido"
            throw Exception(msg)
        }
        return json.getString("idToken") to json.getString("localId")
    }

    private fun restSignIn(email: String, password: String): Pair<String, String> {
        val key = restApiKey()
        val url = URL("https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=$key")
        val conn = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            doOutput = true
        }
        val body = JSONObject().apply {
            put("email", email)
            put("password", password)
            put("returnSecureToken", true)
        }
        OutputStreamWriter(conn.outputStream).use { it.write(body.toString()) }
        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        val response = stream.bufferedReader().use { it.readText() }
        val json = JSONObject(response)
        if (code !in 200..299) {
            val msg = json.optJSONObject("error")?.optString("message") ?: "Error desconocido"
            throw Exception(msg)
        }
        return json.getString("idToken") to json.getString("localId")
    }

    fun registrar(nombre: String, correo: String, contrasena: String, tipo: String = "Ciudadano"): Long {
        val (_, localId) = restSignUp(correo.trim().lowercase(), contrasena)
        val id = legacyId(localId)
        com.example.practicafinal.session.SesionManager.guardarFirebaseUid(context, localId)
        val user = Usuario(id, nombre.trim(), correo.trim().lowercase(), tipo)
        await(db.collection(USERS).document(localId).set(user.toFirestoreMap(localId)))
        return id
    }

    fun validarLogin(correo: String, contrasena: String): Usuario? {
        val (_, localId) = restSignIn(correo.trim().lowercase(), contrasena)
        com.example.practicafinal.session.SesionManager.guardarFirebaseUid(context, localId)
        val snapshot = await(db.collection(USERS).document(localId).get())
        if (snapshot.exists()) return snapshot.toUsuario(localId)
        val user = Usuario(
            legacyId(localId),
            correo.trim().lowercase().substringBefore("@"),
            correo.trim().lowercase(),
            "Ciudadano"
        )
        await(db.collection(USERS).document(localId).set(user.toFirestoreMap(localId)))
        return user
    }

    fun obtenerPorId(id: Long): Usuario? {
        val result = await(db.collection(USERS).whereEqualTo("legacyId", id).limit(1).get())
        return result.documents.firstOrNull()?.toUsuario(result.documents.first().getString("uid"))
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
        val uploadedPhoto = foto?.let { uploadPhoto(it, "publicaciones/$id.jpg") }
        val publication = Publicacion(
            id = id,
            usuarioId = usuarioId ?: legacyId(uid),
            tipo = tipo,
            nombre = nombre,
            descripcion = descripcion,
            foto = uploadedPhoto,
            ultimoLugar = ultimoLugar,
            especie = especie,
            latitud = latitud,
            longitud = longitud,
            fechaCreacion = System.currentTimeMillis(),
            estado = "Activa"
        )
        await(db.collection(PUBLICATIONS).document(id.toString()).set(publication.toFirestoreMap(currentUid())))
        return id
    }

    fun obtenerPublicaciones(): List<Publicacion> =
        await(db.collection(PUBLICATIONS).orderBy("fechaCreacion").get()).documents
            .asReversed().map { it.toPublicacion() }

    fun obtenerPerdidas(): List<Publicacion> =
        obtenerPublicaciones().filter { it.tipo == "Perdida" }

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
        val uploadedPhoto = foto?.let { uploadPhoto(it, "avistamientos/$id.jpg") }
        val sighting = Avistamiento(
            id,
            publicacionId,
            usuarioId ?: legacyId(currentUid()),
            latitud,
            longitud,
            descripcion,
            uploadedPhoto,
            System.currentTimeMillis()
        )
        await(db.collection(SIGHTINGS).document(id.toString()).set(sighting.toFirestoreMap(currentUid())))
        return id
    }

    fun obtenerAvistamientos(): List<Avistamiento> =
        await(db.collection(SIGHTINGS).orderBy("fecha").get()).documents
            .asReversed().map { it.toAvistamiento() }

    fun obtenerAvistamientosPorPublicacion(publicacionId: Long): List<Avistamiento> =
        obtenerAvistamientos().filter { it.publicacionId == publicacionId }

    fun marcarResuelta(id: Long) {
        val uid = currentUid()
        val ref = db.collection(PUBLICATIONS).document(id.toString())
        val snapshot = await(ref.get())
        check(snapshot.getString("usuarioId") == currentUid()) {
            "Solo el dueño puede marcar la publicación como resuelta"
        }
        await(ref.update("estado", "Resuelta"))
    }

    fun actualizarAvistamiento(id: Long, latitud: Double, longitud: Double) {
        val uid = currentUid()
        val ref = db.collection(PUBLICATIONS).document(id.toString())
        val snapshot = await(ref.get())
        check(snapshot.getString("usuarioId") == currentUid()) {
            "Solo el dueño puede actualizar esta publicación"
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
        await(db.collection(SHELTERS).document(id.toString()).set(shelter.toFirestoreMap()))
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
        val uploadedPhoto = foto?.let { uploadPhoto(it, "denuncias/$id.jpg") }
        val report = Denuncia(id, motivo, descripcion, uploadedPhoto, latitud, longitud, System.currentTimeMillis())
        await(db.collection(REPORTS).document(id.toString()).set(report.toFirestoreMap(currentUid())))
        return id
    }

    fun obtenerDenuncias(): List<Denuncia> {
        val uid = currentUid()
        if (uid.isEmpty()) return emptyList()
        return await(db.collection(REPORTS).whereEqualTo("usuarioId", uid).get()).documents
            .map { it.toDenuncia() }.sortedByDescending { it.fecha }
    }

    fun seedIfNeeded() {
        val uid = currentUid()
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
            seeds.forEach { a -> await(shelters.document(a.id.toString()).set(a.toFirestoreMap())) }
        }
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

    private fun uploadPhoto(value: String, path: String): String {
        val uri = Uri.parse(value)
        if (uri.scheme == "http" || uri.scheme == "https" || uri.scheme == "android.resource") return value
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: throw IllegalArgumentException("No se pudo leer la imagen")
        require(bytes.size.toLong() <= MAX_IMAGE_BYTES) { "La imagen supera 5 MB" }
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        val payload = if (bitmap == null) bytes else compress(bitmap)
        val reference = storage.reference.child(path)
        await(reference.putBytes(payload))
        return await(reference.downloadUrl).toString()
    }

    private fun compress(bitmap: Bitmap): ByteArray {
        val maxDimension = 1600
        val scale = minOf(1f, maxDimension.toFloat() / maxOf(bitmap.width, bitmap.height))
        val resized = if (scale < 1f) Bitmap.createScaledBitmap(
            bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true
        ) else bitmap
        var quality = 85
        var result: ByteArray
        do {
            val output = ByteArrayOutputStream()
            resized.compress(Bitmap.CompressFormat.JPEG, quality, output)
            result = output.toByteArray()
            quality -= 10
        } while (result.size > MAX_IMAGE_BYTES && quality >= 35)
        check(result.size.toLong() <= MAX_IMAGE_BYTES) { "La imagen comprimida supera 5 MB" }
        if (resized !== bitmap) resized.recycle()
        return result
    }

    private fun newId(): Long = abs(UUID.randomUUID().mostSignificantBits).let { if (it == 0L) 1L else it }
    private fun legacyId(uid: String): Long = (uid.hashCode().toLong() and Long.MAX_VALUE).let { if (it == 0L) 1L else it }

    private fun DocumentSnapshot.toUsuario(uid: String?): Usuario = Usuario(
        (get("legacyId") as? Number)?.toLong() ?: uid?.let(::legacyId) ?: id.toLongOrNull() ?: 0L,
        getString("nombre").orEmpty(),
        getString("correo").orEmpty(),
        getString("tipo") ?: "Ciudadano"
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
