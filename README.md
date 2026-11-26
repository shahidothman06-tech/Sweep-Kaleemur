# Sweep: Hidden Camera Detector (v2.0.0)

Full rewrite of **Hidden Camera & Bug Detector** in Kotlin + Jetpack Compose.
Package name is unchanged (`com.hidden.devices.detector.bugs.finder`), so this ships as an update to the app already on Google Play.

## What's inside

| Area | Where |
|---|---|
| Splash screen (Android 12 style, works back to Android 8) | `res/values/themes.xml` (`Theme.App.Starting`), `MainActivity.kt` |
| Home, full scan, results, device detail | `ui/screens/HomeScreen`, `ScanScreen`, `ResultsScreen`, `DeviceScreen` |
| Wi-Fi camera scan (ports, UPnP, ONVIF) | `wifiscan/` (unit-tested) |
| Lens finder (CameraX, red filter, flash, Night IR) | `ui/screens/LensScreen.kt` |
| Bluetooth tracker scan (AirTag, Tile, SmartTag) | `bluetooth/BleScanner.kt`, `ui/screens/BluetoothScreen.kt` |
| Magnet meter with beeps | `magnet/`, `ui/screens/MagnetScreen.kt` |
| Room checklist (bedroom, bathroom, living room) | `ui/screens/ChecklistScreen.kt` |
| Reports history | `ui/screens/ReportsScreen.kt` |
| Settings, language picker, help, privacy policy | `ui/screens/SettingsScreen`, `LanguageScreen`, `HelpScreen`, `PrivacyScreen` |
| Rate-us prompt (after 2nd scan; unhappy users get a feedback email instead) | `ui/screens/RateDialog.kt` |
| Sweep Pro subscription (weekly + monthly) | `billing/BillingManager.kt`, `ui/screens/ProScreen.kt` |
| Navigation and bottom bar | `ui/SweepApp.kt` |

Languages: English, Arabic, Urdu, Hindi, Spanish, Portuguese, French, Indonesian (`res/values-xx/strings.xml`).
The privacy policy body stays in English on purpose (`res/values/privacy.xml`); its heading is translated.

## Open and run

1. Open the `sweep-android` folder in Android Studio (Ladybug 2024.2 or newer). Let Gradle sync.
2. Run on a real phone. The emulator has no Wi-Fi devices, Bluetooth trackers or magnetometer to find.

> Note: this project was written in a cloud container that can't download the Android SDK, so it has **not been compiled yet**. The scan engine (`wifiscan/`) is unit-tested and passes. Expect to fix a few small compile errors on the first build in Android Studio, if any.

## Before you upload to Play

1. **versionCode**: in `app/build.gradle.kts`, set `versionCode` higher than the version live on Play now (currently 100 as a placeholder).
2. **Signing**: sign the release with the **same upload key** you used for the current app, otherwise Play rejects the update.
3. **Support email**: replace `support@example.com` in `res/values/strings.xml` (`support_email`).
4. **App name**: `app_name` is "Sweep" and `app_full_name` is "Sweep: Hidden Camera Detector". If you keep the old name, change those two strings.
5. **Subscription** (Play Console > Monetize > Products > Subscriptions):
   - Product ID: `sweep_pro`
   - Base plan `weekly`, auto-renewing, 1 week
   - Base plan `monthly`, auto-renewing, 1 month
   - Optional: add a free-trial offer to either base plan; the app picks it up automatically.
   - Add your Gmail as a license tester (Settings > License testing) to test purchases for free.
6. **Privacy policy URL**: host the text from `res/values/privacy.xml` on a public page (Google Sites works) and put the link in the Play listing.
7. **Data safety form**: answer "No data collected" and "No data shared". The app has no ads, analytics or servers. Purchases go through Google Play. This also fixes the current listing, which contradicts itself.
8. **Permissions declaration**: Play may ask why the app uses location. Answer: required by Android for Bluetooth scanning of trackers; GPS is never read.

## What Pro unlocks

All scans are free. Pro adds:
- Full report history (free keeps the last 3)
- Sharing scan results and reports
- Night IR lens mode

## Tests

```
./gradlew :app:testDebugUnitTest
```
