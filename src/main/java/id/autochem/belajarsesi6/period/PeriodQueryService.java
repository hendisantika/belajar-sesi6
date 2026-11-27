package id.autochem.belajarsesi6.period;

import id.autochem.belajarsesi6.common.exception.NotFoundException;
import id.autochem.belajarsesi6.period.dto.PeriodResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PeriodQueryService {

    private final AccountingPeriodRepository periods;

    public List<PeriodResponse> list() {
        return periods.findAllByOrderByStartDateAsc().stream().map(PeriodResponse::from).toList();
    }

    public PeriodResponse get(String code) {
        return PeriodResponse.from(require(code));
    }

    public AccountingPeriod require(String code) {
        return periods.findByCode(code).orElseThrow(() -> new NotFoundException("Periode", code));
    }
}
