#!/usr/bin/env bash
# Analyse SonarQube du projet E-Bank (Membre 2 - Mohamed).
#
# 1. demarre le serveur SonarQube (docker compose) et attend qu'il soit UP ;
# 2. execute les tests backend (mvn test) qui generent la couverture JaCoCo ;
# 3. lance le SonarScanner (conteneur) sur tout le depot ;
# 4. affiche le resultat du Quality Gate et sort en erreur s'il echoue.
#
# Prerequis : SONAR_TOKEN (voir devops/sonarqube/README.md), Docker, Java 21+.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
cd "$ROOT"

# Token : variable d'environnement ou fichier local non versionne.
if [[ -z "${SONAR_TOKEN:-}" && -f devops/sonarqube/.env ]]; then
  set -a
  # shellcheck disable=SC1091
  . devops/sonarqube/.env
  set +a
fi

SONAR_PORT="${SONAR_PORT:-9000}"
SONAR_URL_LOCAL="${SONAR_URL_LOCAL:-http://localhost:${SONAR_PORT}}"
PROJECT_KEY="${SONAR_PROJECT_KEY:-e-bank}"

if [[ -z "${SONAR_TOKEN:-}" ]]; then
  echo "ERREUR : SONAR_TOKEN est vide." >&2
  echo "  Creer un token dans SonarQube (My Account > Security), puis :" >&2
  echo "  export SONAR_TOKEN=xxxxxxxx   # ou le mettre dans devops/sonarqube/.env" >&2
  exit 1
fi

echo "==> 1/4 Demarrage de SonarQube (${SONAR_URL_LOCAL})"
docker compose up -d sonar-db sonarqube

echo "==> Attente de l'API /api/system/status (max 6 min)"
for _ in $(seq 1 72); do
  if curl -fsS "${SONAR_URL_LOCAL}/api/system/status" 2>/dev/null | grep -q '"status":"UP"'; then
    echo "    SonarQube est UP."
    break
  fi
  sleep 5
done
if ! curl -fsS "${SONAR_URL_LOCAL}/api/system/status" 2>/dev/null | grep -q '"status":"UP"'; then
  echo "ERREUR : SonarQube n'a pas demarre." >&2
  docker compose logs --tail 60 sonarqube >&2
  exit 1
fi

echo "==> 2/4 Tests backend + couverture JaCoCo (mvn test)"
(cd pro && ./mvnw -B test)
REPORT="pro/target/site/jacoco/jacoco.xml"
if [[ -f "$REPORT" ]]; then
  echo "    Rapport de couverture : $REPORT"
else
  echo "    ATTENTION : $REPORT absent, SonarQube analysera sans couverture Java." >&2
fi

echo "==> 3/4 SonarScanner"
if [[ -f e-bank-frontend/package.json ]] && grep -q '"test"' e-bank-frontend/package.json; then
  if [[ ! -f e-bank-frontend/coverage/lcov.info ]]; then
    echo "    Couverture frontend absente : lancer (cd e-bank-frontend && npm test -- --coverage)."
  fi
else
  echo "    Tests frontend non configures : analyse TypeScript sans couverture lcov."
fi

docker compose --profile quality run --rm sonar-scanner

echo "==> 4/4 Quality Gate"
STATUS_JSON="$(curl -fsS -H "Authorization: Bearer ${SONAR_TOKEN}" \
  "${SONAR_URL_LOCAL}/api/qualitygates/project_status?projectKey=${PROJECT_KEY}")"
echo "$STATUS_JSON" | python3 -c '
import json, sys
data = json.load(sys.stdin)["projectStatus"]
print("    Statut :", data["status"])
for condition in data.get("conditions", []):
    if condition.get("status") != "OK":
        print("    - %s : %s (seuil %s, valeur %s)" % (
            condition["metricKey"], condition["status"],
            condition.get("errorThreshold"), condition.get("actualValue")))
'
if echo "$STATUS_JSON" | grep -q '"status":"ERROR"'; then
  echo "Quality Gate ECHOUE : le pipeline doit s'arreter." >&2
  exit 2
fi
echo "Quality Gate OK : analyse disponible sur ${SONAR_URL_LOCAL}/dashboard?id=${PROJECT_KEY}"
