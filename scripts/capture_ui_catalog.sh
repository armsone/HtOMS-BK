#!/bin/zsh
set -euo pipefail

project_root="${0:A:h:h}"
device_name="${CATALOG_DEVICE:-iPhone 17 Pro}"
output_root="${CATALOG_OUTPUT:-$project_root/artifacts/matchup}"
device_slug="$(print -r -- "$device_name" | tr '[:upper:] ' '[:lower:]_')"
screenshots_dir="$output_root/screenshots/$device_slug"
derived_data="$project_root/.build/MatchupCatalogDerivedData"
app_path="$derived_data/Build/Products/Debug-iphonesimulator/HtOMSBrief.app"
bundle_id="com.htoms.brief"

mkdir -p "$screenshots_dir"

xcodebuild \
  -project "$project_root/HtOMSBrief.xcodeproj" \
  -scheme HtOMSBrief \
  -destination "platform=iOS Simulator,name=$device_name,OS=26.5" \
  -derivedDataPath "$derived_data" \
  build >/dev/null

device_id="$(xcrun simctl list devices available | awk -v name="$device_name" 'index($0, name) && $0 !~ /unavailable/ {for (i=1; i<=NF; i++) if ($i ~ /^\([0-9A-F-]+\)$/) {gsub(/[()]/, "", $i); print $i; exit}}')"
if [[ -z "$device_id" ]]; then
  print -u2 "Simulator not found: $device_name"
  exit 1
fi

xcrun simctl boot "$device_id" 2>/dev/null || true
xcrun simctl bootstatus "$device_id" -b
xcrun simctl install "$device_id" "$app_path"
xcrun simctl ui "$device_id" appearance dark

capture() {
  local state_id="$1"
  shift
  xcrun simctl launch --terminate-running-process "$device_id" "$bundle_id" "$@" >/dev/null
  sleep 2
  xcrun simctl io "$device_id" screenshot --type=png "$screenshots_dir/${state_id}.png" >/dev/null
}

capture login_default -ui-catalog-login
capture brief_populated -ui-catalog-brief

for image in "$screenshots_dir/login_default.png" "$screenshots_dir/brief_populated.png"; do
  dimensions="$(sips -g pixelWidth -g pixelHeight "$image" 2>/dev/null | awk '/pixelWidth|pixelHeight/ {printf "%s%s", sep, $2; sep="x"}')"
  digest="$(shasum -a 256 "$image" | awk '{print $1}')"
  print "$(basename "$image"),$device_name,portrait,dark,$dimensions,$digest"
done | sort > "$output_root/manifest-$device_slug.csv"

print "$output_root"
