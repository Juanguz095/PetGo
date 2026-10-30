package com.example.practicafinal.session

import android.content.Context
import android.content.SharedPreferences
import com.example.practicafinal.modelo.Usuario
import com.google.firebase.auth.FirebaseAuth

data class RegistroPendiente(val firebaseUid: String, val usuario: Usuario)

object SesionManager {

    private const val PREFS = "sesion"
    private const val KEY_ID = "usuario_id"
    private const val KEY_UID = "firebase_uid"
    private const val KEY_PENDING_UID = "registro_pendiente_uid"
    private const val KEY_PENDING_ID = "registro_pendiente_id"
    private const val KEY_PENDING_NOMBRE = "registro_pendiente_nombre"
    private const val KEY_PENDING_CORREO = "registro_pendiente_correo"
    private const val KEY_PENDING_TIPO = "registro_pendiente_tipo"
    private const val KEY_PENDING_CIUDAD = "registro_pendiente_ciudad"

    fun guardarSesion(context: Context, id: Long) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        guardarSesion(context, id, uid)
    }

    fun guardarSesion(context: Context, id: Long, firebaseUid: String) {
        prefs(context).edit().putLong(KEY_ID, id).putString(KEY_UID, firebaseUid).apply()
    }

    fun guardarRegistroPendiente(context: Context, firebaseUid: String, usuario: Usuario) {
        prefs(context).edit()
            .putLong(KEY_ID, usuario.id)
            .putString(KEY_UID, firebaseUid)
            .putString(KEY_PENDING_UID, firebaseUid)
            .putLong(KEY_PENDING_ID, usuario.id)
            .putString(KEY_PENDING_NOMBRE, usuario.nombre)
            .putString(KEY_PENDING_CORREO, usuario.correo)
            .putString(KEY_PENDING_TIPO, usuario.tipo)
            .putString(KEY_PENDING_CIUDAD, usuario.ciudad)
            .apply()
    }

    fun obtenerRegistroPendiente(context: Context): RegistroPendiente? {
        val uid = currentFirebaseUid(context) ?: return null
        val preferences = prefs(context)
        if (preferences.getString(KEY_PENDING_UID, null) != uid) return null
        val id = preferences.getLong(KEY_PENDING_ID, -1L)
        val nombre = preferences.getString(KEY_PENDING_NOMBRE, null) ?: return null
        val correo = preferences.getString(KEY_PENDING_CORREO, null) ?: return null
        val tipo = preferences.getString(KEY_PENDING_TIPO, null) ?: return null
        val ciudad = preferences.getString(KEY_PENDING_CIUDAD, null) ?: return null
        if (id == -1L) return null
        return RegistroPendiente(uid, Usuario(id, nombre, correo, tipo, ciudad))
    }

    fun hayRegistroPendiente(context: Context): Boolean = obtenerRegistroPendiente(context) != null

    fun limpiarRegistroPendiente(context: Context) {
        prefs(context).edit()
            .remove(KEY_PENDING_UID)
            .remove(KEY_PENDING_ID)
            .remove(KEY_PENDING_NOMBRE)
            .remove(KEY_PENDING_CORREO)
            .remove(KEY_PENDING_TIPO)
            .remove(KEY_PENDING_CIUDAD)
            .apply()
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
