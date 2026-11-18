# 0021: Releases with one local script

Status: accepted (2026-11-18).

## Context
A release has several steps that are easy to forget or get wrong: bump the version, build the signed APK,
check it, commit the bump, tag, push, copy the APK, and create the GitHub release with the changelog notes.
The release keystore exists only on the development Mac (decision 0020), so the release must run there.

## Decision
- `scripts/release.sh <version> [--dry-run]` does the whole routine. It is the only supported way to release.
- It refuses to run unless: the branch is `main` and in sync with `origin/main`, the working tree is clean,
  the tag does not exist yet, `CHANGELOG.md` has a `## [<version>]` section, release signing is configured,
  and `gh` is logged in.
- The script does not write the changelog. Release notes are a human judgement about what users see, so the
  section must be written and committed first. When it is missing, the script prints the commits since the
  last tag to help.
- `versionCode` is increased by one automatically. `versionName` is the version argument.
- If the build or the version check fails before the bump is committed, the script restores
  `app/build.gradle.kts`.
- `--dry-run` runs the checks, the bump and the signed build, then restores the build file. It commits,
  tags, pushes and publishes nothing.
- A GitHub Action is not used for now. It would need the keystore stored as a repository secret. Revisit
  this when Obtainium is added (see the Obtainium discussion), because then releases need to be automated
  from any machine.

## Consequences
- A release is: write the changelog section, commit it on `main`, run `scripts/release.sh <version>`.
- Runs on macOS only (`sed -i ''`, the Android SDK under `~/Library/Android/sdk`).
- The guards and the failure restore were tested in a throwaway worktree. The publish steps (push, tag,
  GitHub release) have not run yet and will be first exercised by the next real release.
- If the script fails after pushing but before creating the GitHub release, the tag is already on GitHub;
  create the release by hand with `gh release create`.
