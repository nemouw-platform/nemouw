-- V5: decouple signup from onboarding
-- Purge job: DELETE FROM users WHERE status = 'deleted' AND deleted_at < now() - interval '30 days';
-- then drain media_purge_queue, deleting each s3_key from object storage.

ALTER TABLE users DROP CONSTRAINT IF EXISTS users_status_check;
ALTER TABLE users
    ADD CONSTRAINT ck_users_status
        CHECK (status IN ('pending', 'active', 'suspended', 'deleted')),
    ADD CONSTRAINT ck_users_deleted_consistency
        CHECK ((status = 'deleted') = (deleted_at IS NOT NULL));

DROP INDEX IF EXISTS idx_users_purge;
CREATE INDEX idx_users_purge ON users (deleted_at) WHERE status = 'deleted';

CREATE TABLE media_purge_queue (
                                   id        bigint GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                   s3_key    text        NOT NULL,
                                   queued_at timestamptz NOT NULL DEFAULT now()
);

CREATE FUNCTION media_enqueue_s3_delete() RETURNS trigger AS $$
BEGIN
    INSERT INTO media_purge_queue (s3_key) VALUES (OLD.s3_key);
    RETURN OLD;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_media_enqueue_s3_delete
    AFTER DELETE ON media
    FOR EACH ROW EXECUTE FUNCTION media_enqueue_s3_delete();

CREATE FUNCTION specialists_release_media() RETURNS trigger AS $$
BEGIN
    IF TG_OP = 'DELETE' THEN
        DELETE FROM media WHERE id IN (OLD.profile_picture_id, OLD.cover_image_id);
        RETURN OLD;
    END IF;
    IF OLD.profile_picture_id IS NOT NULL
        AND NEW.profile_picture_id IS DISTINCT FROM OLD.profile_picture_id THEN
        DELETE FROM media WHERE id = OLD.profile_picture_id;
    END IF;
    IF OLD.cover_image_id IS NOT NULL
        AND NEW.cover_image_id IS DISTINCT FROM OLD.cover_image_id THEN
        DELETE FROM media WHERE id = OLD.cover_image_id;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_specialists_release_media
    AFTER DELETE OR UPDATE OF profile_picture_id, cover_image_id ON specialists
    FOR EACH ROW EXECUTE FUNCTION specialists_release_media();

CREATE FUNCTION specialist_credentials_release_media() RETURNS trigger AS $$
BEGIN
    IF TG_OP = 'DELETE' THEN
        IF OLD.document_id IS NOT NULL THEN
            DELETE FROM media WHERE id = OLD.document_id;
        END IF;
        RETURN OLD;
    END IF;
    IF OLD.document_id IS NOT NULL AND NEW.document_id IS DISTINCT FROM OLD.document_id THEN
        DELETE FROM media WHERE id = OLD.document_id;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_specialist_credentials_release_media
    AFTER DELETE OR UPDATE OF document_id ON specialist_credentials
    FOR EACH ROW EXECUTE FUNCTION specialist_credentials_release_media();

ALTER TABLE specialists
    ADD CONSTRAINT uq_specialists_profile_picture UNIQUE (profile_picture_id),
    ADD CONSTRAINT uq_specialists_cover_image UNIQUE (cover_image_id),
    ADD CONSTRAINT ck_specialists_picture_cover_differ CHECK (profile_picture_id <> cover_image_id);
ALTER TABLE specialist_credentials
    ADD CONSTRAINT uq_specialist_credentials_document UNIQUE (document_id);

-- Unaffiliated rows from the old signup-time flow count as "not onboarded"; the users stay.
DELETE FROM specialists WHERE syndicate_id IS NULL;

ALTER TABLE specialists
    DROP CONSTRAINT ck_specialists_affiliation_all_or_none,
    DROP CONSTRAINT ck_specialists_verified_needs_syndicate;

ALTER TABLE specialists
    ALTER COLUMN syndicate_id            SET NOT NULL,
    ALTER COLUMN specialty_id            SET NOT NULL,
    ALTER COLUMN syndicate_member_number SET NOT NULL;

ALTER TABLE specialists
    ADD CONSTRAINT ck_specialists_username_format
        CHECK (username ~ '^[a-z0-9._-]{3,30}$'),
    ADD CONSTRAINT ck_specialists_username_not_reserved
        CHECK (username NOT IN ('me', 'by-id'));

CREATE OR REPLACE FUNCTION specialists_lock_syndicate() RETURNS trigger AS $$
BEGIN
    IF NEW.syndicate_id IS DISTINCT FROM OLD.syndicate_id
        AND (OLD.verification_status = 'active' OR NEW.verification_status = 'active') THEN
        RAISE EXCEPTION 'syndicate_id cannot change once verified' USING ERRCODE = 'check_violation';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE FUNCTION specialists_derive_specialty() RETURNS trigger AS $$
BEGIN
    SELECT specialty_id INTO NEW.specialty_id FROM syndicates WHERE id = NEW.syndicate_id;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_specialists_derive_specialty
    BEFORE INSERT OR UPDATE OF syndicate_id ON specialists
    FOR EACH ROW EXECUTE FUNCTION specialists_derive_specialty();

CREATE FUNCTION specialists_sync_username_identifier() RETURNS trigger AS $$
BEGIN
    UPDATE user_identifiers
    SET identifier = NEW.username
    WHERE user_id = NEW.user_id AND identifier_type = 'username';
    IF NOT FOUND THEN
        INSERT INTO user_identifiers (user_id, identifier_type, identifier, is_primary, verified_at)
        VALUES (NEW.user_id, 'username', NEW.username, true, now());
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_specialists_sync_username
    AFTER INSERT OR UPDATE OF username ON specialists
    FOR EACH ROW EXECUTE FUNCTION specialists_sync_username_identifier();

INSERT INTO user_identifiers (user_id, identifier_type, identifier, is_primary, verified_at)
SELECT s.user_id, 'username', s.username, true, now()
FROM specialists s
WHERE NOT EXISTS (SELECT 1 FROM user_identifiers ui
                  WHERE ui.user_id = s.user_id AND ui.identifier_type = 'username')
ON CONFLICT DO NOTHING;