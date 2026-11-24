--
-- PostgreSQL database dump
--

\restrict FzHjYcuv5dCmbJS1pzIel3O7sdbbbdaCzsgnHb7sFCArsxxGvaENbrWKs7dxjvU

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

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: administrateur; Type: TABLE; Schema: public; Owner: ebank
--

CREATE TABLE public.administrateur (
    user_id bigint NOT NULL,
    niveux_acce integer NOT NULL,
    matricule character varying(255)
);


ALTER TABLE public.administrateur OWNER TO ebank;

--
-- Name: agent_support; Type: TABLE; Schema: public; Owner: ebank
--

CREATE TABLE public.agent_support (
    user_id bigint NOT NULL,
    matricule character varying(255),
    specialite character varying(255)
);


ALTER TABLE public.agent_support OWNER TO ebank;

--
-- Name: beneficiaire; Type: TABLE; Schema: public; Owner: ebank
--

CREATE TABLE public.beneficiaire (
    id bigint NOT NULL,
    client_id bigint,
    nom character varying(255),
    numero_compte character varying(255),
    prenom character varying(100)
);


ALTER TABLE public.beneficiaire OWNER TO ebank;

--
-- Name: beneficiaire_seq; Type: SEQUENCE; Schema: public; Owner: ebank
--

CREATE SEQUENCE public.beneficiaire_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.beneficiaire_seq OWNER TO ebank;

--
-- Name: client; Type: TABLE; Schema: public; Owner: ebank
--

CREATE TABLE public.client (
    user_id bigint NOT NULL,
    date_naissance timestamp(6) without time zone,
    address character varying(255),
    numero_client character varying(255)
);


ALTER TABLE public.client OWNER TO ebank;

--
-- Name: compte_bancaire; Type: TABLE; Schema: public; Owner: ebank
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


ALTER TABLE public.compte_bancaire OWNER TO ebank;

--
-- Name: compte_bancaire_seq; Type: SEQUENCE; Schema: public; Owner: ebank
--

CREATE SEQUENCE public.compte_bancaire_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.compte_bancaire_seq OWNER TO ebank;

--
-- Name: documentkyc; Type: TABLE; Schema: public; Owner: ebank
--

CREATE TABLE public.documentkyc (
    id bigint NOT NULL,
    dossier_id bigint,
    type_document character varying(255),
    url character varying(255)
);


ALTER TABLE public.documentkyc OWNER TO ebank;

--
-- Name: documentkyc_seq; Type: SEQUENCE; Schema: public; Owner: ebank
--

CREATE SEQUENCE public.documentkyc_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.documentkyc_seq OWNER TO ebank;

--
-- Name: dossierkyc; Type: TABLE; Schema: public; Owner: ebank
--

CREATE TABLE public.dossierkyc (
    id bigint NOT NULL,
    admin_id bigint,
    client_id bigint NOT NULL,
    date_soumission timestamp(6) without time zone,
    statutkyc character varying(255),
    CONSTRAINT dossierkyc_statutkyc_check CHECK (((statutkyc)::text = ANY ((ARRAY['EN_ATTENTE'::character varying, 'VALIDE'::character varying, 'REJETE'::character varying])::text[])))
);


ALTER TABLE public.dossierkyc OWNER TO ebank;

--
-- Name: dossierkyc_seq; Type: SEQUENCE; Schema: public; Owner: ebank
--

CREATE SEQUENCE public.dossierkyc_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.dossierkyc_seq OWNER TO ebank;

--
-- Name: flyway_schema_history; Type: TABLE; Schema: public; Owner: ebank
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


ALTER TABLE public.flyway_schema_history OWNER TO ebank;

--
-- Name: journal_audit; Type: TABLE; Schema: public; Owner: ebank
--

CREATE TABLE public.journal_audit (
    id bigint NOT NULL,
    date_action timestamp(6) without time zone,
    utilisateur_id bigint,
    action character varying(255),
    details character varying(255)
);


ALTER TABLE public.journal_audit OWNER TO ebank;

--
-- Name: journal_audit_seq; Type: SEQUENCE; Schema: public; Owner: ebank
--

CREATE SEQUENCE public.journal_audit_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.journal_audit_seq OWNER TO ebank;

--
-- Name: operation_guichet; Type: TABLE; Schema: public; Owner: ebank
--

