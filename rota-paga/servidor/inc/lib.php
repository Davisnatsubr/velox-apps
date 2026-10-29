<?php
/* Funções comuns do servidor: banco de dados, configurações, regras de acesso. */
require_once __DIR__ . '/config.php';

function db(): PDO {
    static $pdo = null;
    if ($pdo) return $pdo;
    if (DB_DRIVER === 'mysql') {
        $pdo = new PDO('mysql:host=' . DB_HOST . ';dbname=' . DB_NOME . ';charset=utf8mb4', DB_USUARIO, DB_SENHA);
    } else {
        $dir = dirname(DB_SQLITE);
        if (!is_dir($dir)) mkdir($dir, 0775, true);
        $pdo = new PDO('sqlite:' . DB_SQLITE);
        $pdo->exec('PRAGMA journal_mode=WAL; PRAGMA foreign_keys=ON;');
    }
    $pdo->setAttribute(PDO::ATTR_ERRMODE, PDO::ERRMODE_EXCEPTION);
    $pdo->setAttribute(PDO::ATTR_DEFAULT_FETCH_MODE, PDO::FETCH_ASSOC);
    instalar($pdo);
    return $pdo;
}

/** Cria as tabelas na primeira vez. */
function instalar(PDO $pdo): void {
    $my = DB_DRIVER === 'mysql';
    $id = $my ? 'INT AUTO_INCREMENT PRIMARY KEY' : 'INTEGER PRIMARY KEY AUTOINCREMENT';
    $txt = $my ? 'VARCHAR(190)' : 'TEXT';
    $big = $my ? 'LONGTEXT' : 'TEXT';
    $eng = $my ? ' ENGINE=InnoDB DEFAULT CHARSET=utf8mb4' : '';
    $pdo->exec("CREATE TABLE IF NOT EXISTS config (chave $txt PRIMARY KEY, valor TEXT)$eng");
    $pdo->exec("CREATE TABLE IF NOT EXISTS usuarios (
        id $id, nome $txt NOT NULL, email $txt NOT NULL UNIQUE, telefone $txt, senha_hash $txt NOT NULL,
        status $txt NOT NULL DEFAULT 'ativo', plano $txt, expira $txt, obs TEXT,
        criado_em $txt NOT NULL, ultimo_acesso $txt, ultimo_ip $txt, versao_app $txt)$eng");
    $pdo->exec("CREATE TABLE IF NOT EXISTS sessoes (
        token_hash $txt PRIMARY KEY, usuario_id INT NOT NULL, aparelho_id $txt, aparelho_nome $txt,
        criado_em $txt NOT NULL, visto_em $txt)$eng");
    $pdo->exec("CREATE TABLE IF NOT EXISTS backups (usuario_id INT PRIMARY KEY, dados $big, tamanho INT, atualizado_em $txt)$eng");
    $pdo->exec("CREATE TABLE IF NOT EXISTS admins (id $id, usuario $txt NOT NULL UNIQUE, senha_hash $txt NOT NULL, criado_em $txt)$eng");
    $pdo->exec("CREATE TABLE IF NOT EXISTS tentativas (ip $txt, quando INT)$eng");
    $pdo->exec("CREATE TABLE IF NOT EXISTS eventos (id $id, usuario_id INT, tipo $txt, detalhe TEXT, quando $txt)$eng");
}

function agora(): string { return date('Y-m-d H:i:s'); }
function hoje(): string { return date('Y-m-d'); }
function ip(): string { return substr($_SERVER['HTTP_CF_CONNECTING_IP'] ?? $_SERVER['REMOTE_ADDR'] ?? '', 0, 60); }

/* ---------- configurações gerais (mudam pelo painel) ---------- */
const CFG_PADRAO = [
    'modo' => 'aberto',          // aberto | conta | assinatura
    'aviso' => '',               // mensagem que aparece no app
    'aprovar_cadastros' => '0',  // 1 = conta nova fica "pendente" até você liberar
    'dias_teste' => '0',         // validade da conta nova (só conta no modo assinatura). 0 = sem validade
    'cadastro_aberto' => '1',    // 0 = ninguém novo consegue criar conta pelo app
];

function cfg(string $k): string {
    $st = db()->prepare('SELECT valor FROM config WHERE chave = ?');
    $st->execute([$k]);
    $v = $st->fetchColumn();
    return $v === false ? (CFG_PADRAO[$k] ?? '') : (string)$v;
}

function cfg_set(string $k, string $v): void {
    $pdo = db();
    $pdo->prepare('DELETE FROM config WHERE chave = ?')->execute([$k]);
    $pdo->prepare('INSERT INTO config (chave, valor) VALUES (?, ?)')->execute([$k, $v]);
}

function evento(?int $uid, string $tipo, string $det = ''): void {
    db()->prepare('INSERT INTO eventos (usuario_id, tipo, detalhe, quando) VALUES (?, ?, ?, ?)')->execute([$uid, $tipo, mb_substr($det, 0, 500), agora()]);
}

/* ---------- regra de acesso ---------- */
/**
 * aberto      → todo mundo usa, com ou sem conta
 * conta       → precisa estar logado com conta ativa
 * assinatura  → precisa conta ativa e dentro da validade
 */
function acesso(?array $u): array {
    $modo = cfg('modo');
    if ($u && $u['status'] === 'bloqueado') return ['liberado' => $modo === 'aberto', 'motivo' => 'bloqueado', 'mensagem' => 'Sua conta foi bloqueada. Fale com o suporte.'];
    if ($modo === 'aberto') return ['liberado' => true, 'motivo' => 'aberto', 'mensagem' => ''];
    if (!$u) return ['liberado' => false, 'motivo' => 'sem_login', 'mensagem' => 'Entre ou crie sua conta para continuar.'];
    if ($u['status'] === 'pendente') return ['liberado' => false, 'motivo' => 'pendente', 'mensagem' => 'Sua conta está esperando aprovação.'];
    if ($modo === 'assinatura') {
        if (!$u['expira']) return ['liberado' => false, 'motivo' => 'sem_plano', 'mensagem' => 'Sua conta ainda não tem um plano ativo.'];
        if ($u['expira'] < hoje()) return ['liberado' => false, 'motivo' => 'vencido', 'mensagem' => 'Seu acesso venceu em ' . date('d/m/Y', strtotime($u['expira'])) . '.'];
    }
    return ['liberado' => true, 'motivo' => 'ok', 'mensagem' => ''];
}

function usuario_publico(array $u): array {
    return [
        'id' => (int)$u['id'], 'nome' => $u['nome'], 'email' => $u['email'], 'telefone' => $u['telefone'],
        'status' => $u['status'], 'plano' => $u['plano'], 'expira' => $u['expira'],
    ];
}

function h($s): string { return htmlspecialchars((string)$s, ENT_QUOTES, 'UTF-8'); }
function data_br(?string $d, bool $hora = false): string {
    if (!$d) return '—';
    $t = strtotime($d);
    return $t ? date($hora ? 'd/m/Y H:i' : 'd/m/Y', $t) : '—';
}
