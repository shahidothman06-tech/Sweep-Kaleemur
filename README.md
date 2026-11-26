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
| Ads (AdMob Next-Gen SDK, UMP consent, banner + capped interstitial) | `ads/AdsConfig.kt`, `ads/AdsManager.kt`, `ads/AdBanner.kt` |
| Navigation and bottom bar | `ui/SweepApp.kt` |

Languages: English, Arabic, Urdu, Hindi, Spanish, Portuguese, French, Indonesian (`res/values-xx/strings.xml`).
The privacy policy body stays in English on purpose (`res/values/privacy.xml`); its heading is translated.

## Open and run

1. Open the `Sweep-Kaleemur` folder in Android Studio (2026.1 or newer). Let Gradle sync.
2. Run on a real phone. The emulator has no Wi-Fi devices, Bluetooth trackers or magnetometer to find.

## Before you upload to Play

1. **versionCode**: in `app/build.gradle.kts`, set `versionCode` higher than the version live on Play now (currently 100 as a placeholder).
2. **Signing**: sign the release with the **same upload key** you used for the current app, otherwise Play rejects the update.
3. **Support email**: replace `support@example.com` in `res/values/strings.xml` (`support_email`).
4. **App name**: `app_name` and `app_full_name` are both "Sweep: Hidden Camera Detector". If you keep the old name, change those two strings.
5. **Subscription** (Play Console > Monetize > Products > Subscriptions):
   - Product ID: `sweep_pro`
   - Base plan `weekly`, auto-renewing, 1 week
   - Base plan `monthly`, auto-renewing, 1 month
   - Optional: add a free-trial offer to either base plan; the app picks it up automatically.
   - Add your Gmail as a license tester (Settings > License testing) to test purchases for free.
6. **Privacy policy URL**: host the text from `res/values/privacy.xml` on a public page (Google Sites works) and put the link in the Play listing.
7. **AdMob IDs**: the app uses Google's TEST ad IDs. Before release, put your real AdMob app ID and ad unit IDs in `ads/AdsConfig.kt` and the real app ID in `AndroidManifest.xml` (`com.google.android.gms.ads.APPLICATION_ID`). In AdMob, set up the consent message (Privacy & messaging > GDPR and US states) so the consent form shows.
8. **Ads declaration**: in Play Console > App content, answer **Yes, my app contains ads**. Also fill the **Advertising ID** declaration: Yes, used for Advertising or marketing (and Analytics/Fraud prevention, which the ads SDK does).
9. **Data safety form** (the app now shows Google AdMob ads):
   | Data type | Collected | Shared | Purpose | Optional? |
   |---|---|---|---|---|
   | Device or other IDs (Advertising ID) | Yes | Yes, with Google | Advertising or marketing, Analytics, Fraud prevention, security and compliance | No (needed for ads in the free version) |
   | App interactions (ads seen and tapped) | Yes | Yes, with Google | Advertising or marketing, Analytics | No |
   | Diagnostics (crash logs of the ads SDK) | Yes | Yes, with Google | Analytics | No |
   | Approximate location (from IP address, by the ads SDK) | Yes | Yes, with Google | Advertising or marketing | No |
   - Data is encrypted in transit: **Yes**. Users can request deletion: answer as Google AdMob's own Data safety guidance says (https://support.google.com/admob/answer/11128013).
   - Not collected: name, email, contacts, photos, precise location, scan results. Scans stay on the phone. Purchases go through Google Play.
   - Pro subscribers get no ads and no ad requests.
10. **Permissions declaration**: Play may ask why the app uses location. Answer: required by Android for Bluetooth scanning of trackers; GPS is never read.

## What Pro unlocks

All scans are free. Pro adds:
- No ads
- Full report history (free keeps the last 3)
- Sharing scan results and reports
- Night IR lens mode

## Tests

```
./gradlew :app:testDebugUnitTest
```
