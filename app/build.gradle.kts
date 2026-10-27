plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

fun env(name: String, def: String = ""): String = System.getenv(name)?.takeIf { it.isNotBlank() } ?: def

// Versão: o GitHub Actions passa o número do build (VERSION_CODE); no PC fica 1.
val vCode = env("VERSION_CODE", "1").toInt()
val vName = env("VERSION_NAME", "1.0.$vCode")

android {
    namespace = "com.veloxplay.app"
    compileSdk = 36

    defaultConfig {
        minSdk = 23
        targetSdk = 36
        versionCode = vCode
        versionName = vName
    }

    // ====== OS DOIS APPS ======
    // Cada "sabor" vira um app separado na Play Store, com nome, ícone, ID e site próprios.
    flavorDimensions += listOf("marca", "canal")
    productFlavors {
        create("velox") {
            dimension = "marca"
            applicationId = "com.veloxplay.velox"
            resValue("string", "app_name", "Velox Play")
            buildConfigField("String", "SITE_URL", "\"http://betaspark.saiyajinpixel.online/webplayer/\"")
        }
        create("pulso") {
            dimension = "marca"
            applicationId = "com.veloxplay.pulso"
            resValue("string", "app_name", "Pulso Play")
            buildConfigField("String", "SITE_URL", "\"http://pulso.saiyajinpixel.online/velox-pulso/\"")
        }
        // "play" = Play Store (a loja atualiza). "direto" = APK do Downloader, que se atualiza sozinho pelo painel.
        create("play") {
            dimension = "canal"
            buildConfigField("boolean", "SELF_UPDATE", "false")
        }
        create("direto") {
            dimension = "canal"
            buildConfigField("boolean", "SELF_UPDATE", "true")
        }
    }

    // Chave de assinatura (upload key). No GitHub vem dos "secrets"; sem ela o build sai com a chave de teste.
    val ksPath = env("KEYSTORE_FILE", "../release.jks")
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
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // sem a chave, sai sem assinatura (para assinar depois com apksigner/jarsigner)
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
    implementation("androidx.media3:media3-exoplayer:1.8.0")
    implementation("androidx.media3:media3-exoplayer-hls:1.8.0")
    implementation("androidx.media3:media3-ui:1.8.0")
}