CREATE TABLE public.operation_guichet (
    transaction_id bigint NOT NULL,
    agent_id bigint,
    motif character varying(255),
    type_operation character varying(255),
    CONSTRAINT operation_guichet_type_operation_check CHECK (((type_operation)::text = ANY ((ARRAY['DEPOT'::character varying, 'RETRAIT'::character varying])::text[])))
);


ALTER TABLE public.operation_guichet OWNER TO ebank;

--
-- Name: reclamation; Type: TABLE; Schema: public; Owner: ebank
--

CREATE TABLE public.reclamation (
    id bigint NOT NULL,
    agent_id bigint,
    client_id bigint,
    date_creation timestamp(6) without time zone,
    desciprtion character varying(255),
    objet character varying(255),
    status character varying(255),
    CONSTRAINT reclamation_status_check CHECK (((status)::text = ANY ((ARRAY['OUVERTE'::character varying, 'EN_COURS'::character varying, 'RESOLUE'::character varying, 'FERMEE'::character varying])::text[])))
);


ALTER TABLE public.reclamation OWNER TO ebank;

--
-- Name: reclamation_seq; Type: SEQUENCE; Schema: public; Owner: ebank
--

CREATE SEQUENCE public.reclamation_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.reclamation_seq OWNER TO ebank;

--
-- Name: releve; Type: TABLE; Schema: public; Owner: ebank
--

CREATE TABLE public.releve (
    id bigint NOT NULL,
    compte_id bigint,
    date_generation timestamp(6) without time zone,
    fichier character varying(255),
    periode character varying(255)
);


ALTER TABLE public.releve OWNER TO ebank;

--
-- Name: releve_seq; Type: SEQUENCE; Schema: public; Owner: ebank
--

CREATE SEQUENCE public.releve_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.releve_seq OWNER TO ebank;

--
-- Name: response_reclamation; Type: TABLE; Schema: public; Owner: ebank
--

CREATE TABLE public.response_reclamation (
    id bigint NOT NULL,
    dat_response timestamp(6) without time zone,
    reclamation_id bigint,
    contenu character varying(255)
);


ALTER TABLE public.response_reclamation OWNER TO ebank;

--
-- Name: response_reclamation_seq; Type: SEQUENCE; Schema: public; Owner: ebank
--

CREATE SEQUENCE public.response_reclamation_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.response_reclamation_seq OWNER TO ebank;

--
-- Name: ticket_support; Type: TABLE; Schema: public; Owner: ebank
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


ALTER TABLE public.ticket_support OWNER TO ebank;

--
-- Name: ticket_support_seq; Type: SEQUENCE; Schema: public; Owner: ebank
--

CREATE SEQUENCE public.ticket_support_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.ticket_support_seq OWNER TO ebank;

--
-- Name: transaction; Type: TABLE; Schema: public; Owner: ebank
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


ALTER TABLE public.transaction OWNER TO ebank;

--
-- Name: transaction_seq; Type: SEQUENCE; Schema: public; Owner: ebank
--

CREATE SEQUENCE public.transaction_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.transaction_seq OWNER TO ebank;

--
-- Name: utilisateur; Type: TABLE; Schema: public; Owner: ebank
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
    CONSTRAINT utilisateur_role_check CHECK (((role)::text = ANY ((ARRAY['ADMIN'::character varying, 'CLIENT'::character varying, 'SUPPORT'::character varying])::text[])))
);


ALTER TABLE public.utilisateur OWNER TO ebank;

--
-- Name: utilisateur_seq; Type: SEQUENCE; Schema: public; Owner: ebank
--

CREATE SEQUENCE public.utilisateur_seq
    START WITH 1
    INCREMENT BY 50
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;


ALTER SEQUENCE public.utilisateur_seq OWNER TO ebank;

--
-- Name: virement; Type: TABLE; Schema: public; Owner: ebank
--

CREATE TABLE public.virement (
    transaction_id bigint NOT NULL,
    beneficiaire character varying(255),
    motif character varying(255)
);


ALTER TABLE public.virement OWNER TO ebank;

--
-- Data for Name: administrateur; Type: TABLE DATA; Schema: public; Owner: ebank
--

COPY public.administrateur (user_id, niveux_acce, matricule) FROM stdin;
\.


--
-- Data for Name: agent_support; Type: TABLE DATA; Schema: public; Owner: ebank
--

COPY public.agent_support (user_id, matricule, specialite) FROM stdin;
\.


--
-- Data for Name: beneficiaire; Type: TABLE DATA; Schema: public; Owner: ebank
--

