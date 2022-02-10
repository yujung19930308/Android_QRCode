package com.example.qrcode_reader.view

import android.graphics.RectF
import android.util.Size

interface ScannerOverlay {
    val size : Size
    val scanRect : RectF
}