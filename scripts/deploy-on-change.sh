#!/usr/bin/env bash

set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
GRADLEW="$ROOT_DIR/gradlew"

find_default_adb_bin() {
    local candidate
    for candidate in \
        "${HOME:-/home/ariy}/Android/Sdk/platform-tools/adb" \
        "/opt/android-sdk/platform-tools/adb" \
        "/usr/lib/android-sdk/platform-tools/adb"
    do
        if [[ -x "$candidate" ]]; then
            printf '%s\n' "$candidate"
            return 0
        fi
    done

    if command -v adb >/dev/null 2>&1; then
        command -v adb
        return 0
    fi

    return 1
}

find_default_java_home() {
    local candidate
    for candidate in \
        "/snap/android-studio/current/jbr" \
        "/snap/android-studio/209/jbr" \
        "${HOME:-/home/ariy}/android-studio/jbr"
    do
        if [[ -d "$candidate" ]]; then
            printf '%s\n' "$candidate"
            return 0
        fi
    done

    if command -v java >/dev/null 2>&1; then
        candidate="$(dirname "$(dirname "$(readlink -f "$(command -v java)")")")"
        if [[ -d "$candidate" ]]; then
            printf '%s\n' "$candidate"
            return 0
        fi
    fi

    return 1
}

ADB_BIN="${ADB_BIN:-$(find_default_adb_bin || true)}"
JAVA_HOME="${JAVA_HOME:-$(find_default_java_home || true)}"
DEFAULT_GRADLE_USER_HOME="${HOME:-/home/ariy}/.gradle"
GRADLE_USER_HOME="${GRADLE_USER_HOME:-$DEFAULT_GRADLE_USER_HOME}"
SERIAL="${ADB_SERIAL:-}"
POLL_SECONDS="${POLL_SECONDS:-2}"
DEFAULT_VARIANT="release"
VARIANT="${BUILD_VARIANT:-$DEFAULT_VARIANT}"
FASTDEPLOY_MODE="${ADB_INSTALL_FASTDEPLOY:-auto}"
STREAMING_MODE="${ADB_INSTALL_STREAMING:-auto}"
ONCE=0
APP_ID="com.ariy.retrolauncher"
ADB_UNREACHABLE_EXIT=10
BUILD_FAILED_EXIT=11

usage() {
    cat <<'EOF'
Usage: scripts/deploy-on-change.sh [options]

Rebuilds and installs Retro Launcher whenever watched files change.

Options:
  --serial SERIAL    Target device serial. Defaults to ADB_SERIAL or a single connected device.
  --variant NAME     Build variant to deploy. Defaults to release.
  --fastdeploy       Enable adb fast deploy. Best with USB + debug builds.
  --no-fastdeploy    Disable adb fast deploy explicitly.
  --streaming        Force streaming install mode.
  --no-streaming     Force push-then-install mode.
  --poll SECONDS     Poll interval when waiting for changes. Defaults to 2.
  --once             Run a single build/install cycle and exit.
  -h, --help         Show this help text.

Environment overrides:
  ADB_BIN            adb binary path.
  ADB_SERIAL         Default device serial.
  JAVA_HOME          JDK path for Gradle. Defaults to an auto-detected Android Studio/runtime JDK.
  GRADLE_USER_HOME   Gradle cache location. Defaults to ~/.gradle.
  POLL_SECONDS       Default poll interval.
  BUILD_VARIANT      Default build variant.
  ADB_INSTALL_FASTDEPLOY  Install mode: auto, true, or false.
  ADB_INSTALL_STREAMING   Install transport: auto, streaming, or no-streaming.
EOF
}

log() {
    printf '[%s] %s\n' "$(date '+%H:%M:%S')" "$*"
}

die() {
    log "ERROR: $*"
    exit 1
}

require_command() {
    command -v "$1" >/dev/null 2>&1 || die "Missing required command: $1"
}

while [[ $# -gt 0 ]]; do
    case "$1" in
        --serial)
            [[ $# -ge 2 ]] || die "--serial requires a value"
            SERIAL="$2"
            shift 2
            ;;
        --variant)
            [[ $# -ge 2 ]] || die "--variant requires a value"
            VARIANT="$2"
            shift 2
            ;;
        --fastdeploy)
            FASTDEPLOY_MODE="true"
            shift
            ;;
        --no-fastdeploy)
            FASTDEPLOY_MODE="false"
            shift
            ;;
        --streaming)
            STREAMING_MODE="streaming"
            shift
            ;;
        --no-streaming)
            STREAMING_MODE="no-streaming"
            shift
            ;;
        --poll)
            [[ $# -ge 2 ]] || die "--poll requires a value"
            POLL_SECONDS="$2"
            shift 2
            ;;
        --once)
            ONCE=1
            shift
            ;;
        -h|--help)
            usage
            exit 0
            ;;
        *)
            die "Unknown argument: $1"
            ;;
    esac
