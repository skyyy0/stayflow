# StayFlow

**StayFlow** adalah aplikasi Android (Kotlin + Jetpack Compose) oleh developer **Sky** yang dirancang agar sesi web tetap berjalan lancar meski pengguna mematikan layar atau berpindah sementara ke aplikasi lain, dilengkapi fitur **Paksa Salin Teks**, **Asisten AI**, dan build **R8 + ProGuard + AES-256-GCM Key Vault**.

## Fitur Utama
- **Bebas Matiin Layar & Keluar Aplikasi**: Layanan latar (`foregroundServiceType="dataSync"` + `PARTIAL_WAKE_LOCK`) dan `KeepAliveWebView` menjaga proses halaman web serta timer berjalan akurat secara real-time.
- **Pelindung Fokus & Visibilitas Web**: Skrip *document-start* (`SessionGuardScript`) menahan sinyal `visibilitychange`, `blur`, `focusout`, dan `pagehide` tanpa memodifikasi timer JS asli.
- **Paksa Salin & Seleksi Teks**: Membuka kunci seleksi teks dan tekan lama pada situs yang membatasi *copy-paste*.
- **Asisten AI Terintegrasi**: Tersedia di tab khusus **Tanya AI** maupun sebagai *bottom sheet* di atas browser web aktif (otomatis mengambil teks yang sedang diseleksi).
- **Keamanan & Optimasi R8 + ProGuard**: Build rilis menggunakan `isMinifyEnabled = true`, `isShrinkResources = true`, serta `SecureKeyVault` (enkripsi build-time AES-256-GCM + split XOR shard).

## Build APK Otomatis (GitHub Actions)
Workflow berada di `.github/workflows/build-apk.yml` dan otomatis berjalan pada setiap `push` ke `main`/`master` atau secara manual melalui **Actions → workflow_dispatch**.

### Secrets Opsional di GitHub Repository
Buka **Settings → Secrets and variables → Actions** pada repository ini untuk menambahkan:
- `XINGCHI_API_KEY` — API key endpoint AI OpenAI-kompatibel (dienkripsi AES-256-GCM saat build ke `BuildConfig`).
- `ANDROID_KEYSTORE_BASE64`, `STORE_PASSWORD`, `KEY_PASSWORD` — (opsional) keystore rilis kustom; jika tidak diisi, CI akan membuat keystore sementara untuk menandatangani release APK.
