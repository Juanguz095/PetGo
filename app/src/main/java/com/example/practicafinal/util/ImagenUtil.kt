package com.example.practicafinal.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
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

fun cargarImagen(imageView: ImageView, foto: String?) {
    if (foto.isNullOrBlank()) {
        imageView.setImageResource(android.R.drawable.ic_menu_gallery)
        return
    }
    val context = imageView.context
    if (foto.startsWith("data:image")) {
        try {
            val base64 = foto.substringAfter("base64,")
            val bytes = Base64.decode(base64, Base64.NO_WRAP)
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap)
                return
            }
        } catch (_: Exception) {}
        imageView.setImageResource(android.R.drawable.ic_menu_gallery)
        return
    }
    val uri = Uri.parse(foto)
    if (uri.scheme == "android.resource") {
        val resId = context.resources.getIdentifier(uri.lastPathSegment, "drawable", context.packageName)
        if (resId != 0) { imageView.setImageResource(resId); return }
    }
    Glide.with(context).load(uri).error(android.R.drawable.ic_menu_gallery).into(imageView)
}
