package com.example.practicafinal.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat

fun tienePermisoUbicacion(context: Context): Boolean =
    listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        .any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }

fun ultimaUbicacion(context: Context): Location? {
    if (!tienePermisoUbicacion(context)) return null
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    val ahora = System.currentTimeMillis()
    return manager.allProviders.mapNotNull { provider ->
        try { manager.getLastKnownLocation(provider) } catch (_: SecurityException) { null }
            catch (_: IllegalArgumentException) { null }
    }.filter { it.time <= ahora && it.time >= ahora - MAX_ANTIGUEDAD_UBICACION_MS }
        .maxByOrNull { it.time }
}

private const val MAX_ANTIGUEDAD_UBICACION_MS = 15 * 60 * 1000L