done

[[ -n "$ADB_BIN" ]] || die "Unable to locate adb. Set ADB_BIN or install Android platform-tools."
if [[ "$ADB_BIN" == */* ]]; then
    [[ -x "$ADB_BIN" ]] || die "ADB binary is not executable: $ADB_BIN"
else
    require_command "$ADB_BIN"
fi
require_command sha256sum
require_command find
require_command sort
require_command date

[[ -x "$GRADLEW" ]] || die "Gradle wrapper is not executable: $GRADLEW"
[[ -n "$JAVA_HOME" ]] || die "Unable to locate a JDK. Set JAVA_HOME to Android Studio's bundled JBR or a Java 17+ install."
[[ -d "$JAVA_HOME" ]] || die "JAVA_HOME does not exist: $JAVA_HOME"
[[ "$POLL_SECONDS" =~ ^[0-9]+([.][0-9]+)?$ ]] || die "Poll interval must be numeric"
case "$FASTDEPLOY_MODE" in
    auto|true|false) ;;
    *) die "ADB_INSTALL_FASTDEPLOY must be auto, true, or false" ;;
esac
case "$STREAMING_MODE" in
    auto|streaming|no-streaming) ;;
    *) die "ADB_INSTALL_STREAMING must be auto, streaming, or no-streaming" ;;
esac

VARIANT_LOWER="$(printf '%s' "$VARIANT" | tr '[:upper:]' '[:lower:]')"
VARIANT_CAPITALIZED="${VARIANT_LOWER^}"
ASSEMBLE_TASK="assemble${VARIANT_CAPITALIZED}"
APK_PATH="$ROOT_DIR/app/build/outputs/apk/$VARIANT_LOWER/app-$VARIANT_LOWER.apk"

WATCH_CANDIDATES=(
    "$ROOT_DIR/app/src"
    "$ROOT_DIR/backgrounds"
    "$ROOT_DIR/overlay"
    "$ROOT_DIR/overlay_mame"
    "$ROOT_DIR/app/build.gradle.kts"
    "$ROOT_DIR/app/proguard-rules.pro"
    "$ROOT_DIR/embedded-rpcsx/build.gradle.kts"
    "$ROOT_DIR/vendor/rpcsx-ui-android/app/src/main"
    "$ROOT_DIR/build.gradle.kts"
    "$ROOT_DIR/settings.gradle.kts"
    "$ROOT_DIR/gradle.properties"
)

WATCH_PATHS=()
for path in "${WATCH_CANDIDATES[@]}"; do
    if [[ -e "$path" ]]; then
        WATCH_PATHS+=("$path")
    fi
done

[[ ${#WATCH_PATHS[@]} -gt 0 ]] || die "No watch paths found"

resolve_serial() {
    if [[ -n "$SERIAL" ]]; then
        return 0
    fi

    local usb_serial
    usb_serial="$("$ADB_BIN" -d get-serialno 2>/dev/null || true)"
    if [[ -n "$usb_serial" && "$usb_serial" != "unknown" ]]; then
        SERIAL="$usb_serial"
        return 0
    fi

    mapfile -t devices < <("$ADB_BIN" devices | awk 'NR > 1 && $2 == "device" { print $1 }')
    if [[ ${#devices[@]} -eq 1 ]]; then
        SERIAL="${devices[0]}"
        return 0
    fi

    if [[ ${#devices[@]} -eq 0 ]]; then
        return "$ADB_UNREACHABLE_EXIT"
    fi

    die "Multiple connected devices found. Pass --serial or set ADB_SERIAL."
}

build_install_command() {
    INSTALL_COMMAND=("$ADB_BIN" -s "$SERIAL" install -r)

    case "$FASTDEPLOY_MODE" in
        auto)
            if [[ "$VARIANT_LOWER" == "debug" ]]; then
                INSTALL_COMMAND+=(--fastdeploy)
            fi
            ;;
        true)
            INSTALL_COMMAND+=(--fastdeploy)
            ;;
        false)
            INSTALL_COMMAND+=(--no-fastdeploy)
            ;;
    esac

    case "$STREAMING_MODE" in
        streaming)
            INSTALL_COMMAND+=(--streaming)
            ;;
        no-streaming)
            INSTALL_COMMAND+=(--no-streaming)
            ;;
    esac

    INSTALL_COMMAND+=("$APK_PATH")
}

install_mode_summary() {
    local fastdeploy_summary="$FASTDEPLOY_MODE"
    local streaming_summary="$STREAMING_MODE"
    if [[ "$FASTDEPLOY_MODE" == "auto" && "$VARIANT_LOWER" == "debug" ]]; then
        fastdeploy_summary="auto(debug=>fastdeploy)"
    fi
    printf 'fastdeploy=%s, streaming=%s' "$fastdeploy_summary" "$streaming_summary"
}

ensure_device() {
    resolve_serial

    if [[ "$SERIAL" == *:* ]]; then
        "$ADB_BIN" connect "$SERIAL" >/dev/null 2>&1 || true
    fi

    "$ADB_BIN" -s "$SERIAL" get-state >/dev/null 2>&1 || return "$ADB_UNREACHABLE_EXIT"
}

fingerprint_watch_paths() {
    find "${WATCH_PATHS[@]}" \
        \( -path '*/build/*' -o -path '*/.gradle/*' -o -path '*/.cxx/*' \) -prune -o \
        -type f -printf '%p\t%T@\t%s\n' \
        | LC_ALL=C sort \
        | sha256sum \
        | awk '{print $1}'
}

deploy() {
    ensure_device || return $?

    log "Building $ASSEMBLE_TASK"
    if ! (
        cd "$ROOT_DIR"
        export JAVA_HOME
        export PATH="$JAVA_HOME/bin:$PATH"
        export GRADLE_USER_HOME
        "$GRADLEW" "$ASSEMBLE_TASK"
    ); then
        log "Build failed; keeping the last APK installed"
        return "$BUILD_FAILED_EXIT"
    fi

    [[ -f "$APK_PATH" ]] || die "APK not found after build: $APK_PATH"

    build_install_command
    log "Installing $(basename "$APK_PATH") to $SERIAL"
    log "Install options: $(install_mode_summary)"
    install_output="$("${INSTALL_COMMAND[@]}" 2>&1)" || {
        printf '%s\n' "$install_output"
        if [[ "$install_output" == *"INSTALL_FAILED_UPDATE_INCOMPATIBLE"* ]]; then
            if [[ "$VARIANT_LOWER" == "debug" && -f "$ROOT_DIR/release-keystore.properties" ]]; then
                die "Installed package uses a different signing key. Retry with --variant release or uninstall $APP_ID first."
            fi
            die "Installed package uses a different signing key. Uninstall $APP_ID from the device before deploying this APK."
        fi
        if [[ "$install_output" == *"INSTALL_FAILED_INVALID_APK"* || "$install_output" == *"code is missing"* ]]; then
            log "Package install looks corrupted; attempting data-preserving recovery with uninstall -k"
            recovery_output="$("$ADB_BIN" -s "$SERIAL" uninstall -k "$APP_ID" 2>&1)" || {
                printf '%s\n' "$recovery_output"
                die "Data-preserving recovery failed. Avoid a plain uninstall if you need to keep launcher data."
            }
            printf '%s\n' "$recovery_output"

            build_install_command
            install_output="$("${INSTALL_COMMAND[@]}" 2>&1)" || {
                printf '%s\n' "$install_output"
                if [[ "$install_output" == *"device offline"* || "$install_output" == *"no devices/emulators found"* ]]; then
                    return "$ADB_UNREACHABLE_EXIT"
                fi
                die "Install failed after data-preserving recovery"
            }
            printf '%s\n' "$install_output"
            return 0
        fi
        if [[ "$install_output" == *"device offline"* || "$install_output" == *"no devices/emulators found"* ]]; then
            return "$ADB_UNREACHABLE_EXIT"
        fi
        die "Install failed"
    }
    printf '%s\n' "$install_output"
}

trap 'log "Stopping deploy watcher"; exit 0' INT TERM

log "Watching for changes"
log "Device: ${SERIAL:-auto-detect}"
log "Variant: $VARIANT_LOWER"
log "Poll interval: ${POLL_SECONDS}s"
log "Watching source art in: $ROOT_DIR/backgrounds, $ROOT_DIR/overlay, and $ROOT_DIR/overlay_mame"

last_fingerprint=""

while true; do
    current_fingerprint="$(fingerprint_watch_paths)"
    if [[ "$current_fingerprint" != "$last_fingerprint" ]]; then
        if deploy; then
            last_fingerprint="$current_fingerprint"
            if [[ "$ONCE" -eq 1 ]]; then
                exit 0
            fi
            log "Deploy complete; waiting for the next change"
        else
            deploy_status=$?
            if [[ "$deploy_status" -eq "$ADB_UNREACHABLE_EXIT" ]]; then
                if [[ "$ONCE" -eq 1 ]]; then
                    exit "$deploy_status"
                fi
                log "ADB device is not reachable yet: ${SERIAL:-auto-detect}; will retry the same change"
            elif [[ "$deploy_status" -eq "$BUILD_FAILED_EXIT" ]]; then
                last_fingerprint="$current_fingerprint"
                if [[ "$ONCE" -eq 1 ]]; then
                    exit "$deploy_status"
                fi
                log "Build failed; waiting for the next change"
            else
                exit "$deploy_status"
            fi
        fi
    fi

    sleep "$POLL_SECONDS"
done
