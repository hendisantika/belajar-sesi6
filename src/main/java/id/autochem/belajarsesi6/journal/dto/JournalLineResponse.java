package id.autochem.belajarsesi6.journal.dto;

import id.autochem.belajarsesi6.journal.JournalLine;

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
public record JournalLineResponse(int lineNo, String accountCode, String accountName, String description,
                                  BigDecimal debit, BigDecimal credit) {

    public static JournalLineResponse from(JournalLine l) {
        return new JournalLineResponse(l.getLineNo(), l.getAccount().getCode(), l.getAccount().getName(),
                l.getDescription(), l.getDebit(), l.getCredit());
    }
}
