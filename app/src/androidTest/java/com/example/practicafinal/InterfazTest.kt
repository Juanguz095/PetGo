package com.example.practicafinal

import android.content.Context
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InterfazTest {
    @Test fun loginVacioMuestraErrorSinEnviarCredenciales() {
        ActivityScenario.launch(LoginActivity::class.java).use {
            onView(withId(R.id.btn_login)).perform(click())
            onView(withId(R.id.tv_error)).check(matches(withText("Completa todos los campos")))
        }
    }

    @Test fun enlaceDeRegistroYVolverConservanLaNavegacion() {
        ActivityScenario.launch(LoginActivity::class.java).use {
            onView(withId(R.id.tv_ir_registro)).perform(click())
            onView(withId(R.id.et_confirmar)).perform(androidx.test.espresso.action.ViewActions.scrollTo()).check(matches(isDisplayed()))
            onView(withId(R.id.tv_ir_login)).perform(androidx.test.espresso.action.ViewActions.scrollTo(), click())
            onView(withId(R.id.btn_login)).check(matches(isDisplayed()))
        }
    }

    @Test fun layoutsPrincipalesSeInflanEnLosTamanosSoportados() {
        val context = ContextThemeWrapper(ApplicationProvider.getApplicationContext<Context>(), R.style.Theme_PracticaFinal)
        val layouts = listOf(R.layout.activity_main, R.layout.activity_adopciones,
            R.layout.activity_mascotas_perdidas, R.layout.activity_crear_publicacion,
            R.layout.activity_crear_denuncia, R.layout.activity_reportar_avistamiento,
            R.layout.activity_perfil, R.layout.activity_notificaciones, R.layout.activity_mis_favoritos)
        val sizes = listOf(360 to 640, 360 to 800, 390 to 844, 412 to 915)
        androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().runOnMainSync {
            val density = context.resources.displayMetrics.density
            for (layout in layouts) for ((width, height) in sizes) {
                val view = LayoutInflater.from(context).inflate(layout, FrameLayout(context), false)
                val w = (width * density).toInt()
                val h = (height * density).toInt()
                view.measure(View.MeasureSpec.makeMeasureSpec(w, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(h, View.MeasureSpec.EXACTLY))
                view.layout(0, 0, w, h)
                assertTrue(view.measuredWidth == w && view.measuredHeight == h)
                if (layout == R.layout.activity_main) {
                    val hint = view.findViewById<TextView>(R.id.tv_pista_publicar)
                    val button = view.findViewById<View>(R.id.controls_recenter)
                    assertTrue("El texto del mapa invade el botón de ubicación", hint.right <= button.left)
                }
                view.findViewById<org.osmdroid.views.MapView>(R.id.map)?.onDetach()
                view.findViewById<org.osmdroid.views.MapView>(R.id.mapa_crear)?.onDetach()
                view.findViewById<org.osmdroid.views.MapView>(R.id.mapa_denuncia)?.onDetach()
                view.findViewById<org.osmdroid.views.MapView>(R.id.mapa_avistamiento)?.onDetach()
            }
        }
    }
}
