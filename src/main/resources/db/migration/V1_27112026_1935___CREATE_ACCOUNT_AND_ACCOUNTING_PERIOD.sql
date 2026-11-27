-- =====================================================================
-- Master data: Chart of Accounts (COA) & periode akuntansi
-- Status periode (period cut-off & lock):
--   OPEN        : semua jurnal boleh diposting
--   SOFT_CLOSED : cut-off; hanya jurnal ADJUSTMENT / REVERSAL (koreksi akhir periode)
--   LOCKED      : terkunci; tidak ada jurnal baru / perubahan di periode ini
-- =====================================================================

CREATE TABLE account
(
    id             BIGSERIAL PRIMARY KEY,
    code           VARCHAR(20)  NOT NULL,
    name           VARCHAR(150) NOT NULL,
    account_type   VARCHAR(20)  NOT NULL,
    normal_balance VARCHAR(6)   NOT NULL,
    active         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_by     VARCHAR(100) NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_by     VARCHAR(100),
    updated_at     TIMESTAMPTZ,
    CONSTRAINT uk_account_code UNIQUE (code),
    CONSTRAINT ck_account_type CHECK (account_type IN ('ASSET', 'LIABILITY', 'EQUITY', 'REVENUE', 'EXPENSE')),
    CONSTRAINT ck_account_normal_balance CHECK (normal_balance IN ('DEBIT', 'CREDIT'))
);

COMMENT ON TABLE account IS 'Chart of Accounts';
COMMENT ON COLUMN account.normal_balance IS 'Sisi saldo normal; akun kontra (mis. akumulasi penyusutan) berbeda dari tipe akunnya';

CREATE TABLE accounting_period
(
    id               BIGSERIAL PRIMARY KEY,
    code             VARCHAR(7)   NOT NULL,
    start_date       DATE         NOT NULL,
    end_date         DATE         NOT NULL,
    status           VARCHAR(12)  NOT NULL DEFAULT 'OPEN',
    cutoff_by        VARCHAR(100),
    cutoff_at        TIMESTAMPTZ,
    locked_by        VARCHAR(100),
    locked_at        TIMESTAMPTZ,
    unlocked_by      VARCHAR(100),
    unlocked_at      TIMESTAMPTZ,
    unlock_reason    VARCHAR(500),
    created_by       VARCHAR(100) NOT NULL,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_by       VARCHAR(100),
    updated_at       TIMESTAMPTZ,
    version          BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uk_accounting_period_code UNIQUE (code),
    CONSTRAINT ck_accounting_period_status CHECK (status IN ('OPEN', 'SOFT_CLOSED', 'LOCKED')),
    CONSTRAINT ck_accounting_period_range CHECK (end_date >= start_date),
    CONSTRAINT ck_accounting_period_locked_audit CHECK (
        status <> 'LOCKED' OR (locked_by IS NOT NULL AND locked_at IS NOT NULL)),
    CONSTRAINT ck_accounting_period_unlock_reason CHECK (
        unlocked_by IS NULL OR (unlock_reason IS NOT NULL AND length(trim(unlock_reason)) > 0)),
    -- Periode tidak boleh tumpang tindih
    CONSTRAINT ex_accounting_period_overlap EXCLUDE USING gist (daterange(start_date, end_date, '[]') WITH &&)
);

CREATE INDEX idx_accounting_period_dates ON accounting_period (start_date, end_date);

COMMENT ON TABLE accounting_period IS 'Periode akuntansi bulanan dengan cut-off & lock';
COMMENT ON COLUMN accounting_period.unlock_reason IS 'Alasan wajib saat periode LOCKED dibuka kembali';
