package com.example.practicafinal

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.switchmaterial.SwitchMaterial

class ConfiguracionActivity : AppCompatActivity() {

    private lateinit var switchOscuro: SwitchMaterial

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_configuracion)

        findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }

        switchOscuro = findViewById(R.id.switch_modo_oscuro)

        val saved = getSharedPreferences("configuracion", MODE_PRIVATE)
            .getInt("modo_tema", -1)

        if (saved == -1) {
            val isSystemDark = isSystemDarkMode()
            switchOscuro.isChecked = isSystemDark
        } else {
            switchOscuro.isChecked = saved == AppCompatDelegate.MODE_NIGHT_YES
        }

        switchOscuro.setOnCheckedChangeListener { _, isChecked ->
            val mode = if (isChecked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
            getSharedPreferences("configuracion", MODE_PRIVATE)
                .edit().putInt("modo_tema", mode).apply()
            AppCompatDelegate.setDefaultNightMode(mode)
            recreate()
        }

        findViewById<LinearLayout>(R.id.opcion_perfil).setOnClickListener {
            startActivity(Intent(this, PerfilActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.opcion_favoritos).setOnClickListener {
            startActivity(Intent(this, MisFavoritosActivity::class.java))
        }
        findViewById<LinearLayout>(R.id.opcion_publicaciones).setOnClickListener {
            startActivity(Intent(this, MisPublicacionesActivity::class.java))
        }
    }

    private fun isSystemDarkMode(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            resources.configuration.isNightModeActive
        } else {
            val nightModeFlags = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
            nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES
        }
    }
}
