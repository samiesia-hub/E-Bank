# Vérification des APIs — tâche 5

Date : 2026-11-07 · Branche : `mbenboua` · Backend : `pro/` (Spring Boot 4.1.1, PostgreSQL 16)

Objectif : vérifier que l'ensemble des APIs livrées répond correctement avant le gel du code,
relever les écarts entre le contrat attendu et le comportement réel, puis corriger les bugs bloquants.

## 1. Suite de tests automatisés

```bash
cd pro && ./mvnw test
```

| Indicateur | Valeur |
| --- | --- |
| Tests exécutés | **153** |
| Échecs | 0 |
| Erreurs | 0 |
| Tests ignorés | 0 |
| Build | `BUILD SUCCESS` |

Couverture des contrôleurs (tous couverts par un `@WebMvcTest`) : `AuthController` (7),
`CompteController` (8), `TransactionController` (10), `OperationController` (6),
`ReclamationController` (3).

Tests d'intégration (`@SpringBootTest` + MockMvc sur H2) : `CompteIntegrationTest` (13),
`TransactionIntegrationTest` (13), `AdminExportIntegrationTest` (8).

### Concurrence sur les mouvements de solde

`integration/DebitConcurrentIntegrationTest` (3 tests, vrais threads + vraies transactions) :

| Scénario | Résultat attendu | Résultat |
| --- | --- | --- |
| 10 retraits concurrents de 25 MAD sur un solde de 100 | 4 réussites, 6 refus métier, solde 0.00 | OK |
| 10 dépôts concurrents de 50 MAD | 10 réussites, solde 500.00 | OK |
| 10 débits concurrents sur un compte `BLOQUE` | 10 refus, solde inchangé | OK |

> Le service `VirementService` de ce module est encore un **stub**
> (`UnsupportedOperationException`) : la concurrence est donc vérifiée sur la brique atomique
> réellement utilisée par les débits/crédits (`CompteService.debiter/crediter`).

## 2. Vérification réelle des endpoints

Méthode : base jetable `ebank_api_check` (schéma cloné puis `seed-demo.sql`), backend démarré sur
`http://localhost:8090`, appels `curl` avec les trois jetons de démonstration.

