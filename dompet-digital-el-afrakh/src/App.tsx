import { CheckCircle2, FolderTree, Lock, Smartphone, Terminal } from 'lucide-react';

const modules = [
  {
    title: 'Konfigurasi Gradle & Manifest',
    items: [
      'DompetDigitalElAfrakh/settings.gradle.kts',
      'DompetDigitalElAfrakh/build.gradle.kts',
      'DompetDigitalElAfrakh/gradle/libs.versions.toml',
      'DompetDigitalElAfrakh/app/build.gradle.kts',
      'DompetDigitalElAfrakh/app/src/main/AndroidManifest.xml (Tanpa izin INTERNET)',
    ],
  },
  {
    title: 'Room Database, Keamanan PIN & Penyimpanan Nota',
    items: [
      'data/local/TransactionEntity.kt & TransactionDao.kt & AppDatabase.kt',
      'data/security/PinCryptoManager.kt (PBKDF2WithHmacSHA256 + Salt)',
      'data/prefs/AppPreferences.kt (PIN, Saldo Awal, Light/Dark Mode)',
      'data/storage/ReceiptImageStore.kt (Kompresi JPEG & Penyimpanan Internal)',
      'data/repository/FinanceRepository.kt',
    ],
  },
  {
    title: 'Ekspor Offline & Backup/Restore',
    items: [
      'export/OfflinePdfExporter.kt (Android native PdfDocument)',
      'export/OfflineXlsxExporter.kt (Pure-Kotlin OOXML .xlsx)',
      'backup/ZipBackupManager.kt (Backup & Restore .zip + foto nota)',
      'util/CurrencyFormatter.kt & DateTimeHelper.kt (Asia/Jakarta WIB)',
    ],
  },
  {
    title: 'Jetpack Compose UI (Material 3)',
    items: [
      'SplashScreen.kt, PinScreen.kt, InitialBalanceScreen.kt',
      'DashboardScreen.kt (Saldo, Ringkasan Hari Ini, Grafik 7 Hari)',
      'AddIncomeScreen.kt & AddExpenseScreen.kt (Manual tanpa kategori)',
      'HistoryScreen.kt, ReportScreen.kt, SettingsScreen.kt',
    ],
  },
];

export default function App() {
  return (
    <div className="min-h-screen bg-slate-50 text-slate-900">
      <header className="bg-blue-900 text-white py-8 px-6 shadow-sm">
        <div className="max-w-5xl mx-auto flex flex-col gap-2">
          <div className="inline-flex items-center gap-2 text-blue-200 text-sm font-medium">
            <Smartphone className="w-4 h-4" />
            <span>Proyek Android Native (Kotlin + Jetpack Compose + Room)</span>
          </div>
          <h1 className="text-2xl md:text-3xl font-bold tracking-tight">
            Dompet Digital El Afrakh
          </h1>
          <p className="text-blue-100 text-sm md:text-base max-w-2xl">
            Proyek Android Studio lengkap 100% offline telah dibuat di direktori{' '}
            <code className="bg-blue-800 px-2 py-0.5 rounded text-white">DompetDigitalElAfrakh/</code>.
          </p>
        </div>
      </header>

      <main className="max-w-5xl mx-auto px-6 py-8 space-y-6">
        <div className="bg-amber-50 border border-amber-200 rounded-xl p-4 text-sm text-amber-900 flex items-start gap-3">
          <Lock className="w-5 h-5 text-amber-700 shrink-0 mt-0.5" />
          <div>
            <p className="font-semibold">Catatan Build APK</p>
            <p className="mt-1">
              Lingkungan sandbox ini tidak memiliki JDK/Android SDK, sehingga file{' '}
              <code>.apk</code> perlu di-build di Android Studio melalui{' '}
              <strong>Build → Build Bundle(s) / APK(s) → Build APK(s)</strong> atau{' '}
              <code>gradle assembleDebug</code> di folder <code>DompetDigitalElAfrakh</code>.
            </p>
          </div>
        </div>

        <section className="grid md:grid-cols-2 gap-4">
          {modules.map((mod) => (
            <div
              key={mod.title}
              className="bg-white border border-slate-200 rounded-xl p-5 shadow-xs"
            >
              <div className="flex items-center gap-2 text-blue-900 font-semibold mb-3">
                <FolderTree className="w-4 h-4" />
                <h2>{mod.title}</h2>
              </div>
              <ul className="space-y-2 text-sm text-slate-700">
                {mod.items.map((item) => (
                  <li key={item} className="flex items-start gap-2">
                    <CheckCircle2 className="w-4 h-4 text-emerald-600 shrink-0 mt-0.5" />
                    <span className="font-mono text-xs md:text-sm break-all">{item}</span>
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </section>

        <section className="bg-white border border-slate-200 rounded-xl p-5 shadow-xs">
          <div className="flex items-center gap-2 text-blue-900 font-semibold mb-3">
            <Terminal className="w-4 h-4" />
            <h2>Cara Menjalankan & Build APK</h2>
          </div>
          <ol className="list-decimal list-inside space-y-2 text-sm text-slate-700">
            <li>
              Buka folder <code>DompetDigitalElAfrakh</code> di Android Studio (Ladybug atau lebih baru) dengan JDK 17.
            </li>
            <li>Tunggu Gradle Sync selesai, lalu jalankan ke HP/Emulator (Android 8.0 / API 26+).</li>
            <li>
              Untuk menghasilkan APK debug: pilih{' '}
              <strong>Build → Build Bundle(s) / APK(s) → Build APK(s)</strong>.
            </li>
          </ol>
        </section>
      </main>
    </div>
  );
}
