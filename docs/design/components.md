# Špijun — Components

Every component in the design, who builds it, its sizes, states and accessibility contract. Source: canvas boards **07 Components — table layer** (`frames/P2-06-Controls.png`), **08 Components — card & game layer** (`frames/P2-07-Game.png`), **H·02 Component specs** (`frames/P5-02-Components.png`), the annotated boards H·03/H·04, and the Phase 3 screens.

**Ownership labels**

| Label | Meaning |
|---|---|
| **Shared (Compose)** | Built once in Compose Multiplatform, used on both platforms. Platform differences are token values only (height, radius, haptic). |
| **Native iOS (SwiftUI)** | SwiftUI view embedded via interop (Liquid Glass chrome, system sheets/dialogs, VisionKit). |
| **Native Android (Material)** | Material 3 (Expressive) component from `androidx.compose.material3`, or a platform API (Sharesheet, CameraX). |

Theme access: `Spy.colors.*`, `Spy.type.*`, `SpyShape.*`, `SpySpace.*`, `SpyMotion.*`, `SpyHaptics` — see `tokens.json` and `compose/SpyTheme.kt`. Haptic names (e.g. `RevealSpy`) refer to `tokens.json › haptics`.

Rules that apply to every component:
- **Vermilion (`spy`) means spy.** Never use it for buttons, links, focus, selection or errors. The only other use is the brand mark.
- **Never colour alone.** Selection = fill/border + check/bold; roles = glyph + wording + card lightness + haptic; connection = word + icon.
- **Min target 48 pt/dp**, 8 between neighbours.
- **No emoji.** Icons: SF Symbols in native iOS chrome, Material Symbols Rounded (wght 500) in native Android chrome; Shared icons are the SVGs in `assets/icons/` (24 grid, 2 pt stroke, round caps/joins).

---

## 1 · Chrome (native)

### Tab bar — Native iOS (SwiftUI) / Native Android (Material)
- **iOS:** `TabView` with Liquid Glass (iOS 26). Floating capsule 64 tall, inset 24 sides / 22 bottom; selected item sits in a 54-tall capsule (`tabSelected`). Icon 22 + label 11/700 (selected) / muted.
- **Android:** `NavigationBar`, 88 tall incl. 8 bottom padding, **neutral** indicator 56×32 radius 16 (`tabSelected` tone — not brand colour), label 12.
- Items: **Igraj** (`tab-play` / selected `tab-play-selected`), **Rječnici** (`tab-dictionaries`), **Postavke** (`tab-settings`). Labels always visible. Copy: `tab_play`, `tab_dictionaries`, `tab_settings`.
- States: selected · unselected · **hidden during a game** (immersive from the first reveal until Game over).
- A11y: selected state announced; content scrolls underneath (iOS) — apply bottom content inset.

### Navigation bar / top app bar — Native iOS / Native Android
- **iOS:** `NavigationStack` + `.toolbar`, large title (34/40 Bricolage 800) → inline glass nav bar 17/600 on scroll. Back = glass button 48.
- **Android:** `LargeTopAppBar` (title 30 Bricolage) → small (22/600) on scroll; edge-to-edge; predictive back.
- In-game variant: **no title**; leading close ✕ (glass 48 / `IconButton`), centre **pill** (see Pill), optional trailing slot (lobby settings, room-code pill).
- A11y: title is the screen's accessibility heading.

### Glass button / icon button — Native iOS / Native Android
- iOS: `Button { Image(...) }.glassEffect(.regular.interactive())`, 48×48 circle. Fallback (Reduce Transparency, or if interop unavailable): solid `surface2` + 1 pt `line`; the in-mock look is `glass` + `glassLine` + `glassHighlight`.
- Android: `IconButton` / `FilledTonalIconButton` 48, no glass.
- Used for: close, back, share, import, add, ⋯ (more = SF `ellipsis` / Material `more_horiz`), torch, lobby settings. **Chrome layer only.**
- States: default · pressed · disabled. A11y: `contentDescription` required ("Natrag", "Prekini igru", "Zatvori", "Podijeli rječnik", "Više opcija", "Svjetiljka").