COPY public.beneficiaire (id, client_id, nom, numero_compte, prenom) FROM stdin;
\.


--
-- Data for Name: client; Type: TABLE DATA; Schema: public; Owner: ebank
--

COPY public.client (user_id, date_naissance, address, numero_client) FROM stdin;
\.


--
-- Data for Name: compte_bancaire; Type: TABLE DATA; Schema: public; Owner: ebank
--

COPY public.compte_bancaire (id, solde, client_id, date_creation, numero_compte, rib, devise, statut_compte, type_compte) FROM stdin;
\.


--
-- Data for Name: documentkyc; Type: TABLE DATA; Schema: public; Owner: ebank
--

COPY public.documentkyc (id, dossier_id, type_document, url) FROM stdin;
\.


--
-- Data for Name: dossierkyc; Type: TABLE DATA; Schema: public; Owner: ebank
--

COPY public.dossierkyc (id, admin_id, client_id, date_soumission, statutkyc) FROM stdin;
\.


--
-- Data for Name: flyway_schema_history; Type: TABLE DATA; Schema: public; Owner: ebank
--

COPY public.flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success) FROM stdin;
1	1	schema initial	SQL	V1__schema_initial.sql	1126019629	ebank	2026-11-10 15:06:45.862541	123	t
2	2	index et unicite	SQL	V2__index_et_unicite.sql	-1464955410	ebank	2026-11-10 15:06:46.027283	51	t
3	3	add prenom beneficiaire	SQL	V3__add_prenom_beneficiaire.sql	-552591957	ebank	2026-11-10 15:11:46.272232	19	t
\.


--
-- Data for Name: journal_audit; Type: TABLE DATA; Schema: public; Owner: ebank
--

COPY public.journal_audit (id, date_action, utilisateur_id, action, details) FROM stdin;
\.


--
-- Data for Name: operation_guichet; Type: TABLE DATA; Schema: public; Owner: ebank
--

COPY public.operation_guichet (transaction_id, agent_id, motif, type_operation) FROM stdin;
\.


--
-- Data for Name: reclamation; Type: TABLE DATA; Schema: public; Owner: ebank
--

COPY public.reclamation (id, agent_id, client_id, date_creation, desciprtion, objet, status) FROM stdin;
\.


--
-- Data for Name: releve; Type: TABLE DATA; Schema: public; Owner: ebank
--

COPY public.releve (id, compte_id, date_generation, fichier, periode) FROM stdin;
\.


--
-- Data for Name: response_reclamation; Type: TABLE DATA; Schema: public; Owner: ebank
--

COPY public.response_reclamation (id, dat_response, reclamation_id, contenu) FROM stdin;
\.


--
-- Data for Name: ticket_support; Type: TABLE DATA; Schema: public; Owner: ebank
--

COPY public.ticket_support (id, agent_id, date_fermetur, date_ouverture, reclamation_id, priorite, status) FROM stdin;
\.


--
-- Data for Name: transaction; Type: TABLE DATA; Schema: public; Owner: ebank
--

COPY public.transaction (id, montant, compte_id, date_transaction, sens, statut_transaction) FROM stdin;
\.


--
-- Data for Name: utilisateur; Type: TABLE DATA; Schema: public; Owner: ebank
--

COPY public.utilisateur (id_utilisateur, actif, doubleauth_active, telephone, cin, email, nom, password, role, username) FROM stdin;
\.


--
-- Data for Name: virement; Type: TABLE DATA; Schema: public; Owner: ebank
--

COPY public.virement (transaction_id, beneficiaire, motif) FROM stdin;
\.


--
-- Name: beneficiaire_seq; Type: SEQUENCE SET; Schema: public; Owner: ebank
--

SELECT pg_catalog.setval('public.beneficiaire_seq', 1, false);


--
-- Name: compte_bancaire_seq; Type: SEQUENCE SET; Schema: public; Owner: ebank
--

SELECT pg_catalog.setval('public.compte_bancaire_seq', 1, false);


--
-- Name: documentkyc_seq; Type: SEQUENCE SET; Schema: public; Owner: ebank
--

SELECT pg_catalog.setval('public.documentkyc_seq', 1, false);


--
-- Name: dossierkyc_seq; Type: SEQUENCE SET; Schema: public; Owner: ebank
--

SELECT pg_catalog.setval('public.dossierkyc_seq', 1, false);


