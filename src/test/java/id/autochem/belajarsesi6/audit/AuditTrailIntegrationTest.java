package id.autochem.belajarsesi6.audit;

import com.jayway.jsonpath.JsonPath;
import id.autochem.belajarsesi6.support.ApiTestSupport;
import id.autochem.belajarsesi6.support.FixedClockConfiguration;
import id.autochem.belajarsesi6.support.TestcontainersConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.greaterThan;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
@DisplayName("Sesi 6 - Audit trail terstruktur, Spring Data Auditing & Envers")
class AuditTrailIntegrationTest extends ApiTestSupport {

    @Test
    @DisplayName("Posting sukses: actor, waktu, before/after tercatat; @CreatedBy/@LastModifiedBy terisi otomatis")
    void successfulPostingIsAudited() throws Exception {
        long id = postedJournal("2026-11-26", "GENERAL", false, "5401", "1101", "125000.00");

        Map<String, Object> event = jdbc.queryForMap("""
                select actor, actor_roles, entity_ref, before_state->>'status' as before,
                       after_state->>'status' as after, (after_state->>'ledgerEntries')::int as ledger,
                       occurred_at, correlation_id
                  from audit_event where action = 'JOURNAL_POSTED' and outcome = 'SUCCESS' and entity_id = ?""", id);
        assertThat(event)
                .containsEntry("actor", "approver1")
                .containsEntry("actor_roles", "APPROVER")
                .containsEntry("before", "APPROVED")
                .containsEntry("after", "POSTED")
                .containsEntry("ledger", 2);
        assertThat(event.get("occurred_at")).isNotNull();
        assertThat(event.get("correlation_id")).isNotNull();

        perform(get("/api/journals/{id}", id), AUDITOR)
                .andExpect(jsonPath("$.createdBy").value("accountant1"))
                .andExpect(jsonPath("$.updatedBy").value("approver1"))
                .andExpect(jsonPath("$.postedBy").value("approver1"));

        // Jejak lengkap: CREATED -> APPROVED -> POSTED
        String trail = perform(get("/api/audit-events/JOURNAL_ENTRY/{id}", id), AUDITOR)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<String> actions = JsonPath.read(trail, "$[*].action");
        assertThat(actions).containsExactly("JOURNAL_CREATED", "JOURNAL_APPROVED", "JOURNAL_POSTED");
    }

    @Test
    @DisplayName("Penolakan tetap tercatat walau transaksi bisnis rollback, lengkap dengan correlation id")
    void rejectionSurvivesRollback() throws Exception {
        String correlationId = "it-corr-" + UUID.randomUUID().toString().substring(0, 8);
        mvc.perform(post("/api/journals/5/post").with(APPROVER)
                        .header("Idempotency-Key", "it-" + UUID.randomUUID())
                        .header("X-Correlation-Id", correlationId))
                .andExpect(status().isUnprocessableContent())
                .andExpect(header().string("X-Correlation-Id", correlationId));

        assertThat(jdbc.queryForMap("""
                select action, outcome, error_code, entity_id, reason from audit_event
                 where correlation_id = ?""", correlationId))
                .containsEntry("action", "JOURNAL_POSTED")
                .containsEntry("outcome", "REJECTED")
                .containsEntry("error_code", "PERIOD_LOCKED")
                .containsEntry("entity_id", 5L);

        perform(get("/api/audit-events/rejections"), AUDITOR)
                .andExpect(jsonPath("$.content[0].outcome").exists())
                .andExpect(jsonPath("$.page.totalElements", greaterThan(0)));
    }

    @Test
    @DisplayName("Tabel audit_event append-only: UPDATE / DELETE / TRUNCATE ditolak database")
    void auditEventIsAppendOnly() {
        assertThatThrownBy(() -> jdbc.update("update audit_event set actor = 'hacker' where id = 1"))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("append-only");
        assertThatThrownBy(() -> jdbc.update("delete from audit_event where id = 1"))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("append-only");
        assertThatThrownBy(() -> jdbc.execute("truncate audit_event"))
                .isInstanceOf(DataAccessException.class);
    }

    @Test
    @DisplayName("Audit trail hanya untuk AUDITOR/ADMIN; percobaan lain tercatat DENIED")
    void auditAccessIsRestricted() throws Exception {
        perform(get("/api/audit-events"), ACCOUNTANT).andExpect(status().isForbidden());
        perform(get("/api/audit-events").param("action", "PERIOD_LOCKED").param("entityRef", "2026-10"), ADMIN)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].after.status").value("LOCKED"));
        assertThat(jdbc.queryForObject("""
                select count(*) from audit_event where outcome = 'DENIED' and actor = 'accountant1'
                   and entity_ref like 'GET /api/audit-events%'""", Long.class)).isPositive();
    }

    @Test
    @DisplayName("Envers: setiap versi jurnal tersimpan beserta user & field yang berubah")
    void journalRevisions() throws Exception {
        long id = postedJournal("2026-11-26", "GENERAL", false, "5401", "1101", "99000.00");
        String json = perform(get("/api/journals/{id}/revisions", id), AUDITOR)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        List<String> types = JsonPath.read(json, "$[*].revisionType");
        List<String> users = JsonPath.read(json, "$[*].changedBy");
        List<String> statuses = JsonPath.read(json, "$[*].state.status");
        assertThat(types).containsExactly("ADD", "MOD", "MOD");
        assertThat(users).containsExactly("accountant1", "approver1", "approver1");
        assertThat(statuses).containsExactly("DRAFT", "APPROVED", "POSTED");
        assertThat((String) JsonPath.read(json, "$[2].changes.status.from")).isEqualTo("APPROVED");
    }
}
