#!/bin/bash
# Adwam cloud setup: Android SDK 37 and the Maven Central mirror for Gradle.
# Paste this whole file into the cloud environment's Setup script, or run it by hand in a
# session. Safe to run again: every step skips what is already there.
set -euo pipefail
SDK=/opt/android-sdk
MIRROR=https://maven-central.storage-download.googleapis.com/maven2/

# 1. Android SDK
if [ ! -x "$SDK/cmdline-tools/latest/bin/sdkmanager" ]; then
  mkdir -p "$SDK/cmdline-tools"
  curl -fsSL -o /tmp/cmdline-tools.zip https://dl.google.com/android/repository/commandlinetools-linux-16111833_latest.zip
  unzip -q /tmp/cmdline-tools.zip -d /tmp/cmdline-tools
  mv /tmp/cmdline-tools/cmdline-tools "$SDK/cmdline-tools/latest"
  rm -rf /tmp/cmdline-tools /tmp/cmdline-tools.zip
fi
yes | "$SDK/cmdline-tools/latest/bin/sdkmanager" --sdk_root="$SDK" --licenses >/dev/null || true
"$SDK/cmdline-tools/latest/bin/sdkmanager" --sdk_root="$SDK" "platforms;android-37.0" "build-tools;37.0.0" "build-tools;36.0.0" "platform-tools" >/dev/null

# 2. Gradle: use the Google-hosted Maven Central mirror (repo.maven.apache.org rate-limits the proxy)
mkdir -p ~/.gradle/init.d
cat > ~/.gradle/init.d/mirror.gradle <<GRADLE
def mirror = '$MIRROR'
def swap = { RepositoryHandler repos ->
    repos.all { r ->
        if (r instanceof MavenArtifactRepository && r.url.toString().contains('repo.maven.apache.org')) {
            r.url = mirror
        }
    }
}
beforeSettings { settings ->
    settings.pluginManagement.repositories {
        google()
        maven { url mirror }
        gradlePluginPortal()
    }
    swap(settings.pluginManagement.repositories)
    swap(settings.dependencyResolutionManagement.repositories)
    settings.buildscript.repositories { maven { url mirror } }
}
allprojects {
    swap(buildscript.repositories)
    swap(repositories)
    tasks.withType(Test).configureEach { systemProperty 'robolectric.dependency.repo.url', mirror }
}
GRADLE
grep -q ANDROID_HOME ~/.bashrc 2>/dev/null || echo "export ANDROID_HOME=$SDK" >> ~/.bashrc
