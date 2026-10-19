# Bankati — Frontend E-Bank (Next.js)

Frontend de la banque en ligne **Bankati** : vitrine publique + espaces client / support / admin.
Stack : **Next.js 16 (App Router) + TypeScript + Tailwind v4 + TanStack Query + Zustand + React Hook Form + Zod**.

> Routes et fichiers en **français** (`/connexion`, `/tableau-de-bord`, `/virements/nouveau`, ...).
> Seuls `page.tsx`, `layout.tsx` et `proxy.ts` restent en anglais (imposé par Next.js).

## Prérequis

- **Node.js 22** (`node -v`) et npm
- Backend Spring Boot (optionnel pour démarrer — les pages affichent des placeholders sans lui)

## Lancer le frontend

```bash
cd E-Bank/e-bank-frontend
npm install

# 1) Créer le fichier d'environnement local (OBLIGATOIRE — voir section .env)
cp .env.example .env.local

# 2) Démarrer en dev
npm run dev
# -> http://localhost:3000
```

Autres commandes :

```bash
npm run build   # build production (doit passer avant chaque push)
npm run lint    # 0 erreur exigée
npx tsc --noEmit
```

## Règle `.env`

- **Ne jamais committer `.env.local`** (déjà dans `.gitignore`).
- Seul `.env.example` est versionné et sert de modèle.
- Chaque dev / machine crée son propre `.env.local` via `cp .env.example .env.local`.

| Variable | Usage | Exemple |
|---|---|---|
| `NEXT_PUBLIC_API_URL` | URL du backend (lue côté client par `src/lib/client-api.ts`) | `http://localhost:8080/api` |
| `JWT_SECRET` | Secret de vérification JWT pour `src/proxy.ts` (dev uniquement) | chaîne aléatoire de 32+ caractères |

> En prod : `JWT_SECRET` long et aléatoire, jamais la valeur `dev-only...`.
> Auth attendue : **cookie httpOnly `session`** (JWT avec `{ role: CLIENT | AGENT_SUPPORT | ADMIN }`).

## Routes principales

| URL | Espace |
|---|---|
| `/`, `/services`, `/securite`, `/faq` | Vitrine publique |
| `/connexion`, `/inscription`, `/verification-otp`, `/mot-de-passe-oublie` | Auth |
| `/tableau-de-bord`, `/comptes`, `/virements/nouveau`, `/beneficiaires`, `/reclamations`, `/profil` | Client |
| `/tickets` | Support (AGENT_SUPPORT, ADMIN) |
| `/admin/utilisateurs`, `/admin/comptes-bancaires`, `/admin/transactions`, `/admin/kyc`, `/admin/audit`, `/admin/roles-permissions`, `/admin/employes-support` | Admin (ADMIN) |

## Structure

```
src/
  app/                          # routes (groupes : (public) (auth) (client) (support) + admin/)
  components/interface/           # kit UI : Bouton, ChampSaisie, Badge, Carte, TableauDonnees (DESIGN.md)
  features/                       # authentification, comptes, virements, beneficiaires, reclamations, administration
  lib/                            # client-api.ts (fetch + cookie), formatage.ts (Intl fr-FR, IBAN)
  types/banque.ts                 # miroir du diagramme de classes (montant=string, jamais float)
  proxy.ts                        # garde de routes par rôle (Next 16 — UI only, backend re-vérifie)
```

## Décisions à verrouiller avec l'équipe

1. **Couleur CTA** : `#376FB2` (utilisée) vs `#155698` (liste de tokens) — choisir une seule.
2. **Transport auth** : cookie httpOnly `session` (actuel) — si Bearer, modifier `proxy.ts` + `client-api.ts`.
