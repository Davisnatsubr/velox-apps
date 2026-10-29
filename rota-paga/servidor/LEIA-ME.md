# Servidor do Rota Paga (contas e painel de acessos)

PHP puro, sem nada para instalar. Serve em hospedagem comum (cPanel, Hostinger, HostGator) e em VPS.

- **App** usa `api/`: criar conta, entrar, sair, trocar senha e backup na nuvem.
- **Você** usa `admin/`: painel para ver e gerenciar os usuários.
- **Banco:** SQLite por padrão, um arquivo em `dados/`. Não precisa criar banco. Também funciona com MySQL.

## Instalar

1. Envie a pasta `servidor` inteira para a hospedagem. Pode renomear, por exemplo para `public_html/rotapaga/`.
2. A pasta `dados/` precisa permitir gravação. Na maioria das hospedagens isso já vem certo; se não vier, coloque a permissão 775.
3. Abra `https://seudominio.com.br/rotapaga/admin/` no navegador. No primeiro acesso, o painel pede para você criar o administrador.
4. Em **Configurações**, o painel mostra o endereço da API. Me passe esse endereço para eu deixar fixo no app (`assets/www/config.js`). Para testar antes disso, abra o app em **Ajustes**, toque 5 vezes na versão, lá embaixo, e cole o endereço.

Requisitos:
- PHP 7.4 ou mais novo, com `pdo_sqlite` (ou `pdo_mysql`). Quase toda hospedagem tem.
- **HTTPS recomendado.** Sem ele, as senhas viajam sem criptografia. O certificado gratuito do Let's Encrypt ou da Cloudflare resolve.

### Usar MySQL em vez de SQLite

Crie um banco vazio no painel da hospedagem e preencha `inc/config.php`:

```php
define('DB_DRIVER', 'mysql');
define('DB_HOST', 'localhost');
define('DB_NOME', 'nome_do_banco');
define('DB_USUARIO', 'usuario');
define('DB_SENHA', 'senha');
```

As tabelas são criadas sozinhas no primeiro acesso.

### VPS com Nginx

As pastas `inc/` e `dados/` não podem ficar acessíveis pela web. No Apache, os arquivos `.htaccess` já cuidam disso. No Nginx, acrescente:

```nginx
location ~ ^/rotapaga/(inc|dados)/ { deny all; }
```

## Modos do app (Painel › Configurações)

| Modo | Quem usa o app |
|---|---|
| **Liberado** (padrão) | Todo mundo, com ou sem conta. O login existe, mas é opcional. |
| **Precisa de conta** | Só quem entrou numa conta ativa. |
| **Precisa de plano** | Só quem tem conta ativa com a validade em dia. Você libera os dias pelo painel (+7, +30, +3 meses, +1 ano). |

O app confere o modo toda vez que abre. Sem internet, ele continua funcionando com a última resposta.

Outras opções:
- conta nova fica pendente até você aprovar;
- dias de teste para conta nova;
- fechar novos cadastros;
- aviso que aparece no topo do app.

## No painel

- **Números:** usuários, ativos, pendentes, bloqueados, quem usou na semana e quem vence logo.
- **Usuários:** busca e filtro; bloquear e liberar com um toque (bloquear desconecta todos os aparelhos).
- **Página de cada usuário:**
  - dados, situação, plano, validade e observações;
  - liberar dias;
  - aparelhos conectados;
  - backup;
  - gerar senha nova;
  - excluir.
- **Acontecimentos:** cadastros, logins, bloqueios e mudanças de validade.
- **Administradores:** trocar a sua senha e criar outros administradores.

## Segurança

- Senhas guardadas com `password_hash`. Nem você vê a senha do usuário; só pode gerar uma nova.
- O login do app usa um token aleatório por aparelho. No banco fica só o hash desse token.
- O login é bloqueado por 15 minutos depois de 10 tentativas erradas (8, no painel).
- O painel tem proteção contra envio de formulário por outro site (CSRF) e não abre dentro de outro site.

## Copiar os dados

O banco inteiro é o arquivo `dados/rotapaga.sqlite`. Copie esse arquivo de vez em quando.