### Sheet — Native iOS / Native Android (content Shared)
- iOS: `.sheet` + `presentationDetents([.medium, .large])`, floating (inset 8, radius 34 top / 40 bottom), hosting Compose content. Android: `ModalBottomSheet`, radius 28 top, drag handle 36×5 (`disabledFill`).
- Scrim: 60 % black (dark) / 35 % ink (light). Sheet bg `surface2` (iOS glass fallback `sheetGlass`).
- States: medium · large · dismissing. A11y: focus moves into the sheet; back/Escape closes.
- Used by: mode chooser, create/rename dictionary, export, admin PIN, join errors, connection sheets, offline.

### Confirm dialog — Native iOS / Native Android
- iOS `confirmationDialog` (action sheet); Android `AlertDialog` with destructive text button.
- The **safe action is the prominent one** ("Nastavi igrati", "Odustani"); destructive label names the object ("Izbriši rječnik", "Prekini igru") in `danger`.
- Used by: delete dictionary (`dict_delete_*`), quit game (`quit_*`), remove player.

### Share / file pick / open-in — Native iOS / Native Android
- iOS: `ShareLink` / activity sheet, `.fileImporter` with `UTType.json`, "Open in Špijun" document type. Android: Sharesheet, `ACTION_OPEN_DOCUMENT`, intent filter for `application/json` + app links.
- States: export file · link · open-in-app import. System UI only — nothing custom.

### QR scanner — Native iOS + Native Android + Shared overlay
- iOS: VisionKit `DataScannerViewController`. Android: CameraX + ML Kit barcode. Compose overlay: 260×260 viewfinder with ivory 4 pt corner brackets, a 2 pt scan line (drawn vermilion in the mock — this bends the "vermilion means spy" rule; `text` ivory is the safe alternative, see decisions.md › open issues), title `scan_title`, hint `scan_hint`, torch button, **Upiši kod sobe** secondary.
- States: pre-permission (`camera_*`) · scanning · found (haptic `ScanFound`, route by payload: room vs dictionary) · denied → "Otvori postavke" + manual entry. Always dark (camera).

---

## 2 · Table layer (Shared, Compose)

### SpyButton — Shared
```kotlin
SpyButton(text, onClick, kind = Primary | Secondary | Tertiary | Destructive,
          loading = false, enabled = true, icon: ImageVector? = null, size = Large | Small)
```
| | iOS | Android |
|---|---|---|
| Large height / radius | 56 / full (capsule) | 64 / `md` 16 → full on press (M3 Expressive shape morph) |
| Small height / radius | 52 / 26 | 52 / 14 |
| Label | `button` 17/22 Hanken 700 (mocks use 17–19) | same |

- **Primary:** `primary` fill, `onPrimary` text (ivory in dark, ink in light). One per screen.
- **Secondary:** `surface2` fill, `text`.
- **Tertiary / text:** no fill, `text` 17/600–700.
- **Destructive:** `dangerFill` + white (in dialogs: `danger` text).
- States: default · pressed (scale 0.97, 100 ms; Android also morphs to full) · loading (spinner replaces label, `contentDescription` "Učitavanje") · disabled (`disabledFill` bg, `textMuted` label — not just lowered alpha).
- A11y: role button; 48 min; loading announced.

### SpyStepper — Shared
```kotlin
SpyStepper(label, value, range, hint: String?, hintTone = Neutral | Warning, onChange)
```
- Row 60 tall inside a panel; label `bodyLarge` 600 + optional hint 13 `textMuted`; − / + buttons 48 circles (`surface2`), value 24/700 tabular, min width 32.
- States: default · at min (− disabled) · at max (+ disabled) · hint warning ("najviše 2 za 3 igrača").
- Haptic: `StepperTick` per change. A11y: adjustable (increment/decrement actions); value + hint read together.

