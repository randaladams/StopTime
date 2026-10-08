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
Every push to `main` builds the APK on GitHub: **Actions** tab → latest run → **Artifacts** → `StopTime-APK`.

## Testing "Remove Ads" on a sideloaded test build
Long-press the version line at the bottom of the Achievements screen to pretend the purchase
was made (test builds only). Real purchases only work once the app is in the Play Console
(internal testing) and installed from Google Play.
