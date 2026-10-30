package com.example.practicafinal

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.practicafinal.util.ejecutarSeguro
import com.example.practicafinal.util.actualizarUi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.practicafinal.controlador.ControladorPublicaciones
import com.example.practicafinal.modelo.Publicacion
import com.example.practicafinal.modelo.Avistamiento
import com.example.practicafinal.session.SesionManager
import java.util.concurrent.Executors

class DetallePublicacionActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PUBLICACION_ID = "detalle_publicacion_id"
    }

    private val executor = Executors.newSingleThreadExecutor()
    private var publicacionId: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalle_publicacion)

        findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
            .setNavigationOnClickListener { finish() }

        publicacionId = intent.getLongExtra(EXTRA_PUBLICACION_ID, -1L)
        if (publicacionId == -1L) { finish(); return }

        cargarPublicacion()
    }

    private fun cargarPublicacion() {
        ejecutarSeguro(executor) {
            val pub = ControladorPublicaciones.obtenerPorId(this, publicacionId)
            val avistamientos = if (pub?.tipo == "Perdida") {
                ControladorPublicaciones.obtenerAvistamientosPorPublicacion(this, publicacionId)
            } else emptyList()
            actualizarUi {
                if (pub == null) { finish(); return@actualizarUi }
                mostrar(pub, avistamientos)
            }
        }
    }

    private fun mostrar(p: Publicacion, avistamientos: List<Avistamiento>) {
        val imgFoto = findViewById<android.widget.ImageView>(R.id.img_foto)
        val tvPlaceholder = findViewById<TextView>(R.id.tv_placeholder)
        if (!p.foto.isNullOrBlank()) {
            com.example.practicafinal.util.cargarImagen(imgFoto, p.foto)
            tvPlaceholder.visibility = View.GONE
        } else {
            tvPlaceholder.visibility = View.VISIBLE
            tvPlaceholder.text = when {
                p.especie.equals("Perro", ignoreCase = true) -> "\uD83D\uDC36"
                p.especie.equals("Gato", ignoreCase = true) -> "\uD83D\uDC31"
                else -> "\uD83D\uDC3E"
            }
        }

        val tvEstado = findViewById<TextView>(R.id.tv_estado)
        when (p.estado) {
            "Resuelta" -> { tvEstado.text = if (p.tipo == "Perdida") "Encontrada" else "Resuelta"; tvEstado.setTextColor(ContextCompat.getColor(this, R.color.texto_suave)) }
            "Adoptada" -> { tvEstado.text = "Adoptada"; tvEstado.setTextColor(ContextCompat.getColor(this, R.color.colorSecondary)) }
            else -> { tvEstado.text = "Activa"; tvEstado.setTextColor(ContextCompat.getColor(this, R.color.verde_estado)) }
        }

        findViewById<TextView>(R.id.tv_nombre).text = p.nombre
        findViewById<TextView>(R.id.tv_tipo).text = when (p.tipo) {
            "Perdida" -> "Mascota perdida"
            "Encontrada" -> "Mascota encontrada"
            else -> "En adopcion"
        }
        findViewById<TextView>(R.id.tv_descripcion).text = p.descripcion
        findViewById<TextView>(R.id.tv_detalles_mascota).apply {
            val detalles = listOfNotNull(
                p.color?.takeIf(String::isNotBlank)?.let { "Color: $it" },
                p.tamano?.takeIf(String::isNotBlank)?.let { "Tamaño: $it" },
                p.senasParticulares?.takeIf(String::isNotBlank)?.let { "Señas: $it" }
            )
            text = detalles.joinToString("\n")
            visibility = if (detalles.isEmpty()) View.GONE else View.VISIBLE
            contentDescription = if (detalles.isEmpty()) null else "Datos para reconocer a la mascota. ${detalles.joinToString(". ")}"
        }

        val ultimaActividad = maxOf(p.fechaCreacion, p.ultimaConfirmacion ?: 0L)
        val ultimoAvistamiento = avistamientos.maxOfOrNull { it.fecha }
        findViewById<TextView>(R.id.tv_resumen_actividad).text = buildString {
            append("Alerta actualizada ${com.example.practicafinal.util.fechaRelativa(ultimaActividad)}.")
            if (p.tipo == "Perdida") {
                if (ultimoAvistamiento != null) {
                    append(" Último avistamiento ${com.example.practicafinal.util.fechaRelativa(ultimoAvistamiento)}.")
                } else {
                    append(" Aún no hay avistamientos vinculados.")
                }
            }
        }

        findViewById<TextView>(R.id.tv_fecha).text = com.example.practicafinal.util.fechaRelativa(p.fechaCreacion)
        findViewById<TextView>(R.id.tv_fecha_perdida).apply {
            if (p.tipo == "Perdida" && p.fechaPerdida != null) {
                text = "Última vez vista: ${java.text.SimpleDateFormat("dd/MM/yyyy, HH:mm", java.util.Locale.forLanguageTag("es-PE")).format(java.util.Date(p.fechaPerdida))}"
                visibility = View.VISIBLE
            } else visibility = View.GONE
        }
        findViewById<TextView>(R.id.tv_recompensa).apply {
            text = "🎁 Recompensa ofrecida: ${p.recompensa.orEmpty()}"
            visibility = if (p.tipo == "Perdida" && !p.recompensa.isNullOrBlank()) View.VISIBLE else View.GONE
        }
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_historial_avistamientos).apply {
            visibility = if (p.tipo == "Perdida") View.VISIBLE else View.GONE
            text = "Ver historial de avistamientos (${avistamientos.size})"
            setOnClickListener { mostrarHistorial(p, avistamientos) }
        }

        findViewById<TextView>(R.id.tv_ubicacion).apply {
            val esPropietario = p.usuarioId == SesionManager.obtenerUsuarioId(this@DetallePublicacionActivity)
            text = if (esPropietario) "Ver ubicación en el mapa" else "Ver zona aproximada en el mapa"
            contentDescription = text
            setOnClickListener {
                ejecutarSeguro(executor) {
                    val exactas = if (esPropietario) runCatching {
                        ControladorPublicaciones.obtenerCoordenadasPublicacion(this@DetallePublicacionActivity, p.id)
                    }.getOrNull() else null
                    val latitud = exactas?.first ?: p.latitud
                    val longitud = exactas?.second ?: p.longitud
                    actualizarUi {
                        startActivity(Intent(this@DetallePublicacionActivity, MainActivity::class.java).apply {
                            putExtra("centrar_lat", latitud)
                            putExtra("centrar_lng", longitud)
                            putExtra("mostrar_alerta", publicacionId)
                            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        })
                    }
                }
            }
        }

        val btnAvist = findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_avistamiento)
        if (p.tipo == "Perdida" && p.estado == "Activa") {
            btnAvist.visibility = View.VISIBLE
            btnAvist.setOnClickListener {
                startActivity(Intent(this, ReportarAvistamientoActivity::class.java).apply {
                    putExtra(ReportarAvistamientoActivity.EXTRA_PUBLICACION_ID, p.id)
                    putExtra(ReportarAvistamientoActivity.EXTRA_NOMBRE, p.nombre)
                    putExtra(ReportarAvistamientoActivity.EXTRA_ULTIMA_LATITUD, p.latitud)
                    putExtra(ReportarAvistamientoActivity.EXTRA_ULTIMA_LONGITUD, p.longitud)
                    putExtra(ReportarAvistamientoActivity.EXTRA_ESPECIE, p.especie)
                })
            }
        } else {
            btnAvist.visibility = View.GONE
        }

        findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_contactar).apply {
            visibility = if (!p.contactoWhatsapp.isNullOrBlank()) View.VISIBLE else View.GONE
            setOnClickListener { abrirWhatsApp(p) }
        }

        findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_compartir).setOnClickListener {
            val texto = buildString {
                appendLine("${p.nombre} · ${tl(p.tipo)}")
                p.fechaPerdida?.takeIf { p.tipo == "Perdida" }?.let {
                    appendLine("Última vez vista: ${java.text.SimpleDateFormat("dd/MM/yyyy, HH:mm", java.util.Locale.forLanguageTag("es-PE")).format(java.util.Date(it))}")
                }
                p.ultimoLugar?.takeIf(String::isNotBlank)?.let { appendLine("Última zona: $it") }
                p.recompensa?.takeIf(String::isNotBlank)?.let { appendLine("Recompensa: $it") }
                appendLine("Estado: ${p.estado}")
                appendLine(p.descripcion)
                val latitudAproximada = kotlin.math.round(p.latitud * 1000.0) / 1000.0
                val longitudAproximada = kotlin.math.round(p.longitud * 1000.0) / 1000.0
                append("https://maps.google.com/?q=$latitudAproximada,$longitudAproximada")
            }
            startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"; putExtra(Intent.EXTRA_TEXT, texto)
            }, "Compartir alerta"))
        }
    }

    private fun tl(t: String) = when (t) {
        "Perdida" -> "Mascota perdida"; "Encontrada" -> "Mascota encontrada"; else -> "En adopcion"
    }

    private fun mostrarHistorial(publicacion: Publicacion, reportes: List<Avistamiento>) {
        val vista = LayoutInflater.from(this).inflate(R.layout.dialog_avistamientos, null)
        val lista = vista.findViewById<RecyclerView>(R.id.rv_avistamientos)
        val vacio = vista.findViewById<TextView>(R.id.tv_vacio_avist)
        val ordenados = reportes.sortedByDescending { it.fecha }
        val esPropietario = publicacion.usuarioId == SesionManager.obtenerUsuarioId(this)
        lista.layoutManager = LinearLayoutManager(this)
        vacio.visibility = if (ordenados.isEmpty()) View.VISIBLE else View.GONE
        lista.visibility = if (ordenados.isEmpty()) View.GONE else View.VISIBLE
        lista.adapter = object : RecyclerView.Adapter<HistorialViewHolder>() {
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = HistorialViewHolder(
                LayoutInflater.from(parent.context).inflate(R.layout.item_avistamiento_row, parent, false)
            )

            override fun onBindViewHolder(holder: HistorialViewHolder, position: Int) {
                val reporte = ordenados[position]
                holder.fecha.text = "Visto ${com.example.practicafinal.util.fechaRelativa(reporte.fecha)}"
                holder.descripcion.text = buildString {
                    reporte.especie?.let { append("$it · ") }
                    append(reporte.descripcion.ifBlank { "Sin detalles adicionales" })
                }
                holder.estado.text = when (reporte.estadoRevision) {
                    "Coincide" -> "El dueño confirmó que coincide"
                    "No coincide" -> "El dueño indicó que no coincide"
                    "Duplicado" -> "Reporte marcado como duplicado"
                    else -> "Pendiente de revisión del dueño"
                }
                holder.mapa.text = "Ver punto en el mapa →"
                if (reporte.foto.isNullOrBlank()) holder.foto.visibility = View.GONE else {
                    holder.foto.visibility = View.VISIBLE
                    com.example.practicafinal.util.cargarImagen(holder.foto, reporte.foto)
                }
                holder.revision.visibility = if (esPropietario) View.VISIBLE else View.GONE
                holder.revision.text = "Revisar"
                holder.revision.setOnClickListener {
                    val opciones = arrayOf("Coincide con mi mascota", "No coincide", "Es un duplicado", "Quitar revisión")
                    AlertDialog.Builder(this@DetallePublicacionActivity)
                        .setTitle("Revisar avistamiento")
                        .setItems(opciones) { _, indice ->
                            val estado = when (indice) {
                                0 -> "Coincide"
                                1 -> "No coincide"
                                2 -> "Duplicado"
                                else -> "Pendiente"
                            }
                            ejecutarSeguro(executor) {
                                try {
                                    ControladorPublicaciones.actualizarRevisionAvistamiento(
                                        this@DetallePublicacionActivity, reporte.id, estado
                                    )
                                    actualizarUi { cargarPublicacion() }
                                } catch (error: Exception) {
                                    actualizarUi {
                                        android.widget.Toast.makeText(
                                            this@DetallePublicacionActivity,
                                            error.message ?: "No se pudo actualizar el reporte",
                                            android.widget.Toast.LENGTH_LONG
                                        ).show()
                                    }
                                }
                            }
                        }
                        .show()
                }
                holder.itemView.setOnClickListener {
                    ejecutarSeguro(executor) {
                        val puedeVerExacta = esPropietario || reporte.usuarioId == SesionManager.obtenerUsuarioId(this@DetallePublicacionActivity)
                        val ubicacionExacta = if (puedeVerExacta) {
                            runCatching {
                                ControladorPublicaciones.obtenerCoordenadasAvistamiento(
                                    this@DetallePublicacionActivity, reporte.id
                                )
                            }.getOrNull()
                        } else null
                        val latitud = ubicacionExacta?.first ?: reporte.latitud
                        val longitud = ubicacionExacta?.second ?: reporte.longitud
                        actualizarUi {
                            startActivity(Intent(this@DetallePublicacionActivity, MainActivity::class.java).apply {
                                putExtra("centrar_lat", latitud)
                                putExtra("centrar_lng", longitud)
                                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                            })
                        }
                    }
                }
            }

            override fun getItemCount() = ordenados.size
        }
        AlertDialog.Builder(this)
            .setTitle("Historial de ${publicacion.nombre}")
            .setView(vista)
            .setPositiveButton("Cerrar", null)
            .show()
            .also { com.example.practicafinal.util.Dialogos.aplicar(this, it) }
    }

    private class HistorialViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val fecha: TextView = view.findViewById(R.id.tv_fecha)
        val descripcion: TextView = view.findViewById(R.id.tv_desc)
        val estado: TextView = view.findViewById(R.id.tv_revision)
        val mapa: TextView = view.findViewById(R.id.tv_mapa)
        val foto: android.widget.ImageView = view.findViewById(R.id.img_avistamiento)
        val revision: com.google.android.material.button.MaterialButton =
            view.findViewById(R.id.btn_revision_avistamiento)
    }

    private fun abrirWhatsApp(publicacion: Publicacion) {
        val telefono = publicacion.contactoWhatsapp?.filter(Char::isDigit).orEmpty()
        if (telefono.isBlank()) return
        val texto = "Hola, te contacto por ${publicacion.nombre} (${tl(publicacion.tipo)})."
        startActivity(Intent(Intent.ACTION_VIEW, android.net.Uri.parse(
            "https://wa.me/$telefono?text=${android.net.Uri.encode(texto)}"
        )))
    }

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdown()
    }
}
