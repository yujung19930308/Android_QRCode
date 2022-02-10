package com.example.qrcode_reader.view

import android.app.Dialog
import android.content.Context
import com.example.qrcode_reader.R
import kotlinx.android.synthetic.main.dialog_barcode_text.*

class BarcodeTextDialog(context: Context) : Dialog(context) {

    fun updateBarcodeTextResult(barcodeText: String) {
        result_label?.text = context.getString(R.string.txt_content, barcodeText)
    }
}