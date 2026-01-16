package com.example.homebrowse

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private lateinit var swipeRefresh: androidx.swiperefreshlayout.widget.SwipeRefreshLayout
    private var homeUrl: String = "http://192.168.100.250:51500"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webview)
        swipeRefresh = findViewById(R.id.swipe_refresh)

        // Default to the provided IP if no saved URL
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        homeUrl = prefs.getString("home_url", homeUrl) ?: homeUrl

        setupWebView()
        setupSwipeRefresh()

        // Enable WebView remote debugging only in debug builds
        if (BuildConfig.DEBUG) WebView.setWebContentsDebuggingEnabled(true)

        // Use a Chrome-like user agent to reduce site UA-sniffing inconsistencies
        webView.settings.userAgentString = android.webkit.WebSettings.getDefaultUserAgent(this)

        // Load the configured homepage (no state restoration; activity handles config changes)
        loadUrl(homeUrl)

        // Hidden access to settings: detect swipe from left edge to open Settings
        setupEdgeSwipeForSettings()
    }

    private fun setupSwipeRefresh() {
        swipeRefresh.setOnRefreshListener {
            webView.reload()
        }
    }

    private fun setupEdgeSwipeForSettings() {
        val density = resources.displayMetrics.density
        val leftEdgeThreshold = (24 * density) // dp from left edge
        val swipeThreshold = (120 * density) // dp movement to trigger
        var startX = 0f
        var tracking = false

        webView.setOnTouchListener { _, ev ->
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    startX = ev.x
                    tracking = startX <= leftEdgeThreshold
                }
                MotionEvent.ACTION_MOVE -> {
                    if (tracking) {
                        val dx = ev.x - startX
                        if (dx > swipeThreshold) {
                            // Open settings and stop tracking
                            startActivity(Intent(this, SettingsActivity::class.java))
                            tracking = false
                            return@setOnTouchListener true
                        }
                    }
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> tracking = false
            }
            // Let WebView handle the touch as usual
            false
        }
    }

    private fun setupWebView() {
        val settings = webView.settings
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.allowFileAccess = false
        webView.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val uri = request.url
                val scheme = uri.scheme ?: return false

                // Handle http/https: keep same-host in-webview, external for others
                if (scheme == "http" || scheme == "https") {
                    val homeHost = if (homeUrl.isBlank()) null else Uri.parse(homeUrl).host
                    return if (homeHost != null && uri.host == homeHost) {
                        false
                    } else {
                        startActivity(Intent(Intent.ACTION_VIEW, uri))
                        true
                    }
                }

                // Handle Chrome-style intent:// links
                if (scheme == "intent") {
                    try {
                        val intent = Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME)
                        
                        // Add FLAG_ACTIVITY_NEW_TASK for launching from WebView context
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        
                        // Try to start the activity
                        startActivity(intent)
                        return true
                    } catch (e: android.content.ActivityNotFoundException) {
                        // App not installed, try fallback URL if present
                        try {
                            val intent = Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME)
                            val fallback = intent.getStringExtra("browser_fallback_url")
                            if (!fallback.isNullOrEmpty()) {
                                webView.loadUrl(fallback)
                            }
                        } catch (ex: Exception) {
                            // ignore
                        }
                        return true
                    } catch (e: Exception) {
                        // Malformed intent URL, ignore
                        return true
                    }
                }

                // Common schemes: mailto, tel, sms
                if (scheme == "mailto" || scheme == "tel" || scheme == "sms") {
                    try {
                        startActivity(Intent(Intent.ACTION_VIEW, uri))
                    } catch (e: Exception) {
                        // no handler
                    }
                    return true
                }

                // Try to launch other schemes as intents
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, uri))
                    return true
                } catch (e: Exception) {
                    return false
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun enableFullScreenMode() {
        // Intentionally left blank so the device status and navigation bars remain visible.
        // Previously this enabled immersive mode; keeping it visible avoids hiding phone UI.
    }

    private fun loadUrl(url: String) {
        webView.loadUrl(url)
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        // We handle orientation/configuration changes to keep the WebView instance alive
        // and avoid reloading the page or re-executing JavaScript.
    }

    // No options menu — UI is full-screen and minimal


    override fun onResume() {
        super.onResume()
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val url = prefs.getString("home_url", homeUrl) ?: homeUrl
        if (url != homeUrl) {
            homeUrl = url
            loadUrl(homeUrl)
        }
    }

    // promptForUrl() retained for manual use in Settings but not used on boot
    private fun promptForUrl() {
        // Keep the dialog for backward compatibility if ever needed
        val edit = android.widget.EditText(this)
        edit.hint = "https://example.com"
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(R.string.enter_website)
            .setView(edit)
            .setPositiveButton(R.string.save) { _, _ ->
                val u = edit.text.toString().trim()
                getSharedPreferences("app_prefs", MODE_PRIVATE).edit().putString("home_url", u).apply()
                homeUrl = if (u.isNotBlank()) u else "http://192.168.100.250:51500"
                loadUrl(homeUrl)
            }
            .setCancelable(false)
            .show()
    }


}