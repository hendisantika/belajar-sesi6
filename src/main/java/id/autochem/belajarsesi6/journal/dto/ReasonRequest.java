package id.autochem.belajarsesi6.journal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

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
public record ReasonRequest(@NotBlank @Size(min = 10, max = 500, message = "alasan 10-500 karakter") String reason) {
}
