-- =====================================================================
-- Seed skenario hands-on Sesi 6 ("hari ini" = 27 Nov 2026)
--   Oktober 2026 (akan LOCKED + snapshot saldo):
--     1 JV  01-10 Setoran modal                       POSTED
--     2 JV  05-10 Pembelian peralatan kantor          POSTED
--     3 JV  20-10 Penjualan kredit                    POSTED
--     4 AJ  31-10 Penyesuaian penyusutan Oktober      POSTED (ADJUSTMENT)
--     5 JV  30-10 Pembelian ATK (jurnal susulan)      APPROVED -> posting DITOLAK (periode LOCKED)
--   November 2026 (OPEN):
--     6 JV  02-11 Penarikan tunai                     POSTED
--     7 JV  05-11 Penjualan tunai                     POSTED
--     8 JV  10-11 Sewa kantor November                POSTED
--     9 JV  12-11 Pelunasan piutang                   POSTED
--    10 JV  15-11 Pendapatan jasa SALAH NOMINAL       POSTED (12.500.000, seharusnya 1.250.000 -> reversal)
--    11 JV  20-11 Gaji November                       APPROVED (belum masuk GL / trial balance)
--    12 JV  25-11 Listrik & air November              DRAFT    (tidak boleh ikut trial balance)
-- =====================================================================

INSERT INTO journal_entry (id, journal_number, journal_date, description, journal_type, status,
                           created_by, created_at, approved_by, approved_at, posted_by, posted_at)
VALUES (1, 'JV-202610-00001', DATE '2026-10-01', 'Setoran modal awal pemilik', 'GENERAL', 'POSTED',
        'accountant1', TIMESTAMPTZ '2026-10-01 09:00+07', 'approver1', TIMESTAMPTZ '2026-10-01 10:00+07',
        'approver1', TIMESTAMPTZ '2026-10-01 10:05+07'),
       (2, 'JV-202610-00002', DATE '2026-10-05', 'Pembelian peralatan kantor (laptop & printer)', 'GENERAL', 'POSTED',
        'accountant1', TIMESTAMPTZ '2026-10-05 09:00+07', 'approver1', TIMESTAMPTZ '2026-10-05 11:00+07',
        'approver1', TIMESTAMPTZ '2026-10-05 11:05+07'),
       (3, 'JV-202610-00003', DATE '2026-10-20', 'Penjualan kredit ke PT Maju Jaya', 'GENERAL', 'POSTED',
        'accountant1', TIMESTAMPTZ '2026-10-20 14:00+07', 'approver1', TIMESTAMPTZ '2026-10-20 15:00+07',
        'approver1', TIMESTAMPTZ '2026-10-20 15:05+07'),
       (4, 'AJ-202610-00004', DATE '2026-10-31', 'Penyesuaian: penyusutan peralatan Oktober (24jt / 48 bln)', 'ADJUSTMENT', 'POSTED',
        'accountant1', TIMESTAMPTZ '2026-11-02 08:00+07', 'approver1', TIMESTAMPTZ '2026-11-02 08:30+07',
        'approver1', TIMESTAMPTZ '2026-11-02 08:35+07'),
       (6, 'JV-202611-00006', DATE '2026-11-02', 'Penarikan tunai dari bank untuk kas operasional', 'GENERAL', 'POSTED',
        'accountant1', TIMESTAMPTZ '2026-11-02 09:00+07', 'approver1', TIMESTAMPTZ '2026-11-02 10:00+07',
        'approver1', TIMESTAMPTZ '2026-11-02 10:05+07'),
       (7, 'JV-202611-00007', DATE '2026-11-05', 'Penjualan tunai toko', 'GENERAL', 'POSTED',
        'accountant1', TIMESTAMPTZ '2026-11-05 15:00+07', 'approver1', TIMESTAMPTZ '2026-11-05 16:00+07',
        'approver1', TIMESTAMPTZ '2026-11-05 16:05+07'),
       (8, 'JV-202611-00008', DATE '2026-11-10', 'Pembayaran sewa kantor November', 'GENERAL', 'POSTED',
        'accountant1', TIMESTAMPTZ '2026-11-10 09:00+07', 'approver1', TIMESTAMPTZ '2026-11-10 11:00+07',
        'approver1', TIMESTAMPTZ '2026-11-10 11:05+07'),
       (9, 'JV-202611-00009', DATE '2026-11-12', 'Pelunasan piutang PT Maju Jaya', 'GENERAL', 'POSTED',
        'accountant2', TIMESTAMPTZ '2026-11-12 09:00+07', 'approver1', TIMESTAMPTZ '2026-11-12 10:00+07',
        'approver1', TIMESTAMPTZ '2026-11-12 10:05+07'),
       (10, 'JV-202611-00010', DATE '2026-11-15', 'Pendapatan jasa konsultasi CV Sinar', 'GENERAL', 'POSTED',
        'accountant2', TIMESTAMPTZ '2026-11-15 09:00+07', 'approver1', TIMESTAMPTZ '2026-11-15 10:00+07',
        'approver1', TIMESTAMPTZ '2026-11-15 10:05+07');

