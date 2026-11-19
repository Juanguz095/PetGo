package com.example.practicafinal.controlador

import android.content.Context
import com.example.practicafinal.db.DatabaseHelper
import com.example.practicafinal.modelo.Denuncia

object ControladorDenuncias {
    fun validarDenuncia(motivo: String, descripcion: String): String? = when {
        motivo.isBlank() -> "Selecciona o escribe un motivo"
        motivo.trim().length > 80 -> "El motivo no puede superar 80 caracteres"
        descripcion.isBlank() -> "Escribe una descripción"
        descripcion.trim().length > 1000 -> "La descripción no puede superar 1000 caracteres"
        else -> null
    }

    fun insertarDenuncia(
        context: Context, motivo: String, descripcion: String, foto: String?,
        latitud: Double, longitud: Double
    ): Long {
        validarDenuncia(motivo, descripcion)?.let { throw IllegalArgumentException(it) }
        return DatabaseHelper(context).insertarDenuncia(motivo, descripcion, foto, latitud, longitud)
    }

    fun obtenerDenuncias(context: Context): List<Denuncia> =
        DatabaseHelper(context).obtenerDenuncias()

    fun esModerador(context: Context): Boolean = DatabaseHelper(context).esModeradorDenuncias()

    fun obtenerDenunciasModeracion(context: Context): List<Denuncia> =
        DatabaseHelper(context).obtenerDenunciasModeracion()

    fun actualizarEstadoModeracion(context: Context, id: Long, estado: String) =
        DatabaseHelper(context).actualizarEstadoDenunciaModeracion(id, estado)

    fun contarDenuncias(context: Context): Long = DatabaseHelper(context).contarDenuncias()
}
