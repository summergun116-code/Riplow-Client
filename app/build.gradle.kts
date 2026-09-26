plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.riplow.client"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.riplow.client"
        minSdk = 26
        targetSdk = 35
        versionCode = 3
        versionName = "0.1.2"

        externalNativeBuild {
            cmake {
                cppFlags += "-std=c++20"
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

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.30.5"
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
}
