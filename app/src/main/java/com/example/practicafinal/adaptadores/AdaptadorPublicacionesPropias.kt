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
import com.example.practicafinal.modelo.Publicacion
import com.example.practicafinal.util.fechaRelativa

class AdaptadorPublicacionesPropias(
    private val items: List<Publicacion>,
    private val mostrarBotonResolver: Boolean = true,
    private val onResolver: (Publicacion) -> Unit,
    private val onAdoptar: ((Publicacion) -> Unit)? = null
) : RecyclerView.Adapter<AdaptadorPublicacionesPropias.PubViewHolder>() {
    class PubViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgFoto: ImageView = view.findViewById(R.id.img_foto)
        val tvPlaceholder: TextView = view.findViewById(R.id.tv_placeholder)
        val tvNombre: TextView = view.findViewById(R.id.tv_nombre)
        val tvTipo: TextView = view.findViewById(R.id.tv_tipo)
        val tvFecha: TextView = view.findViewById(R.id.tv_fecha)
        val tvEstado: TextView = view.findViewById(R.id.tv_estado)
        val btnResolver: com.google.android.material.button.MaterialButton = view.findViewById(R.id.btn_resolver)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PubViewHolder =
        PubViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_mi_publicacion, parent, false))
    override fun onBindViewHolder(holder: PubViewHolder, position: Int) {
        val p = items[position]
        if (!p.foto.isNullOrBlank()) {
            com.example.practicafinal.util.cargarImagen(holder.imgFoto, p.foto)
            holder.tvPlaceholder.visibility = View.GONE
        } else {
            holder.imgFoto.setImageResource(android.R.drawable.ic_menu_gallery)
            holder.tvPlaceholder.visibility = View.GONE
        }
        holder.tvNombre.text = p.nombre
        holder.tvTipo.text = when (p.tipo) {
            "Perdida" -> "Mascota perdida"
            "Encontrada" -> "Mascota encontrada"
            else -> "En adopcion"
        }
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
        holder.btnResolver.visibility = if (resuelta || !mostrarBotonResolver) View.GONE else View.VISIBLE
        if (p.tipo == "Adopcion" && onAdoptar != null) {
            holder.btnResolver.text = "Marcar adoptada"
            holder.btnResolver.setOnClickListener { onAdoptar(p) }
        } else {
            holder.btnResolver.text = "Marcar resuelta"
            holder.btnResolver.setOnClickListener { onResolver(p) }
        }
    }
    override fun getItemCount(): Int = items.size
}
