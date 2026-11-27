package id.autochem.belajarsesi6.audit;

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
public enum AuditOutcome {
    /** Aksi berhasil dan ikut ter-commit bersama transaksi bisnis. */
    SUCCESS,
    /** Aksi ditolak aturan bisnis (mis. posting ke periode LOCKED). */
    REJECTED,
    /** Aksi ditolak otorisasi (role tidak berwenang). */
    DENIED
}
