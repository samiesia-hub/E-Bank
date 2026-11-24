-- V5 : complete les modifications du schema qui n'avaient pas ete
-- appliquees lors de l'execution initiale de V3.

-- Beneficiaire.prenom existe deja dans la base.
-- Reclamation : date de mise a jour.
ALTER TABLE reclamation
    ADD COLUMN IF NOT EXISTS date_miseajour timestamp(6) without time zone;

-- Virement : compte destinataire interne et reference.
ALTER TABLE virement
    ADD COLUMN IF NOT EXISTS compte_destinataire_id bigint,
    ADD COLUMN IF NOT EXISTS reference character varying(40);

-- Relation entre un virement et son compte destinataire.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'fk_virement_compte_destinataire'
    ) THEN
        ALTER TABLE virement
            ADD CONSTRAINT fk_virement_compte_destinataire
            FOREIGN KEY (compte_destinataire_id)
            REFERENCES compte_bancaire (id);
    END IF;
END $$;

-- Une reference de virement ne doit pas etre dupliquee.
CREATE UNIQUE INDEX IF NOT EXISTS ux_virement_reference
    ON virement (reference);
