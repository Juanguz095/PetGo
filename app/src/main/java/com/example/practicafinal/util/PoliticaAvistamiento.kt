package com.example.practicafinal.util

import com.example.practicafinal.modelo.Avistamiento
import com.example.practicafinal.modelo.Publicacion
import java.text.Normalizer
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object PoliticaAvistamiento {
    const val VENTANA_DIAS = 30
    const val VENTANA_MS = VENTANA_DIAS * 24L * 60L * 60L * 1000L
    const val DISTANCIA_MAXIMA_METROS = 50_000.0

    fun fechaValida(fecha: Long, ahora: Long = System.currentTimeMillis()): Boolean =
        fecha in (ahora - VENTANA_MS)..ahora
}

data class EvaluacionCoincidencia(
    val puntaje: Int,
    val nivel: String,
    val motivos: List<String>
)

object MotorCoincidenciasAvistamiento {
    fun evaluar(
        alerta: Publicacion,
        avistamiento: Avistamiento,
        ahora: Long = System.currentTimeMillis()
    ): EvaluacionCoincidencia? {
        if (alerta.tipo != "Perdida" || alerta.estado != "Activa" || avistamiento.publicacionId != 0L) return null
        if (!PoliticaAvistamiento.fechaValida(avistamiento.fecha, ahora)) return null

        val fechaPerdida = alerta.fechaPerdida ?: alerta.fechaCreacion
        if (fechaPerdida > 0L && avistamiento.fecha < fechaPerdida) return null

        val especieAlerta = alerta.especie.normalizada()
        val especieVista = avistamiento.especie.normalizada()
        if (especieAlerta.isNotEmpty() && especieVista.isNotEmpty() && especieAlerta != especieVista) return null

        val distancia = distanciaMetros(alerta.latitud, alerta.longitud, avistamiento.latitud, avistamiento.longitud)
        if (distancia > PoliticaAvistamiento.DISTANCIA_MAXIMA_METROS) return null

        val motivos = mutableListOf<String>()
        var puntos = 0
        if (especieAlerta.isNotEmpty() && especieVista.isNotEmpty()) {
            puntos += 22
            motivos += "misma especie"
        } else {
            puntos += 12
        }

        val distanciaTexto = when {
            distancia <= 2_000.0 -> { puntos += 25; "a menos de 2 km" }
            distancia <= 10_000.0 -> { puntos += 20; "a menos de 10 km" }
            distancia <= 25_000.0 -> { puntos += 15; "a menos de 25 km" }
            else -> { puntos += 10; "dentro de 50 km" }
        }
        motivos += distanciaTexto

        val edad = (ahora - avistamiento.fecha).coerceAtLeast(0L)
        puntos += when {
            edad <= 6L * 60 * 60 * 1000 -> 20
            edad <= 24L * 60 * 60 * 1000 -> 16
            edad <= 7L * 24 * 60 * 60 * 1000 -> 12
            else -> 7
        }
        motivos += when {
            edad <= 24L * 60 * 60 * 1000 -> "reporte reciente"
            edad <= 7L * 24 * 60 * 60 * 1000 -> "reportado esta semana"
            else -> "reportado en los últimos 30 días"
        }

        val rasgos = tokens(listOf(alerta.color, alerta.tamano, alerta.senasParticulares))
        val descripcion = tokens(listOf(avistamiento.descripcion))
        if (rasgos.isNotEmpty()) {
            val coincidencias = rasgos.intersect(descripcion)
            if (coincidencias.isNotEmpty()) {
                puntos += (25.0 * coincidencias.size / rasgos.size).toInt().coerceAtLeast(1)
                motivos += "coinciden rasgos: ${coincidencias.take(4).joinToString() }"
            }
        }

        val lugarAlerta = tokens(listOf(alerta.ultimoLugar))
        if (lugarAlerta.isNotEmpty() && lugarAlerta.intersect(descripcion).isNotEmpty()) {
            puntos += 8
            motivos += "la descripción menciona la última zona"
        }

        val score = puntos.coerceIn(0, 100)
        val nivel = when {
            score >= 75 -> "Alta"
            score >= 55 -> "Media"
            else -> "Baja"
        }
        return EvaluacionCoincidencia(score, nivel, motivos)
    }

    private fun distanciaMetros(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val radioTierra = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2) * sin(dLon / 2)
        return radioTierra * 2 * atan2(sqrt(a), sqrt(1 - a))
    }

    private fun tokens(values: List<String?>): Set<String> = values
        .filterNotNull()
        .joinToString(" ")
        .normalizar()
        .split(Regex("[^a-z0-9]+"))
        .filter { it.length >= 3 }
        .toSet()

    private fun String?.normalizada(): String = this.orEmpty().normalizar().trim()

    private fun String.normalizar(): String = Normalizer
        .normalize(lowercase(Locale.ROOT), Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
}

object VigenciaAlertasPerdidas {
    const val RECORDATORIO_MS = 14L * 24 * 60 * 60 * 1000
    const val CADUCIDAD_PUBLICA_MS = 30L * 24 * 60 * 60 * 1000

    fun ultimaActualizacion(alerta: Publicacion): Long =
        alerta.ultimaConfirmacion?.takeIf { it > 0L } ?: alerta.fechaCreacion

    fun visibleParaLaComunidad(alerta: Publicacion, ahora: Long = System.currentTimeMillis()): Boolean =
        alerta.tipo != "Perdida" || alerta.estado != "Activa" ||
            ahora - ultimaActualizacion(alerta) < CADUCIDAD_PUBLICA_MS

    fun necesitaConfirmacion(alerta: Publicacion, ahora: Long = System.currentTimeMillis()): Boolean =
        alerta.tipo == "Perdida" && alerta.estado == "Activa" &&
            ahora - ultimaActualizacion(alerta) >= RECORDATORIO_MS
}
