package id.autochem.belajarsesi6.reversal;

import id.autochem.belajarsesi6.common.web.Headers;
import id.autochem.belajarsesi6.idempotency.IdempotencyService;
import id.autochem.belajarsesi6.idempotency.IdempotentResult;
import id.autochem.belajarsesi6.reversal.dto.ReversalResult;
import id.autochem.belajarsesi6.reversal.dto.ReverseRequest;
import id.autochem.belajarsesi6.security.Roles;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
@Tag(name = "Reversal", description = "Koreksi jurnal POSTED lewat jurnal pembalik")
public class JournalReversalController {

    private final IdempotencyService idempotency;
    private final ReversalService reversalService;

    @PostMapping("/{id}/reverse")
    @PreAuthorize(Roles.CAN_APPROVE_JOURNAL)
    @Operation(summary = "POSTED -> REVERSED + jurnal REVERSAL (debit/kredit ditukar) yang langsung POSTED")
    public ResponseEntity<ReversalResult> reverse(@PathVariable Long id,
                                                  @Valid @RequestBody ReverseRequest request,
                                                  @RequestHeader(Headers.IDEMPOTENCY_KEY) String key,
                                                  Authentication authentication) {
        String actor = authentication.getName();
        String fingerprint = id + "|" + actor + "|" + request.reason() + "|" + request.reversalDate();
        IdempotentResult<ReversalResult> result = idempotency.execute(key, "REVERSE_JOURNAL", fingerprint,
                ReversalResult.class, () -> reversalService.reverse(id, request, actor));
        return ResponseEntity.status(result.replayed() ? HttpStatus.OK : HttpStatus.CREATED)
                .header(Headers.IDEMPOTENT_REPLAYED, String.valueOf(result.replayed()))
                .body(result.body());
    }
}
