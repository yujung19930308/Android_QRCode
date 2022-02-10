package com.example.qrcode_reader.view

import android.os.Bundle
import com.example.qrcode_reader.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

open class BaseBottomFragment: BottomSheetDialogFragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.AppBottomSheetDialogTheme)
    }
}