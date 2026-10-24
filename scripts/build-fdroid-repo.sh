#!/usr/bin/env bash
# Baut aus signierten YuneBridge-Release-APKs ein eigenes F-Droid-Repo (Index signiert mit dem App-Keystore).
#
# Nutzung: scripts/build-fdroid-repo.sh <apk-verzeichnis> <ausgabe-verzeichnis>
#
# Benötigt fdroidserver (pip install fdroidserver), Java (keytool/jarsigner) und
# Umgebungsvariablen: REPO_URL, KEYSTORE_FILE, KEYSTORE_PASSWORD, KEY_ALIAS, KEY_PASSWORD.
# Optional: ANDROID_HOME (für apksigner; sonst nutzt fdroidserver seine Fallbacks).
set -euo pipefail

APK_DIR=$(realpath "$1")
OUT=$(realpath -m "$2")
ROOT=$(cd "$(dirname "$0")/.." && pwd)
APP_ID=io.github.quompacc.yunebridge
: "${REPO_URL:?}" "${KEYSTORE_FILE:?}" "${KEYSTORE_PASSWORD:?}" "${KEY_ALIAS:?}" "${KEY_PASSWORD:?}"

WORK=$(mktemp -d)
trap 'rm -rf "$WORK"' EXIT
mkdir -p "$WORK/repo" "$WORK/metadata/$APP_ID"

cp "$APK_DIR"/*.apk "$WORK/repo/"
# repo_icon (Standard: icon.png) wird relativ zum Arbeitsverzeichnis gesucht
cp "$ROOT/fastlane/metadata/android/en-US/images/icon.png" "$WORK/icon.png"
# Fastlane-Texte, Screenshots und Changelogs übernimmt fdroidserver aus metadata/<appid>/<locale>/
cp -r "$ROOT/fastlane/metadata/android/." "$WORK/metadata/$APP_ID/"

cat > "$WORK/metadata/$APP_ID.yml" <<'EOF'
Categories:
  - Connectivity
  - Sports & Health
License: AGPL-3.0-or-later
AuthorName: quompacc
SourceCode: https://github.com/quompacc/yunebridge
IssueTracker: https://github.com/quompacc/yunebridge/issues
Changelog: https://github.com/quompacc/yunebridge/releases
EOF

cat > "$WORK/config.yml" <<EOF
repo_url: $REPO_URL
repo_name: YuneBridge
repo_description: >-
  Official repository of YuneBridge, a Gadgetbridge fork with a redesigned interface.
  Source code: https://github.com/quompacc/yunebridge
archive_older: 0
keystore: $(realpath "$KEYSTORE_FILE")
repo_keyalias: $KEY_ALIAS
keystorepass: $KEYSTORE_PASSWORD
keypass: $KEY_PASSWORD
EOF
[ -n "${ANDROID_HOME:-}" ] && echo "sdk_path: $ANDROID_HOME" >> "$WORK/config.yml"
chmod 600 "$WORK/config.yml"

(cd "$WORK" && fdroid update --use-date-from-apk)

rm -rf "$OUT"
mkdir -p "$OUT"
cp -r "$WORK/repo" "$OUT/"
echo "F-Droid-Repo erstellt: $OUT/repo"
