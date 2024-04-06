package com.taspen.taspenlife2

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.webkit.*
import android.widget.ProgressBar
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.taspen.taspenlife2.databinding.ActivityMainBinding
import java.io.File
import java.util.*
import java.util.concurrent.ExecutorService

class MainActivity : AppCompatActivity() {

    //URL Link
    private var URL = "https://taspen.life"

    private var getFile: File? = null
    private lateinit var binding: ActivityMainBinding
    private lateinit var mWebView: WebView
    private lateinit var loadingSpinner: ProgressBar
    private lateinit var cameraExecutor: ExecutorService

    private var mFilePathCallback: ValueCallback<Array<Uri>>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mWebView = binding.webview
        loadingSpinner = binding.progressBar

        // Request location permission
        requestLocationPermission()
        requestPermissions(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.CAMERA,
            Manifest.permission_group.CAMERA
        ), 0)

        // Allow ALL Permission
        if (!allPermissionsGranted()) {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS
            )
        }

        webViewComponent()
    }

    @Suppress("DEPRECATION")
    @SuppressLint("SetJavaScriptEnabled", "SupportAnnotationUsage", "ObsoleteSdkInt")
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private fun webViewComponent() {
        /** URL LINK  */
        mWebView.loadUrl(URL)
        /** Js  */
        mWebView.isFocusable = true
        mWebView.isFocusableInTouchMode = true
        mWebView.settings.javaScriptEnabled = true
        /** Location  */
        mWebView.settings.setGeolocationEnabled(true)
        mWebView.settings.setGeolocationDatabasePath(this.filesDir.path)
        /** Etc  */
        mWebView.settings.cacheMode = WebSettings.LOAD_DEFAULT
        mWebView.settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
        mWebView.settings.domStorageEnabled = true
        mWebView.settings.setAppCacheEnabled(true)
        mWebView.settings.databaseEnabled = true

        //Setting WebViewClient
        mWebView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                // Request location permission
                requestLocationPermission()
                super.onPageStarted(view, url, favicon)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
            }

            override fun onLoadResource(view: WebView?, url: String?) {
                super.onLoadResource(view, url)
            }

        }

        //Setting WebChromeClient -- (file attach request) --
        mWebView.webChromeClient = object : WebChromeClient() {
            @SuppressLint("QueryPermissionsNeeded")
            //> Get Camera
            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                if (mFilePathCallback != null) {
                    mFilePathCallback!!.onReceiveValue(null)
                }
                mFilePathCallback = filePathCallback

                val intent = Intent(this@MainActivity, CameraActivity::class.java)
                val callback = ValueCallback<Array<Uri>> { value ->
                    mFilePathCallback?.onReceiveValue(value)
                    mFilePathCallback = null
                }
                startActivityForResult(intent, CAMERA_RESULT_CODE)
                mFilePathCallback = callback

                return true

//                val intent = Intent(this@MainActivity, CameraActivity::class.java)
//                val intentArray: Array<Intent?> = arrayOf(intent)
//                val chooseIntent = Intent(Intent.ACTION_GET_CONTENT)
//                chooseIntent.putExtra(Intent.ACTION_GET_CONTENT, intentArray)
//
//                mFilePathCallback = filePathCallback
//                startActivityForResult(chooseIntent, CAMERA_RESULT_CODE)
//
//                return super.onShowFileChooser(webView, filePathCallback, fileChooserParams)

            }



            //> Get Location
            override fun onGeolocationPermissionsShowPrompt(origin: String?, callback: GeolocationPermissions.Callback?) {
                super.onGeolocationPermissionsShowPrompt(origin, callback)
                // Request location permission if not granted
                if (ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    ActivityCompat.requestPermissions(
                        this@MainActivity, // Replace with your activity reference
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        LOCATION_PERMISSION_REQUEST_CODE
                    )
                } else {
                    // Location permission already granted, invoke callback
                    callback!!.invoke(origin, true, true)
                }
            }
        }

        // Reload URL
        mWebView.reload()

        //Setting Download File "PDF" for some File
        @Suppress("DEPRECATION")
        mWebView.setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
            val request = DownloadManager.Request(Uri.parse(url))
            request.setMimeType(mimeType)
            request.addRequestHeader("cookie", CookieManager.getInstance().getCookie(url))
            request.addRequestHeader("User-Agent", userAgent)
            request.setDescription("Downloading file...")
            request.setTitle(URLUtil.guessFileName(url, contentDisposition, mimeType))
            request.allowScanningByMediaScanner()
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_ONLY_COMPLETION)
            request.setDestinationInExternalFilesDir(
                this@MainActivity,
                Environment.DIRECTORY_DOWNLOADS,
                ".pdf"
            )
            request.setDestinationInExternalPublicDir(
                Environment.DIRECTORY_DOWNLOADS, URLUtil.guessFileName(
                url, contentDisposition, mimeType))
            val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            dm.enqueue(request)
            Toast.makeText(applicationContext, "Downloading File Successfully", Toast.LENGTH_LONG).show()
        }

    }

    private fun requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            // Location permission already granted
            mWebView.settings.setGeolocationEnabled(true)
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    @Suppress("DEPRECATION", "UnusedEquals")
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == Activity.RESULT_OK && resultCode == CAMERA_RESULT_CODE) {
            val uri = data?.data
            val results = uri?.let { arrayOf(it) }
            Log.e(TAG, "Photo failed: $uri")
            mFilePathCallback!!.onReceiveValue(results)
            mFilePathCallback = null
        }
//        Toast.makeText(this, "Failed requestc code $requestCode", Toast.LENGTH_LONG).show()
//        Toast.makeText(this, "Failed result code $resultCode", Toast.LENGTH_LONG).show()
//        Log.e(TAG, "Photo capture failed: $resultCode")

//        if (CAMERA_RESULT_CODE==200){
//            if (mFilePathCallback==null)
//                return
//            mFilePathCallback!!.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode,data))
//            mFilePathCallback==null
//        }
    }

    companion object {
        private const val TAG = "MainActivity ERROR ANJINGGGG"
        const val CAMERA_RESULT_CODE = 200
        private const val REQUEST_CODE_PERMISSIONS = 10
        private const val LOCATION_PERMISSION_REQUEST_CODE = 102
        val REQUIRED_PERMISSIONS =
            mutableListOf (
                Manifest.permission.CAMERA
            ).apply {
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N) {
                    add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }
            }.toTypedArray()
    }
}