package com.velox.rotapaga

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.webkit.JavascriptInterface
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File

/** Funções do Android chamadas pelo app.js (window.RotaApp). Rodam fora da thread da tela. */
class Bridge(private val a: MainActivity) {

    @JavascriptInterface fun version(): String = BuildConfig.VERSION_NAME

    @JavascriptInterface fun setBars(bg: String, dark: Boolean) { a.runOnUiThread { a.setBars(bg, dark) } }

    @JavascriptInterface fun openUrl(url: String): String {
        a.runOnUiThread { a.openExternal(Uri.parse(url)) }
        return "ok"
    }

    /** Salva um arquivo (planilha, backup) e abre o "compartilhar" para mandar ao Drive, WhatsApp etc. */
    @JavascriptInterface fun shareFile(name: String, mime: String, content: String): String {
        return try {
            val dir = File(a.cacheDir, "share").apply { mkdirs() }
            val f = File(dir, name.replace(Regex("[^\\w.-]"), "_"))
            f.writeText(content)
            val uri = FileProvider.getUriForFile(a, a.packageName + ".files", f)
            val send = Intent(Intent.ACTION_SEND).apply {
                type = mime
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, name)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            a.runOnUiThread { a.startActivity(Intent.createChooser(send, "Salvar ou enviar")) }
            "ok"
        } catch (_: Exception) { "erro" }
    }

    /* ---------- alerta de rota ---------- */

    @JavascriptInterface fun alertStatus(): String = JSONObject()
        .put("access", NotificationManagerCompat.getEnabledListenerPackages(a).contains(a.packageName))
        .put("overlay", Settings.canDrawOverlays(a))
        .put("notif", NotificationManagerCompat.from(a).areNotificationsEnabled())
        .put("battery", (a.getSystemService(android.content.Context.POWER_SERVICE) as android.os.PowerManager).isIgnoringBatteryOptimizations(a.packageName))
        .put("seen", Store.seen(a))
        .put("log", Store.logs(a))
        .put("cardRes", Store.p(a).getString("card_res", ""))
        .put("cardTs", Store.p(a).getLong("card_ts", 0))
        .put("fabricante", android.os.Build.MANUFACTURER ?: "")
        .put("modelo", android.os.Build.MODEL ?: "")
        .put("android", android.os.Build.VERSION.RELEASE ?: "")
        .toString()

    /** Mostra o cartão flutuante com números de exemplo, sem tocar o alarme. */
    @JavascriptInterface fun testCard(): String {
        if (!Settings.canDrawOverlays(a)) { Store.setCardRes(a, "sem permissão de aparecer sobre outros apps"); return "permissao" }
        Overlay.showCard(a, Calc.parse("R$ 150,00 · 48 pacotes · 32 km · 20 paradas · 240 min"), "Teste")
        return "ok"
    }

    @JavascriptInterface fun openAppSettings(): String {
        a.runOnUiThread {
            try { a.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + a.packageName))) } catch (_: Exception) { }
        }
        return "ok"
    }

    /** Xiaomi/Redmi/Poco: tela de "Outras permissões" (janelas pop-up em segundo plano). */
    @JavascriptInterface fun openMiuiPerms(): String {
        a.runOnUiThread {
            val i = Intent("miui.intent.action.APP_PERM_EDITOR").apply {
                setClassName("com.miui.securitycenter", "com.miui.permcenter.permissions.PermissionsEditorActivity")
                putExtra("extra_pkgname", a.packageName)
            }
            try { a.startActivity(i) } catch (_: Exception) {
                try { a.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + a.packageName))) } catch (_: Exception) { }
            }
        }
        return "ok"
    }

    @JavascriptInterface fun openBattery(): String {
        a.runOnUiThread {
            try { a.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) } catch (_: Exception) {
                try { a.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + a.packageName))) } catch (_: Exception) { }
            }
        }
        return "ok"
    }

    @JavascriptInterface fun clearLog(): String { Store.p(a).edit().remove("nlog").apply(); return "ok" }

    @JavascriptInterface fun openAlertAccess(): String {
        a.runOnUiThread {
            a.askNotif()
            try { a.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) } catch (_: Exception) { }
        }
        return "ok"
    }

    @JavascriptInterface fun openOverlayPermission(): String {
        a.runOnUiThread {
            try { a.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + a.packageName))) } catch (_: Exception) { }
        }
        return "ok"
    }

    @JavascriptInterface fun setAlertConfig(json: String): String {
        Store.setCfg(a, json)
        Overlay.syncBubble(a)
        return "ok"
    }

    @JavascriptInterface fun testAlert(): String {
        a.runOnUiThread {
            a.askNotif()
            Alert.fire(a, "Teste do Rota Paga · Envios Extra: nova rota disponível para amanhã · R$ 150,00 · 48 pacotes · 32 km · 20 paradas", "Teste")
        }
        return "ok"
    }

    @JavascriptInterface fun stopAlarm(): String { a.runOnUiThread { Alert.stop(a); Overlay.hideCard() }; return "ok" }

    @JavascriptInterface fun takePendingRoute(): String = Store.takePending(a) ?: ""

    /* ---------- cronômetro ---------- */

    @JavascriptInterface fun tripStart(json: String): String {
        val j = try { JSONObject(json) } catch (_: Exception) { JSONObject() }
        TripStore.start(a, j.optDouble("valor", 0.0), j.optDouble("pacotes", 0.0))
        val ok = a.hasLoc()
        a.runOnUiThread { a.askNotif(); a.startTrip() }
        return if (ok) "ok" else "permission"
    }

    @JavascriptInterface fun tripPause(): String { TripStore.pause(a); return "ok" }
    @JavascriptInterface fun tripResume(): String { TripStore.resume(a); return "ok" }

    @JavascriptInterface fun tripSetMeta(json: String): String {
        val j = try { JSONObject(json) } catch (_: Exception) { JSONObject() }
        TripStore.setMeta(a, j.optDouble("valor", 0.0), j.optDouble("pacotes", 0.0))
        return "ok"
    }

    @JavascriptInterface fun tripStop(): String {
        val s = TripStore.stateJson(a)
        TripStore.stop(a)
        a.runOnUiThread { TripService.stop(a) }
        return s
    }

    @JavascriptInterface fun tripState(): String = TripStore.stateJson(a)
}
