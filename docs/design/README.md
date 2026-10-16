# Špijun — Design handoff (iOS + Android, Kotlin Multiplatform / Compose Multiplatform)

Exported 2026-10-16 from the claude.ai Design canvas **"Špijun — Design"** (`https://claude.ai/artifact/XtN3ran4nCfCovxyvT7gUa`, version `1792107143-608d`). The original brief is `../claude-design-prompt.md`. Everything an engineer needs is in this folder; the canvas is not required.

**Concept:** *Mirni stol, glasne karte* — a calm table, loud cards. Dark-first, warm neutrals, vermilion means spy, cut-corner role cards on a round table that turns from player to player.

## Contents

| Path | What |
|---|---|
| [`decisions.md`](decisions.md) | **Start here.** Every design decision (game rules, answered open questions, navigation, multi-device behaviour, copy rules), changes from the brief, the Ideas frame, and **open issues that still need a decision**. |
| [`tokens.json`](tokens.json) | All design tokens (W3C DTCG format): colour (neutral ramp, dark + light semantic roles, card faces, fills, M3 tonal palettes + scheme mapping, contrast overrides), typography, spacing, radius, elevation, motion (durations, easings, springs, idle), haptics map, sound, layout. |
| [`compose/SpyTheme.kt`](compose/SpyTheme.kt) | Reference Compose theme generated from the tokens (`SpyColors`, `SpyType`, `SpyShape`, `SpySpace`, `SpyMotion`, `SpyHaptics`, `SpyTheme`). |
| [`components.md`](components.md) | Every component with sizes, states, API sketch and a11y — each labelled **Shared (Compose)** / **Native iOS (SwiftUI)** / **Native Android (Material)**. |
| [`screens.md`](screens.md) | Every P1–P3 screen and state with layout specs, annotations, copy keys and the 4 renders (iOS/Android × dark/light). Includes global layout rules and the navigation map. |
| [`screens/`](screens/) | 236 PNGs @2x: `screens/<tier>-<nn>-<screen>/<state>__<platform>-<theme>.png`. |
| [`motion.md`](motion.md) | Reveal choreography (chosen: tap-to-flip on the table), discussion tension, elimination, timer, game over, haptics map, sound, **reduced-motion and reduced-transparency fallbacks**, privacy-as-motion rules. |
| [`copy.json`](copy.json) | 275 strings + 12 plural sets, HR + EN (Croatian one/few/other). Plus 38 strings + 4 plurals **proposed** for text visible in mocks without a key, a copy **review** list, and format rules. |
| [`copy/values/strings.xml`](copy/values/strings.xml), [`copy/values-hr/strings.xml`](copy/values-hr/strings.xml) | The same copy as Compose Multiplatform resources (`composeResources/values`, `values-hr`), ready to drop in. Proposed keys are marked with a comment. |
| [`assets/`](assets/) | App icon (iOS default/dark/tinted + Icon Composer layers; Android adaptive background/foreground/monochrome at all densities + Play Store 512), splash (iOS launch dark/light, Android SplashScreen icon + light/dark), brand mark, 5 game glyphs and 39 UI icons as SVG. |
| [`frames/`](frames/) | Every canvas board rendered as PNG (43 boards: Phase 1 directions, Phase 2 language, Phase 3 screens, Phase 4 handoff). Screen boards at @2x. |
| [`source/`](source/) | Raw source of every board (`*.dc.html`), the canvas index `canvas.json`, and the copy-table data (`assets/copy-table.*.json`, plus the earlier v1). |

## Board map

| Canvas page | Board (frames/ & source/) | Title |
|---|---|---|
| Phase 1 · Directions | `Main` (A iOS), `A-Android`, `B-iOS`, `B-Android`, `C-iOS`, `C-Android`, `D-iOS`, `D-Android` | A · Cenzura, B · Pomrčina, C · Karta, D · Klasik — spy-with-fellow-spy reveal |
| Phase 2 · Design language | `P2-01-Concept` … `P2-12-Storyboard` | 01 Concept · 02 Hybrid in practice · 03 Colour · 04 Typography · 05 Space/shape/elevation/icons · 06 Motion/haptics/sound · 07 Components table layer · 08 Components card & game layer · 09 Accessibility · 10 Platform mapping & tokens · 11 Table prototype · 12 Table choreography |
| Phase 3 · P1 screens | `P3-01-Home` … `P3-09-Dictionaries` | Home · Setup · Lobby & join · Role reveal · Round · Voting · Round result · Game over · Dictionaries |
| Phase 3 · P2 & P3 screens | `P4-10-ImportExport`, `P4-11-Settings`, `P4-12-Connection`, `P4-13-Icon`, `P4-14-Transient`, `P4-15-Ideas` | Import & export · Settings · Connection & edge states · App icon & splash · Transient UI · Ideas |
| Phase 4 · Handoff | `P5-01-Tokens` … `P5-08-CopyC` | Tokens → Compose · Component specs · Annotated reveal · Annotated setup · Motion & haptics implementation · Copy keys 1–3 |

## Quick facts for implementation

- **Frames:** iOS 390×844 pt, Android 412×915 dp. Units in all docs: px = pt = dp; font px = pt = sp.
- **Fonts:** Bricolage Grotesque (700/800, display) + Hanken Grotesk (400–700, text, tabular figures) — both OFL, Google Fonts; bundle as Compose resources for both platforms.
- **Native pieces on iOS** (SwiftUI interop): tab bar, navigation bar/toolbar, glass buttons, sheets, confirmation dialogs, share/file import, VisionKit scanner. **Native on Android:** NavigationBar, LargeTopAppBar, IconButton, ModalBottomSheet, AlertDialog, Sharesheet, CameraX + ML Kit. Everything else is Shared Compose.
- **Dynamic colour off.** Role colours are fixed brand meaning.
- **Haptics are meaningful** (spy = double thud, citizen = single) and stay on under Reduce Motion.

## How this export was made

The canvas boards were fetched from the artifact and rendered headlessly (Chromium) with the canvas runtime and the same Google Fonts the canvas uses; each phone frame inside a board was cropped to its own PNG. Interactive boards (Phase 1 heroes, 11 Table prototype) are rendered in their default state — the Table prototype is a live prototype in the canvas (press Play) and is documented step by step in `motion.md`. Tokens, components, motion and copy were transcribed from the boards' data and text; nothing was invented except where marked **proposed** / **derived** / **suggested**.
