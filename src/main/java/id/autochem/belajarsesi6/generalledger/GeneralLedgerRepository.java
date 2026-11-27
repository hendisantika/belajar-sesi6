package id.autochem.belajarsesi6.generalledger;

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
 * Time: 19.44
 */
public interface GeneralLedgerRepository extends Repository<LedgerEntry, Long> {

    /** Saldo awal = akumulasi (debit - kredit) sebelum tanggal awal periode laporan. */
    @Query(value = """
            SELECT coalesce(sum(l.debit - l.credit), 0)::numeric(19, 2)
              FROM ledger_entry l
              JOIN journal_entry j ON j.id = l.journal_id
             WHERE l.account_id = :accountId
               AND l.posting_date < :from
               AND j.status IN ('POSTED', 'REVERSED')
            """, nativeQuery = true)
    BigDecimal openingBalance(@Param("accountId") Long accountId, @Param("from") LocalDate from);

    /** General Ledger satu akun dalam satu periode + saldo berjalan (window function). */
    @Query(value = """
            SELECT l.id             AS "ledgerEntryId",
                   l.posting_date   AS "postingDate",
                   j.id             AS "journalId",
                   j.journal_number AS "journalNo",
                   j.status         AS "journalStatus",
                   l.description    AS "description",
                   l.debit          AS "debit",
                   l.credit         AS "credit",
                   SUM(l.debit - l.credit) OVER (
                       ORDER BY l.posting_date, l.id
                   ) + :openingBalance AS "runningBalance"
              FROM ledger_entry l
              JOIN journal_entry j ON j.id = l.journal_id
             WHERE l.account_id = :accountId
               AND l.posting_date BETWEEN :from AND :to
               AND j.status IN ('POSTED', 'REVERSED')
             ORDER BY l.posting_date, l.id
            """, nativeQuery = true)
    List<GeneralLedgerRow> ledger(@Param("accountId") Long accountId, @Param("from") LocalDate from,
                                  @Param("to") LocalDate to, @Param("openingBalance") BigDecimal openingBalance);

    /** Ringkasan saldo awal, mutasi, dan saldo akhir seluruh akun yang punya mutasi s.d. tanggal akhir. */
    @Query(value = """
            SELECT a.code           AS "code",
                   a.name           AS "name",
                   a.account_type   AS "accountType",
                   a.normal_balance AS "normalBalance",
                   coalesce(sum(l.debit - l.credit) FILTER (WHERE l.posting_date < :from), 0)::numeric(19, 2) AS "opening",
                   coalesce(sum(l.debit) FILTER (WHERE l.posting_date >= :from), 0)::numeric(19, 2)           AS "debit",
                   coalesce(sum(l.credit) FILTER (WHERE l.posting_date >= :from), 0)::numeric(19, 2)          AS "credit"
              FROM account a
              JOIN ledger_entry l ON l.account_id = a.id
              JOIN journal_entry j ON j.id = l.journal_id AND j.status IN ('POSTED', 'REVERSED')
             WHERE l.posting_date <= :to
             GROUP BY a.id, a.code, a.name, a.account_type, a.normal_balance
             ORDER BY a.code
            """, nativeQuery = true)
    List<AccountBalanceRow> balances(@Param("from") LocalDate from, @Param("to") LocalDate to);

    /** Rekonsiliasi buku besar terhadap baris jurnal POSTED/REVERSED per akun. */
    @Query(value = """
            WITH ledger AS (
                SELECT l.account_id, sum(l.debit) AS debit, sum(l.credit) AS credit
                  FROM ledger_entry l
                 GROUP BY l.account_id),
                 lines AS (
                SELECT jl.account_id, sum(jl.debit) AS debit, sum(jl.credit) AS credit
                  FROM journal_line jl
                  JOIN journal_entry j ON j.id = jl.journal_id
                 WHERE j.status IN ('POSTED', 'REVERSED')
                 GROUP BY jl.account_id)
            SELECT a.code                        AS "code",
                   coalesce(ledger.debit, 0)::numeric(19, 2)     AS "ledgerDebit",
                   coalesce(ledger.credit, 0)::numeric(19, 2)    AS "ledgerCredit",
                   coalesce(lines.debit, 0)::numeric(19, 2)      AS "lineDebit",
                   coalesce(lines.credit, 0)::numeric(19, 2)     AS "lineCredit"
              FROM ledger
              FULL OUTER JOIN lines ON lines.account_id = ledger.account_id
              JOIN account a ON a.id = coalesce(ledger.account_id, lines.account_id)
             ORDER BY a.code
            """, nativeQuery = true)
    List<LedgerVerificationRow> verification();

    /** Baris jurnal yang tercatat lebih dari sekali di buku besar (harus 0). */
    @Query(value = """
            SELECT count(*) FROM (
                SELECT journal_line_id FROM ledger_entry GROUP BY journal_line_id HAVING count(*) > 1) d
            """, nativeQuery = true)
    long duplicatePostings();
}
