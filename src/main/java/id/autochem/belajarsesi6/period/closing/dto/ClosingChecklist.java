package id.autochem.belajarsesi6.period.closing.dto;

import id.autochem.belajarsesi6.period.PeriodStatus;

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
public record ClosingChecklist(String period, LocalDate from, LocalDate to, PeriodStatus status,
                               boolean readyToLock, List<ChecklistItem> items) {

    public List<ChecklistItem> failedBlocking() {
        return items.stream().filter(i -> i.blocking() && !i.passed()).toList();
    }
}
