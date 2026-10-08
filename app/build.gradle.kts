plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.adamselite.stoptime"
    compileSdk = 35

    defaultConfig {
        // NOTE: the package name is permanent once published on Google Play.
        applicationId = "com.adamselite.stoptime"
        minSdk = 26
        targetSdk = 35
        versionCode = 11
        versionName = "2.1"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")

    // Google AdMob (banner + full-screen ads)
    implementation("com.google.android.gms:play-services-ads:23.6.0")

    // Google Play Billing ("Remove Ads" purchase)
    implementation("com.android.billingclient:billing:8.0.0")
}
