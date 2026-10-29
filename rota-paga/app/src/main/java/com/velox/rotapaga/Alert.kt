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
        val n = NotificationCompat.Builder(ctx, CH)
            .setSmallIcon(R.drawable.ic_stat_route)
            .setContentTitle(title(o, c))
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
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
