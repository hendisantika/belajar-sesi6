package id.autochem.belajarsesi6.idempotency;

import id.autochem.belajarsesi6.common.exception.BusinessException;
import id.autochem.belajarsesi6.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.util.HexFormat;
import java.util.function.Supplier;
import java.util.regex.Pattern;

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
@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private static final Pattern VALID_KEY = Pattern.compile("[A-Za-z0-9._:-]{8,100}");

    private final IdempotencyRepository repository;
    private final IdempotencyProperties properties;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    /**
     * Menjalankan aksi satu kali per Idempotency-Key. Key + hasil disimpan dalam transaksi
     * yang sama dengan aksi, sehingga bila aksi gagal key ikut rollback dan boleh dicoba ulang.
     *
     * @param key         Idempotency-Key dari client
     * @param operation   nama operasi, mis. POST_JOURNAL
     * @param fingerprint identitas payload; key yang sama dengan payload berbeda ditolak
     */
    @Transactional
    public <T> IdempotentResult<T> execute(String key, String operation, String fingerprint,
                                           Class<T> type, Supplier<T> action) {
        validateKey(key);
        Instant now = clock.instant();
        String hash = sha256(operation + "|" + fingerprint);

        repository.deleteIfExpired(key, now);
        if (repository.tryInsert(key, operation, hash, now, now.plus(properties.ttl())) == 0) {
            return replay(key, operation, hash, type);
        }

        T result = action.get();
        repository.saveResult(key, 200, objectMapper.writeValueAsString(result));
        return new IdempotentResult<>(result, false);
    }

    @Transactional
    public int purgeExpired() {
        return repository.deleteAllExpired(clock.instant());
    }

    private <T> IdempotentResult<T> replay(String key, String operation, String hash, Class<T> type) {
        IdempotencyRecord stored = repository.findById(key)
                .orElseThrow(() -> new BusinessException(ErrorCode.CONCURRENT_MODIFICATION,
                        "Request dengan Idempotency-Key " + key + " sedang diproses"));
        if (!stored.getOperation().equals(operation) || !stored.getRequestHash().equals(hash)) {
            throw new BusinessException(ErrorCode.IDEMPOTENCY_KEY_REUSED,
                    "Idempotency-Key " + key + " sudah dipakai untuk " + stored.getOperation()
                            + " dengan payload berbeda");
        }
        if (stored.getResponseBody() == null) {
            throw new BusinessException(ErrorCode.CONCURRENT_MODIFICATION,
                    "Request dengan Idempotency-Key " + key + " sedang diproses");
        }
        log.info("Idempotent replay for key {} ({})", key, operation);
        return new IdempotentResult<>(objectMapper.readValue(stored.getResponseBody(), type), true);
    }

    private static void validateKey(String key) {
        if (key == null || !VALID_KEY.matcher(key).matches()) {
            throw new BusinessException(ErrorCode.IDEMPOTENCY_KEY_INVALID,
                    "Idempotency-Key wajib 8-100 karakter [A-Za-z0-9._:-], disarankan UUID");
        }
    }

    private static String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 tidak tersedia", e);
        }
    }
}
