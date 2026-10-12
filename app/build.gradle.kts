plugins {
    id("com.android.application")
}

android {
    namespace = "com.pocketpi.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.pocketpi.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 2
        versionName = "0.2.0-m1.5"
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
}
