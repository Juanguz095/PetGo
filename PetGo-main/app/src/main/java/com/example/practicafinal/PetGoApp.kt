package com.example.practicafinal

import android.app.Application
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import com.example.practicafinal.util.NotificacionHelper
import com.google.firebase.FirebaseApp

class PetGoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        NotificacionHelper.crearCanal(this)

        val prefs = getSharedPreferences("configuracion", MODE_PRIVATE)
        val modo = prefs.getInt("modo_tema", -1)

        if (modo == -1) {
            val isDark = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                resources.configuration.isNightModeActive
            } else {
                val flags = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
                flags == android.content.res.Configuration.UI_MODE_NIGHT_YES
            }
            prefs.edit().putInt("modo_tema",
                if (isDark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            ).apply()
            AppCompatDelegate.setDefaultNightMode(
                if (isDark) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            )
        } else {
            AppCompatDelegate.setDefaultNightMode(modo)
        }
    }
}
