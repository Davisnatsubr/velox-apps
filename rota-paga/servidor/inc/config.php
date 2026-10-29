<?php
/*
 * Configuração do servidor do Rota Paga.
 * Na maioria das hospedagens não precisa mudar nada: o padrão usa SQLite (um arquivo na pasta "dados").
 * Para usar MySQL, troque DB_DRIVER para 'mysql' e preencha os dados do banco.
 */

define('DB_DRIVER', 'sqlite');                 // 'sqlite' ou 'mysql'
define('DB_SQLITE', __DIR__ . '/../dados/rotapaga.sqlite');

define('DB_HOST', 'localhost');
define('DB_NOME', 'rotapaga');
define('DB_USUARIO', '');
define('DB_SENHA', '');

// Nome que aparece no painel e no app
define('NOME_SERVICO', 'Rota Paga');

// Tamanho máximo do backup de cada usuário (em MB)
define('BACKUP_MAX_MB', 8);

// Fuso horário das datas no painel
date_default_timezone_set('America/Sao_Paulo');
