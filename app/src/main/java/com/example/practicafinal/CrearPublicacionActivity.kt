package com.example.practicafinal

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.practicafinal.controlador.ControladorPublicaciones
import com.example.practicafinal.controlador.ControladorDenuncias
import com.example.practicafinal.session.SesionManager
import com.example.practicafinal.util.decodificarImagen
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import java.util.concurrent.Executors

class CrearPublicacionActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_LAT = "extra_lat"
        const val EXTRA_LNG = "extra_lng"
        const val EXTRA_TIPO = "extra_tipo"
    }

    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var map: MapView
    private lateinit var pinSeleccion: Marker
    private var puntoSeleccionado: GeoPoint? = null
    private var tipoSeleccionado = "Perdida"
    private var especieSeleccionada = "Perro"
    private var fotoUri: String? = null

    private lateinit var etNombre: EditText
    private lateinit var etUltimoLugar: EditText
    private lateinit var etDescripcion: EditText
    private lateinit var tvError: TextView
    private lateinit var chipPerdida: View
    private lateinit var chipEncontrada: View
    private lateinit var chipAdopcion: View
    private lateinit var chipDenuncia: View
    private lateinit var chipPerro: View
    private lateinit var chipGato: View
    private lateinit var imgFoto: ImageView
    private lateinit var tvFotoHint: TextView
    private lateinit var labelNombre: TextView
    private lateinit var labelEspecie: View
    private lateinit var sectionEspecie: View
    private lateinit var sectionUltimoLugar: View
    private lateinit var sectionMotivosDenuncia: View
    private lateinit var etMotivoCustom: EditText
    private var motivoDenuncia = "Maltrato"
    private var esOtroMotivo = false

    private val pickerGaleria = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            fotoUri = uri.toString()
            imgFoto.setImageBitmap(decodificarImagen(this, uri.toString(), 4))
            tvFotoHint.visibility = View.GONE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Configuration.getInstance().load(
            applicationContext,
            getSharedPreferences("osmdroid", MODE_PRIVATE)
        )
        Configuration.getInstance().userAgentValue = "MapaAlertas/1.0 (juanguz619@gmail.com)"

        setContentView(R.layout.activity_crear_publicacion)

        findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
            .setNavigationOnClickListener { finish() }

        etNombre = findViewById(R.id.et_nombre)
        etUltimoLugar = findViewById(R.id.et_ultimo_lugar)
        etDescripcion = findViewById(R.id.et_descripcion)
        tvError = findViewById(R.id.tv_error)
        chipPerdida = findViewById(R.id.chip_perdida)
        chipEncontrada = findViewById(R.id.chip_encontrada)
        chipAdopcion = findViewById(R.id.chip_adopcion)
        chipDenuncia = findViewById(R.id.chip_denuncia)
        chipPerro = findViewById(R.id.chip_especie_perro)
        chipGato = findViewById(R.id.chip_especie_gato)
        imgFoto = findViewById(R.id.img_foto)
        tvFotoHint = findViewById(R.id.tv_foto_hint)
        labelNombre = findViewById(R.id.label_nombre)
        labelEspecie = findViewById(R.id.label_especie)
        sectionEspecie = findViewById(R.id.section_especie)
        sectionUltimoLugar = findViewById(R.id.section_ultimo_lugar)
        sectionMotivosDenuncia = findViewById(R.id.section_motivos_denuncia)
        etMotivoCustom = findViewById(R.id.et_motivo_custom)

        
        findViewById<View>(R.id.contenedor_foto).setOnClickListener {
            pickerGaleria.launch("image/*")
        }

        chipPerdida.setOnClickListener { seleccionarTipo("Perdida") }
        chipEncontrada.setOnClickListener { seleccionarTipo("Encontrada") }
        chipAdopcion.setOnClickListener { seleccionarTipo("Adopcion") }
        chipDenuncia.setOnClickListener { seleccionarTipo("Denuncia") }
        chipPerro.setOnClickListener { seleccionarEspecie("Perro") }
        chipGato.setOnClickListener { seleccionarEspecie("Gato") }

        val chipMaltrato = findViewById<TextView>(R.id.chip_maltrato)
        val chipAbandono = findViewById<TextView>(R.id.chip_abandono)
        val chipVentaIlegal = findViewById<TextView>(R.id.chip_venta_ilegal)
        val chipMotivoOtro = findViewById<TextView>(R.id.chip_motivo_otro)
        val motivoChips = listOf(chipMaltrato, chipAbandono, chipVentaIlegal, chipMotivoOtro)
        val motivos = listOf("Maltrato", "Abandono", "Venta ilegal", "Otro")
        motivos.forEachIndexed { i, m ->
            motivoChips[i].setOnClickListener {
                esOtroMotivo = (m == "Otro")
                motivoDenuncia = m
                motivoChips.forEachIndexed { j, chip ->
                    val activo = j == i
                    chip.background = ContextCompat.getDrawable(this, if (activo) R.drawable.bg_chip_selected else R.drawable.bg_chip)
                    chip.setTextColor(ContextCompat.getColor(this, if (activo) android.R.color.black else android.R.color.darker_gray))
                }
                etMotivoCustom.visibility = if (esOtroMotivo) View.VISIBLE else View.GONE
                if (esOtroMotivo) etMotivoCustom.requestFocus()
            }
        }

        configurarMapa()

        
        val tipoInicial = intent.getStringExtra(EXTRA_TIPO)
        if (tipoInicial != null) seleccionarTipo(tipoInicial)

        findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_publicar)
            .setOnClickListener { publicar() }
    }

    private fun configurarMapa() {
        map = findViewById(R.id.mapa_crear)
        map.setUseDataConnection(true)
        map.setTileSource(
            XYTileSource(
                "OpenStreetMap", 0, 19, 256, ".png",
                arrayOf(
                    "https://tile.openstreetmap.org/",
                    "https://a.tile.openstreetmap.org/",
                    "https://b.tile.openstreetmap.org/",
                    "https://c.tile.openstreetmap.org/"
                )
            )
        )
        map.setMultiTouchControls(false)
        map.controller.setZoom(18.0)

        
        val lat = intent.getDoubleExtra(EXTRA_LAT, Double.NaN)
        val lng = intent.getDoubleExtra(EXTRA_LNG, Double.NaN)
        val puntoInicial = if (!lat.isNaN() && !lng.isNaN()) GeoPoint(lat, lng) else null

        
        val centro = puntoInicial ?: obtenerUbicacionActual() ?: GeoPoint(-12.0464, -77.0428)
        map.controller.setCenter(centro)

        puntoSeleccionado = centro
        pinSeleccion = Marker(map).apply {
            position = centro
            icon = ContextCompat.getDrawable(this@CrearPublicacionActivity, R.drawable.ic_pin_rojo)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        }
        map.overlays.add(pinSeleccion)

        
        map.overlays.add(
            MapEventsOverlay(object : MapEventsReceiver {
                override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                    puntoSeleccionado = p
                    pinSeleccion.position = p
                    map.invalidate()
                    return true
                }

                override fun longPressHelper(p: GeoPoint): Boolean = false
            })
        )
    }

    private fun obtenerUbicacionActual(): GeoPoint? {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) return null
        val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return try {
            val loc = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            loc?.let { GeoPoint(it.latitude, it.longitude) }
        } catch (_: SecurityException) {
            null
        }
    }

    private fun seleccionarTipo(tipo: String) {
        tipoSeleccionado = tipo
        listOf(
            chipPerdida to "Perdida",
            chipEncontrada to "Encontrada",
            chipAdopcion to "Adopcion",
            chipDenuncia to "Denuncia"
        ).forEach { (chip, t) ->
            val activo = t == tipo
            chip.background = if (activo) {
                ContextCompat.getDrawable(this, R.drawable.bg_chip_selected)
            } else {
                ContextCompat.getDrawable(this, R.drawable.bg_chip)
            }
            (chip as TextView).setTextColor(
                if (activo) ContextCompat.getColor(this, android.R.color.black)
                else ContextCompat.getColor(this, android.R.color.darker_gray)
            )
        }
        if (::pinSeleccion.isInitialized) {
            val icono = when (tipo) {
                "Perdida" -> R.drawable.ic_pin_rojo
                "Encontrada" -> R.drawable.ic_pin_verde
                "Adopcion" -> R.drawable.ic_pin_naranja
                "Denuncia" -> R.drawable.ic_pin_denuncia
                else -> R.drawable.ic_pin_rojo
            }
            pinSeleccion.icon = ContextCompat.getDrawable(this, icono)
            map.invalidate()
        }

        when (tipo) {
            "Denuncia" -> {
                labelNombre.visibility = View.GONE
                etNombre.visibility = View.GONE
                labelEspecie.visibility = View.GONE
                sectionEspecie.visibility = View.GONE
                sectionUltimoLugar.visibility = View.GONE
                sectionMotivosDenuncia.visibility = View.VISIBLE
            }
            "Adopcion" -> {
                labelNombre.visibility = View.VISIBLE
                labelNombre.text = "Nombre de la mascota"
                etNombre.visibility = View.VISIBLE
                etNombre.hint = "Ej. Max, Luna..."
                labelEspecie.visibility = View.VISIBLE
                sectionEspecie.visibility = View.VISIBLE
                sectionUltimoLugar.visibility = View.GONE
                sectionMotivosDenuncia.visibility = View.GONE
            }
            else -> {
                labelNombre.visibility = View.VISIBLE
                labelNombre.text = "Nombre de la mascota"
                etNombre.visibility = View.VISIBLE
                etNombre.hint = "Ej. Max, Luna..."
                labelEspecie.visibility = View.VISIBLE
                sectionEspecie.visibility = View.VISIBLE
                sectionUltimoLugar.visibility = View.VISIBLE
                sectionMotivosDenuncia.visibility = View.GONE
            }
        }
    }

    private fun seleccionarEspecie(especie: String) {
        especieSeleccionada = especie
        listOf(chipPerro to "Perro", chipGato to "Gato").forEach { (chip, e) ->
            val activo = e == especie
            chip.background = if (activo) {
                ContextCompat.getDrawable(this, R.drawable.bg_chip_selected)
            } else {
                ContextCompat.getDrawable(this, R.drawable.bg_chip)
            }
            (chip as TextView).setTextColor(
                if (activo) ContextCompat.getColor(this, android.R.color.black)
                else ContextCompat.getColor(this, android.R.color.darker_gray)
            )
        }
    }

    private fun publicar() {
        val nombre = etNombre.text.toString().trim()
        val descripcion = etDescripcion.text.toString().trim()
        val especie = especieSeleccionada
        val ultimoLugar = etUltimoLugar.text.toString().trim().ifEmpty { null }
        val punto = puntoSeleccionado

        if (tipoSeleccionado == "Denuncia") {
            val motivoFinal = if (esOtroMotivo) etMotivoCustom.text.toString().trim() else motivoDenuncia
            if (motivoFinal.isEmpty()) {
                mostrarError("Selecciona o escribe un motivo")
                return
            }
            if (punto == null) {
                mostrarError("Elige una ubicación en el mapa")
                return
            }
            val button = findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_publicar)
            button.isEnabled = false
            val tieneFoto = !fotoUri.isNullOrBlank()
            executor.execute {
                try {
                    ControladorDenuncias.insertarDenuncia(
                        this, motivoFinal, descripcion,
                        fotoUri, punto.latitude, punto.longitude
                    )
                    runOnUiThread {
                        val msg = if (tieneFoto) "Denuncia enviada (foto subida)" else "Denuncia enviada"
                        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                        finish()
                    }
                } catch (error: Exception) {
                    runOnUiThread {
                        button.isEnabled = true
                        mostrarError(error.message ?: "No se pudo enviar la denuncia")
                    }
                }
            }
            return
        }

        val error = ControladorPublicaciones.validarPublicacion(nombre, descripcion)
        if (error != null) {
            mostrarError(error)
            return
        }
        if (punto == null) {
            mostrarError("Elige una ubicación en el mapa")
            return
        }

        val button = findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_publicar)
        button.isEnabled = false
        val tieneFoto = !fotoUri.isNullOrBlank()
        executor.execute {
            try {
                val usuarioId = SesionManager.obtenerUsuarioId(this)
                ControladorPublicaciones.publicar(
                    this, usuarioId, tipoSeleccionado, nombre, descripcion,
                    fotoUri, ultimoLugar, especie, punto.latitude, punto.longitude
                )
                runOnUiThread {
                    val msg = if (tieneFoto) "Publicacion creada (foto subida)" else "Publicacion creada"
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (error: Exception) {
                runOnUiThread {
                    button.isEnabled = true
                    mostrarError(error.message ?: "No se pudo guardar la publicacion")
                }
            }
        }
    }
    private fun mostrarError(mensaje: String) {
        tvError.text = mensaje
        tvError.visibility = View.VISIBLE
    }

    override fun onResume() {
        super.onResume()
        if (::map.isInitialized) map.onResume()
    }

    override fun onPause() {
        super.onPause()
        if (::map.isInitialized) map.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdown()
    }
}
