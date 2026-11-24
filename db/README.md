# Base de donnees de demonstration

`ebank-demo-2026-11-16.sql` est un dump complet de la base `ebank` (schema,
donnees, historique Flyway) pris le 16/11/2026. Il permet a chacun de tester
l'application avec exactement les memes donnees : comptes, virements, tickets,
dossiers KYC et journal d'audit.

## Restauration

Arreter d'abord le backend Spring (sinon ses connexions bloquent les `DROP`),
puis :

```bash
# 1. base vierge (efface les donnees locales du volume ebank-db-data)
docker compose down -v
docker compose up -d

# 2. chargement du dump
docker exec -i ebank-db psql -U ebank -d ebank < db/ebank-demo-2026-11-16.sql
```

Le dump commence par des `DROP ... IF EXISTS` : il peut aussi etre rejoue sur
une base deja existante, mais il remplace alors tout son contenu.

Sous PowerShell, la redirection `<` n'existe pas, utiliser :

```powershell
Get-Content db/ebank-demo-2026-11-16.sql -Raw | docker exec -i ebank-db psql -U ebank -d ebank
```

Ensuite relancer le backend : Flyway trouve un historique deja a jour et ne
rejoue aucune migration.

## Comptes de demonstration

| Email | Mot de passe | Role |
|---|---|---|
| `admin.demo1@demo.bankati.test` | `Admin1234!` | ADMIN |
| `admin.demo2@demo.bankati.test` | `Admin1234!` | ADMIN |
| `agent.demo1@demo.bankati.test` | `Agent1234!` | SUPPORT |
| `agent.demo2@demo.bankati.test` | `Agent1234!` | SUPPORT |
| `client.demo1@demo.bankati.test` | `Client1234!` | CLIENT |
| `client.demo2@demo.bankati.test` | `Client1234!` | CLIENT |
| `client.demo3@demo.bankati.test` | `Client1234!` | CLIENT |

Comptes pour tester un virement depuis `client.demo1` (compte 2001) :

| Destinataire | Numero de compte | RIB |
|---|---|---|
| client.demo2 | `1000000000003` | `011780001100000000000343` |
| client.demo3 | `1000000000005` | `011780001100000000000537` |

## Regenerer le dump

```bash
docker exec ebank-db pg_dump -U ebank -d ebank --no-owner --no-privileges --clean --if-exists \
  > db/ebank-demo-$(date +%Y-%m-%d).sql
```
