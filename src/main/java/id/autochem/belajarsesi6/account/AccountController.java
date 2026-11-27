package id.autochem.belajarsesi6.account;

import id.autochem.belajarsesi6.account.dto.AccountResponse;
import id.autochem.belajarsesi6.account.dto.CreateAccountRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
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
@RequestMapping("/api/accounts")
@Tag(name = "Chart of Accounts")
public class AccountController {

    private final AccountService service;

    @GetMapping
    @Operation(summary = "Daftar akun (opsional filter aktif/nonaktif)")
    public List<AccountResponse> list(@RequestParam(required = false) Boolean active) {
        return service.list(active);
    }

    @GetMapping("/{code}")
    public AccountResponse get(@PathVariable String code) {
        return service.get(code);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Tambah akun (ADMIN)")
    public AccountResponse create(@Valid @RequestBody CreateAccountRequest request) {
        return service.create(request);
    }

    @PatchMapping("/{code}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public AccountResponse activate(@PathVariable String code) {
        return service.setActive(code, true);
    }

    @PatchMapping("/{code}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Nonaktifkan akun; jurnal yang memakai akun ini ditolak saat posting")
    public AccountResponse deactivate(@PathVariable String code) {
        return service.setActive(code, false);
    }
}
