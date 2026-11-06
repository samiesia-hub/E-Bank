-- =====================================================================
-- E-Bank - jeu de donnees de demonstration (PostgreSQL)
--
-- Objectif : rendre la demo jouable sans aucune creation manuelle.
--   * 2 administrateurs, 2 agents support, 5 clients
--   * 8 comptes (courant / epargne / professionnel) avec des soldes realistes
--   * 27 transactions (virements + operations au guichet) dont la somme
--     correspond exactement au solde de chaque compte
--   * 5 reclamations et 5 tickets support avec des statuts varies
--
-- Idempotent : les lignes de demonstration sont identifiees par le domaine
-- d'e-mail reserve @demo.bankati.test (RFC 2606) et supprimees avant
-- reinsertion. Deux executions successives donnent donc le meme etat.
--
-- Securite : donnees strictement synthetiques. Les mots de passe sont des
-- hashes BCrypt de mots de passe de test documentes dans pro/README.md et
-- ne doivent jamais etre reutilises hors de la demonstration.
--
-- Usage : voir la section "Donnees de demonstration" de pro/README.md
-- =====================================================================

SET client_min_messages = warning;

BEGIN;

-- ---------------------------------------------------------------------
-- 1. Suppression idempotente des donnees de demonstration existantes
-- ---------------------------------------------------------------------
DELETE FROM documentkyc
 WHERE dossier_id IN (SELECT d.id
                        FROM dossierkyc d
                        JOIN client c ON c.user_id = d.client_id
                        JOIN utilisateur u ON u.id_utilisateur = c.user_id
                       WHERE u.email LIKE '%@demo.bankati.test');

DELETE FROM dossierkyc
 WHERE client_id IN (SELECT c.user_id
                       FROM client c
                       JOIN utilisateur u ON u.id_utilisateur = c.user_id
                      WHERE u.email LIKE '%@demo.bankati.test');

DELETE FROM releve
 WHERE compte_id IN (SELECT cb.id
                       FROM compte_bancaire cb
                       JOIN client c ON c.user_id = cb.client_id
                       JOIN utilisateur u ON u.id_utilisateur = c.user_id
                      WHERE u.email LIKE '%@demo.bankati.test');

DELETE FROM beneficiaire
 WHERE client_id IN (SELECT c.user_id
                       FROM client c
                       JOIN utilisateur u ON u.id_utilisateur = c.user_id
                      WHERE u.email LIKE '%@demo.bankati.test');

DELETE FROM response_reclamation
 WHERE reclamation_id IN (SELECT r.id
                            FROM reclamation r
                            JOIN client c ON c.user_id = r.client_id
                            JOIN utilisateur u ON u.id_utilisateur = c.user_id
                           WHERE u.email LIKE '%@demo.bankati.test');

DELETE FROM ticket_support
 WHERE reclamation_id IN (SELECT r.id
                            FROM reclamation r
                            JOIN client c ON c.user_id = r.client_id
                            JOIN utilisateur u ON u.id_utilisateur = c.user_id
                           WHERE u.email LIKE '%@demo.bankati.test');

DELETE FROM operation_guichet
 WHERE transaction_id IN (SELECT t.id
                            FROM transaction t
                            JOIN compte_bancaire cb ON cb.id = t.compte_id
                            JOIN client c ON c.user_id = cb.client_id
                            JOIN utilisateur u ON u.id_utilisateur = c.user_id
                           WHERE u.email LIKE '%@demo.bankati.test');

DELETE FROM virement
 WHERE transaction_id IN (SELECT t.id
                            FROM transaction t
                            JOIN compte_bancaire cb ON cb.id = t.compte_id
                            JOIN client c ON c.user_id = cb.client_id
                            JOIN utilisateur u ON u.id_utilisateur = c.user_id
                           WHERE u.email LIKE '%@demo.bankati.test');

DELETE FROM transaction
 WHERE id IN (SELECT t.id
                FROM transaction t
                JOIN compte_bancaire cb ON cb.id = t.compte_id
                JOIN client c ON c.user_id = cb.client_id
                JOIN utilisateur u ON u.id_utilisateur = c.user_id
               WHERE u.email LIKE '%@demo.bankati.test');

