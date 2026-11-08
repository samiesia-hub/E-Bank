-- V1 : schéma initial de la base E-Bank (PostgreSQL).
-- Reproduit exactement le mapping JPA (types, nullabilité, séquences hi/lo) afin que
-- spring.jpa.hibernate.ddl-auto=validate réussisse sur une base créée par Flyway.
-- Ordre de création : utilisateur, puis les sous-types, puis les tables métier.

CREATE TABLE utilisateur (
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
    CONSTRAINT utilisateur_pkey PRIMARY KEY (id_utilisateur),
    CONSTRAINT utilisateur_username_key UNIQUE (username),
    CONSTRAINT utilisateur_role_check CHECK (role IN ('ADMIN', 'CLIENT', 'SUPPORT'))
);
CREATE SEQUENCE utilisateur_seq START WITH 1 INCREMENT BY 50 NO MINVALUE NO MAXVALUE CACHE 1;

CREATE TABLE administrateur (
    user_id bigint NOT NULL,
    niveux_acce integer NOT NULL,
    matricule character varying(255),
    CONSTRAINT administrateur_pkey PRIMARY KEY (user_id),
    CONSTRAINT fk_administrateur_utilisateur FOREIGN KEY (user_id) REFERENCES utilisateur (id_utilisateur)
);

CREATE TABLE agent_support (
    user_id bigint NOT NULL,
    matricule character varying(255),
    specialite character varying(255),
    CONSTRAINT agent_support_pkey PRIMARY KEY (user_id),
    CONSTRAINT fk_agent_support_utilisateur FOREIGN KEY (user_id) REFERENCES utilisateur (id_utilisateur)
);

CREATE TABLE client (
    user_id bigint NOT NULL,
    date_naissance timestamp(6) without time zone,
    address character varying(255),
    numero_client character varying(255),
    CONSTRAINT client_pkey PRIMARY KEY (user_id),
    CONSTRAINT fk_client_utilisateur FOREIGN KEY (user_id) REFERENCES utilisateur (id_utilisateur)
);

CREATE TABLE compte_bancaire (
    id bigint NOT NULL,
    solde numeric(19,2),
    client_id bigint,
    date_creation timestamp(6) without time zone,
    numero_compte character varying(13),
    rib character varying(24),
    devise character varying(255),
    statut_compte character varying(255),
    type_compte character varying(255),
    CONSTRAINT compte_bancaire_pkey PRIMARY KEY (id),
    CONSTRAINT compte_bancaire_numero_compte_key UNIQUE (numero_compte),
    CONSTRAINT compte_bancaire_rib_key UNIQUE (rib),
    CONSTRAINT compte_bancaire_statut_compte_check CHECK (statut_compte IN ('ACTIF', 'BLOQUE', 'CLOTURE')),
    CONSTRAINT compte_bancaire_type_compte_check CHECK (type_compte IN ('COURANT', 'EPARGNE', 'PROFESSIONNEL')),
    CONSTRAINT fk_compte_bancaire_client FOREIGN KEY (client_id) REFERENCES client (user_id)
);
CREATE SEQUENCE compte_bancaire_seq START WITH 1 INCREMENT BY 50 NO MINVALUE NO MAXVALUE CACHE 1;

CREATE TABLE beneficiaire (
    id bigint NOT NULL,
    client_id bigint,
    nom character varying(255),
    numero_compte character varying(255),
    CONSTRAINT beneficiaire_pkey PRIMARY KEY (id),
    CONSTRAINT fk_beneficiaire_client FOREIGN KEY (client_id) REFERENCES client (user_id)
);
CREATE SEQUENCE beneficiaire_seq START WITH 1 INCREMENT BY 50 NO MINVALUE NO MAXVALUE CACHE 1;

CREATE TABLE dossierkyc (
    id bigint NOT NULL,
    admin_id bigint,
    client_id bigint NOT NULL,
    date_soumission timestamp(6) without time zone,
    statutkyc character varying(255),
    CONSTRAINT dossierkyc_pkey PRIMARY KEY (id),
    CONSTRAINT dossierkyc_client_id_key UNIQUE (client_id),
    CONSTRAINT dossierkyc_statutkyc_check CHECK (statutkyc IN ('EN_ATTENTE', 'VALIDE', 'REJETE')),
    CONSTRAINT fk_dossierkyc_administrateur FOREIGN KEY (admin_id) REFERENCES administrateur (user_id),
    CONSTRAINT fk_dossierkyc_client FOREIGN KEY (client_id) REFERENCES client (user_id)
);
CREATE SEQUENCE dossierkyc_seq START WITH 1 INCREMENT BY 50 NO MINVALUE NO MAXVALUE CACHE 1;

CREATE TABLE documentkyc (
    id bigint NOT NULL,
    dossier_id bigint,
    type_document character varying(255),
    url character varying(255),
    CONSTRAINT documentkyc_pkey PRIMARY KEY (id),
    CONSTRAINT fk_documentkyc_dossier FOREIGN KEY (dossier_id) REFERENCES dossierkyc (id)
);
CREATE SEQUENCE documentkyc_seq START WITH 1 INCREMENT BY 50 NO MINVALUE NO MAXVALUE CACHE 1;

