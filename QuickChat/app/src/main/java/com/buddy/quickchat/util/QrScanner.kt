package com.buddy.quickchat.util

import android.content.Context
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import kotlinx.coroutines.tasks.await

class QrScanner(private val context: Context) {
    suspend fun scanQrCode(): String? {
        return try {
            val options = GmsBarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build()
            val scanner = GmsBarcodeScanning.getClient(context, options)
            val barcode = scanner.startScan().await()
            barcode.rawValue
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
