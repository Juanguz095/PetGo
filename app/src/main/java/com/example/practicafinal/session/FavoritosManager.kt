package com.example.practicafinal.session

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.firestore.FirebaseFirestore
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

object FavoritosManager {

    private const val PREFS = "favoritos"
    private const val KEY_FAVS = "ids_favoritos"
    private const val SUBCOLECCION = "favoritos"
    private const val REVISION = "revision_local"
    private val operationVersions = ConcurrentHashMap<String, AtomicLong>()

    fun esFavorito(context: Context, publicacionId: Long): Boolean =
        localIds(context).contains(publicacionId.toString())

    fun toggle(
        context: Context,
        publicacionId: Long,
        onComplete: (Result<Boolean>) -> Unit = {}
    ): Boolean {
        val uid = SesionManager.currentFirebaseUid(context)
        if (uid == null) {
            onComplete(Result.failure(IllegalStateException("Inicia sesión para guardar favoritos")))
            return false
        }
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
        val operationKey = "$uid:$key"
        val operationVersion = operationVersions.getOrPut(operationKey) { AtomicLong() }.incrementAndGet()
        val ref = FirebaseFirestore.getInstance().collection("usuarios")
            .document(uid).collection(SUBCOLECCION).document(key)
        val task = if (agregado) {
            ref.set(mapOf("publicacionId" to key, "fecha" to System.currentTimeMillis()))
        } else {
            ref.delete()
        }
        task.addOnSuccessListener {
            onComplete(Result.success(agregado))
        }.addOnFailureListener { error ->
            if (operationVersions[operationKey]?.get() == operationVersion) {
                val actual = localIds(context).toMutableSet()
                if (agregado) actual.remove(key) else actual.add(key)
                saveLocal(context, actual)
            }
            onComplete(Result.failure(error))
        }
        return agregado
    }

    fun sincronizar(context: Context, onComplete: () -> Unit = {}) {
        val appContext = context.applicationContext
        val uid = SesionManager.currentFirebaseUid(appContext) ?: return onComplete()
        val revision = prefs(appContext).getLong(REVISION, 0L)
        FirebaseFirestore.getInstance().collection("usuarios").document(uid)
            .collection(SUBCOLECCION).get()
            .addOnSuccessListener { snapshot ->
                // Una respuesta antigua no debe sobrescribir una pulsación reciente ni otra cuenta.
                if (SesionManager.currentFirebaseUid(appContext) == uid &&
                    prefs(appContext).getLong(REVISION, 0L) == revision) {
                    saveLocal(appContext, snapshot.documents.map { it.id }.toSet())
                }
                onComplete()
            }
            .addOnFailureListener { onComplete() }
    }

    fun contar(context: Context): Int = localIds(context).size

    fun obtenerIds(context: Context): Set<String> = localIds(context)

    private fun localIds(context: Context): Set<String> =
        prefs(context).getStringSet(KEY_FAVS, emptySet()).orEmpty()

    private fun saveLocal(context: Context, values: Set<String>) {
        val preferences = prefs(context)
        preferences.edit().putStringSet(KEY_FAVS, values.toSet())
            .putLong(REVISION, preferences.getLong(REVISION, 0L) + 1L).apply()
    }

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences("${PREFS}_${SesionManager.currentFirebaseUid(context) ?: "sin_sesion"}", Context.MODE_PRIVATE)
}
