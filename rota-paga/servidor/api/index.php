<?php
/*
 * API do Rota Paga (o app chama este arquivo).
 * Todas as chamadas: POST com corpo JSON (Content-Type text/plain, para não precisar de "preflight").
 * A ação vai em ?a=... ou no campo "a" do corpo. O login vai no campo "token".
 */
require_once __DIR__ . '/../inc/lib.php';

header('Access-Control-Allow-Origin: *');
header('Access-Control-Allow-Methods: POST, GET, OPTIONS');
header('Access-Control-Allow-Headers: Content-Type');
header('Content-Type: application/json; charset=utf-8');
header('Cache-Control: no-store');
if (($_SERVER['REQUEST_METHOD'] ?? '') === 'OPTIONS') exit;

function sai(array $d, int $code = 200): void {
    http_response_code($code);
    echo json_encode($d, JSON_UNESCAPED_UNICODE);
    exit;
}
function erro(string $msg, int $code = 400): void { sai(['ok' => false, 'erro' => $msg], $code); }

$raw = file_get_contents('php://input') ?: '';
if (strlen($raw) > (BACKUP_MAX_MB + 1) * 1024 * 1024) erro('Envio grande demais.', 413);
$in = json_decode($raw, true);
if (!is_array($in)) $in = [];
$a = $_GET['a'] ?? ($in['a'] ?? 'status');

/** Usuário dono do token (ou null). Atualiza "visto em" do aparelho. */
function logado(array $in): ?array {
    $tok = (string)($in['token'] ?? '');
    if (strlen($tok) < 20) return null;
    $th = hash('sha256', $tok);
    $st = db()->prepare('SELECT u.*, s.aparelho_id FROM sessoes s JOIN usuarios u ON u.id = s.usuario_id WHERE s.token_hash = ?');
    $st->execute([$th]);
    $u = $st->fetch();
    if (!$u) return null;
    db()->prepare('UPDATE sessoes SET visto_em = ? WHERE token_hash = ?')->execute([agora(), $th]);
    db()->prepare('UPDATE usuarios SET ultimo_acesso = ?, ultimo_ip = ?, versao_app = ? WHERE id = ?')
        ->execute([agora(), ip(), substr((string)($in['versao'] ?? ''), 0, 40), $u['id']]);
    return $u;
}

function nova_sessao(int $uid, array $in): string {
    $tok = bin2hex(random_bytes(32));
    $ap = is_array($in['aparelho'] ?? null) ? $in['aparelho'] : [];
    db()->prepare('INSERT INTO sessoes (token_hash, usuario_id, aparelho_id, aparelho_nome, criado_em, visto_em) VALUES (?, ?, ?, ?, ?, ?)')
        ->execute([hash('sha256', $tok), $uid, substr((string)($ap['id'] ?? ''), 0, 80), substr((string)($ap['nome'] ?? ''), 0, 120), agora(), agora()]);
    return $tok;
}

/** No máximo 10 tentativas de login/cadastro erradas por IP a cada 15 minutos. */
function limite(): void {
    $pdo = db();
    $pdo->prepare('DELETE FROM tentativas WHERE quando < ?')->execute([time() - 900]);
    $st = $pdo->prepare('SELECT COUNT(*) FROM tentativas WHERE ip = ?');
    $st->execute([ip()]);
    if ((int)$st->fetchColumn() >= 10) erro('Muitas tentativas. Espere 15 minutos e tente de novo.', 429);
}
function falhou(): void { db()->prepare('INSERT INTO tentativas (ip, quando) VALUES (?, ?)')->execute([ip(), time()]); }

function resposta_status(?array $u, array $extra = []): void {
    sai(array_merge([
        'ok' => true,
        'servico' => NOME_SERVICO,
        'modo' => cfg('modo'),
        'aviso' => cfg('aviso'),
        'cadastro_aberto' => cfg('cadastro_aberto') === '1',
        'usuario' => $u ? usuario_publico($u) : null,
        'acesso' => acesso($u),
        'agora' => agora(),
    ], $extra));
}

