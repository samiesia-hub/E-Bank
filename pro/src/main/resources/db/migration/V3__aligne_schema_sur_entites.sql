-- V3 : aligne le schema SQL sur les entites JPA actuelles.
--
-- V1 avait ete ecrite avant l'ajout de quelques champs dans le code :
--   * Beneficiaire.prenom
--   * Reclamation.dateMiseajour
--   * Virement.compteDestinataire / Virement.reference
-- Hibernate (ddl-auto=validate) refuse alors le demarrage avec
-- "Schema validation: missing column ...".
--
-- Idempotence : IF NOT EXISTS partout, la migration peut etre rejouee sans erreur.

ALTER TABLE beneficiaire
    ADD COLUMN IF NOT EXISTS prenom character varying(255);

ALTER TABLE reclamation
    ADD COLUMN IF NOT EXISTS date_miseajour timestamp(6);

ALTER TABLE virement
    ADD COLUMN IF NOT EXISTS compte_destinataire_id bigint,
    ADD COLUMN IF NOT EXISTS reference character varying(40);

-- Un virement peut designer un compte beneficiaire interne (au lieu d'un IBAN libre).
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_virement_compte_destinataire'
    ) THEN
        ALTER TABLE virement
            ADD CONSTRAINT fk_virement_compte_destinataire
            FOREIGN KEY (compte_destinataire_id) REFERENCES compte_bancaire (id);
    END IF;
END $$;

-- La reference de virement doit rester unique quand elle est renseignee.
CREATE UNIQUE INDEX IF NOT EXISTS ux_virement_reference
    ON virement (reference);
