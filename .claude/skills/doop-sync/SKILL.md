---
name: doop-sync
description: Export the Lucrometro Doop canvas (tokens, style guides, frames) into docs/design/doop/, or port one snapshotted frame to Jetpack Compose. Invoke manually only, with `export` or `port <frame>`.
disable-model-invocation: true
model: sonnet
effort: medium
argument-hint: export | port <frame-slug>
---

# Doop sync

Two modes, chosen by the first argument:

- `export`: snapshot the Doop canvas into the repo, so porting never needs Doop running.
- `port <frame-slug>`: write the Compose code for one snapshotted frame.

The repo is the source for code. Doop is only read during `export`. Rules for the native UI are in
`docs/design/native-look-and-feel.md`; decision 0009 sets the package layout.

## Doop setup (both modes)

Doop tools are deferred. Load them first with ToolSearch, selecting `mcp__doop__get_guide`,
`get_canvas`, `list_canvases`, `get_tokens`, `list_guidelines`, `get_guidelines`, `list_components`,
`get_component`, `get_frame`, `export_frame` and `get_frame_screenshot`.

Before any other Doop call, run `get_guide` with topic `doop-instructions`, as Doop requires. Use
the same `agent_name` ("Claude") on every call.

Never approve frames or set status. Doop's `ready_for_dev` is human sign-off.

## export

1. Find the canvas: `list_canvases`, then use the one named "Lucrometro". If there is more than one
   match, ask the user which one.
2. Write `docs/design/doop/tokens.json` from `get_tokens` in `json` format. Keep the per-theme
   values as Doop returns them: light, dark and any other theme.
3. For each guideline from `list_guidelines`, write `docs/design/doop/guidelines/<name>.md` with the
   text from `get_guidelines`.
4. For each component from `list_components`, save `get_component` output to
   `docs/design/doop/components/<name>.html`.
5. For each frame on the canvas, derive a slug (lowercase, hyphens, e.g. `watchlist`). Save:
   - `frames/<slug>.html` from `get_frame`. This is the exact layout and value source for porting.
   - `frames/<slug>.png` from `export_frame` at scale 1. The tool returns a URL, so download it
     right away with `curl`, since the URL re-renders when the frame changes.
6. Write `docs/design/doop/SNAPSHOT.md`: the canvas name and ID, the export date, and a table of
   slug, frame name and frame ID. Anything that is not in this file was not exported.

Re-running `export` overwrites the snapshot. Show the user `git diff --stat docs/design/doop` before
they commit, so changes are visible.

## port <frame-slug>

Work only from the snapshot. Do not call Doop in this mode.

1. Read `docs/design/doop/SNAPSHOT.md`, `frames/<slug>.html`, `tokens.json`, the relevant
   guidelines, and `docs/design/native-look-and-feel.md`. Read `docs/design/components.md` if it
   exists.
2. Theme (`ui/theme/`): create `Color.kt`, `Type.kt`, `Shape.kt` and `Theme.kt` from the tokens if
   they do not exist, or update only the values if they do. Map colour tokens to `colorScheme`
   slots and type tokens to `typography`. Gain and loss are semantic tokens: `gain`, `loss`,
   `gainContainer`, `onGainContainer`, `lossContainer`, `onLossContainer` (decision 0007). Keep
   light, dark and dynamic colour working.
3. Screen (`ui/<screen>/<Screen>.kt`): use Material 3 components. Doop components map to composables
   in `docs/design/components.md`. Create that file if it is missing, with a row for each mapping
   used. Do not hard-code colours or text sizes. Put user-visible text in
   `res/values/strings.xml`. The screen talks to data only through its ViewModel.
4. Conflicts: when the frame and the platform disagree, the frame wins on visual intent. The repo
   wins where Android cannot do it. Record each deviation under the screen's heading in
   `docs/design/components.md`.
5. Verify, as CLAUDE.md requires: `./gradlew assembleDebug`, install on the emulator, take a
   screenshot and compare it with `frames/<slug>.png`. If the JDK or emulator is not available,
   say so plainly and do not report the port as done. Run the build yourself; do not leave it to
   the user.
6. Do not commit unless the user asks.
