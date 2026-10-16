# Claude Design prompt — Špijun / Spy (iOS + Android)

Attach before sending: screenshots of the current web app (home, setup, reveal hidden,
reveal as citizen, reveal as spy, discussion, word management open) and, if possible,
`app/globals.css`. The screenshots show the *current single-phone web version*; the
prompt describes what the native apps become.

---

## PROMPT (copy from here)

You are the lead product designer for **Špijun** ("Spy"), a social party game moving
from a web app to native iOS and Android apps, and growing from a one-phone game into a
fuller product. I need you to (1) establish a design language, (2) redesign every screen
for both platforms, (3) design the new features below, and (4) make the result feel
native on each platform while sharing one identity.

### 1. The product

A social deduction party game for **3–12 players in a room**. One secret word is drawn
from the dictionary and categories the group picked. Everyone except the spy (or spies)
sees the word; a spy sees only the **category** as their single clue. Players discuss
out loud, trying to work out who doesn't know the word, while spies bluff.

Design implications:

- Used in living rooms, bars and cars, often dim, often with a few drinks in. Big,
  forgiving targets and instant legibility beat density.
- The **role reveal is the heart of the app**: privacy, tension, a little theatre.
  Players glance for ~3 seconds, then hide it. Spend real design effort here.
- Content that is secret must never linger on screen; the hidden and revealed states
  must be unmistakably different (important when a phone is handed around).
- Sessions are short; opened, played, closed.

### 2. Two play modes (chosen at the start of a game)

**A. One phone ("Jedan telefon").** Today's flow: the phone is passed around, each player
privately views their role, then the phone goes down for discussion.

**B. Each on their own phone ("Svaki na svom telefonu").** New. A **host** creates a
room; others join with a **4-letter room code or by scanning a QR code**, over the
internet via a realtime server. No accounts: each player enters a temporary nickname.
Roles are delivered privately to each device. Timer is synchronised across devices.
The host is also a player.

Both modes share the same rules, round structure, setup options and visual language.

### 3. Game rules (design to these)

- **Roles:** 1+ spies (at most players−1). Citizens see the word + category. Spies see
  "You're a spy" + the category. **If there are several spies, each spy is shown who the
  other spies are** (by name) on their reveal screen.
