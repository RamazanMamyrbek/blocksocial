# BlockSocial — Claude Design Export Analysis

**Document:** `DESIGN_EXPORT_ANALYSIS.md`
**Status:** Draft v1
**Date:** July 27, 2026
**Analyzed artifact:** `Design tokens and foundations (1).zip`
**Input it was generated from:** `UI_UX_BRIEF.md` draft v1
**Related documents:** `UI_UX_BRIEF.md`, `docs/PRODUCT.md`, `docs/ARCHITECTURE.md`

---

## 1. Purpose

This document records what the first Claude Design pass produced, how the exported package is structured, which design decisions it contains, and which questions it leaves open.

The export now lives in the repository at `design/files/`. This analysis exists so the design decisions are readable without opening it, and so the Android implementation phase can start from repository text rather than from a browser tab.

The export is a design record, not production code. `support.js` is a generated bundle whose own header says it must not be edited, and `android-frame.jsx` is a copied starter scaffold marked `@ds-adherence-ignore`. Neither is part of the application. Do not edit, reformat, lint, or refactor anything under `design/files/` without a specific reason; replace the folder wholesale when a new design pass is exported.

Nothing in the export is approved yet. Section 9 lists the items that require a decision.

---

## 2. Package Contents

The archive contained five files and one directory. All of it except the duplicated brief is now checked in under `design/files/`.

| File | Size | Role |
|---|---|---|
| `BlockSocial Design.dc.html` | 81 829 B | the design document itself, all screens and tokens |
| `support.js` | 69 150 B | generated Claude Design runtime, renders the document |
| `android-frame.jsx` | 10 316 B | reusable Android device frame component |
| `.thumbnail` | 968 B | WebP preview image of the document |
| `uploads/UI_UX_BRIEF.md` | 21 692 B | the brief uploaded as input, removed as a duplicate |

The uploaded brief was byte-identical to `design/UI_UX_BRIEF.md`, verified by SHA-256 `03f6fe69…8d6bf77`, so the design reflects the current brief rather than an earlier draft. The duplicate copy inside the export has since been removed.

---

## 3. How the Document Is Built

### 3.1 Runtime

`BlockSocial Design.dc.html` is not a plain page. It is a template consumed by `support.js`, a generated bundle described in its own header as built from `dc-runtime/src/*.ts`.

The document body is wrapped in a custom `<x-dc>` element. The runtime parses that element, compiles the markup, and renders it with React.

Consequences for anyone opening the file:

- The three files must stay in the same directory. The HTML references `./support.js` and `./android-frame.jsx` by relative path.
- Rendering requires internet access. The runtime injects React 18.3.1, ReactDOM 18.3.1, and Babel Standalone 7.29.0 from `unpkg.com` with subresource-integrity hashes, and the page loads Manrope and IBM Plex Mono from Google Fonts.
- Opening the HTML offline shows unstyled or partially rendered content. This is a property of the export format, not a defect of the design.

### 3.2 Custom Markup

The runtime adds a small vocabulary on top of HTML.

| Construct | Meaning |
|---|---|
| `<x-dc>` | root of the design document |
| `<helmet data-dc-atomics>` | head content: fonts and the document's own stylesheet |
| `<x-import component-from-global-scope="…" from="./android-frame.jsx">` | mounts a React component around static markup |
| `hint-size="412px,892px"` | declared render size for an imported component |
| `style-hover="…"` | hover styling, applied by the runtime rather than by CSS |
| `data-om-starter` | inert marker the tooling uses to detect starter components |

`style-hover` is not standard CSS. It has no meaning outside this runtime and no equivalent in the implementation; it exists so the mockups can demonstrate pressed and hovered states.

### 3.3 Document Organization

The document is organized into *turns* and *options*.

- A turn is one request to the design tool, rendered as `<section class="dv-turn" id="tN">`.
- An option is one deliverable inside that turn, rendered as `<div class="dv-opt" id="Nx">` with a stable anchor such as `#1c` or `#2e`.
- Each option ends with `<p class="dv-ann">`, a short rationale note explaining the decisions and citing the brief section it satisfies.
- Each turn ends with `<p class="dv-next">`, suggested follow-up requests.

Turns appear in the file newest first: turn 2 (component library, lines 67–317) precedes turn 1 (tokens and block screens, lines 319–552). Read turn 1 first.

