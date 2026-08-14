#!/usr/bin/env bash

set -euo pipefail

readonly APP_PACKAGE="dev.sunls24.sbv"
readonly APPSTORE_PACKAGE="com.tcl.appmarket2"
readonly APPSTORE_RECEIVER="com.tcl.appmarket2/com.huan.appstore.receiver.ThirdAppManagerReceiver"
readonly REMOTE_APK="/sdcard/Download/SBV_release.apk"
readonly PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
readonly VERSION_FILE="$PROJECT_ROOT/buildSrc/src/main/kotlin/AppConfiguration.kt"
readonly APK_OUTPUT_DIR="$PROJECT_ROOT/app/build/outputs/apk/release"
readonly APK_METADATA_FILE="$APK_OUTPUT_DIR/output-metadata.json"
readonly INSTALL_TIMEOUT_SECONDS=60
readonly INSTALL_POLL_INTERVAL_SECONDS=2

build_apk=true
if [[ $# -eq 1 && "$1" == "-s" ]]; then
    build_apk=false
elif [[ $# -ne 0 ]]; then
    echo "用法：$0 [-s]" >&2
    exit 1
fi

cd "$PROJECT_ROOT"

adb get-state >/dev/null

if [[ "$build_apk" == true ]]; then
    current_version_patch="$(sed -n 's/^[[:space:]]*private const val versionPatch = \([0-9][0-9]*\)[[:space:]]*$/\1/p' "$VERSION_FILE")"
    if [[ ! "$current_version_patch" =~ ^[0-9]+$ ]]; then
        echo "无法唯一定位 versionPatch：$VERSION_FILE" >&2
        exit 1
    fi

    next_version_patch=$((current_version_patch + 1))
    sed -i '' -E "s/(^[[:space:]]*private const val versionPatch = )[0-9]+([[:space:]]*$)/\1${next_version_patch}\2/" "$VERSION_FILE"
    echo "版本号递增：versionPatch $current_version_patch -> $next_version_patch"

    echo "构建 release APK..."
    ./gradlew :app:assembleRelease
else
    echo "跳过构建，复用已有 release APK..."
fi

if [[ ! -f "$APK_METADATA_FILE" ]]; then
    echo "未找到 release APK 元数据：$APK_METADATA_FILE" >&2
    exit 1
fi

apk_name="$(sed -n 's/.*"outputFile": "\([^"]*\.apk\)".*/\1/p' "$APK_METADATA_FILE" | head -n 1)"
expected_version="$(sed -n 's/.*"versionName": "\([^"]*\)".*/\1/p' "$APK_METADATA_FILE" | head -n 1)"
if [[ -z "$apk_name" || -z "$expected_version" || ! -f "$APK_OUTPUT_DIR/$apk_name" ]]; then
    echo "未找到有效的 release APK：${apk_name:-未记录文件名}" >&2
    exit 1
fi
apk_path="$APK_OUTPUT_DIR/$apk_name"

echo "使用 APK: $apk_path"

# 仅在应用商店原本为 disabled-user 时，安装完成后恢复该状态。
market_was_disabled_user=false
restore_market() {
    if [[ "$market_was_disabled_user" == true ]]; then
        echo "恢复 TCL 应用商店禁用状态..."
        adb shell pm disable-user --user 0 "$APPSTORE_PACKAGE" >/dev/null
    fi
}
trap restore_market EXIT

market_dump="$(adb shell dumpsys package "$APPSTORE_PACKAGE" | tr -d '\r')"
if printf '%s\n' "$market_dump" | grep -q 'User 0:.*enabled=3'; then
    market_was_disabled_user=true
    adb shell pm enable "$APPSTORE_PACKAGE" >/dev/null
fi

echo "推送 APK..."
adb push "$apk_path" "$REMOTE_APK"

echo "通过 TCL 应用商店安装..."
adb shell am broadcast \
    -a android.intent.action.APPSTORE_INSTALL_APK \
    -n "$APPSTORE_RECEIVER" \
    --es Fileurl "$REMOTE_APK" \
    --es PackageName "$APP_PACKAGE" \
    --es Name SBV

get_installed_version() {
    adb shell dumpsys package "$APP_PACKAGE" 2>/dev/null |
        tr -d '\r' |
        sed -n 's/.*versionName=\([^ ]*\).*/\1/p' |
        head -n 1 || true
}

echo "等待安装并验证版本..."
deadline=$((SECONDS + INSTALL_TIMEOUT_SECONDS))
installed_version=""
while (( SECONDS < deadline )); do
    installed_version="$(get_installed_version)"
    if [[ "$installed_version" == "$expected_version" ]]; then
        break
    fi
    sleep "$INSTALL_POLL_INTERVAL_SECONDS"
done

if [[ "$installed_version" != "$expected_version" ]]; then
    echo "安装验证失败：期望版本 ${expected_version}，实际版本 ${installed_version:-未找到}。" >&2
    exit 1
fi

echo "更新完成：$APP_PACKAGE versionName=$installed_version"
