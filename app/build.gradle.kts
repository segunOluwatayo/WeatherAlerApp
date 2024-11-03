// app/build.gradle.kts (App-Level)

plugins {
//    id("com.android.application")
//    kotlin("android")
//    kotlin("kapt") // For annotation processing with Room
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp") version "1.9.10-1.0.13"
}

android {
    namespace = "com.example.weatheralertapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.weatheralertapp"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        // Enabling multidex support.
        multiDexEnabled = true
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.3"
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packagingOptions {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Kotlin Standard Library
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.10")

    // Android Core
    implementation("androidx.core:core-ktx:1.10.1")

    // Lifecycle Runtime
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")

    // Activity Compose
    implementation("androidx.activity:activity-compose:1.7.2")

    // Jetpack Compose UI
    implementation("androidx.compose.ui:ui:1.5.1")
    implementation("androidx.compose.material:material:1.5.1")
    implementation("androidx.compose.ui:ui-tooling-preview:1.5.1")
    implementation(libs.androidx.material3.android)
    implementation(libs.androidx.monitor)
    implementation(libs.androidx.junit.ktx)
    androidTestImplementation(libs.junit.junit)
    debugImplementation("androidx.compose.ui:ui-tooling:1.5.1")

    // Material Design 3
    implementation("androidx.compose.material3:material3:1.3.1")

    // Navigation Compose
    implementation("androidx.navigation:navigation-compose:2.7.0")

    // Room components
    implementation("androidx.room:room-runtime:2.5.2")
    ksp("androidx.room:room-compiler:2.5.2")
    implementation("androidx.room:room-ktx:2.5.2")

    // Google Play Services Location
    implementation("com.google.android.gms:play-services-location:21.0.1")

    // Lifecycle components for Compose
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.1")

    // Coroutines for Room
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
}
