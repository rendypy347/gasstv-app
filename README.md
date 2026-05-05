# DF21 LIVE - WebView App untuk df21.biz.id

WebView wrapper untuk situs **DF21 LIVE** (df21.biz.id) dengan handling
khusus untuk iklan popunder yang memicu intent ke aplikasi pihak ketiga
(Shopee, Tokopedia, YouTube, dll.) — tidak akan force close walaupun
aplikasi target tidak terinstall.

## ✨ Fitur

- WebView penuh ke `df21.biz.id`
- **Popunder ad-safe**: iklan yang trigger `intent://`, `shopee://`,
  `tokopedia://`, `youtube://`, `whatsapp://`, dll. tidak akan crash app
  meskipun aplikasi target belum terinstall di HP user.
- **Popup modal**: link `target="_blank"` atau `window.open()` muncul
  sebagai popup di dalam app (bisa ditutup dengan tombol X atau tap di luar)
- **Rotasi layar didukung penuh** (portrait + landscape) tanpa restart WebView
- Deep link `df21.biz.id` → buka langsung di app
- Deep link legacy `gasstv.pw` masih jalan
- Swipe ke bawah untuk refresh
- Progress bar merah saat loading
- Video fullscreen support (HTML5 `<video>`)
- Halaman offline jika tidak ada internet
- Back button: tutup popup → navigate web → exit

## 📱 Cara Build APK

### Metode 1: GitHub Actions (otomatis)
Push ke branch `main`/`master` → workflow `build-apk.yml` akan jalan,
APK bisa download dari tab Actions.

### Metode 2: Android Studio
1. Buka folder `DF21LiveApp` di Android Studio
2. Tunggu Gradle sync selesai
3. **Build → Build Bundle(s) / APK(s) → Build APK(s)**
4. APK ada di: `app/build/outputs/apk/debug/app-debug.apk`

### Metode 3: Command Line
```bash
cd DF21LiveApp
./gradlew assembleDebug
# APK: app/build/outputs/apk/debug/app-debug.apk
```

## 📋 Requirements

- Android 5.0+ (minSdk 21)
- Target Android 14 (targetSdk 34)

## 🔗 Konfigurasi

Edit `MainActivity.kt`:

- `TARGET_URL` - URL utama yang dibuka
- `ALLOWED_HOSTS` - domain yang load di dalam WebView (selain ini akan
  dibuka di browser eksternal)
- `EXTERNAL_SCHEMES` - scheme yang dianggap "buka aplikasi lain"
  (untuk dokumentasi; semua scheme non-http otomatis dihandle aman)
