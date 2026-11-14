<?php
/*
 * Tela de faturamento (para ver no computador), montada a partir do backup que o app envia.
 * Usada em painel/ (o próprio motorista) e em admin/?p=faturamento (o administrador).
 * As contas são feitas no navegador, com as mesmas regras do app.
 */

/** Ganho bruto e lucro do mês atual de um backup (para as listas do painel). */
function resumo_mes(?string $dadosJson): array {
    $r = ['bruto' => 0.0, 'gastos' => 0.0, 'pacotes' => 0, 'rotas' => 0];
    $d = $dadosJson ? json_decode($dadosJson, true) : null;
    if (!is_array($d) || !is_array($d['items'] ?? null)) return $r;
    $mes = date('Y-m');
    $num = function ($v) { if ($v === null || $v === '') return 0.0; if (is_numeric($v)) return (float)$v; $s = preg_replace('/[^\d.,-]/', '', (string)$v); if (strpos($s, ',') !== false) $s = str_replace(',', '.', str_replace('.', '', $s)); return (float)$s; };
    $gasto = ['abast' => 1, 'manut' => 1, 'alim' => 1, 'pedag' => 1, 'outros' => 1];
    foreach ($d['items'] as $x) {
        if (!is_array($x) || substr((string)($x['data'] ?? ''), 0, 7) !== $mes) continue;
        $k = $x['kind'] ?? 'lanc';
        if ($k === 'pnr') { $r['gastos'] += $num($x['desconto'] ?? 0); continue; }
        if ($k !== 'lanc') continue;
        $v = $num($x['valor'] ?? 0);
        if (isset($gasto[$x['tipo'] ?? ''])) { $r['gastos'] += $v; continue; }
        $r['bruto'] += $v; $r['gastos'] += $num($x['pedagio'] ?? 0);
        if (($x['tipo'] ?? '') === 'entrega') { $r['rotas']++; $r['pacotes'] += (int)$num($x['pacotes'] ?? 0); }
    }
    $r['lucro'] = $r['bruto'] - $r['gastos'];
    return $r;
}

