package id.autochem.belajarsesi6.period.closing.dto;

/**
 * Created by IntelliJ IDEA.
 * Project : belajar-sesi6
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Link : s.id/hendisantika
 * Date: 27/11/26
 * Time: 19.48
 */
public record ChecklistItem(String code, String description, boolean passed, boolean blocking, String detail) {

    public static ChecklistItem blocking(String code, String description, boolean passed, String detail) {
        return new ChecklistItem(code, description, passed, true, detail);
    }

    public static ChecklistItem advisory(String code, String description, boolean passed, String detail) {
        return new ChecklistItem(code, description, passed, false, detail);
    }
}
