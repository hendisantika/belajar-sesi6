package id.autochem.belajarsesi6.reconciliation;

import id.autochem.belajarsesi6.ledger.LedgerEntry;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
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
 * Time: 19.52
 */
/** Query rekonsiliasi tiga lapis: Trial Balance -> General Ledger -> Jurnal (posted lines). */
public interface ReconciliationRepository extends Repository<LedgerEntry, Long> {

    interface AccountComparison {
        String getCode();

        String getName();

        BigDecimal getTbDebit();

        BigDecimal getTbCredit();

        BigDecimal getGlDebit();

        BigDecimal getGlCredit();

        BigDecimal getJournalDebit();

        BigDecimal getJournalCredit();
    }

    interface Finding {
        Long getJournalId();

        String getJournalNumber();

        String getStatus();

        LocalDate getJournalDate();

        String getDetail();

        BigDecimal getAmount();
    }

    interface Drift {
        String getPeriod();

        String getCode();

        BigDecimal getSnapshotBalance();

        BigDecimal getLiveBalance();
    }

    interface TraceRow {
        Long getLedgerEntryId();

        LocalDate getPostingDate();

        Long getJournalId();

        String getJournalNumber();

        String getJournalType();

        String getJournalStatus();

        LocalDate getJournalDate();

        Integer getLineNo();

        String getDescription();

        BigDecimal getDebit();

        BigDecimal getCredit();

        BigDecimal getLineDebit();

        BigDecimal getLineCredit();

        BigDecimal getRunningBalance();

        String getPostedBy();

        java.time.Instant getPostedAt();
    }

    /**
     * Per akun: TB (ledger + jurnal POSTED/REVERSED), GL mentah (semua ledger), dan baris jurnal
     * POSTED/REVERSED bertanggal s.d. asOf. Ketiganya harus sama.
     */
    @Query(value = """
            WITH tb AS (
                SELECT l.account_id, sum(l.debit) AS debit, sum(l.credit) AS credit
                  FROM ledger_entry l
                  JOIN journal_entry j ON j.id = l.journal_id AND j.status IN ('POSTED', 'REVERSED')
                 WHERE l.posting_date <= :asOf
                 GROUP BY l.account_id),
                 gl AS (
                SELECT l.account_id, sum(l.debit) AS debit, sum(l.credit) AS credit
                  FROM ledger_entry l
                 WHERE l.posting_date <= :asOf
                 GROUP BY l.account_id),
                 jl AS (
                SELECT jl.account_id, sum(jl.debit) AS debit, sum(jl.credit) AS credit
                  FROM journal_line jl
                  JOIN journal_entry j ON j.id = jl.journal_id AND j.status IN ('POSTED', 'REVERSED')
                 WHERE j.journal_date <= :asOf
                 GROUP BY jl.account_id)
            SELECT a.code                                   AS "code",
                   a.name                                   AS "name",
                   coalesce(tb.debit, 0)::numeric(19, 2)    AS "tbDebit",
                   coalesce(tb.credit, 0)::numeric(19, 2)   AS "tbCredit",
                   coalesce(gl.debit, 0)::numeric(19, 2)    AS "glDebit",
                   coalesce(gl.credit, 0)::numeric(19, 2)   AS "glCredit",
                   coalesce(jl.debit, 0)::numeric(19, 2)    AS "journalDebit",
                   coalesce(jl.credit, 0)::numeric(19, 2)   AS "journalCredit"
              FROM account a
              LEFT JOIN tb ON tb.account_id = a.id
              LEFT JOIN gl ON gl.account_id = a.id
              LEFT JOIN jl ON jl.account_id = a.id
             WHERE tb.account_id IS NOT NULL OR gl.account_id IS NOT NULL OR jl.account_id IS NOT NULL
             ORDER BY a.code
            """, nativeQuery = true)
    List<AccountComparison> compareAccounts(@Param("asOf") LocalDate asOf);

    /** Penyebab umum #1: jurnal DRAFT/APPROVED yang akan membuat selisih bila ikut dihitung. */
    @Query(value = """
            SELECT j.id AS "journalId", j.journal_number AS "journalNumber", j.status AS "status",
                   j.journal_date AS "journalDate", j.description AS "detail",
                   coalesce(sum(jl.debit), 0)::numeric(19, 2) AS "amount"
              FROM journal_entry j
              LEFT JOIN journal_line jl ON jl.journal_id = j.id
             WHERE j.status IN ('DRAFT', 'APPROVED') AND j.journal_date <= :asOf
             GROUP BY j.id
             ORDER BY j.journal_date, j.id
            """, nativeQuery = true)
    List<Finding> unpostedJournals(@Param("asOf") LocalDate asOf);

