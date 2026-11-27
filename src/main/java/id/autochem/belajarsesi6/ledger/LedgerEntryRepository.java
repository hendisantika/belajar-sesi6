package id.autochem.belajarsesi6.ledger;

import org.springframework.data.jpa.repository.JpaRepository;

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
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {

    long countByJournal_Id(Long journalId);
}
