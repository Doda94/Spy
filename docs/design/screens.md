# Špijun — Screens

Every P1–P3 screen and state, rendered from the design canvas for **iOS 26 (390×844 pt)** and **Android 16 (412×915 dp)**, **dark and light**. PNGs are @2x (780×1688 / 824×1830 px). Copy in the mock-ups is Croatian (the stress-test language); English is in `copy.json`.

- File naming: `screens/<tier>-<nn>-<screen>/<state-letter>-<state>__<ios|android>-<dark|light>.png`
- Full annotated boards (with the designer's state titles) are in `frames/` — e.g. `frames/P3-04-Reveal.png`. Raw source per board: `source/<board>.dc.html`.
- "Copy keys" lists are auto-matched from the iOS-dark mock text against `copy.json` (names, words and sample data are not keys). Keys starting `plural_` use `pluralStringResource`.
- Tokens are referenced by their `tokens.json` names (`text`, `textMuted`, `surface`, `surface2`, `line`, `primary`, `spy`, `citizen`, `warning`, `danger`, `success`, `eliminated`, `table`, `tableRing`, `cardShadow`…).

## Global layout rules (all screens)

| | iOS 26 | Android 16 (M3 Expressive) |
|---|---|---|
| Frame | 390 × 844 pt; top safe area 58 (never draw a fake status bar), bottom 34 | 412 × 915 dp; edge-to-edge, `WindowInsets`; gesture inset 24 |
| Side gutter | 20–24 | 20–24 |
| Top row (nav) | glass buttons 48 at y≈68 (native SwiftUI toolbar, Liquid Glass) | 64 dp app bar, `IconButton` 48 |
| Large title | 34/40 Bricolage 800, collapses to inline 17/600 on scroll | `LargeTopAppBar` 30 → 22/600 |
| Primary button | 56, capsule (radius full) | 64, radius 16 → morphs to full on press |
| Small button | 52, radius 26 | 52, radius 14 |
| Bottom of content | primary action sits in the bottom third, bottom inset 34 | bottom inset 24 |
| Tab bar | floating Liquid Glass capsule 64 tall, inset 24 sides / 22 bottom (native `TabView`); content scrolls under it | `NavigationBar` 88 (incl. 8 bottom pad), neutral indicator 56×32 |
| Sheets | `.sheet` medium/large detents, floating (inset 8, radius 34 top/40 bottom) | `ModalBottomSheet`, radius 28 top, drag handle |
| Scrim | 60 % black (dark) / 35 % ink (light) | same |
| Glass | chrome only (tab bar, nav/toolbar, floating buttons, sheets) — never content | none — tonal surfaces (`surface2`) |

- **One primary action per screen**, always at the thumb. Everything else is Secondary (`surface2` fill) or a text button.
- **Only cards cast shadows.** Panels are tone-on-tone (`surface` on `bg`), radius 22; sheets/hero panels radius 28.
- **Sticky footers** (setup, lobby, vote…) have a 1 px `line` hairline on top, a 15 `textMuted` status line, then the button. A disabled reason replaces the status line (in `warningText` or `danger`).
- In-game screens (Reveal → Game over) are **immersive**: no tab bar, no title; top row = close ✕ (→ quit confirm) + centre pill (40 tall, 15/600; iOS glass, Android `surface2`).
- Min touch target 48 pt/dp during play; 8 between neighbours.
- Text scales to 200 % (rows reflow, steppers drop under labels, chip rows wrap); display styles cap at 1.3×.

## Navigation map

```
Tab bar (top level):  Igraj (Play) | Rječnici (Dictionaries) | Postavke (Settings)

Igraj ─ Home ─┬─ Nova igra ─▶ Mode sheet ─┬─ Jedan telefon ─▶ Setup ─▶ [GAME]
              │                           └─ Svaki na svom ─▶ Setup (host) ─▶ Lobby (host) ─▶ [GAME]
              │                                    └─ offline ─▶ "Nema interneta" sheet
              ├─ Uđi s kodom ─▶ Join: code ─▶ nickname ─▶ Lobby (guest) ─▶ [GAME]
              │       └─ Skeniraj QR ─▶ Scanner (room or dictionary payload)
              └─ Nastavi igru (unfinished game card)
Room link / scanned room QR ─▶ Join with code pre-filled
Dictionary link / file / scanned dictionary QR ─▶ Import preview

[GAME] (immersive, tab bar hidden):
  Reveal (pass-the-phone or own phone) ─▶ Round/discussion ─▶ Vote (in app) │ Host marks (out loud)
      ─▶ Round result ─▶ next Round … ─▶ Game over ─▶ Igraj ponovno (same settings) │ Nova igra
  ✕ anywhere ─▶ "Prekinuti igru?" confirm

Rječnici ─ List ─▶ Dictionary ─▶ Category words;  ⤓ Import;  + New;  ⇪ Export sheet
Postavke ─ Settings ─▶ Reveal options; Admin PIN sheet (last row of "O aplikaciji")
```


---

## P1 · 01 · Home

Board: `frames/P3-01-Home.png` · source `source/P3-01-Home.dc.html`

**Where:** Play tab (tab bar visible). First screen on launch.

**Layout (iOS 390×844 / Android 412×915)**
- Wordmark block top-left: brand glyph (split diamond, `spy` colour, 26×36) + "Špijun" `titleLarge`-like 46/46 Bricolage 800 −3%; tagline `bodyLarge` 18/25 `textMuted`, max-width 300. Top inset 72 iOS / 56 Android, side gutter 24.
- Hero (state a only): a fanned stack of three cards centred in the remaining space — card back (150×206, −13°), citizen face "Bolnica" (150×206, +11°), spy face "Špijun" (164×226, −1°, front). Cards cast `cardShadow`. Decorative: `aria-hidden`.
- Bottom stack (gap 10): **Nova igra** = `SpyButton.Primary`; **Uđi s kodom** = `SpyButton.Secondary` with `join-code` icon. Heights 56 (iOS capsule) / 64 (Android radius 16). Bottom space reserves the tab bar (104).
- Tab bar: iOS floating Liquid Glass capsule (native `TabView`), 64 tall, inset 24/22; Android M3 `NavigationBar` 88 with neutral 56×32 indicator. Items: Igraj · Rječnici · Postavke.

**State b · unfinished game:** hero replaced by a `surface` panel (radius 28, 1 px `line` ring, padding 22): mini card-stack glyph, label `home_continue_label`, title "Runda 2 · 6 od 8 u igri" (`headline`), meta "Jedan telefon · prije 4 min" (`textMuted`), then row: **Nastavi igru** (primary, flex 1, height 52) + **Završi** (text button). Both bottom buttons become Secondary (only one primary per screen). Never shows role or word.

**State c · mode chooser:** iOS `.sheet` medium detent (floating, inset 8, radius 34 top / 40 bottom, glass fallback `sheetGlass`); Android `ModalBottomSheet` (radius 28 top). Scrim 60 % / 35 %. Content: drag handle 36×5, title `mode_title` 28 Bricolage 800 + close (44 circle, `surface2`); two option buttons (radius 22, `optionFill`, padding 18, 56×56 icon tile radius 16 `surface2`, title 19/700, desc 15 `textMuted`, chevron); footnote `mode_note` 13.
- "Svaki na svom telefonu" when offline → P2·12 e (offline sheet).


### Home — a · Default

_first open, no game in progress_

| iOS dark | iOS light | Android dark | Android light |
|---|---|---|---|
| <img src="screens/p1-01-home/a-default__ios-dark.png" width="170"> | <img src="screens/p1-01-home/a-default__ios-light.png" width="170"> | <img src="screens/p1-01-home/a-default__android-dark.png" width="170"> | <img src="screens/p1-01-home/a-default__android-light.png" width="170"> |

Copy keys: `reveal_spy` · `home_tagline` · `home_new_game` · `home_join` · `tab_play` · `tab_dictionaries` · `tab_settings`


### Home — b · Unfinished game

_app was closed mid-game; nothing secret is shown here_

| iOS dark | iOS light | Android dark | Android light |
|---|---|---|---|
| <img src="screens/p1-01-home/b-unfinished-game__ios-dark.png" width="170"> | <img src="screens/p1-01-home/b-unfinished-game__ios-light.png" width="170"> | <img src="screens/p1-01-home/b-unfinished-game__android-dark.png" width="170"> | <img src="screens/p1-01-home/b-unfinished-game__android-light.png" width="170"> |

Copy keys: `reveal_spy` · `home_continue_label` · `plural_in_game` · `setup_timer_minutes` · `home_continue` · `home_continue_end` · `home_new_game` · `home_join` · `tab_play` · `tab_dictionaries` · `tab_settings`


### Home — c · Mode chooser

_sheet after "Nova igra" · iOS glass sheet (medium detent) · Android modal bottom sheet_

| iOS dark | iOS light | Android dark | Android light |
|---|---|---|---|
| <img src="screens/p1-01-home/c-mode-chooser__ios-dark.png" width="170"> | <img src="screens/p1-01-home/c-mode-chooser__ios-light.png" width="170"> | <img src="screens/p1-01-home/c-mode-chooser__android-dark.png" width="170"> | <img src="screens/p1-01-home/c-mode-chooser__android-light.png" width="170"> |

Copy keys: `reveal_spy` · `home_tagline` · `mode_title` · `mode_one_phone` · `mode_one_phone_desc` · `mode_multi` · `mode_multi_desc` · `mode_note`


---

## P1 · 02 · Game setup

Board: `frames/P3-02-Setup.png` · source `source/P3-02-Setup.dc.html`

**Where:** pushed from the mode chooser (back returns to Home). Drawn without the tab bar (pushed screen). The game itself — immersive, no tab bar — starts at **Započni igru** / **Otvori sobu**.

**Layout:** one scrolling screen; **sticky footer** never scrolls.
- Nav: back (iOS glass button 48 / Android `IconButton` arrow-back), large title `setup_title` (iOS large title 34/40 → inline 17/600 on scroll; Android `LargeTopAppBar` 30 → 22/600).
- Mode chip under the title (`surface2`, height 32, radius full, `mode-chip-phone` icon + mode name + chevron-down) → reopens the mode sheet.
- Panel 1 (`surface`, radius `lg` 22): **Igrači** stepper row (60 tall, buttons 48, value 24/700 tabular) · **Imena igrača** row (one-phone only; `setup_player_names_hint`; trailing "8 od 8" in `success`, `warningText` if missing; chevron → names sheet) · **Špijuni** stepper with hint `setup_spies_max` ("najviše N").
  - Host / own-phone (state b): Players row is read-only: "ti + ostali" + hint `setup_players_from_room`; spies hint "provjera kad svi uđu" (max is validated when the room fills).
- `TIMER PO RUNDI` label + `SpyChoiceRow` chips (48 high, radius full): Bez · 3 min · 5 min · 8 min · Svoje… (custom = dashed outline; opens wheel picker sheet iOS / time-input dialog Android). Selected = `primary` fill + bold (+ check where room allows). Own-phone adds `setup_timer_synced`.
- `GLASANJE` label + segmented control (2 options, track `surface`, selected thumb `primary`). Hint below: in-app → secret-vote hint.
- `RJEČNICI` chips (multi-select, check icon when on) → `KATEGORIJE` with **Odaberi sve** (text button, right). Categories grouped per dictionary with sub-label "Hrvatski · ugrađen" / "Party pack · vlastiti". `CategoryRow` 55 tall: checkbox 22 (radius 6) + name `bodyLarge` + count right `textMuted` tabular.
- Footer (hairline top `line`, bg = `bg`): line 1 `plural_words_in_selection` (+ " · N kategorije" when scrolled into categories) 15 `textMuted` centred; own-phone adds 🔒-icon line `setup_privacy_hint` 13; then **Započni igru** / **Otvori sobu** primary.

**States**
- d · loading: chips and category rows as skeleton blocks (`surface2`, pulse — no shimmer under Reduce Motion), footer shows spinner + `setup_loading`, Start disabled. Only on first launch; afterwards cached data shows instantly and refreshes silently.
- e · nothing selected: category panel gets 1.5 px `warning` ring; footer reason `setup_none_selected` in `warningText`; Start disabled (`disabledFill`, text `textMuted`).
- f · empty pool: categories show count **0** in `danger`; inline card (`surface`, radius 22) with error icon + "Ove kategorije još nemaju riječi." + text button **Dodaj riječi** (→ dictionary); footer `setup_empty_pool` in `danger`; Start disabled.
- g · offline: `warning`-fill banner at top (radius 22, `offline` icon, bold "Nema veze sa serverom." + rest, ink `onWarning`); custom + cached built-ins remain selectable; own-phone mode unavailable.
- Disabled-reason text **replaces** the word count in the footer — never both.


### Game setup — a · One phone · top of screen

_voting in the app, so names are required_

| iOS dark | iOS light | Android dark | Android light |
|---|---|---|---|
| <img src="screens/p1-02-setup/a-one-phone-top-of-screen__ios-dark.png" width="170"> | <img src="screens/p1-02-setup/a-one-phone-top-of-screen__ios-light.png" width="170"> | <img src="screens/p1-02-setup/a-one-phone-top-of-screen__android-dark.png" width="170"> | <img src="screens/p1-02-setup/a-one-phone-top-of-screen__android-light.png" width="170"> |

Copy keys: `setup_title` · `mode_one_phone` · `setup_players` · `setup_player_names` · `setup_player_names_hint` · `setup_spies` · `setup_spies_max` · `setup_timer` · `setup_timer_off` · `setup_timer_minutes` · `setup_timer_custom` · `setup_voting` · `setup_vote_app` · `setup_vote_loud` · `setup_vote_app_hint_one_phone` · `setup_dicts_and_categories` · `plural_words_in_selection` · `setup_start`


### Game setup — b · Host · each on own phone

_players come from the room; nicknames replace names_

| iOS dark | iOS light | Android dark | Android light |
|---|---|---|---|
| <img src="screens/p1-02-setup/b-host-each-on-own-phone__ios-dark.png" width="170"> | <img src="screens/p1-02-setup/b-host-each-on-own-phone__ios-light.png" width="170"> | <img src="screens/p1-02-setup/b-host-each-on-own-phone__android-dark.png" width="170"> | <img src="screens/p1-02-setup/b-host-each-on-own-phone__android-light.png" width="170"> |

Copy keys: `setup_title` · `mode_multi` · `setup_players` · `setup_players_from_room` · `setup_players_you_plus` · `setup_spies` · `setup_names_checked_later` · `setup_timer` · `setup_timer_off` · `setup_timer_minutes` · `setup_timer_custom` · `setup_timer_synced` · `setup_voting` · `setup_vote_app` · `setup_vote_loud` · `plural_words_in_selection` · `setup_privacy_hint` · `setup_open_room`


### Game setup — c · Dictionaries & categories

_scrolled; title collapses into the bar; pick dictionaries, then categories with counts_

| iOS dark | iOS light | Android dark | Android light |
|---|---|---|---|
| <img src="screens/p1-02-setup/c-dictionaries-categories__ios-dark.png" width="170"> | <img src="screens/p1-02-setup/c-dictionaries-categories__ios-light.png" width="170"> | <img src="screens/p1-02-setup/c-dictionaries-categories__android-dark.png" width="170"> | <img src="screens/p1-02-setup/c-dictionaries-categories__android-light.png" width="170"> |

Copy keys: `setup_dictionaries` · `setup_categories` · `setup_select_all` · `setup_dict_builtin_inline` · `setup_dict_custom_inline` · `setup_title` · `plural_categories` · `setup_start`


### Game setup — d · Loading dictionaries

_first launch only; afterwards the cached copy shows instantly and refreshes quietly_

| iOS dark | iOS light | Android dark | Android light |
|---|---|---|---|
| <img src="screens/p1-02-setup/d-loading-dictionaries__ios-dark.png" width="170"> | <img src="screens/p1-02-setup/d-loading-dictionaries__ios-light.png" width="170"> | <img src="screens/p1-02-setup/d-loading-dictionaries__android-dark.png" width="170"> | <img src="screens/p1-02-setup/d-loading-dictionaries__android-light.png" width="170"> |

Copy keys: `setup_dictionaries` · `setup_categories` · `setup_title` · `setup_loading` · `setup_start`


### Game setup — e · Nothing selected

_all categories off; Start disabled with the reason in the footer_

| iOS dark | iOS light | Android dark | Android light |
|---|---|---|---|
| <img src="screens/p1-02-setup/e-nothing-selected__ios-dark.png" width="170"> | <img src="screens/p1-02-setup/e-nothing-selected__ios-light.png" width="170"> | <img src="screens/p1-02-setup/e-nothing-selected__android-dark.png" width="170"> | <img src="screens/p1-02-setup/e-nothing-selected__android-light.png" width="170"> |

Copy keys: `setup_dictionaries` · `setup_categories` · `setup_select_all` · `setup_title` · `setup_none_selected` · `setup_start`


### Game setup — f · Empty pool

_only empty custom categories chosen; links to the dictionary to add words_

| iOS dark | iOS light | Android dark | Android light |
|---|---|---|---|
| <img src="screens/p1-02-setup/f-empty-pool__ios-dark.png" width="170"> | <img src="screens/p1-02-setup/f-empty-pool__ios-light.png" width="170"> | <img src="screens/p1-02-setup/f-empty-pool__android-dark.png" width="170"> | <img src="screens/p1-02-setup/f-empty-pool__android-light.png" width="170"> |

Copy keys: `setup_dictionaries` · `setup_categories_of` · `setup_empty_pool_body` · `setup_add_words` · `setup_title` · `setup_empty_pool` · `setup_start`


### Game setup — g · Offline

_one-phone still works from the cache; "each on own phone" is unavailable_

| iOS dark | iOS light | Android dark | Android light |
|---|---|---|---|
| <img src="screens/p1-02-setup/g-offline__ios-dark.png" width="170"> | <img src="screens/p1-02-setup/g-offline__ios-light.png" width="170"> | <img src="screens/p1-02-setup/g-offline__android-dark.png" width="170"> | <img src="screens/p1-02-setup/g-offline__android-light.png" width="170"> |

Copy keys: `setup_offline_dated` · `setup_dictionaries` · `setup_categories` · `setup_title` · `plural_words_in_selection` · `setup_start`


---

## P1 · 03 · Lobby & join

Board: `frames/P3-03-Lobby.png` · source `source/P3-03-Lobby.dc.html`

**Where:** own-phone mode. Host arrives after **Otvori sobu**; guests via **Uđi s kodom**, a room link, or a scanned QR (code pre-filled).

**Host lobby (a, b)**
- Nav: close ✕ (glass 48) · inline title `lobby_title` "Soba" · trailing `game-settings` button (→ edit setup).
- `RoomCodeCard`: ivory `card.roomCodeCard` in BOTH themes, radius 28, padding 16; left a real QR (≥124 pt incl. quiet zone, on white; the mock checkerboard is a placeholder); right `lobby_code_label` (label, `#6B645C`) + code "K7QX" 44/800 Hanken tabular, +4 % tracking, ink `#1A1714`; **Podijeli** = small dark pill (ink fill, ivory text, `share` icon) → system share sheet with the room link.
- `IGRAČI` label + "4 od 12" (`lobby_players_count`) right.
- Player list panel (`surface`, radius 22): `PlayerRow` 60 tall — avatar circle 32 (`surface2`, initial 14/700), name `bodyLarge` (+ "(ti)" `textMuted`), sub-status line: "Domaćin" / ● "Na vezi" (`success`) / ◌ "Povezuje se…" (`warningText`, dashed ring icon); trailing: host gets the "D" dealer chip (ivory circle 26), others a ⋯ button (host only) → remove player (destructive confirm).
- Rules summary line (13 `textMuted`): spies · timer · voting · words + **Uredi** text button.
- Footer: `lobby_ready` hint + **Započni igru · N igrača** (`plural_start_with_players`).
- b · below minimum (min 3): empty slot row with dashed border (radius 22, "+" circle, "Čeka se igrač…"); footer `plural_need_more_players` in `warningText`; Start disabled.

**Guest (c):** close ✕, room code pill top-right (`K7QX` + share icon, `surface2`). Large title `lobby_in_room` "U sobi si" + subtitle "Soba: Luka · igraš kao **Iva**" (see copy review — avoid "Lukina"). Read-only player list (no ⋯), own row avatar inverted (ivory). Rules card (1 px `line` outline, radius 22): `lobby_rules` + summary. Waiting line: 3-dot typing indicator + `lobby_waiting_host`. Footer: **Napusti sobu** (secondary).

**Join (d–g)**
- d · enter code: back, large title `join_title`, subtitle `join_code_hint`. `RoomCodeInput` = 4 cells (each ~80×76, radius 16, `surface`, glyph 36/700 uppercase); focused cell 1.5 px `text` border + caret; alphabet A–Z 2–9 without 0/O/1/I; auto-uppercase; paste & links fill it. **Dalje** primary (disabled until 4 chars). Divider "ili". **Skeniraj QR kod** secondary with `scan` icon. System keyboard opens automatically (not drawn).
- e · nickname: large title `join_nick_title`, subtitle "Soba K7QX · domaćin Luka · 3 igrača"; `SpyTextField` label `join_nick_label`, counter "3 / 16" (max 16), hint `join_nick_hint`; **Uđi u sobu** primary. Nickname remembered locally.
- f · not found: all 4 cells get 1.5 px `danger` border + `danger` text; error line with `error` icon `join_err_not_found`; shake ×3 (300 ms) + `Error` haptic; typing again clears.
- g · started / full: bottom sheet over the dimmed join screen: lock icon in 56 circle, title `join_err_started_title` (or `join_err_full_title`), body, primary **Čekaj sljedeću igru**, text **Natrag**. Full: "Soba je puna · 12 od 12 igrača."


### Lobby & join — a · Host lobby · ready

_code + QR + share, live player list, ⋯ to remove a player_

| iOS dark | iOS light | Android dark | Android light |
|---|---|---|---|
| <img src="screens/p1-03-lobby-join/a-host-lobby-ready__ios-dark.png" width="170"> | <img src="screens/p1-03-lobby-join/a-host-lobby-ready__ios-light.png" width="170"> | <img src="screens/p1-03-lobby-join/a-host-lobby-ready__android-dark.png" width="170"> | <img src="screens/p1-03-lobby-join/a-host-lobby-ready__android-light.png" width="170"> |

Copy keys: `lobby_title` · `lobby_code_label` · `lobby_share` · `lobby_players_label` · `lobby_host` · `lobby_connected` · `lobby_connecting` · `plural_words` · `lobby_edit` · `lobby_ready` · `plural_start_with_players`


### Lobby & join — b · Host lobby · below minimum

_needs 3 players; empty slots invite more_

| iOS dark | iOS light | Android dark | Android light |
|---|---|---|---|
| <img src="screens/p1-03-lobby-join/b-host-lobby-below-minimum__ios-dark.png" width="170"> | <img src="screens/p1-03-lobby-join/b-host-lobby-below-minimum__ios-light.png" width="170"> | <img src="screens/p1-03-lobby-join/b-host-lobby-below-minimum__android-dark.png" width="170"> | <img src="screens/p1-03-lobby-join/b-host-lobby-below-minimum__android-light.png" width="170"> |

Copy keys: `lobby_title` · `lobby_code_label` · `lobby_share` · `lobby_players_label` · `lobby_host` · `lobby_connected` · `plural_need_more_players` · `setup_start`


### Lobby & join — c · Guest · waiting for host

_joined as Iva; read-only settings; can still invite with the code_

| iOS dark | iOS light | Android dark | Android light |
|---|---|---|---|
| <img src="screens/p1-03-lobby-join/c-guest-waiting-for-host__ios-dark.png" width="170"> | <img src="screens/p1-03-lobby-join/c-guest-waiting-for-host__ios-light.png" width="170"> | <img src="screens/p1-03-lobby-join/c-guest-waiting-for-host__android-dark.png" width="170"> | <img src="screens/p1-03-lobby-join/c-guest-waiting-for-host__android-light.png" width="170"> |

Copy keys: `lobby_in_room` · `lobby_rules` · `lobby_waiting_host` · `lobby_leave`


### Lobby & join — d · Join · enter code

_keyboard opens automatically (system keyboard not drawn); paste and room links fill it in_

| iOS dark | iOS light | Android dark | Android light |
|---|---|---|---|
| <img src="screens/p1-03-lobby-join/d-join-enter-code__ios-dark.png" width="170"> | <img src="screens/p1-03-lobby-join/d-join-enter-code__ios-light.png" width="170"> | <img src="screens/p1-03-lobby-join/d-join-enter-code__android-dark.png" width="170"> | <img src="screens/p1-03-lobby-join/d-join-enter-code__android-light.png" width="170"> |

Copy keys: `join_title` · `join_code_hint` · `join_next` · `join_or` · `join_scan`


### Lobby & join — e · Join · nickname

_remembered on this phone for next time, never sent anywhere else_

| iOS dark | iOS light | Android dark | Android light |
|---|---|---|---|
| <img src="screens/p1-03-lobby-join/e-join-nickname__ios-dark.png" width="170"> | <img src="screens/p1-03-lobby-join/e-join-nickname__ios-light.png" width="170"> | <img src="screens/p1-03-lobby-join/e-join-nickname__android-dark.png" width="170"> | <img src="screens/p1-03-lobby-join/e-join-nickname__android-light.png" width="170"> |

Copy keys: `join_nick_title` · `plural_players` · `join_nick_label` · `join_nick_hint` · `join_title`


### Lobby & join — f · Join · room not found

_cells shake 3×, error haptic; typing again clears it_

| iOS dark | iOS light | Android dark | Android light |
|---|---|---|---|
| <img src="screens/p1-03-lobby-join/f-join-room-not-found__ios-dark.png" width="170"> | <img src="screens/p1-03-lobby-join/f-join-room-not-found__ios-light.png" width="170"> | <img src="screens/p1-03-lobby-join/f-join-room-not-found__android-dark.png" width="170"> | <img src="screens/p1-03-lobby-join/f-join-room-not-found__android-light.png" width="170"> |

Copy keys: `join_title` · `join_code_hint` · `join_err_not_found` · `join_next` · `join_scan`


### Lobby & join — g · Join · game started / room full

_same sheet, two messages; full: "Soba je puna · 12 od 12 igrača."_

| iOS dark | iOS light | Android dark | Android light |
|---|---|---|---|
| <img src="screens/p1-03-lobby-join/g-join-game-started-room-full__ios-dark.png" width="170"> | <img src="screens/p1-03-lobby-join/g-join-game-started-room-full__ios-light.png" width="170"> | <img src="screens/p1-03-lobby-join/g-join-game-started-room-full__android-dark.png" width="170"> | <img src="screens/p1-03-lobby-join/g-join-game-started-room-full__android-light.png" width="170"> |

Copy keys: `join_nick_title` · `join_err_started_title` · `join_err_started_body` · `join_wait_next` · `common_back`


---

## P1 · 04 · Role reveal

Board: `frames/P3-04-Reveal.png` · source `source/P3-04-Reveal.dc.html`

**Chosen mechanic: tap-to-flip on the table** (Settings offers hold-to-peek as the alternative). Auto-hide 8 s default (5 / 8 / off). See motion.md for the choreography.

**Layout (from annotated board H·03)**
- Immersive: no tab bar, no title. Top row (y≈68): close ✕ (iOS glass 48 / Android `IconButton`) → `quit_title` confirm; centre pill 40 tall (`reveal_player_of`, 15/600; iOS glass, Android `surface2` tonal).
- `SpyTable` (Shared): table disc radius 172 (`table` fill, 1.5 px `tableRing`), 8 seat cards 56×78 (CutCorner 5) on radius 130, each rotated to face the centre (45°·i). Camera: `rotationZ = −45°·seatIndex`, scale 1.3 when focused; non-current seats α 0.4. Table centre ≈ (195, 430) iOS / (206, 470) Android.
- `RoleCard` 300×420 (CutCorner 22, inner frame cut 14 inset 12), centred horizontally, top 124 iOS / 132 Android, `cardShadow` while lifted. Pixel-identical on both platforms.
- Bottom: hint/timer line 15 `textMuted` ("Luka, dodirni kartu." / "Sakriva se za 6 s"), then primary button 56 capsule (iOS) / 64 radius 16 (Android) at bottom inset 34 / 24. While face-up, a 4 pt bar along the button's bottom edge empties linearly over the auto-hide time.

**Faces**
- a · handoff (face down): card back (`card.back` + vermilion lattice, centre medallion "Š" in 62 circle, 1.5 px vermilion ring) with name label; copy `reveal_pass_to` → `reveal_tap_card` + `reveal_nobody_look`; button **Prikaži ulogu** (the whole card is also the tap target).
- b · citizen: ivory face, whole-diamond glyph (40×54, `citizenInk`), `reveal_secret_word` label, word in `wordDisplay` (auto-fit 56→40, 2 lines max, e.g. "Svemirska postaja"), dashed hairline, "Kategorija · **Mjesta**" 15 (muted on ivory 5.0:1). Haptic `RevealCitizen`.
- c · spy alone: spy face (`card.spyFace`, vermilion inner frame @55 %), split-diamond glyph (vermilion), `reveal_you_are` label, "Špijun" `roleDisplay` 64, category line with the category in `spy` colour, body `reveal_spy_alone`. Haptic `RevealSpy` (double thud). Dark face in BOTH themes.
- d · spy with others: as c + dashed divider + `reveal_other_spies` label + name chips (pill, `surface2`-on-dark `#26221E`, split-glyph 10×14 + name 17/700). Up to 11 names; >3 → 2-column wrap. VO reads names last.
- Buttons: `reveal_hide_next` "Sakrij i dalje"; last player `reveal_hide_start` "Sakrij i započni raspravu".
- e · own phone (multi-device): no table turn — your seat is always at the bottom, ringed; camera pushes in on your own card only. After viewing: card back in place, pill "Soba K7QX", counter "N / M pogledalo kartu" (`reveal_seen_count` + `reveal_seen_label`), `plural_waiting_players`, hint "Tvoja karta je dolje…" (proposed key `reveal_own_card_hint`), button **Pogledaj ponovno** (`reveal_peek_again`). Other seats show a small neutral tick when that player has looked — never their role.

**Privacy:** on app background every face-up card flips back instantly (no animation); iOS blurs the app-switcher snapshot, Android sets `FLAG_SECURE` during reveal. Toasts are paused during a reveal. With a screen reader on, first reveal asks `a11y_sr_warning` once.


### Role reveal — a · Handoff · card lifted, face down

_one phone · nothing secret on screen yet_

| iOS dark | iOS light | Android dark | Android light |
|---|---|---|---|
| <img src="screens/p1-04-reveal/a-handoff-card-lifted-face-down__ios-dark.png" width="170"> | <img src="screens/p1-04-reveal/a-handoff-card-lifted-face-down__ios-light.png" width="170"> | <img src="screens/p1-04-reveal/a-handoff-card-lifted-face-down__android-dark.png" width="170"> | <img src="screens/p1-04-reveal/a-handoff-card-lifted-face-down__android-light.png" width="170"> |

Copy keys: `reveal_tap_card` · `reveal_nobody_look` · `reveal_player_of` · `reveal_show`


### Role reveal — b · Revealed · citizen

_ivory face, whole diamond, single haptic; the longest words wrap to two lines_

| iOS dark | iOS light | Android dark | Android light |
|---|---|---|---|
| <img src="screens/p1-04-reveal/b-revealed-citizen__ios-dark.png" width="170"> | <img src="screens/p1-04-reveal/b-revealed-citizen__ios-light.png" width="170"> | <img src="screens/p1-04-reveal/b-revealed-citizen__android-dark.png" width="170"> | <img src="screens/p1-04-reveal/b-revealed-citizen__android-light.png" width="170"> |

Copy keys: `reveal_secret_word` · `reveal_category` · `reveal_player_of` · `reveal_autohide` · `reveal_hide_next`


### Role reveal — c · Revealed · spy, alone

_dark face in both themes, split diamond, double thud_

| iOS dark | iOS light | Android dark | Android light |
|---|---|---|---|
| <img src="screens/p1-04-reveal/c-revealed-spy-alone__ios-dark.png" width="170"> | <img src="screens/p1-04-reveal/c-revealed-spy-alone__ios-light.png" width="170"> | <img src="screens/p1-04-reveal/c-revealed-spy-alone__android-dark.png" width="170"> | <img src="screens/p1-04-reveal/c-revealed-spy-alone__android-light.png" width="170"> |

Copy keys: `reveal_you_are` · `reveal_spy` · `reveal_category` · `reveal_spy_alone` · `reveal_player_of` · `reveal_autohide` · `reveal_hide_next`


### Role reveal — d · Revealed · spy with other spies

_3 spies of 8; names listed on the card, read last by VoiceOver/TalkBack_

| iOS dark | iOS light | Android dark | Android light |
|---|---|---|---|
| <img src="screens/p1-04-reveal/d-revealed-spy-with-other-spies__ios-dark.png" width="170"> | <img src="screens/p1-04-reveal/d-revealed-spy-with-other-spies__ios-light.png" width="170"> | <img src="screens/p1-04-reveal/d-revealed-spy-with-other-spies__android-dark.png" width="170"> | <img src="screens/p1-04-reveal/d-revealed-spy-with-other-spies__android-light.png" width="170"> |

Copy keys: `reveal_you_are` · `reveal_spy` · `reveal_category` · `reveal_other_spies` · `reveal_player_of` · `reveal_autohide` · `reveal_hide_next`


### Role reveal — e · Own phone · waiting for others

_your seat is always at the bottom; neutral ticks show who has looked — never what they are_

| iOS dark | iOS light | Android dark | Android light |
|---|---|---|---|
| <img src="screens/p1-04-reveal/e-own-phone-waiting-for-others__ios-dark.png" width="170"> | <img src="screens/p1-04-reveal/e-own-phone-waiting-for-others__ios-light.png" width="170"> | <img src="screens/p1-04-reveal/e-own-phone-waiting-for-others__android-dark.png" width="170"> | <img src="screens/p1-04-reveal/e-own-phone-waiting-for-others__android-light.png" width="170"> |

Copy keys: `reveal_seen_label` · `reveal_room_pill` · `plural_waiting_players` · `reveal_starts_when_all` · `reveal_own_card_hint` · `reveal_peek_again`


---

## P1 · 05 · Round & discussion

Board: `frames/P3-05-Round.png` · source `source/P3-05-Round.dc.html`

**Layout**
- Top row: close ✕ · centre pill `round_category_pill` ("Kategorija · Mjesta"). Under it a 15 `textMuted` status line: `plural_in_game` + out count ("7 u igri · 1 izbačen" — use neutral `plural_out_count`).
- The **table is the "who's still in" list**: face-down seat cards (lattice backs) with names on their outer edge (11/700); a knocked-out seat is an empty dashed outline with the name (tap a seat to see the name). Table disc as in Reveal (r 172 / seats r 130), centred, scale 1.
- Centre: `round_label` "RUNDA 2" (label), countdown `countdown` 96/96 tabular (renders ~64 inside the table on phones — keep `countdown` style, scale to fit the 200 pt centre), sub-line `round_who` "Tko ne zna riječ?" / "Vodi Luka" (guest).
- Footer (host): **+1 min** secondary (compact, ~92 wide) + **Završi raspravu** primary (flex). Guests: no controls; `round_vote_opens` hint + **Pogledaj svoju kartu** secondary (own phone).

**States**
- a · timer on, calm: cards drift (see motion.md); table turns 1×/90 s.
- b · ≤30 s: table ring + digits switch to `warning`; sub-line `round_almost` "Još malo!" in `warning`; one `ThirtySeconds` haptic. ≤10 s: cards tremble, digits pulse 1.0→1.07 each second. 0: "Vrijeme!" (`round_time_up`), ring flashes twice, `TimeUp` haptic; in-app voting opens automatically; out-loud waits for the host.
- c · timer off: centre shows "Razgovarajte" (`round_talk`, Bricolage 34/800) + "bez timera"; table still turns slowly; host button **Na glasanje** (`round_to_vote`).
- d · guest own phone: synced countdown from the server clock, own seat at the bottom with 1.5 px ivory ring; no host controls.
- e · out-loud result (host): title `round_loud_title` "Koga ste izbacili?" (Bricolage 24/800) + `round_loud_tap`; centre "Vrijeme!" + `round_loud_vote` "Glasajte naglas"; host taps a seat card → it lifts (ivory 2 pt outline, rotated, scale 1.2) → primary **Izbaci · Ana** (`round_knock_out`) + text **Nitko ne ispada** (`round_nobody`). Two taps total.


### Round & discussion — a · Timer on · host

_round 2, Marko knocked out in round 1 · calm drift_

| iOS dark | iOS light | Android dark | Android light |
|---|---|---|---|
| <img src="screens/p1-05-round/a-timer-on-host__ios-dark.png" width="170"> | <img src="screens/p1-05-round/a-timer-on-host__ios-light.png" width="170"> | <img src="screens/p1-05-round/a-timer-on-host__android-dark.png" width="170"> | <img src="screens/p1-05-round/a-timer-on-host__android-light.png" width="170"> |

Copy keys: `round_label` · `round_who` · `reveal_category` · `setup_timer_minutes` · `round_end_discussion`


### Round & discussion — b · Time almost up

_≤ 30 s amber, one warning haptic · ≤ 10 s cards tremble, digits pulse_

| iOS dark | iOS light | Android dark | Android light |
|---|---|---|---|
| <img src="screens/p1-05-round/b-time-almost-up__ios-dark.png" width="170"> | <img src="screens/p1-05-round/b-time-almost-up__ios-light.png" width="170"> | <img src="screens/p1-05-round/b-time-almost-up__android-dark.png" width="170"> | <img src="screens/p1-05-round/b-time-almost-up__android-light.png" width="170"> |

Copy keys: `round_label` · `round_almost` · `reveal_category` · `round_vote_opens` · `setup_timer_minutes` · `round_end_discussion`


### Round & discussion — c · Timer off

_no countdown; the table still turns slowly; host opens the vote_

| iOS dark | iOS light | Android dark | Android light |
|---|---|---|---|
| <img src="screens/p1-05-round/c-timer-off__ios-dark.png" width="170"> | <img src="screens/p1-05-round/c-timer-off__ios-light.png" width="170"> | <img src="screens/p1-05-round/c-timer-off__android-dark.png" width="170"> | <img src="screens/p1-05-round/c-timer-off__android-light.png" width="170"> |

Copy keys: `round_label` · `round_talk` · `round_no_timer` · `reveal_category` · `round_to_vote`


### Round & discussion — d · Guest · own phone

_synced countdown, no controls; own seat at the bottom, ringed; peek at own card any time_

| iOS dark | iOS light | Android dark | Android light |
|---|---|---|---|
| <img src="screens/p1-05-round/d-guest-own-phone__ios-dark.png" width="170"> | <img src="screens/p1-05-round/d-guest-own-phone__ios-light.png" width="170"> | <img src="screens/p1-05-round/d-guest-own-phone__android-dark.png" width="170"> | <img src="screens/p1-05-round/d-guest-own-phone__android-light.png" width="170"> |

Copy keys: `round_label` · `round_led_by` · `reveal_category` · `round_own_card`


### Round & discussion — e · Out-loud voting · host marks the result

_after the alarm: tap the card of whoever the group voted out, then confirm — two taps total_

| iOS dark | iOS light | Android dark | Android light |
|---|---|---|---|
| <img src="screens/p1-05-round/e-out-loud-voting-host-marks-the-result__ios-dark.png" width="170"> | <img src="screens/p1-05-round/e-out-loud-voting-host-marks-the-result__ios-light.png" width="170"> | <img src="screens/p1-05-round/e-out-loud-voting-host-marks-the-result__android-dark.png" width="170"> | <img src="screens/p1-05-round/e-out-loud-voting-host-marks-the-result__android-light.png" width="170"> |

Copy keys: `round_time_up` · `round_loud_vote` · `result_pill` · `round_loud_title` · `round_knock_out` · `round_nobody`


---

## P1 · 06 · Voting (in the app)

Board: `frames/P3-06-Vote.png` · source `source/P3-06-Vote.dc.html`

**Rules shown:** anonymous; results only when all votes are in; who hasn't voted may be shown, who voted for whom never; confirm locks the vote; host can close voting (counts only cast votes).

**a · pick a player** — pill "Runda 2 · glasanje"; large title `vote_title` (Bricolage 34/800) + `vote_secret`. 2-column grid of `VoteOption` (height 68, radius 18, `surface`, avatar circle 32 + name 18/700), gap 10. Selected: 1.5 px `text` border + inverted avatar (ivory with dark letter) — not colour alone. Self: dashed `line` border, name + "ti" sub, disabled. Knocked-out: dashed, name struck-through in `eliminated`, sub "izvan igre", disabled. Footer: `vote_locked` hint + **Glasaj · Ana** primary (`vote_confirm`). Haptic `VoteCast` on confirm.

**b · cast, waiting (host)** — success ring 88 (`success` 2 pt stroke, check) · "Glas je predan" (`vote_cast`, 15 muted) · headline `plural_waiting_players` (Bricolage 30/800) · progress segments (one per voter, 6 pt tall, radius full, `success` filled / `disabledFill`). Panel `vote_not_yet` with name chips (pill `surface2`, dot) — dropped players in `warningText` with dashed ring ("Ivan · prekid veze"). Host footer: `vote_close_hint` + **Zatvori glasanje** (secondary). Guests: same minus the host button.

**c · one phone, pass the phone** — same table choreography as reveal: seat turns to the bottom, card lifts showing a card back with a `vote` icon medallion, label "GLASA" and name (Bricolage 40/800); copy `reveal_pass_to` + `vote_pass_secret`; primary `vote_i_am` "Ja sam Petar · glasam" → opens (a) for that player. Pill shows `vote_progress` "Glasalo 4 od 7".

**d · spectator (knocked out)** — pill with `spectating` icon "Gledaš"; their face-down card stamped `result_out_stamp` "IZVAN IGRE" (rotated −8°, 2 pt ivory frame, label 16/800 tracking 10 %); title `vote_spectator_title`, body `vote_spectator_body`; progress segments + `vote_progress`. No buttons; never roles or votes.


### Voting (in the app) — a · Pick a player

_Iva's phone · self and knocked-out players can't be chosen · confirm locks the vote_

| iOS dark | iOS light | Android dark | Android light |
|---|---|---|---|
| <img src="screens/p1-06-vote/a-pick-a-player__ios-dark.png" width="170"> | <img src="screens/p1-06-vote/a-pick-a-player__ios-light.png" width="170"> | <img src="screens/p1-06-vote/a-pick-a-player__android-dark.png" width="170"> | <img src="screens/p1-06-vote/a-pick-a-player__android-light.png" width="170"> |

Copy keys: `result_pill` · `vote_title` · `vote_secret` · `vote_option_out` · `vote_locked` · `vote_confirm`


### Voting (in the app) — b · Vote cast · waiting (host view)

_guests see the same without "Zatvori glasanje"_

| iOS dark | iOS light | Android dark | Android light |
|---|---|---|---|
| <img src="screens/p1-06-vote/b-vote-cast-waiting-host-view__ios-dark.png" width="170"> | <img src="screens/p1-06-vote/b-vote-cast-waiting-host-view__ios-light.png" width="170"> | <img src="screens/p1-06-vote/b-vote-cast-waiting-host-view__android-dark.png" width="170"> | <img src="screens/p1-06-vote/b-vote-cast-waiting-host-view__android-light.png" width="170"> |

Copy keys: `result_pill` · `vote_cast` · `plural_waiting_players` · `vote_not_yet` · `vote_player_dropped` · `vote_close_hint` · `vote_close`


### Voting (in the app) — c · One phone · pass for a private vote

_same table choreography as the reveal: seat turns to the bottom, then the pick screen (a) opens_

| iOS dark | iOS light | Android dark | Android light |
|---|---|---|---|
| <img src="screens/p1-06-vote/c-one-phone-pass-for-a-private-vote__ios-dark.png" width="170"> | <img src="screens/p1-06-vote/c-one-phone-pass-for-a-private-vote__ios-light.png" width="170"> | <img src="screens/p1-06-vote/c-one-phone-pass-for-a-private-vote__android-dark.png" width="170"> | <img src="screens/p1-06-vote/c-one-phone-pass-for-a-private-vote__android-light.png" width="170"> |

Copy keys: `vote_spectator_voting_tag` · `vote_progress` · `reveal_pass_to` · `vote_pass_secret` · `vote_i_am`


### Voting (in the app) — d · Spectator · knocked out

_Marko's phone · sees progress, never roles or votes_

| iOS dark | iOS light | Android dark | Android light |
|---|---|---|---|
| <img src="screens/p1-06-vote/d-spectator-knocked-out__ios-dark.png" width="170"> | <img src="screens/p1-06-vote/d-spectator-knocked-out__ios-light.png" width="170"> | <img src="screens/p1-06-vote/d-spectator-knocked-out__android-dark.png" width="170"> | <img src="screens/p1-06-vote/d-spectator-knocked-out__android-light.png" width="170"> |

Copy keys: `vote_spectating` · `result_out_stamp` · `vote_spectator_title` · `vote_spectator_body` · `vote_progress`


---

## P1 · 07 · Round result

Board: `frames/P3-07-Result.png` · source `source/P3-07-Result.dc.html`

Skipped entirely when the knock-out ends the game (last spy out, or spies ≥ citizens) → Game over plays.

**a · knocked out** — pill `result_pill`. The knocked-out card lands face-down top-centre (≈150×210, rotated +8°, lattice back, stamp "IZVAN IGRE", name below on the card). Title `result_out` "Ana ispada iz igre" (Bricolage 34/800), sub `result_roles_end`. `TallyBars` panel (`surface`, radius 22): `result_votes` label + total (`plural_votes`) right; rows: name (17; winner 700 `text`, others `textMuted`) · bar (track `surface2`, fill `primary` for winner, `disabledFill` others, height 22 radius 6) · count tabular. Primary **Sljedeća runda**. Haptic: `SuspenseTick`×3 → `KnockOut` (identical for every role).

**b · tie** — two face-down cards side by side (Ana, Luka) with an "=" medallion; title `result_tie`, body `result_tie_body`; both tied bars full `primary`; primary **Glasaj ponovno · Ana ili Luka** (`result_revote`). The re-vote is only between the tied players.

**c · second tie** — title `result_tie_again` "Opet neriješeno", body "Nitko ne ispada. Obje karte ostaju na stolu."; tally shows who didn't vote ("bez glasa: Ivan" — proposed neutral key); primary **Runda 3** (`result_round_n`).


### Round result — a · Knocked out

_one player has the most votes · heavy thud, same for every role_

| iOS dark | iOS light | Android dark | Android light |
|---|---|---|---|
| <img src="screens/p1-07-result/a-knocked-out__ios-dark.png" width="170"> | <img src="screens/p1-07-result/a-knocked-out__ios-light.png" width="170"> | <img src="screens/p1-07-result/a-knocked-out__android-dark.png" width="170"> | <img src="screens/p1-07-result/a-knocked-out__android-light.png" width="170"> |

Copy keys: `result_pill` · `result_out_stamp` · `result_out` · `result_roles_end` · `result_votes` · `plural_votes` · `result_next_round`


### Round result — b · Tie

_vote again, only between the tied players_

| iOS dark | iOS light | Android dark | Android light |
|---|---|---|---|
| <img src="screens/p1-07-result/b-tie__ios-dark.png" width="170"> | <img src="screens/p1-07-result/b-tie__ios-light.png" width="170"> | <img src="screens/p1-07-result/b-tie__android-dark.png" width="170"> | <img src="screens/p1-07-result/b-tie__android-light.png" width="170"> |

Copy keys: `result_pill` · `result_tie` · `result_tie_body` · `result_votes` · `plural_votes` · `result_revote`


### Round result — c · Second tie

_the re-vote ties again → nobody is knocked out, next round starts_

| iOS dark | iOS light | Android dark | Android light |
|---|---|---|---|
| <img src="screens/p1-07-result/c-second-tie__ios-dark.png" width="170"> | <img src="screens/p1-07-result/c-second-tie__ios-light.png" width="170"> | <img src="screens/p1-07-result/c-second-tie__android-dark.png" width="170"> | <img src="screens/p1-07-result/c-second-tie__android-light.png" width="170"> |

Copy keys: `result_pill` · `result_tie_again` · `result_tie_again_cards` · `result_votes`


---

## P1 · 08 · Game over

Board: `frames/P3-08-GameOver.png` · source `source/P3-08-GameOver.dc.html`

The only moment roles are revealed. Choreography: every seat card flips in place (spies first, 120 ms stagger) → spy cards + the word card are dealt to the front (`spring.deal`). Haptic `GameOverReveal`.

**Layout** — no close button (game is over). Top-left meta "Kraj igre · 3 runde" (`gameover_label` + `plural_rounds`, 14 muted). Winner headline with glyph: citizens = whole diamond `citizen` colour + `gameover_citizens`; spies = split diamond `spy` colour + `gameover_spies` (Bricolage 36/800). Sub-line: "Oba špijuna su otkrivena." / unmasked spies list (proposed `plural_spies_caught`, `gameover_unmasked_spies`).
- Fanned cards (≈120×165, rotated −8°…+6°): each spy as a spy face (glyph + name 22/800 + "ŠPIJUN" label in `spy`); the ivory word card larger (≈150×200, +6°) with `reveal_secret_word` label + word + category.
- Recap list (`surface`, radius 22): rows 44 — "R1" (13 muted, tabular) · name · role with glyph (`citizen` blue whole diamond "građanin" / `spy` split diamond "špijun"). `role_citizen` / `role_spy`.
- Footer: **Igraj ponovno** primary + `gameover_play_again_hint`; **Nova igra** text button.


### Game over — a · Citizens win

_all spies knocked out_

| iOS dark | iOS light | Android dark | Android light |
|---|---|---|---|
| <img src="screens/p1-08-game-over/a-citizens-win__ios-dark.png" width="170"> | <img src="screens/p1-08-game-over/a-citizens-win__ios-light.png" width="170"> | <img src="screens/p1-08-game-over/a-citizens-win__android-dark.png" width="170"> | <img src="screens/p1-08-game-over/a-citizens-win__android-light.png" width="170"> |

Copy keys: `plural_rounds` · `gameover_citizens` · `reveal_secret_word` · `role_citizen` · `role_spy` · `gameover_play_again` · `gameover_play_again_hint` · `home_new_game`


### Game over — b · Spies win

_spies now equal the remaining citizens (2 : 2)_

| iOS dark | iOS light | Android dark | Android light |
|---|---|---|---|
| <img src="screens/p1-08-game-over/b-spies-win__ios-dark.png" width="170"> | <img src="screens/p1-08-game-over/b-spies-win__ios-light.png" width="170"> | <img src="screens/p1-08-game-over/b-spies-win__android-dark.png" width="170"> | <img src="screens/p1-08-game-over/b-spies-win__android-light.png" width="170"> |

Copy keys: `plural_rounds` · `gameover_spies` · `reveal_secret_word` · `role_citizen` · `gameover_play_again` · `gameover_play_again_hint` · `home_new_game`


---

## P1 · 09 · Dictionaries

Board: `frames/P3-09-Dictionaries.png` · source `source/P3-09-Dictionaries.dc.html`

**a · list** (Rječnici tab) — large title `dict_title`; trailing toolbar buttons: `import` (→ file picker / scanner) and `add` (→ create sheet). Sections: `dict_section_builtin` "UGRAĐENI" and `dict_section_mine` "MOJI · SAMO NA OVOM UREĐAJU". `DictionaryRow` 68: language tile 40 (radius 12, `surface2`, "HR"/"EN" 13/800) · name `bodyLarge` 700 · meta "3 kategorije · 50 riječi" (`plural_categories` · `plural_words`) · trailing: built-in = outlined badge with `lock-builtin` icon "Ugrađen" (radius 8, 1 px line); custom = chevron. Custom with no words: meta in `warningText` "… · još nema riječi".
**b · custom dictionary** — back; trailing `share` (→ export sheet P2·10) and ⋯ (rename, language, duplicate, delete). Large title = name, meta line "English · 6 kategorija · 212 riječi". Category rows 55: name · count · chevron. Bottom of list: **+ Nova kategorija** outlined full-width button (radius 22, 1 px `line`).
**c · category words** — inline title "Filmovi · 40"; add field on top (`SpyTextField` + **Dodaj** button `surface2`), duplicate check case-insensitive → field gets `danger` border + `dict_duplicate` below; search field (`search` icon, `plural_search_words`) for long lists; word rows 51. Delete: iOS swipe action (`dangerFill` "Izbriši"); Android long-press → contextual top bar. Add success → toast `toast_word_added` + **Poništi**.
**d · create / rename sheet** — iOS sheet with Cancel / title "Novi rječnik" / **Spremi** (ivory pill); Android full-height sheet with the same fields. Fields: `dict_name`, `dict_language` segmented (Hrvatski / English), hint with lock icon `dict_local_hint`.
**e · delete confirmation** — native: iOS `confirmationDialog` (title `dict_delete_title`, message counts, destructive `dict_delete` in `danger`, Cancel); Android `AlertDialog`.
**f · empty** — built-ins listed; "MOJI" section shows `EmptyState` (dashed `line` border radius 22, hollow diamond glyph 40, title `dict_empty_title` 18/700, body, buttons **Novi rječnik** primary small + **Uvezi** secondary small).
**g · built-in read-only** — title + "Ugrađen" badge, meta incl. `dict_updated`; info panel with lock icon `dict_readonly`; category rows; footer primary **Napravi kopiju** (`dict_copy`). Admin editing is not reachable here (Settings → PIN).


### Dictionaries — a · List

_built-in vs custom, language, counts_

| iOS dark | iOS light | Android dark | Android light |
|---|---|---|---|
| <img src="screens/p1-09-dictionaries/a-list__ios-dark.png" width="170"> | <img src="screens/p1-09-dictionaries/a-list__ios-light.png" width="170"> | <img src="screens/p1-09-dictionaries/a-list__android-dark.png" width="170"> | <img src="screens/p1-09-dictionaries/a-list__android-light.png" width="170"> |

Copy keys: `tab_dictionaries` · `dict_section_builtin` · `plural_words` · `dict_builtin` · `dict_section_mine` · `tab_play` · `tab_settings`


### Dictionaries — b · Custom dictionary · categories

_share = export sheet (P2) · ⋯ = rename, language, duplicate, delete_

| iOS dark | iOS light | Android dark | Android light |
|---|---|---|---|
| <img src="screens/p1-09-dictionaries/b-custom-dictionary-categories__ios-dark.png" width="170"> | <img src="screens/p1-09-dictionaries/b-custom-dictionary-categories__ios-light.png" width="170"> | <img src="screens/p1-09-dictionaries/b-custom-dictionary-categories__android-dark.png" width="170"> | <img src="screens/p1-09-dictionaries/b-custom-dictionary-categories__android-light.png" width="170"> |

Copy keys: `plural_words` · `dict_new_category`


### Dictionaries — c · Category · words

_add field on top, duplicate check (case-insensitive), search for long lists; iOS swipe / Android long-press to delete_

| iOS dark | iOS light | Android dark | Android light |
|---|---|---|---|
| <img src="screens/p1-09-dictionaries/c-category-words__ios-dark.png" width="170"> | <img src="screens/p1-09-dictionaries/c-category-words__ios-light.png" width="170"> | <img src="screens/p1-09-dictionaries/c-category-words__android-dark.png" width="170"> | <img src="screens/p1-09-dictionaries/c-category-words__android-light.png" width="170"> |

Copy keys: `dict_new_word` · `dict_add` · `dict_duplicate` · `plural_search_words` · `dict_delete_short`


### Dictionaries — d · Create / rename dictionary

_sheet; same form for rename and language change_

| iOS dark | iOS light | Android dark | Android light |
|---|---|---|---|
| <img src="screens/p1-09-dictionaries/d-create-rename-dictionary__ios-dark.png" width="170"> | <img src="screens/p1-09-dictionaries/d-create-rename-dictionary__ios-light.png" width="170"> | <img src="screens/p1-09-dictionaries/d-create-rename-dictionary__android-dark.png" width="170"> | <img src="screens/p1-09-dictionaries/d-create-rename-dictionary__android-light.png" width="170"> |

Copy keys: `tab_dictionaries` · `common_cancel` · `dict_new` · `common_save` · `dict_name` · `dict_language` · `dict_local_hint`


### Dictionaries — e · Delete confirmation

_iOS confirmationDialog (action sheet) · Android AlertDialog — native on both_

| iOS dark | iOS light | Android dark | Android light |
|---|---|---|---|
| <img src="screens/p1-09-dictionaries/e-delete-confirmation__ios-dark.png" width="170"> | <img src="screens/p1-09-dictionaries/e-delete-confirmation__ios-light.png" width="170"> | <img src="screens/p1-09-dictionaries/e-delete-confirmation__android-dark.png" width="170"> | <img src="screens/p1-09-dictionaries/e-delete-confirmation__android-light.png" width="170"> |

Copy keys: `plural_words` · `dict_delete_title` · `dict_delete` · `common_cancel`


### Dictionaries — f · No custom dictionaries yet

_first visit; built-ins stay listed above_

| iOS dark | iOS light | Android dark | Android light |
|---|---|---|---|
| <img src="screens/p1-09-dictionaries/f-no-custom-dictionaries-yet__ios-dark.png" width="170"> | <img src="screens/p1-09-dictionaries/f-no-custom-dictionaries-yet__ios-light.png" width="170"> | <img src="screens/p1-09-dictionaries/f-no-custom-dictionaries-yet__android-dark.png" width="170"> | <img src="screens/p1-09-dictionaries/f-no-custom-dictionaries-yet__android-light.png" width="170"> |

Copy keys: `tab_dictionaries` · `dict_section_builtin` · `plural_words` · `dict_section_mine` · `dict_empty_title` · `dict_empty_body` · `dict_new` · `dict_import` · `tab_play` · `tab_settings`


### Dictionaries — g · Built-in · read-only

_server-synced, cached offline; "copy to edit" makes a custom dictionary_

| iOS dark | iOS light | Android dark | Android light |
|---|---|---|---|
| <img src="screens/p1-09-dictionaries/g-built-in-read-only__ios-dark.png" width="170"> | <img src="screens/p1-09-dictionaries/g-built-in-read-only__ios-light.png" width="170"> | <img src="screens/p1-09-dictionaries/g-built-in-read-only__android-dark.png" width="170"> | <img src="screens/p1-09-dictionaries/g-built-in-read-only__android-light.png" width="170"> |

Copy keys: `dict_builtin` · `dict_readonly` · `dict_copy`


---

## P2 · 10 · Import & export

Board: `frames/P4-10-ImportExport.png` · source `source/P4-10-ImportExport.dc.html`

Local-first: nothing passes through our server. Link/QR payloads carry the dictionary itself (≈2 KB / ≈80 words max).
**a · export sheet** — title `export_title` (Bricolage 28/800), meta "„Ljeto na Visu“ · 24 riječi · 0,6 KB"; three option rows (radius 22, `optionFill`, icon tile 48 `surface2`, title 17/700, desc 14 muted): Datoteka (`export_file`, file icon) · Poveznica (`export_link`) · QR kod (`export_qr`).
**b · QR** — full screen, close ✕; name (Bricolage 30/800) + "24 riječi · Hrvatski"; QR card 300×300 white radius 28 with brand-mark medallion in the centre (dark 52 tile, vermilion glyph); `export_qr_how`; footer **Kopiraj poveznicu** secondary + **Gotovo** primary. Screen brightness boosted while shown.
**c · too big** — Link and QR rows disabled (dashed border, 40 % ink) with desc `export_too_big`; File row selected (1.5 px `text` border) with `export_file_best`.
**d · import preview + conflict** — close ✕, inline title `import_title`; source card (`surface`, radius 22): lang tile + name 24/800 + "party-pack.json" (drop the sender, see copy review); 3 stat tiles (radius 14, `surface2`): categories / words / "već imaš" (count in `warningText`). Warning line (triangle icon, `import_conflict`). Radio cards: **Spoji s postojećim** (selected: 1.5 px `text` border) / **Spremi kao novi** ("Kao „Party pack (2)“"). Footer **Spoji · 194 nove riječi** primary + **Odustani** text. Toast `toast_imported` after save.
**e · errors** — centred: 72 circle with 2 pt `danger` ring + `file-invalid` icon; title `import_err_invalid` (Bricolage 30/800); body; same layout for `import_err_version` and `import_err_empty`. Footer **Odaberi drugu datoteku** primary + **Zatvori** text.
**f · QR scanner** — always dark (camera); close ✕ + torch button (`torch`, glass); viewfinder 260×260 with corner brackets (ivory 4 pt) and a vermilion scan line; title `scan_title`, hint `scan_hint`; footer **Upiši kod sobe** secondary. VisionKit `DataScannerViewController` / CameraX + ML Kit underneath; found → haptic, route by payload type (room vs dictionary).


### Import & export — a · Export · small dictionary

_all three routes available_

| iOS dark | iOS light | Android dark | Android light |
|---|---|---|---|
| <img src="screens/p2-10-import-export/a-export-small-dictionary__ios-dark.png" width="170"> | <img src="screens/p2-10-import-export/a-export-small-dictionary__ios-light.png" width="170"> | <img src="screens/p2-10-import-export/a-export-small-dictionary__android-dark.png" width="170"> | <img src="screens/p2-10-import-export/a-export-small-dictionary__android-light.png" width="170"> |

Copy keys: `plural_words` · `export_title` · `export_file` · `export_file_desc` · `export_link` · `export_link_desc` · `export_qr` · `export_qr_desc`


### Import & export — b · Share by QR

_screen brightness boosted while shown; the code contains the dictionary itself_

| iOS dark | iOS light | Android dark | Android light |
|---|---|---|---|
| <img src="screens/p2-10-import-export/b-share-by-qr__ios-dark.png" width="170"> | <img src="screens/p2-10-import-export/b-share-by-qr__ios-light.png" width="170"> | <img src="screens/p2-10-import-export/b-share-by-qr__android-dark.png" width="170"> | <img src="screens/p2-10-import-export/b-share-by-qr__android-light.png" width="170"> |

Copy keys: `export_qr_how` · `export_copy_link` · `export_done`


### Import & export — c · Export · too big for link or QR

_limit ≈ 2 KB (≈ 80 words); the file route is promoted_

| iOS dark | iOS light | Android dark | Android light |
|---|---|---|---|
| <img src="screens/p2-10-import-export/c-export-too-big-for-link-or-qr__ios-dark.png" width="170"> | <img src="screens/p2-10-import-export/c-export-too-big-for-link-or-qr__ios-light.png" width="170"> | <img src="screens/p2-10-import-export/c-export-too-big-for-link-or-qr__android-dark.png" width="170"> | <img src="screens/p2-10-import-export/c-export-too-big-for-link-or-qr__android-light.png" width="170"> |

Copy keys: `plural_words` · `export_title` · `export_file` · `export_file_best` · `export_link` · `export_too_big` · `export_qr`


### Import & export — d · Import preview · conflict

_opened from a file, link or QR; a dictionary with that name already exists_

| iOS dark | iOS light | Android dark | Android light |
|---|---|---|---|
| <img src="screens/p2-10-import-export/d-import-preview-conflict__ios-dark.png" width="170"> | <img src="screens/p2-10-import-export/d-import-preview-conflict__ios-light.png" width="170"> | <img src="screens/p2-10-import-export/d-import-preview-conflict__android-dark.png" width="170"> | <img src="screens/p2-10-import-export/d-import-preview-conflict__android-light.png" width="170"> |

Copy keys: `import_title` · `import_already_have` · `import_conflict` · `import_merge` · `import_merge_desc_counts` · `import_save_new` · `import_save_new_desc` · `plural_new_words` · `common_cancel`


### Import & export — e · Import errors

_invalid file shown · same layout: "Ova datoteka je iz novije verzije Špijuna — ažuriraj aplikaciju." · "Rječnik je prazan — nema što uvesti."_

| iOS dark | iOS light | Android dark | Android light |
|---|---|---|---|
| <img src="screens/p2-10-import-export/e-import-errors__ios-dark.png" width="170"> | <img src="screens/p2-10-import-export/e-import-errors__ios-light.png" width="170"> | <img src="screens/p2-10-import-export/e-import-errors__android-dark.png" width="170"> | <img src="screens/p2-10-import-export/e-import-errors__android-light.png" width="170"> |

Copy keys: `import_title` · `import_err_invalid` · `import_err_invalid_named` · `import_pick_other` · `common_close`


### Import & export — f · QR scanner

_one scanner for rooms and dictionaries; always dark (camera); VisionKit / ML Kit underneath_

| iOS dark | iOS light | Android dark | Android light |
|---|---|---|---|
| <img src="screens/p2-10-import-export/f-qr-scanner__ios-dark.png" width="170"> | <img src="screens/p2-10-import-export/f-qr-scanner__ios-light.png" width="170"> | <img src="screens/p2-10-import-export/f-qr-scanner__android-dark.png" width="170"> | <img src="screens/p2-10-import-export/f-qr-scanner__android-light.png" width="170"> |

Copy keys: `join_scan` · `scan_hint` · `scan_enter_code`


---

## P2 · 11 · Settings

Board: `frames/P4-11-Settings.png` · source `source/P4-11-Settings.dc.html`

**a · settings** (Postavke tab) — large title; grouped panels (`surface`, radius 22) scrolling under the tab bar. Rows 53: label `bodyLarge` · value `textMuted` · chevron.
- OPĆENITO: Jezik (Hrvatski) · Tema (Kao sustav / Svijetla / Tamna).
- NOVA IGRA · ZADANO: Igrači i špijuni (6 · 1) · Timer (5 min) · Glasanje (U aplikaciji).
- OTKRIVANJE: Način (Dodir) · Automatsko skrivanje (8 s).
- OSJETILA: Vibracija (switch) · Zvukovi (switch) · Zaslon uvijek upaljen (switch). Switch = `SpySwitchRow` (iOS-style on iOS, M3 on Android; on = `success` track).
- O APLIKACIJI: Verzija "1.0 (42)" · Licence · **Uređivanje ugrađenih rječnika** (`settings_admin`, low prominence, `textMuted`, last row).
**b · reveal options** — back + inline title "Otkrivanje uloge". `NAČIN`: two radio cards with a tiny illustration at right — **Dodirni za okretanje** (recommended; selected 1.5 px `text` border) / **Drži za pogled**. `AUTOMATSKO SKRIVANJE`: segmented 5 s · 8 s · Isključeno + description `settings_autohide_desc`. When the OS Reduce Motion is on: info panel `settings_reduce_motion`.
**c · admin PIN sheet** — title `admin_title`, body `admin_body`, 4 PIN dots (16, `danger` when wrong), error `admin_wrong_pin`, shake + `Error` haptic, **Odustani** text. Backend unchanged (existing shared PIN).


### Settings — a · Settings

_grouped list; scrolls under the tab bar_

| iOS dark | iOS light | Android dark | Android light |
|---|---|---|---|
| <img src="screens/p2-11-settings/a-settings__ios-dark.png" width="170"> | <img src="screens/p2-11-settings/a-settings__ios-light.png" width="170"> | <img src="screens/p2-11-settings/a-settings__android-dark.png" width="170"> | <img src="screens/p2-11-settings/a-settings__android-light.png" width="170"> |

Copy keys: `tab_settings` · `settings_general` · `settings_language` · `settings_theme` · `settings_theme_system` · `settings_defaults` · `settings_players_spies` · `settings_timer` · `setup_timer_minutes` · `settings_voting` · `setup_vote_app` · `settings_reveal` · `settings_reveal_mode` · `settings_touch` · `settings_autohide` · `settings_senses` · `settings_haptics` · `settings_sound` · `settings_keep_awake` · `tab_play` · `tab_dictionaries`


### Settings — b · Reveal options

_mechanic + auto-hide; respects Reduce Motion_

| iOS dark | iOS light | Android dark | Android light |
|---|---|---|---|
| <img src="screens/p2-11-settings/b-reveal-options__ios-dark.png" width="170"> | <img src="screens/p2-11-settings/b-reveal-options__ios-light.png" width="170"> | <img src="screens/p2-11-settings/b-reveal-options__android-dark.png" width="170"> | <img src="screens/p2-11-settings/b-reveal-options__android-light.png" width="170"> |

Copy keys: `settings_reveal_title` · `settings_reveal_mode_label` · `settings_reveal_tap` · `settings_reveal_tap_desc` · `settings_reveal_hold` · `settings_reveal_hold_desc` · `settings_autohide_label` · `settings_autohide_off` · `settings_autohide_desc` · `settings_reduce_motion`


### Settings — c · Admin · PIN sheet

_from the last row of "O aplikaciji"; wrong PIN shakes; today's shared-PIN backend unchanged_

| iOS dark | iOS light | Android dark | Android light |
|---|---|---|---|
| <img src="screens/p2-11-settings/c-admin-pin-sheet__ios-dark.png" width="170"> | <img src="screens/p2-11-settings/c-admin-pin-sheet__ios-light.png" width="170"> | <img src="screens/p2-11-settings/c-admin-pin-sheet__android-dark.png" width="170"> | <img src="screens/p2-11-settings/c-admin-pin-sheet__android-light.png" width="170"> |

Copy keys: `settings_version` · `settings_licenses` · `settings_admin` · `admin_title` · `admin_body` · `admin_wrong_pin` · `common_cancel`


---

## P2 · 12 · Connection & edge states

Board: `frames/P4-12-Connection.png` · source `source/P4-12-Connection.dc.html`

Rule: the game never stalls silently; every gap has a visible state, a default and a way out. Roles never travel to other phones.
**a · reconnecting (guest)** — `warning`-fill banner under the top row (radius 22, spinner, title `conn_lost` 15/700, body `conn_reconnecting`); countdown keeps running locally from the server clock; table dims to α ≈0.6; bottom note `conn_card_saved`.
**b · player dropped (host)** — Petar's seat becomes a dashed `warning` outline; after a 60 s grace a bottom sheet: `offline` icon in `warning`, title `conn_player_dropped`, body with countdown (use neutral copy, see copy review), primary **Čekaj · Petar** (`conn_wait_for`), text **Izbaci iz igre** (`conn_remove`).
**c · host left (guest)** — game pauses (centre label `conn_paused`); sheet: host icon, title `conn_host_left`, body; primary **Preuzmi vođenje**, text **Završi igru**. After 30 s the longest-connected player is offered it first.
**d · removed** — plain screen: dim card back, title `conn_removed` (Bricolage 30/800), body `conn_removed_body`, primary **Na početak**. No roles, no word.
**e · offline when creating a room** — sheet over Home: `offline` icon, `conn_offline_title`, `conn_offline_body`, primary **Igraj na jednom telefonu**, text **Pokušaj ponovno** (`common_retry`).
**f · back from background** — ivory neutral banner `conn_back` + `conn_back_body`; any open card was already hidden on background; note `conn_snapshot_blurred`.


### Connection & edge states — a · Reconnecting

_guest lost signal mid-round; countdown keeps running locally from the server clock_

| iOS dark | iOS light | Android dark | Android light |
|---|---|---|---|
| <img src="screens/p2-12-connection/a-reconnecting__ios-dark.png" width="170"> | <img src="screens/p2-12-connection/a-reconnecting__ios-light.png" width="170"> | <img src="screens/p2-12-connection/a-reconnecting__android-dark.png" width="170"> | <img src="screens/p2-12-connection/a-reconnecting__android-light.png" width="170"> |

Copy keys: `round_label` · `round_led_by` · `reveal_category` · `conn_lost` · `conn_reconnecting` · `conn_card_saved`


### Connection & edge states — b · Player dropped · host

_Petar’s seat goes dashed amber; host decides after a 60 s grace_

| iOS dark | iOS light | Android dark | Android light |
|---|---|---|---|
| <img src="screens/p2-12-connection/b-player-dropped-host__ios-dark.png" width="170"> | <img src="screens/p2-12-connection/b-player-dropped-host__ios-light.png" width="170"> | <img src="screens/p2-12-connection/b-player-dropped-host__android-dark.png" width="170"> | <img src="screens/p2-12-connection/b-player-dropped-host__android-light.png" width="170"> |

Copy keys: `round_label` · `round_led_by` · `reveal_category` · `conn_player_dropped` · `conn_wait_for` · `conn_remove`


### Connection & edge states — c · Host left · guest view

_game pauses; anyone can take over; after 30 s the longest-connected player is offered it first_

| iOS dark | iOS light | Android dark | Android light |
|---|---|---|---|
| <img src="screens/p2-12-connection/c-host-left-guest-view__ios-dark.png" width="170"> | <img src="screens/p2-12-connection/c-host-left-guest-view__ios-light.png" width="170"> | <img src="screens/p2-12-connection/c-host-left-guest-view__android-dark.png" width="170"> | <img src="screens/p2-12-connection/c-host-left-guest-view__android-light.png" width="170"> |

Copy keys: `round_label` · `conn_paused` · `reveal_category` · `conn_host_left` · `conn_take_over` · `conn_end_game`


### Connection & edge states — d · Removed from the room

_the removed player sees no roles, no word — just a clean exit_

| iOS dark | iOS light | Android dark | Android light |
|---|---|---|---|
| <img src="screens/p2-12-connection/d-removed-from-the-room__ios-dark.png" width="170"> | <img src="screens/p2-12-connection/d-removed-from-the-room__ios-light.png" width="170"> | <img src="screens/p2-12-connection/d-removed-from-the-room__android-dark.png" width="170"> | <img src="screens/p2-12-connection/d-removed-from-the-room__android-light.png" width="170"> |

Copy keys: `conn_removed` · `conn_removed_body` · `conn_to_home`


### Connection & edge states — e · Offline when creating a room

_from the mode chooser; offers the mode that works without internet_

| iOS dark | iOS light | Android dark | Android light |
|---|---|---|---|
| <img src="screens/p2-12-connection/e-offline-when-creating-a-room__ios-dark.png" width="170"> | <img src="screens/p2-12-connection/e-offline-when-creating-a-room__ios-light.png" width="170"> | <img src="screens/p2-12-connection/e-offline-when-creating-a-room__android-dark.png" width="170"> | <img src="screens/p2-12-connection/e-offline-when-creating-a-room__android-light.png" width="170"> |

Copy keys: `reveal_spy` · `home_tagline` · `conn_offline_title` · `conn_offline_body` · `conn_offline_one_phone` · `common_retry`


### Connection & edge states — f · Back from background

_app was closed for 2 min; any open card was hidden the moment the app left the screen_

| iOS dark | iOS light | Android dark | Android light |
|---|---|---|---|
| <img src="screens/p2-12-connection/f-back-from-background__ios-dark.png" width="170"> | <img src="screens/p2-12-connection/f-back-from-background__ios-light.png" width="170"> | <img src="screens/p2-12-connection/f-back-from-background__android-dark.png" width="170"> | <img src="screens/p2-12-connection/f-back-from-background__android-light.png" width="170"> |

Copy keys: `round_label` · `round_led_by` · `reveal_category` · `conn_back` · `conn_back_body` · `conn_snapshot_blurred`


---

## P3 · 14 · Transient UI

Board: `frames/P4-14-Transient.png` · source `source/P4-14-Transient.dc.html`

One frame each, platforms and themes mixed (as designed).
- **Toast / snackbar** (iOS dark): ivory pill-card (radius 22, `primary` fill, ink text) at the bottom above the safe area: check icon (`success`), text `toast_word_added`, action **Poništi** (`toast_undo`). 4 s, swipe to dismiss, max one action, never during a reveal, never carries secrets.
- **Loading skeleton** (Android light): shapes match the real rows; only appears if loading exceeds 300 ms; gentle pulse (no shimmer sweep under Reduce Motion).
- **Destructive confirmation** (iOS light): from ✕ during a round → `quit_title`, `quit_body`, destructive `quit_confirm`, safe `quit_keep` is the prominent action. Android: `AlertDialog` with the same copy.
- **Camera permission** (Android dark): pre-permission screen before the system prompt: `camera` icon, `camera_title`, `camera_body`, primary `camera_allow`, secondary `camera_manual`. After denial the primary becomes `camera_settings` "Otvori postavke".


### Transient UI — Transient UI

| toast-snackbar__ios-dark | loading-skeleton__android-light | destructive-confirmation__ios-light | camera-permission__android-dark |
|---|---|---|---|
| <img src="screens/p3-14-transient/toast-snackbar__ios-dark.png" width="170"> | <img src="screens/p3-14-transient/loading-skeleton__android-light.png" width="170"> | <img src="screens/p3-14-transient/destructive-confirmation__ios-light.png" width="170"> | <img src="screens/p3-14-transient/camera-permission__android-dark.png" width="170"> |

Copy keys: `toast_word_added` · `toast_undo` · `tab_dictionaries` · `tab_play` · `tab_settings` · `quit_title` · `quit_body` · `quit_confirm` · `quit_keep` · `camera_title` · `camera_body` · `camera_allow` · `camera_manual`

