package id.autochem.belajarsesi6.period.closing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
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
 * Time: 19.48
 */
public interface PeriodBalanceRepository extends JpaRepository<PeriodBalance, Long> {

    @Query("""
            select b from PeriodBalance b join fetch b.account a
             where b.period.id = :periodId
             order by a.code
            """)
    List<PeriodBalance> findByPeriod(@Param("periodId") Long periodId);

    @Modifying
    @Query(value = "DELETE FROM period_balance WHERE period_id = :periodId", nativeQuery = true)
    int deleteByPeriod(@Param("periodId") Long periodId);

    /** Snapshot saldo per akun dihitung di database dari buku besar (jurnal POSTED/REVERSED). */
    @Modifying
    @Query(value = """
            INSERT INTO period_balance (period_id, account_id, opening_balance, debit, credit, closing_balance,
                                        snapshot_by, snapshot_at)
            SELECT :periodId,
                   l.account_id,
                   coalesce(sum(l.debit - l.credit) FILTER (WHERE l.posting_date < :from), 0),
                   coalesce(sum(l.debit) FILTER (WHERE l.posting_date >= :from), 0),
                   coalesce(sum(l.credit) FILTER (WHERE l.posting_date >= :from), 0),
                   coalesce(sum(l.debit - l.credit), 0),
                   :actor,
                   :now
              FROM ledger_entry l
              JOIN journal_entry j ON j.id = l.journal_id AND j.status IN ('POSTED', 'REVERSED')
             WHERE l.posting_date <= :to
             GROUP BY l.account_id
            """, nativeQuery = true)
    int snapshot(@Param("periodId") Long periodId, @Param("from") LocalDate from, @Param("to") LocalDate to,
                 @Param("actor") String actor, @Param("now") Instant now);

    /**
     * Jumlah akun yang mutasi buku besarnya di periode ini tidak sama dengan baris jurnal
     * POSTED/REVERSED bertanggal di periode yang sama (harus 0 sebelum lock).
     */
    @Query(value = """
            WITH ledger AS (
                SELECT l.account_id, sum(l.debit) AS debit, sum(l.credit) AS credit
                  FROM ledger_entry l
                 WHERE l.posting_date BETWEEN :from AND :to
                 GROUP BY l.account_id),
                 lines AS (
                SELECT jl.account_id, sum(jl.debit) AS debit, sum(jl.credit) AS credit
                  FROM journal_line jl
                  JOIN journal_entry j ON j.id = jl.journal_id
                 WHERE j.status IN ('POSTED', 'REVERSED')
                   AND j.journal_date BETWEEN :from AND :to
                 GROUP BY jl.account_id)
            SELECT count(*)
              FROM ledger
              FULL OUTER JOIN lines ON lines.account_id = ledger.account_id
             WHERE coalesce(ledger.debit, 0) <> coalesce(lines.debit, 0)
                OR coalesce(ledger.credit, 0) <> coalesce(lines.credit, 0)
            """, nativeQuery = true)
    long countLedgerMismatches(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
