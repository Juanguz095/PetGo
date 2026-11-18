package com.example.practicafinal

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import com.example.practicafinal.util.ejecutarSeguro
import com.example.practicafinal.util.actualizarUi
import androidx.appcompat.app.AlertDialog
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
    private var puedeModerar = false
    private var modoModeracion = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_denuncias)
        findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }
        rv = findViewById(R.id.rv_denuncias); tvVacio = findViewById(R.id.tv_vacio)
        rv.layoutManager = LinearLayoutManager(this)
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_denunciar).setOnClickListener {
            startActivity(Intent(this, CrearDenunciaActivity::class.java))
        }
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_moderar_denuncias)
            .setOnClickListener {
                if (!puedeModerar) return@setOnClickListener
                modoModeracion = !modoModeracion
                actualizarEtiquetaModo()
                cargar()
            }
        verificarPermisoModeracion()
    }

    private fun verificarPermisoModeracion() {
        ejecutarSeguro(exec) {
            val permitido = runCatching { ControladorDenuncias.esModerador(this) }.getOrDefault(false)
            actualizarUi {
                puedeModerar = permitido
                findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_moderar_denuncias)
                    .visibility = if (permitido) View.VISIBLE else View.GONE
            }
        }
    }

    private fun actualizarEtiquetaModo() {
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_moderar_denuncias)
            .text = if (modoModeracion) "Mis reportes" else "Moderación"
        findViewById<TextView>(R.id.tv_encabezado_denuncias).text = if (modoModeracion) {
            "Revisión privada de reportes"
        } else "Reporta maltrato de forma anónima"
    }

    private fun iconoMotivo(m: String) = when (m) {
        "Maltrato" -> "\uD83D\uDEA8"; "Abandono" -> "\uD83C\uDFE0"; else -> "\uD83D\uDCB0"
    }

    private fun cargar() {
        ejecutarSeguro(exec) {
            val lista = if (modoModeracion) ControladorDenuncias.obtenerDenunciasModeracion(this)
                else ControladorDenuncias.obtenerDenuncias(this)
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
                        (h.itemView.findViewById<TextView>(R.id.tv_estado)).text = "Estado: ${d.estado}"
                        h.itemView.setOnClickListener {
                            if (modoModeracion) mostrarDetalleModeracion(d) else {
                                startActivity(
                                    Intent(
                                        this@DenunciasActivity,
                                        DetalleDenunciaActivity::class.java
                                    ).apply { putExtra("denuncia_id", d.id) })
                            }
                        }
                    }

                    override fun getItemCount() = lista.size
                }
            }
        }
    }

    private fun mostrarDetalleModeracion(denuncia: com.example.practicafinal.modelo.Denuncia) {
        AlertDialog.Builder(this)
            .setTitle("Folio PG-${denuncia.id.toString().takeLast(6)} · ${denuncia.motivo}")
            .setMessage("${denuncia.descripcion}\n\nEstado actual: ${denuncia.estado}\nUbicación: ${denuncia.latitud}, ${denuncia.longitud}")
            .setNegativeButton("Cerrar", null)
            .setPositiveButton("Cambiar estado") { _, _ -> elegirEstadoModeracion(denuncia) }
            .show()
            .also { com.example.practicafinal.util.Dialogos.aplicar(this, it) }
    }

    private fun elegirEstadoModeracion(denuncia: com.example.practicafinal.modelo.Denuncia) {
        val estados = arrayOf("Recibida", "En revisión", "Atendida", "No procede")
        AlertDialog.Builder(this)
            .setTitle("Actualizar folio PG-${denuncia.id.toString().takeLast(6)}")
            .setItems(estados) { _, indice ->
                ejecutarSeguro(exec) {
                    try {
                        ControladorDenuncias.actualizarEstadoModeracion(this, denuncia.id, estados[indice])
                        actualizarUi {
                            Toast.makeText(this, "Estado actualizado", Toast.LENGTH_SHORT).show()
                            cargar()
                        }
                    } catch (error: Exception) {
                        actualizarUi {
                            Toast.makeText(this, error.message ?: "No se pudo actualizar", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
            .show()
            .also { com.example.practicafinal.util.Dialogos.aplicar(this, it) }
    }

    override fun onResume() {
        super.onResume(); cargar()
    }

    override fun onDestroy() {
        super.onDestroy(); exec.shutdown()
    }
}
