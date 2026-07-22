import java.util.Properties

plugins {
    id("com.android.application")
    // Kotlin support built into AGP 9.0+
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.selftrain.app"
    compileSdk = 37

    val keystoreProps = Properties()
    val propsFile = rootProject.file("keystore.properties")
    if (propsFile.exists()) keystoreProps.load(propsFile.inputStream())

    signingConfigs {
        create("release") {
            storeFile = rootProject.file("release.jks")
            storePassword = keystoreProps.getProperty("storePassword") ?: "android"
            keyAlias = keystoreProps.getProperty("keyAlias") ?: "selftrain"
            keyPassword = keystoreProps.getProperty("keyPassword") ?: "android"
        }
    }

    defaultConfig {
        applicationId = "com.selftrain.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 37
        versionName = "1.0.3"
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    // ponytail: readable APK name for GitHub releases
    base { archivesName = "selftrain" }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom-alpha:2026.06.00")
    implementation(composeBom)

    // Compose
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    // M3 Expressive — 1.5.x from alpha BOM
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    // Google Fonts for M3 Expressive (Oswald + Inter)
    implementation("androidx.compose.ui:ui-text-google-fonts")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    // Room
    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")
    ksp("androidx.room:room-compiler:2.8.4")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.60.1")
    ksp("com.google.dagger:hilt-android-compiler:2.60.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    implementation("androidx.hilt:hilt-work:1.2.0")
    ksp("androidx.hilt:hilt-compiler:1.2.0")

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // JSON parsing for seed data
    implementation("com.google.code.gson:gson:2.11.0")

    // Coil for GIF loading
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("io.coil-kt:coil-gif:2.7.0")

    // SAF tree operations for user-chosen backup folder
    implementation("androidx.documentfile:documentfile:1.0.1")

    // Unit tests (JVM, no emulator)
    testImplementation("junit:junit:4.13.2")
}
