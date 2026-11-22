package com.veloxplay.app

import android.content.Context

/** Identificação (User-Agent) que os servidores IPTV veem, como "BlackPro/v1.13.0-Exo/2" nos outros apps. */
object UA {
    private fun name(c: Context) = c.getString(R.string.app_name).replace(" ", "") // VeloxPlay / PulsoPlay

    /** Player nativo (filmes, séries e ao vivo no ExoPlayer). */
    fun player(c: Context) = "${name(c)}/v${BuildConfig.VERSION_NAME}-Exo"

    /** Vídeos do ao vivo tocados na tela do app (passam pelo app). */
    fun web(c: Context) = "${name(c)}/v${BuildConfig.VERSION_NAME}"

    /** Igual ao navegador do computador: usado quando o servidor recusa o nome do app (alguns painéis só liberam navegadores conhecidos). */
    const val DESKTOP = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36"
}
