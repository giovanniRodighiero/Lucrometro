#!/usr/bin/env bash
# Release Lucrometro in one command (decision 0021).
#
#   scripts/release.sh 0.2             # full release
#   scripts/release.sh 0.2 --dry-run   # checks and build only; changes nothing
#
# Before running, you write the "## [0.2]" section in CHANGELOG.md and commit it on main.
# The script then bumps versionName/versionCode, builds and verifies the signed APK, commits the bump,
# tags vX, pushes, copies the APK to $RELEASE_OUT_DIR (default ~/Downloads), and creates the GitHub Release.
set -euo pipefail

cd "$(dirname "$0")/.."
GRADLE_FILE=app/build.gradle.kts
export ANDROID_HOME="${ANDROID_HOME:-$HOME/Library/Android/sdk}"
export PATH="$ANDROID_HOME/platform-tools:$PATH"
OUT_DIR="${RELEASE_OUT_DIR:-$HOME/Downloads}"

VERSION="${1:-}"
DRY_RUN=0
if [ "${2:-}" = "--dry-run" ]; then DRY_RUN=1; fi

die() { echo "release: $*" >&2; exit 1; }
step() { echo; echo "==> $*"; }

# On failure before the bump is committed, put the version back so the working tree is left as it was.
BUMPED=0
COMMITTED=0
NOTES=""
cleanup() {
  status=$?
  if [ "$status" -ne 0 ] && [ "$BUMPED" = 1 ] && [ "$COMMITTED" = 0 ]; then
    git checkout -- "$GRADLE_FILE"
    echo "release: failed; restored $GRADLE_FILE" >&2
  fi
  rm -f "$NOTES"
}
trap cleanup EXIT

[ -n "$VERSION" ] || die "usage: scripts/release.sh <versionName> [--dry-run]"
[[ "$VERSION" =~ ^[0-9]+\.[0-9]+(\.[0-9]+)?$ ]] || die "version must look like 0.2 or 0.2.1"
TAG="v$VERSION"
APK_OUT="$OUT_DIR/lucrometro-$VERSION.apk"
APK_BUILT="app/build/outputs/apk/release/app-release.apk"

step "Checking preconditions"
[ "$(git branch --show-current)" = "main" ] || die "must be on main"
[ -z "$(git status --porcelain)" ] || die "working tree is not clean: commit or stash first"
git fetch -q origin
[ "$(git rev-parse HEAD)" = "$(git rev-parse origin/main)" ] || die "main is not in sync with origin/main"
if git rev-parse -q --verify "refs/tags/$TAG" >/dev/null; then die "tag $TAG already exists"; fi
if ! grep -q "^## \[$VERSION\]" CHANGELOG.md; then
  LAST_TAG=$(git describe --tags --abbrev=0 2>/dev/null || true)
  echo "Commits since ${LAST_TAG:-the beginning} (use these to write the changelog):" >&2
  git log --oneline "${LAST_TAG:+$LAST_TAG..}HEAD" >&2
  die "CHANGELOG.md has no section '## [$VERSION]'. Write it, commit it on main, then run again"
fi
grep -q "^lucrometro.releaseStoreFile=" "$HOME/.gradle/gradle.properties" 2>/dev/null \
  || die "release signing is not configured in ~/.gradle/gradle.properties (decision 0020)"
if [ "$DRY_RUN" = 0 ]; then
  gh auth status >/dev/null 2>&1 || die "gh is not logged in"
fi
echo "ok"

step "Bumping version to $VERSION"
CURRENT_CODE=$(sed -n 's/^ *versionCode = \([0-9]*\)$/\1/p' "$GRADLE_FILE")
[ -n "$CURRENT_CODE" ] || die "could not read versionCode from $GRADLE_FILE"
NEW_CODE=$((CURRENT_CODE + 1))
sed -i '' -e "s/versionCode = $CURRENT_CODE\$/versionCode = $NEW_CODE/" \
          -e "s/versionName = \"[^\"]*\"/versionName = \"$VERSION\"/" "$GRADLE_FILE"
BUMPED=1
echo "versionName $VERSION, versionCode $CURRENT_CODE -> $NEW_CODE"

step "Building the signed release APK"
./gradlew assembleRelease
BT=$(ls -d "$ANDROID_HOME"/build-tools/* | sort -V | tail -1)
"$BT/apksigner" verify "$APK_BUILT" >/dev/null || die "signature check failed"
BADGING=$("$BT/aapt2" dump badging "$APK_BUILT")
grep -q "versionName='$VERSION'" <<< "$BADGING" || die "APK versionName is not $VERSION"
grep -q "versionCode='$NEW_CODE'" <<< "$BADGING" || die "APK versionCode is not $NEW_CODE"
echo "APK signed and version checked: $VERSION ($NEW_CODE)"

if [ "$DRY_RUN" = 1 ]; then
  git checkout -- "$GRADLE_FILE"
  step "Dry run finished. Nothing was committed, tagged, pushed or published."
  echo "A real run would copy the APK to $APK_OUT and create GitHub release $TAG."
  exit 0
fi

step "Committing, tagging and pushing"
git add "$GRADLE_FILE"
git commit -q -m "Release $VERSION" -m "Bump versionName to $VERSION and versionCode to $NEW_CODE.

Co-Authored-By: Claude Haiku 5.5 <noreply@anthropic.com>"
COMMITTED=1
git tag -a "$TAG" -m "Lucrometro $VERSION"
git push -q origin main
git push -q origin "$TAG"

step "Copying the APK and creating the GitHub release"
cp "$APK_BUILT" "$APK_OUT"
NOTES=$(mktemp)
awk -v h="## [$VERSION]" 'index($0, h) == 1 { f = 1; next } /^## / { f = 0 } f' CHANGELOG.md \
  | sed '/./,$!d' > "$NOTES"
gh release create "$TAG" "$APK_OUT" --title "Lucrometro $VERSION" --notes-file "$NOTES" --verify-tag

step "Released $TAG"
echo "APK for your phone: $APK_OUT"
gh release view "$TAG" --json url --jq .url
