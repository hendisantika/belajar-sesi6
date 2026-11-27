# belajar-sesi6 — Trial Balance, Period Closing & Audit Trail

Implementasi use case **Enterprise Java & Spring Boot Engineering – Modul 02 – Sesi 6**
(lihat `Sesi 06 - Trial Balance, Period Closing & Audit Trail.pptx`). Project ini melanjutkan
deliverable Sesi 5 (journal posting, reversal, General Ledger) dan menambahkan:

| Materi slide | Implementasi |
|---|---|
| Trial balance dari agregasi GL & normal balance | `GET /api/trial-balance` memakai JPQL *constructor expression* (`select new TrialBalanceLine(...)`), `group by` akun, filter `postingDate <= asOf`, dan **hanya jurnal POSTED/REVERSED**. Ada kolom debit/kredit, subtotal per tipe akun, deteksi saldo abnormal, serta export CSV |
| Period cut-off, closing checklist, lock/unlock dengan otorisasi | Status periode `OPEN → SOFT_CLOSED (cut-off) → LOCKED`. Lock hanya oleh **ADMIN** setelah checklist lolos. Unlock **wajib alasan** dan tercatat di audit |
| `assertOpen()` dipanggil posting & reversal | `PeriodGuard.assertOpen(date, type)` dipanggil di **setiap** operasi tulis jurnal (create, update, approve, delete, post, reverse), ditambah trigger DB sebagai lapisan terakhir |
| Adjustment & reversal (guided scenario) | Jurnal `ADJUSTMENT` (prefix `AJ`) tetap boleh masuk saat cut-off. Flag `autoReverse` untuk akrual dibalik otomatis di tanggal 1 periode berikutnya. Worksheet menampilkan neraca saldo sebelum penyesuaian, penyesuaian, dan sesudah penyesuaian |
| Audit trail: actor, time, event, before/after | Tabel `audit_event` append-only (trigger DB) berisi actor, role, action, outcome (`SUCCESS`/`REJECTED`/`DENIED`), before/after `jsonb`, correlation id, dan IP. Ditulis via `@TransactionalEventListener` |
| Spring Data Auditing + Envers | `@CreatedBy/@CreatedDate/@LastModifiedBy/@LastModifiedDate` via `AuditorAware` (user dari `SecurityContext`). `@Audited` Hibernate Envers menyimpan setiap versi ke tabel `*_aud`, dan revisi mencatat username |
| Rekonsiliasi Journal – GL – Trial Balance | `GET /api/reconciliation` membandingkan tiga lapis per akun dan mendiagnosa penyebab selisih umum. `GET /api/reconciliation/accounts/{code}/trace` menelusuri TB → GL → jurnal asal → audit |

### Pengayaan use case (di luar slide)

- **Cut-off bertingkat (soft close)**: setelah cut-off, transaksi harian (`GENERAL`) ditolak, tetapi koreksi
  (`ADJUSTMENT`/`REVERSAL`) masih diterima sampai periode di-lock.
- **Closing checklist** (`GET /api/periods/{code}/closing-checklist`): periode sudah berakhir, cut-off sudah
  dilakukan, periode sebelumnya sudah LOCKED, tidak ada jurnal DRAFT/APPROVED, TB seimbang, mutasi GL sama
  dengan baris jurnal, dan pengingat auto-reversal.
- **Urutan periode dijaga**: lock harus berurutan; unlock ditolak bila periode berikutnya masih LOCKED.
- **Snapshot saldo per periode** (`period_balance`) dibuat saat lock. Dipakai untuk neraca saldo cepat
  (`source = SNAPSHOT`) dan untuk mendeteksi *drift* (data periode LOCKED yang diubah diam-diam).
- **Penolakan tetap tercatat** walau transaksi bisnis rollback: `@AuditedOperation` + aspect menulis event
  `REJECTED` di transaksi baru (`AFTER_ROLLBACK` + `REQUIRES_NEW`). Percobaan akses tanpa role tercatat
  `DENIED` lewat `AuthorizationDeniedEvent`.
- **Riwayat revisi Envers** per jurnal/periode/akun, lengkap dengan field yang berubah (`from → to`).
- **Defense in depth di PostgreSQL**: trigger menolak ledger/jurnal di periode LOCKED, jurnal GENERAL di periode
  SOFT_CLOSED, serta UPDATE/DELETE/TRUNCATE `audit_event` dan `ledger_entry`.

## Tech stack

Java 25 · Spring Boot 4.2 (Web MVC, Data JPA, Security, Validation, AspectJ, Actuator) · Hibernate Envers ·
PostgreSQL 17 · Flyway · springdoc OpenAPI · Lombok · JUnit 5 + Testcontainers.

## Menjalankan

```bash
# Spring Boot Docker Compose otomatis menyalakan PostgreSQL dari compose.yaml
./mvnw spring-boot:run
```

