package com.manha.eventassettracker.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

object WhatsAppShare {

    private const val WHATSAPP_PACKAGE = "com.whatsapp"

    /** Shares plain text (a report) directly to WhatsApp if installed, else falls back to the
     *  normal Android share sheet so the user can still pick any other app. */
    fun shareText(context: Context, text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        try {
            intent.setPackage(WHATSAPP_PACKAGE)
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            try {
                intent.setPackage(null)
                context.startActivity(Intent.createChooser(intent, "Share report via"))
            } catch (e2: Exception) {
                Toast.makeText(context, "Koi sharing app nahi mila.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /** Shares a PDF (already-saved content:// Uri) to WhatsApp, with a caption. */
    fun sharePdf(context: Context, uri: Uri, caption: String = "") {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            if (caption.isNotBlank()) putExtra(Intent.EXTRA_TEXT, caption)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            intent.setPackage(WHATSAPP_PACKAGE)
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            try {
                intent.setPackage(null)
                context.startActivity(Intent.createChooser(intent, "Share PDF via"))
            } catch (e2: Exception) {
                Toast.makeText(context, "Koi sharing app nahi mila.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
