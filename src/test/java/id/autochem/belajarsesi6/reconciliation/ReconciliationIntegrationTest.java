package id.autochem.belajarsesi6.reconciliation;

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
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;

import java.sql.Statement;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
 * Rekonsiliasi TB -> GL -> jurnal. Data dirusak sengaja langsung di database (trigger dimatikan
 * per sesi) untuk mensimulasikan penyebab selisih umum, lalu dipastikan terdeteksi.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@Import({TestcontainersConfiguration.class, FixedClockConfiguration.class})
@TestPropertySource(properties = "it.context=reconciliation")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DisplayName("Sesi 6 - Rekonsiliasi Journal - GL - Trial Balance")
class ReconciliationIntegrationTest extends ApiTestSupport {

    @Test
    @Order(1)
    @DisplayName("Data seed terekonsiliasi: TB = GL = posted lines")
    void seedDataIsReconciled() throws Exception {
        perform(get("/api/reconciliation").param("asOf", "2026-11-30").param("includeMatched", "true"), AUDITOR)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RECONCILED"))
                .andExpect(jsonPath("$.totals.trialBalanceBalanced").value(true))
                .andExpect(jsonPath("$.totals.trialBalance.debit").value(183000000.00))
                .andExpect(jsonPath("$.totals.generalLedger.debit").value(183000000.00))
                .andExpect(jsonPath("$.totals.postedJournalLines.debit").value(183000000.00))
                .andExpect(jsonPath("$.findings[*].code", hasItem("UNPOSTED_JOURNALS_EXCLUDED")));
    }

    @Test
    @Order(2)
    @DisplayName("Trace akun Kas: TB -> ledger -> baris jurnal -> audit, baris draft dikecualikan")
    void traceAccount() throws Exception {
        perform(get("/api/reconciliation/accounts/1101/trace").param("asOf", "2026-11-30"), AUDITOR)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.consistent").value(true))
                .andExpect(jsonPath("$.entryCount").value(3))
                .andExpect(jsonPath("$.ledgerBalance").value(20000000.00))
                .andExpect(jsonPath("$.trialBalanceLine.debitBalance").value(20000000.00))
                .andExpect(jsonPath("$.entries[2].runningBalance").value(20000000.00))
                .andExpect(jsonPath("$.excludedUnpostedLines[*].reference",
                        containsInAnyOrder("JV-202610-00005", "JV-202611-00012")))
                .andExpect(jsonPath("$.journalAudit[0].events[0].action").value("JOURNAL_POSTED"));
    }

    @Test
    @Order(3)
    @DisplayName("Selisih sengaja: draft bocor ke GL, tanggal posting beda, reversal tanpa pembalik, drift periode LOCKED")
    void detectsCommonCausesOfMismatch() throws Exception {
        tamper(
                // 1. Jurnal DRAFT ikut masuk GL
                """
                insert into ledger_entry (journal_id, journal_line_id, account_id, posting_date, description, debit, credit)
                select journal_id, id, account_id, date '2026-11-25', 'bocor', debit, credit
                  from journal_line where journal_id = 12""",
                // 2. Filter periode tidak konsisten: posting_date dipindah ke Desember
                "update ledger_entry set posting_date = date '2026-12-01' where journal_id = 9",
                // 3. Jurnal REVERSED tanpa jurnal pembalik
                """
                update journal_entry set status = 'REVERSED', reversed_by = 'ghost', reversed_at = now()
                 where id = 8""",
                // 4. Data periode LOCKED diubah diam-diam (snapshot tidak lagi cocok)
                "update ledger_entry set debit = 23000000.00 where journal_id = 2 and debit > 0");

        String json = perform(get("/api/reconciliation").param("asOf", "2026-11-30"), AUDITOR)
                .andExpect(jsonPath("$.status").value("MISMATCH"))
                .andExpect(jsonPath("$.totals.trialBalanceBalanced").value(false))
                .andReturn().getResponse().getContentAsString();
        List<String> codes = JsonPath.read(json, "$.findings[*].code");
        assertThat(codes).contains("ACCOUNT_MISMATCH", "LEDGER_FROM_UNPOSTED_JOURNAL", "POSTING_DATE_MISMATCH",
                "REVERSAL_INTEGRITY", "LOCKED_PERIOD_DRIFT");

        List<String> leaked = JsonPath.read(json,
                "$.findings[?(@.code == 'LEDGER_FROM_UNPOSTED_JOURNAL')].items[*].reference");
        assertThat(leaked).containsExactly("JV-202611-00012");
        List<String> drift = JsonPath.read(json, "$.findings[?(@.code == 'LOCKED_PERIOD_DRIFT')].items[*].reference");
        assertThat(drift).contains("2026-10/1501");

        // Telusuri satu selisih: Peralatan Kantor (1501) GL tidak cocok dengan baris jurnal asal
        perform(get("/api/reconciliation/accounts/1501/trace").param("asOf", "2026-11-30"), AUDITOR)
                .andExpect(jsonPath("$.consistent").value(false))
                .andExpect(jsonPath("$.entries[0].journalNumber").value("JV-202610-00002"))
                .andExpect(jsonPath("$.entries[0].matchesJournalLine").value(false))
                .andExpect(jsonPath("$.ledgerBalance").value(23000000.00))
                .andExpect(jsonPath("$.postedLineBalance").value(24000000.00));

        // Trial balance sendiri juga memberi peringatan tidak seimbang
        perform(get("/api/trial-balance").param("asOf", "2026-11-30"), AUDITOR)
                .andExpect(jsonPath("$.totals.balanced").value(false))
                .andExpect(jsonPath("$.warnings[0]").value(org.hamcrest.Matchers.containsString("TIDAK seimbang")));
    }

    /** Jalankan SQL dengan trigger dimatikan (session_replication_role = replica) di satu koneksi. */
    private void tamper(String... statements) {
        jdbc.execute((ConnectionCallback<Void>) connection -> {
            try (Statement st = connection.createStatement()) {
                st.execute("set session_replication_role = replica");
                for (String sql : statements) {
                    st.execute(sql);
                }
                st.execute("set session_replication_role = origin");
            }
            return null;
        });
    }
}
