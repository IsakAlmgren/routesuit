import java.util.Base64
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) load(f.inputStream())
}

// Derive versionCode from git tag (e.g. v1.2.3 → 10203). Falls back to 1 locally.
fun gitVersionCode(): Int {
    return try {
        val tag = providers.exec {
            commandLine("git", "describe", "--tags", "--match", "v*", "--abbrev=0")
        }.standardOutput.asText.get().trim()
        val parts = tag.removePrefix("v").split(".")
        val major = parts.getOrNull(0)?.toIntOrNull() ?: 0
        val minor = parts.getOrNull(1)?.toIntOrNull() ?: 0
        val patch = parts.getOrNull(2)?.toIntOrNull() ?: 0
        major * 10000 + minor * 100 + patch
    } catch (_: Exception) { 1 }
}

fun gitVersionName(): String {
    return try {
        providers.exec {
            commandLine("git", "describe", "--tags", "--match", "v*", "--abbrev=0")
        }.standardOutput.asText.get().trim().removePrefix("v")
    } catch (_: Exception) { "1.0" }
}

android {
    namespace = "se.mildtanke.alltidredo"
    compileSdk = 36

    defaultConfig {
        applicationId = "se.mildtanke.alltidredo"
        minSdk = 31
        targetSdk = 36
        versionCode = gitVersionCode()
        versionName = gitVersionName()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val keystoreBase64 = System.getenv("KEYSTORE_BASE64")
            when {
                keystoreBase64 != null -> {
                    val keystoreFile = rootProject.layout.buildDirectory.file("release.keystore").get().asFile
                    keystoreFile.parentFile.mkdirs()
                    keystoreFile.writeBytes(Base64.getDecoder().decode(keystoreBase64))
                    storeFile = keystoreFile
                    storePassword = System.getenv("STORE_PASSWORD")
                    keyAlias = System.getenv("KEY_ALIAS")
                    keyPassword = System.getenv("KEY_PASSWORD")
                }
                keystoreProps.containsKey("storeFile") -> {
                    storeFile = file(keystoreProps["storeFile"] as String)
                    storePassword = keystoreProps["storePassword"] as String
                    keyAlias = keystoreProps["keyAlias"] as String
                    keyPassword = keystoreProps["keyPassword"] as String
                }
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            val releaseSigningConfig = signingConfigs.findByName("release")
            if (releaseSigningConfig?.storeFile != null) {
                signingConfig = releaseSigningConfig
            }
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

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.material)

    // Network
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    implementation(libs.gson)
    
    // Logging
    implementation(libs.timber)
    
    // Coroutines
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)
    
    // ViewModel
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    
    // WorkManager for notifications
    implementation(libs.androidx.work.runtime.ktx)
    
    // Koin for dependency injection
    implementation(libs.koin.android)
    implementation(libs.koin.androidx.compose)
    
    // Navigation Compose
    implementation(libs.androidx.navigation.compose)
    
    // Location Services
    implementation("com.google.android.gms:play-services-location:21.3.0")
    
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}