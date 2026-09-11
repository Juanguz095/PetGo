package com.example.practicafinal.modelo

data class Albergue(
    val id: Long,
    val nombre: String,
    val descripcion: String,
    val direccion: String,
    val telefono: String,
    val foto: String?,
    val latitud: Double,
    val longitud: Double
) {
    fun toFirestoreMap(): Map<String, Any> = buildMap {
        put("nombre", nombre)
        put("descripcion", descripcion)
        put("direccion", direccion)
        put("telefono", telefono)
        foto?.let { put("foto", it) }
        put("latitud", latitud)
        put("longitud", longitud)
    }
}
