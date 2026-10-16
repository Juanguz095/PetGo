package com.example.practicafinal

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.example.practicafinal.util.ejecutarSeguro
import com.example.practicafinal.util.actualizarUi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.practicafinal.adaptadores.AdaptadorPublicacionesPropias
import com.example.practicafinal.controlador.ControladorPublicaciones
import com.example.practicafinal.session.SesionManager
import java.util.concurrent.Executors

class MisPublicacionesActivity : AppCompatActivity() {

    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var rv: androidx.recyclerview.widget.RecyclerView
    private lateinit var tvVacio: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mis_publicaciones)

        findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
            .setNavigationOnClickListener { finish() }

        rv = findViewById(R.id.rv_publicaciones)
        tvVacio = findViewById(R.id.tv_vacio)
        rv.layoutManager = LinearLayoutManager(this)

        cargarPublicaciones()
    }

    private fun cargarPublicaciones() {
        val usuarioId = SesionManager.obtenerUsuarioId(this) ?: return
        ejecutarSeguro(executor) {
            val propias = ControladorPublicaciones.obtenerPropias(this)
            actualizarUi {
                tvVacio.visibility = if (propias.isEmpty()) View.VISIBLE else View.GONE
                rv.adapter = AdaptadorPublicacionesPropias(
                    propias,
                    onResolver = { publicacion -> resolverPublicacion(publicacion) },
                    onAdoptar = { publicacion -> adoptarPublicacion(publicacion) }
                )
            }
        }
    }

    private fun resolverPublicacion(publicacion: com.example.practicafinal.modelo.Publicacion) {
        ejecutarSeguro(executor) {
            ControladorPublicaciones.resolver(this, publicacion.id)
            actualizarUi {
                Toast.makeText(this, "Publicacion resuelta", Toast.LENGTH_SHORT).show()
                cargarPublicaciones()
            }
        }
    }

    private fun adoptarPublicacion(publicacion: com.example.practicafinal.modelo.Publicacion) {
        ejecutarSeguro(executor) {
            ControladorPublicaciones.adoptar(this, publicacion.id)
            actualizarUi {
                Toast.makeText(this, "Mascota marcada como adoptada", Toast.LENGTH_SHORT).show()
                cargarPublicaciones()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdown()
    }
}
