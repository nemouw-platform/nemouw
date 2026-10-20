-- V1: shared helpers and lookup tables
-- gen_random_uuid() is built into Postgres 13+, no extension needed.

CREATE FUNCTION set_updated_at() RETURNS trigger AS $$
BEGIN
    NEW.updated_at = now();
RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TABLE countries (
                           id            uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                           name          text        NOT NULL,
                           iso_code      text        NOT NULL UNIQUE,
                           currency_code text        NOT NULL,
                           phone_code    text        NOT NULL,
                           region        text,
                           is_active     boolean     NOT NULL DEFAULT true,
                           created_at    timestamptz NOT NULL DEFAULT now(),
                           updated_at    timestamptz NOT NULL DEFAULT now()
);

CREATE TRIGGER trg_countries_updated_at
    BEFORE UPDATE ON countries
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE user_roles (
                            id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                            code        text        NOT NULL UNIQUE,
                            name        text        NOT NULL,
                            description text,
                            created_at  timestamptz NOT NULL DEFAULT now()
);

INSERT INTO user_roles (code, name, description) VALUES
                                                     ('admin',           'Admin',           'Platform administrator'),
                                                     ('syndicate_staff', 'Syndicate staff', 'Staff member of a syndicate'),
                                                     ('specialist',      'Specialist',      'Allied health specialist'),
                                                     ('patient',         'Patient',         'Patient or family member');

CREATE TABLE specialties (
                             id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                             code        text        NOT NULL UNIQUE,
                             name        text        NOT NULL,
                             label       text        NOT NULL,
                             label_fr    text,
                             label_ar    text,
                             description text,
                             is_active   boolean     NOT NULL DEFAULT true,
                             created_at  timestamptz NOT NULL DEFAULT now(),
                             updated_at  timestamptz NOT NULL DEFAULT now()
);

CREATE TRIGGER trg_specialties_updated_at
    BEFORE UPDATE ON specialties
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();

CREATE TABLE conditions (
                            id          uuid PRIMARY KEY DEFAULT gen_random_uuid(),
                            code        text        NOT NULL UNIQUE,
                            name        text        NOT NULL,
                            label       text        NOT NULL,
                            label_fr    text,
                            label_ar    text,
                            description text,
                            is_active   boolean     NOT NULL DEFAULT true,
                            created_at  timestamptz NOT NULL DEFAULT now(),
                            updated_at  timestamptz NOT NULL DEFAULT now()
);

CREATE TRIGGER trg_conditions_updated_at
    BEFORE UPDATE ON conditions
    FOR EACH ROW EXECUTE FUNCTION set_updated_at();