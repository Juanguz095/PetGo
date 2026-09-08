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

class RegistroActivity : AppCompatActivity() {

    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var etNombre: EditText
    private lateinit var etCorreo: EditText
    private lateinit var etContrasena: EditText
    private lateinit var etConfirmar: EditText
    private lateinit var tvError: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_registro)

        findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
            .setNavigationOnClickListener { finish() }

        etNombre = findViewById(R.id.et_nombre)
        etCorreo = findViewById(R.id.et_correo)
        etContrasena = findViewById(R.id.et_contrasena)
        etConfirmar = findViewById(R.id.et_confirmar)
        tvError = findViewById(R.id.tv_error)

        findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_registrar)
            .setOnClickListener { registrar() }
    }

    private fun registrar() {
        val nombre = etNombre.text.toString().trim()
        val correo = etCorreo.text.toString().trim()
        val contrasena = etContrasena.text.toString()
        val confirmar = etConfirmar.text.toString()

        val error = ControladorUsuarios.validarRegistro(nombre, correo, contrasena, confirmar)
        if (error != null) {
            mostrarError(error); return
        }

        findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_registrar).isEnabled = false
        executor.execute {
            try {
                val id = ControladorUsuarios.registrar(this, nombre, correo, contrasena)
                runOnUiThread {
                    findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_registrar).isEnabled = true
                    if (id != null) {
                        SesionManager.guardarSesion(this, id)
                        FavoritosManager.sincronizar(this)
                        Toast.makeText(this, "¡Cuenta creada!", Toast.LENGTH_SHORT).show()
                        irAlMapa()
                    } else {
                        mostrarError("No se pudo crear la cuenta")
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_registrar).isEnabled = true
                    mostrarError(traducirErrorFirebase(e))
                }
            }
        }
    }

    private fun traducirErrorFirebase(e: Exception): String {
        val msg = e.message ?: ""
        return when {
            msg.contains("EMAIL_ALREADY_IN_USE", true) || msg.contains("email-already-in-use", true) ->
                "Ese correo ya está registrado"
            msg.contains("WEAK_PASSWORD", true) || msg.contains("password", true) && msg.contains("too short", true) ->
                "La contraseña es muy débil (mínimo 6 caracteres)"
            msg.contains("INVALID_EMAIL", true) || msg.contains("malformed", true) ->
                "El correo electrónico no es válido"
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
