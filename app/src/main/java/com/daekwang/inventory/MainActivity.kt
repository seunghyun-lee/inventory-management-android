package com.daekwang.inventory

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.content.ContentValues
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.view.WindowManager
import android.webkit.JavascriptInterface
import android.webkit.URLUtil
import com.daekwang.inventory.databinding.ActivityMainBinding
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream

class MainActivity : AppCompatActivity() {
    private var activityMainBinding: ActivityMainBinding? = null
    private lateinit var webView: WebView

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
//        enableEdgeToEdge()
        activityMainBinding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(activityMainBinding!!.root)

        webView = findViewById(R.id.webView)
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            allowFileAccessFromFileURLs = true
            allowUniversalAccessFromFileURLs = true
        }
        webView.addJavascriptInterface(WebAppInterface(), "Android")

        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                return false
            }
        }

        webView.setDownloadListener { url, userAgent, contentDisposition, mimetype, contentLength ->
            if (url.startsWith("http") || url.startsWith("https")) {
                val request = DownloadManager.Request(Uri.parse(url))
                request.setMimeType(mimetype)
                request.addRequestHeader("User-Agent", userAgent)
                request.setDescription("Downloading file...")
                request.setTitle(URLUtil.guessFileName(url, contentDisposition, mimetype))
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, URLUtil.guessFileName(url, contentDisposition, mimetype))

                val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
                dm.enqueue(request)

                Toast.makeText(applicationContext, "Downloading File...", Toast.LENGTH_LONG).show()
            }
        }

        webView.loadUrl("https://inventory-management-client-iota.vercel.app/")
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            // WebView에서 뒤로 갈 페이지가 있으면 뒤로 이동
            webView.goBack()
        } else {
            // WebView에서 더 이상 뒤로 갈 페이지가 없으면 기본 동작(앱 종료)
            super.onBackPressed()
        }
    }

    inner class WebAppInterface {
        @JavascriptInterface
        fun showToast(toast: String) {
            runOnUiThread {
                Toast.makeText(applicationContext, toast, Toast.LENGTH_SHORT).show()
            }
        }
        @JavascriptInterface
        fun saveBase64AsFile(base64: String, fileName: String) {
            // 안드로이드 10(API 29) 이상: MediaStore 사용하여 다운로드 폴더에 저장
            saveFileToDownloadsQAndAbove(base64, fileName)
        }

        // 안드로이드 10(API 29) 이상에서 MediaStore를 사용하여 다운로드 폴더에 파일 저장
        private fun saveFileToDownloadsQAndAbove(base64: String, fileName: String) {
            try {
                val resolver = contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.Downloads.DISPLAY_NAME, fileName)
                    put(MediaStore.Downloads.MIME_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                    put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    val outputStream = resolver.openOutputStream(uri)
                    val decodedBytes = Base64.decode(base64, Base64.DEFAULT)
                    outputStream?.use {
                        it.write(decodedBytes)
                        it.close()
                    }
                    runOnUiThread {
                        Toast.makeText(applicationContext, "File saved as $fileName", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(applicationContext, "File save failed", Toast.LENGTH_LONG).show()
                }
            }
        }

        // 안드로이드 10(API 29) 이하에서 Download 폴더에 파일 저장
        private fun saveFileToDownloadsBelowQ(base64: String, fileName: String) {
            try {
                val path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val file = File(path, fileName)

                if (!path.exists()) {
                    path.mkdirs()
                }

                val decodedBytes = Base64.decode(base64, Base64.DEFAULT)
                FileOutputStream(file).use { it.write(decodedBytes) }

                runOnUiThread {
                    Toast.makeText(applicationContext, "File saved as $fileName", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                runOnUiThread {
                    Toast.makeText(applicationContext, "File save failed", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}