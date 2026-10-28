-- V7: mark the owner (the first staff account) of each syndicate
ALTER TABLE syndicate_staff ADD COLUMN is_owner boolean NOT NULL DEFAULT false;
CREATE UNIQUE INDEX uq_syndicate_staff_one_owner ON syndicate_staff (syndicate_id) WHERE is_owner;