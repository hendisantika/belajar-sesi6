package id.autochem.belajarsesi6.account;

import java.math.BigDecimal;

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
public enum NormalBalance {
    DEBIT,
    CREDIT;

    /**
     * Mengubah saldo bertanda (debit - kredit) menjadi saldo dilihat dari sisi normal akun.
     * Positif = saldo normal, negatif = saldo abnormal (mis. kas minus).
     */
    public BigDecimal toNormalSide(BigDecimal debitMinusCredit) {
        return this == DEBIT ? debitMinusCredit : debitMinusCredit.negate();
    }
}