The document header states four assumptions the designer made and three things deliberately not designed. Those are reproduced in section 8 below.

### 3.4 `android-frame.jsx`

A self-contained Material 3 device frame with no dependencies and no image assets. Its header marks it as a copied starter scaffold with `@ds-adherence-ignore`, meaning its own raw hex values and pixel sizes are intentional and are not part of the BlockSocial design system.

Exports, attached to `window`: `AndroidDevice`, `AndroidStatusBar`, `AndroidAppBar`, `AndroidListItem`, `AndroidNavBar`, `AndroidKeyboard`.

`AndroidDevice` props: `width` default 412, `height` default 892, `dark`, `title`, `large`, `keyboard`. It renders a bezel, a status bar with a punch-hole camera, an optional top app bar, the content slot, an optional Gboard mock, and a gesture-nav pill.

This file is a presentation device only. It contains a Material 3 sample palette such as `#006a60` and `#f4fbf8` that has nothing to do with BlockSocial tokens. Do not mine it for product colors.

---

## 4. Turn 1 — Foundations and the Block Screen

### 4.1 Option 1a — Foundations

All color roles are authored in `oklch()`. Dark theme is authored first, light theme is described as authored rather than derived.

Hex values below are recomputed from the `oklch()` sources for implementation convenience. They are approximate; browsers may gamut-map slightly differently. Treat `oklch()` as the source of truth and regenerate hex during implementation.

Section 12 records the regeneration performed in phase 09 and the conversion that produced it. Every hex in the two tables below was reproduced exactly, so no value here needed correcting.

#### Dark theme

| Role | Source | Approx. hex |
|---|---|---|
| surface | `oklch(.20 .012 240)` | `#11171b` |
| surface-container | `oklch(.245 .014 240)` | `#1b2127` |
| surface-container-high | `oklch(.30 .016 240)` | `#272f35` |
| on-surface | `oklch(.94 .006 240)` | `#e8ecef` |
| on-surface-variant | `oklch(.73 .012 240)` | `#a1a9af` |
| outline | `oklch(.45 .012 240)` | `#50565b` |
| primary | `oklch(.75 .078 196)` | `#6fbebe` |
| success | `oklch(.74 .070 158)` | `#85b99a` |
| warning | `oklch(.80 .082 78)` | `#dbb881` |
| danger | `oklch(.66 .105 26)` | `#cb7870` |

#### Light theme

| Role | Source | Approx. hex |
|---|---|---|
| surface | `oklch(.978 .004 240)` | `#f5f8fa` |
| surface-container | `oklch(.945 .006 240)` | `#e9edf0` |
| surface-container-high | `oklch(.905 .008 240)` | `#dbe0e4` |
| on-surface | `oklch(.245 .014 240)` | `#1b2127` |
| on-surface-variant | `oklch(.475 .013 240)` | `#565d63` |
| outline | `oklch(.795 .009 240)` | `#b7bdc1` |
| primary | `oklch(.50 .075 196)` | `#207071` |
| success | `oklch(.48 .068 158)` | `#3a694f` |
| warning | `oklch(.60 .080 68)` | `#a0774a` |
| danger | `oklch(.52 .115 26)` | `#a14b45` |

Direction: cool graphite surfaces with a single desaturated teal accent, low chroma throughout. Warning is amber, used for degraded protection. Danger is reserved for one screen only, deleting all local data.

Elevation is expressed as a step in the surface-container ramp, not as a shadow, so it survives dark theme.

#### Type scale

| Token | Value | Platform mapping |
|---|---|---|
| display-sm | 32 / 700 | `headlineMedium` |
| title-lg | 22 / 600 | `titleLarge` |
| label-lg | 16 / 600 | `labelLarge` |
| body-lg | 15 / 400 | `bodyLarge` |
| numeric | IBM Plex Mono 13 / 500 | — |

Two families: Manrope for interface text, IBM Plex Mono for times, counts, and metadata, so measured values read as measurements. Both are open-licensed and would need bundling in the application; neither is a system font on Android or iOS.

#### Spacing, radius, motion

- Spacing scale: 4, 8, 12, 16, 24, 32, 48.
- Radius scale: 4, 8, 14, 20, full.
- Motion: block-appear 120 ms linear fade with no scale; state-change 180 ms standard easing; sheet 240 ms emphasized decelerate; all durations collapse to 0 ms under Reduce Motion.

