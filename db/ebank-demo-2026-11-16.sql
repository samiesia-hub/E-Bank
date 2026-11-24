--
-- PostgreSQL database dump
--


-- Dumped from database version 16.15
-- Dumped by pg_dump version 16.15

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

ALTER TABLE IF EXISTS ONLY public.virement DROP CONSTRAINT IF EXISTS fk_virement_transaction;
ALTER TABLE IF EXISTS ONLY public.virement DROP CONSTRAINT IF EXISTS fk_virement_compte_destinataire;
ALTER TABLE IF EXISTS ONLY public.transaction DROP CONSTRAINT IF EXISTS fk_transaction_compte;
ALTER TABLE IF EXISTS ONLY public.ticket_support DROP CONSTRAINT IF EXISTS fk_ticket_support_reclamation;
ALTER TABLE IF EXISTS ONLY public.ticket_support DROP CONSTRAINT IF EXISTS fk_ticket_support_agent;
ALTER TABLE IF EXISTS ONLY public.response_reclamation DROP CONSTRAINT IF EXISTS fk_response_reclamation_reclamation;
ALTER TABLE IF EXISTS ONLY public.releve DROP CONSTRAINT IF EXISTS fk_releve_compte;
ALTER TABLE IF EXISTS ONLY public.refresh_tokens DROP CONSTRAINT IF EXISTS fk_refresh_tokens_utilisateur;
ALTER TABLE IF EXISTS ONLY public.reclamation DROP CONSTRAINT IF EXISTS fk_reclamation_client;
ALTER TABLE IF EXISTS ONLY public.reclamation DROP CONSTRAINT IF EXISTS fk_reclamation_agent;
ALTER TABLE IF EXISTS ONLY public.operation_guichet DROP CONSTRAINT IF EXISTS fk_operation_guichet_transaction;
ALTER TABLE IF EXISTS ONLY public.operation_guichet DROP CONSTRAINT IF EXISTS fk_operation_guichet_agent;
ALTER TABLE IF EXISTS ONLY public.journal_audit DROP CONSTRAINT IF EXISTS fk_journal_audit_utilisateur;
ALTER TABLE IF EXISTS ONLY public.dossierkyc DROP CONSTRAINT IF EXISTS fk_dossierkyc_client;
ALTER TABLE IF EXISTS ONLY public.dossierkyc DROP CONSTRAINT IF EXISTS fk_dossierkyc_administrateur;
ALTER TABLE IF EXISTS ONLY public.documentkyc DROP CONSTRAINT IF EXISTS fk_documentkyc_dossier;
ALTER TABLE IF EXISTS ONLY public.compte_bancaire DROP CONSTRAINT IF EXISTS fk_compte_bancaire_client;
ALTER TABLE IF EXISTS ONLY public.client DROP CONSTRAINT IF EXISTS fk_client_utilisateur;
ALTER TABLE IF EXISTS ONLY public.beneficiaire DROP CONSTRAINT IF EXISTS fk_beneficiaire_client;
ALTER TABLE IF EXISTS ONLY public.agent_support DROP CONSTRAINT IF EXISTS fk_agent_support_utilisateur;
ALTER TABLE IF EXISTS ONLY public.administrateur DROP CONSTRAINT IF EXISTS fk_administrateur_utilisateur;
DROP INDEX IF EXISTS public.ux_virement_reference;
DROP INDEX IF EXISTS public.idx_transaction_date;
DROP INDEX IF EXISTS public.idx_transaction_compte_date;
DROP INDEX IF EXISTS public.idx_ticket_support_status;
DROP INDEX IF EXISTS public.idx_ticket_support_agent;
DROP INDEX IF EXISTS public.idx_response_reclamation_reclamation;
DROP INDEX IF EXISTS public.idx_releve_compte;
DROP INDEX IF EXISTS public.idx_refresh_tokens_user;
DROP INDEX IF EXISTS public.idx_reclamation_status;
DROP INDEX IF EXISTS public.idx_reclamation_date;
DROP INDEX IF EXISTS public.idx_reclamation_client;
DROP INDEX IF EXISTS public.idx_reclamation_agent;
DROP INDEX IF EXISTS public.idx_operation_guichet_agent;
DROP INDEX IF EXISTS public.idx_journal_audit_utilisateur;
DROP INDEX IF EXISTS public.idx_journal_audit_date;
DROP INDEX IF EXISTS public.idx_dossierkyc_admin;
DROP INDEX IF EXISTS public.idx_documentkyc_dossier;
DROP INDEX IF EXISTS public.idx_compte_bancaire_client;
DROP INDEX IF EXISTS public.idx_beneficiaire_client;
DROP INDEX IF EXISTS public.flyway_schema_history_s_idx;
ALTER TABLE IF EXISTS ONLY public.virement DROP CONSTRAINT IF EXISTS virement_pkey;
ALTER TABLE IF EXISTS ONLY public.utilisateur DROP CONSTRAINT IF EXISTS utilisateur_username_key;
ALTER TABLE IF EXISTS ONLY public.utilisateur DROP CONSTRAINT IF EXISTS utilisateur_pkey;
ALTER TABLE IF EXISTS ONLY public.utilisateur DROP CONSTRAINT IF EXISTS utilisateur_email_key;
ALTER TABLE IF EXISTS ONLY public.transaction DROP CONSTRAINT IF EXISTS transaction_pkey;
ALTER TABLE IF EXISTS ONLY public.ticket_support DROP CONSTRAINT IF EXISTS ticket_support_reclamation_id_key;
ALTER TABLE IF EXISTS ONLY public.ticket_support DROP CONSTRAINT IF EXISTS ticket_support_pkey;
ALTER TABLE IF EXISTS ONLY public.response_reclamation DROP CONSTRAINT IF EXISTS response_reclamation_pkey;
ALTER TABLE IF EXISTS ONLY public.releve DROP CONSTRAINT IF EXISTS releve_pkey;
ALTER TABLE IF EXISTS ONLY public.refresh_tokens DROP CONSTRAINT IF EXISTS refresh_tokens_token_key;
ALTER TABLE IF EXISTS ONLY public.refresh_tokens DROP CONSTRAINT IF EXISTS refresh_tokens_pkey;
ALTER TABLE IF EXISTS ONLY public.reclamation DROP CONSTRAINT IF EXISTS reclamation_pkey;
ALTER TABLE IF EXISTS ONLY public.operation_guichet DROP CONSTRAINT IF EXISTS operation_guichet_pkey;
ALTER TABLE IF EXISTS ONLY public.journal_audit DROP CONSTRAINT IF EXISTS journal_audit_pkey;
ALTER TABLE IF EXISTS ONLY public.flyway_schema_history DROP CONSTRAINT IF EXISTS flyway_schema_history_pk;
ALTER TABLE IF EXISTS ONLY public.dossierkyc DROP CONSTRAINT IF EXISTS dossierkyc_pkey;
ALTER TABLE IF EXISTS ONLY public.dossierkyc DROP CONSTRAINT IF EXISTS dossierkyc_client_id_key;
ALTER TABLE IF EXISTS ONLY public.documentkyc DROP CONSTRAINT IF EXISTS documentkyc_pkey;
ALTER TABLE IF EXISTS ONLY public.compte_bancaire DROP CONSTRAINT IF EXISTS compte_bancaire_rib_key;
ALTER TABLE IF EXISTS ONLY public.compte_bancaire DROP CONSTRAINT IF EXISTS compte_bancaire_pkey;
ALTER TABLE IF EXISTS ONLY public.compte_bancaire DROP CONSTRAINT IF EXISTS compte_bancaire_numero_compte_key;
ALTER TABLE IF EXISTS ONLY public.client DROP CONSTRAINT IF EXISTS client_pkey;
ALTER TABLE IF EXISTS ONLY public.beneficiaire DROP CONSTRAINT IF EXISTS beneficiaire_pkey;
ALTER TABLE IF EXISTS ONLY public.agent_support DROP CONSTRAINT IF EXISTS agent_support_pkey;
ALTER TABLE IF EXISTS ONLY public.administrateur DROP CONSTRAINT IF EXISTS administrateur_pkey;
ALTER TABLE IF EXISTS public.refresh_tokens ALTER COLUMN id DROP DEFAULT;
DROP TABLE IF EXISTS public.virement;
DROP SEQUENCE IF EXISTS public.utilisateur_seq;
DROP TABLE IF EXISTS public.utilisateur;
DROP SEQUENCE IF EXISTS public.transaction_seq;
DROP TABLE IF EXISTS public.transaction;
DROP SEQUENCE IF EXISTS public.ticket_support_seq;
DROP TABLE IF EXISTS public.ticket_support;
DROP SEQUENCE IF EXISTS public.response_reclamation_seq;
DROP TABLE IF EXISTS public.response_reclamation;
DROP SEQUENCE IF EXISTS public.releve_seq;
DROP TABLE IF EXISTS public.releve;
DROP SEQUENCE IF EXISTS public.refresh_tokens_id_seq;
DROP TABLE IF EXISTS public.refresh_tokens;
DROP SEQUENCE IF EXISTS public.reclamation_seq;
DROP TABLE IF EXISTS public.reclamation;
DROP TABLE IF EXISTS public.operation_guichet;
DROP SEQUENCE IF EXISTS public.journal_audit_seq;
DROP TABLE IF EXISTS public.journal_audit;
DROP TABLE IF EXISTS public.flyway_schema_history;
DROP SEQUENCE IF EXISTS public.dossierkyc_seq;
DROP TABLE IF EXISTS public.dossierkyc;
DROP SEQUENCE IF EXISTS public.documentkyc_seq;
DROP TABLE IF EXISTS public.documentkyc;
DROP SEQUENCE IF EXISTS public.compte_bancaire_seq;
DROP TABLE IF EXISTS public.compte_bancaire;
DROP TABLE IF EXISTS public.client;
DROP SEQUENCE IF EXISTS public.beneficiaire_seq;
DROP TABLE IF EXISTS public.beneficiaire;
DROP TABLE IF EXISTS public.agent_support;
DROP TABLE IF EXISTS public.administrateur;
-- *not* dropping schema, since initdb creates it
--
-- Name: public; Type: SCHEMA; Schema: -; Owner: -
--

