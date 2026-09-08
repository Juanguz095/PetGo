package com.example.practicafinal

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.practicafinal.controlador.ControladorDenuncias
import com.bumptech.glide.Glide
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
        exec.execute {
            val d = ControladorDenuncias.obtenerDenuncias(this).find { it.id == id }
            runOnUiThread {
                if (d == null) {
                    finish(); return@runOnUiThread
                }
                val icono = when (d.motivo) {
                    "Maltrato" -> "ðŸš¨"; "Abandono" -> "ðŸšï¸"; else -> "ðŸ’°"
                }
                val img = findViewById<android.widget.ImageView>(R.id.img_foto)
                val pl = findViewById<TextView>(R.id.tv_placeholder)
                pl.text = icono
                if (!d.foto.isNullOrBlank()) {
                    Glide.with(this).load(d.foto).into(img)
                    pl.visibility = View.GONE
                } else {
                    pl.visibility = View.VISIBLE
                }
                findViewById<TextView>(R.id.tv_motivo).text = "$icono ${d.motivo}"
                findViewById<TextView>(R.id.tv_descripcion).text = d.descripcion
                findViewById<TextView>(R.id.tv_fecha).text = "Reportado ${fechaRelativa(d.fecha)}"
                findViewById<TextView>(R.id.tv_ubicacion).setOnClickListener {
                    startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("geo:${d.latitud},${d.longitud}?q=${d.latitud},${d.longitud}")
                        )
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy(); exec.shutdown()
    }
}
