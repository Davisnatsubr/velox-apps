# Rota Paga

App Android para motorista de entregas (Mercado Livre Envios Extra e parecidos). Ele diz se a rota compensa antes de você aceitar e mede quanto ela rendeu de verdade.

## O que faz

- **Alarme de rota.** Lê a notificação do app de entregas e toca um alarme alto, mesmo com o celular no silencioso. A própria notificação já mostra o R$ por pacote, com bolinha verde, amarela ou vermelha.
- **Cartão por cima do Mercado Livre.** Quando a rota chega, aparece um cartão flutuante com R$/pacote, R$/km, R$/hora e lucro %. Você arrasta o cartão para onde quiser e ele some sozinho.
- **Bolha de cálculo rápido.** Botão flutuante: toque nele, digite o valor, os pacotes e os km da oferta e veja o resultado sem sair do app de entregas.
- **Rotas agendadas.** A rota que você aceitou hoje para amanhã fica salva. No dia, é só tocar em **Iniciar**.
- **Cronômetro com GPS.** Conta o tempo e os km com a tela apagada e mostra ao vivo R$/hora, R$/km e lucro. No fim, já preenche a entrega.
- **Mapa e escolha de rota.** Paradas numeradas no mapa e comparação de rotas: ordem otimizada, ordem da lista ou terminando em casa. Depois de escolher, é só tocar em **Navegar** para abrir no Google Maps ou no Waze.
- **Financeiro.** Ganhos, gastos e lucro por dia, semana, mês e ano, com R$ por km, hora, parada e pacote. Inclui também:
  - consumo real do carro;
  - PNR;
  - fotos de comprovante;
  - planilha e backup.

## Instalar para testar

1. Abra a aba **Releases** do repositório no celular e baixe o `RotaPaga-1.0.N-teste.apk` mais novo.
2. Abra o arquivo e autorize "instalar apps desconhecidos".
3. No app, vá em **Ajustes › Alerta de rota** e libere:
   - **Acesso às notificações:** sem ele, o alarme não funciona;
   - **Aparecer sobre outros apps:** para o cartão e a bolha;
   - **Localização:** pedida quando você liga o cronômetro.
4. Toque em **Testar alarme**.

## Como é compilado

Cada envio para a pasta `rota-paga/` roda **Actions › Gerar app Rota Paga**. O resultado sai em três lugares:

- **Releases:** o APK para instalar;
- branch `builds-rota-paga`: o log do build;
- branch `previa-rota-paga`: prints do app aberto num navegador, com mapa e GPS simulado.

Se existirem os secrets `KEYSTORE_*`, os mesmos dos apps Velox, sai também o `.aab` assinado para a Play Store.

## Antes de publicar na Play Store

- **Nome.** "Rota Paga" é uma sugestão. Antes de publicar, confira se o nome está livre na Play Store e no INPI. Para trocar, edite `app/src/main/res/values/strings.xml` e os textos em `assets/www`. O `applicationId` (`com.velox.rotapaga`) não pode mudar depois de publicado.
- **Mapa.** Hoje o mapa usa os servidores gratuitos do OpenStreetMap:
  - mapa: `tile.openstreetmap.org`;
  - endereços: Nominatim;
  - rotas: OSRM de demonstração.

  Eles servem para testes e uso pequeno. Com muitos usuários, troque por um serviço pago ou próprio, como MapTiler, Stadia, Mapbox ou um OSRM próprio. As URLs ficam em `app.js`, nos objetos `Mapa`, `Geo` e `Rotas`.
- **Permissões sensíveis.** No Play Console, você vai precisar declarar:
  - **Acesso a notificações:** é a função principal (alerta de rota); explique isso na descrição;
  - **Localização em primeiro plano** (serviço `location`): é o cronômetro; grave um vídeo curto mostrando o uso;
  - **Sobreposição a outros apps:** é o cartão e a bolha.
- **Política de privacidade.** Obrigatória. Os dados ficam só no celular; o app manda endereços e coordenadas apenas para o OpenStreetMap, para achar os locais e calcular as rotas.
- **Teste fechado.** Contas pessoais novas precisam de 12 testadores por 14 dias.
- **Mercado Livre.** O app só lê a notificação e mostra números; ele não aceita rotas sozinho, porque aceitar automaticamente pode bloquear a conta do motorista.

## Estrutura

```
app/src/main/assets/www/   o app (HTML, CSS e JS): telas, contas, mapa, cronômetro
app/src/main/java/.../     a parte Android
  MainActivity.kt          abre o app num WebView e liga a ponte
  Bridge.kt                funções chamadas pelo app.js (window.RotaApp)
  RouteListener.kt         lê as notificações
  Alert.kt                 alarme e notificação da rota
  Overlay.kt               cartão flutuante e bolha de cálculo
  TripService.kt, TripStore.kt   GPS do cronômetro com a tela apagada
  Calc.kt, Store.kt        contas e configurações
tools/                     baixar o mapa (Leaflet) e gerar os prints
```
