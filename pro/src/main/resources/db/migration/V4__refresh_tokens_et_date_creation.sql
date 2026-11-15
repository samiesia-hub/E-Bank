-- SCRUM-96 : jetons de rafraichissement (refresh tokens)
-- Seul le hash SHA-256 du jeton est stocke, jamais le jeton brut.
CREATE TABLE refresh_tokens (
    id bigserial NOT NULL,
    token varchar(64) NOT NULL,
    user_id bigint NOT NULL,
    expiry_date timestamp(6) with time zone NOT NULL,
    revoked boolean NOT NULL DEFAULT false,
    CONSTRAINT refresh_tokens_pkey PRIMARY KEY (id),
    CONSTRAINT refresh_tokens_token_key UNIQUE (token),
    CONSTRAINT fk_refresh_tokens_utilisateur FOREIGN KEY (user_id)
        REFERENCES utilisateur (id_utilisateur) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user ON refresh_tokens (user_id);

-- SCRUM-97 : date de creation des utilisateurs (absente du schema initial).
ALTER TABLE utilisateur ADD COLUMN IF NOT EXISTS date_creation timestamp(6) without time zone;
