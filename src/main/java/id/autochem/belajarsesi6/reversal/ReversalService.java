package id.autochem.belajarsesi6.reversal;

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
import id.autochem.belajarsesi6.journal.JournalNumberGenerator;
import id.autochem.belajarsesi6.journal.JournalStatus;
import id.autochem.belajarsesi6.journal.JournalType;
import id.autochem.belajarsesi6.posting.PostingService;
import id.autochem.belajarsesi6.reversal.dto.ReversalResult;
import id.autochem.belajarsesi6.reversal.dto.ReverseRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

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
public class ReversalService {

    private final JournalEntryRepository journals;
    private final JournalNumberGenerator numberGenerator;
    private final PostingService postingService;
    private final AuditTrail audit;
    private final Clock clock;

    /**
     * Membalik jurnal POSTED. Jurnal asli TIDAK diubah isinya (hanya status -> REVERSED);
     * koreksi selalu lewat jurnal baru (tipe REVERSAL) dengan debit &amp; kredit ditukar,
     * bertanggal di periode yang masih terbuka. Jurnal asal di periode LOCKED tetap bisa
     * dikoreksi selama tanggal reversal jatuh di periode terbuka.
     */
    @Transactional
    @AuditedOperation(action = AuditAction.JOURNAL_REVERSED, entityType = EntityTypes.JOURNAL, entityId = "#journalId")
    public ReversalResult reverse(Long journalId, ReverseRequest request, String actor) {
        JournalEntry original = journals.findByIdForUpdate(journalId)
                .orElseThrow(() -> new NotFoundException("Jurnal", journalId));
        Map<String, Object> before = original.snapshot();
        original.getStatus().transitionTo(JournalStatus.REVERSED);

        LocalDate reversalDate = request.reversalDate() != null ? request.reversalDate() : LocalDate.now(clock);
        if (reversalDate.isBefore(original.getJournalDate())) {
            throw new BusinessException(ErrorCode.INVALID_REVERSAL_DATE,
                    "Tanggal reversal " + reversalDate + " sebelum tanggal jurnal asal " + original.getJournalDate());
        }

        Instant now = clock.instant();
        JournalEntry reversal = JournalEntry.draft(numberGenerator.reversalOf(original.getJournalNumber()),
                reversalDate, "Reversal " + original.getJournalNumber() + ": " + request.reason(),
                JournalType.REVERSAL, false);
        reversal.linkReversalOf(original, request.reason());               // jejak referensi ke jurnal asal
        for (JournalLine line : original.getLines()) {
            reversal.addLine(line.reversed());                               // debit & kredit ditukar
        }
        postingService.validatePostingRules(reversal);                      // termasuk assertOpen()
        reversal.approveAndPost(actor, now);
        journals.save(reversal);
        int written = postingService.writeToLedger(reversal, actor, now);
        original.markReversed(actor, now);                                   // POSTED -> REVERSED
        journals.flush();

        Map<String, Object> after = original.snapshot();
        after.put("reversedBy", reversal.getJournalNumber());
        audit.success(AuditAction.JOURNAL_REVERSED, EntityTypes.JOURNAL, original.getId(),
                original.getJournalNumber(), request.reason(), before, after);
        Map<String, Object> reversalAfter = reversal.snapshot();
        reversalAfter.put("reversalOf", original.getJournalNumber());
        reversalAfter.put("ledgerEntries", written);
        audit.success(AuditAction.JOURNAL_POSTED, EntityTypes.JOURNAL, reversal.getId(), reversal.getJournalNumber(),
                "Reversal dari " + original.getJournalNumber(), null, reversalAfter);
        log.info("Journal {} reversed by {} via {}", original.getJournalNumber(), actor, reversal.getJournalNumber());
        return ReversalResult.of(original, reversal, written);
    }
}