#### Non-color state encoding

Every semantic state carries a shape as well as a hue: filled square for success or stayed, hollow ring for a bypass, triangle for warning, dashed square for not-asked or absent. A greyscale screenshot still parses. This is the mechanism that satisfies the brief's rule against color-only meaning.

### 4.2 Options 1b, 1c, 1d — Three Block Screens

Three competing directions were produced for the Android block screen, all dark theme, all inside the 412 × 892 device frame.

**1b, "Quiet".** The minimum that can be on screen: app icon and "You opened Instagram", a 34 px headline naming the rule and its end time, one supporting sentence, then a 60 dp primary button and a 56 dp secondary button, both full width. A single monospace footnote states "4 of 6 opens today ended here". Focus order is title → primary → secondary → footnote, so a screen-reader user reaches the way out first.

**1c, "Held door".** The layout itself expresses the pause. A centre band is held between two horizontal rules, with empty space above and below. Inside the band: a 3 dp vertical progress column showing elapsed versus remaining time in the rule window, a 25 px headline "A rule is holding this open door", the rule's schedule and remaining time in words, then a 62 dp primary button carrying a filled square glyph and a 50 dp borderless secondary carrying a hollow ring. A closing monospace line reads "nothing here is locked · you can change or delete this rule at any time". The annotation marks this option provisional and dependent on spike `A-02`, with 1b as the fallback because it needs no band.

**1d, "Informed".** Adds today's record: an eyebrow label, the app name at 30 px, a rule card with time remaining, and a row of six marks — filled squares for stayed, hollow rings for opened — with a two-item legend reading "4 stayed / 2 opened". Buttons are pill-shaped. The annotation itself flags this as the densest option and the one most likely to break the three-second rule.

All three keep the bypass visible and unpenalized, and none uses a countdown gate, a nag pattern, or the danger color.

### 4.3 Option 1e — Light Theme of 1c

Not a hue inversion. The held band is lighter than the page in dark theme and darker than the page in light theme, because in both cases the band must read as the nearer surface. Primary drops from `oklch(.75 .078 196)` to `oklch(.50 .075 196)` so that white text on the primary button clears 4.5:1.

Only 1c received a light-theme rendering. 1b and 1d exist in dark only.

### 4.4 Option 1f — Stress Test

The most valuable artifact in the pass: 1c rendered simultaneously at the largest font scale and in Russian, which is the worst realistic case for this layout.

Documented behavior:

- The held panel keeps `flex: 0 0 auto` and sizes to its content; the two outer regions take `flex: 1` with `min-height: 0`, so the empty space above and below is what shrinks. The band is the last element to yield.
- Buttons switch from fixed `height` to `min-height` and wrap to two lines rather than truncate.
- The closing reassurance line drops its second sentence at this scale, on the grounds that it is reinforcement rather than information.
- Russian runs roughly 30 percent longer and still fits without an ellipsis.

Russian strings used: "Дверь придерживает правило", "Вечера по будням", "Вернуться к делу", "Открыть на 5 минут", "ничего не заблокировано навсегда". These are design placeholders and have not been reviewed as product copy.

---

## 5. Turn 2 — Component Library

Eight options, each rendered at 428 px wide, most showing dark and light variants and all mandatory states.

| ID | Component | States covered |
|---|---|---|
| 2a | Buttons | primary, secondary, tonal, destructive, text; enabled, pressed, disabled; dark and light |
| 2b | Rule card | active, overnight, disabled, iOS capacity error |
| 2c | Application row | installed, selected, not installed, iOS opaque selection, iOS selection lost |
| 2d | Protection health item | healthy, failed with repair, OEM guidance, not-asked |
| 2e | Statistic tile | measured, estimated, hidden metric, streak |
| 2f | History row | stayed, opened, rule turned off, empty state |
| 2g | Permission explanation card | before asking, granted, denied, revoked later |
| 2h | Empty, loading, offline, error | all four |

Decisions worth carrying into implementation:

