package com.example.practicafinal.controlador

import android.content.Context
import com.example.practicafinal.db.DatabaseHelper
import com.example.practicafinal.modelo.Avistamiento
import com.example.practicafinal.modelo.Denuncia
import com.example.practicafinal.modelo.Publicacion
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.DocumentSnapshot

object ControladorPublicaciones {

    fun validarPublicacion(nombre: String, descripcion: String): String? {
        if (nombre.isBlank() || descripcion.isBlank()) return "Completa el nombre y la descripción"
        if (nombre.trim().length > 80) return "El nombre no puede superar 80 caracteres"
        if (descripcion.trim().length > 1000) return "La descripción no puede superar 1000 caracteres"
        return null
    }

    fun publicar(
        context: Context, usuarioId: Long?, tipo: String, nombre: String, descripcion: String,
        foto: String?, ultimoLugar: String?, especie: String?, latitud: Double, longitud: Double,
        contactoWhatsapp: String? = null, fechaPerdida: Long? = null, recompensa: String? = null,
        color: String? = null, tamano: String? = null, senasParticulares: String? = null,
        edadAproximada: String? = null, caracter: String? = null, cuidados: String? = null
    ): Long = DatabaseHelper(context).insertarPublicacion(
        usuarioId, tipo, nombre, descripcion, foto, ultimoLugar, especie, latitud, longitud,
        contactoWhatsapp, fechaPerdida, recompensa, color, tamano, senasParticulares,
        edadAproximada, caracter, cuidados
    )

    fun actualizar(
        context: Context, id: Long, tipo: String, nombre: String, descripcion: String, foto: String?,
        ultimoLugar: String?, especie: String?, latitud: Double, longitud: Double, contactoWhatsapp: String?,
        fechaPerdida: Long? = null, recompensa: String? = null,
        color: String? = null, tamano: String? = null, senasParticulares: String? = null,
        edadAproximada: String? = null, caracter: String? = null, cuidados: String? = null
    ) = DatabaseHelper(context).actualizarPublicacion(
        id, tipo, nombre, descripcion, foto, ultimoLugar, especie, latitud, longitud,
        contactoWhatsapp, fechaPerdida, recompensa, color, tamano, senasParticulares,
        edadAproximada, caracter, cuidados
    )

    fun eliminar(context: Context, id: Long) = DatabaseHelper(context).eliminarPublicacion(id)

    fun resolver(context: Context, id: Long) = DatabaseHelper(context).marcarResuelta(id)

    fun adoptar(context: Context, id: Long) = DatabaseHelper(context).marcarAdoptada(id)

    fun enviarSolicitudAdopcion(context: Context, id: Long, mensaje: String, contactoWhatsapp: String): String =
        DatabaseHelper(context).enviarSolicitudAdopcion(id, mensaje, contactoWhatsapp)

    fun obtenerMiSolicitudAdopcion(context: Context, id: Long): Map<String, Any>? =
        DatabaseHelper(context).obtenerMiSolicitudAdopcion(id)

    fun obtenerSolicitudesAdopcion(context: Context, id: Long): List<Map<String, Any>> =
        DatabaseHelper(context).obtenerSolicitudesAdopcion(id)

    fun actualizarEstadoSolicitudAdopcion(context: Context, solicitudId: String, estado: String) =
        DatabaseHelper(context).actualizarEstadoSolicitudAdopcion(solicitudId, estado)

    fun reportarAvistamiento(
        context: Context, publicacionId: Long?, usuarioId: Long?, latitud: Double, longitud: Double,
        descripcion: String, foto: String?, fechaAvistamiento: Long = System.currentTimeMillis(),
        especie: String? = null
    ): Long = DatabaseHelper(context).insertarAvistamiento(
        publicacionId, usuarioId, latitud, longitud, descripcion, foto, fechaAvistamiento, especie
    )

    fun obtenerPublicaciones(context: Context): List<Publicacion> =
        DatabaseHelper(context).obtenerPublicaciones()

    fun obtenerPagina(context: Context, tipo: String?, cursor: DocumentSnapshot?) =
        DatabaseHelper(context).obtenerPaginaPublicaciones(tipo, cursor)

    fun obtenerPaginaPropias(context: Context, cursor: DocumentSnapshot?) =
        DatabaseHelper(context).obtenerPaginaPropias(cursor)

    fun obtenerPropias(context: Context): List<Publicacion> = DatabaseHelper(context).obtenerPropias()

    fun contarPropias(context: Context): Long = DatabaseHelper(context).contarPropias()

    fun obtenerPorId(context: Context, id: Long): Publicacion? =
        DatabaseHelper(context).obtenerPorIdPublicacion(id)

    fun obtenerCoordenadasPublicacion(context: Context, id: Long): Pair<Double, Double>? =
        DatabaseHelper(context).obtenerCoordenadasPublicacion(id)

    fun obtenerPerdidas(context: Context): List<Publicacion> =
        DatabaseHelper(context).obtenerPerdidas()

    fun obtenerEncontradas(context: Context): List<Publicacion> =
        DatabaseHelper(context).obtenerEncontradas()

    fun obtenerAdopciones(context: Context): List<Publicacion> =
        DatabaseHelper(context).obtenerAdopciones()

    fun obtenerAvistamientos(context: Context): List<Avistamiento> =
        DatabaseHelper(context).obtenerAvistamientos()

    fun obtenerAvistamientosParaAlertas(context: Context, publicacionIds: List<Long>): List<Avistamiento> =
        DatabaseHelper(context).obtenerAvistamientosParaAlertas(publicacionIds)

    fun obtenerAvistamientosPorPublicacion(context: Context, id: Long): List<Avistamiento> =
        DatabaseHelper(context).obtenerAvistamientosPorPublicacion(id)

    fun actualizarAvistamiento(context: Context, id: Long, latitud: Double, longitud: Double) =
        DatabaseHelper(context).actualizarAvistamiento(id, latitud, longitud)

    fun obtenerCoordenadasAvistamiento(context: Context, id: Long): Pair<Double, Double>? =
        DatabaseHelper(context).obtenerCoordenadasAvistamiento(id)

    fun confirmarAlertaPerdida(context: Context, id: Long) =
        DatabaseHelper(context).confirmarAlertaPerdida(id)

    fun actualizarRevisionAvistamiento(context: Context, id: Long, estado: String) =
        DatabaseHelper(context).actualizarRevisionAvistamiento(id, estado)

    fun vincularAvistamiento(context: Context, id: Long, publicacionId: Long) =
        DatabaseHelper(context).vincularAvistamiento(id, publicacionId)

    fun observarPublicaciones(
        context: Context, onChanged: (List<Publicacion>) -> Unit, onError: (Exception) -> Unit
    ): ListenerRegistration = DatabaseHelper(context).observarPublicaciones(onChanged, onError)

    fun observarAvistamientos(
        context: Context, onChanged: (List<Avistamiento>) -> Unit, onError: (Exception) -> Unit
    ): ListenerRegistration = DatabaseHelper(context).observarAvistamientos(onChanged, onError)

    fun observarDenuncias(
        context: Context, onChanged: (List<Denuncia>) -> Unit, onError: (Exception) -> Unit
    ): ListenerRegistration = DatabaseHelper(context).observarDenuncias(onChanged, onError)
}
