package com.example.practicafinal.session

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.auth.FirebaseAuth

object SesionManager {

    private const val PREFS = "sesion"
    private const val KEY_ID = "usuario_id"
    private const val KEY_UID = "firebase_uid"
    private const val KEY_ID_TOKEN = "firebase_id_token"

    fun guardarSesion(context: Context, id: Long) {
        prefs(context).edit().putLong(KEY_ID, id).apply()
    }

    fun guardarSesion(context: Context, id: Long, firebaseUid: String) {
        prefs(context).edit().putLong(KEY_ID, id).putString(KEY_UID, firebaseUid).apply()
    }

    fun guardarIdToken(context: Context, token: String) {
        prefs(context).edit().putString(KEY_ID_TOKEN, token).apply()
    }

    fun obtenerIdToken(context: Context): String? =
        prefs(context).getString(KEY_ID_TOKEN, null)

    fun guardarFirebaseUid(context: Context, uid: String) {
        prefs(context).edit().putString(KEY_UID, uid).apply()
    }

    fun obtenerUsuarioId(context: Context): Long? {
        val stored = prefs(context).getLong(KEY_ID, -1L)
        if (stored != -1L) return stored
        val uid = currentFirebaseUid(context) ?: return null
        val generated = legacyId(uid)
        prefs(context).edit().putLong(KEY_ID, generated).putString(KEY_UID, uid).apply()
        return generated
    }

    fun currentFirebaseUid(context: Context? = null): String? =
        context?.let { prefs(it).getString(KEY_UID, null) }

    fun tieneSesion(context: Context): Boolean =
        prefs(context).getString(KEY_UID, null) != null || prefs(context).getLong(KEY_ID, -1L) != -1L

    fun cerrarSesion(context: Context) {
        prefs(context).edit().clear().apply()
        FirebaseAuth.getInstance().signOut()
    }

    private fun legacyId(uid: String): Long =
        (uid.hashCode().toLong() and Long.MAX_VALUE).let { if (it == 0L) 1L else it }

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
