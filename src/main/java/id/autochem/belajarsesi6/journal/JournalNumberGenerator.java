package id.autochem.belajarsesi6.journal;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

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
@Component
@RequiredArgsConstructor
public class JournalNumberGenerator {

    private static final DateTimeFormatter YEAR_MONTH = DateTimeFormatter.ofPattern("yyyyMM");

    private final JournalEntryRepository journals;

    /** Format: JV-yyyyMM-00001 (transaksi) / AJ-yyyyMM-00001 (penyesuaian); sequence PostgreSQL. */
    public String next(LocalDate journalDate, JournalType type) {
        return "%s-%s-%05d".formatted(type.prefix(), YEAR_MONTH.format(journalDate), journals.nextJournalSequence());
    }

    /** Nomor jurnal pembalik: nomor asal + "-R". */
    public String reversalOf(String originalNumber) {
        return originalNumber + "-R";
    }
}
