# Špijun — Decisions

> **Provenance.** The design conversation itself (the claude.ai chat where the canvas was made) is not accessible from this export — only the canvas is. Every decision below is therefore reconstructed from what the final canvas **shows or states**, with the board it comes from in brackets, and compared against the original brief in `docs/claude-design-prompt.md`. Where a decision was clearly made in conversation but only its result is visible (e.g. which direction won), it is recorded as the result. If you remember a decision from the chat that is missing here, add it — the canvas is the source of truth for everything else.

Board keys: `P1-*` = Phase 1 hero frames (`frames/Main.png`, `A-Android`, `B-*`, `C-*`, `D-*`), `02…12` = Phase 2 language boards (`frames/P2-*`), `P1·01…P3·14` = screens (`frames/P3-*`, `frames/P4-*`), `H·01…H·08` = Phase 4 handoff (`frames/P5-*`).

---

## 1 · Process

| Phase | What happened | Board |
|---|---|---|
| 1 · Directions | Brief asked for 3 directions; **4** were delivered, each as the spy-with-fellow-spy reveal on iOS + Android: **A · Cenzura** (Redacted — condensed caps, redaction bars, yellow), **B · Pomrčina** (Eclipse — corona glow, press-and-hold), **C · Karta** (Dealt card — playing-card faces, tap-to-flip), **D · Klasik** (Classic, refined — calm native lists). | Main, A–D |
| 2 · Language | Chosen: a **hybrid of C · Karta × D · Klasik** — "Mirni stol, glasne karte" (*A calm table, loud cards*). | 01, 02 |
| 3 · Screens | All P1 screens with all states (iOS + Android × dark + light), then P2 and P3. | P1·01–P3·14 |
| 4 · Handoff | Tokens → Compose, component specs, annotated reveal & setup, motion/haptics implementation, 287 copy keys HR/EN. | H·01–H·08 |

## 2 · Concept & visual direction

- **Concept:** the app is the **table** — quiet, neutral, legible, out of the way (home, setup, lobby, join, round, voting, result, dictionaries, import/export, settings). Only the **cards** are loud (role, elimination, final reveal, app icon, splash). "The mischief lives in the cards, never in the chrome." [01]
- **Table layer from D · Klasik:** warm-neutral ramp, Hanken Grotesk, ivory primary button, 28 px panels, no shadows. **Card layer from C · Karta:** card faces, Bricolage Grotesque, suit glyphs, deal & flip motion, haptic thuds. [01]
- **Four rules:** (1) **Vermilion means spy** — nowhere else except the brand mark. (2) **One primary action per screen**, always at the thumb. (3) **Only cards cast shadows** — the table is flat. (4) **Secrets live on card faces** — never in titles, toasts or the app switcher. [01]
- **Rejected:** A · Cenzura — condensed caps read badly in a dim, noisy room; cold; drifts toward spy-file cliché. B · Pomrčina — glow breaks down in the light theme; the wide face overflows long Croatian words; doesn't extend to lists. C alone — cards everywhere turns settings and dictionaries into a casino. D alone — correct but anonymous; nothing says Špijun. [01]
- **Kept from today's web app:** the flow, the copy, progress dots. **Dropped:** indigo `#6c5ce7`, gradient title, emoji. [01]
- **Personality:** confident (big type, one decisive action), mischievous (the spy card *is* the brand), discreet (a secret only ever lives on a card face). [01]
- **Dark-first**, fully designed light theme. [03]
- **Colour:** warm grey ramp at hue 35°, chroma ≈ 2; vermilion `#FF6B3D` = spy & brand; blue `#6E9BFF` = citizen (orange vs blue survives common colour-vision deficiencies; every role colour ships with its glyph). **Primary is neutral** (ivory/ink) so switches, focus and progress stay calm. [03, 10]
- **Material You dynamic colour is OFF** — role colours carry meaning. Only the launcher icon follows the themed-icon setting. [03, 10, P3·13]
- **Card faces are theme-independent** (spy face dark, citizen face ivory in both themes). The **room-code card is ivory in both themes** so the QR scans across a dim room. [03, P1·03]
- **Typography:** one shared pair on both platforms instead of SF Pro / Roboto — **Bricolage Grotesque** (display, cards) + **Hanken Grotesk** (text, tabular figures), both OFL, bundled as Compose resources. Diacritics verified (Č Ć Š Ž Đ). [04] *(Change vs brief, which suggested SF Pro conventions on iOS.)*
- **Shape:** rounded table (6/10/16/22/28/full) vs **cut-corner cards** (CutCorner 22 / frame 14) — the only non-round shape, cards only. [05]
- **Glass:** iOS 26 Liquid Glass for chrome only (tab bar, nav bar/toolbar, floating buttons, sheets), never content. **Android: no glass**, tonal surfaces. [05, 10]
- **Iconography:** custom game glyphs (whole diamond = citizen, split diamond = spy & brand mark, hollow struck diamond = eliminated, "D" dealer chip = host, mini card back = hidden role, check circle = voted, dashed circle = dropped, timer). SF Symbols in native iOS chrome, Material Symbols Rounded 500 on Android. No emoji, no trench coat/magnifying glass. [05]
- **App icon:** the spy card — dark cut-corner card tilted −9° with the split diamond on a vermilion field; three flat layers (background, card, glyph) for iOS Icon Composer / Liquid Glass and Android adaptive + monochrome. No text. [P3·13]