- **Rounds:** the game does *not* end after one vote. Each round = discussion (with
  optional timer) → vote → one player is eliminated. Eliminated players become
  spectators (they still see the game, can't vote). The game continues until either
  all spies are eliminated (**citizens win**) or spies equal/outnumber the remaining
  citizens (**spies win**), since spies can outvote citizens. Design the round
  indicator, the "who's still in" list, and the elimination moment.
- **Voting is optional and chosen in setup:** "Vote in the app" or "Vote out loud".
  - *In the app:* voting is **anonymous**. Each player picks one person on their own
    device (in one-phone mode the phone is passed around for private votes). Votes stay
    secret until **all votes are in**, then results are revealed together. Design ties,
    "waiting for 3 more players" (it may show who hasn't voted yet, never who voted for
    whom), and a host override for stragglers.
  - *Out loud:* the app only runs the timer; the host marks who the group eliminated
    (or ends the round/game). Propose the lightest possible interaction.
- **Timer (optional):** host sets duration per round in setup (e.g. off / 3 / 5 / 8 min
  / custom). Visible countdown on all devices, a clear "time almost up" state, and when
  it ends it opens the vote (in-app mode) or sounds/haptics an alarm (out-loud mode).
  Host can end the discussion early.
- **Game over:** show the winning side, reveal all spies and the secret word, a short
  round-by-round recap, and "Play again" (same room/settings) and "New game".
- *Open question for you:* does an eliminated player's role get revealed immediately?
  Default assumption: yes (spy / not spy), but the word stays hidden until game over.
  Show it that way.

### 4. Dictionaries (new)

A **dictionary** contains **categories**, which contain **words**. Each dictionary has a
**language** (Hrvatski / English).

- **Built-in dictionaries (2):** "Hrvatski" and "English". Read-only for normal users,
  shown with a clear "built-in" marker. They are served from our server and cached for
  offline use. Today's PIN-protected editing of the shared list remains but becomes a
  low-prominence "admin" affordance (PIN sheet) — it must not clutter the normal UX.
- **Custom dictionaries:** created and stored **only on the user's device** — never on
  our server. Create, rename, set language, add/edit/delete categories and words,
  duplicate, delete (with confirmation). Duplicate-word and empty-category validation,
  empty states, and large lists must all work.
- **Import / export — local-first:**
  - File: export a `.json` file via the system share sheet; import via the system file
    picker (and by opening a `.json` file from other apps).
  - Share link / QR: show a QR and a share link so a friend sitting next to you can
    import instantly. Note the practical size limit (a link/QR can only carry a small
    dictionary): design the "too big, use a file instead" state.
  - Import flow must **preview before saving**: name, language, category and word
    counts, duplicates found, and choices (Save as new / Merge into existing / Cancel).
    Design the error states: invalid file, unsupported version, empty dictionary.
- **In setup**, the user picks one or more dictionaries, then categories within them
  (with word counts). In multi-device mode only the host's device needs the dictionary;
  **only the single chosen word travels to the room** — the custom dictionary itself
  is never uploaded. Mention this reassurance in a one-line UI hint where it fits.

### 5. Languages

The app UI ships in **Croatian and English**; default follows the device, with a
language switch in Settings. Design every screen so both languages fit — Croatian runs
longer and has plural forms ("je 1 špijun", "su 3 špijuna", "je 5 špijuna"). Typeface
must support Latin Extended-A (č ć š ž đ). The secret word must wrap on two lines
at its largest size ("Svemirska postaja", "Policijska postaja", "Nogometni stadion").
Show Croatian as the primary stress test; produce English copy yourself.

Key Croatian copy (from the current app; reuse and extend):
"Jedan telefon, jedna tajna riječ i špijun među vama." · "Igraj" · "Postavke igre" ·
"Broj igrača" · "Broj špijuna" · "Kategorije" · "Odaberi sve" · "Započni igru" ·
"Dodaj telefon igraču 2" · "Neka nitko drugi ne gleda u ekran." · "Prikaži" ·
"Tajna riječ" · "Ti si špijun!" · "Kategorija: …" · "Sakrij i sljedeći igrač" ·
"Rasprava je počela" · "Nova igra" · "Upravljaj riječima" · "Pokušaj ponovno" ·
"Nema veze sa serverom."

### 6. Screens and states to design

Design each for **iPhone** and **Android phone**; phones only. Dark and light themes.
Priority tiers: **P1** must be fully designed with all states, **P2** designed, with key
states, **P3** a single frame.

**P1**
1. **Home** — title/tagline, primary "Play" leading into mode choice (One phone / Each
   on own phone), secondary "Join with code" (code entry + QR scan). State: unfinished
   game ("Continue game").
2. **Game setup** — dictionary + category picker with counts; players (one-phone: count
   and optional names; required names if voting in-app); spies (stepper, "max N" hint);
   **timer** (off/presets/custom); **voting** (in app / out loud); "N words in selection"
   line; Start. States: loading, nothing selected, empty pool, offline.
3. **Lobby (multi-device)** — host view: room code + QR + share, joined players list
   (nicknames, host badge, connection state, remove player), min-players gate, Start.
   Guest view: joined, waiting for host. Join flow: enter code / scan, enter nickname,
   errors (room not found, room full, game already started).
4. **Role reveal (both modes)** — handoff (hidden) → revealed as citizen → revealed as
   spy (alone) → revealed as spy **with other spies listed**. Progress across players in
   one-phone mode; "waiting for others" after viewing in multi-device mode. Explore two
   reveal mechanics (tap-to-flip vs. press-and-hold), with haptics and choreography;
   recommend one. Spy vs. citizen must differ by shape/iconography/motion, not colour
   alone.
5. **Round / discussion** — round number, big countdown (or no timer), who's still in
   and who's eliminated, category chip, host controls (end discussion / open vote /
   add time). Both timer-on and timer-off, and the "time almost up" state.
6. **Voting (in-app)** — pick a player, confirm, then "waiting for N players" (no leak
   of who voted for whom). One-phone variant: pass-the-phone private voting. Spectator
   variant for eliminated players.
7. **Round result** — votes revealed together, the eliminated player and their role,
   tie handling, "Next round" or game over.
8. **Game over** — winners, spies revealed, secret word, round recap, Play again / New game.
9. **Dictionaries tab** — list of dictionaries (built-in vs custom badges, language, word
   counts), dictionary detail (categories → words), create/edit dictionary and words,
   delete confirm, empty state.

**P2**
10. **Import / export** — export sheet (file, link, QR), import preview + conflict
    resolution, QR scanner, size-limit state, error states.
11. **Settings** — language, theme (system/light/dark), haptics, sound, default timer,
    default player/spy count, default voting mode, keep screen awake, reveal mechanic,
    about; admin entry for built-in dictionary editing (PIN sheet).
12. **Connection & edge states (multi-device)** — reconnecting banner, player dropped,
    host left (hand over host / end game), kicked, offline when creating a room, app
    backgrounded and resumed mid-round.

**P3**
13. **App icon + splash** (iOS layered icon suited to Liquid Glass; Android adaptive icon
    with themed monochrome variant).
14. **Transient UI** — toasts, loading skeletons, destructive confirmations, permissions
    (camera for QR scanning).

**Navigation:** propose and justify it. Starting suggestion: a three-destination bar
**Play | Dictionaries | Settings** on the top level; the bar disappears entirely during a
game (immersive). Show how Join-with-code is reached.

### 7. Platform direction

**iOS — iOS 26 / Liquid Glass.** Follow Apple's HIG. Use Liquid Glass for the *navigation
and control layer only* (tab bar, navigation bar/toolbar, floating buttons, sheets),
never for content cards, and show it over real content. SF Pro / SF Symbols conventions,
large titles, native sheet detents, swipe actions, share sheet. Respect Dynamic Type,
Reduce Transparency, Reduce Motion (show fallbacks).

