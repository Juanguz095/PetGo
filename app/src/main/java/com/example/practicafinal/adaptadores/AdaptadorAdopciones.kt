package com.example.practicafinal.adaptadores

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.practicafinal.R
import com.example.practicafinal.modelo.Publicacion

class AdaptadorAdopciones(
    private val items: List<Publicacion>,
    private val onCardClick: (Publicacion) -> Unit
) : RecyclerView.Adapter<AdaptadorAdopciones.AdopcionViewHolder>() {
    class AdopcionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgFoto: ImageView = view.findViewById(R.id.img_foto)
        val tvPlaceholder: TextView = view.findViewById(R.id.tv_placeholder)
        val tvEspecieChip: TextView = view.findViewById(R.id.tv_especie_chip)
        val tvNombre: TextView = view.findViewById(R.id.tv_nombre)
        val tvDesc: TextView = view.findViewById(R.id.tv_desc)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdopcionViewHolder =
        AdopcionViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_adopcion_card, parent, false))
    override fun onBindViewHolder(holder: AdopcionViewHolder, position: Int) {
        val p = items[position]
        if (!p.foto.isNullOrBlank()) {
            Glide.with(holder.itemView.context).load(p.foto).into(holder.imgFoto)
            holder.tvPlaceholder.visibility = View.GONE
        } else {
            holder.imgFoto.setImageDrawable(null)
            holder.tvPlaceholder.visibility = View.VISIBLE
        }
        val especie = p.especie?.take(20).orEmpty()
        holder.tvEspecieChip.text = especie
        holder.tvEspecieChip.visibility = if (especie.isEmpty()) View.GONE else View.VISIBLE
        holder.tvNombre.text = p.nombre
        holder.tvDesc.text = p.descripcion
        holder.itemView.setOnClickListener { onCardClick(p) }
    }
    override fun getItemCount(): Int = items.size
}
