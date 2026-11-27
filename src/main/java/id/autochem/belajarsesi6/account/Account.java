package id.autochem.belajarsesi6.account;

import id.autochem.belajarsesi6.common.persistence.AuditableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.envers.Audited;

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
@Getter
@Entity
@Audited
@Table(name = "account")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Account extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 20)
    private AccountType accountType;

    @Enumerated(EnumType.STRING)
    @Column(name = "normal_balance", nullable = false, length = 6)
    private NormalBalance normalBalance;

    @Column(nullable = false)
    private boolean active;

    public static Account create(String code, String name, AccountType type, NormalBalance normalBalance) {
        Account account = new Account();
        account.code = code;
        account.name = name;
        account.accountType = type;
        account.normalBalance = normalBalance != null ? normalBalance : type.defaultNormalBalance();
        account.active = true;
        return account;
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    public Map<String, Object> snapshot() {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("code", code);
        s.put("name", name);
        s.put("accountType", accountType);
        s.put("normalBalance", normalBalance);
        s.put("active", active);
        return s;
    }
}
