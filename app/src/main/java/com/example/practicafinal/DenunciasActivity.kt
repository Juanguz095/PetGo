package com.example.practicafinal

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.example.practicafinal.util.ejecutarSeguro
import com.example.practicafinal.util.actualizarUi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.practicafinal.controlador.ControladorDenuncias
import com.example.practicafinal.util.fechaRelativa
import java.util.concurrent.Executors

class DenunciasActivity : AppCompatActivity() {

    private val exec = Executors.newSingleThreadExecutor()
    private lateinit var rv: RecyclerView;
    private lateinit var tvVacio: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_denuncias)
        findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }
        rv = findViewById(R.id.rv_denuncias); tvVacio = findViewById(R.id.tv_vacio)
        rv.layoutManager = LinearLayoutManager(this)
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_denunciar).setOnClickListener {
            startActivity(Intent(this, CrearDenunciaActivity::class.java))
        }
    }

    private fun iconoMotivo(m: String) = when (m) {
        "Maltrato" -> "\uD83D\uDEA8"; "Abandono" -> "\uD83C\uDFE0"; else -> "\uD83D\uDCB0"
    }

    private fun cargar() {
        ejecutarSeguro(exec) {
            val lista = ControladorDenuncias.obtenerDenuncias(this)
            actualizarUi {
                tvVacio.visibility = if (lista.isEmpty()) View.VISIBLE else View.GONE
                rv.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
                    override fun onCreateViewHolder(p: ViewGroup, v: Int) = object : RecyclerView.ViewHolder(
                        LayoutInflater.from(p.context).inflate(R.layout.item_denuncia_card, p, false)
                    ) {}

                    override fun onBindViewHolder(h: RecyclerView.ViewHolder, pos: Int) {
                        val d = lista[pos]
                        val img = h.itemView.findViewById<android.widget.ImageView>(R.id.img_foto)
                        val pl = h.itemView.findViewById<TextView>(R.id.tv_placeholder)
                        pl.text = iconoMotivo(d.motivo)
                        if (!d.foto.isNullOrBlank()) {
                            com.example.practicafinal.util.cargarImagen(img, d.foto)
                            pl.visibility = View.GONE
                        } else {
                            com.example.practicafinal.util.limpiarImagen(img)
                            pl.visibility = View.VISIBLE
                        }
                        (h.itemView.findViewById<TextView>(R.id.tv_motivo)).text =
                            "${iconoMotivo(d.motivo)} ${d.motivo}"
                        (h.itemView.findViewById<TextView>(R.id.tv_desc)).text = d.descripcion.take(80)
                        (h.itemView.findViewById<TextView>(R.id.tv_fecha)).text = fechaRelativa(d.fecha)
                        h.itemView.setOnClickListener {
                            startActivity(
                                Intent(
                                    this@DenunciasActivity,
                                    DetalleDenunciaActivity::class.java
                                ).apply { putExtra("denuncia_id", d.id) })
                        }
                    }

                    override fun getItemCount() = lista.size
                }
            }
        }
    }

    override fun onResume() {
        super.onResume(); cargar()
    }

    override fun onDestroy() {
        super.onDestroy(); exec.shutdown()
    }
}