DELETE FROM reclamation
 WHERE client_id IN (SELECT c.user_id
                       FROM client c
                       JOIN utilisateur u ON u.id_utilisateur = c.user_id
                      WHERE u.email LIKE '%@demo.bankati.test');

DELETE FROM compte_bancaire
 WHERE client_id IN (SELECT c.user_id
                       FROM client c
                       JOIN utilisateur u ON u.id_utilisateur = c.user_id
                      WHERE u.email LIKE '%@demo.bankati.test');

-- Detache les lignes reelles eventuellement assignees a un agent de demo
-- avant de supprimer ces agents (sinon la suppression serait bloquee).
UPDATE reclamation SET agent_id = NULL
 WHERE agent_id IN (SELECT id_utilisateur FROM utilisateur WHERE email LIKE '%@demo.bankati.test');
UPDATE ticket_support SET agent_id = NULL
 WHERE agent_id IN (SELECT id_utilisateur FROM utilisateur WHERE email LIKE '%@demo.bankati.test');
UPDATE operation_guichet SET agent_id = NULL
 WHERE agent_id IN (SELECT id_utilisateur FROM utilisateur WHERE email LIKE '%@demo.bankati.test');
UPDATE dossierkyc SET admin_id = NULL
 WHERE admin_id IN (SELECT id_utilisateur FROM utilisateur WHERE email LIKE '%@demo.bankati.test');

DELETE FROM client
 WHERE user_id IN (SELECT id_utilisateur FROM utilisateur WHERE email LIKE '%@demo.bankati.test');

DELETE FROM agent_support
 WHERE user_id IN (SELECT id_utilisateur FROM utilisateur WHERE email LIKE '%@demo.bankati.test');

DELETE FROM administrateur
 WHERE user_id IN (SELECT id_utilisateur FROM utilisateur WHERE email LIKE '%@demo.bankati.test');

DELETE FROM utilisateur WHERE email LIKE '%@demo.bankati.test';

-- ---------------------------------------------------------------------
-- 2. Utilisateurs
--    Mots de passe : Admin1234! / Agent1234! / Client1234!
-- ---------------------------------------------------------------------
INSERT INTO utilisateur (id_utilisateur, cin, actif, doubleauth_active, email, nom, password, role, telephone, username) VALUES
    (1001, 'DEMO0001', TRUE, FALSE, 'admin.demo1@demo.bankati.test',  'Administrateur Demo 1', '$2a$10$bFcFudMg3TgWXK8a4qFf6.4Kk.EXK1b9IJGU4peu26T4YUGQYvNtq', 'ADMIN',   612000101, 'admin.demo1@demo.bankati.test'),
    (1002, 'DEMO0002', TRUE, FALSE, 'admin.demo2@demo.bankati.test',  'Administrateur Demo 2', '$2a$10$bFcFudMg3TgWXK8a4qFf6.4Kk.EXK1b9IJGU4peu26T4YUGQYvNtq', 'ADMIN',   612000102, 'admin.demo2@demo.bankati.test'),
    (1003, 'DEMO0003', TRUE, FALSE, 'agent.demo1@demo.bankati.test',  'Agent Support Demo 1',  '$2a$10$1za7ZYyZuEI8XYnMXRRM5.SC4D5AAfWnTPlBXEF8v5eLpxsOTA22O', 'SUPPORT', 612000103, 'agent.demo1@demo.bankati.test'),
    (1004, 'DEMO0004', TRUE, FALSE, 'agent.demo2@demo.bankati.test',  'Agent Support Demo 2',  '$2a$10$1za7ZYyZuEI8XYnMXRRM5.SC4D5AAfWnTPlBXEF8v5eLpxsOTA22O', 'SUPPORT', 612000104, 'agent.demo2@demo.bankati.test'),
    (1005, 'DEMO0005', TRUE, FALSE, 'client.demo1@demo.bankati.test', 'Client Demo 1',         '$2a$10$yVcIcqr1RjDuF0b0jxY9DeEUCoVakRYoFLT56M8SCVV0fifuJI6rC', 'CLIENT',  612000105, 'client.demo1@demo.bankati.test'),
    (1006, 'DEMO0006', TRUE, FALSE, 'client.demo2@demo.bankati.test', 'Client Demo 2',         '$2a$10$yVcIcqr1RjDuF0b0jxY9DeEUCoVakRYoFLT56M8SCVV0fifuJI6rC', 'CLIENT',  612000106, 'client.demo2@demo.bankati.test'),
    (1007, 'DEMO0007', TRUE, FALSE, 'client.demo3@demo.bankati.test', 'Client Demo 3',         '$2a$10$yVcIcqr1RjDuF0b0jxY9DeEUCoVakRYoFLT56M8SCVV0fifuJI6rC', 'CLIENT',  612000107, 'client.demo3@demo.bankati.test'),
    (1008, 'DEMO0008', TRUE, FALSE, 'client.demo4@demo.bankati.test', 'Client Demo 4',         '$2a$10$yVcIcqr1RjDuF0b0jxY9DeEUCoVakRYoFLT56M8SCVV0fifuJI6rC', 'CLIENT',  612000108, 'client.demo4@demo.bankati.test'),
    (1009, 'DEMO0009', TRUE, FALSE, 'client.demo5@demo.bankati.test', 'Client Demo 5',         '$2a$10$yVcIcqr1RjDuF0b0jxY9DeEUCoVakRYoFLT56M8SCVV0fifuJI6rC', 'CLIENT',  612000109, 'client.demo5@demo.bankati.test');

