package id.autochem.belajarsesi6.period;

import id.autochem.belajarsesi6.common.exception.ErrorCode;
import id.autochem.belajarsesi6.common.exception.InvalidTransitionException;
import id.autochem.belajarsesi6.journal.JournalType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Created by IntelliJ IDEA.
 * Project : belajar-sesi6
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Link : s.id/hendisantika
 * Date: 27/11/26
 * Time: 19.56
 */
@DisplayName("PeriodStatus - aturan cut-off, lock & posting")
class PeriodStatusTest {

    @ParameterizedTest(name = "{0} menerima {1}? {2}")
    @CsvSource({
            "OPEN, GENERAL, true",
            "OPEN, ADJUSTMENT, true",
            "OPEN, REVERSAL, true",
            "SOFT_CLOSED, GENERAL, false",
            "SOFT_CLOSED, ADJUSTMENT, true",
            "SOFT_CLOSED, REVERSAL, true",
            "LOCKED, GENERAL, false",
            "LOCKED, ADJUSTMENT, false",
            "LOCKED, REVERSAL, false"})
    void postingRule(PeriodStatus status, JournalType type, boolean allowed) {
        assertThat(status.allowsPosting(type)).isEqualTo(allowed);
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({"OPEN, SOFT_CLOSED", "SOFT_CLOSED, LOCKED", "SOFT_CLOSED, OPEN", "LOCKED, SOFT_CLOSED"})
    void allowedTransitions(PeriodStatus from, PeriodStatus to) {
        assertThat(from.transitionTo(to)).isEqualTo(to);
    }

    @ParameterizedTest(name = "{0} -> {1} ditolak")
    @CsvSource({"OPEN, LOCKED", "LOCKED, OPEN", "OPEN, OPEN", "LOCKED, LOCKED"})
    void illegalTransitions(PeriodStatus from, PeriodStatus to) {
        assertThatThrownBy(() -> from.transitionTo(to))
                .isInstanceOf(InvalidTransitionException.class)
                .satisfies(ex -> assertThat(((InvalidTransitionException) ex).getCode())
                        .isEqualTo(ErrorCode.INVALID_PERIOD_TRANSITION));
    }

    @Test
    @DisplayName("Lock tidak bisa dilompati: OPEN harus cut-off dulu")
    void cannotLockWithoutCutOff() {
        assertThatThrownBy(() -> PeriodStatus.OPEN.transitionTo(PeriodStatus.LOCKED))
                .hasMessageContaining("OPEN -> LOCKED");
    }
}
