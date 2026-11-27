package id.autochem.belajarsesi6.audit.envers;

import java.time.Instant;
import java.util.Map;

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
/** Satu versi entity dari tabel *_aud beserta field yang berubah dibanding versi sebelumnya. */
public record RevisionResponse(Long revision, Instant changedAt, String changedBy, String revisionType,
                               Map<String, Object> state, Map<String, Change> changes) {

    public record Change(Object from, Object to) {
    }
}
