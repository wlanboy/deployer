#!/bin/bash
set -euo pipefail

# 1. Erstes Token aus tokens.txt holen
TOKEN=$(head -n 1 tokens.txt)

# 2. Login mit Token als Username, Cookies speichern
curl -s -c cookies.txt -X POST "http://localhost:8080/login" -d "username=${TOKEN}" 

# 3. Repos abrufen
echo "==> Repos:"
curl -s -b cookies.txt -X GET "http://localhost:8080/repos"

# 4. Playbooks für repo1 abrufen
echo "==> Playbooks:"
curl -s -b cookies.txt -X GET "http://localhost:8080/api/repo1/playbooks"

# 5. Neues Deployment anlegen
echo "==> Neues Deployment anlegen..."
DEPLOYMENT_RESPONSE=$(curl -s -b cookies.txt -X POST "http://localhost:8080/api/repo1/deployment" \
     -d "name=apt-update")

echo "Response: $DEPLOYMENT_RESPONSE"

# 6. ID aus dem JSON extrahieren
DEPLOYMENT_ID=$(echo "$DEPLOYMENT_RESPONSE" | grep -oE '"id"\s*:\s*"[^"]+"' | cut -d'"' -f4)

echo "Deployment ID: $DEPLOYMENT_ID"

# 7. Step hinzufügen
echo "==> Step hinzufügen..."
STEP_RESPONSE=$(curl -s -b cookies.txt -X PUT "http://localhost:8080/api/repo1/deployment/${DEPLOYMENT_ID}" \
     -d "playbook=apt" \
     -d "inventory=all" \
     -d "tags=setup" \
     -d "skipTags=tests" \
     -d "hostLimit=localhost")

echo "Response: $STEP_RESPONSE"

# 8. Deployment ausführen
echo "==> Deployment ausführen..."
curl -s -b cookies.txt -X GET "http://localhost:8080/api/repo1/rundeployment/${DEPLOYMENT_ID}"
