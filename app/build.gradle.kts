plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.internlink"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.internlink"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            // Add Google Maps API key from local.properties
            val apiKey = project.findProperty("GOOGLE_MAPS_API_KEY") ?: ""
            resValue("string", "google_maps_key", apiKey.toString())
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Add Google Maps API key from local.properties
            val apiKey = project.findProperty("GOOGLE_MAPS_API_KEY") ?: ""
            resValue("string", "google_maps_key", apiKey.toString())

        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.firebase.auth)
    implementation(libs.core.ktx)
    implementation(libs.ui.graphics.android)
    implementation(libs.swiperefreshlayout)
    implementation(libs.firebase.messaging)
    implementation(libs.play.services.maps)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation("com.google.android.material:material:1.11.0")
    implementation(platform("com.google.firebase:firebase-bom:33.13.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth:22.3.0")
    implementation("com.google.firebase:firebase-database:20.3.0")
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    implementation("com.google.firebase:firebase-storage:20.3.0")
    implementation("com.google.android.gms:play-services-base:18.2.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.dropbox.core:dropbox-core-sdk:5.4.6")
    implementation ("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")
    implementation("com.google.firebase:firebase-messaging:23.4.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("com.github.chrisbanes:PhotoView:2.3.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.android.gms:play-services-location:21.0.1")
    implementation("com.google.android.gms:play-services-base:18.2.0")
    implementation("de.hdodenhof:circleimageview:3.1.0")
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("androidx.biometric:biometric:1.2.0-alpha05")

}