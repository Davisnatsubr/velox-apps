package com.veloxplay.app

import android.annotation.SuppressLint
import android.app.UiModeManager
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
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
import android.webkit.WebResourceResponse
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
import java.io.ByteArrayInputStream
import java.net.HttpURLConnection
import java.net.URL

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
    private var autoRetries = 0
    private var pageFailed = false
    private val updater by lazy { Updater(this) { js -> runOnUiThread { web.evaluateJavascript(js, null) } } }
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
        val idx = d?.getIntExtra(PlayerActivity.EXTRA_INDEX, 0) ?: 0
        web.evaluateJavascript("window.__appPlayerDone && window.__appPlayerDone($pos, $dur, $ended, $idx)", null)
        web.requestFocus()
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // celular e tablet: o app fica sempre deitado (como o Spark), inclusive no login
        if (!isTv) requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
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
            textZoom = 100 // ignora o "tamanho da fonte" do sistema, que deixava tudo grande em TV Box
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

            override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                if (url?.startsWith("about:") != true) pageFailed = false
            }

            override fun onPageFinished(view: WebView, url: String?) {
                if (url?.startsWith("about:") == true) return
                if (!pageFailed) { autoRetries = 0; sendSize() }
            }

            /* Como o Spark: os vídeos do ao vivo (HLS/TS) vêm de outro servidor, que não manda permissão (CORS)
               para o navegador ler. O app busca esses arquivos ele mesmo e devolve já com a permissão. */
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
                val u = request.url
                if (u.host == siteHost || (u.scheme != "http" && u.scheme != "https")) return null
                val origin = request.requestHeaders.entries.firstOrNull { it.key.equals("Origin", true) }?.value
                if (request.method.equals("OPTIONS", true)) return corsPreflight(origin)
                if (origin == null || !request.method.equals("GET", true)) return null // imagens e páginas seguem normais
                return try { corsFetch(u.toString(), request.requestHeaders, origin) } catch (e: Exception) { null }
            }

            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: WebResourceError) {
                if (!request.isForMainFrame) return
                pageFailed = true
                // falha passageira (rede acordando, Wi-Fi trocando): tenta de novo sozinho antes de mostrar o erro
                if (autoRetries < 2) {
                    autoRetries++
                    view.stopLoading()
                    view.loadUrl("about:blank")
                    root.postDelayed({ loadSite() }, 2500L * autoRetries)
                } else showError()
            }
        }

        chrome = object : WebChromeClient() {
            // sem isso o Android mostra um quadro cinza com um "play" enquanto o vídeo carrega
            override fun getDefaultVideoPoster(): Bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)

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

        // janela mudou de tamanho (girou a tela, janela redimensionada no PC/Chromebook): o site recalcula a escala
        web.addOnLayoutChangeListener { _, l, t, r, b, ol, ot, orr, ob ->
            if (r - l != orr - ol || b - t != ob - ot) sendSize()
        }

        if (savedInstanceState != null) web.restoreState(savedInstanceState) else loadSite()
        web.requestFocus()
    }

    private fun corsHeaders(origin: String?) = mutableMapOf(
        "Access-Control-Allow-Origin" to (origin ?: "*"),
        "Access-Control-Allow-Credentials" to "true",
        "Access-Control-Allow-Methods" to "GET, HEAD, OPTIONS",
        "Access-Control-Allow-Headers" to "*",
        "Access-Control-Expose-Headers" to "*",
        "Access-Control-Max-Age" to "86400",
    )

    private fun corsPreflight(origin: String?) =
        WebResourceResponse("text/plain", "utf-8", 204, "No Content", corsHeaders(origin), ByteArrayInputStream(ByteArray(0)))

    private fun corsFetch(start: String, reqHeaders: Map<String, String>, origin: String): WebResourceResponse {
        var url = start
        var c: HttpURLConnection
        var hops = 0
        while (true) { // segue redirecionamentos (inclusive http <-> https), como o servidor do canal costuma fazer
            c = URL(url).openConnection() as HttpURLConnection
            c.instanceFollowRedirects = false
            c.connectTimeout = 15000; c.readTimeout = 30000
            for ((k, v) in reqHeaders) if (!k.equals("Origin", true) && !k.equals("Referer", true) && !k.equals("Host", true) && !k.equals("Accept-Encoding", true)) c.setRequestProperty(k, v)
            val code = c.responseCode
            if (code in 300..399 && hops < 8) {
                val loc = c.getHeaderField("Location") ?: break
                url = URL(URL(url), loc).toString(); hops++; c.disconnect(); continue
            }
            break
        }
        val code = c.responseCode
        val ctype = c.contentType ?: "application/octet-stream"
        val mime = ctype.substringBefore(';').trim()
        val headers = corsHeaders(origin)
        for ((k, v) in c.headerFields) if (k != null && v.isNotEmpty() && !k.startsWith("Access-Control", true)
            && !k.equals("Content-Encoding", true) && !k.equals("Transfer-Encoding", true) && !k.equals("Content-Length", true)) headers[k] = v.joinToString(", ")
        val body = if (code >= 400) c.errorStream ?: ByteArrayInputStream(ByteArray(0)) else c.inputStream
        val isM3u8 = mime.contains("mpegurl", true) || url.substringBefore('?').endsWith(".m3u8", true)
        // a lista HLS tem endereços relativos ao servidor final (depois do redirecionamento): deixa todos completos
        val stream = if (isM3u8 && code < 400) {
            val base = URL(url)
            val txt = body.bufferedReader().use { it.readText() }
            val fixed = txt.lineSequence().joinToString("\n") { line ->
                val t = line.trim()
                when {
                    t.isEmpty() -> line
                    t.startsWith("#") -> Regex("URI=\"([^\"]+)\"").replace(line) { m -> "URI=\"" + URL(base, m.groupValues[1]) + "\"" }
                    else -> URL(base, t).toString()
                }
            }
            ByteArrayInputStream(fixed.toByteArray())
        } else body
        val reason = c.responseMessage?.takeIf { it.isNotBlank() } ?: if (code < 400) "OK" else "Error"
        val charset = Regex("charset=([^;]+)", RegexOption.IGNORE_CASE).find(ctype)?.groupValues?.get(1)?.trim()
        return WebResourceResponse(if (isM3u8) "application/vnd.apple.mpegurl" else mime, charset, code, reason, headers, stream)
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
                autoRetries = 0
                loadSite() // carrega do zero (o reload repetia a página que falhou)
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

    /** Tamanho da área do site em "pixels de site" (dp). */
    /** Abre o site pedindo sempre a versão nova da página (o visual atualiza sem reinstalar o app). */
    private fun loadSite() = web.loadUrl(BuildConfig.SITE_URL, mapOf("Cache-Control" to "no-cache", "Pragma" to "no-cache"))

    private fun cssSize(): String {
        val d = resources.displayMetrics.density
        return "${Math.round(web.width / d)},${Math.round(web.height / d)}"
    }

    private fun sendSize() {
        if (web.width <= 0) return
        web.evaluateJavascript("window.__appResize && window.__appResize(${cssSize()})", null)
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
            .putExtra(PlayerActivity.EXTRA_EXT, o.optString("ext"))
            .putExtra(PlayerActivity.EXTRA_API, o.optString("api"))
            .putExtra(PlayerActivity.EXTRA_INDEX, o.optInt("index"))
            .putExtra(PlayerActivity.EXTRA_CHANNELS, o.optJSONArray("channels")?.toString() ?: "")
        playerLauncher.launch(i)
    }

    /** Funções que o site chama: window.AndroidApp.play(...), .version(), .isTv(), .exit() */
    inner class Bridge {
        @JavascriptInterface
        fun play(json: String) { runOnUiThread { openPlayer(json) } }

        @JavascriptInterface
        fun version(): String = BuildConfig.VERSION_NAME

        @JavascriptInterface
        fun versionCode(): Int = BuildConfig.VERSION_CODE

        /** Só a versão por APK (fora da Play Store) se atualiza sozinha. */
        @JavascriptInterface
        fun canUpdate(): Boolean = BuildConfig.SELF_UPDATE

        @JavascriptInterface
        fun update(url: String) { if (BuildConfig.SELF_UPDATE) runOnUiThread { updater.start(url) } }

        @JavascriptInterface
        fun isTv(): Boolean = this@MainActivity.isTv

        @JavascriptInterface
        fun viewSize(): String = if (web.width > 0) cssSize() else {
            val m = resources.displayMetrics
            "${Math.round(m.widthPixels / m.density)},${Math.round(m.heightPixels / m.density)}"
        }

        /** "landscape" = deitado (depois do login), "portrait" = em pé no celular (login). Na TV não faz nada. */
        @JavascriptInterface
        fun orientation(mode: String) {
            if (this@MainActivity.isTv) return
            runOnUiThread {
                                val want = when (mode) {
                    "landscape" -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                    "portrait" -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE // sempre deitado
                    else -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                }
                if (requestedOrientation != want) requestedOrientation = want
            }
        }

        @JavascriptInterface
        fun exit() { runOnUiThread { finish() } }
    }

    override fun onResume() {
        super.onResume()
        web.onResume()
        if (BuildConfig.SELF_UPDATE) updater.resumeIfPending()
    }
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
