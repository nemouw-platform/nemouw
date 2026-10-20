-- V4: specialists and their child tables
-- A specialist row is created right after signup OTP verification with the affiliation
-- (syndicate, specialty, member number) empty; the first onboarding step fills all three together.

CREATE TABLE specialists (
                             id                      uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                             user_id                 uuid        NOT NULL UNIQUE REFERENCES users (id) ON DELETE CASCADE,

    -- affiliation: all three set together, or all three empty
                             syndicate_id            uuid,
                             specialty_id            uuid,
                             syndicate_member_number text,

                             username                text        NOT NULL UNIQUE,
                             profile_picture_id      uuid        REFERENCES media (id) ON DELETE SET NULL,
                             cover_image_id          uuid        REFERENCES media (id) ON DELETE SET NULL,
                             gender                  text,
                             years_of_experience     integer     CHECK (years_of_experience >= 0),
                             whatsapp_number         text,
                             primary_language        text,
                             min_age_treated         integer     CHECK (min_age_treated >= 0),
                             max_age_treated         integer     CHECK (max_age_treated >= 0),

                             verification_status     text        NOT NULL DEFAULT 'pending'
                                 CHECK (verification_status IN ('pending', 'active', 'rejected')),
                             verified_at             timestamptz,

                             created_at              timestamptz NOT NULL DEFAULT now(),
                             updated_at              timestamptz NOT NULL DEFAULT now(),
                             deleted_at              timestamptz,

    -- specialty_id must match the syndicate's specialty
                             CONSTRAINT fk_specialists_syndicate_specialty
                                 FOREIGN KEY (syndicate_id, specialty_id) REFERENCES syndicates (id, specialty_id),
                             CONSTRAINT ck_specialists_affiliation_all_or_none
                                 CHECK ((syndicate_id IS NULL) = (specialty_id IS NULL)
                                     AND (syndicate_id IS NULL) = (syndicate_member_number IS NULL)),
    -- one member number per syndicate
                             CONSTRAINT uq_specialists_syndicate_member UNIQUE (syndicate_id, syndicate_member_number),
    -- cannot be verified before choosing a syndicate
                             CONSTRAINT ck_specialists_verified_needs_syndicate
                                 CHECK (verification_status <> 'active' OR syndicate_id IS NOT NULL),
                             CONSTRAINT ck_specialists_age_range
                                 CHECK (min_age_treated IS NULL OR max_age_treated IS NULL OR min_age_treated <= max_age_treated)
);

CREATE INDEX idx_specialists_syndicate_id ON specialists (syndicate_id);

CREATE TRIGGER trg_specialists_updated_at
    BEFORE UPDATE ON specialists
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

-- syndicate_id can be set once and never changed afterward
CREATE FUNCTION specialists_lock_syndicate() RETURNS trigger AS $$
BEGIN
    IF OLD.syndicate_id IS NOT NULL AND NEW.syndicate_id IS DISTINCT FROM OLD.syndicate_id THEN
        RAISE EXCEPTION 'syndicate_id is immutable once set' USING ERRCODE = 'check_violation';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_specialists_lock_syndicate
    BEFORE UPDATE OF syndicate_id ON specialists
    FOR EACH ROW EXECUTE FUNCTION specialists_lock_syndicate();

CREATE TABLE specialist_profile (
                                    id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                                    specialist_id uuid        NOT NULL REFERENCES specialists (id) ON DELETE CASCADE,
                                    locale_code   text        NOT NULL CHECK (locale_code IN ('en', 'ar', 'fr')),
                                    title         text,
                                    headline      text,
                                    bio           text,
                                    created_at    timestamptz NOT NULL DEFAULT now(),
                                    updated_at    timestamptz NOT NULL DEFAULT now(),
                                    CONSTRAINT uq_specialist_profile_locale UNIQUE (specialist_id, locale_code)
);

