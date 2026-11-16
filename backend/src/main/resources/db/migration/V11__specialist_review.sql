ALTER TABLE specialists RENAME COLUMN verified_at TO reviewed_at;

ALTER TABLE specialists
    ADD COLUMN rejection_reason text,
    ADD COLUMN reviewed_by uuid REFERENCES users (id) ON DELETE SET NULL,
    ADD CONSTRAINT ck_specialists_rejection_reason
        CHECK ((verification_status = 'rejected') = (rejection_reason IS NOT NULL));