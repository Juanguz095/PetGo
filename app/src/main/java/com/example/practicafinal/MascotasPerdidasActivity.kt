package com.example.practicafinal

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.EditText
import androidx.core.widget.doAfterTextChanged
import androidx.core.content.ContextCompat
import com.example.practicafinal.util.filtrarPublicaciones
import androidx.appcompat.app.AlertDialog
import com.example.practicafinal.util.ejecutarSeguro
import com.example.practicafinal.util.actualizarUi
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.practicafinal.adaptadores.AdaptadorPerdidas
import com.example.practicafinal.controlador.ControladorPublicaciones
import com.example.practicafinal.modelo.Avistamiento
import com.example.practicafinal.modelo.Publicacion
import com.example.practicafinal.session.SesionManager
import com.example.practicafinal.util.fechaRelativa
import com.example.practicafinal.util.MotorCoincidenciasAvistamiento
import com.example.practicafinal.util.VigenciaAlertasPerdidas
import com.google.firebase.firestore.DocumentSnapshot
import org.osmdroid.util.GeoPoint
import java.util.concurrent.Executors

class MascotasPerdidasActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_TIPO_LISTADO = "tipo_listado_mascotas"
    }

    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var rvPerdidas: RecyclerView
    private lateinit var tvVacio: TextView
    private lateinit var etBuscar: EditText
    private var publicaciones: List<Publicacion> = emptyList()
    private var avistPorId: Map<Long, List<Avistamiento>> = emptyMap()
    private var filtroEspecie: String? = null
    private var tipoListado = "Perdida"
    private var userLoc: GeoPoint? = null
    private var radioKm: Double? = null
    private var actividadDesdeMs: Long? = null
    private var ordenActual = "actividad"
    private var cursorPagina: DocumentSnapshot? = null
    private var hayMas = false
    private var cargando = false
    private var primeraPaginaCargada = false
    private var actualizarAlVolver = false
    private lateinit var btnCargarMas: com.google.android.material.button.MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mascotas_perdidas)

        tipoListado = intent.getStringExtra(EXTRA_TIPO_LISTADO)?.takeIf { it == "Encontrada" } ?: "Perdida"
        findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar).title =
            if (tipoListado == "Encontrada") "Mascotas encontradas" else "Mascotas perdidas"
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_publicar).contentDescription =
            if (tipoListado == "Encontrada") "Publicar mascota encontrada"
            else "Opciones de mascotas perdidas: publicar alerta o reportar avistamiento"

        findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
            .setNavigationOnClickListener { finish() }

        rvPerdidas = findViewById(R.id.rv_perdidas)
        tvVacio = findViewById(R.id.tv_vacio)
        btnCargarMas = findViewById(R.id.btn_cargar_mas)
        btnCargarMas.setOnClickListener { cargarPerdidas(reset = false) }
        rvPerdidas.layoutManager = LinearLayoutManager(this)

        findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_publicar)
            .setOnClickListener {
                if (tipoListado == "Perdida") {
                    AlertDialog.Builder(this)
                        .setTitle("Ayuda para una mascota perdida")
                        .setItems(arrayOf("Publicar una mascota perdida", "Reportar un avistamiento")) { _, opcion ->
                            if (opcion == 0) abrirFormularioPublicacion("Perdida")
                            else reportarAvistamientoGeneral()
                        }
                        .show()
                        .also { com.example.practicafinal.util.Dialogos.aplicar(this, it) }
                } else abrirFormularioPublicacion(tipoListado)
            }

        userLoc = com.example.practicafinal.util.ultimaUbicacion(this)
            ?.let { GeoPoint(it.latitude, it.longitude) }
        findViewById<TextView>(R.id.btn_radio).setOnClickListener { mostrarMenuRadio() }


        findViewById<TextView>(R.id.btn_fecha_filtro).setOnClickListener { mostrarMenuFecha() }
        findViewById<TextView>(R.id.btn_orden).setOnClickListener { mostrarMenuOrden() }
        val panelFiltros = findViewById<View>(R.id.panel_filtros_secundarios)
        findViewById<View>(R.id.btn_toggle_filtros).setOnClickListener { boton ->
            val mostrar = panelFiltros.visibility != View.VISIBLE
            panelFiltros.visibility = if (mostrar) View.VISIBLE else View.GONE
            boton.contentDescription = if (mostrar) "Ocultar filtros" else "Mostrar filtros"
        }

        etBuscar = findViewById(R.id.et_buscar)
        etBuscar.doAfterTextChanged { mostrarPerdidas() }
        listOf(R.id.chip_todos to null, R.id.chip_perros to "Perro", R.id.chip_gatos to "Gato")
            .forEach { (id, especie) ->
                findViewById<TextView>(id).setOnClickListener {
                    filtroEspecie = especie
                    mostrarPerdidas()
                }
            }
    }

    private fun mostrarMenuFecha() {
        androidx.appcompat.widget.PopupMenu(this, findViewById(R.id.btn_fecha_filtro)).apply {
            val opciones = listOf(
                "Toda la actividad" to null,
                "Últimas 24 horas" to 24L * 60 * 60 * 1000,
                "Últimos 7 días" to 7L * 24 * 60 * 60 * 1000,
                "Últimos 30 días" to 30L * 24 * 60 * 60 * 1000
            )
            opciones.forEach { (etiqueta, intervalo) ->
                menu.add(etiqueta).setOnMenuItemClickListener {
                    actividadDesdeMs = intervalo?.let { System.currentTimeMillis() - it }
                    findViewById<TextView>(R.id.btn_fecha_filtro).apply {
                        text = when (intervalo) {
                            null -> "Actividad: todas"
                            24L * 60 * 60 * 1000 -> "Actividad: 24 h"
                            7L * 24 * 60 * 60 * 1000 -> "Actividad: 7 días"
                            else -> "Actividad: 30 días"
                        }
                        contentDescription = "Filtro de actividad: $etiqueta"
                    }
                    mostrarPerdidas()
                    true
                }
            }
            show()
        }
    }

    private fun mostrarMenuOrden() {
        androidx.appcompat.widget.PopupMenu(this, findViewById(R.id.btn_orden)).apply {
            val opciones = listOf(
                "Actividad reciente" to "actividad",
                "Último avistamiento" to "avistamiento",
                "Más cercanas" to "cercanas"
            )
            opciones.forEach { (etiqueta, valor) ->
                menu.add(etiqueta).setOnMenuItemClickListener {
                    if (valor == "cercanas" && userLoc == null) {
                        android.widget.Toast.makeText(
                            this@MascotasPerdidasActivity,
                            "Necesitas una ubicación reciente para ordenar por distancia",
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                        return@setOnMenuItemClickListener true
                    }
                    ordenActual = valor
                    findViewById<TextView>(R.id.btn_orden).apply {
                        text = when (valor) {
                            "avistamiento" -> "Orden: avistamiento"
                            "cercanas" -> "Orden: cercanas"
                            else -> "Orden: actividad"
                        }
                        contentDescription = "Ordenar por $etiqueta"
                    }
                    mostrarPerdidas()
                    true
                }
            }
            show()
        }
    }

    private fun mostrarMenuRadio() {
        if (userLoc == null) {
            android.widget.Toast.makeText(
                this,
                "Necesitas una ubicación reciente para filtrar por distancia",
                android.widget.Toast.LENGTH_LONG
            ).show()
            return
        }
        androidx.appcompat.widget.PopupMenu(this, findViewById(R.id.btn_radio)).apply {
            listOf("Todas las distancias" to null, "Hasta 5 km" to 5.0, "Hasta 10 km" to 10.0, "Hasta 25 km" to 25.0)
                .forEach { (label, value) ->
                    menu.add(label).setOnMenuItemClickListener {
                        radioKm = value
                        findViewById<TextView>(R.id.btn_radio).text =
                            if (value == null) "Distancia: todas" else "Distancia: ${value.toInt()} km"
                        mostrarPerdidas()
                        true
                    }
                }
            show()
        }
    }

    private fun cargarPerdidas(reset: Boolean = true) {
        if (cargando) return
        if (!reset && !hayMas) return
        if (reset) {
            cursorPagina = null
            publicaciones = emptyList()
            avistPorId = emptyMap()
        }
        cargando = true
        btnCargarMas.isEnabled = false
        ejecutarSeguro(executor) {
            try {
                val pagina = ControladorPublicaciones.obtenerPagina(this, tipoListado, cursorPagina)
                val avistamientos = if (tipoListado == "Perdida") {
                    val cargadas = (if (reset) emptyList() else publicaciones) + pagina.items
                        .filter { it.estado == "Activa" && VigenciaAlertasPerdidas.visibleParaLaComunidad(it) }
                    val alertasCargadas = cargadas.filter { it.tipo == "Perdida" }
                    val reportes = ControladorPublicaciones.obtenerAvistamientosParaAlertas(
                        this, alertasCargadas.map { it.id }
                    )
                    val sinVincular = reportes.filter { it.publicacionId == 0L }
                    alertasCargadas.associate { alerta ->
                        val vinculados = reportes.filter { it.publicacionId == alerta.id }
                        val sugeridos = sinVincular.mapNotNull { reporte ->
                            MotorCoincidenciasAvistamiento.evaluar(alerta, reporte)?.let { evaluacion ->
                                reporte.copy(
                                    puntajeCoincidencia = evaluacion.puntaje,
                                    motivosCoincidencia = evaluacion.motivos
                                )
                            }
                        }
                        alerta.id to (vinculados + sugeridos).distinctBy { it.id }.sortedWith(
                            compareByDescending<Avistamiento> { it.puntajeCoincidencia ?: -1 }
                                .thenByDescending { it.fecha }
                        )
                    }
                } else avistPorId
                val publicacionesCargadas = (if (reset) emptyList() else publicaciones) +
                        pagina.items.filter {
                            it.estado == "Activa" && VigenciaAlertasPerdidas.visibleParaLaComunidad(it)
                        }
                actualizarUi {
                    cursorPagina = pagina.cursor
                    hayMas = pagina.hayMas
                    primeraPaginaCargada = true
                    cargando = false
                    btnCargarMas.isEnabled = true
                    btnCargarMas.visibility = if (hayMas) View.VISIBLE else View.GONE
                    publicaciones = publicacionesCargadas
                    avistPorId = avistamientos
                    mostrarPerdidas(preserveScroll = !reset)
                }
            } catch (error: Exception) {
                actualizarUi { cargando = false; btnCargarMas.isEnabled = true }
                throw error
            }
        }
    }

    private fun mostrarPerdidas(preserveScroll: Boolean = false) {
        var filtradas = filtrarPublicaciones(publicaciones, etBuscar.text.toString(), filtroEspecie)
        radioKm?.let { radio ->
            val origen = userLoc
            if (origen != null) {
                filtradas = filtradas.filter {
                    GeoPoint(it.latitud, it.longitud).distanceToAsDouble(origen) <= radio * 1000.0
                }
            }
        }
        actividadDesdeMs?.let { desde ->
            filtradas = filtradas.filter { actividadMasReciente(it) >= desde }
        }
        filtradas = when (ordenActual) {
            "avistamiento" -> filtradas.sortedWith(
                compareByDescending<Publicacion> { avistPorId[it.id].orEmpty().maxOfOrNull(Avistamiento::fecha) ?: 0L }
                    .thenByDescending { actividadMasReciente(it) }
            )

            "cercanas" -> filtradas.sortedBy {
                userLoc?.let { origen -> GeoPoint(it.latitud, it.longitud).distanceToAsDouble(origen) }
                    ?: Double.MAX_VALUE
            }

            else -> filtradas.sortedByDescending(::actividadMasReciente)
        }
        listOf(R.id.chip_todos to null, R.id.chip_perros to "Perro", R.id.chip_gatos to "Gato")
            .forEach { (id, especie) ->
                val chip = findViewById<TextView>(id)
                val activo = filtroEspecie == especie
                chip.setBackgroundResource(if (activo) R.drawable.bg_chip_lista_on else R.drawable.bg_chip_lista)
                chip.setTextColor(ContextCompat.getColor(this, if (activo) R.color.tinta else R.color.texto_suave))
                chip.isSelected = activo
            }
        findViewById<TextView>(R.id.chip_todos).text = getString(R.string.all_pets_count, publicaciones.size)
        tvVacio.setText(
            when {
                filtradas.isEmpty() && publicaciones.isNotEmpty() && hayMas -> R.string.search_more_loaded
                publicaciones.isNotEmpty() -> R.string.no_pet_results
                tipoListado == "Encontrada" -> R.string.empty_found_pets
                else -> R.string.empty_lost_pets
            }
        )
        tvVacio.visibility = if (filtradas.isEmpty()) View.VISIBLE else View.GONE
        val layoutManager = rvPerdidas.layoutManager as? LinearLayoutManager
        val firstVisible = layoutManager?.findFirstVisibleItemPosition() ?: 0
        val firstOffset = rvPerdidas.getChildAt(0)?.top ?: 0
        rvPerdidas.adapter = AdaptadorPerdidas(
            filtradas, avistPorId,
            onSighting = { p -> reportarAvistamiento(p) },
            onVerAvistamientos = { p -> mostrarAvistamientos(p, avistPorId) },
            onVerMapa = { p -> verEnMapa(p) },
            onDetalles = { p -> verDetalles(p) }
        )
        if (preserveScroll && firstVisible >= 0) {
            rvPerdidas.post { layoutManager?.scrollToPositionWithOffset(firstVisible, firstOffset) }
        } else {
            rvPerdidas.scrollToPosition(0)
        }
    }

    private fun actividadMasReciente(publicacion: Publicacion): Long = maxOf(
        publicacion.fechaCreacion,
        publicacion.ultimaConfirmacion ?: 0L,
        avistPorId[publicacion.id].orEmpty().maxOfOrNull(Avistamiento::fecha) ?: 0L
    )

    private fun reportarAvistamiento(publicacion: Publicacion) {
        actualizarAlVolver = true
        startActivity(
            Intent(this, ReportarAvistamientoActivity::class.java).apply {
                putExtra(ReportarAvistamientoActivity.EXTRA_PUBLICACION_ID, publicacion.id)
                putExtra(ReportarAvistamientoActivity.EXTRA_NOMBRE, publicacion.nombre)
                putExtra(ReportarAvistamientoActivity.EXTRA_ULTIMA_LATITUD, publicacion.latitud)
                putExtra(ReportarAvistamientoActivity.EXTRA_ULTIMA_LONGITUD, publicacion.longitud)
                putExtra(ReportarAvistamientoActivity.EXTRA_ESPECIE, publicacion.especie)
            }
        )
    }

    private fun reportarAvistamientoGeneral() {
        actualizarAlVolver = true
        startActivity(Intent(this, ReportarAvistamientoActivity::class.java))
    }

    private fun abrirFormularioPublicacion(tipo: String) {
        actualizarAlVolver = true
        startActivity(Intent(this, CrearPublicacionActivity::class.java).apply {
            putExtra(CrearPublicacionActivity.EXTRA_TIPO, tipo)
        })
    }

    private fun verEnMapa(publicacion: Publicacion) {
        ejecutarSeguro(executor) {
            val exactas = if (publicacion.usuarioId == SesionManager.obtenerUsuarioId(this)) runCatching {
                ControladorPublicaciones.obtenerCoordenadasPublicacion(this, publicacion.id)
            }.getOrNull() else null
            actualizarUi {
                startActivity(Intent(this, MainActivity::class.java).apply {
                    putExtra("centrar_lat", exactas?.first ?: publicacion.latitud)
                    putExtra("centrar_lng", exactas?.second ?: publicacion.longitud)
                    putExtra("mostrar_alerta", publicacion.id)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                })
            }
        }
    }

    private fun verDetalles(publicacion: Publicacion) {
        startActivity(
            Intent(this, DetallePublicacionActivity::class.java).apply {
                putExtra(DetallePublicacionActivity.EXTRA_PUBLICACION_ID, publicacion.id)
            }
        )
    }

    private fun mostrarAvistamientos(
        publicacion: Publicacion,
        avistPorId: Map<Long, List<Avistamiento>>
    ) {
        val avistamientos = avistPorId[publicacion.id].orEmpty().sortedByDescending { it.fecha }
        val esPropietario = publicacion.usuarioId == SesionManager.obtenerUsuarioId(this)
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_avistamientos, null)
        val rv = dialogView.findViewById<RecyclerView>(R.id.rv_avistamientos)
        val tvVacioAvist = dialogView.findViewById<TextView>(R.id.tv_vacio_avist)
        rv.layoutManager = LinearLayoutManager(this)
        var dialog: AlertDialog? = null

        if (avistamientos.isEmpty()) {
            tvVacioAvist.visibility = View.VISIBLE
            rv.visibility = View.GONE
        } else {
            tvVacioAvist.visibility = View.GONE
            rv.visibility = View.VISIBLE
            rv.adapter = object : RecyclerView.Adapter<AvistViewHolder>() {
                override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = AvistViewHolder(
                    LayoutInflater.from(parent.context).inflate(R.layout.item_avistamiento_row, parent, false)
                )

                override fun onBindViewHolder(holder: AvistViewHolder, position: Int) {
                    val avistamiento = avistamientos[position]
                    holder.tvFecha.text = "Visto " + fechaRelativa(avistamiento.fecha)
                    holder.tvDesc.text = buildString {
                        avistamiento.especie?.let { append(it + " · ") }
                        append(avistamiento.descripcion.ifBlank { "Sin detalles adicionales" })
                    }
                    holder.tvRevision.text = if (avistamiento.publicacionId == 0L) {
                        val puntaje = avistamiento.puntajeCoincidencia
                        if (puntaje == null) "Posible coincidencia · reporte aún sin vincular"
                        else buildString {
                            append(
                                "Coincidencia ${
                                    when {
                                        puntaje >= 75 -> "alta"
                                        puntaje >= 55 -> "media"
                                        else -> "baja"
                                    }
                                } · $puntaje%"
                            )
                            avistamiento.motivosCoincidencia.take(2).takeIf { it.isNotEmpty() }
                                ?.joinToString(" · ")?.let { append(" · $it") }
                        }
                    } else when (avistamiento.estadoRevision) {
                        "Coincide" -> "El dueño confirmó que coincide"
                        "No coincide" -> "El dueño indicó que no coincide"
                        "Duplicado" -> "Reporte marcado como duplicado"
                        else -> "Pendiente de revisión del dueño"
                    }
                    holder.tvMapa.text = "Ver punto en el mapa →"
                    if (avistamiento.foto.isNullOrBlank()) {
                        holder.imgFoto.visibility = View.GONE
                    } else {
                        holder.imgFoto.visibility = View.VISIBLE
                        com.example.practicafinal.util.cargarImagen(holder.imgFoto, avistamiento.foto)
                    }
                    holder.btnRevision.visibility = if (esPropietario) View.VISIBLE else View.GONE
                    holder.btnRevision.text = if (avistamiento.publicacionId == 0L) "Vincular reporte" else "Revisar"
                    holder.btnRevision.setOnClickListener {
                        if (avistamiento.publicacionId == 0L) {
                            AlertDialog.Builder(this@MascotasPerdidasActivity)
                                .setTitle("Vincular avistamiento")
                                .setMessage("¿Este reporte parece corresponder a " + publicacion.nombre + "? Quien lo envió recibirá un aviso.")
                                .setNegativeButton("Cancelar", null)
                                .setPositiveButton("Vincular") { _, _ ->
                                    ejecutarSeguro(executor) {
                                        try {
                                            ControladorPublicaciones.vincularAvistamiento(
                                                this@MascotasPerdidasActivity, avistamiento.id, publicacion.id
                                            )
                                            actualizarUi {
                                                dialog?.dismiss()
                                                cargarPerdidas()
                                            }
                                        } catch (error: Exception) {
                                            actualizarUi {
                                                android.widget.Toast.makeText(
                                                    this@MascotasPerdidasActivity,
                                                    error.message ?: "No se pudo vincular el reporte",
                                                    android.widget.Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        }
                                    }
                                }
                                .show()
                                .also {
                                    com.example.practicafinal.util.Dialogos.aplicar(
                                        this@MascotasPerdidasActivity,
                                        it
                                    )
                                }
                        } else {
                            val opciones = arrayOf(
                                "Coincide con mi mascota",
                                "No coincide",
                                "Es un duplicado",
                                "Quitar revisión"
                            )
                            AlertDialog.Builder(this@MascotasPerdidasActivity)
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
                                                this@MascotasPerdidasActivity, avistamiento.id, estado
                                            )
                                            actualizarUi {
                                                dialog?.dismiss()
                                                cargarPerdidas()
                                            }
                                        } catch (error: Exception) {
                                            actualizarUi {
                                                android.widget.Toast.makeText(
                                                    this@MascotasPerdidasActivity,
                                                    error.message ?: "No se pudo actualizar el reporte",
                                                    android.widget.Toast.LENGTH_LONG
                                                ).show()
                                            }
                                        }
                                    }
                                }
                                .show()
                                .also {
                                    com.example.practicafinal.util.Dialogos.aplicar(
                                        this@MascotasPerdidasActivity,
                                        it
                                    )
                                }
                        }
                    }
                    holder.itemView.contentDescription =
                        "Avistamiento " + fechaRelativa(avistamiento.fecha) + ". Abrir su ubicación en el mapa."
                    holder.itemView.setOnClickListener {
                        ejecutarSeguro(executor) {
                            val puedeVerExacta =
                                esPropietario || avistamiento.usuarioId == SesionManager.obtenerUsuarioId(this@MascotasPerdidasActivity)
                            val ubicacionExacta = if (puedeVerExacta) {
                                runCatching {
                                    ControladorPublicaciones.obtenerCoordenadasAvistamiento(
                                        this@MascotasPerdidasActivity, avistamiento.id
                                    )
                                }.getOrNull()
                            } else null
                            val latitud = ubicacionExacta?.first ?: avistamiento.latitud
                            val longitud = ubicacionExacta?.second ?: avistamiento.longitud
                            actualizarUi {
                                startActivity(Intent(this@MascotasPerdidasActivity, MainActivity::class.java).apply {
                                    putExtra("centrar_lat", latitud)
                                    putExtra("centrar_lng", longitud)
                                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                                })
                            }
                        }
                    }
                }

                override fun getItemCount() = avistamientos.size
            }
        }

        dialog = AlertDialog.Builder(this)
            .setTitle("Avistamientos de " + publicacion.nombre)
            .setView(dialogView)
            .setPositiveButton("Cerrar", null)
            .create()
        dialog?.show()
        dialog?.also { com.example.practicafinal.util.Dialogos.aplicar(this, it) }
    }

    private class AvistViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvFecha: TextView = view.findViewById(R.id.tv_fecha)
        val tvDesc: TextView = view.findViewById(R.id.tv_desc)
        val tvMapa: TextView = view.findViewById(R.id.tv_mapa)
        val tvRevision: TextView = view.findViewById(R.id.tv_revision)
        val imgFoto: android.widget.ImageView = view.findViewById(R.id.img_avistamiento)
        val btnRevision: com.google.android.material.button.MaterialButton =
            view.findViewById(R.id.btn_revision_avistamiento)
    }

    override fun onResume() {
        super.onResume()
        if (!primeraPaginaCargada || actualizarAlVolver) {
            actualizarAlVolver = false
            cargarPerdidas()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdown()
    }
}
