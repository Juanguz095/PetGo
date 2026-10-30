package com.example.practicafinal

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.example.practicafinal.util.filtrarPublicaciones
import androidx.appcompat.app.AlertDialog
import com.example.practicafinal.util.Dialogos
import com.example.practicafinal.util.ejecutarSeguro
import com.example.practicafinal.util.actualizarUi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.practicafinal.adaptadores.AdaptadorAdopciones
import com.example.practicafinal.controlador.ControladorPublicaciones
import com.example.practicafinal.modelo.Publicacion
import com.example.practicafinal.session.FavoritosManager
import com.google.firebase.firestore.DocumentSnapshot
import org.osmdroid.util.GeoPoint
import java.util.concurrent.Executors

class AdopcionesActivity : AppCompatActivity() {

    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var etBuscar: EditText
    private lateinit var rvAnimales: androidx.recyclerview.widget.RecyclerView
    private lateinit var tvFavCount: TextView
    private lateinit var tvOpciones: TextView
    private lateinit var tvVacio: TextView

    private var publicaciones: List<Publicacion> = emptyList()
    private var filtroEspecie: String? = null
    private var ordenActual = "recientes"
    private var userLat: Double? = null
    private var userLng: Double? = null
    private var radioKm: Double? = null
    private var cursorPagina: DocumentSnapshot? = null
    private var hayMas = false
    private var cargando = false
    private var primeraPaginaCargada = false
    private var actualizarAlVolver = false
    private lateinit var btnCargarMas: com.google.android.material.button.MaterialButton
    private lateinit var btnRadio: TextView

