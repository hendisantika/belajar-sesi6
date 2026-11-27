-- =====================================================================
-- Snapshot saldo per akun per periode, dibuat saat periode di-LOCK.
-- Dipakai untuk: (1) neraca saldo cepat tanpa agregasi ulang data besar,
--                (2) rekonsiliasi: deteksi drift antara snapshot dan GL live.
-- =====================================================================

CREATE TABLE period_balance
(
    id              BIGSERIAL PRIMARY KEY,
    period_id       BIGINT         NOT NULL,
    account_id      BIGINT         NOT NULL,
    opening_balance NUMERIC(19, 2) NOT NULL,
    debit           NUMERIC(19, 2) NOT NULL,
    credit          NUMERIC(19, 2) NOT NULL,
    closing_balance NUMERIC(19, 2) NOT NULL,
    snapshot_by     VARCHAR(100)   NOT NULL,
    snapshot_at     TIMESTAMPTZ    NOT NULL,
    CONSTRAINT fk_period_balance_period FOREIGN KEY (period_id) REFERENCES accounting_period (id),
    CONSTRAINT fk_period_balance_account FOREIGN KEY (account_id) REFERENCES account (id),
    CONSTRAINT uk_period_balance UNIQUE (period_id, account_id),
    CONSTRAINT ck_period_balance_closing CHECK (closing_balance = opening_balance + debit - credit)
);

COMMENT ON TABLE period_balance IS 'Saldo akun per periode LOCKED (debit - kredit, bertanda)';
