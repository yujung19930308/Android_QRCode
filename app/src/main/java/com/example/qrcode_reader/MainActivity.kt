package com.example.qrcode_reader

import android.Manifest
import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.telephony.SmsManager
import android.text.TextUtils
import android.util.Patterns
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.camera2api_mvc.util.FullScreenUtil
import com.example.camera2api_mvc.util.IPermissionListener
import com.example.camera2api_mvc.util.LogUtil
import com.example.camera2api_mvc.util.PermissionHelper
import com.example.qrcode_reader.observer.IBarcodeResultListener
import com.example.qrcode_reader.view.BarcodeSMSBottomSheet
import com.example.qrcode_reader.view.BarcodeTextDialog
import com.example.qrcode_reader.view.BarcodeURLBottomSheet
import com.google.common.util.concurrent.ListenableFuture
import com.google.mlkit.vision.barcode.common.Barcode
import kotlinx.android.synthetic.main.activity_main.*
import java.lang.Exception
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity(), IBarcodeResultListener{

    companion object {
        private val TAG = MainActivity::class.java.simpleName
        private val PERMISSIONS_STORAGE = arrayOf(Manifest.permission.CAMERA, Manifest.permission.SEND_SMS)
        private const val millisDiff = 1500
    }
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var analyzer: MyImageAnalyzer
    private var mCamera : Camera ?= null
    private var isFlash = false
    private var bottomSheet = BarcodeURLBottomSheet()
    private var smsBottomSheet = BarcodeSMSBottomSheet()
    private var iSendSMS = false
    private var iErrorURL = false
    private var curMillis = 0L
    private var barcodeTextDialog: BarcodeTextDialog ?= null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        init()
    }

    override fun onStart() {
        super.onStart()
        //請求權限
        if(PermissionHelper.checkPermission(this, PERMISSIONS_STORAGE)) {
            LogUtil.d(TAG, "[onStart] All permission granted !!")
        }
    }

    override fun onStop() {
        super.onStop()
        if(barcodeTextDialog != null && barcodeTextDialog?.isShowing == true) barcodeTextDialog?.dismiss()
    }

    //初始化物件
    private fun init() {
        cameraExecutor = Executors.newSingleThreadExecutor()
        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        analyzer = MyImageAnalyzer(scan_area, this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            bindPreview(cameraProvider)
        }, ContextCompat.getMainExecutor(this))
        flash_btn?.setOnTouchListener(btnFlashTouch)
        barcodeTextDialog = BarcodeTextDialog(this)
    }

    //閃光燈按鈕監聽
    @SuppressLint("ClickableViewAccessibility")
    private val btnFlashTouch = View.OnTouchListener { _, event ->
        if(event?.action == MotionEvent.ACTION_DOWN) {
            val animScaleUp =
                AnimationUtils.loadAnimation(this@MainActivity, R.anim.scale_up)
            flash_btn?.startAnimation(animScaleUp)
        } else if (event?.action == MotionEvent.ACTION_UP) {
            val animScaleDown =
                AnimationUtils.loadAnimation(this@MainActivity, R.anim.scale_down)
            flash_btn?.startAnimation(animScaleDown)
            isFlash = !isFlash
            if(isFlash) {
                flash_btn?.setBackgroundResource(R.drawable.ic_baseline_flash_on_24)
            } else {
                flash_btn?.setBackgroundResource(R.drawable.ic_baseline_flash_off_24)
            }
            mCamera?.cameraControl?.enableTorch(isFlash)
        }
        true
    }

    @SuppressLint("UnsafeExperimentalUsageError")
    private fun bindPreview(cameraProvider: ProcessCameraProvider) {
        val preview: Preview = Preview.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_16_9)
            .build()
        val cameraSelector: CameraSelector = CameraSelector.Builder()
            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
            .build()
        preview.setSurfaceProvider(camera_preview.surfaceProvider)
        val imageAnalysis = ImageAnalysis.Builder()
            .setTargetAspectRatio(AspectRatio.RATIO_16_9)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
        imageAnalysis.setAnalyzer(cameraExecutor, analyzer)
        cameraProvider.unbindAll()
        mCamera = cameraProvider.bindToLifecycle(
            this as LifecycleOwner,
            cameraSelector,
            imageAnalysis,
            preview
        )
    }

    //處理全屏
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if(hasFocus) FullScreenUtil.hideNavigationBar(window)
    }

    //請求權限回傳
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        PermissionHelper.onRequestPermissionsResult(requestCode, grantResults, object :
            IPermissionListener {
            override fun onPermissionGranted() {
                LogUtil.d(TAG, "[onRequestPermissionsResult] onPermissionGranted !!")
            }

            override fun onPermissionDenied() {
                LogUtil.d(TAG, "[onRequestPermissionsResult] onPermissionDenied !!")
                finish()
            }
        })
    }

    //讀取後的資料分析
    override fun readBarcodeData(barcodes: List<Barcode>) {
        if (barcodes.isEmpty()) {
            if (System.currentTimeMillis() - curMillis >= millisDiff) {
                iSendSMS = false
                iErrorURL = false
            }
            return
        }
        curMillis = System.currentTimeMillis()
        for (barcode in barcodes) {
            when (barcode.valueType) {
                //you can check if the barcode has other values
                //For now I am using it just for URL
                Barcode.TYPE_URL -> {
                    //we have the URL here
                    val url = barcode.url?.url
                    if(!url?.isValidUrl()!! && !iErrorURL) {
                        Toast.makeText(this, "URL錯誤，請確認URL是否正確", Toast.LENGTH_SHORT).show()
                        iErrorURL = true
                    }
                    if (!bottomSheet.isAdded && !iErrorURL) {
                        bottomSheet.show(supportFragmentManager, "URL")
                        bottomSheet.updateURL(barcode.url?.url.toString())
                    }
                }
                Barcode.TYPE_TEXT -> {
                    LogUtil.d(TAG, "${barcode.rawValue?.toString()}")
                    if(!barcodeTextDialog?.isShowing!!) showBarcodeTextDialog(barcode.rawValue.toString())
                }
                Barcode.TYPE_SMS -> {
                    if(!TextUtils.isEmpty(barcode.sms?.phoneNumber) && !TextUtils.isEmpty(barcode.sms?.message)){
                        if (barcode.sms?.phoneNumber.toString() == "1922") {
                            if(!iSendSMS) {
                                LogUtil.d(TAG, "Send SMS ----")
                                sendSMS(barcode.sms?.phoneNumber.toString(), barcode.sms?.message.toString())
                                iSendSMS = true
                            }
                        } else {
                            if(!smsBottomSheet.isAdded) {
                                smsBottomSheet.show(supportFragmentManager, "SMS")
                                smsBottomSheet.updateSMS(barcode.sms?.phoneNumber.toString(), barcode.sms?.message.toString())
                            }
                        }
                    }
                }
            }
        }
    }

    @Suppress("DEPRECATION")
    //發送簡訊
    private fun sendSMS(phone: String, msg: String) {
        try {
            val smsManager = SmsManager.getDefault()
            smsManager.sendTextMessage(phone, null, msg, null, null)
            Toast.makeText(this, "1922 簡訊發送成功", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "1922 簡訊發送失敗", Toast.LENGTH_SHORT).show()
            e.printStackTrace()
        }
    }
    //顯示 text dialog
    private fun showBarcodeTextDialog(barcodeText:String) {
        if(barcodeTextDialog != null && barcodeTextDialog?.isShowing == true) barcodeTextDialog?.dismiss()
        barcodeTextDialog?.setContentView(R.layout.dialog_barcode_text)
        barcodeTextDialog?.updateBarcodeTextResult(barcodeText)
        val width = (resources.displayMetrics.widthPixels * 0.85).toInt()
        barcodeTextDialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        barcodeTextDialog?.window?.setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT)
        barcodeTextDialog?.window?.let { FullScreenUtil.hideNavigationBar(it) }
        barcodeTextDialog?.setCancelable(true)
        barcodeTextDialog?.show()
    }
    //確認URL是否有可用
    private fun String.isValidUrl(): Boolean = Patterns.WEB_URL.matcher(this).matches()
}