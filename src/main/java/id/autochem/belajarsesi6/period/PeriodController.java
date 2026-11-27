package id.autochem.belajarsesi6.period;

import id.autochem.belajarsesi6.period.dto.PeriodResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/periods")
@Tag(name = "Accounting Periods")
public class PeriodController {

    private final PeriodQueryService service;

    @GetMapping
    public List<PeriodResponse> list() {
        return service.list();
    }

    @GetMapping("/{code}")
    public PeriodResponse get(@PathVariable String code) {
        return service.get(code);
    }
}
