package com.example.practicafinal

import android.Manifest;
import android.content.Context;
import android.content.Intent
import android.location.Location
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle
import android.view.View;
import android.widget.TextView;
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.example.practicafinal.util.ejecutarSeguro
import com.example.practicafinal.util.actualizarUi
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.practicafinal.controlador.ControladorAlbergues
import com.example.practicafinal.controlador.ControladorPublicaciones
import com.example.practicafinal.modelo.Albergue
import com.example.practicafinal.modelo.Avistamiento;
import com.example.practicafinal.modelo.Denuncia
import com.example.practicafinal.modelo.Publicacion
import com.example.practicafinal.session.SesionManager;
import com.example.practicafinal.util.fechaRelativa
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.ListenerRegistration
import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.XYTileSource;
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polygon
import java.util.Locale;
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var map: MapView;
    private val exec = Executors.newSingleThreadExecutor()
    private val locationHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var locationListener: android.location.LocationListener? = null
    private var locationTimeout: Runnable? = null
    private var userLoc: GeoPoint? = null;
    private var markerUsuario: Marker? = null
    private var circUser: Polygon? = null;
    private var circBusq: Polygon? = null
    private val marcPub = mutableListOf<Marker>();
    private val marcAvist = mutableListOf<Marker>()
    private val marcAlb = mutableListOf<Marker>()
    private val marcDenuncias = mutableListOf<Marker>()
    private lateinit var pnl: View;
    private lateinit var pnlEmoji: TextView;
    private lateinit var pnlTit: TextView
    private lateinit var pnlTipo: TextView;
    private lateinit var pnlDesc: TextView
    private lateinit var pnlUbi: TextView;
    private lateinit var pnlEst: TextView;
    private lateinit var pnlFoto: android.widget.ImageView
    private lateinit var pnlBotonesTipo: android.widget.LinearLayout
    private lateinit var pnlCompartir: MaterialButton
    private lateinit var pnlBtnIzq: MaterialButton
    private lateinit var pnlBtnDer: MaterialButton
    private var textoCompartirActual: String? = null
    private var marcBusqAvist: MutableList<Polygon> = mutableListOf()
    private lateinit var btnCercanas: TextView
    private lateinit var btnNotificaciones: TextView
    private lateinit var dividerAlertasMapa: View
    private lateinit var panelControlesMapa: View
    private lateinit var btnToggleControlesMapa: MaterialButton
    private var pendienteMostrarAlerta: Long? = null
    private val firebaseListeners = mutableListOf<ListenerRegistration>()
    private var listenersAttached = false
    private var publicacionesActuales: List<Publicacion> = emptyList()
    private var avistamientosActuales: List<Avistamiento> = emptyList()
    private var alberguesActuales: List<Albergue> = emptyList()
    private var denunciasActuales: List<Denuncia> = emptyList()

    private val permiso = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permisos ->
        if (permisos.values.any { it }) centrarUsuario() else {
            Toast.makeText(this, "Permiso denegado", Toast.LENGTH_SHORT).show(); centrarLima()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (SesionManager.hayRegistroPendiente(this)) {
            startActivity(Intent(this, RegistroActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
            return
        }
        if (!SesionManager.tieneSesion(this)) {
            startActivity(Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }); finish(); return
        }
        Configuration.getInstance().load(
            applicationContext,
            getSharedPreferences("osmdroid", MODE_PRIVATE)
        ); Configuration.getInstance().userAgentValue = "MapaAlertas/1.0 (juanguz619@gmail.com)"
        setContentView(R.layout.activity_main)
        map = findViewById(R.id.map); map.setUseDataConnection(true); map.setTileSource(
            XYTileSource(
                "OpenStreetMap",
                0,
                19,
                256,
                ".png",
                arrayOf(
                    "https://tile.openstreetmap.org/",
                    "https://a.tile.openstreetmap.org/",
                    "https://b.tile.openstreetmap.org/",
                    "https://c.tile.openstreetmap.org/"
                )
            )
        ); map.setMultiTouchControls(true); map.setBuiltInZoomControls(false)
        map.overlays.add(MapEventsOverlay(object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint) = false;
            override fun longPressHelper(p: GeoPoint): Boolean {
                startActivity(Intent(this@MainActivity, CrearPublicacionActivity::class.java).apply {
                    putExtra(
                        CrearPublicacionActivity.EXTRA_LAT,
                        p.latitude
                    ); putExtra(CrearPublicacionActivity.EXTRA_LNG, p.longitude)
                }); return true
            }
        }))
        pnl = findViewById(R.id.panel_alerta); pnlEmoji = findViewById(R.id.panel_emoji); pnlTit =
            findViewById(R.id.panel_titulo); pnlTipo = findViewById(R.id.panel_tipo); pnlDesc =
            findViewById(R.id.panel_desc); pnlUbi = findViewById(R.id.panel_ubicacion); pnlEst =
            findViewById(R.id.panel_estado)
        pnlFoto = findViewById(R.id.panel_foto)
        pnlBotonesTipo = findViewById(R.id.panel_botones_tipo)
        pnlCompartir = findViewById(R.id.panel_compartir)
        pnlBtnIzq = findViewById(R.id.panel_btn_izq)
        pnlBtnDer = findViewById(R.id.panel_btn_der)
        pnl.findViewById<TextView>(R.id.panel_cerrar)
            .setOnClickListener { ocultarPanel() }
        pnlCompartir.setOnClickListener { compartir() }
        findViewById<com.google.android.material.button.MaterialButton>(R.id.fab_menu).setOnClickListener {
            startActivity(
                Intent(this, MenuOpcionesActivity::class.java)
            )
        }
        findViewById<com.google.android.material.button.MaterialButton>(R.id.fab_perfil).setOnClickListener {
            startActivity(
                Intent(this, PerfilActivity::class.java)
            )
        }
        findViewById<com.google.android.material.button.MaterialButton>(R.id.fab_recenter).setOnClickListener { centrarUsuario() }
        findViewById<TextView>(R.id.btn_leyenda).setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(this).setTitle("Leyenda del mapa")
                .setView(R.layout.dialog_leyenda).setPositiveButton("Entendido", null).show()
                .also { com.example.practicafinal.util.Dialogos.aplicar(this, it) }
        }
        findViewById<com.google.android.material.button.MaterialButton>(R.id.fab_zoom_in).setOnClickListener { map.controller.zoomIn() }
        findViewById<com.google.android.material.button.MaterialButton>(R.id.fab_zoom_out).setOnClickListener { map.controller.zoomOut() }
        btnCercanas = findViewById(R.id.btn_cercanas)
        dividerAlertasMapa = findViewById(R.id.divisor_alertas_mapa)
        btnCercanas.setOnClickListener { dialogoCercanas() }
        panelControlesMapa = findViewById(R.id.panel_controles_mapa)
        btnToggleControlesMapa = findViewById(R.id.btn_toggle_controles_mapa)
        btnToggleControlesMapa.setOnClickListener {
            val mostrar = panelControlesMapa.visibility != View.VISIBLE
            panelControlesMapa.visibility = if (mostrar) View.VISIBLE else View.GONE
            btnToggleControlesMapa.text = if (mostrar) "Ocultar controles" else "Opciones del mapa"
            btnToggleControlesMapa.contentDescription =
                if (mostrar) "Ocultar controles del mapa" else "Mostrar controles del mapa"
        }
        btnNotificaciones = findViewById(R.id.btn_notificaciones)
        btnNotificaciones.setOnClickListener {
            startActivity(Intent(this, NotificacionesActivity::class.java))
        }
        cargarBD()
        val clat = intent.getDoubleExtra("centrar_lat", Double.NaN);
        val clng = intent.getDoubleExtra("centrar_lng", Double.NaN)
        if (!clat.isNaN() && !clng.isNaN()) {
            map.controller.setZoom(19.5); centrarArriba(GeoPoint(clat, clng))
        }
        val alertaId = intent.getLongExtra("mostrar_alerta", -1L)
        pendienteMostrarAlerta = alertaId.takeIf { it != -1L }
        if (pendienteMostrarAlerta != null) {
            mostrarAlertaPendiente()
        }
        val tieneDestino = (!clat.isNaN() && !clng.isNaN()) || pendienteMostrarAlerta != null
        if (!tieneDestino) {
            if (tienePermiso()) centrarUsuario() else permiso.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }

    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent);
        setIntent(intent)
        val lat = intent.getDoubleExtra("centrar_lat", Double.NaN);
        val lng = intent.getDoubleExtra("centrar_lng", Double.NaN)
        val alertaId = intent.getLongExtra("mostrar_alerta", -1L)
        if ((!lat.isNaN() && !lng.isNaN()) || alertaId != -1L) {
            cancelarUbicacion()
        }
        if (!lat.isNaN() && !lng.isNaN()) {
            map.controller.setZoom(19.5); centrarArriba(GeoPoint(lat, lng))
        }
        pendienteMostrarAlerta = alertaId.takeIf { it != -1L }
        if (pendienteMostrarAlerta != null) {
            mostrarAlertaPendiente()
        }
    }

    private fun mostrarAlertaPendiente() {
        val id = pendienteMostrarAlerta ?: return
        val publicacion = publicacionesActuales.firstOrNull { it.id == id }
        if (publicacion != null) {
            pendienteMostrarAlerta = null
            centrarArriba(GeoPoint(publicacion.latitud, publicacion.longitud))
            showPanel(publicacion)
            return
        }

        ejecutarSeguro(exec) {
            val encontrada = ControladorPublicaciones.obtenerPorId(this, id)
            actualizarUi {
                if (pendienteMostrarAlerta == id && encontrada != null) {
                    pendienteMostrarAlerta = null
                    centrarArriba(GeoPoint(encontrada.latitud, encontrada.longitud))
                    showPanel(encontrada)
                }
            }
        }
    }

    private fun centrarArriba(p: GeoPoint) {
        val b = map.boundingBox; map.controller.animateTo(
            GeoPoint(
                p.latitude - (b.latNorth - b.latSouth) * 0.25,
                p.longitude
            )
        )
    }

    private fun tienePermiso() = com.example.practicafinal.util.tienePermisoUbicacion(this)

    private fun getLastLoc(): Location? = com.example.practicafinal.util.ultimaUbicacion(this)

    private fun cancelarUbicacion() {
        val manager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        locationListener?.let { try { manager.removeUpdates(it) } catch (_: SecurityException) {} }
        locationListener = null
        locationTimeout?.let { locationHandler.removeCallbacks(it) }
        locationTimeout = null
    }

    private fun mostrarUbicacion(loc: Location) {
        userLoc = GeoPoint(loc.latitude, loc.longitude)
        marcUser(userLoc!!)
        circUser(userLoc!!)
        map.controller.setZoom(19.5)
        map.controller.setCenter(userLoc)
    }

    private fun centrarUsuario() {
        cancelarUbicacion()
        if (!tienePermiso()) {
            permiso.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            return
        }
        getLastLoc()?.let { mostrarUbicacion(it); return }
        val manager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val callback = object : android.location.LocationListener {
            override fun onLocationChanged(loc: Location) {
                cancelarUbicacion()
                if (!isFinishing && !isDestroyed) mostrarUbicacion(loc)
            }
            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }
        locationListener = callback
        try {
            val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
                .filter { manager.isProviderEnabled(it) }
            if (providers.isEmpty()) {
                cancelarUbicacion()
                Toast.makeText(this, "Activa la ubicación para encontrar alertas cercanas", Toast.LENGTH_SHORT).show()
                centrarLima()
                return
            }
            providers.forEach { manager.requestLocationUpdates(it, 1000L, 5f, callback, mainLooper) }
            locationTimeout = Runnable {
                cancelarUbicacion()
                if (!isFinishing && !isDestroyed) {
                    Toast.makeText(this, "No se pudo obtener tu ubicación", Toast.LENGTH_SHORT).show()
                    centrarLima()
                }
            }.also { locationHandler.postDelayed(it, 15000) }
        } catch (_: SecurityException) {
            cancelarUbicacion()
            centrarLima()
        }
    }

    private fun centrarLima() {
        map.controller.setCenter(GeoPoint(-12.0464, -77.0428)); map.controller.setZoom(12.0); map.invalidate()
    }

    private fun marcUser(p: GeoPoint) {
        if (markerUsuario == null) {
            markerUsuario = Marker(map).apply {
                position = p; title = "Mi ubicacion"; icon =
                ContextCompat.getDrawable(this@MainActivity, R.drawable.ic_punto_azul); setAnchor(
                    Marker.ANCHOR_CENTER,
                    Marker.ANCHOR_CENTER
                )
            }
            map.overlays.add(markerUsuario)
        } else {
            markerUsuario!!.position = p
        }
        map.invalidate()
    }

    private fun circUser(c: GeoPoint) {
        circUser?.let { map.overlays.remove(it) }; circUser = Polygon().apply {
            points = (0..48).map { c.destinationPoint(2000.0, it * 360.0 / 48) }; fillColor = 0x141E88E5; strokeColor =
            0x401E88E5; strokeWidth = 2f
        }; map.overlays.add(0, circUser); map.invalidate()
    }

    private fun circBusq(pub: Publicacion) {
        circBusq?.let { map.overlays.remove(it) }; circBusq =
            null; if (pub.tipo == "Perdida" && pub.estado == "Activa") {
            circBusq = Polygon().apply {
                points = (0..48).map {
                    GeoPoint(pub.latitud, pub.longitud).destinationPoint(
                        1500.0,
                        it * 360.0 / 48
                    )
                }; fillColor = 0x14E53935; strokeColor = 0x40E53935; strokeWidth = 2f
            }; map.overlays.add(circBusq); map.invalidate()
        }
    }

    private fun cargarBD() {
        ejecutarSeguro(exec) {
            if (listenersAttached) return@ejecutarSeguro
            actualizarUi {
                if (listenersAttached) return@actualizarUi
                listenersAttached = true
                val errorHandler: (Exception) -> Unit = { error ->
                    android.util.Log.e("PetGo", "Error sincronizando los datos del mapa", error)
                    actualizarUi {
                        Toast.makeText(this, "Error de sincronización o red", Toast.LENGTH_SHORT).show()
                    }
                }
                firebaseListeners += ControladorPublicaciones.observarPublicaciones(
                    this,
                    { values ->
                        publicacionesActuales = values
                        render(
                            publicacionesActuales,
                            avistamientosActuales,
                            alberguesActuales,
                            denunciasActuales,
                            publicacionesActuales.associate { it.id to it.nombre }
                        )
                    },
                    errorHandler
                )
                firebaseListeners += ControladorPublicaciones.observarAvistamientos(
                    this,
                    { values ->
                        avistamientosActuales = values
                        render(
                            publicacionesActuales,
                            avistamientosActuales,
                            alberguesActuales,
                            denunciasActuales,
                            publicacionesActuales.associate { it.id to it.nombre }
                        )
                    },
                    errorHandler
                )
                firebaseListeners += ControladorAlbergues.observarAlbergues(
                    this,
                    { values ->
                        alberguesActuales = values
                        render(
                            publicacionesActuales,
                            avistamientosActuales,
                            alberguesActuales,
                            denunciasActuales,
                            publicacionesActuales.associate { it.id to it.nombre }
                        )
                    },
                    errorHandler
                )
                firebaseListeners += ControladorPublicaciones.observarDenuncias(
                    this,
                    { values ->
                        denunciasActuales = values
                        render(
                            publicacionesActuales,
                            avistamientosActuales,
                            alberguesActuales,
                            denunciasActuales,
                            publicacionesActuales.associate { it.id to it.nombre }
                        )
                    },
                    errorHandler
                )
            }
        }
    }
    private fun render(l: List<Publicacion>, a: List<Avistamiento>, alb: List<Albergue>, den: List<Denuncia>, n: Map<Long, String>) {
        marcPub.forEach { map.overlays.remove(it) }; marcPub.clear(); marcAvist.forEach { map.overlays.remove(it) }; marcAvist.clear(); marcAlb.forEach {
            map.overlays.remove(
                it
            )
        }; marcAlb.clear(); marcDenuncias.forEach { map.overlays.remove(it) }; marcDenuncias.clear()
        marcBusqAvist.forEach { map.overlays.remove(it) }; marcBusqAvist.clear()
        val activas = l.filter { it.estado == "Activa" }
        for (pub in activas) {
            val m = Marker(map); m.position = GeoPoint(pub.latitud, pub.longitud); m.icon =
                ContextCompat.getDrawable(this, ico(pub)); m.setAnchor(
                Marker.ANCHOR_CENTER,
                Marker.ANCHOR_BOTTOM
            ); m.relatedObject = pub; m.setOnMarkerClickListener { mk, _ ->
                (mk.relatedObject as? Publicacion)?.let {
                    centrarArriba(mk.position); circBusq(
                    it
                ); showPanel(it)
                }; true
            }; map.overlays.add(m); marcPub.add(m)
        }
        val alertasPerdidasActivas = l.filter { it.tipo == "Perdida" && it.estado == "Activa" }
            .map { it.id }.toSet()
        val avistamientosVisibles = a.filter { avistamiento ->
            avistamiento.estadoRevision !in setOf("No coincide", "Duplicado") &&
                (avistamiento.publicacionId == 0L || avistamiento.publicacionId in alertasPerdidasActivas)
        }
        for (av in avistamientosVisibles) {
            val m = Marker(map); m.position = GeoPoint(av.latitud, av.longitud); m.icon =
                ContextCompat.getDrawable(this, R.drawable.ic_pin_amarillo); m.setAnchor(
                Marker.ANCHOR_CENTER,
                Marker.ANCHOR_BOTTOM
            ); m.relatedObject = av; m.setOnMarkerClickListener { mk, _ ->
                (mk.relatedObject as? Avistamiento)?.let {
                    val relacionada = l.firstOrNull { publicacion -> publicacion.id == it.publicacionId }
                    val esPropietario = relacionada?.usuarioId == SesionManager.obtenerUsuarioId(this)
                    if (esPropietario && it.publicacionId > 0L) {
                        ejecutarSeguro(exec) {
                            val ubicacionExacta = runCatching {
                                ControladorPublicaciones.obtenerCoordenadasAvistamiento(this, it.id)
                            }.getOrNull()
                            val reporte = if (ubicacionExacta != null) {
                                it.copy(latitud = ubicacionExacta.first, longitud = ubicacionExacta.second)
                            } else it
                            actualizarUi {
                                centrarArriba(GeoPoint(reporte.latitud, reporte.longitud))
                                showPanelAvist(reporte, n[it.publicacionId] ?: "la mascota")
                            }
                        }
                    } else {
                        centrarArriba(mk.position)
                        showPanelAvist(it, n[it.publicacionId] ?: "la mascota")
                    }
                }; true
            }; map.overlays.add(m); marcAvist.add(m)
        }
        for (al in alb) {
            val m = Marker(map); m.position = GeoPoint(al.latitud, al.longitud); m.icon =
                ContextCompat.getDrawable(this, R.drawable.ic_pin_albergue); m.setAnchor(
                Marker.ANCHOR_CENTER,
                Marker.ANCHOR_BOTTOM
            ); m.relatedObject = al; m.setOnMarkerClickListener { mk, _ ->
                (mk.relatedObject as? Albergue)?.let {
                    centrarArriba(mk.position); mostrarAlbergue(it)
                }; true
            }; map.overlays.add(m); marcAlb.add(m)
        }
        for (d in den) {
            val m = Marker(map); m.position = GeoPoint(d.latitud, d.longitud); m.icon =
                ContextCompat.getDrawable(this, R.drawable.ic_pin_denuncia); m.setAnchor(
                Marker.ANCHOR_CENTER,
                Marker.ANCHOR_BOTTOM
            ); m.relatedObject = d; m.setOnMarkerClickListener { mk, _ ->
                (mk.relatedObject as? Denuncia)?.let {
                    centrarArriba(mk.position); mostrarDenuncia(it)
                }; true
            }; map.overlays.add(m); marcDenuncias.add(m)
        }

        dibujarBusquedaColaborativa(l, avistamientosVisibles)

        map.invalidate(); actualizarBtnCercanas(l)
        pendienteMostrarAlerta?.let { id ->
            l.find { it.id == id }?.let {
                pendienteMostrarAlerta = null
                centrarArriba(GeoPoint(it.latitud, it.longitud))
                showPanel(it)
            }
        }
    }

    private fun mostrarAlbergue(al: Albergue) {
        val telefono = al.telefono.filter(Char::isDigit).takeIf(String::isNotBlank)
        val direccion = valorInformativo(al.direccion)
        val texto = buildList {
            add(al.nombre)
            direccion?.let(::add)
            telefono?.let { add("Teléfono: $it") }
            add("https://maps.google.com/?q=${al.latitud},${al.longitud}")
        }.joinToString("\n")
        prepararPanel(
            etiqueta = "ALBERGUE",
            colorEtiqueta = R.color.colorSecondary,
            titulo = al.nombre,
            subtitulo = direccion,
            descripcion = al.descripcion,
            latitud = al.latitud,
            longitud = al.longitud,
            estado = telefono?.let { "Teléfono: $it" },
            colorEstado = R.color.texto_suave,
            foto = al.foto,
            textoCompartir = texto
        )
        configurarAcciones(
            izquierda = telefono?.let { numero -> "Llamar" to { llamar(numero) } },
            derecha = "Cómo llegar" to { abrirRuta(al.latitud, al.longitud) }
        )
        mostrarPanelAnimado()
    }

    private fun prepararPanel(
        etiqueta: String,
        colorEtiqueta: Int,
        titulo: String,
        subtitulo: String?,
        descripcion: String?,
        latitud: Double,
        longitud: Double,
        estado: String?,
        colorEstado: Int,
        foto: String?,
        textoCompartir: String?
    ) {
        pnlEmoji.visibility = View.VISIBLE
        pnlEmoji.text = etiqueta
        pnlEmoji.setTextColor(ContextCompat.getColor(this, colorEtiqueta))
        pnlTit.text = titulo
        pnlTipo.text = subtitulo.orEmpty()
        pnlTipo.visibility = if (subtitulo.isNullOrBlank()) View.GONE else View.VISIBLE
        pnlDesc.text = descripcion?.trim()?.takeIf(String::isNotEmpty)
            ?: "Sin detalles adicionales."
        val distanciaMetros = userLoc?.distanceToAsDouble(GeoPoint(latitud, longitud))
        pnlUbi.text = distanciaMetros?.let { metros ->
            if (metros < 1000) "A ${metros.toInt()} m de ti"
            else "A ${String.format(Locale.forLanguageTag("es-PE"), "%.1f", metros / 1000.0)} km de ti"
        } ?: "Ubicación marcada en el mapa"
        pnlEst.text = estado.orEmpty()
        pnlEst.visibility = if (estado.isNullOrBlank()) View.GONE else View.VISIBLE
        pnlEst.setTextColor(ContextCompat.getColor(this, colorEstado))
        pnlFoto.setImageDrawable(null)
        if (!foto.isNullOrBlank()) {
            com.example.practicafinal.util.cargarImagen(pnlFoto, foto)
            pnlFoto.visibility = View.VISIBLE
        } else {
            pnlFoto.visibility = View.GONE
        }
        textoCompartirActual = textoCompartir?.takeIf(String::isNotBlank)
        val mostrarCompartir = textoCompartirActual != null
        pnlCompartir.visibility = if (mostrarCompartir) View.VISIBLE else View.GONE
        (pnlCompartir.parent as? View)?.visibility = if (mostrarCompartir) View.VISIBLE else View.GONE
        configurarAcciones(null, null)
    }

    private fun valorInformativo(valor: String?): String? = valor?.trim()?.takeIf {
        it.isNotEmpty() && !it.equals("desconocido", ignoreCase = true) &&
            !it.equals("desconocida", ignoreCase = true) && !it.equals("null", ignoreCase = true) &&
            !it.equals("no disponible", ignoreCase = true)
    }

    private fun configurarAcciones(
        izquierda: Pair<String, () -> Unit>?,
        derecha: Pair<String, () -> Unit>?
    ) {
        fun configurarBoton(boton: MaterialButton, accion: Pair<String, () -> Unit>?) {
            if (accion == null) {
                boton.visibility = View.GONE
                boton.setOnClickListener(null)
                return
            }
            boton.text = accion.first
            boton.contentDescription = accion.first
            boton.setStrokeColorResource(when (accion.first) {
                "Contactar", "Llamar" -> R.color.colorSuccess
                "Avisar que la vi" -> R.color.colorRed
                "Ver detalles", "Ver alerta" -> R.color.colorSecondary
                else -> R.color.tinta
            })
            boton.visibility = View.VISIBLE
            boton.setOnClickListener { accion.second.invoke() }
        }

        configurarBoton(pnlBtnIzq, izquierda)
        configurarBoton(pnlBtnDer, derecha)
        pnlBotonesTipo.visibility =
            if (izquierda == null && derecha == null) View.GONE else View.VISIBLE
    }

    private fun mostrarPanelAnimado() {
        pnl.visibility = View.VISIBLE
        pnl.alpha = 0f
        pnl.post {
            pnl.translationY = pnl.height.toFloat()
            pnl.animate().translationY(0f).alpha(1f).setDuration(250).start()
        }
    }

    private fun actualizarBtnCercanas(l: List<Publicacion>) {
        val u = userLoc; if (u == null) {
            btnCercanas.visibility = View.GONE
            dividerAlertasMapa.visibility = View.GONE
            return
        };
        val c = l.count { it.estado == "Activa" }; if (c > 0) {
            btnCercanas.visibility = View.VISIBLE
            dividerAlertasMapa.visibility = View.VISIBLE
            btnCercanas.text = "Alertas ($c)"
            btnCercanas.contentDescription = "Alertas activas: $c"
        } else {
            btnCercanas.visibility = View.GONE
            dividerAlertasMapa.visibility = View.GONE
        }
    }

    private fun dialogoCercanas() {
        if (userLoc == null) return
        val radios = arrayOf("Hasta 5 km", "Hasta 10 km", "Hasta 25 km", "Cualquier distancia")
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Buscar alertas dentro de")
            .setItems(radios) { _, which ->
                val radioKm = when (which) {
                    0 -> 5.0
                    1 -> 10.0
                    2 -> 25.0
                    else -> null
                }
                mostrarAlertasCercanas(radioKm)
            }
            .show()
            .also { com.example.practicafinal.util.Dialogos.aplicar(this, it) }
    }

    private fun mostrarAlertasCercanas(radioKm: Double?) {
        val origen = userLoc ?: return
        data class AlertaCercana(val publicacion: Publicacion, val kilometros: Double)
        val items = publicacionesActuales
            .filter { it.estado == "Activa" }
            .map { AlertaCercana(it, GeoPoint(it.latitud, it.longitud).distanceToAsDouble(origen) / 1000.0) }
            .filter { radioKm == null || it.kilometros <= radioKm }
            .sortedBy { it.kilometros }
            .take(20)
        if (items.isEmpty()) {
            Toast.makeText(this, "No hay alertas dentro de ese radio", Toast.LENGTH_SHORT).show()
            return
        }
        val v = android.view.LayoutInflater.from(this).inflate(R.layout.dialog_cercanas, null)
        val rv = v.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rv_dialog_cercanas)
        rv.layoutManager = LinearLayoutManager(this)
        val titulo = if (radioKm == null) "Alertas cercanas · cualquier distancia"
        else "Alertas dentro de ${radioKm.toInt()} km"
        val dialogo = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(titulo)
            .setView(v)
            .setPositiveButton("Cerrar", null)
            .create()
        rv.adapter = object : androidx.recyclerview.widget.RecyclerView.Adapter<androidx.recyclerview.widget.RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int) = object :
                androidx.recyclerview.widget.RecyclerView.ViewHolder(
                    android.view.LayoutInflater.from(parent.context).inflate(R.layout.item_alerta_cercana, parent, false)
                ) {}

            override fun onBindViewHolder(holder: androidx.recyclerview.widget.RecyclerView.ViewHolder, position: Int) {
                val alerta = items[position]
                val publicacion = alerta.publicacion
                holder.itemView.findViewById<View>(R.id.dot).background = ContextCompat.getDrawable(
                    this@MainActivity,
                    when (publicacion.tipo) {
                        "Perdida" -> R.drawable.bg_dot_rojo
                        "Encontrada" -> R.drawable.bg_dot_verde
                        else -> R.drawable.bg_dot_naranja
                    }
                )
                holder.itemView.findViewById<TextView>(R.id.tv_info).text =
                    "${publicacion.nombre} · ${String.format(Locale.forLanguageTag("es-PE"), "%.1f km", alerta.kilometros)} · ${publicacion.tipo}"
                holder.itemView.setOnClickListener {
                    dialogo.dismiss()
                    centrarArriba(GeoPoint(publicacion.latitud, publicacion.longitud))
                    showPanel(publicacion)
                }
            }

            override fun getItemCount() = items.size
        }
        dialogo.show()
        com.example.practicafinal.util.Dialogos.aplicar(this, dialogo)
    }

    private fun ico(p: Publicacion) = when {
        p.estado == "Resuelta" || p.estado == "Adoptada" -> R.drawable.ic_pin_gris
        p.tipo == "Perdida" -> R.drawable.ic_pin_rojo
        p.tipo == "Encontrada" -> R.drawable.ic_pin_verde
        else -> R.drawable.ic_pin_naranja
    }

    private fun dibujarBusquedaColaborativa(publicaciones: List<Publicacion>, avistamientos: List<Avistamiento>) {
        val perdidasActivas = publicaciones.filter { it.tipo == "Perdida" && it.estado == "Activa" }
        for (pub in perdidasActivas) {
            val avistDePub = avistamientos
                .filter { it.publicacionId == pub.id && it.estadoRevision !in setOf("No coincide", "Duplicado") }
                .sortedBy { it.fecha }
            if (avistDePub.isEmpty()) continue
            val puntos = listOf(GeoPoint(pub.latitud, pub.longitud)) +
                avistDePub.map { GeoPoint(it.latitud, it.longitud) }
            val ahora = System.currentTimeMillis()
            for (indice in 0 until puntos.lastIndex) {
                val fecha = avistDePub.getOrNull(indice)?.fecha ?: ahora
                val horasPasadas = (ahora - fecha) / (1000.0 * 60 * 60)
                val opacidad = if (horasPasadas < 24) 0x60FFA000 else if (horasPasadas < 72) 0x40FFA000 else 0x20FFA000
                val linea = Polygon()
                linea.points = listOf(puntos[indice], puntos[indice + 1])
                linea.fillColor = 0x00000000
                linea.strokeColor = opacidad
                linea.strokeWidth = 3f
                map.overlays.add(linea)
                marcBusqAvist.add(linea)
            }
        }
    }

    private fun tl(t: String) = when (t) {
        "Perdida" -> "Mascota perdida"
        "Encontrada" -> "Mascota encontrada"
        else -> "En adopción"
    }

    private fun showPanel(pub: Publicacion) {
        val especie = valorInformativo(pub.especie)
        val ultimaZona = valorInformativo(pub.ultimoLugar)
        val subtitulo = buildList {
            especie?.let(::add)
            if (pub.tipo == "Perdida") ultimaZona?.let { add("Última zona: $it") }
        }.joinToString(" · ").takeIf(String::isNotEmpty)
        val numeroAvistamientos = avistamientosActuales.count {
            it.publicacionId == pub.id && it.estadoRevision !in setOf("No coincide", "Duplicado")
        }
        val textoEstado = when {
            pub.estado == "Resuelta" && pub.tipo == "Perdida" -> "Encontrada"
            pub.estado == "Resuelta" -> "Resuelta"
            pub.estado == "Adoptada" -> "Adoptada"
            pub.tipo == "Perdida" && numeroAvistamientos > 0 -> "En búsqueda · $numeroAvistamientos avistamientos"
            pub.tipo == "Perdida" -> "En búsqueda"
            pub.tipo == "Encontrada" -> "Busca a su familia"
            else -> "Disponible"
        }
        val fechaPublicacion = pub.fechaCreacion.takeIf { it > 0L }?.let(::fechaRelativa)
        val fechaPerdida = pub.fechaPerdida?.let(::fechaRelativa)
        val estado = buildString {
            append(textoEstado)
            fechaPerdida?.let { append(" · vista por última vez $it") }
            if (fechaPerdida == null) fechaPublicacion?.let { append(" · publicada $it") }
        }
        val colorEstado = when (pub.estado) {
            "Resuelta" -> R.color.texto_suave
            "Adoptada" -> R.color.colorSecondary
            else -> if (pub.tipo == "Perdida") R.color.colorRed else R.color.verde_estado
        }
        val textoCompartir = buildString {
            appendLine("${pub.nombre} · ${tl(pub.tipo)}")
            especie?.let { appendLine("Especie: $it") }
            ultimaZona?.let { appendLine("Última zona: $it") }
            appendLine("Estado: $estado")
            pub.descripcion.takeIf(String::isNotBlank)?.let { appendLine(it) }
            pub.recompensa?.takeIf(String::isNotBlank)?.let { appendLine("Recompensa ofrecida: $it") }
            val latitudAproximada = kotlin.math.round(pub.latitud * 1000.0) / 1000.0
            val longitudAproximada = kotlin.math.round(pub.longitud * 1000.0) / 1000.0
            append("https://maps.google.com/?q=$latitudAproximada,$longitudAproximada")
        }
        prepararPanel(
            etiqueta = when (pub.tipo) {
                "Perdida" -> "PERDIDA"
                "Encontrada" -> "ENCONTRADA"
                else -> "ADOPCIÓN"
            },
            colorEtiqueta = when (pub.tipo) {
                "Perdida" -> R.color.colorRed
                "Encontrada" -> R.color.colorSuccess
                else -> R.color.colorSecondary
            },
            titulo = pub.nombre,
            subtitulo = subtitulo,
            descripcion = buildString {
                append(pub.descripcion)
                pub.recompensa?.takeIf(String::isNotBlank)?.let { append("\n\nRecompensa ofrecida: $it") }
            },
            latitud = pub.latitud,
            longitud = pub.longitud,
            estado = estado,
            colorEstado = colorEstado,
            foto = pub.foto,
            textoCompartir = textoCompartir
        )

        val accionDetalle: () -> Unit = { abrirDetalles(pub) }
        val contactoDisponible = pub.contactoWhatsapp?.any(Char::isDigit) == true && pub.estado != "Adoptada"
        val accionContactar = if (contactoDisponible) {
            "Contactar" to { contactarPublicacion(pub) }
        } else null

        when (pub.tipo) {
            "Perdida" -> {
                val accionAvistamiento = if (pub.estado == "Activa") {
                    "Avisar que la vi" to { abrirFormularioAvistamiento(pub) }
                } else null
                configurarAcciones(accionAvistamiento, "Ver detalles" to accionDetalle)
            }
            "Encontrada" -> configurarAcciones(
                accionContactar,
                "Ver detalles" to accionDetalle
            )
            else -> configurarAcciones(
                accionContactar,
                "Ver detalles" to accionDetalle
            )
        }
        mostrarPanelAnimado()
    }

    private fun showPanelAvist(a: Avistamiento, nom: String) {
        val relacionada = publicacionesActuales.firstOrNull { it.id == a.publicacionId }
        val nombreMascota = valorInformativo(relacionada?.nombre)
            ?: a.especie?.let { "una mascota: $it" }
            ?: nom
        val descripcion = a.descripcion.trim().ifBlank { "El reporte no incluye detalles adicionales." }
        val fechaAvistamiento = a.fecha.takeIf { it > 0L }?.let(::fechaRelativa)
        val textoCompartir = buildString {
            appendLine("Avistamiento de $nombreMascota")
            appendLine(fechaAvistamiento?.let { "Reportado $it" } ?: "Fecha no disponible")
            appendLine(descripcion)
            append("https://maps.google.com/?q=${a.latitud},${a.longitud}")
        }
        prepararPanel(
            etiqueta = "AVISTAMIENTO",
            colorEtiqueta = R.color.ambar_estado,
            titulo = when {
                relacionada != null -> "Vieron a $nombreMascota"
                a.publicacionId > 0L -> "Avistamiento de una alerta cerrada"
                else -> "Avistamiento sin alerta vinculada"
            },
            subtitulo = valorInformativo(relacionada?.especie ?: a.especie)
                ?: "Avistamiento reportado por la comunidad",
            descripcion = descripcion,
            latitud = a.latitud,
            longitud = a.longitud,
            estado = buildString {
                append(when (a.estadoRevision) {
                    "Coincide" -> "Coincide con la alerta"
                    "No coincide" -> "No coincide con la alerta"
                    "Duplicado" -> "Reporte duplicado"
                    else -> if (a.publicacionId == 0L) "Sin alerta relacionada" else "Pendiente de revisión"
                })
                fechaAvistamiento?.let { append(" · Visto $it") }
            },
            colorEstado = R.color.ambar_estado,
            foto = a.foto,
            textoCompartir = textoCompartir
        )
        val accionVerAlerta = a.publicacionId.takeIf { it > 0L }?.let { id ->
            "Ver alerta" to {
                startActivity(Intent(this, DetallePublicacionActivity::class.java).apply {
                    putExtra(DetallePublicacionActivity.EXTRA_PUBLICACION_ID, id)
                })
            }
        }
        configurarAcciones(
            accionVerAlerta,
            "Cómo llegar" to { abrirRuta(a.latitud, a.longitud) }
        )
        mostrarPanelAnimado()
    }

    private fun mostrarDenuncia(d: Denuncia) {
        prepararPanel(
            etiqueta = "REPORTE ANÓNIMO",
            colorEtiqueta = R.color.colorRed,
            titulo = d.motivo,
            subtitulo = "Reporte de la comunidad",
            descripcion = d.descripcion,
            latitud = d.latitud,
            longitud = d.longitud,
            estado = "Reportado ${fechaRelativa(d.fecha)}",
            colorEstado = R.color.colorRed,
            foto = d.foto,
            textoCompartir = null
        )
        mostrarPanelAnimado()
    }

    private fun ocultarPanel() {
        pnl.animate().translationY(pnl.height.toFloat()).alpha(0f).setDuration(200)
            .withEndAction { pnl.visibility = View.GONE }.start()
        textoCompartirActual = null
        circBusq?.let { map.overlays.remove(it) }
        circBusq = null
        map.invalidate()
    }

    private fun abrirDetalles(pub: Publicacion) {
        if (pub.tipo == "Adopcion") {
            startActivity(Intent(this, DetalleAdopcionActivity::class.java).apply {
                putExtra(DetalleAdopcionActivity.EXTRA_PUBLICACION_ID, pub.id)
            })
        } else {
            startActivity(Intent(this, DetallePublicacionActivity::class.java).apply {
                putExtra(DetallePublicacionActivity.EXTRA_PUBLICACION_ID, pub.id)
            })
        }
    }

    private fun abrirFormularioAvistamiento(pub: Publicacion) {
        startActivity(Intent(this, ReportarAvistamientoActivity::class.java).apply {
            putExtra(ReportarAvistamientoActivity.EXTRA_PUBLICACION_ID, pub.id)
            putExtra(ReportarAvistamientoActivity.EXTRA_NOMBRE, pub.nombre)
            putExtra(ReportarAvistamientoActivity.EXTRA_ULTIMA_LATITUD, pub.latitud)
            putExtra(ReportarAvistamientoActivity.EXTRA_ULTIMA_LONGITUD, pub.longitud)
        })
    }

    private fun contactarPublicacion(pub: Publicacion) {
        val telefono = pub.contactoWhatsapp?.filter(Char::isDigit).orEmpty()
        if (telefono.isBlank()) {
            Toast.makeText(this, "Esta alerta no tiene un número de contacto", Toast.LENGTH_SHORT).show()
            return
        }
        val texto = "Hola, te contacto por ${pub.nombre} (${tl(pub.tipo)})."
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(
                "https://wa.me/$telefono?text=${Uri.encode(texto)}"
            )))
        } catch (_: Exception) {
            Toast.makeText(this, "No se pudo abrir WhatsApp", Toast.LENGTH_SHORT).show()
        }
    }

    private fun llamar(telefono: String) {
        try {
            startActivity(Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", telefono, null)))
        } catch (_: Exception) {
            Toast.makeText(this, "No se pudo abrir el marcador", Toast.LENGTH_SHORT).show()
        }
    }

    private fun abrirRuta(latitud: Double, longitud: Double) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q=$latitud,$longitud")))
        } catch (_: Exception) {
            try {
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("geo:$latitud,$longitud?q=$latitud,$longitud")))
            } catch (_: Exception) {
                Toast.makeText(this, "No hay una aplicación de mapas disponible", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun compartir() {
        val texto = textoCompartirActual ?: return
        startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, texto)
        }, "Compartir información"))
    }

    override fun onResume() {
        super.onResume(); if (::map.isInitialized) { map.onResume(); cargarBD() }
    }

    override fun onPause() {
        super.onPause(); cancelarUbicacion(); if (::map.isInitialized) map.onPause()
    }

    override fun onDestroy() {
        cancelarUbicacion()
        if (::map.isInitialized) map.onDetach()
        firebaseListeners.forEach { it.remove() }
        firebaseListeners.clear()
        super.onDestroy(); exec.shutdown()
    }
}
