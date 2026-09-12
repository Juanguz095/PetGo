package com.example.practicafinal.db

import android.content.Context
import com.example.practicafinal.modelo.Albergue
import com.example.practicafinal.modelo.Avistamiento
import com.example.practicafinal.modelo.Denuncia
import com.example.practicafinal.modelo.Publicacion
import com.example.practicafinal.modelo.Usuario
import com.google.firebase.firestore.ListenerRegistration

/**
 * Fachada de compatibilidad. Conserva la API que usan los Activities,
 * pero todos los datos viven en Firebase (Firestore y Storage).
 */
class DatabaseHelper(context: Context) {

    private val repository = FirebaseRepository(context.applicationContext)

    fun registrar(nombre: String, correo: String, contrasena: String, tipo: String = "Ciudadano"): Long? =
        repository.registrar(nombre, correo, contrasena, tipo)

    fun validarLogin(correo: String, contrasena: String): Usuario? =
        repository.validarLogin(correo, contrasena)

    fun obtenerPorId(id: Long): Usuario? =
        repository.obtenerPorId(id)

    fun insertarPublicacion(
        usuarioId: Long?, tipo: String, nombre: String, descripcion: String,
        foto: String?, ultimoLugar: String?, especie: String?,
        latitud: Double, longitud: Double
    ): Long = repository.insertarPublicacion(
        usuarioId, tipo, nombre, descripcion, foto, ultimoLugar, especie, latitud, longitud
    )

    fun obtenerPublicaciones(): List<Publicacion> = repository.obtenerPublicaciones()
    fun obtenerPerdidas(): List<Publicacion> = repository.obtenerPerdidas()
    fun obtenerAdopciones(): List<Publicacion> = repository.obtenerAdopciones()
    fun obtenerPorIdPublicacion(id: Long): Publicacion? = repository.obtenerPorIdPublicacion(id)

    fun insertarAvistamiento(
        publicacionId: Long, usuarioId: Long?, latitud: Double, longitud: Double,
        descripcion: String, foto: String?
    ): Long = repository.insertarAvistamiento(
        publicacionId, usuarioId, latitud, longitud, descripcion, foto
    )

    fun obtenerAvistamientos(): List<Avistamiento> = repository.obtenerAvistamientos()
    fun obtenerAvistamientosPorPublicacion(publicacionId: Long): List<Avistamiento> =
        repository.obtenerAvistamientosPorPublicacion(publicacionId)

    fun marcarResuelta(id: Long) = repository.marcarResuelta(id)
    fun marcarAdoptada(id: Long) = repository.marcarAdoptada(id)
    fun actualizarAvistamiento(id: Long, latitud: Double, longitud: Double) =
        repository.actualizarAvistamiento(id, latitud, longitud)

    fun insertarAlbergue(
        nombre: String, descripcion: String, direccion: String,
        telefono: String, foto: String?, latitud: Double, longitud: Double
    ): Long = repository.insertarAlbergue(
        nombre, descripcion, direccion, telefono, foto, latitud, longitud
    )

    fun obtenerAlbergues(): List<Albergue> = repository.obtenerAlbergues()

    fun insertarDenuncia(
        motivo: String, descripcion: String, foto: String?,
        latitud: Double, longitud: Double
    ): Long = repository.insertarDenuncia(motivo, descripcion, foto, latitud, longitud)

    fun obtenerDenuncias(): List<Denuncia> = repository.obtenerDenuncias()

    fun seedIfNeeded() = repository.seedIfNeeded()

    fun observarPublicaciones(
        onChanged: (List<Publicacion>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration = repository.observarPublicaciones(onChanged, onError)

    fun observarAvistamientos(
        onChanged: (List<Avistamiento>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration = repository.observarAvistamientos(onChanged, onError)

    fun observarAlbergues(
        onChanged: (List<Albergue>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration = repository.observarAlbergues(onChanged, onError)

    fun observarDenuncias(
        onChanged: (List<Denuncia>) -> Unit,
        onError: (Exception) -> Unit
    ): ListenerRegistration = repository.observarDenuncias(onChanged, onError)

    fun obtenerNotificaciones(): List<Map<String, Any>> = repository.obtenerNotificaciones()
    fun contarNoLeidas(): Int = repository.contarNoLeidas()
    fun marcarNotificacionLeida(notifId: String) = repository.marcarNotificacionLeida(notifId)
    fun marcarTodasLeidas() = repository.marcarTodasLeidas()
}
