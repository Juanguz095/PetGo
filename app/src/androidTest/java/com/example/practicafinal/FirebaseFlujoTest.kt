package com.example.practicafinal

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.practicafinal.db.FirebaseRepository
import com.example.practicafinal.session.FavoritosManager
import com.example.practicafinal.session.SesionManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import org.junit.Assert.*
import org.junit.Before
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class FirebaseFlujoTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val repository = FirebaseRepository(context)

    @Before fun usarSoloEmuladoresLocales() {
        org.junit.Assume.assumeTrue(
            InstrumentationRegistry.getArguments().getString("firebaseEmulators") == "true"
        )
        configurarUnaVez()
        FirebaseAuth.getInstance().signOut()
    }

    @After fun cerrarSesionDePrueba() {
        if (InstrumentationRegistry.getArguments().getString("firebaseEmulators") == "true") {
            SesionManager.cerrarSesion(context)
        }
    }

    @Test fun sesionYFavoritosSeAislanEntreDosCuentas() {
        val emailA = "a-${UUID.randomUUID()}@example.com"
        val emailB = "b-${UUID.randomUUID()}@example.com"
        val idA = repository.registrar("Ana", emailA, "prueba123", ciudad = "Lima")
        assertTrue(SesionManager.tieneSesion(context))
        assertEquals(idA, repository.obtenerPorId(idA)?.id)
        assertTrue(FavoritosManager.toggle(context, 123L))
        sincronizarFavoritos()
        assertTrue(FavoritosManager.esFavorito(context, 123L))

        SesionManager.cerrarSesion(context)
        assertFalse(SesionManager.tieneSesion(context))
        repository.registrar("Bruno", emailB, "prueba123", ciudad = "Lima")
        assertTrue(FavoritosManager.obtenerIds(context).isEmpty())

        repository.validarLogin(emailA, "prueba123")
        sincronizarFavoritos()
        assertTrue(FavoritosManager.esFavorito(context, 123L))
    }

    @Test fun avistamientoNotificaAlDuenoYActualizaSuPropioRegistro() {
        val emailA = "owner-${UUID.randomUUID()}@example.com"
        val emailB = "observer-${UUID.randomUUID()}@example.com"
        val owner = repository.registrar("Dueño", emailA, "prueba123", ciudad = "Lima")
        val publication = repository.insertarPublicacion(owner, "Perdida", "Luna", "Collar rojo",
            null, "Parque", "Perro", -12.0, -77.0)
        repository.registrar("Observador", emailB, "prueba123", ciudad = "Lima")
        assertTrue(repository.obtenerPropias().isEmpty())
        try {
            repository.marcarResuelta(publication)
            fail("Otra cuenta no debe resolver la publicación")
        } catch (_: IllegalStateException) { }
        val sighting = repository.insertarAvistamiento(publication, SesionManager.obtenerUsuarioId(context),
            -12.1, -77.1, "La vi en el parque", null)
        repository.actualizarAvistamiento(sighting, -12.2, -77.2)
        assertEquals(-12.0, repository.obtenerPorIdPublicacion(publication)!!.latitud, 0.001)
        assertEquals(-12.2, repository.obtenerAvistamientosPorPublicacion(publication).first().latitud, 0.001)

        repository.validarLogin(emailA, "prueba123")
        assertEquals(listOf(publication), repository.obtenerPropias().map { it.id })
        val notifications = repository.obtenerNotificaciones()
        assertEquals(1, notifications.size)
        assertEquals(publication.toString(), notifications.first()["publicacionId"])
        repository.marcarTodasLeidas()
        assertEquals(0, repository.contarNoLeidas())
    }

    @Test fun fotoSeGuardaEnUnFormatoLegibleSinPersistirUnaUriPrivada() {
        val id = repository.registrar("Fotos", "foto-${UUID.randomUUID()}@example.com", "prueba123", ciudad = "Lima")
        val file = java.io.File(context.cacheDir, "test-photo.jpg")
        val bitmap = android.graphics.Bitmap.createBitmap(64, 64, android.graphics.Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(android.graphics.Color.GREEN)
        file.outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, it) }
        bitmap.recycle()
        try {
            val publication = repository.insertarPublicacion(id, "Adopcion", "Michi", "Busca hogar",
                android.net.Uri.fromFile(file).toString(), null, "Gato", -12.0, -77.0)
            val photo = repository.obtenerPorIdPublicacion(publication)!!.foto!!
            assertTrue(photo.startsWith("data:image/jpeg;base64,"))
            val bytes = android.util.Base64.decode(photo.substringAfter("base64,"), android.util.Base64.NO_WRAP)
            val decoded = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            assertNotNull(decoded)
            decoded?.recycle()
        } finally { file.delete() }
    }

    @Test fun unaContrasenaIncorrectaNoSeConfundeConUnCorreoInvalido() {
        val email = "invalid-${UUID.randomUUID()}@example.com"
        repository.registrar("Acceso", email, "prueba123", ciudad = "Lima")
        SesionManager.cerrarSesion(context)
        try {
            repository.validarLogin(email, "otraClave123")
            fail("La contraseña incorrecta debe rechazarse")
        } catch (error: Exception) {
            assertEquals("Correo o contraseña incorrectos", com.example.practicafinal.util.mensajeErrorAuth(error))
        }
    }

    private fun sincronizarFavoritos() {
        val latch = CountDownLatch(1)
        FavoritosManager.sincronizar(context) { latch.countDown() }
        assertTrue("La sincronización no terminó", latch.await(20, TimeUnit.SECONDS))
    }

    companion object {
        private var configured = false
        @Synchronized private fun configurarUnaVez() {
            if (!configured) {
                // Proyecto demo independiente: estas pruebas nunca escriben en el Firebase real.
                com.google.firebase.FirebaseApp.getInstance().delete()
                com.google.firebase.FirebaseApp.initializeApp(
                    ApplicationProvider.getApplicationContext<Context>(),
                    com.google.firebase.FirebaseOptions.Builder()
                        .setApplicationId("1:123456:android:petgoreview")
                        .setApiKey("fake-api-key")
                        .setProjectId("demo-petgo-review")
                        .setStorageBucket("demo-petgo-review.appspot.com")
                        .build()
                )
                FirebaseAuth.getInstance().useEmulator("10.0.2.2", 9190)
                FirebaseFirestore.getInstance().useEmulator("10.0.2.2", 8180)
                configured = true
            }
        }
    }
}
