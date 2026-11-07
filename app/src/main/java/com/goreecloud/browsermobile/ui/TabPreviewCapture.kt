package com.goreecloud.browsermobile.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.webkit.WebView

/**
 * Captures only the already-visible WebView viewport on the Android UI thread.
 *
 * The bounded thumbnail stays in BrowserTab memory. It is never persisted,
 * synchronized, sent to a service, or logged. Inactive/detached WebViews are
 * never brought to the foreground or loaded solely to create thumbnails.
 */
internal object TabPreviewCapture {
    private const val WIDTH = 320
    private const val HEIGHT = 480

    fun capture(view: WebView?): Bitmap? {
        if (view == null || !view.isAttachedToWindow || view.width < 48 || view.height < 48) {
            return null
        }
        return try {
            val bitmap = Bitmap.createBitmap(WIDTH, HEIGHT, Bitmap.Config.RGB_565)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.WHITE)
            val scale = WIDTH.toFloat() / view.width.toFloat()
            canvas.scale(scale, scale)
            view.draw(canvas)
            bitmap
        } catch (_: RuntimeException) {
            // Some protected/hardware-composited surfaces cannot be drawn.
            // The overview explicitly labels the unavailable preview.
            null
        }
    }
}