INSERT INTO administrateur (matricule, niveux_acce, user_id) VALUES
    ('ADM-DEMO-01', 1, 1001),
    ('ADM-DEMO-02', 2, 1002);

INSERT INTO agent_support (matricule, specialite, user_id) VALUES
    ('SUP-DEMO-01', 'Support client',    1003),
    ('SUP-DEMO-02', 'Support technique', 1004);

INSERT INTO client (address, date_naissance, numero_client, user_id) VALUES
    ('12 rue Synthetique, Casablanca', TIMESTAMP '1990-01-15 00:00:00', 'CLI-DEMO-0001', 1005),
    ('48 avenue Exemple, Rabat',       TIMESTAMP '1985-06-02 00:00:00', 'CLI-DEMO-0002', 1006),
    ('7 boulevard Test, Marrakech',    TIMESTAMP '1993-11-23 00:00:00', 'CLI-DEMO-0003', 1007),
    ('3 impasse Demo, Fes',            TIMESTAMP '1979-03-08 00:00:00', 'CLI-DEMO-0004', 1008),
    ('25 place Fictive, Tanger',       TIMESTAMP '1998-09-30 00:00:00', 'CLI-DEMO-0005', 1009);

-- ---------------------------------------------------------------------
-- 3. Comptes (NUMERO + RIB synthetiques, cle RIB calculee modulo 97)
-- ---------------------------------------------------------------------
INSERT INTO compte_bancaire (id, date_creation, devise, numero_compte, rib, solde, statut_compte, type_compte, client_id) VALUES
    (2001, TIMESTAMP '2026-09-01 09:00:00', 'MAD', '1000000000001', '011780001100000000000149', 15000.00, 'ACTIF',  'COURANT',       1005),
    (2002, TIMESTAMP '2026-08-01 09:00:00', 'MAD', '1000000000002', '011780001100000000000246', 40000.00, 'ACTIF',  'EPARGNE',       1005),
    (2003, TIMESTAMP '2026-09-02 09:30:00', 'MAD', '1000000000003', '011780001100000000000343',  8349.50, 'ACTIF',  'COURANT',       1006),
    (2004, TIMESTAMP '2026-08-15 10:00:00', 'MAD', '1000000000004', '011780001100000000000440', 55249.25, 'ACTIF',  'PROFESSIONNEL', 1006),
    (2005, TIMESTAMP '2026-09-03 09:00:00', 'MAD', '1000000000005', '011780001100000000000537',  5599.75, 'ACTIF',  'COURANT',       1007),
    (2006, TIMESTAMP '2026-08-20 09:00:00', 'MAD', '1000000000006', '011780001100000000000634', 17500.00, 'ACTIF',  'EPARGNE',       1007),
    (2007, TIMESTAMP '2026-09-08 09:00:00', 'MAD', '1000000000007', '011780001100000000000731',  2049.70, 'BLOQUE', 'COURANT',       1008),
    (2008, TIMESTAMP '2026-09-11 09:00:00', 'MAD', '1000000000008', '011780001100000000000828',  6750.00, 'ACTIF',  'COURANT',       1009);

