package id.autochem.belajarsesi6.common.web;

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
public final class Headers {

    /** Kunci idempotency dari client untuk aksi posting & reversal. */
    public static final String IDEMPOTENCY_KEY = "Idempotency-Key";

    /** Penanda response diambil dari hasil tersimpan (replay idempotency). */
    public static final String IDEMPOTENT_REPLAYED = "Idempotent-Replayed";

    /** ID korelasi request; ikut tercatat di audit event untuk penelusuran. */
    public static final String CORRELATION_ID = "X-Correlation-Id";

    private Headers() {
    }
}
