package com.example.practicafinal.adaptadores

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.practicafinal.R
import com.example.practicafinal.modelo.Avistamiento
import com.example.practicafinal.modelo.Publicacion
import com.example.practicafinal.util.fechaRelativa

class AdaptadorPerdidas(
    private val items: List<Publicacion>,
    private val avistPorPublicacion: Map<Long, List<Avistamiento>>,
    private val onSighting: (Publicacion) -> Unit,
    private val onVerAvistamientos: (Publicacion) -> Unit
) : RecyclerView.Adapter<AdaptadorPerdidas.PerdidaViewHolder>() {
    class PerdidaViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgFoto: ImageView = view.findViewById(R.id.img_foto)
        val tvPlaceholder: TextView = view.findViewById(R.id.tv_placeholder)
        val tvNombre: TextView = view.findViewById(R.id.tv_nombre)
        val tvUltimoLugar: TextView = view.findViewById(R.id.tv_ultimo_lugar)
        val tvFecha: TextView = view.findViewById(R.id.tv_fecha)
        val tvEstado: TextView = view.findViewById(R.id.tv_estado)
        val btnVi: com.google.android.material.button.MaterialButton = view.findViewById(R.id.btn_vi)
        val tvAvist: TextView = view.findViewById(R.id.tv_avist)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PerdidaViewHolder =
        PerdidaViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_perdida_card, parent, false))
    override fun onBindViewHolder(holder: PerdidaViewHolder, position: Int) {
        val p = items[position]
        if (!p.foto.isNullOrBlank()) {
            com.example.practicafinal.util.cargarImagen(holder.imgFoto, p.foto)
            holder.tvPlaceholder.visibility = View.GONE
        } else {
            holder.imgFoto.setImageResource(android.R.drawable.ic_menu_gallery)
            holder.tvPlaceholder.visibility = View.GONE
        }
        holder.tvNombre.text = p.nombre
        holder.tvUltimoLugar.text = p.ultimoLugar?.let { "📍 Última vez: " + it } ?: "📍 Sin ubicación de referencia"
        holder.tvFecha.text = fechaRelativa(p.fechaCreacion)
        val resuelta = p.estado == "Resuelta" || p.estado == "Adoptada"
        holder.tvEstado.text = when {
            p.estado == "Adoptada" -> "Adoptada"
            p.estado == "Resuelta" -> "Resuelta"
            else -> "Activa"
        }
        holder.tvEstado.setTextColor(ContextCompat.getColor(holder.itemView.context, when {
            p.estado == "Adoptada" -> R.color.colorSecondary
            p.estado == "Resuelta" -> android.R.color.darker_gray
            else -> R.color.verde_estado
        }))
        holder.btnVi.isEnabled = !resuelta
        holder.btnVi.setOnClickListener { onSighting(p) }
        val count = avistPorPublicacion[p.id]?.size ?: 0
        holder.tvAvist.visibility = if (count > 0) View.VISIBLE else View.GONE
        holder.tvAvist.text = "👀 " + count + " avistamiento" + if (count > 1) "s" else ""
        holder.tvAvist.setOnClickListener(if (count > 0) View.OnClickListener { onVerAvistamientos(p) } else null)
    }
    override fun getItemCount(): Int = items.size
}
