package id.autochem.belajarsesi6.trialbalance;

import com.jayway.jsonpath.JsonPath;
import id.autochem.belajarsesi6.support.ApiTestSupport;
import id.autochem.belajarsesi6.support.FixedClockConfiguration;
import id.autochem.belajarsesi6.support.TestcontainersConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, FixedClockConfiguration.class})
@DisplayName("Sesi 6 - Trial Balance dari agregasi GL")
class TrialBalanceIntegrationTest extends ApiTestSupport {

    @Test
    @DisplayName("Trial balance Oktober (seed) seimbang & sesuai perhitungan manual")
    void octoberTrialBalanceIsBalanced() throws Exception {
        perform(get("/api/trial-balance").param("asOf", "2026-10-31"), AUDITOR)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value("LIVE"))
                .andExpect(jsonPath("$.accountCount").value(7))
                .andExpect(jsonPath("$.totals.balanced").value(true))
                .andExpect(jsonPath("$.totals.totalDebitBalance").value(108500000.00))
                .andExpect(jsonPath("$.totals.totalCreditBalance").value(108500000.00))
                .andExpect(jsonPath("$.totals.difference").value(0))
                .andExpect(jsonPath("$.lines[?(@.code == '1102')].debitBalance").value(76000000.00))
                // Akumulasi penyusutan: akun kontra aset bersaldo normal kredit
                .andExpect(jsonPath("$.lines[?(@.code == '1502')].creditBalance").value(500000.00))
                .andExpect(jsonPath("$.lines[?(@.code == '1502')].abnormal").value(false))
                .andExpect(jsonPath("$.subtotals[?(@.accountType == 'EQUITY')].creditBalance").value(100000000.00))
                .andExpect(jsonPath("$.journalStatusesIncluded", hasSize(2)))
                .andExpect(jsonPath("$.warnings", hasSize(0)));
    }

    @Test
    @DisplayName("Jurnal DRAFT & APPROVED tidak ikut; baru masuk setelah POSTED")
    void draftAndApprovedJournalsAreExcluded() throws Exception {
        BigDecimal before = totalDebit("2026-11-30");

        createJournal("2026-11-26", "GENERAL", false, "5401", "1102", "777000.00");          // DRAFT
        long approved = approvedJournal("2026-11-27", "GENERAL", false, "5401", "1102", "333000.00");

        assertThat(totalDebit("2026-11-30")).isEqualByComparingTo(before);

        postJournal(approved).andExpect(status().isOk());
        assertThat(totalDebit("2026-11-30")).isEqualByComparingTo(before.add(new BigDecimal("333000.00")));
        perform(get("/api/trial-balance").param("asOf", "2026-11-30"), AUDITOR)
                .andExpect(jsonPath("$.totals.balanced").value(true));
    }

    @Test
    @DisplayName("Filter periode: mutasi setelah asOf tidak ikut")
    void asOfFilter() throws Exception {
        perform(get("/api/trial-balance").param("asOf", "2026-10-04"), AUDITOR)
                .andExpect(jsonPath("$.totals.totalDebitBalance").value(100000000.00))
                .andExpect(jsonPath("$.accountCount").value(2));
    }

    @Test
    @DisplayName("Worksheet Oktober: sebelum penyesuaian, penyesuaian, sesudah penyesuaian")
    void octoberWorksheet() throws Exception {
        perform(get("/api/trial-balance/periods/2026-10/worksheet"), AUDITOR)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.periodStatus").value("LOCKED"))
                .andExpect(jsonPath("$.totals.unadjusted.debit").value(108000000.00))
                .andExpect(jsonPath("$.totals.adjustment.debit").value(500000.00))
                .andExpect(jsonPath("$.totals.adjusted.debit").value(108500000.00))
                .andExpect(jsonPath("$.totals.adjusted.credit").value(108500000.00))
                .andExpect(jsonPath("$.unadjustedBalanced").value(true))
                .andExpect(jsonPath("$.adjustedBalanced").value(true))
                .andExpect(jsonPath("$.periodNetIncome").value(7500000.00));
    }

    @Test
    @DisplayName("Snapshot periode LOCKED = trial balance live per akhir periode")
    void lockedPeriodSnapshot() throws Exception {
        perform(get("/api/periods/2026-10/trial-balance"), AUDITOR)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value("SNAPSHOT"))
                .andExpect(jsonPath("$.totals.balanced").value(true))
                .andExpect(jsonPath("$.totals.totalDebitBalance").value(108500000.00));
        perform(get("/api/periods/2026-11/trial-balance"), AUDITOR)
                .andExpect(status().isConflict());
    }

    @Test
    void includeZeroListsAllActiveAccounts() throws Exception {
        perform(get("/api/trial-balance").param("asOf", "2026-10-31").param("includeZero", "true"), AUDITOR)
                .andExpect(jsonPath("$.accountCount").value(19))
                .andExpect(jsonPath("$.totals.balanced").value(true));
    }

    @Test
    void csvExport() throws Exception {
        perform(get("/api/trial-balance/export").param("asOf", "2026-10-31"), AUDITOR)
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("1102,\"Bank BCA\",ASSET,DEBIT,76000000.00,0.00")))
                .andExpect(content().string(containsString(",TOTAL,,,108500000.00,108500000.00")));
    }

    @Test
    void requiresAuthentication() throws Exception {
        mvc.perform(get("/api/trial-balance")).andExpect(status().isUnauthorized());
    }

    private BigDecimal totalDebit(String asOf) throws Exception {
        String json = perform(get("/api/trial-balance").param("asOf", asOf), AUDITOR)
                .andReturn().getResponse().getContentAsString();
        return new BigDecimal(JsonPath.read(json, "$.totals.totalDebit").toString());
    }
}
