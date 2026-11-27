package id.autochem.belajarsesi6.period.closing;

import id.autochem.belajarsesi6.period.closing.dto.AutoReversalResult;
import id.autochem.belajarsesi6.period.closing.dto.ClosingChecklist;
import id.autochem.belajarsesi6.period.closing.dto.LockResult;
import id.autochem.belajarsesi6.period.closing.dto.PeriodActionRequest;
import id.autochem.belajarsesi6.period.closing.dto.PeriodBalanceResponse;
import id.autochem.belajarsesi6.period.dto.PeriodResponse;
import id.autochem.belajarsesi6.security.Roles;
import id.autochem.belajarsesi6.trialbalance.dto.TrialBalanceReport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
 * Time: 19.48
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/periods/{code}")
@Tag(name = "Period Closing", description = "Cut-off, closing checklist, lock/unlock (ADMIN) & snapshot saldo")
public class PeriodClosingController {

    private final PeriodClosingService service;

    @GetMapping("/closing-checklist")
    @Operation(summary = "Checklist sebelum lock: cut-off, jurnal draft/approved, TB seimbang, GL cocok, urutan periode")
    public ClosingChecklist checklist(@PathVariable String code) {
        return service.checklist(code);
    }

    @PostMapping("/cut-off")
    @PreAuthorize(Roles.CAN_MANAGE_PERIOD)
    @Operation(summary = "OPEN -> SOFT_CLOSED: transaksi harian ditutup, hanya adjustment/reversal (ADMIN)")
    public PeriodResponse cutOff(@PathVariable String code, Authentication authentication) {
        return service.cutOff(code, authentication.getName());
    }

    @PostMapping("/reopen")
    @PreAuthorize(Roles.CAN_MANAGE_PERIOD)
    @Operation(summary = "SOFT_CLOSED -> OPEN dengan alasan (ADMIN)")
    public PeriodResponse reopen(@PathVariable String code, @Valid @RequestBody PeriodActionRequest request) {
        return service.reopen(code, request.reason());
    }

    @PostMapping("/lock")
    @PreAuthorize(Roles.CAN_MANAGE_PERIOD)
    @Operation(summary = "SOFT_CLOSED -> LOCKED setelah checklist lolos + snapshot saldo (ADMIN)")
    public LockResult lock(@PathVariable String code, Authentication authentication) {
        return service.lock(code, authentication.getName());
    }

    @PostMapping("/unlock")
    @PreAuthorize(Roles.CAN_MANAGE_PERIOD)
    @Operation(summary = "LOCKED -> SOFT_CLOSED; alasan wajib & tercatat di audit (ADMIN)")
    public PeriodResponse unlock(@PathVariable String code, @Valid @RequestBody PeriodActionRequest request,
                                 Authentication authentication) {
        return service.unlock(code, request.reason(), authentication.getName());
    }

    @PostMapping("/auto-reversals")
    @PreAuthorize(Roles.CAN_MANAGE_PERIOD)
    @Operation(summary = "Balik jurnal ADJUSTMENT auto-reverse (akrual) di tanggal pertama periode berikutnya")
    public AutoReversalResult autoReverse(@PathVariable String code, Authentication authentication) {
        return service.autoReverse(code, authentication.getName());
    }

    @GetMapping("/balances")
    @Operation(summary = "Snapshot saldo per akun yang dibuat saat periode di-lock")
    public List<PeriodBalanceResponse> balances(@PathVariable String code) {
        return service.snapshot(code);
    }

    @GetMapping("/trial-balance")
    @Operation(summary = "Neraca saldo periode LOCKED dari snapshot (source = SNAPSHOT)")
    public TrialBalanceReport snapshotTrialBalance(@PathVariable String code) {
        return service.snapshotTrialBalance(code);
    }
}
