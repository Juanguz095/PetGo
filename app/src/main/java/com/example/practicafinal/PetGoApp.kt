package com.example.practicafinal

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.example.practicafinal.util.NotificacionHelper
import com.google.firebase.FirebaseApp

class PetGoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        NotificacionHelper.crearCanal(this)

        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    }
}
