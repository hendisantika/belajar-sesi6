package id.autochem.belajarsesi6.period.dto;

import id.autochem.belajarsesi6.period.AccountingPeriod;
import id.autochem.belajarsesi6.period.PeriodStatus;

import java.time.Instant;
import java.time.LocalDate;

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
public record PeriodResponse(String code, LocalDate startDate, LocalDate endDate, PeriodStatus status,
                             String cutoffBy, Instant cutoffAt, String lockedBy, Instant lockedAt,
                             String unlockedBy, Instant unlockedAt, String unlockReason, Long version) {

    public static PeriodResponse from(AccountingPeriod p) {
        return new PeriodResponse(p.getCode(), p.getStartDate(), p.getEndDate(), p.getStatus(),
                p.getCutoffBy(), p.getCutoffAt(), p.getLockedBy(), p.getLockedAt(),
                p.getUnlockedBy(), p.getUnlockedAt(), p.getUnlockReason(), p.getVersion());
    }
}