--
-- Name: journal_audit_seq; Type: SEQUENCE SET; Schema: public; Owner: ebank
--

SELECT pg_catalog.setval('public.journal_audit_seq', 1, false);


--
-- Name: reclamation_seq; Type: SEQUENCE SET; Schema: public; Owner: ebank
--

SELECT pg_catalog.setval('public.reclamation_seq', 1, false);


--
-- Name: releve_seq; Type: SEQUENCE SET; Schema: public; Owner: ebank
--

SELECT pg_catalog.setval('public.releve_seq', 1, false);


--
-- Name: response_reclamation_seq; Type: SEQUENCE SET; Schema: public; Owner: ebank
--

SELECT pg_catalog.setval('public.response_reclamation_seq', 1, false);


--
-- Name: ticket_support_seq; Type: SEQUENCE SET; Schema: public; Owner: ebank
--

SELECT pg_catalog.setval('public.ticket_support_seq', 1, false);


--
-- Name: transaction_seq; Type: SEQUENCE SET; Schema: public; Owner: ebank
--

SELECT pg_catalog.setval('public.transaction_seq', 1, false);


--
-- Name: utilisateur_seq; Type: SEQUENCE SET; Schema: public; Owner: ebank
--

SELECT pg_catalog.setval('public.utilisateur_seq', 1, false);


--
-- Name: administrateur administrateur_pkey; Type: CONSTRAINT; Schema: public; Owner: ebank
--

ALTER TABLE ONLY public.administrateur
    ADD CONSTRAINT administrateur_pkey PRIMARY KEY (user_id);


--
-- Name: agent_support agent_support_pkey; Type: CONSTRAINT; Schema: public; Owner: ebank
--

ALTER TABLE ONLY public.agent_support
    ADD CONSTRAINT agent_support_pkey PRIMARY KEY (user_id);


--
-- Name: beneficiaire beneficiaire_pkey; Type: CONSTRAINT; Schema: public; Owner: ebank
--

ALTER TABLE ONLY public.beneficiaire
    ADD CONSTRAINT beneficiaire_pkey PRIMARY KEY (id);


--
-- Name: client client_pkey; Type: CONSTRAINT; Schema: public; Owner: ebank
--

ALTER TABLE ONLY public.client
    ADD CONSTRAINT client_pkey PRIMARY KEY (user_id);


--
-- Name: compte_bancaire compte_bancaire_numero_compte_key; Type: CONSTRAINT; Schema: public; Owner: ebank
--

ALTER TABLE ONLY public.compte_bancaire
    ADD CONSTRAINT compte_bancaire_numero_compte_key UNIQUE (numero_compte);


--
-- Name: compte_bancaire compte_bancaire_pkey; Type: CONSTRAINT; Schema: public; Owner: ebank
--

ALTER TABLE ONLY public.compte_bancaire
    ADD CONSTRAINT compte_bancaire_pkey PRIMARY KEY (id);


--
-- Name: compte_bancaire compte_bancaire_rib_key; Type: CONSTRAINT; Schema: public; Owner: ebank
--

ALTER TABLE ONLY public.compte_bancaire
    ADD CONSTRAINT compte_bancaire_rib_key UNIQUE (rib);


--
-- Name: documentkyc documentkyc_pkey; Type: CONSTRAINT; Schema: public; Owner: ebank
--

ALTER TABLE ONLY public.documentkyc
    ADD CONSTRAINT documentkyc_pkey PRIMARY KEY (id);


--
-- Name: dossierkyc dossierkyc_client_id_key; Type: CONSTRAINT; Schema: public; Owner: ebank
--

ALTER TABLE ONLY public.dossierkyc
    ADD CONSTRAINT dossierkyc_client_id_key UNIQUE (client_id);


--
-- Name: dossierkyc dossierkyc_pkey; Type: CONSTRAINT; Schema: public; Owner: ebank
--

ALTER TABLE ONLY public.dossierkyc
    ADD CONSTRAINT dossierkyc_pkey PRIMARY KEY (id);


--
-- Name: flyway_schema_history flyway_schema_history_pk; Type: CONSTRAINT; Schema: public; Owner: ebank
--

ALTER TABLE ONLY public.flyway_schema_history
    ADD CONSTRAINT flyway_schema_history_pk PRIMARY KEY (installed_rank);


--
-- Name: journal_audit journal_audit_pkey; Type: CONSTRAINT; Schema: public; Owner: ebank
--

