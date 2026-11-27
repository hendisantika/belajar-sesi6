package id.autochem.belajarsesi6.generalledger;

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
public interface AccountBalanceRow {

    String getCode();

    String getName();

    String getAccountType();

    String getNormalBalance();

    BigDecimal getOpening();

    BigDecimal getDebit();

    BigDecimal getCredit();
}
