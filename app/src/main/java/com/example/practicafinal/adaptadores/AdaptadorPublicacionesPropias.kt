package com.example.practicafinal.adaptadores

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.practicafinal.R
import com.example.practicafinal.modelo.Publicacion
import com.example.practicafinal.util.fechaRelativa
import com.example.practicafinal.util.VigenciaAlertasPerdidas

class AdaptadorPublicacionesPropias(
    private val items: List<Publicacion>,
    private val mostrarBotonResolver: Boolean = true,
    private val onResolver: (Publicacion) -> Unit,
    private val onAdoptar: ((Publicacion) -> Unit)? = null,
    private val onQuitarFavorito: ((Publicacion) -> Unit)? = null,
    private val onAbrir: ((Publicacion) -> Unit)? = null,
    private val onEditar: ((Publicacion) -> Unit)? = null,
    private val onEliminar: ((Publicacion) -> Unit)? = null,
    private val onConfirmar: ((Publicacion) -> Unit)? = null,
    private val onVerAvistamientos: ((Publicacion) -> Unit)? = null,
    private val onVerSolicitudesAdopcion: ((Publicacion) -> Unit)? = null
) : RecyclerView.Adapter<AdaptadorPublicacionesPropias.PubViewHolder>() {
    class PubViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgFoto: ImageView = view.findViewById(R.id.img_foto)
        val tvPlaceholder: TextView = view.findViewById(R.id.tv_placeholder)
        val tvNombre: TextView = view.findViewById(R.id.tv_nombre)
        val tvTipo: TextView = view.findViewById(R.id.tv_tipo)
        val tvFecha: TextView = view.findViewById(R.id.tv_fecha)
        val tvEstado: TextView = view.findViewById(R.id.tv_estado)
        val tvAntigua: TextView = view.findViewById(R.id.tv_antigua)
        val btnResolver: com.google.android.material.button.MaterialButton = view.findViewById(R.id.btn_resolver)
        val btnEditar: com.google.android.material.button.MaterialButton = view.findViewById(R.id.btn_editar)
        val btnEliminar: com.google.android.material.button.MaterialButton = view.findViewById(R.id.btn_eliminar)
        val btnVerAvistamientos: com.google.android.material.button.MaterialButton =
            view.findViewById(R.id.btn_ver_avistamientos_mios)
        val btnVerSolicitudes: com.google.android.material.button.MaterialButton =
            view.findViewById(R.id.btn_solicitudes_adopcion)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PubViewHolder =
        PubViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_mi_publicacion, parent, false))
    override fun onBindViewHolder(holder: PubViewHolder, position: Int) {
        val p = items[position]
        if (!p.foto.isNullOrBlank()) {
            com.example.practicafinal.util.cargarImagen(holder.imgFoto, p.foto)
            holder.tvPlaceholder.visibility = View.GONE
        } else {
            com.example.practicafinal.util.cargarImagen(holder.imgFoto, null)
            holder.tvPlaceholder.visibility = View.GONE
        }
        holder.tvNombre.text = p.nombre
        holder.tvTipo.text = when (p.tipo) {
            "Perdida" -> "Mascota perdida"
            "Encontrada" -> "Mascota encontrada"
            else -> "En adopcion"
        }
        holder.tvFecha.text = fechaRelativa(p.fechaCreacion)
        val necesitaRevision = VigenciaAlertasPerdidas.necesitaConfirmacion(p)
        val fueraDeBusqueda = !VigenciaAlertasPerdidas.visibleParaLaComunidad(p)
        holder.tvAntigua.visibility = if (necesitaRevision) View.VISIBLE else View.GONE
        holder.tvAntigua.text = when {
            fueraDeBusqueda -> "Oculta de la comunidad: toca para reactivarla"
            p.tipo == "Perdida" && onConfirmar != null -> "¿Sigue perdida? Toca aquí para confirmar"
            else -> "Revisa si esta publicación sigue activa"
        }
        holder.tvAntigua.isClickable = necesitaRevision && p.tipo == "Perdida" && onConfirmar != null
        holder.tvAntigua.setOnClickListener {
            if (necesitaRevision && p.tipo == "Perdida") onConfirmar?.invoke(p)
        }
        holder.btnVerAvistamientos.visibility =
            if (p.tipo == "Perdida" && onVerAvistamientos != null) View.VISIBLE else View.GONE
        holder.btnVerAvistamientos.setOnClickListener { onVerAvistamientos?.invoke(p) }
        holder.btnVerSolicitudes.visibility =
            if (p.tipo == "Adopcion" && onVerSolicitudesAdopcion != null) View.VISIBLE else View.GONE
        holder.btnVerSolicitudes.setOnClickListener { onVerSolicitudesAdopcion?.invoke(p) }
        val resuelta = p.estado == "Resuelta" || p.estado == "Adoptada"
        holder.tvEstado.text = when {
            p.estado == "Adoptada" -> "Adoptada"
            p.estado == "Resuelta" && p.tipo == "Perdida" -> "Encontrada"
            p.estado == "Resuelta" -> "Resuelta"
            else -> "Activa"
        }
        holder.tvEstado.setTextColor(ContextCompat.getColor(holder.itemView.context, when {
            p.estado == "Adoptada" -> R.color.colorSecondary
            p.estado == "Resuelta" -> R.color.texto_suave
            else -> R.color.verde_estado
        }))
        holder.itemView.setOnClickListener { onAbrir?.invoke(p) }
        holder.btnResolver.visibility = if (onQuitarFavorito != null || (!resuelta && mostrarBotonResolver)) View.VISIBLE else View.GONE
        if (onQuitarFavorito != null) {
            holder.btnResolver.text = "Quitar de favoritos"
            holder.btnResolver.setOnClickListener { onQuitarFavorito(p) }
        } else if (p.tipo == "Adopcion" && onAdoptar != null) {
            holder.btnResolver.text = "Marcar adoptada"
            holder.btnResolver.setOnClickListener { onAdoptar(p) }
        } else if (p.tipo == "Perdida") {
            holder.btnResolver.text = "Marcar como encontrada"
            holder.btnResolver.setOnClickListener { onResolver(p) }
        } else {
            holder.btnResolver.text = "Marcar resuelta"
            holder.btnResolver.setOnClickListener { onResolver(p) }
        }
        holder.btnEditar.visibility = if (!resuelta && onEditar != null) View.VISIBLE else View.GONE
        holder.btnEliminar.visibility = if (!resuelta && onEliminar != null) View.VISIBLE else View.GONE
        holder.btnEditar.setOnClickListener { onEditar?.invoke(p) }
        holder.btnEliminar.setOnClickListener { onEliminar?.invoke(p) }
    }
    override fun getItemCount(): Int = items.size
}
