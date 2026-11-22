#!/usr/bin/env bash
# Cree (ou met a jour) le Quality Gate "E-Bank Quality Gate" et le rend
# par defaut. Idempotent : relancable sans creer de doublon.
#
# Prerequis : SONAR_TOKEN d'un compte administrateur (token de type USER_TOKEN,
# un token d'analyse ne suffit pas pour administrer un Quality Gate), curl, python3.
#
# Compatible SonarQube Community Build 26.x : l'API qualite travaille par nom
# (gateName / name), les identifiants numeriques n'existent plus.
set -euo pipefail

SONAR_PORT="${SONAR_PORT:-9000}"
SONAR_URL="${SONAR_URL:-http://localhost:${SONAR_PORT}}"
GATE_NAME="${GATE_NAME:-E-Bank Quality Gate}"

if [[ -z "${SONAR_TOKEN:-}" && -f "$(dirname "${BASH_SOURCE[0]}")/.env" ]]; then
  set -a
  # shellcheck disable=SC1091
  . "$(dirname "${BASH_SOURCE[0]}")/.env"
  set +a
fi
if [[ -z "${SONAR_TOKEN:-}" ]]; then
  echo "ERREUR : SONAR_TOKEN est vide (compte admin requis pour creer un Quality Gate)." >&2
  exit 1
fi

api() {
  local method="$1" path="$2"
  shift 2
  curl -fsS -X "$method" -H "Authorization: Bearer ${SONAR_TOKEN}" \
    "${SONAR_URL}/api/${path}" "$@"
}

show_gate() {
  api GET qualitygates/show --get --data-urlencode "name=${GATE_NAME}"
}

# Seuils projet (evalues a chaque analyse) + verrou sur le code nouveau.
#
#  - couverture plancher 50 % : le backend est a ~56 % ; le seuil passera a
#    80 % sur le code nouveau quand les tests frontend (lcov) seront livres ;
#  - violations : 4 aujourd'hui (1 bug, 1 vulnerabilite, 2 code smells) ;
#    le plafond 10 bloque une derive, il sera abaisse apres correction ;
#  - duplication quasi nulle (0 %) ;
#  - aucune violation nouvelle toleree (mode MQR : bugs + vulnerabilites
#    + code smells regroupes dans la metrique "violations").
CONDITIONS=(
  "coverage|LT|50"
  "violations|GT|10"
  "duplicated_lines_density|GT|5"
  "new_violations|GT|0"
)

echo "==> Quality Gate \"${GATE_NAME}\""

if show_gate >/dev/null 2>&1; then
  echo "    Deja present : conditions remplacees."
  show_gate \
    | python3 -c 'import json,sys
for condition in json.load(sys.stdin).get("conditions", []):
    print(condition["id"])' \
    | while read -r condition_id; do
        api POST qualitygates/delete_condition --data-urlencode "id=${condition_id}" >/dev/null
      done
else
  api POST qualitygates/create --data-urlencode "name=${GATE_NAME}" >/dev/null
  echo "    Cree."
fi

for entry in "${CONDITIONS[@]}"; do
  IFS='|' read -r metric op threshold <<< "$entry"
  if ! api POST qualitygates/create_condition \
    --data-urlencode "gateName=${GATE_NAME}" \
    --data-urlencode "metric=${metric}" \
    --data-urlencode "op=${op}" \
    --data-urlencode "error=${threshold}" >/dev/null; then
    echo "ERREUR : condition ${metric} ${op} ${threshold} refusee par le serveur." >&2
    exit 1
  fi
  echo "    ${metric} ${op} ${threshold}"
done

api POST qualitygates/set_as_default --data-urlencode "name=${GATE_NAME}" >/dev/null
echo "==> Quality Gate par defaut : ${GATE_NAME}"

# Sans periode de code nouveau, les conditions "new_*" ne sont pas evaluables
# (statut NONE au premier scan). 30 jours rendent le gate operationnel des la
# premiere analyse, sans forcer une seconde analyse de reference.
echo "==> Periode de code nouveau : 30 jours (globale)"
if ! api POST new_code_periods/set \
  --data-urlencode "type=NUMBER_OF_DAYS" \
  --data-urlencode "value=30" >/dev/null; then
  echo "ERREUR : impossible de definir la periode de code nouveau." >&2
  exit 1
fi

echo "==> Conditions enregistrees"
show_gate \
  | python3 -c 'import json,sys
data = json.load(sys.stdin)
print("    defaut :", data.get("isDefault"))
for condition in data.get("conditions", []):
    print("    - %s %s %s" % (condition["metric"], condition["op"], condition["error"]))'