| Méthode | Endpoint | Rôle testé | Attendu | Obtenu | Verdict |
| --- | --- | --- | --- | --- | --- |
| POST | `/api/auth/login` | ADMIN / CLIENT / SUPPORT | 200 | 200 | OK |
| POST | `/api/auth/login` (mot de passe faux) | — | 401 | 401 | OK |
| POST | `/api/auth/logout` | CLIENT | 200 | 200 | OK |
| GET | `/api/admin/comptes` | ADMIN | 200 | 200 | OK |
| GET | `/api/admin/comptes` | CLIENT / SUPPORT | 403 | 403 | OK |
| GET | `/api/admin/comptes` | anonyme | 401 | 401 | OK |
| GET | `/api/admin/transactions` | ADMIN | 200 | 200 | OK |
| GET | `/api/admin/transactions` | CLIENT | 403 | 403 | OK |
| GET | `/api/admin/transactions/export` | ADMIN | 200 + CSV/BOM | 200, `text/csv`, BOM `EF BB BF` | OK |
| POST | `/api/admin/comptes` | ADMIN | 201 | 201 | OK |
| POST | `/api/admin/comptes` | CLIENT / SUPPORT | 403 | 403 | OK |
| POST | `/api/admin/comptes/{id}/depot` | ADMIN | 201 | 201 | OK |
| POST | `/api/admin/comptes/{id}/retrait` | ADMIN | 201 | 201 | OK |
| POST | `/api/admin/comptes/{id}/retrait` (solde insuffisant) | ADMIN | 4xx | 400 | OK |
| POST | `/api/admin/comptes/{id}/depot` (compte bloqué) | ADMIN | 4xx | 400 | OK |
| PATCH | `/api/admin/comptes/{id}/statut` | ADMIN | 200 | 200 | OK |
| PATCH | `/api/admin/comptes/{id}/statut` (`CLOTURE`) | ADMIN | 400 | 400 | OK |
| GET | `/api/client/comptes` | CLIENT | 200 | 200 | OK |
| GET | `/api/client/comptes` | ADMIN / SUPPORT | 403 | 403 | OK |
| GET | `/api/comptes/{id}` (compte du client) | CLIENT | 200 | 200 | OK |
| GET | `/api/comptes/{id}` (compte d'un autre client) | CLIENT | 404 | 404 | OK |
| GET | `/api/client/transactions` | CLIENT | 200 | 200 | OK |
| GET | `/api/comptes/{id}/transactions` | CLIENT | 200 | 200 | OK |
| GET | `/api/client/transactions?size=0` | CLIENT | 400 | 400 | OK |
| GET | `/api/comptes/{id}` | SUPPORT | 4xx | 404 | OK (anti-énumération) |
| GET | `/api/comptes/999999` | ADMIN | 404 | 404 | OK |
| GET | `/api/comptes/abc` | ADMIN | 400 | 400 | OK |
| GET | `/api/reclamations/{id}` | CLIENT propriétaire | 200 | 200 (corps JSON valide après correctif) | **CORRIGÉ** |
| GET | `/api/reclamations/{id}` | SUPPORT | 200 | 200 (corps valide) | **CORRIGÉ** |
| POST | `/api/reclamations` | SUPPORT | 201 | 201 (corps valide) | **CORRIGÉ** |

Comportements de référence retenus : opération métier refusée (solde insuffisant, compte bloqué,
clôture, montant invalide) → **400** ; ressource absente ou compte d'un tiers → **404** ;
rôle insuffisant → **403** ; absence de jeton → **401**.

## 3. Écarts relevés

### 3.1 CORRIGÉ — `/api/reclamations` renvoyait un corps JSON cassé

- **Symptôme** : statut correct (200/201) mais corps illisible ; log serveur :
  `HttpMessageNotWritableException: Could not write JSON: Document nesting depth (501) exceeds the maximum allowed (500)`.
- **Cause** : sérialisation cyclique `Reclamation → client → compteBancaires → client → …`
  (et `reclamation ↔ ticketSupport`, `reclamation ↔ responseReclamations`).
- **Correctif** : `@JsonIgnore` sur les références arrière `Client.compteBancaires`,
  `Client.beneficiaires`, `Client.dossierKYC`, `TicketSupport.reclamation`,
  `ResponseReclamation.reclamation`. Le contrat utile est conservé
  (`client`, `ticketSupport`, `responseReclamations` restent exposés).
- **Régression couverte** : `ReclamationControllerTest.serialisationAvecGrapheCompletNeBouclePas`.
- **Vérifié** : `GET /api/reclamations/4001` → 200, 1326 octets, JSON exploitable ; `POST` → 201 ;
  0 occurrence de `nesting depth` dans le log après correctif.

### 3.2 OUVERT (sécurité) — contrôle d'accès absent sur les réclamations

`POST /api/reclamations?clientId=…` accepte n'importe quel `clientId`, et
`GET /api/reclamations/{id}` ne vérifie pas le propriétaire : un client authentifié peut lire la
réclamation d'un autre client, et un agent support peut en créer une au nom de n'importe qui.

Preuve observée : `client.demo2` lit la réclamation `4001` de `client.demo1` → 200 ;
`agent.demo1` crée une réclamation pour `clientId=1005` → 201.

Correctif recommandé (non appliqué : code du module client/support, à arbitrer avec l'équipe) :
exiger que le `clientId` corresponde au porteur du jeton pour un `CLIENT`, et n'autoriser
`GET /{id}` que pour le propriétaire ou un rôle `ADMIN`/`SUPPORT`.

### 3.3 RÉSOLU (tâche 7) — contrat Swagger et collection Postman

Au moment de cette vérification, `pom.xml` n'avait aucune dépendance `springdoc` : ni `swagger-ui`
ni `/v3/api-docs`, donc aucune comparaison contrat ↔ réel possible. C'est depuis corrigé :

- `springdoc-openapi-starter-webmvc-ui` 3.2.0 (seule ligne compatible Spring Boot 4.1.x),
  `/v3/api-docs` et `/swagger-ui.html` exposés en lecture publique ;
- 16 chemins / 17 opérations, chacune avec résumé, exemple de requête et codes d'erreur ;
- collection Postman (`pro/docs/postman/`) exécutée de bout en bout : **32 requêtes, 0 échec**.

La matrice de la section 2 reste la référence des codes réellement renvoyés.

### 3.4 OUVERT — endpoints support / KYC / audit inexistants

Les services existent mais ne sont exposés par aucun contrôleur :

| Service | Contrôleur | Conséquence |
| --- | --- | --- |
| `TicketService` | aucun | pas d'API de traitement des tickets |
| `KycService` | aucun | pas d'API de validation KYC |
| `AuditService` | aucun | journaux consultables seulement en base |
| `VirementService` | aucun | **stub** (`UnsupportedOperationException`) |
| `ReleveService`, `BeneficiaireService` | aucun | pas d'API relevés / bénéficiaires |

Les « tests manquants sur les contrôleurs support, KYC et audit » ne peuvent donc pas être écrits
sans créer ces endpoints : à planifier avec l'équipe qui porte ces modules.

## 4. Reproduction

```bash
# base jetable + seed
docker compose exec -T db psql -U ebank -d postgres -c "DROP DATABASE IF EXISTS ebank_api_check" -c "CREATE DATABASE ebank_api_check"
docker compose exec -T db pg_dump -U ebank -d ebank --schema-only --no-owner \
  | docker compose exec -T db psql -U ebank -d ebank_api_check -q
docker compose exec -T db psql -U ebank -d ebank_api_check -q < pro/src/main/resources/db/demo/seed-demo.sql

# backend sur la base de vérification
cd pro && DB_URL='jdbc:postgresql://localhost:5432/ebank_api_check' DB_USER=ebank DB_PASSWORD=ebank \
  SERVER_PORT=8090 ./mvnw spring-boot:run
```

Identifiants : voir `pro/README.md`, section « Données de démonstration ».
