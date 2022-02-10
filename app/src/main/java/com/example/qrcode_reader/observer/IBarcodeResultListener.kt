package com.example.qrcode_reader.observer

import com.google.mlkit.vision.barcode.common.Barcode

interface IBarcodeResultListener {
    fun readBarcodeData(barcodes: List<Barcode>)
}