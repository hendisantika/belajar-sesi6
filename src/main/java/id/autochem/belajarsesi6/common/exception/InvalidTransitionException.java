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
public class InvalidTransitionException extends BusinessException {

    public InvalidTransitionException(ErrorCode code, Enum<?> from, Enum<?> to) {
        super(code, from + " -> " + to + " tidak diizinkan");
        with("from", from);
        with("to", to);
    }
}
