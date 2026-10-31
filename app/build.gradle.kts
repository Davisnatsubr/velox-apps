plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

fun env(name: String, def: String = ""): String = System.getenv(name)?.takeIf { it.isNotBlank() } ?: def

val vCode = env("VERSION_CODE", "1").toInt()

android {
    namespace = "br.painel.renovacoes"
    compileSdk = 36

    defaultConfig {
        applicationId = "br.painel.renovacoes"
        minSdk = 23
        targetSdk = 36
        versionCode = vCode
        versionName = "1.0.$vCode"
        // Endereço que já vem preenchido na primeira abertura (dá para trocar dentro do app)
        buildConfigField("String", "SITE_URL", "\"${env("SITE_URL", "")}\"")
    }

    // Chave fixa para que cada APK novo instale por cima do anterior
    signingConfigs {
        create("release") {
            storeFile = file("../assinatura/renovacoes.keystore")
            storePassword = "renovacoes2026"
            keyAlias = "renovacoes"
            keyPassword = "renovacoes2026"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("release")
        }
    }

    buildFeatures { buildConfig = true }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin { jvmToolchain(17) }
