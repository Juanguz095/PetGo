package com.example.practicafinal

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import com.example.practicafinal.util.ejecutarSeguro
import com.example.practicafinal.util.actualizarUi
import androidx.appcompat.app.AppCompatActivity
import com.example.practicafinal.controlador.ControladorDenuncias
import com.example.practicafinal.util.fechaRelativa
import java.util.concurrent.Executors

class DetalleDenunciaActivity : AppCompatActivity() {
    private val exec = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalle_denuncia)
        findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }
        val id = intent.getLongExtra("denuncia_id", -1L)
        if (id == -1L) {
            finish(); return
        }
        ejecutarSeguro(exec) {
            val d = ControladorDenuncias.obtenerDenuncias(this).find { it.id == id }
            actualizarUi {
                if (d == null) {
                    finish(); return@actualizarUi
                }
                val icono = when (d.motivo) {
                    "Maltrato" -> "\uD83D\uDEA8"; "Abandono" -> "\uD83C\uDFE0"; else -> "\uD83D\uDCB0"
                }
                val img = findViewById<android.widget.ImageView>(R.id.img_foto)
                val pl = findViewById<TextView>(R.id.tv_placeholder)
                pl.text = icono
                if (!d.foto.isNullOrBlank()) {
                    com.example.practicafinal.util.cargarImagen(img, d.foto)
                    pl.visibility = View.GONE
                } else {
                    pl.visibility = View.VISIBLE
                }
                findViewById<TextView>(R.id.tv_motivo).text = "$icono ${d.motivo}"
                findViewById<TextView>(R.id.tv_descripcion).text = d.descripcion
                findViewById<TextView>(R.id.tv_fecha).text = "Reportado ${fechaRelativa(d.fecha)}"
                findViewById<TextView>(R.id.tv_ubicacion).setOnClickListener {
                    startActivity(Intent(this, MainActivity::class.java).apply {
                        putExtra("centrar_lat", d.latitud)
                        putExtra("centrar_lng", d.longitud)
                        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    })
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy(); exec.shutdown()
    }
}
