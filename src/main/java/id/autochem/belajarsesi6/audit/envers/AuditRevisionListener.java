package id.autochem.belajarsesi6.audit.envers;

import id.autochem.belajarsesi6.security.CurrentUser;
import org.hibernate.envers.RevisionListener;

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
public class AuditRevisionListener implements RevisionListener {

    @Override
    public void newRevision(Object revisionEntity) {
        ((AuditRevision) revisionEntity).setUsername(CurrentUser.username());
    }
}
