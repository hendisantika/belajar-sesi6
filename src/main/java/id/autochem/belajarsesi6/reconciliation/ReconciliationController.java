package id.autochem.belajarsesi6.reconciliation;

import id.autochem.belajarsesi6.reconciliation.dto.AccountTraceReport;
import id.autochem.belajarsesi6.reconciliation.dto.ReconciliationReport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * Created by IntelliJ IDEA.
 * Project : belajar-sesi6
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Link : s.id/hendisantika
 * Date: 27/11/26
 * Time: 19.52
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reconciliation")
@Tag(name = "Reconciliation", description = "Rekonsiliasi Trial Balance -> General Ledger -> Jurnal -> Audit trail")
public class ReconciliationController {

    private final ReconciliationService service;

    @GetMapping
    @Operation(summary = "Bandingkan TB, GL, dan baris jurnal per akun + diagnosa penyebab selisih umum")
    public ReconciliationReport reconcile(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf,
            @RequestParam(defaultValue = "false") boolean includeMatched) {
        return service.reconcile(asOf, includeMatched);
    }

    @GetMapping("/accounts/{code}/trace")
    @Operation(summary = "Telusuri satu akun: baris TB -> ledger entry -> baris jurnal asal -> audit trail jurnal")
    public AccountTraceReport trace(
            @PathVariable String code,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf) {
        return service.trace(code, asOf);
    }
}
