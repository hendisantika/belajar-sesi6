package id.autochem.belajarsesi6.common.exception;

import org.springframework.http.HttpStatus;

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
public enum ErrorCode {
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "Data tidak ditemukan"),
    INVALID_STATUS_TRANSITION(HttpStatus.CONFLICT, "Transisi status jurnal tidak diizinkan"),
    JOURNAL_NOT_EDITABLE(HttpStatus.CONFLICT, "Jurnal tidak dapat diubah"),
    JOURNAL_UNBALANCED(HttpStatus.UNPROCESSABLE_CONTENT, "Total debit tidak sama dengan total kredit"),
    JOURNAL_EMPTY(HttpStatus.UNPROCESSABLE_CONTENT, "Jurnal minimal memiliki dua baris dengan nilai"),
    INVALID_LINE_AMOUNT(HttpStatus.UNPROCESSABLE_CONTENT, "Nilai baris jurnal tidak valid"),
    INVALID_JOURNAL_TYPE(HttpStatus.UNPROCESSABLE_CONTENT, "Tipe jurnal tidak valid"),
    ACCOUNT_NOT_FOUND(HttpStatus.UNPROCESSABLE_CONTENT, "Akun tidak ditemukan"),
    ACCOUNT_INACTIVE(HttpStatus.UNPROCESSABLE_CONTENT, "Akun nonaktif"),
    DUPLICATE_ACCOUNT(HttpStatus.CONFLICT, "Kode akun sudah dipakai"),
    PERIOD_NOT_FOUND(HttpStatus.UNPROCESSABLE_CONTENT, "Periode akuntansi tidak ditemukan"),
    PERIOD_LOCKED(HttpStatus.UNPROCESSABLE_CONTENT, "Periode akuntansi sudah dikunci"),
    PERIOD_CUT_OFF(HttpStatus.UNPROCESSABLE_CONTENT, "Periode akuntansi sudah cut-off"),
    INVALID_PERIOD_TRANSITION(HttpStatus.CONFLICT, "Transisi status periode tidak diizinkan"),
    PERIOD_CHECKLIST_FAILED(HttpStatus.UNPROCESSABLE_CONTENT, "Closing checklist belum terpenuhi"),
    PERIOD_SEQUENCE_VIOLATION(HttpStatus.UNPROCESSABLE_CONTENT, "Urutan tutup/buka periode tidak valid"),
    FUTURE_DATED(HttpStatus.UNPROCESSABLE_CONTENT, "Tanggal posting di masa depan"),
    SEGREGATION_OF_DUTIES(HttpStatus.UNPROCESSABLE_CONTENT, "Pembuat jurnal tidak boleh menyetujui jurnalnya sendiri"),
    INVALID_REVERSAL_DATE(HttpStatus.UNPROCESSABLE_CONTENT, "Tanggal reversal tidak valid"),
    INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST, "Rentang tanggal tidak valid"),
    IDEMPOTENCY_KEY_INVALID(HttpStatus.BAD_REQUEST, "Idempotency-Key tidak valid"),
    IDEMPOTENCY_KEY_REUSED(HttpStatus.UNPROCESSABLE_CONTENT, "Idempotency-Key sudah dipakai untuk request berbeda"),
    CONCURRENT_MODIFICATION(HttpStatus.CONFLICT, "Data sedang/telah diubah oleh proses lain"),
    DATA_INTEGRITY(HttpStatus.CONFLICT, "Pelanggaran integritas data");

    private final HttpStatus status;
    private final String title;

    ErrorCode(HttpStatus status, String title) {
        this.status = status;
        this.title = title;
    }

    public HttpStatus status() {
        return status;
    }

    public String title() {
        return title;
    }
}
