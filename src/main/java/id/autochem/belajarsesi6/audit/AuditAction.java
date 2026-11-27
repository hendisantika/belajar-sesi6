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
public enum AuditAction {
    ACCOUNT_CREATED,
    ACCOUNT_ACTIVATED,
    ACCOUNT_DEACTIVATED,
    JOURNAL_CREATED,
    JOURNAL_UPDATED,
    JOURNAL_DELETED,
    JOURNAL_APPROVED,
    JOURNAL_RETURNED_TO_DRAFT,
    JOURNAL_POSTED,
    JOURNAL_REVERSED,
    PERIOD_CUT_OFF,
    PERIOD_REOPENED,
    PERIOD_LOCKED,
    PERIOD_UNLOCKED,
    PERIOD_AUTO_REVERSAL,
    ACCESS_DENIED
}
