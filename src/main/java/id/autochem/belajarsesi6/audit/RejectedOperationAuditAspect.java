package id.autochem.belajarsesi6.audit;

import id.autochem.belajarsesi6.common.exception.BusinessException;
import id.autochem.belajarsesi6.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

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
/**
 * Dipasang paling luar (sebelum @Transactional) sehingga ketika exception sampai di sini
 * transaksi bisnis sudah rollback; penolakan lalu dicatat di transaksi baru.
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
@Order(0) // setelah ExposeInvocationInterceptor, sebelum TransactionInterceptor (LOWEST_PRECEDENCE)
public class RejectedOperationAuditAspect {

    private static final ExpressionParser PARSER = new SpelExpressionParser();
    private static final ParameterNameDiscoverer NAMES = new DefaultParameterNameDiscoverer();

    private final AuditTrail audit;

    @Around("@annotation(operation)")
    public Object auditRejection(ProceedingJoinPoint pjp, AuditedOperation operation) throws Throwable {
        try {
            return pjp.proceed();
        } catch (BusinessException ex) {
            if (!ex.isAudited()) {
                record(pjp, operation, ex.getMessage(), ex.getCode().name());
                ex.markAudited();
            }
            throw ex;
        } catch (DataIntegrityViolationException ex) {
            // Lapisan terakhir: constraint/trigger DB menolak (mis. posting ke periode LOCKED)
            record(pjp, operation, ex.getMostSpecificCause().getMessage(), ErrorCode.DATA_INTEGRITY.name());
            throw ex;
        }
    }

    private void record(ProceedingJoinPoint pjp, AuditedOperation operation, String reason, String errorCode) {
        Method method = ((MethodSignature) pjp.getSignature()).getMethod();
        MethodBasedEvaluationContext context = new MethodBasedEvaluationContext(null, method, pjp.getArgs(), NAMES);
        Object id = evaluate(operation.entityId(), context);
        Object ref = evaluate(operation.entityRef(), context);
        audit.rejected(operation.action(), operation.entityType(),
                id instanceof Number n ? n.longValue() : null,
                ref != null ? ref.toString() : (id != null ? id.toString() : null),
                reason, errorCode);
    }

    private static Object evaluate(String expression, MethodBasedEvaluationContext context) {
        if (expression == null || expression.isBlank()) {
            return null;
        }
        try {
            return PARSER.parseExpression(expression).getValue(context);
        } catch (RuntimeException e) {
            log.warn("Gagal mengevaluasi SpEL audit '{}': {}", expression, e.getMessage());
            return null;
        }
    }
}