-- ---------------------------------------------------------------------
-- 4. Historique des transactions
--    solde de chaque compte = somme(credits) - somme(debits)
-- ---------------------------------------------------------------------
INSERT INTO transaction (id, date_transaction, montant, sens, statut_transaction, compte_id) VALUES
    -- Compte 2001 (client 1, courant) : 12 000 - 2 500 + 3 000 - 1 200 + 4 500 - 800 = 15 000
    (3001, TIMESTAMP '2026-09-01 09:10:00', 12000.00, 'CREDIT', 'EXECUTEE', 2001),
    (3002, TIMESTAMP '2026-09-05 14:30:00',  2500.00, 'DEBIT',  'EXECUTEE', 2001),
    (3003, TIMESTAMP '2026-09-12 10:15:00',  3000.00, 'CREDIT', 'EXECUTEE', 2001),
    (3004, TIMESTAMP '2026-10-01 08:45:00',  1200.00, 'DEBIT',  'EXECUTEE', 2001),
    (3005, TIMESTAMP '2026-11-01 09:10:00',  4500.00, 'CREDIT', 'EXECUTEE', 2001),
    (3006, TIMESTAMP '2026-10-28 16:20:00',   800.00, 'DEBIT',  'EXECUTEE', 2001),
    -- Compte 2002 (client 1, epargne) : 30 000 + 5 000 + 5 000 = 40 000
    (3007, TIMESTAMP '2026-08-01 09:10:00', 30000.00, 'CREDIT', 'EXECUTEE', 2002),
    (3008, TIMESTAMP '2026-09-01 09:00:00',  5000.00, 'CREDIT', 'EXECUTEE', 2002),
    (3009, TIMESTAMP '2026-10-01 09:00:00',  5000.00, 'CREDIT', 'EXECUTEE', 2002),
    -- Compte 2003 (client 2, courant) : 8 000 - 1 350,50 + 2 200 - 500 = 8 349,50
    (3010, TIMESTAMP '2026-09-02 09:40:00',  8000.00, 'CREDIT', 'EXECUTEE', 2003),
    (3011, TIMESTAMP '2026-09-18 11:00:00',  1350.50, 'DEBIT',  'EXECUTEE', 2003),
    (3012, TIMESTAMP '2026-10-03 12:40:00',  2200.00, 'CREDIT', 'EXECUTEE', 2003),
    (3013, TIMESTAMP '2026-10-20 17:05:00',   500.00, 'DEBIT',  'EXECUTEE', 2003),
    -- Compte 2004 (client 2, professionnel) : 50 000 - 12 750,75 + 18 000 = 55 249,25
    (3014, TIMESTAMP '2026-08-15 10:10:00', 50000.00, 'CREDIT', 'EXECUTEE', 2004),
    (3015, TIMESTAMP '2026-09-10 15:20:00', 12750.75, 'DEBIT',  'EXECUTEE', 2004),
    (3016, TIMESTAMP '2026-10-05 09:00:00', 18000.00, 'CREDIT', 'EXECUTEE', 2004),
    -- Compte 2005 (client 3, courant) : 5 000 + 1 500 - 900,25 = 5 599,75
    (3017, TIMESTAMP '2026-09-03 09:10:00',  5000.00, 'CREDIT', 'EXECUTEE', 2005),
    (3018, TIMESTAMP '2026-10-02 10:30:00',  1500.00, 'CREDIT', 'EXECUTEE', 2005),
    (3019, TIMESTAMP '2026-10-22 13:15:00',   900.25, 'DEBIT',  'EXECUTEE', 2005),
    -- Compte 2006 (client 3, epargne) : 15 000 + 2 500 = 17 500
    (3020, TIMESTAMP '2026-08-20 09:10:00', 15000.00, 'CREDIT', 'EXECUTEE', 2006),
    (3021, TIMESTAMP '2026-10-01 09:00:00',  2500.00, 'CREDIT', 'EXECUTEE', 2006),
    -- Compte 2007 (client 4, courant bloque) : 3 200 - 700 - 450,30 = 2 049,70
    (3022, TIMESTAMP '2026-09-08 09:10:00',  3200.00, 'CREDIT', 'EXECUTEE', 2007),
    (3023, TIMESTAMP '2026-10-06 18:00:00',   700.00, 'DEBIT',  'EXECUTEE', 2007),
    (3024, TIMESTAMP '2026-11-04 11:45:00',   450.30, 'DEBIT',  'EXECUTEE', 2007),
    -- Compte 2008 (client 5, courant) : 6 000 + 1 100 - 350 = 6 750
    (3025, TIMESTAMP '2026-09-11 09:10:00',  6000.00, 'CREDIT', 'EXECUTEE', 2008),
    (3026, TIMESTAMP '2026-11-03 14:00:00',  1100.00, 'CREDIT', 'EXECUTEE', 2008),
    (3027, TIMESTAMP '2026-11-05 10:20:00',   350.00, 'DEBIT',  'EXECUTEE', 2008);

