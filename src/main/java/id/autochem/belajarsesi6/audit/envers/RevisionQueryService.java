package id.autochem.belajarsesi6.audit.envers;

import id.autochem.belajarsesi6.account.Account;
import id.autochem.belajarsesi6.account.AccountRepository;
import id.autochem.belajarsesi6.common.exception.NotFoundException;
import id.autochem.belajarsesi6.journal.JournalEntry;
import id.autochem.belajarsesi6.period.AccountingPeriod;
import id.autochem.belajarsesi6.period.AccountingPeriodRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Created by IntelliJ IDEA.
 * Project : belajar-sesi6
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Link : s.id/hendisantika
 * Date: 27/11/26
 * Time: 19.52
 */
/** Membaca riwayat versi entity @Audited (Hibernate Envers) untuk kebutuhan auditor. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RevisionQueryService {

    private final EntityManager entityManager;
    private final AccountRepository accounts;
    private final AccountingPeriodRepository periods;

    public List<RevisionResponse> journal(Long id) {
        return revisions(JournalEntry.class, id, JournalEntry::snapshot);
    }

    public List<RevisionResponse> period(String code) {
        AccountingPeriod period = periods.findByCode(code).orElseThrow(() -> new NotFoundException("Periode", code));
        return revisions(AccountingPeriod.class, period.getId(), AccountingPeriod::snapshot);
    }

    public List<RevisionResponse> account(String code) {
        Account account = accounts.findByCode(code).orElseThrow(() -> new NotFoundException("Akun", code));
        return revisions(Account.class, account.getId(), Account::snapshot);
    }

    @SuppressWarnings("unchecked")
    private <T> List<RevisionResponse> revisions(Class<T> type, Long id, Function<T, Map<String, Object>> snapshot) {
        AuditReader reader = AuditReaderFactory.get(entityManager);
        List<Object[]> rows = reader.createQuery()
                .forRevisionsOfEntity(type, false, true)
                .add(AuditEntity.id().eq(id))
                .addOrder(AuditEntity.revisionNumber().asc())
                .getResultList();

        List<RevisionResponse> result = new ArrayList<>();
        Map<String, Object> previous = Map.of();
        for (Object[] row : rows) {
            T entity = (T) row[0];
            AuditRevision revision = (AuditRevision) row[1];
            RevisionType revisionType = (RevisionType) row[2];
            Map<String, Object> state = snapshot.apply(entity);
            Map<String, RevisionResponse.Change> changes = new LinkedHashMap<>();
            for (Map.Entry<String, Object> e : state.entrySet()) {
                Object before = previous.get(e.getKey());
                if (!Objects.equals(asComparable(before), asComparable(e.getValue()))) {
                    changes.put(e.getKey(), new RevisionResponse.Change(before, e.getValue()));
                }
            }
            result.add(new RevisionResponse(revision.getId(), Instant.ofEpochMilli(revision.getTimestamp()),
                    revision.getUsername(), revisionType.name(), state, changes));
            previous = state;
        }
        return result;
    }

    /** BigDecimal 100.0 dan 100.00 dianggap sama. */
    private static Object asComparable(Object value) {
        return value instanceof java.math.BigDecimal bd ? bd.stripTrailingZeros() : value;
    }
}
