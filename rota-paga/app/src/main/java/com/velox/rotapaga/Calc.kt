package com.velox.rotapaga

import java.text.Normalizer
import java.util.Locale

/** Números tirados do texto de uma notificação de rota. */
data class Oferta(
    val valor: Double?,
    val pacotes: Int?,
    val km: Double?,
    val paradas: Int?,
    val minutos: Int?,
)

/** Mesma leitura e mesmas contas do app (app.js), para o alarme e o cartão flutuante. */
object Calc {
    private val opt = setOf(RegexOption.IGNORE_CASE)
    private val rValor = Regex("""R\$\s*([\d.]+(?:,\d{1,2})?|\d+(?:\.\d{1,2})?)""", opt)
    private val rPct = Regex("""(\d+)\s*(?:pacotes?|pcts?|volumes?|itens|encomendas?)""", opt)
    private val rKm = Regex("""(\d+(?:[.,]\d+)?)\s*km\b""", opt)
    private val rPar = Regex("""(\d+)\s*(?:paradas?|endere[çc]os?|entregas?|stops?)""", opt)
    private val rMin = Regex("""(\d+)\s*(?:min\b|minutos)""", opt)
    private val rH = Regex("""(\d+(?:[.,]\d+)?)\s*h(?:oras?)?\b""", opt)
    private val ptBR: Locale = Locale.forLanguageTag("pt-BR")

    fun num(s: String?): Double? {
        if (s == null) return null
        var t = s.trim().replace(Regex("[^\\d.,-]"), "")
        if (t.contains(",")) t = t.replace(".", "").replace(",", ".")
        return t.toDoubleOrNull()
    }

    fun parse(text: String): Oferta {
        val t = text.replace(Regex("\\s+"), " ")
        val valor = rValor.find(t)?.groupValues?.get(1)?.let { num(it) }
        val pct = rPct.find(t)?.groupValues?.get(1)?.toIntOrNull()
        val km = rKm.find(t)?.groupValues?.get(1)?.let { num(it.replace(".", ",")) }
        val par = rPar.find(t)?.groupValues?.get(1)?.toIntOrNull()
        val min = rMin.find(t)?.groupValues?.get(1)?.toIntOrNull()
            ?: rH.find(t)?.groupValues?.get(1)?.let { num(it.replace(".", ",")) }?.let { (it * 60).toInt() }
        return Oferta(valor, pct, km, par, min)
    }

    /** Sem acentos e minúsculo, para comparar palavras-chave. */
    fun norm(s: String): String =
        Normalizer.normalize(s.lowercase(ptBR), Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "")

    /** 0 = sem dado, 1 = bom (verde), 2 = no limite (amarelo), 3 = ruim (vermelho). */
    fun grade(v: Double?, ruim: Double, bom: Double): Int {
        if (v == null || (bom <= 0 && ruim <= 0)) return 0
        if (bom > 0 && v >= bom) return 1
        if (ruim > 0 && v < ruim) return 3
        return if (bom > 0) 2 else 1
    }

    fun brl(v: Double): String = "R$ " + String.format(ptBR, "%,.2f", v)
    fun dec(v: Double, d: Int = 1): String = String.format(ptBR, "%,.${d}f", v)
}
