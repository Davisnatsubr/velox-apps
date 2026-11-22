package com.veloxplay.app

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.CookieManager
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.annotation.OptIn
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.media3.ui.TrackSelectionDialogBuilder
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

/**
 * Player nativo (ExoPlayer). Toca HLS, TS, MP4 e MKV usando os decodificadores do aparelho
 * (AC3/E-AC3 e HEVC quando a TV/TV Box suporta). Tenta a DNS reserva sozinho se a principal falhar.
 */
@OptIn(UnstableApi::class)
class PlayerActivity : ComponentActivity() {

    companion object {
        const val EXTRA_URL = "url"
        const val EXTRA_URL2 = "url2"
        const val EXTRA_TITLE = "title"
        const val EXTRA_SUB = "sub"
        const val EXTRA_LIVE = "live"
        const val EXTRA_START = "start"
        const val EXTRA_POS = "pos"
        const val EXTRA_DUR = "dur"
        const val EXTRA_ENDED = "ended"
        const val EXTRA_EXT = "ext"
        const val EXTRA_API = "api"
        const val EXTRA_INDEX = "index"
        const val EXTRA_CHANNELS = "channels"
        private val RESIZE = intArrayOf(
            AspectRatioFrameLayout.RESIZE_MODE_FIT,
            AspectRatioFrameLayout.RESIZE_MODE_FILL,
            AspectRatioFrameLayout.RESIZE_MODE_ZOOM,
        )
        private val RESIZE_NAMES = arrayOf("Original", "Esticar", "Zoom")
    }

    private lateinit var view: PlayerView
    private lateinit var top: LinearLayout
    private lateinit var aspectBtn: TextView
    private lateinit var errorText: TextView
    private var player: ExoPlayer? = null
    private val urls = mutableListOf<String>()
    private var urlIdx = 0
    private var live = false
    private var startPos = 0L
    private var lastPos = 0L
    private var lastDur = 0L
    private var ended = false
    private var liveRetries = 0
    private var resizeIdx = 0
    private val handler = Handler(Looper.getMainLooper())
    private var desktopUa = false

    // lista de canais do ao vivo (troca de canal dentro do player)
    private data class Ch(val id: String, val name: String)
    private val channels = mutableListOf<Ch>()
    private var chIdx = 0
    private var zapToken = 0
    private var liveExt = "m3u8"
    private var apiUrl = ""
    private val io = Executors.newSingleThreadExecutor()
    private lateinit var titleView: TextView
    private lateinit var osd: TextView
    private var listPanel: ListView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        live = intent.getBooleanExtra(EXTRA_LIVE, false)
        startPos = if (live) 0L else intent.getLongExtra(EXTRA_START, 0L)
        intent.getStringExtra(EXTRA_URL)?.takeIf { it.isNotEmpty() }?.let { urls.add(it) }
        intent.getStringExtra(EXTRA_URL2)?.takeIf { it.isNotEmpty() && it !in urls }?.let { urls.add(it) }
        liveExt = intent.getStringExtra(EXTRA_EXT)?.takeIf { it == "ts" || it == "m3u8" } ?: "m3u8"
        apiUrl = intent.getStringExtra(EXTRA_API) ?: ""
        try {
            val a = JSONArray(intent.getStringExtra(EXTRA_CHANNELS) ?: "")
            for (i in 0 until a.length()) a.optJSONObject(i)?.let { o -> channels.add(Ch(o.optString("id"), o.optString("name"))) }
        } catch (_: Exception) {}
        chIdx = intent.getIntExtra(EXTRA_INDEX, 0).coerceIn(0, (channels.size - 1).coerceAtLeast(0))
        resizeIdx = getSharedPreferences("player", Context.MODE_PRIVATE).getInt("resize", 0).coerceIn(0, 2)

