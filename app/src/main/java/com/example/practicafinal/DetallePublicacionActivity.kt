package com.example.practicafinal

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.practicafinal.controlador.ControladorPublicaciones
import com.example.practicafinal.modelo.Publicacion
import com.bumptech.glide.Glide
import java.util.concurrent.Executors

class DetallePublicacionActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PUBLICACION_ID = "detalle_publicacion_id"
    }

    private val executor = Executors.newSingleThreadExecutor()
    private var publicacionId: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalle_publicacion)

        findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
            .setNavigationOnClickListener { finish() }

        publicacionId = intent.getLongExtra(EXTRA_PUBLICACION_ID, -1L)
        if (publicacionId == -1L) { finish(); return }

        cargarPublicacion()
    }

    private fun cargarPublicacion() {
        executor.execute {
            val pub = ControladorPublicaciones.obtenerPorId(this, publicacionId)
            runOnUiThread {
                if (pub == null) { finish(); return@runOnUiThread }
                mostrar(pub)
            }
        }
    }

    private fun mostrar(p: Publicacion) {
        val imgFoto = findViewById<android.widget.ImageView>(R.id.img_foto)
        val tvPlaceholder = findViewById<TextView>(R.id.tv_placeholder)
        if (!p.foto.isNullOrBlank()) {
            com.example.practicafinal.util.cargarImagen(imgFoto, p.foto)
            tvPlaceholder.visibility = View.GONE
        } else {
            tvPlaceholder.visibility = View.VISIBLE
            tvPlaceholder.text = when {
                p.especie.equals("Perro", ignoreCase = true) -> "\uD83D\uDC36"
                p.especie.equals("Gato", ignoreCase = true) -> "\uD83D\uDC31"
                else -> "\uD83D\uDC3E"
            }
        }

        val tvEstado = findViewById<TextView>(R.id.tv_estado)
        when (p.estado) {
            "Resuelta" -> { tvEstado.text = "Resuelta"; tvEstado.setTextColor(ContextCompat.getColor(this, android.R.color.darker_gray)) }
            "Adoptada" -> { tvEstado.text = "Adoptada"; tvEstado.setTextColor(ContextCompat.getColor(this, R.color.colorSecondary)) }
            else -> { tvEstado.text = "Activa"; tvEstado.setTextColor(ContextCompat.getColor(this, R.color.verde_estado)) }
        }

        findViewById<TextView>(R.id.tv_nombre).text = p.nombre
        findViewById<TextView>(R.id.tv_tipo).text = when (p.tipo) {
            "Perdida" -> "Mascota perdida"
            "Encontrada" -> "Mascota encontrada"
            else -> "En adopcion"
        }
        findViewById<TextView>(R.id.tv_descripcion).text = p.descripcion

        findViewById<TextView>(R.id.tv_fecha).text = com.example.practicafinal.util.fechaRelativa(p.fechaCreacion)

        findViewById<TextView>(R.id.tv_ubicacion).apply {
            text = "Ver ubicacion en el mapa"
            setOnClickListener {
                startActivity(Intent(this@DetallePublicacionActivity, MainActivity::class.java).apply {
                    putExtra("centrar_lat", p.latitud)
                    putExtra("centrar_lng", p.longitud)
                    putExtra("mostrar_alerta", publicacionId)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                })
            }
        }

        val btnAvist = findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_avistamiento)
        val resuelta = p.estado == "Resuelta" || p.estado == "Adoptada"
        if (p.tipo == "Perdida" && !resuelta) {
            btnAvist.visibility = View.VISIBLE
            btnAvist.setOnClickListener {
                startActivity(Intent(this, ReportarAvistamientoActivity::class.java).apply {
                    putExtra(ReportarAvistamientoActivity.EXTRA_PUBLICACION_ID, p.id)
                    putExtra(ReportarAvistamientoActivity.EXTRA_NOMBRE, p.nombre)
                })
            }
        } else {
            btnAvist.visibility = View.GONE
        }

        findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_compartir).setOnClickListener {
            val texto = "${p.nombre} . ${tl(p.tipo)}\n${p.descripcion}\nEstado: ${p.estado}\nhttps://maps.google.com/?q=${p.latitud},${p.longitud}"
            startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"; putExtra(Intent.EXTRA_TEXT, texto)
            }, "Compartir alerta"))
        }
    }

    private fun tl(t: String) = when (t) {
        "Perdida" -> "Mascota perdida"; "Encontrada" -> "Mascota encontrada"; else -> "En adopcion"
    }

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdown()
    }
}
