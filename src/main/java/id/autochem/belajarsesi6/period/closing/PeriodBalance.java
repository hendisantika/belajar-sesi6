package id.autochem.belajarsesi6.period.closing;

import id.autochem.belajarsesi6.account.Account;
import id.autochem.belajarsesi6.period.AccountingPeriod;
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
import org.hibernate.annotations.Immutable;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Created by IntelliJ IDEA.
 * Project : belajar-sesi6
 * User: hendisantika
 * Email: hendisantika@gmail.com
 * Telegram : @hendisantika34
 * Link : s.id/hendisantika
 * Date: 27/11/26
 * Time: 19.48
 */
/** Snapshot saldo akun saat periode di-LOCK (debit - kredit, bertanda). */
@Getter
@Entity
@Immutable
@Table(name = "period_balance")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PeriodBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "period_id", nullable = false)
    private AccountingPeriod period;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @Column(name = "opening_balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal openingBalance;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal debit;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal credit;

    @Column(name = "closing_balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal closingBalance;

    @Column(name = "snapshot_by", nullable = false, length = 100)
    private String snapshotBy;

    @Column(name = "snapshot_at", nullable = false)
    private Instant snapshotAt;
}
