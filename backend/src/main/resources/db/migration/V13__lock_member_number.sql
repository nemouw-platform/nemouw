CREATE FUNCTION specialists_lock_member_number() RETURNS trigger AS $$
BEGIN
    IF NEW.syndicate_member_number IS DISTINCT FROM OLD.syndicate_member_number
        AND (OLD.verification_status = 'active' OR NEW.verification_status = 'active') THEN
        RAISE EXCEPTION 'syndicate_member_number cannot change once verified' USING ERRCODE = 'check_violation';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_specialists_lock_member_number
    BEFORE UPDATE OF syndicate_member_number ON specialists
    FOR EACH ROW EXECUTE FUNCTION specialists_lock_member_number();