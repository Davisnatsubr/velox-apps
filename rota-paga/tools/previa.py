"""Abre o app num Chromium com GPS simulado, calcula rotas de verdade (OpenStreetMap/OSRM) e tira prints.
Uso: python previa.py <pasta www> <pasta de saída>"""
import datetime, functools, http.server, json, os, sys, threading, time
from playwright.sync_api import sync_playwright

www, out = sys.argv[1], sys.argv[2]
os.makedirs(out, exist_ok=True)
log = open(os.path.join(out, "log.txt"), "w")
def say(*a):
    print(*a); print(*a, file=log); log.flush()

srv = http.server.ThreadingHTTPServer(("127.0.0.1", 8765), functools.partial(http.server.SimpleHTTPRequestHandler, directory=www))
threading.Thread(target=srv.serve_forever, daemon=True).start()

hoje = datetime.date.today()
amanha = (hoje + datetime.timedelta(days=1)).isoformat()
t = hoje.isoformat()
# Teresina (bairro Dirceu Arcoverde e arredores)
stops = [
    ("Rua Breno Teodomiro de Carvalho, Teresina", None, None, 3),
    ("Avenida José Francisco de Almeida Neto, Teresina", None, None, 2),
    ("Ponto A", -5.0905, -42.7480, 4),
    ("Ponto B", -5.0825, -42.7600, 1),
    ("Ponto C", -5.0950, -42.7560, 5),
    ("Ponto D", -5.0790, -42.7505, 2),
]
items = []
for i, (e, la, lo, p) in enumerate(stops):
    d = {"id": "s%d" % i, "kind": "parada", "endereco": e, "pacotes": str(p), "status": "pendente", "ordem": i + 1, "data": t, "ts": i}
    if la: d.update(lat=la, lon=lo, geo="ok")
    items.append(d)
items += [
    {"id": "l1", "kind": "lanc", "tipo": "entrega", "data": t, "cidade": "Teresina", "cd": "CD Sul", "valor": "150", "km": "32", "paradas": "20", "pacotes": "48", "tempo": "240", "ts": 1},
    {"id": "l2", "kind": "lanc", "tipo": "abast", "data": t, "posto": "Posto Ipiranga", "litros": "20", "valor": "126", "ts": 2},
    {"id": "a1", "kind": "agenda", "data": amanha, "hora": "06:30", "cd": "CD Sul", "valor": "180", "pacotes": "55", "km": "38", "ts": 3},
]
cfg = {"nome": "Davi", "cidade": "Teresina - PI"}

# ~25 m a cada 1,3 s (uns 70 km/h), para passar no filtro de saltos impossíveis
path = [(-5.0870 - i * 0.00016, -42.7530 + i * 0.00016) for i in range(14)]
errs = []
with sync_playwright() as pw:
    b = pw.chromium.launch()
    ctx = b.new_context(viewport={"width": 400, "height": 860}, device_scale_factor=2, locale="pt-BR",
                        geolocation={"latitude": path[0][0], "longitude": path[0][1], "accuracy": 10}, permissions=["geolocation"])
    pg = ctx.new_page()
    pg.on("console", lambda m: m.type == "error" and errs.append(m.text))
    pg.on("pageerror", lambda e: errs.append(str(e)))
    pg.goto("http://127.0.0.1:8765/index.html")
    pg.evaluate("([i,c])=>{localStorage.setItem('rp_items',JSON.stringify(i));localStorage.setItem('rp_cfg',JSON.stringify(c))}", [items, cfg])
    pg.reload(); pg.wait_for_timeout(1200)
    pg.screenshot(path=f"{out}/1-inicio.png", full_page=True)

    pg.click("[data-tab=rota]")
    pg.wait_for_timeout(9000)  # geocodifica os 2 endereços (1 por segundo) e carrega o mapa
    st = pg.evaluate("JSON.parse(localStorage.getItem('rp_items')).filter(x=>x.kind==='parada').map(x=>[x.endereco,x.lat,x.geo])")
    say("paradas:", json.dumps(st, ensure_ascii=False))
    pg.screenshot(path=f"{out}/2-rota.png")
    pg.click("#calcRoutes")
    pg.wait_for_timeout(9000)
    say("opcoes:", pg.inner_text("#routeOpts")[:600].replace("\n", " | "))
    pg.evaluate("window.scrollTo(0,0)")
    pg.screenshot(path=f"{out}/3-rotas.png", full_page=True)
    if pg.query_selector("[data-ropt='1']"):
        pg.click("[data-ropt='1']"); pg.wait_for_timeout(600)
        pg.evaluate("window.scrollTo(0,0)")
        pg.screenshot(path=f"{out}/4-rota-alternativa.png")

    # cronômetro com GPS andando
    pg.click("[data-tab=inicio]"); pg.wait_for_timeout(300)
    pg.click("#startCard"); pg.fill("#t_valor", "150"); pg.fill("#t_pct", "48"); pg.click("#tGo")
    for la, lo in path:
        ctx.set_geolocation({"latitude": la, "longitude": lo, "accuracy": 8}); pg.wait_for_timeout(1300)
    pg.wait_for_timeout(1500)
    pg.click("#tripbar"); pg.wait_for_timeout(800)
    say("trip:", pg.inner_text("#tBody")[:300].replace("\n", " | "))
    pg.screenshot(path=f"{out}/5-cronometro.png")
    pg.click("#shClose")
    pg.click("[data-tab=rota]"); pg.wait_for_timeout(1500)
    pg.evaluate("window.scrollTo(0,0)")
    pg.screenshot(path=f"{out}/6-rota-trajeto.png")

    # notificação colada
    pg.click("[data-tab=inicio]"); pg.click("#btnAlert")
    pg.fill("#detText", "Envios Extra: nova rota para amanhã · R$ 180,00 · 55 pacotes · 38 km · 24 paradas")
    pg.wait_for_timeout(400)
    pg.screenshot(path=f"{out}/7-rota-detectada.png", full_page=True)

    pg.click("[data-tab=ajustes]"); pg.wait_for_timeout(400)
    pg.screenshot(path=f"{out}/8-ajustes.png", full_page=True)

    pg.emulate_media(color_scheme="dark"); pg.click("[data-tab=rota]"); pg.wait_for_timeout(1200)
    pg.evaluate("window.scrollTo(0,0)")
    pg.screenshot(path=f"{out}/9-rota-escuro.png")
    say("erros:", json.dumps(errs, ensure_ascii=False))
    b.close()
srv.shutdown()
