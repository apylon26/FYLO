plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}
android {
    namespace = "com.apylon26.fylo"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.apylon26.fylo.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 14
        versionName = "0.2.2"
    }
    buildTypes { getByName("debug") { signingConfig = null } }
    sourceSets.getByName("main").res.srcDir("../ui-icons/src/main/res")
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.ui:ui")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}
