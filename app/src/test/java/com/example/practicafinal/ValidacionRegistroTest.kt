package com.example.practicafinal

import com.example.practicafinal.controlador.ControladorUsuarios
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ValidacionRegistroTest {
    @Test
    fun aceptaRegistroSinCiudad() {
        assertNull(
            ControladorUsuarios.validarRegistro(
                nombre = "Ana Pérez",
                correo = "ana@example.com",
                contrasena = "clave123",
                confirmar = "clave123"
            )
        )
    }

    @Test
    fun sigueValidandoQueLasContrasenasCoincidanSinCiudad() {
        assertEquals(
            "Las contraseñas no coinciden",
            ControladorUsuarios.validarRegistro(
                nombre = "Ana Pérez",
                correo = "ana@example.com",
                contrasena = "clave123",
                confirmar = "otraClave"
            )
        )
    }
}
