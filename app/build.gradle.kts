plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.ismael.daybyday"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ismael.daybyday"
        minSdk = 26
        targetSdk = 35
        versionCode = 52
        versionName = "5.12"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        resourceConfigurations += listOf("fr")

        // Instant ou cette version a ete construite, affiche dans "A propos".
        // C'est la date de fin de la mise a jour, pas celle de l'installation
        // sur le telephone : elle dit quelle version tourne, meme apres une
        // reinstallation.
        buildConfigField("long", "BUILD_TIME", "${System.currentTimeMillis()}L")
    }

    // Cle de signature fixe et volontairement publique : l'application n'est
    // pas distribuee et n'a aucune permission sensible. Elle sert uniquement a
    // ce que chaque nouvelle version s'installe par-dessus la precedente sans
    // effacer les donnees deja saisies.
    signingConfigs {
        getByName("debug") {
            storeFile = file("../keystore/daybyday.jks")
            storePassword = "daybyday"
            keyAlias = "daybyday"
            keyPassword = "daybyday"
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

/**
 * Le filet de securite de la regle numero un.
 *
 * Le manifeste retire la permission INTERNET meme si une bibliotheque la
 * reclame (`tools:node="remove"`). Cette tache verifie que c'est bien arrive :
 * elle lit le manifeste **fusionne**, celui qui part reellement dans l'APK, et
 * fait echouer la compilation s'il contient encore la permission.
 *
 * Autrement dit, la garantie « aucune donnee ne sort du telephone » n'est plus
 * tenue par la vigilance de qui ajoute une dependance, mais par le build.
 */
androidComponents {
    onVariants { variant ->
        val merged = variant.artifacts.get(com.android.build.api.artifact.SingleArtifact.MERGED_MANIFEST)
        val name = variant.name.replaceFirstChar { it.uppercase() }
        val verify = tasks.register("verifyNoInternet$name") {
            inputs.file(merged)
            doLast {
                val text = merged.get().asFile.readText()
                if (text.contains("android.permission.INTERNET")) {
                    throw GradleException(
                        "Le manifeste fusionne contient la permission INTERNET. " +
                            "C'est la regle que DayByDay ne casse jamais : sans reseau, " +
                            "aucune donnee ne peut sortir du telephone. Retire la " +
                            "dependance qui la reclame."
                    )
                }
            }
        }
        tasks.matching { it.name == "assemble$name" }.configureEach { dependsOn(verify) }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.incremental", "true")
}

dependencies {
    // Compose 1.7 a reecrit la saisie de texte. On reste sur la derniere
    // correction de cette ligne. Elle n'a pas rendu le double appui : la
    // selection d'un mot est ecrite a la main dans DoubleTapWord.kt.
    val composeBom = platform("androidx.compose:compose-bom:2025.02.00")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.fragment:fragment-ktx:1.8.5")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.4")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")

    // Lottie ne sert qu'a jouer des fichiers **embarques dans l'APK**
    // (res/raw). La bibliotheque sait aussi charger une animation depuis une
    // adresse, et declare donc la permission INTERNET dans son propre
    // manifeste : elle est retiree a la fusion par le `tools:node="remove"` du
    // notre, et la tache `verifyNoInternet…` ci-dessus fait echouer la
    // compilation si jamais elle survivait.
    implementation("com.airbnb.android:lottie-compose:6.6.6")

    implementation("androidx.biometric:biometric:1.1.0")
    implementation("androidx.exifinterface:exifinterface:1.3.7")

    implementation("androidx.work:work-runtime-ktx:2.9.1")
    implementation("androidx.health.connect:connect-client:1.1.0-alpha07")
    implementation("androidx.documentfile:documentfile:1.0.1")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("junit:junit:4.13.2")

    androidTestImplementation(composeBom)
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
