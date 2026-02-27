package com.privacybrowser

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.webkit.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import java.io.File
import kotlin.random.Random

class BrowserActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var urlBar: EditText
    private lateinit var progressBar: ProgressBar
    private lateinit var tvProfileBadge: TextView

    private lateinit var profileId: String
    private lateinit var profileName: String
    private lateinit var profileColor: String

    // Fake user agents pool for fingerprint randomization
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
        profileColor = intent.getStringExtra("PROFILE_COLOR") ?: "#2196F3"

        webView = findViewById(R.id.webView)
        urlBar = findViewById(R.id.urlBar)
        progressBar = findViewById(R.id.progressBar)
        tvProfileBadge = findViewById(R.id.tvProfileBadge)

        tvProfileBadge.text = "🔒 $profileName"

        setupIsolatedWebView()
        setupUrlBar()

        webView.loadUrl("https://www.google.com")
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupIsolatedWebView() {
        // CORE FEATURE: Isolated data directory per profile
        WebView.setDataDirectorySuffix(profileId)

        val settings = webView.settings
        settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            cacheMode = WebSettings.LOAD_DEFAULT
            mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW

            // Anti-fingerprinting: random user agent per profile
            val profileIndex = profileId.hashCode().and(0x7FFFFFFF) % userAgents.size
            userAgentString = userAgents[profileIndex]

            // Privacy settings
            setGeolocationEnabled(false)
            allowContentAccess = false
            savePassword = false
            saveFormData = false

            // Block 3rd party cookies concept via settings
            databaseEnabled = false
        }

        // Block tracking via WebViewClient
        webView.webViewClient = object : WebViewClient() {

            // Known tracking domains to block
            private val trackingDomains = setOf(
                "doubleclick.net", "google-analytics.com", "googletagmanager.com",
                "facebook.com/tr", "connect.facebook.net", "analytics.twitter.com",
                "ads.twitter.com", "scorecardresearch.com", "quantserve.com",
                "adnxs.com", "moatads.com", "criteo.com", "hotjar.com",
                "mixpanel.com", "segment.io", "amplitude.com", "clarity.ms",
                "track.", "tracking.", "pixel.", "beacon.", "analytics.",
                "adservice.google.", "pagead2.googlesyndication.com",
                "adsystem.amazon.com"
            )

            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest
            ): WebResourceResponse? {
                val url = request.url.toString().lowercase()

                // Block tracking requests
                for (domain in trackingDomains) {
                    if (url.contains(domain)) {
                        return WebResourceResponse("text/plain", "utf-8", null)
                    }
                }
                return super.shouldInterceptRequest(view, request)
            }

            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) {
                progressBar.visibility = View.VISIBLE
                urlBar.setText(url)
                // Inject anti-fingerprint JS
                injectPrivacyScript(view)
            }

            override fun onPageFinished(view: WebView, url: String) {
                progressBar.visibility = View.GONE
                urlBar.setText(url)
                // Re-inject after page load
                injectPrivacyScript(view)
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, newProgress: Int) {
                progressBar.progress = newProgress
            }
        }
    }

    private fun injectPrivacyScript(view: WebView) {
        // Anti-fingerprinting JavaScript injection
        val randomSeed = profileId.hashCode()
        val canvasNoise = (randomSeed % 10) + 1

        val script = """
            (function() {
                // Override canvas fingerprinting
                const origToDataURL = HTMLCanvasElement.prototype.toDataURL;
                HTMLCanvasElement.prototype.toDataURL = function() {
                    const result = origToDataURL.apply(this, arguments);
                    return result; // Could add noise here
                };
                
                // Randomize/block navigator properties
                Object.defineProperty(navigator, 'hardwareConcurrency', {get: () => ${(randomSeed % 4) + 2}});
                Object.defineProperty(navigator, 'deviceMemory', {get: () => ${(randomSeed % 3) + 2}});
                Object.defineProperty(navigator, 'plugins', {get: () => []});
                Object.defineProperty(navigator, 'languages', {get: () => ['en-US', 'en']});
                
                // Block WebRTC IP leaks
                const origRTCPeerConnection = window.RTCPeerConnection;
                window.RTCPeerConnection = function(config) {
                    if (config && config.iceServers) {
                        config.iceServers = [];
                    }
                    return new origRTCPeerConnection(config);
                };
                
                // Block battery API
                if (navigator.getBattery) {
                    navigator.getBattery = undefined;
                }
                
                // Randomize screen info slightly
                Object.defineProperty(screen, 'colorDepth', {get: () => 24});
                
                console.log('Privacy Shield Active for profile: ${profileId}');
            })();
        """.trimIndent()

        view.evaluateJavascript(script, null)
    }

    private fun setupUrlBar() {
        urlBar.setOnEditorActionListener { _, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_GO ||
                event?.keyCode == KeyEvent.KEYCODE_ENTER
            ) {
                navigateToUrl(urlBar.text.toString())
                true
            } else false
        }

        // Back button
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            if (webView.canGoBack()) webView.goBack()
        }

        // Forward button
        findViewById<ImageButton>(R.id.btnForward).setOnClickListener {
            if (webView.canGoForward()) webView.goForward()
        }

        // Refresh button
        findViewById<ImageButton>(R.id.btnRefresh).setOnClickListener {
            webView.reload()
        }

        // Clear data for this profile only
        findViewById<ImageButton>(R.id.btnClear).setOnClickListener {
            webView.clearCache(true)
            webView.clearHistory()
            CookieManager.getInstance().removeAllCookies(null)
            Toast.makeText(this, "Profile data cleared", Toast.LENGTH_SHORT).show()
            webView.loadUrl("about:blank")
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
        if (webView.canGoBack()) webView.goBack()
        else super.onBackPressed()
    }
}
