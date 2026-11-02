
-- Location and availability rules, applied on top of V4/V8.
--
-- Rules:
--   * every specialist with locations has exactly one primary location
--   * the same coordinates cannot be saved twice for one specialist
--   * availabilities.day_of_week uses day names ('monday'...'sunday')

-- ---------------------------------------------------------------------
-- 1. Location constraints
-- ---------------------------------------------------------------------

-- Prevent duplicate coordinates for the same specialist.
-- Locations without coordinates are not compared.
CREATE UNIQUE INDEX uq_specialist_locations_coordinates
    ON specialist_locations (specialist_id, latitude, longitude)
    WHERE latitude IS NOT NULL;

-- V4 already enforces at most one primary location.
-- This additionally requires at least one primary when locations exist.
-- Deferred to commit to allow changing the primary within a transaction.
CREATE FUNCTION enforce_one_primary_location() RETURNS trigger AS
$$
DECLARE
    owner_id  uuid;
    total     integer;
    primaries integer;
BEGIN
    IF TG_OP = 'DELETE' THEN
        owner_id := OLD.specialist_id;
    ELSE
        owner_id := NEW.specialist_id;
    END IF;

    SELECT count(*), count(*) FILTER (WHERE is_primary)
    INTO total, primaries
    FROM specialist_locations
    WHERE specialist_id = owner_id;

    IF total > 0 AND primaries <> 1 THEN
        RAISE EXCEPTION
            'specialist % must have exactly one primary location, found %',
            owner_id, primaries
            USING ERRCODE = 'check_violation',
                CONSTRAINT = 'ck_specialist_locations_exactly_one_primary';
    END IF;

    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE CONSTRAINT TRIGGER trg_specialist_locations_exactly_one_primary
    AFTER INSERT OR UPDATE OR DELETE ON specialist_locations
    DEFERRABLE INITIALLY DEFERRED
    FOR EACH ROW
EXECUTE FUNCTION enforce_one_primary_location();

-- ---------------------------------------------------------------------
-- 2. Availabilities: day_of_week becomes a day name
-- ---------------------------------------------------------------------

-- Drop the old numeric day_of_week check constraint.
DO
$$
    DECLARE
        c text;
    BEGIN
        FOR c IN
            SELECT conname
            FROM pg_constraint
            WHERE conrelid = 'specialist_location_availabilities'::regclass
              AND contype = 'c'
              AND pg_get_constraintdef(oid) LIKE '%day_of_week%'
            LOOP
                EXECUTE format(
                        'ALTER TABLE specialist_location_availabilities DROP CONSTRAINT %I',
                        c
                        );
            END LOOP;
    END;
$$;

-- No data conversion is needed because the table is empty.
ALTER TABLE specialist_location_availabilities
    ALTER COLUMN day_of_week TYPE text
        USING day_of_week::text;

ALTER TABLE specialist_location_availabilities
    ADD CONSTRAINT ck_availability_day_of_week
        CHECK (day_of_week IN (
                               'monday', 'tuesday', 'wednesday', 'thursday',
                               'friday', 'saturday', 'sunday'
            ));
