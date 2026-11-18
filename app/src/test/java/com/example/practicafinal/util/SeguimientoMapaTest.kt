package com.example.practicafinal.util

import org.junit.Assert.assertEquals
import org.junit.Test

class SeguimientoMapaTest {
    @Test
    fun propagaCadaPosicionMientrasActivoYDejaDeSeguirAlPausar() {
        val posicionesRecibidas = mutableListOf<Pair<Double, Double>>()
        val seguimiento = SeguimientoMapa { latitud: Double, longitud: Double ->
            posicionesRecibidas.add(latitud to longitud)
        }

        seguimiento.iniciar()
        seguimiento.recibir(-12.04, -77.03)
        seguimiento.recibir(-12.05, -77.04)
        seguimiento.pausar()
        seguimiento.recibir(-12.06, -77.05)

        assertEquals(listOf(-12.04 to -77.03, -12.05 to -77.04), posicionesRecibidas)
    }
}
