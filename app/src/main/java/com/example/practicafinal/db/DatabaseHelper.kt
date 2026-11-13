package com.example.practicafinal.db

import android.content.Context
import com.example.practicafinal.modelo.Albergue
import com.example.practicafinal.modelo.Avistamiento
import com.example.practicafinal.modelo.Denuncia
import com.example.practicafinal.modelo.PreferenciasAlerta
import com.example.practicafinal.modelo.Publicacion
import com.example.practicafinal.modelo.ResultadoRegistro
import com.example.practicafinal.modelo.Usuario
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.DocumentSnapshot

/**
 * Fachada de compatibilidad. Conserva la API que usan los Activities,
 * pero todos los datos viven en Firebase (Firestore y Storage).
 */
class DatabaseHelper(context: Context) {

    private val repository = FirebaseRepository(context.applicationContext)

    fun registrar(nombre: String, correo: String, contrasena: String, tipo: String = "Ciudadano", ciudad: String = ""): ResultadoRegistro =
        repository.registrar(nombre, correo, contrasena, tipo, ciudad)

    fun reintentarGuardarPerfilPendiente(): Usuario? = repository.reintentarGuardarPerfilPendiente()

    fun validarLogin(correo: String, contrasena: String): Usuario? =
        repository.validarLogin(correo, contrasena)

    fun obtenerPorId(id: Long): Usuario? =
        repository.obtenerPorId(id)

    fun pushHabilitado(): Boolean = repository.pushHabilitado()

    fun actualizarPushHabilitado(habilitadas: Boolean) =
        repository.actualizarPushHabilitado(habilitadas)

    fun guardarPreferenciaAlerta(pref: PreferenciasAlerta, latitud: Double?, longitud: Double?) =
        repository.guardarPreferenciaAlerta(pref, latitud, longitud)

    fun obtenerPreferenciaAlerta(): PreferenciasAlerta = repository.obtenerPreferenciaAlerta()

    fun quitarDispositivoPushActual() = repository.quitarDispositivoPushActual()

    fun registrarTokenPushSiHabilitado(token: String) =
        repository.registrarTokenPushSiHabilitado(token)

    fun insertarPublicacion(
        usuarioId: Long?, tipo: String, nombre: String, descripcion: String,
        foto: String?, ultimoLugar: String?, especie: String?,
        latitud: Double, longitud: Double, contactoWhatsapp: String? = null,
        fechaPerdida: Long? = null, recompensa: String? = null,
        color: String? = null, tamano: String? = null, senasParticulares: String? = null,
        edadAproximada: String? = null, caracter: String? = null, cuidados: String? = null
    ): Long = repository.insertarPublicacion(
        usuarioId, tipo, nombre, descripcion, foto, ultimoLugar, especie, latitud, longitud,
        contactoWhatsapp, fechaPerdida, recompensa, color, tamano, senasParticulares,
        edadAproximada, caracter, cuidados
    )

    fun actualizarPublicacion(
        id: Long, tipo: String, nombre: String, descripcion: String, foto: String?,
        ultimoLugar: String?, especie: String?, latitud: Double, longitud: Double,
        contactoWhatsapp: String?, fechaPerdida: Long? = null, recompensa: String? = null,
        color: String? = null, tamano: String? = null, senasParticulares: String? = null,
        edadAproximada: String? = null, caracter: String? = null, cuidados: String? = null
    ) = repository.actualizarPublicacion(
        id, tipo, nombre, descripcion, foto, ultimoLugar, especie, latitud, longitud,
        contactoWhatsapp, fechaPerdida, recompensa, color, tamano, senasParticulares,
        edadAproximada, caracter, cuidados
    )

    fun eliminarPublicacion(id: Long) = repository.eliminarPublicacion(id)

    fun obtenerPublicaciones(): List<Publicacion> = repository.obtenerPublicaciones()
    fun obtenerPaginaPublicaciones(tipo: String?, after: DocumentSnapshot?, pageSize: Int = 24) =
        repository.obtenerPaginaPublicaciones(tipo, after, pageSize)
    fun obtenerPaginaPropias(after: DocumentSnapshot?, pageSize: Int = 24) =
        repository.obtenerPaginaPropias(after, pageSize)
    fun obtenerPaginaAlbergues(after: DocumentSnapshot?, pageSize: Int = 24) =
        repository.obtenerPaginaAlbergues(after, pageSize)
    fun obtenerPropias(): List<Publicacion> = repository.obtenerPropias()
    fun contarPropias(): Long = repository.contarPropias()
    fun obtenerPerdidas(): List<Publicacion> = repository.obtenerPerdidas()
    fun obtenerEncontradas(): List<Publicacion> = repository.obtenerEncontradas()
    fun obtenerAdopciones(): List<Publicacion> = repository.obtenerAdopciones()
    fun obtenerPorIdPublicacion(id: Long): Publicacion? = repository.obtenerPorIdPublicacion(id)
    fun obtenerCoordenadasPublicacion(id: Long): Pair<Double, Double>? =
        repository.obtenerCoordenadasPublicacion(id)

    fun insertarAvistamiento(
        publicacionId: Long?, usuarioId: Long?, latitud: Double, longitud: Double,
        descripcion: String, foto: String?, fechaAvistamiento: Long = System.currentTimeMillis(),
        especie: String? = null
    ): Long = repository.insertarAvistamiento(
        publicacionId, usuarioId, latitud, longitud, descripcion, foto, fechaAvistamiento, especie
    )

    fun obtenerAvistamientos(): List<Avistamiento> = repository.obtenerAvistamientos()
    fun obtenerAvistamientosPorPublicacion(publicacionId: Long): List<Avistamiento> =
        repository.obtenerAvistamientosPorPublicacion(publicacionId)

    fun marcarResuelta(id: Long) = repository.marcarResuelta(id)
    fun marcarAdoptada(id: Long) = repository.marcarAdoptada(id)
    fun actualizarAvistamiento(id: Long, latitud: Double, longitud: Double) =
        repository.actualizarAvistamiento(id, latitud, longitud)
    fun obtenerCoordenadasAvistamiento(id: Long): Pair<Double, Double>? =
        repository.obtenerCoordenadasAvistamiento(id)
    fun confirmarAlertaPerdida(id: Long) = repository.confirmarAlertaPerdida(id)
    fun actualizarRevisionAvistamiento(id: Long, estado: String) =
        repository.actualizarRevisionAvistamiento(id, estado)
    fun vincularAvistamiento(id: Long, publicacionId: Long) =
        repository.vincularAvistamiento(id, publicacionId)

    fun obtenerAlbergues(): List<Albergue> = repository.obtenerAlbergues()

    fun insertarDenuncia(
        motivo: String, descripcion: String, foto: String?,
        latitud: Double, longitud: Double
    ): Long = repository.insertarDenuncia(motivo, descripcion, foto, latitud, longitud)

    fun obtenerDenuncias(): List<Denuncia> = repository.obtenerDenuncias()
    fun contarDenuncias(): Long = repository.contarDenuncias()

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