-- *not* creating schema, since initdb creates it


--
-- Name: SCHEMA public; Type: COMMENT; Schema: -; Owner: -
--

COMMENT ON SCHEMA public IS '';


SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: administrateur; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.administrateur (
    user_id bigint NOT NULL,
    niveux_acce integer NOT NULL,
    matricule character varying(255)
);


--
-- Name: agent_support; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.agent_support (
    user_id bigint NOT NULL,
    matricule character varying(255),
    specialite character varying(255)
);


--
-- Name: beneficiaire; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.beneficiaire (
    id bigint NOT NULL,
    client_id bigint,
    nom character varying(255),
    numero_compte character varying(255),
    prenom character varying(255)
);


--
-- Name: beneficiaire_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.beneficiaire_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: client; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.client (
    user_id bigint NOT NULL,
    date_naissance timestamp(6) without time zone,
    address character varying(255),
    numero_client character varying(255)
);


--
-- Name: compte_bancaire; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.compte_bancaire (
    id bigint NOT NULL,
    solde numeric(19,2),
    client_id bigint,
    date_creation timestamp(6) without time zone,
    numero_compte character varying(13),
    rib character varying(24),
    devise character varying(255),
    statut_compte character varying(255),
    type_compte character varying(255),
    CONSTRAINT compte_bancaire_statut_compte_check CHECK (((statut_compte)::text = ANY ((ARRAY['ACTIF'::character varying, 'BLOQUE'::character varying, 'CLOTURE'::character varying])::text[]))),
    CONSTRAINT compte_bancaire_type_compte_check CHECK (((type_compte)::text = ANY ((ARRAY['COURANT'::character varying, 'EPARGNE'::character varying, 'PROFESSIONNEL'::character varying])::text[])))
);


--
-- Name: compte_bancaire_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.compte_bancaire_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: documentkyc; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.documentkyc (
    id bigint NOT NULL,
    dossier_id bigint,
    type_document character varying(255),
    url character varying(255)
);


--
-- Name: documentkyc_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.documentkyc_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: dossierkyc; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.dossierkyc (
    id bigint NOT NULL,
    admin_id bigint,
    client_id bigint NOT NULL,
    date_soumission timestamp(6) without time zone,
    statutkyc character varying(255),
    CONSTRAINT dossierkyc_statutkyc_check CHECK (((statutkyc)::text = ANY ((ARRAY['EN_ATTENTE'::character varying, 'VALIDE'::character varying, 'REJETE'::character varying])::text[])))
);


--
-- Name: dossierkyc_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.dossierkyc_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: flyway_schema_history; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.flyway_schema_history (
    installed_rank integer NOT NULL,
    version character varying(50),
    description character varying(200) NOT NULL,
    type character varying(20) NOT NULL,
    script character varying(1000) NOT NULL,
    checksum integer,
    installed_by character varying(100) NOT NULL,
    installed_on timestamp without time zone DEFAULT now() NOT NULL,
    execution_time integer NOT NULL,
    success boolean NOT NULL
);


--
-- Name: journal_audit; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.journal_audit (
    id bigint NOT NULL,
    date_action timestamp(6) without time zone,
    utilisateur_id bigint,
    action character varying(255),
    details character varying(255)
);


--
-- Name: journal_audit_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.journal_audit_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: operation_guichet; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.operation_guichet (
    transaction_id bigint NOT NULL,
    agent_id bigint,
    motif character varying(255),
    type_operation character varying(255),
    CONSTRAINT operation_guichet_type_operation_check CHECK (((type_operation)::text = ANY ((ARRAY['DEPOT'::character varying, 'RETRAIT'::character varying])::text[])))
);