- **Button heights** 56 / 52 / 50 dp, all above the 48 dp minimum, primary always the tallest. Disabled is a surface step plus a lowered on-surface color, never whole-button opacity, so the label keeps 4.5:1.
- **Destructive** is outlined rather than filled and carries a triangle glyph, so it is never red alone.
- **Overnight intervals are stated three ways**: the word "overnight", a two-segment 24-hour bar with a labelled midnight crossing, and the plain times. This directly answers the brief's requirement that `22:00–07:00` cannot be misread.
- **Capacity error copy** names the exact rules to turn off and confirms that nothing was silently truncated: "iOS can watch 20 things at once. This rule would be the 21st."
- **Not-installed is a normal row**, not an error: dashed placeholder, plain explanation, and `N/A` instead of an unchecked box that would imply it could be selected.
- **iOS never fakes what it cannot see**: a count such as "7 apps and 1 category selected", an explanation that iOS keeps the selection private, and a button that reopens the system picker.
- **Protection health uses four status shapes**, not four colors: filled square with a tick, triangle, ring with an *i*, dashed square. Every failure states what stops working *and* what keeps working, then offers one repair button.
- **Permission cards use four fixed slots** in a fixed order: what it enables, what is read, what is never read, how to revoke. A line under the buttons states "the next screen is Android's own, not ours", which is how the card avoids reading as a system sheet.
- **History gives a bypass row the same size, type, and position as a stayed row.** Only the leading glyph differs. No red, no strikethrough, no dimming.
- **Loading skeletons are static**, with an explicit note that shimmer loops are forbidden by the brief's motion rule.
- **Offline is documented as a non-state**, with a card saying there is no connection warning anywhere in the app, so nobody adds one later.

---

## 6. Statistics Handling

Turn 2 option 2e is the concrete answer to the brief's honesty rule.

- Measured values are plain: "Stayed focused 4, of 6 opens today", "Opened anyway 2, 5 minutes each", "Declined 67% of opens, last 7 days".
- The single estimated value is marked in three channels at once: an `EST` badge, a dashed border, and a tilde in the number itself — "Time in app ~10 min, approximate, from grant length".
- The unmeasurable metric is named and explained rather than hidden silently: "**Hours saved** is not shown. BlockSocial cannot measure it honestly, so it is absent rather than reported as zero."
- The streak explains its own rule inline, before it can be lost, and never appears in a warning color.

---

## 7. Coverage Against the Brief

### Delivered

- design tokens for both themes, section 12 of the brief
- component library covering every component the brief lists
- Android block screen in three directions, plus a light variant and a large-scale Russian stress test
- all mandatory cross-cutting states from brief section 9
- rationale notes citing brief sections throughout
- provisional markers tied to spikes `A-02`, `A-04`, `I-06`

### Not yet delivered

- assembled screens: dashboard, rule editor, history screen, settings, onboarding flow. Turn 2 delivers the components these screens are made of, not the screens themselves.
- every iOS surface, including the shield copy, which the brief defines as copy and configuration rather than a mockup
- light-theme renderings of block screens 1b and 1d
- landscape orientation, which the brief requires for the Android block screen
- a formal accessibility annotation artifact; accessibility reasoning currently lives inside prose notes
- an English copy deck with localization placeholders
- a screen catalog document

This distribution is expected. The brief instructs the designer to work one screen group per request, and the export's own header says the pass covers the token system and the signature surface only.

---

## 8. Assumptions the Design Made

Stated in the export header:

1. No existing design system, so tokens were designed from scratch.
2. Dark theme authored first, because night scrolling is the core scenario.
3. Copy written as real product copy rather than lorem, but every string is a localization placeholder.
4. The bypass grant is shown as five minutes wall-clock, the Android baseline.

Deliberately not designed: accounts, sync, paywalls, ad slots, coach surfaces; countdown gates and nag patterns; a shared Android/iOS block mockup; any connectivity state.

---

## 9. Findings Requiring a Decision

### 9.1 Streak rule conflicts with the concept — resolved

Resolved in favour of `docs/PRODUCT.md`: a day counts when bypasses that day are at or below a configured maximum, default two. The export's copy in option 2e is wrong and must be replaced when the dashboard is designed. The original finding follows.

Option 2e states: "A streak counts days where you stayed with every rule. Opening an app ends it, and it starts again the next day."

`docs/PRODUCT.md` records the opposite as the working recommendation: a day counts when the user does not exceed a configured maximum number of bypasses, and a streak should not disappear because of one action. It is tracked there as open question `Q-01`.

