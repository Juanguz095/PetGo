package com.example.practicafinal

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import com.example.practicafinal.controlador.ControladorUsuarios
import com.example.practicafinal.modelo.PreferenciasAlerta
import com.example.practicafinal.util.actualizarUi
import com.example.practicafinal.util.ejecutarSeguro
import com.example.practicafinal.util.tienePermisoUbicacion
import com.example.practicafinal.util.ultimaUbicacion
import com.google.android.material.appbar.MaterialToolbar
import java.util.concurrent.Executors

class ConfiguracionActivity : AppCompatActivity() {

    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var switchAlertas: SwitchCompat
    private lateinit var rowRadios: View
    private var radioKm = 10

    private val permisoUbicacion = registerForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        if (concedido) guardarAlertas(habilitadas = true)
        else {
            switchAlertas.isChecked = false
            rowRadios.visibility = View.GONE
            Toast.makeText(this, "Se necesita la ubicación para recibir alertas cercanas", Toast.LENGTH_LONG).show()
        }
    }

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

        switchAlertas = findViewById(R.id.switch_alertas_cercanas)
        rowRadios = findViewById(R.id.row_radios_alerta)

        switchAlertas.setOnCheckedChangeListener { _, checked ->
            rowRadios.visibility = if (checked) View.VISIBLE else View.GONE
            if (checked) {
                if (tienePermisoUbicacion(this)) guardarAlertas(habilitadas = true)
                else permisoUbicacion.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            } else {
                guardarAlertas(habilitadas = false)
            }
        }

        val chips = listOf(
            R.id.chip_radio_alerta_5 to 5,
            R.id.chip_radio_alerta_10 to 10,
            R.id.chip_radio_alerta_25 to 25,
            R.id.chip_radio_alerta_50 to 50
        )
        chips.forEach { (id, km) ->
            findViewById<TextView>(id).setOnClickListener {
                radioKm = km
                pintarRadios(chips)
                guardarAlertas(habilitadas = switchAlertas.isChecked)
            }
        }
        pintarRadios(chips)
    }

    private fun pintarRadios(chips: List<Pair<Int, Int>>) {
        chips.forEach { (id, km) ->
            val activo = km == radioKm
            findViewById<TextView>(id).background = ContextCompat.getDrawable(
                this,
                if (activo) R.drawable.bg_chip_selected else R.drawable.bg_chip
            )
            findViewById<TextView>(id).setTextColor(
                ContextCompat.getColor(this, if (activo) R.color.tinta else R.color.texto_suave)
            )
        }
    }

    private fun guardarAlertas(habilitadas: Boolean) {
        ejecutarSeguro(executor) {
            try {
                val ubicacion = if (habilitadas) ultimaUbicacion(this) else null
                ControladorUsuarios.guardarPreferenciaAlerta(
                    this,
                    PreferenciasAlerta(habilitadas, radioKm),
                    ubicacion?.latitude,
                    ubicacion?.longitude
                )
                actualizarUi {
                    Toast.makeText(this, "Preferencia de alertas actualizada", Toast.LENGTH_SHORT).show()
                }
            } catch (error: Exception) {
                actualizarUi {
                    Toast.makeText(this, R.string.operation_failed, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        ejecutarSeguro(executor) {
            val pref = runCatching { ControladorUsuarios.obtenerPreferenciaAlerta(this) }
                .getOrDefault(PreferenciasAlerta())
            actualizarUi {
                radioKm = pref.radioKm
                switchAlertas.setOnCheckedChangeListener(null)
                switchAlertas.isChecked = pref.habilitadas
                switchAlertas.setOnCheckedChangeListener { _, checked ->
                    rowRadios.visibility = if (checked) View.VISIBLE else View.GONE
                    if (checked) {
                        if (tienePermisoUbicacion(this)) guardarAlertas(habilitadas = true)
                        else permisoUbicacion.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                    } else {
                        guardarAlertas(habilitadas = false)
                    }
                }
                rowRadios.visibility = if (pref.habilitadas) View.VISIBLE else View.GONE
                pintarRadios(listOf(
                    R.id.chip_radio_alerta_5 to 5,
                    R.id.chip_radio_alerta_10 to 10,
                    R.id.chip_radio_alerta_25 to 25,
                    R.id.chip_radio_alerta_50 to 50
                ))
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdown()
    }
}
