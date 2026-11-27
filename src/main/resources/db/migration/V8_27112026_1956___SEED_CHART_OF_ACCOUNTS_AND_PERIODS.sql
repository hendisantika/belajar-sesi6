-- =====================================================================
-- Seed Chart of Accounts & periode akuntansi 2026 (semua OPEN dulu;
-- periode Jan-Okt dikunci di V9 setelah data sampel Oktober diposting)
-- =====================================================================

INSERT INTO account (code, name, account_type, normal_balance, active, created_by)
VALUES ('1101', 'Kas', 'ASSET', 'DEBIT', TRUE, 'system'),
       ('1102', 'Bank BCA', 'ASSET', 'DEBIT', TRUE, 'system'),
       ('1201', 'Piutang Usaha', 'ASSET', 'DEBIT', TRUE, 'system'),
       ('1301', 'Persediaan Barang', 'ASSET', 'DEBIT', TRUE, 'system'),
       ('1401', 'Sewa Dibayar Dimuka', 'ASSET', 'DEBIT', TRUE, 'system'),
       ('1501', 'Peralatan Kantor', 'ASSET', 'DEBIT', TRUE, 'system'),
       ('1502', 'Akumulasi Penyusutan Peralatan', 'ASSET', 'CREDIT', TRUE, 'system'),
       ('2101', 'Utang Usaha', 'LIABILITY', 'CREDIT', TRUE, 'system'),
       ('2102', 'Beban Akrual (Utang Biaya)', 'LIABILITY', 'CREDIT', TRUE, 'system'),
       ('2201', 'Utang Pajak', 'LIABILITY', 'CREDIT', TRUE, 'system'),
       ('3101', 'Modal Disetor', 'EQUITY', 'CREDIT', TRUE, 'system'),
       ('3201', 'Laba Ditahan', 'EQUITY', 'CREDIT', TRUE, 'system'),
       ('4101', 'Pendapatan Penjualan', 'REVENUE', 'CREDIT', TRUE, 'system'),
       ('4201', 'Pendapatan Jasa', 'REVENUE', 'CREDIT', TRUE, 'system'),
       ('5101', 'Beban Gaji', 'EXPENSE', 'DEBIT', TRUE, 'system'),
       ('5201', 'Beban Sewa', 'EXPENSE', 'DEBIT', TRUE, 'system'),
       ('5301', 'Beban Listrik & Air', 'EXPENSE', 'DEBIT', TRUE, 'system'),
       ('5401', 'Beban ATK', 'EXPENSE', 'DEBIT', TRUE, 'system'),
       ('5501', 'Beban Penyusutan', 'EXPENSE', 'DEBIT', TRUE, 'system');

INSERT INTO accounting_period (code, start_date, end_date, status, created_by)
SELECT to_char(m, 'YYYY-MM'),
       m::date,
       (m + INTERVAL '1 month - 1 day')::date,
       'OPEN',
       'system'
FROM generate_series(DATE '2026-01-01', DATE '2026-12-01', INTERVAL '1 month') AS m;
