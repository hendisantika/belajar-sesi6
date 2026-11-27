package id.autochem.belajarsesi6.idempotency;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

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
public interface IdempotencyRepository extends JpaRepository<IdempotencyRecord, String> {

    /**
     * INSERT ... ON CONFLICT DO NOTHING: unique key di DB menjadi "kunci" idempotency.
     * Request kedua dengan key yang sama akan menunggu transaksi pertama selesai lalu mendapat 0.
     *
     * @return 1 bila key baru, 0 bila key sudah ada
     */
    @Modifying
    @Query(value = """
            INSERT INTO idempotency_key (idem_key, operation, request_hash, created_at, expires_at)
            VALUES (:key, :operation, :hash, :now, :expiresAt)
            ON CONFLICT (idem_key) DO NOTHING
            """, nativeQuery = true)
    int tryInsert(@Param("key") String key, @Param("operation") String operation, @Param("hash") String hash,
                  @Param("now") Instant now, @Param("expiresAt") Instant expiresAt);

    @Modifying
    @Query(value = """
            UPDATE idempotency_key SET response_status = :status, response_body = :body
            WHERE idem_key = :key
            """, nativeQuery = true)
    int saveResult(@Param("key") String key, @Param("status") int status, @Param("body") String body);

    @Modifying
    @Query(value = "DELETE FROM idempotency_key WHERE idem_key = :key AND expires_at < :now", nativeQuery = true)
    int deleteIfExpired(@Param("key") String key, @Param("now") Instant now);

    @Modifying
    @Query(value = "DELETE FROM idempotency_key WHERE expires_at < :now", nativeQuery = true)
    int deleteAllExpired(@Param("now") Instant now);
}
