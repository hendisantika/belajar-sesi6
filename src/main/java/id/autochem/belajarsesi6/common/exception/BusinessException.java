package id.autochem.belajarsesi6.common.exception;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

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
public class BusinessException extends RuntimeException {

    private final ErrorCode code;
    private final Map<String, Object> properties = new LinkedHashMap<>();
    /** Penanda bahwa penolakan ini sudah dicatat ke audit log (hindari pencatatan ganda). */
    private boolean audited;

    public BusinessException(ErrorCode code, String message) {
        super(message);
        this.code = code;
    }

    public ErrorCode getCode() {
        return code;
    }

    /** Detail tambahan untuk ProblemDetail, mis. daftar checklist yang gagal. */
    public BusinessException with(String key, Object value) {
        properties.put(key, value);
        return this;
    }

    public Map<String, Object> getProperties() {
        return Collections.unmodifiableMap(properties);
    }

    public boolean isAudited() {
        return audited;
    }

    public void markAudited() {
        this.audited = true;
    }
}
