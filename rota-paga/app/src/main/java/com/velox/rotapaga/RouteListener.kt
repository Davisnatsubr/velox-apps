package com.velox.rotapaga

import android.app.Notification
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * Lê as notificações (com a permissão "Acesso a notificações") e dispara o alerta
 * quando chega uma rota no app de entregas. Nada sai do celular.
 */
class RouteListener : NotificationListenerService() {

    override fun onListenerConnected() {
        Overlay.syncBubble(this)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return
        try { handle(sbn) } catch (_: Exception) { }
    }

    private fun handle(sbn: StatusBarNotification) {
        val pkg = sbn.packageName ?: return
        if (pkg == packageName || pkg == "android" || pkg == "com.android.systemui") return
        val n = sbn.notification ?: return
        if (n.flags and Notification.FLAG_GROUP_SUMMARY != 0) return
        val ex = n.extras ?: return
        val title = ex.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
        val text = ex.getCharSequence(Notification.EXTRA_TEXT)?.toString().orEmpty()
        val big = ex.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString().orEmpty()
        val lines = ex.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)?.joinToString(" · ").orEmpty()
        val body = if (big.length > text.length) big else text
        val full = listOf(title, body, lines).filter { it.isNotBlank() }.joinToString(" · ")
        if (full.isBlank()) return

        val label = appLabel(pkg)
        if (!sbn.isOngoing) Store.recordSeen(this, pkg, label)

        val c = Store.cfg(this)
        val words = Store.keywords(c)
        val t = Calc.norm(full)
        val hit = words.isNotEmpty() && words.any { t.contains(it) }
        // notificações fixas (música, GPS...) só entram no registro se tiverem palavra-chave
        val log = { res: String -> if (!sbn.isOngoing || hit) Store.log(this, label, full, res) }
        if (!c.optBoolean("enabled", true)) { log("alerta desligado"); return }
        val apps = Store.apps(c)
        if (apps.isNotEmpty() && pkg !in apps) { log("app não marcado em \"Só destes apps\""); return }
        if (!hit) { log("sem palavra-chave"); return }
        if (Store.isDuplicate(this, full)) { log("repetida, ignorada"); return }
        log("ALERTA disparado")
        Alert.fire(this, full, label)
    }

    private fun appLabel(pkg: String): String = try {
        val pm = packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
    } catch (_: PackageManager.NameNotFoundException) { pkg } catch (_: Exception) { pkg }
}
