plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services") version "4.4.2"
}

android {
    namespace = "com.example.bodega"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.bodega"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    implementation(libs.glide)
    implementation(libs.recyclerview)
    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)
    implementation(libs.zxing.embedded)
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)

    // Firebase BoM y dependencias base
    implementation(platform("com.google.firebase:firebase-bom:33.4.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-database")
}