CREATE TRIGGER trg_specialist_profile_updated_at
    BEFORE UPDATE ON specialist_profile
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE specialist_locations (
                                      id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                                      specialist_id uuid          NOT NULL REFERENCES specialists (id) ON DELETE CASCADE,
                                      country_id    uuid          NOT NULL REFERENCES countries (id),
                                      label         text,
                                      address_line1 text,
                                      address_line2 text,
                                      city          text          NOT NULL,
                                      latitude      numeric(9, 6) CHECK (latitude BETWEEN -90 AND 90),
                                      longitude     numeric(9, 6) CHECK (longitude BETWEEN -180 AND 180),
                                      phone         text,
                                      is_primary    boolean       NOT NULL DEFAULT false,
                                      created_at    timestamptz   NOT NULL DEFAULT now(),
                                      updated_at    timestamptz   NOT NULL DEFAULT now(),
    -- target of the composite FK on availabilities
                                      CONSTRAINT uq_specialist_locations_id_specialist UNIQUE (id, specialist_id)
);

CREATE INDEX idx_specialist_locations_specialist_id ON specialist_locations (specialist_id);
CREATE UNIQUE INDEX uq_specialist_locations_one_primary
    ON specialist_locations (specialist_id) WHERE is_primary;

CREATE TRIGGER trg_specialist_locations_updated_at
    BEFORE UPDATE ON specialist_locations
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE specialist_location_availabilities (
                                                    id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                                                    specialist_id uuid        NOT NULL REFERENCES specialists (id) ON DELETE CASCADE,
    -- NULL means a general window not tied to a location
                                                    location_id   uuid,
                                                    day_of_week   smallint    NOT NULL CHECK (day_of_week BETWEEN 0 AND 6),
                                                    start_time    time        NOT NULL,
                                                    end_time      time        NOT NULL,
                                                    created_at    timestamptz NOT NULL DEFAULT now(),
                                                    updated_at    timestamptz NOT NULL DEFAULT now(),
                                                    CONSTRAINT ck_availability_time_order CHECK (end_time > start_time),
    -- the location must belong to the same specialist; deleting a location removes its windows
                                                    CONSTRAINT fk_availability_location_owner
                                                        FOREIGN KEY (location_id, specialist_id)
                                                            REFERENCES specialist_locations (id, specialist_id) ON DELETE CASCADE
);

CREATE INDEX idx_availabilities_specialist_id ON specialist_location_availabilities (specialist_id);

CREATE TRIGGER trg_availabilities_updated_at
    BEFORE UPDATE ON specialist_location_availabilities
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE specialist_credentials (
                                        id                   uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                                        specialist_id        uuid        NOT NULL REFERENCES specialists (id) ON DELETE CASCADE,
                                        credential_type      text        NOT NULL
                                            CHECK (credential_type IN ('degree', 'license', 'certification', 'membership')),
                                        title                text        NOT NULL,
                                        issuing_organization text        NOT NULL,
                                        credential_number    text,
                                        issue_date           date,
                                        expiry_date          date,
                                        document_id          uuid        REFERENCES media (id) ON DELETE SET NULL,
                                        verification_status  text        NOT NULL DEFAULT 'pending'
                                            CHECK (verification_status IN ('pending', 'verified', 'rejected')),
                                        verified_by          uuid        REFERENCES users (id) ON DELETE SET NULL,
                                        verified_at          timestamptz,
                                        rejection_reason     text,
                                        created_at           timestamptz NOT NULL DEFAULT now(),
                                        updated_at           timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX idx_specialist_credentials_specialist_id ON specialist_credentials (specialist_id);

CREATE TRIGGER trg_specialist_credentials_updated_at
    BEFORE UPDATE ON specialist_credentials
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE specialist_template (
                                     id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                                     specialist_id uuid        NOT NULL UNIQUE REFERENCES specialists (id) ON DELETE CASCADE,
                                     content       jsonb       NOT NULL DEFAULT '{}'::jsonb,
                                     primary_color text,
                                     created_at    timestamptz NOT NULL DEFAULT now(),
                                     updated_at    timestamptz NOT NULL DEFAULT now()
);

CREATE TRIGGER trg_specialist_template_updated_at
    BEFORE UPDATE ON specialist_template
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE specialist_treated_conditions (
                                               id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                                               specialist_id uuid        NOT NULL REFERENCES specialists (id) ON DELETE CASCADE,
    -- RESTRICT: a condition in use cannot be deleted (the API will need a 409 for it)
                                               condition_id  uuid        NOT NULL REFERENCES conditions (id),
                                               created_at    timestamptz NOT NULL DEFAULT now(),
                                               CONSTRAINT uq_specialist_treated_conditions UNIQUE (specialist_id, condition_id)
);

CREATE INDEX idx_specialist_treated_conditions_condition_id ON specialist_treated_conditions (condition_id);