package id.autochem.belajarsesi6.account.dto;

import id.autochem.belajarsesi6.account.AccountType;
import id.autochem.belajarsesi6.account.NormalBalance;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

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
public record CreateAccountRequest(
        @NotBlank @Pattern(regexp = "\\d{4,20}", message = "kode akun berupa 4-20 digit") String code,
        @NotBlank @Size(max = 150) String name,
        @NotNull AccountType accountType,
        /* Opsional; default mengikuti tipe akun. Isi CREDIT untuk akun kontra aset (akumulasi penyusutan). */
        NormalBalance normalBalance) {
}
