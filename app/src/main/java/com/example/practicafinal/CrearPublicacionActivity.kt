package com.example.practicafinal

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doAfterTextChanged
import com.example.practicafinal.util.ejecutarSeguro
import com.example.practicafinal.util.actualizarUi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.practicafinal.controlador.ControladorPublicaciones
import com.example.practicafinal.controlador.ControladorDenuncias
import com.example.practicafinal.session.SesionManager
import com.example.practicafinal.util.decodificarImagen
import com.example.practicafinal.util.BorradoresLocales
import com.example.practicafinal.util.MonitorConectividad
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.XYTileSource
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import java.util.concurrent.Executors
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class CrearPublicacionActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_LAT = "extra_lat"
        const val EXTRA_LNG = "extra_lng"
        const val EXTRA_TIPO = "extra_tipo"
        const val EXTRA_EDIT_ID = "extra_edit_publicacion_id"
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
    private lateinit var etContactoWhatsapp: EditText
    private lateinit var etRecompensa: EditText
    private lateinit var etColor: EditText
    private lateinit var etTamano: EditText
    private lateinit var etSenas: EditText
    private lateinit var tvError: TextView
    private lateinit var tvEstadoRed: TextView
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
    private lateinit var sectionContacto: View
    private lateinit var sectionDatosPerdida: View
    private lateinit var sectionIdentificativos: View
    private lateinit var etMotivoCustom: EditText
    private var editandoId: Long? = null
    private var motivoDenuncia = "Maltrato"
    private var esOtroMotivo = false
    private var fechaPerdidaSeleccionada: Long = System.currentTimeMillis()
    private lateinit var monitorConectividad: MonitorConectividad
    private var formularioListo = false
    private var borradorActivo = false

    private val pickerGaleria = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val imagen = decodificarImagen(this, uri.toString(), 4)
            if (imagen == null) {
                fotoUri = null
                imgFoto.setImageDrawable(null)
                tvFotoHint.visibility = View.VISIBLE
                Toast.makeText(this, "No se pudo leer esa imagen. Puedes publicar sin foto.", Toast.LENGTH_LONG).show()
            } else {
                fotoUri = uri.toString()
                imgFoto.setImageBitmap(imagen)
                tvFotoHint.visibility = View.GONE
                borradorActivo = true
                guardarBorrador()
            }
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
        etContactoWhatsapp = findViewById(R.id.et_contacto_whatsapp)
        etRecompensa = findViewById(R.id.et_recompensa)
        etColor = findViewById(R.id.et_color_mascota)
        etTamano = findViewById(R.id.et_tamano_mascota)
        etSenas = findViewById(R.id.et_senas_mascota)
        tvError = findViewById(R.id.tv_error)
        tvEstadoRed = findViewById(R.id.tv_estado_red)
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
        sectionContacto = findViewById(R.id.section_contacto)
        sectionDatosPerdida = findViewById(R.id.section_datos_perdida)
        sectionIdentificativos = findViewById(R.id.section_identificativos)
        etMotivoCustom = findViewById(R.id.et_motivo_custom)

        
        findViewById<View>(R.id.contenedor_foto).setOnClickListener {
            pickerGaleria.launch("image/*")
        }
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_fecha_perdida).setOnClickListener {
            elegirFechaPerdida()
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
                    chip.setTextColor(ContextCompat.getColor(this, if (activo) R.color.tinta else R.color.texto_suave))
                }
                etMotivoCustom.visibility = if (esOtroMotivo) View.VISIBLE else View.GONE
                if (esOtroMotivo) etMotivoCustom.requestFocus()
                if (formularioListo) {
                    borradorActivo = true
                    guardarBorrador()
                }
            }
        }

        configurarMapa()

        
        val tipoInicial = intent.getStringExtra(EXTRA_TIPO)
        seleccionarTipo(tipoInicial ?: tipoSeleccionado)

        findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_publicar)
            .setOnClickListener { publicar() }

        editandoId = intent.getLongExtra(EXTRA_EDIT_ID, -1L).takeIf { it > 0L }
        monitorConectividad = MonitorConectividad(this) { conectado ->
            tvEstadoRed.visibility = if (conectado) View.GONE else View.VISIBLE
            tvEstadoRed.announceForAccessibility(if (conectado) "Conexión restablecida" else "Sin conexión a internet")
        }
        if (editandoId != null) {
            cargarEdicion(editandoId!!)
        } else {
            formularioListo = true
            instalarAutoGuardado()
            ofrecerBorrador()
        }
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
        map.setMultiTouchControls(true)
        map.setBuiltInZoomControls(false)
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
                    borradorActivo = true
                    guardarBorrador()
                    return true
                }

                override fun longPressHelper(p: GeoPoint): Boolean = false
            })
        )
    }

    private fun obtenerUbicacionActual(): GeoPoint? =
        com.example.practicafinal.util.ultimaUbicacion(this)?.let { GeoPoint(it.latitude, it.longitude) }

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
                if (activo) ContextCompat.getColor(this, R.color.tinta)
                else ContextCompat.getColor(this, R.color.texto_suave)
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
        sectionContacto.visibility = if (tipo == "Denuncia") View.GONE else View.VISIBLE
        sectionDatosPerdida.visibility = if (tipo == "Perdida") View.VISIBLE else View.GONE
        sectionIdentificativos.visibility = if (tipo == "Denuncia") View.GONE else View.VISIBLE
        if (formularioListo) {
            borradorActivo = true
            guardarBorrador()
        }
    }

    private fun elegirFechaPerdida() {
        val calendario = Calendar.getInstance().apply { timeInMillis = fechaPerdidaSeleccionada }
        android.app.DatePickerDialog(
            this,
            { _, year, month, day ->
                calendario.set(Calendar.YEAR, year)
                calendario.set(Calendar.MONTH, month)
                calendario.set(Calendar.DAY_OF_MONTH, day)
                android.app.TimePickerDialog(this, { _, hour, minute ->
                    calendario.set(Calendar.HOUR_OF_DAY, hour)
                    calendario.set(Calendar.MINUTE, minute)
                    calendario.set(Calendar.SECOND, 0)
                    calendario.set(Calendar.MILLISECOND, 0)
                    val elegido = calendario.timeInMillis
                    if (elegido > System.currentTimeMillis()) {
                        Toast.makeText(this, "La fecha no puede estar en el futuro", Toast.LENGTH_LONG).show()
                        return@TimePickerDialog
                    }
                    fechaPerdidaSeleccionada = elegido
                    findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_fecha_perdida).text =
                        "Última vez: ${SimpleDateFormat("dd/MM/yyyy, HH:mm", Locale.forLanguageTag("es-PE")).format(Date(elegido))}"
                    borradorActivo = true
                    guardarBorrador()
                }, calendario.get(Calendar.HOUR_OF_DAY), calendario.get(Calendar.MINUTE), true).show()
            }, calendario.get(Calendar.YEAR), calendario.get(Calendar.MONTH), calendario.get(Calendar.DAY_OF_MONTH)
        ).apply { datePicker.maxDate = System.currentTimeMillis() }.show()
    }

    private fun seleccionarEspecie(especie: String) {
        especieSeleccionada = especie
        if (formularioListo) borradorActivo = true
        listOf(chipPerro to "Perro", chipGato to "Gato").forEach { (chip, e) ->
            val activo = e == especie
            chip.background = if (activo) {
                ContextCompat.getDrawable(this, R.drawable.bg_chip_selected)
            } else {
                ContextCompat.getDrawable(this, R.drawable.bg_chip)
            }
            (chip as TextView).setTextColor(
                if (activo) ContextCompat.getColor(this, R.color.tinta)
                else ContextCompat.getColor(this, R.color.texto_suave)
            )
        }
    }

    private fun cargarEdicion(id: Long) {
        findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar).title = "Editar publicación"
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_publicar).text = "Guardar cambios"
        findViewById<View>(R.id.chip_denuncia).visibility = View.GONE
        ejecutarSeguro(executor) {
            val publicacion = ControladorPublicaciones.obtenerPorId(this, id)
            val coordenadasExactas = runCatching {
                ControladorPublicaciones.obtenerCoordenadasPublicacion(this, id)
            }.getOrNull()
            actualizarUi {
                if (publicacion == null || publicacion.estado != "Activa") {
                    Toast.makeText(this, "La publicación ya no se puede editar", Toast.LENGTH_LONG).show()
                    finish()
                    return@actualizarUi
                }
                seleccionarTipo(publicacion.tipo)
                etNombre.setText(publicacion.nombre)
                etDescripcion.setText(publicacion.descripcion)
                etUltimoLugar.setText(publicacion.ultimoLugar.orEmpty())
                etContactoWhatsapp.setText(publicacion.contactoWhatsapp.orEmpty().let {
                    if (it.startsWith("51") && it.length == 11) it.drop(2) else it
                })
                etRecompensa.setText(publicacion.recompensa.orEmpty())
                etColor.setText(publicacion.color.orEmpty())
                etTamano.setText(publicacion.tamano.orEmpty())
                etSenas.setText(publicacion.senasParticulares.orEmpty())
                fechaPerdidaSeleccionada = publicacion.fechaPerdida ?: publicacion.fechaCreacion
                findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_fecha_perdida).text =
                    "Última vez: ${SimpleDateFormat("dd/MM/yyyy, HH:mm", Locale.forLanguageTag("es-PE")).format(Date(fechaPerdidaSeleccionada))}"
                publicacion.especie?.let(::seleccionarEspecie)
                fotoUri = publicacion.foto
                if (!publicacion.foto.isNullOrBlank()) {
                    com.example.practicafinal.util.cargarImagen(imgFoto, publicacion.foto)
                    tvFotoHint.visibility = View.GONE
                }
                val punto = coordenadasExactas?.let { GeoPoint(it.first, it.second) }
                    ?: GeoPoint(publicacion.latitud, publicacion.longitud)
                puntoSeleccionado = punto
                pinSeleccion.position = punto
                map.controller.setCenter(punto)
                map.invalidate()
            }
        }
    }

    private fun publicar() {
        val nombre = etNombre.text.toString().trim()
        val descripcion = etDescripcion.text.toString().trim()
        val especie = especieSeleccionada
        val tipo = tipoSeleccionado
        val foto = fotoUri
        val ultimoLugar = etUltimoLugar.text.toString().trim().ifEmpty { null }
        val punto = puntoSeleccionado
        val contactoEscrito = etContactoWhatsapp.text.toString().filter(Char::isDigit)
        if (contactoEscrito.isNotEmpty() && !(contactoEscrito.length == 9 || (contactoEscrito.length == 11 && contactoEscrito.startsWith("51")))) {
            mostrarError("Ingresa un celular de 9 dígitos o con prefijo 51")
            return
        }
        val contactoWhatsapp = when {
            contactoEscrito.length == 9 -> "51$contactoEscrito"
            contactoEscrito.isNotEmpty() -> contactoEscrito
            else -> null
        }
        val fechaPerdida = fechaPerdidaSeleccionada.takeIf { tipo == "Perdida" }
        val recompensa = etRecompensa.text.toString().trim().takeIf { tipo == "Perdida" && it.isNotEmpty() }
        val color = etColor.text.toString().trim().ifEmpty { null }
        val tamano = etTamano.text.toString().trim().ifEmpty { null }
        val senas = etSenas.text.toString().trim().ifEmpty { null }

        if (tipoSeleccionado == "Denuncia") {
            if (descripcion.isBlank()) {
                mostrarError("Escribe una descripción")
                return
            }
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
            guardarBorrador()
            if (!monitorConectividad.hayInternet()) {
                mostrarError("Sin conexión. El borrador queda guardado en este teléfono; vuelve a intentarlo cuando tengas internet.")
                return
            }
            button.isEnabled = false
            ejecutarSeguro(executor) {
                try {
                    ControladorDenuncias.insertarDenuncia(
                        this, motivoFinal, descripcion,
                        foto, punto.latitude, punto.longitude
                    )
                    actualizarUi {
                        borradorActivo = false
                        BorradoresLocales.eliminar(this, "publicacion")
                        Toast.makeText(this, "Denuncia enviada", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                } catch (error: Exception) {
                    actualizarUi {
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
        guardarBorrador()
        if (!monitorConectividad.hayInternet()) {
            mostrarError("Sin conexión. El borrador queda guardado en este teléfono; vuelve a intentarlo cuando tengas internet.")
            return
        }
        button.isEnabled = false
        ejecutarSeguro(executor) {
            try {
                val usuarioId = SesionManager.obtenerUsuarioId(this)
                if (editandoId != null) {
                    ControladorPublicaciones.actualizar(
                        this, editandoId!!, tipo, nombre, descripcion, foto, ultimoLugar,
                        especie, punto.latitude, punto.longitude, contactoWhatsapp,
                        fechaPerdida, recompensa, color, tamano, senas
                    )
                } else {
                    ControladorPublicaciones.publicar(
                        this, usuarioId, tipo, nombre, descripcion,
                        foto, ultimoLugar, especie, punto.latitude, punto.longitude, contactoWhatsapp,
                        fechaPerdida, recompensa, color, tamano, senas
                    )
                }
                actualizarUi {
                    borradorActivo = false
                    BorradoresLocales.eliminar(this, "publicacion")
                    val msg = if (editandoId != null) "Publicación actualizada" else "Publicación creada"
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (error: Exception) {
                actualizarUi {
                    button.isEnabled = true
                    mostrarError(error.message ?: "No se pudo guardar la publicacion")
                }
            }
        }
    }
    private fun mostrarError(mensaje: String) {
        tvError.text = mensaje
        tvError.visibility = View.VISIBLE
        tvError.announceForAccessibility(mensaje)
        findViewById<android.widget.ScrollView>(R.id.scroll_formulario).post {
            findViewById<android.widget.ScrollView>(R.id.scroll_formulario)
                .smoothScrollTo(0, tvError.top)
        }
    }

    private fun instalarAutoGuardado() {
        listOf(etNombre, etUltimoLugar, etDescripcion, etContactoWhatsapp, etRecompensa,
            etColor, etTamano, etSenas, etMotivoCustom).forEach { campo ->
            campo.doAfterTextChanged {
                borradorActivo = true
                guardarBorrador()
            }
        }
    }

    private fun ofrecerBorrador() {
        val datos = BorradoresLocales.obtener(this, "publicacion") ?: return
        AlertDialog.Builder(this)
            .setTitle("Tienes un borrador")
            .setMessage("¿Quieres continuar donde lo dejaste?")
            .setPositiveButton("Continuar") { _, _ -> restaurarBorrador(datos) }
            .setNegativeButton("Empezar de nuevo") { _, _ -> BorradoresLocales.eliminar(this, "publicacion") }
            .setCancelable(false)
            .show()
            .also { com.example.practicafinal.util.Dialogos.aplicar(this, it) }
    }

    private fun guardarBorrador() {
        if (!formularioListo || !borradorActivo || editandoId != null) return
        val punto = puntoSeleccionado
        val datos = JSONObject()
            .put("tipo", tipoSeleccionado)
            .put("especie", especieSeleccionada)
            .put("nombre", etNombre.text.toString())
            .put("descripcion", etDescripcion.text.toString())
            .put("ultimoLugar", etUltimoLugar.text.toString())
            .put("contacto", etContactoWhatsapp.text.toString())
            .put("recompensa", etRecompensa.text.toString())
            .put("color", etColor.text.toString())
            .put("tamano", etTamano.text.toString())
            .put("senas", etSenas.text.toString())
            .put("motivo", motivoDenuncia)
            .put("otroMotivo", esOtroMotivo)
            .put("motivoCustom", etMotivoCustom.text.toString())
            .put("fecha", fechaPerdidaSeleccionada)
            .put("lat", punto?.latitude ?: JSONObject.NULL)
            .put("lng", punto?.longitude ?: JSONObject.NULL)
            .put("foto", fotoUri ?: "")
        BorradoresLocales.guardar(this, "publicacion", datos)
    }

    private fun restaurarBorrador(datos: JSONObject) {
        formularioListo = false
        seleccionarTipo(datos.optString("tipo", tipoSeleccionado))
        seleccionarEspecie(datos.optString("especie", "Perro"))
        etNombre.setText(datos.optString("nombre"))
        etDescripcion.setText(datos.optString("descripcion"))
        etUltimoLugar.setText(datos.optString("ultimoLugar"))
        etContactoWhatsapp.setText(datos.optString("contacto"))
        etRecompensa.setText(datos.optString("recompensa"))
        etColor.setText(datos.optString("color"))
        etTamano.setText(datos.optString("tamano"))
        etSenas.setText(datos.optString("senas"))
        motivoDenuncia = datos.optString("motivo", "Maltrato")
        esOtroMotivo = datos.optBoolean("otroMotivo", false)
        etMotivoCustom.setText(datos.optString("motivoCustom"))
        etMotivoCustom.visibility = if (esOtroMotivo) View.VISIBLE else View.GONE
        fechaPerdidaSeleccionada = datos.optLong("fecha", System.currentTimeMillis())
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_fecha_perdida).text =
            "Última vez: ${SimpleDateFormat("dd/MM/yyyy, HH:mm", Locale.forLanguageTag("es-PE")).format(Date(fechaPerdidaSeleccionada))}"
        val lat = datos.optDouble("lat", Double.NaN)
        val lng = datos.optDouble("lng", Double.NaN)
        if (lat.isFinite() && lng.isFinite()) {
            val punto = GeoPoint(lat, lng)
            puntoSeleccionado = punto
            pinSeleccion.position = punto
            map.controller.setCenter(punto)
        }
        fotoUri = datos.optString("foto").takeIf(String::isNotBlank)
        if (!fotoUri.isNullOrBlank()) {
            val bitmap = decodificarImagen(this, fotoUri!!, 4)
            if (bitmap != null) {
                imgFoto.setImageBitmap(bitmap)
                tvFotoHint.visibility = View.GONE
            } else {
                fotoUri = null
                tvFotoHint.visibility = View.VISIBLE
                Toast.makeText(this, "El acceso a la foto anterior venció; selecciónala otra vez.", Toast.LENGTH_LONG).show()
            }
        }
        map.invalidate()
        formularioListo = true
        borradorActivo = true
        guardarBorrador()
    }

    override fun onResume() {
        super.onResume()
        if (::monitorConectividad.isInitialized) monitorConectividad.iniciar()
        if (::map.isInitialized) map.onResume()
    }

    override fun onPause() {
        guardarBorrador()
        super.onPause()
        if (::map.isInitialized) map.onPause()
    }

    override fun onStop() {
        if (::monitorConectividad.isInitialized) monitorConectividad.detener()
        super.onStop()
    }

    override fun onDestroy() {
        if (::map.isInitialized) map.onDetach()
        super.onDestroy()
        executor.shutdown()
    }
}
