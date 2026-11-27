package id.autochem.belajarsesi6.common.exception;

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
public class NotFoundException extends BusinessException {

    public NotFoundException(String resource, Object id) {
        super(ErrorCode.RESOURCE_NOT_FOUND, resource + " '" + id + "' tidak ditemukan");
    }
}
