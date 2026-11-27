package id.autochem.belajarsesi6.audit;

import id.autochem.belajarsesi6.audit.dto.AuditEventResponse;
import id.autochem.belajarsesi6.security.Roles;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

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
@RequestMapping("/api/audit-events")
@PreAuthorize(Roles.CAN_READ_AUDIT)
@Tag(name = "Audit Trail", description = "Audit event append-only: actor, waktu, aksi, referensi, before/after (AUDITOR/ADMIN)")
public class AuditController {

    private final AuditQueryService service;

    @GetMapping
    @Operation(summary = "Cari audit event (filter aksi, outcome, entity, actor, rentang waktu)")
    public Page<AuditEventResponse> search(
            @RequestParam(required = false) AuditAction action,
            @RequestParam(required = false) Set<AuditOutcome> outcome,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) Long entityId,
            @RequestParam(required = false) String entityRef,
            @RequestParam(required = false) String actor,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @PageableDefault(size = 50, sort = {"occurredAt", "id"}, direction = Sort.Direction.DESC) Pageable pageable) {
        return service.search(new AuditQueryService.Filter(action, outcome, entityType, entityId, entityRef, actor,
                from, to), pageable);
    }

    @GetMapping("/rejections")
    @Operation(summary = "Aksi yang ditolak aturan bisnis (REJECTED) atau otorisasi (DENIED)")
    public Page<AuditEventResponse> rejections(
            @PageableDefault(size = 50, sort = {"occurredAt", "id"}, direction = Sort.Direction.DESC) Pageable pageable) {
        return service.search(new AuditQueryService.Filter(null, EnumSet.of(AuditOutcome.REJECTED,
                AuditOutcome.DENIED), null, null, null, null, null, null), pageable);
    }

    @GetMapping("/{entityType}/{entityId}")
    @Operation(summary = "Jejak lengkap satu entity, mis. JOURNAL_ENTRY/5 atau ACCOUNTING_PERIOD/10")
    public List<AuditEventResponse> trail(@PathVariable String entityType, @PathVariable Long entityId) {
        return service.trail(entityType, entityId);
    }
}
