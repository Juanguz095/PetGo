package com.example.practicafinal.session

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object FavoritosManager {

    private const val PREFS = "favoritos"
    private const val KEY_FAVS = "ids_favoritos"
    private const val SUBCOLECCION = "favoritos"

    fun esFavorito(context: Context, publicacionId: Long): Boolean =
        localIds(context).contains(publicacionId.toString())

    fun toggle(context: Context, publicacionId: Long): Boolean {
        val set = localIds(context).toMutableSet()
        val key = publicacionId.toString()
        val agregado = if (set.contains(key)) {
            set.remove(key)
            false
        } else {
            set.add(key)
            true
        }
        saveLocal(context, set)
        val uid = SesionManager.currentFirebaseUid(context)
        if (uid != null) {
            val ref = FirebaseFirestore.getInstance().collection("usuarios")
                .document(uid).collection(SUBCOLECCION).document(key)
            if (agregado) {
                ref.set(mapOf("publicacionId" to key, "fecha" to System.currentTimeMillis()))
            } else {
                ref.delete()
            }
        }
        return agregado
    }

    fun sincronizar(context: Context) {
        val uid = SesionManager.currentFirebaseUid(context) ?: return
        FirebaseFirestore.getInstance().collection("usuarios").document(uid)
            .collection(SUBCOLECCION).get()
            .addOnSuccessListener { snapshot ->
                saveLocal(context, snapshot.documents.mapNotNull { it.id }.toSet())
            }
    }

    fun contar(context: Context): Int = localIds(context).size

    fun obtenerIds(context: Context): Set<String> = localIds(context)

    private fun localIds(context: Context): Set<String> =
        prefs(context).getStringSet(KEY_FAVS, emptySet()).orEmpty()

    private fun saveLocal(context: Context, values: Set<String>) {
        prefs(context).edit().putStringSet(KEY_FAVS, values.toSet()).apply()
    }

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
