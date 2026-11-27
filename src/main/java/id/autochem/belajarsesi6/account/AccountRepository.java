package id.autochem.belajarsesi6.account;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
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
public interface AccountRepository extends JpaRepository<Account, Long> {

    Optional<Account> findByCode(String code);

    List<Account> findByCodeIn(Collection<String> codes);

    List<Account> findAllByOrderByCodeAsc();

    List<Account> findByActiveOrderByCodeAsc(boolean active);

    boolean existsByCode(String code);
}
