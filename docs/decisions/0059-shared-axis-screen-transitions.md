# 0059: Shared axis transitions between screens

Status: accepted (2026-11-25), supersedes the default `NavDisplay` transitions of 0044

## Context
With the default transitions, the back swipe shrinks the screen into the center and removes it. The user disliked it.
A full slide and a shared axis (short slide and fade) were both tried on the API 33 emulator.

## Decision
Material shared axis on the X axis for opening a screen, the back arrow and the back swipe: each screen moves a tenth
of the width over 300 ms; the old one fades out in 90 ms, then the new one fades in over 210 ms. The direction is the
same from either swipe edge. The rest of 0044 stands.

## Consequences
- Matches the motion rule in `docs/design/native-look-and-feel.md`.
- Not tested on Android 14+, where the transition should follow the finger during the back swipe.
- The tab switch (0051) and the system's own back-to-home animation are unchanged.
