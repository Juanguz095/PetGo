package com.example.practicafinal

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.practicafinal.controlador.ControladorPublicaciones
import com.example.practicafinal.util.actualizarUi
import com.example.practicafinal.util.ejecutarSeguro
import com.example.practicafinal.util.fechaRelativa
import java.util.concurrent.Executors

class SolicitudesAdopcionActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_PUBLICACION_ID = "solicitudes_adopcion_publicacion_id"
    }

    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var recycler: RecyclerView
    private lateinit var tvVacio: TextView
    private lateinit var tvEstadoProceso: TextView
    private var publicacionId = -1L
    private var nombreMascota = "la mascota"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_solicitudes_adopcion)
        findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
            .setNavigationOnClickListener { finish() }
        publicacionId = intent.getLongExtra(EXTRA_PUBLICACION_ID, -1L)
        if (publicacionId <= 0L) {
            finish()
            return
        }
        recycler = findViewById(R.id.rv_solicitudes)
        tvVacio = findViewById(R.id.tv_vacio)
        tvEstadoProceso = findViewById(R.id.tv_estado_proceso)
        recycler.layoutManager = LinearLayoutManager(this)
    }

    private fun cargar() {
        ejecutarSeguro(executor) {
            try {
                val publicacion = ControladorPublicaciones.obtenerPorId(this, publicacionId)
                    ?: error("No se encontró la publicación")
                val solicitudes = ControladorPublicaciones.obtenerSolicitudesAdopcion(this, publicacionId)
                actualizarUi {
                    nombreMascota = publicacion.nombre
                    val cerrada = publicacion.estado == "Adoptada" || publicacion.estado != "Activa"
                    tvEstadoProceso.text = when (publicacion.estado) {
                        "Adoptada" -> "La adopción de ${publicacion.nombre} se marcó como concretada."
                        "Activa" -> "Revisa cada solicitud y continúa la conversación con la persona interesada."
                        else -> "Esta publicación ya no acepta solicitudes nuevas."
                    }
                    tvVacio.visibility = if (solicitudes.isEmpty()) View.VISIBLE else View.GONE
                    recycler.visibility = if (solicitudes.isEmpty()) View.GONE else View.VISIBLE
                    recycler.adapter = SolicitudesAdapter(
                        solicitudes,
                        cerrada,
                        onConversacion = { solicitud -> cambiarEstado(solicitud, "En conversación") },
                        onCerrar = { solicitud -> confirmarCierre(solicitud) },
                        onContactar = { solicitud -> contactar(solicitud) }
                    )
                }
            } catch (error: Exception) {
                android.util.Log.e("PetGo", "No se pudieron cargar las solicitudes de adopción", error)
                actualizarUi {
                    tvVacio.text = error.message ?: "No se pudieron cargar las solicitudes."
                    tvVacio.visibility = View.VISIBLE
                    recycler.visibility = View.GONE
                }
            }
        }
    }

    private fun cambiarEstado(solicitud: Map<String, Any>, estado: String) {
        ejecutarSeguro(executor) {
            try {
                ControladorPublicaciones.actualizarEstadoSolicitudAdopcion(
                    this, solicitud["id"].toString(), estado
                )
                actualizarUi {
                    Toast.makeText(this, "Solicitud actualizada", Toast.LENGTH_SHORT).show()
                    cargar()
                }
            } catch (error: Exception) {
                actualizarUi {
                    Toast.makeText(this, error.message ?: "No se pudo actualizar", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun confirmarCierre(solicitud: Map<String, Any>) {
        AlertDialog.Builder(this)
            .setTitle("Cerrar solicitud")
            .setMessage("La persona interesada verá que no continuarás con esta solicitud.")
            .setNegativeButton("Volver", null)
            .setPositiveButton("Cerrar solicitud") { _, _ -> cambiarEstado(solicitud, "No seleccionado") }
            .show()
            .also { com.example.practicafinal.util.Dialogos.aplicar(this, it) }
    }

    private fun contactar(solicitud: Map<String, Any>) {
        val phone = solicitud["contactoWhatsapp"].toString().filter(Char::isDigit)
        if (!phone.matches(Regex("^(51)?[0-9]{9}$"))) return
        val numero = if (phone.startsWith("51")) phone else "51$phone"
        val texto = "Hola, te escribo por tu solicitud de adopción de $nombreMascota en PetGo."
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$numero?text=${Uri.encode(texto)}")))
        } catch (_: Exception) {
            Toast.makeText(this, "No se pudo abrir WhatsApp", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        if (::recycler.isInitialized) cargar()
    }

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdown()
    }

    private class SolicitudesAdapter(
        private val items: List<Map<String, Any>>,
        private val procesoCerrado: Boolean,
        private val onConversacion: (Map<String, Any>) -> Unit,
        private val onCerrar: (Map<String, Any>) -> Unit,
        private val onContactar: (Map<String, Any>) -> Unit
    ) : RecyclerView.Adapter<SolicitudesAdapter.ViewHolder>() {

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val estado: TextView = view.findViewById(R.id.tv_estado)
            val fecha: TextView = view.findViewById(R.id.tv_fecha)
            val mensaje: TextView = view.findViewById(R.id.tv_mensaje)
            val contactar: com.google.android.material.button.MaterialButton = view.findViewById(R.id.btn_contactar)
            val conversacion: com.google.android.material.button.MaterialButton = view.findViewById(R.id.btn_conversacion)
            val cerrar: com.google.android.material.button.MaterialButton = view.findViewById(R.id.btn_cerrar)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = ViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_solicitud_adopcion, parent, false)
        )

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val solicitud = items[position]
            val estadoActual = solicitud["estado"]?.toString().orEmpty()
            val estadoVisible = if (procesoCerrado) "Adopción completada" else estadoActual
            holder.estado.text = "Estado: $estadoVisible"
            holder.fecha.text = "Recibida ${fechaRelativa(solicitud["fecha"] as? Long ?: 0L)}"
            holder.mensaje.text = solicitud["mensaje"]?.toString()?.takeIf { it.isNotBlank() }
                ?: "No agregó un mensaje."

            val abierta = !procesoCerrado && estadoActual in setOf("Pendiente", "En conversación")
            val telefono = solicitud["contactoWhatsapp"]?.toString().orEmpty()
            holder.contactar.visibility = if (abierta && telefono.isNotBlank()) View.VISIBLE else View.GONE
            holder.conversacion.visibility = if (abierta && estadoActual == "Pendiente") View.VISIBLE else View.GONE
            holder.cerrar.visibility = if (abierta) View.VISIBLE else View.GONE
            holder.contactar.setOnClickListener { onContactar(solicitud) }
            holder.conversacion.setOnClickListener { onConversacion(solicitud) }
            holder.cerrar.setOnClickListener { onCerrar(solicitud) }
        }

        override fun getItemCount(): Int = items.size
    }
}
