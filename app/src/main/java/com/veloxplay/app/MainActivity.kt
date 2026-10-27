package com.veloxplay.app

import android.annotation.SuppressLint
import android.app.UiModeManager
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import org.json.JSONObject

/**
 * Tela principal: o site (Velox ou Pulso) dentro de um WebView.
 * Os vídeos em tela cheia abrem no PlayerActivity (ExoPlayer), que toca formatos que o navegador não toca.
 */
class MainActivity : ComponentActivity() {

    private lateinit var root: FrameLayout
    private lateinit var web: WebView
    private var errorView: View? = null
    private var customView: View? = null
    private var customCallback: WebChromeClient.CustomViewCallback? = null
    private lateinit var chrome: WebChromeClient
    private var lastBack = 0L
    private val siteHost: String by lazy { Uri.parse(BuildConfig.SITE_URL).host ?: "" }

    private val isTv: Boolean by lazy {
        val ui = getSystemService(UI_MODE_SERVICE) as UiModeManager
        ui.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION ||
            packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK)
    }

    // Volta do player nativo: devolve posição, duração e se terminou para o site salvar o progresso.
    private val playerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        val d = res.data
        val pos = (d?.getLongExtra(PlayerActivity.EXTRA_POS, 0L) ?: 0L) / 1000.0
        val dur = (d?.getLongExtra(PlayerActivity.EXTRA_DUR, 0L) ?: 0L).coerceAtLeast(0L) / 1000.0
        val ended = d?.getBooleanExtra(PlayerActivity.EXTRA_ENDED, false) ?: false
        web.evaluateJavascript("window.__appPlayerDone && window.__appPlayerDone($pos, $dur, $ended)", null)
        web.requestFocus()
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        root = FrameLayout(this)
        root.setBackgroundColor(ContextCompat.getColor(this, R.color.brand_bg))
        web = WebView(this)
        root.addView(web, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        setContentView(root)
        hideSystemBars()
        // em celulares com "notch", o conteúdo não fica escondido atrás da câmera
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val c = insets.getInsets(WindowInsetsCompat.Type.displayCutout())
            v.setPadding(c.left, c.top, c.right, c.bottom)
            insets
        }

        with(web.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            mediaPlaybackRequiresUserGesture = false
            mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            useWideViewPort = true
            loadWithOverviewMode = true
            setSupportZoom(false)
            builtInZoomControls = false
            userAgentString = userAgentString + " VeloxApp/" + BuildConfig.VERSION_NAME + if (isTv) " AndroidTV" else ""
        }
        web.setBackgroundColor(ContextCompat.getColor(this, R.color.brand_bg))
        web.isFocusable = true
        web.isFocusableInTouchMode = true
        web.addJavascriptInterface(Bridge(), "AndroidApp")

        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                val u = request.url
                if (u.host == siteHost || u.scheme == "about" || u.scheme == "javascript") return false
                // links de fora (WhatsApp, Instagram...) abrem no app certo
                return try { startActivity(Intent(Intent.ACTION_VIEW, u)); true } catch (e: Exception) { true }
            }

            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                if (request.isForMainFrame) showError()
            }
        }

        chrome = object : WebChromeClient() {
            // tela cheia pedida pelo site (mini player)
            override fun onShowCustomView(view: View, callback: CustomViewCallback) {
                if (customView != null) { callback.onCustomViewHidden(); return }
                customView = view
                customCallback = callback
                root.addView(view, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
                web.visibility = View.GONE
                hideSystemBars()
            }

            override fun onHideCustomView() {
                val v = customView ?: return
                root.removeView(v)
                customView = null
                web.visibility = View.VISIBLE
                customCallback?.onCustomViewHidden()
                customCallback = null
                web.requestFocus()
            }
        }
        web.webChromeClient = chrome

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = onBack()
        })

        if (savedInstanceState != null) web.restoreState(savedInstanceState) else web.loadUrl(BuildConfig.SITE_URL)
        web.requestFocus()
    }

    private fun onBack() {
        if (customView != null) { chrome.onHideCustomView(); return }
        if (errorView != null) { finish(); return }
        // o site fecha o que estiver aberto; se já estiver na tela inicial responde "false"
        web.evaluateJavascript("(function(){try{return window.__appBack?window.__appBack():false}catch(e){return false}})()") { r ->
            if (r == "true") return@evaluateJavascript
            val now = SystemClock.elapsedRealtime()
            if (now - lastBack < 2000) finish()
            else {
                lastBack = now
                Toast.makeText(this, "Aperte voltar de novo para sair", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val c = WindowInsetsControllerCompat(window, window.decorView)
        c.hide(WindowInsetsCompat.Type.systemBars())
        c.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    /** Sem internet ou site fora do ar: mensagem com botão para tentar de novo. */
    private fun showError() {
        if (errorView != null) return
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(ContextCompat.getColor(this@MainActivity, R.color.brand_bg))
            setPadding(48, 48, 48, 48)
        }
        val msg = TextView(this).apply {
            text = "Não foi possível abrir o ${getString(R.string.app_name)}.\nConfira a internet e tente de novo."
            setTextColor(Color.WHITE)
            textSize = 20f
            gravity = Gravity.CENTER
        }
        val btn = Button(this).apply {
            text = "Tentar de novo"
            isFocusable = true
            setOnClickListener {
                root.removeView(box)
                errorView = null
                web.visibility = View.VISIBLE
                web.reload()
                web.requestFocus()
            }
        }
        box.addView(msg)
        box.addView(btn, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = 40 })
        root.addView(box, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        web.visibility = View.INVISIBLE
        errorView = box
        btn.requestFocus()
    }

    private fun openPlayer(json: String) {
        val o = try { JSONObject(json) } catch (e: Exception) { return }
        val url = o.optString("url")
        if (url.isEmpty()) return
        val i = Intent(this, PlayerActivity::class.java)
            .putExtra(PlayerActivity.EXTRA_URL, url)
            .putExtra(PlayerActivity.EXTRA_URL2, o.optString("url2"))
            .putExtra(PlayerActivity.EXTRA_TITLE, o.optString("title"))
            .putExtra(PlayerActivity.EXTRA_SUB, o.optString("sub"))
            .putExtra(PlayerActivity.EXTRA_LIVE, o.optBoolean("live"))
            .putExtra(PlayerActivity.EXTRA_START, o.optLong("start"))
        playerLauncher.launch(i)
    }

    /** Funções que o site chama: window.AndroidApp.play(...), .version(), .isTv(), .exit() */
    inner class Bridge {
        @JavascriptInterface
        fun play(json: String) { runOnUiThread { openPlayer(json) } }

        @JavascriptInterface
        fun version(): String = BuildConfig.VERSION_NAME

        @JavascriptInterface
        fun isTv(): Boolean = this@MainActivity.isTv

        @JavascriptInterface
        fun exit() { runOnUiThread { finish() } }
    }

    override fun onResume() { super.onResume(); web.onResume() }
    override fun onPause() { web.onPause(); super.onPause() }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        web.saveState(outState)
    }

    override fun onDestroy() {
        web.destroy()
        super.onDestroy()
    }
}
