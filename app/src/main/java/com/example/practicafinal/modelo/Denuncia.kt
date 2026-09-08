package com.example.practicafinal.modelo

data class Denuncia(
    val id: Long,
    val motivo: String,
    val descripcion: String,
    val foto: String?,
    val latitud: Double,
    val longitud: Double,
    val fecha: Long
) {
    fun toFirestoreMap(firebaseUid: String? = null): Map<String, Any> = buildMap {
        firebaseUid?.let { put("usuarioId", it) }
        put("motivo", motivo)
        put("descripcion", descripcion)
        foto?.let { put("foto", it) }
        put("latitud", latitud)
        put("longitud", longitud)
        put("fecha", fecha)
    }
}
