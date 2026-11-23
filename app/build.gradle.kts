plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.sherubhai.assistant"
    compileSdk = 35
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    defaultConfig {
        applicationId = "com.sherubhai.assistant"
        minSdk = 26
        targetSdk = 35
        versionCode = 2
        versionName = "0.2-safe"
    }
}
kotlin {
    jvmToolchain(17)
}
