package com.example.practicafinal

import com.example.practicafinal.controlador.ControladorDenuncias
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ValidacionDenunciaTest {
    @Test
    fun noPermiteUnMotivoEnBlanco() {
        assertNotNull(ControladorDenuncias.validarDenuncia("  ", "Un animal necesita ayuda"))
    }

    @Test
    fun noPermiteDescripcionEnBlanco() {
        assertNotNull(ControladorDenuncias.validarDenuncia("Maltrato", "\n  "))
    }

    @Test
    fun rechazaMotivoQueSuperaElLimiteDeFirestore() {
        assertEquals(
            "El motivo no puede superar 80 caracteres",
            ControladorDenuncias.validarDenuncia("x".repeat(81), "Descripción válida")
        )
    }

    @Test
    fun rechazaDescripcionQueSuperaElLimiteDeFirestore() {
        assertEquals(
            "La descripción no puede superar 1000 caracteres",
            ControladorDenuncias.validarDenuncia("Maltrato", "x".repeat(1001))
        )
    }

    @Test
    fun aceptaDescripcionYMotivoPersonalizado() {
        assertNull(ControladorDenuncias.validarDenuncia("Animal herido", "Necesita atención en el parque"))
    }
}
