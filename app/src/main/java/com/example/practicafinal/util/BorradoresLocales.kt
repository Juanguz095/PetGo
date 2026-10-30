package com.example.practicafinal.util

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import org.json.JSONObject

/** Borradores privados en este dispositivo; nunca se sincronizan automáticamente. */
object BorradoresLocales {
    private const val PREFS = "borradores_locales"

    private fun clave(context: Context, formulario: String): String {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
            ?: context.getSharedPreferences("sesion", Context.MODE_PRIVATE).getString("usuario_id", "anonimo")
            ?: "anonimo"
        return "${uid}_$formulario"
    }

    fun obtener(context: Context, formulario: String): JSONObject? = runCatching {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(clave(context, formulario), null)
            ?.let(::JSONObject)
    }.getOrNull()

    fun guardar(context: Context, formulario: String, datos: JSONObject) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(clave(context, formulario), datos.toString()).apply()
    }

    fun eliminar(context: Context, formulario: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().remove(clave(context, formulario)).apply()
    }
}
