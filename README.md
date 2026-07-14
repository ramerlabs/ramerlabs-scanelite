# ScanElite

![ScanElite](brand/logo.png)

Premium mobile document scanner — capture, auto-align, enhance, and share.

**A product by [RamerLabs](https://ramerlabs.com)** · [ramerlabs.com](https://ramerlabs.com)

---

## App (Android)

Kotlin · Jetpack Compose · CameraX · Room · Hilt

### Run
1. Open this folder in Android Studio  
2. Gradle sync  
3. Run `app` on emulator/device (grant camera permission)

```bash
./gradlew :app:assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

### Flows
- **Home** → New Scan → **Camera** (Single / Batch, live edge overlay, auto-capture)
- **Enhance** → Magic Color / B&W / Original + rotate
- **Review** → reorder, rotate, delete pages
- **Share Hub** → PDF / JPEG → WhatsApp, Telegram, Facebook, Email

### Spec & config
- [`docs/SCANELITE_UI_UX_SPEC.md`](docs/SCANELITE_UI_UX_SPEC.md)
- [`config/scanelite.config.json`](config/scanelite.config.json)
- Brand: `brand/icon.png`, `brand/logo.png`

---

## Credits

© RamerLabs · [https://ramerlabs.com](https://ramerlabs.com)
