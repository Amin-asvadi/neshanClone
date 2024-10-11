import java.io.FileInputStream
import java.util.Properties
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.dagger.hilt.android)
    alias(libs.plugins.kotlinx.serialization)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.neshan.neshantask"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.neshan.neshantask"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures {
        //noinspection DataBindingWithoutKapt
        dataBinding = true
        viewBinding =true
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
    defaultConfig {
        multiDexEnabled =true

    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    splits {
        abi {
            isEnable = true
            reset()
            include("armeabi-v7a")
            isUniversalApk = false
        }
    }
}

dependencies {
    // Network
    implementation (libs.retrofit)
    implementation (libs.logging.interceptor)
    implementation (libs.converter.gson)

    // RxJava
    implementation (libs.rxandroid)
    implementation (libs.rxjava)
    // RxJava3 Adapter for retrofit
    implementation (libs.rxjava3.retrofit.adapter)

    //Neshan sdk library
    implementation (libs.mobile.sdk)
    implementation (libs.services.sdk)
    implementation (libs.common.sdk)

    //Play Services
    implementation (libs.play.services.gcm)
    implementation (libs.play.services.location)
    implementation (libs.multidex)
    implementation (libs.hilt.navigation.fragment)
    implementation(libs.dagger.hilt.android)
    ksp(libs.dagger.hilt.compiler)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
fun getNeshanApiKey(): String {
    val propFile = rootProject.file("local.properties")
    val properties = Properties()

    if (propFile.exists()) {
        properties.load(FileInputStream(propFile))
        return properties.getProperty("NESHAN_API_KEY", "YOUR_DEFAULT_API_KEY")
    }
    return "YOUR_DEFAULT_API_KEY" // Fallback if local.properties doesn't exist
}
