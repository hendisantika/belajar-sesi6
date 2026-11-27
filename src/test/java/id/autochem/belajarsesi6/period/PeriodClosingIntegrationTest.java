package id.autochem.belajarsesi6.period;

import com.jayway.jsonpath.JsonPath;
import id.autochem.belajarsesi6.support.ApiTestSupport;
import id.autochem.belajarsesi6.support.FixedClockConfiguration;
import id.autochem.belajarsesi6.support.TestcontainersConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Created by IntelliJ IDEA.
 * Project : belajar-sesi6
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Link : s.id/hendisantika
 * Date: 27/11/26
 * Time: 19.56
 */
/**
 * Skenario period closing end-to-end (berurutan, DB sendiri):
 * lock menolak posting -> cut-off -> checklist -> lock -> koreksi di periode terbuka -> auto-reversal -> unlock.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Import({TestcontainersConfiguration.class, FixedClockConfiguration.class})
@TestPropertySource(properties = "it.context=period-closing")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("Sesi 6 - Period cut-off, lock/unlock & audit penolakan")
class PeriodClosingIntegrationTest extends ApiTestSupport {

    private static final String UNLOCK_REASON = "{\"reason\":\"Koreksi nilai pendapatan jasa (memo CFO-21)\"}";

    private static long accrualId;

    @Test
    @Order(1)
    @DisplayName("Posting ke periode LOCKED ditolak dan penolakan tercatat di audit log")
    void postingToLockedPeriodIsRejectedAndAudited() throws Exception {
        long rejectedBefore = auditCount("JOURNAL_POSTED", "REJECTED", 5);

        postJournal(5)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("PERIOD_LOCKED"))
                .andExpect(jsonPath("$.period").value("2026-10"));

        assertThat(auditCount("JOURNAL_POSTED", "REJECTED", 5)).isEqualTo(rejectedBefore + 1);
        assertThat(jdbc.queryForMap("""
                select actor, actor_roles, error_code from audit_event
                 where action = 'JOURNAL_POSTED' and outcome = 'REJECTED' and entity_id = 5
                 order by id desc limit 1"""))
                .containsEntry("actor", "approver1")
                .containsEntry("actor_roles", "APPROVER")
                .containsEntry("error_code", "PERIOD_LOCKED");
        // Transaksi bisnis rollback: status & GL tidak berubah
        assertThat(jdbc.queryForObject("select status from journal_entry where id = 5", String.class))
                .isEqualTo("APPROVED");
        assertThat(jdbc.queryForObject("select count(*) from ledger_entry where journal_id = 5", Long.class))
                .isZero();
    }

    @Test
    @Order(2)
    @DisplayName("Membuat jurnal bertanggal periode LOCKED ditolak (assertOpen di setiap operasi tulis)")
    void creatingJournalInLockedPeriodIsRejected() throws Exception {
        postJson("/api/journals", ACCOUNTANT, """
                {"journalDate":"2026-10-15","description":"Jurnal mundur",
                 "lines":[{"accountCode":"5401","debit":100},{"accountCode":"1101","credit":100}]}""")
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("PERIOD_LOCKED"));
        assertThat(jdbc.queryForObject("""
                select count(*) from audit_event
                 where action = 'JOURNAL_CREATED' and outcome = 'REJECTED' and actor = 'accountant1'""", Long.class))
                .isPositive();
    }

    @Test
    @Order(3)
    @DisplayName("Trigger DB menolak ledger ke periode LOCKED walau aplikasi dilewati")
    void databaseGuardsLockedPeriod() {
        assertThatThrownBy(() -> jdbc.update("""
                insert into ledger_entry (journal_id, journal_line_id, account_id, posting_date, debit, credit)
                select journal_id, id, account_id, date '2026-10-30', debit, credit
                  from journal_line where journal_id = 5 and line_no = 1"""))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("LOCKED");
    }

    @Test
    @Order(4)
    @DisplayName("Hanya ADMIN yang boleh cut-off / lock; percobaan lain tercatat DENIED")
    void onlyAdminCanManagePeriods() throws Exception {
        long deniedBefore = deniedCount("accountant1");
        perform(post("/api/periods/2026-11/lock"), ACCOUNTANT).andExpect(status().isForbidden());
        perform(post("/api/periods/2026-11/cut-off"), APPROVER).andExpect(status().isForbidden());
        postJson("/api/periods/2026-10/unlock", AUDITOR, UNLOCK_REASON).andExpect(status().isForbidden());
        assertThat(deniedCount("accountant1")).isEqualTo(deniedBefore + 1);
        assertThat(jdbc.queryForObject("""
                select entity_ref from audit_event where outcome = 'DENIED' and actor = 'accountant1'
                 order by id desc limit 1""", String.class)).contains("POST /api/periods/2026-11/lock");
    }

    @Test
    @Order(5)
    @DisplayName("Lock tanpa cut-off ditolak")
    void lockRequiresCutOff() throws Exception {
        perform(post("/api/periods/2026-11/lock"), ADMIN)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INVALID_PERIOD_TRANSITION"));
    }

    @Test
    @Order(6)
    @DisplayName("Cut-off: jurnal GENERAL ditolak, jurnal ADJUSTMENT (akrual) tetap boleh")
    void cutOffAllowsOnlyCorrections() throws Exception {
        perform(post("/api/periods/2026-11/cut-off"), ADMIN)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SOFT_CLOSED"))
                .andExpect(jsonPath("$.cutoffBy").value("admin"));

        postJournal(11)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("PERIOD_CUT_OFF"));

        accrualId = postedJournal("2026-11-30", "ADJUSTMENT", true, "5301", "2102", "650000.00");
        perform(get("/api/journals/{id}", accrualId), AUDITOR)
                .andExpect(jsonPath("$.journalNumber", containsString("AJ-202611-")))
                .andExpect(jsonPath("$.status").value("POSTED"))
                .andExpect(jsonPath("$.autoReverse").value(true));
    }

    @Test
    @Order(7)
    @DisplayName("Closing checklist memblokir lock selama ada jurnal DRAFT/APPROVED")
    void checklistBlocksLock() throws Exception {
        perform(get("/api/periods/2026-11/closing-checklist"), AUDITOR)
                .andExpect(jsonPath("$.readyToLock").value(false))
                .andExpect(jsonPath("$.items[?(@.code == 'NO_DRAFT_JOURNALS')].passed").value(false))
                .andExpect(jsonPath("$.items[?(@.code == 'NO_UNPOSTED_APPROVED')].passed").value(false))
                .andExpect(jsonPath("$.items[?(@.code == 'TRIAL_BALANCE_BALANCED')].passed").value(true))
                .andExpect(jsonPath("$.items[?(@.code == 'PREVIOUS_PERIODS_LOCKED')].passed").value(true))
                .andExpect(jsonPath("$.items[?(@.code == 'PERIOD_ENDED')].passed").value(true));

        perform(post("/api/periods/2026-11/lock"), ADMIN)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("PERIOD_CHECKLIST_FAILED"))
                .andExpect(jsonPath("$.detail", containsString("JV-202611-00012")))
                .andExpect(jsonPath("$.failedItems.length()").value(2));
        assertThat(jdbc.queryForObject("""
                select count(*) from audit_event
                 where action = 'PERIOD_LOCKED' and outcome = 'REJECTED' and entity_ref = '2026-11'
                   and error_code = 'PERIOD_CHECKLIST_FAILED'""", Long.class))
                .isEqualTo(1);
    }

    @Test
    @Order(8)
    @DisplayName("Reopen wajib alasan; jurnal tertunda diselesaikan lalu cut-off ulang")
    void reopenAndResolvePendingJournals() throws Exception {
        postJson("/api/periods/2026-11/reopen", ADMIN, "{}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.reason").exists());
        postJson("/api/periods/2026-11/reopen", ADMIN, "{\"reason\":\"Gaji & listrik November belum diposting\"}")
                .andExpect(jsonPath("$.status").value("OPEN"));

        postJournal(11).andExpect(status().isOk());
        perform(post("/api/journals/12/approve"), APPROVER).andExpect(status().isOk());
        postJournal(12).andExpect(status().isOk());

        perform(post("/api/periods/2026-11/cut-off"), ADMIN).andExpect(jsonPath("$.status").value("SOFT_CLOSED"));
    }

    @Test
    @Order(9)
    @DisplayName("Lock sukses: status LOCKED, snapshot saldo, audit before/after")
    void lockSucceedsAfterChecklistPasses() throws Exception {
        perform(get("/api/periods/2026-11/closing-checklist"), AUDITOR)
                .andExpect(jsonPath("$.readyToLock").value(true));

        perform(post("/api/periods/2026-11/lock"), ADMIN)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.period.status").value("LOCKED"))
                .andExpect(jsonPath("$.period.lockedBy").value("admin"))
                .andExpect(jsonPath("$.snapshotAccounts", greaterThan(0)))
                .andExpect(jsonPath("$.totalDebitBalance").value(136650000.00))
                .andExpect(jsonPath("$.totalCreditBalance").value(136650000.00));

        assertThat(jdbc.queryForMap("""
                select before_state->>'status' as before, after_state->>'status' as after, actor
                  from audit_event where action = 'PERIOD_LOCKED' and outcome = 'SUCCESS' and entity_ref = '2026-11'"""))
                .containsEntry("before", "SOFT_CLOSED")
                .containsEntry("after", "LOCKED")
                .containsEntry("actor", "admin");
        perform(get("/api/periods/2026-11/trial-balance"), AUDITOR)
                .andExpect(jsonPath("$.source").value("SNAPSHOT"))
                .andExpect(jsonPath("$.totals.totalDebitBalance").value(136650000.00));
    }

    @Test
    @Order(10)
    @DisplayName("Koreksi setelah lock lewat reversal di periode terbuka; laporan November tidak berubah")
    void correctionAfterLockGoesToOpenPeriod() throws Exception {
        String reverseLocked = "{\"reason\":\"Nominal salah\",\"reversalDate\":\"2026-11-28\"}";
        postJson("/api/journals/10/reverse", APPROVER, reverseLocked).andExpect(status().isBadRequest()); // tanpa key
        mvc.perform(post("/api/journals/10/reverse").with(APPROVER)
                        .header("Idempotency-Key", "it-" + UUID.randomUUID())
                        .contentType("application/json").content(reverseLocked))
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("PERIOD_LOCKED"));

        mvc.perform(post("/api/journals/10/reverse").with(APPROVER)
                        .header("Idempotency-Key", "it-" + UUID.randomUUID())
                        .contentType("application/json")
                        .content("{\"reason\":\"Nominal seharusnya 1.250.000\",\"reversalDate\":\"2026-12-02\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.originalStatus").value("REVERSED"))
                .andExpect(jsonPath("$.reversalNumber").value("JV-202611-00010-R"));

        // Snapshot & TB per 30 Nov tetap sama; koreksi baru terlihat per Desember
        perform(get("/api/trial-balance").param("asOf", "2026-11-30"), AUDITOR)
                .andExpect(jsonPath("$.totals.totalDebitBalance").value(136650000.00));
        perform(get("/api/reconciliation").param("asOf", "2026-12-31"), AUDITOR)
                .andExpect(jsonPath("$.status").value("RECONCILED"));
    }

    @Test
    @Order(11)
    @DisplayName("Auto-reversal akrual November di tanggal 1 Desember, aman diulang")
    void autoReversalOfAccruals() throws Exception {
        perform(post("/api/periods/2026-11/auto-reversals"), ADMIN)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.targetPeriod").value("2026-12"))
                .andExpect(jsonPath("$.reversalDate").value("2026-12-01"))
                .andExpect(jsonPath("$.reversedCount").value(1))
                .andExpect(jsonPath("$.reversals[0].originalId").value(accrualId));
        perform(post("/api/periods/2026-11/auto-reversals"), ADMIN)
                .andExpect(jsonPath("$.reversedCount").value(0));
        // Saldo beban akrual kembali nol per Desember
        perform(get("/api/trial-balance").param("asOf", "2026-12-31"), AUDITOR)
                .andExpect(jsonPath("$.lines[?(@.code == '2102')].debitBalance").value(0.00))
                .andExpect(jsonPath("$.totals.balanced").value(true));
    }

    @Test
    @Order(12)
    @DisplayName("Unlock: urutan periode dijaga, alasan wajib & tercatat, snapshot dihapus")
    void unlockRequiresReasonAndOrder() throws Exception {
        postJson("/api/periods/2026-10/unlock", ADMIN, UNLOCK_REASON)
                .andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.code").value("PERIOD_SEQUENCE_VIOLATION"));
        postJson("/api/periods/2026-11/unlock", ADMIN, "{\"reason\":\"\"}")
                .andExpect(status().isBadRequest());

        postJson("/api/periods/2026-11/unlock", ADMIN, UNLOCK_REASON)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SOFT_CLOSED"))
                .andExpect(jsonPath("$.unlockedBy").value("admin"))
                .andExpect(jsonPath("$.unlockReason").value("Koreksi nilai pendapatan jasa (memo CFO-21)"));

        assertThat(jdbc.queryForObject("""
                select reason from audit_event
                 where action = 'PERIOD_UNLOCKED' and outcome = 'SUCCESS' and entity_ref = '2026-11'""", String.class))
                .isEqualTo("Koreksi nilai pendapatan jasa (memo CFO-21)");
        assertThat(jdbc.queryForObject("""
                select count(*) from period_balance b join accounting_period p on p.id = b.period_id
                 where p.code = '2026-11'""", Long.class)).isZero();
    }

    @Test
    @Order(13)
    @DisplayName("Envers menyimpan setiap versi periode beserta user")
    void periodRevisionsAreKept() throws Exception {
        String json = perform(get("/api/periods/2026-11/revisions"), AUDITOR)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<String> statuses = JsonPath.read(json, "$[*].state.status");
        List<String> users = JsonPath.read(json, "$[*].changedBy");
        assertThat(statuses).containsExactly("SOFT_CLOSED", "OPEN", "SOFT_CLOSED", "LOCKED", "SOFT_CLOSED");
        assertThat(users).containsOnly("admin");
    }

    private long deniedCount(String actor) {
        Long count = jdbc.queryForObject(
                "select count(*) from audit_event where outcome = 'DENIED' and actor = ?", Long.class, actor);
        return count == null ? 0 : count;
    }
}
