package id.autochem.belajarsesi6.audit;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

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
 * Menandai operasi tulis yang penolakannya (BusinessException) wajib tercatat di audit log
 * dengan outcome REJECTED. Ekspresi SpEL dievaluasi terhadap parameter method, mis. {@code "#journalId"}.
 */
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface AuditedOperation {

    AuditAction action();

    String entityType();

    /** SpEL untuk entity id (opsional). */
    String entityId() default "";

    /** SpEL untuk referensi bisnis, mis. nomor jurnal / kode periode (opsional). */
    String entityRef() default "";
}