ALTER TABLE ONLY public.journal_audit
    ADD CONSTRAINT journal_audit_pkey PRIMARY KEY (id);


--
-- Name: operation_guichet operation_guichet_pkey; Type: CONSTRAINT; Schema: public; Owner: ebank
--

ALTER TABLE ONLY public.operation_guichet
    ADD CONSTRAINT operation_guichet_pkey PRIMARY KEY (transaction_id);


--
-- Name: reclamation reclamation_pkey; Type: CONSTRAINT; Schema: public; Owner: ebank
--

ALTER TABLE ONLY public.reclamation
    ADD CONSTRAINT reclamation_pkey PRIMARY KEY (id);


--
-- Name: releve releve_pkey; Type: CONSTRAINT; Schema: public; Owner: ebank
--

ALTER TABLE ONLY public.releve
    ADD CONSTRAINT releve_pkey PRIMARY KEY (id);


--
-- Name: response_reclamation response_reclamation_pkey; Type: CONSTRAINT; Schema: public; Owner: ebank
--

ALTER TABLE ONLY public.response_reclamation
    ADD CONSTRAINT response_reclamation_pkey PRIMARY KEY (id);


--
-- Name: ticket_support ticket_support_pkey; Type: CONSTRAINT; Schema: public; Owner: ebank
--

ALTER TABLE ONLY public.ticket_support
    ADD CONSTRAINT ticket_support_pkey PRIMARY KEY (id);


--
-- Name: ticket_support ticket_support_reclamation_id_key; Type: CONSTRAINT; Schema: public; Owner: ebank
--

ALTER TABLE ONLY public.ticket_support
    ADD CONSTRAINT ticket_support_reclamation_id_key UNIQUE (reclamation_id);


--
-- Name: transaction transaction_pkey; Type: CONSTRAINT; Schema: public; Owner: ebank
--

ALTER TABLE ONLY public.transaction
    ADD CONSTRAINT transaction_pkey PRIMARY KEY (id);


--
-- Name: utilisateur utilisateur_email_key; Type: CONSTRAINT; Schema: public; Owner: ebank
--

ALTER TABLE ONLY public.utilisateur
    ADD CONSTRAINT utilisateur_email_key UNIQUE (email);


--
-- Name: utilisateur utilisateur_pkey; Type: CONSTRAINT; Schema: public; Owner: ebank
--

ALTER TABLE ONLY public.utilisateur
    ADD CONSTRAINT utilisateur_pkey PRIMARY KEY (id_utilisateur);


--
-- Name: utilisateur utilisateur_username_key; Type: CONSTRAINT; Schema: public; Owner: ebank
--

ALTER TABLE ONLY public.utilisateur
    ADD CONSTRAINT utilisateur_username_key UNIQUE (username);


--
-- Name: virement virement_pkey; Type: CONSTRAINT; Schema: public; Owner: ebank
--

ALTER TABLE ONLY public.virement
    ADD CONSTRAINT virement_pkey PRIMARY KEY (transaction_id);


--
-- Name: flyway_schema_history_s_idx; Type: INDEX; Schema: public; Owner: ebank
--

CREATE INDEX flyway_schema_history_s_idx ON public.flyway_schema_history USING btree (success);


--
-- Name: idx_beneficiaire_client; Type: INDEX; Schema: public; Owner: ebank
--

CREATE INDEX idx_beneficiaire_client ON public.beneficiaire USING btree (client_id);


--
-- Name: idx_compte_bancaire_client; Type: INDEX; Schema: public; Owner: ebank
--

CREATE INDEX idx_compte_bancaire_client ON public.compte_bancaire USING btree (client_id);


--
-- Name: idx_documentkyc_dossier; Type: INDEX; Schema: public; Owner: ebank
--

CREATE INDEX idx_documentkyc_dossier ON public.documentkyc USING btree (dossier_id);


--
-- Name: idx_dossierkyc_admin; Type: INDEX; Schema: public; Owner: ebank
--

CREATE INDEX idx_dossierkyc_admin ON public.dossierkyc USING btree (admin_id);


--
-- Name: idx_journal_audit_date; Type: INDEX; Schema: public; Owner: ebank
--

CREATE INDEX idx_journal_audit_date ON public.journal_audit USING btree (date_action DESC);


--
-- Name: idx_journal_audit_utilisateur; Type: INDEX; Schema: public; Owner: ebank
--

