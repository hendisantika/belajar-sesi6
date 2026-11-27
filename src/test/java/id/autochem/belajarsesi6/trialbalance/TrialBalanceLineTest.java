package id.autochem.belajarsesi6.trialbalance;

import id.autochem.belajarsesi6.account.AccountType;
import id.autochem.belajarsesi6.account.NormalBalance;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

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
@DisplayName("TrialBalanceLine - kolom debit/kredit & normal balance")
class TrialBalanceLineTest {

    @Test
    void assetWithDebitBalance() {
        TrialBalanceLine line = new TrialBalanceLine("1102", "Bank", AccountType.ASSET, NormalBalance.DEBIT,
                new BigDecimal("100.00"), new BigDecimal("24.00"));
        assertThat(line.debitBalance()).isEqualByComparingTo("76.00");
        assertThat(line.creditBalance()).isEqualByComparingTo("0");
        assertThat(line.normalSideBalance()).isEqualByComparingTo("76.00");
        assertThat(line.isAbnormal()).isFalse();
    }

    @Test
    void contraAssetHasCreditNormalBalance() {
        TrialBalanceLine line = new TrialBalanceLine("1502", "Akumulasi Penyusutan", AccountType.ASSET,
                NormalBalance.CREDIT, BigDecimal.ZERO, new BigDecimal("500.00"));
        assertThat(line.creditBalance()).isEqualByComparingTo("500.00");
        assertThat(line.normalSideBalance()).isEqualByComparingTo("500.00");
        assertThat(line.isAbnormal()).isFalse();
    }

    @Test
    @DisplayName("Kas bersaldo kredit = saldo abnormal")
    void abnormalBalance() {
        TrialBalanceLine line = new TrialBalanceLine("1101", "Kas", AccountType.ASSET, NormalBalance.DEBIT,
                new BigDecimal("10.00"), new BigDecimal("35.00"));
        assertThat(line.creditBalance()).isEqualByComparingTo("25.00");
        assertThat(line.normalSideBalance()).isEqualByComparingTo("-25.00");
        assertThat(line.isAbnormal()).isTrue();
    }

    @Test
    void nullSumsBecomeZero() {
        TrialBalanceLine line = new TrialBalanceLine("4101", "Penjualan", AccountType.REVENUE, NormalBalance.CREDIT,
                null, null);
        assertThat(line.balance()).isEqualByComparingTo("0");
    }
}
