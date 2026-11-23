plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "com.example.app_events"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.example.app_events"
        minSdk = 29
        targetSdk = 36
        versionCode = 2
        versionName = "1.0-basic"
    }

    val apiUrl = providers.gradleProperty("eventlyApiUrl")
    buildTypes {
        debug {
            val url = apiUrl.orElse("http://10.0.2.2:3001/api/").get().trimEnd('/')
            buildConfigField("String", "EVENTLY_API_URL", "\"" + url + "/\"")
        }
        release {
            val url = apiUrl.orElse("").get().trimEnd('/')
            buildConfigField("String", "EVENTLY_API_URL", "\"" + (if (url.startsWith("https://")) "$url/" else "") + "\"")
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
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.material3)
    implementation(libs.compose.icons)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.navigation.compose)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.android)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    implementation(libs.retrofit)
    implementation(libs.converter.moshi)
    implementation(libs.okhttp)
    implementation(libs.coroutines.android)
    ksp(libs.hilt.compiler)
    ksp(libs.room.compiler)
    ksp(libs.moshi.codegen)
    testImplementation(libs.junit)
    testImplementation(libs.coroutines.test)
}
