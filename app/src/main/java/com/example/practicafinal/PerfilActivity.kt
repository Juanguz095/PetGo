package com.example.practicafinal

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import com.example.practicafinal.util.ejecutarSeguro
import com.example.practicafinal.util.actualizarUi
import androidx.appcompat.app.AppCompatActivity
import com.example.practicafinal.controlador.ControladorDenuncias
import com.example.practicafinal.controlador.ControladorPublicaciones
import com.example.practicafinal.controlador.ControladorUsuarios
import com.example.practicafinal.session.FavoritosManager
import com.example.practicafinal.session.SesionManager
import java.util.concurrent.Executors

class PerfilActivity : AppCompatActivity() {

    private val executor = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_perfil)

        findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
            .setNavigationOnClickListener { finish() }

        findViewById<View>(R.id.row_cerrar_sesion).setOnClickListener {
            SesionManager.cerrarSesion(this)
            startActivity(Intent(this, LoginActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            })
            finish()
        }

        
        findViewById<View>(R.id.row_publicaciones).setOnClickListener {
            startActivity(Intent(this, MisPublicacionesActivity::class.java))
        }

        
        findViewById<View>(R.id.row_denuncias).setOnClickListener {
            startActivity(Intent(this, DenunciasActivity::class.java))
        }

        
        findViewById<View>(R.id.row_favoritos).setOnClickListener {
            startActivity(Intent(this, MisFavoritosActivity::class.java))
        }

        
        findViewById<View>(R.id.row_configuracion).setOnClickListener {
            startActivity(Intent(this, ConfiguracionActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        ejecutarSeguro(executor) {
            val usuarioId = SesionManager.obtenerUsuarioId(this)
            val usuario = usuarioId?.let { ControladorUsuarios.obtenerPorId(this, it) }
            val publicaciones = if (usuarioId != null)
                ControladorPublicaciones.obtenerPropias(this).size else 0
            val denuncias = ControladorDenuncias.obtenerDenuncias(this).size
            val favoritos = FavoritosManager.contar(this)
            actualizarUi {
                usuario?.let {
                    findViewById<TextView>(R.id.tv_nombre).text = it.nombre
                    findViewById<TextView>(R.id.tv_correo).text = it.correo
                    findViewById<TextView>(R.id.tv_tipo).text = it.tipo
                }
                findViewById<TextView>(R.id.tv_publicaciones).text = publicaciones.toString()
                findViewById<TextView>(R.id.tv_denuncias).text = denuncias.toString()
                findViewById<TextView>(R.id.tv_favoritos).text = favoritos.toString()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy(); executor.shutdown()
    }
}