INSERT INTO journal_entry (id, journal_number, journal_date, description, journal_type, status,
                           created_by, created_at, approved_by, approved_at)
VALUES (5, 'JV-202610-00005', DATE '2026-10-30', 'Pembelian ATK Oktober (jurnal susulan)', 'GENERAL', 'APPROVED',
        'accountant1', TIMESTAMPTZ '2026-10-30 16:00+07', 'approver1', TIMESTAMPTZ '2026-10-31 17:00+07'),
       (11, 'JV-202611-00011', DATE '2026-11-20', 'Pembayaran gaji karyawan November', 'GENERAL', 'APPROVED',
        'accountant1', TIMESTAMPTZ '2026-11-20 09:00+07', 'approver1', TIMESTAMPTZ '2026-11-20 13:00+07');

INSERT INTO journal_entry (id, journal_number, journal_date, description, journal_type, status, created_by, created_at)
VALUES (12, 'JV-202611-00012', DATE '2026-11-25', 'Pembayaran listrik & air November', 'GENERAL', 'DRAFT',
        'accountant1', TIMESTAMPTZ '2026-11-25 09:00+07');

SELECT setval('journal_entry_id_seq', (SELECT max(id) FROM journal_entry));
SELECT setval('journal_number_seq', 12);

INSERT INTO journal_line (journal_id, line_no, account_id, description, debit, credit)
SELECT v.journal_id, v.line_no, a.id, v.description, v.debit, v.credit
FROM (VALUES (1, 1, '1102', 'Setoran modal ke rekening BCA', 100000000.00, 0.00),
             (1, 2, '3101', 'Modal disetor pemilik', 0.00, 100000000.00),
             (2, 1, '1501', 'Laptop & printer kantor', 24000000.00, 0.00),
             (2, 2, '1102', 'Transfer ke vendor', 0.00, 24000000.00),
             (3, 1, '1201', 'Piutang PT Maju Jaya', 8000000.00, 0.00),
             (3, 2, '4101', 'Penjualan kredit', 0.00, 8000000.00),
             (4, 1, '5501', 'Beban penyusutan Oktober', 500000.00, 0.00),
             (4, 2, '1502', 'Akumulasi penyusutan', 0.00, 500000.00),
             (5, 1, '5401', 'ATK Oktober', 350000.00, 0.00),
             (5, 2, '1101', 'Bayar tunai', 0.00, 350000.00),
             (6, 1, '1101', 'Kas operasional', 10000000.00, 0.00),
             (6, 2, '1102', 'Penarikan tunai', 0.00, 10000000.00),
             (7, 1, '1101', 'Penerimaan penjualan tunai', 15000000.00, 0.00),
             (7, 2, '4101', 'Penjualan tunai', 0.00, 15000000.00),
             (8, 1, '5201', 'Sewa kantor November', 5000000.00, 0.00),
             (8, 2, '1101', 'Bayar sewa tunai', 0.00, 5000000.00),
             (9, 1, '1102', 'Transfer masuk PT Maju Jaya', 8000000.00, 0.00),
             (9, 2, '1201', 'Pelunasan piutang', 0.00, 8000000.00),
             (10, 1, '1102', 'Transfer masuk CV Sinar', 12500000.00, 0.00),
             (10, 2, '4201', 'Jasa konsultasi', 0.00, 12500000.00),
             (11, 1, '5101', 'Gaji November', 8000000.00, 0.00),
             (11, 2, '1102', 'Transfer gaji', 0.00, 8000000.00),
             (12, 1, '5301', 'Tagihan PLN & PDAM', 750000.00, 0.00),
             (12, 2, '1101', 'Bayar tunai', 0.00, 750000.00))
         AS v(journal_id, line_no, account_code, description, debit, credit)
         JOIN account a ON a.code = v.account_code;

