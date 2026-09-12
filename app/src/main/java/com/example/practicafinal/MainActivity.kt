package com.example.practicafinal

import android.Manifest;
import android.content.Context;
import android.content.Intent
import android.content.pm.PackageManager;
import android.location.Location
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build
import android.os.Bundle
import android.view.View;
import android.widget.TextView;
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.practicafinal.controlador.ControladorAlbergues
import com.example.practicafinal.controlador.ControladorPublicaciones
import com.example.practicafinal.db.DatabaseHelper
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
    private lateinit var pnlBtnIzq: MaterialButton
    private lateinit var pnlBtnDer: MaterialButton
    private var pubActual: Publicacion? = null
    private var marcBusqAvist: MutableList<Polygon> = mutableListOf()
    private lateinit var btnCercanas: TextView
    private lateinit var btnNotificaciones: TextView
    private var pendienteMostrarAlerta: Long? = null
    private val firebaseListeners = mutableListOf<ListenerRegistration>()
    private var listenersAttached = false
    private var publicacionesActuales: List<Publicacion> = emptyList()
    private var avistamientosActuales: List<Avistamiento> = emptyList()
    private var alberguesActuales: List<Albergue> = emptyList()
    private var denunciasActuales: List<Denuncia> = emptyList()

    private val permiso = registerForActivityResult(ActivityResultContracts.RequestPermission()) { g ->
        if (g) centrarUsuario() else {
            Toast.makeText(this, "Permiso denegado", Toast.LENGTH_SHORT).show(); centrarLima()
        }
    }

    private val permisoNotif = registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
        pnlBtnIzq = findViewById(R.id.panel_btn_izq)
        pnlBtnDer = findViewById(R.id.panel_btn_der)
        pnl.findViewById<TextView>(R.id.panel_cerrar)
            .setOnClickListener { ocultarPanel() }; pnl.findViewById<MaterialButton>(R.id.panel_compartir)
            .setOnClickListener { compartir() }
        findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fab_menu).setOnClickListener {
            startActivity(
                Intent(this, MenuOpcionesActivity::class.java)
            )
        }
        findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fab_perfil).setOnClickListener {
            startActivity(
                Intent(this, PerfilActivity::class.java)
            )
        }
        findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fab_recenter).setOnClickListener { centrarUsuario() }
        findViewById<TextView>(R.id.btn_leyenda).setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(this).setTitle("Leyenda del mapa")
                .setView(R.layout.dialog_leyenda).setPositiveButton("Entendido", null).show()
        }
        findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fab_zoom_in).setOnClickListener { map.controller.zoomIn() }
        findViewById<com.google.android.material.floatingactionbutton.FloatingActionButton>(R.id.fab_zoom_out).setOnClickListener { map.controller.zoomOut() }
        btnCercanas = findViewById(R.id.btn_cercanas); btnCercanas.setOnClickListener { dialogoCercanas() }
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
        if (alertaId != -1L) pendienteMostrarAlerta = alertaId
        if (tienePermiso()) centrarUsuario() else permiso.launch(Manifest.permission.ACCESS_FINE_LOCATION)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                permisoNotif.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent);
        val lat = intent.getDoubleExtra("centrar_lat", Double.NaN);
        val lng = intent.getDoubleExtra("centrar_lng", Double.NaN); if (!lat.isNaN() && !lng.isNaN()) {
            map.controller.setZoom(19.5); centrarArriba(GeoPoint(lat, lng))
        }
        val alertaId = intent.getLongExtra("mostrar_alerta", -1L)
        if (alertaId != -1L) pendienteMostrarAlerta = alertaId
    }

    private fun centrarArriba(p: GeoPoint) {
        val b = map.boundingBox; map.controller.animateTo(
            GeoPoint(
                p.latitude - (b.latNorth - b.latSouth) * 0.25,
                p.longitude
            )
        )
    }

    private fun tienePermiso() = ContextCompat.checkSelfPermission(
        this,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    private fun getLastLoc(): Location? {
        val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager; return try {
            lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
        } catch (_: SecurityException) {
            null
        }
    }

    private fun centrarUsuario() {
        val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        var ubicado = false

        val callback = object : android.location.LocationListener {
            override fun onLocationChanged(loc: Location) {
                if (ubicado) return
                ubicado = true
                try { lm.removeUpdates(this) } catch (_: Exception) {}
                userLoc = GeoPoint(loc.latitude, loc.longitude)
                marcUser(userLoc!!); circUser(userLoc!!)
                map.controller.setZoom(19.5); map.controller.setCenter(userLoc)
            }
            @Deprecated("Deprecated in Java")
            override fun onStatusChanged(provider: String?, status: Int, extras: android.os.Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }

        try {
            if (tienePermiso()) {
                if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER))
                    lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500L, 5f, callback, mainLooper)
                if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER))
                    lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000L, 10f, callback, mainLooper)

                android.os.Handler(mainLooper).postDelayed({
                    if (!ubicado) {
                        ubicado = true
                        try { lm.removeUpdates(callback) } catch (_: Exception) {}
                        centrarLima()
                    }
                }, 15000)
            } else {
                centrarLima()
            }
        } catch (_: SecurityException) {
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
        exec.execute {
            try {
                DatabaseHelper(this).seedIfNeeded()
            } catch (error: Exception) {
                runOnUiThread {
                    Toast.makeText(this, "No se pudo sincronizar con Firebase", Toast.LENGTH_LONG).show()
                }
                return@execute
            }
            if (listenersAttached) return@execute
            runOnUiThread {
                if (listenersAttached) return@runOnUiThread
                listenersAttached = true
                val errorHandler: (Exception) -> Unit = {
                    runOnUiThread {
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
        val activas = l.filter { it.estado != "Resuelta" && it.estado != "Adoptada" }
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
        for (av in a) {
            val m = Marker(map); m.position = GeoPoint(av.latitud, av.longitud); m.icon =
                ContextCompat.getDrawable(this, R.drawable.ic_pin_amarillo); m.setAnchor(
                Marker.ANCHOR_CENTER,
                Marker.ANCHOR_BOTTOM
            ); m.relatedObject = av; m.setOnMarkerClickListener { mk, _ ->
                (mk.relatedObject as? Avistamiento)?.let {
                    centrarArriba(mk.position); showPanelAvist(
                    it,
                    n[it.publicacionId] ?: "la mascota"
                )
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

        dibujarBusquedaColaborativa(l, a)

        map.invalidate(); actualizarBtnCercanas(l)
        pendienteMostrarAlerta?.let { id ->
            pendienteMostrarAlerta = null
            l.find { it.id == id }?.let { showPanel(it) }
        }
    }

    private fun mostrarAlbergue(al: Albergue) {
        pnlEmoji.text = "ALBERGUE"
        pnlEmoji.setTextColor(ContextCompat.getColor(this, R.color.colorSecondary))
        pnlEmoji.textSize = 14f
        pnlTit.text = al.nombre
        pnlTipo.text = "Albergue"
        pnlDesc.text = al.descripcion
        pnlUbi.text = al.direccion
        pnlEst.text = ""
        if (!al.foto.isNullOrBlank()) {
            com.example.practicafinal.util.cargarImagen(pnlFoto, al.foto)
            pnlFoto.visibility = View.VISIBLE
        } else {
            pnlFoto.visibility = View.GONE
        }
        pnlBotonesTipo.visibility = View.GONE
        pnl.visibility = View.VISIBLE
        pnl.alpha = 0f
        pnl.post {
            pnl.translationY = pnl.height.toFloat()
            pnl.animate().translationY(0f).alpha(1f).setDuration(250).start()
        }
    }

    private fun actualizarBtnCercanas(l: List<Publicacion>) {
        val u = userLoc; if (u == null) {
            btnCercanas.visibility = View.GONE; return
        };
        val c = l.filter { it.estado != "Resuelta" && it.estado != "Adoptada" }.size; if (c > 0) {
            btnCercanas.visibility = View.VISIBLE; btnCercanas.text = "Alertas cerca ($c)"
        } else btnCercanas.visibility = View.GONE
    }

    private fun dialogoCercanas() {
        val u = userLoc ?: return; exec.execute {
            val pubs =
                ControladorPublicaciones.obtenerPublicaciones(this).filter { it.estado != "Resuelta" && it.estado != "Adoptada" }; data class I(
            val p: Publicacion,
            val km: Double
        );
            val items =
                pubs.map { I(it, GeoPoint(it.latitud, it.longitud).distanceToAsDouble(u) / 1000.0) }.sortedBy { it.km }
                    .take(20); runOnUiThread {
            if (items.isEmpty()) {
                Toast.makeText(this, "No hay alertas cercanas", Toast.LENGTH_SHORT).show(); return@runOnUiThread
            };
            val v = android.view.LayoutInflater.from(this).inflate(R.layout.dialog_cercanas, null);
            val rv =
                v.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.rv_dialog_cercanas); rv.layoutManager =
            LinearLayoutManager(this); rv.adapter = object :
            androidx.recyclerview.widget.RecyclerView.Adapter<androidx.recyclerview.widget.RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(pg: android.view.ViewGroup, vt: Int) = object :
                androidx.recyclerview.widget.RecyclerView.ViewHolder(
                    android.view.LayoutInflater.from(pg.context).inflate(R.layout.item_alerta_cercana, pg, false)
                ) {};

            override fun onBindViewHolder(h: androidx.recyclerview.widget.RecyclerView.ViewHolder, pos: Int) {
                val d = items[pos]; h.itemView.findViewById<View>(R.id.dot).background = when (d.p.tipo) {
                    "Perdida" -> ContextCompat.getDrawable(
                        this@MainActivity,
                        R.drawable.bg_dot_rojo
                    ); "Encontrada" -> ContextCompat.getDrawable(
                        this@MainActivity,
                        R.drawable.bg_dot_verde
                    ); else -> ContextCompat.getDrawable(this@MainActivity, R.drawable.bg_dot_naranja)
                }; (h.itemView.findViewById<TextView>(R.id.tv_info)).text =
                    "${d.p.nombre} . ${String.format(Locale("es"), "%.1f km", d.km)} - ${
                        when (d.p.tipo) {
                            "Perdida" -> "Perdida"; "Encontrada" -> "Encontrada"; else -> "Adopcion"
                        }
                    }"; h.itemView.setOnClickListener { centrarArriba(GeoPoint(d.p.latitud, d.p.longitud)) }
            };
            override fun getItemCount() = items.size
        }; androidx.appcompat.app.AlertDialog.Builder(this).setTitle("Alertas cercanas").setView(v)
            .setPositiveButton("Cerrar", null).show()
        }
        }
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
            val avistDePub = avistamientos.filter { it.publicacionId == pub.id }
            if (avistDePub.size < 2) continue

            val puntos = mutableListOf(GeoPoint(pub.latitud, pub.longitud))
            puntos.addAll(avistDePub.map { GeoPoint(it.latitud, it.longitud) })
            puntos.add(GeoPoint(pub.latitud, pub.longitud))

            val ahora = System.currentTimeMillis()
            for (av in avistDePub) {
                val horasPasadas = (ahora - av.fecha) / (1000.0 * 60 * 60)
                val opacidad = if (horasPasadas < 24) 0x60FFA000 else if (horasPasadas < 72) 0x40FFA000 else 0x20FFA000

                val linea = Polygon()
                linea.points = listOf(
                    GeoPoint(pub.latitud, pub.longitud),
                    GeoPoint(av.latitud, av.longitud)
                )
                linea.fillColor = 0x00000000
                linea.strokeColor = opacidad
                linea.strokeWidth = 3f
                map.overlays.add(linea)
                marcBusqAvist.add(linea)
            }

            val poligono = Polygon()
            poligono.points = puntos
            poligono.fillColor = 0x18FFA000
            poligono.strokeColor = 0x60FFA000
            poligono.strokeWidth = 2f
            map.overlays.add(poligono)
            marcBusqAvist.add(poligono)
        }
    }

    private fun tl(t: String) = when (t) {
        "Perdida" -> "Mascota perdida"; "Encontrada" -> "Mascota encontrada"; else -> "En adopcion"
    }

    private fun showPanel(pub: Publicacion) {
        pubActual = pub
        pnlEmoji.text = when (pub.tipo) {
            "Perdida" -> "PERDIDA"; "Encontrada" -> "ENCONTRADA"; else -> "ADOPCION"
        }
        pnlEmoji.setTextColor(ContextCompat.getColor(this, when (pub.tipo) {
            "Perdida" -> R.color.colorRed; "Encontrada" -> R.color.colorSuccess; else -> R.color.colorSecondary
        }))
        pnlEmoji.textSize = 14f
        pnlTit.text = pub.nombre
        pnlTipo.text = if (pub.tipo == "Perdida" && pub.ultimoLugar != null) "Mascota perdida . Ultima vez: ${pub.ultimoLugar}" else tl(pub.tipo)
        pnlDesc.text = pub.descripcion
        val d = GeoPoint(pub.latitud, pub.longitud).let { userLoc?.distanceToAsDouble(it)?.div(1000.0) }
        pnlUbi.text = if (d != null) "A %.1f km de ti".format(d) else "Lima, Peru"
        val r = pub.estado == "Resuelta"
        val a = pub.estado == "Adoptada"
        pnlEst.text = when {
            r -> "Resuelta . ${fechaRelativa(pub.fechaCreacion)}"
            a -> "Adoptada . ${fechaRelativa(pub.fechaCreacion)}"
            else -> "Activa . ${fechaRelativa(pub.fechaCreacion)}"
        }
        pnlEst.setTextColor(ContextCompat.getColor(this, when {
            r -> android.R.color.darker_gray
            a -> R.color.colorSecondary
            else -> R.color.verde_estado
        }))

        if (!pub.foto.isNullOrBlank()) {
            com.example.practicafinal.util.cargarImagen(pnlFoto, pub.foto)
            pnlFoto.visibility = View.VISIBLE
        } else {
            pnlFoto.setImageResource(android.R.drawable.ic_menu_gallery)
            pnlFoto.visibility = View.VISIBLE
        }

        pnlBotonesTipo.visibility = View.VISIBLE
        pnlBtnDer.visibility = View.GONE

        when (pub.tipo) {
            "Perdida", "Encontrada" -> {
                pnlBtnIzq.text = "Ver detalles"
                pnlBtnIzq.isEnabled = !r
                pnlBtnIzq.setStrokeColorResource(R.color.colorSecondary)
                pnlBtnIzq.setOnClickListener {
                    startActivity(Intent(this, DetallePublicacionActivity::class.java).apply {
                        putExtra(DetallePublicacionActivity.EXTRA_PUBLICACION_ID, pub.id)
                    })
                }
            }
            "Adopcion" -> {
                pnlBtnIzq.text = "WhatsApp"
                pnlBtnIzq.setStrokeColorResource(R.color.colorSuccess)
                pnlBtnIzq.isEnabled = true
                pnlBtnIzq.setOnClickListener { enviarWA() }

                pnlBtnDer.text = "Mas detalles"
                pnlBtnDer.setStrokeColorResource(R.color.colorSecondary)
                pnlBtnDer.visibility = View.VISIBLE
                pnlBtnDer.isEnabled = !a
                pnlBtnDer.setOnClickListener {
                    startActivity(Intent(this, DetalleAdopcionActivity::class.java).apply {
                        putExtra(DetalleAdopcionActivity.EXTRA_PUBLICACION_ID, pub.id)
                    })
                }
            }
        }

        if (r || a) {
            pnlBotonesTipo.visibility = View.GONE
        }

        pnl.visibility = View.VISIBLE
        pnl.alpha = 0f
        pnl.post {
            pnl.translationY = pnl.height.toFloat()
            pnl.animate().translationY(0f).alpha(1f).setDuration(250).start()
        }
    }

    private fun showPanelAvist(a: Avistamiento, nom: String) {
        pubActual = null
        pnlEmoji.text = "AVISTAMIENTO"
        pnlEmoji.setTextColor(ContextCompat.getColor(this, R.color.ambar_estado))
        pnlEmoji.textSize = 14f
        pnlTit.text = "Vieron a $nom"
        pnlTipo.text = "Reportado por la comunidad"
        pnlDesc.text = a.descripcion.ifEmpty { "Alguien reporto haber visto a esta mascota aqui." }
        val d = GeoPoint(a.latitud, a.longitud).let { userLoc?.distanceToAsDouble(it)?.div(1000.0) }
        pnlUbi.text = if (d != null) "A %.1f km de ti".format(d) else "Lima, Peru"
        pnlEst.text = "Avistamiento . ${fechaRelativa(a.fecha)}"
        pnlEst.setTextColor(ContextCompat.getColor(this, R.color.ambar_estado))
        if (!a.foto.isNullOrBlank()) {
            com.example.practicafinal.util.cargarImagen(pnlFoto, a.foto)
            pnlFoto.visibility = View.VISIBLE
        } else {
            pnlFoto.visibility = View.GONE
        }
        pnlBotonesTipo.visibility = View.GONE
        pnl.visibility = View.VISIBLE
        pnl.alpha = 0f
        pnl.post {
            pnl.translationY = pnl.height.toFloat()
            pnl.animate().translationY(0f).alpha(1f).setDuration(250).start()
        }
    }

    private fun mostrarDenuncia(d: Denuncia) {
        pubActual = null
        pnlEmoji.text = ""
        pnlEmoji.visibility = View.GONE
        pnlTit.text = d.motivo
        pnlTipo.text = "Denuncia anonima"
        pnlDesc.text = d.descripcion.ifEmpty { "Reporte enviado por la comunidad." }
        val d2 = GeoPoint(d.latitud, d.longitud).let { userLoc?.distanceToAsDouble(it)?.div(1000.0) }
        pnlUbi.text = if (d2 != null) "A %.1f km de ti".format(d2) else "Lima, Peru"
        pnlEst.text = "Reportado . ${fechaRelativa(d.fecha)}"
        pnlEst.setTextColor(ContextCompat.getColor(this, R.color.colorRed))
        if (!d.foto.isNullOrBlank()) {
            com.example.practicafinal.util.cargarImagen(pnlFoto, d.foto)
            pnlFoto.visibility = View.VISIBLE
        } else {
            pnlFoto.visibility = View.GONE
        }
        pnlBotonesTipo.visibility = View.GONE
        pnl.visibility = View.VISIBLE
        pnl.alpha = 0f
        pnl.post {
            pnl.translationY = pnl.height.toFloat()
            pnl.animate().translationY(0f).alpha(1f).setDuration(250).start()
        }
    }

    private fun ocultarPanel() {
        pnl.animate().translationY(pnl.height.toFloat()).alpha(0f).setDuration(200)
            .withEndAction { pnl.visibility = View.GONE }.start(); pubActual =
            null; circBusq?.let { map.overlays.remove(it) }; circBusq = null; map.invalidate()
    }

    private fun txt(pub: Publicacion) =
        "${pub.nombre} . ${tl(pub.tipo)}\n${pub.descripcion}\nEstado: ${pub.estado} . ${fechaRelativa(pub.fechaCreacion)}\nhttps://maps.google.com/?q=${pub.latitud},${pub.longitud}"

    private fun enviarWA() {
        val p = pubActual ?: return; try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/?text=${Uri.encode(txt(p))}")))
        } catch (_: Exception) {
            Toast.makeText(this, "WhatsApp no instalado", Toast.LENGTH_SHORT).show()
        }
    }

    private fun compartir() {
        val p = pubActual ?: return; startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"; putExtra(Intent.EXTRA_TEXT, txt(p))
        }, "Compartir alerta"))
    }

    override fun onResume() {
        super.onResume(); map.onResume(); cargarBD()
    }

    override fun onPause() {
        super.onPause(); map.onPause()
    }

    override fun onDestroy() {
        firebaseListeners.forEach { it.remove() }
        firebaseListeners.clear()
        super.onDestroy(); exec.shutdown()
    }
}
