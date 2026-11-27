package id.autochem.belajarsesi6.journal.dto;

import id.autochem.belajarsesi6.journal.JournalType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

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
public record JournalRequest(
        @NotNull LocalDate journalDate,
        @NotBlank @Size(max = 255) String description,
        /* GENERAL (default) atau ADJUSTMENT. REVERSAL hanya dibuat lewat endpoint reverse. */
        JournalType journalType,
        /* Khusus ADJUSTMENT: dibalik otomatis di awal periode berikutnya (mis. akrual). */
        Boolean autoReverse,
        @NotEmpty @Size(min = 2, message = "jurnal minimal 2 baris") List<@Valid JournalLineRequest> lines) {

    public boolean autoReverseOrDefault() {
        return Boolean.TRUE.equals(autoReverse);
    }

    public JournalType typeOrDefault() {
        return journalType != null ? journalType : JournalType.GENERAL;
    }
}