    /** Ledger milik jurnal yang statusnya bukan POSTED/REVERSED (jurnal draft "bocor" ke GL). */
    @Query(value = """
            SELECT j.id AS "journalId", j.journal_number AS "journalNumber", j.status AS "status",
                   j.journal_date AS "journalDate", count(l.id) || ' ledger entry' AS "detail",
                   sum(l.debit)::numeric(19, 2) AS "amount"
              FROM ledger_entry l
              JOIN journal_entry j ON j.id = l.journal_id
             WHERE j.status NOT IN ('POSTED', 'REVERSED') AND l.posting_date <= :asOf
             GROUP BY j.id
             ORDER BY j.journal_date, j.id
            """, nativeQuery = true)
    List<Finding> ledgerFromUnpostedJournals(@Param("asOf") LocalDate asOf);

    /** Penyebab umum #2: filter periode tidak konsisten (tanggal posting != tanggal jurnal). */
    @Query(value = """
            SELECT j.id AS "journalId", j.journal_number AS "journalNumber", j.status AS "status",
                   j.journal_date AS "journalDate",
                   'posting_date ' || min(l.posting_date) || ' != journal_date ' || j.journal_date AS "detail",
                   sum(l.debit)::numeric(19, 2) AS "amount"
              FROM ledger_entry l
              JOIN journal_entry j ON j.id = l.journal_id
             WHERE l.posting_date <> j.journal_date
               AND least(l.posting_date, j.journal_date) <= :asOf
             GROUP BY j.id
             ORDER BY j.journal_date, j.id
            """, nativeQuery = true)
    List<Finding> postingDateMismatches(@Param("asOf") LocalDate asOf);

    /** Jurnal POSTED/REVERSED yang jumlah ledger entry-nya tidak sama dengan jumlah baris. */
    @Query(value = """
            SELECT j.id AS "journalId", j.journal_number AS "journalNumber", j.status AS "status",
                   j.journal_date AS "journalDate",
                   (SELECT count(*) FROM journal_line x WHERE x.journal_id = j.id) || ' baris vs '
                       || (SELECT count(*) FROM ledger_entry x WHERE x.journal_id = j.id) || ' ledger entry' AS "detail",
                   (SELECT coalesce(sum(x.debit), 0) FROM journal_line x WHERE x.journal_id = j.id)::numeric(19, 2)
                       AS "amount"
              FROM journal_entry j
             WHERE j.status IN ('POSTED', 'REVERSED') AND j.journal_date <= :asOf
               AND (SELECT count(*) FROM journal_line x WHERE x.journal_id = j.id)
                   <> (SELECT count(*) FROM ledger_entry x WHERE x.journal_id = j.id)
             ORDER BY j.journal_date, j.id
            """, nativeQuery = true)
    List<Finding> postedWithoutCompleteLedger(@Param("asOf") LocalDate asOf);

    /** Penyebab umum #3: reversal tanpa jurnal asal / jurnal REVERSED tanpa jurnal pembalik. */
    @Query(value = """
            SELECT j.id AS "journalId", j.journal_number AS "journalNumber", j.status AS "status",
                   j.journal_date AS "journalDate",
                   'REVERSED tetapi tidak ada jurnal pembalik' AS "detail", NULL::numeric(19, 2) AS "amount"
              FROM journal_entry j
             WHERE j.status = 'REVERSED'
               AND NOT EXISTS (SELECT 1 FROM journal_entry r WHERE r.reversal_of_id = j.id)
            UNION ALL
            SELECT r.id, r.journal_number, r.status, r.journal_date,
                   'Jurnal pembalik ' || r.status || ' tetapi jurnal asal ' || o.journal_number || ' berstatus ' || o.status,
                   NULL::numeric(19, 2)
              FROM journal_entry r
              JOIN journal_entry o ON o.id = r.reversal_of_id
             WHERE r.status IN ('POSTED', 'REVERSED') AND o.status <> 'REVERSED'
            UNION ALL
            SELECT j.id, j.journal_number, j.status, j.journal_date,
                   'Jurnal tipe REVERSAL tanpa referensi jurnal asal', NULL::numeric(19, 2)
              FROM journal_entry j
             WHERE j.journal_type = 'REVERSAL' AND j.reversal_of_id IS NULL
            """, nativeQuery = true)
    List<Finding> reversalIntegrityIssues();

