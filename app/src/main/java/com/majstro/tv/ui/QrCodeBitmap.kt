package com.majstro.tv.ui

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

/** Generates a crisp QR code [ImageBitmap] styled with midnight-navy modules on warm parchment. */
fun generateQrCodeBitmap(
    content: String,
    sizePx: Int = 320,
    darkColor: Int = 0xFF0E151C.toInt(),
    lightColor: Int = 0xFFEDE7DF.toInt()
): ImageBitmap? {
    if (content.isBlank()) return null
    return runCatching {
        val hints = mapOf(EncodeHintType.MARGIN to 1)
        val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
        val pixels = IntArray(sizePx * sizePx)
        for (y in 0 until sizePx) {
            val offset = y * sizePx
            for (x in 0 until sizePx) {
                pixels[offset + x] = if (matrix[x, y]) darkColor else lightColor
            }
        }
        Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888).apply {
            setPixels(pixels, 0, sizePx, 0, 0, sizePx, sizePx)
        }.asImageBitmap()
    }.getOrNull()
}
