package id.autochem.belajarsesi6.reversal.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

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
public record ReverseRequest(
        @NotBlank @Size(max = 200) String reason,
        /* Opsional; default hari ini. Harus di periode terbuka dan tidak sebelum tanggal jurnal asal. */
        LocalDate reversalDate) {
}
