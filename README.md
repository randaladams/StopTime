# StopTime

Stop the clock at exactly 1.00 second. Earn achievements.

- **free** version: banner ad at the bottom + full-screen ad every 4th try (Google AdMob, test ads for now)
- **pro** version: no ads

## Where things are
| What | File |
|---|---|
| Game screen logic | `app/src/main/java/com/stoptime/game/MainActivity.kt` |
| Achievement list (add new ones here) | `app/src/main/java/com/stoptime/game/achievements/Achievements.kt` |
| Ads (free version) | `app/src/free/java/com/stoptime/game/ads/AdsManager.kt` |
| No-ads (pro version) | `app/src/pro/java/com/stoptime/game/ads/AdsManager.kt` |
| Screen layout | `app/src/main/res/layout/activity_main.xml` |
| GitHub build | `.github/workflows/build.yml` |

## Building
Every push to `main` builds both APKs on GitHub. Download them from the **Actions** tab → latest run → **Artifacts**.
