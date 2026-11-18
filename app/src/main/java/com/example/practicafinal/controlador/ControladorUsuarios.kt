package com.example.practicafinal.controlador

import android.content.Context
import com.example.practicafinal.db.DatabaseHelper
import com.example.practicafinal.modelo.Usuario
import com.example.practicafinal.modelo.PreferenciasAlerta
import com.example.practicafinal.modelo.ResultadoRegistro


object ControladorUsuarios {


    fun validarRegistro(
        nombre: String, correo: String,
        contrasena: String, confirmar: String
    ): String? {
        if (nombre.isBlank() || correo.isBlank() || contrasena.isBlank() || confirmar.isBlank())
            return "Completa todos los campos"
        if (contrasena.length < 6)
            return "La contraseña debe tener al menos 6 caracteres"
        if (contrasena != confirmar)
            return "Las contraseñas no coinciden"
        return null
    }


    fun registrar(
        context: Context,
        nombre: String,
        correo: String,
        contrasena: String,
        ciudad: String = ""
    ): ResultadoRegistro {
        return DatabaseHelper(context).registrar(nombre, correo, contrasena, "Ciudadano", ciudad)
    }

    fun reintentarGuardarPerfilPendiente(context: Context): Usuario? =
        DatabaseHelper(context).reintentarGuardarPerfilPendiente()


    fun iniciarSesion(context: Context, correo: String, contrasena: String): Usuario? {
        return DatabaseHelper(context).validarLogin(correo, contrasena)
    }


    fun obtenerPorId(context: Context, id: Long): Usuario? {
        return DatabaseHelper(context).obtenerPorId(id)
    }

    fun guardarPreferenciaAlerta(context: Context, pref: PreferenciasAlerta, latitud: Double?, longitud: Double?) =
        DatabaseHelper(context).guardarPreferenciaAlerta(pref, latitud, longitud)

    fun obtenerPreferenciaAlerta(context: Context): PreferenciasAlerta =
        DatabaseHelper(context).obtenerPreferenciaAlerta()
}