- Swagger UI: http://localhost:8091/swagger-ui.html (klik **Authorize**, HTTP Basic)
- Health & Flyway: http://localhost:8091/actuator/health, http://localhost:8091/actuator/flyway
- Skenario hands-on: [`http/sesi6-trial-balance-closing.http`](http/sesi6-trial-balance-closing.http)

Untuk DB eksternal, set `DB_URL`, `DB_USERNAME`, dan `DB_PASSWORD`. Port default `8091` (override dengan `SERVER_PORT`).

```bash
./mvnw test   # unit test + integration test (Testcontainers, butuh Docker)
```

### Akun demo (HTTP Basic, hanya untuk latihan)

| Username | Password | Role | Hak akses |
|---|---|---|---|
| `admin` | `admin123` | ADMIN | Master akun, cut-off, lock/unlock, reopen, auto-reversal, semua aksi jurnal, baca audit |
| `accountant1`, `accountant2` | `accountant123` | ACCOUNTANT | Buat/ubah/hapus jurnal DRAFT (maker) |
| `approver1` | `approver123` | APPROVER | Approve, post, reverse, return-to-draft (checker) |
| `auditor1` | `auditor123` | AUDITOR | Read-only laporan + audit trail & revisi Envers |

Semua endpoint laporan (`GET`) cukup login dengan role apa pun. Audit trail & revisi hanya untuk AUDITOR/ADMIN.

## Data sampel ("hari ini" = 27 Nov 2026)

- Periode Jan–Okt 2026 **LOCKED** (dengan snapshot saldo), Nov–Des 2026 **OPEN**.
- Oktober: setoran modal, pembelian peralatan, penjualan kredit, dan `AJ-202610-00004` penyesuaian penyusutan.
- `JV-202610-00005` APPROVED bertanggal 30 Okt (jurnal susulan): **posting ditolak karena periode LOCKED**.
- November: 5 jurnal POSTED (termasuk `JV-202611-00010` dengan nominal salah sebagai bahan reversal),
  `JV-202611-00011` APPROVED, dan `JV-202611-00012` DRAFT (keduanya tidak ikut trial balance).

Neraca saldo per 31 Okt 2026 (`GET /api/trial-balance?asOf=2026-10-31`):

| Akun | Nama | Debit | Kredit |
|---|---|---:|---:|
| 1102 | Bank BCA | 76.000.000 | |
| 1201 | Piutang Usaha | 8.000.000 | |
| 1501 | Peralatan Kantor | 24.000.000 | |
| 1502 | Akumulasi Penyusutan Peralatan | | 500.000 |
| 3101 | Modal Disetor | | 100.000.000 |
| 4101 | Pendapatan Penjualan | | 8.000.000 |
| 5501 | Beban Penyusutan | 500.000 | |
| | **Total** | **108.500.000** | **108.500.000** |

## Endpoint utama

| Method | Path | Role | Keterangan |
|---|---|---|---|
| GET | `/api/trial-balance?asOf=&includeZero=` | semua | Neraca saldo s.d. tanggal |
| GET | `/api/trial-balance/periods/{code}/worksheet` | semua | Sebelum penyesuaian → penyesuaian → sesudah penyesuaian + laba periode |
| GET | `/api/trial-balance/export?asOf=` | semua | CSV |
| GET | `/api/periods`, `/api/periods/{code}` | semua | Status periode |
| GET | `/api/periods/{code}/closing-checklist` | semua | Checklist sebelum lock |
| POST | `/api/periods/{code}/cut-off` | ADMIN | `OPEN → SOFT_CLOSED` |
| POST | `/api/periods/{code}/reopen` `{reason}` | ADMIN | `SOFT_CLOSED → OPEN` |
| POST | `/api/periods/{code}/lock` | ADMIN | `SOFT_CLOSED → LOCKED` + snapshot |
| POST | `/api/periods/{code}/unlock` `{reason}` | ADMIN | `LOCKED → SOFT_CLOSED` |
| POST | `/api/periods/{code}/auto-reversals` | ADMIN | Balik akrual `autoReverse` di periode berikutnya |
| GET | `/api/periods/{code}/balances`, `/api/periods/{code}/trial-balance` | semua | Snapshot periode LOCKED |
| POST | `/api/journals` | ACCOUNTANT/ADMIN | `journalType`: `GENERAL`/`ADJUSTMENT`, `autoReverse` |
| POST | `/api/journals/{id}/approve` | APPROVER/ADMIN | Maker-checker |
| POST | `/api/journals/{id}/post` + `Idempotency-Key` | APPROVER/ADMIN | Ditolak di periode LOCKED / cut-off |
| POST | `/api/journals/{id}/reverse` + `Idempotency-Key` | APPROVER/ADMIN | Jurnal `REVERSAL` di periode terbuka |
| GET | `/api/general-ledger/{account}`, `/api/general-ledger`, `/api/general-ledger/verification` | semua | GL (dari Sesi 5) |
| GET | `/api/reconciliation?asOf=&includeMatched=` | semua | TB vs GL vs jurnal + temuan |
| GET | `/api/reconciliation/accounts/{code}/trace?asOf=` | semua | Drill-down satu akun |
| GET | `/api/audit-events?action=&outcome=&entityType=&entityRef=&actor=&from=&to=` | AUDITOR/ADMIN | Cari audit event |
| GET | `/api/audit-events/rejections` | AUDITOR/ADMIN | `REJECTED` + `DENIED` |
| GET | `/api/audit-events/{entityType}/{entityId}` | AUDITOR/ADMIN | Jejak satu entity |
| GET | `/api/journals/{id}/revisions`, `/api/periods/{code}/revisions`, `/api/accounts/{code}/revisions` | AUDITOR/ADMIN | Riwayat Envers |

