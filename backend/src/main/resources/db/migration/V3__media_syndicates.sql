-- V3: media and syndicates

CREATE TABLE media (
                       id                uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                       s3_key            text        NOT NULL UNIQUE,
                       original_filename text,
                       mime_type         text,
                       created_at        timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE syndicates (
                            id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                            name          text        NOT NULL,
                            specialty_id  uuid        NOT NULL REFERENCES specialties (id),
                            country_id    uuid        NOT NULL REFERENCES countries (id),
                            contact_email text        NOT NULL,
                            contact_phone text,
                            status        text        NOT NULL DEFAULT 'active',
                            created_at    timestamptz NOT NULL DEFAULT now(),
                            updated_at    timestamptz NOT NULL DEFAULT now(),
    -- one syndicate per specialty within a country
                            CONSTRAINT uq_syndicates_country_specialty UNIQUE (country_id, specialty_id),
    -- target of the composite FK on specialists, keeps specialty_id consistent with syndicate_id
                            CONSTRAINT uq_syndicates_id_specialty UNIQUE (id, specialty_id)
);

CREATE TRIGGER trg_syndicates_updated_at
    BEFORE UPDATE ON syndicates
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE syndicate_staff (
                                 id           uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                                 syndicate_id uuid        NOT NULL REFERENCES syndicates (id) ON DELETE CASCADE,
                                 user_id      uuid        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
                                 title        text,
                                 created_at   timestamptz NOT NULL DEFAULT now(),
                                 updated_at   timestamptz NOT NULL DEFAULT now(),
                                 CONSTRAINT uq_syndicate_staff_syndicate_user UNIQUE (syndicate_id, user_id)
);

CREATE INDEX idx_syndicate_staff_user_id ON syndicate_staff (user_id);

CREATE TRIGGER trg_syndicate_staff_updated_at
    BEFORE UPDATE ON syndicate_staff
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();