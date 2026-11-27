package id.autochem.belajarsesi6.journal;

import id.autochem.belajarsesi6.account.Account;
import id.autochem.belajarsesi6.account.AccountRepository;
import id.autochem.belajarsesi6.audit.AuditAction;
import id.autochem.belajarsesi6.audit.AuditTrail;
import id.autochem.belajarsesi6.audit.AuditedOperation;
import id.autochem.belajarsesi6.audit.EntityTypes;
import id.autochem.belajarsesi6.common.exception.BusinessException;
import id.autochem.belajarsesi6.common.exception.ErrorCode;
import id.autochem.belajarsesi6.common.exception.NotFoundException;
import id.autochem.belajarsesi6.journal.dto.JournalLineRequest;
import id.autochem.belajarsesi6.journal.dto.JournalRequest;
import id.autochem.belajarsesi6.journal.dto.JournalResponse;
import id.autochem.belajarsesi6.journal.dto.JournalSummaryResponse;
import id.autochem.belajarsesi6.period.PeriodGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

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
public class JournalService {

    private final JournalEntryRepository journals;
    private final AccountRepository accounts;
    private final JournalNumberGenerator numberGenerator;
    private final PeriodGuard periodGuard;
    private final AuditTrail audit;
    private final Clock clock;

    @Transactional(readOnly = true)
    public Page<JournalSummaryResponse> list(JournalStatus status, JournalType type, LocalDate from, LocalDate to,
                                             Pageable pageable) {
        return journals.findAll(JournalSpecifications.filter(status, type, from, to), pageable)
                .map(JournalSummaryResponse::from);
    }

    @Transactional(readOnly = true)
    public JournalResponse get(Long id) {
        JournalEntry entry = require(id);
        return JournalResponse.from(entry, journals.findByReversalOf(entry).orElse(null));
    }

    @Transactional(readOnly = true)
    public JournalResponse getByNumber(String number) {
        JournalEntry entry = journals.findByJournalNumber(number)
                .orElseThrow(() -> new NotFoundException("Jurnal", number));
        return JournalResponse.from(entry, journals.findByReversalOf(entry).orElse(null));
    }

    @Transactional
    @AuditedOperation(action = AuditAction.JOURNAL_CREATED, entityType = EntityTypes.JOURNAL,
            entityRef = "#request.journalDate + ' ' + #request.description")
    public JournalResponse create(JournalRequest request) {
        JournalType type = request.typeOrDefault();
        if (type == JournalType.REVERSAL) {
            throw new BusinessException(ErrorCode.INVALID_JOURNAL_TYPE,
                    "Jurnal REVERSAL dibuat lewat POST /api/journals/{id}/reverse agar selalu punya jurnal asal");
        }
        periodGuard.assertOpen(request.journalDate(), type);
        JournalEntry entry = JournalEntry.draft(numberGenerator.next(request.journalDate(), type),
                request.journalDate(), request.description(), type, request.autoReverseOrDefault());
        addLines(entry, request.lines());
        journals.saveAndFlush(entry);
        audit.success(AuditAction.JOURNAL_CREATED, EntityTypes.JOURNAL, entry.getId(), entry.getJournalNumber(),
                null, entry.snapshot());
        return JournalResponse.from(entry);
    }

    @Transactional
    @AuditedOperation(action = AuditAction.JOURNAL_UPDATED, entityType = EntityTypes.JOURNAL, entityId = "#id")
    public JournalResponse update(Long id, JournalRequest request) {
        JournalEntry entry = requireForUpdate(id);
        entry.assertEditable();
        Map<String, Object> before = entry.snapshot();
        // Periode lama DAN periode baru harus masih terbuka
        periodGuard.assertOpen(entry.getJournalDate(), entry.getJournalType());
        periodGuard.assertOpen(request.journalDate(), entry.getJournalType());
        entry.updateHeader(request.journalDate(), request.description(), request.autoReverseOrDefault());
        entry.clearLines();
        journals.flush();
        addLines(entry, request.lines());
        JournalEntry saved = journals.saveAndFlush(entry);
        audit.success(AuditAction.JOURNAL_UPDATED, EntityTypes.JOURNAL, saved.getId(), saved.getJournalNumber(),
                before, saved.snapshot());
        return JournalResponse.from(saved);
    }

    @Transactional
    @AuditedOperation(action = AuditAction.JOURNAL_APPROVED, entityType = EntityTypes.JOURNAL, entityId = "#id")
    public JournalResponse approve(Long id, String actor) {
        JournalEntry entry = requireForUpdate(id);
        Map<String, Object> before = entry.snapshot();
        periodGuard.assertOpen(entry.getJournalDate(), entry.getJournalType());
        entry.approve(actor, clock.instant());
        JournalEntry saved = journals.saveAndFlush(entry);
        audit.success(AuditAction.JOURNAL_APPROVED, EntityTypes.JOURNAL, saved.getId(), saved.getJournalNumber(),
                before, saved.snapshot());
        return JournalResponse.from(saved);
    }

    @Transactional
    @AuditedOperation(action = AuditAction.JOURNAL_RETURNED_TO_DRAFT, entityType = EntityTypes.JOURNAL, entityId = "#id")
    public JournalResponse returnToDraft(Long id, String reason) {
        JournalEntry entry = requireForUpdate(id);
        Map<String, Object> before = entry.snapshot();
        entry.returnToDraft();
        JournalEntry saved = journals.saveAndFlush(entry);
        audit.success(AuditAction.JOURNAL_RETURNED_TO_DRAFT, EntityTypes.JOURNAL, saved.getId(),
                saved.getJournalNumber(), reason, before, saved.snapshot());
        return JournalResponse.from(saved);
    }

    @Transactional
    @AuditedOperation(action = AuditAction.JOURNAL_DELETED, entityType = EntityTypes.JOURNAL, entityId = "#id")
    public void delete(Long id) {
        JournalEntry entry = requireForUpdate(id);
        entry.assertEditable();
        periodGuard.assertOpen(entry.getJournalDate(), entry.getJournalType());
        audit.success(AuditAction.JOURNAL_DELETED, EntityTypes.JOURNAL, entry.getId(), entry.getJournalNumber(),
                entry.snapshot(), null);
        journals.delete(entry);
    }

    private void addLines(JournalEntry entry, List<JournalLineRequest> lines) {
        Set<String> codes = lines.stream().map(JournalLineRequest::accountCode).collect(Collectors.toSet());
        Map<String, Account> byCode = accounts.findByCodeIn(codes).stream()
                .collect(Collectors.toMap(Account::getCode, Function.identity()));
        Set<String> missing = new TreeSet<>(codes);
        missing.removeAll(byCode.keySet());
        if (!missing.isEmpty()) {
            throw new BusinessException(ErrorCode.ACCOUNT_NOT_FOUND, "Akun tidak ditemukan: " + missing);
        }
        for (JournalLineRequest line : lines) {
            entry.addLine(new JournalLine(byCode.get(line.accountCode()), line.description(),
                    line.debit(), line.credit()));
        }
    }

    private JournalEntry require(Long id) {
        return journals.findById(id).orElseThrow(() -> new NotFoundException("Jurnal", id));
    }

    private JournalEntry requireForUpdate(Long id) {
        return journals.findByIdForUpdate(id).orElseThrow(() -> new NotFoundException("Jurnal", id));
    }
}
