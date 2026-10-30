package com.example.practicafinal.modelo

sealed class ResultadoRegistro {
    data class Completo(val usuario: Usuario) : ResultadoRegistro()
    data class PerfilPendiente(val usuario: Usuario, val error: Exception) : ResultadoRegistro()
}
