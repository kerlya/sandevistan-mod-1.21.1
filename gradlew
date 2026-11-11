#!/usr/bin/env sh
set -eu
V=8.10.2
B="${GRADLE_USER_HOME:-$HOME/.gradle}/bootstrap"
D="$B/gradle-$V"
Z="$B/gradle-$V-bin.zip"
if [ ! -x "$D/bin/gradle" ]; then
 mkdir -p "$B"
 if command -v curl >/dev/null 2>&1; then curl -fL "https://services.gradle.org/distributions/gradle-$V-bin.zip" -o "$Z"; else wget "https://services.gradle.org/distributions/gradle-$V-bin.zip" -O "$Z"; fi
 unzip -oq "$Z" -d "$B"
fi
exec "$D/bin/gradle" "$@"
