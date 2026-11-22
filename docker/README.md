# Conteneurisation Orabank

Depuis la racine du dépôt :

```sh
docker compose up -d --build
docker compose ps
```

| Service Compose | Rôle | Port dans le réseau `orabank` | Port sur l'hôte |
| --- | --- | --- | --- |
| `db` | PostgreSQL 16 | `5432` | `5432` (`POSTGRES_PORT`) |
| `backend` | API Spring Boot | `8080` | `8080` (`BACKEND_PORT`) |
| `frontend` | Site Next.js | `3000` | `3000` (`FRONTEND_PORT`) |

Le site s'ouvre sur `http://localhost:3000` et l'API sur `http://localhost:8080`. Les services se joignent par leurs noms Compose : le backend utilise `db:5432`. Le réseau et le volume `ebank-db-data` sont créés automatiquement. Les futurs services de monitoring et d'analyse peuvent être ajoutés au même `docker-compose.yml` et au réseau `orabank`.

La commande `docker compose ps` doit afficher `healthy` pour les trois services. La base est vérifiée avec `pg_isready`, l'API avec `/v3/api-docs`, et le site avec `/`. Le frontend attend que le backend soit sain, lequel attend PostgreSQL. Pour les journaux : `docker compose logs --tail=100 db backend frontend`.

## Configuration locale

Les valeurs par défaut de PostgreSQL et du secret JWT servent uniquement au développement local. Pour les remplacer, créer un fichier `.env` à la racine (ignoré par Git) :

```dotenv
POSTGRES_DB=ebank
POSTGRES_USER=ebank
POSTGRES_PASSWORD=remplacer-par-un-mot-de-passe
JWT_SECRET=remplacer-par-une-chaine-aleatoire-de-32-caracteres-minimum
POSTGRES_PORT=5432
BACKEND_PORT=8080
FRONTEND_PORT=3000
NEXT_PUBLIC_API_URL=http://localhost:8080/api
```

`JWT_SECRET` est transmis aux deux applications et doit être identique. `NEXT_PUBLIC_API_URL` est incorporée au site pendant la construction de son image : modifier cette URL exige `docker compose up -d --build`. Elle doit être accessible depuis le navigateur de l'utilisateur, et le backend doit autoriser cette origine dans sa configuration CORS. Pour un déploiement exposé publiquement, configurer aussi HTTPS et des secrets dédiés avant de lancer les services.

`docker compose stop` et `docker compose down` conservent les données PostgreSQL. `docker compose down -v` détruit le volume : ne l'utiliser que pour réinitialiser volontairement la base.

Pour ouvrir `psql` : `docker compose exec db psql -U ebank -d ebank` (adapter le nom et l'utilisateur si `.env` les modifie).
