-- V2: users and authentication tables
-- Deleting a users row cascades to everything owned by that user.
-- The 30-day purge job simply runs: DELETE FROM users WHERE deleted_at < now() - interval '30 days'.

CREATE TABLE users (
                       id                 uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                       role_id            uuid        NOT NULL REFERENCES user_roles (id),
                       country_id         uuid        REFERENCES countries (id),
                       first_name         text        NOT NULL,
                       last_name          text        NOT NULL,
                       preferred_language text,
                       status             text        NOT NULL DEFAULT 'pending'
                           CHECK (status IN ('pending', 'active', 'suspended')),
                       last_login_at      timestamptz,
                       created_at         timestamptz NOT NULL DEFAULT now(),
                       updated_at         timestamptz NOT NULL DEFAULT now(),
                       deleted_at         timestamptz
);

CREATE INDEX idx_users_role_id ON users (role_id);
CREATE INDEX idx_users_purge ON users (deleted_at) WHERE deleted_at IS NOT NULL;

CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE user_identifiers (
                                  id              uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                                  user_id         uuid        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
                                  identifier_type text        NOT NULL CHECK (identifier_type IN ('email', 'phone', 'username')),
                                  identifier      text        NOT NULL,
                                  is_primary      boolean     NOT NULL DEFAULT false,
                                  verified_at     timestamptz,
                                  created_at      timestamptz NOT NULL DEFAULT now(),
                                  updated_at      timestamptz NOT NULL DEFAULT now(),
                                  CONSTRAINT uq_user_identifiers_type_identifier UNIQUE (identifier_type, identifier),
                                  CONSTRAINT ck_user_identifiers_email_lowercase
                                      CHECK (identifier_type <> 'email' OR identifier = lower(identifier))
);

CREATE INDEX idx_user_identifiers_user_id ON user_identifiers (user_id);
CREATE UNIQUE INDEX uq_user_identifiers_one_primary
    ON user_identifiers (user_id, identifier_type) WHERE is_primary;

CREATE TRIGGER trg_user_identifiers_updated_at
    BEFORE UPDATE ON user_identifiers
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE user_passwords (
                                user_id       uuid PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
                                password_hash text        NOT NULL,
                                updated_at    timestamptz NOT NULL DEFAULT now()
);

CREATE TRIGGER trg_user_passwords_updated_at
    BEFORE UPDATE ON user_passwords
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE user_social_accounts (
                                      id               uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                                      user_id          uuid        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
                                      provider         text        NOT NULL,
                                      provider_user_id text        NOT NULL,
                                      is_active        boolean     NOT NULL DEFAULT true,
                                      profile_data     jsonb,
                                      linked_at        timestamptz NOT NULL DEFAULT now(),
                                      updated_at       timestamptz NOT NULL DEFAULT now(),
                                      CONSTRAINT uq_user_social_accounts_provider UNIQUE (provider, provider_user_id)
);

CREATE INDEX idx_user_social_accounts_user_id ON user_social_accounts (user_id);

CREATE TRIGGER trg_user_social_accounts_updated_at
    BEFORE UPDATE ON user_social_accounts
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE user_verification_tokens (
                                          id                uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                                          user_id           uuid        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
                                          identifier_id     uuid        NOT NULL REFERENCES user_identifiers (id) ON DELETE CASCADE,
                                          purpose           text        NOT NULL CHECK (purpose IN ('signup_otp', 'password_reset')),
                                          token_hash        text        NOT NULL,
                                          failed_attempts   integer     NOT NULL DEFAULT 0 CHECK (failed_attempts >= 0),
                                          expires_at        timestamptz NOT NULL,
                                          last_sent_at      timestamptz NOT NULL DEFAULT now(),
                                          window_started_at timestamptz NOT NULL DEFAULT now(),
                                          send_count        integer     NOT NULL DEFAULT 1,
                                          day_started_at    timestamptz NOT NULL DEFAULT now(),
                                          day_count         integer     NOT NULL DEFAULT 1,
                                          created_at        timestamptz NOT NULL DEFAULT now(),
                                          CONSTRAINT uq_user_verification_tokens_identifier_purpose UNIQUE (identifier_id, purpose)
);

CREATE INDEX idx_user_verification_tokens_user_id ON user_verification_tokens (user_id);
CREATE INDEX idx_user_verification_tokens_user_purpose ON user_verification_tokens (user_id, purpose);
CREATE INDEX idx_user_verification_tokens_token_hash ON user_verification_tokens (token_hash);

CREATE TABLE user_sessions (
                               id                 uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                               user_id            uuid        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
                               refresh_token_hash text        NOT NULL UNIQUE,
                               user_agent         text,
                               device_type        text,
                               os_name            text,
                               browser            text,
                               ip_address         inet,
                               expires_at         timestamptz NOT NULL,
                               revoked_at         timestamptz,
                               created_at         timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_user_sessions_user_id ON user_sessions (user_id);