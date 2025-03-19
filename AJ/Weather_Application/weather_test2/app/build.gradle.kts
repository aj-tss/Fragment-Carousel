plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
}

android {
    namespace = "com.example.weather_test2"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.weather_test2"
        minSdk = 26
        targetSdk = 34
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

    buildFeatures {
        viewBinding =  true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)  // Keep only one Material dependency
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // Testing dependencies
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // ViewModel and LiveData
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)

    // Network requests (Choose one: Retrofit or Volley)
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    // implementation(libs.volley)  // Remove if not needed

    // UI & Animation
    implementation(libs.glide)
    implementation(libs.android.spinkit)  // Keep only one SpinKit version
    implementation(libs.lottie)

    // Location Services
    implementation(libs.play.services.location)

    // Swipe Refresh
    implementation(libs.androidx.swiperefreshlayout)

    // Responsive screen size
    implementation(libs.sdp.android)  // Ensure correct mapping

    // Multi Dex Enable
    implementation(libs.androidx.multidex)
}