--
-- Name: reclamation; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.reclamation (
    id bigint NOT NULL,
    agent_id bigint,
    client_id bigint,
    date_creation timestamp(6) without time zone,
    desciprtion character varying(255),
    objet character varying(255),
    status character varying(255),
    date_miseajour timestamp(6) without time zone,
    CONSTRAINT reclamation_status_check CHECK (((status)::text = ANY ((ARRAY['OUVERTE'::character varying, 'EN_COURS'::character varying, 'RESOLUE'::character varying, 'FERMEE'::character varying])::text[])))
);


--
-- Name: reclamation_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.reclamation_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: refresh_tokens; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.refresh_tokens (
    id bigint NOT NULL,
    token character varying(64) NOT NULL,
    user_id bigint NOT NULL,
    expiry_date timestamp(6) with time zone NOT NULL,
    revoked boolean DEFAULT false NOT NULL
);


--
-- Name: refresh_tokens_id_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.refresh_tokens_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: refresh_tokens_id_seq; Type: SEQUENCE OWNED BY; Schema: public; Owner: -
--

ALTER SEQUENCE public.refresh_tokens_id_seq OWNED BY public.refresh_tokens.id;


--
-- Name: releve; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.releve (
    id bigint NOT NULL,
    compte_id bigint,
    date_generation timestamp(6) without time zone,
    fichier character varying(255),
    periode character varying(255)
);


--
-- Name: releve_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.releve_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: response_reclamation; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.response_reclamation (
    id bigint NOT NULL,
    dat_response timestamp(6) without time zone,
    reclamation_id bigint,
    contenu character varying(255)
);


--
-- Name: response_reclamation_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.response_reclamation_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: ticket_support; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.ticket_support (
    id bigint NOT NULL,
    agent_id bigint,
    date_fermetur timestamp(6) without time zone,
    date_ouverture timestamp(6) without time zone,
    reclamation_id bigint,
    priorite character varying(255),
    status character varying(255),
    CONSTRAINT ticket_support_priorite_check CHECK (((priorite)::text = ANY ((ARRAY['BASSE'::character varying, 'MOYENNE'::character varying, 'HAUTE'::character varying, 'CRITIQUE'::character varying])::text[]))),
    CONSTRAINT ticket_support_status_check CHECK (((status)::text = ANY ((ARRAY['OUVERT'::character varying, 'EN_TRAITEMENT'::character varying, 'EN_ATTENTE_CLIENT'::character varying, 'FERME'::character varying])::text[])))
);


--
-- Name: ticket_support_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.ticket_support_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: transaction; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.transaction (
    id bigint NOT NULL,
    montant numeric(38,2),
    compte_id bigint,
    date_transaction timestamp(6) without time zone,
    sens character varying(255),
    statut_transaction character varying(255),
    CONSTRAINT transaction_sens_check CHECK (((sens)::text = ANY ((ARRAY['DEBIT'::character varying, 'CREDIT'::character varying])::text[]))),
    CONSTRAINT transaction_statut_transaction_check CHECK (((statut_transaction)::text = ANY ((ARRAY['EN_ATTENTE'::character varying, 'EXECUTEE'::character varying, 'REJETEE'::character varying])::text[])))
);


--
-- Name: transaction_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.transaction_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: utilisateur; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.utilisateur (
    id_utilisateur bigint NOT NULL,
    actif boolean NOT NULL,
    doubleauth_active boolean NOT NULL,
    telephone integer NOT NULL,
    cin character varying(255),
    email character varying(255),
    nom character varying(255),
    password character varying(255),
    role character varying(255),
    username character varying(255),
    date_creation timestamp(6) without time zone,
    CONSTRAINT utilisateur_role_check CHECK (((role)::text = ANY ((ARRAY['ADMIN'::character varying, 'CLIENT'::character varying, 'SUPPORT'::character varying])::text[])))
);


--
-- Name: utilisateur_seq; Type: SEQUENCE; Schema: public; Owner: -
--

CREATE SEQUENCE public.utilisateur_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


--
-- Name: virement; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.virement (
    transaction_id bigint NOT NULL,
    beneficiaire character varying(255),
    motif character varying(255),
    compte_destinataire_id bigint,
    reference character varying(40)
);


--
-- Name: refresh_tokens id; Type: DEFAULT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.refresh_tokens ALTER COLUMN id SET DEFAULT nextval('public.refresh_tokens_id_seq'::regclass);


--
-- Data for Name: administrateur; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.administrateur (user_id, niveux_acce, matricule) FROM stdin;
1001	1	ADM-DEMO-01
1002	2	ADM-DEMO-02
\.


--
-- Data for Name: agent_support; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.agent_support (user_id, matricule, specialite) FROM stdin;
1003	SUP-DEMO-01	Support client
1004	SUP-DEMO-02	Support technique
\.


--
-- Data for Name: beneficiaire; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.beneficiaire (id, client_id, nom, numero_compte, prenom) FROM stdin;
\.


--
-- Data for Name: client; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.client (user_id, date_naissance, address, numero_client) FROM stdin;
1	\N	\N	CLI-2B5CEB74
1005	1990-01-15 00:00:00	12 rue Synthetique, Casablanca	CLI-DEMO-0001
1006	1985-06-02 00:00:00	48 avenue Exemple, Rabat	CLI-DEMO-0002
1007	1993-11-23 00:00:00	7 boulevard Test, Marrakech	CLI-DEMO-0003
1008	1979-03-08 00:00:00	3 impasse Demo, Fes	CLI-DEMO-0004
1009	1998-09-30 00:00:00	25 place Fictive, Tanger	CLI-DEMO-0005
1011	\N	\N	CLI-50B4E7D3
1061	\N	\N	CLI-F6002836
1062	\N	60000	CLI-99DFD4FB
\.


--
-- Data for Name: compte_bancaire; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.compte_bancaire (id, solde, client_id, date_creation, numero_compte, rib, devise, statut_compte, type_compte) FROM stdin;
2002	40000.00	1005	2026-08-01 09:00:00	1000000000002	011780001100000000000246	MAD	ACTIF	EPARGNE
2004	55249.25	1006	2026-08-15 10:00:00	1000000000004	011780001100000000000440	MAD	ACTIF	PROFESSIONNEL
2005	5599.75	1007	2026-09-03 09:00:00	1000000000005	011780001100000000000537	MAD	ACTIF	COURANT
2006	17500.00	1007	2026-08-20 09:00:00	1000000000006	011780001100000000000634	MAD	ACTIF	EPARGNE
2007	2049.70	1008	2026-09-08 09:00:00	1000000000007	011780001100000000000731	MAD	BLOQUE	COURANT
2008	6750.00	1009	2026-09-11 09:00:00	1000000000008	011780001100000000000828	MAD	ACTIF	COURANT
2001	14000.00	1005	2026-09-01 09:00:00	1000000000001	011780001100000000000149	MAD	ACTIF	COURANT
2003	9349.50	1006	2026-09-02 09:30:00	1000000000003	011780001100000000000343	MAD	ACTIF	COURANT
\.


