package id.autochem.belajarsesi6.audit.envers;

import id.autochem.belajarsesi6.security.Roles;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
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
 * Time: 19.52
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
@PreAuthorize(Roles.CAN_READ_AUDIT)
@Tag(name = "Revisions (Envers)", description = "Riwayat versi entity dari tabel *_aud (AUDITOR/ADMIN)")
public class RevisionController {

    private final RevisionQueryService service;

    @GetMapping("/journals/{id}/revisions")
    @Operation(summary = "Setiap versi jurnal: siapa, kapan, field apa yang berubah")
    public List<RevisionResponse> journal(@PathVariable Long id) {
        return service.journal(id);
    }

    @GetMapping("/periods/{code}/revisions")
    @Operation(summary = "Riwayat status periode (cut-off, lock, unlock, reopen)")
    public List<RevisionResponse> period(@PathVariable String code) {
        return service.period(code);
    }

    @GetMapping("/accounts/{code}/revisions")
    public List<RevisionResponse> account(@PathVariable String code) {
        return service.account(code);
    }
}
