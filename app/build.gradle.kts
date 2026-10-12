plugins {
    id("com.android.application")
}

android {
    namespace = "com.pocketpi.app"
    compileSdk = 34

    signingConfigs {
        create("fixed") {
            storeFile = file("../keystore/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
    }

    defaultConfig {
        applicationId = "com.pocketpi.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 4
        versionName = "0.3.0-m2"
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("fixed")
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("fixed")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}