--
-- Data for Name: documentkyc; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.documentkyc (id, dossier_id, type_document, url) FROM stdin;
\.


--
-- Data for Name: dossierkyc; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.dossierkyc (id, admin_id, client_id, date_soumission, statutkyc) FROM stdin;
6001	\N	1005	2026-11-11 10:56:59.290715	EN_ATTENTE
6002	\N	1006	2026-11-13 10:56:59.290715	EN_ATTENTE
6003	1001	1007	2026-11-05 10:56:59.290715	VALIDE
\.


--
-- Data for Name: flyway_schema_history; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) FROM stdin;
1	1	schema initial	SQL	V1__schema_initial.sql	1126019629	ebank	2026-11-12 15:20:44.106842	973	t
2	2	index et unicite	SQL	V2__index_et_unicite.sql	-1464955410	ebank	2026-11-12 15:20:45.287126	366	t
3	3	aligne schema sur entites	SQL	V3__aligne_schema_sur_entites.sql	-639539475	ebank	2026-11-12 15:23:27.261249	123	t
4	4	refresh tokens et date creation	SQL	V4__refresh_tokens_et_date_creation.sql	661667172	ebank	2026-11-15 18:36:32.440586	284	t
\.


--
-- Data for Name: journal_audit; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.journal_audit (id, date_action, utilisateur_id, action, details) FROM stdin;
9001	2026-11-11 10:56:59.290715	1001	OUVERTURE_COMPTE	Compte courant 1000000000004 ouvert pour Client Demo 4
9002	2026-11-12 10:56:59.290715	1002	DEPOT_GUICHET	Depot de 2500 MAD sur le compte 1000000000001
9003	2026-11-13 10:56:59.290715	1003	REPONSE_TICKET	Reponse apportee au ticket 5001
9004	2026-11-14 04:56:59.290715	1001	VALIDATION_KYC	Dossier KYC 6003 valide
9005	2026-11-14 06:56:59.290715	1002	EXPORT_TRANSACTIONS	Export CSV des transactions sur 30 jours
9006	2026-11-14 10:26:59.290715	1001	CONNEXION_ADMIN	Connexion a la console d administration
9008	2026-11-15 19:49:56.908118	1001	UTILISATEURS_CONSULTES	role=null, actif=null, page=0, taille=3
9009	2026-11-15 19:50:07.23033	1001	UTILISATEURS_CONSULTES	role=null, actif=null, page=0, taille=2
9010	2026-11-15 19:50:07.730581	1001	UTILISATEUR_CONSULTE	utilisateur#1002
9011	2026-11-15 19:50:08.101355	1001	UTILISATEUR_SUSPENDU	utilisateur#1, email=dasmed@gmail.com, motif=test technique
9012	2026-11-15 19:50:08.28193	1001	UTILISATEUR_ACTIVE	utilisateur#1, email=dasmed@gmail.com, motif=fin de test
9058	2026-11-16 10:08:35.058105	1001	UTILISATEURS_CONSULTES	role=null, actif=null, page=0, taille=20
9059	2026-11-16 10:09:41.989272	1001	EXPORT_CSV_TRANSACTIONS	Export CSV de 7 transaction(s) du 2026-10-18 au 2026-11-16
9060	2026-11-16 10:22:36.213389	1001	UTILISATEURS_CONSULTES	role=null, actif=null, page=0, taille=20
9061	2026-11-16 10:46:45.045211	1001	UTILISATEURS_CONSULTES	role=null, actif=null, page=0, taille=20
\.


--
-- Data for Name: operation_guichet; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.operation_guichet (transaction_id, agent_id, motif, type_operation) FROM stdin;
3001	1003	Depot especes - ouverture de compte	DEPOT
3003	1003	Depot especes	DEPOT
3006	1004	Retrait especes au guichet	RETRAIT
3007	1003	Epargne initiale	DEPOT
3010	1004	Depot especes - ouverture de compte	DEPOT
3013	1003	Retrait especes au guichet	RETRAIT
3014	1003	Apport initial activite	DEPOT
3017	1004	Depot especes - ouverture de compte	DEPOT
3018	1004	Depot especes	DEPOT
3020	1003	Epargne initiale	DEPOT
3022	1004	Depot especes - ouverture de compte	DEPOT
3023	1004	Retrait especes au guichet	RETRAIT
3025	1003	Depot especes - ouverture de compte	DEPOT
\.


--
-- Data for Name: reclamation; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.reclamation (id, agent_id, client_id, date_creation, desciprtion, objet, status, date_miseajour) FROM stdin;
4001	1003	1005	2026-10-12 10:00:00	Des frais de tenue de compte apparaissent sur mon releve alors qu ils avaient ete supprimes.	Frais bancaires inattendus	EN_COURS	\N
4002	\N	1006	2026-10-26 09:15:00	Le virement de 2 200 MAD envoye le 03/10 n apparait toujours pas sur le compte du beneficiaire.	Virement non recu	OUVERTE	\N
4003	1004	1007	2026-09-29 15:40:00	Ma carte a ete bloquee apres trois codes errones. Je souhaite la debloquer.	Carte bloquee	RESOLUE	\N
4004	1004	1008	2026-09-15 11:20:00	Le releve du mois de septembre ne se telecharge pas depuis le portail client.	Releve indisponible	FERMEE	\N
4005	1003	1009	2026-10-18 16:05:00	Je souhaite mettre a jour mon numero de telephone pour recevoir les codes de validation.	Mise a jour du numero de mobile	EN_COURS	\N
4007	\N	1062	2026-11-16 11:07:13.292	iizdcuieuz	hhzfuzeu	EN_COURS	2026-11-16 11:07:13.292
\.


