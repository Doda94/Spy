# Špijun — Motion, haptics & sound

Sources: canvas boards **06 Motion, haptics, sound** (`frames/P2-05-Motion.png`), **11 Table prototype** (`frames/P2-11-Table.png`, interactive in the canvas — press Play), **12 Table choreography** (`frames/P2-12-Storyboard.png`), **H·05 Motion & haptics implementation** (`frames/P5-05-Motion.png`), annotated reveal **H·03**. Token values are in `tokens.json › motion / haptics / sound`.

**Principle — "The table glides. Cards snap."** Table (UI) motion is short and quiet. Card motion is physical — lift, flip, settle — and **always paired with a haptic**, so a player feels which role they got before they read it.

All motion is standard Compose: `graphicsLayer`, `Animatable`, `animate*AsState`, `updateTransition`, `rememberInfiniteTransition`, `AnimatedContent`, `SharedTransitionLayout`. **No shaders.** Constants live in `SpyMotion`; haptics go through the `SpyHaptics` expect/actual interface.

---

## 1 · Tokens

| Token | Value | Use |
|---|---|---|
| `motion.press` | 100 ms · ease-out | scale 0.97 on press; Android buttons also morph shape md 16 → full |
| `motion.standard` | 220 ms · cubic-bezier(.2, 0, 0, 1) | table transitions, list changes, chips |
| `motion.emphasized` | 400 ms · cubic-bezier(.3, 0, 0, 1) | screen enter, results, hide card, table turn (700 ms variant) |
| `spring.card` | dampingRatio 0.75 · stiffness 400 | card flip settle |
| `spring.deal` | dampingRatio 0.8 · stiffness 200 | card dealt in, cards fanned at game over |
| lift | 480 ms spring, overshoot 1.08 | seat → centre shared-element |

Compose mapping: `spring(dampingRatio = 0.75f, stiffness = 400f)`, `spring(0.8f, 200f)`, `tween(220, easing = CubicBezierEasing(.2f, 0f, 0f, 1f))`, `tween(400, easing = CubicBezierEasing(.3f, 0f, 0f, 1f))`.

---

## 2 · The chosen reveal mechanic: **tap to flip, on the table**

Two mechanics were explored in Phase 1/2 — **tap-to-flip** (direction C "Karta") and **press-and-hold** (direction B "Pomrčina"). **Recommended & default: tap to flip** with auto-hide. **Hold to peek** ships as a Settings alternative (`settings_reveal_hold`): never lingers, but harder for some hands, so not the default.

The reveal happens on a **round table** that acts as the camera (boards 11–12): every player has a face-down card on a seat; the table turns the current player's seat towards the phone holder, the camera pushes in, the card lifts into their hands, they tap to flip, it auto-hides, drops back, and the table turns to the next seat — a visible "pass the phone".

### Full sequence (one-phone mode)