The design's rule is stricter than the concept's and turns a legitimate bypass into a streak-ending event, which sits uneasily beside the principle that a bypass is not a failure. This must be resolved in favor of one document before the dashboard is designed.

### 9.2 Estimated time is derived from grant length, not from usage — resolved

Option 2e derives "Time in app" from the length of granted bypasses. Spike `A-05` has since returned and `Q-03` is answered in `docs/PRODUCT.md`: the metric comes from Android usage statistics read as events, not from grant length. The design's provenance label is therefore wrong and the tile must be relabelled when the dashboard is designed. The `EST` badge, the dashed border and the tilde stay, because the measurement is still approximate.

### 9.3 Contrast figures in the export are approximate

The export annotates several pairs with contrast ratios. Recomputed independently from the `oklch()` sources:

| Pair | Stated | Recomputed |
|---|---|---|
| dark on-surface on surface | 13.9:1 | ≈ 15.2:1 |
| dark on-surface-variant on surface | 7.2:1 | ≈ 7.6:1 |
| light on-surface on surface | 14.1:1 | ≈ 15.2:1 |
| light on-surface-variant on surface | 7.4:1 | ≈ 6.3:1 |
| light primary on surface | 5.6:1 | ≈ 5.4:1 |

Every pair still clears the 4.5:1 threshold, so no token fails. The figures in the export are estimates rather than measurements, and one of them overstates the result. Contrast must be measured properly during the design system sign-off rather than quoted from these notes.

### 9.4 Fonts are not free of consequence

Manrope and IBM Plex Mono are both open-licensed, but neither ships with Android or iOS. Bundling two families increases the APK and the iOS bundle, and the mono family is used for small text where hinting matters. A fallback strategy for Dynamic Type and for the largest font scales must be decided before implementation.

### 9.5 Device coverage is a single size

Everything is rendered at 412 × 892. The brief requires the block screen to be decidable without scrolling on the smallest supported device. That case has not been tested.

### 9.6 The block screen direction is unchosen — resolved

Three directions existed and none was selected. `A-02` has since returned and `docs/GATE_ANDROID.md` answers `Q-02`: the baseline is **1b, "Quiet"**. 1c stays a candidate for a later design pass. 1d is not implemented until a real user test exists.

### 9.7 Three roles cannot carry text at the contrast the brief requires

Section 9.3 recomputed five pairs, all of them `on-` roles, and all of them pass. Phase 09 measured **every** role against every surface step, which the export never did. Three results need a decision.

| Role | Theme | On `surface` | On `surfaceContainer` | On `surfaceContainerHigh` |
|---|---|---|---|---|
| `warning` | light | 3.76:1 | 3.41:1 | 3.02:1 |
| `outline` | dark | 2.43:1 | 2.18:1 | 1.83:1 |
| `outline` | light | 1.78:1 | 1.61:1 | 1.43:1 |
| `primary` | light | 5.44:1 | 4.93:1 | 4.37:1 |
| `danger` | dark | 5.56:1 | 4.99:1 | 4.18:1 |

**`warning` cannot be text in light theme.** It clears 3:1 everywhere, so it is sound as a fill, a glyph or a border, but amber warning *text* on a light surface fails 4.5:1 on every step. The protection health item, option 2d, is the surface where this matters.

**`outline` clears no contrast threshold at all**, not even the 3:1 that WCAG asks of a control boundary. As a divider that is correct and intended; the problem is that option 2a makes the destructive button *outlined rather than filled*, so on that one control the outline is what identifies it. Either that button needs a second cue beyond the triangle glyph it already carries, or it needs a stronger border color.

**`primary` and `danger` fall just under 4.5:1 on `surfaceContainerHigh`** — 4.37 and 4.18. Both are fine on the two lower steps. A colored label on the highest surface step is the case to avoid.

None of this is a token defect. The palette is deliberately low-chroma and every role clears the non-text minimum. What it means is that the *usage* rules have to be written down, because the numbers do not permit "any role may be text on any surface". The design system sign-off named in 9.3 is where that belongs.

The token tests in `android/core-ui` encode what is settled: 4.5:1 for every `on-` role on every surface step, 4.5:1 for `onPrimary` on `primary` and on `danger`, and 3:1 for every accent role. `outline` is deliberately excluded from the ratio gate and asserted only to differ from each surface step.

---

## 10. How to Reopen the Export

