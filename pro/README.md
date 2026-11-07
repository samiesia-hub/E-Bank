# E-Banking Application

Application web de gestion bancaire développée avec Spring Boot et REACT. Le projet permet de mettre en pratique le développement full-stack, la gestion d'une base de données relationnelle et le déploiement avec Docker.

## Technologies utilisées

### Backend
- Java
- Spring Boot
- Maven
- Spring Data JPA
- Spring Security



### DevOps
- Git et GitHub
- Docker
- Docker Compose
- Kubernetes 
- Jenkins


## Architecture du projet


Adaptez cette structure aux dossiers présents dans le dépôt.

## Prérequis

Pour exécuter le projet en développement :

- JDK compatible avec le `pom.xml`
- IntelliJ IDEA
- Maven

Pour le lancement conteneurisé :

- Docker
- Docker Compose

## 1. Cloner le dépôt


## 2. Lancer le backend avec IntelliJ IDEA

1. Ouvrez IntelliJ IDEA.
2. Cliquez sur **Open**.
3. Sélectionnez le dossier `backend`.
4. Importez le projet Maven à partir du fichier `pom.xml`.
5. Configurez le JDK compatible avec le projet.
6. Attendez que Maven télécharge les dépendances.
7. Ouvrez la classe principale de Spring Boot.
8. Cliquez sur **Run** pour démarrer l'application.

script n'existe pas, vérifiez les scripts disponibles dans `package.json`. Une application Angular utilise souvent `ng serve` pour le développement.




## 7. Commandes Maven utiles

Compiler le projet :

```bash
mvn clean compile
```

Exécuter les tests :

```bash
mvn test
```

Construire le package :

```bash
mvn clean package
```

Lancer Spring Boot :

```bash
mvn spring-boot:run
```

## 8. Base de données (Docker)

Le plus simple : PostgreSQL dans Docker, sans rien installer.

```bash
docker compose up -d      # démarre PostgreSQL (base ebank / utilisateur ebank / mot de passe ebank)
docker compose ps         # la colonne STATUS doit afficher (healthy)
```

Puis lancez le backend :

```bash
cd pro
./mvnw spring-boot:run
```

Par défaut le backend se connecte à `jdbc:postgresql://localhost:5432/ebank` avec `ebank` / `ebank`,
c'est-à-dire les valeurs de `docker-compose.yml`. Pour une autre base, définissez `DB_URL`, `DB_USER` et
`DB_PASSWORD`. Les tests, eux, utilisent H2 en mémoire et ne demandent aucun conteneur.

```bash
docker compose stop         # arrête la base (les données restent dans le volume)
docker compose down -v      # supprime aussi les données
```

Authentification : le frontend (`e-bank-frontend`) et le backend doivent partager le **même** `JWT_SECRET`
(32 caractères minimum). Sans `JWT_SECRET`, le backend génère une clé aléatoire à chaque démarrage et les
cookies du frontend ne sont plus valides.

## 9. Dépannage


### Problème de version Java
- Vérifiez `java -version`.
- Vérifiez la version utilisée par Maven avec `mvn -version`.
- Vérifiez le JDK configuré dans IntelliJ IDEA.
- Respectez la version Java déclarée dans le `pom.xml`.


## 10. Données de démonstration (seed)

Le jeu de données de démonstration est fourni sous forme de scripts SQL, dans
`pro/src/main/resources/db/demo/` :

