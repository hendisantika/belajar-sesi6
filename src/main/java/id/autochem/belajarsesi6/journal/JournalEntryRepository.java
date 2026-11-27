package id.autochem.belajarsesi6.journal;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

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
public interface JournalEntryRepository extends JpaRepository<JournalEntry, Long>,
        JpaSpecificationExecutor<JournalEntry> {

    /** SELECT ... FOR UPDATE: mencegah dua transaksi memproses jurnal yang sama bersamaan. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "5000"))
    @Query("select j from JournalEntry j where j.id = :id")
    Optional<JournalEntry> findByIdForUpdate(@Param("id") Long id);

    Optional<JournalEntry> findByJournalNumber(String journalNumber);

    Optional<JournalEntry> findByReversalOf(JournalEntry original);

    List<JournalEntry> findByJournalDateBetweenAndStatusInOrderByJournalDateAscIdAsc(
            LocalDate from, LocalDate to, Collection<JournalStatus> statuses);

    @Query("""
            select j from JournalEntry j
             where j.journalType = id.autochem.belajarsesi6.journal.JournalType.ADJUSTMENT
               and j.autoReverse = true
               and j.status = id.autochem.belajarsesi6.journal.JournalStatus.POSTED
               and j.journalDate between :from and :to
             order by j.journalDate, j.id
            """)
    List<JournalEntry> findAutoReversibleAdjustments(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query(value = "select nextval('journal_number_seq')", nativeQuery = true)
    long nextJournalSequence();
}
