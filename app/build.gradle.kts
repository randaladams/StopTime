plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

// =====================================================================
//  ADMOB IDs  —  the ONLY place to put your real AdMob IDs.
//  Test builds (debug) always use Google's test ads, so you can never
//  accidentally click your own real ads (which can get AdMob banned).
// =====================================================================
val admobAppIdReal        = "ca-app-pub-7909301840077799~8130792870"
val admobBannerIdReal     = "ca-app-pub-7909301840077799/1667482991"
val admobInterstitialReal = "ca-app-pub-7909301840077799/8065370380"

val admobAppIdTest        = "ca-app-pub-3940256099942544~3347511713"
val admobBannerIdTest     = "ca-app-pub-3940256099942544/9214589741"
val admobInterstitialTest = "ca-app-pub-3940256099942544/1033173712"

// =====================================================================
//  VERSION  —  set automatically by GitHub Actions, never edited by hand.
//  * Releases: Actions -> "Android" -> Run workflow -> type a version like 1.0.1.
//    versionCode = major*10000 + minor*100 + patch  (1.0.1 -> 10001), so it always
//    goes up when the version goes up, as Google Play requires.
//  * Test builds: versionName like "1.0.1-test.42", versionCode 1.
// =====================================================================
val appVersionName: String = System.getenv("STOPTIME_VERSION_NAME") ?: "0.0.0-dev"
val appVersionCode: Int = System.getenv("STOPTIME_VERSION_CODE")?.toIntOrNull() ?: 1

android {
    namespace = "com.adamselite.stoptime"
    compileSdk = 36

    defaultConfig {
        // NOTE: the package name is permanent once published on Google Play.
        applicationId = "com.adamselite.stoptime"
        minSdk = 26
        targetSdk = 36
        // Set automatically by the GitHub workflow (see "VERSION" above). Don't edit by hand.
        versionCode = appVersionCode
        versionName = appVersionName
    }

    // Release signing. The key + passwords come from GitHub Secrets (see .github/workflows/release.yml).
    signingConfigs {
        create("release") {
            val ks = System.getenv("STOPTIME_KEYSTORE_FILE")
            if (ks != null) {
                storeFile = file(ks)
                storePassword = System.getenv("STOPTIME_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("STOPTIME_KEY_ALIAS")
                keyPassword = System.getenv("STOPTIME_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        debug {
            manifestPlaceholders["admobAppId"] = admobAppIdTest
            buildConfigField("String", "ADMOB_BANNER_ID", "\"$admobBannerIdTest\"")
            buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"$admobInterstitialTest\"")
        }
        release {
            isMinifyEnabled = false
            manifestPlaceholders["admobAppId"] = admobAppIdReal
            buildConfigField("String", "ADMOB_BANNER_ID", "\"$admobBannerIdReal\"")
            buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"$admobInterstitialReal\"")
            if (System.getenv("STOPTIME_KEYSTORE_FILE") != null) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

// Stop a release build if the real AdMob IDs were never filled in.
tasks.matching { it.name == "bundleRelease" || it.name == "assembleRelease" }.configureEach {
    doFirst {
        if (admobAppIdReal.contains("XXXX") || admobBannerIdReal.contains("XXXX") || admobInterstitialReal.contains("XXXX")) {
            throw GradleException("Fill in your real AdMob IDs at the top of app/build.gradle.kts before building a release.")
        }
    }
}

// Versions are in gradle/libs.versions.toml
dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.play.services.ads)   // Google AdMob (banner + full-screen ads)
    implementation(libs.ump)                 // Consent pop-up for Europe/UK (required to show ads there)
    implementation(libs.billing)             // Google Play Billing ("Remove Ads" purchase)
}
