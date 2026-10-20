ALTER TABLE user_sessions
    ADD COLUMN previous_refresh_token_hash text,
    ADD COLUMN rotated_at timestamptz;

CREATE INDEX idx_user_sessions_previous_hash
    ON user_sessions (previous_refresh_token_hash)
    WHERE previous_refresh_token_hash IS NOT NULL;