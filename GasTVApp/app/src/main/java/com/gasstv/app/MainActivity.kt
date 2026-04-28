package com.gasstv.app

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.webkit.*
import android.widget.*
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var progressBar: ProgressBar
    private lateinit var swipeRefresh: SwipeRefreshLayout

    private val TARGET_URL = "https://gasstv.pw/absensi.html"
    private val FILE_CHOOSER_REQUEST = 1001
    private var filePathCallback: ValueCallback<Array<Uri>>? = null

    // Popup WebView dialog
    private var popupWindow: android.app.Dialog? = null
    private var popupWebView: WebView? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webview)
        progressBar = findViewById(R.id.progressBar)
        swipeRefresh = findViewById(R.id.swipeRefresh)

        setupWebView(webView, isPopup = false)

        // Swipe to refresh
        swipeRefresh.setColorSchemeColors(Color.parseColor("#E74C3C"))
        swipeRefresh.setOnRefreshListener { webView.reload() }

        // Back button
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                // Close popup first if open
                if (popupWindow?.isShowing == true) {
                    closePopup()
                    return
                }
                if (webView.canGoBack()) webView.goBack()
                else finish()
            }
        })

        webView.loadUrl(TARGET_URL)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView(wv: WebView, isPopup: Boolean) {
        with(wv.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            loadWithOverviewMode = true
            useWideViewPort = true
            setSupportZoom(false)
            builtInZoomControls = false
            displayZoomControls = false
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            cacheMode = WebSettings.LOAD_DEFAULT
            mediaPlaybackRequiresUserGesture = false
            setSupportMultipleWindows(true)   // ← penting untuk tangkap new window
            javaScriptCanOpenWindowsAutomatically = true
        }

        wv.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                if (!isPopup) progressBar.visibility = View.VISIBLE
            }
            override fun onPageFinished(view: WebView, url: String) {
                if (!isPopup) {
                    progressBar.visibility = View.GONE
                    swipeRefresh.isRefreshing = false
                }
            }
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                // Semua URL dibuka di dalam WebView saja
                return false
            }
            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                if (request.isForMainFrame && !isPopup) {
                    view.loadData(offlineHtml(), "text/html", "UTF-8")
                }
            }
        }

        wv.webChromeClient = object : WebChromeClient() {
            private var customView: View? = null
            private var customViewCallback: CustomViewCallback? = null
            private var originalOrientation = 0

            override fun onProgressChanged(view: WebView, newProgress: Int) {
                if (!isPopup) progressBar.progress = newProgress
            }

            // ← Tangkap semua link target="_blank" → tampilkan sebagai popup modal
            override fun onCreateWindow(
                view: WebView,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: android.os.Message
            ): Boolean {
                showPopupWebView(resultMsg)
                return true
            }

            override fun onCloseWindow(window: WebView) {
                closePopup()
            }

            // Fullscreen video
            override fun onShowCustomView(view: View, callback: CustomViewCallback) {
                customView = view
                customViewCallback = callback
                originalOrientation = requestedOrientation
                val decor = window.decorView as FrameLayout
                decor.addView(customView, FrameLayout.LayoutParams(-1, -1))
                window.decorView.systemUiVisibility = (
                    View.SYSTEM_UI_FLAG_FULLSCREEN or
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                )
            }
            override fun onHideCustomView() {
                customView?.let { (window.decorView as FrameLayout).removeView(it) }
                customView = null
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
                requestedOrientation = originalOrientation
                customViewCallback?.onCustomViewHidden()
            }

            override fun onShowFileChooser(
                webView: WebView,
                filePath: ValueCallback<Array<Uri>>,
                fileChooserParams: FileChooserParams
            ): Boolean {
                filePathCallback?.onReceiveValue(null)
                filePathCallback = filePath
                try { startActivityForResult(fileChooserParams.createIntent(), FILE_CHOOSER_REQUEST) }
                catch (e: Exception) { filePathCallback = null; return false }
                return true
            }

            override fun onJsAlert(view: WebView, url: String, message: String, result: JsResult): Boolean {
                result.confirm(); return true
            }
        }
    }

    // -------- POPUP MODAL --------
    @SuppressLint("SetJavaScriptEnabled")
    private fun showPopupWebView(resultMsg: android.os.Message) {
        // Buat dialog fullscreen dengan overlay gelap
        val dialog = android.app.Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.parseColor("#CC000000")))
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setGravity(Gravity.CENTER)
        }

        // Root layout — tap di luar popup = close
        val rootLayout = FrameLayout(this)
        rootLayout.setBackgroundColor(Color.parseColor("#AA000000"))
        rootLayout.setOnClickListener { closePopup() }

        // Popup container (card di tengah)
        val cardLayout = FrameLayout(this)
        cardLayout.setBackgroundColor(Color.parseColor("#141414"))
        cardLayout.elevation = 24f

        val cardParams = FrameLayout.LayoutParams(
            (resources.displayMetrics.widthPixels * 0.93).toInt(),
            (resources.displayMetrics.heightPixels * 0.85).toInt()
        )
        cardParams.gravity = Gravity.CENTER
        cardLayout.layoutParams = cardParams
        // Cegah tap di dalam card menutup popup
        cardLayout.setOnClickListener { /* consume */ }

        // Progress bar popup
        val popupProgress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal)
        popupProgress.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, 6
        )
        popupProgress.progressTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#E74C3C"))

        // WebView popup
        val newWebView = WebView(this)
        newWebView.layoutParams = FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT,
            FrameLayout.LayoutParams.MATCH_PARENT
        )
        setupWebView(newWebView, isPopup = true)

        // Update progress bar untuk popup
        newWebView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                popupProgress.progress = newProgress
                popupProgress.visibility = if (newProgress < 100) View.VISIBLE else View.GONE
            }
            override fun onCreateWindow(view: WebView, isDialog: Boolean, isUserGesture: Boolean, resultMsg: android.os.Message): Boolean {
                // Link di dalam popup → buka di popup yang sama
                val transport = resultMsg.obj as WebView.WebViewTransport
                transport.webView = newWebView
                resultMsg.sendToTarget()
                return true
            }
        }

        // Tombol X (close)
        val closeBtn = TextView(this)
        closeBtn.text = "✕"
        closeBtn.textSize = 20f
        closeBtn.setTextColor(Color.WHITE)
        closeBtn.setBackgroundColor(Color.parseColor("#E74C3C"))
        val closeBtnSize = 44
        val closeBtnParams = FrameLayout.LayoutParams(
            dpToPx(closeBtnSize), dpToPx(closeBtnSize)
        )
        closeBtnParams.gravity = Gravity.TOP or Gravity.END
        closeBtn.layoutParams = closeBtnParams
        closeBtn.gravity = Gravity.CENTER
        closeBtn.setPadding(0, 0, 0, 0)
        closeBtn.setOnClickListener { closePopup() }

        // Susun layout
        cardLayout.addView(newWebView)
        cardLayout.addView(popupProgress)
        cardLayout.addView(closeBtn)
        rootLayout.addView(cardLayout)

        dialog.setContentView(rootLayout)

        // Hubungkan WebView baru ke message dari window.open
        val transport = resultMsg.obj as WebView.WebViewTransport
        transport.webView = newWebView
        resultMsg.sendToTarget()

        popupWebView = newWebView
        popupWindow = dialog
        dialog.show()
    }

    private fun closePopup() {
        popupWebView?.destroy()
        popupWebView = null
        popupWindow?.dismiss()
        popupWindow = null
    }

    private fun dpToPx(dp: Int): Int =
        (dp * resources.displayMetrics.density).toInt()

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == FILE_CHOOSER_REQUEST) {
            val result = if (resultCode == Activity.RESULT_OK && data != null)
                WebChromeClient.FileChooserParams.parseResult(resultCode, data) else null
            filePathCallback?.onReceiveValue(result)
            filePathCallback = null
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    override fun onResume() { super.onResume(); webView.onResume() }
    override fun onPause() { super.onPause(); webView.onPause() }
    override fun onDestroy() {
        closePopup()
        webView.destroy()
        super.onDestroy()
    }

    private fun offlineHtml() = """
        <!DOCTYPE html><html><head><meta charset=UTF-8>
        <meta name=viewport content='width=device-width,initial-scale=1'>
        <style>
          body{background:#0a0a0a;color:#e0e0e0;font-family:sans-serif;
            display:flex;flex-direction:column;align-items:center;
            justify-content:center;min-height:100vh;margin:0;text-align:center;padding:24px}
          .icon{font-size:64px;margin-bottom:16px}
          h2{color:#fff;margin-bottom:8px}
          p{color:#666;margin-bottom:24px;line-height:1.6}
          button{background:#e74c3c;color:#fff;border:none;border-radius:10px;
            padding:14px 28px;font-size:16px;font-weight:700;cursor:pointer}
        </style></head><body>
        <div class=icon>📡</div>
        <h2>Tidak Ada Koneksi</h2>
        <p>Periksa koneksi internet kamu<br>lalu coba lagi.</p>
        <button onclick='location.reload()'>🔄 Coba Lagi</button>
        </body></html>
    """.trimIndent()
}