Error memakai `ProblemDetail` (RFC 9457) dengan properti `code`, misalnya `PERIOD_LOCKED`, `PERIOD_CUT_OFF`,
`PERIOD_CHECKLIST_FAILED` (+ `failedItems`), `PERIOD_SEQUENCE_VIOLATION`, dan `INVALID_PERIOD_TRANSITION`.

## Kriteria keberhasilan (deliverable)

| Kriteria | Bukti |
|---|---|
| Trial balance seimbang | `TrialBalanceIntegrationTest.octoberTrialBalanceIsBalanced`, checklist `TRIAL_BALANCE_BALANCED` |
| Posting ke periode locked tertolak | `PeriodClosingIntegrationTest.postingToLockedPeriodIsRejectedAndAudited` (HTTP 422 `PERIOD_LOCKED`) dan `databaseGuardsLockedPeriod` (trigger DB) |
| Penolakan tercatat di audit/event log | Event `JOURNAL_POSTED` dengan outcome `REJECTED` + `error_code`, actor, dan correlation id (`AuditTrailIntegrationTest.rejectionSurvivesRollback`) |
| Draft tidak ikut laporan | `TrialBalanceIntegrationTest.draftAndApprovedJournalsAreExcluded` |
| Rekonsiliasi menemukan selisih | `ReconciliationIntegrationTest.detectsCommonCausesOfMismatch` |

## Struktur package

```
id.autochem.belajarsesi6
├── account          Chart of Accounts (@Audited)
├── period           AccountingPeriod (@Audited), PeriodStatus, PeriodGuard.assertOpen()
│   └── closing      Cut-off, checklist, lock/unlock, snapshot (period_balance), auto-reversal
├── journal          JournalEntry/JournalLine (@Audited), JournalType, lifecycle API
├── posting          PostingService (lock + validasi + ledger), endpoint idempotent
├── reversal         ReversalService (jurnal REVERSAL), endpoint idempotent
├── ledger           LedgerEntry (@Immutable, append-only)
├── generalledger    GL + saldo berjalan, ringkasan, verifikasi (Sesi 5)
├── trialbalance     Trial balance (constructor expression), worksheet, CSV
├── reconciliation   Rekonsiliasi 3 lapis + diagnosa + trace akun
├── audit            AuditTrail, @AuditedOperation + aspect, writer @TransactionalEventListener, query API
│   └── envers       Revision entity (username), riwayat revisi
├── security         Spring Security HTTP Basic, role, CurrentUser
├── idempotency      Idempotency-Key (Sesi 5)
├── common           ErrorCode, exception, ProblemDetail handler, AuditableEntity, correlation id
└── config           Clock, JPA auditing, OpenAPI, scheduling, pagination
```

## Database migration (Flyway)

Format nama file: `Vx_DDMMYYYY_HHMM___DESCRIPTION.sql` di `src/main/resources/db/migration`.
Flyway memisahkan versi dan deskripsi di `__` pertama, sehingga versi menjadi `x.DDMMYYYY.HHMM`.

| Versi | Isi |
|---|---|
| V1 | `account`, `accounting_period` (OPEN/SOFT_CLOSED/LOCKED, unlock reason, exclusion constraint overlap) |
| V2 | `journal_entry` (tipe GENERAL/ADJUSTMENT/REVERSAL, `auto_reverse`, constraint reversal wajib punya jurnal asal), `journal_line` |
| V3 | `ledger_entry` + index `(account_id, posting_date)` |
| V4 | `idempotency_key`, `audit_event` terstruktur (before/after `jsonb`, outcome) |
| V5 | `period_balance` (snapshot saldo saat lock) |
| V6 | Envers: `revinfo` (+username), `account_aud`, `accounting_period_aud`, `journal_entry_aud`, `journal_line_aud` |
| V7 | Trigger append-only, immutability jurnal POSTED, guard periode LOCKED/SOFT_CLOSED |
| V8 | Seed COA & periode 2026 |
| V9 | Seed jurnal Okt–Nov, lock Jan–Okt + snapshot, audit seed |
| V10 | Perbaikan guard: jurnal di periode LOCKED tetap bisa ditandai REVERSED (tanggal tidak berubah) |
