-- =====================================================================
-- E-Bank - reinitialisation complete de la base de developpement
--
-- Efface TOUTES les donnees (pas seulement celles de demonstration),
-- remet les sequences a zero puis recharge le jeu de demonstration.
-- A n utiliser que sur une base de developpement locale.
--
-- Usage (depuis la racine du depot) :
--   docker compose cp pro/src/main/resources/db/demo/. db:/tmp/ebank-demo
--   docker compose exec -T db psql -U ebank -d ebank -v ON_ERROR_STOP=1 \
--       -f /tmp/ebank-demo/reset-demo.sql
-- =====================================================================

SET client_min_messages = warning;

TRUNCATE TABLE
    journal_audit,
    operation_guichet,
    virement,
    transaction,
    ticket_support,
    response_reclamation,
    reclamation,
    beneficiaire,
    releve,
    documentkyc,
    dossierkyc,
    compte_bancaire,
    client,
    agent_support,
    administrateur,
    utilisateur
RESTART IDENTITY CASCADE;

-- \ir resout le chemin par rapport a ce fichier : le seed est toujours
-- execute depuis le meme dossier, quel que soit le repertoire courant.
\ir seed-demo.sql