--
-- Data for Name: refresh_tokens; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.refresh_tokens (id, token, user_id, expiry_date, revoked) FROM stdin;
2	GQGG8Y4gRAYsE6dMayuaLkhVIqPj2w9xxEQtiXrsij8	1001	2026-11-22 18:45:26.573413+00	f
1	OyLTYKrBdTyDJ2ZwBQtmHIGmwxWn4JMyyXSrsA_EOD4	1001	2026-11-22 18:45:16.93549+00	t
3	MlJeqgdvBPcZGpen8-JJ6Go8WsHRQfR7kzVMy9-ZdYI	1005	2026-11-22 18:50:08.96357+00	f
4	C5MBKboiV8kVyDPXBVA4CXgta-40ArVFmCIcF_eIMy4	1061	2026-11-22 23:31:10.327818+00	f
5	u9WTDIeOJIT0e9fv7kHRYeuusvUeB4z_FCx-MuGIKXE	1001	2026-11-23 09:06:09.821386+00	f
6	3YaWZdBbZD8xnxSs7gKO-KhXaesd0VH2UWYjevrxw68	1002	2026-11-23 09:06:10.684669+00	f
7	IXq6dr90L9PAZpqMAektT42DWSJZm5NorQ3g8DVkkvY	1003	2026-11-23 09:06:12.294732+00	f
8	cK6udN98czhtpX1XWm0y3SvUJrp0uE4x28-3xONKoUc	1004	2026-11-23 09:06:12.950226+00	f
9	Mt1cwUv8TqUJMIdwbZqH_eQsoRIbTE3Cg3AByrxVy5I	1005	2026-11-23 09:06:13.764132+00	f
10	uY_qGnUpywjNlmGd_0pMVYfEM3Fxxfs6i-F8y_DRwf8	1006	2026-11-23 09:06:14.943737+00	f
11	qeGoCBBdjqDJThekg9oMgcd7NTmr1uGRtT3wOFj_-nM	1001	2026-11-23 09:06:24.16419+00	f
12	9nhxHpKr7m6ULVpyZO1m97eTb2GSBAmhEpDcBTOvdr8	1001	2026-11-23 09:08:02.883099+00	f
13	EXE_4ZBbuwRYpX9CreL5l-8IM3tnVe2kac4GE_tJPec	1003	2026-11-23 09:11:59.320631+00	f
14	vXBIn0t2Vm87cUjmKxLKp4wxcC49OIbFBcjktnlVQCE	1005	2026-11-23 09:12:48.872573+00	f
15	aCPYactu_pb6okD_FnZIrwsVZozOQCtRw0zE8xadYBw	1011	2026-11-23 09:21:09.1883+00	f
16	Vgp48xv1oGelTKdaR3jmJ2gpUbtvJlYJz9qNXpsSN7k	1061	2026-11-23 09:21:32.825117+00	f
17	HJQgHDSkvTj6etcrPM3GepRguFJET8v4M7hy3suTr9A	1001	2026-11-23 09:22:11.428083+00	f
18	O0S7oBhP9EhTtN8uJQx8neuxaXes5FwTIpas7gadJTI	1005	2026-11-23 09:24:24.557842+00	f
19	eGbskCSYfX3_eW8vh5rVftUhTEQQrOtPtllR9e0_5RQ	1006	2026-11-23 09:25:17.014602+00	f
20	RPMG__G0Hokm7VJWkZH1LbHaSAxmPWAarAsZDB0ycrM	1006	2026-11-23 09:25:24.394918+00	f
21	O9uIWNSnOfXjwoYcEpB1Lq6jlgDlFbyk1NuvWDiM2yY	1006	2026-11-23 09:25:25.981888+00	f
22	krwVE_eHqAcy4_7IO_ipdDT_-X0XenbWsAxOwj0iTbw	1006	2026-11-23 09:25:26.564947+00	f
23	z7iOZqXfhOcf_Xdqszc71RGGed4dST1TcmqGwsya9Ys	1001	2026-11-23 09:39:03.832207+00	f
24	YmcWBvIB5v-u9gsKbrPFag1C4jhl7Gu0M1VVfmJCFcI	1005	2026-11-23 09:40:31.388988+00	f
25	nwz7p2WXxYA8-8IoBbXSWKEQghe1pvj3txG9ry3KQj4	1062	2026-11-23 10:02:26.915268+00	f
26	J4mGRo9UapWSDrS1yuZWLms810Xlj2VPtzyOX78MjKw	1001	2026-11-23 10:08:45.158973+00	f
27	oYfia5pZq3ukClV-xjcXEhf_v6K_PfmO1H4pkK11UCo	1005	2026-11-23 10:15:09.512217+00	f
28	Kqoo5BSLH7VpHI4X4OoJ_pUNoOtRQrsZUOnOtgwcHb8	1006	2026-11-23 10:15:26.938499+00	f
29	V3PysPoR7Rnl8zCIwypC7iRwjWg95upgiteNZK3KXBI	1005	2026-11-23 10:24:22.699565+00	f
30	HokA_U_6_cCWyZ1FbxDXd4LO6bpfNGM6Zz-pgY8jnlk	1005	2026-11-23 10:33:26.376341+00	f
31	hf1j9lY5JXlY9aG5kIRnZUa0aumfb58DnHpTfNe3tPg	1006	2026-11-23 10:33:59.645733+00	f
32	gvh1EPzRgTuE_sFjIQnk4YYvAFO-n8SYqLYYVnkjk_s	1006	2026-11-23 10:34:06.275827+00	f
33	Qik2FsFFeRU7bCUw5fOg5qn3U3LOGlAiRJCahttXR1Q	1006	2026-11-23 10:34:36.020036+00	f
34	czHOH6YvNWe617EMO-u4gfYmddvFCiY8t1yVAJxlj1c	1006	2026-11-23 10:34:37.46609+00	f
35	U_L2jPlaDq4fJrPqX89OEwioeDxZfuPs5-jtrnWyhnw	1006	2026-11-23 10:34:38.44637+00	f
36	bAmG7tghh9GFZm4RQrJH1XvKjOfe_GPYojshZD4rYjU	1006	2026-11-23 10:34:38.814242+00	f
37	nxtaDHkNAVfGE_9PpcXKJju9RKOltzY0sdIai37jips	1006	2026-11-23 10:35:07.184484+00	f
38	tCRwD7TLLxYp_CJk0wpe9RIJS5yjTTNuH2ZKwM5Aklk	1006	2026-11-23 10:35:48.424329+00	f
39	23t_tzPNs7mvIwx4ARQkl7OwedpsMYiKOpVE6peWDCs	1005	2026-11-23 10:36:54.277361+00	f
40	Kj9C18B6TRYeBVpXnkN3SPe-hsWRstk1lqxG_BLPMoo	1006	2026-11-23 10:38:45.965948+00	f
41	BFlsuEjzGKNtuVweNqTpX7XDmS-A2H38xqMkIIB3Xag	1001	2026-11-23 11:05:58.441383+00	f
\.


--
-- Data for Name: releve; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.releve (id, compte_id, date_generation, fichier, periode) FROM stdin;
\.


--
-- Data for Name: response_reclamation; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.response_reclamation (id, dat_response, reclamation_id, contenu) FROM stdin;
\.


--
-- Data for Name: ticket_support; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.ticket_support (id, agent_id, date_fermetur, date_ouverture, reclamation_id, priorite, status) FROM stdin;
5001	1003	\N	2026-10-12 10:05:00	4001	HAUTE	EN_TRAITEMENT
5002	\N	\N	2026-10-26 09:20:00	4002	MOYENNE	OUVERT
5003	1004	2026-09-30 09:00:00	2026-09-29 15:50:00	4003	BASSE	FERME
5004	1004	2026-09-16 10:15:00	2026-09-15 11:30:00	4004	BASSE	FERME
5005	1003	\N	2026-10-18 16:15:00	4005	MOYENNE	EN_ATTENTE_CLIENT
\.


