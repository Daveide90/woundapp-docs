import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

// Credenziali di firma release lette da keystore.properties (gitignorato, MAI committato).
// Vedi keystore.properties.example. Se il file manca, il release viene assemblato non firmato
// (build di sviluppo/CI): la firma di produzione va configurata prima della pubblicazione.
val keystorePropsFile = rootProject.file("keystore.properties")
val keystoreProps = Properties().apply {
    if (keystorePropsFile.exists()) keystorePropsFile.inputStream().use { load(it) }
}

android {
    namespace = "com.example.woundmanagement"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.daveide.wounddocs"
        minSdk = 26
        targetSdk = 34
        // Numerazione progressiva: versionName = versione "commerciale" (v1.0);
        // versionCode = numero di build, +1 a ogni consegna (il più alto è il più recente).
        versionCode = 57
        versionName = "1.6.6-docs"
    }

    signingConfigs {
        create("release") {
            if (keystorePropsFile.exists()) {
                storeFile = file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Firma il release SOLO se le credenziali sono presenti, così build di sviluppo/CI
            // senza keystore non falliscono; la release pubblicata DEVE avere keystore.properties.
            if (keystorePropsFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        debug {
            isMinifyEnabled = false
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
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.5"
    }

    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    // androidx.biometric:1.1.0 trascina il VECCHIO androidx.fragment:1.2.5, il cui
    // FragmentActivity.startActivityForResult rifiuta i requestCode > 16 bit generati dal
    // registro di rememberLauncherForActivityResult (activity 1.8.2) -> IllegalArgumentException
    // ("Can only use lower 16 bits for requestCode") sul selettore file di backup.
    // Forziamo una versione di fragment allineata ad activity 1.8.2.
    implementation("androidx.fragment:fragment-ktx:1.6.2")

    implementation(platform("androidx.compose:compose-bom:2024.02.02"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("androidx.navigation:navigation-compose:2.7.6")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Biometric authentication
    implementation("androidx.biometric:biometric:1.1.0")

    // Security: database encryption + keystore
    // Sicurezza (3/11/2026): SQLCipher per Android 4.17.0, con SQLite 3.53.3, al posto di
    // android-database-sqlcipher 4.5.4 (dismessa, SQLite 3.41.2 del 2023, dieci CVE note di
    // SQLite). Stesso formato dei database (SQLCipher 4) e stessa chiave in byte. La 4.19 e la
    // 4.20 chiedono compileSdk 36, che con questo Android Gradle Plugin (8.2) non si può.
    implementation("net.zetetic:sqlcipher-android:4.17.0@aar")
    // androidx.sqlite resta 2.4.0: la 2.6 porta Kotlin 2.1, che il compilatore 1.9 di questo
    // progetto non legge. Verificato sul bytecode (3/11/2026): le 8 classi e gli 11 metodi e
    // campi di androidx.sqlite usati da SQLCipher 4.17.0 esistono tutti, uguali, nella 2.4.0.
    implementation("androidx.sqlite:sqlite:2.4.0")
    implementation("androidx.security:security-crypto:1.1.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // Test
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.11.1")
    testImplementation("androidx.test:core:1.5.0")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
