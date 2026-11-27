package id.autochem.belajarsesi6.period.closing.dto;

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
 * Time: 19.48
 */
/** Alasan wajib untuk aksi sensitif (unlock / reopen periode); tercatat di audit trail. */
public record PeriodActionRequest(
        @NotBlank(message = "alasan wajib diisi")
        @Size(min = 10, max = 500, message = "alasan 10-500 karakter") String reason) {
}
