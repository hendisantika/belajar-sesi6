package id.autochem.belajarsesi6.trialbalance;

import id.autochem.belajarsesi6.journal.JournalStatus;
import id.autochem.belajarsesi6.ledger.LedgerEntry;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * Project : belajar-sesi6
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Link : s.id/hendisantika
 * Date: 27/11/26
 * Time: 19.46
 */
public interface TrialBalanceRepository extends Repository<LedgerEntry, Long> {

    /**
     * Neraca saldo per akun s.d. tanggal asOf. Hanya mutasi jurnal POSTED/REVERSED
     * (jurnal DRAFT/APPROVED tidak pernah ikut). Memanfaatkan index (account_id, posting_date).
     */
    @Query("""
            select new id.autochem.belajarsesi6.trialbalance.TrialBalanceLine(
                       a.code, a.name, a.accountType, a.normalBalance, sum(l.debit), sum(l.credit))
              from LedgerEntry l
              join l.account a
              join l.journal j
             where l.postingDate <= :asOf
               and j.status in :statuses
             group by a.code, a.name, a.accountType, a.normalBalance
             order by a.code
            """)
    List<TrialBalanceLine> trialBalance(@Param("asOf") LocalDate asOf,
                                        @Param("statuses") Collection<JournalStatus> statuses);

    /**
     * Worksheet periode: saldo awal, mutasi reguler, dan mutasi penyesuaian (jurnal ADJUSTMENT)
     * untuk membentuk neraca saldo sebelum &amp; sesudah penyesuaian.
     */
    @Query(value = """
            SELECT a.code           AS "code",
                   a.name           AS "name",
                   a.account_type   AS "accountType",
                   a.normal_balance AS "normalBalance",
                   coalesce(sum(l.debit - l.credit) FILTER (WHERE l.posting_date < :from), 0)::numeric(19, 2)
                       AS "opening",
                   coalesce(sum(l.debit) FILTER (WHERE l.posting_date >= :from
                                                   AND j.journal_type <> 'ADJUSTMENT'), 0)::numeric(19, 2)
                       AS "debit",
                   coalesce(sum(l.credit) FILTER (WHERE l.posting_date >= :from
                                                    AND j.journal_type <> 'ADJUSTMENT'), 0)::numeric(19, 2)
                       AS "credit",
                   coalesce(sum(l.debit) FILTER (WHERE l.posting_date >= :from
                                                   AND j.journal_type = 'ADJUSTMENT'), 0)::numeric(19, 2)
                       AS "adjustmentDebit",
                   coalesce(sum(l.credit) FILTER (WHERE l.posting_date >= :from
                                                    AND j.journal_type = 'ADJUSTMENT'), 0)::numeric(19, 2)
                       AS "adjustmentCredit"
              FROM ledger_entry l
              JOIN journal_entry j ON j.id = l.journal_id AND j.status IN ('POSTED', 'REVERSED')
              JOIN account a ON a.id = l.account_id
             WHERE l.posting_date <= :to
             GROUP BY a.id, a.code, a.name, a.account_type, a.normal_balance
             ORDER BY a.code
            """, nativeQuery = true)
    List<WorksheetRow> worksheet(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