**Android — Material 3 Expressive.** Follow Material guidance: expressive shape/spring
motion, large top app bars, navigation bar, floating toolbar/FAB where it fits, button
groups, bottom sheets, edge-to-edge, predictive back, Android share sheet. Brand colour
stays **fixed** (role colours carry game meaning, so don't let Material You wallpaper
colours replace them); show how the tonal system derives from the brand seed.

The two apps must be recognisably the same game — shared colour, type voice, iconography,
copy and reveal choreography — while navigation, controls, sheets and motion feel native
to each OS. Don't force pixel parity.

### 8. Implementation constraints (important)

Built with **Kotlin Multiplatform + Compose Multiplatform**; shared UI is Compose. On
iOS, a few elements are **native SwiftUI embedded via interop** for Liquid Glass: tab bar,
navigation bar/toolbar, glass buttons. Therefore:

- Design tokens (colour, type, spacing, radius, elevation, motion) form one token set
  that maps to a Compose theme.
- Custom components must be buildable with standard Compose drawing, gradients,
  layering and animation APIs. No custom shaders or private platform APIs, apart from
  the native iOS glass chrome.
- Label every component **Shared (Compose)**, **Native iOS (SwiftUI)** or **Native
  Android (Material component)**.
- Content under glass bars must scroll underneath safely; show insets and safe areas.
- Realtime play means latency and disconnects exist; always show a sensible state.

### 9. Design language deliverable

Concise and usable, not a mood board:

- **Concept & personality**, with 2–3 rejected directions and why. Aim: modern,
  confident, a little mischievous. Avoid trench-coat/magnifying-glass cliché and avoid
  emoji as illustration. Dark-first (party in dim rooms) with a fully designed light theme.
  The current look (indigo `#6c5ce7` on near-black navy, generic web form feel) is
  replaceable; keep only what serves your concept.
- **Colour:** brand, neutral ramp, semantic roles (citizen, spy, success, danger,
  warning, eliminated/spectator), light + dark, contrast-checked (≥4.5:1 body, ≥3:1
  large text/UI).
- **Typography:** families per platform or a shared one, a scale including a display
  style for the secret word and a tabular numeric style for the countdown; diacritics
  verified.
- **Spacing, radius, elevation, iconography, motion** (durations, easings, springs,
  reveal and elimination choreography), **haptics map**, optional sound cues.
- **Components with states:** buttons (primary/secondary/destructive/glass), stepper,
  category row with count, dictionary row, player chip (connected/dropped/eliminated/
  host/spy), progress dots, countdown, role card, room-code display + QR card, code
  input, vote option, list row, text field, banner/toast, sheet, empty state.
- **Accessibility:** 44pt/48dp minimum targets, Dynamic Type/font scaling to 200%,
  screen-reader labels for every reveal and vote state, reduced motion/transparency,
  colour-blind-safe role distinction, countdown announced sensibly (not every second).

### 10. Out of scope

No accounts or profiles, no monetisation, no server-side storage of custom dictionaries,
no tablet or web layouts. If you see low-risk, high-value ideas beyond this, put them in
a separate labelled "Ideas" frame, not in the main flow.

### 11. How to work

Work in phases and stop after each for my feedback:

1. **Phase 1:** 3 distinct design directions, one hero frame each — the reveal screen as
   a spy with a fellow spy listed, iOS + Android. Recommend one.
2. **Phase 2:** the full design language for the chosen direction.
3. **Phase 3:** P1 screens with all states (iOS + Android, dark + light), then P2/P3.
4. **Phase 4:** component specs and annotated handoff (tokens, native vs shared, motion,
   copy keys EN/HR).

Start with Phase 1 only.

---

## Notes for you (not part of the prompt)

**Assumptions I made that you should confirm or change in the prompt:**

- Win condition: citizens win when all spies are out; spies win when spies ≥ remaining
  citizens. Eliminated player's role is revealed, the word is not until game over.
- The host is also a player; max 12 players in both modes.
- In-app voting needs player names, so one-phone mode gets optional names in setup
  (required if voting in-app).
- "Vote out loud" still lets the host mark who was eliminated, so rounds can continue.
- Built-in dictionaries stay server-synced + cached; PIN editing becomes a hidden admin
  entry.
- Share links/QR can only carry small dictionaries (a few KB); larger ones must use the
  file route. That limit is real because nothing is stored on a server.
- No accounts: multi-device identity is a temporary nickname per room.

**Engineering consequences (outside the design prompt, but worth planning):**

- Multi-device needs a realtime backend (e.g. Cloudflare Durable Objects + WebSockets).
  The server should only relay game state and the chosen word, never the dictionaries.
- Roles must be sent to each device individually (never broadcast all roles), and votes
  held server-side until everyone has voted.
- Reconnection, host migration and backgrounded apps are the hard parts of multi-device
  play; the design includes states for them on purpose.
