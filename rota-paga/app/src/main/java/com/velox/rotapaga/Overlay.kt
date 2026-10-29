package com.velox.rotapaga

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.util.TypedValue
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.abs

/**
 * Janelas por cima de outros apps (permissão "Aparecer sobre outros apps"):
 * - cartão com o cálculo da rota, quando a notificação chega;
 * - bolha de cálculo rápido, para digitar os números da oferta sem sair do app de entregas.
 */
object Overlay {
    private val h = Handler(Looper.getMainLooper())
    private var card: View? = null
    private var bubble: View? = null
    private var panel: View? = null
    private val hideCardRun = Runnable { card?.let { remove(it.context, it) }; card = null }

    private const val BG = "#111816"
    private const val INK = "#F2FFF8"
    private const val MUTED = "#93A39B"
    private const val ACC = "#0A6B4B"
    private val COR = mapOf(0 to "#5B6A63", 1 to "#2ECC71", 2 to "#F5B81C", 3 to "#FF5A47")

    fun can(ctx: Context) = Settings.canDrawOverlays(ctx)
    private fun wm(ctx: Context) = ctx.applicationContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private fun dp(ctx: Context, v: Int) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), ctx.resources.displayMetrics).toInt()
    private fun remove(ctx: Context, v: View) { try { wm(ctx).removeView(v) } catch (_: Exception) { } }

    private fun lp(w: Int, focusable: Boolean) = WindowManager.LayoutParams(
        w, WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        (if (focusable) WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL else WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE) or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
        PixelFormat.TRANSLUCENT,
    )

    private fun round(color: String, radius: Int, stroke: String? = null, sw: Int = 0) = GradientDrawable().apply {
        setColor(Color.parseColor(color)); cornerRadius = radius.toFloat()
        if (stroke != null) setStroke(sw, Color.parseColor(stroke))
    }

    private fun tv(ctx: Context, s: String, sp: Float, color: String, bold: Boolean = false) = TextView(ctx).apply {
        text = s; setTextSize(TypedValue.COMPLEX_UNIT_SP, sp); setTextColor(Color.parseColor(color))
        if (bold) typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        includeFontPadding = false
    }

    /** Uma métrica com a barrinha colorida do semáforo, como "R$/pct | 3,13". */
    private fun cell(ctx: Context, label: String, value: String, grade: Int): View {
        val box = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(ctx, 4), 0, dp(ctx, 4), 0) }
        box.addView(tv(ctx, label, 11f, MUTED))
        val row = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        row.addView(View(ctx).apply { background = round(COR[grade] ?: COR[0]!!, dp(ctx, 2)) }, LinearLayout.LayoutParams(dp(ctx, 4), dp(ctx, 24)).apply { marginEnd = dp(ctx, 6) })
        row.addView(tv(ctx, value, 24f, INK, true))
        box.addView(row, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(ctx, 4) })
        return box
    }

    data class Res(val cells: List<Triple<String, String, Int>>, val verdict: Int, val line: String)

    fun compute(ctx: Context, o: Oferta): Res {
        val c = Store.cfg(ctx)
        val out = mutableListOf<Triple<String, String, Int>>()
        val v = o.valor
        val grades = mutableListOf<Int>()
        if (v != null && o.pacotes != null && o.pacotes > 0) {
            val x = v / o.pacotes; val g = Calc.grade(x, c.optDouble("ruimPct", 0.0), c.optDouble("metaPct", 0.0))
            out += Triple("R$/pct", Calc.dec(x, 2), g); grades += g
        }
        if (v != null && o.km != null && o.km > 0) {
            val x = v / o.km; val g = Calc.grade(x, c.optDouble("ruimKm", 0.0), c.optDouble("metaKm", 0.0))
            out += Triple("R$/km", Calc.dec(x, 2), g); grades += g
        }
        if (v != null && o.minutos != null && o.minutos > 0) {
            val x = v / (o.minutos / 60.0); val g = Calc.grade(x, c.optDouble("ruimHora", 0.0), c.optDouble("metaHora", 0.0))
            out += Triple("R$/hora", Calc.dec(x, 0), g); grades += g
        }
        val cons = c.optDouble("cons", 0.0)
        if (v != null && v > 0 && o.km != null && cons > 0) {
            val lucro = v - o.km / cons * c.optDouble("gas", 0.0)
            out += Triple("Lucro %", Calc.dec(lucro / v * 100, 0), if (lucro <= 0) 3 else 0)
        }
        val ver = when { grades.isEmpty() -> 0; 3 in grades -> 3; 2 in grades -> 2; else -> 1 }
        val parts = mutableListOf<String>()
        if (v != null) parts += Calc.brl(v)
        if (o.pacotes != null) parts += "${o.pacotes} pct"
        if (o.km != null) parts += "${Calc.dec(o.km, 1)} km"
        if (o.paradas != null) parts += "${o.paradas} paradas"
        return Res(out, ver, parts.joinToString(" · "))
    }

    private fun verdictText(g: Int) = when (g) { 1 -> "Compensa"; 2 -> "No limite"; 3 -> "Abaixo do seu mínimo"; else -> "" }

    @SuppressLint("ClickableViewAccessibility")
    private fun draggable(ctx: Context, v: View, p: WindowManager.LayoutParams, key: String, horizontal: Boolean, onTap: () -> Unit) {
        val slop = ViewConfiguration.get(ctx).scaledTouchSlop
        var sx = 0f; var sy = 0f; var ox = 0; var oy = 0; var moved = false
        v.setOnTouchListener { _, e ->
            when (e.action) {
                MotionEvent.ACTION_DOWN -> { sx = e.rawX; sy = e.rawY; ox = p.x; oy = p.y; moved = false; true }
                MotionEvent.ACTION_MOVE -> {
                    val dx = e.rawX - sx; val dy = e.rawY - sy
                    if (abs(dx) > slop || abs(dy) > slop) moved = true
                    if (moved) {
                        if (horizontal) p.x = ox + dx.toInt()
                        p.y = (oy + dy.toInt()).coerceAtLeast(0)
                        try { wm(ctx).updateViewLayout(v, p) } catch (_: Exception) { }
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (moved) Store.p(ctx).edit().putInt(key + "_x", p.x).putInt(key + "_y", p.y).apply() else onTap()
                    true
                }
                else -> false
            }
        }
    }

    private fun openApp(ctx: Context) {
        ctx.startActivity(Intent(ctx, MainActivity::class.java).apply {
            putExtra(MainActivity.EXTRA_ROUTE, true)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        })
    }

    /* ---------------- cartão da rota ---------------- */

    fun showCard(ctx0: Context, o: Oferta, app: String) {
        val ctx = ctx0.applicationContext
        if (!can(ctx)) return
        h.post {
            hideCard()
            val r = compute(ctx, o)
            val wrap = FrameLayout(ctx).apply { setPadding(dp(ctx, 12), 0, dp(ctx, 12), 0) }
            val box = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                background = round(BG, dp(ctx, 18), COR[r.verdict], dp(ctx, 3))
                setPadding(dp(ctx, 14), dp(ctx, 12), dp(ctx, 14), dp(ctx, 12))
                elevation = dp(ctx, 8).toFloat()
            }
            val top = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
            top.addView(tv(ctx, "Rota Paga · $app", 12f, MUTED, true), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
            val x = tv(ctx, "✕", 18f, MUTED, true).apply { setPadding(dp(ctx, 12), dp(ctx, 4), dp(ctx, 4), dp(ctx, 4)); setOnClickListener { hideCard(); Alert.stop(ctx) } }
            top.addView(x)
            box.addView(top)
            if (r.line.isNotEmpty()) box.addView(tv(ctx, r.line, 15f, INK, true), LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(ctx, 6) })
            if (r.cells.isNotEmpty()) {
                val row = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
                r.cells.forEach { (l, v, g) -> row.addView(cell(ctx, l, v, g), LinearLayout.LayoutParams(0, -2, 1f)) }
                box.addView(row, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(ctx, 10) })
                if (r.verdict > 0) box.addView(tv(ctx, verdictText(r.verdict), 13f, COR[r.verdict]!!, true), LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(ctx, 8) })
            } else {
                box.addView(tv(ctx, "Não veio o valor e os pacotes na notificação. Toque na bolha ou aqui para calcular.", 13f, MUTED), LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(ctx, 8) })
            }
            wrap.addView(box)
            val p = lp(WindowManager.LayoutParams.MATCH_PARENT, false).apply {
                gravity = Gravity.TOP or Gravity.START
                y = Store.p(ctx).getInt("card_y", dp(ctx, 90))
            }
            draggable(ctx, wrap, p, "card", false) { hideCard(); Alert.stop(ctx); openApp(ctx) }
            try { wm(ctx).addView(wrap, p); card = wrap } catch (_: Exception) { }
            h.postDelayed(hideCardRun, 45_000L)
        }
    }

    fun hideCard() {
        h.removeCallbacks(hideCardRun)
        card?.let { remove(it.context, it) }
        card = null
    }

    /* ---------------- bolha de cálculo rápido ---------------- */

    fun syncBubble(ctx0: Context) {
        val ctx = ctx0.applicationContext
        h.post {
            val want = Store.cfg(ctx).optBoolean("bubble", false) && can(ctx)
            if (want && bubble == null) showBubble(ctx)
            if (!want) { closePanel(ctx); bubble?.let { remove(ctx, it) }; bubble = null }
        }
    }

    private fun showBubble(ctx: Context) {
        val b = tv(ctx, "R$", 18f, "#FFFFFF", true).apply {
            gravity = Gravity.CENTER
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(Color.parseColor(ACC)); setStroke(dp(ctx, 3), Color.WHITE) }
            elevation = dp(ctx, 6).toFloat()
            contentDescription = "Cálculo rápido"
        }
        val s = dp(ctx, 54)
        val p = lp(s, false).apply {
            height = s
            gravity = Gravity.TOP or Gravity.START
            x = Store.p(ctx).getInt("bubble_x", dp(ctx, 8))
            y = Store.p(ctx).getInt("bubble_y", dp(ctx, 220))
        }
        draggable(ctx, b, p, "bubble", true) { if (panel == null) openPanel(ctx, p.y + s + dp(ctx, 8)) else closePanel(ctx) }
        try { wm(ctx).addView(b, p); bubble = b } catch (_: Exception) { }
    }

    private fun field(ctx: Context, hint: String, decimal: Boolean) = EditText(ctx).apply {
        this.hint = hint
        setHintTextColor(Color.parseColor(MUTED)); setTextColor(Color.parseColor(INK))
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
        inputType = if (decimal) InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL else InputType.TYPE_CLASS_NUMBER
        background = round("#1C2622", dp(ctx, 10), "#2C3A35", dp(ctx, 1))
        setPadding(dp(ctx, 10), dp(ctx, 8), dp(ctx, 10), dp(ctx, 8))
        setSingleLine()
    }

    private fun openPanel(ctx: Context, y: Int) {
        val box = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            background = round(BG, dp(ctx, 18), "#2C3A35", dp(ctx, 1))
            setPadding(dp(ctx, 14), dp(ctx, 12), dp(ctx, 14), dp(ctx, 12))
            elevation = dp(ctx, 8).toFloat()
        }
        val top = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        top.addView(tv(ctx, "Quanto paga esta rota?", 14f, INK, true), LinearLayout.LayoutParams(0, -2, 1f))
        top.addView(tv(ctx, "✕", 18f, MUTED, true).apply { setPadding(dp(ctx, 12), dp(ctx, 4), dp(ctx, 4), dp(ctx, 4)); setOnClickListener { closePanel(ctx) } })
        box.addView(top)
        val fv = field(ctx, "Valor R$", true); val fp = field(ctx, "Pacotes", false); val fk = field(ctx, "Km", true)
        val row = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        listOf(fv, fp, fk).forEachIndexed { i, f -> row.addView(f, LinearLayout.LayoutParams(0, -2, 1f).apply { if (i > 0) marginStart = dp(ctx, 6) }) }
        box.addView(row, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(ctx, 10) })
        val res = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        box.addView(res, LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(ctx, 10) })
        val save = tv(ctx, "Abrir no Rota Paga", 14f, "#3DB883", true).apply { setPadding(0, dp(ctx, 10), 0, dp(ctx, 2)) }
        box.addView(save)
        fun upd() {
            res.removeAllViews()
            val o = Oferta(Calc.num(fv.text.toString()), fp.text.toString().toIntOrNull(), Calc.num(fk.text.toString()), null, null)
            val r = compute(ctx, o)
            if (r.cells.isEmpty()) { res.addView(tv(ctx, "Digite o valor e os pacotes (o km é opcional).", 13f, MUTED)); return }
            val cr = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
            r.cells.forEach { (l, v, g) -> cr.addView(cell(ctx, l, v, g), LinearLayout.LayoutParams(0, -2, 1f)) }
            res.addView(cr)
            if (r.verdict > 0) res.addView(tv(ctx, verdictText(r.verdict), 13f, COR[r.verdict]!!, true), LinearLayout.LayoutParams(-2, -2).apply { topMargin = dp(ctx, 8) })
        }
        val w = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) = upd()
        }
        listOf(fv, fp, fk).forEach { it.addTextChangedListener(w) }
        upd()
        save.setOnClickListener {
            val parts = mutableListOf<String>()
            if (fv.text.isNotBlank()) parts += "R$ " + fv.text
            if (fp.text.isNotBlank()) parts += fp.text.toString() + " pacotes"
            if (fk.text.isNotBlank()) parts += fk.text.toString() + " km"
            Store.setPending(ctx, parts.joinToString(" · "), "Cálculo rápido")
            closePanel(ctx); openApp(ctx)
        }
        val wrap = FrameLayout(ctx).apply { setPadding(dp(ctx, 12), 0, dp(ctx, 12), 0); addView(box) }
        val p = lp(WindowManager.LayoutParams.MATCH_PARENT, true).apply {
            gravity = Gravity.TOP or Gravity.START
            this.y = y
            softInputMode = WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE or WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN
        }
        try {
            wm(ctx).addView(wrap, p); panel = wrap
            fv.requestFocus()
            h.postDelayed({ (ctx.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager).showSoftInput(fv, 0) }, 150)
        } catch (_: Exception) { }
    }

    private fun closePanel(ctx: Context) {
        panel?.let { remove(ctx, it) }
        panel = null
    }
}
