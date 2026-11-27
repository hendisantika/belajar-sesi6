package id.autochem.belajarsesi6.trialbalance;

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
public interface WorksheetRow {

    String getCode();

    String getName();

    String getAccountType();

    String getNormalBalance();

    BigDecimal getOpening();

    BigDecimal getDebit();

    BigDecimal getCredit();

    BigDecimal getAdjustmentDebit();

    BigDecimal getAdjustmentCredit();
}
