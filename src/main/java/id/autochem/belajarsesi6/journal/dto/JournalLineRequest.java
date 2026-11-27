package id.autochem.belajarsesi6.journal.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

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
public record JournalLineRequest(
        @NotBlank String accountCode,
        @Size(max = 255) String description,
        @PositiveOrZero @Digits(integer = 17, fraction = 2) BigDecimal debit,
        @PositiveOrZero @Digits(integer = 17, fraction = 2) BigDecimal credit) {
}