--
-- Data for Name: transaction; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.transaction (id, montant, compte_id, date_transaction, sens, statut_transaction) FROM stdin;
3001	12000.00	2001	2026-09-01 09:10:00	CREDIT	EXECUTEE
3002	2500.00	2001	2026-09-05 14:30:00	DEBIT	EXECUTEE
3003	3000.00	2001	2026-09-12 10:15:00	CREDIT	EXECUTEE
3004	1200.00	2001	2026-10-01 08:45:00	DEBIT	EXECUTEE
3005	4500.00	2001	2026-11-01 09:10:00	CREDIT	EXECUTEE
3006	800.00	2001	2026-10-28 16:20:00	DEBIT	EXECUTEE
3007	30000.00	2002	2026-08-01 09:10:00	CREDIT	EXECUTEE
3008	5000.00	2002	2026-09-01 09:00:00	CREDIT	EXECUTEE
3009	5000.00	2002	2026-10-01 09:00:00	CREDIT	EXECUTEE
3010	8000.00	2003	2026-09-02 09:40:00	CREDIT	EXECUTEE
3011	1350.50	2003	2026-09-18 11:00:00	DEBIT	EXECUTEE
3012	2200.00	2003	2026-10-03 12:40:00	CREDIT	EXECUTEE
3013	500.00	2003	2026-10-20 17:05:00	DEBIT	EXECUTEE
3014	50000.00	2004	2026-08-15 10:10:00	CREDIT	EXECUTEE
3015	12750.75	2004	2026-09-10 15:20:00	DEBIT	EXECUTEE
3016	18000.00	2004	2026-10-05 09:00:00	CREDIT	EXECUTEE
3017	5000.00	2005	2026-09-03 09:10:00	CREDIT	EXECUTEE
3018	1500.00	2005	2026-10-02 10:30:00	CREDIT	EXECUTEE
3019	900.25	2005	2026-10-22 13:15:00	DEBIT	EXECUTEE
3020	15000.00	2006	2026-08-20 09:10:00	CREDIT	EXECUTEE
3021	2500.00	2006	2026-10-01 09:00:00	CREDIT	EXECUTEE
3022	3200.00	2007	2026-09-08 09:10:00	CREDIT	EXECUTEE
3023	700.00	2007	2026-10-06 18:00:00	DEBIT	EXECUTEE
3024	450.30	2007	2026-11-04 11:45:00	DEBIT	EXECUTEE
3025	6000.00	2008	2026-09-11 09:10:00	CREDIT	EXECUTEE
3026	1100.00	2008	2026-11-03 14:00:00	CREDIT	EXECUTEE
3027	350.00	2008	2026-11-05 10:20:00	DEBIT	EXECUTEE
3129	1000.00	2001	2026-11-16 11:37:13.471939	DEBIT	EXECUTEE
3130	1000.00	2003	2026-11-16 11:37:13.471939	CREDIT	EXECUTEE
\.


--
-- Data for Name: utilisateur; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.utilisateur (id_utilisateur, actif, doubleauth_active, telephone, cin, email, nom, password, role, username, date_creation) FROM stdin;
1001	t	f	612000101	DEMO0001	admin.demo1@demo.bankati.test	Administrateur Demo 1	$2a$10$bFcFudMg3TgWXK8a4qFf6.4Kk.EXK1b9IJGU4peu26T4YUGQYvNtq	ADMIN	admin.demo1@demo.bankati.test	\N
1002	t	f	612000102	DEMO0002	admin.demo2@demo.bankati.test	Administrateur Demo 2	$2a$10$bFcFudMg3TgWXK8a4qFf6.4Kk.EXK1b9IJGU4peu26T4YUGQYvNtq	ADMIN	admin.demo2@demo.bankati.test	\N
1003	t	f	612000103	DEMO0003	agent.demo1@demo.bankati.test	Agent Support Demo 1	$2a$10$1za7ZYyZuEI8XYnMXRRM5.SC4D5AAfWnTPlBXEF8v5eLpxsOTA22O	SUPPORT	agent.demo1@demo.bankati.test	\N
1004	t	f	612000104	DEMO0004	agent.demo2@demo.bankati.test	Agent Support Demo 2	$2a$10$1za7ZYyZuEI8XYnMXRRM5.SC4D5AAfWnTPlBXEF8v5eLpxsOTA22O	SUPPORT	agent.demo2@demo.bankati.test	\N
1005	t	f	612000105	DEMO0005	client.demo1@demo.bankati.test	Client Demo 1	$2a$10$yVcIcqr1RjDuF0b0jxY9DeEUCoVakRYoFLT56M8SCVV0fifuJI6rC	CLIENT	client.demo1@demo.bankati.test	\N
1006	t	f	612000106	DEMO0006	client.demo2@demo.bankati.test	Client Demo 2	$2a$10$yVcIcqr1RjDuF0b0jxY9DeEUCoVakRYoFLT56M8SCVV0fifuJI6rC	CLIENT	client.demo2@demo.bankati.test	\N
1007	t	f	612000107	DEMO0007	client.demo3@demo.bankati.test	Client Demo 3	$2a$10$yVcIcqr1RjDuF0b0jxY9DeEUCoVakRYoFLT56M8SCVV0fifuJI6rC	CLIENT	client.demo3@demo.bankati.test	\N
1008	t	f	612000108	DEMO0008	client.demo4@demo.bankati.test	Client Demo 4	$2a$10$yVcIcqr1RjDuF0b0jxY9DeEUCoVakRYoFLT56M8SCVV0fifuJI6rC	CLIENT	client.demo4@demo.bankati.test	\N
1009	t	f	612000109	DEMO0009	client.demo5@demo.bankati.test	Client Demo 5	$2a$10$yVcIcqr1RjDuF0b0jxY9DeEUCoVakRYoFLT56M8SCVV0fifuJI6rC	CLIENT	client.demo5@demo.bankati.test	\N
1011	t	f	766321890	\N	haqmcha@gmail.com	test test	$2a$10$2aIDg07N3bKVJrmha0/n2OaRZ5Q.3mP1icgr8mPEl23OvT35642E6	CLIENT	haqmcha@gmail.com	\N
1	t	f	0	\N	dasmed@gmail.com	dahmed samir	$2a$10$wHmP.R94xZGKamG/691Z3e.wVrWhnWauQVqT2tRcxhEd1ySqu1zX6	CLIENT	dasmed@gmail.com	\N
1061	t	f	0	\N	haqmcha1@gmail.com	adfsf ewre	$2a$10$ZAEfkRmEZWUecwyqnRWIsu2gGv82/aL/Mi.P/nrs6dFakLA/EsoDm	CLIENT	haqmcha1@gmail.com	2026-11-16 00:31:10.153848
1062	t	f	766321890	\N	soukaina@gmail.com	soukaina elgarni	$2a$10$Y7coY5unyK.UJKOCmeebQ.ANij9LzvL/3vIsVdtRWbWIKIq337Qe2	CLIENT	soukaina@gmail.com	2026-11-16 11:02:26.871598
\.


