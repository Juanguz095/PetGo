package com.example.practicafinal

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.practicafinal.adaptadores.AdaptadorAdopciones
import com.example.practicafinal.controlador.ControladorPublicaciones
import com.example.practicafinal.modelo.Publicacion
import com.example.practicafinal.session.FavoritosManager
import org.osmdroid.util.GeoPoint
import java.util.concurrent.Executors

class AdopcionesActivity : AppCompatActivity() {

    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var etBuscar: EditText
    private lateinit var rvAnimales: androidx.recyclerview.widget.RecyclerView
    private lateinit var tvFavCount: TextView
    private lateinit var tvOpciones: TextView

    private var publicaciones: List<Publicacion> = emptyList()
    private var filtroEspecie: String? = null
    private var ordenActual = "recientes"
    private var userLat: Double? = null
    private var userLng: Double? = null

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
        chipRecientes = findViewById(R.id.chip_orden_recientes)
        chipCercanos = findViewById(R.id.chip_orden_cercanos)
        chipNombre = findViewById(R.id.chip_orden_nombre)
        rvAnimales.layoutManager = LinearLayoutManager(this)

        findViewById<View>(R.id.btn_favoritos).setOnClickListener {
            startActivity(Intent(this, MisFavoritosActivity::class.java))
        }

        findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fab_agregar)
            .setOnClickListener {
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

        fun seleccionarChip(chip: TextView, tipo: String?) {
            listOf(chipTodos to null, chipPerros to "Perro", chipGatos to "Gato").forEach { (c, t) ->
                val activo = t == tipo
                c.background = if (activo) ContextCompat.getDrawable(this, R.drawable.bg_chip_selected)
                else ContextCompat.getDrawable(this, R.drawable.bg_chip)
                c.setTextColor(
                    if (activo) ContextCompat.getColor(this, android.R.color.black)
                    else ContextCompat.getColor(this, android.R.color.darker_gray)
                )
            }
            filtroEspecie = tipo
            aplicarFiltros()
        }

        chipTodos.setOnClickListener { seleccionarChip(chipTodos, null) }
        chipPerros.setOnClickListener { seleccionarChip(chipPerros, "Perro") }
        chipGatos.setOnClickListener { seleccionarChip(chipGatos, "Gato") }

        chipRecientes.setOnClickListener { seleccionarOrden("recientes") }
        chipCercanos.setOnClickListener { seleccionarOrden("cercanos") }
        chipNombre.setOnClickListener { seleccionarOrden("nombre") }

        obtenerUbicacion()
        cargarAdopciones()
    }

    private fun seleccionarOrden(orden: String) {
        ordenActual = orden
        listOf(chipRecientes to "recientes", chipCercanos to "cercanos", chipNombre to "nombre").forEach { (chip, o) ->
            val activo = o == orden
            chip.background = if (activo) ContextCompat.getDrawable(this, R.drawable.bg_chip_selected)
            else ContextCompat.getDrawable(this, R.drawable.bg_chip)
            chip.setTextColor(
                if (activo) ContextCompat.getColor(this, android.R.color.black)
                else ContextCompat.getColor(this, android.R.color.darker_gray)
            )
        }
        aplicarFiltros()
    }

    private fun obtenerUbicacion() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) return
        val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        try {
            val loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            if (loc != null) {
                userLat = loc.latitude
                userLng = loc.longitude
            }
        } catch (_: SecurityException) {}
    }

    private fun cargarAdopciones() {
        executor.execute {
            publicaciones = ControladorPublicaciones.obtenerAdopciones(this)
            runOnUiThread {
                aplicarFiltros()
                actualizarContadorFav()
                val n = publicaciones.size
                tvOpciones.text = if (n == 1) "1 opcion para adoptar" else "$n opciones para adoptar"
            }
        }
    }

    private fun aplicarFiltros() {
        val texto = etBuscar.text.toString().trim().lowercase()
        var filtradas = publicaciones.filter { p ->
            val coincideNombre = p.nombre.lowercase().contains(texto) ||
                    p.descripcion.lowercase().contains(texto)
            val coincideEspecie = filtroEspecie == null ||
                    p.especie?.equals(filtroEspecie, ignoreCase = true) == true
            coincideNombre && coincideEspecie
        }

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

        rvAnimales.adapter = AdaptadorAdopciones(filtradas, userLat, userLng) { publicacion ->
            startActivity(
                Intent(this, DetalleAdopcionActivity::class.java).apply {
                    putExtra(DetalleAdopcionActivity.EXTRA_PUBLICACION_ID, publicacion.id)
                }
            )
        }
    }

    private fun actualizarContadorFav() {
        val idsFav = FavoritosManager.obtenerIds(this)
        val count = publicaciones.count { idsFav.contains(it.id.toString()) }
        tvFavCount.text = count.toString()
    }

    override fun onResume() {
        super.onResume()
        cargarAdopciones()
    }

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdown()
    }
}
