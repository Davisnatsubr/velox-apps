package com.velox.rotapaga

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import org.json.JSONObject
import java.util.Calendar

/**
 * Lembretes das rotas agendadas (noite anterior e antes da saída) e o aviso diário "hora das rotas".
 * O app.js manda a lista pronta; aqui só se agenda no relógio do Android (e de novo depois de reiniciar).
 */
object Reminders {
    private const val CH = "lembretes"
    private fun p(ctx: Context) = Store.p(ctx)
    private fun am(ctx: Context) = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun canExact(ctx: Context): Boolean = Build.VERSION.SDK_INT < 31 || am(ctx).canScheduleExactAlarms()

    /** json = {"lembretes":[{"id","quando"(ms),"titulo","texto","agenda"}], "diario":{"on":bool,"h":10,"m":55}} */
    fun set(ctx0: Context, json: String) {
        val ctx = ctx0.applicationContext
        val old = try { JSONObject(p(ctx).getString("lembretes", "{}")!!) } catch (_: Exception) { JSONObject() }
        // cancela os antigos antes de agendar os novos
        old.optJSONArray("lembretes")?.let { a -> for (i in 0 until a.length()) cancel(ctx, a.optJSONObject(i)?.optString("id") ?: continue) }
        cancel(ctx, "diario")
        p(ctx).edit().putString("lembretes", json).apply()
        schedule(ctx)
    }

    fun schedule(ctx0: Context) {
        val ctx = ctx0.applicationContext
        val o = try { JSONObject(p(ctx).getString("lembretes", "{}")!!) } catch (_: Exception) { return }
        val now = System.currentTimeMillis()
        o.optJSONArray("lembretes")?.let { a ->
            for (i in 0 until a.length()) {
                val r = a.optJSONObject(i) ?: continue
                val t = r.optLong("quando")
                if (t > now) at(ctx, r.optString("id"), t, r)
            }
        }
        val d = o.optJSONObject("diario")
        if (d != null && d.optBoolean("on")) {
            val c = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, d.optInt("h", 10)); set(Calendar.MINUTE, d.optInt("m", 55)); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                if (timeInMillis <= now + 5000) add(Calendar.DAY_OF_MONTH, 1)
            }
            at(ctx, "diario", c.timeInMillis, JSONObject().put("id", "diario").put("titulo", "Hora das rotas")
                .put("texto", d.optString("texto", "As rotas do Envios Extra costumam aparecer agora. Deixe o celular por perto.")))
        }
    }

    private fun pi(ctx: Context, id: String, r: JSONObject?): PendingIntent {
        val i = Intent(ctx, ReminderReceiver::class.java).setAction("rotapaga.lembrete.$id")
        if (r != null) i.putExtra("r", r.toString())
        return PendingIntent.getBroadcast(ctx, id.hashCode(), i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun cancel(ctx: Context, id: String) { try { am(ctx).cancel(pi(ctx, id, null)) } catch (_: Exception) { } }

    private fun at(ctx: Context, id: String, t: Long, r: JSONObject) {
        val m = am(ctx)
        val p = pi(ctx, id, r)
        try {
            if (canExact(ctx)) m.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t, p)
            else m.setWindow(AlarmManager.RTC_WAKEUP, t, 5 * 60_000L, p) // até 5 min de atraso sem a permissão de horário exato
        } catch (_: SecurityException) {
            m.setWindow(AlarmManager.RTC_WAKEUP, t, 5 * 60_000L, p)
        }
    }

    fun show(ctx: Context, r: JSONObject) {
        if (Build.VERSION.SDK_INT >= 26) {
            ctx.getSystemService(NotificationManager::class.java)?.createNotificationChannel(
                NotificationChannel(CH, "Lembretes de rota", NotificationManager.IMPORTANCE_HIGH).apply { description = "Rota agendada e hora das rotas" })
        }
        val agenda = r.optString("agenda", "")
        val open = Intent(ctx, MainActivity::class.java).apply {
            if (agenda.isNotEmpty()) putExtra(MainActivity.EXTRA_AGENDA, agenda)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pi = PendingIntent.getActivity(ctx, ("abrir" + r.optString("id")).hashCode(), open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val b = NotificationCompat.Builder(ctx, CH)
            .setSmallIcon(R.drawable.ic_stat_route)
            .setContentTitle(r.optString("titulo", "Rota Paga"))
            .setContentText(r.optString("texto"))
            .setStyle(NotificationCompat.BigTextStyle().bigText(r.optString("texto")))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pi)
        if (agenda.isNotEmpty()) b.addAction(0, "Abrir a rota", pi)
        try {
            val nm = NotificationManagerCompat.from(ctx)
            if (nm.areNotificationsEnabled()) nm.notify(r.optString("id").hashCode(), b.build())
        } catch (_: SecurityException) { }
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val r = try { JSONObject(intent.getStringExtra("r") ?: return) } catch (_: Exception) { return }
        Reminders.show(context, r)
        if (r.optString("id") == "diario") Reminders.schedule(context) // agenda o de amanhã
    }
}

/** Depois de reiniciar o celular (ou atualizar o app) o Android apaga os alarmes: agenda de novo. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Reminders.schedule(context)
        if (TripStore.running(context)) TripService.start(context)
    }
}