| # | Moment | Visual | Timing | Haptic | Compose |
|---|---|---|---|---|---|
| 01 | **Deal** (once, game start) | cards fly from the centre to each seat, one after another; pill "Dijeljenje" | stagger 70 ms · `spring.deal` · 8 cards ≈ 900 ms | `StepperTick` per card (+ deal sound) | per seat `Animatable(offset/scale/alpha).animateTo(target, SpyMotion.deal())` with `delay(i * 70)`; start: `translate(-x,-y) scale(0.6)` α 0 |
| 02 | **Turn & push in** | table rotates until this seat faces the bottom; camera scale 1 → 1.3; other cards dim to α 0.4; button "Telefon ide: Marko" (disabled look) | 700 ms · emphasized | — | `table.graphicsLayer { rotationZ = −(360/n)·seat; scaleX = scaleY = cam; transformOrigin = TableCentre }` via `animateFloatAsState(tween(700, Emphasized))` |
| 03 | **Lift** | the seat card lifts out and grows into the 300×420 RoleCard at centre (shared element, no cut); hint "Luka, dodirni kartu."; button **Prikaži ulogu** | 480 ms spring, overshoot 1.08 | `HideCard` (soft) on lift | `SharedTransitionLayout { Modifier.sharedElement(rememberSharedContentState("card-$seat")) }` |
| 04 | **Flip / reveal** (tap card or button) | lift 1.04 → rotateY 0→90° → **face swap** → 90→0° on `spring.card`, settle at −2° tilt; content staggers in 60 ms: glyph → role/word → category → other spies | 160 ms in → swap → spring out; ≈ 800 ms total | at swap: **citizen `RevealCitizen`** (one medium) · **spy `RevealSpy`** (heavy ×2, 90 ms apart) | `graphicsLayer { rotationY = flip; cameraDistance = 12f * density }`; `face = if (flip > 90f) Face else Back` (mirror the face content by `rotationY = 180` so text isn't reversed) |
| 05 | **Auto-hide** | 4 pt bar on the button's bottom edge empties linearly; label "Sakriva se za N s" | default 8 s (5 / 8 / off) | `HideCard` | `LaunchedEffect(flipped) { delay(autoHide); flipBack() }`; bar = `animateFloatAsState(linear)` |
| 06 | **Hide + pass** ("Sakrij i dalje") | flip back 300 ms emphasized → card drops back into its seat 480 ms → table turns one seat on 700 ms (the phone changes hands while it turns) | ≈ 1.5 s total | `HideCard` on drop | `flipBack(300) → drop(480) → camRot −= 360/n` |
| 07 | **Last player → Discuss** ("Sakrij i započni raspravu") | camera pulls back (scale 1.3 → 1), whole table visible, countdown in the middle, cards start breathing, table starts its slow turn | 700 ms | — | — |

Sound (optional, Settings › Zvukovi): card flick on flip, deal on deal.

### Own-phone (multi-device) variant
Same table on every device, but **your seat is always at the bottom** — no turning, the camera only pushes in on your own card. Other seats get a small neutral tick when that player has looked; roles never travel to other phones. After hiding: "Pogledaj ponovno" lets you peek again any time; the discussion starts when everyone has looked (`reveal_starts_when_all`).

### Hold-to-peek (Settings alternative)
Card flips (same 04 animation and haptic) on press-down and flips back on release/cancel/pointer-leave. Label "Drži za prikaz" → "Uloga je vidljiva samo dok držiš." No auto-hide bar (release is the hide).

### Spy vs citizen — never colour alone
Four independent signals plus hue: **card lightness** (dark spy face vs ivory citizen face), **suit shape** (split vs whole diamond), **wording** ("Špijun" vs the word), **haptic** (double thud vs single). Vermilion vs blue is the fifth signal, not the first.

---

## 3 · Discussion — idle tension on the table

| Tension | Trigger | Cards | Table / digits | Haptic |
|---|---|---|---|---|
| **Calm** | > 30 s (or timer off) | every third seat (0, 3, 6 …) drifts ±1.4° rotate and +1 → −4 px y, 3.2–4.6 s per card, phase offset −0.6 s × i, alternate — never in unison | table turns once every 90 s (linear); digits `text` | — |
| **Uneasy** | ≤ 30 s | every seat now drifts, speeding up to 1.3–1.9 s (phase −0.3 s × i) | time arc + digits turn `warning` (the ring too when there is no timer); sub-line "Još malo!" | `ThirtySeconds` once |
| **Nervous** | ≤ 10 s | tremble ±1 px / ±1°, 0.26–0.32 s linear | digits pulse 1.0 → 1.07 each second | — |
| **Frozen / time up** | 0 | everything stops | "Vrijeme!", `warning` edge flashes twice | `TimeUp` alarm (iOS `.error` + Core Haptics 1.2 s; Android 3-pulse waveform) + bell sound |

At 0: in-app voting opens automatically; out-loud mode waits for the host. Ambient turn pauses at 0 s.

Reference keyframes from the prototype (CSS → Compose):
```css
@keyframes drift   { from { transform: translate(0, 1px) rotate(-1.4deg) } to { transform: translate(0, -4px) rotate(1.4deg) } }   /* alternate, infinite */
@keyframes tremble { 0% { transform: translate(0,0) rotate(0) } 25% { transform: translate(-1px,.6px) rotate(-1deg) } … }      /* linear, infinite */
@keyframes tableSpin { to { transform: rotate(360deg) } }   /* 90 s linear */
@keyframes pulse   { 50% { transform: scale(1.07) } }       /* 1 s */
```
Compose: `rememberInfiniteTransition().animateFloat(-1.4f, 1.4f, infiniteRepeatable(tween(d), RepeatMode.Reverse))` per card, `d` per tension level; ambient turn `infiniteRepeatable(tween(90_000, easing = LinearEasing))`.

### Countdown digits
`AnimatedContent(targetState = secs, transitionSpec = { slideInVertically { it } + fadeIn() togetherWith slideOutVertically { -it } + fadeOut() })`, 120 ms per digit change, tabular figures so width never jumps. ≤10 s pulse 1.0 → 1.07.

---

## 4 · Elimination — knock-out, **no reveal**

Identical for spies and citizens — the role is **not** shown until game over.

1. The player's seat card lifts out of the table and becomes a face-down card at centre (300 ms).
2. **Suspense:** 700 ms hold, three light ticks (`SuspenseTick` ×3 — iOS `.light` ×3, Android `PRIMITIVE_TICK` ×3).
3. **Knock-out:** the card is flicked off the table, spinning out to the side — `rotationZ 0 → 540°`, `translationX` → off-table, 280 ms — with one heavy thud (`KnockOut`: iOS `.heavy`, Android `PRIMITIVE_THUD`) + low drum sound.
4. It lands face-down on the result screen / "Izbačeni" pile stamped **IZVAN IGRE**; on the table the seat stays as a faint dashed empty seat; in lists the name gets a strikethrough.

Total ≈ 1.3 s. Then `result_out` "Ana ispada iz igre" + tally bars (bars grow with `motion.emphasized`).

**Tie:** both cards come forward side by side with an "=" medallion; no knock-out animation. **Second tie:** cards return to their seats ("Obje karte ostaju na stolu").

---

## 5 · Game over reveal

1. All seats flip face-up **in place**, spies first, 120 ms stagger (same flip as §2-04, no tilt).
2. Spy cards + the ivory word card are dealt to the front in a fan (`spring.deal`), ≈ 2 s total.
3. Haptic `GameOverReveal` (iOS `.heavy` ×2 + `.success`; Android `THUD` ×2 + `CONFIRM`). Sound: soft chime.
4. Recap list fades/slides in (`motion.standard`).

---

## 6 · Other moments

| Moment | Spec | Haptic |
|---|---|---|
| Button press | scale 0.97, 100 ms; Android shape morph md → full (M3 Expressive `motionScheme`) | — |
| Stepper / chip change | value cross-fade, `motion.standard` | `StepperTick` |
| Vote cast | success ring draws (check stroke), `motion.emphasized` | `VoteCast` |
| All votes in | results revealed together | chime sound |
| Error (room code, PIN) | `translationX` keyframes ±8, ±6, ±3 — 300 ms, 3 shakes | `Error` |
| Screen enter / results | `motion.emphasized` | — |
| Sheet | native (iOS detent spring / M3 sheet) | — |
| Toast | slide up + fade, 4 s, swipe to dismiss | — |
| Skeleton | pulse α 0.5 ↔ 1, 1.2 s (only if loading > 300 ms) | — |
| Splash (Android 12+) | the card does one 300 ms flip as the exit animation, then the table deals in | — |

---

## 7 · Haptics map

| Event | `SpyHaptic` | iOS | Android |
|---|---|---|---|
| Stepper / chip change; each card dealt | `StepperTick` | `UISelectionFeedbackGenerator` | `SEGMENT_TICK` (API 34) · fallback `CLOCK_TICK` |
| Reveal · citizen | `RevealCitizen` | Impact `.medium` | `CONFIRM` |
| Reveal · spy | `RevealSpy` | Impact `.heavy` ×2, 90 ms apart | Composition `PRIMITIVE_THUD` ×2 |
| Hide card / lift / drop | `HideCard` | Impact `.soft` | `CLOCK_TICK` |
| Vote cast | `VoteCast` | Notification `.success` | `CONFIRM` |
| Elimination suspense | `SuspenseTick` | Impact `.light` ×3 | Composition `PRIMITIVE_TICK` ×3 |
| Knock-out (every role) | `KnockOut` | Impact `.heavy` | Composition `PRIMITIVE_THUD` |
| Game over · roles revealed | `GameOverReveal` | `.heavy` ×2 + Notification `.success` | `THUD` ×2 + `CONFIRM` |
| 30 s left | `ThirtySeconds` | Notification `.warning` | Composition `PRIMITIVE_QUICK_RISE` |
| Time up (alarm) | `TimeUp` | Notification `.error` + Core Haptics pattern 1.2 s | Waveform, 3 pulses |
| Error | `Error` | Notification `.error` | `REJECT` |
| QR found | `ScanFound` | Notification `.success` *(not specified in design — suggested)* | `CONFIRM` *(suggested)* |

Settings › Vibracija off disables all of them. Haptics **stay on under Reduce Motion** — they carry meaning, not decoration. Android: check `Vibrator.areAllPrimitivesSupported()`; fall back to `HapticFeedbackConstants` / one-shot `VibrationEffect` when compositions are unavailable.

## 8 · Sound cues

Optional, Settings › Zvukovi (default **on**), silenced by the ringer switch / DND. Each < 400 ms: **flick** (card flip) · **deal** · **low drum** (elimination) · **bell** (time up) · **soft chime** (all votes in). No audio assets exist in the design — they need to be sourced.

---

## 9 · Reduced motion

Triggered by iOS **Reduce Motion** / Android **Remove animations** (`ANIMATOR_DURATION_SCALE == 0`). Settings › Otkrivanje shows `settings_reduce_motion` when it's on.

| Moment | Full | Reduced |
|---|---|---|
| Deal | staggered fly-in | all seats fade in together, 200 ms |
| Table turn & push in | rotation + 1.3× scale | no rotation, no scale; **ivory ring on the current seat** |
| Lift | shared-element grow | cross-fade 200 ms |
| Flip | rotateY + tilt + stagger | **cross-fade 200 ms with scale 0.98 → 1**; haptic kept |
| Pass to next | flip back, drop, turn | cut to next seat with ring |
| Idle drift / tremble / ambient turn | on | **off** — static cards; tension shown by ring/digit colour only |
| Countdown digits | roll + pulse | digits swap without roll; no pulse |
| Knock-out | spin off table | card fades to the pile; same haptics |
| Game over | flip in place + deal fan | fade in face-up; list appears |
| Buttons | scale / shape morph | no scale |
| Errors | shake | no shake — colour + text only |
| Skeleton | pulse | static (no shimmer sweep) |

## 10 · Reduced transparency & increased contrast

- **Reduce Transparency (iOS):** all glass chrome → solid `surface2` with a 1 pt `line` stroke (tab bar, nav bar, glass buttons, pills, sheets).
- **Increase Contrast:** `line` → n400 `#6B645C`, `textMuted` → n700 `#C9C2B9`, card frame stroke 2 pt.
- Android has no glass; tonal surfaces already satisfy both.

## 11 · Privacy rules that are also motion rules

- On app background (`ON_STOP` / `scenePhase == .background`) every face-up card flips to back **instantly, no animation**.
- App-switcher snapshot is blurred on iOS; Android sets `FLAG_SECURE` while a role can be on screen.
- Card shadows render only while a card is lifted or dealt.
- Toasts are paused during a reveal; secrets never appear in titles, toasts, notifications or the app switcher.
