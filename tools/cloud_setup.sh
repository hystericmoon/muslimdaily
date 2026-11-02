#!/bin/bash
# Adwam cloud setup: Android SDK 37, Maven Central mirror for Gradle, haho-design in mavenLocal.
# Paste this whole file into the cloud environment's Setup script (the environment needs
# hystericmoon/haho-design under Repositories, it is private), or run it by hand in a session
# after attaching haho-design. Safe to run again: every step skips what is already there.
set -euo pipefail
SDK=/opt/android-sdk
MIRROR=https://maven-central.storage-download.googleapis.com/maven2/
APP=/home/user/muslimdaily
# Used only when the app repo is not cloned yet when this runs; otherwise the pin in
# gradle/libs.versions.toml wins. Keep it at the current pin.
HAHO_FALLBACK=0.2.0

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

# 3. haho-design (shared design system) into mavenLocal, at the version the app pins.
VERSION=$(sed -n 's/^hahoDesign = "\(.*\)"/\1/p' "$APP/gradle/libs.versions.toml" 2>/dev/null || true)
VERSION=${VERSION:-$HAHO_FALLBACK}
if [ -d ~/.m2/repository/id/haho/haho-design/"$VERSION" ]; then
  echo "haho-design $VERSION already in ~/.m2"
else
  rm -rf /tmp/haho-design
  if git clone -q --depth 1 --branch "v$VERSION" https://github.com/hystericmoon/haho-design.git /tmp/haho-design; then
    (cd /tmp/haho-design && ANDROID_HOME=$SDK ./gradlew -q publishToMavenLocal)
    rm -rf /tmp/haho-design
    echo "haho-design $VERSION published to ~/.m2"
  else
    # Not fatal: the session attaches hystericmoon/haho-design (add_repo) and runs this again.
    echo "haho-design not reachable from the setup script; publish it in-session" >&2
  fi
fi
