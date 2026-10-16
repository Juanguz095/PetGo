package com.example.practicafinal

import android.os.Bundle
import android.content.Intent
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.example.practicafinal.util.ejecutarSeguro
import com.example.practicafinal.util.actualizarUi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.practicafinal.adaptadores.AdaptadorPublicacionesPropias
import com.example.practicafinal.controlador.ControladorPublicaciones
import com.example.practicafinal.session.FavoritosManager
import java.util.concurrent.Executors

class MisFavoritosActivity : AppCompatActivity() {

    private val exec = Executors.newSingleThreadExecutor()
    private lateinit var rv: androidx.recyclerview.widget.RecyclerView
    private lateinit var tvVacio: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mis_favoritos)
        findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }
        rv = findViewById(R.id.rv_favoritos); tvVacio = findViewById(R.id.tv_vacio)
        rv.layoutManager = LinearLayoutManager(this)
    }

    private fun cargar() {
        ejecutarSeguro(exec) {
            val idsFav = FavoritosManager.obtenerIds(this)
            val pubs = ControladorPublicaciones.obtenerPublicaciones(this).filter { idsFav.contains(it.id.toString()) }
            actualizarUi {
                tvVacio.visibility = if (pubs.isEmpty()) View.VISIBLE else View.GONE
                rv.adapter = AdaptadorPublicacionesPropias(
                    pubs, onResolver = {}, onQuitarFavorito = { publicacion ->
                        FavoritosManager.toggle(this, publicacion.id)
                        Toast.makeText(this, "Quitado de favoritos", Toast.LENGTH_SHORT).show()
                        cargar()
                    },
                    mostrarBotonResolver = false,
                    onAbrir = { publicacion ->
                        val destino = if (publicacion.tipo == "Adopcion") DetalleAdopcionActivity::class.java else DetallePublicacionActivity::class.java
                        startActivity(Intent(this, destino).putExtra("detalle_publicacion_id", publicacion.id))
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        cargar()
        FavoritosManager.sincronizar(this) { actualizarUi { cargar() } }
    }

    override fun onDestroy() {
        super.onDestroy(); exec.shutdown()
    }
}
