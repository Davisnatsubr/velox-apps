package br.painel.renovacoes

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.InputType
import android.util.Base64
import android.view.View
import android.view.ViewGroup
import android.webkit.*
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.Toast

/**
 * App do Controle de Renovações: abre o painel online numa tela cheia.
 * - Na primeira vez pergunta o endereço do painel (fica salvo).
 * - Login fica salvo (cookies), igual ao navegador.
 * - Importar planilha (escolher arquivo), backup em Excel (salva em Downloads),
 *   links de e-mail/WhatsApp abrem no app certo, botão voltar do celular funciona.
 */
class MainActivity : Activity() {

    private lateinit var web: WebView
    private lateinit var barra: ProgressBar
    private var escolherArquivo: ValueCallback<Array<Uri>>? = null
    private val prefs by lazy { getSharedPreferences("config", Context.MODE_PRIVATE) }

    companion object { private const val PEDIR_ARQUIVO = 10 }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val raiz = FrameLayout(this).apply { setBackgroundColor(0xFF0F1712.toInt()) }
        web = WebView(this).apply { setBackgroundColor(0xFF0F1712.toInt()) }
        barra = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            visibility = View.GONE
        }
        raiz.addView(web, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        raiz.addView(barra, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 8))
        setContentView(raiz)

        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, true)

        web.settings.apply {
            javaScriptEnabled = true
            domStorageEnabled = true
            databaseEnabled = true
            allowFileAccess = false
            loadWithOverviewMode = true
            useWideViewPort = true
            builtInZoomControls = false
            mediaPlaybackRequiresUserGesture = true
            userAgentString = "$userAgentString RenovacoesApp/${BuildConfig.VERSION_NAME}"
        }
        web.addJavascriptInterface(Ponte(), "AppRenovacoes")

        web.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, req: WebResourceRequest): Boolean {
                val url = req.url
                val base = Uri.parse(enderecoSalvo() ?: "")
                // Páginas do próprio painel ficam dentro do app; o resto abre fora (e-mail, WhatsApp, Telegram, outros sites)
                if ((url.scheme == "http" || url.scheme == "https") && url.host == base.host) return false
                abrirFora(url)
                return true
            }
            override fun onPageStarted(view: WebView, url: String, favicon: Bitmap?) { barra.visibility = View.VISIBLE }
            override fun onPageCommitVisible(view: WebView, url: String) { forcarEscuro(view) }
            override fun onPageFinished(view: WebView, url: String) {
                forcarEscuro(view)
                barra.visibility = View.GONE
                CookieManager.getInstance().flush()
            }
            override fun onReceivedError(view: WebView, req: WebResourceRequest, err: WebResourceError) {
                if (req.isForMainFrame) mostrarErro(err.description?.toString() ?: "sem conexão")
            }
        }

        web.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView, p: Int) { barra.progress = p }
            override fun onShowFileChooser(view: WebView, cb: ValueCallback<Array<Uri>>, params: FileChooserParams): Boolean {
                escolherArquivo?.onReceiveValue(null)
                escolherArquivo = cb
                val i = Intent(Intent.ACTION_GET_CONTENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "*/*"
                }
                return try {
                    startActivityForResult(Intent.createChooser(i, "Escolher planilha"), PEDIR_ARQUIVO); true
                } catch (e: Exception) {
                    escolherArquivo = null; false
                }
            }
        }

        // Downloads comuns (links de arquivo) vão para o navegador/gerenciador do celular
        web.setDownloadListener { url, _, _, _, _ ->
            if (url.startsWith("http")) abrirFora(Uri.parse(url))
        }

        if (savedInstanceState != null) {
            web.restoreState(savedInstanceState)
        } else {
            val salvo = enderecoSalvo()
            if (salvo.isNullOrBlank()) pedirEndereco(primeiraVez = true) else abrir(salvo, intent)
        }
    }

    // Atalhos do ícone (#adicionar, #avisar...) quando o app já está aberto
    override fun onNewIntent(i: Intent) {
        super.onNewIntent(i)
        val frag = i.data?.fragment
        if (!frag.isNullOrBlank()) web.evaluateJavascript("location.hash='#$frag'", null)
    }

    // O app é sempre escuro: avisa o painel para usar o tema escuro
    private fun forcarEscuro(view: WebView) {
        view.evaluateJavascript("document.documentElement.setAttribute('data-theme','dark')", null)
    }

    private fun enderecoSalvo(): String? = prefs.getString("url", null)

    private fun abrir(endereco: String, i: Intent? = null) {
        val frag = i?.data?.fragment
        web.loadUrl(if (frag.isNullOrBlank()) endereco else "$endereco#$frag")
    }

    private fun normalizar(txt: String): String {
        var u = txt.trim()
        if (!u.startsWith("http://") && !u.startsWith("https://")) u = "https://$u"
        if (!u.endsWith("/") && !u.substringAfterLast('/').contains('.')) u += "/"
        return u
    }

    private fun pedirEndereco(primeiraVez: Boolean) {
        val campo = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            hint = "https://seudominio.com/painel/"
            setText(enderecoSalvo() ?: BuildConfig.SITE_URL.ifBlank { "https://" })
            setSelection(text.length)
        }
        val caixa = FrameLayout(this).apply {
            val p = (20 * resources.displayMetrics.density).toInt()
            setPadding(p, p / 2, p, 0)
            addView(campo)
        }
        AlertDialog.Builder(this)
            .setTitle(if (primeiraVez) "Endereço do painel" else "Trocar endereço do painel")
            .setMessage("Digite o endereço onde o painel está hospedado.")
            .setView(caixa)
            .setCancelable(!primeiraVez)
            .setPositiveButton("Abrir") { _, _ ->
                val u = normalizar(campo.text.toString())
                prefs.edit().putString("url", u).apply()
                abrir(u)
            }
            .apply { if (!primeiraVez) setNegativeButton("Cancelar", null) }
            .show()
    }

    private fun mostrarErro(motivo: String) {
        web.loadData(
            "<html><body style='font-family:sans-serif;background:#0f1712;color:#e3ece5;padding:32px'>" +
                "<h2>Não foi possível abrir o painel</h2><p>Confira a internet do celular.</p>" +
                "<p style='color:#93a598;font-size:13px'>$motivo</p></body></html>",
            "text/html; charset=utf-8", "utf-8"
        )
        AlertDialog.Builder(this)
            .setTitle("Sem conexão com o painel")
            .setMessage("Endereço: ${enderecoSalvo()}")
            .setPositiveButton("Tentar de novo") { _, _ -> enderecoSalvo()?.let { abrir(it) } }
            .setNeutralButton("Trocar endereço") { _, _ -> pedirEndereco(false) }
            .show()
    }

    private fun abrirFora(uri: Uri) {
        try {
            startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
            Toast.makeText(this, "Nenhum app para abrir este link", Toast.LENGTH_SHORT).show()
        }
    }

    @Deprecated("Activity clássica")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == PEDIR_ARQUIVO) {
            val res = WebChromeClient.FileChooserParams.parseResult(resultCode, data)
            escolherArquivo?.onReceiveValue(res)
            escolherArquivo = null
            return
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    @Deprecated("Activity clássica")
    override fun onBackPressed() {
        if (web.canGoBack()) web.goBack() else super.onBackPressed()
    }

    override fun onSaveInstanceState(out: Bundle) {
        super.onSaveInstanceState(out)
        web.saveState(out)
    }

    override fun onPause() {
        super.onPause()
        CookieManager.getInstance().flush()
    }

    /** Funções que o painel chama de dentro do app (window.AppRenovacoes) */
    inner class Ponte {
        @JavascriptInterface
        fun versao(): String = BuildConfig.VERSION_NAME

        @JavascriptInterface
        fun trocarEndereco() { runOnUiThread { pedirEndereco(false) } }

        /** Salva um arquivo (ex.: backup .xlsx) na pasta Downloads do celular */
        @JavascriptInterface
        fun salvarArquivo(base64: String, nome: String, tipo: String): Boolean {
            return try {
                val bytes = Base64.decode(base64, Base64.DEFAULT)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val valores = ContentValues().apply {
                        put(MediaStore.Downloads.DISPLAY_NAME, nome)
                        put(MediaStore.Downloads.MIME_TYPE, tipo)
                        put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    }
                    val uri = contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, valores)
                        ?: throw IllegalStateException("sem acesso a Downloads")
                    contentResolver.openOutputStream(uri)!!.use { it.write(bytes) }
                } else {
                    val pasta = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)!!
                    java.io.File(pasta, nome).writeBytes(bytes)
                }
                runOnUiThread { Toast.makeText(this@MainActivity, "Salvo em Downloads: $nome", Toast.LENGTH_LONG).show() }
                true
            } catch (e: Exception) {
                runOnUiThread { Toast.makeText(this@MainActivity, "Não consegui salvar: ${e.message}", Toast.LENGTH_LONG).show() }
                false
            }
        }
    }
}
