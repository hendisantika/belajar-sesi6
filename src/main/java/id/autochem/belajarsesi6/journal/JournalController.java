package id.autochem.belajarsesi6.journal;

import id.autochem.belajarsesi6.journal.dto.JournalRequest;
import id.autochem.belajarsesi6.journal.dto.JournalResponse;
import id.autochem.belajarsesi6.journal.dto.JournalSummaryResponse;
import id.autochem.belajarsesi6.journal.dto.ReasonRequest;
import id.autochem.belajarsesi6.security.Roles;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
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
@RequestMapping("/api/journals")
@Tag(name = "Journals", description = "Lifecycle jurnal: DRAFT -> APPROVED -> POSTED -> REVERSED")
public class JournalController {

    private final JournalService service;

    @GetMapping
    @Operation(summary = "Daftar jurnal dengan filter status, tipe & rentang tanggal")
    public Page<JournalSummaryResponse> list(
            @RequestParam(required = false) JournalStatus status,
            @RequestParam(required = false) JournalType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @PageableDefault(size = 20, sort = {"journalDate", "id"}, direction = Sort.Direction.ASC) Pageable pageable) {
        return service.list(status, type, from, to, pageable);
    }

    @GetMapping("/{id}")
    public JournalResponse get(@PathVariable Long id) {
        return service.get(id);
    }

    @GetMapping("/number/{journalNumber}")
    public JournalResponse getByNumber(@PathVariable String journalNumber) {
        return service.getByNumber(journalNumber);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize(Roles.CAN_WRITE_JOURNAL)
    @Operation(summary = "Buat jurnal DRAFT (GENERAL / ADJUSTMENT); ditolak bila periode sudah ditutup")
    public JournalResponse create(@Valid @RequestBody JournalRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize(Roles.CAN_WRITE_JOURNAL)
    @Operation(summary = "Ubah jurnal (hanya DRAFT, periode harus terbuka)")
    public JournalResponse update(@PathVariable Long id, @Valid @RequestBody JournalRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize(Roles.CAN_WRITE_JOURNAL)
    @Operation(summary = "Hapus jurnal (hanya DRAFT)")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize(Roles.CAN_APPROVE_JOURNAL)
    @Operation(summary = "DRAFT -> APPROVED (approver harus berbeda dari pembuat)")
    public JournalResponse approve(@PathVariable Long id, Authentication authentication) {
        return service.approve(id, authentication.getName());
    }

    @PostMapping("/{id}/return-to-draft")
    @PreAuthorize(Roles.CAN_APPROVE_JOURNAL)
    @Operation(summary = "APPROVED -> DRAFT (dikembalikan untuk revisi)")
    public JournalResponse returnToDraft(@PathVariable Long id, @Valid @RequestBody ReasonRequest request) {
        return service.returnToDraft(id, request.reason());
    }
}
