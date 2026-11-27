package id.autochem.belajarsesi6.trialbalance;

import id.autochem.belajarsesi6.account.AccountType;
import id.autochem.belajarsesi6.account.NormalBalance;

import java.math.BigDecimal;

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
 * Satu baris neraca saldo. Dibentuk langsung oleh JPQL constructor expression
 * ({@code select new ...}) sehingga agregasi terjadi di database, bukan di memori Java.
 */
public record TrialBalanceLine(String code,
                               String name,
                               AccountType accountType,
                               NormalBalance normalBalance,
                               BigDecimal totalDebit,
                               BigDecimal totalCredit) {

    public TrialBalanceLine {
        totalDebit = totalDebit != null ? totalDebit : BigDecimal.ZERO;
        totalCredit = totalCredit != null ? totalCredit : BigDecimal.ZERO;
    }

    public static TrialBalanceLine zero(String code, String name, AccountType type, NormalBalance normalBalance) {
        return new TrialBalanceLine(code, name, type, normalBalance, BigDecimal.ZERO, BigDecimal.ZERO);
    }

    /** Saldo bertanda: debit - kredit. */
    public BigDecimal balance() {
        return totalDebit.subtract(totalCredit);
    }

    /** Kolom "Debit" neraca saldo: saldo bila bersisi debit. */
    public BigDecimal debitBalance() {
        return balance().signum() > 0 ? balance() : BigDecimal.ZERO.setScale(2);
    }

    /** Kolom "Kredit" neraca saldo: saldo bila bersisi kredit. */
    public BigDecimal creditBalance() {
        return balance().signum() < 0 ? balance().negate() : BigDecimal.ZERO.setScale(2);
    }

    /** Saldo dilihat dari sisi normal akun; negatif berarti saldo abnormal (mis. kas minus). */
    public BigDecimal normalSideBalance() {
        return normalBalance.toNormalSide(balance());
    }

    public boolean isAbnormal() {
        return normalSideBalance().signum() < 0;
    }
}
