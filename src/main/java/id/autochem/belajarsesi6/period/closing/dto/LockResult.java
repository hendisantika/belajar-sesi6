package id.autochem.belajarsesi6.period.closing.dto;

import id.autochem.belajarsesi6.period.dto.PeriodResponse;

import java.math.BigDecimal;

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
public record LockResult(PeriodResponse period, int snapshotAccounts, BigDecimal totalDebitBalance,
                         BigDecimal totalCreditBalance, ClosingChecklist checklist) {
}
