package com.veloxplay.app

import android.content.Context

/** Identificação (User-Agent) que os servidores IPTV veem, como "BlackPro/v1.13.0-Exo/2" nos outros apps. */
object UA {
    private fun name(c: Context) = c.getString(R.string.app_name).replace(" ", "") // VeloxPlay / PulsoPlay

    /** Player nativo (filmes, séries e ao vivo no ExoPlayer). */
    fun player(c: Context) = "${name(c)}/v${BuildConfig.VERSION_NAME}-Exo"

    /** Vídeos do ao vivo tocados na tela do app (passam pelo app). */
    fun web(c: Context) = "${name(c)}/v${BuildConfig.VERSION_NAME}"
}