| Fichier | Rôle |
| --- | --- |
| `seed-demo.sql` | Insère les données de démo. **Idempotent** : les lignes de démonstration (domaine d'e-mail réservé `@demo.bankati.test`) sont supprimées puis réinsérées, deux exécutions donnent donc le même état. |
| `reset-demo.sql` | `TRUNCATE` de toutes les tables (séquences remises à zéro) puis rechargement du seed. Efface **toutes** les données : base de développement uniquement. |

Contenu inséré : 2 administrateurs, 2 agents support, 5 clients, 8 comptes (courant, épargne,
professionnel, un compte bloqué), 27 transactions (virements et opérations au guichet),
5 réclamations et 5 tickets support avec des statuts variés.
Pour chaque compte, **le solde est exactement la somme de son historique**
(`somme(crédits) − somme(débits)`), ce qui rend la démo cohérente en consultation comme en export CSV.

### Mise en place

Le schéma est créé par Hibernate au premier démarrage du backend : lancez-le une fois
(`cd pro && ./mvnw spring-boot:run`), puis arrêtez-le et chargez le seed.

```bash
# rend les scripts accessibles dans le conteneur
docker compose cp pro/src/main/resources/db/demo/. db:/tmp/ebank-demo

# charge (ou recharge) les données de démonstration
docker compose exec -T db psql -U ebank -d ebank -v ON_ERROR_STOP=1 \
    -f /tmp/ebank-demo/seed-demo.sql

# réinitialisation complète (efface tout, puis recharge le seed)
docker compose exec -T db psql -U ebank -d ebank -v ON_ERROR_STOP=1 \
    -f /tmp/ebank-demo/reset-demo.sql
```

### Identifiants de démonstration

| Rôle | E-mail | Mot de passe |
| --- | --- | --- |
| Administrateur | `admin.demo1@demo.bankati.test` | `Admin1234!` |
| Administrateur | `admin.demo2@demo.bankati.test` | `Admin1234!` |
| Agent support | `agent.demo1@demo.bankati.test` | `Agent1234!` |
| Agent support | `agent.demo2@demo.bankati.test` | `Agent1234!` |
| Client | `client.demo1@demo.bankati.test` | `Client1234!` |
| Client | `client.demo2@demo.bankati.test` | `Client1234!` |
| Client | `client.demo3@demo.bankati.test` | `Client1234!` |
| Client | `client.demo4@demo.bankati.test` | `Client1234!` |
| Client | `client.demo5@demo.bankati.test` | `Client1234!` |

> ⚠️ **Données de test non sécurisées.** Noms, numéros de comptes, RIB et téléphones sont
> entièrement synthétiques, et les mots de passe ci-dessus sont publics (seuls leurs hashes
> BCrypt figurent dans le script). Ils ne doivent jamais être réutilisés pour un compte réel
> ni conservés sur une base exposée. Les scripts ne sont jamais exécutés automatiquement par
> l'application : `ddl-auto` ne crée que le schéma.

### Vérifier la cohérence

`check-demo.sql` (lecture seule) contrôle la volumétrie, les soldes face à l'historique, les clés
étrangères, les RIB (clé modulo 97), les statuts variés et le scénario jouable de chaque compte.
Il est limité aux données de démonstration, donc exécutable sur une base déjà peuplée :

```bash
docker compose cp pro/src/main/resources/db/demo/. db:/tmp/ebank-demo
docker compose exec -T db psql -U ebank -d ebank -v ON_ERROR_STOP=1 \
    -f /tmp/ebank-demo/check-demo.sql
```

Les 19 lignes de la première section doivent toutes afficher le verdict `OK` (vérifié sur base
vierge puis après une seconde exécution du seed). La deuxième section rappelle le scénario de
chaque compte : `client.demo1` à `client.demo3` et `client.demo5` ont des comptes actifs avec
historique, et `client.demo4` porte le **compte bloqué** (2007), qui sert à jouer le scénario de
refus d'opération (dépôt/retrait → 400).

## 11. Authentification (JWT)

```bash
# 1. obtenir un jeton (validite 24 h, reglable via jwt.expiration-ms)
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"client.demo1@demo.bankati.test","motDePasse":"Client1234!"}' | jq -r .token)

# 2. appeler l'API
curl -s http://localhost:8080/api/client/comptes -H "Authorization: Bearer $TOKEN"
```

`/api/auth/login` renvoie `{email, nom, role, token}` et pose en plus un cookie httpOnly `session`
(utilisé par le frontend). Rôles : `ADMIN`, `CLIENT`, `AGENT_SUPPORT` — `/api/admin/**` exige `ADMIN`,
`/api/client/**` exige `CLIENT`, le reste demande un jeton valide. Il n'y a **pas de refresh token** :
le jeton vit 24 h et il suffit de se reconnecter ; `/api/auth/logout` efface le cookie, le jeton déjà
émis reste valable jusqu'à son expiration (le supprimer côté client).

## 12. Documentation de l'API (Swagger / Postman)

- Contrat OpenAPI : `http://localhost:8080/v3/api-docs` · Swagger UI : `http://localhost:8080/swagger-ui.html`
  (généré par springdoc 3.2.0 depuis le code : 16 chemins / 17 opérations, chacune avec résumé, exemple et codes d'erreur).
- Collection Postman : `pro/docs/postman/E-Bank.postman_collection.json` avec l'environnement
  `pro/docs/postman/E-Bank.postman_environment.json` (ajuster `baseUrl`). Exécuter d'abord le dossier
  **1. Authentification** : il mémorise les jetons `adminToken`, `clientToken`, `supportToken`.
- Sans Postman (parcours complet des 32 requêtes, codes HTTP vérifiés) :

```bash
python3 pro/docs/postman/executer-collection.py \
    pro/docs/postman/E-Bank.postman_collection.json \
    pro/docs/postman/E-Bank.postman_environment.json
```

## Auteur

Projet académique Taoufik Mansouri
