package id.autochem.belajarsesi6.period.closing.dto;

import id.autochem.belajarsesi6.reversal.dto.ReversalResult;

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
 * Time: 19.48
 */
public record AutoReversalResult(String period, String targetPeriod, LocalDate reversalDate, int reversedCount,
                                 List<ReversalResult> reversals) {
}
