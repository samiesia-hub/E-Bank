-- =====================================================================
--  Controle de coherence des donnees de demonstration (LECTURE SEULE)
--
--  Usage :
--    docker compose cp pro/src/main/resources/db/demo/. db:/tmp/ebank-demo
--    docker compose exec -T db psql -U ebank -d ebank \
--        -v ON_ERROR_STOP=1 -f /tmp/ebank-demo/check-demo.sql
--
--  Toutes les verifications sont limitees aux donnees de demonstration
--  (domaine @demo.bankati.test et identifiants reserves), le script peut
--  donc etre execute sur une base de developpement contenant d'autres
--  lignes. La colonne "verdict" doit valoir OK partout.
-- =====================================================================

\pset pager off

\echo '=== 1. Volumetrie et integrite (verdict OK attendu partout)'

SELECT controle, attendu, obtenu,
       CASE WHEN attendu IS NOT DISTINCT FROM obtenu THEN 'OK' ELSE 'ECHEC' END AS verdict
FROM (
    SELECT 'utilisateurs de demo' AS controle, 9::bigint AS attendu,
           (SELECT count(*) FROM utilisateur WHERE email LIKE '%@demo.bankati.test') AS obtenu
    UNION ALL
    SELECT 'comptes de demo', 8,
           (SELECT count(*) FROM compte_bancaire WHERE id BETWEEN 2001 AND 2008)
    UNION ALL
    SELECT 'transactions des comptes de demo', 27,
           (SELECT count(*) FROM transaction WHERE compte_id BETWEEN 2001 AND 2008)
    UNION ALL
    SELECT 'reclamations de demo', 5,
           (SELECT count(*) FROM reclamation WHERE id BETWEEN 4001 AND 4005)
    UNION ALL
    SELECT 'tickets de demo', 5,
           (SELECT count(*) FROM ticket_support WHERE id BETWEEN 5001 AND 5005)
    UNION ALL
    -- Compte limite aux comptes de demo : le script doit rester valide sur une base de
    -- developpement qui contient aussi des operations hors demonstration.
    SELECT 'virements de demo', 14,
           (SELECT count(*) FROM virement v
              JOIN transaction t ON t.id = v.transaction_id
             WHERE t.compte_id BETWEEN 2001 AND 2008)
    UNION ALL
    SELECT 'operations au guichet de demo', 13,
           (SELECT count(*) FROM operation_guichet o
              JOIN transaction t ON t.id = o.transaction_id
             WHERE t.compte_id BETWEEN 2001 AND 2008)
    UNION ALL
    -- Aucun compte de demo ne doit avoir un solde negatif.
    SELECT 'comptes de demo a solde negatif', 0,
           (SELECT count(*) FROM compte_bancaire WHERE id BETWEEN 2001 AND 2008 AND solde < 0)
    UNION ALL
    -- Le solde doit etre exactement la somme des mouvements executes.
    SELECT 'comptes dont solde <> historique execute', 0,
           (SELECT count(*) FROM (
                SELECT cb.id
                  FROM compte_bancaire cb
                  LEFT JOIN transaction t ON t.compte_id = cb.id
                 WHERE cb.id BETWEEN 2001 AND 2008
                 GROUP BY cb.id, cb.solde
                HAVING cb.solde <> COALESCE(SUM(
                         CASE WHEN t.statut_transaction = 'EXECUTEE' AND t.sens = 'CREDIT' THEN t.montant
                              WHEN t.statut_transaction = 'EXECUTEE' AND t.sens = 'DEBIT'  THEN -t.montant
                              ELSE 0 END), 0)) ecarts)
    UNION ALL
    SELECT 'transactions sans compte', 0,
           (SELECT count(*) FROM transaction t
              LEFT JOIN compte_bancaire cb ON cb.id = t.compte_id
             WHERE t.compte_id IS NOT NULL AND cb.id IS NULL)
    UNION ALL
    SELECT 'transactions de demo sans compte', 0,
           (SELECT count(*) FROM transaction WHERE compte_id IS NULL)
    UNION ALL
    SELECT 'virements sans transaction', 0,
           (SELECT count(*) FROM virement v
              LEFT JOIN transaction t ON t.id = v.transaction_id
             WHERE t.id IS NULL)
    UNION ALL
    SELECT 'reclamations de demo sans ticket', 0,
           (SELECT count(*) FROM reclamation r
              LEFT JOIN ticket_support ti ON ti.reclamation_id = r.id
             WHERE r.id BETWEEN 4001 AND 4005 AND ti.id IS NULL)
    UNION ALL
    -- Statuts varies pour que la demo support soit credible (au moins 3).
    SELECT 'statuts de tickets distincts en moins (< 3)', 0,
           (SELECT GREATEST(0, 3 - count(DISTINCT status)) FROM ticket_support)
    UNION ALL
    SELECT 'statuts de reclamations distincts en moins (< 3)', 0,
           (SELECT GREATEST(0, 3 - count(DISTINCT status)) FROM reclamation)
    UNION ALL
    SELECT 'e-mails de demo en doublon', 0,
           (SELECT count(*) FROM (
                SELECT email FROM utilisateur
                 WHERE email LIKE '%@demo.bankati.test'
                 GROUP BY email HAVING count(*) > 1) d)
    UNION ALL
    -- RIB synthetiques valides : (numero * 100 + cle) doit etre multiple de 97.
    SELECT 'RIB de demo invalides (cle mod 97)', 0,
           (SELECT count(*) FROM compte_bancaire
             WHERE id BETWEEN 2001 AND 2008
               AND (left(rib, length(rib) - 2)::numeric * 100 + right(rib, 2)::int) % 97 <> 0)
    UNION ALL
    -- Chaque client de demo doit avoir au moins un compte et un historique.
    SELECT 'clients de demo sans compte', 0,
           (SELECT count(*) FROM client cl
              JOIN utilisateur u ON u.id_utilisateur = cl.user_id
             WHERE u.email LIKE 'client.demo%@demo.bankati.test'
               AND NOT EXISTS (SELECT 1 FROM compte_bancaire cb WHERE cb.client_id = cl.user_id))
    UNION ALL
    SELECT 'comptes de demo a historique vide', 0,
           (SELECT count(*) FROM compte_bancaire cb
             WHERE cb.id BETWEEN 2001 AND 2008
               AND NOT EXISTS (SELECT 1 FROM transaction t WHERE t.compte_id = cb.id))
) controles;

