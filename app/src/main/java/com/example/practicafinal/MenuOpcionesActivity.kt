package com.example.practicafinal

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class MenuOpcionesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_menu_opciones)

        findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
            .setNavigationOnClickListener { finish() }

        findViewById<View>(R.id.item_perdidas).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Mascotas de la comunidad")
                .setItems(arrayOf("Mascotas perdidas", "Mascotas encontradas")) { _, which ->
                    val tipo = if (which == 1) "Encontrada" else "Perdida"
                    startActivity(Intent(this, MascotasPerdidasActivity::class.java)
                        .putExtra(MascotasPerdidasActivity.EXTRA_TIPO_LISTADO, tipo))
                }
                .show()
        }

        findViewById<View>(R.id.item_adopciones).setOnClickListener {
            startActivity(Intent(this, AdopcionesActivity::class.java))
        }

        findViewById<View>(R.id.item_albergues).setOnClickListener {
            startActivity(Intent(this, AlberguesActivity::class.java))
        }

        findViewById<View>(R.id.item_denuncias).setOnClickListener {
            startActivity(Intent(this, DenunciasActivity::class.java))
        }

        findViewById<View>(R.id.item_favoritos).setOnClickListener {
            startActivity(Intent(this, MisFavoritosActivity::class.java))
        }

        findViewById<View>(R.id.item_seguimiento).setOnClickListener {
            startActivity(Intent(this, MisPublicacionesActivity::class.java))
        }
    }
}
