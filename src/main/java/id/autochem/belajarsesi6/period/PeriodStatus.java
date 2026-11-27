package id.autochem.belajarsesi6.period;

import id.autochem.belajarsesi6.common.exception.ErrorCode;
import id.autochem.belajarsesi6.common.exception.InvalidTransitionException;
import id.autochem.belajarsesi6.journal.JournalType;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by IntelliJ IDEA.
 * Project : belajar-sesi6
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Link : s.id/hendisantika
 * Date: 27/11/26
 * Time: 19.44
 */
public enum PeriodStatus {
    /** Periode berjalan: semua jurnal boleh diposting. */
    OPEN,
    /** Cut-off: transaksi harian ditutup, hanya jurnal penyesuaian / pembalik. */
    SOFT_CLOSED,
    /** Terkunci: tidak ada jurnal baru maupun perubahan. */
    LOCKED;

    /**
     * <pre>
     * OPEN        -> SOFT_CLOSED        (cut-off)
     * SOFT_CLOSED -> LOCKED | OPEN      (lock setelah checklist / reopen dengan alasan)
     * LOCKED      -> SOFT_CLOSED        (unlock dengan alasan; hanya koreksi yang boleh masuk)
     * </pre>
     */
    private static final Map<PeriodStatus, Set<PeriodStatus>> ALLOWED = Map.of(
            OPEN, EnumSet.of(SOFT_CLOSED),
            SOFT_CLOSED, EnumSet.of(LOCKED, OPEN),
            LOCKED, EnumSet.of(SOFT_CLOSED));

    public PeriodStatus transitionTo(PeriodStatus target) {
        if (!ALLOWED.get(this).contains(target)) {
            throw new InvalidTransitionException(ErrorCode.INVALID_PERIOD_TRANSITION, this, target);
        }
        return target;
    }

    public boolean allowsPosting(JournalType type) {
        return switch (this) {
            case OPEN -> true;
            case SOFT_CLOSED -> type.isCorrection();
            case LOCKED -> false;
        };
    }
}
