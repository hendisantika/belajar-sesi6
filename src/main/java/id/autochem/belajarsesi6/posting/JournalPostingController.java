package id.autochem.belajarsesi6.posting;

import id.autochem.belajarsesi6.common.web.Headers;
import id.autochem.belajarsesi6.idempotency.IdempotencyService;
import id.autochem.belajarsesi6.idempotency.IdempotentResult;
import id.autochem.belajarsesi6.posting.dto.PostingResult;
import id.autochem.belajarsesi6.security.Roles;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
@Tag(name = "Posting", description = "Posting jurnal APPROVED ke General Ledger (idempotent, cek period lock)")
public class JournalPostingController {

    private final IdempotencyService idempotency;
    private final PostingService postingService;

    @PostMapping("/{id}/post")
    @PreAuthorize(Roles.CAN_APPROVE_JOURNAL)
    @Operation(summary = "APPROVED -> POSTED; ditolak bila periode LOCKED / cut-off (tercatat di audit log)")
    public ResponseEntity<PostingResult> post(@PathVariable Long id,
                                              @RequestHeader(Headers.IDEMPOTENCY_KEY) String key,
                                              Authentication authentication) {
        String actor = authentication.getName();
        IdempotentResult<PostingResult> result = idempotency.execute(key, "POST_JOURNAL", id + "|" + actor,
                PostingResult.class, () -> postingService.post(id, actor));
        return ResponseEntity.ok()
                .header(Headers.IDEMPOTENT_REPLAYED, String.valueOf(result.replayed()))
                .body(result.body());
    }
}
