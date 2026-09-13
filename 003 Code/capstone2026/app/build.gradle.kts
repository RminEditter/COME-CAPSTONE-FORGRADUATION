import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}


val photoProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
// Public endpoint only. The Google API key stays in server Secret Manager.
val cafePhotoEndpoint = photoProperties.getProperty("cafe.photo.endpoint", "").trim()
require(cafePhotoEndpoint.isEmpty() || cafePhotoEndpoint.matches(
    Regex("https://[A-Za-z0-9.-]+/(?:[A-Za-z0-9_/-]*)"))) { "Invalid cafe.photo.endpoint HTTPS URL" }

android {
    namespace = "com.example.capstone2026"
    compileSdk = 35


    defaultConfig {
        buildConfigField("String", "CAFE_PHOTO_ENDPOINT", "\"$cafePhotoEndpoint\"")
        applicationId = "com.example.capstone2026"
        minSdk = 24
        targetSdk = 35
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
    buildFeatures { buildConfig = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.android.gms:play-services-location:21.0.1")
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    implementation("org.json:json:20210307")
    implementation("androidx.room:room-runtime:2.6.1")
    annotationProcessor("androidx.room:room-compiler:2.6.1")
    implementation("com.google.firebase:firebase-auth")
    implementation("androidx.viewpager2:viewpager2:1.1.0")
    implementation("androidx.cardview:cardview:1.0.0")

    implementation("org.maplibre.gl:android-sdk-opengl:13.0.2")
}