-- Virements (transactions sortantes ou entrants par virement)
INSERT INTO virement (transaction_id, beneficiaire, motif) VALUES
    (3002, 'Atlas Energie SARL',          'Facture electricite septembre'),
    (3004, 'Agence Immobiliere Anfa',     'Loyer octobre'),
    (3005, 'Synt Bank SARL',              'Salaire octobre'),
    (3008, 'Epargne automatique',         'Virement epargne mensuel'),
    (3009, 'Epargne automatique',         'Virement epargne mensuel'),
    (3011, 'Maroc Telecom',               'Facture internet'),
    (3012, 'Atelier Nord SARL',           'Reglement facture 2026-118'),
    (3015, 'Fournisseur Textile SARL',    'Achat matieres premieres'),
    (3016, 'Groupe Retail Maghreb',       'Facture 2026-045'),
    (3019, 'Lydec',                       'Facture eau'),
    (3021, 'Epargne automatique',         'Virement epargne mensuel'),
    (3024, 'Inwi',                        'Abonnement mobile'),
    (3026, 'Freelance Digital SARL',      'Prestation design'),
    (3027, 'Cafe Central',                'Note de frais');

-- Operations au guichet (depot / retrait effectues par un agent)
INSERT INTO operation_guichet (transaction_id, type_operation, motif, agent_id) VALUES
    (3001, 'DEPOT',   'Depot especes - ouverture de compte', 1003),
    (3003, 'DEPOT',   'Depot especes',                       1003),
    (3006, 'RETRAIT', 'Retrait especes au guichet',          1004),
    (3007, 'DEPOT',   'Epargne initiale',                    1003),
    (3010, 'DEPOT',   'Depot especes - ouverture de compte', 1004),
    (3013, 'RETRAIT', 'Retrait especes au guichet',          1003),
    (3014, 'DEPOT',   'Apport initial activite',             1003),
    (3017, 'DEPOT',   'Depot especes - ouverture de compte', 1004),
    (3018, 'DEPOT',   'Depot especes',                       1004),
    (3020, 'DEPOT',   'Epargne initiale',                    1003),
    (3022, 'DEPOT',   'Depot especes - ouverture de compte', 1004),
    (3023, 'RETRAIT', 'Retrait especes au guichet',          1004),
    (3025, 'DEPOT',   'Depot especes - ouverture de compte', 1003);

