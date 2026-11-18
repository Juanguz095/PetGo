package com.example.practicafinal

import com.example.practicafinal.modelo.Avistamiento
import com.example.practicafinal.modelo.Publicacion
import com.example.practicafinal.util.MotorCoincidenciasAvistamiento
import com.example.practicafinal.util.PoliticaAvistamiento
import com.example.practicafinal.util.VigenciaAlertasPerdidas
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PoliticaAvistamientoTest {
    private val ahora = 1_800_000_000_000L
    private val alerta = Publicacion(
        id = 15,
        usuarioId = 1,
        tipo = "Perdida",
        nombre = "Luna",
        descripcion = "Perrita perdida",
        foto = null,
        ultimoLugar = "Parque central",
        especie = "Perro",
        latitud = -12.0,
        longitud = -77.0,
        fechaCreacion = ahora - 5L * 24 * 60 * 60 * 1000,
        estado = "Activa",
        fechaPerdida = ahora - 5L * 24 * 60 * 60 * 1000,
        color = "blanca",
        tamano = "pequeña",
        senasParticulares = "mancha negra en la pata"
    )

    @Test fun laFechaNoSeAceptaSiEsFuturaNiAnteriorATreintaDias() {
        assertFalse(PoliticaAvistamiento.fechaValida(ahora + 1, ahora))
        assertFalse(PoliticaAvistamiento.fechaValida(ahora - PoliticaAvistamiento.VENTANA_MS - 1, ahora))
        assertTrue(PoliticaAvistamiento.fechaValida(ahora - PoliticaAvistamiento.VENTANA_MS, ahora))
    }

    @Test fun priorizaReportesCercanosRecientesConRasgosCoincidentes() {
        val reporte = Avistamiento(
            id = 20,
            publicacionId = 0,
            usuarioId = 2,
            latitud = -12.001,
            longitud = -77.001,
            descripcion = "Perrita blanca pequeña con mancha negra en la pata, junto al parque central",
            foto = null,
            fecha = ahora - 2L * 60 * 60 * 1000,
            especie = "Perro"
        )

        val coincidencia = MotorCoincidenciasAvistamiento.evaluar(alerta, reporte, ahora)

        assertNotNull(coincidencia)
        assertTrue(coincidencia!!.puntaje >= 75)
        assertEquals("Alta", coincidencia.nivel)
        assertTrue(coincidencia.motivos.any { it.contains("rasgos") })
    }

    @Test fun rechazaEspecieDistanciaYFechasIncompatibles() {
        val base = Avistamiento(21, 0, 2, -12.001, -77.001, "Se veía blanca", null, ahora - 1_000, "Gato")
        val lejos = base.copy(especie = "Perro", latitud = -12.8)
        val antiguo = base.copy(especie = "Perro", fecha = ahora - PoliticaAvistamiento.VENTANA_MS - 1)

        assertNull(MotorCoincidenciasAvistamiento.evaluar(alerta, base, ahora))
        assertNull(MotorCoincidenciasAvistamiento.evaluar(alerta, lejos, ahora))
        assertNull(MotorCoincidenciasAvistamiento.evaluar(alerta, antiguo, ahora))
    }

    @Test fun lasAlertasPerdidasSeOcultanTrasTreintaDiasYSeReactivanAlConfirmar() {
        val vieja = alerta.copy(ultimaConfirmacion = ahora - VigenciaAlertasPerdidas.CADUCIDAD_PUBLICA_MS)
        val renovada = vieja.copy(ultimaConfirmacion = ahora)

        assertFalse(VigenciaAlertasPerdidas.visibleParaLaComunidad(vieja, ahora))
        assertTrue(VigenciaAlertasPerdidas.necesitaConfirmacion(vieja, ahora))
        assertTrue(VigenciaAlertasPerdidas.visibleParaLaComunidad(renovada, ahora))
    }
}
