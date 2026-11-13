package com.example.practicafinal.modelo

/** Preferencia de avisos automáticos de mascotas perdidas cercanas. */
data class PreferenciasAlerta(
    val habilitadas: Boolean = false,
    val radioKm: Int = 10
)
