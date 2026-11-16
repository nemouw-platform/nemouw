-- V12: public discovery: search normalization and session pricing

CREATE EXTENSION IF NOT EXISTS unaccent SCHEMA public;
CREATE EXTENSION IF NOT EXISTS pg_trgm SCHEMA public;

-- Folds case, accents and Arabic letter variants; applied to both sides of every search comparison.
-- unaccent() is STABLE; declared IMMUTABLE so the function can back an index later.
-- Schema-qualified for Postgres 17/18 restricted search_path during ANALYZE, REINDEX, and autovacuum.
CREATE FUNCTION public.search_normalize(value text) RETURNS text AS
$$
SELECT translate(
               regexp_replace(
                       lower(public.unaccent('public.unaccent'::regdictionary, value)),
                       U&'[\064B-\065F\0640]',
                       '',
                       'g'
               ),
               U&'\0623\0625\0622\0671\0649\0629',
               U&'\0627\0627\0627\0627\064A\0647'
       )
$$ LANGUAGE sql IMMUTABLE
                PARALLEL SAFE
                STRICT;

ALTER TABLE specialists
    ADD COLUMN session_price_min numeric(10, 2) CHECK (session_price_min >= 0),
    ADD COLUMN session_price_max numeric(10, 2),
    ADD COLUMN price_currency    text NOT NULL DEFAULT 'USD',
    ADD CONSTRAINT ck_specialists_price_range
        CHECK (session_price_max IS NULL
            OR (session_price_min IS NOT NULL AND session_price_max >= session_price_min)),
    ADD CONSTRAINT ck_specialists_price_currency
        CHECK (price_currency IN ('USD'));

-- Closed value sets. Free text already stored is normalized; anything unrecognized is cleared.
UPDATE specialists
SET gender = CASE WHEN lower(btrim(gender)) IN ('male', 'female') THEN lower(btrim(gender)) END
WHERE gender IS NOT NULL;

UPDATE specialists
SET primary_language = CASE
                           WHEN lower(btrim(primary_language)) IN ('en', 'ar', 'fr')
                               THEN lower(btrim(primary_language)) END
WHERE primary_language IS NOT NULL;

ALTER TABLE specialists
    ADD CONSTRAINT ck_specialists_gender CHECK (gender IN ('male', 'female')),
    ADD CONSTRAINT ck_specialists_primary_language CHECK (primary_language IN ('en', 'ar', 'fr'));

CREATE INDEX idx_specialists_discoverable
    ON specialists (specialty_id)
    WHERE verification_status = 'active' AND deleted_at IS NULL;
