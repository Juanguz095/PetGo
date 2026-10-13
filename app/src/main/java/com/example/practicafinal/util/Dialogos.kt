package com.example.practicafinal.util

import android.content.Context
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.example.practicafinal.R

object Dialogos {

    fun aplicar(context: Context, dlg: AlertDialog) {
        dlg.window?.setBackgroundDrawableResource(R.drawable.bg_dialogo)
        dlg.getButton(AlertDialog.BUTTON_POSITIVE)
            .setTextColor(ContextCompat.getColor(context, R.color.tinta))
    }
}