CREATE TABLE journal_audit (
    id bigint NOT NULL,
    date_action timestamp(6) without time zone,
    utilisateur_id bigint,
    action character varying(255),
    details character varying(255),
    CONSTRAINT journal_audit_pkey PRIMARY KEY (id),
    CONSTRAINT fk_journal_audit_utilisateur FOREIGN KEY (utilisateur_id) REFERENCES utilisateur (id_utilisateur)
);
CREATE SEQUENCE journal_audit_seq START WITH 1 INCREMENT BY 50 NO MINVALUE NO MAXVALUE CACHE 1;

CREATE TABLE transaction (
    id bigint NOT NULL,
    montant numeric(38,2),
    compte_id bigint,
    date_transaction timestamp(6) without time zone,
    sens character varying(255),
    statut_transaction character varying(255),
    CONSTRAINT transaction_pkey PRIMARY KEY (id),
    CONSTRAINT transaction_sens_check CHECK (sens IN ('DEBIT', 'CREDIT')),
    CONSTRAINT transaction_statut_transaction_check CHECK (statut_transaction IN ('EN_ATTENTE', 'EXECUTEE', 'REJETEE')),
    CONSTRAINT fk_transaction_compte FOREIGN KEY (compte_id) REFERENCES compte_bancaire (id)
);
CREATE SEQUENCE transaction_seq START WITH 1 INCREMENT BY 50 NO MINVALUE NO MAXVALUE CACHE 1;

CREATE TABLE virement (
    transaction_id bigint NOT NULL,
    beneficiaire character varying(255),
    motif character varying(255),
    CONSTRAINT virement_pkey PRIMARY KEY (transaction_id),
    CONSTRAINT fk_virement_transaction FOREIGN KEY (transaction_id) REFERENCES transaction (id)
);

CREATE TABLE operation_guichet (
    transaction_id bigint NOT NULL,
    agent_id bigint,
    motif character varying(255),
    type_operation character varying(255),
    CONSTRAINT operation_guichet_pkey PRIMARY KEY (transaction_id),
    CONSTRAINT operation_guichet_type_operation_check CHECK (type_operation IN ('DEPOT', 'RETRAIT')),
    CONSTRAINT fk_operation_guichet_transaction FOREIGN KEY (transaction_id) REFERENCES transaction (id),
    CONSTRAINT fk_operation_guichet_agent FOREIGN KEY (agent_id) REFERENCES utilisateur (id_utilisateur)
);

CREATE TABLE reclamation (
    id bigint NOT NULL,
    agent_id bigint,
    client_id bigint,
    date_creation timestamp(6) without time zone,
    desciprtion character varying(255),
    objet character varying(255),
    status character varying(255),
    CONSTRAINT reclamation_pkey PRIMARY KEY (id),
    CONSTRAINT reclamation_status_check CHECK (status IN ('OUVERTE', 'EN_COURS', 'RESOLUE', 'FERMEE')),
    CONSTRAINT fk_reclamation_client FOREIGN KEY (client_id) REFERENCES client (user_id),
    CONSTRAINT fk_reclamation_agent FOREIGN KEY (agent_id) REFERENCES agent_support (user_id)
);
CREATE SEQUENCE reclamation_seq START WITH 1 INCREMENT BY 50 NO MINVALUE NO MAXVALUE CACHE 1;

CREATE TABLE ticket_support (
    id bigint NOT NULL,
    agent_id bigint,
    date_fermetur timestamp(6) without time zone,
    date_ouverture timestamp(6) without time zone,
    reclamation_id bigint,
    priorite character varying(255),
    status character varying(255),
    CONSTRAINT ticket_support_pkey PRIMARY KEY (id),
    CONSTRAINT ticket_support_reclamation_id_key UNIQUE (reclamation_id),
    CONSTRAINT ticket_support_priorite_check CHECK (priorite IN ('BASSE', 'MOYENNE', 'HAUTE', 'CRITIQUE')),
    CONSTRAINT ticket_support_status_check CHECK (status IN ('OUVERT', 'EN_TRAITEMENT', 'EN_ATTENTE_CLIENT', 'FERME')),
    CONSTRAINT fk_ticket_support_reclamation FOREIGN KEY (reclamation_id) REFERENCES reclamation (id),
    CONSTRAINT fk_ticket_support_agent FOREIGN KEY (agent_id) REFERENCES agent_support (user_id)
);
CREATE SEQUENCE ticket_support_seq START WITH 1 INCREMENT BY 50 NO MINVALUE NO MAXVALUE CACHE 1;

CREATE TABLE response_reclamation (
    id bigint NOT NULL,
    dat_response timestamp(6) without time zone,
    reclamation_id bigint,
    contenu character varying(255),
    CONSTRAINT response_reclamation_pkey PRIMARY KEY (id),
    CONSTRAINT fk_response_reclamation_reclamation FOREIGN KEY (reclamation_id) REFERENCES reclamation (id)
);
CREATE SEQUENCE response_reclamation_seq START WITH 1 INCREMENT BY 50 NO MINVALUE NO MAXVALUE CACHE 1;

CREATE TABLE releve (
    id bigint NOT NULL,
    compte_id bigint,
    date_generation timestamp(6) without time zone,
    fichier character varying(255),
    periode character varying(255),
    CONSTRAINT releve_pkey PRIMARY KEY (id),
    CONSTRAINT fk_releve_compte FOREIGN KEY (compte_id) REFERENCES compte_bancaire (id)
);
CREATE SEQUENCE releve_seq START WITH 1 INCREMENT BY 50 NO MINVALUE NO MAXVALUE CACHE 1;