1. Open `design/files/BlockSocial Design.dc.html` in a browser with internet access, because React, ReactDOM, Babel, and both fonts load from external hosts.
2. Keep the three files together. The HTML resolves `./support.js` and `./android-frame.jsx` by relative path, so moving or renaming either one breaks rendering.
3. Navigate by anchor: `#1a` foundations, `#1b`/`#1c`/`#1d` block screens, `#1e` light theme, `#1f` stress test, `#2a`–`#2h` components.

Treat the folder as read-only. Design decisions that must survive belong in this document and in the screen catalog, not in the export, because a later design pass replaces the export entirely.

---

## 11. Recommended Next Passes

1. Resolve section 9.1 as open question `Q-01` in `docs/PRODUCT.md`, since it changes dashboard and block-screen copy.
2. Choose the block-screen baseline as described in 9.6.
3. Request the dashboard in healthy and degraded states, assembled from 2b and 2e.
4. Request the rule editor with the overnight picker and the capacity error from 2b.
5. Request the iOS pass: authorization explanation, selection summary, and shield copy in English and Russian.
6. Request light-theme renderings for the chosen block screen if it is not 1c.
7. Produce the screen catalog and the copy deck, then re-check every provisional marker against spike results.

---

## 12. Token Conversion Record

Written in plan phase 09, when the tokens were implemented in `android/core-ui`.

### 12.1 How the hex values were derived

`oklch()` → OKLab rectangular coordinates → LMS → linear sRGB → sRGB transfer function → 8-bit channels, rounded half-up. D65 throughout, no chromatic adaptation, no gamut mapping.

No channel of any of the twenty tokens fell outside `[0, 1]` before clamping, so nothing was gamut-mapped and no token lost chroma in conversion. This matters: the palette is low-chroma by design, and that is what keeps it inside sRGB.

The conversion was run twice, in two languages, by two independent implementations written from the same formula — once to produce the values and once inside the test suite to check them. Both agree, and both reproduce the export's own approximate table **exactly**, all twenty values. The recomputed contrast figures also reproduce the five in section 9.3, including the one that overstates its pair.

### 12.2 Where the tokens live and what stops them drifting

`android/core-ui/src/main/kotlin/com/blocksocial/core/ui/theme/Palette.kt` holds the twenty values as `Color` constants.

`ColorTokenConversionTest` holds the `oklch()` triples copied from section 4.1 and re-derives every one at test time. Editing a hex without editing the documented source, or the reverse, fails the build. The design document is the source of truth in a way the build can check, which is what risk "colors drift from the design" asked for.

### 12.3 Roles the design did not define

The export defines ten roles per theme. Two additions were needed to render anything.

| Addition | Value | Reason |
|---|---|---|
| `onPrimary`, dark | `surface`, `#11171B` | dark `primary` is light, so its label must be dark. 8.42:1 |
| `onPrimary`, light | `#FFFFFF` | section 4.3 states white text on the light primary button. 5.81:1 |

Material 3 requires forty-nine color slots; the design defines ten. Every slot is mapped from a documented token, with `scrim` as the single exception — pure black, which is the platform convention and carries no hue. `ColorSchemeTest` asserts that no slot holds a color outside the documented palette, so a stray Material default cannot reach the screen.

Mappings worth knowing: `error` is `danger`, `surfaceVariant` is `surfaceContainer`, `secondary` and `tertiary` are both `primary` because the design has one accent, and `surfaceBright`/`surfaceDim` swap between themes because the surface ramp runs in opposite directions.

### 12.4 What was implemented and what was deferred

The type scale carries the documented sizes and weights. **The families are the platform defaults, not Manrope and IBM Plex Mono**, because finding 9.4 is unresolved and bundling two families is a decision with size and licensing consequences. Measured values use `FontFamily.Monospace` and prose uses `FontFamily.Default`, so the *distinction* the design makes is live and only the specific faces are pending. Line height is left unset because the design does not specify one; the platform derives it from font metrics rather than inheriting a Material default sized for a different scale.

Spacing, radius and motion are implemented as documented. Reduce Motion reads the system animator duration scale and collapses all three durations to zero, live, without an application restart.

`danger` is defined, converted, tested and mapped to the Material `error` slot. No product surface references it, per the export's rule that it is reserved for deleting all local data. It appears in the debug token preview because that screen's purpose is to show every token.
