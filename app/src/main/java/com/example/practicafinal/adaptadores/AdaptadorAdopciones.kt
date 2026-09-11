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
import org.osmdroid.util.GeoPoint
import java.util.Locale
import java.util.concurrent.TimeUnit

class AdaptadorAdopciones(
    private val items: List<Publicacion>,
    private val userLat: Double?,
    private val userLng: Double?,
    private val onCardClick: (Publicacion) -> Unit
) : RecyclerView.Adapter<AdaptadorAdopciones.AdopcionViewHolder>() {

    class AdopcionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgFoto: ImageView = view.findViewById(R.id.img_foto)
        val tvPlaceholder: TextView = view.findViewById(R.id.tv_placeholder)
        val tvEspecieChip: TextView = view.findViewById(R.id.tv_especie_chip)
        val tvNombre: TextView = view.findViewById(R.id.tv_nombre)
        val tvDesc: TextView = view.findViewById(R.id.tv_desc)
        val tvDistancia: TextView = view.findViewById(R.id.tv_distancia)
        val tvBadgeNuevo: TextView = view.findViewById(R.id.tv_badge_nuevo)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdopcionViewHolder =
        AdopcionViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_adopcion_card, parent, false))

    override fun onBindViewHolder(holder: AdopcionViewHolder, position: Int) {
        val p = items[position]

        if (!p.foto.isNullOrBlank()) {
            Glide.with(holder.itemView.context)
                .load(p.foto)
                .centerCrop()
                .into(holder.imgFoto)
            holder.tvPlaceholder.visibility = View.GONE
        } else {
            holder.imgFoto.setImageDrawable(null)
            holder.tvPlaceholder.visibility = View.VISIBLE
            holder.tvPlaceholder.text = when {
                p.especie.equals("Perro", ignoreCase = true) -> "🐶"
                p.especie.equals("Gato", ignoreCase = true) -> "🐱"
                else -> "🐾"
            }
        }

        val especie = p.especie?.take(20).orEmpty()
        holder.tvEspecieChip.text = especie
        holder.tvEspecieChip.visibility = if (especie.isEmpty()) View.GONE else View.VISIBLE

        holder.tvNombre.text = p.nombre
        holder.tvDesc.text = p.descripcion

        if (userLat != null && userLng != null) {
            val distMeters = GeoPoint(p.latitud, p.longitud)
                .distanceToAsDouble(GeoPoint(userLat, userLng))
            val km = distMeters / 1000.0
            holder.tvDistancia.text = if (km < 1.0) {
                String.format(Locale("es"), "%.0f m", distMeters)
            } else {
                String.format(Locale("es"), "%.1f km", km)
            }
            holder.tvDistancia.visibility = View.VISIBLE
        } else {
            holder.tvDistancia.visibility = View.GONE
        }

        val ahora = System.currentTimeMillis()
        val horasDesdeCreacion = TimeUnit.MILLISECONDS.toHours(ahora - p.fechaCreacion)
        holder.tvBadgeNuevo.visibility = if (horasDesdeCreacion < 24) View.VISIBLE else View.GONE

        holder.itemView.setOnClickListener { onCardClick(p) }
    }

    override fun getItemCount(): Int = items.size
}
