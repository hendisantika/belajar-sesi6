package id.autochem.belajarsesi6.generalledger.dto;

import id.autochem.belajarsesi6.account.AccountType;
import id.autochem.belajarsesi6.account.NormalBalance;

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
public record GeneralLedgerReport(
        AccountInfo account,
        LocalDate from,
        LocalDate to,
        BigDecimal openingBalance,
        BigDecimal totalDebit,
        BigDecimal totalCredit,
        BigDecimal closingBalance,
        /* Saldo akhir dilihat dari sisi normal akun (positif = saldo normal). */
        BigDecimal closingBalanceNormalSide,
        int entryCount,
        List<Entry> entries) {

    public record AccountInfo(String code, String name, AccountType type, NormalBalance normalBalance) {
    }

    public record Entry(Long ledgerEntryId, LocalDate postingDate, Long journalId, String journalNo,
                        String journalStatus, String description, BigDecimal debit, BigDecimal credit,
                        BigDecimal runningBalance) {
    }
}
