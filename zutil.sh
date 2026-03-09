#!/bin/zsh

source /Users/dasarathi/.zshrc
setJDK21;
export LOCAL_USER_HOME=/Users/dasarathi/.gradle
export GRADLE_USER_HOME=/Users/dasarathi/Development/WSELF/OCI-DevOps-ToolKit/.gradle-user-home
./gradlew --stop
rm -rf "$LOCAL_USER_HOME";
mkdir -p "$LOCAL_USER_HOME";

rm -rf "$GRADLE_USER_HOME";
mkdir -p "$GRADLE_USER_HOME"
./gradlew test --no-daemon
./gradlew clean build --refresh-dependencies --no-daemon