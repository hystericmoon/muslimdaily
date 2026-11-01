#!/bin/bash
# Setup script for Adwam cloud sessions (paste into the environment's Setup script, or run by hand).
# Publishes the haho-design version that gradle/libs.versions.toml pins to ~/.m2, so Gradle builds
# work without the manual steps in docs/handover.md, "Session setup (cloud)".
# Needs hystericmoon/haho-design under the environment's Repositories (it is private).
set -euo pipefail
export ANDROID_HOME=/opt/android-sdk

APP=/home/user/muslimdaily
LIB=/home/user/haho-design
TOML="$APP/gradle/libs.versions.toml"

if [ -f "$TOML" ]; then
  VERSION=$(sed -n 's/^hahoDesign = "\(.*\)"/\1/p' "$TOML")
else
  VERSION=""  # the app repo is not cloned yet: use the library's default branch
fi

if [ ! -d "$LIB/.git" ]; then
  git clone --depth 1 ${VERSION:+--branch "v$VERSION"} https://github.com/hystericmoon/haho-design "$LIB"
elif [ -n "$VERSION" ]; then
  git -C "$LIB" fetch --depth 1 origin tag "v$VERSION"
  git -C "$LIB" checkout -q "v$VERSION"
fi

if [ -n "$VERSION" ] && [ -d "$HOME/.m2/repository/id/haho/haho-design/$VERSION" ]; then
  echo "haho-design $VERSION already in ~/.m2"
else
  (cd "$LIB" && ./gradlew -q publishToMavenLocal)
  echo "haho-design ${VERSION:-default branch} published to ~/.m2"
fi
