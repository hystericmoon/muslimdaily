import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.roborazzi)
    alias(libs.plugins.baselineprofile)
}

val reportEmail = providers.gradleProperty("muslimdaily.reportEmail").getOrElse("")
val updateRepo = providers.gradleProperty("muslimdaily.updateRepo").getOrElse("hystericmoon/muslimdaily")

// Release signing comes from keystore.properties (never committed); see docs/release.md.
val keystoreProps = rootProject.file("keystore.properties").takeIf { it.exists() }?.let { file ->
    Properties().apply { file.inputStream().use { load(it) } }
}
val robolectricRepoUrl: String? = providers.gradleProperty("muslimdaily.robolectricRepoUrl").orNull

android {
    namespace = "id.haho.muslimdaily"
    compileSdk = 37

    defaultConfig {
        applicationId = "id.haho.muslimdaily"
        minSdk = 26
        targetSdk = 37
        versionCode = 25
        versionName = "0.7.0-beta.4"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "REPORT_EMAIL", "\"$reportEmail\"")
        // GitHub repository whose releases the sideload updater reads (debug and sideload builds).
        buildConfigField("String", "UPDATE_REPO", "\"$updateRepo\"")
    }

    signingConfigs {
        // Shared debug key (not secret) so sideload updates built on CI install over local builds.
        getByName("debug") {
            storeFile = file("debug.keystore")
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

    // The in-app language switch needs every language's resources installed, so a Play bundle
    // must not split them out by device language.
    bundle {
        language { enableSplit = false }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            buildConfigField("boolean", "SELF_UPDATE", "true")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Store builds stay fully offline: no updater, no INTERNET permission.
            buildConfigField("boolean", "SELF_UPDATE", "false")
            signingConfig = signingConfigs.findByName("release")
        }
        // What GitHub Releases ship: optimised like release (R8, not debuggable), but with the
        // debug package, shared key and updater, so it installs over earlier sideload builds.
        create("sideload") {
            initWith(getByName("release"))
            applicationIdSuffix = ".debug"
            buildConfigField("boolean", "SELF_UPDATE", "true")
            signingConfig = signingConfigs.getByName("debug")
            matchingFallbacks += listOf("release")
        }
    }

    // The sideload build uses the updater's manifest and file provider paths from the debug set.
    sourceSets.getByName("sideload") {
        manifest.srcFile("src/debug/AndroidManifest.xml")
        res.srcDirs("src/debug/res")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all { test ->
                test.systemProperty("robolectric.pixelCopyRenderMode", "hardware")
                test.jvmArgs(
                    "--add-opens=java.base/jdk.internal.access=ALL-UNNAMED",
                    "--add-exports=java.base/jdk.internal.access=ALL-UNNAMED",
                    "--add-opens=java.base/java.io=ALL-UNNAMED",
                )
                test.maxHeapSize = "3g"
                // Optional mirror for the Robolectric Android runtime (set in ~/.gradle/gradle.properties).
                robolectricRepoUrl?.let { test.systemProperty("robolectric.dependency.repo.url", it) }
            }
        }
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

// The app's own startup and Baseline Profiles, generated on a phone with
// `./gradlew :app:generateSideloadBaselineProfile` and committed under src/sideload/generated
// (the build GitHub Releases ship). The startup profile also orders the dex for launch.
baselineProfile {
    automaticGenerationDuringBuild = false
    dexLayoutOptimization = true
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
        optIn.add("androidx.compose.material3.ExperimentalMaterial3Api")
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

androidComponents {
    onVariants { variant ->
        val taskName = "generate${variant.name.replaceFirstChar { it.uppercase() }}ContentAssets"
        val task = tasks.register<ContentAssetsTask>(taskName) {
            contentDir.set(rootProject.layout.projectDirectory.dir("content"))
        }
        variant.sources.assets?.addGeneratedSourceDirectory(task, ContentAssetsTask::outputDir)
    }
}

dependencies {
    baselineProfile(project(":baselineprofile"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.text)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.animation)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.play.services.wearable)
    implementation(libs.play.services.location)
    implementation(libs.adhan)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(libs.robolectric)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.androidx.work.testing)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.junit.rule)
}

roborazzi {
    outputDir.set(rootProject.layout.projectDirectory.dir("docs/screenshots"))
}
