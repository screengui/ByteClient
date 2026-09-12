#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "${BASH_SOURCE[0]}")/.."

# Fabric Loom for this project requires Java 21, while the default Java on
# some Replit environments may be older. Prefer an installed non-debug JDK.
java_home="$(printf '%s\n' /nix/store/*-openjdk-21* 2>/dev/null | grep -v -- '-debug$' | sort -V | head -n 1)"
if [[ -z "$java_home" || ! -x "$java_home/bin/java" ]]; then
	echo "Post-merge setup requires an installed Java 21 runtime." >&2
	exit 1
fi

export JAVA_HOME="$java_home"
export PATH="$JAVA_HOME/bin:$PATH"

./gradlew --no-daemon build