package id.autochem.belajarsesi6.generalledger.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
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
public record LedgerSummaryReport(LocalDate from, LocalDate to, BigDecimal totalDebit, BigDecimal totalCredit,
                                  boolean balanced, List<AccountBalance> accounts) {

    public record AccountBalance(String code, String name, String accountType, String normalBalance,
                                 BigDecimal openingBalance, BigDecimal debit, BigDecimal credit,
                                 BigDecimal closingBalance) {
    }
}
