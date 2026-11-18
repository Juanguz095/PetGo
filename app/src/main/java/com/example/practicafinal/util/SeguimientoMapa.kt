package com.example.practicafinal.util

internal class SeguimientoMapa(
    private val alRecibirPosicion: (Double, Double) -> Unit
) {
    var activo: Boolean = false
        private set

    fun iniciar() {
        activo = true
    }

    fun pausar() {
        activo = false
    }

    fun recibir(latitud: Double, longitud: Double) {
        if (activo) alRecibirPosicion(latitud, longitud)
    }
}
