package id.autochem.belajarsesi6.period.closing.dto;

import id.autochem.belajarsesi6.period.closing.PeriodBalance;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Created by IntelliJ IDEA.
 * Project : belajar-sesi6
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Link : s.id/hendisantika
 * Date: 27/11/26
 * Time: 19.48
 */
public record PeriodBalanceResponse(String accountCode, String accountName, BigDecimal openingBalance,
                                    BigDecimal debit, BigDecimal credit, BigDecimal closingBalance,
                                    String snapshotBy, Instant snapshotAt) {

    public static PeriodBalanceResponse from(PeriodBalance b) {
        return new PeriodBalanceResponse(b.getAccount().getCode(), b.getAccount().getName(), b.getOpeningBalance(),
                b.getDebit(), b.getCredit(), b.getClosingBalance(), b.getSnapshotBy(), b.getSnapshotAt());
    }
}
