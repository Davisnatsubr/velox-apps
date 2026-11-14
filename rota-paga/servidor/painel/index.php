<?php
/* Faturamento no computador: o motorista entra com a mesma conta do app e vê os números do último backup. */
require_once __DIR__ . '/../inc/lib.php';
require_once __DIR__ . '/../inc/faturamento.php';

session_name('rotapaga_motorista');
session_set_cookie_params(['httponly' => true, 'samesite' => 'Lax', 'secure' => !empty($_SERVER['HTTPS']) && $_SERVER['HTTPS'] !== 'off']);
session_start();
header('X-Frame-Options: DENY');
header('Cache-Control: no-store');
if (empty($_SESSION['csrf'])) $_SESSION['csrf'] = bin2hex(random_bytes(16));

if (($_GET['p'] ?? '') === 'sair') { session_destroy(); header('Location: ./'); exit; }

$erro = '';
if (($_SERVER['REQUEST_METHOD'] ?? '') === 'POST') {
    if (!hash_equals($_SESSION['csrf'], (string)($_POST['csrf'] ?? ''))) $erro = 'A página expirou. Tente de novo.';
    else {
        $pdo = db();
        $pdo->prepare('DELETE FROM tentativas WHERE quando < ?')->execute([time() - 900]);
        $st = $pdo->prepare('SELECT COUNT(*) FROM tentativas WHERE ip = ?'); $st->execute([ip()]);
        if ((int)$st->fetchColumn() >= 10) $erro = 'Muitas tentativas. Espere 15 minutos.';
        else {
            $st = $pdo->prepare('SELECT * FROM usuarios WHERE email = ?'); $st->execute([mb_strtolower(trim((string)($_POST['email'] ?? '')))]);
            $u = $st->fetch();
            if ($u && password_verify((string)($_POST['senha'] ?? ''), $u['senha_hash'])) {
                if ($u['status'] === 'bloqueado') $erro = 'Sua conta está bloqueada. Fale com o suporte.';
                else { session_regenerate_id(true); $_SESSION['uid'] = (int)$u['id']; evento((int)$u['id'], 'login', 'faturamento no computador'); header('Location: ./'); exit; }
            } else {
                $pdo->prepare('INSERT INTO tentativas (ip, quando) VALUES (?, ?)')->execute([ip(), time()]);
                $erro = 'E-mail ou senha errados.';
            }
        }
    }
}

$uid = (int)($_SESSION['uid'] ?? 0);
$u = null;
if ($uid) { $st = db()->prepare('SELECT * FROM usuarios WHERE id = ?'); $st->execute([$uid]); $u = $st->fetch() ?: null; }
if ($u && $u['status'] === 'bloqueado') { session_destroy(); $u = null; $erro = 'Sua conta está bloqueada.'; }

if ($u) {
    $b = dados_usuario((int)$u['id']);
    if ($b) { render_faturamento($b['json'], $u['nome'], $b['em'], '', '?p=sair'); exit; }
}

/* tela de entrar (ou aviso de que ainda não há backup) */
?><!doctype html><html lang="pt-BR"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><meta name="robots" content="noindex">
<title>Meu faturamento · <?= h(NOME_SERVICO) ?></title>
<style>
:root{--bg:#F2F5F3;--s:#fff;--ink:#13201A;--mu:#5B6A63;--li:#D9E1DC;--ac:#0A6B4B;--bb:#FBE4E0;--bad:#BF3B28}
@media(prefers-color-scheme:dark){:root{--bg:#0E1412;--s:#171F1C;--ink:#E7EEEA;--mu:#93A39B;--li:#26322D;--ac:#3DB883;--bb:#3A1B16;--bad:#EE6C57;color-scheme:dark}}
*{box-sizing:border-box}body{margin:0;background:var(--bg);color:var(--ink);font:15px/1.5 system-ui,-apple-system,"Segoe UI",Roboto,sans-serif}
.auth{max-width:400px;margin:70px auto;background:var(--s);border:1px solid var(--li);border-radius:16px;padding:26px}
h1{margin:0 0 4px;font-size:26px}p{color:var(--mu)}label{display:block;font-weight:600;font-size:14px;margin:0 0 12px}
input{display:block;width:100%;margin-top:4px;padding:11px;border:1px solid var(--li);border-radius:10px;background:var(--bg);color:var(--ink);font:inherit}
button{width:100%;border:0;border-radius:10px;padding:12px;background:var(--ac);color:#fff;font:inherit;font-weight:700;cursor:pointer}
.err{background:var(--bb);color:var(--bad);padding:10px 12px;border-radius:10px}a{color:var(--ac)}
</style></head><body><div class="auth">
<?php if ($u): ?>
  <h1>Olá, <?= h($u['nome']) ?></h1>
  <p>Ainda não chegou nenhum backup do seu celular. Abra o app, vá em <b>Ajustes › Conta</b> e toque em <b>Enviar backup para a nuvem</b>. Depois atualize esta página.</p>
  <p><a href="?p=sair">Sair</a></p>
<?php else: ?>
  <h1>Meu faturamento</h1>
  <p>Entre com a mesma conta do app <?= h(NOME_SERVICO) ?>.</p>
  <?= $erro ? '<p class="err">' . h($erro) . '</p>' : '' ?>
  <form method="post"><input type="hidden" name="csrf" value="<?= h($_SESSION['csrf']) ?>">
    <label>E-mail<input name="email" type="email" autocomplete="email" required autofocus></label>
    <label>Senha<input name="senha" type="password" autocomplete="current-password" required></label>
    <button>Entrar</button>
  </form>
<?php endif; ?>
</div></body></html>