function render_faturamento(string $dadosJson, string $nome, ?string $atualizado, string $voltar = '', string $sair = ''): void {
    $dados = json_decode($dadosJson, true);
    if (!is_array($dados)) $dados = ['items' => [], 'cfg' => []];
    $payload = json_encode(['items' => $dados['items'] ?? [], 'cfg' => $dados['cfg'] ?? []], JSON_UNESCAPED_UNICODE | JSON_HEX_TAG | JSON_HEX_AMP | JSON_HEX_APOS | JSON_HEX_QUOT);
    $titulo = 'Faturamento · ' . $nome;
    ?><!doctype html>
<html lang="pt-BR"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><meta name="robots" content="noindex">
<title><?= h($titulo) ?></title>
<style>
:root{--bg:#F2F5F3;--s:#fff;--ink:#13201A;--mu:#5B6A63;--li:#D9E1DC;--ac:#0A6B4B;--soft:#E2EFE8;--sign:#0B5E42;--good:#1D8A4E;--warn:#A86E12;--bad:#BF3B28;--gb:#E1F3E8;--bb:#FBE4E0;--c2:#2F6FD6}
@media(prefers-color-scheme:dark){:root{--bg:#0E1412;--s:#171F1C;--ink:#E7EEEA;--mu:#93A39B;--li:#26322D;--ac:#3DB883;--soft:#1B2C25;--sign:#0D4E38;--good:#4CC583;--warn:#E3AC45;--bad:#EE6C57;--gb:#16301F;--bb:#3A1B16;--c2:#6EA0FF;color-scheme:dark}}
*{box-sizing:border-box}body{margin:0;background:var(--bg);color:var(--ink);font:15px/1.5 system-ui,-apple-system,"Segoe UI",Roboto,sans-serif}
[hidden]{display:none!important}.num{font-variant-numeric:tabular-nums}a{color:var(--ac)}
header{background:var(--sign);color:#fff;padding:14px 24px;display:flex;flex-wrap:wrap;gap:6px 18px;align-items:center}
header b{font-size:18px;letter-spacing:.04em}header span{opacity:.85;flex:1;min-width:200px}header a{color:#fff;opacity:.9}
main{max-width:1200px;margin:0 auto;padding:20px 24px 60px;display:flex;flex-direction:column;gap:18px}
h2{font-size:13px;text-transform:uppercase;letter-spacing:.08em;color:var(--mu);margin:0 0 10px}
.bar{display:flex;flex-wrap:wrap;gap:8px;align-items:center}
.seg{display:flex;gap:4px;background:var(--s);border:1px solid var(--li);border-radius:12px;padding:4px}
.seg button{border:0;background:none;border-radius:9px;padding:8px 14px;font:inherit;font-weight:600;color:var(--mu);cursor:pointer}
.seg button[aria-pressed=true]{background:var(--ac);color:#fff}
.nav{display:flex;align-items:center;gap:8px}.nav button,.btn{border:1px solid var(--li);background:var(--s);color:var(--ink);border-radius:10px;padding:8px 12px;font:inherit;font-weight:600;cursor:pointer}
.nav b{min-width:190px;text-align:center}
input[type=date]{border:1px solid var(--li);background:var(--s);color:var(--ink);border-radius:10px;padding:7px 10px;font:inherit}
.hero{background:var(--sign);color:#fff;border-radius:16px;padding:6px}.hero-in{border:2px solid rgba(255,255,255,.5);border-radius:12px;padding:18px 20px;display:grid;grid-template-columns:repeat(auto-fit,minmax(170px,1fr));gap:14px}
.hero small{display:block;font-size:12px;letter-spacing:.1em;text-transform:uppercase;opacity:.85}.hero b{font-size:30px;line-height:1.15}.hero .big b{font-size:42px}
.cards{display:grid;grid-template-columns:repeat(auto-fill,minmax(170px,1fr));gap:10px}
.card,.box{background:var(--s);border:1px solid var(--li);border-radius:14px;padding:14px}
.card small{display:block;color:var(--mu);font-size:12px;text-transform:uppercase;letter-spacing:.06em}.card b{font-size:24px;display:block}.card span{color:var(--mu);font-size:13px}
.good{color:var(--good)}.warn{color:var(--warn)}.bad{color:var(--bad)}
.cols{display:grid;grid-template-columns:repeat(auto-fit,minmax(340px,1fr));gap:18px}
.chart svg{width:100%;height:240px;display:block}.legend{display:flex;gap:16px;font-size:13px;color:var(--mu)}.legend i{display:inline-block;width:10px;height:10px;border-radius:3px;margin-right:6px;vertical-align:-1px}
.brk{display:flex;flex-direction:column;gap:10px}.brk div{display:grid;grid-template-columns:120px 1fr 110px;gap:10px;align-items:center;font-size:14px}
.brk em{display:block;height:10px;border-radius:5px;background:var(--ac)}.brk .g em{background:var(--bad)}.brk span:last-child{text-align:right;font-weight:600}
.tw{overflow-x:auto;background:var(--s);border:1px solid var(--li);border-radius:14px}
table{border-collapse:collapse;width:100%;font-size:14px}th,td{padding:9px 12px;border-bottom:1px solid var(--li);text-align:right;white-space:nowrap}
th{font-size:11px;text-transform:uppercase;letter-spacing:.06em;color:var(--mu);cursor:pointer;user-select:none}th:nth-child(-n+3),td:nth-child(-n+3){text-align:left}.cd th:nth-child(n+2),.cd td:nth-child(n+2){text-align:right}
tbody tr:hover{background:var(--soft)}td.neg{color:var(--bad)}
.note{color:var(--mu);font-size:14px}.empty{padding:30px;text-align:center;color:var(--mu)}
@media print{header a,.bar,.btn{display:none!important}body{background:#fff}}
</style></head><body>
<header><b>Rota Paga</b><span><?= h($nome) ?> · dados do celular de <?= $atualizado ? h(data_br($atualizado, true)) : '—' ?></span><?= $voltar ? '<a href="' . h($voltar) . '">‹ Voltar</a>' : '' ?><?= $sair ? '<a href="' . h($sair) . '">Sair</a>' : '' ?></header>
<main>
  <div class="bar">
    <div class="seg" id="per"><button data-p="semana">Semana</button><button data-p="mes">Mês</button><button data-p="ano">Ano</button><button data-p="tudo">Tudo</button><button data-p="livre">Escolher</button></div>
    <div class="nav" id="nav"><button id="prev" aria-label="Anterior">‹</button><b id="lbl"></b><button id="next" aria-label="Próximo">›</button></div>
    <div class="nav" id="livre" hidden><input type="date" id="d1"> até <input type="date" id="d2"></div>
    <span style="flex:1"></span><button class="btn" id="csv">Baixar planilha</button><button class="btn" onclick="print()">Imprimir</button>
  </div>
  <div id="out"></div>
  <p class="note">Os números vêm do último backup que o app enviou. Para ver o que lançou agora há pouco, abra o app: ele envia sozinho quando há coisa nova (ou use Ajustes › Conta › Enviar backup).</p>
</main>
<script>
const D=<?= $payload ?>;
(function(){
"use strict";
const $=s=>document.querySelector(s);
const items=Array.isArray(D.items)?D.items:[], cfg=Object.assign({gas:6.29,cons:11,desg:.25},D.cfg||{});
const NOMES={uber:"Uber",uberx:"Uber X",entrega:"Entrega",frete:"Frete",extra:"Extra",abast:"Abastecimento",manut:"Manutenção",alim:"Alimentação",pedag:"Pedágio",outros:"Outros gastos"};
const GASTO={abast:1,manut:1,alim:1,pedag:1,outros:1};
function num(v){ if(v==null||v==="") return 0; if(typeof v==="number") return isFinite(v)?v:0; let s=String(v).trim().replace(/[^\d.,-]/g,""); if(s.includes(",")) s=s.replace(/\./g,"").replace(",","."); const n=parseFloat(s); return isFinite(n)?n:0; }
const brl=n=>n.toLocaleString("pt-BR",{style:"currency",currency:"BRL"});
const dec=(n,d)=>n.toLocaleString("pt-BR",{minimumFractionDigits:d,maximumFractionDigits:d});
const esc=s=>String(s??"").replace(/[&<>"']/g,c=>({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#39;"}[c]));
const iso=d=>d.getFullYear()+"-"+String(d.getMonth()+1).padStart(2,"0")+"-"+String(d.getDate()).padStart(2,"0");
const parse=s=>{const[y,m,d]=s.split("-").map(Number);return new Date(y,m-1,d);};
const addDays=(s,k)=>{const d=parse(s);d.setDate(d.getDate()+k);return iso(d);};
const hoje=iso(new Date());
const hm=min=>{const h=Math.floor(min/60),m=Math.round(min%60);return h?h+"h"+String(m).padStart(2,"0"):m+"min";};
/* custo do carro por km (manutenção + depreciação + fixos), igual ao app */
function custoInv(){ const c=cfg.custo||{}; const man=(c.manut||[]).reduce((a,m)=>a+(num(m[2])>0?num(m[1])/num(m[2]):0),0); const ka=num(c.kmAno);
  const dep=ka>0?Math.max(0,num(c.vHoje)-num(c.vAno))/ka:0, fix=ka>0?(num(c.seguro)+num(c.ipva)+num(c.cel)+num(c.equip))/ka:0;
  return man+dep+fix>0?man+dep+fix:num(cfg.desg); }
const INV=custoInv();
const lancs=items.filter(x=>(x.kind||"lanc")==="lanc"), pnrs=items.filter(x=>x.kind==="pnr");

let per="mes", ref=hoje;
function range(){
  const d=parse(ref);
  if(per==="semana"){ const s=addDays(ref,-((d.getDay()+6)%7)); return [s,addDays(s,6)]; }
  if(per==="mes") return [iso(new Date(d.getFullYear(),d.getMonth(),1)),iso(new Date(d.getFullYear(),d.getMonth()+1,0))];
  if(per==="ano") return [d.getFullYear()+"-01-01",d.getFullYear()+"-12-31"];
  if(per==="livre") return [$("#d1").value||addDays(hoje,-29),$("#d2").value||hoje];
  const ds=items.map(x=>x.data).filter(Boolean).sort(); return [ds[0]||hoje,ds[ds.length-1]||hoje];
}
function label([f,l]){
  if(per==="mes") { const s=parse(f).toLocaleDateString("pt-BR",{month:"long",year:"numeric"}); return s[0].toUpperCase()+s.slice(1); }
  if(per==="ano") return f.slice(0,4);
  return parse(f).toLocaleDateString("pt-BR")+" a "+parse(l).toLocaleDateString("pt-BR");
}
function shift(k){ const d=parse(ref);
  if(per==="semana") ref=addDays(ref,7*k); else if(per==="mes") ref=iso(new Date(d.getFullYear(),d.getMonth()+k,1)); else if(per==="ano") ref=iso(new Date(d.getFullYear()+k,0,1));
  draw(); }

function calc(f,l){
  const inR=x=>{const d=x.data||"";return d>=f&&d<=l;};
  const L=lancs.filter(inR).sort((a,b)=>(a.data||"").localeCompare(b.data||"")||(a.ts||0)-(b.ts||0));
  const r={bruto:0,gastos:0,km:0,min:0,pct:0,valPct:0,rotas:0,dias:new Set(),porTipo:{},porGasto:{},porCd:{},porDia:{},L};
  L.forEach(x=>{ const v=num(x.valor), d=x.data; r.porDia[d]=r.porDia[d]||{b:0,g:0};
    if(GASTO[x.tipo]){ r.gastos+=v; r.porGasto[x.tipo]=(r.porGasto[x.tipo]||0)+v; r.porDia[d].g+=v; return; }
    r.bruto+=v; r.porDia[d].b+=v; r.dias.add(d); r.km+=num(x.km); r.min+=num(x.tempo);
    const ped=num(x.pedagio); if(ped){ r.gastos+=ped; r.porGasto.pedag=(r.porGasto.pedag||0)+ped; r.porDia[d].g+=ped; }
    r.porTipo[x.tipo]=(r.porTipo[x.tipo]||0)+v;
    if(x.tipo==="entrega"){ r.rotas++; if(num(x.pacotes)){ r.pct+=num(x.pacotes); r.valPct+=v; }
      const cd=(x.cd||x.cidade||"Sem CD").trim(); const c=r.porCd[cd]=r.porCd[cd]||{v:0,p:0,km:0,n:0}; c.v+=v; c.p+=num(x.pacotes); c.km+=num(x.km); c.n++; } });
  pnrs.filter(inR).forEach(p=>{ const v=num(p.desconto); if(v){ r.gastos+=v; r.porGasto.pnr=(r.porGasto.pnr||0)+v; const d=p.data; r.porDia[d]=r.porDia[d]||{b:0,g:0}; r.porDia[d].g+=v; } });
  r.lucro=r.bruto-r.gastos; r.inv=r.km*INV; r.real=r.lucro-r.inv;
  return r;
}

function chart(f,l,r){
  const days=Math.round((parse(l)-parse(f))/864e5)+1, mensal=days>62, pts=[];
  if(mensal){ const s=parse(f), e=parse(l); for(let d=new Date(s.getFullYear(),s.getMonth(),1); d<=e; d.setMonth(d.getMonth()+1)){
      const k=iso(d).slice(0,7); let b=0,g=0; Object.keys(r.porDia).forEach(x=>{ if(x.slice(0,7)===k){ b+=r.porDia[x].b; g+=r.porDia[x].g; } });
      pts.push({k:d.toLocaleDateString("pt-BR",{month:"short"}).replace(".",""),b,l:b-g}); } }
  else for(let i=0;i<days;i++){ const k=addDays(f,i), x=r.porDia[k]||{b:0,g:0}; pts.push({k:k.slice(8),b:x.b,l:x.b-x.g}); }
  if(!pts.some(p=>p.b||p.l)) return '<div class="empty">Nada lançado neste período.</div>';
  const W=Math.max(320,pts.length*22), H=220, top=Math.max(1,...pts.map(p=>Math.max(p.b,p.l))), low=Math.min(0,...pts.map(p=>p.l));
  const y=v=>12+(top-v)/(top-low)*(H-34), bw=(W-40)/pts.length;
  let s='<svg viewBox="0 0 '+W+' '+H+'" preserveAspectRatio="none" role="img" aria-label="Ganho bruto e lucro por '+(mensal?"mês":"dia")+'">';
  for(let i=0;i<=4;i++){ const v=low+(top-low)*i/4; s+='<line x1="36" x2="'+W+'" y1="'+y(v)+'" y2="'+y(v)+'" style="stroke:var(--li)" stroke-width="1"/><text x="32" y="'+(y(v)+4)+'" text-anchor="end" style="fill:var(--mu);font-size:10px">'+Math.round(v)+'</text>'; }
  pts.forEach((p,i)=>{ const x=40+i*bw;
    if(p.b) s+='<rect x="'+(x+bw*.12)+'" y="'+y(p.b)+'" width="'+(bw*.36)+'" height="'+(y(0)-y(p.b))+'" rx="2" style="fill:var(--ac)"><title>'+p.k+': bruto '+brl(p.b)+'</title></rect>';
    if(p.l) s+='<rect x="'+(x+bw*.52)+'" y="'+Math.min(y(p.l),y(0))+'" width="'+(bw*.36)+'" height="'+Math.abs(y(0)-y(p.l))+'" rx="2" style="fill:var('+(p.l<0?"--bad":"--c2")+')"><title>'+p.k+': lucro '+brl(p.l)+'</title></rect>';
    if(pts.length<=31||i%2===0) s+='<text x="'+(x+bw/2)+'" y="'+(H-6)+'" text-anchor="middle" style="fill:var(--mu);font-size:10px">'+p.k+'</text>'; });
  return s+'</svg><div class="legend"><span><i style="background:var(--ac)"></i>Ganho bruto</span><span><i style="background:var(--c2)"></i>Lucro (bruto − despesas)</span></div>';
}

function brk(obj,cls){
  const ks=Object.keys(obj).sort((a,b)=>obj[b]-obj[a]), tot=ks.reduce((a,k)=>a+obj[k],0);
  if(!ks.length) return '<p class="note">Nada neste período.</p>';
  return '<div class="brk '+(cls||"")+'">'+ks.map(k=>'<div><span>'+esc(NOMES[k]||(k==="pnr"?"PNR":k))+'</span><span><em style="width:'+(obj[k]/tot*100)+'%"></em></span><span class="num">'+brl(obj[k])+'</span></div>').join("")+'</div>';
}

let sortK="data", sortD=1, last=null;
function draw(){
  document.querySelectorAll("#per button").forEach(b=>b.setAttribute("aria-pressed",String(b.dataset.p===per)));
  $("#nav").hidden=per==="tudo"||per==="livre"; $("#livre").hidden=per!=="livre";
  const R=range(), [f,l]=R, r=calc(f,l); last=r;
  $("#lbl").textContent=label(R);
  const h=r.min/60, c=(lbl,v,sub,cls)=>'<div class="card"><small>'+lbl+'</small><b class="num '+(cls||"")+'">'+v+'</b>'+(sub?'<span>'+sub+'</span>':'')+'</div>';
  let o='<section class="hero"><div class="hero-in num">'+
    '<div class="big"><small>Lucro no período</small><b>'+brl(r.lucro)+'</b></div>'+
    '<div><small>Ganho bruto</small><b>'+brl(r.bruto)+'</b></div>'+
    '<div><small>Despesas</small><b>'+brl(r.gastos)+'</b></div>'+
    '<div><small>Lucro real</small><b>'+brl(r.real)+'</b></div></div></section>';
  o+='<p class="note">Lucro real = lucro − '+dec(r.km,0)+' km × '+brl(INV)+' (custo do carro por km: manutenção, depreciação e fixos, do jeito que está no app).</p>';
  o+='<div class="cards">'+
    c("Dias trabalhados",r.dias.size,r.dias.size?brl(r.bruto/r.dias.size)+" por dia":"")+
    c("Rotas de entrega",r.rotas,r.rotas?brl((r.porTipo.entrega||0)/r.rotas)+" por rota":"")+
    c("Pacotes",r.pct.toLocaleString("pt-BR"),r.pct?brl(r.valPct/r.pct)+" por pacote":"")+
    c("Km rodados",dec(r.km,0),r.km?brl(r.bruto/r.km)+" por km":"")+
    c("Horas",h?hm(r.min):"—",h?brl(r.bruto/h)+" por hora":"")+
    c("Lucro real por km",r.km?brl(r.real/r.km):"—","",r.km&&r.real<0?"bad":"good")+'</div>';
  o+='<section class="box chart"><h2>'+(Math.round((parse(l)-parse(f))/864e5)>62?"Por mês":"Por dia")+'</h2>'+chart(f,l,r)+'</section>';
  const cds=Object.keys(r.porCd);
  o+='<div class="cols"><section class="box"><h2>De onde veio o dinheiro</h2>'+brk(r.porTipo)+'</section><section class="box"><h2>Para onde foi</h2>'+brk(r.porGasto,"g")+'</section></div>';
  if(cds.length) o+='<section class="box"><h2>Entregas por CD</h2><div class="tw" style="border:0"><table class="cd"><thead><tr><th>CD</th><th>Rotas</th><th>Ganho</th><th>Pacotes</th><th>R$/pacote</th><th>Km</th><th>R$/km</th></tr></thead><tbody>'+
    cds.sort((a,b)=>r.porCd[b].v-r.porCd[a].v).map(k=>{const x=r.porCd[k];return '<tr><td>'+esc(k)+'</td><td>'+x.n+'</td><td>'+brl(x.v)+'</td><td>'+x.p+'</td><td>'+(x.p?brl(x.v/x.p):"—")+'</td><td>'+dec(x.km,0)+'</td><td>'+(x.km?brl(x.v/x.km):"—")+'</td></tr>';}).join("")+'</tbody></table></div></section>';
  o+='<section><h2>Lançamentos ('+r.L.length+')</h2><div class="tw"><table id="tb"><thead><tr>'+
    [["data","Data"],["tipo","Tipo"],["desc","Descrição"],["valor","Valor"],["km","Km"],["pacotes","Pacotes"],["porPct","R$/pct"],["tempo","Tempo"],["porKm","R$/km"]].map(([k,t])=>'<th data-k="'+k+'">'+t+(sortK===k?(sortD>0?" ↑":" ↓"):"")+'</th>').join("")+'</tr></thead><tbody>'+rows(r.L)+'</tbody></table></div></section>';
  $("#out").innerHTML=o;
  document.querySelectorAll("#tb th").forEach(th=>th.onclick=()=>{ const k=th.dataset.k; sortD=sortK===k?-sortD:1; sortK=k; draw(); });
}
function rowData(x){ const v=num(x.valor), km=num(x.km), p=num(x.pacotes), g=!!GASTO[x.tipo];
  return {x,data:x.data||"",tipo:NOMES[x.tipo]||x.tipo,desc:[x.cd,x.cidade,x.cliente,x.descricao,x.posto].filter(Boolean).join(" · "),valor:g?-v:v,km,pacotes:p,porPct:!g&&p?v/p:null,tempo:num(x.tempo),porKm:!g&&km?v/km:null}; }
function rows(L){
  const R=L.map(rowData).sort((a,b)=>{ const A=a[sortK],B=b[sortK]; return (A==null?-1e12:A)>(B==null?-1e12:B)?sortD:(A==null?-1e12:A)<(B==null?-1e12:B)?-sortD:0; });
  if(!R.length) return '<tr><td colspan="9" class="empty" style="text-align:center">Nada lançado neste período.</td></tr>';
  return R.map(r=>'<tr><td>'+r.data.split("-").reverse().join("/")+'</td><td>'+esc(r.tipo)+'</td><td style="max-width:280px;overflow:hidden;text-overflow:ellipsis">'+esc(r.desc)+'</td><td class="num'+(r.valor<0?" neg":"")+'">'+brl(r.valor)+'</td><td class="num">'+(r.km?dec(r.km,1):"")+'</td><td class="num">'+(r.pacotes||"")+'</td><td class="num">'+(r.porPct!=null?brl(r.porPct):"")+'</td><td class="num">'+(r.tempo?hm(r.tempo):"")+'</td><td class="num">'+(r.porKm!=null?brl(r.porKm):"")+'</td></tr>').join("");
}
function csv(){
  if(!last) return; const q=v=>{v=String(v??"");return /[;"\n]/.test(v)?'"'+v.replace(/"/g,'""')+'"':v;}, n=v=>v==null?"":String(Math.round(v*100)/100).replace(".",",");
  const L=["Data;Tipo;Descrição;Valor;Km;Pacotes;R$/pacote;Minutos;R$/km"].concat(last.L.map(rowData).map(r=>[r.data.split("-").reverse().join("/"),r.tipo,r.desc,n(r.valor),n(r.km),r.pacotes||"",n(r.porPct),r.tempo||"",n(r.porKm)].map(q).join(";")));
  L.push("",["TOTAL","Ganho bruto","",n(last.bruto)].join(";"),["","Despesas","",n(-last.gastos)].join(";"),["","Lucro","",n(last.lucro)].join(";"),["","Lucro real","",n(last.real)].join(";"));
  const a=document.createElement("a"); a.href=URL.createObjectURL(new Blob(["﻿"+L.join("\n")],{type:"text/csv"})); a.download="faturamento-"+range().join("-a-")+".csv"; document.body.appendChild(a); a.click(); a.remove();
}
document.querySelectorAll("#per button").forEach(b=>b.onclick=()=>{ per=b.dataset.p; ref=hoje; if(per==="livre"&&!$("#d1").value){ $("#d1").value=addDays(hoje,-29); $("#d2").value=hoje; } draw(); });
$("#prev").onclick=()=>shift(-1); $("#next").onclick=()=>shift(1);
$("#d1").onchange=draw; $("#d2").onchange=draw; $("#csv").onclick=csv;
draw();
})();
</script>
</body></html>
<?php
}
