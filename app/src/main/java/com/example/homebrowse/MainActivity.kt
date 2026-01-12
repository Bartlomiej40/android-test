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
    private var homeUrl: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        webView = findViewById(R.id.webview)
        val prefs = getSharedPreferences("app_prefs", MODE_PRIVATE)
        homeUrl = prefs.getString("home_url", "") ?: ""

        setupWebView()

        if (homeUrl.isBlank()) {
            promptForUrl()
        } else {
            loadUrl(homeUrl)
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
        }
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