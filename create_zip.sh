#!/usr/bin/env bash
# Packages the project into transit-routing-service.zip (run from the project root).
# If Maven is installed and no wrapper exists yet, a Maven Wrapper (./mvnw) is generated first.
set -euo pipefail
cd "$(dirname "$0")"
if [ ! -f mvnw ] && command -v mvn >/dev/null 2>&1; then
  mvn -q -N wrapper:wrapper -Dmaven=3.9.9 || echo "wrapper generation failed; use 'mvn spring-boot:run'"
fi
NAME="transit-routing-service"
OUT="${NAME}.zip"
rm -f "$OUT"
cd ..
zip -r "$OUT" "$NAME" -x "$NAME/target/*" "$NAME/.idea/*" >/dev/null
echo "Created $(cd "$(dirname "$OUT")" && pwd)/${NAME}.zip"