CREATE INDEX idx_journal_audit_utilisateur ON public.journal_audit USING btree (utilisateur_id);


--
-- Name: idx_operation_guichet_agent; Type: INDEX; Schema: public; Owner: ebank
--

CREATE INDEX idx_operation_guichet_agent ON public.operation_guichet USING btree (agent_id);


--
-- Name: idx_reclamation_agent; Type: INDEX; Schema: public; Owner: ebank
--

CREATE INDEX idx_reclamation_agent ON public.reclamation USING btree (agent_id);


--
-- Name: idx_reclamation_client; Type: INDEX; Schema: public; Owner: ebank
--

CREATE INDEX idx_reclamation_client ON public.reclamation USING btree (client_id);


--
-- Name: idx_reclamation_date; Type: INDEX; Schema: public; Owner: ebank
--

CREATE INDEX idx_reclamation_date ON public.reclamation USING btree (date_creation DESC);


--
-- Name: idx_reclamation_status; Type: INDEX; Schema: public; Owner: ebank
--

CREATE INDEX idx_reclamation_status ON public.reclamation USING btree (status);


--
-- Name: idx_releve_compte; Type: INDEX; Schema: public; Owner: ebank
--

CREATE INDEX idx_releve_compte ON public.releve USING btree (compte_id);


--
-- Name: idx_response_reclamation_reclamation; Type: INDEX; Schema: public; Owner: ebank
--

CREATE INDEX idx_response_reclamation_reclamation ON public.response_reclamation USING btree (reclamation_id);


--
-- Name: idx_ticket_support_agent; Type: INDEX; Schema: public; Owner: ebank
--

CREATE INDEX idx_ticket_support_agent ON public.ticket_support USING btree (agent_id);


--
-- Name: idx_ticket_support_status; Type: INDEX; Schema: public; Owner: ebank
--

CREATE INDEX idx_ticket_support_status ON public.ticket_support USING btree (status);


--
-- Name: idx_transaction_compte_date; Type: INDEX; Schema: public; Owner: ebank
--

CREATE INDEX idx_transaction_compte_date ON public.transaction USING btree (compte_id, date_transaction DESC);


--
-- Name: idx_transaction_date; Type: INDEX; Schema: public; Owner: ebank
--

CREATE INDEX idx_transaction_date ON public.transaction USING btree (date_transaction DESC);


--
-- Name: administrateur fk_administrateur_utilisateur; Type: FK CONSTRAINT; Schema: public; Owner: ebank
--

ALTER TABLE ONLY public.administrateur
    ADD CONSTRAINT fk_administrateur_utilisateur FOREIGN KEY (user_id) REFERENCES public.utilisateur(id_utilisateur);


--
-- Name: agent_support fk_agent_support_utilisateur; Type: FK CONSTRAINT; Schema: public; Owner: ebank
--

ALTER TABLE ONLY public.agent_support
    ADD CONSTRAINT fk_agent_support_utilisateur FOREIGN KEY (user_id) REFERENCES public.utilisateur(id_utilisateur);


--
-- Name: beneficiaire fk_beneficiaire_client; Type: FK CONSTRAINT; Schema: public; Owner: ebank
--

ALTER TABLE ONLY public.beneficiaire
    ADD CONSTRAINT fk_beneficiaire_client FOREIGN KEY (client_id) REFERENCES public.client(user_id);


--
-- Name: client fk_client_utilisateur; Type: FK CONSTRAINT; Schema: public; Owner: ebank
--

ALTER TABLE ONLY public.client
    ADD CONSTRAINT fk_client_utilisateur FOREIGN KEY (user_id) REFERENCES public.utilisateur(id_utilisateur);


--
-- Name: compte_bancaire fk_compte_bancaire_client; Type: FK CONSTRAINT; Schema: public; Owner: ebank
--

ALTER TABLE ONLY public.compte_bancaire
    ADD CONSTRAINT fk_compte_bancaire_client FOREIGN KEY (client_id) REFERENCES public.client(user_id);


--
-- Name: documentkyc fk_documentkyc_dossier; Type: FK CONSTRAINT; Schema: public; Owner: ebank
--

ALTER TABLE ONLY public.documentkyc
    ADD CONSTRAINT fk_documentkyc_dossier FOREIGN KEY (dossier_id) REFERENCES public.dossierkyc(id);


