package id.autochem.belajarsesi6.generalledger;

import java.math.BigDecimal;
import java.time.LocalDate;

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
public interface GeneralLedgerRow {

    Long getLedgerEntryId();

    LocalDate getPostingDate();

    Long getJournalId();

    String getJournalNo();

    String getJournalStatus();

    String getDescription();

    BigDecimal getDebit();

    BigDecimal getCredit();

    BigDecimal getRunningBalance();
}
