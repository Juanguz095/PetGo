package com.example.practicafinal

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.practicafinal.db.DatabaseHelper
import com.example.practicafinal.util.fechaRelativa
import com.google.android.material.appbar.MaterialToolbar
import java.util.concurrent.Executors

class NotificacionesActivity : AppCompatActivity() {

    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var rvNotificaciones: RecyclerView
    private lateinit var tvVacio: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notificaciones)

        findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }

        rvNotificaciones = findViewById(R.id.rv_notificaciones)
        tvVacio = findViewById(R.id.tv_vacio)
        rvNotificaciones.layoutManager = LinearLayoutManager(this)

        findViewById<TextView>(R.id.tv_marcar_leidas).setOnClickListener {
            executor.execute {
                DatabaseHelper(this).marcarTodasLeidas()
                runOnUiThread { cargarNotificaciones() }
            }
        }

        cargarNotificaciones()
    }

    private fun cargarNotificaciones() {
        executor.execute {
            val notifs = DatabaseHelper(this).obtenerNotificaciones()
            runOnUiThread {
                if (notifs.isEmpty()) {
                    tvVacio.visibility = View.VISIBLE
                    rvNotificaciones.visibility = View.GONE
                } else {
                    tvVacio.visibility = View.GONE
                    rvNotificaciones.visibility = View.VISIBLE
                    rvNotificaciones.adapter = AdaptadorNotificacion(notifs) { notif ->
                        val pubId = (notif["publicacionId"] as? String)?.toLongOrNull()
                        if (pubId != null) {
                            executor.execute {
                                DatabaseHelper(this).marcarNotificacionLeida(notif["id"] as String)
                            }
                            val intent = Intent(this, MainActivity::class.java)
                            intent.putExtra("mostrar_alerta", pubId)
                            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                            startActivity(intent)
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        cargarNotificaciones()
    }

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdown()
    }

    private class AdaptadorNotificacion(
        private val items: List<Map<String, Any>>,
        private val onClick: (Map<String, Any>) -> Unit
    ) : RecyclerView.Adapter<AdaptadorNotificacion.VH>() {

        class VH(view: View) : RecyclerView.ViewHolder(view) {
            val dot: View = view.findViewById(R.id.dot_estado)
            val tvMensaje: TextView = view.findViewById(R.id.tv_mensaje)
            val tvTipo: TextView = view.findViewById(R.id.tv_tipo)
            val tvFecha: TextView = view.findViewById(R.id.tv_fecha)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_notificacion, parent, false)
            return VH(view)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val notif = items[position]
            val leida = notif["leida"] as? Boolean ?: false

            holder.tvMensaje.text = notif["mensaje"] as? String ?: ""
            val tipo = notif["tipo"] as? String ?: ""
            holder.tvTipo.text = when (tipo) {
                "avistamiento" -> "Avistamiento de mascota"
                else -> tipo
            }
            val fecha = notif["fecha"] as? Long ?: 0L
            holder.tvFecha.text = fechaRelativa(fecha)

            holder.dot.background = ContextCompat.getDrawable(
                holder.itemView.context,
                if (leida) R.drawable.bg_dot_gris else R.drawable.bg_circle_azul
            )

            if (!leida) {
                holder.itemView.alpha = 1.0f
            } else {
                holder.itemView.alpha = 0.6f
            }

            holder.itemView.setOnClickListener { onClick(notif) }
        }

        override fun getItemCount() = items.size
    }
}
