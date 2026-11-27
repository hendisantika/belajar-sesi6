package id.autochem.belajarsesi6.generalledger.dto;

import java.math.BigDecimal;
import java.util.List;

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
public record LedgerVerificationReport(boolean matched, long duplicatePostings, List<AccountCheck> accounts) {

    public record AccountCheck(String code, BigDecimal ledgerDebit, BigDecimal ledgerCredit,
                               BigDecimal postedLineDebit, BigDecimal postedLineCredit, boolean matched) {
    }
}
