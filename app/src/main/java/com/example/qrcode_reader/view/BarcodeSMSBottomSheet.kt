package com.example.qrcode_reader.view

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.example.qrcode_reader.R
import java.util.concurrent.Executors

class BarcodeSMSBottomSheet: BaseBottomFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_barcode_sms, container, false)
    }
    //顯示收件者、內文以及發送簡訊
    fun updateSMS(phone: String, message: String) {
        val executor = Executors.newSingleThreadExecutor()
        val handler = Handler(Looper.getMainLooper())
        executor.execute {
            handler.post {
                view?.apply {
                    findViewById<TextView>(R.id.txt_recipient)?.text = resources.getString(R.string.sms_recipient, phone)
                    findViewById<TextView>(R.id.txt_content)?.text = resources.getString(R.string.sms_content, message)
                    findViewById<TextView>(R.id.text_view_sms_send).setOnClickListener { _ ->
                        Intent(Intent.ACTION_VIEW).also {
                            it.data = Uri.parse("smsto:$phone")  // This ensures only SMS apps respond
                            it.putExtra("sms_body", message)
                            startActivity(it)
                        }
                    }
                }
            }
        }
    }
}