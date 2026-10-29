package com.velox.rotapaga

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject

/** Configuração do alerta (vem do app.js) e memória curta das notificações. */
object Store {
    fun p(ctx: Context): SharedPreferences = ctx.getSharedPreferences("rotapaga", Context.MODE_PRIVATE)

    private const val DEF_CFG =
        """{"enabled":true,"keywords":["rota","envio extra","envios extra","nova oferta","pacotes"],"apps":[],"seconds":10,
           "ruimPct":2.2,"metaPct":2.8,"ruimKm":1.4,"metaKm":1.8,"ruimHora":25,"metaHora":35,"gas":6.29,"cons":11,"overlay":true,"bubble":false}"""

    fun cfg(ctx: Context): JSONObject {
        val s = p(ctx).getString("alert_cfg", null) ?: DEF_CFG
        return try { JSONObject(s) } catch (e: Exception) { JSONObject(DEF_CFG) }
    }

    fun setCfg(ctx: Context, json: String) {
        try { JSONObject(json); p(ctx).edit().putString("alert_cfg", json).apply() } catch (_: Exception) { }
    }

    fun keywords(c: JSONObject): List<String> {
        val a = c.optJSONArray("keywords") ?: return emptyList()
        return (0 until a.length()).map { Calc.norm(a.optString(it)) }.filter { it.isNotBlank() }
    }

    fun apps(c: JSONObject): Set<String> {
        val a = c.optJSONArray("apps") ?: return emptySet()
        return (0 until a.length()).map { a.optString(it) }.filter { it.isNotBlank() }.toSet()
    }

    /** Apps que mandaram notificação recentemente (para o motorista escolher quais vigiar). */
    fun seen(ctx: Context): JSONArray = try { JSONArray(p(ctx).getString("seen", "[]")) } catch (e: Exception) { JSONArray() }

    fun recordSeen(ctx: Context, pkg: String, label: String) {
        val old = seen(ctx)
        val out = JSONArray().put(JSONObject().put("pkg", pkg).put("label", label))
        for (i in 0 until old.length()) {
            val o = old.optJSONObject(i) ?: continue
            if (o.optString("pkg") != pkg && out.length() < 25) out.put(o)
        }
        p(ctx).edit().putString("seen", out.toString()).apply()
    }

    fun setPending(ctx: Context, text: String, app: String) {
        p(ctx).edit().putString("pending", JSONObject().put("text", text).put("app", app).put("ts", System.currentTimeMillis()).toString()).apply()
    }

    /** Devolve a última rota detectada (até 30 min atrás) e apaga. */
    fun takePending(ctx: Context): String? {
        val s = p(ctx).getString("pending", null) ?: return null
        p(ctx).edit().remove("pending").apply()
        return try {
            val o = JSONObject(s)
            if (System.currentTimeMillis() - o.optLong("ts") > 30 * 60_000L) null else s
        } catch (e: Exception) { null }
    }

    /** Últimas notificações lidas e o que o app fez com cada uma (aparece em Ajustes, para entender por que não disparou). */
    fun log(ctx: Context, app: String, text: String, res: String) {
        val old = logs(ctx)
        val out = JSONArray().put(JSONObject().put("ts", System.currentTimeMillis()).put("app", app).put("text", text.take(220)).put("res", res))
        for (i in 0 until minOf(old.length(), 19)) out.put(old.opt(i))
        p(ctx).edit().putString("nlog", out.toString()).apply()
    }

    fun logs(ctx: Context): JSONArray = try { JSONArray(p(ctx).getString("nlog", "[]")) } catch (e: Exception) { JSONArray() }

    /** Resultado da última tentativa de mostrar o cartão flutuante. */
    fun setCardRes(ctx: Context, s: String) {
        p(ctx).edit().putString("card_res", s).putLong("card_ts", System.currentTimeMillis()).apply()
    }

    /** A mesma notificação costuma chegar várias vezes; ignora repetições por 90 s. */
    fun isDuplicate(ctx: Context, text: String): Boolean {
        val sp = p(ctx)
        val h = text.hashCode()
        val now = System.currentTimeMillis()
        val dup = sp.getInt("last_hash", 0) == h && now - sp.getLong("last_ts", 0) < 90_000L
        sp.edit().putInt("last_hash", h).putLong("last_ts", now).apply()
        return dup
    }
}
