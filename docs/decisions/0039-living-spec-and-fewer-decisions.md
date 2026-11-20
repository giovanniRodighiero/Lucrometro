# 0039: A living spec for the current state; decisions are the archive

Status: accepted (2026-11-20)

## Context
After 0.3 there are 38 decision records, about 12,000 words. Many refine earlier ones (0035 moves what 0023 and 0032
set, 0038 changes 0035), so learning how the app works today meant reading them in order and working out which
parts still hold. That cost grows with every feature. Deleting the old records was considered and rejected: 28
source files and most commit messages cite them by number, and they are the record of why each choice was made.

## Decision
- `docs/spec.md` describes how the app works now, one section per area, citing decisions by number. Agents read it
  before the decisions. `docs/prd.md` stays as the v1 starting point and is not updated.
- Decision records are kept unchanged as the archive of the reasoning. Open one only when the spec's short version is
  not enough.
- At each release, before writing the changelog section, update the spec with that release's decisions and set
  its "Up to date with" line to the version.
- New decisions only for product, design, architecture or process choices. Review fixes and choices made while
  building go in the commit message, unless a later change needs their reasoning (as 0036 and 0037 did).

## Consequences
- Between releases the spec can lag the newest decisions; those are the records numbered above its "Up to date
  with" line.
- One more document to keep in step at each release. `scripts/release.sh` refuses to run unless the spec's
  "Up to date with" line names the version being released.
