<?php
/* Painel do Rota Paga: gerenciar acessos, usuários e o modo do app. */
require_once __DIR__ . '/../inc/lib.php';

const CSS = <<<'CSS'
:root{--bg:#F2F5F3;--s:#fff;--ink:#13201A;--mu:#5B6A63;--li:#D9E1DC;--ac:#0A6B4B;--soft:#E2EFE8;--good:#1D8A4E;--warn:#A86E12;--bad:#BF3B28;--gb:#E1F3E8;--wb:#FBF0DA;--bb:#FBE4E0}
@media(prefers-color-scheme:dark){:root{--bg:#0E1412;--s:#171F1C;--ink:#E7EEEA;--mu:#93A39B;--li:#26322D;--ac:#3DB883;--soft:#1B2C25;--good:#4CC583;--warn:#E3AC45;--bad:#EE6C57;--gb:#16301F;--wb:#33280F;--bb:#3A1B16;color-scheme:dark}}
*{box-sizing:border-box}body{margin:0;background:var(--bg);color:var(--ink);font:15px/1.5 system-ui,-apple-system,"Segoe UI",Roboto,sans-serif}
a{color:var(--ac)}h1{font-size:26px;margin:8px 0 4px}h2{font-size:15px;text-transform:uppercase;letter-spacing:.06em;margin:22px 0 10px;color:var(--mu)}
header{display:flex;flex-wrap:wrap;align-items:center;gap:6px 16px;padding:12px 20px;background:#0B5E42;color:#fff}
.logo{font-size:18px;letter-spacing:.04em;margin-right:8px}header nav{display:flex;flex-wrap:wrap;gap:4px;flex:1}
header nav a,.out{color:#fff;text-decoration:none;padding:6px 10px;border-radius:8px;opacity:.85}header nav a.on{background:rgba(255,255,255,.18);opacity:1}
main{max-width:1100px;margin:0 auto;padding:18px 20px 60px}
.muted{color:var(--mu)}small{display:block;color:var(--mu);font-size:13px}
.cards{display:grid;grid-template-columns:repeat(auto-fill,minmax(160px,1fr));gap:10px}
.card,.box{background:var(--s);border:1px solid var(--li);border-radius:14px;padding:14px}.card b{font-size:30px;display:block}
.good{color:var(--good)}.warn{color:var(--warn)}.bad{color:var(--bad)}
.chip{font-size:12px;font-weight:700;text-transform:uppercase;padding:2px 8px;border-radius:999px;white-space:nowrap;vertical-align:middle}
.chip.good{background:var(--gb);color:var(--good)}.chip.warn{background:var(--wb);color:var(--warn)}.chip.bad{background:var(--bb);color:var(--bad)}.chip.none{background:var(--soft);color:var(--mu)}
.tw{overflow-x:auto;background:var(--s);border:1px solid var(--li);border-radius:14px}
table{border-collapse:collapse;width:100%}th,td{padding:10px 12px;border-bottom:1px solid var(--li);text-align:left;vertical-align:top}th{font-size:12px;text-transform:uppercase;color:var(--mu)}tr:last-child td{border-bottom:0}
.btn{border:0;border-radius:10px;padding:10px 16px;background:var(--ac);color:#fff;font:inherit;font-weight:700;cursor:pointer}
.btn.ghost{background:var(--s);color:var(--ink);border:1px solid var(--li)}.btn.danger{background:var(--bad)}.btn.sm{padding:6px 10px;font-size:13px}
label{display:block;margin:0 0 12px;font-weight:600;font-size:14px}
input,select,textarea{display:block;width:100%;margin-top:4px;padding:10px;border:1px solid var(--li);border-radius:10px;background:var(--bg);color:var(--ink);font:inherit}
.check,.radio{display:flex;gap:10px;align-items:flex-start;font-weight:500}.check input,.radio input{width:auto;margin-top:4px}
.radio{border:1px solid var(--li);border-radius:12px;padding:12px}.radio span b{display:block}
.cols{display:grid;grid-template-columns:repeat(auto-fit,minmax(320px,1fr));gap:14px}.wide{max-width:720px;margin-bottom:14px}
.btnrow{display:flex;flex-wrap:wrap;gap:8px}.inline{display:flex;gap:8px}.inline input{margin:0}
.list{background:var(--s);border:1px solid var(--li);border-radius:14px;margin-bottom:10px}.row{display:flex;justify-content:space-between;align-items:center;gap:10px;padding:10px 14px;border-bottom:1px solid var(--li)}.row:last-child{border-bottom:0}.row a{text-decoration:none;color:inherit}
.filtros{display:flex;flex-wrap:wrap;gap:8px;margin-bottom:8px}.filtros input{flex:1;min-width:200px;margin:0}.filtros select{width:auto;margin:0}
.okbox,.warnbox,.err{padding:10px 14px;border-radius:12px}.okbox{background:var(--gb)}.warnbox{background:var(--wb)}.err{background:var(--bb);color:var(--bad)}
.modo{display:flex;flex-wrap:wrap;gap:4px 12px;align-items:baseline;padding:14px;border-radius:14px;margin-bottom:14px;background:var(--gb)}.modo.conta,.modo.assinatura{background:var(--wb)}.modo span{flex:1;min-width:220px}
.auth{max-width:380px;margin:60px auto;background:var(--s);border:1px solid var(--li);border-radius:16px;padding:24px}.auth .btn{width:100%}
code.big{display:block;padding:12px;background:var(--bg);border-radius:10px;word-break:break-all;font-size:15px}
CSS;

session_name('rotapaga_admin');
session_set_cookie_params(['httponly' => true, 'samesite' => 'Lax', 'secure' => !empty($_SERVER['HTTPS']) && $_SERVER['HTTPS'] !== 'off']);
session_start();
header('X-Frame-Options: DENY');
header('Cache-Control: no-store');

if (empty($_SESSION['csrf'])) $_SESSION['csrf'] = bin2hex(random_bytes(16));
$CSRF = $_SESSION['csrf'];
$p = $_GET['p'] ?? 'inicio';
$msg = $_SESSION['msg'] ?? ''; unset($_SESSION['msg']);

function voltar(string $para, string $m = ''): void { if ($m) $_SESSION['msg'] = $m; header('Location: ?p=' . $para); exit; }
function post_ok(): bool {
    if (($_SERVER['REQUEST_METHOD'] ?? '') !== 'POST') return false;
    if (!hash_equals($_SESSION['csrf'] ?? '', (string)($_POST['csrf'] ?? ''))) { http_response_code(400); exit('Sessão expirada. Volte e tente de novo.'); }
    return true;
}
$temAdmin = (int)db()->query('SELECT COUNT(*) FROM admins')->fetchColumn() > 0;
$admin = $_SESSION['admin'] ?? null;

/* ---------- primeiro acesso: criar o administrador ---------- */
if (!$temAdmin) {
    $erro = '';
    if (post_ok()) {
        $us = trim((string)($_POST['usuario'] ?? '')); $s1 = (string)($_POST['senha'] ?? ''); $s2 = (string)($_POST['senha2'] ?? '');
        if (strlen($us) < 3) $erro = 'Escolha um usuário com pelo menos 3 letras.';
        elseif (strlen($s1) < 8) $erro = 'A senha precisa ter pelo menos 8 caracteres.';
        elseif ($s1 !== $s2) $erro = 'As duas senhas não são iguais.';
        else {
            db()->prepare('INSERT INTO admins (usuario, senha_hash, criado_em) VALUES (?, ?, ?)')->execute([$us, password_hash($s1, PASSWORD_DEFAULT), agora()]);
            session_regenerate_id(true); $_SESSION['admin'] = $us;
            voltar('inicio', 'Administrador criado. Bem-vindo!');
        }
    }
    pagina('Primeiro acesso', '<div class="auth"><h1>Criar administrador</h1><p class="muted">É o primeiro acesso ao painel. Crie o usuário e a senha de quem vai gerenciar o app.</p>'
        . ($erro ? '<p class="err">' . h($erro) . '</p>' : '')
        . '<form method="post"><input type="hidden" name="csrf" value="' . $CSRF . '">'
        . '<label>Usuário<input name="usuario" autocomplete="username" required></label>'
        . '<label>Senha (mínimo 8)<input name="senha" type="password" autocomplete="new-password" required></label>'
        . '<label>Repita a senha<input name="senha2" type="password" autocomplete="new-password" required></label>'
        . '<button class="btn">Criar e entrar</button></form></div>', false);
    exit;
}

/* ---------- login ---------- */
if ($p === 'sair') { session_destroy(); header('Location: ./'); exit; }
if (!$admin) {
    $erro = '';
    if (post_ok()) {
        $pdo = db();
        $pdo->prepare('DELETE FROM tentativas WHERE quando < ?')->execute([time() - 900]);
        $st = $pdo->prepare('SELECT COUNT(*) FROM tentativas WHERE ip = ?'); $st->execute(['adm:' . ip()]);
        if ((int)$st->fetchColumn() >= 8) $erro = 'Muitas tentativas. Espere 15 minutos.';
        else {
            $st = $pdo->prepare('SELECT * FROM admins WHERE usuario = ?'); $st->execute([trim((string)($_POST['usuario'] ?? ''))]);
            $a = $st->fetch();
            if ($a && password_verify((string)($_POST['senha'] ?? ''), $a['senha_hash'])) {
                session_regenerate_id(true); $_SESSION['admin'] = $a['usuario']; voltar('inicio');
            }
            $pdo->prepare('INSERT INTO tentativas (ip, quando) VALUES (?, ?)')->execute(['adm:' . ip(), time()]);
            $erro = 'Usuário ou senha errados.';
        }
    }
    pagina('Entrar', '<div class="auth"><h1>' . h(NOME_SERVICO) . '</h1><p class="muted">Painel de acessos</p>'
        . ($erro ? '<p class="err">' . h($erro) . '</p>' : '')
        . '<form method="post"><input type="hidden" name="csrf" value="' . $CSRF . '">'
        . '<label>Usuário<input name="usuario" autocomplete="username" required autofocus></label>'
        . '<label>Senha<input name="senha" type="password" autocomplete="current-password" required></label>'
        . '<button class="btn">Entrar</button></form></div>', false);
    exit;
}

/* ---------- ações ---------- */
$pdo = db();
if (post_ok()) {
    $acao = $_POST['acao'] ?? '';
    $uid = (int)($_POST['id'] ?? 0);
    switch ($acao) {
        case 'config':
            $modo = in_array($_POST['modo'] ?? '', ['aberto', 'conta', 'assinatura'], true) ? $_POST['modo'] : 'aberto';
            cfg_set('modo', $modo);
            cfg_set('aviso', mb_substr(trim((string)($_POST['aviso'] ?? '')), 0, 300));
            cfg_set('aprovar_cadastros', !empty($_POST['aprovar_cadastros']) ? '1' : '0');
            cfg_set('cadastro_aberto', !empty($_POST['cadastro_aberto']) ? '1' : '0');
            cfg_set('dias_teste', (string)max(0, min(365, (int)($_POST['dias_teste'] ?? 0))));
            evento(null, 'config', 'modo=' . $modo . ' por ' . $admin);
            voltar('config', 'Configurações salvas.');
        case 'usuario':
            $status = in_array($_POST['status'] ?? '', ['ativo', 'bloqueado', 'pendente'], true) ? $_POST['status'] : 'ativo';
            $exp = trim((string)($_POST['expira'] ?? ''));
            if ($exp !== '' && !preg_match('/^\d{4}-\d{2}-\d{2}$/', $exp)) $exp = '';
            $pdo->prepare('UPDATE usuarios SET nome = ?, telefone = ?, status = ?, plano = ?, expira = ?, obs = ? WHERE id = ?')->execute([
                mb_substr(trim((string)$_POST['nome']), 0, 120), substr(preg_replace('/[^\d+]/', '', (string)$_POST['telefone']), 0, 20),
                $status, mb_substr(trim((string)$_POST['plano']), 0, 60) ?: null, $exp ?: null, mb_substr(trim((string)$_POST['obs']), 0, 2000), $uid]);
            evento($uid, 'editado', "status=$status validade=" . ($exp ?: '—') . " por $admin");
            voltar('usuario&id=' . $uid, 'Usuário salvo.');
        case 'estender':
            $dias = (int)($_POST['dias'] ?? 0);
            $st = $pdo->prepare('SELECT expira FROM usuarios WHERE id = ?'); $st->execute([$uid]);
            $base = (string)$st->fetchColumn();
            $base = ($base && $base > hoje()) ? $base : hoje();
            $nova = date('Y-m-d', strtotime("$base +$dias days"));
            $pdo->prepare("UPDATE usuarios SET expira = ?, status = CASE WHEN status = 'pendente' THEN 'ativo' ELSE status END WHERE id = ?")->execute([$nova, $uid]);
            evento($uid, 'validade', "+$dias dias até $nova por $admin");
            voltar('usuario&id=' . $uid, 'Acesso liberado até ' . data_br($nova) . '.');
        case 'status':
            $s = in_array($_POST['status'] ?? '', ['ativo', 'bloqueado'], true) ? $_POST['status'] : 'ativo';
            $pdo->prepare('UPDATE usuarios SET status = ? WHERE id = ?')->execute([$s, $uid]);
            if ($s === 'bloqueado') $pdo->prepare('DELETE FROM sessoes WHERE usuario_id = ?')->execute([$uid]);
            evento($uid, $s === 'bloqueado' ? 'bloqueio' : 'liberado', "por $admin");
            voltar($_POST['volta'] ?? 'usuarios', $s === 'bloqueado' ? 'Usuário bloqueado e desconectado.' : 'Usuário liberado.');
        case 'senha_reset':
            $nova = substr(str_replace(['+', '/', '='], '', base64_encode(random_bytes(9))), 0, 10);
            $pdo->prepare('UPDATE usuarios SET senha_hash = ? WHERE id = ?')->execute([password_hash($nova, PASSWORD_DEFAULT), $uid]);
            $pdo->prepare('DELETE FROM sessoes WHERE usuario_id = ?')->execute([$uid]);
            evento($uid, 'senha', "redefinida por $admin");
            voltar('usuario&id=' . $uid, 'Senha nova: ' . $nova . ' — passe para o usuário. Ela não aparece de novo.');
        case 'sessoes_sair':
            $pdo->prepare('DELETE FROM sessoes WHERE usuario_id = ?')->execute([$uid]);
            evento($uid, 'sessoes', "encerradas por $admin");
            voltar('usuario&id=' . $uid, 'O usuário foi desconectado de todos os aparelhos.');
        case 'excluir':
            if (($_POST['confirma'] ?? '') !== 'EXCLUIR') voltar('usuario&id=' . $uid, 'Para excluir, digite EXCLUIR.');
            foreach (['sessoes', 'backups'] as $t) $pdo->prepare("DELETE FROM $t WHERE usuario_id = ?")->execute([$uid]);
            $pdo->prepare('DELETE FROM usuarios WHERE id = ?')->execute([$uid]);
            evento($uid, 'excluido', "por $admin");
            voltar('usuarios', 'Usuário excluído.');
        case 'admin_senha':
            $st = $pdo->prepare('SELECT * FROM admins WHERE usuario = ?'); $st->execute([$admin]); $a = $st->fetch();
            if (!$a || !password_verify((string)$_POST['atual'], $a['senha_hash'])) voltar('conta', 'A senha atual está errada.');
            if (strlen((string)$_POST['nova']) < 8) voltar('conta', 'A senha nova precisa ter pelo menos 8 caracteres.');
            $pdo->prepare('UPDATE admins SET senha_hash = ? WHERE id = ?')->execute([password_hash((string)$_POST['nova'], PASSWORD_DEFAULT), $a['id']]);
            voltar('conta', 'Senha do painel trocada.');
        case 'admin_novo':
            $us = trim((string)$_POST['usuario']);
            if (strlen($us) < 3 || strlen((string)$_POST['senha']) < 8) voltar('conta', 'Usuário com 3+ letras e senha com 8+ caracteres.');
            try { $pdo->prepare('INSERT INTO admins (usuario, senha_hash, criado_em) VALUES (?, ?, ?)')->execute([$us, password_hash((string)$_POST['senha'], PASSWORD_DEFAULT), agora()]); }
            catch (Throwable $e) { voltar('conta', 'Esse usuário já existe.'); }
            voltar('conta', 'Administrador ' . $us . ' criado.');
    }
    voltar('inicio');
}

/* ---------- páginas ---------- */
function n(string $sql, array $a = []): int { $st = db()->prepare($sql); $st->execute($a); return (int)$st->fetchColumn(); }
function chip(string $s): string {
    $c = ['ativo' => 'good', 'bloqueado' => 'bad', 'pendente' => 'warn'][$s] ?? 'none';
    return '<span class="chip ' . $c . '">' . h($s) . '</span>';
}
function validade(?string $e): string {
    if (!$e) return '<span class="muted">sem validade</span>';
    $d = (int)floor((strtotime($e) - strtotime(hoje())) / 86400);
    $c = $d < 0 ? 'bad' : ($d <= 5 ? 'warn' : 'good');
    return '<span class="chip ' . $c . '">' . data_br($e) . ($d < 0 ? ' · venceu' : " · {$d}d") . '</span>';
}
$MODOS = [
    'aberto' => ['Liberado', 'Todo mundo usa o app, com ou sem conta. O login fica disponível, mas não é obrigatório.'],
    'conta' => ['Precisa de conta', 'O app só abre para quem entrou numa conta ativa. Contas bloqueadas ou pendentes não entram.'],
    'assinatura' => ['Precisa de plano', 'Só entra quem tem conta ativa e validade em dia. Você libera os dias aqui no painel.'],
];
$modo = cfg('modo');
$html = '';

if ($p === 'inicio') {
    $d7 = date('Y-m-d H:i:s', strtotime('-7 days'));
    $tot = n('SELECT COUNT(*) FROM usuarios');
    $cards = [
        ['Usuários', $tot, ''], ['Ativos', n("SELECT COUNT(*) FROM usuarios WHERE status='ativo'"), 'good'],
        ['Pendentes', n("SELECT COUNT(*) FROM usuarios WHERE status='pendente'"), 'warn'], ['Bloqueados', n("SELECT COUNT(*) FROM usuarios WHERE status='bloqueado'"), 'bad'],
        ['Usaram nos últimos 7 dias', n('SELECT COUNT(*) FROM usuarios WHERE ultimo_acesso >= ?', [$d7]), ''], ['Novos em 7 dias', n('SELECT COUNT(*) FROM usuarios WHERE criado_em >= ?', [$d7]), ''],
        ['Vencem em 7 dias', n('SELECT COUNT(*) FROM usuarios WHERE expira >= ? AND expira <= ?', [hoje(), date('Y-m-d', strtotime('+7 days'))]), 'warn'],
        ['Aparelhos conectados', n('SELECT COUNT(*) FROM sessoes'), ''],
    ];
    $html .= '<div class="modo ' . h($modo) . '"><b>Modo do app: ' . h($MODOS[$modo][0] ?? $modo) . '</b><span>' . h($MODOS[$modo][1] ?? '') . '</span><a href="?p=config">Mudar</a></div>';
    if (empty($_SERVER['HTTPS']) || $_SERVER['HTTPS'] === 'off') $html .= '<p class="warnbox">Este endereço está sem HTTPS: as senhas viajam sem criptografia. Ative o certificado gratuito (Let\'s Encrypt ou Cloudflare) na sua hospedagem.</p>';
    $html .= '<div class="cards">';
    foreach ($cards as [$l, $v, $c]) $html .= '<div class="card"><small>' . h($l) . '</small><b class="' . $c . '">' . $v . '</b></div>';
    $html .= '</div>';
    $pend = db()->query("SELECT * FROM usuarios WHERE status='pendente' ORDER BY criado_em DESC LIMIT 20")->fetchAll();
    if ($pend) {
        $html .= '<h2>Esperando aprovação</h2><div class="list">';
        foreach ($pend as $u) $html .= '<div class="row"><a href="?p=usuario&id=' . $u['id'] . '"><b>' . h($u['nome']) . '</b><small>' . h($u['email']) . ' · ' . data_br($u['criado_em'], true) . '</small></a>'
            . '<form method="post"><input type="hidden" name="csrf" value="' . $GLOBALS['CSRF'] . '"><input type="hidden" name="acao" value="status"><input type="hidden" name="status" value="ativo"><input type="hidden" name="volta" value="inicio"><input type="hidden" name="id" value="' . $u['id'] . '"><button class="btn sm">Aprovar</button></form></div>';
        $html .= '</div>';
    }
    $ev = db()->query('SELECT e.*, u.nome FROM eventos e LEFT JOIN usuarios u ON u.id = e.usuario_id ORDER BY e.id DESC LIMIT 15')->fetchAll();
    $html .= '<h2>Últimos acontecimentos</h2>' . tabela_eventos($ev) . '<p><a href="?p=eventos">Ver todos</a></p>';
}

function tabela_eventos(array $ev): string {
    if (!$ev) return '<p class="muted">Nada ainda.</p>';
    $nomes = ['cadastro' => 'Criou conta', 'login' => 'Entrou', 'bloqueio' => 'Bloqueado', 'liberado' => 'Liberado', 'validade' => 'Validade', 'editado' => 'Editado', 'senha' => 'Senha', 'sessoes' => 'Desconectado', 'excluido' => 'Excluído', 'config' => 'Configuração'];
    $o = '<div class="tw"><table><tr><th>Quando</th><th>Usuário</th><th>O quê</th><th>Detalhe</th></tr>';
    foreach ($ev as $e) $o .= '<tr><td>' . data_br($e['quando'], true) . '</td><td>' . ($e['usuario_id'] ? '<a href="?p=usuario&id=' . (int)$e['usuario_id'] . '">' . h($e['nome'] ?? '#' . $e['usuario_id']) . '</a>' : '—') . '</td><td>' . h($nomes[$e['tipo']] ?? $e['tipo']) . '</td><td class="muted">' . h($e['detalhe']) . '</td></tr>';
    return $o . '</table></div>';
}

if ($p === 'usuarios') {
    $q = trim((string)($_GET['q'] ?? '')); $f = (string)($_GET['f'] ?? '');
    $w = []; $a = [];
    if ($q !== '') { $w[] = '(nome LIKE ? OR email LIKE ? OR telefone LIKE ?)'; array_push($a, "%$q%", "%$q%", "%$q%"); }
    if (in_array($f, ['ativo', 'bloqueado', 'pendente'], true)) { $w[] = 'status = ?'; $a[] = $f; }
    if ($f === 'vencido') { $w[] = 'expira < ?'; $a[] = hoje(); }
    $st = db()->prepare('SELECT u.*, (SELECT COUNT(*) FROM sessoes s WHERE s.usuario_id = u.id) AS aparelhos FROM usuarios u' . ($w ? ' WHERE ' . implode(' AND ', $w) : '') . ' ORDER BY u.criado_em DESC LIMIT 500');
    $st->execute($a);
    $rows = $st->fetchAll();
    $html .= '<form class="filtros" method="get"><input type="hidden" name="p" value="usuarios"><input name="q" value="' . h($q) . '" placeholder="Buscar nome, e-mail ou telefone">'
        . '<select name="f"><option value="">Todos</option>';
    foreach (['ativo' => 'Ativos', 'pendente' => 'Pendentes', 'bloqueado' => 'Bloqueados', 'vencido' => 'Vencidos'] as $k => $l) $html .= '<option value="' . $k . '"' . ($f === $k ? ' selected' : '') . '>' . $l . '</option>';
    $html .= '</select><button class="btn">Filtrar</button></form>';
    $html .= '<p class="muted">' . count($rows) . ' usuário(s)</p><div class="tw"><table><tr><th>Nome</th><th>Contato</th><th>Situação</th><th>Validade</th><th>Último uso</th><th>Aparelhos</th><th></th></tr>';
    foreach ($rows as $u) {
        $html .= '<tr><td><a href="?p=usuario&id=' . $u['id'] . '"><b>' . h($u['nome']) . '</b></a>' . ($u['plano'] ? '<br><small class="muted">' . h($u['plano']) . '</small>' : '') . '</td>'
            . '<td>' . h($u['email']) . ($u['telefone'] ? '<br><small class="muted">' . h($u['telefone']) . '</small>' : '') . '</td>'
            . '<td>' . chip($u['status']) . '</td><td>' . validade($u['expira']) . '</td><td>' . data_br($u['ultimo_acesso'], true) . '</td><td>' . (int)$u['aparelhos'] . '</td>'
            . '<td><form method="post"><input type="hidden" name="csrf" value="' . $CSRF . '"><input type="hidden" name="acao" value="status"><input type="hidden" name="id" value="' . $u['id'] . '"><input type="hidden" name="volta" value="usuarios">'
            . ($u['status'] === 'bloqueado' ? '<input type="hidden" name="status" value="ativo"><button class="btn sm ghost">Liberar</button>' : '<input type="hidden" name="status" value="bloqueado"><button class="btn sm danger">Bloquear</button>')
            . '</form></td></tr>';
    }
    $html .= '</table></div>';
}

if ($p === 'usuario') {
    $id = (int)($_GET['id'] ?? 0);
    $st = db()->prepare('SELECT * FROM usuarios WHERE id = ?'); $st->execute([$id]); $u = $st->fetch();
    if (!$u) voltar('usuarios', 'Usuário não encontrado.');
    $ac = acesso($u);
    $html .= '<p><a href="?p=usuarios">‹ Usuários</a></p><h1>' . h($u['nome']) . ' ' . chip($u['status']) . '</h1>'
        . '<p class="muted">' . h($u['email']) . ' · criou a conta em ' . data_br($u['criado_em'], true) . ' · último uso ' . data_br($u['ultimo_acesso'], true) . ($u['versao_app'] ? ' · app ' . h($u['versao_app']) : '') . '</p>'
        . '<p class="' . ($ac['liberado'] ? 'okbox' : 'warnbox') . '">No modo atual (' . h($MODOS[$modo][0] ?? $modo) . '), este usuário ' . ($ac['liberado'] ? '<b>consegue usar o app</b>.' : '<b>não consegue usar o app</b>: ' . h($ac['mensagem'])) . '</p>';
    $hid = '<input type="hidden" name="csrf" value="' . $CSRF . '"><input type="hidden" name="id" value="' . $id . '">';
    $html .= '<div class="cols"><form method="post" class="box">' . $hid . '<input type="hidden" name="acao" value="usuario"><h2>Dados e acesso</h2>'
        . '<label>Nome<input name="nome" value="' . h($u['nome']) . '"></label><label>Telefone / WhatsApp<input name="telefone" value="' . h($u['telefone']) . '"></label>'
        . '<label>Situação<select name="status">';
    foreach (['ativo' => 'Ativo', 'pendente' => 'Pendente (esperando aprovação)', 'bloqueado' => 'Bloqueado'] as $k => $l) $html .= '<option value="' . $k . '"' . ($u['status'] === $k ? ' selected' : '') . '>' . $l . '</option>';
    $html .= '</select></label><label>Plano (texto livre)<input name="plano" value="' . h($u['plano']) . '" placeholder="Mensal, Anual, Teste…"></label>'
        . '<label>Válido até<input type="date" name="expira" value="' . h($u['expira']) . '"></label><label>Observações<textarea name="obs" rows="3">' . h($u['obs']) . '</textarea></label>'
        . '<button class="btn">Salvar</button></form>';
    $html .= '<div class="box"><h2>Liberar dias</h2><p class="muted">Soma a partir da validade atual (ou de hoje, se já venceu). Hoje: ' . validade($u['expira']) . '</p><div class="btnrow">';
    foreach ([7 => '+7 dias', 30 => '+30 dias', 90 => '+3 meses', 365 => '+1 ano'] as $d => $l)
        $html .= '<form method="post">' . $hid . '<input type="hidden" name="acao" value="estender"><input type="hidden" name="dias" value="' . $d . '"><button class="btn ghost">' . $l . '</button></form>';
    $html .= '</div>';
    $ses = db()->prepare('SELECT * FROM sessoes WHERE usuario_id = ? ORDER BY visto_em DESC'); $ses->execute([$id]); $ses = $ses->fetchAll();
    $html .= '<h2>Aparelhos conectados</h2>';
    if ($ses) { $html .= '<div class="list">'; foreach ($ses as $s) $html .= '<div class="row"><span><b>' . h($s['aparelho_nome'] ?: 'Aparelho') . '</b><small>entrou ' . data_br($s['criado_em'], true) . ' · visto ' . data_br($s['visto_em'], true) . '</small></span></div>'; $html .= '</div>'
        . '<form method="post">' . $hid . '<input type="hidden" name="acao" value="sessoes_sair"><button class="btn ghost">Desconectar de todos</button></form>'; }
    else $html .= '<p class="muted">Nenhum.</p>';
    $b = db()->prepare('SELECT tamanho, atualizado_em FROM backups WHERE usuario_id = ?'); $b->execute([$id]); $b = $b->fetch();
    $html .= '<h2>Backup na nuvem</h2><p class="muted">' . ($b ? round($b['tamanho'] / 1024) . ' KB · ' . data_br($b['atualizado_em'], true) : 'Nenhum backup ainda.') . '</p>';
    $html .= '<h2>Senha</h2><form method="post">' . $hid . '<input type="hidden" name="acao" value="senha_reset"><button class="btn ghost">Gerar senha nova</button></form>';
    $html .= '<h2>Excluir</h2><form method="post" class="inline">' . $hid . '<input type="hidden" name="acao" value="excluir"><input name="confirma" placeholder="Digite EXCLUIR"><button class="btn danger">Excluir usuário</button></form></div></div>';
    $ev = db()->prepare('SELECT e.*, u.nome FROM eventos e LEFT JOIN usuarios u ON u.id = e.usuario_id WHERE e.usuario_id = ? ORDER BY e.id DESC LIMIT 50'); $ev->execute([$id]);
    $html .= '<h2>Histórico</h2>' . tabela_eventos($ev->fetchAll());
}

if ($p === 'config') {
    $html .= '<h1>Configurações do app</h1><form method="post" class="box wide"><input type="hidden" name="csrf" value="' . $CSRF . '"><input type="hidden" name="acao" value="config"><h2>Quem pode usar o app</h2>';
    foreach ($MODOS as $k => [$l, $d]) $html .= '<label class="radio"><input type="radio" name="modo" value="' . $k . '"' . ($modo === $k ? ' checked' : '') . '><span><b>' . $l . '</b><small>' . $d . '</small></span></label>';
    $html .= '<p class="muted">O app confere o modo toda vez que abre. Sem internet, ele continua funcionando por até 7 dias com a última resposta.</p>'
        . '<h2>Cadastro</h2><label class="check"><input type="checkbox" name="cadastro_aberto" value="1"' . (cfg('cadastro_aberto') === '1' ? ' checked' : '') . '> Permitir criar conta pelo app</label>'
        . '<label class="check"><input type="checkbox" name="aprovar_cadastros" value="1"' . (cfg('aprovar_cadastros') === '1' ? ' checked' : '') . '> Conta nova fica pendente até eu aprovar</label>'
        . '<label>Dias de teste para conta nova (0 = sem validade)<input type="number" min="0" max="365" name="dias_teste" value="' . h(cfg('dias_teste')) . '"></label>'
        . '<h2>Aviso no app</h2><label>Mensagem que aparece no topo do app (deixe vazio para não mostrar)<textarea name="aviso" rows="2" maxlength="300">' . h(cfg('aviso')) . '</textarea></label>'
        . '<button class="btn">Salvar</button></form>';
    $api = (empty($_SERVER['HTTPS']) || $_SERVER['HTTPS'] === 'off' ? 'http' : 'https') . '://' . ($_SERVER['HTTP_HOST'] ?? 'seu-dominio') . preg_replace('#/admin/?.*$#', '/api/', $_SERVER['REQUEST_URI'] ?? '/admin/');
    $html .= '<div class="box wide"><h2>Endereço para colocar no app</h2><p class="muted">No app: Ajustes, toque 5 vezes na versão (lá embaixo) e cole este endereço. Ou me passe para eu deixar fixo no app.</p><code class="big">' . h($api) . '</code></div>';
}

if ($p === 'eventos') {
    $ev = db()->query('SELECT e.*, u.nome FROM eventos e LEFT JOIN usuarios u ON u.id = e.usuario_id ORDER BY e.id DESC LIMIT 300')->fetchAll();
    $html .= '<h1>Acontecimentos</h1>' . tabela_eventos($ev);
}

if ($p === 'conta') {
    $adms = db()->query('SELECT usuario, criado_em FROM admins ORDER BY id')->fetchAll();
    $html .= '<h1>Administradores</h1><div class="cols"><form method="post" class="box"><input type="hidden" name="csrf" value="' . $CSRF . '"><input type="hidden" name="acao" value="admin_senha"><h2>Trocar minha senha</h2>'
        . '<label>Senha atual<input type="password" name="atual" autocomplete="current-password"></label><label>Senha nova (mínimo 8)<input type="password" name="nova" autocomplete="new-password"></label><button class="btn">Trocar</button></form>'
        . '<form method="post" class="box"><input type="hidden" name="csrf" value="' . $CSRF . '"><input type="hidden" name="acao" value="admin_novo"><h2>Novo administrador</h2>'
        . '<label>Usuário<input name="usuario"></label><label>Senha (mínimo 8)<input type="password" name="senha" autocomplete="new-password"></label><button class="btn">Criar</button>'
        . '<p class="muted">Quem tem acesso: ' . implode(', ', array_map(fn($a) => h($a['usuario']), $adms)) . '</p></form></div>';
}

pagina(['inicio' => 'Painel', 'usuarios' => 'Usuários', 'usuario' => 'Usuário', 'config' => 'Configurações', 'eventos' => 'Acontecimentos', 'conta' => 'Administradores'][$p] ?? 'Painel', $html, true);

/* ---------- layout ---------- */
function pagina(string $titulo, string $corpo, bool $menu): void {
    global $p, $msg, $admin;
    $itens = ['inicio' => 'Painel', 'usuarios' => 'Usuários', 'config' => 'Configurações', 'eventos' => 'Acontecimentos', 'conta' => 'Administradores'];
    echo '<!doctype html><html lang="pt-BR"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><meta name="robots" content="noindex">'
        . '<title>' . h($titulo) . ' · ' . h(NOME_SERVICO) . '</title><style>' . CSS . '</style></head><body>';
    if ($menu) {
        echo '<header><b class="logo">' . h(NOME_SERVICO) . '</b><nav>';
        foreach ($itens as $k => $l) echo '<a href="?p=' . $k . '"' . (($p === $k || ($p === 'usuario' && $k === 'usuarios')) ? ' class="on"' : '') . '>' . $l . '</a>';
        echo '</nav><a class="out" href="?p=sair">Sair (' . h($admin) . ')</a></header>';
    }
    echo '<main>' . ($msg ? '<p class="okbox">' . h($msg) . '</p>' : '') . $corpo . '</main></body></html>';
}
