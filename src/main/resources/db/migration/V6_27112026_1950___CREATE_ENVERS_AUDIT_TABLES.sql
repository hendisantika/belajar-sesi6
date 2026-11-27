-- =====================================================================
-- Hibernate Envers: revinfo + tabel *_aud (setiap versi entity @Audited)
-- revtype: 0 = ADD, 1 = MOD, 2 = DEL
-- =====================================================================

CREATE TABLE revinfo
(
    id       BIGSERIAL PRIMARY KEY,
    revtstmp BIGINT       NOT NULL,
    username VARCHAR(100)
);

COMMENT ON TABLE revinfo IS 'Satu revisi Envers per transaksi, beserta user yang melakukan perubahan';

CREATE TABLE account_aud
(
    id             BIGINT   NOT NULL,
    rev            BIGINT   NOT NULL,
    revtype        SMALLINT,
    code           VARCHAR(20),
    name           VARCHAR(150),
    account_type   VARCHAR(20),
    normal_balance VARCHAR(6),
    active         BOOLEAN,
    created_by     VARCHAR(100),
    created_at     TIMESTAMPTZ,
    updated_by     VARCHAR(100),
    updated_at     TIMESTAMPTZ,
    CONSTRAINT pk_account_aud PRIMARY KEY (id, rev),
    CONSTRAINT fk_account_aud_rev FOREIGN KEY (rev) REFERENCES revinfo (id)
);

CREATE TABLE accounting_period_aud
(
    id            BIGINT   NOT NULL,
    rev           BIGINT   NOT NULL,
    revtype       SMALLINT,
    code          VARCHAR(7),
    start_date    DATE,
    end_date      DATE,
    status        VARCHAR(12),
    cutoff_by     VARCHAR(100),
    cutoff_at     TIMESTAMPTZ,
    locked_by     VARCHAR(100),
    locked_at     TIMESTAMPTZ,
    unlocked_by   VARCHAR(100),
    unlocked_at   TIMESTAMPTZ,
    unlock_reason VARCHAR(500),
    created_by    VARCHAR(100),
    created_at    TIMESTAMPTZ,
    updated_by    VARCHAR(100),
    updated_at    TIMESTAMPTZ,
    version       BIGINT,
    CONSTRAINT pk_accounting_period_aud PRIMARY KEY (id, rev),
    CONSTRAINT fk_accounting_period_aud_rev FOREIGN KEY (rev) REFERENCES revinfo (id)
);

CREATE TABLE journal_entry_aud
(
    id              BIGINT   NOT NULL,
    rev             BIGINT   NOT NULL,
    revtype         SMALLINT,
    journal_number  VARCHAR(40),
    journal_date    DATE,
    description     VARCHAR(255),
    journal_type    VARCHAR(12),
    status          VARCHAR(10),
    auto_reverse    BOOLEAN,
    reversal_of_id  BIGINT,
    reversal_reason VARCHAR(255),
    approved_by     VARCHAR(100),
    approved_at     TIMESTAMPTZ,
    posted_by       VARCHAR(100),
    posted_at       TIMESTAMPTZ,
    reversed_by     VARCHAR(100),
    reversed_at     TIMESTAMPTZ,
    created_by      VARCHAR(100),
    created_at      TIMESTAMPTZ,
    updated_by      VARCHAR(100),
    updated_at      TIMESTAMPTZ,
    version         BIGINT,
    CONSTRAINT pk_journal_entry_aud PRIMARY KEY (id, rev),
    CONSTRAINT fk_journal_entry_aud_rev FOREIGN KEY (rev) REFERENCES revinfo (id)
);

CREATE TABLE journal_line_aud
(
    id          BIGINT   NOT NULL,
    rev         BIGINT   NOT NULL,
    revtype     SMALLINT,
    journal_id  BIGINT,
    line_no     INTEGER,
    account_id  BIGINT,
    description VARCHAR(255),
    debit       NUMERIC(19, 2),
    credit      NUMERIC(19, 2),
    CONSTRAINT pk_journal_line_aud PRIMARY KEY (id, rev),
    CONSTRAINT fk_journal_line_aud_rev FOREIGN KEY (rev) REFERENCES revinfo (id)
);

CREATE INDEX idx_journal_entry_aud_rev ON journal_entry_aud (rev);
CREATE INDEX idx_accounting_period_aud_rev ON accounting_period_aud (rev);
