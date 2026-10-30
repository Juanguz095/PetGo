package com.example.practicafinal.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Base64
import android.widget.ImageView
import com.bumptech.glide.Glide
import java.util.concurrent.Executors

private val decodificadorBase64 = Executors.newFixedThreadPool(2)
private val handlerImagenes = Handler(Looper.getMainLooper())

fun decodificarImagen(context: Context, uriStr: String, sample: Int = 4): Bitmap? =
    try {
        val uri = Uri.parse(uriStr)
        context.contentResolver.openInputStream(uri)?.use { input ->
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            BitmapFactory.decodeStream(input, null, opts)
        }
    } catch (_: Exception) {
        null
    }

fun limpiarImagen(imageView: ImageView) {
    Glide.with(imageView).clear(imageView)
    imageView.tag = null
    imageView.setImageDrawable(null)
}

fun cargarImagen(imageView: ImageView, foto: String?) {
    Glide.with(imageView).clear(imageView)
    val requestToken = Any()
    imageView.tag = requestToken
    val dataUri = foto?.takeIf { it.startsWith("data:image") }
    if (dataUri != null) {
        imageView.setImageResource(android.R.drawable.ic_menu_gallery)
        val esperado = dataUri
        decodificadorBase64.execute {
            val bytes = try {
                Base64.decode(esperado.substringAfter("base64,"), Base64.NO_WRAP)
            } catch (_: IllegalArgumentException) {
                null
            }
            handlerImagenes.post {
                if (imageView.tag === requestToken && bytes != null) {
                    try {
                        Glide.with(imageView).load(bytes)
                            .placeholder(android.R.drawable.ic_menu_gallery)
                            .error(android.R.drawable.ic_menu_gallery)
                            .into(imageView)
                    } catch (_: RuntimeException) {
                        // La vista pudo pertenecer a una actividad que ya se cerró.
                    }
                }
            }
        }
        return
    }

    Glide.with(imageView).load(foto)
        .placeholder(android.R.drawable.ic_menu_gallery)
        .fallback(android.R.drawable.ic_menu_gallery)
        .error(android.R.drawable.ic_menu_gallery)
        .into(imageView)
}
