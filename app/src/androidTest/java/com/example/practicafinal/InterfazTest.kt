package com.example.practicafinal

import android.content.Context
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import com.google.android.material.button.MaterialButton
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InterfazTest {
    @Test
    fun loginVacioMuestraErrorSinEnviarCredenciales() {
        ActivityScenario.launch(LoginActivity::class.java).use {
            onView(withId(R.id.btn_login)).perform(click())
            onView(withId(R.id.tv_error)).check(matches(withText("Completa todos los campos")))
        }
    }

    @Test
    fun enlaceDeRegistroYVolverConservanLaNavegacion() {
        ActivityScenario.launch(LoginActivity::class.java).use {
            onView(withId(R.id.tv_ir_registro)).perform(click())
            onView(withId(R.id.et_confirmar)).perform(androidx.test.espresso.action.ViewActions.scrollTo())
                .check(matches(isDisplayed()))
            onView(withId(R.id.tv_ir_login)).perform(androidx.test.espresso.action.ViewActions.scrollTo(), click())
            onView(withId(R.id.btn_login)).check(matches(isDisplayed()))
        }
    }

    @Test
    fun layoutsPrincipalesSeInflanEnLosTamanosSoportados() {
        val context =
            ContextThemeWrapper(ApplicationProvider.getApplicationContext<Context>(), R.style.Theme_PracticaFinal)
        val layouts = listOf(
            R.layout.activity_main, R.layout.activity_adopciones,
            R.layout.activity_mascotas_perdidas, R.layout.activity_crear_publicacion,
            R.layout.activity_crear_denuncia, R.layout.activity_reportar_avistamiento,
            R.layout.activity_perfil, R.layout.activity_notificaciones, R.layout.activity_mis_favoritos
        )
        val sizes = listOf(360 to 640, 360 to 800, 390 to 844, 412 to 915)
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val density = context.resources.displayMetrics.density
            for (layout in layouts) for ((width, height) in sizes) {
                val view = LayoutInflater.from(context).inflate(layout, FrameLayout(context), false)
                val w = (width * density).toInt()
                val h = (height * density).toInt()
                view.measure(
                    View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY)
                )
                view.layout(0, 0, w, h)
                assertTrue(view.measuredWidth == w && view.measuredHeight == h)
                if (layout == R.layout.activity_main) {
                    val hint = view.findViewById<TextView>(R.id.tv_pista_publicar)
                    assertEquals(View.GONE, hint.visibility)
                }
                view.findViewById<org.osmdroid.views.MapView>(R.id.map)?.onDetach()
                view.findViewById<org.osmdroid.views.MapView>(R.id.mapa_crear)?.onDetach()
                view.findViewById<org.osmdroid.views.MapView>(R.id.mapa_denuncia)?.onDetach()
                view.findViewById<org.osmdroid.views.MapView>(R.id.mapa_avistamiento)?.onDetach()
            }
        }
    }

    @Test
    fun botonUbicacionAnunciaSeguimientoDeLaPosicion() {
        val context =
            ContextThemeWrapper(ApplicationProvider.getApplicationContext<Context>(), R.style.Theme_PracticaFinal)
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val view = LayoutInflater.from(context).inflate(R.layout.activity_main, FrameLayout(context), false)
            val button = view.findViewById<MaterialButton>(R.id.fab_recenter)

            assertEquals("Seguir mi ubicación", button.contentDescription.toString())
            view.findViewById<org.osmdroid.views.MapView>(R.id.map).onDetach()
        }
    }

    @Test
    fun fotoDelPanelDelMapaSeMuestraCompleta() {
        val context =
            ContextThemeWrapper(ApplicationProvider.getApplicationContext<Context>(), R.style.Theme_PracticaFinal)
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val view = LayoutInflater.from(context).inflate(R.layout.activity_main, FrameLayout(context), false)
            val foto = view.findViewById<android.widget.ImageView>(R.id.panel_foto)

            assertEquals(android.widget.ImageView.ScaleType.FIT_CENTER, foto.scaleType)
            assertTrue(foto.layoutParams.height >= (160 * context.resources.displayMetrics.density).toInt())
            view.findViewById<org.osmdroid.views.MapView>(R.id.map).onDetach()
        }
    }

    @Test
    fun botonesAccionesDelMapaNoPartenSusEtiquetas() {
        val context =
            ContextThemeWrapper(ApplicationProvider.getApplicationContext<Context>(), R.style.Theme_PracticaFinal)
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val view = LayoutInflater.from(context).inflate(R.layout.activity_main, FrameLayout(context), false)
            val botones = listOf(R.id.panel_btn_izq, R.id.panel_btn_der, R.id.panel_btn_extra)
                .map { view.findViewById<MaterialButton>(it) }

            assertTrue(botones.all { it.maxLines == 1 })
            assertTrue(botones.all { it.ellipsize == android.text.TextUtils.TruncateAt.END })
            view.findViewById<org.osmdroid.views.MapView>(R.id.map).onDetach()
        }
    }

    @Test
    fun listaAdopcionesNoCortaLasTarjetas() {
        val context =
            ContextThemeWrapper(ApplicationProvider.getApplicationContext<Context>(), R.style.Theme_PracticaFinal)
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val view = LayoutInflater.from(context).inflate(R.layout.activity_adopciones, FrameLayout(context), false)
            val lista = view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rv_animales)
            assertEquals(ViewGroup.LayoutParams.WRAP_CONTENT, lista.layoutParams.height)
            assertFalse(lista.isNestedScrollingEnabled)
        }
    }

    @Test
    fun avisoWhatsAppExplicaPrivacidadYAlternativaDeAdopcion() {
        val context =
            ContextThemeWrapper(ApplicationProvider.getApplicationContext<Context>(), R.style.Theme_PracticaFinal)
        val view =
            LayoutInflater.from(context).inflate(R.layout.activity_crear_publicacion, FrameLayout(context), false)
        val avisoId = context.resources.getIdentifier("tv_contacto_privacidad", "id", context.packageName)
        assertTrue("Falta la aclaración del contacto por WhatsApp", avisoId != 0)
        val aviso = view.findViewById<TextView>(avisoId)

        assertNotNull("Falta la aclaración del contacto por WhatsApp", aviso)
        assertTrue(aviso.text.contains("Opcional"))
        assertTrue(aviso.text.contains("WhatsApp"))
        assertTrue(aviso.text.contains("solicitud desde PetGo"))
    }

    @Test
    fun menuOpcionesNoMuestraCuadroAnuncio() {
        val context =
            ContextThemeWrapper(ApplicationProvider.getApplicationContext<Context>(), R.style.Theme_PracticaFinal)
        val view = LayoutInflater.from(context).inflate(R.layout.activity_menu_opciones, FrameLayout(context), false)
        val anuncioId = context.resources.getIdentifier("item_anuncio", "id", context.packageName)

        assertEquals("El menú no debe contener el cuadro Anuncio", 0, anuncioId)
        assertFalse(textosVisibles(view).contains("Anuncio"))
    }

    @Test
    fun registroNoMuestraCampoCiudad() {
        val context =
            ContextThemeWrapper(ApplicationProvider.getApplicationContext<Context>(), R.style.Theme_PracticaFinal)
        val view = LayoutInflater.from(context).inflate(R.layout.activity_registro, FrameLayout(context), false)
        val cityId = context.resources.getIdentifier("et_ciudad", "id", context.packageName)
        assertEquals("El registro no debe tener un campo Ciudad", 0, cityId)
        assertFalse(textosVisibles(view).contains("Ciudad"))
    }

    @Test
    fun filtrosPerdidasSeDesplieganDesdeElIcono() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val toggleId = context.resources.getIdentifier("btn_toggle_filtros", "id", context.packageName)
        val panelId = context.resources.getIdentifier("panel_filtros_secundarios", "id", context.packageName)
        assertTrue("Faltan los controles para desplegar filtros de perdidas", toggleId != 0 && panelId != 0)

        ActivityScenario.launch(MascotasPerdidasActivity::class.java).use {
            onView(withId(panelId)).check(matches(withEffectiveVisibility(androidx.test.espresso.matcher.ViewMatchers.Visibility.GONE)))
            onView(withId(toggleId)).perform(click())
            onView(withId(panelId)).check(matches(isDisplayed()))
            onView(withId(toggleId)).check(matches(withContentDescription("Ocultar filtros")))
            onView(withId(toggleId)).perform(click())
            onView(withId(panelId)).check(matches(withEffectiveVisibility(androidx.test.espresso.matcher.ViewMatchers.Visibility.GONE)))
        }
    }

    @Test
    fun filtrosAdopcionesSeDesplieganDesdeElIcono() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val toggleId = context.resources.getIdentifier("btn_toggle_filtros", "id", context.packageName)
        val panelId = context.resources.getIdentifier("panel_filtros_secundarios", "id", context.packageName)
        assertTrue("Faltan los controles para desplegar filtros de adopciones", toggleId != 0 && panelId != 0)

        ActivityScenario.launch(AdopcionesActivity::class.java).use {
            onView(withId(panelId)).check(matches(withEffectiveVisibility(androidx.test.espresso.matcher.ViewMatchers.Visibility.GONE)))
            onView(withId(toggleId)).perform(click())
            onView(withId(panelId)).check(matches(isDisplayed()))
            onView(withId(toggleId)).check(matches(withContentDescription("Ocultar filtros")))
            onView(withId(toggleId)).perform(click())
            onView(withId(panelId)).check(matches(withEffectiveVisibility(androidx.test.espresso.matcher.ViewMatchers.Visibility.GONE)))
        }
    }

    @Test
    fun panelDelMapaUsaImagenYAccionesProporcionadas() {
        val context =
            ContextThemeWrapper(ApplicationProvider.getApplicationContext<Context>(), R.style.Theme_PracticaFinal)
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val view = LayoutInflater.from(context).inflate(R.layout.activity_main, FrameLayout(context), false)
            try {
                val imagen = view.findViewById<View>(R.id.panel_foto)
                val tarjetaPerdida = LayoutInflater.from(context)
                    .inflate(R.layout.item_perdida_card_big, FrameLayout(context), false)
                val marcoFotoPerdida = tarjetaPerdida.findViewById<View>(R.id.img_foto).parent as View
                assertEquals(marcoFotoPerdida.layoutParams.height, imagen.layoutParams.height)

                val botones = listOf(
                    view.findViewById<View>(R.id.panel_compartir),
                    view.findViewById<View>(R.id.panel_btn_izq),
                    view.findViewById<View>(R.id.panel_btn_der)
                )
                assertEquals(1, botones.map { it.layoutParams.height }.distinct().size)
            } finally {
                view.findViewById<org.osmdroid.views.MapView>(R.id.map)?.onDetach()
            }
        }
    }

    @Test
    fun detalleAdopcionMuestraPublicadorYAccionesDelMismoTamano() {
        val context =
            ContextThemeWrapper(ApplicationProvider.getApplicationContext<Context>(), R.style.Theme_PracticaFinal)
        val view = LayoutInflater.from(context).inflate(R.layout.activity_detalle_adopcion, FrameLayout(context), false)
        val ubicacion = view.findViewById<View>(R.id.tv_ubicacion)
        assertTrue("La ubicación debe ser un botón", ubicacion is MaterialButton)
        assertEquals(ViewGroup.LayoutParams.MATCH_PARENT, ubicacion.layoutParams.width)
        assertTrue("No repetir el encabezado Ubicación", textosVisibles(view).none { it == "Ubicación" })

        val idPublicador = context.resources.getIdentifier("tv_publicado_por", "id", context.packageName)
        val publicador = view.findViewById<TextView>(idPublicador)
        assertNotNull(publicador)
        assertTrue(publicador.text.startsWith("Publicado por "))

        val botones = listOf(R.id.tv_ubicacion, R.id.btn_solicitud_adopcion, R.id.btn_whatsapp, R.id.btn_favorito)
            .map { view.findViewById<View>(it) }
        assertTrue(botones.all { it.layoutParams.width == ViewGroup.LayoutParams.MATCH_PARENT })
        assertEquals(1, botones.map { it.layoutParams.height }.distinct().size)
    }

    @Test
    fun detallePerdidaDejaLaActualizacionAlFinalYUniformaLosBotones() {
        val context =
            ContextThemeWrapper(ApplicationProvider.getApplicationContext<Context>(), R.style.Theme_PracticaFinal)
        val view =
            LayoutInflater.from(context).inflate(R.layout.activity_detalle_publicacion, FrameLayout(context), false)
        val ubicacion = view.findViewById<View>(R.id.tv_ubicacion)
        assertTrue("Ver zona aproximada debe ser un botón", ubicacion is MaterialButton)
        assertEquals(ViewGroup.LayoutParams.MATCH_PARENT, ubicacion.layoutParams.width)

        val resumen = view.findViewById<View>(R.id.tv_resumen_actividad)
        val contenido = resumen.parent as ViewGroup
        assertEquals(contenido.childCount - 1, contenido.indexOfChild(resumen))
        val botones = listOf(
            R.id.tv_ubicacion,
            R.id.btn_historial_avistamientos,
            R.id.btn_avistamiento,
            R.id.btn_contactar,
            R.id.btn_compartir
        ).map { view.findViewById<View>(it) }
        assertEquals(1, botones.map { it.layoutParams.height }.distinct().size)
    }

    @Test
    fun formularioAvistamientoPliegaLaInformacionDeSeguridad() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val toggleId = context.resources.getIdentifier("btn_info_seguridad", "id", context.packageName)
        val infoId = context.resources.getIdentifier("tv_info_seguridad", "id", context.packageName)
        ActivityScenario.launch(ReportarAvistamientoActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val info = checkNotNull(activity.findViewById<View>(infoId))
                assertEquals(View.GONE, info.visibility)
                val textos = textosVisibles(activity.window.decorView)
                assertFalse(textos.any { it.startsWith("El reporte se guarda en este teléfono") })
                assertFalse(textos.any { it.startsWith("Por seguridad,") })
            }
            onView(withId(infoId)).check(matches(withEffectiveVisibility(androidx.test.espresso.matcher.ViewMatchers.Visibility.GONE)))
            onView(withId(toggleId)).perform(click())
            onView(withId(infoId)).check(matches(isDisplayed()))
            onView(withId(toggleId)).perform(click())
            onView(withId(infoId)).check(matches(withEffectiveVisibility(androidx.test.espresso.matcher.ViewMatchers.Visibility.GONE)))
        }
    }

    @Test
    fun adopcionesQuitaLaGuiaYConservaLaProporcionDeLasTarjetas() {
        val context =
            ContextThemeWrapper(ApplicationProvider.getApplicationContext<Context>(), R.style.Theme_PracticaFinal)
        val view = LayoutInflater.from(context).inflate(R.layout.activity_adopciones, FrameLayout(context), false)
        val textos = textosVisibles(view)
        assertFalse(textos.contains("¿Cómo funciona la adopción?"))
        assertFalse(textos.contains("Conocer el proceso completo"))

        val adapter = com.example.practicafinal.adaptadores.AdaptadorAdopciones(emptyList(), null, null) {}
        val holder = adapter.onCreateViewHolder(FrameLayout(context), 1)
        val tarjetaPerdida = LayoutInflater.from(context)
            .inflate(R.layout.item_perdida_card_big, FrameLayout(context), false)
        assertEquals(
            tarjetaPerdida.findViewById<View>(R.id.img_foto).parent.let { (it as View).layoutParams.height },
            (holder.itemView.findViewById<View>(R.id.img_foto).parent as View).layoutParams.height
        )
    }

    @Test
    fun tarjetaAdopcionEmpiezaCompactaYSeExpandeAlTocarla() {
        val context =
            ContextThemeWrapper(ApplicationProvider.getApplicationContext<Context>(), R.style.Theme_PracticaFinal)
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val publicacion = com.example.practicafinal.modelo.Publicacion(
                id = 1,
                usuarioId = 7,
                tipo = "Adopcion",
                nombre = "Kira",
                descripcion = "Busca familia",
                foto = null,
                ultimoLugar = null,
                especie = "Perro",
                latitud = -12.0,
                longitud = -77.0,
                fechaCreacion = System.currentTimeMillis(),
                estado = "Activa"
            )
            var detallesAbiertos = false
            var mapaAbierto = false
            val adapter = com.example.practicafinal.adaptadores.AdaptadorAdopciones(
                listOf(publicacion), null, null,
                onVerMapa = { mapaAbierto = true }
            ) { detallesAbiertos = true }
            val density = context.resources.displayMetrics.density
            val recycler = androidx.recyclerview.widget.RecyclerView(context).apply {
                layoutManager = androidx.recyclerview.widget.LinearLayoutManager(context)
                this.adapter = adapter
            }
            val ancho = (340 * density).toInt()
            val alto = (600 * density).toInt()
            fun layoutRecycler() {
                recycler.measure(
                    View.MeasureSpec.makeMeasureSpec(ancho, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(alto, View.MeasureSpec.EXACTLY)
                )
                recycler.layout(0, 0, ancho, alto)
            }
            assertEquals(0, adapter.getItemViewType(0))
            layoutRecycler()
            val compacta = checkNotNull(recycler.findViewHolderForAdapterPosition(0))
            val marcoCompacto = compacta.itemView.findViewById<View>(R.id.img_foto).parent as View
            assertEquals((80 * density).toInt(), marcoCompacto.layoutParams.height)

            compacta.itemView.performClick()
            assertEquals(1, adapter.getItemViewType(0))
            layoutRecycler()
            val ampliada = checkNotNull(recycler.findViewHolderForAdapterPosition(0))
            val marcoAmpliado = ampliada.itemView.findViewById<View>(R.id.img_foto).parent as View
            assertEquals((160 * density).toInt(), marcoAmpliado.layoutParams.height)
            assertNotNull(ampliada.itemView.findViewById<MaterialButton>(R.id.btn_detalles))
            assertNotNull(ampliada.itemView.findViewById<MaterialButton>(R.id.btn_ver_mapa))
            ampliada.itemView.findViewById<MaterialButton>(R.id.btn_detalles).performClick()
            assertTrue(detallesAbiertos)
            ampliada.itemView.findViewById<MaterialButton>(R.id.btn_ver_mapa).performClick()
            assertTrue(mapaAbierto)
        }
    }

    private fun textosVisibles(view: View): List<String> {
        if (view.visibility != View.VISIBLE) return emptyList()
        val propio = (view as? TextView)?.text?.toString()?.let(::listOf).orEmpty()
        if (view !is ViewGroup) return propio
        return propio + (0 until view.childCount).flatMap { textosVisibles(view.getChildAt(it)) }
    }
}
