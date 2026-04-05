plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    // Actualizado para que coincida con CleanReport
    namespace = "com.krist.cleanreport"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.krist.cleanreport"
        minSdk = 30
        targetSdk = 34
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    kotlinOptions {
        jvmTarget = "17"
    }

    // Importante para Ktor: Evita duplicados en el empaquetado
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/INDEX.LIST"
            excludes += "META-INF/io.netty.versions.properties"
        }
    }
}

dependencies {
    // Ktor Server Core y Netty
    implementation("io.ktor:ktor-server-core:2.3.0")
    implementation("io.ktor:ktor-server-netty:2.3.0")
    implementation("io.ktor:ktor-server-host-common:2.3.0")
    
    // Logs para el servidor
    implementation("org.slf4j:slf4j-simple:1.7.30")
    
    // Android Standard Libs
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    
    // Webkit (por si decides usar vistas web locales más adelante)
    implementation("androidx.webkit:webkit:1.11.0")

    // Nota: He quitado NanoHTTPD porque ya estamos usando Ktor, 
    // tener dos servidores en el mismo APK podría causar conflictos de puertos.
}
