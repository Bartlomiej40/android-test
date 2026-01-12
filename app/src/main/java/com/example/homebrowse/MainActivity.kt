package com.example.homebrowse

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
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
        enableFullScreenMode()

        loadUrl(homeUrl)

        // Hidden access to settings: long-press anywhere on the WebView
        webView.setOnLongClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
            true
        }
        webView.isLongClickable = true
    }

    private fun setupSwipeRefresh() {
        swipeRefresh.setOnRefreshListener {
            webView.reload()
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
                val homeHost = if (homeUrl.isBlank()) null else Uri.parse(homeUrl).host
                if (uri.scheme == "http" || uri.scheme == "https") {
                    // If link is same host as home page, load in-app; otherwise open external
                    return if (homeHost != null && uri.host == homeHost) {
                        false
                    } else {
                        startActivity(Intent(Intent.ACTION_VIEW, uri))
                        true
                    }
                }
                return false
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                swipeRefresh.isRefreshing = false
            }
        }
    }

    private fun enableFullScreenMode() {
        // Hide status and navigation for an immersive experience
        window.decorView.systemUiVisibility = (
            android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
    }

    private fun loadUrl(url: String) {
        webView.loadUrl(url)
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            R.id.action_reload -> {
                webView.reload()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onResume() {
        super.onResume()
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        val url = prefs.getString("home_url", "") ?: ""
        if (url != homeUrl) {
            homeUrl = url
            if (homeUrl.isNotBlank()) loadUrl(homeUrl) else promptForUrl()
        }
    }

    private fun promptForUrl() {
        val edit = EditText(this)
        edit.hint = "https://example.com"
        AlertDialog.Builder(this)
            .setTitle(R.string.enter_website)
            .setView(edit)
            .setPositiveButton(R.string.save) { _, _ ->
                val u = edit.text.toString().trim()
                if (u.isNotBlank()) {
                    getSharedPreferences("app_prefs", MODE_PRIVATE).edit().putString("home_url", u).apply()
                    homeUrl = u
                    loadUrl(homeUrl)
                } else {
                    promptForUrl()
                }
            }
            .setCancelable(false)
            .show()
    }
}