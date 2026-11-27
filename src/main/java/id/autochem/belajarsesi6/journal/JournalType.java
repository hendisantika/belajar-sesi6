package id.autochem.belajarsesi6.journal;

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
public enum JournalType {
    /** Transaksi harian. */
    GENERAL("JV"),
    /** Jurnal penyesuaian akhir periode (akrual, penyusutan, dsb.). */
    ADJUSTMENT("AJ"),
    /** Jurnal pembalik untuk koreksi; selalu menunjuk jurnal asal. */
    REVERSAL("RV");

    private final String prefix;

    JournalType(String prefix) {
        this.prefix = prefix;
    }

    public String prefix() {
        return prefix;
    }

    /** Jurnal koreksi masih boleh diposting saat periode sudah cut-off (SOFT_CLOSED). */
    public boolean isCorrection() {
        return this == ADJUSTMENT || this == REVERSAL;
    }
}