    /** Jurnal POSTED/REVERSED yang baris debit != kredit (seharusnya mustahil). */
    @Query(value = """
            SELECT j.id AS "journalId", j.journal_number AS "journalNumber", j.status AS "status",
                   j.journal_date AS "journalDate",
                   'debit ' || sum(jl.debit) || ' != kredit ' || sum(jl.credit) AS "detail",
                   (sum(jl.debit) - sum(jl.credit))::numeric(19, 2) AS "amount"
              FROM journal_entry j
              JOIN journal_line jl ON jl.journal_id = j.id
             WHERE j.status IN ('POSTED', 'REVERSED') AND j.journal_date <= :asOf
             GROUP BY j.id
            HAVING sum(jl.debit) <> sum(jl.credit)
            """, nativeQuery = true)
    List<Finding> unbalancedJournals(@Param("asOf") LocalDate asOf);

    /** Saldo snapshot periode LOCKED dibanding saldo live per akhir periode (harus sama). */
    @Query(value = """
            WITH live AS (
                SELECT p.id AS period_id, l.account_id, sum(l.debit - l.credit) AS balance
                  FROM accounting_period p
                  JOIN ledger_entry l ON l.posting_date <= p.end_date
                  JOIN journal_entry j ON j.id = l.journal_id AND j.status IN ('POSTED', 'REVERSED')
                 WHERE p.status = 'LOCKED' AND p.end_date <= :asOf
                 GROUP BY p.id, l.account_id),
                 snap AS (
                SELECT b.period_id, b.account_id, b.closing_balance AS balance
                  FROM period_balance b
                  JOIN accounting_period p ON p.id = b.period_id
                 WHERE p.status = 'LOCKED' AND p.end_date <= :asOf)
            SELECT p.code AS "period", a.code AS "code",
                   coalesce(snap.balance, 0)::numeric(19, 2) AS "snapshotBalance",
                   coalesce(live.balance, 0)::numeric(19, 2) AS "liveBalance"
              FROM snap
              FULL OUTER JOIN live ON live.period_id = snap.period_id AND live.account_id = snap.account_id
              JOIN accounting_period p ON p.id = coalesce(snap.period_id, live.period_id)
              JOIN account a ON a.id = coalesce(snap.account_id, live.account_id)
             WHERE coalesce(snap.balance, 0) <> coalesce(live.balance, 0)
             ORDER BY p.code, a.code
            """, nativeQuery = true)
    List<Drift> lockedPeriodDrift(@Param("asOf") LocalDate asOf);

    /** Drill-down satu akun: setiap ledger entry beserta baris jurnal sumbernya. */
    @Query(value = """
            SELECT l.id              AS "ledgerEntryId",
                   l.posting_date    AS "postingDate",
                   j.id              AS "journalId",
                   j.journal_number  AS "journalNumber",
                   j.journal_type    AS "journalType",
                   j.status          AS "journalStatus",
                   j.journal_date    AS "journalDate",
                   jl.line_no        AS "lineNo",
                   l.description     AS "description",
                   l.debit           AS "debit",
                   l.credit          AS "credit",
                   jl.debit          AS "lineDebit",
                   jl.credit         AS "lineCredit",
                   SUM(l.debit - l.credit) OVER (ORDER BY l.posting_date, l.id) AS "runningBalance",
                   j.posted_by       AS "postedBy",
                   j.posted_at       AS "postedAt"
              FROM ledger_entry l
              JOIN account a ON a.id = l.account_id
              JOIN journal_entry j ON j.id = l.journal_id
              JOIN journal_line jl ON jl.id = l.journal_line_id
             WHERE a.code = :code AND l.posting_date <= :asOf
             ORDER BY l.posting_date, l.id
            """, nativeQuery = true)
    List<TraceRow> trace(@Param("code") String code, @Param("asOf") LocalDate asOf);

    /** Baris jurnal DRAFT/APPROVED untuk akun ini yang (benar) dikecualikan dari trial balance. */
    @Query(value = """
            SELECT j.id AS "journalId", j.journal_number AS "journalNumber", j.status AS "status",
                   j.journal_date AS "journalDate", coalesce(jl.description, j.description) AS "detail",
                   (jl.debit - jl.credit)::numeric(19, 2) AS "amount"
              FROM journal_line jl
              JOIN journal_entry j ON j.id = jl.journal_id
              JOIN account a ON a.id = jl.account_id
             WHERE a.code = :code AND j.status IN ('DRAFT', 'APPROVED') AND j.journal_date <= :asOf
             ORDER BY j.journal_date, j.id
            """, nativeQuery = true)
    List<Finding> excludedLines(@Param("code") String code, @Param("asOf") LocalDate asOf);
}
