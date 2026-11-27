package id.autochem.belajarsesi6.journal;

import id.autochem.belajarsesi6.account.Account;
import id.autochem.belajarsesi6.common.exception.BusinessException;
import id.autochem.belajarsesi6.common.exception.ErrorCode;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.envers.Audited;

import java.math.BigDecimal;
import java.math.RoundingMode;

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
@Table(name = "journal_line")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class JournalLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "journal_id", nullable = false)
    private JournalEntry journal;

    @Column(name = "line_no", nullable = false)
    private int lineNo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(length = 255)
    private String description;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal debit;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal credit;

    public JournalLine(Account account, String description, BigDecimal debit, BigDecimal credit) {
        BigDecimal d = normalize(debit);
        BigDecimal c = normalize(credit);
        if (d.signum() < 0 || c.signum() < 0) {
            throw new BusinessException(ErrorCode.INVALID_LINE_AMOUNT, "Debit/kredit tidak boleh negatif");
        }
        if ((d.signum() > 0) == (c.signum() > 0)) {
            throw new BusinessException(ErrorCode.INVALID_LINE_AMOUNT,
                    "Baris akun " + account.getCode() + " harus diisi debit ATAU kredit (salah satu > 0)");
        }
        this.account = account;
        this.description = description;
        this.debit = d;
        this.credit = c;
    }

    /** Baris kebalikan untuk jurnal reversal: debit dan kredit ditukar. */
    public JournalLine reversed() {
        return new JournalLine(account, "Reversal: " + (description == null ? "" : description), credit, debit);
    }

    void attach(JournalEntry journal, int lineNo) {
        this.journal = journal;
        this.lineNo = lineNo;
    }

    private static BigDecimal normalize(BigDecimal value) {
        try {
            return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException e) {
            throw new BusinessException(ErrorCode.INVALID_LINE_AMOUNT, "Nominal maksimal 2 angka desimal: " + value);
        }
    }
}
