/* Rota Paga — lógica do app. Roda dentro do app Android (window.RotaApp existe) ou num navegador comum. */
(function(){
"use strict";

/* ================= ícones ================= */
const S=p=>'<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true">'+p+'</svg>';
const I={
  car:S('<path d="M5 16V12l2-5h10l2 5v4"/><path d="M3 16h18v2H3z"/><circle cx="7.5" cy="16.5" r="1.6"/><circle cx="16.5" cy="16.5" r="1.6"/><path d="M5 12h14"/>'),
  carx:S('<path d="M3 15v-3l2-5h9l3 5h3v3"/><path d="M2 15h20v2H2z"/><circle cx="7" cy="16" r="1.6"/><circle cx="17" cy="16" r="1.6"/><path d="M3 12h17"/>'),
  box:S('<path d="M3 7.5 12 3l9 4.5v9L12 21l-9-4.5z"/><path d="M3 7.5 12 12l9-4.5M12 12v9"/><path d="m7.5 5.3 9 4.5"/>'),
  boxx:S('<path d="M3 7.5 12 3l9 4.5v5"/><path d="M3 7.5v9L12 21v-9L3 7.5M12 12l9-4.5"/><path d="m16 16 5 5M21 16l-5 5"/>'),
  truck:S('<path d="M2 6h12v10H2zM14 10h4l3 3v3h-7z"/><circle cx="6" cy="17.5" r="1.8"/><circle cx="17" cy="17.5" r="1.8"/>'),
  case:S('<rect x="3" y="7" width="18" height="13" rx="2"/><path d="M9 7V5h6v2M3 12h18"/>'),
  fuel:S('<path d="M4 21V5a2 2 0 0 1 2-2h6a2 2 0 0 1 2 2v16M3 21h12M6 8h6"/><path d="M14 11h2a2 2 0 0 1 2 2v4a1.5 1.5 0 0 0 3 0V8l-3-3"/>'),
  wrench:S('<path d="M14.5 6.5a4 4 0 0 0 5 5L21 13l-8 8-3-3 8-8-1.5-1.5a4 4 0 0 1-5-5L8 7 4 3 3 4l4 4"/>'),
  food:S('<path d="M7 3v8M5 3v5a2 2 0 0 0 4 0V3M7 11v10M17 3c-2 1-3 4-3 7h3v11"/>'),
  coin:S('<circle cx="12" cy="12" r="8"/><path d="M14.5 9.5c-.5-1-1.5-1.5-2.5-1.5-1.4 0-2.5.8-2.5 2s1 1.6 2.5 2 2.5.8 2.5 2-1.1 2-2.5 2c-1 0-2-.5-2.5-1.5M12 6.5V8M12 16v1.5"/>'),
  dots:S('<circle cx="6" cy="12" r="1.4"/><circle cx="12" cy="12" r="1.4"/><circle cx="18" cy="12" r="1.4"/>'),
  up:S('<path d="m3 17 6-6 4 4 8-8"/><path d="M15 7h6v6"/>'),
  down:S('<path d="m3 7 6 6 4-4 8 8"/><path d="M15 17h6v-6"/>'),
  pin:S('<path d="M12 21s-7-6.5-7-12a7 7 0 0 1 14 0c0 5.5-7 12-7 12z"/><circle cx="12" cy="9" r="2.5"/>'),
  calc:S('<rect x="5" y="3" width="14" height="18" rx="2"/><path d="M8 7h8M8 11h2M12 11h2M16 11v6M8 15h2M12 15h2M8 18h6"/>'),
  camera:S('<path d="M4 8h3l2-3h6l2 3h3v11H4z"/><circle cx="12" cy="13" r="3.5"/>'),
  home:S('<path d="M4 11 12 4l8 7v9h-5v-6H9v6H4z"/>'),
  map:S('<path d="m3 6 6-3 6 3 6-3v15l-6 3-6-3-6 3z"/><path d="M9 3v15M15 6v15"/>'),
  list:S('<rect x="5" y="4" width="14" height="17" rx="2"/><path d="M9 3h6v3H9zM9 11h6M9 15h6"/>'),
  gear:S('<circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.7 1.7 0 0 0 .3 1.8l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.7 1.7 0 0 0-1.8-.3 1.7 1.7 0 0 0-1 1.5V21a2 2 0 1 1-4 0v-.1a1.7 1.7 0 0 0-1.1-1.5 1.7 1.7 0 0 0-1.8.3l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1a1.7 1.7 0 0 0 .3-1.8 1.7 1.7 0 0 0-1.5-1H3a2 2 0 1 1 0-4h.1a1.7 1.7 0 0 0 1.5-1.1 1.7 1.7 0 0 0-.3-1.8l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1a1.7 1.7 0 0 0 1.8.3H9a1.7 1.7 0 0 0 1-1.5V3a2 2 0 1 1 4 0v.1a1.7 1.7 0 0 0 1 1.5 1.7 1.7 0 0 0 1.8-.3l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1.7 1.7 0 0 0-.3 1.8V9a1.7 1.7 0 0 0 1.5 1H21a2 2 0 1 1 0 4h-.1a1.7 1.7 0 0 0-1.5 1z"/>'),
  plus:S('<path d="M12 5v14M5 12h14" stroke-width="2.4"/>'),
  nav:S('<path d="M3 11 21 3l-8 18-2-8z"/>'),
  clock:S('<circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/>'),
  timer:S('<circle cx="12" cy="13" r="8"/><path d="M12 9v4l2.5 2M9 2h6M12 2v3"/>'),
  gauge:S('<path d="M4 18a9 9 0 1 1 16 0"/><path d="m12 14 4-5"/>'),
  user:S('<circle cx="12" cy="8" r="4"/><path d="M4 21c1-4 4-6 8-6s7 2 8 6"/>'),
  route:S('<circle cx="6" cy="19" r="2"/><circle cx="18" cy="5" r="2"/><path d="M8 19h8a3 3 0 0 0 0-6H8a3 3 0 0 1 0-6h8"/>'),
  sheet:S('<rect x="4" y="3" width="16" height="18" rx="2"/><path d="M4 9h16M4 15h16M10 3v18"/>'),
  bell:S('<path d="M6 16V11a6 6 0 0 1 12 0v5l2 2H4z"/><path d="M10 20a2 2 0 0 0 4 0"/>'),
  cal:S('<rect x="3" y="5" width="18" height="16" rx="2"/><path d="M3 10h18M8 3v4M16 3v4"/><path d="m9 15 2 2 4-4"/>'),
  target:S('<circle cx="12" cy="12" r="7"/><circle cx="12" cy="12" r="2.5"/><path d="M12 2v3M12 19v3M2 12h3M19 12h3"/>'),
  expand:S('<path d="M4 9V4h5M20 9V4h-5M4 15v5h5M20 15v5h-5"/>')
};
const paintIcons=root=>(root||document).querySelectorAll("[data-i]").forEach(e=>{ if(!e.firstChild) e.innerHTML=I[e.dataset.i]||""; });
paintIcons();

/* ================= ponte com o Android ================= */
const N=window.RotaApp||null;
function nat(fn,...a){ try{ return N&&typeof N[fn]==="function"?N[fn](...a):null; }catch(e){ return null; } }
function natJ(fn,...a){ const r=nat(fn,...a); if(!r) return null; try{ return JSON.parse(r); }catch(e){ return null; } }

/* ================= definições ================= */
const MOTIVOS=["Cliente diz que não recebeu","Pacote avariado","Extravio","Endereço errado","Outro"];
const STATUS_PNR=["Aberto","Contestado","Resolvido"];
const F={
  data:{l:"Data",t:"date",full:1}, cliente:{l:"Nome do cliente",t:"text",full:1},
  cidade:{l:"Cidade",t:"text",list:1}, cd:{l:"CD",t:"text",list:1},
  descricao:{l:"Descrição",t:"text",full:1}, posto:{l:"Posto",t:"text",full:1,list:1},
  valor:{l:"Valor (R$)",t:"num",ph:"150,00"}, km:{l:"Km",t:"num",ph:"32"},
  paradas:{l:"Paradas",t:"int",ph:"20"}, pacotes:{l:"Pacotes",t:"int",ph:"48"},
  cons:{l:"Cons. km/L",t:"num"}, tempo:{l:"Tempo (min)",t:"int",ph:"240"},
  pedagio:{l:"Pedágio (R$)",t:"num",ph:"0"}, embutidas:{l:"Corridas embutidas",t:"int",ph:"0"},
  litros:{l:"Litros",t:"num",ph:"40"}, codigo:{l:"Código do pacote",t:"text",full:1},
  endereco:{l:"Endereço",t:"text",full:1}, motivo:{l:"Motivo",t:"select",opts:MOTIVOS,full:1},
  desconto:{l:"Desconto (R$)",t:"num",ph:"0"}, status:{l:"Situação",t:"select",opts:STATUS_PNR},
  obs:{l:"Observação",t:"text",full:1}, hora:{l:"Saída",t:"text",ph:"07:00"}
};
const T={
  uber:{n:"Uber",i:"car",f:["data","cliente","valor","km","cons","tempo","pedagio","embutidas"]},
  uberx:{n:"Uber X",i:"carx",f:["data","cliente","valor","km","cons","tempo","pedagio","embutidas"]},
  entrega:{n:"Entrega",i:"box",f:["data","cidade","cd","valor","km","paradas","pacotes","cons","tempo","pedagio","embutidas"]},
  frete:{n:"Frete",i:"truck",f:["data","cliente","valor","km","cons","tempo","pedagio"]},
  extra:{n:"Extra",i:"case",f:["data","descricao","valor"]},
  abast:{n:"Abastecimento",i:"fuel",g:1,f:["data","posto","litros","valor"]},
  manut:{n:"Manutenção",i:"wrench",g:1,f:["data","descricao","valor"]},
  alim:{n:"Alimentação",i:"food",g:1,f:["data","descricao","valor"]},
  pedag:{n:"Pedágio",i:"coin",g:1,f:["data","descricao","valor"]},
  outros:{n:"Outros gastos",i:"dots",g:1,f:["data","descricao","valor"]},
  pnr:{n:"PNR",i:"boxx",kind:"pnr",f:["data","codigo","endereco","motivo","desconto","status","obs"]},
  parada:{n:"Parada",i:"pin",kind:"parada",f:["endereco","pacotes","obs"]},
  agenda:{n:"Rota agendada",i:"cal",kind:"agenda",f:["data","hora","cd","cidade","valor","pacotes","km","paradas","obs"]}
};
const GANHOS=["uber","uberx","entrega","frete","extra"], GASTOS=["abast","manut","alim","pedag","outros"];
const VEIC={bike:{cons:0,desg:.03},moto:{cons:35,desg:.12},carro:{cons:11,desg:.25},util:{cons:8,desg:.38},caminhao:{cons:4,desg:.9}};
const DEF_CFG={nome:"",gas:6.29,cons:11,desg:.25,ruimPct:2.2,metaPct:2.8,ruimKm:1.4,metaKm:1.8,ruimHora:25,metaHora:35,nav:"gmaps",tParada:3,veiculo:"carro",cidade:"",casa:"",trabalho:"",tema:"sistema",
  alertOn:true,overlayOn:true,bubbleOn:false,alertWords:"rota, envio extra, envios extra, nova oferta, pacotes",alertApps:[],alertSecs:10};

/* ================= estado e utilidades ================= */
let items=[], cfg={...DEF_CFG}, view="inicio", hP="mes", hRef="", hTab="lanc";
const $=s=>document.querySelector(s);
const esc=s=>String(s??"").replace(/[&<>"']/g,c=>({"&":"&amp;","<":"&lt;",">":"&gt;",'"':"&quot;","'":"&#39;"}[c]));
function num(v){ if(v==null||v==="") return 0; if(typeof v==="number") return isFinite(v)?v:0; let s=String(v).trim().replace(/[^\d.,-]/g,""); if(s.includes(",")) s=s.replace(/\./g,"").replace(",","."); const n=parseFloat(s); return isFinite(n)?n:0; }
const brl=n=>n.toLocaleString("pt-BR",{style:"currency",currency:"BRL"});
const brl0=n=>(n<0?"−":"")+"R$ "+Math.abs(Math.round(n)).toLocaleString("pt-BR");
const dec=(n,d=1)=>n.toLocaleString("pt-BR",{minimumFractionDigits:d,maximumFractionDigits:d});
const fmtCfg=v=>String(v).replace(".",",");
const iso=d=>d.getFullYear()+"-"+String(d.getMonth()+1).padStart(2,"0")+"-"+String(d.getDate()).padStart(2,"0");
const parse=s=>{const [y,m,d]=s.split("-").map(Number); return new Date(y,m-1,d);};
const today=()=>iso(new Date());
const addDays=(s,k)=>{const d=parse(s); d.setDate(d.getDate()+k); return iso(d);};
const cap=s=>s.charAt(0).toUpperCase()+s.slice(1);
function dayLabel(s){ const t=today(); if(s===t) return "Hoje"; if(s===addDays(t,-1)) return "Ontem"; if(s===addDays(t,1)) return "Amanhã"; return parse(s).toLocaleDateString("pt-BR",{weekday:"short",day:"2-digit",month:"short"}); }
function hm(min){ if(!min) return "0min"; const h=Math.floor(min/60), m=Math.round(min%60); return h?(h+"h"+(m?String(m).padStart(2,"0"):"")):m+"min"; }
function hms(ms){ const s=Math.max(0,Math.floor(ms/1000)); return Math.floor(s/3600)+":"+String(Math.floor(s/60)%60).padStart(2,"0")+":"+String(s%60).padStart(2,"0"); }
function toast(msg){ const t=$("#toast"); t.textContent=msg; t.hidden=false; clearTimeout(toast.h); toast.h=setTimeout(()=>t.hidden=true,2600); }
const lsGet=(k,d)=>{ try{ const v=localStorage.getItem(k); return v?JSON.parse(v):d; }catch(e){ return d; } };
const lsSet=(k,v)=>{ try{ localStorage.setItem(k,JSON.stringify(v)); }catch(e){} };
const kindOf=x=>x.kind||"lanc";
const isG=t=>!!(T[t]&&T[t].g);
const lancs=()=>items.filter(x=>kindOf(x)==="lanc");
const pnrs=()=>items.filter(x=>kindOf(x)==="pnr");
const stops=()=>items.filter(x=>kindOf(x)==="parada");
const agendas=()=>items.filter(x=>kindOf(x)==="agenda");
const newId=()=>Date.now().toString(36)+Math.random().toString(36).slice(2,7);
const sleep=ms=>new Promise(r=>setTimeout(r,ms));

/* ================= fotos (IndexedDB, só neste aparelho) ================= */
const Fotos=(()=>{
  let dbp=null; const urls={};
  function db(){ if(!dbp) dbp=new Promise((res,rej)=>{ try{ const r=indexedDB.open("rotapro",1); r.onupgradeneeded=()=>r.result.createObjectStore("fotos"); r.onsuccess=()=>res(r.result); r.onerror=()=>rej(r.error); }catch(e){ rej(e); } }); return dbp; }
  async function tx(mode,fn){ const d=await db(); return new Promise((res,rej)=>{ const t=d.transaction("fotos",mode); const s=t.objectStore("fotos"); const q=fn(s); t.oncomplete=()=>res(q&&q.result); t.onerror=()=>rej(t.error); }); }
  return {
    put:(id,blob)=>tx("readwrite",s=>s.put(blob,id)),
    del:id=>tx("readwrite",s=>s.delete(id)).catch(()=>{}),
    async url(id){ if(urls[id]) return urls[id]; try{ const b=await tx("readonly",s=>s.get(id)); if(!b) return ""; return urls[id]=URL.createObjectURL(b); }catch(e){ return ""; } },
    hydrate(root){ (root||document).querySelectorAll("img[data-foto]").forEach(async img=>{ const u=await Fotos.url(img.dataset.foto); if(u) img.src=u; }); }
  };
})();
async function shrink(file){
  try{
    const bmp=await createImageBitmap(file); const k=Math.min(1,1400/Math.max(bmp.width,bmp.height));
    const c=document.createElement("canvas"); c.width=Math.round(bmp.width*k); c.height=Math.round(bmp.height*k);
    c.getContext("2d").drawImage(bmp,0,0,c.width,c.height);
    return await new Promise(r=>c.toBlob(b=>r(b||file),"image/jpeg",.8));
  }catch(e){ return file; }
}
function pickFile(accept){
  return new Promise(res=>{ const inp=document.createElement("input"); inp.type="file"; inp.accept=accept;
    inp.onchange=()=>res(inp.files&&inp.files[0]||null); inp.click(); });
}
async function pickPhoto(){
  const f=await pickFile("image/*"); if(!f) return null;
  try{ const b=await shrink(f); const id="f_"+newId(); await Fotos.put(id,b); toast("Foto anexada"); return id; }
  catch(e){ toast("Não deu para guardar a foto."); return null; }
}

/* ================= contas ================= */
/* Custo real por km, nos 5 passos da planilha "Custo por KM":
   combustível + manutenção + depreciação + custos fixos anuais. "inv" é o custo que não aparece no dia (tudo menos o combustível). */
const MANUT_DEF=[["Troca de óleo + filtro + aditivos","",""],["Pneus (jogo)","",""],["Palhetas / lâmpadas","",""],["Freios (pastilhas/lonas)","",""],["Revisões / imprevistos","",""]];
function custoKm(c){
  c=c||cfg.custo||{};
  const comb=cfg.cons?cfg.gas/cfg.cons:0;
  const man=(c.manut||[]).reduce((a,m)=>a+(num(m[2])>0?num(m[1])/num(m[2]):0),0);
  const ka=num(c.kmAno);
  const dep=ka>0?Math.max(0,num(c.vHoje)-num(c.vAno))/ka:0;
  const fix=ka>0?(num(c.seguro)+num(c.ipva)+num(c.cel)+num(c.equip))/ka:0;
  const set=man+dep+fix>0, inv=set?man+dep+fix:num(cfg.desg);
  return {comb,man,dep,fix,inv,set,total:comb+inv};
}
function metrics(it){
  const v=num(it.valor), km=num(it.km), pct=num(it.pacotes), par=num(it.paradas), min=num(it.tempo), ped=num(it.pedagio);
  const cons=num(it.cons)||cfg.cons, ck=custoKm();
  const comb=km>0&&cons>0?km/cons*cfg.gas:0, inv=km*ck.inv, custo=comb+inv;
  const lucro=v-custo-ped; /* lucro real: já desconta o desgaste do carro */
  return {v,km,comb,inv,custo,ped,lucro,pacotes:pct,
    porPct:pct>0?v/pct:null, porParada:par>0?v/par:null, porKm:km>0?v/km:null,
    porHora:min>0?v/(min/60):null, lucroHora:min>0?lucro/(min/60):null};
}
const GK={pct:["ruimPct","metaPct"],km:["ruimKm","metaKm"],hora:["ruimHora","metaHora"]};
/* semáforo: abaixo de "ruim" é vermelho, a partir de "bom" é verde, no meio é amarelo */
function grade(val,k){ const g=GK[k]; if(val==null||!g) return null; const ruim=num(cfg[g[0]]), bom=num(cfg[g[1]]); if(!bom&&!ruim) return null;
  if(bom&&val>=bom) return "good"; if(ruim&&val<ruim) return "bad"; return bom?"warn":"good"; }
function verdict(tipo,m){
  const g=tipo==="entrega"?[grade(m.porPct,"pct"),grade(m.porKm,"km")]:[grade(m.porKm,"km"),grade(m.porHora,"hora")];
  const k=g.filter(Boolean); if(!k.length) return "none";
  return k.includes("bad")?"bad":k.includes("warn")?"warn":"good";
}
const VERD={good:"Compensa",warn:"No limite",bad:"Abaixo da meta",none:"Preencha valor e km"};
function sums(from,to){
  const inR=x=>{const d=x.data||today(); return d>=from&&d<=to;};
  const r={ganhos:0,despesas:0,km:0,min:0,pct:0,valPct:0,par:0,valPar:0,litros:0,n:0,pnr:0};
  lancs().filter(inR).forEach(x=>{
    const v=num(x.valor); r.n++;
    if(isG(x.tipo)){ r.despesas+=v; if(x.tipo==="abast") r.litros+=num(x.litros); return; }
    r.ganhos+=v; r.despesas+=num(x.pedagio); r.km+=num(x.km); r.min+=num(x.tempo);
    if(x.tipo==="entrega"){ if(num(x.pacotes)){ r.pct+=num(x.pacotes); r.valPct+=v; } if(num(x.paradas)){ r.par+=num(x.paradas); r.valPar+=v; } }
  });
  pnrs().filter(inR).forEach(p=>{ r.despesas+=num(p.desconto); r.pnr++; });
  r.lucro=r.ganhos-r.despesas;
  return r;
}

/* ================= gravação ================= */
function persist(){ lsSet("rp_items",items); lsSet("rp_cfg",cfg); }
function saveDoc(o,quiet){ const i=items.findIndex(x=>x.id===o.id); if(i>=0) items[i]=o; else items.push(o); persist(); if(!quiet) renderAll(); }
function delDoc(id){ const o=items.find(x=>x.id===id); if(o&&o.foto&&!items.some(x=>x!==o&&x.foto===o.foto)) Fotos.del(o.foto); items=items.filter(x=>x.id!==id); persist(); renderAll(); }
function saveCfg(){ persist(); applyTema(); pushAlertCfg(); renderAll(); }

/* ================= tema ================= */
const mqDark=window.matchMedia?matchMedia("(prefers-color-scheme: dark)"):null;
function isDark(){ return cfg.tema==="escuro"||(cfg.tema==="sistema"&&mqDark&&mqDark.matches); }
function applyTema(){
  const r=document.documentElement;
  if(cfg.tema==="claro") r.setAttribute("data-theme","light"); else if(cfg.tema==="escuro") r.setAttribute("data-theme","dark"); else r.removeAttribute("data-theme");
  r.setAttribute("data-dark",isDark()?"1":"0");
  const bg=getComputedStyle(r).getPropertyValue("--bg").trim();
  nat("setBars",bg,isDark());
}
if(mqDark&&mqDark.addEventListener) mqDark.addEventListener("change",applyTema);

/* ================= navegação externa ================= */
function fullAddr(a){ a=String(a||"").trim(); if(cfg.cidade && !a.toLowerCase().includes(cfg.cidade.toLowerCase().split(/[-,]/)[0].trim())) a+=", "+cfg.cidade; return a; }
const ll=s=>s.lat+","+s.lon;
function navLink(s){
  const dest=s.lat?ll(s):fullAddr(s.endereco);
  if(cfg.nav==="waze") return s.lat?"https://waze.com/ul?ll="+ll(s)+"&navigate=yes":"https://waze.com/ul?q="+encodeURIComponent(dest)+"&navigate=yes";
  return "https://www.google.com/maps/dir/?api=1&travelmode=driving&destination="+encodeURIComponent(dest);
}
function gmapsRoute(list,origin){
  const pts=list.slice(0,10).map(s=>s.lat?ll(s):fullAddr(s.endereco));
  if(!pts.length) return null;
  const dest=pts.pop(), p=["api=1","travelmode=driving"];
  if(origin) p.push("origin="+origin[0]+","+origin[1]);
  p.push("destination="+encodeURIComponent(dest));
  if(pts.length) p.push("waypoints="+encodeURIComponent(pts.join("|")));
  return "https://www.google.com/maps/dir/?"+p.join("&");
}
function openExt(url){ if(N&&nat("openUrl",url)!==null) return; window.open(url,"_blank","noopener"); }

/* ================= abas ================= */
const VIEWS=["inicio","rota","historico","ajustes"];
function show(v){
  view=VIEWS.includes(v)?v:"inicio";
  VIEWS.forEach(n=>$("#v-"+n).hidden=n!==view);
  document.querySelectorAll(".tab").forEach(t=>{ if(t.dataset.tab===view) t.setAttribute("aria-current","page"); else t.removeAttribute("aria-current"); });
  window.scrollTo(0,0);
  if(view==="rota") Mapa.show();
}
function go(v){ if(location.hash!=="#"+v) location.hash=v; else show(v); }
function onHash(){
  const h=location.hash.replace("#","");
  if(h==="alerta"){ show("inicio"); showDetect(); return; }
  show(h||"inicio");
}
function renderAll(){ renderInicio(); renderRota(); renderHist(); renderAjustes(); }

/* ---------- Início ---------- */
function lancRow(it){
  const t=T[it.tipo]||T.extra, m=metrics(it), meta=[], g=isG(it.tipo);
  if(it.tipo==="entrega"){ if(m.porPct!=null) meta.push(brl(m.porPct)+"/pct"); if(m.pacotes) meta.push(m.pacotes+" pct"); if(m.km) meta.push(dec(m.km,0)+" km"); if(num(it.tempo)) meta.push(hm(num(it.tempo))); }
  else if(it.tipo==="abast"){ const l=num(it.litros); if(l){ meta.push(dec(l,1)+" L"); meta.push(brl(num(it.valor)/l)+"/L"); } }
  else if(!g && it.tipo!=="extra"){ if(m.porKm!=null) meta.push(brl(m.porKm)+"/km"); if(m.porHora!=null) meta.push(brl0(m.porHora)+"/h"); }
  const v=verdict(it.tipo,m);
  const dot=(!g&&it.tipo!=="extra"&&v!=="none")?'<span class="dot '+v+'"></span>':"";
  const name=it.cliente||(it.cidade?it.cidade+(it.cd?" · "+it.cd:""):"")||it.descricao||it.posto||"";
  return '<button class="item" type="button" data-edit="'+esc(it.id)+'"><span class="ico'+(g?" neg":"")+'">'+I[t.i]+'</span>'+
    '<span style="min-width:0"><span class="t1">'+esc(t.n)+(name?" · "+esc(name):"")+'</span><span class="t2 num">'+dot+esc(meta.join(" · ")||(it.foto?"Com comprovante":"—"))+'</span></span>'+
    '<span class="amt num'+(g?" neg":"")+'">'+(g?"−":"")+brl(num(it.valor))+'</span></button>';
}
function pnrRow(p){
  const st=p.status||"Aberto", c=st==="Resolvido"?"good":st==="Contestado"?"warn":"bad";
  return '<button class="item" type="button" data-edit="'+esc(p.id)+'"><span class="ico neg">'+I.boxx+'</span>'+
    '<span style="min-width:0"><span class="t1">'+esc(p.codigo||"PNR")+'</span><span class="t2">'+esc(p.motivo||"")+(p.endereco?" · "+esc(p.endereco):"")+'</span></span>'+
    '<span style="display:flex;flex-direction:column;align-items:flex-end;gap:4px"><span class="chip '+c+'">'+esc(st)+'</span>'+(num(p.desconto)?'<span class="amt num neg" style="font-size:18px">−'+brl(num(p.desconto))+'</span>':'')+'</span></button>';
}
function agRow(a){
  const m=metrics(a), v=verdict("entrega",m), t=today(), meta=[];
  if(a.hora) meta.push("saída "+a.hora); if(a.cd) meta.push(a.cd); if(m.pacotes) meta.push(m.pacotes+" pct"); if(m.porPct!=null) meta.push(brl(m.porPct)+"/pct");
  const can=(a.data||t)<=t&&!Trip.st.running;
  return '<div class="stop"><span class="ico">'+I.cal+'</span><button class="main" type="button" data-edit="'+esc(a.id)+'"><span class="t1">'+esc(dayLabel(a.data||t))+' · '+brl(num(a.valor))+'</span><span class="t2 num">'+(v!=="none"?'<span class="dot '+v+'"></span>':'')+esc(meta.join(" · ")||"—")+'</span></button>'+
    (can?'<button class="go" type="button" data-agstart="'+esc(a.id)+'">'+I.timer+'Iniciar</button>':'<span></span>')+'</div>';
}
function startAgenda(id){ const a=items.find(x=>x.id===id); if(!a) return; lsSet("rp_trip_agenda",id); Trip.start(a.valor,a.pacotes); closeSheet(); toast("Cronômetro ligado"); }
function emptyBox(icon,title,text){ return '<div class="panel empty"><div class="ico">'+I[icon]+'</div><b>'+title+'</b>'+text+'</div>'; }

function renderInicio(){
  $("#hello").textContent="Olá, "+(cfg.nome||"motorista");
  $("#today").textContent=cap(new Date().toLocaleDateString("pt-BR",{weekday:"long",day:"numeric",month:"long"}));
  const t=today(), d=sums(t,t);
  $("#sgNet").textContent=brl(d.lucro);
  $("#sgIn").textContent=brl0(d.ganhos); $("#sgOut").textContent=brl0(d.despesas);
  $("#sgPkg").textContent=String(d.pct); $("#sgPct").textContent=d.pct?dec(d.valPct/d.pct,2):"—";
  $("#startCard").hidden=Trip.st.running;

  const first=t.slice(0,8)+"01", dt=parse(first), dim=new Date(dt.getFullYear(),dt.getMonth()+1,0).getDate();
  const m=sums(first,t.slice(0,8)+String(dim).padStart(2,"0"));
  $("#mTot").textContent=brl(m.lucro);
  $("#mTot").style.color=m.lucro<0?"var(--bad)":"var(--good)";
  $("#mLbl").textContent=cap(dt.toLocaleDateString("pt-BR",{month:"long",year:"numeric"}));
  const vals=[]; for(let i=1;i<=dim;i++){ const s=t.slice(0,8)+String(i).padStart(2,"0"); vals.push(sums(s,s).lucro); }
  const max=Math.max(1,...vals.map(Math.abs)), W=dim*10, H=100, hasNeg=vals.some(v=>v<0), base=hasNeg?H/2:H-2, span=hasNeg?H/2-4:H-6;
  let svg='<svg viewBox="0 0 '+W+' '+H+'" preserveAspectRatio="none" role="img" aria-label="Lucro por dia do mês">';
  svg+='<line x1="0" x2="'+W+'" y1="'+base+'" y2="'+base+'" style="stroke:var(--line)" stroke-width="1" vector-effect="non-scaling-stroke"/>';
  const todayN=Number(t.slice(8));
  vals.forEach((v,i)=>{ if(!v) return; const h=Math.max(2,Math.abs(v)/max*span); const y=v>=0?base-h:base;
    svg+='<rect x="'+(i*10+2)+'" y="'+y+'" width="6" height="'+h+'" rx="1.5" style="fill:var('+(v<0?"--bad":"--accent")+');opacity:'+(i+1===todayN?1:.75)+'"/>'; });
  if(!vals.some(Boolean)) svg+='<text x="'+W/2+'" y="'+(H/2+4)+'" text-anchor="middle" style="fill:var(--muted);font-size:9px;font-family:var(--f-body)">Sem lançamentos neste mês ainda</text>';
  $("#mChart").innerHTML=svg+'</svg>';
  $("#mAxis").innerHTML=[1,8,15,22,dim].map(n=>"<span>"+n+"</span>").join("");

  const ag=agendas().filter(a=>(a.data||t)>=t).sort((a,b)=>((a.data||"")+(a.hora||"")).localeCompare((b.data||"")+(b.hora||"")));
  $("#agList").innerHTML=ag.length?ag.map(agRow).join(""):
    '<div class="panel empty"><b>Nenhuma rota agendada</b>Aceitou uma rota para amanhã? Agende aqui. No dia, é só tocar em Iniciar.</div>';
  const pend=stopsToday().filter(s=>(s.status||"pendente")==="pendente");
  $("#nextStops").innerHTML=pend.length?pend.slice(0,3).map(stopRow).join(""):
    emptyBox("pin","Nenhuma parada ainda","Monte a rota do dia na aba Rota para ver as próximas entregas aqui.");
  const last=lancs().slice().sort((a,b)=>(b.data||"").localeCompare(a.data||"")||(b.ts||0)-(a.ts||0)).slice(0,4);
  $("#lastL").innerHTML=last.length?last.map(lancRow).join(""):
    '<div class="panel empty"><b>Nenhum lançamento ainda</b>Toque no + para registrar uma rota, corrida ou gasto.<div class="example num">Exemplo de entrega: R$ 150,00 ÷ 48 pacotes = <b style="display:inline">R$ 3,13 por pacote</b></div></div>';
}

/* ---------- Rota (lista) ---------- */
function stopsToday(){ const t=today(); return stops().filter(s=>(s.data||t)===t||(s.status||"pendente")==="pendente").sort((a,b)=>(a.ordem||0)-(b.ordem||0)); }
function stopRow(s){
  const list=stopsToday(); const n=list.indexOf(s)+1;
  const st=s.status||"pendente", cls=st==="entregue"?" done":st==="falha"?" fail":"";
  const meta=[]; if(num(s.pacotes)) meta.push(num(s.pacotes)+" pct"); if(st==="entregue") meta.push("Entregue"); if(st==="falha") meta.push("Não entregue"); if(s.obs) meta.push(s.obs); if(s.foto) meta.push("Com foto");
  const geo=st==="pendente"&&!s.lat?(s.geo==="fail"?'<span class="nolc">Não achei no mapa · toque para marcar</span>':'<span class="muted">Procurando no mapa…</span>'):"";
  return '<div class="stop'+cls+'"><span class="n num">'+(st==="entregue"?"✓":st==="falha"?"✕":n)+'</span>'+
    '<button class="main" type="button" data-edit="'+esc(s.id)+'"><span class="t1">'+esc(s.endereco)+'</span><span class="t2">'+(geo||esc(meta.join(" · ")||"Toque para marcar"))+(geo&&meta.length?" · "+esc(meta.join(" · ")):"")+'</span></button>'+
    (st==="pendente"?'<button class="go" type="button" data-nav="'+esc(s.id)+'">'+I.nav+'Ir</button>':'<span></span>')+'</div>';
}
function renderRota(){
  const list=stopsToday(), pend=list.filter(s=>(s.status||"pendente")==="pendente"), done=list.filter(s=>s.status==="entregue");
  $("#rsPar").textContent=list.length;
  $("#rsPkg").textContent=list.reduce((a,s)=>a+num(s.pacotes),0);
  $("#rsDone").textContent=done.length;
  const r=Rotas.chosen;
  $("#rsTime").textContent=pend.length?hm((r&&r.min?r.min:0)+pend.length*cfg.tParada):"—";
  $("#stops").innerHTML=list.length?list.map(stopRow).join(""):
    emptyBox("pin","Adicione as primeiras paradas","Digite um endereço, cole a lista inteira ou toque no alfinete para marcar no mapa.");
  $("#stopActions").hidden=!list.length;
  $("#routeBox").hidden=pend.filter(s=>s.lat).length<1;
  Rotas.renderCards();
  Mapa.draw();
  Geo.kick();
}
function nextOrdem(){ return Math.max(0,...stops().map(s=>s.ordem||0))+1; }
function addStops(arr){
  let o=nextOrdem();
  arr.forEach(x=>{ saveDoc({id:newId(),kind:"parada",endereco:x.endereco,pacotes:x.pacotes||"",lat:x.lat,lon:x.lon,status:"pendente",ordem:o++,data:today(),ts:Date.now()},true); });
  Rotas.reset(); renderAll();
}
function moveStop(id,dir){
  const list=stopsToday(), i=list.findIndex(s=>s.id===id), j=i+dir;
  if(i<0||j<0||j>=list.length) return;
  list.forEach((s,k)=>s.ordem=k+1);
  const a=list[i]; list[i]=list[j]; list[j]=a;
  list.forEach((s,k)=>{ s.ordem=k+1; saveDoc(s,true); });
  Rotas.reset(); renderAll();
}

/* ---------- geocodificação (OpenStreetMap / Nominatim, 1 por segundo) ---------- */
const Geo=(()=>{
  let busy=false;
  async function search(q){
    const u="https://nominatim.openstreetmap.org/search?format=jsonv2&limit=1&countrycodes=br&accept-language=pt-BR&q="+encodeURIComponent(q);
    const r=await fetch(u); if(!r.ok) throw new Error("http "+r.status); const j=await r.json();
    return j&&j[0]?{lat:+j[0].lat,lon:+j[0].lon}:null;
  }
  async function reverse(lat,lon){
    try{ const r=await fetch("https://nominatim.openstreetmap.org/reverse?format=jsonv2&zoom=18&accept-language=pt-BR&lat="+lat+"&lon="+lon);
      const j=await r.json(); const a=j.address||{};
      const s=[a.road||a.pedestrian||a.footway,a.house_number].filter(Boolean).join(", "), b=a.suburb||a.neighbourhood||a.city_district||"";
      return (s||j.name||"Ponto no mapa")+(b?" - "+b:""); }catch(e){ return "Ponto no mapa ("+dec(lat,5)+", "+dec(lon,5)+")"; }
  }
  async function kick(){
    if(busy) return; busy=true;
    try{
      for(;;){
        const s=stops().find(x=>(x.status||"pendente")==="pendente"&&!x.lat&&x.geo!=="fail"&&x.endereco);
        if(!s) break;
        let r=null, ok=true;
        try{ r=await search(fullAddr(s.endereco)); if(!r&&cfg.cidade) r=await search(s.endereco); }catch(e){ ok=false; }
        if(!ok){ break; } // sem internet: tenta de novo depois
        const cur=items.find(x=>x.id===s.id); if(cur){ Object.assign(cur,r?{lat:r.lat,lon:r.lon,geo:"ok"}:{geo:"fail"}); saveDoc(cur,true); renderRota(); renderInicio(); }
        await sleep(1100);
      }
      if(cfg.trabalho&&!cfg.trabalhoLL){ try{ const r=await search(fullAddr(cfg.trabalho)); if(r){ cfg.trabalhoLL=[r.lat,r.lon]; persist(); Mapa.draw(); } }catch(e){} }
    } finally { busy=false; }
  }
  return {kick,reverse,search};
})();

/* ---------- mapa (Leaflet + OpenStreetMap) ---------- */
const Mapa=(()=>{
  let m=null, layer=null, routeLayer=null, trail=null, me=null, cd=null, pickCb=null, fitted=false;
  const ok=()=>!!window.L;
  function init(){
    if(m||!ok()) return;
    m=L.map("map",{zoomControl:false,attributionControl:true}).setView([-5.09,-42.80],13);
    L.tileLayer("https://tile.openstreetmap.org/{z}/{x}/{y}.png",{maxZoom:19,attribution:'&copy; OpenStreetMap'}).addTo(m);
    layer=L.layerGroup().addTo(m); routeLayer=L.layerGroup().addTo(m);
    m.on("click",async e=>{ if(!pickCb) return; const cb=pickCb; setPick(null); cb(e.latlng.lat,e.latlng.lng); });
  }
  function setPick(cb){ pickCb=cb; $("#pickHint").hidden=!cb; if(cb&&view!=="rota") go("rota"); }
  function show(){
    if(!ok()){ $("#mapOff").hidden=false; return; }
    init(); setTimeout(()=>{ m.invalidateSize(); draw(); if(!fitted) fit(); },60);
  }
  function icon(cls,txt){ return L.divIcon({className:"",html:'<div class="'+cls+'"><b>'+txt+'</b></div>',iconSize:[30,26],iconAnchor:[15,33]}); }
  function draw(){
    if(!m) return;
    layer.clearLayers();
    const list=stopsToday(), pv=Rotas.preview, nDone=list.filter(s=>(s.status||"pendente")!=="pendente").length;
    list.forEach((s,i)=>{ if(!s.lat) return; const st=s.status||"pendente";
      const n=pv&&pv.includes(s.id)?nDone+pv.indexOf(s.id)+1:i+1; /* numeração da rota que está sendo comparada */
      L.marker([s.lat,s.lon],{icon:icon("pinmk"+(st==="entregue"?" done":st==="falha"?" fail":""),st==="entregue"?"✓":st==="falha"?"✕":n),title:s.endereco})
        .on("click",()=>openForm("parada",items.find(x=>x.id===s.id))).addTo(layer); });
    if(cfg.trabalhoLL){ L.marker(cfg.trabalhoLL,{icon:L.divIcon({className:"",html:'<div class="cdmk">CD</div>',iconSize:[26,26],iconAnchor:[13,13]})}).addTo(layer); }
    const st=Trip.st;
    if(st.points&&st.points.length>1){ L.polyline(st.points,{color:css("--trail"),weight:5,opacity:.85}).addTo(layer); }
    const pos=Trip.st.last||lastPos;
    if(pos){ L.marker([pos[0],pos[1]],{icon:L.divIcon({className:"",html:'<div class="memk"></div>',iconSize:[20,20],iconAnchor:[10,10]}),interactive:false}).addTo(layer); }
  }
  function drawRoutes(opts,sel){
    if(!m) return; routeLayer.clearLayers();
    opts.forEach((o,i)=>{ if(i===sel) return; L.polyline(o.geo,{color:css(o.color),weight:5,opacity:.45,dashArray:"8 8"}).on("click",()=>Rotas.select(i)).addTo(routeLayer); });
    const o=opts[sel]; if(o){ L.polyline(o.geo,{color:"#fff",weight:9,opacity:.9}).addTo(routeLayer); L.polyline(o.geo,{color:css(o.color),weight:6}).addTo(routeLayer); }
  }
  function clearRoutes(){ if(routeLayer) routeLayer.clearLayers(); }
  function fit(){
    if(!m) return; const pts=stopsToday().filter(s=>s.lat).map(s=>[s.lat,s.lon]);
    const pos=Trip.st.last||lastPos; if(pos) pts.push([pos[0],pos[1]]);
    if(pts.length>1){ m.fitBounds(pts,{padding:[40,40],maxZoom:16}); fitted=true; }
    else if(pts.length===1){ m.setView(pts[0],15); fitted=true; }
  }
  function center(p){ if(m) m.setView(p,16); }
  return {show,draw,drawRoutes,clearRoutes,fit,center,setPick,get ready(){return !!m;}};
})();
const css=n=>getComputedStyle(document.documentElement).getPropertyValue(n).trim()||"#0A6B4B";

/* localização atual */
let lastPos=null;
function locate(timeout){
  return new Promise(res=>{
    const t=Trip.st.last; if(t&&Date.now()-(t[3]||0)<60000) return res(t);
    if(!navigator.geolocation) return res(lastPos);
    navigator.geolocation.getCurrentPosition(p=>{ lastPos=[p.coords.latitude,p.coords.longitude,p.coords.accuracy,Date.now()]; res(lastPos); },
      ()=>res(lastPos),{enableHighAccuracy:true,timeout:timeout||8000,maximumAge:30000});
  });
}

/* ---------- escolha de rota (OSRM) ---------- */
const Rotas=(()=>{
  let opts=[], sel=0, from=null, loading=false;
  const API="https://router.project-osrm.org";
  const C=["--route1","--route2","--route3"];
  async function osrm(path){ const r=await fetch(API+path); if(!r.ok) throw new Error("http "+r.status); const j=await r.json(); if(j.code!=="Ok") throw new Error(j.code); return j; }
  const geoOf=g=>g.coordinates.map(c=>[c[1],c[0]]);
  async function calc(){
    if(loading) return; loading=true; $("#calcRoutes").disabled=true; $("#calcRoutes").textContent="Calculando…";
    try{
      const pend=stopsToday().filter(s=>(s.status||"pendente")==="pendente"&&s.lat);
      if(!pend.length){ toast("Nenhuma parada com local no mapa"); return; }
      let start=await locate(8000), fromLbl="Saindo de onde você está";
      if(!start&&cfg.trabalhoLL){ start=cfg.trabalhoLL; fromLbl="Saindo do CD"; }
      if(!start){ start=[pend[0].lat,pend[0].lon]; fromLbl="Saindo da 1ª parada"; }
      from=start; $("#routeFrom").textContent=fromLbl;
      const pts=[[start[1],start[0]],...pend.map(s=>[s.lon,s.lat])];
      const coord=pts.map(p=>p[0].toFixed(6)+","+p[1].toFixed(6)).join(";");
      const out=[];
      if(pend.length===1){
        const j=await osrm("/route/v1/driving/"+coord+"?overview=full&geometries=geojson&alternatives=3");
        j.routes.forEach((r,i)=>out.push({nome:i?"Alternativa "+i:"Rota principal",km:r.distance/1000,min:r.duration/60,geo:geoOf(r.geometry),ordem:pend.map(s=>s.id)}));
      } else {
        const trip=await osrm("/trip/v1/driving/"+coord+"?source=first&destination=any&roundtrip=false&overview=full&geometries=geojson");
        const ord=pend.map((s,i)=>({id:s.id,k:trip.waypoints[i+1].waypoint_index})).sort((a,b)=>a.k-b.k).map(x=>x.id);
        const tr=trip.trips[0];
        out.push({nome:"Ordem otimizada",km:tr.distance/1000,min:tr.duration/60,geo:geoOf(tr.geometry),ordem:ord});
        const lst=await osrm("/route/v1/driving/"+coord+"?overview=full&geometries=geojson");
        const r=lst.routes[0];
        out.push({nome:"Na ordem da lista",km:r.distance/1000,min:r.duration/60,geo:geoOf(r.geometry),ordem:pend.map(s=>s.id)});
        if(cfg.casa){ /* volta para casa no fim */
          try{ let h=cfg.casaLL; if(!h){ const g=await Geo.search(fullAddr(cfg.casa)); if(g){ h=cfg.casaLL=[g.lat,g.lon]; persist(); } }
            if(h){ const c2=coord+";"+h[1].toFixed(6)+","+h[0].toFixed(6);
              const t2=await osrm("/trip/v1/driving/"+c2+"?source=first&destination=last&roundtrip=false&overview=full&geometries=geojson");
              const o2=pend.map((s,i)=>({id:s.id,k:t2.waypoints[i+1].waypoint_index})).sort((a,b)=>a.k-b.k).map(x=>x.id);
              out.push({nome:"Otimizada terminando em casa",km:t2.trips[0].distance/1000,min:t2.trips[0].duration/60,geo:geoOf(t2.trips[0].geometry),ordem:o2,casa:1}); } }catch(e){}
        }
      }
      out.forEach((o,i)=>o.color=C[i%3]);
      const minKm=Math.min(...out.map(o=>o.km)), minMin=Math.min(...out.map(o=>o.min));
      out.forEach(o=>{ o.tags=[]; if(out.length>1&&o.km===minKm) o.tags.push("Mais curta"); if(out.length>1&&o.min===minMin) o.tags.push("Mais rápida"); });
      opts=out; sel=0; renderCards(); Mapa.drawRoutes(opts,sel); Mapa.draw(); Mapa.fit();
    }catch(e){ toast("Não consegui calcular. Confira a internet e tente de novo."); }
    finally{ loading=false; $("#calcRoutes").disabled=false; $("#calcRoutes").textContent=opts.length?"Recalcular":"Calcular rotas"; }
  }
  function renderCards(){
    const box=$("#routeOpts"); if(!box) return;
    const n=stopsToday().filter(s=>(s.status||"pendente")==="pendente").length;
    box.innerHTML=opts.map((o,i)=>{
      const comb=o.km*custoKm().total, tot=o.min+n*cfg.tParada;
      return '<button type="button" class="ropt" data-ropt="'+i+'" aria-pressed="'+(i===sel)+'"><span class="bar" style="background:var('+o.color+')"></span>'+
        '<span style="min-width:0"><span class="t1">'+esc(o.nome)+'</span><span class="t2 num">'+hm(o.min)+' dirigindo + '+hm(n*cfg.tParada)+' nas paradas · custo '+brl(comb)+'</span>'+
        (o.tags.length?'<span class="tag">'+o.tags.join(" · ")+'</span>':'')+'</span>'+
        '<span style="text-align:right"><b class="num">'+dec(o.km,1)+' km</b><span class="t2 num">'+hm(tot)+'</span></span></button>';
    }).join("")+(opts.length?'<div class="btns"><button class="btn" type="button" id="useRoute">Usar esta rota</button><button class="btn ghost" type="button" id="navRoute">'+I.nav+'Navegar</button></div>':'');
    box.querySelectorAll("[data-ropt]").forEach(b=>b.onclick=()=>select(+b.dataset.ropt));
    if(opts.length){ $("#useRoute").onclick=use; $("#navRoute").onclick=()=>{ use(true); const pend=stopsToday().filter(s=>(s.status||"pendente")==="pendente"); openExt(cfg.nav==="waze"?navLink(pend[0]):gmapsRoute(pend,from)); }; }
  }
  function select(i){ sel=i; renderCards(); Mapa.drawRoutes(opts,sel); Mapa.draw(); }
  function use(quiet){
    const o=opts[sel]; if(!o) return;
    const list=stopsToday(); const rest=list.filter(s=>!o.ordem.includes(s.id));
    const done=rest.filter(s=>(s.status||"pendente")!=="pendente"), other=rest.filter(s=>(s.status||"pendente")==="pendente");
    let k=1; done.forEach(s=>{s.ordem=k++; saveDoc(s,true);});
    o.ordem.forEach(id=>{ const s=items.find(x=>x.id===id); if(s){ s.ordem=k++; saveDoc(s,true);} });
    other.forEach(s=>{s.ordem=k++; saveDoc(s,true);});
    Rotas.chosen={km:o.km,min:o.min}; lsSet("rp_chosen",Rotas.chosen);
    renderAll(); if(quiet!==true) toast("Paradas na ordem de \""+o.nome.toLowerCase()+"\"");
  }
  function reset(){ opts=[]; Mapa.clearRoutes(); }
  return {calc,renderCards,select,reset,chosen:lsGet("rp_chosen",null),get preview(){ return opts.length&&opts[sel]?opts[sel].ordem:null; }};
})();

/* ---------- cronômetro da rota ---------- */
const Trip=(()=>{
  const EMPTY={running:false,paused:false,startedAt:0,elapsedMs:0,km:0,points:[],last:null,valor:0,pacotes:0,gps:"wait"};
  let st={...EMPTY}, fetchedAt=Date.now(), web=lsGet("rp_trip",null), watchId=null, timer=null;

  /* versão navegador (sem o app Android): GPS só com a tela aberta */
  function hav(a,b){ const R=6371, dLa=(b[0]-a[0])*Math.PI/180, dLo=(b[1]-a[1])*Math.PI/180, x=Math.sin(dLa/2)**2+Math.cos(a[0]*Math.PI/180)*Math.cos(b[0]*Math.PI/180)*Math.sin(dLo/2)**2; return 2*R*Math.asin(Math.sqrt(x)); }
  function webElapsed(w){ return w.accMs+(w.running&&!w.paused?Date.now()-w.segStart:0); }
  function webState(){ const w=web; if(!w) return {...EMPTY}; return {running:w.running,paused:w.paused,startedAt:w.startedAt,elapsedMs:webElapsed(w),km:w.km,points:w.points,last:w.last,valor:w.valor,pacotes:w.pacotes,gps:w.gps||"wait"}; }
  function webWatch(on){
    if(!navigator.geolocation) return;
    if(on&&watchId==null){ watchId=navigator.geolocation.watchPosition(p=>{
        const c=p.coords, pt=[c.latitude,c.longitude,c.accuracy,Date.now()]; web.gps="ok";
        if(c.accuracy<=35&&web.running&&!web.paused){
          if(web.last){ const d=hav(web.last,pt), dt=(pt[3]-web.last[3])/3600000; if(d>=0.008&&(dt<=0||d/dt<200)){ web.km+=d; web.points.push([pt[0],pt[1]]); if(web.points.length>3000) web.points.splice(1,1); web.last=pt; } else if(!web.points.length){ web.last=pt; } }
          else { web.last=pt; web.points.push([pt[0],pt[1]]); }
        } lastPos=pt; lsSet("rp_trip",web);
      },()=>{ web.gps="off"; },{enableHighAccuracy:true,maximumAge:2000,timeout:20000}); }
    if(!on&&watchId!=null){ navigator.geolocation.clearWatch(watchId); watchId=null; }
  }

  function refresh(){
    const s=N?natJ("tripState"):webState();
    st={...EMPTY,...(s||{})}; fetchedAt=Date.now();
    if(st.last) lastPos=st.last;
    return st;
  }
  function elapsed(){ return st.elapsedMs+(st.running&&!st.paused?Date.now()-fetchedAt:0); }
  function start(valor,pacotes){
    const v=num(valor), p=num(pacotes);
    if(N){ const r=nat("tripStart",JSON.stringify({valor:v,pacotes:p})); if(r==="permission") toast("Permita a localização para contar os km"); }
    else { web={running:true,paused:false,startedAt:Date.now(),accMs:0,segStart:Date.now(),km:0,points:[],last:null,valor:v,pacotes:p,gps:"wait"}; lsSet("rp_trip",web); webWatch(true); }
    refresh(); loop(); ui();
  }
  function pause(){ if(N) nat("tripPause"); else if(web){ web.accMs=webElapsed(web); web.paused=true; lsSet("rp_trip",web); } refresh(); ui(); }
  function resume(){ if(N) nat("tripResume"); else if(web){ web.segStart=Date.now(); web.paused=false; lsSet("rp_trip",web); } refresh(); ui(); }
  function setMeta(valor,pacotes){ if(N) nat("tripSetMeta",JSON.stringify({valor:num(valor),pacotes:num(pacotes)})); else if(web){ web.valor=num(valor); web.pacotes=num(pacotes); lsSet("rp_trip",web); } refresh(); ui(); }
  function stop(){
    refresh(); const fin={...st,elapsedMs:elapsed()};
    if(N) nat("tripStop"); else { webWatch(false); web=null; lsSet("rp_trip",null); }
    st={...EMPTY}; ui(); renderAll(); return fin;
  }
  function live(){
    const h=elapsed()/3600000, km=st.km, v=st.valor, p=st.pacotes;
    const ck=custoKm(), comb=km*ck.total;
    const list=stopsToday(), tot=list.reduce((a,s)=>a+num(s.pacotes),0), dn=list.filter(s=>s.status==="entregue").reduce((a,s)=>a+num(s.pacotes),0);
    const pace=h>0.05&&dn?dn/h:null;
    return {h,km,v,p,comb,lucro:v-comb,porHora:h>0.02&&v?v/h:null,lucroHora:h>0.02&&v?(v-comb)/h:null,porKm:km>=1&&v?v/km:null,porPct:p?v/p:null,tot,dn,pace,eta:pace&&tot>dn?(tot-dn)/pace*60:null};
  }
  function ui(){
    const on=st.running; document.body.classList.toggle("trip-on",on); $("#tripbar").hidden=!on;
    $("#startCard").hidden=on;
    if(!on) return;
    const L=live();
    $("#tbRec").className="rec"+(st.paused?" paused":"");
    $("#tbTime").textContent=hms(elapsed());
    $("#tbKm").textContent=dec(L.km,1)+" km";
    $("#tbRate").textContent=st.paused?"Pausado":L.porHora!=null?brl(L.porHora)+"/h":(L.porPct!=null?brl(L.porPct)+"/pct":"Sem valor");
    if(sheetIs("trip")) fillTripSheet();
  }
  let tick=0;
  function loop(){
    clearInterval(timer); if(!st.running) return;
    timer=setInterval(()=>{ if(document.hidden) return; tick++; if(tick%3===0){ refresh(); if(view==="rota") Mapa.draw(); } ui(); if(!st.running) clearInterval(timer); },1000);
  }
  function boot(){ refresh(); if(!N&&web&&web.running) webWatch(true); ui(); loop(); }
  return {boot,start,pause,resume,stop,setMeta,refresh,live,elapsed,ui,loop,get st(){return st;}};
})();

/* ---------- Histórico ---------- */
function hRange(){
  const r=hRef, d=parse(r);
  if(hP==="dia") return [r,r];
  if(hP==="semana"){ const s=addDays(r,-((d.getDay()+6)%7)); return [s,addDays(s,6)]; }
  if(hP==="mes") return [iso(new Date(d.getFullYear(),d.getMonth(),1)),iso(new Date(d.getFullYear(),d.getMonth()+1,0))];
  return [d.getFullYear()+"-01-01",d.getFullYear()+"-12-31"];
}
function hShift(k){
  const d=parse(hRef);
  if(hP==="dia") hRef=addDays(hRef,k); else if(hP==="semana") hRef=addDays(hRef,7*k);
  else if(hP==="mes") hRef=iso(new Date(d.getFullYear(),d.getMonth()+k,1)); else hRef=iso(new Date(d.getFullYear()+k,0,1));
  renderHist();
}
function hLabel([f,l]){
  if(hP==="dia") return cap(parse(f).toLocaleDateString("pt-BR",{weekday:"short",day:"2-digit",month:"2-digit",year:"numeric"}));
  if(hP==="semana") return parse(f).toLocaleDateString("pt-BR",{day:"2-digit",month:"2-digit"})+" – "+parse(l).toLocaleDateString("pt-BR",{day:"2-digit",month:"2-digit"});
  if(hP==="mes") return cap(parse(f).toLocaleDateString("pt-BR",{month:"long",year:"numeric"}));
  return f.slice(0,4);
}
function renderHist(){
  document.querySelectorAll("#hSeg button").forEach(b=>b.setAttribute("aria-pressed",String(b.dataset.p===hP)));
  document.querySelectorAll("#hTab button").forEach(b=>b.setAttribute("aria-pressed",String(b.dataset.t===hTab)));
  const R=hRange(), [f,l]=R, s=sums(f,l);
  $("#hLbl").textContent=hLabel(R);
  $("#hNext").disabled=l>=today();
  $("#hNet").textContent=brl(s.lucro);
  $("#hIn").textContent=brl0(s.ganhos); $("#hOut").textContent=brl0(s.despesas);
  $("#hKm").textContent=Math.round(s.km).toLocaleString("pt-BR")+" km";
  const st=(lbl,val,sub,g)=>'<div class="stat"><small>'+lbl+'</small><b'+(g?' class="'+g+'"':'')+'>'+val+'</b><span>'+sub+'</span></div>';
  $("#hStats").innerHTML=
    st("Por km",s.km?brl(s.ganhos/s.km):"—",dec(s.km,1)+" km",s.km?grade(s.ganhos/s.km,"km"):"")+
    st("Por hora",s.min?brl(s.ganhos/(s.min/60)):"—",hm(s.min)+" rodando",s.min?grade(s.ganhos/(s.min/60),"hora"):"")+
    st("Por parada",s.par?brl(s.valPar/s.par):"—",s.par+" paradas")+
    st("Por pacote",s.pct?brl(s.valPct/s.pct):"—",s.pct+" pacotes",s.pct?grade(s.valPct/s.pct,"pct"):"");
  const realCons=s.litros&&s.km?dec(s.km/s.litros,1)+" km/L":"—";
  const row=(ic,k,v)=>'<div class="r">'+I[ic]+'<span class="k">'+k+'</span><span class="v">'+v+'</span></div>';
  $("#hPerf").innerHTML=row("route","Distância",dec(s.km,1)+" km")+row("clock","Tempo ativo",hm(s.min))+row("box","Pacotes entregues",String(s.pct))+
    row("gauge","Consumo real (km ÷ litros)",realCons)+row("boxx","PNR no período",String(s.pnr));
  const inR=x=>{const d=x.data||today(); return d>=f&&d<=l;};
  let html="";
  if(hTab==="lanc"){
    const arr=lancs().filter(inR);
    if(!arr.length) html=emptyBox("list","Nenhum lançamento","Adicione um ganho ou um gasto pelo botão +.");
    else{ const by={}; arr.forEach(x=>{(by[x.data]=by[x.data]||[]).push(x)});
      html=Object.keys(by).sort().reverse().map(d=>{ const a=by[d].sort((x,y)=>(y.ts||0)-(x.ts||0));
        return '<div class="day"><div class="day-h"><span>'+esc(dayLabel(d))+'</span><span class="num">'+brl(sums(d,d).lucro)+'</span></div>'+a.map(lancRow).join("")+'</div>'; }).join(""); }
  } else if(hTab==="pnr"){
    const arr=pnrs().filter(inR).sort((a,b)=>(b.data||"").localeCompare(a.data||""));
    html=arr.length?arr.map(pnrRow).join(""):emptyBox("boxx","Nenhum PNR neste período","Registre pelo botão + quando um cliente disser que não recebeu. Guarde a foto da entrega como prova.");
  } else {
    const arr=items.filter(x=>x.foto&&inR(x)).sort((a,b)=>(b.ts||0)-(a.ts||0));
    html=arr.length?'<div class="gallery">'+arr.map(x=>{ const t=T[kindOf(x)==="lanc"?x.tipo:kindOf(x)]||T.extra;
      return '<figure><button type="button" data-edit="'+esc(x.id)+'"><img data-foto="'+esc(x.foto)+'" alt="Comprovante de '+esc(t.n)+'"></button><figcaption>'+esc(t.n)+' · '+esc(dayLabel(x.data||today()))+'</figcaption></figure>'; }).join("")+'</div>'
      :emptyBox("camera","Nenhum comprovante","Anexe fotos em abastecimentos, gastos, paradas entregues e PNR.");
  }
  $("#hList").innerHTML=html; Fotos.hydrate($("#hList"));
  renderSemana(f);
}

/* ---------- Ajustes ---------- */
function renderAjustes(){
  Conta.renderConta();
  const set=(id,v)=>{ const e=$(id); if(e&&document.activeElement!==e) e.value=v; };
  set("#a_nome",cfg.nome); set("#a_nav",cfg.nav); set("#a_tParada",String(cfg.tParada)); set("#a_cidade",cfg.cidade);
  set("#a_veiculo",cfg.veiculo); set("#a_cons",fmtCfg(cfg.cons)); set("#a_gas",fmtCfg(cfg.gas)); const ck=custoKm(); $("#a_custoV").textContent=brl(ck.total)+"/km"; $("#a_custoS").textContent=ck.set?"combustível "+brl(ck.comb)+" + carro "+brl(ck.inv):"Toque para calcular o seu (hoje é só uma estimativa)";
  ["Pct","Km","Hora"].forEach(k=>{ set("#a_meta"+k,fmtCfg(cfg["meta"+k])); set("#a_ruim"+k,fmtCfg(cfg["ruim"+k])); });
  set("#a_casa",cfg.casa); set("#a_trabalho",cfg.trabalho);
  document.querySelectorAll("#aTema button").forEach(b=>b.setAttribute("aria-pressed",String(b.dataset.v===cfg.tema)));
  renderAlert();
}
function renderAlert(){
  $("#alertSet").hidden=!N; $("#macroGuide").hidden=!!N;
  if(!N) return;
  const s=natJ("alertStatus")||{};
  $("#alAccess").textContent=s.access?"Liberado":"Ainda não liberado. Sem isso o alarme não funciona.";
  $("#alAccess").style.color=s.access?"var(--good)":"var(--bad)";
  $("#alGrant").textContent=s.access?"Abrir":"Permitir";
  $("#alOn").checked=!!cfg.alertOn;
  $("#ovAccess").textContent=s.overlay?"Liberado":"Precisa liberar para o cartão e a bolha aparecerem";
  $("#ovAccess").style.color=s.overlay?"var(--good)":"var(--bad)";
  $("#ovGrant").textContent=s.overlay?"Abrir":"Permitir";
  $("#ovOn").checked=!!cfg.overlayOn; $("#bbOn").checked=!!cfg.bubbleOn;
  if(document.activeElement!==$("#alWords")) $("#alWords").value=cfg.alertWords;
  $("#alSecs").value=String(cfg.alertSecs);
  const seen=s.seen||[];
  const map={}; seen.forEach(a=>map[a.pkg]=a.label); (cfg.alertApps||[]).forEach(p=>{ if(!map[p]) map[p]=p; });
  const keys=Object.keys(map);
  $("#alApps").innerHTML=keys.map(p=>'<label><input type="checkbox" data-app="'+esc(p)+'"'+((cfg.alertApps||[]).includes(p)?" checked":"")+'>'+esc(map[p])+'</label>').join("");
  $("#alAppsHint").hidden=false;
  renderDiag(s);
}
/* diagnóstico do alerta e do cartão: cada item que falta, com o botão que resolve */
function renderDiag(s){
  const ok=(b,txt,fix,btn)=>'<div class="dg"><span class="chip '+(b?"good":"bad")+'">'+(b?"ok":"falta")+'</span><span class="k">'+txt+'</span>'+(!b&&btn?'<button type="button" class="link" data-fix="'+fix+'">'+btn+'</button>':'')+'</div>';
  const fab=String(s.fabricante||"").toLowerCase();
  let h=ok(s.access,"Acesso às notificações","openAlertAccess","Permitir")+
    ok(s.overlay,"Aparecer sobre outros apps","openOverlayPermission","Permitir")+
    ok(s.notif,"Notificações do Rota Paga ligadas","openAppSettings","Abrir")+
    ok(s.battery,"Bateria sem restrição (o Android não desliga o app)","openBattery","Ajustar")+
    ok(!!cfg.overlayOn,"Cartão por cima ligado aqui em cima","","");
  if(s.cardRes) h+='<p class="sub">Último cartão ('+new Date(s.cardTs||0).toLocaleTimeString("pt-BR",{hour:"2-digit",minute:"2-digit"})+'): <b>'+esc(s.cardRes)+'</b></p>';
  $("#diagList").innerHTML=h;
  let b="";
  if(/xiaomi|redmi|poco/.test(fab)) b='<div class="note-box"><b>Celular '+esc(s.fabricante)+':</b> a Xiaomi bloqueia janelas flutuantes mesmo com a permissão dada. Em <b>Outras permissões</b>, ligue <b>Mostrar janelas pop-up enquanto em segundo plano</b> e <b>Exibir na tela de bloqueio</b>. Em <b>Economia de bateria</b>, escolha <b>Sem restrições</b>, e ligue o <b>Início automático</b>.<button type="button" class="btn ghost" data-fix="openMiuiPerms" style="margin-top:10px;width:100%">Abrir permissões da Xiaomi</button></div>';
  else if(/samsung/.test(fab)) b='<div class="note-box"><b>Celular Samsung:</b> a Samsung costuma desligar apps que leem notificações. Faça os três:<br>1. <b>Aplicativos › Rota Paga › Bateria</b>: escolha <b>Sem restrições</b>.<br>2. <b>Assistência do aparelho e bateria › Bateria › Limites de uso em segundo plano</b>: tire o Rota Paga de <b>Apps em suspensão</b> e de <b>Apps em suspensão profunda</b> e coloque em <b>Apps que nunca são suspensos</b>.<br>3. <b>Notificações › Configurações avançadas › Acesso à notificação do dispositivo</b>: confira se o Rota Paga está ligado. Se já estava, desligue e ligue de novo.<button type="button" class="btn ghost" data-fix="openAppSettings" style="margin-top:10px;width:100%">Abrir configurações do Rota Paga</button></div>';
  else if(/motorola|oppo|realme|vivo|oneplus|huawei|honor|infinix|tecno/.test(fab)) b='<div class="note-box"><b>Celular '+esc(s.fabricante)+':</b> procure nas configurações do app por <b>Início automático</b>, <b>Execução em segundo plano</b> e <b>Bateria sem restrição</b>, e ligue os três.</div>';
  $("#diagBrand").innerHTML=b;
  const log=s.log||[];
  $("#logList").innerHTML=log.length?log.map(l=>'<div class="lg"><div class="sec-h"><b>'+esc(l.app)+'</b><span class="chip '+(/ALERTA/.test(l.res)?"good":"none")+'">'+esc(l.res)+'</span></div><span class="t2" style="white-space:normal">'+
    new Date(l.ts).toLocaleString("pt-BR",{day:"2-digit",month:"2-digit",hour:"2-digit",minute:"2-digit"})+' · '+esc(l.text)+'</span></div>').join(""):
    '<p class="sub">Nenhuma notificação lida ainda. Confira o acesso às notificações acima.</p>';
}
function pushAlertCfg(){
  if(!N) return;
  nat("setAlertConfig",JSON.stringify({enabled:!!cfg.alertOn,keywords:String(cfg.alertWords||"").split(",").map(s=>s.trim()).filter(Boolean),apps:cfg.alertApps||[],seconds:+cfg.alertSecs||10,ruimPct:num(cfg.ruimPct),metaPct:num(cfg.metaPct),ruimKm:num(cfg.ruimKm),metaKm:num(cfg.metaKm),ruimHora:num(cfg.ruimHora),metaHora:num(cfg.metaHora),gas:cfg.gas,cons:cfg.cons,custoKm:custoKm().total,overlay:!!cfg.overlayOn,bubble:!!cfg.bubbleOn}));
}

/* ================= folhas (telas que sobem) ================= */
let sheetKind="";
const sheetIs=k=>sheetKind===k&&!!$("#scrim");
function closeSheet(){ $("#sheetRoot").innerHTML=""; sheetKind=""; }
function sheet(title,body,foot,kind){
  sheetKind=kind||"";
  $("#sheetRoot").innerHTML='<div class="scrim" id="scrim"><div class="sheet" role="dialog" aria-modal="true" aria-label="'+esc(title)+'">'+
    '<div class="sheet-h"><h3>'+esc(title)+'</h3><button class="close" id="shClose" type="button" aria-label="Fechar">✕</button></div>'+
    '<div class="sheet-b">'+body+'</div>'+(foot?'<div class="sheet-f">'+foot+'</div>':'')+'</div></div>';
  $("#shClose").onclick=closeSheet;
  $("#scrim").addEventListener("click",e=>{ if(e.target.id==="scrim") closeSheet(); });
}
function openMenu(){
  const it=(id,ic,t,s,neg)=>'<button type="button" id="'+id+'"><span class="ico'+(neg?" neg":"")+'">'+I[ic]+'</span><span>'+t+'<small>'+s+'</small></span></button>';
  sheet("Novo",'<div class="menu">'+
    it("mTrip","timer",Trip.st.running?"Ver cronômetro":"Iniciar cronômetro","Tempo e km da rota pelo GPS")+
    it("mAgenda","cal","Agendar rota","Rota aceita para amanhã (ou outro dia)")+
    it("mGanho","up","Adicionar ganho","Entrega, frete, Uber, extra")+
    it("mGasto","down","Adicionar gasto","Abastecimento, manutenção, alimentação…",1)+
    it("mParada","pin","Nova parada","Endereço para a rota de hoje")+
    it("mFrete","calc","Fretômetro","Quanto cobrar por um frete")+
    it("mPnr","boxx","Registrar PNR","Pacote que o cliente diz não ter recebido",1)+
    it("mAlerta","bell","Colar rota","Ler a notificação de uma rota nova")+'</div>');
  $("#mTrip").onclick=()=>openTrip();
  $("#mAgenda").onclick=()=>openForm("agenda",null,{data:addDays(today(),1)});
  $("#mGanho").onclick=()=>openTypes("Adicionar ganho",GANHOS);
  $("#mGasto").onclick=()=>openTypes("Adicionar gasto",GASTOS);
  $("#mParada").onclick=()=>{ closeSheet(); go("rota"); setTimeout(()=>$("#stAddr").focus(),250); };
  $("#mFrete").onclick=()=>openFretometro();
  $("#mPnr").onclick=()=>openForm("pnr");
  $("#mAlerta").onclick=()=>{ closeSheet(); go("inicio"); showDetect(); };
}
function openTypes(title,keys){
  sheet(title,'<div class="types">'+keys.map(k=>'<button class="type" type="button" data-t="'+k+'"><span class="ico'+(isG(k)?" neg":"")+'">'+I[T[k].i]+'</span>'+esc(T[k].n)+'</button>').join("")+'</div>');
  document.querySelectorAll(".type").forEach(b=>b.onclick=()=>openForm(b.dataset.t));
}
function suggestions(key){ return [...new Set(items.map(x=>x[key]).filter(Boolean))].slice(-20); }

function openForm(tipo,it,prefill){
  const t=T[tipo], kind=t.kind||"lanc", edit=!!it;
  it=it?{...it}:{tipo,kind,data:today(),...(kind==="pnr"?{status:"Aberto",motivo:MOTIVOS[0]}:{}),...(prefill||{})};
  if(!edit && t.f.includes("cons") && !it.cons) it.cons=fmtCfg(cfg.cons);
  let foto=it.foto||null;
  const fields=t.f.map(k=>{
    const f=F[k], id="f_"+k, val=it[k]??"";
    if(f.t==="select") return '<div class="fld'+(f.full?" full":"")+'"><label for="'+id+'">'+f.l+'</label><select id="'+id+'">'+f.opts.map(o=>'<option'+(o===val?" selected":"")+'>'+esc(o)+'</option>').join("")+'</select></div>';
    const type=f.t==="date"?"date":"text", mode=f.t==="num"?' inputmode="decimal"':f.t==="int"?' inputmode="numeric"':'';
    return '<div class="fld'+(f.full?" full":"")+'"><label for="'+id+'">'+f.l+'</label><input id="'+id+'" type="'+type+'"'+mode+(f.list?' list="dl_'+k+'"':'')+
      ' value="'+esc(val)+'"'+(f.ph?' placeholder="'+esc(f.ph)+'"':'')+' autocomplete="off"></div>';
  }).join("");
  const dls=t.f.filter(k=>F[k].list).map(k=>'<datalist id="dl_'+k+'">'+suggestions(k).map(s=>'<option value="'+esc(s)+'">').join("")+'</datalist>').join("");
  const showCalc=(GANHOS.includes(tipo)&&tipo!=="extra")||kind==="agenda";
  const wantsPhoto=isG(tipo)||kind==="pnr"||kind==="parada"||tipo==="extra";
  let statusSeg="";
  if(kind==="parada"&&edit){
    const st=it.status||"pendente";
    statusSeg='<div class="seg" id="pStatus" role="group" aria-label="Situação">'+[["pendente","Pendente"],["entregue","Entregue"],["falha","Não entregue"]].map(([v,l])=>'<button type="button" data-v="'+v+'" aria-pressed="'+(st===v)+'">'+l+'</button>').join("")+'</div>';
  }
  const hint=tipo==="abast"?'<p class="sub" id="abHint">O preço do litro deste abastecimento vira o preço usado nos cálculos.</p>':
    kind==="pnr"?'<p class="sub">O desconto entra como despesa no dia do PNR. A foto da entrega ajuda na contestação.</p>':
    kind==="parada"&&edit?'<p class="sub">'+(it.lat?"Local no mapa: "+dec(it.lat,5)+", "+dec(it.lon,5):"Esta parada ainda não tem local no mapa.")+'</p>':'';
  sheet((edit?"Editar ":"")+t.n, statusSeg+'<form id="frm" class="grid" novalidate>'+fields+'</form>'+dls+(showCalc?'<div class="calc" id="calc"></div>':'')+
    (wantsPhoto?'<div class="photo" id="photoBox"></div>':'')+hint,
    '<button class="btn" id="frmSave" type="button">'+(edit?"Salvar alterações":kind==="parada"?"Adicionar parada":kind==="agenda"?"Agendar":"Confirmar lançamento")+'</button>'+
    (kind==="parada"&&edit?'<div class="btns"><button class="btn ghost" type="button" id="pUp">↑</button><button class="btn ghost" type="button" id="pDown">↓</button><button class="btn ghost" type="button" id="pPick">'+I.pin+'Mapa</button><button class="btn ghost" type="button" id="pNav">'+I.nav+'Ir</button></div>':'')+
    (kind==="parada"&&edit&&it.status==="falha"?'<button class="btn ghost" type="button" id="pToPnr">Registrar PNR desta parada</button>':'')+
    (kind==="agenda"&&edit&&!Trip.st.running?'<button class="btn ghost" type="button" id="agGo">'+I.timer+'Iniciar cronômetro desta rota</button>':'')+
    (edit?'<button class="btn danger" id="frmDel" type="button">Excluir</button>':''),"form");
  const read=()=>{ const o={...it}; t.f.forEach(k=>o[k]=$("#f_"+k).value.trim()); return o; };
  const drawPhoto=()=>{ const b=$("#photoBox"); if(!b) return;
    b.innerHTML=(foto?'<img class="thumb" data-foto="'+esc(foto)+'" alt="Comprovante">':'')+
      '<button class="btn ghost" type="button" id="phBtn" style="flex:1">'+I.camera+(foto?"Trocar foto":"Anexar foto")+'</button>'+
      (foto?'<button class="btn ghost" type="button" id="phDel" style="flex:0 0 auto" aria-label="Remover foto">✕</button>':'');
    Fotos.hydrate(b);
    $("#phBtn").onclick=async()=>{ const id=await pickPhoto(); if(id){ foto=id; drawPhoto(); } };
    if(foto) $("#phDel").onclick=()=>{ foto=null; drawPhoto(); };
  };
  drawPhoto();
  const upd=()=>{ if(showCalc) $("#calc").innerHTML=calcHtml(kind==="agenda"?"entrega":tipo,read());
    if(tipo==="abast"){ const o=read(), l=num(o.litros), v=num(o.valor); if(l&&v) $("#abHint").textContent="Preço do litro: "+brl(v/l)+". Ele vira o preço usado nos cálculos."; } };
  $("#frm").addEventListener("input",upd); upd();
  if($("#agGo")) $("#agGo").onclick=()=>{ const o=read(); Object.assign(it,o); saveDoc({...it},true); startAgenda(it.id); };
  if($("#pStatus")) $("#pStatus").addEventListener("click",e=>{ const b=e.target.closest("button"); if(!b) return; it.status=b.dataset.v; document.querySelectorAll("#pStatus button").forEach(x=>x.setAttribute("aria-pressed",String(x===b))); });
  $("#frmSave").onclick=()=>{
    const o=read();
    if((kind==="lanc"||kind==="agenda")&&!num(o.valor)){ toast("Informe o valor"); $("#f_valor").focus(); return; }
    if(kind==="parada"&&!o.endereco){ toast("Informe o endereço"); $("#f_endereco").focus(); return; }
    if(kind==="pnr"&&!o.codigo&&!o.endereco){ toast("Informe o código ou o endereço"); $("#f_codigo").focus(); return; }
    o.id=it.id||newId(); o.kind=kind; if(kind==="lanc") o.tipo=tipo; o.ts=it.ts||Date.now(); o.data=o.data||it.data||today();
    if(foto) o.foto=foto; else delete o.foto;
    if(kind==="parada"){ o.status=it.status||"pendente"; o.ordem=it.ordem??nextOrdem(); if(edit&&o.endereco!==it.endereco&&!it.manual){ delete o.lat; delete o.lon; delete o.geo; } }
    if(tipo==="abast"){ const l=num(o.litros); if(l>0){ cfg.gas=Math.round(num(o.valor)/l*100)/100; persist(); } }
    const agId=o._agenda; delete o._agenda;
    saveDoc(o); if(agId&&items.some(x=>x.id===agId)) delDoc(agId);
    closeSheet(); toast(edit?"Alterado":kind==="parada"?"Parada adicionada":kind==="agenda"?"Rota agendada para "+dayLabel(o.data).toLowerCase():"Lançado");
  };
  if(edit){ const d=$("#frmDel"); d.onclick=()=>{ if(d.dataset.arm){ delDoc(it.id); closeSheet(); toast("Excluído"); } else { d.dataset.arm=1; d.textContent="Toque de novo para excluir"; } }; }
  if($("#pUp")){
    $("#pUp").onclick=()=>{ moveStop(it.id,-1); closeSheet(); }; $("#pDown").onclick=()=>{ moveStop(it.id,1); closeSheet(); };
    $("#pNav").onclick=()=>openExt(navLink(it));
    $("#pPick").onclick=()=>{ closeSheet(); Mapa.setPick((lat,lon)=>{ const s=items.find(x=>x.id===it.id); if(s){ Object.assign(s,{lat,lon,geo:"ok",manual:1}); saveDoc(s); Rotas.reset(); toast("Local da parada marcado"); } }); };
  }
  if($("#pToPnr")) $("#pToPnr").onclick=()=>openForm("pnr",null,{endereco:it.endereco,foto:it.foto});
  if(!edit){ const first=t.f.find(k=>k!=="data"&&F[k].t!=="select"&&!it[k]); first&&setTimeout(()=>$("#f_"+first)?.focus(),250); }
}
function calcHtml(tipo,o){
  const m=metrics(o), v=verdict(tipo,m);
  const cell=(lbl,val,g)=>'<div><small>'+lbl+'</small><b class="num'+(g?" "+g:"")+'">'+(val==null?"—":val)+'</b></div>';
  let cells;
  if(tipo==="entrega"){
    cells=cell("R$/pacote",m.porPct!=null?brl(m.porPct):null,grade(m.porPct,"pct"))+cell("R$/parada",m.porParada!=null?brl(m.porParada):null)+
      cell("R$/km",m.porKm!=null?brl(m.porKm):null,grade(m.porKm,"km"))+cell("R$/hora",m.porHora!=null?brl0(m.porHora):null,grade(m.porHora,"hora"))+
      cell("Custo do km",m.custo?brl(m.custo):null)+cell("Lucro real",m.v?brl(m.lucro):null,m.v&&m.lucro<=0?"bad":"");
  } else {
    cells=cell("R$/km",m.porKm!=null?brl(m.porKm):null,grade(m.porKm,"km"))+cell("R$/hora",m.porHora!=null?brl0(m.porHora):null,grade(m.porHora,"hora"))+
      cell("Custo do km",m.custo?brl(m.custo):null)+cell("Pedágio",m.ped?brl(m.ped):null)+cell("Lucro real",m.v?brl(m.lucro):null,m.v&&m.lucro<=0?"bad":"")+cell("Lucro/hora",m.lucroHora!=null?brl0(m.lucroHora):null);
  }
  let f="";
  if(tipo==="entrega"&&m.porPct!=null) f=brl(m.v)+" ÷ "+m.pacotes+" pacotes = "+brl(m.porPct)+" por pacote";
  if(m.custo) f+=(f?"<br>":"")+"Custo do km: "+dec(m.km,1)+" km × "+brl(m.custo/m.km)+" (combustível "+brl(m.comb/m.km)+" + carro "+brl(custoKm().inv)+")";
  return '<div class="calc-h '+v+'"><span>'+VERD[v]+'</span><span style="font-weight:600;font-size:12px">'+(tipo==="entrega"?"Bom a partir de "+brl(cfg.metaPct)+"/pct":"Bom a partir de "+brl(cfg.metaKm)+"/km")+'</span></div><div class="calc-g">'+cells+'</div>'+(f?'<div class="formula num">'+f+'</div>':'');
}

/* ---------- Cronômetro (tela) ---------- */
function openTrip(pre){
  Trip.refresh();
  if(!Trip.st.running){
    const pend=stopsToday().filter(s=>(s.status||"pendente")==="pendente");
    const pk=pre&&pre.pacotes||pend.reduce((a,s)=>a+num(s.pacotes),0)||"";
    sheet("Cronômetro",
      '<p class="sub" style="margin:0">Ligue quando sair para a rota. O app conta o tempo e os km pelo GPS, mesmo com a tela apagada, e mostra quanto a rota está rendendo.</p>'+
      '<form class="grid" id="tf" novalidate><div class="fld"><label for="t_valor">Valor da rota (R$)</label><input id="t_valor" inputmode="decimal" placeholder="150,00" value="'+esc(pre&&pre.valor||"")+'"></div>'+
      '<div class="fld"><label for="t_pct">Pacotes</label><input id="t_pct" inputmode="numeric" placeholder="48" value="'+esc(pk)+'"></div></form>'+
      '<div class="calc" id="tPrev"></div>'+
      (!N?'<p class="sub">No navegador, o GPS só conta com esta tela aberta. No app instalado ele conta com a tela apagada.</p>':''),
      '<button class="btn" type="button" id="tGo">'+I.timer+'Iniciar</button>',"tripstart");
    const prev=()=>{ const v=num($("#t_valor").value), p=num($("#t_pct").value);
      $("#tPrev").innerHTML=v&&p?'<div class="calc-h '+(grade(v/p,"pct")||"none")+'"><span>'+brl(v/p)+' por pacote</span><span style="font-size:12px">Bom a partir de '+brl(cfg.metaPct)+'</span></div>':'<div class="calc-h none"><span>Valor e pacotes são opcionais</span></div>'; };
    $("#tf").addEventListener("input",prev); prev();
    $("#tGo").onclick=()=>{ Trip.start($("#t_valor").value,$("#t_pct").value); closeSheet(); toast("Cronômetro ligado"); };
    setTimeout(()=>{ if(!pre||!pre.valor) $("#t_valor").focus(); },250);
    return;
  }
  sheet("Cronômetro",'<div id="tBody" class="stack"></div>',
    '<div class="btns"><button class="btn ghost" type="button" id="tPause"></button><button class="btn" type="button" id="tStop">Finalizar</button></div>'+
    '<button class="btn ghost" type="button" id="tMap">'+I.map+'Ver no mapa</button>',"trip");
  fillTripSheet();
  $("#tPause").onclick=()=>{ Trip.st.paused?Trip.resume():Trip.pause(); fillTripSheet(); };
  $("#tMap").onclick=()=>{ closeSheet(); go("rota"); setTimeout(()=>Mapa.fit(),300); };
  $("#tStop").onclick=()=>{
    const b=$("#tStop"); if(!b.dataset.arm){ b.dataset.arm=1; b.textContent="Confirmar"; b.className="btn warnb"; return; }
    const f=Trip.stop(); closeSheet();
    const done=stopsToday().filter(s=>s.status==="entregue");
    const agId=lsGet("rp_trip_agenda",null), ag=agId&&items.find(x=>x.id===agId); lsSet("rp_trip_agenda",null);
    openForm("entrega",null,{...(ag?{cd:ag.cd||"",cidade:ag.cidade||"",_agenda:ag.id}:{}),valor:f.valor?dec(f.valor,2):"",pacotes:f.pacotes?String(f.pacotes):String(done.reduce((a,s)=>a+num(s.pacotes),0)||""),
      paradas:done.length?String(done.length):"",km:f.km?dec(f.km,1):"",tempo:String(Math.max(1,Math.round(f.elapsedMs/60000)))});
    toast("Confira e confirme o lançamento");
  };
}
function fillTripSheet(){
  const b=$("#tBody"); if(!b) return;
  const st=Trip.st, L=Trip.live(), cell=(l,v,g)=>'<div><small>'+l+'</small><b class="num'+(g?" "+g:"")+'">'+(v==null?"—":v)+'</b></div>';
  const gps=st.gps==="ok"?'<span class="gps ok"><i></i>GPS ok</span>':st.gps==="off"?'<span class="gps bad"><i></i>GPS desligado</span>':'<span class="gps"><i></i>Procurando GPS…</span>';
  b.innerHTML='<div><div class="clock num">'+hms(Trip.elapsed())+'</div><div class="clock-sub num">'+dec(L.km,1)+' km · '+(st.paused?"pausado":"rodando")+' · '+gps+'</div></div>'+
    (L.tot?'<div class="stack8"><div class="sec-h"><span class="sub strong">'+L.dn+' de '+L.tot+' pacotes entregues</span><span class="sub">'+(L.eta?"termina em ~"+hm(L.eta):"")+'</span></div><div class="prog"><i style="width:'+Math.min(100,L.dn/L.tot*100)+'%"></i></div></div>':'')+
    '<div class="calc"><div class="calc-h '+(L.porHora!=null?grade(L.porHora,"hora")||"none":"none")+'"><span>'+(L.v?"Rendendo agora":"Sem valor da rota")+'</span><button type="button" class="link" id="tEdit" style="font-size:13px">Editar valor</button></div><div class="calc-g">'+
      cell("R$/hora",L.porHora!=null?brl(L.porHora):null,grade(L.porHora,"hora"))+cell("R$/km",L.porKm!=null?brl(L.porKm):null,grade(L.porKm,"km"))+cell("R$/pacote",L.porPct!=null?brl(L.porPct):null,grade(L.porPct,"pct"))+
      cell("Custo do km",brl(L.comb))+cell("Lucro real",L.v?brl(L.lucro):null)+cell("Lucro/hora",L.lucroHora!=null?brl(L.lucroHora):null)+'</div>'+
      '<div class="formula num">Valor '+brl(L.v)+(L.p?' · '+L.p+' pacotes':'')+' · custo = '+dec(L.km,1)+' km × '+brl(custoKm().total)+'/km</div></div>';
  const p=$("#tPause"); if(p) p.textContent=st.paused?"Retomar":"Pausar";
  $("#tEdit").onclick=()=>{
    sheet("Valor da rota",'<form class="grid" id="tf2" novalidate><div class="fld"><label for="t2v">Valor (R$)</label><input id="t2v" inputmode="decimal" value="'+esc(st.valor?dec(st.valor,2):"")+'"></div><div class="fld"><label for="t2p">Pacotes</label><input id="t2p" inputmode="numeric" value="'+esc(st.pacotes||"")+'"></div></form>',
      '<button class="btn" type="button" id="t2ok">Salvar</button>',"tripmeta");
    $("#t2ok").onclick=()=>{ Trip.setMeta($("#t2v").value,$("#t2p").value); openTrip(); };
  };
}

/* ---------- Custo por km (planilha "Custo por KM", 5 passos) ---------- */
function kmPorAno(){
  /* média dos últimos 30 dias (ou desde o primeiro lançamento, se o app é novo) levada para o ano */
  const t=today(), ds=lancs().map(x=>x.data).filter(d=>d&&d>=addDays(t,-29)&&d<=t).sort();
  if(!ds.length) return 0;
  const dias=Math.max(7,Math.round((parse(t)-parse(ds[0]))/86400000)+1), s=sums(ds[0],t);
  return s.km?Math.round(s.km/dias*365/100)*100:0;
}
function openCusto(){
  const c=JSON.parse(JSON.stringify(cfg.custo||{}));
  const man=(c.manut&&c.manut.length?c.manut:MANUT_DEF).map(m=>m.slice());
  const inp=(id,v,ph)=>'<input id="'+id+'" inputmode="decimal" value="'+esc(v==null||v===0?"":String(v).replace(".",","))+'" placeholder="'+(ph||"0")+'">';
  const fld=(id,l,v,ph)=>'<div class="fld"><label for="'+id+'">'+l+'</label>'+inp(id,v,ph)+'</div>';
  const kmSug=kmPorAno();
  sheet("Custo por km",
    '<p class="sub" style="margin:0">Quanto custa cada km rodado, contando o que chega depois: óleo, pneu, o carro valendo menos, seguro. O app usa esse número no lucro real de cada rota.</p>'+
    '<div class="stack8"><h4 class="step">1 · Combustível</h4><div class="grid">'+fld("ck_gas","Preço (R$/litro)",cfg.gas)+fld("ck_cons","Consumo (km/litro)",cfg.cons)+'</div></div>'+
    '<div class="stack8"><h4 class="step">2 · Manutenção</h4><p class="sub" style="margin:0">Quanto custa e quantos km dura cada item.</p>'+
      man.map((m,i)=>'<div class="mrow"><span class="t1" style="white-space:normal">'+esc(m[0])+'</span>'+inp("ck_mc"+i,m[1],"R$")+inp("ck_md"+i,m[2],"dura km")+'</div>').join("")+'</div>'+
    '<div class="stack8"><h4 class="step">3 · Depreciação do carro</h4><div class="grid">'+fld("ck_vh","Valor do carro hoje",c.vHoje)+fld("ck_va","Valor daqui a 1 ano",c.vAno)+
      '<div class="fld full"><label for="ck_ka">Km que você roda por ano</label>'+inp("ck_ka",c.kmAno,kmSug?String(kmSug):"30000")+
      (kmSug?'<button type="button" class="link" id="ck_kaUse" style="text-align:left;font-size:14px">Estimar pelos meus lançamentos: '+kmSug.toLocaleString("pt-BR")+' km/ano</button>':'')+'</div></div></div>'+
    '<div class="stack8"><h4 class="step">4 · Custos fixos do ano</h4><div class="grid">'+fld("ck_seg","Seguro / proteção",c.seguro)+fld("ck_ipva","IPVA + licenciamento",c.ipva)+
      fld("ck_cel","Celular + apps",c.cel)+fld("ck_eq","Equipamentos",c.equip,"capa, bota…")+'</div></div>'+
    '<div class="calc" id="ckOut"></div>'+
    '<p class="sub">Se a troca de óleo ou o pneu já estão aqui, não precisa lançar como gasto quando pagar: o app já desconta um pouco a cada km.</p>',
    '<button class="btn" type="button" id="ckSave">Salvar e usar</button>',"custo");
  const read=()=>{ const g=id=>$("#"+id).value.trim();
    return {manut:man.map((m,i)=>[m[0],g("ck_mc"+i),g("ck_md"+i)]),vHoje:g("ck_vh"),vAno:g("ck_va"),kmAno:g("ck_ka"),seguro:g("ck_seg"),ipva:g("ck_ipva"),cel:g("ck_cel"),equip:g("ck_eq")}; };
  const upd=()=>{
    const old={gas:cfg.gas,cons:cfg.cons}; cfg.gas=num($("#ck_gas").value)||old.gas; cfg.cons=num($("#ck_cons").value)||old.cons;
    const k=custoKm(read()); cfg.gas=old.gas; cfg.cons=old.cons;
    const g=(l,v)=>'<div><small>'+l+'</small><b class="num">'+brl(v)+'</b></div>';
    const ganho=(()=>{ const t=today(), s=sums(addDays(t,-29),t); return s.km?s.ganhos/s.km:0; })();
    $("#ckOut").innerHTML='<div class="calc-h good"><span>Custo total por km</span><span class="num" style="font-size:20px">'+brl(k.total)+'</span></div><div class="calc-g">'+
      g("Combustível",k.comb)+g("Manutenção",k.man)+g("Depreciação",k.dep)+g("Fixos",k.fix)+g("Custo invisível",k.inv)+
      (ganho?'<div><small>Seu ganho/km</small><b class="num">'+brl(ganho)+'</b></div>':'<div><small>Seu ganho/km</small><b>—</b></div>')+'</div>'+
      '<div class="formula">'+(ganho?(ganho>k.total?"Nos últimos 30 dias sobrou "+brl(ganho-k.total)+" por km. O custo levou "+dec(k.total/ganho*100,0)+"% do bruto.":"Nos últimos 30 dias o ganho por km não cobriu o custo: você rodou pagando para trabalhar."):
        "Rota abaixo de "+brl(k.total)+" por km não paga o carro.")+'</div>';
  };
  $("#scrim").addEventListener("input",upd); upd();
  if($("#ck_kaUse")) $("#ck_kaUse").onclick=()=>{ $("#ck_ka").value=String(kmSug); upd(); };
  $("#ckSave").onclick=()=>{
    cfg.custo=read(); cfg.gas=num($("#ck_gas").value)||cfg.gas; cfg.cons=num($("#ck_cons").value)||cfg.cons;
    const k=custoKm(); if(k.set) cfg.desg=Math.round(k.inv*100)/100;
    if(k.total>num(cfg.ruimKm)){ cfg.ruimKm=Math.ceil(k.total*100)/100; toast("Semáforo: rota abaixo de "+brl(cfg.ruimKm)+"/km agora fica vermelha"); } else toast("Custo por km salvo");
    saveCfg(); closeSheet();
  };
}

/* ---------- Controle semanal (planilha "Controle Semanal") ---------- */
function semana(f){
  const dias=["Segunda","Terça","Quarta","Quinta","Sexta","Sábado","Domingo"], ck=custoKm(), rows=[];
  for(let i=0;i<7;i++){
    const d=addDays(f,i), r={dia:dias[i],data:d,km:0,pct:0,bruto:0,comb:0,outros:0};
    lancs().filter(x=>(x.data||today())===d).forEach(x=>{ const v=num(x.valor);
      if(x.tipo==="abast") r.comb+=v; else if(isG(x.tipo)) r.outros+=v;
      else { r.bruto+=v; r.km+=num(x.km); r.outros+=num(x.pedagio); if(x.tipo==="entrega") r.pct+=num(x.pacotes); } });
    pnrs().filter(x=>(x.data||today())===d).forEach(x=>r.outros+=num(x.desconto));
    r.liq=r.bruto-r.comb-r.outros; rows.push(r);
  }
  const T={km:0,pct:0,bruto:0,comb:0,outros:0,liq:0}; rows.forEach(r=>Object.keys(T).forEach(k=>T[k]+=r[k]));
  /* a planilha multiplica os km pelo custo TOTAL, que já tem o combustível que o "líquido" descontou;
     aqui o custo invisível é só o que falta: manutenção + depreciação + fixos */
  const inv=T.km*ck.inv, real=T.liq-inv;
  return {rows,T,ck,inv,real};
}
function renderSemana(f){
  const box=$("#semBox"); if(hP!=="semana"){ box.hidden=true; return; } box.hidden=false;
  const S=semana(f), t=today(), m=v=>v?brl(v):"—";
  $("#semTab").innerHTML='<table><thead><tr><th>Dia</th><th>Km</th><th>Pct</th><th>Bruto</th><th>Comb.</th><th>Outros</th><th>Líquido</th><th>R$/km</th></tr></thead><tbody>'+
    S.rows.map(r=>'<tr'+(r.data===t?' class="hoje"':'')+'><td><b>'+r.dia.slice(0,3)+'</b><small>'+r.data.slice(8)+"/"+r.data.slice(5,7)+'</small></td><td>'+(r.km?dec(r.km,0):"—")+'</td><td>'+(r.pct||"—")+'</td><td>'+m(r.bruto)+'</td><td>'+m(r.comb)+'</td><td>'+m(r.outros)+'</td><td class="'+(r.liq<0?"neg":"")+'">'+(r.bruto||r.comb||r.outros?brl(r.liq):"—")+'</td><td>'+(r.km?brl(r.liq/r.km):"—")+'</td></tr>').join("")+
    '</tbody><tfoot><tr><td><b>Total</b></td><td>'+dec(S.T.km,0)+'</td><td>'+S.T.pct+'</td><td>'+brl(S.T.bruto)+'</td><td>'+brl(S.T.comb)+'</td><td>'+brl(S.T.outros)+'</td><td class="'+(S.T.liq<0?"neg":"")+'">'+brl(S.T.liq)+'</td><td>'+(S.T.km?brl(S.T.liq/S.T.km):"—")+'</td></tr></tfoot></table>';
  const row=(k,v,cls)=>'<div class="r"><span class="k">'+k+'</span><span class="v'+(cls?" "+cls:"")+'">'+v+'</span></div>';
  $("#semReal").innerHTML=
    row("Custo do carro por km"+(S.ck.set?"":" <small class=\"muted\">(estimado)</small>"),brl(S.ck.inv))+
    row("Custo invisível da semana ("+dec(S.T.km,0)+" km)","−"+brl(S.inv),"neg")+
    row("<b>Lucro real da semana</b>","<b>"+brl(S.real)+"</b>",S.real<0?"neg":"pos")+
    row("Ganho bruto por km",S.T.km?brl(S.T.bruto/S.T.km):"—")+
    row("Lucro real por km",S.T.km?brl(S.real/S.T.km):"—",S.T.km&&S.real<0?"neg":"");
}
function exportSemana(){
  const [f]=hRange(), S=semana(f), q=v=>String(v).replace(".",",");
  const L=["Dia;Data;Km rodados;Pacotes;Ganho bruto;Combustível;Outros gastos;Líquido do dia;Líquido/km"];
  S.rows.forEach(r=>L.push([r.dia,r.data.split("-").reverse().join("/"),q(r.km),r.pct,q(r.bruto.toFixed(2)),q(r.comb.toFixed(2)),q(r.outros.toFixed(2)),q(r.liq.toFixed(2)),r.km?q((r.liq/r.km).toFixed(2)):""].join(";")));
  L.push(["TOTAL","",q(S.T.km),S.T.pct,q(S.T.bruto.toFixed(2)),q(S.T.comb.toFixed(2)),q(S.T.outros.toFixed(2)),q(S.T.liq.toFixed(2)),S.T.km?q((S.T.liq/S.T.km).toFixed(2)):""].join(";"));
  L.push("",["Custo do carro por km (manutenção + depreciação + fixos)","","","","","","","",q(S.ck.inv.toFixed(2))].join(";"),
    ["Custo invisível da semana","","","","","","","",q(S.inv.toFixed(2))].join(";"),["LUCRO REAL da semana","","","","","","","",q(S.real.toFixed(2))].join(";"));
  saveText("rota-paga-semana-"+f+".csv","text/csv","\ufeff"+L.join("\n"));
}

/* ---------- Fretômetro ---------- */
function openFretometro(){
  const fld=(id,l,v,ph)=>'<div class="fld"><label for="'+id+'">'+l+'</label><input id="'+id+'" inputmode="decimal" value="'+esc(v)+'"'+(ph?' placeholder="'+ph+'"':'')+'></div>';
  sheet("Fretômetro",'<p class="sub" style="margin:0">Some a ida e a volta. O app soma combustível, desgaste, pedágio e o valor da sua hora.</p>'+
    '<form id="fr" class="grid" novalidate>'+fld("fr_km","Km total (ida + volta)","","120")+fld("fr_h","Horas de trabalho","","4")+
    fld("fr_ped","Pedágio (R$)","","0")+fld("fr_marg","Margem de lucro (%)","20")+
    fld("fr_cons","Consumo km/L",fmtCfg(cfg.cons))+fld("fr_gas","Combustível R$/L",fmtCfg(cfg.gas))+
    fld("fr_desg","Desgaste R$/km",fmtCfg(cfg.desg))+fld("fr_hora","Sua hora (R$)",fmtCfg(cfg.metaHora))+
    '<div class="fld full"><label for="fr_of">Valor oferecido (opcional)</label><input id="fr_of" inputmode="decimal" placeholder="Para saber se compensa"></div></form><div class="calc" id="frOut"></div>',
    '<button class="btn" id="frGo" type="button">Lançar como frete</button>');
  const g=id=>num($("#"+id).value); let sug=0;
  const upd=()=>{
    const km=g("fr_km"), h=g("fr_h"), cons=g("fr_cons"), comb=km&&cons?km/cons*g("fr_gas"):0, desg=km*g("fr_desg"), ped=g("fr_ped");
    const custo=comb+desg+ped, min=custo+h*g("fr_hora"); sug=min*(1+g("fr_marg")/100);
    const of=g("fr_of"); let v="none", lbl="Preencha km e horas";
    if(km){ if(of){ v=of>=sug?"good":of>=min?"warn":"bad"; lbl=of>=sug?"Compensa":of>=min?"Cobre os custos, margem baixa":"Prejuízo"; } else { v="good"; lbl="Preço sugerido"; } }
    const cell=(l,val)=>'<div><small>'+l+'</small><b class="num">'+val+'</b></div>';
    $("#frOut").innerHTML='<div class="calc-h '+v+'"><span>'+lbl+'</span><span class="num" style="font-size:18px">'+(km?brl(sug):"")+'</span></div><div class="calc-g">'+
      cell("Combustível",brl(comb))+cell("Desgaste",brl(desg))+cell("Pedágio",brl(ped))+cell("Custo",brl(custo))+cell("Mínimo",brl(min))+cell("Por km",km?brl(sug/km):"—")+'</div>'+
      (of&&km?'<div class="formula num">Oferta '+brl(of)+' − custo '+brl(custo)+' = '+brl(of-custo)+(h?' · '+brl((of-custo)/h)+'/h':'')+'</div>':'');
  };
  $("#fr").addEventListener("input",upd); upd(); setTimeout(()=>$("#fr_km").focus(),250);
  $("#frGo").onclick=()=>{ const of=g("fr_of"), v=of||sug; openForm("frete",null,{valor:v?dec(v,2):"",km:$("#fr_km").value,tempo:g("fr_h")?String(Math.round(g("fr_h")*60)):"",pedagio:$("#fr_ped").value,cons:$("#fr_cons").value}); };
}

/* ================= leitura de notificação ================= */
function parseNotif(s){
  const t=String(s||"").replace(/\s+/g," "), o={};
  const v=t.match(/R\$\s*([\d.]+(?:,\d{1,2})?|\d+(?:\.\d{1,2})?)/i); if(v) o.valor=v[1];
  const p=t.match(/(\d+)\s*(?:pacotes?|pcts?|volumes?|itens|encomendas?)/i); if(p) o.pacotes=p[1];
  const k=t.match(/(\d+(?:[.,]\d+)?)\s*km\b/i); if(k) o.km=k[1].replace(".",",");
  const pa=t.match(/(\d+)\s*(?:paradas?|endere[çc]os?|entregas?|stops?)/i); if(pa) o.paradas=pa[1];
  const mi=t.match(/(\d+)\s*(?:min\b|minutos)/i), h=t.match(/(\d+(?:[.,]\d+)?)\s*h(?:oras?)?\b/i);
  if(mi) o.tempo=mi[1]; else if(h) o.tempo=String(Math.round(num(h[1])*60));
  return o;
}
function showDetect(text,app){
  const d=$("#detect"); d.hidden=false;
  if(text){ $("#detText").value=text; $("#detTitle").textContent="Rota detectada"+(app?" · "+app:""); $("#detSub").textContent="Veja se compensa antes de aceitar no app de entregas."; }
  updDetect();
  if(view!=="inicio") show("inicio");
  setTimeout(()=>{ d.scrollIntoView({behavior:"smooth",block:"start"}); if(!text) $("#detText").focus(); },150);
}
function updDetect(){
  const txt=$("#detText").value, o=parseNotif(txt), keys=[["valor","Valor",x=>brl(num(x))],["pacotes","Pacotes",x=>x],["km","Km",x=>x],["paradas","Paradas",x=>x],["tempo","Min",x=>x]];
  $("#detFound").innerHTML=txt.trim()?keys.map(([k,l,f])=>o[k]?'<span class="pill num">'+l+': '+esc(f(o[k]))+'</span>':'<span class="pill miss">'+l+' não achado</span>').join(""):"";
  const c=$("#detCalc");
  if(o.valor&&(o.pacotes||o.km)){ c.hidden=false; c.innerHTML=calcHtml("entrega",{valor:o.valor,pacotes:o.pacotes,km:o.km,paradas:o.paradas,tempo:o.tempo}); } else c.hidden=true;
  $("#detGo").disabled=!Object.keys(o).length; $("#detTrip").disabled=Trip.st.running; $("#detAgenda").disabled=!o.valor;
  return o;
}
function closeDetect(){ $("#detect").hidden=true; $("#detText").value=""; $("#detTitle").textContent="Rota detectada"; updDetect(); }

/* eventos vindos do Android */
window.__rota=function(type,data){
  try{
    if(type==="route"){ nat("stopAlarm"); closeSheet(); showDetect(data&&data.text||String(data||""),data&&data.app); }
    else if(type==="trip"){ Trip.refresh(); Trip.ui(); Trip.loop(); renderInicio(); if(data==="denied") toast("Sem a localização, o cronômetro conta só o tempo."); }
    else if(type==="resume"){ renderAlert(); Conta.refresh(); Trip.refresh(); Trip.ui(); Trip.loop(); const p=nat("takePendingRoute"); if(p){ const j=JSON.parse(p); showDetect(j.text,j.app); } }
  }catch(e){}
};
window.__appBack=function(){
  if($("#scrim")){ closeSheet(); return true; }
  if(!$("#pickHint").hidden){ Mapa.setPick(null); return true; }
  if(!$("#detect").hidden){ closeDetect(); return true; }
  if(view!=="inicio"){ go("inicio"); return true; }
  return false;
};

/* ================= exportar / backup ================= */
function saveText(name,mime,text){
  if(N&&nat("shareFile",name,mime,text)!==null) return;
  const a=document.createElement("a"); a.href=URL.createObjectURL(new Blob([text],{type:mime})); a.download=name; document.body.appendChild(a); a.click(); setTimeout(()=>a.remove(),500);
}
function exportCsv(){
  const cols=["data","tipo","valor","km","paradas","pacotes","tempo","pedagio","litros","cliente","cidade","cd","descricao","posto"];
  const q=v=>{ v=String(v??""); return /[;"\n]/.test(v)?'"'+v.replace(/"/g,'""')+'"':v; };
  const rows=lancs().sort((a,b)=>(a.data||"").localeCompare(b.data||"")).map(x=>cols.map(c=>c==="tipo"?(T[x.tipo]||{}).n||x.tipo:x[c]).map(q).join(";"));
  pnrs().forEach(p=>rows.push([p.data,"PNR",p.desconto?"-"+p.desconto:"","","","","","","","",p.endereco,"",(p.codigo||"")+" "+(p.motivo||""),""].map(q).join(";")));
  saveText("rota-paga-"+today()+".csv","text/csv","﻿"+cols.join(";")+"\n"+rows.join("\n"));
}
function backup(){ saveText("rota-paga-backup-"+today()+".json","application/json",JSON.stringify({app:"rota-paga",v:1,em:new Date().toISOString(),items,cfg})); }
/** Junta um backup aos dados atuais (não apaga nada; só traz o que falta). Devolve quantos registros entraram. */
function mergeBackup(j){
  if(!j||!Array.isArray(j.items)) throw new Error("backup inválido");
  const ids=new Set(items.map(x=>x.id)); let n=0; j.items.forEach(x=>{ if(x&&x.id&&!ids.has(x.id)){ items.push(x); n++; } });
  if(j.cfg) cfg={...DEF_CFG,...j.cfg,...{tema:cfg.tema}}; persist(); applyTema(); pushAlertCfg(); renderAll(); return n;
}
const backupJson=()=>JSON.stringify({app:"rota-paga",v:1,em:new Date().toISOString(),items,cfg});
async function restore(){
  const f=await pickFile(".json,application/json"); if(!f) return;
  try{ toast(mergeBackup(JSON.parse(await f.text()))+" registros restaurados"); }
  catch(e){ toast("Esse arquivo não é um backup do Rota Paga."); }
}

/* ================= conta (servidor de acessos) ================= */
const Conta=(()=>{
  let st=lsGet("rp_conta",null)||{};
  const save=()=>lsSet("rp_conta",st);
  const srv=()=>String(lsGet("rp_srv","")||window.ROTA_SERVIDOR||"").trim();
  function aparelho(){
    let id=lsGet("rp_dev",""); if(!id){ id="d_"+newId()+newId(); lsSet("rp_dev",id); }
    const s=natJ("alertStatus")||{};
    return {id,nome:s.modelo?((s.fabricante||"")+" "+s.modelo+" · Android "+(s.android||"")).trim():navigator.userAgent.slice(0,110)};
  }
  async function call(a,data,url){
    const u=(url||srv()); if(!u) throw new Error("O servidor de contas ainda não foi configurado.");
    let r; try{ r=await fetch(u.replace(/\/?$/,"/")+"?a="+a,{method:"POST",headers:{"Content-Type":"text/plain"},
      body:JSON.stringify({...(data||{}),a,token:st.token||undefined,versao:nat("version")||"web",aparelho:aparelho()})}); }
    catch(e){ throw new Error("Sem conexão com o servidor. Confira a internet."); }
    let j; try{ j=await r.json(); }catch(e){ throw new Error("O servidor respondeu algo estranho ("+r.status+"). Confira o endereço."); }
    if(!j.ok) throw new Error(j.erro||"Erro no servidor");
    return j;
  }
  function absorb(j){
    st.modo=j.modo; st.aviso=j.aviso||""; st.usuario=j.usuario||null; st.acesso=j.acesso||{liberado:true}; st.cadastro=j.cadastro_aberto!==false; st.checkedAt=Date.now();
    if(j.token) st.token=j.token;
    if(st.token&&!j.usuario) st.token=null; /* token não vale mais (desconectado pelo painel) */
    save(); ui();
  }
  async function refresh(){
    if(!srv()) { ui(); return; }
    try{ absorb(await call("status",{})); autoBackup(); }catch(e){ ui(); }
  }
  /* bloqueia o app só quando o servidor mandou (modo "conta" ou "assinatura"); sem internet, vale a última resposta por 7 dias */
  function bloqueado(){
    if(!srv()||!st.acesso||st.acesso.liberado) return false;
    if(st.checkedAt&&Date.now()-st.checkedAt>7*864e5&&!st.usuario) return true;
    return true;
  }
  function ui(){
    const av=$("#avisoBox"); if(av){ av.hidden=!st.aviso; av.textContent=st.aviso||""; }
    renderConta(); gate();
  }
  function gate(){
    const g=$("#gate"), b=bloqueado(); g.hidden=!b; document.body.classList.toggle("gated",b);
    if(!b) return;
    const m=st.acesso||{};
    $("#gateTitle").textContent=m.motivo==="vencido"||m.motivo==="sem_plano"?"Seu acesso precisa ser renovado":m.motivo==="bloqueado"?"Conta bloqueada":m.motivo==="pendente"?"Conta em análise":"Entre na sua conta";
    $("#gateMsg").textContent=m.mensagem||"";
    $("#gateForm").innerHTML=st.usuario?'<p class="sub">Conectado como <b>'+esc(st.usuario.email)+'</b>.</p><button class="btn ghost" type="button" id="gOut">Entrar com outra conta</button>':authForm("g");
    if(st.usuario) $("#gOut").onclick=sair; else bindAuth("g");
  }
  function authForm(p,tab){
    tab=tab||(st.cadastro===false?"entrar":"entrar");
    return '<div class="seg" id="'+p+'Tabs"><button type="button" data-t="entrar" aria-pressed="'+(tab==="entrar")+'">Entrar</button>'+(st.cadastro!==false?'<button type="button" data-t="criar" aria-pressed="'+(tab==="criar")+'">Criar conta</button>':'')+'</div>'+
      '<form class="stack" id="'+p+'Auth" novalidate data-tab="'+tab+'">'+
      (tab==="criar"?'<div class="fld"><label for="'+p+'Nome">Nome</label><input id="'+p+'Nome" autocomplete="name"></div>':'')+
      '<div class="fld"><label for="'+p+'Email">E-mail</label><input id="'+p+'Email" type="email" autocomplete="email" inputmode="email"></div>'+
      (tab==="criar"?'<div class="fld"><label for="'+p+'Tel">WhatsApp (opcional)</label><input id="'+p+'Tel" inputmode="tel" autocomplete="tel"></div>':'')+
      '<div class="fld"><label for="'+p+'Senha">Senha'+(tab==="criar"?" (mínimo 6)":"")+'</label><input id="'+p+'Senha" type="password" autocomplete="'+(tab==="criar"?"new-password":"current-password")+'"></div>'+
      '<p class="err" id="'+p+'Err" hidden></p>'+
      '<button class="btn" type="submit">'+(tab==="criar"?"Criar conta":"Entrar")+'</button></form>';
  }
  function bindAuth(p,after){
    const box=$("#"+p+"Auth").parentElement;
    $("#"+p+"Tabs").onclick=e=>{ const b=e.target.closest("[data-t]"); if(!b) return; const html=authForm(p,b.dataset.t);
      const tmp=document.createElement("div"); tmp.innerHTML=html; $("#"+p+"Tabs").replaceWith(tmp.children[0]); $("#"+p+"Auth").replaceWith(tmp.children[0]); bindAuth(p,after); };
    $("#"+p+"Auth").onsubmit=async e=>{
      e.preventDefault(); const f=e.target, tab=f.dataset.tab, err=$("#"+p+"Err"), btn=f.querySelector("button[type=submit]");
      const v=id=>($("#"+p+id)||{}).value||"";
      err.hidden=true; btn.disabled=true; btn.textContent="Aguarde…";
      try{
        const j=tab==="criar"?await call("registrar",{nome:v("Nome").trim(),email:v("Email").trim(),telefone:v("Tel"),senha:v("Senha")}):await call("entrar",{email:v("Email").trim(),senha:v("Senha")});
        absorb(j); toast(tab==="criar"?"Conta criada":"Pronto, você entrou"); if(after) after(); offerRestore();
      }catch(x){ err.textContent=x.message; err.hidden=false; btn.disabled=false; btn.textContent=tab==="criar"?"Criar conta":"Entrar"; }
    };
    void box;
  }
  async function offerRestore(){
    try{ const i=await call("backup_info",{}); if(i.backup&&!items.length){ const b=await call("backup_baixar",{}); const n=mergeBackup(JSON.parse(b.dados)); toast(n+" registros trazidos do seu backup na nuvem"); } }catch(e){}
  }
  async function sair(){ try{ await call("sair",{}); }catch(e){} st.token=null; st.usuario=null; save(); refresh(); toast("Você saiu da conta"); }
  async function backupUp(quiet){ const j=await call("backup_enviar",{dados:backupJson()}); lsSet("rp_bk_at",Date.now()); if(!quiet) toast("Backup enviado para a nuvem"); renderConta(); return j; }
  async function backupDown(){ const b=await call("backup_baixar",{}); const n=mergeBackup(JSON.parse(b.dados)); toast(n+" registros novos trazidos da nuvem"); }
  function autoBackup(){ if(!st.token||!items.length) return; if(Date.now()-(lsGet("rp_bk_at",0)||0)>20*3600e3) backupUp(true).catch(()=>{}); }
  function openLogin(){
    if(!srv()){ toast("O login ainda não está ligado nesta versão do app."); return; }
    sheet("Sua conta",'<p class="sub" style="margin:0">Com a conta, seus lançamentos ficam guardados na nuvem e voltam se você trocar de celular.</p><div class="stack" id="shAuthBox">'+authForm("s")+'</div>',"","auth");
    bindAuth("s",()=>closeSheet());
  }
  function openSenha(){
    sheet("Trocar senha",'<form class="stack" id="pwF" novalidate><div class="fld"><label for="pw1">Senha atual</label><input id="pw1" type="password" autocomplete="current-password"></div><div class="fld"><label for="pw2">Senha nova (mínimo 6)</label><input id="pw2" type="password" autocomplete="new-password"></div><p class="err" id="pwErr" hidden></p><button class="btn" type="submit">Trocar senha</button></form>',"","pw");
    $("#pwF").onsubmit=async e=>{ e.preventDefault(); try{ await call("senha",{senha_atual:$("#pw1").value,senha_nova:$("#pw2").value}); closeSheet(); toast("Senha trocada"); }catch(x){ $("#pwErr").textContent=x.message; $("#pwErr").hidden=false; } };
  }
  function openServidor(){
    sheet("Servidor de contas",'<p class="sub" style="margin:0">Endereço da pasta api do servidor (termina em /api/). Isto é para quem administra o app.</p>'+
      '<div class="fld"><label for="srvU">Endereço</label><input id="srvU" inputmode="url" value="'+esc(srv())+'" placeholder="https://seudominio.com.br/rotapaga/api/"></div><p class="sub" id="srvR"></p>',
      '<div class="btns"><button class="btn ghost" type="button" id="srvT">Testar</button><button class="btn" type="button" id="srvS">Salvar</button></div>',"srv");
    $("#srvT").onclick=async()=>{ $("#srvR").textContent="Testando…"; try{ const j=await call("status",{},$("#srvU").value.trim()); $("#srvR").textContent="Funcionando: "+j.servico+" · modo "+j.modo; }catch(x){ $("#srvR").textContent=x.message; } };
    $("#srvS").onclick=()=>{ lsSet("rp_srv",$("#srvU").value.trim()); st={}; save(); closeSheet(); refresh(); toast("Servidor salvo"); };
  }
  function renderConta(){
    const box=$("#contaBox"); if(!box) return;
    if(!srv()){ box.innerHTML='<div class="r"><span class="ico">'+I.user+'</span><span class="k">Conta e backup na nuvem<span class="t2">Em breve</span></span></div>'; return; }
    const u=st.usuario;
    if(!u){ box.innerHTML='<button class="r" type="button" id="cEnt"><span class="ico">'+I.user+'</span><span class="k">Entrar ou criar conta<span class="t2">Guarde seus lançamentos na nuvem</span></span><span class="v muted">›</span></button>'; $("#cEnt").onclick=openLogin; return; }
    const val=u.expira?"válido até "+u.expira.split("-").reverse().join("/"):(st.modo==="aberto"?"acesso liberado":"sem validade");
    const bk=lsGet("rp_bk_at",0);
    box.innerHTML='<div class="r"><span class="ico">'+I.user+'</span><span class="k">'+esc(u.nome)+'<span class="t2">'+esc(u.email)+' · '+esc(u.plano?u.plano+" · ":"")+val+'</span></span></div>'+
      '<button class="r" type="button" id="cUp"><span class="ico">'+I.up+'</span><span class="k">Enviar backup para a nuvem<span class="t2">'+(bk?"Último: "+new Date(bk).toLocaleString("pt-BR",{day:"2-digit",month:"2-digit",hour:"2-digit",minute:"2-digit"})+" · automático 1 vez por dia":"Automático 1 vez por dia")+'</span></span><span class="v muted">›</span></button>'+
      '<button class="r" type="button" id="cDown"><span class="ico">'+I.down+'</span><span class="k">Trazer backup da nuvem<span class="t2">Junta com o que já está neste celular</span></span><span class="v muted">›</span></button>'+
      '<button class="r" type="button" id="cPw"><span class="ico">'+I.gear+'</span><span class="k">Trocar senha</span><span class="v muted">›</span></button>'+
      '<button class="r" type="button" id="cOut"><span class="ico neg">'+I.user+'</span><span class="k">Sair da conta</span></button>';
    $("#cUp").onclick=()=>backupUp().catch(x=>toast(x.message));
    $("#cDown").onclick=()=>backupDown().catch(x=>toast(x.message));
    $("#cPw").onclick=openSenha;
    $("#cOut").onclick=()=>{ const b=$("#cOut .k"); if(!b.dataset.arm){ b.dataset.arm=1; b.textContent="Toque de novo para sair"; return; } sair(); };
  }
  return {refresh,openLogin,openServidor,renderConta,ui,get st(){return st;}};
})();

/* ================= ligações ================= */
document.querySelectorAll(".tab").forEach(b=>b.onclick=()=>go(b.dataset.tab));
document.querySelectorAll("[data-go]").forEach(b=>b.onclick=()=>go(b.dataset.go));
$("#fab").onclick=openMenu;
$("#agNew").onclick=()=>openForm("agenda",null,{data:addDays(today(),1)});
$("#startCard").onclick=()=>openTrip();
$("#tripbar").onclick=()=>openTrip();
$("#tripbar").onkeydown=e=>{ if(e.key==="Enter"||e.key===" "){ e.preventDefault(); openTrip(); } };
$("#btnAlert").onclick=()=>showDetect();
$("#detClose").onclick=closeDetect;
$("#detText").addEventListener("input",updDetect);
$("#detGo").onclick=()=>openForm("entrega",null,updDetect());
$("#detTrip").onclick=()=>{ const o=updDetect(); closeDetect(); openTrip({valor:o.valor,pacotes:o.pacotes}); };
$("#detAgenda").onclick=()=>{ const o=updDetect(); closeDetect(); openForm("agenda",null,{data:addDays(today(),1),valor:o.valor||"",pacotes:o.pacotes||"",km:o.km||"",paradas:o.paradas||""}); };
document.addEventListener("click",e=>{
  const ag=e.target.closest("[data-agstart]"); if(ag){ startAgenda(ag.dataset.agstart); return; }
  const n=e.target.closest("[data-nav]"); if(n){ const s=items.find(x=>x.id===n.dataset.nav); if(s) openExt(navLink(s)); return; }
  const b=e.target.closest("[data-edit]"); if(!b||b.closest(".sheet")) return;
  const it=items.find(x=>x.id===b.dataset.edit); if(!it) return;
  const k=kindOf(it); openForm(k==="lanc"?it.tipo:k,it);
});
$("#addStop").addEventListener("submit",e=>{ e.preventDefault(); const a=$("#stAddr").value.trim(); if(!a){ $("#stAddr").focus(); return; }
  addStops([{endereco:a,pacotes:$("#stPkg").value.trim()}]); $("#stAddr").value=""; $("#stPkg").value=""; $("#stAddr").focus(); });
$("#btnPasteList").onclick=()=>{ const b=$("#pasteBox"); b.hidden=!b.hidden; if(!b.hidden) $("#pasteText").focus(); };
$("#pasteAdd").onclick=()=>{
  const lines=$("#pasteText").value.split(/\n+/).map(s=>s.trim()).filter(Boolean);
  if(!lines.length){ toast("Cole pelo menos um endereço"); return; }
  addStops(lines.map(l=>{ const m=l.match(/^(.*?)\s*[-–|;]\s*(\d{1,3})\s*(?:pct|pacotes?)?$/i); return m?{endereco:m[1].trim(),pacotes:m[2]}:{endereco:l,pacotes:""}; }));
  $("#pasteText").value=""; $("#pasteBox").hidden=true; toast(lines.length+" paradas adicionadas");
};
$("#btnPick").onclick=()=>{ if(!window.L){ toast("O mapa precisa de internet"); return; }
  Mapa.setPick(async(lat,lon)=>{ const end=await Geo.reverse(lat,lon); addStops([{endereco:end,pacotes:$("#stPkg").value.trim(),lat,lon}]); $("#stPkg").value=""; toast("Parada marcada no mapa"); });
  window.scrollTo({top:0,behavior:"smooth"}); };
$("#pickCancel").onclick=()=>Mapa.setPick(null);
$("#mLocate").onclick=async()=>{ const p=await locate(10000); if(p) Mapa.center([p[0],p[1]]), Mapa.draw(); else toast("Não consegui pegar sua localização"); };
$("#mFit").onclick=()=>Mapa.fit();
$("#calcRoutes").onclick=()=>Rotas.calc();
$("#clearDone").onclick=()=>{ const b=$("#clearDone"); if(!b.dataset.arm){ b.dataset.arm=1; b.textContent="Confirmar"; setTimeout(()=>{ b.dataset.arm=""; b.textContent="Limpar feitas"; },3000); return; }
  stopsToday().filter(s=>s.status==="entregue"||s.status==="falha").forEach(s=>{ items=items.filter(x=>x.id!==s.id); }); persist(); Rotas.reset(); renderAll(); b.dataset.arm=""; b.textContent="Limpar feitas"; };
$("#finishRoute").onclick=()=>{
  if(Trip.st.running){ openTrip(); return; }
  const l=stopsToday(), d=l.filter(s=>s.status==="entregue"), r=Rotas.chosen;
  openForm("entrega",null,{paradas:String(d.length||l.length),pacotes:String(d.reduce((a,s)=>a+num(s.pacotes),0)||l.reduce((a,s)=>a+num(s.pacotes),0)||""),km:r&&r.km?dec(r.km,1):""}); };

document.querySelectorAll("#hSeg button").forEach(b=>b.onclick=()=>{ hP=b.dataset.p; hRef=today(); renderHist(); });
document.querySelectorAll("#hTab button").forEach(b=>b.onclick=()=>{ hTab=b.dataset.t; renderHist(); });
$("#semCsv").onclick=exportSemana; $("#semCusto").onclick=openCusto;
$("#hPrev").onclick=()=>hShift(-1); $("#hNext").onclick=()=>hShift(1);

$("#a_tParada").innerHTML=[1,2,3,4,5,6,8,10,15].map(n=>'<option value="'+n+'">'+n+' minuto'+(n>1?"s":"")+'</option>').join("");
const bindCfg=(id,key,isNum,after)=>$(id).addEventListener("change",e=>{ const v=e.target.value.trim(); cfg[key]=isNum?num(v):v; if(after) after(); saveCfg(); });
bindCfg("#a_nome","nome"); bindCfg("#a_nav","nav"); bindCfg("#a_cidade","cidade");
bindCfg("#a_casa","casa",0,()=>{ delete cfg.casaLL; }); bindCfg("#a_trabalho","trabalho",0,()=>{ delete cfg.trabalhoLL; setTimeout(Geo.kick,100); });
bindCfg("#a_tParada","tParada",1); bindCfg("#a_cons","cons",1); bindCfg("#a_gas","gas",1); $("#a_custo").onclick=openCusto;
["Pct","Km","Hora"].forEach(k=>{ bindCfg("#a_meta"+k,"meta"+k,1); bindCfg("#a_ruim"+k,"ruim"+k,1); });
$("#a_veiculo").addEventListener("change",e=>{ const v=e.target.value; cfg.veiculo=v; if(VEIC[v]){ if(VEIC[v].cons) cfg.cons=VEIC[v].cons; if(!custoKm().set) cfg.desg=VEIC[v].desg; } saveCfg(); toast("Consumo e desgaste ajustados para "+e.target.selectedOptions[0].text.toLowerCase()); });
document.querySelectorAll("#aTema button").forEach(b=>b.onclick=()=>{ cfg.tema=b.dataset.v; saveCfg(); });
let verTaps=0; $("#ver").onclick=()=>{ verTaps++; clearTimeout(verTaps.t); setTimeout(()=>verTaps=0,2500); if(verTaps>=5){ verTaps=0; Conta.openServidor(); } };
$("#gateRetry").onclick=()=>Conta.refresh();
$("#btnExport").onclick=exportCsv; $("#btnBackup").onclick=backup; $("#btnRestore").onclick=restore;
$("#alGrant").onclick=()=>nat("openAlertAccess");
$("#ovGrant").onclick=()=>nat("openOverlayPermission");
$("#ovOn").onchange=e=>{ cfg.overlayOn=e.target.checked; saveCfg(); if(e.target.checked&&!(natJ("alertStatus")||{}).overlay) nat("openOverlayPermission"); };
$("#bbOn").onchange=e=>{ cfg.bubbleOn=e.target.checked; saveCfg(); if(e.target.checked&&!(natJ("alertStatus")||{}).overlay) nat("openOverlayPermission"); };
$("#alOn").onchange=e=>{ cfg.alertOn=e.target.checked; saveCfg(); };
$("#alSecs").onchange=e=>{ cfg.alertSecs=+e.target.value; saveCfg(); };
$("#alWords").onchange=e=>{ cfg.alertWords=e.target.value; saveCfg(); };
$("#alApps").addEventListener("change",e=>{ const c=e.target.closest("[data-app]"); if(!c) return; const s=new Set(cfg.alertApps||[]); c.checked?s.add(c.dataset.app):s.delete(c.dataset.app); cfg.alertApps=[...s]; saveCfg(); });
$("#cardTest").onclick=()=>{ pushAlertCfg(); const r=nat("testCard");
  if(r==="permissao"){ toast("Falta liberar Aparecer sobre outros apps"); nat("openOverlayPermission"); return; }
  toast("Cartão de teste enviado. Ele deve aparecer no topo da tela.");
  setTimeout(()=>{ const s=natJ("alertStatus")||{}; renderDiag(s); if(s.cardRes&&s.cardRes!=="mostrado") toast("O cartão não apareceu: "+s.cardRes); },900); };
$("#diag").addEventListener("click",e=>{ const b=e.target.closest("[data-fix]"); if(b) nat(b.dataset.fix); });
$("#logClear").onclick=()=>{ nat("clearLog"); renderAlert(); };
$("#alTest").onclick=()=>{ pushAlertCfg(); nat("testAlert"); toast("Alarme de teste disparado"); };
window.addEventListener("hashchange",onHash);
document.addEventListener("visibilitychange",()=>{ if(!document.hidden) window.__rota("resume"); });

/* ================= início ================= */
items=lsGet("rp_items",[]); cfg={...DEF_CFG,...lsGet("rp_cfg",{})}; hRef=today();
$("#ver").textContent="Rota Paga · versão "+(nat("version")||"web");
applyTema(); pushAlertCfg(); Trip.boot(); renderAll(); onHash(); Conta.ui(); Conta.refresh();
setTimeout(()=>window.__rota("resume"),300);
})();
