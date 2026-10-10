# StopTime  (com.adamselite.stoptime)

Stop the clock at exactly 1.00 second. Earn achievements.

One app: free with ads (banner + occasional full-screen ad when leaving the Achievements
screen), with a one-time **"Remove Ads"** in-app purchase (Google Play Billing).

## Where things are
| What | File |
|---|---|
| Game screen logic | `app/src/main/java/com/adamselite/stoptime/MainActivity.kt` |
| Achievement list (add new ones here) | `.../achievements/Achievements.kt` |
| Ads | `.../ads/AdsManager.kt` |
| "Remove Ads" purchase | `.../Premium.kt`  (product id: `remove_ads`) |
| Sounds | `.../Sounds.kt` + `app/src/main/res/raw/*.ogg` (made by `tools/make_sounds.py`) |
| Screen layouts | `app/src/main/res/layout/` |
| GitHub build | `.github/workflows/build.yml` |

## Building
- **Test APK:** every push to `main` builds it on GitHub: **Actions** → latest run → **Artifacts** → `StopTime-APK`.
  Test builds always use Google's test ads.
- **Google Play release (.aab):** **Actions** → **Build StopTime RELEASE** → **Run workflow** → `StopTime-Release-AAB`.
  Needs the four signing secrets (KEYSTORE_BASE64, KEYSTORE_PASSWORD, KEY_ALIAS, KEY_PASSWORD) and your real
  AdMob IDs at the top of `app/build.gradle.kts`.
- Raise `versionCode` in `app/build.gradle.kts` before every Google Play upload.

## Store listing
`store/` holds the Play icon, feature graphic and listing text; `docs/` is the public GitHub Pages site with the privacy policy (regenerate the art with `tools/make_store_art.py`).

## Testing "Remove Ads" on a sideloaded test build
Long-press the version line at the bottom of the Achievements screen to pretend the purchase
was made (test builds only). Real purchases only work once the app is in the Play Console
(internal testing) and installed from Google Play.
