package com.example.practicafinal

import com.example.practicafinal.modelo.Publicacion
import com.example.practicafinal.util.filtrarPublicaciones
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BusquedaPublicacionesTest {
    private val mascotas = listOf(
        Publicacion(1, 10, "Perdida", "Lúna", "Perrita con collar rojo", null,
            "Parque Kennedy", "Perro", -12.0, -77.0, 100, "Activa"),
        Publicacion(2, 20, "Perdida", "Michi", "Gato gris", null,
            "Jesús María", "Gato", -12.0, -77.0, 200, "Activa"),
        Publicacion(3, 20, "Perdida", "Rocky", "Collar azul", null,
            null, null, -12.0, -77.0, 300, "Activa")
    )

    @Test fun buscarNombreIgnoraTildesMayusculasYEspacios() {
        assertEquals(listOf(1L), filtrarPublicaciones(mascotas, "  LUNA  ", null).map { it.id })
    }

    @Test fun buscarEspecieUsaLosDatosDeLaMascota() {
        assertEquals(listOf(2L), filtrarPublicaciones(mascotas, "gato", null).map { it.id })
    }

    @Test fun buscarUbicacionIgnoraTildes() {
        assertEquals(listOf(2L), filtrarPublicaciones(mascotas, "jesus maria", null).map { it.id })
    }

    @Test fun combinaBusquedaYFiltroDeEspecie() {
        assertTrue(filtrarPublicaciones(mascotas, "Luna", "Gato").isEmpty())
        assertEquals(listOf(1L), filtrarPublicaciones(mascotas, "collar rojo", "Perro").map { it.id })
    }

    @Test fun todasIncluyeMascotasSinEspecie() {
        assertEquals(mascotas, filtrarPublicaciones(mascotas, "", null))
        assertEquals(listOf(1L), filtrarPublicaciones(mascotas, "", "Perro").map { it.id })
    }

    @Test fun consultaSinResultadosNoDevuelveLaListaOriginal() {
        assertTrue(filtrarPublicaciones(mascotas, "mascota inexistente", null).isEmpty())
    }
}
