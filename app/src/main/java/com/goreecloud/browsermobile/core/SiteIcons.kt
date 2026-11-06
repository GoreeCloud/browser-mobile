package com.goreecloud.browsermobile.core

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream
import java.io.File
import java.security.MessageDigest

/**
 * Saves only favicons provided by visited pages through WebView.onReceivedIcon.
 * No third-party favicon API, tracking pixels, favicon scraping, or network fetch.
 */
class SiteIcons(context: Context) {
    private val directory = File(context.filesDir, "site-favicons").apply { mkdirs() }

    private fun fileFor(host: String): File {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(host.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        return File(directory, "$digest.png")
    }

    fun load(host: String): Bitmap? {
        if (host.length !in 1..253) return null
        val file = fileFor(host)
        if (!file.isFile || file.length() !in 1..65_536) return null
        return try { BitmapFactory.decodeFile(file.absolutePath) } catch (_: Exception) { null }
    }

    fun save(pageUrl: String, favicon: Bitmap): Boolean {
        val host = VisitPolicy.host(pageUrl) ?: return false
        return try {
            val scaled = Bitmap.createScaledBitmap(favicon, 72, 72, true)
            val stream = ByteArrayOutputStream()
            val saved = scaled.compress(Bitmap.CompressFormat.PNG, 100, stream)
            if (scaled !== favicon) scaled.recycle()
            val data = stream.toByteArray()
            if (!saved || data.size !in 1..65_536) return false
            val dest = fileFor(host)
            val temp = File(directory, dest.name + ".tmp")
            temp.writeBytes(data)
            if (!temp.renameTo(dest)) {
                temp.delete()
                return false
            }
            // Retain only the 100 most recently updated site icons.
            directory.listFiles { f -> f.isFile && f.name.endsWith(".png") }
                ?.sortedByDescending { it.lastModified() }?.drop(100)?.forEach { it.delete() }
            true
        } catch (_: Exception) { false }
    }

    fun clear() {
        directory.listFiles()?.filter { it.isFile }?.forEach { it.delete() }
    }
}
