package com.example.practicafinal

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import com.example.practicafinal.util.ejecutarSeguro
import com.example.practicafinal.util.actualizarUi
import androidx.appcompat.app.AppCompatActivity
import com.example.practicafinal.controlador.ControladorUsuarios
import com.example.practicafinal.modelo.ResultadoRegistro
import com.example.practicafinal.modelo.Usuario
import com.example.practicafinal.session.FavoritosManager
import com.example.practicafinal.session.SesionManager
import java.util.concurrent.Executors

class RegistroActivity : AppCompatActivity() {

    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var etNombre: EditText
    private lateinit var etCorreo: EditText
    private lateinit var etContrasena: EditText
    private lateinit var etConfirmar: EditText
    private lateinit var etCiudad: EditText
    private lateinit var tvError: TextView
    private lateinit var btnRegistrar: com.google.android.material.button.MaterialButton
    private lateinit var tvIrLogin: TextView
    private var perfilPendiente = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro)

        findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
            .setNavigationOnClickListener { finish() }

        etNombre = findViewById(R.id.et_nombre)
        etCorreo = findViewById(R.id.et_correo)
        etContrasena = findViewById(R.id.et_contrasena)
        etConfirmar = findViewById(R.id.et_confirmar)
        etCiudad = findViewById(R.id.et_ciudad)
        tvError = findViewById(R.id.tv_error)

        btnRegistrar = findViewById(R.id.btn_registrar)
        btnRegistrar.setOnClickListener { if (perfilPendiente) reintentarPerfil() else registrar() }

        tvIrLogin = findViewById(R.id.tv_ir_login)
        tvIrLogin.setOnClickListener { finish() }

        SesionManager.obtenerRegistroPendiente(this)?.let {
            mostrarPerfilPendiente(it.usuario)
            mostrarError(getString(R.string.registration_profile_pending))
        }
    }

    private fun registrar() {
        val nombre = etNombre.text.toString().trim()
        val correo = etCorreo.text.toString().trim()
        val contrasena = etContrasena.text.toString()
        val confirmar = etConfirmar.text.toString()
        val ciudad = etCiudad.text.toString().trim()

        val error = ControladorUsuarios.validarRegistro(nombre, correo, contrasena, confirmar, ciudad)
        if (error != null) {
            mostrarError(error); return
        }

        btnRegistrar.isEnabled = false
        ejecutarSeguro(executor) {
            try {
                val resultado = ControladorUsuarios.registrar(this, nombre, correo, contrasena, ciudad)
                actualizarUi {
                    btnRegistrar.isEnabled = true
                    when (resultado) {
                        is ResultadoRegistro.Completo -> completarRegistro()
                        is ResultadoRegistro.PerfilPendiente -> {
                            mostrarPerfilPendiente(resultado.usuario)
                            mostrarError(getString(R.string.registration_profile_pending))
                        }
                    }
                }
            } catch (e: Exception) {
                actualizarUi {
                    btnRegistrar.isEnabled = true
                    mostrarError(traducirErrorFirebase(e))
                }
            }
        }
    }

    private fun mostrarPerfilPendiente(usuario: Usuario) {
        perfilPendiente = true
        etNombre.setText(usuario.nombre)
        etCorreo.setText(usuario.correo)
        etCiudad.setText(usuario.ciudad)
        etNombre.isEnabled = false
        etCorreo.isEnabled = false
        etCiudad.isEnabled = false
        etContrasena.text.clear()
        etConfirmar.text.clear()
        etContrasena.visibility = View.GONE
        etConfirmar.visibility = View.GONE
        findViewById<View>(R.id.label_contrasena).visibility = View.GONE
        findViewById<View>(R.id.label_confirmar).visibility = View.GONE
        btnRegistrar.text = getString(R.string.retry_profile_save)
        tvIrLogin.visibility = View.GONE
    }

    private fun reintentarPerfil() {
        btnRegistrar.isEnabled = false
        ejecutarSeguro(executor) {
            try {
                val usuario = ControladorUsuarios.reintentarGuardarPerfilPendiente(this)
                actualizarUi {
                    btnRegistrar.isEnabled = true
                    if (usuario != null) completarRegistro()
                    else mostrarError(getString(R.string.registration_profile_retry_failed))
                }
            } catch (error: Exception) {
                android.util.Log.e("PetGo", "No se pudo reintentar el perfil", error)
                actualizarUi {
                    btnRegistrar.isEnabled = true
                    mostrarError(getString(R.string.registration_profile_retry_failed))
                }
            }
        }
    }

    private fun completarRegistro() {
        FavoritosManager.sincronizar(this)
        Toast.makeText(this, getString(R.string.account_ready), Toast.LENGTH_SHORT).show()
        irAlMapa()
    }

    private fun traducirErrorFirebase(e: Exception): String =
        com.example.practicafinal.util.mensajeErrorAuth(e, registro = true)

    private fun mostrarError(mensaje: String) {
        tvError.text = mensaje
        tvError.visibility = View.VISIBLE
    }

    private fun irAlMapa() {
        startActivity(
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
        )
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdown()
    }
}
