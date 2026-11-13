package com.example.practicafinal.adaptadores

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.practicafinal.R
import com.example.practicafinal.modelo.Publicacion
import com.google.android.material.button.MaterialButton
import org.osmdroid.util.GeoPoint
import java.util.Locale
import java.util.concurrent.TimeUnit

class AdaptadorAdopciones(
    private val items: List<Publicacion>,
    private val userLat: Double?,
    private val userLng: Double?,
    private val onCardClick: (Publicacion) -> Unit
) : RecyclerView.Adapter<AdaptadorAdopciones.AdopcionViewHolder>() {

    private val expandidos = mutableSetOf<Int>()

    class AdopcionViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val fotoCard: FrameLayout = view.findViewById(R.id.foto_card)
        val imgFoto: ImageView = view.findViewById(R.id.img_foto)
        val tvPlaceholder: TextView = view.findViewById(R.id.tv_placeholder)
        val tvEspecieChip: TextView = view.findViewById(R.id.tv_especie_chip)
        val tvNombre: TextView = view.findViewById(R.id.tv_nombre)
        val tvDesc: TextView = view.findViewById(R.id.tv_desc)
        val tvDistancia: TextView = view.findViewById(R.id.tv_distancia)
        val tvBadgeNuevo: TextView = view.findViewById(R.id.tv_badge_nuevo)
        val detalleCard: View = view.findViewById(R.id.detalle_card)
        val tvDescCompleta: TextView = view.findViewById(R.id.tv_desc_completa)
        val btnVerDetalle: MaterialButton = view.findViewById(R.id.btn_ver_detalle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AdopcionViewHolder =
        AdopcionViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_adopcion_card, parent, false))

    override fun onBindViewHolder(holder: AdopcionViewHolder, position: Int) {
        val p = items[position]
        val expandido = position in expandidos

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

        val especie = p.especie?.take(20).orEmpty()
        holder.tvEspecieChip.text = especie
        holder.tvEspecieChip.visibility = if (especie.isEmpty()) View.GONE else View.VISIBLE

        holder.tvNombre.text = p.nombre

        val density = holder.itemView.context.resources.displayMetrics.density
        val altura = (if (expandido) 210 else 164) * density
        holder.fotoCard.layoutParams = holder.fotoCard.layoutParams.apply { height = altura.toInt() }

        holder.tvDesc.visibility = if (expandido) View.GONE else View.VISIBLE
        holder.tvDesc.text = p.descripcion

        holder.detalleCard.visibility = if (expandido) View.VISIBLE else View.GONE
        if (expandido) {
            holder.tvDescCompleta.text = buildString {
                append(p.descripcion)
                val extras = listOfNotNull(
                    p.edadAproximada?.takeIf { it.isNotBlank() }?.let { "Edad: $it" },
                    p.caracter?.takeIf { it.isNotBlank() }?.let { "Caracter: $it" },
                    p.cuidados?.takeIf { it.isNotBlank() }?.let { "Cuidados: $it" }
                )
                if (extras.isNotEmpty()) append(" · " + extras.joinToString(" · "))
            }
        }

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

        holder.btnVerDetalle.setOnClickListener { onCardClick(p) }
        holder.itemView.setOnClickListener {
            if (expandido) expandidos.remove(position) else expandidos.add(position)
            notifyItemChanged(position)
        }
    }

    override fun getItemCount(): Int = items.size
}
