# Prompt: build Špijun v2 (iOS + Android, Kotlin Multiplatform)

Copy everything below the line into a new coding-agent session started in this repository.

---

You are the lead engineer building **Špijun v2**: native iOS and Android apps for the social party game Špijun ("Spy"), written in **Kotlin Multiplatform + Compose Multiplatform**, plus the small Cloudflare backend they need. The design is finished. Your job is to implement it faithfully, one reviewable piece at a time.

## Repository and branch rules (read first)

- Repo: `github.com/Doda94/Spy`. **`v2` is the main line for this project** and the GitHub default branch.
- **`main` is frozen.** It holds the old Next.js web app, which Cloudflare Pages deploys from `main` to production. Never commit to, merge into, rebase or push `main`. Never deploy over its Pages project, and never write to its D1 database (`spy-words`).
- Workflow: create a feature branch from the latest `v2` (`v2/<short-topic>`), commit there, open a PR with **base `v2`**, and merge into `v2`. Keep PRs small: one milestone or sub-milestone each.
- Commit messages follow the repo's existing style: **Croatian, imperative, short subject** (e.g. "Dodaj tablicu za otkrivanje uloga"), with a body explaining why when it isn't obvious.
- Update the root `README.md` on `v2` as the project takes shape (how to build, run and test each part). Croatian is fine, matching the current README.

## The design: your spec

Everything is in `docs/design/`. Read it before writing code, in this order:

1. `docs/design/README.md`: index and quick facts.
2. `docs/design/decisions.md`: game rules, navigation, multi-device behaviour and copy rules. **§12 lists open issues that need the product owner's answer.** Don't silently pick an answer for those. Ask, or implement behind an obvious single constant and say so in the PR.
3. `docs/design/tokens.json` and `docs/design/compose/SpyTheme.kt`: the token set and a reference Compose theme. Use the theme as your starting point, and keep `tokens.json` as the source of truth.
4. `docs/design/components.md`: every component and who builds it: **Shared (Compose)**, **Native iOS (SwiftUI)** or **Native Android (Material)**.
5. `docs/design/screens.md` and `docs/design/screens/`: every screen and state, with layout specs and PNG renders for iOS and Android, dark and light. **Compare your running screens against these PNGs.**
6. `docs/design/motion.md`: the reveal choreography on the round table, discussion tension, elimination, game over, the haptics map, and the reduced-motion and reduced-transparency fallbacks.
7. `docs/design/copy.json` and `docs/design/copy/values*/strings.xml`: every string in HR and EN, with Croatian plurals (one/few/other). Copy the XML into `composeResources`. Keys marked *proposed* are fine to use, but list them in your PR.
8. `docs/design/assets/`: app icon (iOS layers, Android adaptive + monochrome), splash, brand mark, game glyphs and UI icons as SVG. Convert the icons to `ImageVector`.
9. `docs/design/source/*.dc.html`: raw board source, if you need an exact value the docs don't state.

Also read `docs/claude-design-prompt.md` (the original brief, including the engineering notes at the end) and the old app on `main` (`lib/game.ts`, `functions/api/*`, `data/words.json`) for the current rules, the word list and the admin PIN mechanism.

Non-negotiables from the design:
- Vermilion means spy, and nothing else except the brand mark. One primary action per screen. Only cards cast shadows. Secrets live only on card faces: never in titles, toasts, notifications or the app switcher.
- Dynamic colour is off. Card faces are theme-independent. Fonts are Bricolage Grotesque + Hanken Grotesk, bundled for both platforms.
- iOS Liquid Glass is used only for native chrome (tab bar, nav bar/toolbar, glass buttons, sheets) through SwiftUI interop. Android uses Material 3 Expressive with no glass.
- Roles are never broadcast. Each device receives only its own role. Votes are held server-side until everyone has voted. Custom dictionaries never leave the device; only the drawn word travels to a room.
- Accessibility: 48 pt/dp targets, text scaling to 200 %, screen-reader labels for every reveal and vote state, reduced motion and transparency fallbacks. Haptics stay on under Reduce Motion.
- Never inflect player names, and never use gendered verb forms about players (see the copy rules).

## Target architecture (propose changes in your first PR if you disagree)

```
v2/
├─ mobile/                  KMP + Compose Multiplatform project (Gradle)
│  ├─ shared/               game engine (pure Kotlin, no UI), models, protocol, repositories
│  ├─ composeApp/           shared Compose UI + theme + resources; androidMain / iosMain
│  └─ iosApp/               Xcode project; SwiftUI host + native chrome (TabView, toolbar, glass buttons)
├─ server/                  NEW Cloudflare Worker: built-in dictionaries API + rooms (Durable Objects + WebSockets)
├─ docs/                    design handoff (don't edit docs/design except to fix errors, in a separate PR)
└─ data/words.json          seed for the Croatian built-in dictionary
```

