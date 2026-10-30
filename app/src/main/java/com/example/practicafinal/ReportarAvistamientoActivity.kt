package com.example.practicafinal

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doAfterTextChanged
import com.example.practicafinal.util.ejecutarSeguro
import com.example.practicafinal.util.actualizarUi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.practicafinal.controlador.ControladorPublicaciones
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

class ReportarAvistamientoActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PUBLICACION_ID = "extra_publicacion_id"
        const val EXTRA_NOMBRE = "extra_nombre"
        const val EXTRA_ULTIMA_LATITUD = "extra_ultima_latitud"
        const val EXTRA_ULTIMA_LONGITUD = "extra_ultima_longitud"
        const val EXTRA_ESPECIE = "extra_especie"
    }

    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var map: MapView
    private lateinit var pinSeleccion: Marker
    private var puntoSeleccionado: GeoPoint? = null
    private var ubicacionConfirmada = false
    private lateinit var etDescripcion: EditText
    private lateinit var tvAyudaUbicacion: TextView
    private lateinit var btnReportar: com.google.android.material.button.MaterialButton
    private lateinit var btnHoraAvistamiento: com.google.android.material.button.MaterialButton
    private var fechaAvistamiento = System.currentTimeMillis()
    private var publicacionSeleccionadaId: Long? = null
    private var especieSeleccionada = "Perro"
    private lateinit var btnAlertaRelacionada: com.google.android.material.button.MaterialButton
    private lateinit var btnEspecieAvistamiento: com.google.android.material.button.MaterialButton
    private var fotoAvistUri: String? = null
    private lateinit var imgFotoAvist: ImageView
    private lateinit var tvFotoHintAvist: TextView
    private lateinit var tvEstadoRed: TextView
    private lateinit var monitorConectividad: MonitorConectividad
    private var formularioListo = false
    private var borradorActivo = false
    private var nombreAlertaSeleccionada: String? = null

    private val pickerGaleria = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val imagen = decodificarImagen(this, uri.toString(), 4)
            if (imagen == null) {
                fotoAvistUri = null
                imgFotoAvist.setImageDrawable(null)
                tvFotoHintAvist.visibility = View.VISIBLE
                Toast.makeText(this, "No se pudo leer esa imagen. El reporte puede enviarse sin foto.", Toast.LENGTH_LONG).show()
            } else {
                fotoAvistUri = uri.toString()
                imgFotoAvist.setImageBitmap(imagen)
                tvFotoHintAvist.visibility = View.GONE
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

        setContentView(R.layout.activity_reportar_avistamiento)

        findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
            .setNavigationOnClickListener { finish() }

        val nombre = intent.getStringExtra(EXTRA_NOMBRE)
        val publicacionId = intent.getLongExtra(EXTRA_PUBLICACION_ID, -1L).takeIf { it > 0L }
        especieSeleccionada = intent.getStringExtra(EXTRA_ESPECIE) ?: "Perro"
        publicacionSeleccionadaId = publicacionId
        nombreAlertaSeleccionada = nombre
        findViewById<TextView>(R.id.tv_mascota).text = nombre?.let { "Avistamiento de $it" }
            ?: "Reportar avistamiento de mascota perdida"

        etDescripcion = findViewById(R.id.et_descripcion)
        imgFotoAvist = findViewById(R.id.img_foto_avist)
        tvFotoHintAvist = findViewById(R.id.tv_foto_hint_avist)
        tvAyudaUbicacion = findViewById(R.id.tv_ayuda_ubicacion)
        tvEstadoRed = findViewById(R.id.tv_estado_red)
        btnHoraAvistamiento = findViewById(R.id.btn_hora_avistamiento)
        btnHoraAvistamiento.setOnClickListener { elegirHoraAvistamiento() }
        btnAlertaRelacionada = findViewById(R.id.btn_alerta_relacionada)
        btnEspecieAvistamiento = findViewById(R.id.btn_especie_avistamiento)
        btnAlertaRelacionada.text = nombre?.let { "Alerta: $it · cambiar" }
            ?: "Vincular con una alerta perdida (opcional)"
        btnAlertaRelacionada.setOnClickListener { elegirAlertaRelacionada() }
        btnEspecieAvistamiento.text = "Especie: $especieSeleccionada"
        btnEspecieAvistamiento.visibility = if (publicacionId == null) View.VISIBLE else View.GONE
        btnEspecieAvistamiento.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Especie del animal")
                .setItems(arrayOf("Perro", "Gato")) { dialog, indice ->
                    especieSeleccionada = if (indice == 0) "Perro" else "Gato"
                    btnEspecieAvistamiento.text = "Especie: $especieSeleccionada"
                    borradorActivo = true
                    guardarBorrador()
                    dialog.dismiss()
                }
                .show()
                .also { com.example.practicafinal.util.Dialogos.aplicar(this, it) }
        }

        findViewById<View>(R.id.contenedor_foto_avist).setOnClickListener {
            pickerGaleria.launch("image/*")
        }

        configurarMapa()

        btnReportar = findViewById(R.id.btn_reportar)
        btnReportar.isEnabled = false
        btnReportar.text = "Selecciona el lugar en el mapa"
        btnReportar.setOnClickListener { reportar() }
        monitorConectividad = MonitorConectividad(this) { conectado ->
            tvEstadoRed.visibility = if (conectado) View.GONE else View.VISIBLE
            tvEstadoRed.announceForAccessibility(if (conectado) "Conexión restablecida" else "Sin conexión a internet")
        }
        formularioListo = true
        etDescripcion.doAfterTextChanged {
            borradorActivo = true
            guardarBorrador()
        }
        ofrecerBorrador()
    }

    private fun configurarMapa() {
        map = findViewById(R.id.mapa_avistamiento)
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
        map.setBuiltInZoomControls(false)
        map.setMultiTouchControls(true)
        map.controller.setZoom(15.0)

        val ubicacionActual = obtenerUbicacionActual()
        val ultimaLatitud = intent.getDoubleExtra(EXTRA_ULTIMA_LATITUD, Double.NaN)
        val ultimaLongitud = intent.getDoubleExtra(EXTRA_ULTIMA_LONGITUD, Double.NaN)
        val ubicacionAlerta = if (!ultimaLatitud.isNaN() && !ultimaLongitud.isNaN()) {
            GeoPoint(ultimaLatitud, ultimaLongitud)
        } else null
        val centro = ubicacionActual ?: ubicacionAlerta ?: GeoPoint(-12.0464, -77.0428)
        map.controller.setCenter(centro)

        puntoSeleccionado = null
        tvAyudaUbicacion.text = if (ubicacionActual != null) {
            "Pin sugerido desde tu ubicación reciente. Toca el mapa para confirmar o ajustar dónde viste a la mascota."
        } else if (ubicacionAlerta != null) {
            "El mapa muestra la última ubicación reportada como referencia. Toca el punto exacto donde viste a la mascota."
        } else {
            "No hay una ubicación reciente disponible. Toca el mapa exactamente donde viste a la mascota."
        }
        pinSeleccion = Marker(map).apply {
            position = centro
            icon = ContextCompat.getDrawable(this@ReportarAvistamientoActivity, R.drawable.ic_pin_amarillo)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            alpha = if (ubicacionActual != null) 1f else 0.75f
        }
        map.overlays.add(pinSeleccion)

        map.overlays.add(
            MapEventsOverlay(object : MapEventsReceiver {
                override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                    puntoSeleccionado = p
                    ubicacionConfirmada = true
                    pinSeleccion.position = p
                    pinSeleccion.alpha = 1f
                    tvAyudaUbicacion.text = "Lugar seleccionado. Revisa que el pin esté donde viste a la mascota."
                    btnReportar.isEnabled = true
                    btnReportar.text = "Reportar avistamiento"
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

    private fun elegirHoraAvistamiento() {
        val etiquetas = arrayOf("Ahora", "Hace 1 hora", "Hace 3 horas", "Ayer", "Elegir fecha y hora")
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("¿Cuándo la viste?")
            .setItems(etiquetas) { dialog, indice ->
                if (indice == 4) {
                    dialog.dismiss()
                    elegirFechaHora()
                    return@setItems
                }
                val horasAtras = longArrayOf(0L, 1L, 3L, 24L)[indice]
                fechaAvistamiento = System.currentTimeMillis() - horasAtras * 60L * 60L * 1000L
                btnHoraAvistamiento.text = "La vi: ${etiquetas[indice]}"
                borradorActivo = true
                guardarBorrador()
                dialog.dismiss()
            }
            .show()
            .also { com.example.practicafinal.util.Dialogos.aplicar(this, it) }
    }

    private fun elegirFechaHora() {
        val calendario = Calendar.getInstance().apply { timeInMillis = fechaAvistamiento }
        DatePickerDialog(
            this,
            { _, year, month, day ->
                calendario.set(Calendar.YEAR, year)
                calendario.set(Calendar.MONTH, month)
                calendario.set(Calendar.DAY_OF_MONTH, day)
                TimePickerDialog(this, { _, hour, minute ->
                    calendario.set(Calendar.HOUR_OF_DAY, hour)
                    calendario.set(Calendar.MINUTE, minute)
                    calendario.set(Calendar.SECOND, 0)
                    calendario.set(Calendar.MILLISECOND, 0)
                    val elegido = calendario.timeInMillis
                    val ahora = System.currentTimeMillis()
                    val limite = ahora - 30L * 24 * 60 * 60 * 1000
                    if (elegido > ahora || elegido < limite) {
                        Toast.makeText(this, "Elige una fecha dentro de los últimos 30 días", Toast.LENGTH_LONG).show()
                        return@TimePickerDialog
                    }
                    fechaAvistamiento = elegido
                    btnHoraAvistamiento.text = "La vi: ${SimpleDateFormat("dd/MM, HH:mm", Locale.forLanguageTag("es-PE")).format(Date(elegido))}"
                    borradorActivo = true
                    guardarBorrador()
                }, calendario.get(Calendar.HOUR_OF_DAY), calendario.get(Calendar.MINUTE), true).show()
            },
            calendario.get(Calendar.YEAR), calendario.get(Calendar.MONTH), calendario.get(Calendar.DAY_OF_MONTH)
        ).apply { datePicker.maxDate = System.currentTimeMillis() }.show()
    }

    private fun elegirAlertaRelacionada() {
        ejecutarSeguro(executor) {
            val alertas = ControladorPublicaciones.obtenerPerdidas(this)
                .filter { it.estado == "Activa" }
                .sortedBy { alerta ->
                    puntoSeleccionado?.distanceToAsDouble(GeoPoint(alerta.latitud, alerta.longitud))
                        ?: Double.MAX_VALUE
                }
            actualizarUi {
                if (alertas.isEmpty()) {
                    Toast.makeText(this, "No hay alertas perdidas activas. El reporte quedará sin vincular.", Toast.LENGTH_LONG).show()
                    return@actualizarUi
                }
                val opciones = listOf("Dejar sin vincular") + alertas.map { alerta ->
                    val distancia = puntoSeleccionado?.distanceToAsDouble(GeoPoint(alerta.latitud, alerta.longitud))
                    val cercania = distancia?.let { " · ${String.format(Locale.forLanguageTag("es-PE"), "%.1f km", it / 1000.0)}" }.orEmpty()
                    "${alerta.nombre} · ${alerta.especie ?: "Mascota"}$cercania"
                }
                androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle("¿A qué alerta corresponde?")
                    .setItems(opciones.toTypedArray()) { dialog, indice ->
                        if (indice == 0) {
                            publicacionSeleccionadaId = null
                            nombreAlertaSeleccionada = null
                            btnAlertaRelacionada.text = "Vincular con una alerta perdida (opcional)"
                            btnEspecieAvistamiento.visibility = View.VISIBLE
                        } else {
                            val seleccionada = alertas[indice - 1]
                            publicacionSeleccionadaId = seleccionada.id
                            nombreAlertaSeleccionada = seleccionada.nombre
                            especieSeleccionada = seleccionada.especie ?: especieSeleccionada
                            btnAlertaRelacionada.text = "Alerta: ${seleccionada.nombre} · cambiar"
                            btnEspecieAvistamiento.text = "Especie: $especieSeleccionada"
                            btnEspecieAvistamiento.visibility = View.GONE
                            findViewById<TextView>(R.id.tv_mascota).text = "Avistamiento de ${seleccionada.nombre}"
                        }
                        borradorActivo = true
                        guardarBorrador()
                        dialog.dismiss()
                    }
                    .show()
                    .also { com.example.practicafinal.util.Dialogos.aplicar(this, it) }
            }
        }
    }

    private fun reportar() {
        val publicacionId = publicacionSeleccionadaId
        val punto = puntoSeleccionado
        if (punto == null) {
            Toast.makeText(this, "No se pudo reportar", Toast.LENGTH_SHORT).show()
            return
        }
        val descripcion = etDescripcion.text.toString().trim()
        val foto = fotoAvistUri
        if (!ubicacionConfirmada) {
            Toast.makeText(this, "Toca el mapa para confirmar dónde la viste", Toast.LENGTH_LONG).show()
            return
        }
        guardarBorrador()
        if (!monitorConectividad.hayInternet()) {
            btnReportar.isEnabled = true
            btnReportar.text = "Reportar avistamiento"
            Toast.makeText(this, "Sin conexión. El reporte quedó guardado como borrador en este teléfono.", Toast.LENGTH_LONG).show()
            return
        }
        btnReportar.isEnabled = false
        btnReportar.text = "Enviando reporte…"
        ejecutarSeguro(executor) {
            try {
                val usuarioId = SesionManager.obtenerUsuarioId(this)
                ControladorPublicaciones.reportarAvistamiento(
                    this, publicacionId, usuarioId, punto.latitude, punto.longitude,
                    descripcion, foto, fechaAvistamiento, especieSeleccionada
                )

                actualizarUi {
                    borradorActivo = false
                    BorradoresLocales.eliminar(this, claveBorrador())
                    Toast.makeText(this, "Avistamiento reportado!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            } catch (error: Exception) {
                actualizarUi {
                    btnReportar.isEnabled = true
                    btnReportar.text = "Reportar avistamiento"
                    Toast.makeText(
                        this,
                        error.message ?: "No se pudo guardar el avistamiento",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }
    private fun claveBorrador() = "avistamiento_${intent.getLongExtra(EXTRA_PUBLICACION_ID, 0L).takeIf { it > 0L } ?: 0L}"

    private fun ofrecerBorrador() {
        val datos = BorradoresLocales.obtener(this, claveBorrador()) ?: return
        AlertDialog.Builder(this)
            .setTitle("Tienes un reporte sin enviar")
            .setMessage("¿Quieres continuar el avistamiento donde lo dejaste?")
            .setPositiveButton("Continuar") { _, _ -> restaurarBorrador(datos) }
            .setNegativeButton("Empezar de nuevo") { _, _ -> BorradoresLocales.eliminar(this, claveBorrador()) }
            .setCancelable(false)
            .show()
            .also { com.example.practicafinal.util.Dialogos.aplicar(this, it) }
    }

    private fun guardarBorrador() {
        if (!formularioListo || !borradorActivo) return
        val punto = puntoSeleccionado
        val datos = JSONObject()
            .put("descripcion", etDescripcion.text.toString())
            .put("alertaId", publicacionSeleccionadaId ?: JSONObject.NULL)
            .put("alertaNombre", nombreAlertaSeleccionada ?: "")
            .put("especie", especieSeleccionada)
            .put("fecha", fechaAvistamiento)
            .put("ubicacionConfirmada", ubicacionConfirmada)
            .put("lat", punto?.latitude ?: JSONObject.NULL)
            .put("lng", punto?.longitude ?: JSONObject.NULL)
            .put("foto", fotoAvistUri ?: "")
        BorradoresLocales.guardar(this, claveBorrador(), datos)
    }

    private fun restaurarBorrador(datos: JSONObject) {
        formularioListo = false
        etDescripcion.setText(datos.optString("descripcion"))
        especieSeleccionada = datos.optString("especie", especieSeleccionada)
        btnEspecieAvistamiento.text = "Especie: $especieSeleccionada"
        fechaAvistamiento = datos.optLong("fecha", System.currentTimeMillis())
        btnHoraAvistamiento.text = "La vi: ${SimpleDateFormat("dd/MM, HH:mm", Locale.forLanguageTag("es-PE")).format(Date(fechaAvistamiento))}"
        publicacionSeleccionadaId = datos.optLong("alertaId", 0L).takeIf { it > 0L }
        nombreAlertaSeleccionada = datos.optString("alertaNombre").takeIf(String::isNotBlank)
        nombreAlertaSeleccionada?.let { nombre ->
            btnAlertaRelacionada.text = "Alerta: $nombre · cambiar"
            findViewById<TextView>(R.id.tv_mascota).text = "Avistamiento de $nombre"
            btnEspecieAvistamiento.visibility = View.GONE
        }
        val lat = datos.optDouble("lat", Double.NaN)
        val lng = datos.optDouble("lng", Double.NaN)
        ubicacionConfirmada = datos.optBoolean("ubicacionConfirmada", false) && lat.isFinite() && lng.isFinite()
        if (lat.isFinite() && lng.isFinite()) {
            val punto = GeoPoint(lat, lng)
            puntoSeleccionado = punto
            pinSeleccion.position = punto
            pinSeleccion.alpha = 1f
            map.controller.setCenter(punto)
            btnReportar.isEnabled = ubicacionConfirmada
            btnReportar.text = if (ubicacionConfirmada) "Reportar avistamiento" else "Selecciona el lugar en el mapa"
            if (ubicacionConfirmada) tvAyudaUbicacion.text = "Lugar restaurado. Revisa que el pin esté donde viste a la mascota."
        }
        fotoAvistUri = datos.optString("foto").takeIf(String::isNotBlank)
        if (!fotoAvistUri.isNullOrBlank()) {
            val bitmap = decodificarImagen(this, fotoAvistUri!!, 4)
            if (bitmap != null) {
                imgFotoAvist.setImageBitmap(bitmap)
                tvFotoHintAvist.visibility = View.GONE
            } else {
                fotoAvistUri = null
                tvFotoHintAvist.visibility = View.VISIBLE
                Toast.makeText(this, "El acceso a la foto anterior venció; selecciónala otra vez.", Toast.LENGTH_LONG).show()
            }
        }
        formularioListo = true
        borradorActivo = true
        guardarBorrador()
    }

    override fun onStart() {
        super.onStart()
        if (::monitorConectividad.isInitialized) monitorConectividad.iniciar()
    }

    override fun onStop() {
        if (::monitorConectividad.isInitialized) monitorConectividad.detener()
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        if (::map.isInitialized) map.onResume()
    }

    override fun onPause() {
        guardarBorrador()
        super.onPause()
        if (::map.isInitialized) map.onPause()
    }

    override fun onDestroy() {
        if (::map.isInitialized) map.onDetach()
        super.onDestroy()
        executor.shutdown()
    }
}
