-- V2 : index de recherche et de jointure, plus l'unicité de l'email.
--
-- 1. Unicité métier : l'email identifie un compte (AuthService refuse un doublon) mais
--    la base ne le garantissait pas. La contrainte protège la donnée et sert aussi
--    d'index pour findByEmail (chaque login, chaque requête authentifiée).
-- 2. Colonnes de recherche : dates (supervision, export, historique) et statuts.
-- 3. Clés étrangères : PostgreSQL n'indexe pas automatiquement le côté enfant, ce qui
--    provoque des parcours séquentiels sur tous les filtres "mes comptes / mes
--    transactions / tickets d'une réclamation".
-- Idempotence : IF NOT EXISTS partout, la migration peut être rejouée sans erreur.

ALTER TABLE utilisateur
    ADD CONSTRAINT utilisateur_email_key UNIQUE (email);

-- Recherche / tri sur les dates
CREATE INDEX IF NOT EXISTS idx_transaction_date ON transaction (date_transaction DESC);
CREATE INDEX IF NOT EXISTS idx_journal_audit_date ON journal_audit (date_action DESC);
CREATE INDEX IF NOT EXISTS idx_reclamation_date ON reclamation (date_creation DESC);

-- Filtres par statut (listes admin / support)
CREATE INDEX IF NOT EXISTS idx_ticket_support_status ON ticket_support (status);
CREATE INDEX IF NOT EXISTS idx_reclamation_status ON reclamation (status);

-- Jointures et filtres par compte / client
CREATE INDEX IF NOT EXISTS idx_transaction_compte_date ON transaction (compte_id, date_transaction DESC);
CREATE INDEX IF NOT EXISTS idx_compte_bancaire_client ON compte_bancaire (client_id);
CREATE INDEX IF NOT EXISTS idx_beneficiaire_client ON beneficiaire (client_id);
CREATE INDEX IF NOT EXISTS idx_releve_compte ON releve (compte_id);
CREATE INDEX IF NOT EXISTS idx_dossierkyc_admin ON dossierkyc (admin_id);
CREATE INDEX IF NOT EXISTS idx_documentkyc_dossier ON documentkyc (dossier_id);

-- Relations support / audit
CREATE INDEX IF NOT EXISTS idx_reclamation_client ON reclamation (client_id);
CREATE INDEX IF NOT EXISTS idx_reclamation_agent ON reclamation (agent_id);
CREATE INDEX IF NOT EXISTS idx_ticket_support_agent ON ticket_support (agent_id);
CREATE INDEX IF NOT EXISTS idx_response_reclamation_reclamation ON response_reclamation (reclamation_id);
CREATE INDEX IF NOT EXISTS idx_operation_guichet_agent ON operation_guichet (agent_id);
CREATE INDEX IF NOT EXISTS idx_journal_audit_utilisateur ON journal_audit (utilisateur_id);
