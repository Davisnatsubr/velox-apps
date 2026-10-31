# App Android — Controle de Renovações

App que abre o painel de renovações em tela cheia no celular.

- Na primeira vez, o app pergunta o endereço do painel (ex.: `https://seudominio.com/painel/`). Fica salvo.
- Login fica salvo, igual ao navegador.
- Importar planilha, backup em Excel (vai para Downloads), links de e-mail/WhatsApp e botão voltar funcionam.
- Tudo que muda no painel aparece no app sem reinstalar.

O GitHub compila sozinho a cada envio para esta branch (aba **Actions → Gerar app Renovações**).
O APK fica em **Artifacts** da execução e também na branch `builds-renovacoes`.

A chave em `assinatura/` serve para que cada APK novo instale por cima do anterior.
