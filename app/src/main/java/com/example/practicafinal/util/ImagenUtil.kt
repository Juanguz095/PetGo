package com.example.practicafinal.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.widget.ImageView
import com.bumptech.glide.Glide

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
    imageView.setImageDrawable(null)
}

fun cargarImagen(imageView: ImageView, foto: String?) {
    // Al reciclar una tarjeta, cancelar la petici?n anterior evita que aparezca otra mascota.
    Glide.with(imageView).clear(imageView)
    val contenido: Any? = if (foto?.startsWith("data:image") == true) {
        try {
            Base64.decode(foto.substringAfter("base64,"), Base64.NO_WRAP)
        } catch (_: IllegalArgumentException) {
            null
        }
    } else foto
    Glide.with(imageView).load(contenido)
        .placeholder(android.R.drawable.ic_menu_gallery)
        .fallback(android.R.drawable.ic_menu_gallery)
        .error(android.R.drawable.ic_menu_gallery)
        .into(imageView)
}
