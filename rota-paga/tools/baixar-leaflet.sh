#!/usr/bin/env bash
# Baixa a biblioteca do mapa (Leaflet 1.9.4) para dentro do app, para o mapa funcionar sem depender de CDN.
set -euo pipefail
D="$(dirname "$0")/../app/src/main/assets/www/vendor"
mkdir -p "$D"
curl -fsSL https://unpkg.com/leaflet@1.9.4/dist/leaflet.js -o "$D/leaflet.js"
curl -fsSL https://unpkg.com/leaflet@1.9.4/dist/leaflet.css -o "$D/leaflet.css"
ls -la "$D"
