plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    /* Plugin do Google Services necessário para a integração com o Firebase */
    id("com.google.gms.google-services")
}

android {
    buildFeatures {
        viewBinding = true
    }
    namespace = "com.example.acendeuareserva"

    /* Configurações de SDK e compilação para compatibilidade com versões recentes do Android */
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.acendeuareserva"
        minSdk = 24
        targetSdk = 36
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

    /* Define a compatibilidade das versões do Java e da JVM para o projeto Kotlin */
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    /* Bibliotecas base da plataforma Android e Material Design */
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    /* Bibliotecas para renderização de mapas (OSMDroid) e serviços de geolocalização */
    implementation("org.osmdroid:osmdroid-android:6.1.18")
    implementation("com.google.android.gms:play-services-location:21.0.1")

    /* Bibliotecas para comunicação em rede e consumo de APIs REST (Retrofit/OkHttp) */
    implementation("com.squareup.okhttp3:okhttp:4.9.3")
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-scalars:2.9.0")

    /* Bibliotecas para execução de tarefas assíncronas e gerenciamento de ciclo de vida (MVVM) */
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.2")

    /* Integração com a plataforma Firebase para autenticação e banco de dados em tempo real */
    implementation(platform("com.google.firebase:firebase-bom:32.8.0"))
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-database-ktx")
}