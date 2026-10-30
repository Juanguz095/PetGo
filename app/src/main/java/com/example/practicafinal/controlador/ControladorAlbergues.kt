package com.example.practicafinal.controlador

import android.content.Context
import com.example.practicafinal.db.DatabaseHelper
import com.example.practicafinal.modelo.Albergue
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.DocumentSnapshot

object ControladorAlbergues {
    fun obtenerAlbergues(context: Context): List<Albergue> = DatabaseHelper(context).obtenerAlbergues()

    fun obtenerPagina(context: Context, cursor: DocumentSnapshot?) =
        DatabaseHelper(context).obtenerPaginaAlbergues(cursor)

    fun observarAlbergues(
        context: Context, onChanged: (List<Albergue>) -> Unit, onError: (Exception) -> Unit
    ): ListenerRegistration = DatabaseHelper(context).observarAlbergues(onChanged, onError)
}
