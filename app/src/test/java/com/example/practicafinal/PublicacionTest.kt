package com.example.practicafinal

import com.example.practicafinal.modelo.Publicacion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class PublicacionTest {
    @Test
    fun publicaElNombreDelAutorSinExponerDatosPrivados() {
        val data = Publicacion(
            id = 1,
            usuarioId = 7,
            tipo = "Adopcion",
            nombre = "Michi",
            descripcion = "Busca hogar",
            foto = null,
            ultimoLugar = null,
            especie = "Gato",
            latitud = -12.0,
            longitud = -77.0,
            fechaCreacion = 100,
            estado = "Activa",
            usuarioNombre = "Ana"
        ).toFirestoreMap("uid-ana")

        assertEquals("Ana", data["usuarioNombre"])
        assertFalse(data.containsKey("correo"))
    }

    @Test
    fun omiteNombreDelAutorQueSuperaElLimiteDeFirestore() {
        val data = Publicacion(
            id = 1,
            usuarioId = 7,
            tipo = "Adopcion",
            nombre = "Michi",
            descripcion = "Busca hogar",
            foto = null,
            ultimoLugar = null,
            especie = "Gato",
            latitud = -12.0,
            longitud = -77.0,
            fechaCreacion = 100,
            estado = "Activa",
            usuarioNombre = "A".repeat(81)
        ).toFirestoreMap("uid-ana")

        assertFalse(data.containsKey("usuarioNombre"))
    }

    @Test
    fun omiteNombreDelAutorEnBlanco() {
        val data = Publicacion(
            id = 1,
            usuarioId = 7,
            tipo = "Adopcion",
            nombre = "Michi",
            descripcion = "Busca hogar",
            foto = null,
            ultimoLugar = null,
            especie = "Gato",
            latitud = -12.0,
            longitud = -77.0,
            fechaCreacion = 100,
            estado = "Activa",
            usuarioNombre = "  "
        ).toFirestoreMap("uid-ana")

        assertFalse(data.containsKey("usuarioNombre"))
    }
}
