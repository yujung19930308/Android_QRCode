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
import android.widget.Toast
import com.example.qrcode_reader.R
import org.jsoup.Jsoup
import java.util.concurrent.Executors

class BarcodeURLBottomSheet : BaseBottomFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_barcode_data, container, false)
    }

    //We will call this function to update the URL displayed
    fun updateURL(url: String) {
        fetchUrlMetaData(url) { title ->
            view?.apply {
                findViewById<TextView>(R.id.text_view_title)?.text = title
                findViewById<TextView>(R.id.text_view_link)?.text = url
                findViewById<TextView>(R.id.text_view_visit_link).setOnClickListener { _ ->
                    Intent(Intent.ACTION_VIEW).also {
                        it.data = Uri.parse(url)
                        startActivity(it)
                    }
                }
            }
        }
    }

    //獲得 URL 的資訊
    private fun fetchUrlMetaData(url: String,
                                 callback: (title: String) -> Unit) {
        val executor = Executors.newSingleThreadExecutor()
        val handler = Handler(Looper.getMainLooper())
        executor.execute {
            val doc = Jsoup.connect(url).get()
            //val desc = doc.select("meta[name=description]")[0].attr("content")
            handler.post {
                callback(doc.title())
            }
        }
    }
}