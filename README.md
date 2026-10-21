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

## Building and releasing

Everything runs in one GitHub workflow: `.github/workflows/android.yml` ("Android").

| You want to... | Do this | You get |
|---|---|---|
| Try a change on your phone | Push / upload to `main` (or open a pull request) | **Actions → latest run → Artifacts →** `StopTime-<version>-test.<n>` (test ads) |
| Release to Google Play | **Actions → Android → Run workflow →** type the new version, e.g. `1.0.1` | A GitHub **Release** `v1.0.1` with `StopTime-1.0.1.aab` (upload to Play Console) and a signed `.apk` |

### Versions (automatic)
- You only choose the version number when releasing: `major.minor.patch`, e.g. `1.0.1` for a fix, `1.1.0` for new features.
- `versionCode` is calculated from it: `major*10000 + minor*100 + patch` (1.0.1 → 10001), so it always goes up, as Google Play requires.
- The workflow refuses a version that was already released or is lower than the last one. Before the first automatic release the last version counts as 1.0.0 (what was on Google Play), so the first release is 1.0.1 or higher.
- Every release is tagged (`v1.0.1`), so you can always see exactly what code went to Google Play.
- Test builds are named like `1.0.1-test.42` (last release + build number).

### Tool and library versions
- Gradle is pinned by the Gradle wrapper (`gradle/wrapper/gradle-wrapper.properties`).
- Plugin and library versions are all in `gradle/libs.versions.toml`.
- Dependabot checks monthly and opens a pull request for updates; the test build runs on it first.

### Secrets needed for releases
`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` (repo → Settings → Secrets and variables → Actions).
Real AdMob IDs are at the top of `app/build.gradle.kts`; test builds always use Google's test ads.

## Store listing
`store/` holds the Play icon, feature graphic and listing text; `docs/` only redirects to the official privacy policy at https://adamselite.com/apps/stop-time/privacy/.

## Testing "Remove Ads" on a sideloaded test build
Long-press the version line at the bottom of the Achievements screen to pretend the purchase
was made (test builds only). Real purchases only work once the app is in the Play Console
(internal testing) and installed from Google Play.
