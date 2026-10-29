package com.velox.rotapaga

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.ViewGroup
import android.webkit.GeolocationPermissions
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.webkit.WebViewAssetLoader
import java.lang.ref.WeakReference
import kotlin.math.max

/** Tela única: o app (assets/www) num WebView, ligado ao Android pela ponte "RotaApp". */
class MainActivity : ComponentActivity() {

    companion object {
        const val EXTRA_ROUTE = "rota_detectada"
        private const val HOST = "appassets.androidplatform.net"
        private const val START = "https://$HOST/assets/www/index.html"
        private var ref: WeakReference<MainActivity>? = null

        /** Chamado quando o alerta dispara com o app aberto: mostra a rota na hora. */
        fun notifyRoute() {
            ref?.get()?.let { a -> a.runOnUiThread { a.js("window.__rota&&window.__rota('resume')") } }
        }
    }

    lateinit var web: WebView
    private lateinit var root: FrameLayout
    private var fileCb: ValueCallback<Array<Uri>>? = null
    private var geoPending: Pair<String, GeolocationPermissions.Callback>? = null
    private var pendingTrip = false
    private var lastBack = 0L

    private val fileLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        fileCb?.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(res.resultCode, res.data))
        fileCb = null
    }

    private val locPerm = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { r ->
        val ok = r[Manifest.permission.ACCESS_FINE_LOCATION] == true || r[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        geoPending?.let { (origin, cb) -> cb.invoke(origin, ok, false) }
        geoPending = null
        if (pendingTrip) {
            pendingTrip = false
            if (ok) TripService.start(this)
            js("window.__rota&&window.__rota('trip','" + (if (ok) "started" else "denied") + "')")
        }
    }

    private val notifPerm = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    fun hasLoc() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    fun askNotif() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) notifPerm.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    /** Liga o GPS do cronômetro (pede a localização se ainda não tiver). */
    fun startTrip() {
        if (hasLoc()) { TripService.start(this); return }
        pendingTrip = true
        locPerm.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
    }

    fun js(code: String) { if (::web.isInitialized) web.evaluateJavascript(code, null) }

    fun setBars(bg: String, dark: Boolean) {
        val c = try { Color.parseColor(bg) } catch (_: Exception) { return }
        root.setBackgroundColor(c)
        web.setBackgroundColor(c)
        val ctl = WindowInsetsControllerCompat(window, window.decorView)
        ctl.isAppearanceLightStatusBars = !dark
        ctl.isAppearanceLightNavigationBars = !dark
    }

    fun openExternal(u: Uri) {
        try { startActivity(Intent(Intent.ACTION_VIEW, u).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        catch (_: Exception) { Toast.makeText(this, "Nenhum app abre este link", Toast.LENGTH_SHORT).show() }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        root = FrameLayout(this)
        root.setBackgroundColor(Color.parseColor("#F2F5F3"))
        web = WebView(this)
        root.addView(web, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        setContentView(root)
        // conteúdo fora da barra de status, da barra de navegação e do teclado
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val b = insets.getInsets(WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())
            val ime = insets.getInsets(WindowInsetsCompat.Type.ime())
            v.setPadding(b.left, b.top, b.right, max(b.bottom, ime.bottom))
            WindowInsetsCompat.CONSUMED
        }

        val loader = WebViewAssetLoader.Builder()
            .setDomain(HOST)
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()

        with(web.settings) {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            setGeolocationEnabled(true)
            allowFileAccess = false
            // permite servidor de contas em http (sem certificado). O ideal é https.
            mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            textZoom = 100
            userAgentString = userAgentString + " RotaPaga/" + BuildConfig.VERSION_NAME
        }
        web.addJavascriptInterface(Bridge(this), "RotaApp")

        web.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? =
                loader.shouldInterceptRequest(request.url)

            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                if (request.url.host == HOST) return false
                openExternal(request.url) // Maps, Waze, WhatsApp...
                return true
            }
        }

        web.webChromeClient = object : WebChromeClient() {
            override fun onGeolocationPermissionsShowPrompt(origin: String, callback: GeolocationPermissions.Callback) {
                if (hasLoc()) { callback.invoke(origin, true, false); return }
                geoPending = origin to callback
                locPerm.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
            }

            override fun onShowFileChooser(view: WebView, cb: ValueCallback<Array<Uri>>, params: FileChooserParams): Boolean {
                fileCb?.onReceiveValue(null)
                fileCb = cb
                return try { fileLauncher.launch(params.createIntent()); true } catch (_: Exception) { fileCb = null; false }
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                web.evaluateJavascript("(function(){try{return window.__appBack?window.__appBack():false}catch(e){return false}})()") { r ->
                    if (r == "true") return@evaluateJavascript
                    val now = SystemClock.elapsedRealtime()
                    if (now - lastBack < 2000) finish()
                    else { lastBack = now; Toast.makeText(this@MainActivity, "Aperte voltar de novo para sair", Toast.LENGTH_SHORT).show() }
                }
            }
        })

        if (savedInstanceState != null) web.restoreState(savedInstanceState) else web.loadUrl(START)
        handleIntent(intent)
        // o cronômetro estava ligado (app foi fechado): garante o GPS de novo
        if (TripStore.running(this) && hasLoc()) TripService.start(this)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(i: Intent?) {
        if (i?.getBooleanExtra(EXTRA_ROUTE, false) == true) {
            Alert.stop(this)
            Overlay.hideCard()
            js("window.__rota&&window.__rota('resume')")
        }
    }

    override fun onResume() {
        super.onResume()
        ref = WeakReference(this)
        Overlay.syncBubble(this)
        // alguns celulares (Xiaomi, Samsung) desligam o leitor de notificações; pede para religar
        try {
            if (androidx.core.app.NotificationManagerCompat.getEnabledListenerPackages(this).contains(packageName))
                android.service.notification.NotificationListenerService.requestRebind(android.content.ComponentName(this, RouteListener::class.java))
        } catch (_: Exception) { }
        js("window.__rota&&window.__rota('resume')")
    }

    override fun onDestroy() {
        if (ref?.get() === this) ref = null
        super.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        web.saveState(outState)
    }
}
