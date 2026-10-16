package com.example.practicafinal.util

import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException

fun mensajeErrorAuth(error: Exception, registro: Boolean = false): String {
    val causas = generateSequence(error as Throwable) { it.cause }.take(10).toList()
    if (causas.any { it is FirebaseNetworkException || it is java.util.concurrent.TimeoutException }) {
        return "Sin conexión a internet. Verifica tu red"
    }
    val codigo = causas.filterIsInstance<FirebaseAuthException>().firstOrNull()?.errorCode
    return when (codigo) {
        "ERROR_EMAIL_ALREADY_IN_USE" -> "Ese correo ya está registrado"
        "ERROR_WEAK_PASSWORD" -> "La contraseña debe tener al menos 6 caracteres"
        "ERROR_INVALID_EMAIL" -> "El correo electrónico no es válido"
        "ERROR_USER_NOT_FOUND", "ERROR_WRONG_PASSWORD", "ERROR_INVALID_CREDENTIAL", "ERROR_INVALID_LOGIN_CREDENTIALS" ->
            "Correo o contraseña incorrectos"
        "ERROR_USER_DISABLED" -> "Esta cuenta está deshabilitada"
        "ERROR_TOO_MANY_REQUESTS" -> "Demasiados intentos. Espera unos minutos"
        else -> if (registro) "No se pudo crear la cuenta. Vuelve a intentarlo" else "No se pudo iniciar sesión. Vuelve a intentarlo"
    }
}
