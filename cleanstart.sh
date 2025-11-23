#!/bin/bash
set -e

echo "🧹 Cleanstart: lösche alte H2 Datenbanken und tokens.txt..."

find . -name "*.mv.db" -type f -delete
find . -name "*.trace.db" -type f -delete
rm -f tokens.txt

echo "✅ Cleanup fertig. Starte Spring Boot..."

mvn clean spring-boot:run
