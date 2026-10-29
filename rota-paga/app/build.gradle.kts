plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

fun env(name: String, def: String = ""): String = System.getenv(name)?.takeIf { it.isNotBlank() } ?: def

// Versão: o GitHub Actions passa o número do build (VERSION_CODE); no PC fica 1.
val vCode = env("VERSION_CODE", "1").toInt()
val vName = env("VERSION_NAME", "1.0.$vCode")

android {
    namespace = "com.velox.rotapaga"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.velox.rotapaga"
        minSdk = 26
        targetSdk = 36
        versionCode = vCode
        versionName = vName
    }

    // Mesma chave de upload dos outros apps (secrets do GitHub). Sem ela, o release sai sem assinatura.
    val ksPath = env("KEYSTORE_FILE", "../../release.jks")
    val hasKs = file(ksPath).exists()
    signingConfigs {
        create("release") {
            if (hasKs) {
                storeFile = file(ksPath)
                storePassword = env("KEYSTORE_PASSWORD")
                keyAlias = env("KEY_ALIAS", "upload")
                keyPassword = env("KEY_PASSWORD", env("KEYSTORE_PASSWORD"))
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = if (hasKs) signingConfigs.getByName("release") else null
        }
    }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
    buildFeatures {
        buildConfig = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.activity:activity-ktx:1.10.1")
    implementation("androidx.webkit:webkit:1.12.1")
}
