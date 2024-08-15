plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.proximo.ali"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.proximo.ali"
        minSdk = 24
        targetSdk = 34
        versionCode = 3
        versionName = "0.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures {
        buildConfig = true  // Habilitar a geração de BuildConfig customizado
    }

    buildTypes {
        val userSyncApiEndpoint: String? = project.findProperty("USER_SYNC_API_ENDPOINT") as String?

        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            userSyncApiEndpoint?.let {
                buildConfigField("String", "USER_SYNC_API_ENDPOINT", "\"$it\"")
            }
        }

        debug {
            userSyncApiEndpoint?.let {
                buildConfigField("String", "USER_SYNC_API_ENDPOINT", "\"$it\"")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation("androidx.sqlite:sqlite:2.1.0")
    implementation("com.squareup.okhttp3:okhttp:4.9.3")
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    implementation("org.java-websocket:Java-WebSocket:1.5.7")
    implementation(kotlin("stdlib"))
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
}
