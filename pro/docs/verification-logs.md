# Vérification des logs, des erreurs et du profil production (tâche 8)

Date : 2026-11-07 · Base : `ebank` (seed de démonstration, 8 comptes) · Branche : `mbenboua`

Contrôles demandés : aucune trace technique exposée au client, aucun mot de passe / jeton /
numéro de compte complet dans les logs, un message fonctionnel + un code HTTP cohérent pour
chaque exception métier, niveaux INFO en production et DEBUG désactivé.

## 1. Aucun secret dans les logs

Le parcours complet a été rejoué sur l'application réelle (3 connexions admin/client/support,
1 connexion refusée, 1 dépôt, 1 export CSV, 1 pagination invalide, 1 refus 403, 1 accès
anonyme, 1 lecture support), puis les logs ont été filtrés :

| Motif recherché | Occurrences (profil par défaut) | Occurrences (profil `prod`) |
| --- | --- | --- |
| `Admin1234!` / `Client1234!` / `Agent1234!` | 0 | 0 |
| mot de passe erroné saisi au login | 0 | — |
| jeton JWT (`eyJ…`) | 0 | 0 |
| RIB complet d'un compte (`011780001100000000000149`) | 0 | — |
| numéro de compte complet (`1000000000001`) | 0 | — |
| `Using generated security password` | 0 après correctif (voir ci-dessous) | 0 |

Écart trouvé et corrigé : sans `UserDetailsService`, Spring Boot crée un utilisateur `user`
par défaut et **écrit son mot de passe généré dans les logs** (`Using generated security
password: <uuid>`). Même si l'authentification HTTP Basic est désactivée (donc cet utilisateur
inutilisable), cela viole le critère « aucun mot de passe dans les logs ».
Correctif : bean `InMemoryUserDetailsManager` vide dans `SecurityConfig` — l'utilisateur par
défaut et sa ligne de log disparaissent, rien d'autre ne change.

Aucun contrôleur ni service ne journalise de corps de requête, d'en-tête `Authorization`, de
mot de passe ou de RIB : le seul `log` applicatif est un avertissement sur `JWT_SECRET` absent
qui ne contient aucune valeur de secret.

## 2. Niveaux de log

`application.properties` (tous profils) : `logging.level.root=INFO`,
`logging.level.com.SecBank.pro=INFO`, SQL Hibernate coupé (`spring.jpa.show-sql=false`,
`format_sql=false`, `org.hibernate.SQL=WARN`).

`application-prod.properties` (nouveau, `SPRING_PROFILES_ACTIVE=prod`) : `root=INFO`,
application `INFO`, `org.springframework` et `org.hibernate` en `WARN`, SQL coupé, erreurs
HTTP sans message ni trace.

Mesures sur un démarrage réel :

| Profil | Lignes DEBUG/TRACE | Requêtes SQL journalisées | Niveaux observés |
| --- | --- | --- | --- |
| défaut (`:8090`) | 0 | 0 | 27 INFO, 4 WARN |
| `prod` (`:8091`) | 0 | 0 | 10 INFO, 4 WARN |

Sous `prod`, le parcours reste fonctionnel : `/v3/api-docs` 200, `/api/client/comptes` sans
jeton 401, login admin 200, export CSV 200, pagination invalide 400.

WARN restants, laissés volontairement (aucun n'expose de donnée) :
`spring.jpa.open-in-view is enabled` (le désactiver risquerait des `LazyInitializationException`
sur les réponses actuelles), les deux avertissements springdoc (documentation conservée aussi
en production pour le jury), et une ligne `400 BAD_REQUEST "page doit etre superieur…"` qui
journalise le motif d'un refus de pagination — message fonctionnel, sans donnée personnelle.

## 3. Erreurs : message générique côté client, trace côté serveur

`ApiExceptionHandler` possède désormais un filet de sécurité :

- toute exception déjà qualifiée par Spring (`ErrorResponse` : 400 de validation, 415, 404 de
  ressource statique…) garde son statut **et son corps standard** ;
- toute autre exception (bug, panne base) est journalisée en `ERROR` avec la stack trace
  complète et renvoyée au client en **500** avec un message unique :
  `{"message":"Erreur interne du serveur, réessayer plus tard"}` — ni classe, ni message
  d'origine, ni trace.

Vérifié par `ApiExceptionHandlerTest` (4 tests, `@WebMvcTest` + `OutputCaptureExtension`) :

| Test | Attendu | Résultat |
| --- | --- | --- |
| panne technique (`IllegalStateException`) | 500 générique, corps sans `IllegalStateException` / `java.lang` / `at com.SecBank` / RIB présent dans le message | OK |
| même panne, côté serveur | log `Erreur inattendue sur GET /test/panne` + stack trace complète | OK |
| ressource inconnue | 404 + `{"message":"Client introuvable avec id :9999"}` | OK |
| ressource d'un autre client | 403 + motif fonctionnel | OK |
| donnée invalide | 400 + motif fonctionnel | OK |

Sur l'application réelle, les corps d'erreur restent inchangés (le filtre ne dégrade rien) :
`{"message":"Le montant doit etre strictement positif"}` (400), pagination 400 en
`ProblemDetail` standard, 401 sans jeton, 403 mauvais rôle.

## 4. Contrat d'erreurs par exception métier

| Exception | Code HTTP | Message exposé |
| --- | --- | --- |
| `DonneeInvalideException` | 400 | motif métier |
| `OperationRefuseeException` | 400 | motif métier (solde insuffisant, compte bloqué…) |
| `RibInvalideException` | 400 | motif métier |
| `ResourceNotFoundException` | 404 | identifiant recherché |
| `AccesRefuseException` | 403 | motif métier |
| `RibDejaUtiliseException` | 409 | motif métier |
| `ResponseStatusException` (contrôleurs) | 400/404… | corps `ProblemDetail` standard |
| toute autre exception | 500 | message générique, trace uniquement dans les logs |

## 5. Reproduire

```bash
# profil par défaut, logs dans /tmp/ebank-logs2.log
cd pro && ./mvnw spring-boot:run
# profil production
SPRING_PROFILES_ACTIVE=prod ./mvnw spring-boot:run
# contrôle des fuites
grep -cE 'Admin1234!|Client1234!|eyJ|Using generated security password' /tmp/ebank-logs2.log
grep -cE ' (DEBUG|TRACE) ' /tmp/ebank-logs2.log
# contrat d'erreur
./mvnw -Dtest=ApiExceptionHandlerTest test
```

Suite complète : `./mvnw test` → **157 tests, 0 échec, 0 erreur, 0 ignoré**.
