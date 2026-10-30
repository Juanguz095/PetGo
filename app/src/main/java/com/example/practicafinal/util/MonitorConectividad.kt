package com.example.practicafinal.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Handler
import android.os.Looper

class MonitorConectividad(context: Context, private val alCambiar: (Boolean) -> Unit) {
    private val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    private val mainHandler = Handler(Looper.getMainLooper())
    private var registrado = false
    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) = notificar()
        override fun onLost(network: Network) = notificar()
        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) = notificar()
    }

    fun iniciar() {
        if (registrado) return
        registrado = true
        runCatching {
            manager.registerNetworkCallback(
                NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build(),
                callback
            )
        }.onFailure { registrado = false }
        notificar()
    }

    fun detener() {
        if (!registrado) return
        runCatching { manager.unregisterNetworkCallback(callback) }
        registrado = false
    }

    fun hayInternet(): Boolean = runCatching {
        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }.getOrDefault(false)

    private fun notificar() {
        mainHandler.post { alCambiar(hayInternet()) }
    }
}
