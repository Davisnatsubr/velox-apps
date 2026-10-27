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
import android.widget.FrameLayout
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        live = intent.getBooleanExtra(EXTRA_LIVE, false)
        startPos = if (live) 0L else intent.getLongExtra(EXTRA_START, 0L)
        intent.getStringExtra(EXTRA_URL)?.takeIf { it.isNotEmpty() }?.let { urls.add(it) }
        intent.getStringExtra(EXTRA_URL2)?.takeIf { it.isNotEmpty() && it !in urls }?.let { urls.add(it) }
        resizeIdx = getSharedPreferences("player", Context.MODE_PRIVATE).getInt("resize", 0).coerceIn(0, 2)

        val frame = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }
        view = PlayerView(this).apply {
            useController = true
            controllerShowTimeoutMs = 4000
            controllerAutoShow = true
            setShowSubtitleButton(true)
            setShowNextButton(false)
            setShowPreviousButton(false)
            setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
            resizeMode = RESIZE[resizeIdx]
            keepScreenOn = true
            isFocusable = true
        }
        frame.addView(view, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        // barra de cima: título + botão de formato da tela (aparece junto com os controles)
        top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(24), dp(16), dp(24), dp(16))
            background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(0xCC000000.toInt(), 0x00000000))
        }
        val titles = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        titles.addView(TextView(this).apply {
            text = intent.getStringExtra(EXTRA_TITLE) ?: ""
            setTextColor(Color.WHITE); textSize = 20f; maxLines = 1
        })
        val sub = intent.getStringExtra(EXTRA_SUB) ?: ""
        if (sub.isNotEmpty() || live) titles.addView(TextView(this).apply {
            text = if (live) "● AO VIVO" + (if (sub.isNotEmpty()) "  ·  $sub" else "") else sub
            setTextColor(if (live) 0xFFFF4D6D.toInt() else 0xFFB8C0D0.toInt()); textSize = 14f; maxLines = 1
        })
        top.addView(titles, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        aspectBtn = TextView(this).apply {
            setTextColor(Color.WHITE); textSize = 15f
            setPadding(dp(16), dp(8), dp(16), dp(8))
            background = GradientDrawable().apply { cornerRadius = dp(20).toFloat(); setColor(0x33FFFFFF) }
            isFocusable = true
            isClickable = true
            setOnClickListener { cycleResize() }
            setOnFocusChangeListener { v, f -> (v.background as GradientDrawable).setColor(if (f) 0xAAFFFFFF.toInt() else 0x33FFFFFF) ; (v as TextView).setTextColor(if (f) Color.BLACK else Color.WHITE) }
        }
        top.addView(aspectBtn)
        updateAspectLabel()
        frame.addView(top, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.TOP))

        errorText = TextView(this).apply {
            setTextColor(Color.WHITE); textSize = 18f; gravity = Gravity.CENTER
            setPadding(dp(40), dp(24), dp(40), dp(24))
            background = GradientDrawable().apply { cornerRadius = dp(16).toFloat(); setColor(0xDD141C30.toInt()) }
            visibility = View.GONE
        }
        frame.addView(errorText, FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER))

        view.setControllerVisibilityListener(PlayerView.ControllerVisibilityListener { v -> top.visibility = v })
        setContentView(frame)
        hideSystemBars()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = finishWithResult()
        })
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
            .setUserAgent("Mozilla/5.0 (Linux; Android) VeloxApp/" + BuildConfig.VERSION_NAME)
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(20000)
        val msf = DefaultMediaSourceFactory(this).setDataSourceFactory(DefaultDataSource.Factory(this, http))
        val renderers = DefaultRenderersFactory(this).setEnableDecoderFallback(true)
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
            .putExtra(EXTRA_ENDED, ended))
        finish()
    }

    // Controle remoto: botão MENU (ou o de "opções") troca o formato da tela.
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN && (event.keyCode == KeyEvent.KEYCODE_MENU || event.keyCode == KeyEvent.KEYCODE_SETTINGS)) {
            cycleResize(); return true
        }
        // qualquer tecla com os controles escondidos só mostra os controles
        if (event.action == KeyEvent.ACTION_DOWN && !view.isControllerFullyVisible && event.keyCode != KeyEvent.KEYCODE_BACK
            && event.keyCode != KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE && !aspectBtn.isFocused) {
            view.showController()
            if (event.keyCode == KeyEvent.KEYCODE_DPAD_CENTER || event.keyCode == KeyEvent.KEYCODE_ENTER) return true
        }
        return super.dispatchKeyEvent(event)
    }
}
