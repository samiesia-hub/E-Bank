-- =====================================================================
--  Controle du schema, des index, des contraintes et des migrations
--  (LECTURE SEULE — tache 9)
--
--  Usage :
--    docker compose cp pro/src/main/resources/db/demo/. db:/tmp/ebank-demo
--    docker compose exec -T db psql -U ebank -d ebank \
--        -v ON_ERROR_STOP=1 -f /tmp/ebank-demo/check-schema.sql
--
--  Le script ne modifie rien et ne depend pas des donnees : il peut etre
--  execute sur la base de demonstration comme sur une base creee par
--  Flyway. La colonne "verdict" doit valoir OK partout.
-- =====================================================================

\pset pager off

\echo '=== 1. Migrations (flyway_schema_history)'

SELECT controle, attendu, obtenu,
       CASE WHEN attendu IS NOT DISTINCT FROM obtenu THEN 'OK' ELSE 'ECHEC' END AS verdict
FROM (
    SELECT 'migrations en echec' AS controle, 0::bigint AS attendu,
           (SELECT count(*) FROM flyway_schema_history WHERE success = false) AS obtenu
    UNION ALL
    SELECT 'version de migration V1 presente', 1,
           (SELECT count(*) FROM flyway_schema_history WHERE version = '1' AND success)
    UNION ALL
    SELECT 'version de migration V2 presente', 1,
           (SELECT count(*) FROM flyway_schema_history WHERE version = '2' AND success)
    UNION ALL
    SELECT 'base existante baselinee (et non recreee)', 0,
           (SELECT count(*) FROM flyway_schema_history WHERE type = 'BASELINE' AND version <> '1')
) controles;

\echo '=== 2. Tables attendues (16)'

SELECT controle, attendu, obtenu,
       CASE WHEN attendu IS NOT DISTINCT FROM obtenu THEN 'OK' ELSE 'ECHEC' END AS verdict
FROM (
    SELECT 'tables metier presentes' AS controle,
           count(*)::bigint AS attendu,
           count(*) FILTER (WHERE to_regclass('public.' || nom) IS NOT NULL)::bigint AS obtenu
      FROM unnest(ARRAY['utilisateur', 'administrateur', 'agent_support', 'client',
                        'compte_bancaire', 'beneficiaire', 'dossierkyc', 'documentkyc',
                        'journal_audit', 'transaction', 'virement', 'operation_guichet',
                        'reclamation', 'ticket_support', 'response_reclamation',
                        'releve']) AS nom
) controles;

\echo '=== 3. Cles primaires et sequences'

SELECT controle, attendu, obtenu,
       CASE WHEN attendu IS NOT DISTINCT FROM obtenu THEN 'OK' ELSE 'ECHEC' END AS verdict
FROM (
    SELECT 'tables sans cle primaire' AS controle, 0::bigint AS attendu,
           (SELECT count(*) FROM pg_tables t
             WHERE t.schemaname = 'public'
               AND NOT EXISTS (SELECT 1 FROM pg_constraint c
                                WHERE c.conrelid = (quote_ident(t.tablename))::regclass
                                  AND c.contype = 'p')) AS obtenu
    UNION ALL
    SELECT 'sequences hi/lo manquantes', 0,
           (SELECT count(*) FROM unnest(ARRAY['utilisateur_seq', 'compte_bancaire_seq',
                        'beneficiaire_seq', 'dossierkyc_seq', 'documentkyc_seq',
                        'journal_audit_seq', 'transaction_seq', 'reclamation_seq',
                        'ticket_support_seq', 'response_reclamation_seq',
                        'releve_seq']) AS nom
             WHERE to_regclass('public.' || nom) IS NULL)
) controles;

\echo '=== 4. Unicites metier (email, username, rib, numero de compte, 1-1)'

SELECT controle, attendu, obtenu,
       CASE WHEN attendu IS NOT DISTINCT FROM obtenu THEN 'OK' ELSE 'ECHEC' END AS verdict
FROM (
    SELECT 'unicites attendues manquantes' AS controle, 0::bigint AS attendu,
           (SELECT count(*)
              FROM (VALUES ('utilisateur', 'email'),
                           ('utilisateur', 'username'),
                           ('compte_bancaire', 'rib'),
                           ('compte_bancaire', 'numero_compte'),
                           ('dossierkyc', 'client_id'),
                           ('ticket_support', 'reclamation_id')) AS attendu(table_name, colonne)
             WHERE NOT EXISTS (
                   SELECT 1
                     FROM pg_index i
                     JOIN pg_class t ON t.oid = i.indrelid
                     JOIN pg_attribute a ON a.attrelid = t.oid AND a.attnum = ANY (i.indkey)
                    WHERE t.relname = attendu.table_name
                      AND a.attname = attendu.colonne
                      AND i.indisunique)) AS obtenu
) controles;

\echo '=== 5. Index attendus sur les colonnes de recherche et les cles etrangeres'

SELECT controle, attendu, obtenu,
       CASE WHEN attendu IS NOT DISTINCT FROM obtenu THEN 'OK' ELSE 'ECHEC' END AS verdict
FROM (
    SELECT 'index attendus manquants' AS controle, 0::bigint AS attendu,
           (SELECT count(*) FROM unnest(ARRAY[
                        'idx_transaction_date', 'idx_journal_audit_date',
                        'idx_reclamation_date', 'idx_ticket_support_status',
                        'idx_reclamation_status', 'idx_transaction_compte_date',
                        'idx_compte_bancaire_client', 'idx_beneficiaire_client',
                        'idx_releve_compte', 'idx_dossierkyc_admin',
                        'idx_documentkyc_dossier', 'idx_reclamation_client',
                        'idx_reclamation_agent', 'idx_ticket_support_agent',
                        'idx_response_reclamation_reclamation',
                        'idx_operation_guichet_agent',
                        'idx_journal_audit_utilisateur']) AS nom
             WHERE to_regclass('public.' || nom) IS NULL) AS obtenu
    UNION ALL
    -- Chaque cle etrangere doit avoir un index sur ses colonnes (sinon jointure = seq scan).
    SELECT 'cles etrangeres sans index', 0,
           (SELECT count(*) FROM pg_constraint c
             WHERE c.contype = 'f'
               AND c.connamespace = 'public'::regnamespace
               AND NOT EXISTS (
                   SELECT 1 FROM pg_index i
                    WHERE i.indrelid = c.conrelid
                      AND (i.indkey::smallint[])[0:cardinality(c.conkey)-1] @> c.conkey)) AS obtenu
) controles;

\echo '=== 6. Regles metier (CHECK des enums)'

SELECT controle, attendu, obtenu,
       CASE WHEN attendu IS NOT DISTINCT FROM obtenu THEN 'OK' ELSE 'ECHEC' END AS verdict
FROM (
    SELECT 'contraintes CHECK attendues manquantes' AS controle, 0::bigint AS attendu,
           (SELECT count(*) FROM unnest(ARRAY[
                        'utilisateur_role_check', 'compte_bancaire_statut_compte_check',
                        'compte_bancaire_type_compte_check', 'transaction_sens_check',
                        'transaction_statut_transaction_check', 'dossierkyc_statutkyc_check',
                        'operation_guichet_type_operation_check', 'reclamation_status_check',
                        'ticket_support_priorite_check', 'ticket_support_status_check']) AS nom
             WHERE NOT EXISTS (SELECT 1 FROM pg_constraint c
                                WHERE c.conname = nom
                                  AND c.contype = 'c'
                                  AND c.connamespace = 'public'::regnamespace)) AS obtenu
) controles;
