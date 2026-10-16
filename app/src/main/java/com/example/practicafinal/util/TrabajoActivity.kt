package com.example.practicafinal.util

import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.practicafinal.R
import java.util.concurrent.ExecutorService
import java.util.concurrent.RejectedExecutionException

/** Evita actualizar una pantalla cerrada y muestra los fallos de red sin cerrar la app. */
fun AppCompatActivity.actualizarUi(action: () -> Unit) {
    runOnUiThread {
        if (!isFinishing && !isDestroyed) action()
    }
}

fun AppCompatActivity.ejecutarSeguro(executor: ExecutorService, action: () -> Unit) {
    if (isFinishing || isDestroyed || executor.isShutdown) return
    try {
        executor.execute {
            if (isFinishing || isDestroyed) return@execute
            try {
                action()
            } catch (error: Exception) {
                Log.e("PetGo", "No se pudo completar la operación", error)
                actualizarUi {
                    Toast.makeText(this, R.string.operation_failed, Toast.LENGTH_LONG).show()
                }
            }
        }
    } catch (_: RejectedExecutionException) {
        // La actividad puede cerrarse mientras se envía una tarea al ejecutor.
    }
}
