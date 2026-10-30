package com.example.practicafinal.adaptadores

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.practicafinal.R
import com.example.practicafinal.modelo.Avistamiento
import com.example.practicafinal.modelo.Publicacion

class AdaptadorPerdidas(
    private val items: List<Publicacion>,
    private val avistPorPublicacion: Map<Long, List<Avistamiento>>,
    private val onSighting: (Publicacion) -> Unit,
    private val onVerAvistamientos: (Publicacion) -> Unit,
    private val onVerMapa: (Publicacion) -> Unit,
    private val onDetalles: (Publicacion) -> Unit
) : RecyclerView.Adapter<AdaptadorPerdidas.PerdidaViewHolder>() {

    private var expandedPos = -1

    class PerdidaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgFoto: ImageView = view.findViewById(R.id.img_foto)
        val tvPlaceholder: TextView = view.findViewById(R.id.tv_placeholder)
        val tvNombre: TextView = view.findViewById(R.id.tv_nombre)
        val tvEspecie: TextView = view.findViewById(R.id.tv_especie)
        val tvUltimoLugar: TextView = view.findViewById(R.id.tv_ultimo_lugar)
        val tvRecompensa: TextView? = view.findViewById(R.id.tv_recompensa)
        val tvFecha: TextView = view.findViewById(R.id.tv_fecha)
        val tvEstado: TextView = view.findViewById(R.id.tv_estado)
        val tvAvist: TextView = view.findViewById(R.id.tv_avist)
        val tvEspecieBadge: TextView? = view.findViewById(R.id.tv_especie_badge)
        val btnVi: com.google.android.material.button.MaterialButton? =
            view.findViewById(R.id.btn_vi)
        val btnVerMapa: com.google.android.material.button.MaterialButton? =
            view.findViewById(R.id.btn_ver_mapa)
        val btnDetalles: com.google.android.material.button.MaterialButton? =
            view.findViewById(R.id.btn_detalles)
    }

    override fun getItemViewType(position: Int): Int =
        if (position == expandedPos) 1 else 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PerdidaViewHolder =
        PerdidaViewHolder(
            LayoutInflater.from(parent.context).inflate(
                if (viewType == 1) R.layout.item_perdida_card_big
                else R.layout.item_perdida_card_small,
                parent,
                false
            )
        )

    override fun onBindViewHolder(holder: PerdidaViewHolder, position: Int) {
        val p = items[position]
        if (!p.foto.isNullOrBlank()) {
            com.example.practicafinal.util.cargarImagen(holder.imgFoto, p.foto)
            holder.imgFoto.contentDescription = "Foto de ${p.nombre}"
            holder.tvPlaceholder.visibility = View.GONE
        } else {
            com.example.practicafinal.util.cargarImagen(holder.imgFoto, null)
            holder.imgFoto.contentDescription = "Sin foto de ${p.nombre}"
            holder.tvPlaceholder.text = when {
                p.especie.equals("Perro", ignoreCase = true) -> "🐶"
                p.especie.equals("Gato", ignoreCase = true) -> "🐱"
                else -> "🐾"
            }
            holder.tvPlaceholder.visibility = View.VISIBLE
        }
        holder.tvPlaceholder.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        holder.tvNombre.text = p.nombre
        holder.tvEspecie.text = p.especie.orEmpty()
        holder.tvEspecie.visibility = if (p.especie.isNullOrBlank()) View.GONE else View.VISIBLE
        holder.tvEspecieBadge?.text = p.especie?.uppercase() ?: ""
        holder.tvEspecieBadge?.visibility =
            if (p.especie.isNullOrBlank()) View.GONE else View.VISIBLE
        holder.tvUltimoLugar.text = if (p.tipo == "Encontrada") {
            p.ultimoLugar?.let { "📍 Encontrada en: $it" } ?: "📍 Ubicación en el mapa"
        } else {
            p.ultimoLugar?.let { "📍 Última vez: " + it } ?: "📍 Sin ubicación de referencia"
        }
        holder.tvFecha.text = if (p.tipo == "Perdida" && p.fechaPerdida != null) {
            "Última vez vista ${com.example.practicafinal.util.fechaRelativa(p.fechaPerdida)}"
        } else "Alerta publicada ${com.example.practicafinal.util.fechaRelativa(p.fechaCreacion)}"
        holder.tvRecompensa?.apply {
            text = "🎁 Recompensa: ${p.recompensa.orEmpty()}"
            visibility = if (p.tipo == "Perdida" && !p.recompensa.isNullOrBlank()) View.VISIBLE else View.GONE
        }
        val avistamientos = avistPorPublicacion[p.id].orEmpty()
        val count = avistamientos.size
        val resuelta = p.estado == "Resuelta" || p.estado == "Adoptada"
        holder.tvEstado.text = when {
            p.estado == "Adoptada" -> "Adoptada"
            p.estado == "Resuelta" && p.tipo == "Perdida" -> "Encontrada"
            p.estado == "Resuelta" -> "Resuelta"
            p.tipo == "Perdida" && count > 0 -> "Avistamientos · $count"
            p.tipo == "Perdida" -> "En búsqueda"
            else -> "Activa"
        }
        holder.tvEstado.setTextColor(
            ContextCompat.getColor(
                holder.itemView.context,
                when {
                    p.estado == "Adoptada" -> R.color.azul_enlace
                    p.estado == "Resuelta" -> R.color.texto_suave
                    p.tipo == "Perdida" && count > 0 -> R.color.ambar_estado
                    else -> R.color.verde_petgo
                }
            )
        )
        holder.btnVi?.visibility = if (p.tipo == "Perdida" && !resuelta) View.VISIBLE else View.GONE
        holder.btnVi?.setOnClickListener { onSighting(p) }
        holder.btnVerMapa?.setOnClickListener { onVerMapa(p) }
        holder.btnDetalles?.setOnClickListener { onDetalles(p) }

        holder.tvAvist.visibility = if (count > 0) View.VISIBLE else View.GONE
        if (count > 0) {
            val ultimo = avistamientos.maxByOrNull { it.fecha }
            val plural = if (count == 1) "avistamiento" else "avistamientos"
            val hora = ultimo?.let { com.example.practicafinal.util.fechaRelativa(it.fecha) }.orEmpty()
            holder.tvAvist.text = "👀 $count $plural · último $hora"
            holder.tvAvist.contentDescription =
                "$count $plural. Toca para ver los reportes y sus ubicaciones."
        }
        holder.tvAvist.setOnClickListener(
            if (count > 0) View.OnClickListener { onVerAvistamientos(p) } else null
        )

        holder.itemView.setOnClickListener {
            val pos = holder.bindingAdapterPosition
            if (pos == RecyclerView.NO_POSITION) return@setOnClickListener
            val anterior = expandedPos
            expandedPos = if (anterior == pos) -1 else pos
            if (anterior in 0 until itemCount) notifyItemChanged(anterior)
            notifyItemChanged(pos)
        }
    }

    override fun getItemCount(): Int = items.size
}
