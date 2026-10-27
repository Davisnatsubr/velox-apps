# Apps Android — Velox Play e Pulso Play

Um projeto só gera **dois apps separados**:

| App | ID na Play Store | Site que abre |
|---|---|---|
| Velox Play | `com.veloxplay.velox` | `http://betaspark.saiyajinpixel.online/webplayer/` |
| Pulso Play | `com.veloxplay.pulso` | `http://pulso.saiyajinpixel.online/velox-pulso/` |

Funcionam em celular, tablet, Android TV, TV Box e Fire TV (o mesmo arquivo).

**Como funcionam:**
- A interface é o próprio site dentro do app. O que você muda no site ou no painel aparece no app sem precisar atualizar.
- Os vídeos em tela cheia abrem no **player nativo do Android (ExoPlayer)**. Ele:
  - toca áudio AC3/E-AC3, vídeo HEVC e canais em TS (o navegador não toca), quando o aparelho suporta;
  - usa a aceleração de vídeo da TV Box, então trava menos;
  - tenta sozinho a DNS reserva se a principal falhar;
  - tem botão "Tela: Original / Esticar / Zoom" (no controle remoto, o botão MENU também troca);
  - tem escolha de áudio e legenda;
  - devolve o ponto onde o vídeo parou para o "Continuar assistindo".
- O botão voltar do celular/controle fecha o que estiver aberto no site. Na tela inicial, aperte duas vezes para sair.

> Os sites precisam estar na versão **Velox 0.1.2** e **Pulso 2.0.4** (ou mais nova). São elas que mandam o vídeo para o player nativo.

---

## 1. Gerar os arquivos (GitHub Actions)

Você não precisa instalar nada; o GitHub compila de graça.

1. Crie uma conta em https://github.com (se ainda não tiver).
2. Clique em **New repository**, dê o nome `velox-apps`, marque **Private** e clique em **Create repository**.
3. Na página do repositório vazio, clique em **uploading an existing file**. Arraste **todo o conteúdo** desta pasta (inclusive a pasta `.github`; no Windows ela aparece normalmente, no Mac aperte Cmd+Shift+. para ver pastas ocultas). Clique em **Commit changes**.
4. Vá em **Settings › Secrets and variables › Actions › New repository secret** e crie os 4 segredos do arquivo `chave-upload-LEIA.txt`:
   - `KEYSTORE_BASE64`: cole todo o conteúdo do arquivo `upload.jks.base64`;
   - `KEYSTORE_PASSWORD`;
   - `KEY_ALIAS`: `upload`;
   - `KEY_PASSWORD`.
5. Vá na aba **Actions**, clique em **Gerar apps Android › Run workflow**. Leva uns 5 a 8 minutos.
6. Quando ficar verde, clique na execução e baixe **apps-android-1.0.N** (lá embaixo, em *Artifacts*). Dentro vêm:
   - `VeloxPlay-1.0.N.apk` e `PulsoPlay-1.0.N.apk`: para instalar direto no celular/TV Box e testar;
   - `VeloxPlay-1.0.N.aab` e `PulsoPlay-1.0.N.aab`: para enviar à Play Store.

Cada vez que você enviar uma mudança ou clicar em *Run workflow*, sai uma versão nova com número maior (a Play Store exige isso).

Se a compilação falhar (fica vermelho), abra a execução, clique no passo vermelho, copie o texto do erro e me mande.

**Guarde o `upload.jks` e o `chave-upload-LEIA.txt` em lugar seguro.** São eles que provam à Play Store que as atualizações são suas. A mesma chave serve para os dois apps.

## 2. Testar antes de publicar

- **Celular:** mande o `.apk` para o celular (WhatsApp, Drive) e abra; autorize "instalar apps desconhecidos".
- **TV Box / Android TV / Fire TV:** instale o app *Downloader*, ponha o `.apk` num link (Google Drive com link público, ou no seu site) e baixe por ele.

Confira: login, um canal, um filme (de preferência um que no site dá erro de áudio), uma série, o botão voltar e o controle remoto.

## 3. Publicar na Play Store (para cada app)

No Play Console (https://play.google.com/console):

1. **Criar app** › nome (`Velox Play` / `Pulso Play`), idioma Português (Brasil), *App*, *Gratuito*.
2. **Configurar o app** (o painel mostra a lista de tarefas):
   - **Acesso ao app:** marque que precisa de login e **informe um usuário e senha de teste** que funcionem. Sem isso, a revisão reprova.
   - **Anúncios:** não contém.
   - **Classificação do conteúdo:** responda o questionário (app de streaming).
   - **Público-alvo:** 18+ se tiver qualquer conteúdo adulto na lista; senão, 13+ ou 16+.
   - **Segurança dos dados:** o app envia usuário e senha para o seu servidor, com criptografia apenas se o site estiver em HTTPS. Não coleta localização, contatos nem fotos.
   - **Política de privacidade:** um link público. Posso escrever o texto e você coloca no seu site.
3. **Página da loja:** use as imagens da pasta `loja/`:
   - ícone 512×512;
   - imagem de destaque 1024×500;
   - banner de TV 1280×720.

   Tire também pelo menos 2 prints do celular e, para TV, 1 print 1920×1080. Não use prints mostrando logos de canais ou capas de filmes.
4. **Android TV:** em *Configuração avançada › Formatos*, adicione **Android TV**. A Google faz uma revisão extra de TV, que verifica se tudo funciona com o controle remoto e se o banner existe. As duas coisas já estão prontas no app.
5. **Teste fechado:** se a sua conta de desenvolvedor for pessoal e criada depois de novembro de 2023, a Google exige um teste fechado com **12 testadores por 14 dias seguidos** antes de liberar a produção. Crie a faixa *Teste fechado*, envie o `.aab`, adicione os e-mails dos testadores e mande o link de convite para eles.
6. **Produção:** depois do teste, crie a versão de produção com o `.aab` e envie para revisão.

**Atenção às regras da loja:**
- O app é um *player*: na descrição e nos prints, não cite nomes de canais, emissoras, filmes ou campeonatos.
- Apps de IPTV recebem denúncias de direitos autorais com frequência. A Google pode remover o app e, em casos repetidos, suspender a conta. Por isso, mantenha o APK também disponível fora da loja (no seu site ou pelo Downloader).

## Mudar o endereço, nome ou ID

Tudo fica em `app/build.gradle.kts`, nos blocos `create("velox")` e `create("pulso")`:
- `SITE_URL`: endereço do site (termine com `/`). Se o site mudar para a VPS, troque aqui e gere de novo.
- `app_name`: nome que aparece embaixo do ícone.
- `applicationId`: **não mude depois de publicado** (vira outro app).

Ícones: `app/src/velox/res` e `app/src/pulso/res`.

## Estrutura

```
app/build.gradle.kts                 os dois apps (sabores velox e pulso), versões, assinatura
app/src/main/AndroidManifest.xml     permissões, TV, telas
app/src/main/java/.../MainActivity.kt  site no WebView, botão voltar, tela de erro
app/src/main/java/.../PlayerActivity.kt  player nativo ExoPlayer
app/src/velox/res, app/src/pulso/res ícones, banner de TV, cores
.github/workflows/android.yml        compilação automática no GitHub
loja/                                imagens para a página da Play Store
```
