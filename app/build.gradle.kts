plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
    alias(libs.plugins.androidx.room3)
}

// Release signing keys live outside the repo, in ~/.gradle/gradle.properties (see docs/decisions/0020).
// Without them the release build is left unsigned instead of failing.
val releaseStoreFile = providers.gradleProperty("lucrometro.releaseStoreFile").orNull

android {
    namespace = "com.lucrometro"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.lucrometro"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1"
    }

    if (releaseStoreFile != null) {
        signingConfigs {
            create("release") {
                storeFile = file(releaseStoreFile)
                storePassword = providers.gradleProperty("lucrometro.releaseStorePassword").get()
                keyAlias = providers.gradleProperty("lucrometro.releaseKeyAlias").get()
                keyPassword = providers.gradleProperty("lucrometro.releaseKeyPassword").get()
            }
        }
        buildTypes {
            release {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    buildFeatures {
        compose = true
    }

    // Per-app language (Android 13+ Settings > Apps > Lucrometro > Language): the list is generated
    // from the values-* folders, with English as the default (res/resources.properties).
    androidResources {
        generateLocaleConfig = true
    }
}

// Exported schemas are committed: they are the base for future migrations.
room3 {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.cronet.embedded)
    implementation(libs.androidx.room3.runtime)
    implementation(libs.androidx.sqlite.framework)
    ksp(libs.androidx.room3.compiler)
    testImplementation(libs.junit)
    testImplementation(libs.org.json)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
