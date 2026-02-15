plugins {
    alias(libs.plugins.android.application)
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.group04.scrapbookwidget"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.group04.scrapbookwidget"
        minSdk = 29
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

    buildFeatures {
        dataBinding = true
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation(libs.dagger.hilt.android)
    annotationProcessor(libs.hilt.compiler)

    debugImplementation(libs.fragment.testing.manifest)
    androidTestImplementation(libs.androidx.fragment.testing)
    androidTestImplementation(libs.androidx.runner)
    androidTestImplementation(libs.rules)
}