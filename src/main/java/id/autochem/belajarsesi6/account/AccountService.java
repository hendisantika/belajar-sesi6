package id.autochem.belajarsesi6.account;

import id.autochem.belajarsesi6.account.dto.AccountResponse;
import id.autochem.belajarsesi6.account.dto.CreateAccountRequest;
import id.autochem.belajarsesi6.audit.AuditAction;
import id.autochem.belajarsesi6.audit.AuditTrail;
import id.autochem.belajarsesi6.audit.AuditedOperation;
import id.autochem.belajarsesi6.audit.EntityTypes;
import id.autochem.belajarsesi6.common.exception.BusinessException;
import id.autochem.belajarsesi6.common.exception.ErrorCode;
import id.autochem.belajarsesi6.common.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
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
@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accounts;
    private final AuditTrail audit;

    @Transactional(readOnly = true)
    public List<AccountResponse> list(Boolean active) {
        List<Account> result = active == null
                ? accounts.findAllByOrderByCodeAsc()
                : accounts.findByActiveOrderByCodeAsc(active);
        return result.stream().map(AccountResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public AccountResponse get(String code) {
        return AccountResponse.from(require(code));
    }

    @Transactional
    @AuditedOperation(action = AuditAction.ACCOUNT_CREATED, entityType = EntityTypes.ACCOUNT, entityRef = "#request.code")
    public AccountResponse create(CreateAccountRequest request) {
        if (accounts.existsByCode(request.code())) {
            throw new BusinessException(ErrorCode.DUPLICATE_ACCOUNT, "Kode akun " + request.code() + " sudah dipakai");
        }
        Account account = accounts.saveAndFlush(Account.create(request.code(), request.name(),
                request.accountType(), request.normalBalance()));
        audit.success(AuditAction.ACCOUNT_CREATED, EntityTypes.ACCOUNT, account.getId(), account.getCode(),
                null, account.snapshot());
        return AccountResponse.from(account);
    }

    @Transactional
    @AuditedOperation(action = AuditAction.ACCOUNT_DEACTIVATED, entityType = EntityTypes.ACCOUNT, entityRef = "#code")
    public AccountResponse setActive(String code, boolean active) {
        Account account = require(code);
        Map<String, Object> before = account.snapshot();
        if (active) {
            account.activate();
        } else {
            account.deactivate();
        }
        audit.success(active ? AuditAction.ACCOUNT_ACTIVATED : AuditAction.ACCOUNT_DEACTIVATED,
                EntityTypes.ACCOUNT, account.getId(), code, before, account.snapshot());
        return AccountResponse.from(accounts.saveAndFlush(account));
    }

    private Account require(String code) {
        return accounts.findByCode(code).orElseThrow(() -> new NotFoundException("Akun", code));
    }
}