    private lateinit var chipRecientes: TextView
    private lateinit var chipCercanos: TextView
    private lateinit var chipNombre: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_adopciones)

        findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
            .setNavigationOnClickListener { finish() }

        etBuscar = findViewById(R.id.et_buscar)
        rvAnimales = findViewById(R.id.rv_animales)
        tvFavCount = findViewById(R.id.tv_fav_count)
        tvOpciones = findViewById(R.id.tv_opciones)
        tvVacio = findViewById(R.id.tv_vacio)
        chipRecientes = findViewById(R.id.chip_orden_recientes)
        chipCercanos = findViewById(R.id.chip_orden_cercanos)
        chipNombre = findViewById(R.id.chip_orden_nombre)
        rvAnimales.layoutManager = LinearLayoutManager(this)
        btnCargarMas = findViewById(R.id.btn_cargar_mas)
        btnCargarMas.setOnClickListener { cargarAdopciones(reset = false) }
        btnRadio = findViewById(R.id.btn_radio)
        btnRadio.setOnClickListener { mostrarMenuRadio() }

        findViewById<View>(R.id.btn_favoritos).setOnClickListener {
            startActivity(Intent(this, MisFavoritosActivity::class.java))
        }

        findViewById<com.google.android.material.button.MaterialButton>(R.id.fab_agregar)
            .setOnClickListener {
                actualizarAlVolver = true
                startActivity(Intent(this, CrearPublicacionActivity::class.java).apply {
                    putExtra(CrearPublicacionActivity.EXTRA_TIPO, "Adopcion")
                })
            }

        etBuscar.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = aplicarFiltros()
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        val chipTodos = findViewById<TextView>(R.id.chip_todos)
        val chipPerros = findViewById<TextView>(R.id.chip_perros)
        val chipGatos = findViewById<TextView>(R.id.chip_gatos)

        fun seleccionarChip(tipo: String?) {
            listOf(chipTodos to null, chipPerros to "Perro", chipGatos to "Gato").forEach { (c, t) ->
                val activo = t == tipo
                c.background = if (activo) ContextCompat.getDrawable(this, R.drawable.bg_chip_selected)
                else ContextCompat.getDrawable(this, R.drawable.bg_chip)
                c.setTextColor(
                    if (activo) ContextCompat.getColor(this, R.color.tinta)
                    else ContextCompat.getColor(this, R.color.texto_suave)
                )
            }
            filtroEspecie = tipo
            aplicarFiltros()
        }

        chipTodos.setOnClickListener { seleccionarChip(null) }
        chipPerros.setOnClickListener { seleccionarChip("Perro") }
        chipGatos.setOnClickListener { seleccionarChip("Gato") }

        chipRecientes.setOnClickListener { seleccionarOrden("recientes") }
        chipCercanos.setOnClickListener { seleccionarOrden("cercanos") }
        chipNombre.setOnClickListener { seleccionarOrden("nombre") }

        findViewById<View>(R.id.btn_proceso).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle(R.string.adoption_process_title)
                .setMessage(R.string.adoption_process_body)
                .setPositiveButton(R.string.understood, null)
                .show().also { Dialogos.aplicar(this, it) }
        }
        obtenerUbicacion()
    }

    private fun seleccionarOrden(orden: String) {
        if (orden == "cercanos" && (userLat == null || userLng == null)) {
            Toast.makeText(this, "No hay una ubicación reciente; se mostrarán primero las publicaciones nuevas", Toast.LENGTH_LONG).show()
            seleccionarOrden("recientes")
            return
        }
        ordenActual = orden
        listOf(chipRecientes to "recientes", chipCercanos to "cercanos", chipNombre to "nombre").forEach { (chip, o) ->
            val activo = o == orden
            chip.background = if (activo) ContextCompat.getDrawable(this, R.drawable.bg_chip_selected)
            else ContextCompat.getDrawable(this, R.drawable.bg_chip)
            chip.setTextColor(
                if (activo) ContextCompat.getColor(this, R.color.tinta)
                else ContextCompat.getColor(this, R.color.texto_suave)
            )
        }
        aplicarFiltros()
    }

    private fun mostrarMenuRadio() {
        if (userLat == null || userLng == null) {
            Toast.makeText(this, "Necesitas una ubicación reciente para filtrar por distancia", Toast.LENGTH_LONG).show()
            return
        }
        androidx.appcompat.widget.PopupMenu(this, btnRadio).apply {
            listOf("Todas las distancias" to null, "Hasta 5 km" to 5.0, "Hasta 10 km" to 10.0, "Hasta 25 km" to 25.0)
                .forEach { (label, value) ->
                    menu.add(label).setOnMenuItemClickListener {
                        radioKm = value
                        btnRadio.text = if (value == null) "Radio: todas" else "Radio: ${value.toInt()} km"
                        aplicarFiltros()
                        true
                    }
                }
            show()
        }
    }

    private fun obtenerUbicacion() {
        com.example.practicafinal.util.ultimaUbicacion(this)?.let {
            userLat = it.latitude
            userLng = it.longitude
        }
    }

    private fun cargarAdopciones(reset: Boolean = true) {
        if (cargando) return
        if (!reset && !hayMas) return
        if (reset) {
            cursorPagina = null
            publicaciones = emptyList()
        }
        cargando = true
        btnCargarMas.isEnabled = false
        ejecutarSeguro(executor) {
            try {
                val pagina = ControladorPublicaciones.obtenerPagina(this, "Adopcion", cursorPagina)
                actualizarUi {
                    cursorPagina = pagina.cursor
                    hayMas = pagina.hayMas
                    primeraPaginaCargada = true
                    cargando = false
                    btnCargarMas.isEnabled = true
                    btnCargarMas.visibility = if (hayMas) View.VISIBLE else View.GONE
                    publicaciones = (if (reset) emptyList() else publicaciones) +
                        pagina.items.filter { it.estado == "Activa" }
                    aplicarFiltros(preserveScroll = !reset)
                    actualizarContadorFav()
                }
            } catch (error: Exception) {
                actualizarUi { cargando = false; btnCargarMas.isEnabled = true }
                throw error
            }
        }
    }

    private fun aplicarFiltros(preserveScroll: Boolean = false) {
        var filtradas = filtrarPublicaciones(publicaciones, etBuscar.text.toString(), filtroEspecie)
        radioKm?.let { radio ->
            val origen = userLat?.let { lat -> userLng?.let { lng -> GeoPoint(lat, lng) } }
            if (origen != null) {
                filtradas = filtradas.filter {
                    GeoPoint(it.latitud, it.longitud).distanceToAsDouble(origen) <= radio * 1000.0
                }
            }
        }
        val n = filtradas.size
        tvOpciones.text = resources.getQuantityString(R.plurals.adoption_options, n, n, publicaciones.size)
        tvVacio.setText(
            if (filtradas.isEmpty() && publicaciones.isNotEmpty() && hayMas)
                R.string.search_more_loaded
            else R.string.no_adoption_results
        )
        tvVacio.visibility = if (filtradas.isEmpty()) View.VISIBLE else View.GONE

        filtradas = when (ordenActual) {
            "cercanos" -> {
                if (userLat != null && userLng != null) {
                    filtradas.sortedBy { p ->
                        GeoPoint(p.latitud, p.longitud).distanceToAsDouble(GeoPoint(userLat!!, userLng!!))
                    }
                } else {
                    filtradas.sortedByDescending { it.fechaCreacion }
                }
            }
            "nombre" -> filtradas.sortedBy { it.nombre.lowercase() }
            else -> filtradas.sortedByDescending { it.fechaCreacion }
        }

        val layoutManager = rvAnimales.layoutManager as? LinearLayoutManager
        val firstVisible = layoutManager?.findFirstVisibleItemPosition() ?: 0
        val firstOffset = rvAnimales.getChildAt(0)?.top ?: 0
        rvAnimales.adapter = AdaptadorAdopciones(filtradas, userLat, userLng) { publicacion ->
            startActivity(
                Intent(this, DetalleAdopcionActivity::class.java).apply {
                    putExtra(DetalleAdopcionActivity.EXTRA_PUBLICACION_ID, publicacion.id)
                }
            )
        }
        if (preserveScroll && firstVisible >= 0) {
            rvAnimales.post { layoutManager?.scrollToPositionWithOffset(firstVisible, firstOffset) }
        } else {
            rvAnimales.scrollToPosition(0)
        }
    }

    private fun actualizarContadorFav() {
        val idsFav = FavoritosManager.obtenerIds(this)
        val count = publicaciones.count { idsFav.contains(it.id.toString()) }
        tvFavCount.text = count.toString()
    }

    override fun onResume() {
        super.onResume()
        if (!primeraPaginaCargada || actualizarAlVolver) {
            actualizarAlVolver = false
            cargarAdopciones()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdown()
    }
}
