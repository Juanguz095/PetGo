package com.example.practicafinal

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.practicafinal.controlador.ControladorUsuarios
import com.example.practicafinal.session.FavoritosManager
import com.example.practicafinal.session.SesionManager
import java.util.concurrent.Executors

class LoginActivity : AppCompatActivity() {

    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var etCorreo: EditText
    private lateinit var etContrasena: EditText
    private lateinit var tvError: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        
        if (SesionManager.tieneSesion(this)) {
            irAlMapa()
            return
        }

        setContentView(R.layout.activity_login)

        etCorreo = findViewById(R.id.et_correo)
        etContrasena = findViewById(R.id.et_contrasena)
        tvError = findViewById(R.id.tv_error)

        findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_login)
            .setOnClickListener { iniciarSesion() }

        findViewById<TextView>(R.id.tv_ir_registro)
            .setOnClickListener {
                startActivity(Intent(this, RegistroActivity::class.java))
            }
    }

    private fun iniciarSesion() {
        val correo = etCorreo.text.toString().trim()
        val contrasena = etContrasena.text.toString()

        if (correo.isEmpty() || contrasena.isEmpty()) {
            mostrarError("Completa todos los campos")
            return
        }

        findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_login).isEnabled = false
        executor.execute {
            try {
                val usuario = ControladorUsuarios.iniciarSesion(this, correo, contrasena)
                runOnUiThread {
                    findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_login).isEnabled = true
                    if (usuario != null) {
                        SesionManager.guardarSesion(this, usuario.id)
                        FavoritosManager.sincronizar(this)
                        Toast.makeText(this, "¡Hola, ${usuario.nombre}!", Toast.LENGTH_SHORT).show()
                        irAlMapa()
                    } else {
                        mostrarError("Correo o contraseña incorrectos")
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_login).isEnabled = true
                    mostrarError(traducirErrorFirebase(e))
                }
            }
        }
    }

    private fun traducirErrorFirebase(e: Exception): String {
        val msg = e.message ?: ""
        return when {
            msg.contains("USER_NOT_FOUND", true) || msg.contains("user-not-found", true) ->
                "No existe una cuenta con ese correo"
            msg.contains("wrong-password", true) || msg.contains("INVALID_PASSWORD", true) ->
                "Contraseña incorrecta"
            msg.contains("INVALID_EMAIL", true) || msg.contains("malformed", true) ->
                "El correo electrónico no es válido"
            msg.contains("TOO_MANY_REQUESTS", true) || msg.contains("too-many-requests", true) ->
                "Demasiados intentos. Espera unos minutos"
            msg.contains("network", true) || msg.contains("timeout", true) || msg.contains("unavailable", true) ->
                "Sin conexión a internet. Verifica tu red"
            msg.contains("API key not valid", true) || msg.contains("api_key", true) ->
                "Error de configuración de Firebase"
            else -> "Error: ${e.localizedMessage ?: "Error desconocido"}"
        }
    }

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
