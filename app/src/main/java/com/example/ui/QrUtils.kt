package com.example.ui

import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.net.URLEncoder

object QrUtils {
    fun generateQrCode(text: String, size: Int = 400): Bitmap? {
        return try {
            val writer = QRCodeWriter()
            val bitMatrix = writer.encode(text, BarcodeFormat.QR_CODE, size, size)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(
                        x, 
                        y, 
                        if (bitMatrix.get(x, y)) AndroidColor.BLACK else AndroidColor.WHITE
                    )
                }
            }
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getSearchUrl(query: String): String {
        if (query.startsWith("http://") || query.startsWith("https://")) {
            return query
        }
        return try {
            "https://www.google.com/search?q=" + URLEncoder.encode(query, "UTF-8")
        } catch (e: Exception) {
            "https://www.google.com/search?q=" + query
        }
    }
}
