# Qualité du code — SonarQube

**Responsable : Membre 2 (Mohamed) — branche `feature/sonarqube`.**

Analyse statique des deux applications du dépôt (backend Spring Boot Java 21 et
frontend Next.js/TypeScript), avec les rapports de couverture fournis par les
tests, et un **Quality Gate** que Jenkins peut utiliser pour bloquer le pipeline.

## Livrables et emplacements

| Livrable | Fichier |
| --- | --- |
| Serveur SonarQube (Community Build) + base dédiée | `docker-compose.yml` (services `sonarqube`, `sonar-db`) |
| Configuration de l'analyse (racine du dépôt) | `sonar-project.properties` |
| Couverture backend (JaCoCo) | `pro/pom.xml` (plugin `jacoco-maven-plugin`) |
| Commande d'analyse complète | `devops/sonarqube/analyze.sh` |
| Création du Quality Gate et de ses seuils | `devops/sonarqube/quality-gate.sh` |
| Cette documentation (URL, clé projet, token, seuils) | `devops/sonarqube/README.md` |

Identifiants de l'instance :

| Élément | Valeur |
| --- | --- |
| URL | <http://localhost:9000> (dans Docker : `http://sonarqube:9000`) |
| Clé du projet (`sonar.projectKey`) | `e-bank` |
| Nom affiché | `E-Bank` |
| Quality Gate par défaut | `E-Bank Quality Gate` |
| Compte initial | `admin` / `admin` (mot de passe à changer au premier login) |

Le token n'est **jamais** versionné : il se crée à la première connexion (section
suivante) et se stocke dans Jenkins (credential `sonarqube-token`) ou dans
`devops/sonarqube/.env`, ignoré par Git.

## 1. Démarrer le serveur

```bash
# à la racine du dépôt
docker compose up -d sonar-db sonarqube
docker compose ps            # sonar-db (healthy) puis sonarqube
```

Le premier démarrage prend 1 à 2 minutes (initialisation d'Elasticsearch et du
schéma). L'API répond `status: UP` quand le serveur est prêt :

```bash
curl -s http://localhost:9000/api/system/status
```

Prérequis machine : 4 Go de RAM disponibles et `vm.max_map_count >= 524288`
(valeur par défaut d'Ubuntu : 1048576, rien à faire sur ce poste).

Ouvrir <http://localhost:9000>, se connecter avec `admin` / `admin`, changer le
mot de passe. SonarQube crée le projet `e-bank` automatiquement à la première
analyse (la clé vient de `sonar-project.properties`).

## 2. Créer le token d'analyse

Dans l'interface : **My Account → Security → Generate Tokens**, ou en ligne de
commande. Deux portées utiles :

- **User Token** : hérite des droits du compte, donc permet aussi de créer le
  Quality Gate (`quality-gate.sh`). C'est celui documenté ici ;
- **Global Analysis Token** : limité au scan, suffisant pour le stage
d'analyse de Jenkins. En revanche `waitForQualityGate` interroge l'API web :
la configuration serveur de Jenkins doit utiliser un **User Token** (droits de
lecture suffisent).

```bash
curl -u admin:VOTRE_MOT_DE_PASSE -X POST \
  "http://localhost:9000/api/user_tokens/generate" \
  -d name=jenkins -d type=USER_TOKEN
```

Conserver la valeur renvoyée (elle n'est affichée qu'une fois) :

```bash
export SONAR_TOKEN=xxxxxxxxxxxxxxxx
# ou, pour ne pas la répéter :
printf 'SONAR_TOKEN=%s\n' "$SONAR_TOKEN" > devops/sonarqube/.env   # ignoré par Git
```

## 3. Lancer une analyse

```bash
devops/sonarqube/analyze.sh
```

Le script enchaîne :

1. `docker compose up -d sonar-db sonarqube` puis attente de `/api/system/status` ;
2. `cd pro && ./mvnw -B test` → tests + `pro/target/site/jacoco/jacoco.xml` ;
3. `docker compose --profile quality run --rm sonar-scanner` (l'image
   `sonarsource/sonar-scanner-cli`, aucun scanner à installer) ;
4. lecture du Quality Gate, code de sortie `2` s'il échoue (utilisable tel quel
   dans Jenkins).

Équivalent manuel :

```bash
export SONAR_TOKEN=xxxxxxxx
(cd pro && ./mvnw -B test)
docker compose --profile quality run --rm sonar-scanner
curl -s -H "Authorization: Bearer $SONAR_TOKEN" \
  "http://localhost:9000/api/qualitygates/project_status?projectKey=e-bank"
```

Résultat : <http://localhost:9000/dashboard?id=e-bank>

## 4. Quality Gate

```bash
SONAR_TOKEN=xxxxxxxx devops/sonarqube/quality-gate.sh     # compte admin requis
```

Le script crée le gate `E-Bank Quality Gate` (ou remplace ses conditions), le
rend par défaut et fixe la période de code nouveau à 30 jours (globale).

| Métrique | Seuil | Portée | Effet |
| --- | --- | --- | --- |
| `coverage` | ≥ 50 % | projet | couverture globale insuffisante → échec |
| `violations` | ≤ 10 | projet | dérive du nombre d'anomalies → échec |
| `duplicated_lines_density` | ≤ 5 % | projet | duplication excessive → échec |
| `new_violations` | 0 | code nouveau | un bug/vulnérabilité nouveau → échec |

Depuis SonarQube 2025.3 (mode MQR), bugs, vulnérabilités et code smells sont
regroupés dans la métrique `violations` : `new_violations` porte le critère
« aucun bug ni vulnérabilité nouvelle », `violations` le contrôle global.

