package id.autochem.belajarsesi6.trialbalance;

import id.autochem.belajarsesi6.trialbalance.dto.TrialBalanceReport;
import id.autochem.belajarsesi6.trialbalance.dto.WorksheetReport;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
 * Time: 19.46
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/trial-balance")
@Tag(name = "Trial Balance", description = "Neraca saldo dari agregasi General Ledger (hanya jurnal POSTED/REVERSED)")
public class TrialBalanceController {

    private final TrialBalanceService service;

    @GetMapping
    @Operation(summary = "Neraca saldo s.d. tanggal asOf (default hari ini); total debit harus = total kredit")
    public TrialBalanceReport trialBalance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf,
            @RequestParam(defaultValue = "false") boolean includeZero) {
        return service.trialBalance(asOf, includeZero);
    }

    @GetMapping("/periods/{code}/worksheet")
    @Operation(summary = "Kertas kerja periode: neraca saldo sebelum penyesuaian, penyesuaian, sesudah penyesuaian")
    public WorksheetReport worksheet(@PathVariable String code) {
        return service.worksheet(code);
    }

    @GetMapping(value = "/export", produces = "text/csv")
    @Operation(summary = "Unduh neraca saldo dalam format CSV")
    public ResponseEntity<String> exportCsv(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf) {
        TrialBalanceReport report = service.trialBalance(asOf, false);
        StringBuilder csv = new StringBuilder("code,name,account_type,normal_balance,debit,credit\n");
        for (TrialBalanceReport.Line l : report.lines()) {
            csv.append(l.code()).append(',')
                    .append('"').append(l.name().replace("\"", "\"\"")).append('"').append(',')
                    .append(l.accountType()).append(',')
                    .append(l.normalBalance()).append(',')
                    .append(l.debitBalance().toPlainString()).append(',')
                    .append(l.creditBalance().toPlainString()).append('\n');
        }
        csv.append(",TOTAL,,,").append(report.totals().totalDebitBalance().toPlainString()).append(',')
                .append(report.totals().totalCreditBalance().toPlainString()).append('\n');
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"trial-balance-" + report.asOf() + ".csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv.toString());
    }
}
