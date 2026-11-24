# Orabank

Application bancaire en ligne (projet **M2I S3**, équipe Genie Logiciel) : vitrine publique, espace client,
espace support et espace administrateur.

Le dépôt contient les deux applications et la base de données de développement :

| Dossier | Rôle | Techno |
|---|---|---|
| `e-bank-frontend/` | Interface web | Next.js 16 (App Router), TypeScript, Tailwind v4 |
| `pro/` | API REST | Spring Boot 4.1.1, Java 21, Spring Security, JPA |
| `docker-compose.yml` | Application complète et base de données | Docker Compose |

---

## Sommaire

1. [Aperçu fonctionnel](#1-aperçu-fonctionnel)
2. [Stack technique](#2-stack-technique)
3. [Architecture du dépôt](#3-architecture-du-dépôt)
4. [Prérequis](#4-prérequis)
5. [Démarrage rapide](#5-démarrage-rapide)
6. [Variables d'environnement](#6-variables-denvironnement)
7. [Authentification et rôles](#7-authentification-et-rôles)
8. [API disponibles](#8-api-disponibles)
9. [Tests et qualité](#9-tests-et-qualité)
10. [Commandes Docker](#10-commandes-docker)
11. [Dépannage](#11-dépannage)
12. [Conventions d'équipe](#12-conventions-déquipe)
13. [Documentation détaillée](#13-documentation-détaillée)

---

## 1. Aperçu fonctionnel

**Public** — accueil, services, sécurité, FAQ.

**Client** — inscription, connexion, tableau de bord, comptes, virements, bénéficiaires, réclamations, profil.

**Support** — file de tickets (`AGENT_SUPPORT`, `ADMIN`).

**Admin** — utilisateurs, comptes bancaires, transactions, KYC, audit, rôles et permissions, employés support.

État actuel : l'authentification (JWT + cookie httpOnly) et les réclamations sont branchées sur l'API.
Les écrans client / support / admin existent côté interface et affichent des données de démonstration
tant que leurs endpoints ne sont pas livrés. Les visuels sont dans `screenshots/`, le diagramme de
classes dans `diagramme de classe.jpeg`.

## 2. Stack technique

**Backend** : Java 21, Spring Boot 4.1.1, Spring Web, Spring Security, Spring Data JPA, PostgreSQL (H2 pour
les tests), JWT (`jjwt 0.12.6`), BCrypt, Maven (wrapper inclus).

**Frontend** : Next.js 16.4, React 19, TypeScript, Tailwind CSS v4, TanStack Query, Zustand, React Hook
Form, Zod, `jose` (vérification JWT dans `proxy.ts`).

**Outils** : Git / GitHub, Docker et Docker Compose, Jira (projet `SCRUM`).

## 3. Architecture du dépôt

```
E-Bank/
├── docker-compose.yml            # frontend, backend et PostgreSQL
├── screenshots/                  # captures des écrans
├── diagramme de classe.jpeg      # modèle de données
│
├── pro/                          # API Spring Boot
│   ├── src/main/java/com/SecBank/pro/
│   │   ├── Configurations/       # SecurityConfig, JwtService, JwtAuthFilter
│   │   ├── Conrollers/           # AuthController, Clients/, Admin/
│   │   ├── Services/             # AuthService, OtpService, Clients/, Admin/
│   │   ├── Repositories/         # UserRepo, Client/, Admin/
│   │   ├── Models/               # Utilisateur, Cliens/, Admin/, Enums/
│   │   ├── Dtos/ · Mappters/ · Exceptions/
│   │   └── ProApplication.java
│   ├── src/main/resources/application.properties
│   └── src/test/                 # tests JUnit (base H2 en mémoire)
│
└── e-bank-frontend/              # interface Next.js
    ├── src/app/                  # (public) (auth) (client) (support) admin/
    ├── src/components/interface/ # kit UI (Bouton, ChampSaisie, TableauDonnees…)
    ├── src/features/             # auth, comptes, virements, reclamations, admin
    ├── src/lib/client-api.ts     # fetch + cookie de session
    ├── src/types/banque.ts       # types miroir du diagramme de classes
    └── src/proxy.ts              # garde de routes par rôle (UI seulement)
```

## 4. Prérequis

- **JDK 21 ou plus** (`java -version`) — le projet cible Java 21
- **Node.js 22 ou plus** et npm
- **Docker Desktop** (pour PostgreSQL) — ou un serveur PostgreSQL existant

Aucune installation de PostgreSQL n'est nécessaire : la base tourne dans un conteneur.

## 5. Démarrage rapide

### 1) Base de données

```bash
docker compose up -d db     # pour exécuter seulement PostgreSQL en développement
docker compose ps          # STATUS doit afficher (healthy)
```

### 2) Backend (terminal 1)

```bash
cd pro
export JWT_SECRET="change-moi-chaine-aleatoire-32-caracteres"   # 32 caractères minimum
./mvnw spring-boot:run
```

PowerShell :

```powershell
cd pro
$env:JWT_SECRET = "change-moi-chaine-aleatoire-32-caracteres"
.\mvnw.cmd spring-boot:run
```

API sur **http://localhost:8080**.

### 3) Frontend (terminal 2)

```bash
cd e-bank-frontend
npm install
cp .env.example .env.local     # même JWT_SECRET que le backend
npm run dev
```

Interface sur **http://localhost:3000**.

> Le `JWT_SECRET` doit être **identique** des deux côtés : le backend signe le cookie `session`,
> `proxy.ts` le vérifie côté Next.js. Sinon les pages protégées renvoient vers `/connexion`.

## 6. Variables d'environnement

### Backend (`pro/`)

| Variable | Défaut | Rôle |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/ebank` | URL JDBC |
| `DB_USER` | `ebank` | Utilisateur base |
| `DB_PASSWORD` | `ebank` | Mot de passe base (valeur de dev) |
| `DDL_AUTO` | `update` | Stratégie Hibernate (`update`, `validate`, `none`…) |
| `JWT_SECRET` | *(vide)* | Clé de signature, 32 caractères minimum. Vide = clé aléatoire générée au démarrage (tokens perdus au redémarrage) |
| `JWT_EXPIRATION_MS` | `86400000` | Durée du token (24 h) |

### Frontend (`e-bank-frontend/.env.local`)

| Variable | Défaut | Rôle |
|---|---|---|
| `JWT_SECRET` | `dev-only-secret-change-me` | Vérification du cookie dans `proxy.ts` — même valeur que le backend |
| `NEXT_PUBLIC_API_URL` | `http://localhost:8080/api` | URL de l'API |

### Docker

| Variable | Défaut |
|---|---|
| `POSTGRES_DB` | `ebank` |
| `POSTGRES_USER` | `ebank` |
| `POSTGRES_PASSWORD` | `ebank` |
| `POSTGRES_PORT` | `5432` |

Aucun secret réel n'est versionné : les valeurs par défaut sont réservées au développement local.
En production, `JWT_SECRET`, `DB_PASSWORD` et `DB_URL` viennent de l'environnement.

## 7. Authentification et rôles

1. `POST /api/auth/register` crée un compte **CLIENT** et renvoie un token JWT.
2. `POST /api/auth/login` vérifie le mot de passe (BCrypt) et renvoie le même format.
3. Le token est renvoyé dans le corps **et** posé dans le cookie `session` (`httpOnly`, `SameSite=Lax`).
4. Le frontend n'utilise que le cookie (`credentials: "include"`), jamais `localStorage`.
5. `JwtAuthFilter` lit le cookie (ou l'en-tête `Authorization: Bearer <token>`) et remplit le contexte
   de sécurité. L'API revérifie toujours les rôles : `proxy.ts` n'est qu'un garde d'interface.

| Rôle | Accès |
|---|---|
| `CLIENT` | `/tableau-de-bord`, `/comptes`, `/virements`, `/beneficiaires`, `/reclamations`, `/profil` |
| `AGENT_SUPPORT` | `/tickets`, `/profil` |
| `ADMIN` | tout, dont `/admin/*` |

Le rôle `SUPPORT` fourni à l'inscription est normalisé en `AGENT_SUPPORT`.

## 8. API disponibles

| Méthode | Chemin | Accès | Description |
|---|---|---|---|
| `POST` | `/api/auth/register` | public | `{ email, motDePasse, nom }` → compte CLIENT + token |
| `POST` | `/api/auth/login` | public | `{ email, motDePasse }` → token + cookie `session` |
| `POST` | `/api/auth/logout` | public | Efface le cookie |
| `POST` | `/api/reclamations` | authentifié | Crée une réclamation |
| `GET` | `/api/reclamations/{id}` | authentifié | Suit une réclamation |

Réponses d'erreur : `401` sans session valide, `403` rôle insuffisant, `400` corps invalide.
Les endpoints comptes / virements / bénéficiaires / admin sont en cours de développement.

## 9. Tests et qualité

```bash
# Backend — base H2 en mémoire, aucun conteneur requis
cd pro
./mvnw test                                # Windows : .\mvnw.cmd test
./mvnw -B spring-boot:run                  # démarrage local
```

```bash
# Frontend
cd e-bank-frontend
npm run lint          # 0 erreur exigée
npx tsc --noEmit      # vérification des types
npm run build         # build de production
```

Avant toute pull request : tests backend verts, `lint` et `build` frontend verts, application démarrée
et endpoints testés à la main.

## 10. Commandes Docker

La conteneurisation complète et les ports sont décrits dans [`docker/README.md`](docker/README.md).

```bash
docker compose up -d --build  # démarre le site, l'API et PostgreSQL
docker compose ps             # état des trois services
docker compose logs -f        # journaux
docker compose stop           # arrête, conserve les données
docker compose down           # supprime les conteneurs, conserve le volume
docker compose down -v        # supprime aussi les données
```

Se connecter à la base :

```bash
docker compose exec db psql -U ebank -d ebank
```

## 11. Dépannage

| Symptôme | Cause probable | Solution |
|---|---|---|
| `Connection refused` vers `localhost:5432` | Base arrêtée | `docker compose up -d`, attendre `healthy` |
| `401` sur toutes les routes protégées | Cookie absent, expiré, ou `JWT_SECRET` différent entre backend et frontend | Même secret des deux côtés, redémarrer les deux |
| Le backend change de clé à chaque démarrage | `JWT_SECRET` vide | Exporter `JWT_SECRET` (32+ caractères) |
| `release version 21 not supported` | Ancien JDK | Installer JDK 21+ et vérifier `java -version` |
| Port `8080` / `3000` déjà utilisé | Ancien processus | Fermer l'ancienne instance ou changer le port |
| `/tableau-de-bord` s'ouvre sans connexion | Raccourci volontaire de `proxy.ts` en mode développement | Le backend reste protégé ; tester avec un autre rôle |
| Docker Desktop ne répond pas | Démon arrêté | Lancer Docker Desktop et attendre la fin du démarrage |

## 12. Conventions d'équipe

- **Langue** : noms de classes, méthodes, variables et routes en **français** (`seConnecter`,
  `reinitialiserMotDePasse`, `/tableau-de-bord`). Seuls `page.tsx`, `layout.tsx` et `proxy.ts` restent en
  anglais (imposé par Next.js).
- **Packages** : racine `com.SecBank.pro`. Le dossier `Conrollers` (orthographe historique) est conservé
  pour ne pas casser les imports — ne pas le renommer sans coordination.
- **Modèles** : toute modification des entités (`Models/`) doit être annoncée à l'équipe (elle impacte le
  diagramme de classes et les autres services).
- **Secrets** : jamais de mot de passe, de clé JWT ou de `.env` dans Git. Uniquement des variables
  d'environnement (`.env.local` est ignoré).
- **Git** : une branche par fonctionnalité (`feature/…`, `<prenom>_Branch`), une pull request relue avant
  merge sur `main`. Pas de commit direct sur `main`.
- **Jira** : une tâche = un périmètre. Ne pas élargir une tâche sans validation du responsable.

## 13. Documentation détaillée

- `pro/README.md` — backend : technologies, lancement, base de données
- `e-bank-frontend/README.md` — frontend : routes, kit UI, décisions à verrouiller
- `screenshots/` — captures des écrans (connexion, inscription, tableaux de bord)
- `diagramme de classe.jpeg` — modèle de données de référence