--
-- Name: dossierkyc fk_dossierkyc_administrateur; Type: FK CONSTRAINT; Schema: public; Owner: ebank
--

ALTER TABLE ONLY public.dossierkyc
    ADD CONSTRAINT fk_dossierkyc_administrateur FOREIGN KEY (admin_id) REFERENCES public.administrateur(user_id);


--
-- Name: dossierkyc fk_dossierkyc_client; Type: FK CONSTRAINT; Schema: public; Owner: ebank
--

ALTER TABLE ONLY public.dossierkyc
    ADD CONSTRAINT fk_dossierkyc_client FOREIGN KEY (client_id) REFERENCES public.client(user_id);


--
-- Name: journal_audit fk_journal_audit_utilisateur; Type: FK CONSTRAINT; Schema: public; Owner: ebank
--

ALTER TABLE ONLY public.journal_audit
    ADD CONSTRAINT fk_journal_audit_utilisateur FOREIGN KEY (utilisateur_id) REFERENCES public.utilisateur(id_utilisateur);


--
-- Name: operation_guichet fk_operation_guichet_agent; Type: FK CONSTRAINT; Schema: public; Owner: ebank
--

ALTER TABLE ONLY public.operation_guichet
    ADD CONSTRAINT fk_operation_guichet_agent FOREIGN KEY (agent_id) REFERENCES public.utilisateur(id_utilisateur);


--
-- Name: operation_guichet fk_operation_guichet_transaction; Type: FK CONSTRAINT; Schema: public; Owner: ebank
--

ALTER TABLE ONLY public.operation_guichet
    ADD CONSTRAINT fk_operation_guichet_transaction FOREIGN KEY (transaction_id) REFERENCES public.transaction(id);


--
-- Name: reclamation fk_reclamation_agent; Type: FK CONSTRAINT; Schema: public; Owner: ebank
--

ALTER TABLE ONLY public.reclamation
    ADD CONSTRAINT fk_reclamation_agent FOREIGN KEY (agent_id) REFERENCES public.agent_support(user_id);


--
-- Name: reclamation fk_reclamation_client; Type: FK CONSTRAINT; Schema: public; Owner: ebank
--

ALTER TABLE ONLY public.reclamation
    ADD CONSTRAINT fk_reclamation_client FOREIGN KEY (client_id) REFERENCES public.client(user_id);


--
-- Name: releve fk_releve_compte; Type: FK CONSTRAINT; Schema: public; Owner: ebank
--

ALTER TABLE ONLY public.releve
    ADD CONSTRAINT fk_releve_compte FOREIGN KEY (compte_id) REFERENCES public.compte_bancaire(id);


--
-- Name: response_reclamation fk_response_reclamation_reclamation; Type: FK CONSTRAINT; Schema: public; Owner: ebank
--

ALTER TABLE ONLY public.response_reclamation
    ADD CONSTRAINT fk_response_reclamation_reclamation FOREIGN KEY (reclamation_id) REFERENCES public.reclamation(id);


--
-- Name: ticket_support fk_ticket_support_agent; Type: FK CONSTRAINT; Schema: public; Owner: ebank
--

ALTER TABLE ONLY public.ticket_support
    ADD CONSTRAINT fk_ticket_support_agent FOREIGN KEY (agent_id) REFERENCES public.agent_support(user_id);


--
-- Name: ticket_support fk_ticket_support_reclamation; Type: FK CONSTRAINT; Schema: public; Owner: ebank
--

ALTER TABLE ONLY public.ticket_support
    ADD CONSTRAINT fk_ticket_support_reclamation FOREIGN KEY (reclamation_id) REFERENCES public.reclamation(id);


--
-- Name: transaction fk_transaction_compte; Type: FK CONSTRAINT; Schema: public; Owner: ebank
--

ALTER TABLE ONLY public.transaction
    ADD CONSTRAINT fk_transaction_compte FOREIGN KEY (compte_id) REFERENCES public.compte_bancaire(id);


--
-- Name: virement fk_virement_transaction; Type: FK CONSTRAINT; Schema: public; Owner: ebank
--

ALTER TABLE ONLY public.virement
    ADD CONSTRAINT fk_virement_transaction FOREIGN KEY (transaction_id) REFERENCES public.transaction(id);


--
-- PostgreSQL database dump complete
--

\unrestrict FzHjYcuv5dCmbJS1pzIel3O7sdbbbdaCzsgnHb7sFCArsxxGvaENbrWKs7dxjvU

