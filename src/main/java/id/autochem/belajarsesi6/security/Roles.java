package id.autochem.belajarsesi6.security;

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
public final class Roles {

    /** Controller / kepala akuntansi: master data, cut-off, lock & unlock periode. */
    public static final String ADMIN = "ADMIN";
    /** Maker: membuat & mengubah jurnal DRAFT. */
    public static final String ACCOUNTANT = "ACCOUNTANT";
    /** Checker: menyetujui, memposting, dan membalik jurnal. */
    public static final String APPROVER = "APPROVER";
    /** Auditor internal/eksternal: read-only + akses audit trail. */
    public static final String AUDITOR = "AUDITOR";

    public static final String CAN_WRITE_JOURNAL = "hasAnyRole('ACCOUNTANT', 'ADMIN')";
    public static final String CAN_APPROVE_JOURNAL = "hasAnyRole('APPROVER', 'ADMIN')";
    public static final String CAN_MANAGE_PERIOD = "hasRole('ADMIN')";
    public static final String CAN_READ_AUDIT = "hasAnyRole('AUDITOR', 'ADMIN')";

    private Roles() {
    }
}