--
-- Data for Name: virement; Type: TABLE DATA; Schema: public; Owner: -
--

COPY public.virement (transaction_id, beneficiaire, motif, compte_destinataire_id, reference) FROM stdin;
3002	Atlas Energie SARL	Facture electricite septembre	\N	\N
3004	Agence Immobiliere Anfa	Loyer octobre	\N	\N
3005	Synt Bank SARL	Salaire octobre	\N	\N
3008	Epargne automatique	Virement epargne mensuel	\N	\N
3009	Epargne automatique	Virement epargne mensuel	\N	\N
3011	Maroc Telecom	Facture internet	\N	\N
3012	Atelier Nord SARL	Reglement facture 2026-118	\N	\N
3015	Fournisseur Textile SARL	Achat matieres premieres	\N	\N
3016	Groupe Retail Maghreb	Facture 2026-045	\N	\N
3019	Lydec	Facture eau	\N	\N
3021	Epargne automatique	Virement epargne mensuel	\N	\N
3024	Inwi	Abonnement mobile	\N	\N
3026	Freelance Digital SARL	Prestation design	\N	\N
3027	Cafe Central	Note de frais	\N	\N
3130	Client Demo 1	Virement	\N	\N
3129	Client Demo 2	Virement	2003	VIR-20261116-003129
\.


--
-- Name: beneficiaire_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.beneficiaire_seq', 51, true);


--
-- Name: compte_bancaire_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.compte_bancaire_seq', 2009, true);


--
-- Name: documentkyc_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.documentkyc_seq', 1, true);


--
-- Name: dossierkyc_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.dossierkyc_seq', 6004, true);


--
-- Name: journal_audit_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.journal_audit_seq', 9107, true);


--
-- Name: reclamation_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.reclamation_seq', 4056, true);


--
-- Name: refresh_tokens_id_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.refresh_tokens_id_seq', 41, true);


--
-- Name: releve_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.releve_seq', 1, true);


--
-- Name: response_reclamation_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.response_reclamation_seq', 1, true);


--
-- Name: ticket_support_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.ticket_support_seq', 5006, true);


--
-- Name: transaction_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.transaction_seq', 3178, true);


--
-- Name: utilisateur_seq; Type: SEQUENCE SET; Schema: public; Owner: -
--

SELECT pg_catalog.setval('public.utilisateur_seq', 1110, true);


--
-- Name: administrateur administrateur_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.administrateur
    ADD CONSTRAINT administrateur_pkey PRIMARY KEY (user_id);


--
-- Name: agent_support agent_support_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.agent_support
    ADD CONSTRAINT agent_support_pkey PRIMARY KEY (user_id);


--
-- Name: beneficiaire beneficiaire_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.beneficiaire
    ADD CONSTRAINT beneficiaire_pkey PRIMARY KEY (id);


--
-- Name: client client_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.client
    ADD CONSTRAINT client_pkey PRIMARY KEY (user_id);


--
-- Name: compte_bancaire compte_bancaire_numero_compte_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.compte_bancaire
    ADD CONSTRAINT compte_bancaire_numero_compte_key UNIQUE (numero_compte);


--
-- Name: compte_bancaire compte_bancaire_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.compte_bancaire
    ADD CONSTRAINT compte_bancaire_pkey PRIMARY KEY (id);


--
-- Name: compte_bancaire compte_bancaire_rib_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.compte_bancaire
    ADD CONSTRAINT compte_bancaire_rib_key UNIQUE (rib);


--
-- Name: documentkyc documentkyc_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.documentkyc
    ADD CONSTRAINT documentkyc_pkey PRIMARY KEY (id);


--
-- Name: dossierkyc dossierkyc_client_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dossierkyc
    ADD CONSTRAINT dossierkyc_client_id_key UNIQUE (client_id);


--
-- Name: dossierkyc dossierkyc_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dossierkyc
    ADD CONSTRAINT dossierkyc_pkey PRIMARY KEY (id);


--
-- Name: flyway_schema_history flyway_schema_history_pk; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.flyway_schema_history
    ADD CONSTRAINT flyway_schema_history_pk PRIMARY KEY (installed_rank);


--
-- Name: journal_audit journal_audit_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.journal_audit
    ADD CONSTRAINT journal_audit_pkey PRIMARY KEY (id);


--
-- Name: operation_guichet operation_guichet_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.operation_guichet
    ADD CONSTRAINT operation_guichet_pkey PRIMARY KEY (transaction_id);


--
-- Name: reclamation reclamation_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.reclamation
    ADD CONSTRAINT reclamation_pkey PRIMARY KEY (id);


--
-- Name: refresh_tokens refresh_tokens_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.refresh_tokens
    ADD CONSTRAINT refresh_tokens_pkey PRIMARY KEY (id);


--
-- Name: refresh_tokens refresh_tokens_token_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.refresh_tokens
    ADD CONSTRAINT refresh_tokens_token_key UNIQUE (token);


--
-- Name: releve releve_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.releve
    ADD CONSTRAINT releve_pkey PRIMARY KEY (id);


--
-- Name: response_reclamation response_reclamation_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.response_reclamation
    ADD CONSTRAINT response_reclamation_pkey PRIMARY KEY (id);


--
-- Name: ticket_support ticket_support_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ticket_support
    ADD CONSTRAINT ticket_support_pkey PRIMARY KEY (id);


--
-- Name: ticket_support ticket_support_reclamation_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ticket_support
    ADD CONSTRAINT ticket_support_reclamation_id_key UNIQUE (reclamation_id);


--
-- Name: transaction transaction_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.transaction
    ADD CONSTRAINT transaction_pkey PRIMARY KEY (id);


--
-- Name: utilisateur utilisateur_email_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.utilisateur
    ADD CONSTRAINT utilisateur_email_key UNIQUE (email);


--
-- Name: utilisateur utilisateur_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.utilisateur
    ADD CONSTRAINT utilisateur_pkey PRIMARY KEY (id_utilisateur);


--
-- Name: utilisateur utilisateur_username_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.utilisateur
    ADD CONSTRAINT utilisateur_username_key UNIQUE (username);


--
-- Name: virement virement_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.virement
    ADD CONSTRAINT virement_pkey PRIMARY KEY (transaction_id);


--
-- Name: flyway_schema_history_s_idx; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX flyway_schema_history_s_idx ON public.flyway_schema_history USING btree (success);


--
-- Name: idx_beneficiaire_client; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_beneficiaire_client ON public.beneficiaire USING btree (client_id);


