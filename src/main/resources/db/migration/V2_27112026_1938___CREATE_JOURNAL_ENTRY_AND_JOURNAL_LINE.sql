-- =====================================================================
-- Jurnal umum: header (journal_entry) & detail (journal_line)
-- Lifecycle : DRAFT -> APPROVED -> POSTED -> REVERSED (APPROVED -> DRAFT boleh)
-- Tipe      : GENERAL    = transaksi harian
--             ADJUSTMENT = jurnal penyesuaian akhir periode (akrual, penyusutan, ...)
--             REVERSAL   = jurnal pembalik (koreksi), wajib menunjuk jurnal asal
-- =====================================================================

CREATE SEQUENCE journal_number_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE journal_entry
(
    id              BIGSERIAL PRIMARY KEY,
    journal_number  VARCHAR(40)  NOT NULL,
    journal_date    DATE         NOT NULL,
    description     VARCHAR(255) NOT NULL,
    journal_type    VARCHAR(12)  NOT NULL DEFAULT 'GENERAL',
    status          VARCHAR(10)  NOT NULL DEFAULT 'DRAFT',
    auto_reverse    BOOLEAN      NOT NULL DEFAULT FALSE,
    reversal_of_id  BIGINT,
    reversal_reason VARCHAR(255),
    approved_by     VARCHAR(100),
    approved_at     TIMESTAMPTZ,
    posted_by       VARCHAR(100),
    posted_at       TIMESTAMPTZ,
    reversed_by     VARCHAR(100),
    reversed_at     TIMESTAMPTZ,
    created_by      VARCHAR(100) NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_by      VARCHAR(100),
    updated_at      TIMESTAMPTZ,
    version         BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uk_journal_entry_number UNIQUE (journal_number),
    CONSTRAINT ck_journal_entry_status CHECK (status IN ('DRAFT', 'APPROVED', 'POSTED', 'REVERSED')),
    CONSTRAINT ck_journal_entry_type CHECK (journal_type IN ('GENERAL', 'ADJUSTMENT', 'REVERSAL')),
    CONSTRAINT fk_journal_entry_reversal_of FOREIGN KEY (reversal_of_id) REFERENCES journal_entry (id),
    -- Satu jurnal hanya boleh dibalik satu kali
    CONSTRAINT uk_journal_entry_reversal_of UNIQUE (reversal_of_id),
    -- Jurnal pembalik wajib punya jurnal asal (mencegah "reversal tanpa jurnal asal")
    CONSTRAINT ck_journal_entry_reversal_ref CHECK ((journal_type = 'REVERSAL') = (reversal_of_id IS NOT NULL)),
    -- Auto-reverse hanya untuk jurnal penyesuaian (mis. akrual)
    CONSTRAINT ck_journal_entry_auto_reverse CHECK (NOT auto_reverse OR journal_type = 'ADJUSTMENT'),
    CONSTRAINT ck_journal_entry_posted_audit CHECK (
        status NOT IN ('POSTED', 'REVERSED') OR (posted_by IS NOT NULL AND posted_at IS NOT NULL)),
    CONSTRAINT ck_journal_entry_reversed_audit CHECK (
        status <> 'REVERSED' OR (reversed_by IS NOT NULL AND reversed_at IS NOT NULL))
);

CREATE INDEX idx_journal_entry_status_date ON journal_entry (status, journal_date);
CREATE INDEX idx_journal_entry_date ON journal_entry (journal_date);
CREATE INDEX idx_journal_entry_type ON journal_entry (journal_type);

CREATE TABLE journal_line
(
    id          BIGSERIAL PRIMARY KEY,
    journal_id  BIGINT         NOT NULL,
    line_no     INTEGER        NOT NULL,
    account_id  BIGINT         NOT NULL,
    description VARCHAR(255),
    debit       NUMERIC(19, 2) NOT NULL DEFAULT 0,
    credit      NUMERIC(19, 2) NOT NULL DEFAULT 0,
    CONSTRAINT fk_journal_line_journal FOREIGN KEY (journal_id) REFERENCES journal_entry (id) ON DELETE CASCADE,
    CONSTRAINT fk_journal_line_account FOREIGN KEY (account_id) REFERENCES account (id),
    CONSTRAINT uk_journal_line_no UNIQUE (journal_id, line_no),
    CONSTRAINT ck_journal_line_amount CHECK (
        debit >= 0 AND credit >= 0 AND ((debit > 0 AND credit = 0) OR (credit > 0 AND debit = 0)))
);

CREATE INDEX idx_journal_line_journal ON journal_line (journal_id);
CREATE INDEX idx_journal_line_account ON journal_line (account_id);