Les seuils projet sont des **planchers de départ** calibrés sur l'existant
(couverture mesurée 56,6 %, duplication 0 %, 4 anomalies). Quand les tests
frontend de Soukayna produiront `coverage/lcov.info`, la couverture globale
montera : relever alors `coverage` à 80 % et ajouter une condition
`new_coverage ≥ 80 %` sur le code nouveau.

État mesuré après la mise en place : statut **OK** (`coverage` 56,6 % ≥ 50,
`violations` 4 ≤ 10, duplication 0 % ≤ 5, `new_violations` 0). L'interface
marque ce gate « non conforme Clean as You Code » parce qu'il mélange seuils
projet et seuil sur le code nouveau : c'est volontaire (plancher global +
verrou sur le nouveau code).

La période de code nouveau ne produit pas encore de lignes « nouvelles » : le
scanner lancé en conteneur n'obtient pas les informations SCM (`git blame`).
Les conditions `new_*` s'évalueront dès qu'une analyse contiendra du code
modifié (cas de Jenkins sur un commit récent).

Le gate s'applique aussi aux analyses manuelles ; il est visible dans
**Quality Gates** de l'interface. Pour bloquer le pipeline, Jenkins se contente
d'attendre le résultat (étape `waitForQualityGate`, section suivante).

## 5. Couverture attendue (fournie par Soukayna)

| Côté | Commande | Rapport |
| --- | --- | --- |
| Backend | `cd pro && ./mvnw test` | `pro/target/site/jacoco/jacoco.xml` (HTML : `pro/target/site/jacoco/index.html`) |
| Frontend | `cd e-bank-frontend && npm test -- --coverage` | `e-bank-frontend/coverage/lcov.info` |

`sonar-project.properties` contient déjà les deux chemins
(`sonar.coverage.jacoco.xmlReportPaths`, `sonar.javascript.lcov.reportPaths`) :
il suffit que les rapports existent au moment du scan. Côté frontend, la
configuration de test doit produire un reporter `lcov` (`coverage.reporter:
['lcov']` dans `vitest.config.ts`). Si le rapport est absent, le scanner affiche
un avertissement et l'analyse continue sans couverture JavaScript.

Sont exclus de l'exigence de couverture : `Dtos`, `Models` (entités JPA),
`ProApplication` et `ServletInitializer` (voir `sonar.coverage.exclusions`).

## 6. Intégration Jenkins (pour Taoufik)

Dans **Manage Jenkins → System → SonarQube servers** :

- Name : `SonarQube`
- Server URL : `http://sonarqube:9000` (si Jenkins tourne dans le même réseau
  Docker) ou `http://localhost:9000`
- Server authentication token : credential *Secret text* `sonarqube-token`

Le serveur SonarQube doit être démarré avant le stage (service `sonarqube` du
`docker-compose.yml` unique du projet).

```groovy
stage('Qualité - analyse SonarQube') {
    steps {
        withSonarQubeEnv('SonarQube') {
            sh 'cd pro && ./mvnw -B test'
            sh 'docker compose --profile quality run --rm sonar-scanner'
        }
    }
}

stage('Quality Gate') {
    steps {
        timeout(time: 5, unit: 'MINUTES') {
            waitForQualityGate abortPipeline: true
        }
    }
}
```

`waitForQualityGate` renvoie `OK` ou `ERROR` ; avec `abortPipeline: true`, le
pipeline s'arrête si le gate échoue. Le scanner tourne dans le réseau Compose et
joint `http://sonarqube:9000` (variable `SONAR_HOST_URL` du service
`sonar-scanner`).

## 7. Dépannage

| Symptôme | Cause / solution |
| --- | --- |
| `sonarqube` redémarre en boucle, `max virtual memory areas` | `sudo sysctl -w vm.max_map_count=524288` sur l'hôte |
| Conteneur tué (OOM) | SonarQube demande ~4 Go ; arrêter les autres conteneurs lourds |
| `401 Unauthorized` au scan | `SONAR_TOKEN` vide ou expiré (recréer un token dans *My Account → Security*) |
| `Project not found` | la clé du `sonar-project.properties` doit être `e-bank` |
| `No coverage report can be found` | rapport absent : lancer `mvn test` avant le scan (le script le fait) |
| API pas encore prête | attendre `status: UP` (`docker compose logs -f sonarqube`) |
| Repartir d'un serveur propre | `docker compose rm -sf sonarqube sonar-db && docker volume rm` des volumes `ebank-sonar-*` |

Attention : `docker compose down -v` supprime **tous** les volumes, y compris la
base applicative `ebank-db-data`. Pour ne réinitialiser que SonarQube, supprimer
uniquement `ebank-sonar-data`, `ebank-sonar-logs`, `ebank-sonar-extensions` et
`ebank-sonar-db-data`.

## 8. Validation (critères du PDF)

- [x] Analyse manuelle du projet affichée dans SonarQube : `analyze.sh` puis
      <http://localhost:9000/dashboard?id=e-bank>.
- [x] Quality Gate avec résultat passé/échoué : `quality-gate.sh` crée le gate,
      `analyze.sh` affiche son statut et sort en erreur s'il échoue.
- [x] Backend **et** frontend analysés : `sonar.sources` couvre
      `pro/src/main/java` et `e-bank-frontend/src`.
- [x] Couverture backend intégrée (JaCoCo, 56,6 % mesurés) ; le chemin lcov
      frontend est déjà configuré, le rapport sera produit par les tests frontend.
- [x] URL, clé projet et procédure de token documentées ci-dessus ; un token
      local (compte admin) est rangé dans `devops/sonarqube/.env`, non versionné.