try {
    switch ($a) {

    case 'status':
        resposta_status(logado($in));

    case 'registrar':
        limite();
        if (cfg('cadastro_aberto') !== '1') erro('Cadastros fechados no momento.');
        $nome = trim((string)($in['nome'] ?? ''));
        $email = mb_strtolower(trim((string)($in['email'] ?? '')));
        $tel = preg_replace('/[^\d+]/', '', (string)($in['telefone'] ?? ''));
        $senha = (string)($in['senha'] ?? '');
        if (mb_strlen($nome) < 2) erro('Informe seu nome.');
        if (!filter_var($email, FILTER_VALIDATE_EMAIL)) erro('E-mail inválido.');
        if (strlen($senha) < 6) erro('A senha precisa ter pelo menos 6 caracteres.');
        $st = db()->prepare('SELECT id FROM usuarios WHERE email = ?');
        $st->execute([$email]);
        if ($st->fetchColumn()) { falhou(); erro('Já existe uma conta com esse e-mail. Use "Entrar".'); }
        $status = cfg('aprovar_cadastros') === '1' ? 'pendente' : 'ativo';
        $dias = (int)cfg('dias_teste');
        $expira = $dias > 0 ? date('Y-m-d', strtotime("+$dias days")) : null;
        db()->prepare('INSERT INTO usuarios (nome, email, telefone, senha_hash, status, plano, expira, criado_em, ultimo_acesso, ultimo_ip) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)')
            ->execute([mb_substr($nome, 0, 120), $email, substr($tel, 0, 20), password_hash($senha, PASSWORD_DEFAULT), $status, $dias > 0 ? 'Teste' : null, $expira, agora(), agora(), ip()]);
        $uid = (int)db()->lastInsertId();
        evento($uid, 'cadastro', $email);
        $tok = nova_sessao($uid, $in);
        $st = db()->prepare('SELECT * FROM usuarios WHERE id = ?');
        $st->execute([$uid]);
        resposta_status($st->fetch(), ['token' => $tok]);

    case 'entrar':
        limite();
        $email = mb_strtolower(trim((string)($in['email'] ?? '')));
        $st = db()->prepare('SELECT * FROM usuarios WHERE email = ?');
        $st->execute([$email]);
        $u = $st->fetch();
        if (!$u || !password_verify((string)($in['senha'] ?? ''), $u['senha_hash'])) { falhou(); erro('E-mail ou senha errados.'); }
        if ($u['status'] === 'bloqueado') { evento((int)$u['id'], 'login', 'recusado: conta bloqueada'); erro('Sua conta foi bloqueada. Fale com o suporte.', 403); }
        if (password_needs_rehash($u['senha_hash'], PASSWORD_DEFAULT))
            db()->prepare('UPDATE usuarios SET senha_hash = ? WHERE id = ?')->execute([password_hash((string)$in['senha'], PASSWORD_DEFAULT), $u['id']]);
        $tok = nova_sessao((int)$u['id'], $in);
        evento((int)$u['id'], 'login', (string)(($in['aparelho']['nome'] ?? '') ?: ip()));
        resposta_status(logado(['token' => $tok] + $in), ['token' => $tok]);

    case 'sair':
        $tok = (string)($in['token'] ?? '');
        if ($tok) db()->prepare('DELETE FROM sessoes WHERE token_hash = ?')->execute([hash('sha256', $tok)]);
        sai(['ok' => true]);

    case 'senha':
        $u = logado($in) ?: erro('Entre na sua conta de novo.', 401);
        if (!password_verify((string)($in['senha_atual'] ?? ''), $u['senha_hash'])) erro('A senha atual está errada.');
        if (strlen((string)($in['senha_nova'] ?? '')) < 6) erro('A senha nova precisa ter pelo menos 6 caracteres.');
        db()->prepare('UPDATE usuarios SET senha_hash = ? WHERE id = ?')->execute([password_hash((string)$in['senha_nova'], PASSWORD_DEFAULT), $u['id']]);
        evento((int)$u['id'], 'senha', 'trocada pelo app');
        sai(['ok' => true]);

    case 'backup_enviar':
        $u = logado($in) ?: erro('Entre na sua conta para fazer backup.', 401);
        $dados = (string)($in['dados'] ?? '');
        if ($dados === '' || json_decode($dados) === null) erro('Backup vazio ou inválido.');
        if (strlen($dados) > BACKUP_MAX_MB * 1024 * 1024) erro('Backup maior que ' . BACKUP_MAX_MB . ' MB.');
        $pdo = db();
        $pdo->prepare('DELETE FROM backups WHERE usuario_id = ?')->execute([$u['id']]);
        $pdo->prepare('INSERT INTO backups (usuario_id, dados, tamanho, atualizado_em) VALUES (?, ?, ?, ?)')->execute([$u['id'], $dados, strlen($dados), agora()]);
        sai(['ok' => true, 'em' => agora(), 'tamanho' => strlen($dados)]);

    case 'backup_baixar':
        $u = logado($in) ?: erro('Entre na sua conta para baixar o backup.', 401);
        $st = db()->prepare('SELECT dados, atualizado_em FROM backups WHERE usuario_id = ?');
        $st->execute([$u['id']]);
        $b = $st->fetch();
        if (!$b) erro('Você ainda não tem backup na nuvem.');
        sai(['ok' => true, 'dados' => $b['dados'], 'em' => $b['atualizado_em']]);

    case 'backup_info':
        $u = logado($in) ?: erro('Entre na sua conta.', 401);
        $st = db()->prepare('SELECT tamanho, atualizado_em FROM backups WHERE usuario_id = ?');
        $st->execute([$u['id']]);
        $b = $st->fetch();
        sai(['ok' => true, 'backup' => $b ?: null]);

    default:
        erro('Ação desconhecida.', 404);
    }
} catch (Throwable $e) {
    error_log('rotapaga api: ' . $e->getMessage());
    erro('Erro no servidor. Tente de novo em instantes.', 500);
}
