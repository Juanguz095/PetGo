package com.example.practicafinal.modelo

data class Usuario(
    val id: Long,
    val nombre: String,
    val correo: String,
    val tipo: String
) {
    fun toFirestoreMap(firebaseUid: String? = null): Map<String, Any> = buildMap {
        put("nombre", nombre)
        put("correo", correo)
        put("tipo", tipo)
        put("fechaRegistro", System.currentTimeMillis())
        put("legacyId", id)
        firebaseUid?.let { put("uid", it) }
    }
}
