# 0046: No Android backup

Status: accepted (2026-11-23)

## Context
The manifest did not set `android:allowBackup`, so Android's Auto Backup was on by default: the app's database
(watchlist, quotes, average prices and the private trade log of decision 0040) was uploaded to the owner's Google
account. That contradicts the spec's principle "All local: everything stays on the phone", and nobody had chosen it.

## Decision
`android:allowBackup="false"`: no cloud backup and no device-to-device transfer of the app's data.

## Consequences
- The trade log never leaves the phone.
- A new or reset phone starts empty. The watchlist is retyped, and the trades are imported again from the broker's CSV
  (decision 0041); ids already saved are skipped, so importing the same files again is safe.
- If a backup is wanted later, it should be an explicit export the owner triggers, not a silent upload.
