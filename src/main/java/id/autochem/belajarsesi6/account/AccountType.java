package id.autochem.belajarsesi6.account;

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
public enum AccountType {
    ASSET(NormalBalance.DEBIT),
    LIABILITY(NormalBalance.CREDIT),
    EQUITY(NormalBalance.CREDIT),
    REVENUE(NormalBalance.CREDIT),
    EXPENSE(NormalBalance.DEBIT);

    private final NormalBalance defaultNormalBalance;

    AccountType(NormalBalance defaultNormalBalance) {
        this.defaultNormalBalance = defaultNormalBalance;
    }

    public NormalBalance defaultNormalBalance() {
        return defaultNormalBalance;
    }
}
