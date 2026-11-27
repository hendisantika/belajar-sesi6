package id.autochem.belajarsesi6.period;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

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
public interface AccountingPeriodRepository extends JpaRepository<AccountingPeriod, Long> {

    @Query("select p from AccountingPeriod p where :date between p.startDate and p.endDate")
    Optional<AccountingPeriod> findContaining(@Param("date") LocalDate date);

    Optional<AccountingPeriod> findByCode(String code);

    /** Lock baris periode saat cut-off/lock/unlock agar tidak balapan dengan proses lain. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from AccountingPeriod p where p.code = :code")
    Optional<AccountingPeriod> findByCodeForUpdate(@Param("code") String code);

    List<AccountingPeriod> findAllByOrderByStartDateAsc();

    List<AccountingPeriod> findByEndDateBeforeAndStatusNotOrderByStartDateAsc(LocalDate date, PeriodStatus status);

    Optional<AccountingPeriod> findFirstByStartDateAfterOrderByStartDateAsc(LocalDate date);
}
