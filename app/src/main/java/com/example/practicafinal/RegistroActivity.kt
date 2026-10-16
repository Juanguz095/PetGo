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

        findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_registrar)
            .setOnClickListener { registrar() }

        findViewById<android.widget.TextView>(R.id.tv_ir_login)
            .setOnClickListener { finish() }
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

        findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_registrar).isEnabled = false
        ejecutarSeguro(executor) {
            try {
                val id = ControladorUsuarios.registrar(this, nombre, correo, contrasena, ciudad)
                actualizarUi {
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
                actualizarUi {
                    findViewById<com.google.android.material.button.MaterialButton>(R.id.btn_registrar).isEnabled = true
                    mostrarError(traducirErrorFirebase(e))
                }
            }
        }
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