--
-- Name: idx_compte_bancaire_client; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_compte_bancaire_client ON public.compte_bancaire USING btree (client_id);


--
-- Name: idx_documentkyc_dossier; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_documentkyc_dossier ON public.documentkyc USING btree (dossier_id);


--
-- Name: idx_dossierkyc_admin; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_dossierkyc_admin ON public.dossierkyc USING btree (admin_id);


--
-- Name: idx_journal_audit_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_journal_audit_date ON public.journal_audit USING btree (date_action DESC);


--
-- Name: idx_journal_audit_utilisateur; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_journal_audit_utilisateur ON public.journal_audit USING btree (utilisateur_id);


--
-- Name: idx_operation_guichet_agent; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_operation_guichet_agent ON public.operation_guichet USING btree (agent_id);


--
-- Name: idx_reclamation_agent; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_reclamation_agent ON public.reclamation USING btree (agent_id);


--
-- Name: idx_reclamation_client; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_reclamation_client ON public.reclamation USING btree (client_id);


--
-- Name: idx_reclamation_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_reclamation_date ON public.reclamation USING btree (date_creation DESC);


--
-- Name: idx_reclamation_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_reclamation_status ON public.reclamation USING btree (status);


--
-- Name: idx_refresh_tokens_user; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_refresh_tokens_user ON public.refresh_tokens USING btree (user_id);


--
-- Name: idx_releve_compte; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_releve_compte ON public.releve USING btree (compte_id);


--
-- Name: idx_response_reclamation_reclamation; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_response_reclamation_reclamation ON public.response_reclamation USING btree (reclamation_id);


--
-- Name: idx_ticket_support_agent; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_ticket_support_agent ON public.ticket_support USING btree (agent_id);


--
-- Name: idx_ticket_support_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_ticket_support_status ON public.ticket_support USING btree (status);


--
-- Name: idx_transaction_compte_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_transaction_compte_date ON public.transaction USING btree (compte_id, date_transaction DESC);


--
-- Name: idx_transaction_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_transaction_date ON public.transaction USING btree (date_transaction DESC);


--
-- Name: ux_virement_reference; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX ux_virement_reference ON public.virement USING btree (reference);


--
-- Name: administrateur fk_administrateur_utilisateur; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.administrateur
    ADD CONSTRAINT fk_administrateur_utilisateur FOREIGN KEY (user_id) REFERENCES public.utilisateur(id_utilisateur);


--
-- Name: agent_support fk_agent_support_utilisateur; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.agent_support
    ADD CONSTRAINT fk_agent_support_utilisateur FOREIGN KEY (user_id) REFERENCES public.utilisateur(id_utilisateur);


--
-- Name: beneficiaire fk_beneficiaire_client; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.beneficiaire
    ADD CONSTRAINT fk_beneficiaire_client FOREIGN KEY (client_id) REFERENCES public.client(user_id);


--
-- Name: client fk_client_utilisateur; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.client
    ADD CONSTRAINT fk_client_utilisateur FOREIGN KEY (user_id) REFERENCES public.utilisateur(id_utilisateur);


--
-- Name: compte_bancaire fk_compte_bancaire_client; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.compte_bancaire
    ADD CONSTRAINT fk_compte_bancaire_client FOREIGN KEY (client_id) REFERENCES public.client(user_id);


--
-- Name: documentkyc fk_documentkyc_dossier; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.documentkyc
    ADD CONSTRAINT fk_documentkyc_dossier FOREIGN KEY (dossier_id) REFERENCES public.dossierkyc(id);


--
-- Name: dossierkyc fk_dossierkyc_administrateur; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dossierkyc
    ADD CONSTRAINT fk_dossierkyc_administrateur FOREIGN KEY (admin_id) REFERENCES public.administrateur(user_id);


--
-- Name: dossierkyc fk_dossierkyc_client; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.dossierkyc
    ADD CONSTRAINT fk_dossierkyc_client FOREIGN KEY (client_id) REFERENCES public.client(user_id);


--
-- Name: journal_audit fk_journal_audit_utilisateur; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.journal_audit
    ADD CONSTRAINT fk_journal_audit_utilisateur FOREIGN KEY (utilisateur_id) REFERENCES public.utilisateur(id_utilisateur);


--
-- Name: operation_guichet fk_operation_guichet_agent; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.operation_guichet
    ADD CONSTRAINT fk_operation_guichet_agent FOREIGN KEY (agent_id) REFERENCES public.utilisateur(id_utilisateur);


--
-- Name: operation_guichet fk_operation_guichet_transaction; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.operation_guichet
    ADD CONSTRAINT fk_operation_guichet_transaction FOREIGN KEY (transaction_id) REFERENCES public.transaction(id);


--
-- Name: reclamation fk_reclamation_agent; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.reclamation
    ADD CONSTRAINT fk_reclamation_agent FOREIGN KEY (agent_id) REFERENCES public.agent_support(user_id);


--
-- Name: reclamation fk_reclamation_client; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.reclamation
    ADD CONSTRAINT fk_reclamation_client FOREIGN KEY (client_id) REFERENCES public.client(user_id);


--
-- Name: refresh_tokens fk_refresh_tokens_utilisateur; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.refresh_tokens
    ADD CONSTRAINT fk_refresh_tokens_utilisateur FOREIGN KEY (user_id) REFERENCES public.utilisateur(id_utilisateur) ON DELETE CASCADE;


--
-- Name: releve fk_releve_compte; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.releve
    ADD CONSTRAINT fk_releve_compte FOREIGN KEY (compte_id) REFERENCES public.compte_bancaire(id);


--
-- Name: response_reclamation fk_response_reclamation_reclamation; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.response_reclamation
    ADD CONSTRAINT fk_response_reclamation_reclamation FOREIGN KEY (reclamation_id) REFERENCES public.reclamation(id);


--
-- Name: ticket_support fk_ticket_support_agent; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ticket_support
    ADD CONSTRAINT fk_ticket_support_agent FOREIGN KEY (agent_id) REFERENCES public.agent_support(user_id);


--
-- Name: ticket_support fk_ticket_support_reclamation; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ticket_support
    ADD CONSTRAINT fk_ticket_support_reclamation FOREIGN KEY (reclamation_id) REFERENCES public.reclamation(id);


--
-- Name: transaction fk_transaction_compte; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.transaction
    ADD CONSTRAINT fk_transaction_compte FOREIGN KEY (compte_id) REFERENCES public.compte_bancaire(id);


--
-- Name: virement fk_virement_compte_destinataire; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.virement
    ADD CONSTRAINT fk_virement_compte_destinataire FOREIGN KEY (compte_destinataire_id) REFERENCES public.compte_bancaire(id);


--
-- Name: virement fk_virement_transaction; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.virement
    ADD CONSTRAINT fk_virement_transaction FOREIGN KEY (transaction_id) REFERENCES public.transaction(id);


--
-- PostgreSQL database dump complete
--


