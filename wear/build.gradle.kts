import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

/** versionCode / versionName from app/build.gradle.kts, the single place a release bumps. */
fun phoneVersion(key: String): String {
    val text = providers.fileContents(rootProject.layout.projectDirectory.file("app/build.gradle.kts")).asText.get()
    return Regex("""$key = "?([^"\s]+)"?""").find(text)?.groupValues?.get(1) ?: error("No $key in app/build.gradle.kts")
}

// Same release key as the phone app (keystore.properties, never committed).
val keystoreProps = rootProject.file("keystore.properties").takeIf { it.exists() }?.let { file ->
    Properties().apply { file.inputStream().use { load(it) } }
}

// The watch app shares the phone app's id and signing key: the Wear data layer only connects
// apps with the same package name and signature.
android {
    namespace = "id.haho.muslimdaily.wear"
    compileSdk = 37

    defaultConfig {
        applicationId = "id.haho.muslimdaily"
        minSdk = 30
        targetSdk = 36
        // Follows the phone app, so each release's watch APK installs over the previous one.
        versionCode = phoneVersion("versionCode").toInt()
        versionName = phoneVersion("versionName")
    }

    signingConfigs {
        getByName("debug") {
            storeFile = rootProject.file("app/debug.keystore")
            storePassword = "android"
            keyAlias = "androiddebugkey"
            keyPassword = "android"
        }
        if (keystoreProps != null) {
            create("release") {
                storeFile = rootProject.file(keystoreProps.getProperty("storeFile"))
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            // Matches the phone's sideload build (id.haho.muslimdaily.debug).
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation(libs.androidx.wear.tiles)
    implementation(libs.androidx.wear.protolayout)
    implementation(libs.androidx.wear.protolayout.expression)
    implementation(libs.androidx.concurrent.futures.ktx)
    implementation(libs.play.services.wearable)
    implementation(libs.kotlinx.coroutines.android)
    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
}
