package com.example.practicafinal.util

import com.example.practicafinal.modelo.Publicacion
import java.text.Normalizer
import java.util.Locale

fun normalizarBusqueda(texto: String): String = Normalizer.normalize(texto.trim(), Normalizer.Form.NFD)
    .replace("\\p{M}+".toRegex(), "")
    .lowercase(Locale.ROOT)

fun filtrarPublicaciones(
    publicaciones: List<Publicacion>,
    consulta: String,
    especie: String?
): List<Publicacion> {
    val palabras = normalizarBusqueda(consulta).split("\\s+".toRegex()).filter { it.isNotEmpty() }
    return publicaciones.filter { p ->
        val contenido = normalizarBusqueda(
            listOfNotNull(p.nombre, p.descripcion, p.especie, p.ultimoLugar).joinToString(" ")
        )
        (especie == null || p.especie.equals(especie, ignoreCase = true)) &&
            palabras.all { contenido.contains(it) }
    }
}
