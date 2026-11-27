package id.autochem.belajarsesi6.period;

import id.autochem.belajarsesi6.common.exception.BusinessException;
import id.autochem.belajarsesi6.common.exception.ErrorCode;
import id.autochem.belajarsesi6.journal.JournalType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

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
/**
 * Period closing rule. Dipanggil oleh SETIAP operasi tulis jurnal (create, update, approve,
 * post, reverse) sehingga tidak ada jurnal baru/perubahan di periode yang sudah ditutup.
 */
@Component
@RequiredArgsConstructor
public class PeriodGuard {

    private final AccountingPeriodRepository periods;

    @Transactional(readOnly = true)
    public AccountingPeriod assertOpen(LocalDate date, JournalType type) {
        AccountingPeriod period = periods.findContaining(date)
                .orElseThrow(() -> new BusinessException(ErrorCode.PERIOD_NOT_FOUND,
                        "Tidak ada periode akuntansi untuk tanggal " + date));
        if (period.getStatus().allowsPosting(type)) {
            return period;
        }
        if (period.getStatus() == PeriodStatus.LOCKED) {
            throw new BusinessException(ErrorCode.PERIOD_LOCKED,
                    "Periode " + period.getCode() + " sudah LOCKED (oleh " + period.getLockedBy() + "); jurnal tanggal "
                            + date + " ditolak. Koreksi dicatat sebagai adjustment di periode yang masih terbuka")
                    .with("period", period.getCode())
                    .with("periodStatus", period.getStatus());
        }
        throw new BusinessException(ErrorCode.PERIOD_CUT_OFF,
                "Periode " + period.getCode() + " sudah cut-off; hanya jurnal ADJUSTMENT/REVERSAL yang diterima, bukan "
                        + type)
                .with("period", period.getCode())
                .with("periodStatus", period.getStatus());
    }
}
