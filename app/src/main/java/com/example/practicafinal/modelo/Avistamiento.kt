package com.example.practicafinal.modelo

data class Avistamiento(
    val id: Long,
    val publicacionId: Long,
    val usuarioId: Long?,
    val latitud: Double,
    val longitud: Double,
    val descripcion: String,
    val foto: String?,
    val fecha: Long
) {
    fun toFirestoreMap(firebaseUid: String? = null): Map<String, Any> = buildMap {
        put("publicacionId", publicacionId.toString())
        firebaseUid?.let { put("usuarioId", it) }
        put("usuarioLegacyId", usuarioId ?: 0L)
        put("latitud", latitud)
        put("longitud", longitud)
        put("descripcion", descripcion)
        foto?.let { put("foto", it) }
        put("fecha", fecha)
    }
}
