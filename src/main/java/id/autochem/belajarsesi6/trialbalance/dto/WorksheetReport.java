package id.autochem.belajarsesi6.trialbalance.dto;

import id.autochem.belajarsesi6.period.PeriodStatus;

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
 * Time: 19.46
 */
/**
 * Kertas kerja periode: Neraca Saldo sebelum penyesuaian -> Penyesuaian -> Neraca Saldo setelah penyesuaian.
 */
public record WorksheetReport(
        String period,
        LocalDate from,
        LocalDate to,
        PeriodStatus periodStatus,
        List<Row> rows,
        Columns totals,
        boolean unadjustedBalanced,
        boolean adjustedBalanced,
        /* Laba (rugi) periode dari neraca saldo setelah penyesuaian: pendapatan - beban. */
        BigDecimal periodNetIncome) {

    public record Row(String code, String name, String accountType, String normalBalance,
                      BigDecimal openingBalance,
                      DebitCredit movement,
                      DebitCredit unadjusted,
                      DebitCredit adjustment,
                      DebitCredit adjusted) {
    }

    public record DebitCredit(BigDecimal debit, BigDecimal credit) {

        public static DebitCredit ofBalance(BigDecimal balance) {
            return balance.signum() >= 0
                    ? new DebitCredit(balance, BigDecimal.ZERO.setScale(2))
                    : new DebitCredit(BigDecimal.ZERO.setScale(2), balance.negate());
        }

        public DebitCredit plus(DebitCredit other) {
            return new DebitCredit(debit.add(other.debit), credit.add(other.credit));
        }

        public static DebitCredit zero() {
            return new DebitCredit(BigDecimal.ZERO.setScale(2), BigDecimal.ZERO.setScale(2));
        }

        public boolean balanced() {
            return debit.compareTo(credit) == 0;
        }
    }

    public record Columns(DebitCredit movement, DebitCredit unadjusted, DebitCredit adjustment,
                          DebitCredit adjusted) {
    }
}
