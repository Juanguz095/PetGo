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
import com.example.practicafinal.util.ejecutarSeguro
import com.example.practicafinal.util.actualizarUi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.practicafinal.controlador.ControladorAlbergues
import com.example.practicafinal.modelo.Albergue
import org.osmdroid.util.GeoPoint
import java.util.Locale
import java.util.concurrent.Executors

class AlberguesActivity : AppCompatActivity() {

    private val exec = Executors.newSingleThreadExecutor()
    private lateinit var rv: RecyclerView;
    private lateinit var tvVacio: TextView;
    private lateinit var etBuscar: EditText
    private var albergues: List<Albergue> = emptyList();
    private var userLoc: GeoPoint? = null
    private var ordenPorCercanos = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_albergues)
        findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }
        rv = findViewById(R.id.rv_albergues); tvVacio = findViewById(R.id.tv_vacio); etBuscar =
            findViewById(R.id.et_buscar)
        rv.layoutManager = LinearLayoutManager(this)
        etBuscar.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) = mostrarLista()
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        val chipCercanos = findViewById<TextView>(R.id.chip_cercanos)
        val chipAz = findViewById<TextView>(R.id.chip_az)
        chipCercanos.setOnClickListener {
            ordenPorCercanos = true
            chipCercanos.background = ContextCompat.getDrawable(this, R.drawable.bg_chip_selected)
            chipCercanos.setTextColor(ContextCompat.getColor(this, R.color.tinta))
            chipAz.background = ContextCompat.getDrawable(this, R.drawable.bg_chip); chipAz.setTextColor(
            ContextCompat.getColor(
                this,
                R.color.texto_suave
            )
        ); mostrarLista()
        }
        chipAz.setOnClickListener {
            ordenPorCercanos = false
            chipAz.background = ContextCompat.getDrawable(this, R.drawable.bg_chip_selected)
            chipAz.setTextColor(ContextCompat.getColor(this, R.color.tinta))
            chipCercanos.background = ContextCompat.getDrawable(this, R.drawable.bg_chip); chipCercanos.setTextColor(
            ContextCompat.getColor(this, R.color.texto_suave)
        ); mostrarLista()
        }

        cargar()
    }

    private fun obtenerUbicacion(): GeoPoint? =
        com.example.practicafinal.util.ultimaUbicacion(this)?.let { GeoPoint(it.latitude, it.longitude) }

    private fun cargar() {
        ejecutarSeguro(exec) {
            val lista = ControladorAlbergues.obtenerAlbergues(this)
            val ubicacion = obtenerUbicacion()
            actualizarUi { albergues = lista; userLoc = ubicacion; mostrarLista() }
        }
    }

    private fun mostrarLista() {
        val texto = etBuscar.text.toString().trim().lowercase()
        val filtrados =
            albergues.filter { it.nombre.lowercase().contains(texto) || it.direccion.lowercase().contains(texto) }
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
