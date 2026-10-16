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
            holder.tvPlaceholder.visibility = View.GONE
        } else {
            com.example.practicafinal.util.cargarImagen(holder.imgFoto, null)
            holder.tvPlaceholder.visibility = View.GONE
        }
        holder.tvNombre.text = p.nombre
        holder.tvEspecie.text = p.especie.orEmpty()
        holder.tvEspecie.visibility = if (p.especie.isNullOrBlank()) View.GONE else View.VISIBLE
        holder.tvEspecieBadge?.text = p.especie?.uppercase() ?: ""
        holder.tvEspecieBadge?.visibility =
            if (p.especie.isNullOrBlank()) View.GONE else View.VISIBLE
        holder.tvUltimoLugar.text =
            p.ultimoLugar?.let { "📍 Última vez: " + it } ?: "📍 Sin ubicación de referencia"
        holder.tvFecha.text = com.example.practicafinal.util.fechaRelativa(p.fechaCreacion)
        val resuelta = p.estado == "Resuelta" || p.estado == "Adoptada"
        holder.tvEstado.text = when {
            p.estado == "Adoptada" -> "Adoptada"
            p.estado == "Resuelta" -> "Resuelta"
            else -> "Activa"
        }
        holder.tvEstado.setTextColor(
            ContextCompat.getColor(
                holder.itemView.context,
                when {
                    p.estado == "Adoptada" -> R.color.azul_enlace
                    p.estado == "Resuelta" -> R.color.texto_suave
                    else -> R.color.verde_petgo
                }
            )
        )
        holder.btnVi?.visibility = if (resuelta) View.GONE else View.VISIBLE
        holder.btnVi?.setOnClickListener { onSighting(p) }
        holder.btnVerMapa?.setOnClickListener { onVerMapa(p) }
        holder.btnDetalles?.setOnClickListener { onDetalles(p) }

        val count = avistPorPublicacion[p.id]?.size ?: 0
        holder.tvAvist.visibility = if (count > 0) View.VISIBLE else View.GONE
        holder.tvAvist.text =
            "👀 " + count + " avistamiento" + if (count > 1) "s" else ""
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
