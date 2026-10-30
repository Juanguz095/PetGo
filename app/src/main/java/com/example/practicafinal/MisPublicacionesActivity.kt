package com.example.practicafinal

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.example.practicafinal.util.ejecutarSeguro
import com.example.practicafinal.util.actualizarUi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.practicafinal.adaptadores.AdaptadorPublicacionesPropias
import com.example.practicafinal.controlador.ControladorPublicaciones
import com.example.practicafinal.session.SesionManager
import com.google.firebase.firestore.DocumentSnapshot
import java.util.concurrent.Executors

class MisPublicacionesActivity : AppCompatActivity() {

    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var rv: androidx.recyclerview.widget.RecyclerView
    private lateinit var tvVacio: TextView
    private lateinit var btnCargarMas: com.google.android.material.button.MaterialButton
    private var publicaciones: List<com.example.practicafinal.modelo.Publicacion> = emptyList()
    private var cursorPagina: DocumentSnapshot? = null
    private var hayMas = false
    private var cargando = false
    private var primeraPaginaCargada = false
    private var actualizarAlVolver = false
    private var mostrandoErrorCarga = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mis_publicaciones)

        findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
            .setNavigationOnClickListener { finish() }

        rv = findViewById(R.id.rv_publicaciones)
        tvVacio = findViewById(R.id.tv_vacio)
        btnCargarMas = findViewById(R.id.btn_cargar_mas)
        btnCargarMas.setOnClickListener {
            if (mostrandoErrorCarga) cargarPublicaciones(reset = true)
            else cargarPublicaciones(reset = false)
        }
        rv.layoutManager = LinearLayoutManager(this)

        cargarPublicaciones()
    }

    private fun cargarPublicaciones(reset: Boolean = true) {
        if (SesionManager.obtenerUsuarioId(this) == null) {
            mostrarErrorCarga(getString(R.string.posts_session_required))
            return
        }
        if (cargando) return
        if (!reset && !hayMas) return
        if (reset) {
            cursorPagina = null
            publicaciones = emptyList()
            btnCargarMas.visibility = View.GONE
            tvVacio.text = getString(R.string.loading_posts)
            tvVacio.visibility = View.VISIBLE
        }
        mostrandoErrorCarga = false
        cargando = true
        btnCargarMas.isEnabled = false
        ejecutarSeguro(executor) {
            try {
                val pagina = ControladorPublicaciones.obtenerPaginaPropias(this, cursorPagina)
                actualizarUi {
                    cursorPagina = pagina.cursor
                    hayMas = pagina.hayMas
                    primeraPaginaCargada = true
                    cargando = false
                    btnCargarMas.isEnabled = true
                    btnCargarMas.visibility = if (hayMas) View.VISIBLE else View.GONE
                    btnCargarMas.setText(R.string.load_more)
                    publicaciones = (if (reset) emptyList() else publicaciones) + pagina.items
                    tvVacio.setText(R.string.empty_posts)
                    tvVacio.visibility = if (publicaciones.isEmpty()) View.VISIBLE else View.GONE
                    val layoutManager = rv.layoutManager as? LinearLayoutManager
                    val firstVisible = layoutManager?.findFirstVisibleItemPosition() ?: 0
                    val firstOffset = rv.getChildAt(0)?.top ?: 0
                    rv.adapter = AdaptadorPublicacionesPropias(
                        publicaciones,
                        onResolver = { publicacion -> resolverPublicacion(publicacion) },
                        onAdoptar = { publicacion -> adoptarPublicacion(publicacion) },
                        onEditar = { publicacion ->
                            actualizarAlVolver = true
                            startActivity(android.content.Intent(this, CrearPublicacionActivity::class.java).apply {
                                putExtra(CrearPublicacionActivity.EXTRA_EDIT_ID, publicacion.id)
                            })
                        },
                        onEliminar = { publicacion -> confirmarRetiro(publicacion.id) },
                        onConfirmar = { publicacion -> confirmarSiguePerdida(publicacion) },
                        onVerAvistamientos = { publicacion ->
                            startActivity(android.content.Intent(this, DetallePublicacionActivity::class.java).apply {
                                putExtra(DetallePublicacionActivity.EXTRA_PUBLICACION_ID, publicacion.id)
                            })
                        }
                    )
                    if (!reset && firstVisible >= 0) {
                        rv.post { layoutManager?.scrollToPositionWithOffset(firstVisible, firstOffset) }
                    } else {
                        rv.scrollToPosition(0)
                    }
                }
            } catch (error: Exception) {
                android.util.Log.e("PetGo", "No se pudieron cargar las publicaciones propias", error)
                actualizarUi {
                    cargando = false
                    btnCargarMas.isEnabled = true
                    mostrandoErrorCarga = true
                    tvVacio.text = getString(R.string.posts_load_failed)
                    tvVacio.visibility = View.VISIBLE
                    btnCargarMas.setText(R.string.retry)
                    btnCargarMas.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun mostrarErrorCarga(mensaje: String) {
        mostrandoErrorCarga = true
        tvVacio.text = mensaje
        tvVacio.visibility = View.VISIBLE
        btnCargarMas.setText(R.string.retry)
        btnCargarMas.visibility = View.VISIBLE
        btnCargarMas.isEnabled = true
    }

    private fun resolverPublicacion(publicacion: com.example.practicafinal.modelo.Publicacion) {
        ejecutarSeguro(executor) {
            ControladorPublicaciones.resolver(this, publicacion.id)
            actualizarUi {
                Toast.makeText(this, "Publicacion resuelta", Toast.LENGTH_SHORT).show()
                cargarPublicaciones()
            }
        }
    }

    private fun adoptarPublicacion(publicacion: com.example.practicafinal.modelo.Publicacion) {
        ejecutarSeguro(executor) {
            ControladorPublicaciones.adoptar(this, publicacion.id)
            actualizarUi {
                Toast.makeText(this, "Mascota marcada como adoptada", Toast.LENGTH_SHORT).show()
                cargarPublicaciones()
            }
        }
    }

    private fun confirmarRetiro(id: Long) {
        AlertDialog.Builder(this)
            .setTitle("Retirar publicación")
            .setMessage("La publicación dejará de aparecer en el mapa y en las búsquedas. Se conservará el historial de avistamientos.")
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Retirar") { _, _ ->
                ejecutarSeguro(executor) {
                    ControladorPublicaciones.eliminar(this, id)
                    actualizarUi {
                        Toast.makeText(this, "Publicación retirada", Toast.LENGTH_SHORT).show()
                        cargarPublicaciones()
                    }
                }
            }
            .show()
    }

    private fun confirmarSiguePerdida(publicacion: com.example.practicafinal.modelo.Publicacion) {
        AlertDialog.Builder(this)
            .setTitle("¿Sigue perdida ${publicacion.nombre}?")
            .setMessage("Confirmar la alerta la mantiene actualizada para la comunidad. También puedes marcarla como resuelta si ya volvió a casa.")
            .setNegativeButton("Ahora no", null)
            .setPositiveButton("Sigue perdida") { _, _ ->
                ejecutarSeguro(executor) {
                    try {
                        ControladorPublicaciones.confirmarAlertaPerdida(this, publicacion.id)
                        actualizarUi {
                            Toast.makeText(this, "Alerta confirmada y actualizada", Toast.LENGTH_SHORT).show()
                            cargarPublicaciones()
                        }
                    } catch (error: Exception) {
                        actualizarUi {
                            Toast.makeText(this, error.message ?: "No se pudo actualizar la alerta", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
            .show()
            .also { com.example.practicafinal.util.Dialogos.aplicar(this, it) }
    }

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdown()
    }

    override fun onResume() {
        super.onResume()
        if (::rv.isInitialized && !cargando && (!primeraPaginaCargada || actualizarAlVolver)) {
            actualizarAlVolver = false
            cargarPublicaciones()
        }
    }
}
