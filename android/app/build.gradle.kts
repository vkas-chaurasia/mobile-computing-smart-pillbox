

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.teamA.pillbox"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.teamA.pillbox"
        minSdk = 23
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.4.3"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)

    // Lifecycle dependencies
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.10.0")
    // ADDED: ViewModel utilities for Compose
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")

    implementation("no.nordicsemi.android:ble-ktx:2.11.0")
    // Note: You have an odd dependency here, 'room-external-antlr'.
    // This is not a standard Room library. If you intended to use Room,
    // you should use dependencies like 'androidx.room:room-runtime' and 'androidx.room:room-ktx'.
    implementation(libs.androidx.room.external.antlr)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // Compose Bill of Materials (BOM) - This manages versions for other Compose libraries
    implementation(platform("androidx.compose:compose-bom:2025.11.01"))

    // Compose dependencies
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Activity dependencies for Compose integration
    implementation("androidx.activity:activity-ktx:1.12.0")
    implementation("androidx.activity:activity-compose:1.12.0")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")

}
