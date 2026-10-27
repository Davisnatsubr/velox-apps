package com.veloxplay.app

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlin.concurrent.thread

/**
 * Atualização da versão por APK (Downloader/TV Box): baixa o APK novo informado no painel
 * e abre o instalador do Android por cima do app atual. O progresso vai para o site (window.__appUpdateProgress).
 */
class Updater(private val act: Activity, private val js: (String) -> Unit) {

    private var pending: File? = null
    @Volatile private var busy = false

    fun start(url: String) {
        if (busy) return
        busy = true
        thread(name = "atualizacao") {
            try {
                val dir = File(act.cacheDir, "apk").apply { mkdirs() }
                val out = File(dir, "update.apk")
                var u = URL(url)
                var conn: HttpURLConnection
                var hops = 0
                while (true) {
                    conn = (u.openConnection() as HttpURLConnection).apply {
                        instanceFollowRedirects = true
                        connectTimeout = 15000
                        readTimeout = 30000
                        setRequestProperty("User-Agent", "VeloxApp/" + BuildConfig.VERSION_NAME)
                    }
                    val code = conn.responseCode
                    if (code in 300..399 && hops < 5) { // redireciona também de http para https
                        u = URL(u, conn.getHeaderField("Location"))
                        conn.disconnect()
                        hops++
                        continue
                    }
                    if (code !in 200..299) throw IOException("o servidor respondeu $code")
                    break
                }
                val total = conn.contentLength.toLong()
                conn.inputStream.use { inp ->
                    out.outputStream().use { o ->
                        val buf = ByteArray(64 * 1024)
                        var done = 0L
                        var last = -1
                        while (true) {
                            val n = inp.read(buf)
                            if (n <= 0) break
                            o.write(buf, 0, n)
                            done += n
                            if (total > 0) {
                                val p = (done * 100 / total).toInt().coerceAtMost(99)
                                if (p != last) { last = p; js("window.__appUpdateProgress && window.__appUpdateProgress($p)") }
                            }
                        }
                    }
                }
                conn.disconnect()
                js("window.__appUpdateProgress && window.__appUpdateProgress(100)")
                act.runOnUiThread { pending = out; install() }
            } catch (e: Exception) {
                val msg = JSONObject.quote("Não foi possível baixar a atualização (" + (e.message ?: "erro") + "). Confira a internet e tente de novo.")
                js("window.__appUpdateError && window.__appUpdateError($msg)")
            } finally {
                busy = false
            }
        }
    }

    /** Volta das configurações de "instalar apps desconhecidos": continua a instalação. */
    fun resumeIfPending() {
        if (pending != null && canInstall()) install()
    }

    private fun canInstall(): Boolean = Build.VERSION.SDK_INT < 26 || act.packageManager.canRequestPackageInstalls()

    private fun install() {
        val f = pending ?: return
        if (!canInstall()) {
            Toast.makeText(act, "Permita instalar apps deste app e volte para continuar", Toast.LENGTH_LONG).show()
            try {
                act.startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:" + act.packageName)))
            } catch (e: ActivityNotFoundException) {
                try { act.startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS)) } catch (_: Exception) { }
            }
            return
        }
        pending = null
        val uri = FileProvider.getUriForFile(act, act.packageName + ".files", f)
        val i = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, "application/vnd.android.package-archive")
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            act.startActivity(i)
        } catch (e: Exception) {
            js("window.__appUpdateError && window.__appUpdateError(" + JSONObject.quote("Este aparelho não abriu o instalador. Baixe o APK novo pelo Downloader.") + ")")
        }
    }
}
