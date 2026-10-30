package com.example.practicafinal

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.example.practicafinal.util.ejecutarSeguro
import com.example.practicafinal.util.actualizarUi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.practicafinal.controlador.ControladorAlbergues
import com.example.practicafinal.modelo.Albergue
import com.google.firebase.firestore.DocumentSnapshot
import org.osmdroid.util.GeoPoint
import java.util.Locale
import java.util.concurrent.Executors

class AlberguesActivity : AppCompatActivity() {

    private val exec = Executors.newSingleThreadExecutor()
    private lateinit var rv: RecyclerView;
    private lateinit var tvVacio: TextView;
    private lateinit var etBuscar: EditText
    private lateinit var chipCercanos: TextView
    private lateinit var chipAz: TextView
    private lateinit var btnRadio: TextView
    private var albergues: List<Albergue> = emptyList();
    private var userLoc: GeoPoint? = null
    private var radioKm: Double? = null
    private var ordenPorCercanos = true
    private var cursorPagina: DocumentSnapshot? = null
    private var hayMas = false
    private var cargando = false
    private lateinit var btnCargarMas: com.google.android.material.button.MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_albergues)
        findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }
        rv = findViewById(R.id.rv_albergues); tvVacio = findViewById(R.id.tv_vacio); etBuscar =
            findViewById(R.id.et_buscar)
        rv.layoutManager = LinearLayoutManager(this)
        btnCargarMas = findViewById(R.id.btn_cargar_mas)
        btnCargarMas.setOnClickListener { cargar(reset = false) }
        etBuscar.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = mostrarLista()
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        chipCercanos = findViewById(R.id.chip_cercanos)
        chipAz = findViewById(R.id.chip_az)
        btnRadio = findViewById(R.id.btn_radio)
        btnRadio.setOnClickListener { mostrarMenuRadio() }
        chipCercanos.setOnClickListener {
            if (userLoc == null) {
                Toast.makeText(this, "No hay una ubicación reciente; se mostrarán por nombre", Toast.LENGTH_LONG).show()
                seleccionarOrden(false)
            } else seleccionarOrden(true)
        }
        chipAz.setOnClickListener { seleccionarOrden(false) }

        cargar()
    }

    private fun obtenerUbicacion(): GeoPoint? =
        com.example.practicafinal.util.ultimaUbicacion(this)?.let { GeoPoint(it.latitude, it.longitude) }

    private fun seleccionarOrden(cercanos: Boolean) {
        ordenPorCercanos = cercanos
        chipCercanos.background = ContextCompat.getDrawable(
            this,
            if (cercanos) R.drawable.bg_chip_selected else R.drawable.bg_chip
        )
        chipCercanos.setTextColor(
            ContextCompat.getColor(this, if (cercanos) R.color.tinta else R.color.texto_suave)
        )
        chipAz.background = ContextCompat.getDrawable(
            this,
            if (cercanos) R.drawable.bg_chip else R.drawable.bg_chip_selected
        )
        chipAz.setTextColor(
            ContextCompat.getColor(this, if (cercanos) R.color.texto_suave else R.color.tinta)
        )
        mostrarLista()
    }

    private fun mostrarMenuRadio() {
        if (userLoc == null) {
            Toast.makeText(this, "Necesitas una ubicación reciente para filtrar por distancia", Toast.LENGTH_LONG).show()
            return
        }
        androidx.appcompat.widget.PopupMenu(this, btnRadio).apply {
            listOf("Todas las distancias" to null, "Hasta 5 km" to 5.0, "Hasta 10 km" to 10.0, "Hasta 25 km" to 25.0)
                .forEach { (label, value) ->
                    menu.add(label).setOnMenuItemClickListener {
                        radioKm = value
                        btnRadio.text = if (value == null) "Radio: todas" else "Radio: ${value.toInt()} km"
                        mostrarLista()
                        true
                    }
                }
            show()
        }
    }

    private fun cargar(reset: Boolean = true) {
        if (cargando) return
        if (!reset && !hayMas) return
        if (reset) {
            cursorPagina = null
            albergues = emptyList()
        }
        cargando = true
        btnCargarMas.isEnabled = false
        ejecutarSeguro(exec) {
            try {
                val pagina = ControladorAlbergues.obtenerPagina(this, cursorPagina)
                val ubicacion = if (reset) obtenerUbicacion() else userLoc
                actualizarUi {
                    cursorPagina = pagina.cursor
                    hayMas = pagina.hayMas
                    cargando = false
                    btnCargarMas.isEnabled = true
                    btnCargarMas.visibility = if (hayMas) View.VISIBLE else View.GONE
                    albergues = (if (reset) emptyList() else albergues) + pagina.items
                    userLoc = ubicacion
                    if (ubicacion == null && ordenPorCercanos) {
                        ordenPorCercanos = false
                        Toast.makeText(this, "No hay una ubicación reciente; se mostrarán por nombre", Toast.LENGTH_LONG).show()
                    }
                    seleccionarOrden(ordenPorCercanos)
                }
            } catch (error: Exception) {
                actualizarUi { cargando = false; btnCargarMas.isEnabled = true }
                throw error
            }
        }
    }

    private fun mostrarLista() {
        val texto = etBuscar.text.toString().trim().lowercase()
        val filtrados =
            albergues.filter { it.nombre.lowercase().contains(texto) || it.direccion.lowercase().contains(texto) }
                .filter { albergue ->
                    val radio = radioKm
                    val ubicacion = userLoc
                    radio == null || (ubicacion != null && GeoPoint(albergue.latitud, albergue.longitud)
                        .distanceToAsDouble(ubicacion) <= radio * 1000.0)
                }
                .sortedWith(if (ordenPorCercanos && userLoc != null) compareBy {
                    GeoPoint(
                        it.latitud,
                        it.longitud
                    ).distanceToAsDouble(userLoc!!)
                } else compareBy { it.nombre.lowercase() })

        tvVacio.visibility = if (filtrados.isEmpty()) View.VISIBLE else View.GONE
        rv.adapter = object : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
            override fun onCreateViewHolder(p: ViewGroup, v: Int) = object : RecyclerView.ViewHolder(
                LayoutInflater.from(p.context).inflate(R.layout.item_albergue_card, p, false)
            ) {}

            override fun onBindViewHolder(h: RecyclerView.ViewHolder, pos: Int) {
                val a = filtrados[pos]
                val img = h.itemView.findViewById<android.widget.ImageView>(R.id.img_foto)
                val pl = h.itemView.findViewById<TextView>(R.id.tv_placeholder)
                if (!a.foto.isNullOrBlank()) {
                    com.example.practicafinal.util.cargarImagen(img, a.foto)
                    pl.visibility = View.GONE
                } else {
                    com.example.practicafinal.util.limpiarImagen(img)
                    pl.visibility = View.VISIBLE
                }
                (h.itemView.findViewById<TextView>(R.id.tv_nombre)).text = a.nombre
                (h.itemView.findViewById<TextView>(R.id.tv_direccion)).text = a.direccion
                (h.itemView.findViewById<TextView>(R.id.tv_telefono)).text = "📞 ${a.telefono}"
                if (userLoc != null) (h.itemView.findViewById<TextView>(R.id.tv_distancia)).text = String.format(
                    Locale("es"),
                    "A %.1f km",
                    GeoPoint(a.latitud, a.longitud).distanceToAsDouble(userLoc!!) / 1000.0
                )
                else h.itemView.findViewById<TextView>(R.id.tv_distancia).text = ""
                h.itemView.setOnClickListener {
                    startActivity(
                        Intent(
                            this@AlberguesActivity,
                            DetalleAlbergueActivity::class.java
                        ).apply { putExtra("albergue_id", a.id) })
                }
                h.itemView.findViewById<android.widget.ImageView>(R.id.btn_llamar)
                    .setOnClickListener { startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${a.telefono}"))) }
            }

            override fun getItemCount() = filtrados.size
        }
    }

    override fun onDestroy() {
        super.onDestroy(); exec.shutdown()
    }
}
