package com.velox.rotapaga

import android.content.Context
import android.content.SharedPreferences
import android.location.Location
import org.json.JSONArray
import org.json.JSONObject

/** Estado do cronômetro (tempo, km e trajeto). Fica gravado: sobrevive ao app fechado. */
object TripStore {
    private fun p(ctx: Context): SharedPreferences = ctx.getSharedPreferences("trip", Context.MODE_PRIVATE)
    private var points: JSONArray? = null

    private fun d(sp: SharedPreferences, k: String) = sp.getString(k, null)?.toDoubleOrNull() ?: 0.0
    private fun now() = System.currentTimeMillis()

    fun running(ctx: Context) = p(ctx).getBoolean("running", false)
    fun paused(ctx: Context) = p(ctx).getBoolean("paused", false)

    fun start(ctx: Context, valor: Double, pacotes: Double) {
        points = JSONArray()
        p(ctx).edit().clear()
            .putBoolean("running", true).putBoolean("paused", false)
            .putLong("startedAt", now()).putLong("accMs", 0).putLong("segStart", now())
            .putString("km", "0").putString("valor", valor.toString()).putString("pacotes", pacotes.toString())
            .putString("points", "[]").putString("gps", "wait")
            .apply()
    }

    fun setMeta(ctx: Context, valor: Double, pacotes: Double) {
        p(ctx).edit().putString("valor", valor.toString()).putString("pacotes", pacotes.toString()).apply()
    }

    fun elapsed(ctx: Context): Long {
        val sp = p(ctx)
        if (!sp.getBoolean("running", false)) return 0
        val acc = sp.getLong("accMs", 0)
        return if (sp.getBoolean("paused", false)) acc else acc + (now() - sp.getLong("segStart", now()))
    }

    fun km(ctx: Context) = d(p(ctx), "km")
    fun valor(ctx: Context) = d(p(ctx), "valor")

    fun pause(ctx: Context) {
        if (!running(ctx) || paused(ctx)) return
        // o trecho rodado com a pausa ligada não conta
        p(ctx).edit().putLong("accMs", elapsed(ctx)).putBoolean("paused", true).remove("accLat").remove("accLon").apply()
    }

    fun resume(ctx: Context) {
        if (!running(ctx) || !paused(ctx)) return
        p(ctx).edit().putLong("segStart", now()).putBoolean("paused", false).apply()
    }

    fun stop(ctx: Context) {
        points = null
        p(ctx).edit().clear().apply()
    }

    fun setGps(ctx: Context, s: String) { p(ctx).edit().putString("gps", s).apply() }

    private fun pts(sp: SharedPreferences): JSONArray {
        points?.let { return it }
        val a = try { JSONArray(sp.getString("points", "[]")) } catch (_: Exception) { JSONArray() }
        points = a
        return a
    }

    /** Cada leitura do GPS: ignora imprecisas (> 35 m), tremidas (< 8 m) e saltos impossíveis (> 200 km/h). */
    fun onFix(ctx: Context, loc: Location) {
        val sp = p(ctx)
        if (!sp.getBoolean("running", false)) return
        val t = now()
        val e = sp.edit()
        e.putString("gps", "ok")
        e.putString("last", JSONArray().put(loc.latitude).put(loc.longitude).put(loc.accuracy.toDouble()).put(t).toString())
        val good = !loc.hasAccuracy() || loc.accuracy <= 35f
        if (!sp.getBoolean("paused", false) && good) {
            val la = sp.getString("accLat", null)?.toDoubleOrNull()
            val lo = sp.getString("accLon", null)?.toDoubleOrNull()
            var accept = false
            if (la == null || lo == null) accept = true
            else {
                val r = FloatArray(1)
                Location.distanceBetween(la, lo, loc.latitude, loc.longitude, r)
                val m = r[0]
                val dt = (t - sp.getLong("accT", t)) / 1000.0
                if (m >= 8f && (dt <= 0 || m / dt < 55.0)) {
                    e.putString("km", (d(sp, "km") + m / 1000.0).toString())
                    accept = true
                }
            }
            if (accept) {
                e.putString("accLat", loc.latitude.toString()).putString("accLon", loc.longitude.toString()).putLong("accT", t)
                val a = pts(sp)
                a.put(JSONArray().put(loc.latitude).put(loc.longitude))
                if (a.length() > 3000) {
                    // afina o trajeto antigo pela metade para não crescer sem fim
                    val thin = JSONArray()
                    for (i in 0 until a.length()) if (i % 2 == 0 || i == a.length() - 1) thin.put(a.get(i))
                    points = thin
                }
                e.putString("points", points.toString())
            }
        }
        e.apply()
    }

    fun stateJson(ctx: Context): String {
        val sp = p(ctx)
        val o = JSONObject()
            .put("running", sp.getBoolean("running", false))
            .put("paused", sp.getBoolean("paused", false))
            .put("startedAt", sp.getLong("startedAt", 0))
            .put("elapsedMs", elapsed(ctx))
            .put("km", d(sp, "km"))
            .put("valor", d(sp, "valor"))
            .put("pacotes", d(sp, "pacotes"))
            .put("gps", sp.getString("gps", "wait"))
            .put("points", pts(sp))
        sp.getString("last", null)?.let { try { o.put("last", JSONArray(it)) } catch (_: Exception) { } }
        return o.toString()
    }
}
