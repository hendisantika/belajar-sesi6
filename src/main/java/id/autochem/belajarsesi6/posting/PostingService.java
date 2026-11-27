package id.autochem.belajarsesi6.posting;

import id.autochem.belajarsesi6.audit.AuditAction;
import id.autochem.belajarsesi6.audit.AuditTrail;
import id.autochem.belajarsesi6.audit.AuditedOperation;
import id.autochem.belajarsesi6.audit.EntityTypes;
import id.autochem.belajarsesi6.common.exception.BusinessException;
import id.autochem.belajarsesi6.common.exception.ErrorCode;
import id.autochem.belajarsesi6.common.exception.NotFoundException;
import id.autochem.belajarsesi6.journal.JournalEntry;
import id.autochem.belajarsesi6.journal.JournalEntryRepository;
import id.autochem.belajarsesi6.journal.JournalLine;
import id.autochem.belajarsesi6.journal.JournalStatus;
import id.autochem.belajarsesi6.ledger.LedgerEntry;
import id.autochem.belajarsesi6.ledger.LedgerEntryRepository;
import id.autochem.belajarsesi6.period.PeriodGuard;
import id.autochem.belajarsesi6.posting.dto.PostingResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

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
@Slf4j
@Service
@RequiredArgsConstructor
public class PostingService {

    private final JournalEntryRepository journals;
    private final LedgerEntryRepository ledger;
    private final PeriodGuard periodGuard;
    private final AuditTrail audit;
    private final Clock clock;

    /**
     * Posting jurnal ke buku besar dalam satu transaksi. Jika periode LOCKED, posting ditolak
     * dan penolakan tercatat di audit log (outcome REJECTED) lewat {@link AuditedOperation}.
     */
    @Transactional
    @AuditedOperation(action = AuditAction.JOURNAL_POSTED, entityType = EntityTypes.JOURNAL, entityId = "#journalId")
    public PostingResult post(Long journalId, String postedBy) {
        // 1. Lock (SELECT ... FOR UPDATE) agar jurnal tidak diposting dua kali secara bersamaan
        JournalEntry entry = journals.findByIdForUpdate(journalId)
                .orElseThrow(() -> new NotFoundException("Jurnal", journalId));
        Map<String, Object> before = entry.snapshot();

        // 2. Validasi berlapis: status -> tanggal -> periode -> seimbang -> akun aktif
        entry.getStatus().transitionTo(JournalStatus.POSTED);
        validatePostingRules(entry);

        // 3. Tulis buku besar + ubah status
        int written = writeToLedger(entry, postedBy, clock.instant());
        Map<String, Object> after = entry.snapshot();
        after.put("ledgerEntries", written);
        audit.success(AuditAction.JOURNAL_POSTED, EntityTypes.JOURNAL, entry.getId(), entry.getJournalNumber(),
                before, after);
        log.info("Journal {} posted by {} ({} ledger entries)", entry.getJournalNumber(), postedBy, written);
        return PostingResult.of(entry, written);
    }

    /** Posting rule: tanggal tidak di masa depan, periode terbuka (assertOpen), debit = kredit, akun aktif. */
    public void validatePostingRules(JournalEntry entry) {
        LocalDate today = LocalDate.now(clock);
        if (entry.getJournalDate().isAfter(today)) {
            throw new BusinessException(ErrorCode.FUTURE_DATED,
                    "Tanggal jurnal " + entry.getJournalDate() + " melewati hari ini (" + today + ")");
        }
        periodGuard.assertOpen(entry.getJournalDate(), entry.getJournalType());
        entry.assertBalanced();
        TreeSet<String> inactive = new TreeSet<>();
        for (JournalLine line : entry.getLines()) {
            if (!line.getAccount().isActive()) {
                inactive.add(line.getAccount().getCode() + " " + line.getAccount().getName());
            }
        }
        if (!inactive.isEmpty()) {
            throw new BusinessException(ErrorCode.ACCOUNT_INACTIVE,
                    "Jurnal " + entry.getJournalNumber() + " memakai akun nonaktif: " + inactive);
        }
    }

    /** Membuat satu LedgerEntry per baris jurnal; hanya setelah validatePostingRules lolos. */
    public int writeToLedger(JournalEntry entry, String postedBy, Instant now) {
        if (entry.getStatus() == JournalStatus.APPROVED) {
            entry.markPosted(postedBy, now);
        }
        List<LedgerEntry> rows = entry.getLines().stream()
                .map(line -> LedgerEntry.of(entry, line, now))
                .toList();
        ledger.saveAll(rows);
        return rows.size();
    }
}
