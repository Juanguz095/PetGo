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
    val estado: String,
    val contactoWhatsapp: String? = null,
    val fechaPerdida: Long? = null,
    val recompensa: String? = null,
    val ultimaConfirmacion: Long? = null,
    val color: String? = null,
    val tamano: String? = null,
    val senasParticulares: String? = null,
    val edadAproximada: String? = null,
    val caracter: String? = null,
    val cuidados: String? = null,
    val usuarioNombre: String? = null
) {
    fun toFirestoreMap(firebaseUid: String? = null): Map<String, Any> = buildMap {
        firebaseUid?.let { put("usuarioId", it) }
        put("usuarioLegacyId", usuarioId ?: 0L)
        usuarioNombre?.trim()?.takeIf { it.isNotEmpty() }?.let { put("usuarioNombre", it) }
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
        contactoWhatsapp?.takeIf { it.isNotBlank() }?.let { put("contactoWhatsapp", it) }
        fechaPerdida?.let { put("fechaPerdida", it) }
        recompensa?.takeIf { it.isNotBlank() }?.let { put("recompensa", it) }
        ultimaConfirmacion?.let { put("ultimaConfirmacion", it) }
        color?.trim()?.takeIf { it.isNotEmpty() }?.let { put("color", it) }
        tamano?.trim()?.takeIf { it.isNotEmpty() }?.let { put("tamano", it) }
        senasParticulares?.trim()?.takeIf { it.isNotEmpty() }?.let { put("senasParticulares", it) }
        edadAproximada?.trim()?.takeIf { it.isNotEmpty() }?.let { put("edadAproximada", it) }
        caracter?.trim()?.takeIf { it.isNotEmpty() }?.let { put("caracter", it) }
        cuidados?.trim()?.takeIf { it.isNotEmpty() }?.let { put("cuidados", it) }
    }
}
