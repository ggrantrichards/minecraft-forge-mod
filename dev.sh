#!/usr/bin/env bash
# One command for everything. Run ./dev.sh help to see the list.
set -euo pipefail
cd "$(dirname "$0")"

usage() {
    cat <<'USAGE'
Usage: ./dev.sh [command]

  run       Launch Minecraft with your mod (the default)
  build     Build your mod jar into build/libs/
  source    Unpack Minecraft's source code, textures and data into ~/minecraft-source
  crash     Show the start of the newest crash report (paste this when asking for help)
  repair    Fix a broken first-time download, then build again
  help      Show this list
USAGE
}

# Minecraft 26.x needs Java 25. On a Mac, find it even if another Java is the default.
if [[ "$(uname)" == Darwin ]] && /usr/libexec/java_home -v 25 >/dev/null 2>&1; then
    export JAVA_HOME="$(/usr/libexec/java_home -v 25)"
fi
if [[ -n "${JAVA_HOME:-}" ]]; then
    export PATH="$JAVA_HOME/bin:$PATH"
fi

check_java() {
    local version
    version=$(java -version 2>&1 | awk -F'"' '/version/ {print $2; exit}' | cut -d. -f1)
    if [[ "$version" != 25 ]]; then
        echo "This needs Java 25, but found: ${version:-no Java}."
        echo "Install Temurin 25 (the .pkg on a Mac): https://adoptium.net/temurin/releases/?version=25"
        exit 1
    fi
}

sources_jar() {
    find .gradle/mavenizer/repo -name '*-sources.jar' 2>/dev/null | head -1
}

assets_jar() {
    find .gradle/mavenizer/repo/net/minecraft/client-extra -name '*.jar' 2>/dev/null | head -1
}

case "${1:-run}" in
    run)
        check_java
        ./gradlew runClient
        ;;
    build)
        check_java
        ./gradlew build
        echo
        echo "Your mod jar: $(ls build/libs/*.jar)"
        ;;
    source)
        check_java
        if [[ -z "$(sources_jar)" ]]; then
            echo "Setting up Minecraft first (the first time takes 5-15 minutes)..."
            ./gradlew build
        fi
        dest="$HOME/minecraft-source"
        mkdir -p "$dest"
        unzip -qo "$(sources_jar)" -d "$dest"
        unzip -qo "$(assets_jar)" -d "$dest"
        echo "Minecraft's source is in $dest"
        echo "  Java code:           $dest/net/minecraft/"
        echo "  Textures and models: $dest/assets/minecraft/"
        echo "  Recipes, loot, tags: $dest/data/minecraft/"
        echo "Open that folder in your editor (File > Add Folder to Workspace)."
        ;;
    crash)
        report=$(ls -t run/crash-reports/*.txt 2>/dev/null | head -1 || true)
        if [[ -z "$report" ]]; then
            echo "No crash reports in run/crash-reports/."
            exit 0
        fi
        echo "== $report"
        head -60 "$report"
        ;;
    repair)
        check_java
        ./gradlew --stop
        echo "Removing broken downloads..."
        find "$HOME/.gradle/caches/minecraftforge" -name '*.jar' -exec sh -c \
            'unzip -tqq "$1" >/dev/null 2>&1 || { echo "  deleted $1"; rm "$1"; }' _ {} \;
        rm -rf .gradle/mavenizer .gradle/configuration-cache
        ./gradlew build
        ;;
    help | -h | --help)
        usage
        ;;
    *)
        echo "Unknown command: $1"
        echo
        usage
        exit 1
        ;;
esac
