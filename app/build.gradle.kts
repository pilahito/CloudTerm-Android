plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.pilahito.cloudterm.android"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.pilahito.cloudterm.android"
        minSdk = 26
        targetSdk = 34
        versionCode = 6
        versionName = "1.3.1"
    }

    signingConfigs {
        create("stable") {
            val ks = rootProject.file("store/cloudterm.jks")
            if (ks.exists()) {
                storeFile = ks
                storePassword = "cloudterm"
                keyAlias = "cloudterm"
                keyPassword = "cloudterm"
            }
        }
    }

    buildTypes {
        debug {
            val stable = signingConfigs.findByName("stable")
            if (stable != null && stable.storeFile != null) {
                signingConfig = stable
            }
        }
        release {
            isMinifyEnabled = false
            val stable = signingConfigs.findByName("stable")
            if (stable != null && stable.storeFile != null) {
                signingConfig = stable
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "META-INF/DEPENDENCIES",
                "META-INF/INDEX.LIST",
                "META-INF/LICENSE.md",
                "META-INF/NOTICE.md",
                "META-INF/versions/**",
            )
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.09.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.fragment:fragment-ktx:1.8.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    implementation("com.github.mwiede:jsch:0.2.20")
    implementation("org.bouncycastle:bcprov-jdk18on:1.78.1")
    implementation("commons-net:commons-net:3.11.1")
}
