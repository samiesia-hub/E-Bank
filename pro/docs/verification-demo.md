# Vérification des données de démonstration — tâche 6

Date : 2026-11-07 · Branche : `mbenboua` · Base : PostgreSQL 16 (conteneur `db`)

Objectif : s'assurer que le seed produit un parcours crédible et rejouable pour le jury.

## 1. Méthode

| Étape | Commande |
| --- | --- |
| Base vierge | `CREATE DATABASE ebank_demo_check` + schéma cloné depuis `ebank` |
| Seed #1 | `psql -f pro/src/main/resources/db/demo/seed-demo.sql` |
| Contrôles | `psql -f pro/src/main/resources/db/demo/check-demo.sql` |
| Seed #2 (base déjà peuplée) | même script relancé |
| Contrôles | relancés à l'identique |

## 2. Résultat : 19 contrôles, 0 écart

Exécution après le seed #1 **et** après le seed #2, résultats identiques :

| Contrôle | Attendu | Obtenu |
| --- | --- | --- |
| Utilisateurs de démo | 9 | 9 |
| Comptes de démo | 8 | 8 |
| Transactions des comptes de démo | 27 | 27 |
| Réclamations de démo | 5 | 5 |
| Tickets de démo | 5 | 5 |
| Virements | 14 | 14 |
| Opérations au guichet | 13 | 13 |
| Comptes à solde négatif | 0 | 0 |
| Comptes dont `solde <> historique exécuté` | 0 | 0 |
| Transactions sans compte | 0 | 0 |
| Virements sans transaction | 0 | 0 |
| Réclamations sans ticket | 0 | 0 |
| Statuts de tickets distincts (≥ 3) | OK | 4 |
| Statuts de réclamations distincts (≥ 3) | OK | 4 |
| E-mails de démo en doublon | 0 | 0 |
| RIB de démo invalides (clé mod 97) | 0 | 0 |
| Clients de démo sans compte | 0 | 0 |
| Comptes de démo à historique vide | 0 | 0 |

Idempotence : deux exécutions successives du seed ne produisent aucune violation d'unicité et
laissent exactement le même état (les lignes `@demo.orabank.test` sont supprimées puis réinsérées).

## 3. Scénario jouable par compte

| Client | Compte | Type | Statut | Solde | Tx | Dernière tx | Scénario |
| --- | --- | --- | --- | --- | --- | --- | --- |
| demo1 | 2001 | COURANT | ACTIF | 15 000.00 | 6 | 2026-11-01 | consultation + dépôt/retrait |
| demo1 | 2002 | EPARGNE | ACTIF | 40 000.00 | 3 | 2026-10-01 | consultation + dépôt/retrait |
| demo2 | 2003 | COURANT | ACTIF | 8 349.50 | 4 | 2026-10-20 | consultation + dépôt/retrait |
| demo2 | 2004 | PROFESSIONNEL | ACTIF | 55 249.25 | 3 | 2026-10-05 | consultation + dépôt/retrait |
| demo3 | 2005 | COURANT | ACTIF | 5 599.75 | 3 | 2026-10-22 | consultation + dépôt/retrait |
| demo3 | 2006 | EPARGNE | ACTIF | 17 500.00 | 2 | 2026-10-01 | consultation + dépôt/retrait |
| demo4 | 2007 | COURANT | **BLOQUE** | 2 049.70 | 3 | 2026-11-04 | refus attendu (dépôt/retrait → 400) |
| demo5 | 2008 | COURANT | ACTIF | 6 750.00 | 3 | 2026-11-05 | consultation + dépôt/retrait |

Tous les soldes sont positifs et égaux au cumul de l'historique exécuté
(`somme(crédits) − somme(débits)`), donc la consultation et l'export CSV affichent des chiffres
cohérents.

`client.demo4` ne possède que le compte bloqué : c'est **volontaire**, ce compte sert à démontrer
le refus d'opération (vérifié en réel : `POST …/2007/depot` → 400). Le cahier des charges fixe
8 comptes, on ne peut donc pas lui ajouter un compte actif sans sortir du format demandé.

## 4. Connexion des trois rôles (vérifié en réel)

Backend démarré sur la base ainsi seedée (`:8090`) :

| Compte | Rôle | Login | Données visibles |
| --- | --- | --- | --- |
| `admin.demo1@…` | `ADMIN` | 200 | `GET /api/admin/comptes` → 200, export CSV → 200 (BOM + 9 lignes) |
| `agent.demo1@…` | `AGENT_SUPPORT` | 200 | `GET /api/reclamations/4001` → 200 (client + ticket inclus) |
| `client.demo1@…` | `CLIENT` | 200 | `GET /api/client/comptes` → 200, `GET /api/client/transactions` → 200 |

## 5. Points relevés (hors périmètre du seed)

1. **Aucun endpoint de virement.** Le critère « virement possible » de la tâche ne peut pas être
   démontré : `VirementService` est un stub (`UnsupportedOperationException`) et n'est exposé par
   aucun contrôleur. Les seuls mouvements de fonds disponibles sont les dépôts/retraits admin.
2. **Une réclamation créée via l'API n'ouvre pas de ticket support.** Le seed associe un ticket à
   chacune des 5 réclamations, mais `POST /api/reclamations` crée la réclamation seule : elle
   n'apparaîtrait dans aucune file support (qui n'a d'ailleurs pas d'API de listing). À traiter
   avec l'équipe support.
3. **Fenêtre d'export par défaut de 30 jours.** L'historique de démo s'étale du 2026-08-01 au
   2026-11-05 : l'export sans paramètre ne contient que les 7 transactions des 30 derniers jours
   (9 lignes avec en-tête). C'est conforme au service (période par défaut de 30 jours), à garder
   en tête pendant la démo.

## 6. Reproduire

```bash
# base jetable + schéma + seed
docker compose exec -T db psql -U ebank -d postgres -c "DROP DATABASE IF EXISTS ebank_demo_check" \
  -c "CREATE DATABASE ebank_demo_check"
docker compose exec -T db pg_dump -U ebank -d ebank --schema-only --no-owner \
  | docker compose exec -T db psql -U ebank -d ebank_demo_check -q
docker compose exec -T db psql -U ebank -d ebank_demo_check -q \
  < pro/src/main/resources/db/demo/seed-demo.sql

# contrôles
docker compose exec -T db psql -U ebank -d ebank_demo_check -v ON_ERROR_STOP=1 \
  < pro/src/main/resources/db/demo/check-demo.sql
```