## 3 · Game rules (final)

| Topic | Decision | Board |
|---|---|---|
| Players | **3–12** in both modes. Host is also a player. | P1·03, copy `setup_players_from_room` |
| Spies | Stepper, "najviše N" with N = players − 1 (see open issue #1). Several spies: **each spy sees the other spies' names** on their card. | P1·02, P1·04 d |
| Roles | Citizens see word + category. Spies see "Ti si Špijun" + category only. | P1·04 |
| Reveal | **Tap to flip** (default), **auto-hide after 8 s** (5 / 8 / off). Hold-to-peek is a Settings alternative. | 06, P2·11 |
| Own-phone reveal | Your seat is always at the bottom; you can **peek again** any time; discussion starts **once everyone has looked**. | P1·04 e |
| Round | Discussion (optional timer) → vote → one player knocked out (or nobody on a double tie) → next round. | P1·05–07 |
| Timer | Off / 3 / 5 / 8 min / custom, per round, **synced on all phones** from the server clock. Host can **+1 min** or **end discussion**. ≤30 s "almost up" state. At 0: in-app → vote opens automatically; out-loud → alarm, host marks result. | P1·02, P1·05 |
| Voting mode | Chosen in setup: **in the app** or **out loud**. | P1·02 |
| In-app voting | **Anonymous.** Votes held (server / hidden on the one phone) until everyone has voted, then revealed together. Can't vote for yourself; knocked-out players can't vote and can't be picked. **Confirm locks the vote.** Waiting screen shows **who hasn't voted, never who voted for whom**. Host can **close voting** (counts only cast votes). One-phone: the phone is passed for private votes using the same table choreography. | P1·06 |
| Names | One-phone: names optional, **required if voting in the app**. Own-phone: nicknames per room. | P1·02 |
| Out-loud voting | Lightest interaction: after the alarm the host **taps the card of the player the group voted out, then confirms** — two taps — or "Nitko ne ispada". | P1·05 e |
| Ties | **Re-vote only between the tied players.** If the re-vote ties again, **nobody is knocked out** and the next round starts. | P1·07 |
| **Eliminated player's role** | **NOT revealed** when knocked out — no flip, no role, no word; the card is flicked off the table face-down and stamped "IZVAN IGRE". **All roles are revealed at game over.** *(Answers the brief's open question — the brief's default assumption "yes, spy / not spy" was rejected.)* | 06, P1·07, 08 |
| Spectators | Knocked-out players keep watching: see vote progress, never roles or votes; can't vote. | P1·06 d |
| Win | **Citizens win** when all spies are out. **Spies win** when spies ≥ remaining citizens. The round-result screen is skipped when the knock-out ends the game. | P1·07, P1·08 |
| Game over | Winning side, every card flips (spies first), spies + secret word dealt to the front, **round-by-round recap with each knock-out's role**. **Igraj ponovno** = same settings, new word, new roles; **Nova igra**. | P1·08 |
| Rematch | Roles are fully random every game — the same player can be spy twice in a row. (Ideas frame suggests reshuffling seats.) | Ideas |
| Quit | ✕ during a game → confirm; ends the game for everyone in the room; roles and word are **not** revealed. | P3·14 |

## 4 · Multi-device ("Svaki na svom telefonu")

- No accounts; temporary **nickname per room** (max 16 chars, unique in the room, remembered on the phone, never sent elsewhere). [P1·03]
- **Room code: 4 characters, A–Z + 2–9, without 0/O/1/I**; join by typing, pasting, a room link, or scanning the QR. [P1·03, 07]
- Lobby: host sees code + QR + share, live player list with connection states, can remove players (⋯), min-players gate (3), **Start shows the player count**. Guest sees read-only list and rules, can still share the code, can leave. [P1·03]
- Join errors: room not found (cells shake), name taken, **game already started → "Čekaj sljedeću igru" (code stays the same)**, room full (12). [P1·03]
- Only the **single chosen word** travels to the room; dictionaries never leave the host's phone (UI hint in setup). Roles are delivered per device and **never travel to other phones**, even when someone drops. [P1·02, P2·12]
- **Reconnecting:** banner; the game goes on; countdown runs locally from the server clock; "your card is saved on this phone". [P2·12 a]
- **Player dropped:** seat goes dashed amber; host decides after a **60 s grace**: wait or remove; a removed player counts as knocked out and their role is revealed only at the end. [P2·12 b]
- **Host left:** game pauses; **anyone can take over**; after 30 s the longest-connected player is offered it first; or end game. Roles stay the same. [P2·12 c]
- **Removed:** clean exit, no roles, no word; can rejoin the next game with the same code. [P2·12 d]
- **Offline when creating a room:** sheet offers one-phone play. [P2·12 e]
- **Backgrounded mid-round:** any open card is hidden the moment the app leaves the screen; on return "Opet si tu · Runda N je još u tijeku". App-switcher snapshot always blurred. [P2·12 f]

## 5 · Navigation

- **Three destinations — Igraj | Rječnici | Postavke** ("people do three things: play, curate words, tweak"). iOS `TabView` Liquid Glass; Android `NavigationBar` with neutral indicator. [10]
- **The bar disappears once a game starts** — immersive, only a close button. [10, P1·04]
- **Join with code** is the second button on the Play home, beside "Nova igra". A room link or scanned QR opens straight into join with the code filled in. One scanner handles rooms and dictionaries. [10, P1·01, P2·10 f]
- "Nova igra" opens a **mode chooser sheet** (One phone / Each on own phone). [P1·01 c]
- **Continue game** card on Home when the app was closed mid-game (shows round and players in, never role/word). [P1·01 b]
- Admin editing of built-in dictionaries is **hidden in Settings → last row of "O aplikaciji"**, behind a PIN sheet (today's shared-PIN backend unchanged). [P2·11]

## 6 · Dictionaries

- Dictionary → categories → words; each dictionary has a word language (Hrvatski / English). [P1·09]
- **Built-in (Hrvatski, English):** server-synced, cached offline, **read-only**, "Ugrađen" lock badge; **"Napravi kopiju"** creates an editable custom copy. [P1·09 g]
- **Custom:** stored only on this phone ("MOJI · SAMO NA OVOM UREĐAJU"); create, rename, change language, add/edit/delete categories and words, duplicate, delete (native confirm). Duplicate word check is **case-insensitive**; search for long lists; iOS swipe / Android long-press to delete; add → toast with Undo. [P1·09]
- **Setup:** pick one or more dictionaries (chips), then categories with counts; "N riječi u odabiru" footer. [P1·02]
- **Export:** File (.json via share sheet), Link and QR that **carry the dictionary itself** (nothing on our server). Limit **≈ 2 KB ≈ 80 words**; above that Link/QR are disabled and File is promoted. QR screen boosts brightness. [P2·10]
- **Import:** always **previewed** — name, language, category/word counts, duplicates ("već imaš"); on a name conflict choose **Merge (adds new, skips existing)** or **Save as new ("Party pack (2)")**; errors: invalid file, newer version, empty. [P2·10]

## 7 · Settings

Language (default follows device) · Theme (system / light / dark) · New-game defaults (players & spies, timer, voting) · Reveal (tap to flip vs hold to peek; auto-hide 5 s / 8 s / off) · Haptics · Sounds (default on) · Keep screen on · About (version, licences, admin entry). Reduce Motion notice shown when the OS setting is on. [P2·11]

## 8 · Copy rules (decided in Phase 4)

- **No declined names** — player names are user input, so strings never inflect them ("Dodaj telefon Luki" → "Telefon ide: Luka", "Glasaj za Anu" → "Glasaj · Ana", "Izbaci Anu" → "Izbaci · Ana", "između Ane i Luke" → "Ana ili Luka"). [H·06]
- **No gendered verbs** about players — nicknames carry no gender ("Ana je izbačena" → "Ana ispada iz igre", "Povezan/a" → "Na vezi", "Dobrodošla natrag" → "Opet si tu", "Ispao si" → "Izvan igre si", stamp "IZBAČENA" → "IZVAN IGRE"). [H·06] The stamp key was later revised from "IZBAČEN" to "IZVAN IGRE" (copy table v2).
- **Whole sentences are keyed** (ICU plurals hr one/few/other, en one/other; verb agreement "je/su/je"). Positional args only. [04, H·06]
- Croatian is the primary stress test; the secret word must wrap on two lines at its largest size ("Svemirska postaja", "Policijska postaja", "Nogometni stadion"). [04]

## 9 · Accessibility decisions

- 48 pt/dp minimum targets during play (above iOS 44); primary actions in the bottom third; the whole card is the flip target. [09]
- Text to 200 %; display styles cap at 1.3× and auto-fit. [09]
- Screen reader: labels for every reveal and vote state; **before the first reveal ask once "Uloga će se izgovoriti naglas. Stavi slušalice."** and offer silent large-text reveal; announce once, never re-read on focus. [09]
- Countdown announced politely at full minutes, 30 s, 10 s, 0 — never every second. [09]
- Reduce Motion / Remove animations: cross-fades, no table turn/drift; **haptics stay**. Reduce Transparency: glass → solid surface-2 + 1 pt line. Increase Contrast: stronger lines and muted text, 2 pt card frame. [06, 09]

## 10 · Changes from the original prompt

| Brief said | Final design |
|---|---|
| 3 directions in Phase 1 | 4 directions; final = hybrid of two (C × D) |
| Default: eliminated player's role revealed immediately (spy / not spy) | **No reveal on elimination**; all roles at game over (with per-round recap) |
| Explore tap-to-flip vs press-and-hold, recommend one | **Tap-to-flip** recommended + default; hold-to-peek is a setting; auto-hide 8 s added |
| Reveal screen | Reveal staged on a **round table** that turns between players (pass-the-phone made visible); the table persists through discussion as the "who's still in" list |
| iOS: SF Pro / SF Symbols conventions | Shared custom type (Bricolage + Hanken) on both platforms; SF Symbols only in native chrome |
| Join with code + QR | Same, plus room links and a single scanner that recognises rooms and dictionaries |
| Ties: "design ties" | Re-vote between tied; second tie → nobody out |
| Out-loud: "lightest possible interaction" | Two taps: tap the seat card → confirm (or "Nitko ne ispada") |
| Settings list | As briefed, plus auto-hide duration and Reduce-Motion notice |
| Copy reused from web app | Reused and extended; copy rules added (no declined names, no gendered participles) |
| Today's look (indigo, gradient, emoji) | Replaced; kept flow, copy, progress dots |
| Admin PIN editing "low prominence" | Last row of Settings › O aplikaciji, PIN sheet |

## 11 · Ideas frame (outside the main flow)

From `frames/P4-15-Ideas.png` — "Small bets, big table feel". Low-risk additions; none needs accounts or server storage; none blocks the release.

| # | Effort | Idea |
|---|---|---|
| 01 | small | **Same crew as last time?** Remember the last group's names and seat order on this phone. One tap refills one-phone setup — names are the slowest part of starting. |
| 02 | small | **Fresh table on rematch.** "Igraj ponovno" reshuffles the seats so the table looks new. Roles stay fully random every game — the same player can be the spy twice in a row. |
| 03 | small | **Shake to hide.** A quick shake flips any open card back instantly — a panic button when someone leans over your shoulder. |
| 04 | small | **Haptic-only reveal.** For low-vision players in a noisy room: the role is given only by vibration (double thud = spy), nothing spoken aloud. Word read on demand with headphones. |
| 05 | medium | **Flag a bad word.** At game over, long-press the word card: "Preteško / nejasno". Custom dictionaries: mark locally. Built-ins: one anonymous flag to the existing admin list. |
| 06 | small | **Night stats.** Local-only fun facts after a few games: "Ana je preživjela 3 igre kao špijun." Cleared when the app closes; nothing stored. *(Note: this example sentence uses a gendered participle — rephrase per the copy rules.)* |

## 12 · Open issues found during export (need a decision)

1. **Spy maximum vs win condition.** Setup allows up to *players − 1* spies ("najviše 7" for 8; the component board shows "najviše 2 za 3 igrača"), but spies win as soon as spies ≥ citizens — so any count ≥ half the players ends the game before it starts. Suggest **max spies = ⌊(players − 1) / 2⌋** (3–4 players → 1, 5–6 → 2, 7–8 → 3, …). Host/own-phone setup defers the check "until everyone joins" — define what happens if too few join (block Start with a reason).
2. **Not designed:** the player-names entry sheet (setup "Imena igrača" row), custom timer picker (described: iOS wheel sheet / Android time dialog), language & theme pickers, "Igrači i špijuni" defaults screen, licences, the remove-player confirm in the lobby, out-loud mode in **one-phone** play (assumed identical to P1·05 e), the "Continue game" restore flow beyond the Home card.
3. **Gendered / declined copy still in mocks:** `conn_player_dropped_body` ("ga… mu"), host-left mock ("Luka je izašao"), guest lobby ("Lukina soba"), result ("Ivan nije glasao"), game over ("Ana i Nina su ostale neotkrivene"), import ("od Ane"), round ("Dodirni njegovu kartu"). Neutral proposals are in `copy.json › proposed` and `review`.
4. **Vermilion scan line** in the QR scanner bends rule #1 (vermilion = spy). Suggest ivory.
5. **Hanken Grotesk tabular figures** must be verified in the shipped font file; fallback is a fixed-width box per digit. [H·01]
6. **Minor token drift between boards** (canonical values chosen in `tokens.json`): light `table` `#E8E0D3` (one board `#EAE3D7`); light disabled fill `#C9C2B9` (table boards use `#B5AC9F`); card drop-shadow varies 0 12 20 → 0 24 40 by card size. Light-theme M3 `tertiary` (T40 `#AE3203`) differs from the semantic `spy` light `#C2410C` — game UI uses `spy`.
7. **Missing deliverables referenced by the canvas:** H·01 mentions a `spijun-handoff/` package (`SpyTheme.kt`, `tokens.json`, string resources) — it was never attached to the canvas. This export replaces it (`tokens.json`, `compose/SpyTheme.kt`, `copy/`). **Sound assets** (flick, deal, drum, bell, chime) were specified but not produced.
8. **QR codes in mocks are placeholders** (checkerboard) — generate real QR (e.g. ZXing / CoreImage `CIQRCodeGenerator`) with ≥ 4-module quiet zone; the dictionary QR has a brand-mark medallion in the centre (use error-correction level H).
9. **Room capacity copy** "Soba je puna · 12 od 12 igrača." exists only in a section description — key `join_err_full_body` says "U sobi je već 12 igrača.".