        val frame = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }
        view = PlayerView(this).apply {
            useController = true
            controllerShowTimeoutMs = 4000
            controllerAutoShow = true
            setShowSubtitleButton(false) // legenda e áudio ficam na barra de cima (como no Spark)
            setShowNextButton(false)
            setShowPreviousButton(false)
            setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
            resizeMode = RESIZE[resizeIdx]
            keepScreenOn = true
            isFocusable = true
        }
        frame.addView(view, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        // barra de cima no modelo do Spark: "← Voltar" | título no centro | legenda, áudio e formato da tela
        top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(14), dp(10), dp(18), dp(10))
            background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(0xCC000000.toInt(), 0x00000000))
        }
        val back = TextView(this).apply {
            text = "←   Voltar"
            setTextColor(Color.WHITE); textSize = 16f
            setPadding(dp(12), dp(8), dp(14), dp(8))
            isFocusable = true; isClickable = true
            background = focusBg()
            setOnClickListener { finishWithResult() }
        }
        top.addView(back)
        val titles = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL }
        titleView = TextView(this).apply {
            text = if (hasChannels()) chLabel(chIdx) else intent.getStringExtra(EXTRA_TITLE) ?: ""
            setTextColor(Color.WHITE); textSize = 19f; maxLines = 1; gravity = Gravity.CENTER
            ellipsize = android.text.TextUtils.TruncateAt.END
        }
        titles.addView(titleView)
        val sub = intent.getStringExtra(EXTRA_SUB) ?: ""
        if (sub.isNotEmpty() || live) titles.addView(TextView(this).apply {
            text = if (live) "● AO VIVO" + (if (sub.isNotEmpty()) "  ·  $sub" else "") else sub
            setTextColor(if (live) 0xFFFF4D6D.toInt() else 0xFFB8C0D0.toInt()); textSize = 13f; maxLines = 1; gravity = Gravity.CENTER
        })
        top.addView(titles, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { leftMargin = dp(12); rightMargin = dp(12) })
        if (hasChannels()) top.addView(pill("☰ Canais") { openList() }, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { rightMargin = dp(8) })
        top.addView(iconBtn(androidx.media3.ui.R.drawable.exo_styled_controls_subtitle_off, "Legendas") { pickTrack(C.TRACK_TYPE_TEXT, "Legendas") })
        top.addView(iconBtn(androidx.media3.ui.R.drawable.exo_styled_controls_audiotrack, "Áudio") { pickTrack(C.TRACK_TYPE_AUDIO, "Áudio") })
        aspectBtn = TextView(this).apply { visibility = View.GONE } // o formato agora é o ícone abaixo (o texto aparece como aviso)
        top.addView(iconBtn(androidx.media3.ui.R.drawable.exo_styled_controls_fullscreen_enter, "Formato da tela") { cycleResize() })
        frame.addView(top, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP))

        errorText = TextView(this).apply {
            setTextColor(Color.WHITE); textSize = 18f; gravity = Gravity.CENTER
            setPadding(dp(40), dp(24), dp(40), dp(24))
            background = GradientDrawable().apply { cornerRadius = dp(16).toFloat(); setColor(0xDD141C30.toInt()) }
            visibility = View.GONE
        }
        frame.addView(errorText, FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER))

        // nome do canal ao trocar (aparece uns segundos no canto)
        osd = TextView(this).apply {
            setTextColor(Color.WHITE); textSize = 22f
            setPadding(dp(22), dp(12), dp(22), dp(12))
            background = GradientDrawable().apply { cornerRadius = dp(12).toFloat(); setColor(0xCC000000.toInt()) }
            visibility = View.GONE
        }
        frame.addView(osd, FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP or Gravity.END).apply { topMargin = dp(24); rightMargin = dp(24) })

        if (hasChannels()) {
            val lv = ListView(this).apply {
                setBackgroundColor(0xE6101420.toInt())
                divider = null
                isFocusable = true
                visibility = View.GONE
                choiceMode = ListView.CHOICE_MODE_SINGLE
                setSelector(GradientDrawable().apply { setColor(0xFF2B48D8.toInt()) })
                adapter = object : ArrayAdapter<String>(this@PlayerActivity, android.R.layout.simple_list_item_1, channels.indices.map { chLabel(it) }) {
                    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View =
                        (super.getView(position, convertView, parent) as TextView).apply {
                            setTextColor(Color.WHITE); textSize = 17f; maxLines = 1
                            setPadding(dp(20), dp(12), dp(20), dp(12))
                            setTypeface(null, if (position == chIdx) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL)
                        }
                }
                onItemClickListener = AdapterView.OnItemClickListener { _, _, pos, _ -> closeList(); if (pos != chIdx) playChannel(pos) }
            }
            frame.addView(lv, FrameLayout.LayoutParams(dp(380), ViewGroup.LayoutParams.MATCH_PARENT, Gravity.START))
            listPanel = lv
        }

        view.setControllerVisibilityListener(PlayerView.ControllerVisibilityListener { v -> top.visibility = v })
        setContentView(frame)
        hideSystemBars()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() { if (listPanel?.visibility == View.VISIBLE) closeList() else finishWithResult() }
        })
    }

    /** Fundo que acende com o foco do controle remoto (redondo, discreto). */
    private fun focusBg() = android.graphics.drawable.StateListDrawable().apply {
        addState(intArrayOf(android.R.attr.state_focused), GradientDrawable().apply { cornerRadius = dp(22).toFloat(); setColor(0x55FFFFFF) })
        addState(intArrayOf(android.R.attr.state_pressed), GradientDrawable().apply { cornerRadius = dp(22).toFloat(); setColor(0x33FFFFFF) })
        addState(intArrayOf(), GradientDrawable().apply { cornerRadius = dp(22).toFloat(); setColor(Color.TRANSPARENT) })
    }

    private fun iconBtn(res: Int, desc: String, onClick: () -> Unit) = ImageView(this).apply {
        setImageResource(res)
        contentDescription = desc
        setColorFilter(Color.WHITE)
        setPadding(dp(10), dp(10), dp(10), dp(10))
        isFocusable = true; isClickable = true
        background = focusBg()
        setOnClickListener { onClick() }
        layoutParams = LinearLayout.LayoutParams(dp(46), dp(46)).apply { leftMargin = dp(6) }
    }

    /** Escolha de faixa de áudio ou legenda (lista do próprio ExoPlayer). */
    private fun pickTrack(type: Int, title: String) {
        val p = player ?: return
        val has = p.currentTracks.groups.any { it.type == type }
        if (!has) { Toast.makeText(this, if (type == C.TRACK_TYPE_TEXT) "Este vídeo não tem legendas." else "Este vídeo tem só um áudio.", Toast.LENGTH_SHORT).show(); return }
        TrackSelectionDialogBuilder(this, title, p, type)
            .setAllowAdaptiveSelections(false)
            .setShowDisableOption(type == C.TRACK_TYPE_TEXT)
            .build().show()
    }

    private fun hasChannels() = live && channels.size > 1 && apiUrl.isNotEmpty()

    private fun chLabel(i: Int) = "${i + 1}. ${channels.getOrNull(i)?.name ?: ""}"

    private fun pill(label: String, onClick: () -> Unit) = TextView(this).apply {
        text = label
        setTextColor(Color.WHITE); textSize = 15f
        setPadding(dp(16), dp(8), dp(16), dp(8))
        background = GradientDrawable().apply { cornerRadius = dp(20).toFloat(); setColor(0x33FFFFFF) }
        isFocusable = true; isClickable = true
        setOnClickListener { onClick() }
        setOnFocusChangeListener { v, f -> (v.background as GradientDrawable).setColor(if (f) 0xAAFFFFFF.toInt() else 0x33FFFFFF); (v as TextView).setTextColor(if (f) Color.BLACK else Color.WHITE) }
    }

    private fun showOsd(t: String) {
        osd.text = t; osd.visibility = View.VISIBLE
        handler.removeCallbacks(hideOsd); handler.postDelayed(hideOsd, 3500)
    }
    private val hideOsd = Runnable { osd.visibility = View.GONE }

    private fun openList() {
        val lv = listPanel ?: return
        view.hideController()
        (lv.adapter as ArrayAdapter<*>).notifyDataSetChanged()
        lv.visibility = View.VISIBLE
        lv.setSelection(chIdx); lv.setItemChecked(chIdx, true)
        lv.requestFocus()
    }

    private fun closeList() { listPanel?.visibility = View.GONE; view.requestFocus() }

    private fun zap(d: Int) {
        if (!hasChannels()) return
        playChannel((chIdx + d + channels.size) % channels.size)
    }

    /** Pede ao site o endereço do canal (com a sessão do app) e troca o vídeo. */
    private fun playChannel(i: Int) {
        val ch = channels.getOrNull(i) ?: return
        chIdx = i
        val token = ++zapToken
        titleView.text = chLabel(i)
        showOsd(chLabel(i))
        errorText.visibility = View.GONE
        player?.stop()
        io.execute {
            val r = try {
                val c = URL("$apiUrl?action=url&type=live&id=${ch.id}&ext=$liveExt&native=1").openConnection() as HttpURLConnection
                c.connectTimeout = 12000; c.readTimeout = 15000
                CookieManager.getInstance().getCookie(apiUrl)?.let { c.setRequestProperty("Cookie", it) }
                c.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android) VeloxApp/" + BuildConfig.VERSION_NAME)
                val body = (if (c.responseCode in 200..299) c.inputStream else c.errorStream).bufferedReader().use { it.readText() }
                JSONObject(body)
            } catch (e: Exception) { null }
            runOnUiThread {
                if (token != zapToken || isFinishing) return@runOnUiThread
                val u = r?.optString("url").orEmpty()
                if (u.isEmpty()) {
                    errorText.text = (r?.optString("error")?.takeIf { it.isNotEmpty() } ?: "Não foi possível abrir este canal.") + "\n\nUse cima/baixo para trocar de canal."
                    errorText.visibility = View.VISIBLE
                    return@runOnUiThread
                }
                urls.clear(); urlIdx = 0; liveRetries = 0
                urls.add(URL(URL(apiUrl), u).toString())
                r?.optString("url2")?.takeIf { it.isNotEmpty() && it != "null" }?.let { urls.add(URL(URL(apiUrl), it).toString()) }
                load(urls[0], 0L)
            }
        }
    }

    private fun dp(v: Int): Int = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), resources.displayMetrics).toInt()

    private fun hideSystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val c = WindowInsetsControllerCompat(window, window.decorView)
        c.hide(WindowInsetsCompat.Type.systemBars())
        c.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    private fun cycleResize() {
        resizeIdx = (resizeIdx + 1) % RESIZE.size
        view.resizeMode = RESIZE[resizeIdx]
        getSharedPreferences("player", Context.MODE_PRIVATE).edit().putInt("resize", resizeIdx).apply()
        updateAspectLabel()
        Toast.makeText(this, "Tela: " + RESIZE_NAMES[resizeIdx], Toast.LENGTH_SHORT).show()
    }

    private fun updateAspectLabel() { aspectBtn.text = "Tela: " + RESIZE_NAMES[resizeIdx] }

    override fun onStart() {
        super.onStart()
        initPlayer()
    }

    override fun onStop() {
        releasePlayer()
        super.onStop()
    }

    private fun initPlayer() {
        if (player != null || urls.isEmpty()) return
        val http = DefaultHttpDataSource.Factory()
            // nome que aparece no painel do servidor IPTV (Sigma, XUI...): ex. "VeloxPlay/v1.0.17-Exo"
            .setUserAgent(if (desktopUa) UA.DESKTOP else UA.player(this))
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(20000)
        val msf = DefaultMediaSourceFactory(this).setDataSourceFactory(DefaultDataSource.Factory(this, http))
        val renderers = DefaultRenderersFactory(this).setEnableDecoderFallback(true)
            // usa o decodificador do aparelho; se ele não tocar o áudio, entra o FFmpeg
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
        val p = ExoPlayer.Builder(this, renderers)
            .setMediaSourceFactory(msf)
            .setSeekBackIncrementMs(10000)
            .setSeekForwardIncrementMs(10000)
            .build()
        p.setAudioAttributes(AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(), true)
        p.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) = handleError(error)
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) { liveRetries = 0; errorText.visibility = View.GONE }
                if (state == Player.STATE_ENDED && !live) { ended = true; finishWithResult() }
            }
        })
        player = p
        view.player = p
        view.requestFocus()
        load(urls[urlIdx], if (live) 0L else if (lastPos > 0) lastPos else startPos)
    }

    private fun load(url: String, pos: Long) {
        val p = player ?: return
        val b = MediaItem.Builder().setUri(url)
        if (url.contains(".m3u8", ignoreCase = true)) b.setMimeType(MimeTypes.APPLICATION_M3U8)
        if (live) b.setLiveConfiguration(MediaItem.LiveConfiguration.Builder().setTargetOffsetMs(8000).build())
        if (pos > 0) p.setMediaItem(b.build(), pos) else p.setMediaItem(b.build())
        p.prepare()
        p.playWhenReady = true
    }

    private fun handleError(e: PlaybackException) {
        val p = player ?: return
        // servidor recusou (4xx): tenta uma vez com a identificação de navegador de computador
        if (e.errorCode == PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS && !desktopUa) {
            desktopUa = true
            if (!live) lastPos = p.currentPosition
            releasePlayer(); initPlayer(); return
        }
        // ao vivo atrasado demais: volta para o ponto atual da transmissão
        if (e.errorCode == PlaybackException.ERROR_CODE_BEHIND_LIVE_WINDOW) {
            p.seekToDefaultPosition(); p.prepare(); return
        }
        // ao vivo com queda rápida: tenta de novo algumas vezes
        if (live && liveRetries < 3 && e.errorCode in listOf(
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
                PlaybackException.ERROR_CODE_PARSING_CONTAINER_MALFORMED,
                PlaybackException.ERROR_CODE_PARSING_MANIFEST_MALFORMED)) {
            liveRetries++
            handler.postDelayed({ player?.let { it.seekToDefaultPosition(); it.prepare() } }, 1500L * liveRetries)
            return
        }
        // tenta a DNS reserva
        if (urlIdx + 1 < urls.size) {
            urlIdx++
            val pos = if (live) 0L else p.currentPosition.coerceAtLeast(startPos)
            load(urls[urlIdx], pos)
            return
        }
        errorText.text = message(e) + "\n\nAperte voltar para sair."
        errorText.visibility = View.VISIBLE
    }

    private fun message(e: PlaybackException): String = when (e.errorCode) {
        PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ->
            "O servidor recusou o vídeo.\nNormalmente é limite de telas (feche em outro aparelho) ou o conteúdo está fora do ar."
        PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED, PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
            "Sem conexão com o servidor do vídeo.\nConfira a internet e tente de novo."
        PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND -> "Este conteúdo não foi encontrado no servidor."
        PlaybackException.ERROR_CODE_DECODER_INIT_FAILED, PlaybackException.ERROR_CODE_DECODING_FORMAT_UNSUPPORTED,
        PlaybackException.ERROR_CODE_DECODING_FORMAT_EXCEEDS_CAPABILITIES ->
            "Este aparelho não consegue tocar o formato deste vídeo (áudio ou vídeo)."
        PlaybackException.ERROR_CODE_PARSING_CONTAINER_UNSUPPORTED -> "Formato de arquivo não suportado."
        else -> "Não foi possível reproduzir (${e.errorCodeName})."
    }

    override fun onDestroy() { io.shutdownNow(); super.onDestroy() }

    private fun releasePlayer() {
        val p = player ?: return
        lastPos = p.currentPosition
        lastDur = if (p.duration == C.TIME_UNSET) 0L else p.duration
        handler.removeCallbacksAndMessages(null)
        view.player = null
        p.release()
        player = null
    }

    private fun finishWithResult() {
        player?.let {
            lastPos = it.currentPosition
            lastDur = if (it.duration == C.TIME_UNSET) 0L else it.duration
        }
        setResult(RESULT_OK, Intent()
            .putExtra(EXTRA_POS, if (live) 0L else lastPos)
            .putExtra(EXTRA_DUR, if (live) 0L else lastDur)
            .putExtra(EXTRA_ENDED, ended)
            .putExtra(EXTRA_INDEX, chIdx))
        finish()
    }

    // Controle remoto: botão MENU (ou o de "opções") troca o formato da tela.
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN && (event.keyCode == KeyEvent.KEYCODE_MENU || event.keyCode == KeyEvent.KEYCODE_SETTINGS)) {
            cycleResize(); return true
        }
        // ao vivo com lista: cima/baixo e CH+/CH- trocam de canal; OK abre a lista de canais
        if (event.action == KeyEvent.ACTION_DOWN && hasChannels() && listPanel?.visibility != View.VISIBLE) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_CHANNEL_UP -> { zap(1); return true }
                KeyEvent.KEYCODE_CHANNEL_DOWN -> { zap(-1); return true }
            }
            if (!view.isControllerFullyVisible && !top.hasFocus()) when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_UP -> { zap(-1); return true }
                KeyEvent.KEYCODE_DPAD_DOWN -> { zap(1); return true }
                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> { openList(); return true }
            }
        }
        if (listPanel?.visibility == View.VISIBLE) return super.dispatchKeyEvent(event)
        // qualquer tecla com os controles escondidos só mostra os controles
        if (event.action == KeyEvent.ACTION_DOWN && !view.isControllerFullyVisible && event.keyCode != KeyEvent.KEYCODE_BACK
            && event.keyCode != KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE && !top.hasFocus()) {
            view.showController()
            if (event.keyCode == KeyEvent.KEYCODE_DPAD_CENTER || event.keyCode == KeyEvent.KEYCODE_ENTER) return true
        }
        return super.dispatchKeyEvent(event)
    }
}
