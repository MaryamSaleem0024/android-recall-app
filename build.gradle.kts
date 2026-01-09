plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.selftalker"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.selftalker"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
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

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        }
    }


    buildFeatures {
        compose = true
    }

}
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    /* -------------------- Compose -------------------- */
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.compose.material.icons.extended)

    /* -------------------- Firebase -------------------- */
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)

    /* -------------------- Google Sign-In -------------------- */
    implementation(libs.play.services.auth)

    /* -------------------- Credentials API (NEW) -------------------- */
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)

    /* -------------------- Storage / DocumentFile -------------------- */
    implementation(libs.androidx.documentfile)

    /* -------------------- Room -------------------- */
    implementation(libs.androidx.room.runtime.v261)
    implementation(libs.androidx.room.ktx.v261)
    ksp(libs.androidx.room.compiler.v261)

    /* -------------------- DataStore -------------------- */
    implementation(libs.androidx.datastore.preferences)

    // Unit testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")

}