-- ---------------------------------------------------------------------
-- 5. Reclamations et tickets support (statuts varies pour la demo)
-- ---------------------------------------------------------------------
INSERT INTO reclamation (id, date_creation, desciprtion, objet, status, agent_id, client_id) VALUES
    (4001, TIMESTAMP '2026-10-12 10:00:00', 'Des frais de tenue de compte apparaissent sur mon releve alors qu ils avaient ete supprimes.', 'Frais bancaires inattendus',      'EN_COURS', 1003, 1005),
    (4002, TIMESTAMP '2026-10-26 09:15:00', 'Le virement de 2 200 MAD envoye le 03/10 n apparait toujours pas sur le compte du beneficiaire.', 'Virement non recu',              'OUVERTE',  NULL, 1006),
    (4003, TIMESTAMP '2026-09-29 15:40:00', 'Ma carte a ete bloquee apres trois codes errones. Je souhaite la debloquer.',                     'Carte bloquee',                  'RESOLUE',  1004, 1007),
    (4004, TIMESTAMP '2026-09-15 11:20:00', 'Le releve du mois de septembre ne se telecharge pas depuis le portail client.',                     'Releve indisponible',            'FERMEE',   1004, 1008),
    (4005, TIMESTAMP '2026-10-18 16:05:00', 'Je souhaite mettre a jour mon numero de telephone pour recevoir les codes de validation.',          'Mise a jour du numero de mobile', 'EN_COURS', 1003, 1009);

INSERT INTO ticket_support (id, date_fermetur, date_ouverture, priorite, status, agent_id, reclamation_id) VALUES
    (5001, NULL,                        TIMESTAMP '2026-10-12 10:05:00', 'HAUTE',    'EN_TRAITEMENT',     1003, 4001),
    (5002, NULL,                        TIMESTAMP '2026-10-26 09:20:00', 'MOYENNE',  'OUVERT',            NULL, 4002),
    (5003, TIMESTAMP '2026-09-30 09:00:00', TIMESTAMP '2026-09-29 15:50:00', 'BASSE',  'FERME',             1004, 4003),
    (5004, TIMESTAMP '2026-09-16 10:15:00', TIMESTAMP '2026-09-15 11:30:00', 'BASSE',  'FERME',             1004, 4004),
    (5005, NULL,                        TIMESTAMP '2026-10-18 16:15:00', 'MOYENNE',  'EN_ATTENTE_CLIENT', 1003, 4005);

-- ---------------------------------------------------------------------
-- 6. Sequences : les prochains identifiants generes par Hibernate restent
--    au-dessus des identifiants de demonstration (1001 / 2001 / 3001 ...).
-- ---------------------------------------------------------------------
SELECT setval('utilisateur_seq', (SELECT COALESCE(MAX(id_utilisateur), 0) + 1 FROM utilisateur));
SELECT setval('compte_bancaire_seq', (SELECT COALESCE(MAX(id), 0) + 1 FROM compte_bancaire));
SELECT setval('transaction_seq', (SELECT COALESCE(MAX(id), 0) + 1 FROM transaction));
SELECT setval('reclamation_seq', (SELECT COALESCE(MAX(id), 0) + 1 FROM reclamation));
SELECT setval('ticket_support_seq', (SELECT COALESCE(MAX(id), 0) + 1 FROM ticket_support));
SELECT setval('beneficiaire_seq', (SELECT COALESCE(MAX(id), 0) + 1 FROM beneficiaire));
SELECT setval('releve_seq', (SELECT COALESCE(MAX(id), 0) + 1 FROM releve));
SELECT setval('dossierkyc_seq', (SELECT COALESCE(MAX(id), 0) + 1 FROM dossierkyc));
SELECT setval('documentkyc_seq', (SELECT COALESCE(MAX(id), 0) + 1 FROM documentkyc));
SELECT setval('response_reclamation_seq', (SELECT COALESCE(MAX(id), 0) + 1 FROM response_reclamation));
SELECT setval('journal_audit_seq', (SELECT COALESCE(MAX(id), 0) + 1 FROM journal_audit));

COMMIT;
