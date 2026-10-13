package com.example.practicafinal

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar

class ConfiguracionActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_configuracion)

        findViewById<MaterialToolbar>(R.id.toolbar).setNavigationOnClickListener { finish() }

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
}
