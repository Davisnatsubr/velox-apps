package com.velox.rotapaga

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat

/** Conta os km pelo GPS com a tela apagada, enquanto o cronômetro está ligado. */
class TripService : Service(), LocationListener {

    companion object {
        private const val NID = 4202
        fun start(ctx: Context) {
            try { ContextCompat.startForegroundService(ctx, Intent(ctx, TripService::class.java)) } catch (_: Exception) { }
        }
        fun stop(ctx: Context) { ctx.stopService(Intent(ctx, TripService::class.java)) }
    }

    private var lm: LocationManager? = null
    private var listening = false
    private val h = Handler(Looper.getMainLooper())
    private val tick = object : Runnable {
        override fun run() {
            if (!TripStore.running(this@TripService)) { stopSelf(); return }
            notifyNow(); h.postDelayed(this, 30_000L)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Alert.channels(this)
        try {
            ServiceCompat.startForeground(this, NID, build(),
                if (Build.VERSION.SDK_INT >= 29) ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION else 0)
        } catch (_: Exception) {
            stopSelf(); return START_NOT_STICKY
        }
        if (!TripStore.running(this)) { stopSelf(); return START_NOT_STICKY }
        if (!listening) {
            val m = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            lm = m
            try {
                m.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000L, 5f, this, Looper.getMainLooper())
                listening = true
                if (!m.isProviderEnabled(LocationManager.GPS_PROVIDER)) TripStore.setGps(this, "off")
            } catch (_: SecurityException) { TripStore.setGps(this, "off") } catch (_: Exception) { TripStore.setGps(this, "off") }
        }
        h.removeCallbacks(tick); h.post(tick)
        return START_STICKY
    }

    override fun onDestroy() {
        h.removeCallbacks(tick)
        try { lm?.removeUpdates(this) } catch (_: Exception) { }
        listening = false
        super.onDestroy()
    }

    private var lastNotify = 0L
    override fun onLocationChanged(location: Location) {
        TripStore.onFix(this, location)
        val t = System.currentTimeMillis()
        if (t - lastNotify > 15_000L) { lastNotify = t; notifyNow() }
    }

    override fun onProviderEnabled(provider: String) { TripStore.setGps(this, "wait") }
    override fun onProviderDisabled(provider: String) { TripStore.setGps(this, "off") }
    @Deprecated("Exigido em Android 8 a 10")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) { }

    private fun build(): android.app.Notification {
        val ms = TripStore.elapsed(this)
        val km = TripStore.km(this)
        val v = TripStore.valor(this)
        val horas = ms / 3_600_000.0
        val min = ms / 60_000
        var txt = "${min / 60}h${(min % 60).toString().padStart(2, '0')} · ${Calc.dec(km, 1)} km"
        if (v > 0 && horas > 0.02) txt += " · ${Calc.brl(v / horas)}/h"
        if (TripStore.paused(this)) txt = "Pausado · $txt"
        val pi = PendingIntent.getActivity(this, 3, Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        return NotificationCompat.Builder(this, Alert.CH_TRIP)
            .setSmallIcon(R.drawable.ic_stat_route)
            .setContentTitle("Rota em andamento")
            .setContentText(txt)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pi)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    private fun notifyNow() {
        try {
            val nm = NotificationManagerCompat.from(this)
            if (nm.areNotificationsEnabled()) nm.notify(NID, build())
        } catch (_: SecurityException) { }
    }
}
