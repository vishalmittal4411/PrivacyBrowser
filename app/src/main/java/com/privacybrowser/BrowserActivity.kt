package com.privacybrowser

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.webkit.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class BrowserActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var urlBar: EditText
    private lateinit var progressBar: ProgressBar
    private lateinit var tvProfileBadge: TextView
    private lateinit var profileId: String
    private lateinit var profileName: String

    private val userAgents = listOf(
        "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36",
        "Mozilla/5.0 (Linux; Android 12; SM-G991B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Mobile Safari/537.36",
        "Mozilla/5.0 (Linux; Android 14; OnePlus 11) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/121.0.0.0 Mobile Safari/537.36",
        "Mozilla/5.0 (Linux; Android 11; Redmi Note 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/118.0.0.0 Mobile Safari/537.36"
    )

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_browser)
        profileId = intent.getStringExtra("PROFILE_ID") ?: "default"
        profileName = intent.getStringExtra("PROFILE_NAME") ?: "Profile"
        webView = findViewById(R.id.webView)
        urlBar = findViewById(R.id.urlBar)
        progressBar = findViewById(R.id.progressBar)
        tvProfileBadge = findViewById(R.id.tvProfileBadge)
        tvProfileBadge.text = "🔒 $profileName"
        setupWebView()
        setupControls()
        webView.loadUrl("https://www.google.com")
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupWebView() {
        WebView.setDataDirectorySuffix(profileId)
        webView.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            cacheMode = WebSettings.LOAD_DEFAULT
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
            setGeolocationEnabled(false)
            allowContentAccess = false
            val idx = profileId.hashCode().and(0x7FFFFFFF) % userAgents.size
            userAgentString = userAgents[idx]
        }
        val trackingDomains = setOf(
            "doubleclick.net", "google-analytics.com", "googletagmanager.com",
            "connect.facebook.net", "scorecardresearch.com", "quantserve.com",
            "adnxs.com", "criteo.com", "hotjar.com", "mixpanel.com",
            "segment.io", "amplitude.com", "clarity.ms", "pagead2.googlesyndication.com"
        )
        webView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                val url = request.url.toString().lowercase()
                for (domain in trackingDomains) {
                    if (url.contains(domain)) return WebResourceResponse("text/plain", "utf-8", null)
                }
                return super.shouldInterceptRequest(view, request)
            }
            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                progressBar.visibility = View.VISIBLE
                urlBar.setText(url)
                injectPrivacyScript(view)
            }
            override fun onPageFinished(view: WebView, url: String) {
                progressBar.visibility = View.GONE
                urlBar.setText(url)
            }
        }
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                progressBar.progress = newProgress
            }
        }
    }

    private fun injectPrivacyScript(view: WebView) {
        val script = """
            (function() {
                Object.defineProperty(navigator, 'hardwareConcurrency', {get: () => 4});
                Object.defineProperty(navigator, 'plugins', {get: () => []});
                if (navigator.getBattery) navigator.getBattery = undefined;
                const origRTC = window.RTCPeerConnection;
                if (origRTC) {
                    window.RTCPeerConnection = function(config) {
                        if (config && config.iceServers) config.iceServers = [];
                        return new origRTC(config);
                    };
                }
            })();
        """.trimIndent()
        view.evaluateJavascript(script, null)
    }

    private fun setupControls() {
        urlBar.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_GO || event?.keyCode == KeyEvent.KEYCODE_ENTER) {
                navigateToUrl(urlBar.text.toString()); true
            } else false
        }
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener { if (webView.canGoBack()) webView.goBack() }
        findViewById<ImageButton>(R.id.btnForward).setOnClickListener { if (webView.canGoForward()) webView.goForward() }
        findViewById<ImageButton>(R.id.btnRefresh).setOnClickListener { webView.reload() }
        findViewById<ImageButton>(R.id.btnClear).setOnClickListener {
            webView.clearCache(true)
            webView.clearHistory()
            CookieManager.getInstance().removeAllCookies(null)
            Toast.makeText(this, "Profile cleared!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToUrl(input: String) {
        val url = when {
            input.startsWith("http://") || input.startsWith("https://") -> input
            input.contains(".") && !input.contains(" ") -> "https://$input"
            else -> "https://www.google.com/search?q=${input.replace(" ", "+")}"
        }
        webView.loadUrl(url)
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) webView.goBack() else super.onBackPressed()
    }
}
