package com.example.practicafinal.controlador

import android.content.Context
import com.example.practicafinal.db.DatabaseHelper
import com.example.practicafinal.modelo.Albergue
import com.google.firebase.firestore.ListenerRegistration

object ControladorAlbergues {
    fun obtenerAlbergues(context: Context): List<Albergue> = DatabaseHelper(context).obtenerAlbergues()

    fun insertarAlbergue(
        context: Context, nombre: String, descripcion: String, direccion: String,
        telefono: String, foto: String?, latitud: Double, longitud: Double
    ): Long = DatabaseHelper(context).insertarAlbergue(
        nombre, descripcion, direccion, telefono, foto, latitud, longitud
    )

    fun observarAlbergues(
        context: Context, onChanged: (List<Albergue>) -> Unit, onError: (Exception) -> Unit
    ): ListenerRegistration = DatabaseHelper(context).observarAlbergues(onChanged, onError)
}
