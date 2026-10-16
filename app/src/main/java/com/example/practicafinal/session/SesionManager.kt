package com.example.practicafinal.session

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.auth.FirebaseAuth

object SesionManager {

    private const val PREFS = "sesion"
    private const val KEY_ID = "usuario_id"
    private const val KEY_UID = "firebase_uid"

    fun guardarSesion(context: Context, id: Long) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        guardarSesion(context, id, uid)
    }

    fun guardarSesion(context: Context, id: Long, firebaseUid: String) {
        prefs(context).edit().putLong(KEY_ID, id).putString(KEY_UID, firebaseUid).apply()
    }

    fun obtenerUsuarioId(context: Context): Long? {
        val uid = currentFirebaseUid(context) ?: return null
        val storedUid = prefs(context).getString(KEY_UID, null)
        val storedId = prefs(context).getLong(KEY_ID, -1L)
        if (storedUid == uid && storedId != -1L) return storedId
        val generated = legacyId(uid)
        guardarSesion(context, generated, uid)
        return generated
    }

    fun currentFirebaseUid(context: Context? = null): String? =
        FirebaseAuth.getInstance().currentUser?.uid

    fun tieneSesion(context: Context): Boolean = currentFirebaseUid(context) != null

    fun cerrarSesion(context: Context) {
        prefs(context).edit().clear().apply()
        FirebaseAuth.getInstance().signOut()
    }

    private fun legacyId(uid: String): Long =
        (uid.hashCode().toLong() and Long.MAX_VALUE).let { if (it == 0L) 1L else it }

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
