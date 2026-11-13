# Vérification du schéma, des index et des migrations (tâche 9)

Date : 2026-11-07 · Branche : `mbenboua` · PostgreSQL 16.15 (docker compose) · Spring Boot 4.1.1

Contrôles demandés : rejouer les migrations sur une base vide, vérifier les index (email, rib,
dates, clés étrangères), les contraintes d'unicité et les clés étrangères, puis comparer le schéma
obtenu avec celui de l'environnement de démonstration.

## 1. Migrations versionnées

| Fichier | Contenu |
| --- | --- |
| `db/migration/V1__schema_initial.sql` | 16 tables, clés primaires, 19 clés étrangères, contraintes `CHECK` des énumérations, 11 séquences hi/lo (incrément 50) |
| `db/migration/V2__index_et_unicite.sql` | unicité de `utilisateur.email`, index de dates et de statuts, index sur les clés étrangères (17 index nommés) |

Configuration : `spring.flyway.enabled=true`, `baseline-on-migrate=true`, `baseline-version=1`,
`spring.jpa.hibernate.ddl-auto=validate`. Hibernate ne modifie donc plus le schéma : il vérifie
que la base correspond au mapping.

Écart trouvé et corrigé : avec Spring Boot 4, les auto-configurations sont modularisées — ajouter
seulement `flyway-core` laisse les propriétés `spring.flyway.*` **sans effet** (aucune ligne Flyway
au démarrage, `flyway_schema_history` absente, puis `Schema validation: missing table
[administrateur]` sur base vide). Il faut `spring-boot-starter-flyway` (Flyway 12.4.0) ; le module
`flyway-database-postgresql` reste nécessaire. Vérifié après correctif : démarrage réussi.

## 2. Base vide → migrations appliquées dans l'ordre

| Étape | Résultat |
| --- | --- |
| Base `ebank_fresh` créée vide (0 table) | — |
| Démarrage de l'application | **OK**, `Tomcat started`, `/v3/api-docs` 200 |
| `flyway_schema_history` | `1 | schema initial | SQL | success=true` puis `2 | index et unicite | SQL | success=true` |
| Tables créées | **16** (+ `flyway_schema_history`) |
| Validation Hibernate | **0** erreur (`SchemaManagementException` absente) |
| Rejeu du seed sur cette base | **OK** : 8 comptes, 27 transactions, 5 tickets → les séquences de V1 sont bien celles attendues par Hibernate |

## 3. Base de démonstration → baseline sans perte

La base `ebank` contient déjà les tables d'un démarrage Hibernate précédent. Au démarrage :

```
Schema history table "public"."flyway_schema_history" does not exist yet
Creating Schema History table "public"."flyway_schema_history" with baseline ...
Successfully applied 1 migration to schema "public", now at version v2
```

| Étape | Résultat |
| --- | --- |
| Historique | `1 | << Flyway Baseline >> | BASELINE` + `2 | index et unicite | SQL | success=true` |
| Tables recréées | aucune (V1 est sautée par le baseline) |
| Données | intactes (comptes et transactions inchangés), puis remises à l'état du seed |
| Validation Hibernate | OK sur cette base aussi |

## 4. Index et contraintes

Le script `db/demo/check-schema.sql` (lecture seule, 11 contrôles) donne **0 écart** sur les deux
bases — celle créée par Flyway et celle de démonstration :

| Contrôle | Résultat |
| --- | --- |
| migrations en échec | 0 |
| V1 et V2 présentes en succès | 1 / 1 |
| 16 tables métier présentes | 16 / 16 |
| tables sans clé primaire | 0 |
| séquences hi/lo manquantes | 0 |
| unicité `email`, `username`, `rib`, `numero_compte`, `dossierkyc.client_id`, `ticket_support.reclamation_id` | 0 manquante |
| index attendus (17 noms) | 0 manquant |
| **clés étrangères sans index** | **0** (19 clés étrangères couvertes) |
| contraintes `CHECK` des énumérations | 10 / 10 |

Colonnes de recherche couvertes : `utilisateur.email` et `username` (unicité), `compte_bancaire.rib`
et `numero_compte` (unicité), `transaction.date_transaction` (+ composite `compte_id,
date_transaction`), `journal_audit.date_action`, `reclamation.date_creation`, statuts de tickets et
de réclamations, et toutes les clés étrangères.

## 5. Comparaison des schémas (base vide migrée ↔ démonstration)

Comparaison par catalogue (les noms d'index générés par Hibernate diffèrent, donc les définitions
sont normalisées avant diff) :

| Élément | Base migrée | Démonstration | Différences |
| --- | --- | --- | --- |
| Colonnes (nom, type, nullabilité, précision/longueur) | 83 | 83 | **0** |
| Contraintes (PK, UNIQUE, FK, CHECK) | 51 | 51 | **0** |
| Index (table, colonnes, unicité) | 163 | 163 | **0** (hors index internes `pg_toast`) |
| Séquences (nom, incrément) | 11 | 11 | **0** |

Seule différence avant normalisation : le **nom** des index adossés aux contraintes d'unicité
(`compte_bancaire_rib_key` créé par la migration contre `ukbek6px4i8xd0lvv3e2f3omjm` généré par
Hibernate). La structure est identique.

## 6. Deux défauts corrigés au passage

1. **`seed-demo.sql` échouait** si des entrées d'audit existaient pour les utilisateurs de démo
   (`update or delete on table "utilisateur" violates foreign key constraint ... on table
   "journal_audit"`). Le seed purge désormais `journal_audit` (portée `@demo.orabank.test`) avant
   de supprimer les utilisateurs. Vérifié : deux exécutions successives **OK** avec 2 entrées
   d'audit présentes.
2. **`check-demo.sql` comptait** les virements et opérations au guichet **globalement** (14 et 13)
   alors que le script se déclare valide sur une base contenant d'autres données. Les deux
   contrôles sont maintenant limités aux comptes 2001-2008 → **19/19 OK** sur la base de
   démonstration.

## 7. Reproduire

```bash
# 1. base vide : migrations + validation
docker compose exec -T db psql -U ebank -d postgres -c "CREATE DATABASE ebank_fresh"
cd pro && DB_URL=jdbc:postgresql://localhost:5432/ebank_fresh ./mvnw spring-boot:run
docker compose exec -T db psql -U ebank -d ebank_fresh -c "SELECT version, type, success FROM flyway_schema_history"

# 2. contrôles schéma + données (lecture seule)
docker compose cp pro/src/main/resources/db/demo/. db:/tmp/ebank-demo
docker compose exec -T db psql -U ebank -d ebank -f /tmp/ebank-demo/check-schema.sql
docker compose exec -T db psql -U ebank -d ebank -f /tmp/ebank-demo/check-demo.sql

# 3. suite de tests (H2, Flyway désactivé sur ce profil)
cd pro && ./mvnw test
```

Suite complète : **157 tests, 0 échec, 0 erreur, 0 ignoré**. Bases de contrôle supprimées après
vérification (seule `ebank` subsiste).
