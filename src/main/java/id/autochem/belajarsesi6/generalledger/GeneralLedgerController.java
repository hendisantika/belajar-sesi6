package id.autochem.belajarsesi6.generalledger;

import id.autochem.belajarsesi6.generalledger.dto.GeneralLedgerReport;
import id.autochem.belajarsesi6.generalledger.dto.LedgerSummaryReport;
import id.autochem.belajarsesi6.generalledger.dto.LedgerVerificationReport;
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
 * Time: 19.44
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/general-ledger")
@Tag(name = "General Ledger", description = "Buku besar dari jurnal POSTED/REVERSED dengan saldo berjalan")
public class GeneralLedgerController {

    private final GeneralLedgerService service;

    @GetMapping("/{accountCode}")
    @Operation(summary = "General Ledger satu akun + saldo berjalan (default: bulan berjalan)")
    public GeneralLedgerReport ledger(@PathVariable String accountCode,
                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.ledger(accountCode, from, to);
    }

    @GetMapping
    @Operation(summary = "Ringkasan saldo awal, mutasi, dan saldo akhir per akun")
    public LedgerSummaryReport summary(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return service.summary(from, to);
    }

    @GetMapping("/verification")
    @Operation(summary = "Cocokkan saldo GL dengan baris jurnal POSTED dan deteksi double posting")
    public LedgerVerificationReport verification() {
        return service.verify();
    }
}
