package id.autochem.belajarsesi6.common.web;

import id.autochem.belajarsesi6.common.exception.BusinessException;
import id.autochem.belajarsesi6.common.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

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
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final String TYPE_PREFIX = "https://belajar-sesi6.autochem.id/errors/";

    @ExceptionHandler(BusinessException.class)
    ProblemDetail handleBusiness(BusinessException ex) {
        log.info("Business rule rejected [{}]: {}", ex.getCode(), ex.getMessage());
        ProblemDetail problem = problem(ex.getCode(), ex.getMessage());
        ex.getProperties().forEach(problem::setProperty);
        return problem;
    }

    @ExceptionHandler({OptimisticLockingFailureException.class, PessimisticLockingFailureException.class})
    ProblemDetail handleLocking(RuntimeException ex) {
        log.warn("Concurrent modification: {}", ex.getMessage());
        return problem(ErrorCode.CONCURRENT_MODIFICATION, "Data sedang diproses oleh request lain, silakan ulangi");
    }

    /** Pelanggaran constraint / trigger DB (lapisan pertahanan terakhir, mis. posting ke periode LOCKED). */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail handleIntegrity(DataIntegrityViolationException ex) {
        log.warn("Data integrity violation: {}", ex.getMostSpecificCause().getMessage());
        return problem(ErrorCode.DATA_INTEGRITY, ex.getMostSpecificCause().getMessage());
    }

    @Override
    protected @Nullable ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                          HttpHeaders headers,
                                                                          HttpStatusCode status,
                                                                          WebRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(fe.getField(), fe.getDefaultMessage());
        }
        ProblemDetail problem = ex.getBody();
        problem.setTitle("Validasi request gagal");
        problem.setProperty("code", "VALIDATION_FAILED");
        problem.setProperty("errors", errors);
        problem.setProperty("timestamp", Instant.now());
        return handleExceptionInternal(ex, problem, headers, status, request);
    }

    private ProblemDetail problem(ErrorCode code, String detail) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(code.status(), detail);
        problem.setTitle(code.title());
        problem.setType(URI.create(TYPE_PREFIX + code.name().toLowerCase().replace('_', '-')));
        problem.setProperty("code", code.name());
        problem.setProperty("timestamp", Instant.now());
        return problem;
    }
}