\echo ''
\echo '=== 2. Scenario jouable par compte de demonstration'

SELECT u.email,
       cb.id AS compte,
       cb.type_compte,
       cb.statut_compte,
       cb.solde,
       count(t.id) AS nb_transactions,
       max(t.date_transaction)::date AS derniere_transaction,
       CASE
           WHEN cb.statut_compte <> 'ACTIF' THEN 'refus attendu (compte ' || cb.statut_compte || ')'
           WHEN count(t.id) = 0 THEN 'a verifier : historique vide'
           WHEN cb.solde <= 0 THEN 'a verifier : solde nul'
           ELSE 'jouable : consultation + depot/retrait'
       END AS scenario
  FROM client cl
  JOIN utilisateur u ON u.id_utilisateur = cl.user_id
  JOIN compte_bancaire cb ON cb.client_id = cl.user_id
  LEFT JOIN transaction t ON t.compte_id = cb.id
 WHERE u.email LIKE 'client.demo%@demo.bankati.test'
 GROUP BY u.email, cb.id, cb.type_compte, cb.statut_compte, cb.solde
 ORDER BY u.email, cb.id;

\echo ''
\echo '=== 3. Repartition des statuts (support)'

SELECT 'ticket' AS objet, status, count(*) AS nb
  FROM ticket_support GROUP BY status
UNION ALL
SELECT 'reclamation', status, count(*)
  FROM reclamation GROUP BY status
ORDER BY objet, status;
