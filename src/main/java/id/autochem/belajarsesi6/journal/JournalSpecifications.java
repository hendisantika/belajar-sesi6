package id.autochem.belajarsesi6.journal;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.ArrayList;
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
final class JournalSpecifications {

    private JournalSpecifications() {
    }

    static Specification<JournalEntry> filter(JournalStatus status, JournalType type, LocalDate from, LocalDate to) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (type != null) {
                predicates.add(cb.equal(root.get("journalType"), type));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("journalDate"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("journalDate"), to));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }
}
