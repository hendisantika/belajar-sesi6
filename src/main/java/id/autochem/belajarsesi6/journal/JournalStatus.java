package id.autochem.belajarsesi6.journal;

import id.autochem.belajarsesi6.common.exception.ErrorCode;
import id.autochem.belajarsesi6.common.exception.InvalidTransitionException;

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
public enum JournalStatus {
    DRAFT,
    APPROVED,
    POSTED,
    REVERSED;

    /**
     * <pre>
     * DRAFT    -> APPROVED          (approve)
     * APPROVED -> POSTED | DRAFT    (post / dikembalikan untuk revisi)
     * POSTED   -> REVERSED          (reverse)
     * REVERSED -> (final)
     * </pre>
     */
    private static final Map<JournalStatus, Set<JournalStatus>> ALLOWED = Map.of(
            DRAFT, EnumSet.of(APPROVED),
            APPROVED, EnumSet.of(POSTED, DRAFT),
            POSTED, EnumSet.of(REVERSED),
            REVERSED, EnumSet.noneOf(JournalStatus.class));

    public boolean canTransitionTo(JournalStatus target) {
        return ALLOWED.get(this).contains(target);
    }

    public JournalStatus transitionTo(JournalStatus target) {
        if (!canTransitionTo(target)) {
            throw new InvalidTransitionException(ErrorCode.INVALID_STATUS_TRANSITION, this, target);
        }
        return target;
    }

    public Set<JournalStatus> allowedTransitions() {
        Set<JournalStatus> targets = ALLOWED.get(this);
        return targets.isEmpty() ? EnumSet.noneOf(JournalStatus.class) : EnumSet.copyOf(targets);
    }

    public boolean isEditable() {
        return this == DRAFT;
    }

    /** Jurnal yang mutasinya sudah tercatat di buku besar (ikut trial balance). */
    public boolean isInLedger() {
        return this == POSTED || this == REVERSED;
    }

    public static Set<JournalStatus> inLedger() {
        return EnumSet.of(POSTED, REVERSED);
    }
}
