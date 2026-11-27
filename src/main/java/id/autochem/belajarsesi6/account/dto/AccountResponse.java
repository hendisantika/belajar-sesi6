package id.autochem.belajarsesi6.account.dto;

import id.autochem.belajarsesi6.account.Account;
import id.autochem.belajarsesi6.account.AccountType;
import id.autochem.belajarsesi6.account.NormalBalance;

import java.time.Instant;

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
public record AccountResponse(Long id, String code, String name, AccountType accountType,
                              NormalBalance normalBalance, boolean active,
                              String createdBy, Instant createdAt, String updatedBy, Instant updatedAt) {

    public static AccountResponse from(Account a) {
        return new AccountResponse(a.getId(), a.getCode(), a.getName(), a.getAccountType(), a.getNormalBalance(),
                a.isActive(), a.getCreatedBy(), a.getCreatedAt(), a.getUpdatedBy(), a.getUpdatedAt());
    }
}
