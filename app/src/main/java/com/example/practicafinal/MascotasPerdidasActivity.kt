package com.example.practicafinal

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.EditText
import androidx.core.widget.doAfterTextChanged
import androidx.core.content.ContextCompat
import com.example.practicafinal.util.filtrarPublicaciones
import androidx.appcompat.app.AlertDialog
import com.example.practicafinal.util.ejecutarSeguro
import com.example.practicafinal.util.actualizarUi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.practicafinal.adaptadores.AdaptadorPerdidas
import com.example.practicafinal.controlador.ControladorPublicaciones
import com.example.practicafinal.modelo.Avistamiento
import com.example.practicafinal.modelo.Publicacion
import com.example.practicafinal.util.fechaRelativa
import java.util.concurrent.Executors

class MascotasPerdidasActivity : AppCompatActivity() {

    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var rvPerdidas: RecyclerView
    private lateinit var tvVacio: TextView
    private lateinit var etBuscar: EditText
    private var publicaciones: List<Publicacion> = emptyList()
    private var avistPorId: Map<Long, List<Avistamiento>> = emptyMap()
    private var filtroEspecie: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mascotas_perdidas)

        findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
            .setNavigationOnClickListener { finish() }

        rvPerdidas = findViewById(R.id.rv_perdidas)
        tvVacio = findViewById(R.id.tv_vacio)
        rvPerdidas.layoutManager = LinearLayoutManager(this)

        findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_publicar)
            .setOnClickListener {
                startActivity(
                    Intent(this, CrearPublicacionActivity::class.java).apply {
                        putExtra(CrearPublicacionActivity.EXTRA_TIPO, "Perdida")
                    }
                )
            }

        etBuscar = findViewById(R.id.et_buscar)
        etBuscar.doAfterTextChanged { mostrarPerdidas() }
        listOf(R.id.chip_todos to null, R.id.chip_perros to "Perro", R.id.chip_gatos to "Gato")
            .forEach { (id, especie) ->
                findViewById<TextView>(id).setOnClickListener {
                    filtroEspecie = especie
                    mostrarPerdidas()
                }
            }
    }

    private fun cargarPerdidas() {
        ejecutarSeguro(executor) {
            val lista = ControladorPublicaciones.obtenerPerdidas(this)
            val avistamientos = ControladorPublicaciones.obtenerAvistamientos(this).groupBy { it.publicacionId }
            actualizarUi {
                publicaciones = lista
                avistPorId = avistamientos
                mostrarPerdidas()
            }
        }
    }

    private fun mostrarPerdidas() {
        val filtradas = filtrarPublicaciones(publicaciones, etBuscar.text.toString(), filtroEspecie)
        listOf(R.id.chip_todos to null, R.id.chip_perros to "Perro", R.id.chip_gatos to "Gato")
            .forEach { (id, especie) ->
                val chip = findViewById<TextView>(id)
                val activo = filtroEspecie == especie
                chip.setBackgroundResource(if (activo) R.drawable.bg_chip_lista_on else R.drawable.bg_chip_lista)
                chip.setTextColor(ContextCompat.getColor(this, if (activo) R.color.tinta else R.color.texto_suave))
                chip.isSelected = activo
            }
        findViewById<TextView>(R.id.chip_todos).text = getString(R.string.all_pets_count, publicaciones.size)
        tvVacio.setText(if (publicaciones.isEmpty()) R.string.empty_lost_pets else R.string.no_pet_results)
        tvVacio.visibility = if (filtradas.isEmpty()) View.VISIBLE else View.GONE
        rvPerdidas.adapter = AdaptadorPerdidas(
            filtradas, avistPorId,
            onSighting = { p -> reportarAvistamiento(p) },
            onVerAvistamientos = { p -> mostrarAvistamientos(p, avistPorId) },
            onVerMapa = { p -> verEnMapa(p) },
            onDetalles = { p -> verDetalles(p) }
        )
    }

    private fun reportarAvistamiento(publicacion: Publicacion) {
        startActivity(
            Intent(this, ReportarAvistamientoActivity::class.java).apply {
                putExtra(ReportarAvistamientoActivity.EXTRA_PUBLICACION_ID, publicacion.id)
                putExtra(ReportarAvistamientoActivity.EXTRA_NOMBRE, publicacion.nombre)
            }
        )
    }

    private fun verEnMapa(publicacion: Publicacion) {
        startActivity(Intent(this, MainActivity::class.java).apply {
            putExtra("centrar_lat", publicacion.latitud)
            putExtra("centrar_lng", publicacion.longitud)
            putExtra("mostrar_alerta", publicacion.id)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        })
    }

    private fun verDetalles(publicacion: Publicacion) {
        startActivity(
            Intent(this, DetallePublicacionActivity::class.java).apply {
                putExtra(DetallePublicacionActivity.EXTRA_PUBLICACION_ID, publicacion.id)
            }
        )
    }

    private fun mostrarAvistamientos(
        publicacion: Publicacion,
        avistPorId: Map<Long, List<Avistamiento>>
    ) {
        val avistamientos = avistPorId[publicacion.id] ?: emptyList()
        val dialogView = LayoutInflater.from(this)
            .inflate(R.layout.dialog_avistamientos, null)
        val rv = dialogView.findViewById<RecyclerView>(R.id.rv_avistamientos)
        val tvVacioAvist = dialogView.findViewById<TextView>(R.id.tv_vacio_avist)

        rv.layoutManager = LinearLayoutManager(this)
        if (avistamientos.isEmpty()) {
            tvVacioAvist.visibility = View.VISIBLE
            rv.visibility = View.GONE
        } else {
            tvVacioAvist.visibility = View.GONE
            rv.visibility = View.VISIBLE
            rv.adapter = object : RecyclerView.Adapter<AvistViewHolder>() {
                override fun onCreateViewHolder(p: ViewGroup, viewType: Int) = AvistViewHolder(
                    LayoutInflater.from(p.context).inflate(R.layout.item_avistamiento_row, p, false)
                )

                override fun onBindViewHolder(h: AvistViewHolder, pos: Int) {
                    val a = avistamientos[pos]
                    h.tvFecha.text = "👀 ${fechaRelativa(a.fecha)}"
                    h.tvDesc.text = a.descripcion.ifEmpty { "Reportado por la comunidad" }
                }

                override fun getItemCount() = avistamientos.size
            }
        }

        AlertDialog.Builder(this)
            .setTitle("Avistamientos de ${publicacion.nombre}")
            .setView(dialogView)
            .setPositiveButton("Cerrar", null)
            .show()
            .also { com.example.practicafinal.util.Dialogos.aplicar(this, it) }
    }

    private class AvistViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvFecha: TextView = view.findViewById(R.id.tv_fecha)
        val tvDesc: TextView = view.findViewById(R.id.tv_desc)
    }

    override fun onResume() {
        super.onResume()
        cargarPerdidas()
    }

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdown()
    }
}
