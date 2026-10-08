plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.stoptime.game"
    compileSdk = 35

    defaultConfig {
        // NOTE: the package name is permanent once published on Google Play.
        applicationId = "com.stoptime.game"
        minSdk = 26
        targetSdk = 35
        versionCode = 8
        versionName = "1.7"
    }

    // Two versions of the app are built from the same code:
    //   free = shows ads (banner + full-screen every 4th try)
    //   pro  = no ads
    flavorDimensions += "version"
    productFlavors {
        create("free") {
            dimension = "version"
            applicationIdSuffix = ".free"
            versionNameSuffix = "-free"
            resValue("string", "app_name", "StopTime")
            buildConfigField("boolean", "IS_PRO", "false")
        }
        create("pro") {
            dimension = "version"
            resValue("string", "app_name", "StopTime Pro")
            buildConfigField("boolean", "IS_PRO", "true")
        }
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

    // Google AdMob - only included in the FREE version
    "freeImplementation"("com.google.android.gms:play-services-ads:23.6.0")
}