### SpyChoiceRow / Segment — Shared
```kotlin
SpyChoiceRow(options, selected, onSelect, style = Chips | Segmented)
```
- **Chips:** height 48, radius full, `surface2` → selected `primary` fill + bold (+ check icon when there's room); "custom" chip has a dashed `checkboxOutline` border (opens wheel picker sheet on iOS / time input dialog on Android). Wraps to 2 lines at large text.
- **Segmented:** track `surface` radius full, height 52; selected thumb `primary`.
- Haptic `StepperTick`. A11y: radio-group semantics; never colour-only.

### SpySwitchRow — Shared
- Row 53: optional leading icon, label `bodyLarge`, platform-styled switch (iOS look on iOS, M3 on Android); on track `success`.
- States: on · off · disabled. A11y: switch role; whole row is the target.

### SpyTextField — Shared
```kotlin
SpyTextField(label, value, onValue, error: String?, maxLength = 16, placeholder)
```
- Label above (13/700), field 56 tall, radius `sm` 10 (mocks 14–16), `surface` fill, 1 px `line`; focused 2 px `text` border; counter "3 / 16" right below; error: `danger` border + error icon + message (live region).
- States: default · focused · filled · error · disabled. A11y: label always visible.

### RoomCodeInput — Shared
- 4 cells (≈ 80×76, gap 10, radius 16, `surface`), glyph 36/700 Hanken uppercase tabular; one real text field underneath (paste, IME, a11y).
- Alphabet A–Z + 2–9, **no 0/O/1/I**; auto-uppercase; room links/QR pre-fill.
- States: empty · typing (focused cell 1.5 px `text` border + caret) · complete · checking · error (all cells `danger` border/text, shake ×3 300 ms, `Error` haptic; typing clears).
- A11y: reads "Kod sobe, K 7 Q".

### RoomCodeCard — Shared
- **Always ivory** (`card.roomCodeCard`) in both themes, radius 28, padding 16. QR left (≥124 pt incl. quiet zone, black on white), right: label `lobby_code_label`, code 44/800 tabular +4 % tracking, small dark **Podijeli** pill.
- A11y: QR labelled "QR kod za ulazak u sobu K7QX".

### CategoryRow — Shared
```kotlin
CategoryRow(name, count, checked, enabled, onToggle)
```
- 55 tall; checkbox 22 radius 6 (checked: `primary` fill + check in `onPrimary`; unchecked: 1.5 px `checkboxOutline`), name `bodyLarge`, count right `textMuted` tabular.
- States: checked · unchecked · empty (dashed checkbox, disabled, "prazna"/count 0 in `danger` when selected in setup).
- A11y: checkbox role: "Mjesta, 22 riječi, odabrano".

### DictionaryRow — Shared (+ native swipe)
```kotlin
DictionaryRow(dict, onOpen, actions = listOf(Duplicate, Delete))
```
- 68 tall; language tile 40 (radius 12, `surface2`, "HR"/"EN" 13/800), name 17/700, meta 14 `textMuted` (`plural_categories` · `plural_words`); trailing **Ugrađen** badge (lock icon, 1 px `line`, radius 8, 12/700) for built-ins, chevron for custom.
- States: built-in (lock badge, read-only) · custom · empty words (meta in `warningText`).
- iOS: native swipe actions (Kopiraj `surface2`, Izbriši `dangerFill`). Android: long-press → contextual top bar (no swipe-to-delete). Both also in ⋯.

### List row / panel — Shared
- Grouped panel: `surface`, radius `lg` 22, rows separated by 1 px `line` inset 16. Row 52–55: label + value (`textMuted`) + chevron.
- Section label above: `label` 13/700 caps +8 % `textMuted`, 20 from gutter.

### PlayerRow / chip — Shared
```kotlin
PlayerRow(player, isMe, isHost, status = Connected | Connecting | Dropped | Voted | Out)
```
- 60 tall; avatar circle 32 (`surface2`, initial 14/700; "me" inverted ivory), name `bodyLarge` (+ "(ti)" muted), status line 13: "Domaćin" · ● "Na vezi" `success` · ◌ "Povezuje se…" `warningText` · dashed amber "Veza prekinuta · čeka se 0:42" · ✓ voted (`voted` icon + `vote_cast`; the P2 mock's "Glasala" is gendered — don't use) · Out: name struck-through in `eliminated`, "Izvan igre od runde 1", tag "GLEDA".
- Host: **D dealer chip** (`assets/glyphs/glyph-host-dealer.svg`, 26 circle `primary` + "D" 14/800 `onPrimary`). Host sees a trailing ⋯ on others → remove.
- A11y: status always in words, never dot-only. **Out never shows the role** (until game over).

### Banner / Toast — Shared
```kotlin
SpyBanner(tone = Warning | Neutral, title, body, spinner = false)
SpyToast(text, action: Pair<String, () -> Unit>? = null)
```
- Banner: radius 22, padding 14–16, under the top row. Warning = `warning` fill + `onWarning` text; Neutral = `primary` (ivory) fill + ink. Optional spinner/icon leading.
- Toast: `primary` fill pill-card radius 22 above the bottom safe area; leading `check` icon (`success`), text, one action max ("Poništi"). 4 s, swipe away.
- Live region polite. **Never shows secrets; paused during a reveal.**

### EmptyState — Shared
```kotlin
EmptyState(glyph, title, body, primary, secondary?)
```
- Dashed 1.5 px `line` border, radius 22, padding 28, centred: hollow diamond 40, title 18/700, body 15 `textMuted`, small Primary + small Secondary.
- Variants: dictionaries · categories · words. Title is a heading.

### Loading skeleton — Shared
- Blocks in `surface2` matching real rows (chips 44 tall radius full, rows with 22 checkbox + bar). Shows only if loading > 300 ms. Pulse α 0.5↔1 (1.2 s); no shimmer sweep under Reduce Motion.

### Pill (in-game header) — Shared (iOS uses glass fallback look or native glass)
- 40 tall, padding 0 16, radius full, 15/600. iOS glass; Android `surface2`. Optional leading icon (`spectating`).
- Content: `reveal_player_of`, `round_category_pill`, "Runda 2 · glasanje", `result_pill`, `vote_progress`, "Soba K7QX".

---

## 3 · Card & game layer (Shared, Compose)

### RoleCard — Shared
```kotlin
RoleCard(face = Back | Citizen | Spy | SpyWithOthers | KnockedOut,
         name, word, category, others: List<String>, flipped, onFlip)
```
- Size 300×420 in reveal (scales: 164×226 home hero, 150×210 result, 120×165 game-over fan, 60×84 seat). Shape `SpyShape.card()` = `CutCornerShape(22)` (scale cut with size: 4 at 60 wide). Inner frame `CutCornerShape(14)` inset 12, 1–1.5 pt stroke.
- **Back:** `card.back` #2A221C + lattice (two 45°/−45° repeating 1.5 px lines every 12, vermilion @22–25 %), centre medallion 62 circle `card.back` with 1.5 vermilion ring + "Š" 34/800 vermilion; stroke `card.backStroke`. **On the table (seats), dark theme:** fill `card.seatBack` #33291F, stroke `card.seatBackStroke` (ivory @ 34 %), lattice `card.seatBackLattice` (vermilion @ 30 %); light theme seats keep `card.back`.
- **Citizen:** `card.citizenFace` ivory; frame stroke `citizenInk` @45 %; whole diamond 40×54 `citizenInk`; label `reveal_secret_word` (12/700 +12 % `#6B645C`); word `wordDisplay` (auto-fit 56→40, 2 lines, break between words only) `citizenText`; dashed divider; "Kategorija · **Mjesta**" 15 (muted 5.0:1 on ivory).
- **Spy:** `card.spyFace` #0F0D0B, frame stroke vermilion @55 %; split diamond 40×54 vermilion; label `reveal_you_are` (`#A39C94`); "Špijun" `roleDisplay` 64 ivory; category line with category in vermilion.
- **Spy + others:** + dashed divider + `reveal_other_spies` label + name chips (radius full, fill `#26221E`, split glyph 10×14 vermilion + name 17/700). Up to 11 names; above 3 → 2-column wrap.
- **Knocked out (face-down):** back + stamp `result_out_stamp` "IZVAN IGRE" (2 pt ivory frame radius 6, 16/800 +10 %, rotated −8°) + name. No role, no word until game over.
- Card faces are **theme-independent**. `cardShadow` only while lifted/dealt.
- Haptics: flip to citizen `RevealCitizen`; to spy `RevealSpy` (at the 90° face swap); hide `HideCard`.
- A11y: announce once on flip (`a11y_citizen`, `a11y_spy`, `a11y_other_spies` read last); hidden card `a11y_card_hidden`; after hide `a11y_role_hidden`. Never re-read on focus. The whole card is the flip target (48+).

### SpyTable — Shared
```kotlin
SpyTable(seats, currentSeat, camera = Overview | Focus,
         tension = Calm | Uneasy | Nervous | Frozen, onSeatTap)
```
- Disc radius 172, fill `table` (#221E1A dark, one step above the background), 1.5 px `tableRing`. With a timer a 3 px **time arc** runs round the rim from 12 o'clock: `text` @ 75 % draining clockwise with the time left, `warningText` at ≤30 s, easing one second at a time; without a timer the ring itself turns `warning` at tension. Seats on radius 128, 60×84 cards rotated 45°·i to face the centre. Names 12/700 on a plate (`card.spyFace` @ 86 %, radius 8, max one card wide, ellipsis) centred on the card that turns against the table so it stays upright; knocked-out and lifted seats show the name without a plate. Up to 12 seats (angle = 360/n).
- Camera: `graphicsLayer { rotationZ = −(360/n)·seat; scale = 1.3 (Focus) }`, origin = table centre; non-current seats α 0.4.
- Seat states: face-down · current (lifted out → RoleCard) · looked (own-phone: small neutral tick) · knocked-out (empty dashed outline + name) · dropped (dashed `warning`) · selectable (out-loud host: tap lifts 1.2× with 2 pt ivory outline) · game-over (flip face-up in place, spies first).
- Centre slot: countdown (68 sp) / "Razgovarajte" / "Vrijeme!" (title 24, one line) / "pauzirano". The free circle inside the seats is about 170 wide, so sub-lines are 13 sp and at most 130 wide.
- A11y: seats exposed as a list in seat order. Reduce Motion: no turn/drift; current seat gets an ivory ring.

### Countdown — Shared
```kotlin
Countdown(remaining, total, running, style = Center | Compact)
```
- Center: label "RUNDA N" + digits `countdown` (Hanken 700 tabular; 96 spec, fits the 200 pt table centre) + sub-line.
- States: running (`text`) · ≤30 s `warning` + "Još malo!" · ≤10 s pulse 1.0→1.07 per second · ended "Vrijeme!" (`warning`, 2 edge flashes) → vote opens · off ("Razgovarajte" + "bez timera").
- Digit change: 120 ms vertical roll (`AnimatedContent`), no width jump.
- Host-only controls next to it: **+1 min** (small secondary) and **Završi raspravu** (primary). Guests: synced from server clock, no controls.
- A11y: `role=timer`; polite announcements at each full minute, 30 s, 10 s, 0 — never every second; on focus "Preostalo 4 minute i 32 sekunde".

### VoteOption — Shared
```kotlin
VoteOption(player, selected, enabled, onSelect)
```
- Grid of 2 columns, cell 68 tall, radius 18, `surface`; avatar 32 + name 18/700.
- States: default · selected (1.5 px `text` border + inverted avatar/check) · self (dashed border, "ti", disabled) · out (dashed, strikethrough, "izvan igre", disabled — or omitted).
- A11y: `a11y_vote_selected`, `a11y_vote_self`; disabled reason read aloud. Confirm locks the vote.

### VoteProgress — Shared
```kotlin
VoteProgress(cast, total, notYet: List<Name>)
```
- Success ring 88 + `vote_cast`; headline `plural_waiting_players`; segment bar (one per voter, 6 tall, radius full, `success` / `disabledFill`, gap 6); `vote_not_yet` panel with name chips (dropped in `warningText`).
- Shows **who hasn't voted — never who voted for whom.** States: waiting · all in → result.

### TallyBars — Shared
```kotlin
TallyBars(rows: List<Pair<Name, Int>>)
```
- Panel `surface` radius 22; header `result_votes` + total. Row: name 72 wide · bar (track `surface2`, height 22, radius 6; winner `primary`, others `disabledFill`) · count tabular.
- States: winner · tie (equal full bars) · revote. A11y: list "Ana, 4 glasa".

### ProgressPips — Shared
```kotlin
ProgressPips(total, current, skipped)
```
- Done (filled `text`) · current (lifted, ringed) · upcoming (`disabledFill`) · skipped/eliminated. Hidden from a11y (the pill text carries "Igrač 3 od 8").

### Round indicator — Shared
- "RUNDA 2" label + "6 u igri · 2 izbačena" (`plural_in_game` + proposed `plural_out_count`) + category pill.

### Glyphs — Shared
`assets/glyphs/`: citizen (whole diamond), spy (split diamond = brand mark), eliminated (hollow + strike, 5 pt stroke), host-dealer "D", hidden-role (mini card back); `assets/icons/`: voted (check circle, `success`), dropped (dashed circle, `warning`), timer, timer-off. Convert SVG → `ImageVector` (e.g. via Valkyrie / svg-to-compose).

---

## 4 · Component inventory (quick reference)

| Component | Owner | Screens |
|---|---|---|
| Tab bar | Native iOS · Native Android | Home, Dictionaries, Settings |
| Navigation bar / top app bar | Native iOS · Native Android | all non-game screens |
| Glass button / icon button | Native iOS · Native Android | all |
| Sheet | Native iOS · Native Android (+ Shared content) | mode, create dict, export, PIN, join errors, connection |
| Confirm dialog | Native iOS · Native Android | delete dict, quit, remove player |
| Share / file pick | Native iOS · Native Android | export, import |
| QR scanner | Native iOS · Native Android · Shared overlay | join, import |
| SpyButton | Shared | all |
| SpyStepper | Shared | setup |
| SpyChoiceRow (chips/segmented) | Shared | setup, settings, create dict |
| SpySwitchRow | Shared | settings |
| SpyTextField | Shared | join nickname, words, create dict |
| RoomCodeInput | Shared | join |
| RoomCodeCard | Shared | lobby |
| CategoryRow | Shared | setup |
| DictionaryRow | Shared (+native swipe) | dictionaries |
| PlayerRow | Shared | lobby, vote progress |
| Banner / Toast | Shared | setup offline, connection, words |
| EmptyState | Shared | dictionaries |
| Loading skeleton | Shared | setup, dictionaries |
| Pill | Shared | in-game header |
| RoleCard | Shared | reveal, vote (pass), result, game over, home hero |
| SpyTable | Shared | reveal, round, vote (pass), connection |
| Countdown | Shared | round |
| VoteOption / VoteProgress / TallyBars | Shared | vote, result |
| ProgressPips / Round indicator | Shared | reveal, round |
