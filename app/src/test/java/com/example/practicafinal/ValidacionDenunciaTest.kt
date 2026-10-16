package com.example.practicafinal

import com.example.practicafinal.controlador.ControladorDenuncias
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ValidacionDenunciaTest {
    @Test fun noPermiteUnMotivoEnBlanco() {
        assertNotNull(ControladorDenuncias.validarDenuncia("  ", "Un animal necesita ayuda"))
    }

    @Test fun noPermiteDescripcionEnBlanco() {
        assertNotNull(ControladorDenuncias.validarDenuncia("Maltrato", "\n  "))
    }

    @Test fun aceptaDescripcionYMotivoPersonalizado() {
        assertNull(ControladorDenuncias.validarDenuncia("Animal herido", "Necesita atención en el parque"))
    }
}