- **Retire the Next.js web app on `v2`** (it lives on `main`). Remove `app/`, `components/`, `lib/`, `next.config.mjs` and the root Next tooling in a dedicated PR. Move what the server still needs (word rules, seed script, D1 schema) into `server/`.
- **Backend isolation:** use a **new** Worker name and a **new** D1 database for v2 (for example `spy-v2` and `spy-dictionaries`) so production on `main` is never affected. Model the built-in dictionaries as dictionary → category → word, with a language per dictionary: Hrvatski (seed from `data/words.json`) and English (needs a word list; ask the product owner or draft one for review). Keep the shared-PIN admin edit flow (`x-spy-pin` header, `ADMIN_PIN` secret).
- Suggested libraries (use current stable versions and justify any swaps): Compose Multiplatform + Material 3, AndroidX Navigation for Compose Multiplatform, kotlinx.coroutines, kotlinx.serialization, Ktor client (HTTP + WebSockets), SQLDelight or Room KMP for local dictionaries and settings, multiplatform-settings or DataStore for preferences, a KMP QR generator, VisionKit (iOS) and CameraX + ML Kit (Android) for scanning, and expect/actual `SpyHaptics`.

## Milestones (one or more PRs each, in order)

1. **Scaffold.** Gradle KMP project under `mobile/`, Android app and iOS app both launching an empty Compose screen; `SpyTheme` wired to tokens and fonts; string resources HR + EN (device language by default); app icons and splash for both platforms; CI (GitHub Actions) building Android and the iOS framework and running shared tests on PRs to `v2`.
2. **Game engine** in `shared/`, fully unit-tested and UI-free: setup validation (3–12 players, spy limit: open issue §12-1), role assignment with other-spy lists, word drawing from the selected categories, rounds, timer state, anonymous vote collection, ties (re-vote between tied players, then nobody out on a second tie), out-loud knock-out, win conditions, game-over recap, and serialisable state for "Continue game".
3. **One-phone mode end to end:** Home (incl. Continue game), mode sheet, setup with all states, role reveal on the table (deal, turn, lift, tap-to-flip, auto-hide, pass), discussion with tension states, in-app pass-the-phone voting, out-loud host marking, round result, game over, and quit confirm. Tab bar with native chrome; immersive in-game screens. Haptics, reduced motion and privacy-on-background included.
4. **Dictionaries:** local custom dictionaries (CRUD, validation, empty states), built-in fetch + offline cache from the new server, setup picker, import/export (file, link, QR with the ≈2 KB limit, preview + merge / save-as-new, error states), and the admin PIN sheet in Settings.
5. **Settings** complete: language, theme, defaults, reveal style and auto-hide, haptics, sound, keep screen on, about.
6. **Server rooms:** Durable Object per room, WebSocket protocol (document it in `server/PROTOCOL.md` first): create/join with the 4-character code (A–Z 2–9, no 0/O/1/I), nicknames, host controls, per-device role delivery, server-clock timer, server-held votes, reconnection, 60 s drop grace, host handover, removal. Test it with automated tests (Miniflare / vitest).
7. **Multi-device mode in the apps:** lobby (host and guest), join (code, link, QR), own-phone reveal and peek again, synced round, voting with progress, spectators, and all P2·12 connection and edge states.
8. **Polish and release readiness:** full accessibility pass (VoiceOver/TalkBack labels and announcements from the copy keys, Dynamic Type at 200 %), sound cues (assets need sourcing; ask), performance on mid-range Android, store metadata checklist.

## How to work

- Before each milestone, re-read the relevant sections of `screens.md`, `components.md` and `motion.md`, and list the states you'll build in the PR description.
- Verify on real targets: run the Android app on an emulator and the iOS app in the iOS Simulator, check dark and light themes, and attach screenshots next to the matching `docs/design/screens/` PNGs in each UI PR. Test Croatian (the stress-test language) and English.
- Keep the game engine and server logic covered by tests; UI gets screenshot or semantics tests where practical.
- When the design is silent or contradictory (see `decisions.md` §12), stop and ask the product owner with a concrete recommendation rather than guessing. Note any deviation from the design in the PR.
- Start now with milestone 1: inspect the repo, propose the exact layout and library versions in a short plan, then scaffold.
