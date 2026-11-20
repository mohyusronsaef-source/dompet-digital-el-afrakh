# Dompet Digital El Afrakh — Aplikasi Pencatatan Keuangan Kantin (Android Offline)

Aplikasi Android native berbasis **Kotlin**, **Jetpack Compose**, **Material 3**, dan **Room Database (SQLite)** yang dirancang khusus untuk pencatatan keuangan kantin secara **100% offline**.

## Fitur Utama
- **Splash Screen & Keamanan PIN 6 Digit**: Hashing lokal `PBKDF2WithHmacSHA256` dengan salt acak, kode pemulihan offline, dan kunci otomatis saat keluar/layar terkunci.
- **Saldo Awal & Perhitungan Otomatis**: `SALDO AKHIR = SALDO AWAL + TOTAL PEMASUKAN - TOTAL PENGELUARAN`.
- **Dashboard Modern**: Saldo kantin, pemasukan/pengeluaran/selisih hari ini, grafik 7 hari terakhir (Compose `Canvas`), dan daftar transaksi terbaru.
- **Pemasukan & Pengeluaran Sederhana**: Input nominal dan catatan/keterangan manual (tanpa kategori & tanpa kolom nama toko), timestamp otomatis zona waktu `Asia/Jakarta` (`DD/MM/YYYY • HH:mm`), serta lampiran foto nota opsional (Kamera/Galeri) yang dikompresi ke penyimpanan internal.
- **Riwayat & Detail Transaksi**: Filter Semua/Pemasukan/Pengeluaran, pencarian keterangan, filter tanggal, lihat foto nota layar penuh, edit transaksi, dan hapus dengan konfirmasi.
- **Laporan Keuangan & Ekspor Offline**: Tab Harian, Mingguan, Bulanan, dan Tahunan lengkap dengan ekspor **PDF** (`PdfDocument`) dan **Excel (.xlsx)** (OOXML pure-Kotlin) via Android Storage Access Framework.
- **Pengaturan Lengkap**: Ubah PIN, Ubah Saldo Awal dengan konfirmasi, **Light Mode / Dark Mode**, serta **Backup & Restore (.zip)** yang mencakup database transaksi, saldo awal, dan seluruh foto nota.

---

## Petunjuk Menjalankan Proyek di Android Studio
1. Pastikan **Android Studio (Ladybug / Meerkat atau versi lebih baru)** dan **JDK 17** sudah terpasang.
2. Buka Android Studio, pilih **File → Open**, lalu arahkan ke folder `DompetDigitalElAfrakh`.
3. Tunggu hingga proses **Gradle Sync** selesai mengunduh dependensi Jetpack Compose & Room.
4. Sambungkan HP Android (aktifkan *USB Debugging*) atau jalankan Android Emulator (API 26 / Android 8.0 ke atas).
5. Klik tombol **Run (▶)** untuk memasang dan menjalankan aplikasi.

---

## Petunjuk Build APK (Debug & Release)

### Melalui Android Studio (GUI)
- **APK Debug (Siap Uji Langsung)**:
  Pilih menu **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
  File APK akan dihasilkan di:
  `app/build/outputs/apk/debug/app-debug.apk`
- **APK Release (Tanda Tangan Keystore)**:
  Pilih menu **Build → Generate Signed Bundle / APK... → APK**, buat/pilih file `.jks` keystore Anda, lalu pilih build variant `release`.

### Melalui Terminal / Command Line
Jika `gradle-wrapper.jar` belum dibuat oleh Android Studio, jalankan `gradle wrapper` terlebih dahulu (atau gunakan `gradle assembleDebug` langsung), lalu jalankan di dalam folder `DompetDigitalElAfrakh`:
```bash
# Menjalankan unit test
./gradlew testDebugUnitTest

# Build APK Debug
./gradlew assembleDebug
```
Hasil APK Debug dapat langsung disalin ke HP Android dan dipasang:
`app/build/outputs/apk/debug/app-debug.apk`
