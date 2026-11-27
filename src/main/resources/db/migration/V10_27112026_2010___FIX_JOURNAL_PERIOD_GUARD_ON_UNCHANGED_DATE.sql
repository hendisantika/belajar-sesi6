-- =====================================================================
-- Perbaikan trigger period guard journal_entry.
-- "UPDATE OF journal_date" tetap terpicu bila kolom ada di SET walau nilainya sama
-- (Hibernate menulis semua kolom). Akibatnya jurnal di periode LOCKED tidak bisa
-- ditandai REVERSED padahal jurnal pembaliknya bertanggal di periode terbuka.
-- Sekarang hanya INSERT atau perubahan tanggal yang benar-benar berbeda yang dicek.
-- =====================================================================

CREATE OR REPLACE FUNCTION fn_journal_entry_period_guard() RETURNS TRIGGER AS
$$
BEGIN
    IF TG_OP = 'UPDATE' AND NEW.journal_date IS NOT DISTINCT FROM OLD.journal_date THEN
        RETURN NEW;
    END IF;
    IF fn_period_status_of(NEW.journal_date) = 'LOCKED' THEN
        RAISE EXCEPTION 'Periode untuk tanggal % sudah LOCKED; jurnal % ditolak', NEW.journal_date, NEW.journal_number
            USING ERRCODE = 'restrict_violation';
    END IF;
    IF TG_OP = 'UPDATE' AND fn_period_status_of(OLD.journal_date) = 'LOCKED' THEN
        RAISE EXCEPTION 'Jurnal % berada di periode LOCKED; tanggal tidak boleh dipindah', OLD.journal_number
            USING ERRCODE = 'restrict_violation';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;
