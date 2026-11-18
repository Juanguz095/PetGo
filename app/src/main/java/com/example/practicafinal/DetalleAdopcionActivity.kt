package com.example.practicafinal

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import com.example.practicafinal.util.ejecutarSeguro
import com.example.practicafinal.util.actualizarUi
import androidx.appcompat.app.AppCompatActivity
import com.example.practicafinal.controlador.ControladorPublicaciones
import com.example.practicafinal.modelo.Publicacion
import com.example.practicafinal.session.FavoritosManager
import com.example.practicafinal.session.SesionManager
import java.util.concurrent.Executors

class DetalleAdopcionActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_PUBLICACION_ID = "detalle_publicacion_id"
    }

    private val executor = Executors.newSingleThreadExecutor()
    private var publicacionId: Long = -1L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detalle_adopcion)

        findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
            .setNavigationOnClickListener { finish() }

        publicacionId = intent.getLongExtra(EXTRA_PUBLICACION_ID, -1L)
        if (publicacionId == -1L) {
            finish(); return
        }

        cargarPublicacion()
    }

    private fun cargarPublicacion() {
        ejecutarSeguro(executor) {
            val publicacion = ControladorPublicaciones.obtenerPorId(this, publicacionId)
            actualizarUi {
                if (publicacion == null) {
                    finish(); return@actualizarUi
                }
                mostrar(publicacion)
            }
        }
    }

    private fun mostrar(p: Publicacion) {

        val imgFoto = findViewById<android.widget.ImageView>(R.id.img_foto)
        val tvPlaceholder = findViewById<TextView>(R.id.tv_placeholder)
        if (!p.foto.isNullOrBlank()) {
            com.example.practicafinal.util.cargarImagen(imgFoto, p.foto)
            tvPlaceholder.visibility = View.GONE
        } else {
            tvPlaceholder.visibility = View.VISIBLE
        }
        val tvEspecie = findViewById<TextView>(R.id.tv_especie_detalle)
        if (!p.especie.isNullOrEmpty()) {
            tvEspecie.text = p.especie
            tvEspecie.visibility = View.VISIBLE
        } else {
            tvEspecie.visibility = View.GONE
        }

        findViewById<TextView>(R.id.tv_nombre).text = p.nombre
        findViewById<TextView>(R.id.tv_descripcion).text = buildString {
            append(p.descripcion)
            val extras = listOfNotNull(
                p.edadAproximada?.takeIf { it.isNotBlank() }?.let { "Edad aproximada: $it" },
                p.caracter?.takeIf { it.isNotBlank() }?.let { "Caracter: $it" },
                p.cuidados?.takeIf { it.isNotBlank() }?.let { "Cuidados: $it" }
            )
            if (extras.isNotEmpty()) append(" · " + extras.joinToString(" · "))
        }
        val nombrePublicador = p.usuarioNombre?.takeIf { it.isNotBlank() } ?: "un miembro de PetGo"
        findViewById<TextView>(R.id.tv_publicado_por).text = "Publicado por $nombrePublicador"

        findViewById<TextView>(R.id.tv_ubicacion).apply {
            text = "📍 Ver ubicación en el mapa"
            setOnClickListener {
                val intent = Intent(this@DetalleAdopcionActivity, MainActivity::class.java).apply {
                    putExtra("centrar_lat", p.latitud)
                    putExtra("centrar_lng", p.longitud)
                    putExtra("mostrar_alerta", publicacionId)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
                startActivity(intent)
            }
        }


        val btnFav = findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_favorito)
        actualizarBotonFavorito(btnFav)
        btnFav.setOnClickListener {
            btnFav.isEnabled = false
            FavoritosManager.toggle(this, publicacionId) { result ->
                actualizarUi {
                    btnFav.isEnabled = true
                    actualizarBotonFavorito(btnFav)
                    if (result.isFailure) {
                        Toast.makeText(this, R.string.favorite_sync_failed, Toast.LENGTH_LONG).show()
                    }
                }
            }
            actualizarBotonFavorito(btnFav)
        }


        findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_whatsapp).apply {
            visibility = if (p.contactoWhatsapp.isNullOrBlank()) View.GONE else View.VISIBLE
            setOnClickListener { contactarWhatsApp(p) }
        }

        val btnSolicitud = findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_solicitud_adopcion)
        val esPropietario = p.usuarioId == SesionManager.obtenerUsuarioId(this)
        btnSolicitud.visibility = if (p.estado == "Activa" && !esPropietario) View.VISIBLE else View.GONE
        if (btnSolicitud.visibility == View.VISIBLE) {
            btnSolicitud.setOnClickListener { mostrarFormularioSolicitud(btnSolicitud) }
            ejecutarSeguro(executor) {
                val solicitud = ControladorPublicaciones.obtenerMiSolicitudAdopcion(this, publicacionId)
                solicitud?.get("estado")?.toString()?.let { estado ->
                    actualizarUi {
                        btnSolicitud.isEnabled = false
                        btnSolicitud.text = "Solicitud enviada · ${etiquetaEstadoSolicitud(estado)}"
                    }
                }
            }
        }
    }

    private fun mostrarFormularioSolicitud(boton: com.google.android.material.button.MaterialButton) {
        val contenedor = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 8, 48, 0)
        }
        val telefono = EditText(this).apply {
            hint = "Tu WhatsApp (opcional)"
            inputType = android.text.InputType.TYPE_CLASS_PHONE
            maxLines = 1
        }
        val mensaje = EditText(this).apply {
            hint = "Cuéntale brevemente por qué te interesa (opcional)"
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_CAP_SENTENCES or
                    android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
            minLines = 2
            maxLines = 4
        }
        contenedor.addView(telefono)
        contenedor.addView(mensaje)
        val dialogo = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Enviar solicitud de adopción")
            .setMessage("Comparte un número para que puedan coordinar. Solo el dueño de la publicación verá tus datos.")
            .setView(contenedor)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Enviar", null)
            .create()
        dialogo.setOnShowListener {
            dialogo.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val phone = telefono.text.toString().filter(Char::isDigit)
                if (phone.isNotBlank() && !phone.matches(Regex("^(51)?[0-9]{9}$"))) {
                    telefono.error = "Escribe 9 dígitos, con 51 solo si incluyes el país"
                    return@setOnClickListener
                }
                val note = mensaje.text.toString().trim()
                if (note.length > 500) {
                    mensaje.error = "Máximo 500 caracteres"
                    return@setOnClickListener
                }
                boton.isEnabled = false
                boton.text = "Enviando solicitud…"
                ejecutarSeguro(executor) {
                    try {
                        val estado = ControladorPublicaciones.enviarSolicitudAdopcion(
                            this, publicacionId, note, phone
                        )
                        actualizarUi {
                            boton.text = "Solicitud enviada · ${etiquetaEstadoSolicitud(estado)}"
                            Toast.makeText(this, "El dueño recibirá tu solicitud", Toast.LENGTH_LONG).show()
                            dialogo.dismiss()
                        }
                    } catch (error: Exception) {
                        actualizarUi {
                            boton.isEnabled = true
                            boton.text = "Me interesa · enviar solicitud"
                            Toast.makeText(this, error.message ?: "No se pudo enviar la solicitud", Toast.LENGTH_LONG)
                                .show()
                        }
                    }
                }
            }
        }
        dialogo.show()
        com.example.practicafinal.util.Dialogos.aplicar(this, dialogo)
    }

    private fun etiquetaEstadoSolicitud(estado: String): String = when (estado) {
        "En conversación" -> "en conversación"
        "No seleccionado" -> "cerrada"
        "Adopción completada" -> "adopción concretada"
        else -> "pendiente"
    }

    private fun actualizarBotonFavorito(btn: com.google.android.material.button.MaterialButton) {
        val esFav = FavoritosManager.esFavorito(this, publicacionId)
        btn.text = if (esFav) "❤️  Quitar de favoritos" else "👍  Agregar a favoritos"
    }

    private fun contactarWhatsApp(p: Publicacion) {
        val telefono = p.contactoWhatsapp?.filter(Char::isDigit).orEmpty()
        if (telefono.isBlank()) return
        val latitudAproximada = kotlin.math.round(p.latitud * 1000.0) / 1000.0
        val longitudAproximada = kotlin.math.round(p.longitud * 1000.0) / 1000.0
        val texto =
            "${p.nombre} · En adopción\n${p.descripcion}\n📍 https://maps.google.com/?q=$latitudAproximada,$longitudAproximada"
        try {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$telefono?text=${Uri.encode(texto)}")))
        } catch (_: Exception) {
            Toast.makeText(this, "WhatsApp no instalado", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdown()
    }
}
