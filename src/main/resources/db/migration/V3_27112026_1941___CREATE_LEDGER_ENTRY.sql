-- =====================================================================
-- Buku besar (General Ledger): satu ledger_entry per journal_line yang diposting.
-- Sumber agregasi trial balance -> index komposit (account_id, posting_date).
-- =====================================================================

CREATE TABLE ledger_entry
(
    id              BIGSERIAL PRIMARY KEY,
    journal_id      BIGINT         NOT NULL,
    journal_line_id BIGINT         NOT NULL,
    account_id      BIGINT         NOT NULL,
    posting_date    DATE           NOT NULL,
    description     VARCHAR(255),
    debit           NUMERIC(19, 2) NOT NULL DEFAULT 0,
    credit          NUMERIC(19, 2) NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT fk_ledger_entry_journal FOREIGN KEY (journal_id) REFERENCES journal_entry (id),
    CONSTRAINT fk_ledger_entry_line FOREIGN KEY (journal_line_id) REFERENCES journal_line (id),
    CONSTRAINT fk_ledger_entry_account FOREIGN KEY (account_id) REFERENCES account (id),
    CONSTRAINT uk_ledger_entry_journal_line UNIQUE (journal_line_id),
    CONSTRAINT ck_ledger_entry_amount CHECK (debit >= 0 AND credit >= 0)
);

-- Agregasi trial balance & GL per akun s.d. tanggal tertentu
CREATE INDEX idx_ledger_entry_account_date ON ledger_entry (account_id, posting_date, id);
CREATE INDEX idx_ledger_entry_posting_date ON ledger_entry (posting_date);
CREATE INDEX idx_ledger_entry_journal ON ledger_entry (journal_id);
