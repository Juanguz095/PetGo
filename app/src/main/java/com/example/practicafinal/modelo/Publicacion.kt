package com.example.practicafinal.modelo

data class Publicacion(
    val id: Long,
    val usuarioId: Long?,
    val tipo: String,
    val nombre: String,
    val descripcion: String,
    val foto: String?,
    val ultimoLugar: String?,
    val especie: String?,
    val latitud: Double,
    val longitud: Double,
    val fechaCreacion: Long,
    val estado: String
) {
    fun toFirestoreMap(firebaseUid: String? = null): Map<String, Any> = buildMap {
        firebaseUid?.let { put("usuarioId", it) }
        put("usuarioLegacyId", usuarioId ?: 0L)
        put("tipo", tipo)
        put("nombre", nombre)
        put("descripcion", descripcion)
        foto?.let { put("foto", it) }
        ultimoLugar?.let { put("ultimoLugar", it) }
        especie?.let { put("especie", it) }
        put("latitud", latitud)
        put("longitud", longitud)
        put("fechaCreacion", fechaCreacion)
        put("estado", estado)
    }
}
