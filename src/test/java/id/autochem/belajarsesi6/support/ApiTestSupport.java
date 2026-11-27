package id.autochem.belajarsesi6.support;

import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
/** Helper HTTP untuk integration test: kredensial demo + operasi jurnal yang sering dipakai. */
public abstract class ApiTestSupport {

    protected static final RequestPostProcessor ADMIN = httpBasic("admin", "admin123");
    protected static final RequestPostProcessor ACCOUNTANT = httpBasic("accountant1", "accountant123");
    protected static final RequestPostProcessor APPROVER = httpBasic("approver1", "approver123");
    protected static final RequestPostProcessor AUDITOR = httpBasic("auditor1", "auditor123");

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected JdbcTemplate jdbc;

    protected ResultActions perform(MockHttpServletRequestBuilder request, RequestPostProcessor user) throws Exception {
        return mvc.perform(request.with(user));
    }

    protected ResultActions postJson(String url, RequestPostProcessor user, String body) throws Exception {
        return mvc.perform(post(url).with(user).contentType(MediaType.APPLICATION_JSON).content(body));
    }

    protected ResultActions postJournal(long id) throws Exception {
        return mvc.perform(post("/api/journals/{id}/post", id).with(APPROVER)
                .header("Idempotency-Key", "it-" + UUID.randomUUID()));
    }

    /** Buat jurnal (DRAFT) oleh accountant, lalu kembalikan id-nya. */
    protected long createJournal(String date, String type, boolean autoReverse, String debitAccount,
                                 String creditAccount, String amount) throws Exception {
        String body = """
                {"journalDate":"%s","description":"IT %s %s","journalType":"%s","autoReverse":%s,
                 "lines":[{"accountCode":"%s","debit":%s},{"accountCode":"%s","credit":%s}]}
                """.formatted(date, type, date, type, autoReverse, debitAccount, amount, creditAccount, amount);
        String json = postJson("/api/journals", ACCOUNTANT, body)
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(json, "$.id")).longValue();
    }

    protected long approvedJournal(String date, String type, boolean autoReverse, String debitAccount,
                                   String creditAccount, String amount) throws Exception {
        long id = createJournal(date, type, autoReverse, debitAccount, creditAccount, amount);
        perform(post("/api/journals/{id}/approve", id), APPROVER).andExpect(status().isOk());
        return id;
    }

    protected long postedJournal(String date, String type, boolean autoReverse, String debitAccount,
                                 String creditAccount, String amount) throws Exception {
        long id = approvedJournal(date, type, autoReverse, debitAccount, creditAccount, amount);
        postJournal(id).andExpect(status().isOk());
        return id;
    }

    protected long auditCount(String action, String outcome, long entityId) {
        Long count = jdbc.queryForObject(
                "select count(*) from audit_event where action = ? and outcome = ? and entity_id = ?",
                Long.class, action, outcome, entityId);
        return count == null ? 0 : count;
    }
}