-- Buku besar untuk jurnal yang sudah POSTED
INSERT INTO ledger_entry (journal_id, journal_line_id, account_id, posting_date, description, debit, credit, created_at)
SELECT l.journal_id, l.id, l.account_id, j.journal_date, coalesce(l.description, j.description),
       l.debit, l.credit, j.posted_at
FROM journal_line l
         JOIN journal_entry j ON j.id = l.journal_id
WHERE j.status = 'POSTED'
ORDER BY j.journal_date, l.journal_id, l.line_no;

-- ---------------------------------------------------------------- lock Jan-Okt 2026
UPDATE accounting_period
SET status     = 'LOCKED',
    cutoff_by  = 'admin',
    cutoff_at  = (end_date + INTERVAL '18 hours') AT TIME ZONE 'Asia/Jakarta',
    locked_by  = 'admin',
    locked_at  = (end_date + INTERVAL '5 days 17 hours') AT TIME ZONE 'Asia/Jakarta',
    updated_by = 'admin',
    updated_at = (end_date + INTERVAL '5 days 17 hours') AT TIME ZONE 'Asia/Jakarta'
WHERE end_date <= DATE '2026-10-31';

-- Snapshot saldo per akun untuk setiap periode yang LOCKED (hanya akun yang punya saldo/mutasi)
INSERT INTO period_balance (period_id, account_id, opening_balance, debit, credit, closing_balance,
                            snapshot_by, snapshot_at)
SELECT p.id,
       a.id,
       coalesce(sum(l.debit - l.credit) FILTER (WHERE l.posting_date < p.start_date), 0),
       coalesce(sum(l.debit) FILTER (WHERE l.posting_date >= p.start_date), 0),
       coalesce(sum(l.credit) FILTER (WHERE l.posting_date >= p.start_date), 0),
       coalesce(sum(l.debit - l.credit), 0),
       'admin',
       p.locked_at
FROM accounting_period p
         JOIN ledger_entry l ON l.posting_date <= p.end_date
         JOIN journal_entry j ON j.id = l.journal_id AND j.status IN ('POSTED', 'REVERSED')
         JOIN account a ON a.id = l.account_id
WHERE p.status = 'LOCKED'
GROUP BY p.id, a.id, p.start_date, p.locked_at;

-- ---------------------------------------------------------------- audit trail seed
INSERT INTO audit_event (event_id, action, outcome, entity_type, entity_id, entity_ref, actor, actor_roles,
                         after_state, occurred_at)
SELECT gen_random_uuid(), 'JOURNAL_POSTED', 'SUCCESS', 'JOURNAL_ENTRY', id, journal_number, posted_by, 'APPROVER',
       jsonb_build_object('status', status, 'journalType', journal_type, 'journalDate', journal_date),
       posted_at
FROM journal_entry
WHERE status = 'POSTED';

INSERT INTO audit_event (event_id, action, outcome, entity_type, entity_id, entity_ref, actor, actor_roles,
                         before_state, after_state, occurred_at)
SELECT gen_random_uuid(), 'PERIOD_LOCKED', 'SUCCESS', 'ACCOUNTING_PERIOD', id, code, 'admin', 'ADMIN',
       jsonb_build_object('status', 'SOFT_CLOSED'),
       jsonb_build_object('status', 'LOCKED', 'lockedBy', locked_by),
       locked_at
FROM accounting_period
WHERE status = 'LOCKED';
