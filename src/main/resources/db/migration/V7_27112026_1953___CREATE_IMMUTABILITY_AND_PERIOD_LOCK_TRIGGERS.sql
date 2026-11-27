-- =====================================================================
-- Defense in depth di level database (aplikasi tetap memvalidasi lebih dulu):
--  * ledger_entry & audit_event append-only (tidak boleh UPDATE / DELETE)
--  * journal_line milik jurnal POSTED / REVERSED tidak boleh diubah / dihapus
--  * journal_entry POSTED hanya boleh berubah menjadi REVERSED; REVERSED final
--  * tidak ada ledger_entry / jurnal baru di periode LOCKED;
--    periode SOFT_CLOSED hanya menerima jurnal ADJUSTMENT / REVERSAL
-- =====================================================================

-- ------------------------------------------------------------ append-only
CREATE OR REPLACE FUNCTION fn_append_only() RETURNS TRIGGER AS
$$
BEGIN
    RAISE EXCEPTION '% bersifat append-only (operasi % ditolak)', TG_TABLE_NAME, TG_OP
        USING ERRCODE = 'restrict_violation';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_ledger_entry_append_only
    BEFORE UPDATE OR DELETE
    ON ledger_entry
    FOR EACH ROW
EXECUTE FUNCTION fn_append_only();

CREATE TRIGGER trg_audit_event_append_only
    BEFORE UPDATE OR DELETE
    ON audit_event
    FOR EACH ROW
EXECUTE FUNCTION fn_append_only();

CREATE TRIGGER trg_audit_event_no_truncate
    BEFORE TRUNCATE
    ON audit_event
    FOR EACH STATEMENT
EXECUTE FUNCTION fn_append_only();

-- ------------------------------------------------------------ journal line
CREATE OR REPLACE FUNCTION fn_journal_line_guard() RETURNS TRIGGER AS
$$
DECLARE
    v_status VARCHAR(10);
BEGIN
    SELECT status INTO v_status FROM journal_entry WHERE id = OLD.journal_id;
    IF v_status IN ('POSTED', 'REVERSED') THEN
        RAISE EXCEPTION 'Baris jurnal % milik jurnal % tidak boleh diubah', OLD.id, v_status
            USING ERRCODE = 'restrict_violation';
    END IF;
    IF TG_OP = 'DELETE' THEN
        RETURN OLD;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_journal_line_guard
    BEFORE UPDATE OR DELETE
    ON journal_line
    FOR EACH ROW
EXECUTE FUNCTION fn_journal_line_guard();

-- ------------------------------------------------------------ journal entry
CREATE OR REPLACE FUNCTION fn_journal_entry_guard() RETURNS TRIGGER AS
$$
BEGIN
    IF TG_OP = 'DELETE' THEN
        IF OLD.status IN ('POSTED', 'REVERSED') THEN
            RAISE EXCEPTION 'Jurnal % (%) tidak boleh dihapus', OLD.journal_number, OLD.status
                USING ERRCODE = 'restrict_violation';
        END IF;
        RETURN OLD;
    END IF;

    IF OLD.status = 'REVERSED' THEN
        RAISE EXCEPTION 'Jurnal % sudah REVERSED dan final', OLD.journal_number
            USING ERRCODE = 'restrict_violation';
    END IF;

    IF OLD.status = 'POSTED' AND (
        NEW.status <> 'REVERSED'
            OR NEW.journal_number IS DISTINCT FROM OLD.journal_number
            OR NEW.journal_date IS DISTINCT FROM OLD.journal_date
            OR NEW.description IS DISTINCT FROM OLD.description
            OR NEW.journal_type IS DISTINCT FROM OLD.journal_type
            OR NEW.auto_reverse IS DISTINCT FROM OLD.auto_reverse
            OR NEW.posted_by IS DISTINCT FROM OLD.posted_by
            OR NEW.posted_at IS DISTINCT FROM OLD.posted_at) THEN
        RAISE EXCEPTION 'Jurnal POSTED % immutable; koreksi lewat reversal / adjustment', OLD.journal_number
            USING ERRCODE = 'restrict_violation';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_journal_entry_guard
    BEFORE UPDATE OR DELETE
    ON journal_entry
    FOR EACH ROW
EXECUTE FUNCTION fn_journal_entry_guard();

-- ------------------------------------------------------------ period lock
CREATE OR REPLACE FUNCTION fn_period_status_of(p_date DATE) RETURNS VARCHAR AS
$$
SELECT status
FROM accounting_period
WHERE p_date BETWEEN start_date AND end_date;
$$ LANGUAGE sql STABLE;

-- Jurnal baru / perubahan tanggal jurnal ke periode LOCKED ditolak
CREATE OR REPLACE FUNCTION fn_journal_entry_period_guard() RETURNS TRIGGER AS
$$
BEGIN
    IF fn_period_status_of(NEW.journal_date) = 'LOCKED' THEN
        RAISE EXCEPTION 'Periode untuk tanggal % sudah LOCKED; jurnal % ditolak', NEW.journal_date, NEW.journal_number
            USING ERRCODE = 'restrict_violation';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_journal_entry_period_guard
    BEFORE INSERT OR UPDATE OF journal_date
    ON journal_entry
    FOR EACH ROW
EXECUTE FUNCTION fn_journal_entry_period_guard();

-- Posting ke buku besar hanya ke periode OPEN (atau SOFT_CLOSED untuk koreksi)
CREATE OR REPLACE FUNCTION fn_ledger_entry_period_guard() RETURNS TRIGGER AS
$$
DECLARE
    v_status VARCHAR(12);
    v_type   VARCHAR(12);
BEGIN
    v_status := fn_period_status_of(NEW.posting_date);
    IF v_status IS NULL THEN
        RAISE EXCEPTION 'Tidak ada periode akuntansi untuk tanggal %', NEW.posting_date
            USING ERRCODE = 'restrict_violation';
    END IF;
    IF v_status = 'LOCKED' THEN
        RAISE EXCEPTION 'Periode untuk tanggal % sudah LOCKED; posting ditolak', NEW.posting_date
            USING ERRCODE = 'restrict_violation';
    END IF;
    IF v_status = 'SOFT_CLOSED' THEN
        SELECT journal_type INTO v_type FROM journal_entry WHERE id = NEW.journal_id;
        IF v_type = 'GENERAL' THEN
            RAISE EXCEPTION 'Periode untuk tanggal % sudah cut-off; hanya jurnal ADJUSTMENT/REVERSAL', NEW.posting_date
                USING ERRCODE = 'restrict_violation';
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_ledger_entry_period_guard
    BEFORE INSERT
    ON ledger_entry
    FOR EACH ROW
EXECUTE FUNCTION fn_ledger_entry_period_guard();
