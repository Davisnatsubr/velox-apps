package com.velox.rotapaga

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.graphics.Color
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

/** Rota detectada: notificação com o cálculo, alarme alto (toca no silencioso) e cartão por cima do app. */
object Alert {
    const val CH = "rota_alerta"
    const val CH_TRIP = "cronometro"
    private const val NID = 4201
    private var player: MediaPlayer? = null
    private var savedVol = -1
    private val h = Handler(Looper.getMainLooper())

    fun channels(ctx: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val nm = ctx.getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(NotificationChannel(CH, "Rota detectada", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Aviso quando chega uma rota nova no app de entregas"
            setSound(null, null) // o som é o alarme tocado pelo próprio app
            enableVibration(false)
        })
        nm.createNotificationChannel(NotificationChannel(CH_TRIP, "Cronômetro da rota", NotificationManager.IMPORTANCE_LOW).apply {
            description = "Tempo e km da rota em andamento"
            setShowBadge(false)
        })
    }

    fun title(o: Oferta, c: org.json.JSONObject): String {
        val v = o.valor
        val p = o.pacotes
        if (v != null && p != null && p > 0) {
            val porPct = v / p
            val g = Calc.grade(porPct, c.optDouble("ruimPct", 0.0), c.optDouble("metaPct", 0.0))
            val sinal = when (g) { 1 -> "🟢"; 2 -> "🟡"; 3 -> "🔴"; else -> "📦" }
            return "$sinal ${Calc.brl(porPct)}/pacote · ${Calc.brl(v)} · $p pct"
        }
        if (v != null) return "📦 Rota de ${Calc.brl(v)}"
        return "📦 Rota nova"
    }

    fun fire(ctx0: Context, text: String, app: String) {
        val ctx = ctx0.applicationContext
        val c = Store.cfg(ctx)
        val o = Calc.parse(text)
        Store.setPending(ctx, text, app)
        channels(ctx)
        val open = Intent(ctx, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_ROUTE, true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pi = PendingIntent.getActivity(ctx, 1, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val stopPi = PendingIntent.getBroadcast(ctx, 2, Intent(ctx, AlarmStopReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val r = Overlay.compute(ctx, o)
        val b = NotificationCompat.Builder(ctx, CH)
            .setSmallIcon(R.drawable.ic_stat_route)
            .setContentTitle(title(o, c))
            .setContentText(text)
        if (r.cells.isNotEmpty()) {
            // cartão completo ao puxar a notificação, como o cartão flutuante
            b.setStyle(NotificationCompat.DecoratedCustomViewStyle())
                .setCustomContentView(cardViews(ctx, R.layout.notif_card_small, r, text, app))
                .setCustomBigContentView(cardViews(ctx, R.layout.notif_card_big, r, text, app))
                .setCustomHeadsUpContentView(cardViews(ctx, R.layout.notif_card_small, r, text, app))
        } else b.setStyle(NotificationCompat.BigTextStyle().bigText(text))
        val n = b
            .setSubText(app)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pi)
            .setDeleteIntent(stopPi)
            .addAction(0, "Parar alarme", stopPi)
            .addAction(0, "Ver cálculo", pi)
            .build()
        try {
            val nmc = NotificationManagerCompat.from(ctx)
            if (nmc.areNotificationsEnabled()) nmc.notify(NID, n)
        } catch (_: SecurityException) { }
        if (c.optBoolean("overlay", true)) Overlay.showCard(ctx, o, app)
        play(ctx, c.optInt("seconds", 10).coerceIn(3, 60))
        MainActivity.notifyRoute()
    }

    private val COR = mapOf(0 to "#8A9A93", 1 to "#2E9E5B", 2 to "#D99A0B", 3 to "#E0452F")

    private fun cardViews(ctx: Context, layout: Int, r: Overlay.Res, text: String, app: String): RemoteViews {
        val v = RemoteViews(ctx.packageName, layout)
        v.setTextViewText(R.id.linha, if (r.line.isNotEmpty()) "$app · ${r.line}" else app)
        val ids = listOf(
            arrayOf(R.id.c0, R.id.l0, R.id.b0, R.id.v0), arrayOf(R.id.c1, R.id.l1, R.id.b1, R.id.v1),
            arrayOf(R.id.c2, R.id.l2, R.id.b2, R.id.v2), arrayOf(R.id.c3, R.id.l3, R.id.b3, R.id.v3),
        )
        ids.forEachIndexed { i, id ->
            val cell = r.cells.getOrNull(i)
            if (cell == null) { v.setViewVisibility(id[0], android.view.View.GONE); return@forEachIndexed }
            v.setViewVisibility(id[0], android.view.View.VISIBLE)
            v.setTextViewText(id[1], cell.first)
            v.setTextViewText(id[3], cell.second)
            v.setInt(id[2], "setBackgroundColor", Color.parseColor(COR[cell.third] ?: COR[0]!!))
        }
        if (layout == R.layout.notif_card_big) {
            val ver = when (r.verdict) { 1 -> "Compensa"; 2 -> "No limite"; 3 -> "Abaixo do seu mínimo"; else -> "" }
            v.setTextViewText(R.id.veredito, ver)
            v.setTextColor(R.id.veredito, Color.parseColor(COR[r.verdict] ?: COR[0]!!))
            v.setTextViewText(R.id.texto, text)
        }
        return v
    }

    private fun play(ctx: Context, secs: Int) {
        stop(ctx)
        val am = ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        try {
            savedVol = am.getStreamVolume(AudioManager.STREAM_ALARM)
            am.setStreamVolume(AudioManager.STREAM_ALARM, am.getStreamMaxVolume(AudioManager.STREAM_ALARM), 0)
        } catch (_: Exception) { savedVol = -1 }
        val uri: Uri = RingtoneManager.getActualDefaultRingtoneUri(ctx, RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ?: Settings.System.DEFAULT_RINGTONE_URI
        player = try {
            MediaPlayer().apply {
                setAudioAttributes(AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build())
                setDataSource(ctx, uri)
                isLooping = true
                prepare()
                start()
            }
        } catch (_: Exception) { null }
        vibrator(ctx)?.let { v ->
            try { v.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 700, 300, 700, 300, 1000), -1)) } catch (_: Exception) { }
        }
        h.postDelayed({ stop(ctx) }, secs * 1000L)
    }

    fun stop(ctx0: Context) {
        val ctx = ctx0.applicationContext
        h.removeCallbacksAndMessages(null)
        player?.let { p -> try { p.stop() } catch (_: Exception) { }; p.release() }
        player = null
        if (savedVol >= 0) {
            try { (ctx.getSystemService(Context.AUDIO_SERVICE) as AudioManager).setStreamVolume(AudioManager.STREAM_ALARM, savedVol, 0) } catch (_: Exception) { }
            savedVol = -1
        }
        try { vibrator(ctx)?.cancel() } catch (_: Exception) { }
    }

    private fun vibrator(ctx: Context): Vibrator? =
        if (Build.VERSION.SDK_INT >= 31) (ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        else @Suppress("DEPRECATION") (ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)
}

/** Botão "Parar alarme" da notificação (e quando ela é arrastada para o lado). */
class AlarmStopReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Alert.stop(context)
    }
}
