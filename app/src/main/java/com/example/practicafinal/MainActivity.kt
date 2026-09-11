package com.example.practicafinal

import android.Manifest;
import android.content.Context;
import android.content.Intent
import android.content.pm.PackageManager;
import android.location.Location
import android.location.LocationManager;
import android.net.Uri;
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
    private lateinit var pnl: View;
    private lateinit var pnlEmoji: TextView;
    private lateinit var pnlTit: TextView
    private lateinit var pnlTipo: TextView;
    private lateinit var pnlDesc: TextView
    private lateinit var pnlUbi: TextView;
    private lateinit var pnlEst: TextView;
    private var pubActual: Publicacion? = null
    private lateinit var btnCercanas: TextView
    private var pendienteMostrarAlerta: Long? = null
    private val firebaseListeners = mutableListOf<ListenerRegistration>()
    private var listenersAttached = false
    private var publicacionesActuales: List<Publicacion> = emptyList()
    private var avistamientosActuales: List<Avistamiento> = emptyList()
    private var alberguesActuales: List<Albergue> = emptyList()

    private val permiso = registerForActivityResult(ActivityResultContracts.RequestPermission()) { g ->
        if (g) centrarUsuario() else {
            Toast.makeText(this, "Permiso denegado", Toast.LENGTH_SHORT).show(); centrarLima()
        }
    }

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
        pnl.findViewById<TextView>(R.id.panel_cerrar)
            .setOnClickListener { ocultarPanel() }; pnl.findViewById<MaterialButton>(R.id.panel_whatsapp)
            .setOnClickListener { enviarWA() }; pnl.findViewById<MaterialButton>(R.id.panel_compartir)
            .setOnClickListener { compartir() }; pnl.findViewById<MaterialButton>(R.id.panel_ver)
            .setOnClickListener { verAvist() }; pnl.findViewById<MaterialButton>(R.id.panel_resolver)
            .setOnClickListener { resolver() }
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
        cargarBD()
        val clat = intent.getDoubleExtra("centrar_lat", Double.NaN);
        val clng = intent.getDoubleExtra("centrar_lng", Double.NaN)
        if (!clat.isNaN() && !clng.isNaN()) {
            map.controller.setZoom(19.5); centrarArriba(GeoPoint(clat, clng))
        }
        val alertaId = intent.getLongExtra("mostrar_alerta", -1L)
        if (alertaId != -1L) pendienteMostrarAlerta = alertaId
        if (tienePermiso()) centrarUsuario() else permiso.launch(Manifest.permission.ACCESS_FINE_LOCATION)
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
                position = p; title = "Mi ubicaciÃ³n"; icon =
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
                            publicacionesActuales.associate { it.id to it.nombre }
                        )
                    },
                    errorHandler
                )
            }
        }
    }
    private fun render(l: List<Publicacion>, a: List<Avistamiento>, alb: List<Albergue>, n: Map<Long, String>) {
        marcPub.forEach { map.overlays.remove(it) }; marcPub.clear(); marcAvist.forEach { map.overlays.remove(it) }; marcAvist.clear(); marcAlb.forEach {
            map.overlays.remove(
                it
            )
        }; marcAlb.clear()
        for (pub in l) {
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
        map.invalidate(); actualizarBtnCercanas(l)
        pendienteMostrarAlerta?.let { id ->
            pendienteMostrarAlerta = null
            l.find { it.id == id }?.let { showPanel(it) }
        }
    }

    private fun mostrarAlbergue(al: Albergue) {
        pnlEmoji.text = "ðŸ "; pnlTit.text = al.nombre; pnlTipo.text = "Albergue"; pnlDesc.text =
            al.descripcion; pnlUbi.text = al.direccion; pnlEst.text =
            ""; pnl.findViewById<MaterialButton>(R.id.panel_ver).isEnabled =
            false; pnl.findViewById<MaterialButton>(R.id.panel_resolver).isEnabled = false
        pnl.visibility = View.VISIBLE; pnl.alpha = 0f; pnl.post {
            pnl.translationY = pnl.height.toFloat(); pnl.animate().translationY(0f).alpha(1f).setDuration(250).start()
        }
    }

    private fun actualizarBtnCercanas(l: List<Publicacion>) {
        val u = userLoc; if (u == null) {
            btnCercanas.visibility = View.GONE; return
        };
        val c = l.filter { it.estado != "Resuelta" }.size; if (c > 0) {
            btnCercanas.visibility = View.VISIBLE; btnCercanas.text = "Alertas cerca ($c)"
        } else btnCercanas.visibility = View.GONE
    }

    private fun dialogoCercanas() {
        val u = userLoc ?: return; exec.execute {
            val pubs =
                ControladorPublicaciones.obtenerPublicaciones(this).filter { it.estado != "Resuelta" }; data class I(
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
                    "${d.p.nombre} Â· ${String.format(Locale("es"), "%.1f km", d.km)} â€” ${
                        when (d.p.tipo) {
                            "Perdida" -> "Perdida"; "Encontrada" -> "Encontrada"; else -> "AdopciÃ³n"
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
        p.estado == "Resuelta" -> R.drawable.ic_pin_gris; p.tipo == "Perdida" -> R.drawable.ic_pin_rojo; p.tipo == "Encontrada" -> R.drawable.ic_pin_verde; else -> R.drawable.ic_pin_naranja
    }

    private fun tl(t: String) = when (t) {
        "Perdida" -> "Mascota perdida"; "Encontrada" -> "Mascota encontrada"; else -> "En adopciÃ³n"
    }

    private fun showPanel(pub: Publicacion) {
        pubActual = pub; pnlEmoji.text = when (pub.tipo) {
            "Perdida" -> "ðŸ¾"; "Encontrada" -> "ðŸ¶"; else -> "ðŸ±"
        }; pnlTit.text = pub.nombre; pnlTipo.text =
            if (pub.tipo == "Perdida" && pub.ultimoLugar != null) "Mascota perdida Â· Ãšltima vez: ${pub.ultimoLugar}" else tl(
                pub.tipo
            ); pnlDesc.text = pub.descripcion;
        val d = GeoPoint(pub.latitud, pub.longitud).let { userLoc?.distanceToAsDouble(it)?.div(1000.0) }; pnlUbi.text =
            if (d != null) "A %.1f km de ti".format(d) else "Lima, PerÃº";
        val r = pub.estado == "Resuelta"; pnlEst.text =
            if (r) "Resuelta Â· ${fechaRelativa(pub.fechaCreacion)}" else "Activa Â· ${fechaRelativa(pub.fechaCreacion)}"; pnlEst.setTextColor(
            ContextCompat.getColor(this, if (r) android.R.color.darker_gray else R.color.verde_estado)
        ); pnl.findViewById<MaterialButton>(R.id.panel_ver).isEnabled =
            !r; pnl.findViewById<MaterialButton>(R.id.panel_resolver).isEnabled = !r; pnl.visibility =
            View.VISIBLE; pnl.alpha = 0f; pnl.post {
            pnl.translationY = pnl.height.toFloat(); pnl.animate().translationY(0f).alpha(1f).setDuration(250).start()
        }
    }

    private fun showPanelAvist(a: Avistamiento, nom: String) {
        pubActual = null; pnlEmoji.text = "ðŸ‘€"; pnlTit.text = "Avistamiento de $nom"; pnlTipo.text =
            "Reportado por la comunidad"; pnlDesc.text =
            a.descripcion.ifEmpty { "Alguien reportÃ³ haber visto a esta mascota aquÃ­." };
        val d = GeoPoint(a.latitud, a.longitud).let { userLoc?.distanceToAsDouble(it)?.div(1000.0) }; pnlUbi.text =
            if (d != null) "A %.1f km de ti".format(d) else "Lima, PerÃº"; pnlEst.text =
            "Avistamiento Â· ${fechaRelativa(a.fecha)}"; pnlEst.setTextColor(
            ContextCompat.getColor(
                this,
                R.color.ambar_estado
            )
        ); pnl.findViewById<MaterialButton>(R.id.panel_ver).isEnabled =
            false; pnl.findViewById<MaterialButton>(R.id.panel_resolver).isEnabled = false; pnl.visibility =
            View.VISIBLE; pnl.alpha = 0f; pnl.post {
            pnl.translationY = pnl.height.toFloat(); pnl.animate().translationY(0f).alpha(1f).setDuration(250).start()
        }
    }

    private fun ocultarPanel() {
        pnl.animate().translationY(pnl.height.toFloat()).alpha(0f).setDuration(200)
            .withEndAction { pnl.visibility = View.GONE }.start(); pubActual =
            null; circBusq?.let { map.overlays.remove(it) }; circBusq = null; map.invalidate()
    }

    private fun verAvist() {
        val p = pubActual ?: return;
        val pt = userLoc ?: getLastLoc()?.let { GeoPoint(it.latitude, it.longitude) }; if (pt == null) {
            Toast.makeText(this, "No tenemos tu ubicaciÃ³n", Toast.LENGTH_SHORT).show(); return
        }; exec.execute {
            ControladorPublicaciones.actualizarAvistamiento(this, p.id, pt.latitude, pt.longitude); runOnUiThread {
            Toast.makeText(this, "Â¡Gracias!", Toast.LENGTH_SHORT).show(); ocultarPanel(); cargarBD()
        }
        }
    }

    private fun resolver() {
        val p = pubActual ?: return; exec.execute {
            ControladorPublicaciones.resolver(this, p.id); runOnUiThread {
            Toast.makeText(
                this,
                "Â¡Resuelta!",
                Toast.LENGTH_SHORT
            ).show(); ocultarPanel(); cargarBD()
        }
        }
    }

    private fun txt(pub: Publicacion) =
        "${pub.nombre} Â· ${tl(pub.tipo)}\n${pub.descripcion}\nEstado: ${pub.estado} Â· ${fechaRelativa(pub.fechaCreacion)}\nhttps://maps.google.com/?q=${pub.latitud},${pub.longitud}"

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
