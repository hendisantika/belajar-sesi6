package id.autochem.belajarsesi6.generalledger;

import id.autochem.belajarsesi6.account.Account;
import id.autochem.belajarsesi6.account.AccountRepository;
import id.autochem.belajarsesi6.common.exception.BusinessException;
import id.autochem.belajarsesi6.common.exception.ErrorCode;
import id.autochem.belajarsesi6.common.exception.NotFoundException;
import id.autochem.belajarsesi6.generalledger.dto.GeneralLedgerReport;
import id.autochem.belajarsesi6.generalledger.dto.LedgerSummaryReport;
import id.autochem.belajarsesi6.generalledger.dto.LedgerVerificationReport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
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
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GeneralLedgerService {

    private final GeneralLedgerRepository repository;
    private final AccountRepository accounts;
    private final Clock clock;

    public GeneralLedgerReport ledger(String accountCode, LocalDate from, LocalDate to) {
        DateRange range = range(from, to);
        Account account = accounts.findByCode(accountCode)
                .orElseThrow(() -> new NotFoundException("Akun", accountCode));

        BigDecimal opening = repository.openingBalance(account.getId(), range.from());
        List<GeneralLedgerReport.Entry> entries = repository.ledger(account.getId(), range.from(), range.to(), opening)
                .stream()
                .map(r -> new GeneralLedgerReport.Entry(r.getLedgerEntryId(), r.getPostingDate(), r.getJournalId(),
                        r.getJournalNo(), r.getJournalStatus(), r.getDescription(), r.getDebit(), r.getCredit(),
                        r.getRunningBalance()))
                .toList();

        BigDecimal debit = sum(entries.stream().map(GeneralLedgerReport.Entry::debit).toList());
        BigDecimal credit = sum(entries.stream().map(GeneralLedgerReport.Entry::credit).toList());
        BigDecimal closing = opening.add(debit).subtract(credit);

        return new GeneralLedgerReport(
                new GeneralLedgerReport.AccountInfo(account.getCode(), account.getName(), account.getAccountType(),
                        account.getNormalBalance()),
                range.from(), range.to(), opening, debit, credit, closing,
                account.getNormalBalance().toNormalSide(closing), entries.size(), entries);
    }

    public LedgerSummaryReport summary(LocalDate from, LocalDate to) {
        DateRange range = range(from, to);
        List<LedgerSummaryReport.AccountBalance> rows = repository.balances(range.from(), range.to()).stream()
                .map(r -> new LedgerSummaryReport.AccountBalance(r.getCode(), r.getName(), r.getAccountType(),
                        r.getNormalBalance(), r.getOpening(), r.getDebit(), r.getCredit(),
                        r.getOpening().add(r.getDebit()).subtract(r.getCredit())))
                .toList();
        BigDecimal debit = sum(rows.stream().map(LedgerSummaryReport.AccountBalance::debit).toList());
        BigDecimal credit = sum(rows.stream().map(LedgerSummaryReport.AccountBalance::credit).toList());
        return new LedgerSummaryReport(range.from(), range.to(), debit, credit, debit.compareTo(credit) == 0, rows);
    }

    /** Deliverable: saldo GL cocok dengan posted lines dan tidak ada double posting. */
    public LedgerVerificationReport verify() {
        List<LedgerVerificationReport.AccountCheck> checks = repository.verification().stream()
                .map(r -> new LedgerVerificationReport.AccountCheck(r.getCode(), r.getLedgerDebit(),
                        r.getLedgerCredit(), r.getLineDebit(), r.getLineCredit(),
                        r.getLedgerDebit().compareTo(r.getLineDebit()) == 0
                                && r.getLedgerCredit().compareTo(r.getLineCredit()) == 0))
                .toList();
        long duplicates = repository.duplicatePostings();
        boolean matched = duplicates == 0 && checks.stream().allMatch(LedgerVerificationReport.AccountCheck::matched);
        return new LedgerVerificationReport(matched, duplicates, checks);
    }

    private DateRange range(LocalDate from, LocalDate to) {
        LocalDate today = LocalDate.now(clock);
        LocalDate start = from != null ? from : today.withDayOfMonth(1);
        LocalDate end = to != null ? to : start.withDayOfMonth(start.lengthOfMonth());
        if (end.isBefore(start)) {
            throw new BusinessException(ErrorCode.INVALID_DATE_RANGE,
                    "Tanggal akhir " + end + " sebelum tanggal awal " + start);
        }
        return new DateRange(start, end);
    }

    private static BigDecimal sum(List<BigDecimal> values) {
        return values.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private record DateRange(LocalDate from, LocalDate to) {
    }
}
