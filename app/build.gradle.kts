plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
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

android {
    namespace = "com.adamselite.stoptime"
    compileSdk = 36

    defaultConfig {
        // NOTE: the package name is permanent once published on Google Play.
        applicationId = "com.adamselite.stoptime"
        minSdk = 26
        targetSdk = 36
        // versionCode MUST go up by at least 1 for every upload to Google Play.
        versionCode = 13
        versionName = "1.0"
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
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        buildConfig = true
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

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")

    // Google AdMob (banner + full-screen ads)
    implementation("com.google.android.gms:play-services-ads:23.6.0")
    // Google's consent pop-up for Europe/UK (required to show ads there)
    implementation("com.google.android.ump:user-messaging-platform:4.0.0")

    // Google Play Billing ("Remove Ads" purchase)
    implementation("com.android.billingclient:billing:8.0.0")
}
