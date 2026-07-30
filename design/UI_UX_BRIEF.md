# BlockSocial — UI/UX Brief

**Document:** `UI_UX_BRIEF.md`
**Status:** Draft v1
**Date:** July 27, 2026
**Stage:** design input, produced before UI implementation
**Related documents:** `docs/PRODUCT.md`, `docs/TECHNICAL_SPECIFICATION.md`, `docs/ARCHITECTURE.md`

---

## 1. Purpose of This Document

This brief is the single input for the visual and interaction design phase, including AI design tools such as Claude Design.

It defines what must be designed, what must never be designed, and which platform constraints are non-negotiable. It does not define final colors, final typography, or final layout. Those are outputs of the design phase.

The brief is provisional. Several blocking mechanisms are still unverified, so any screen that depends on them is marked accordingly in section 18.

### How to Use It With a Design Tool

1. Provide sections 2 to 5 as product and constraint context.
2. Request one screen group at a time, using the prompt pack in section 17.
3. Reject any output that violates section 4, section 6, or section 15.
4. Record accepted screens in a separate screen catalog document.

Do not ask a design tool for the entire application in one request. Android and iOS blocking surfaces have different technical ceilings and must be designed separately.

---

## 2. Product Context

BlockSocial is a native Android and iOS attention-management application. It interrupts the automatic launch of a social-media application, gives the user a deliberate choice, and records that choice locally.

The user selects supported applications, creates a rule such as a weekday schedule, and receives a soft block when the restricted application is opened while the rule is active. The soft block offers two paths: return to the previous task, or take limited temporary access. Every decision is stored locally and shown as statistics.

Core positioning:

> System timers measure time. BlockSocial measures decisions.

The product is not parental control, not surveillance, not a VPN, and not a way to make an application unreachable. The user always keeps control.

---

## 3. Design Principles

### 3.1 Awareness, Not Punishment

The interface never shames, accuses, or scores the user as a failure. A bypass is a legitimate outcome, not a defeat.

### 3.2 The Interruption Must Be Calm

The block screen is a pause, not an alarm. It should feel like a held door, not a siren. Avoid alarm-red as the dominant color, avoid aggressive iconography, avoid urgency patterns.

### 3.3 Decide in Under Three Seconds

The block screen is read in a moment of low attention. One primary decision, one secondary decision, no more. No scrolling, no nested navigation, no forms.

### 3.4 Minimal Setup Friction

The first useful rule must be reachable within a few minutes of install, including permission granting.

### 3.5 Honest Permission Requests

Every system permission is explained in the product's own words, with a concrete reason, before the system dialog appears. Never imitate a system dialog.

### 3.6 Platform Honesty

Android and iOS keep the same concept, terminology, and metrics, but not identical screens. The design must never promise behavior a platform cannot deliver.

### 3.7 Honest Numbers

Statistics never display false precision and never show an unavailable metric as a zero.

---

## 4. Design Non-Goals

Do not design any of the following. They are outside the MVP or outside the product entirely.

- account creation, login, profile, or social features
- cloud sync indicators, backup screens, or device lists
- paywalls, subscription screens, price tables, or upsell banners
- advertising slots
- selective blocking of Reels, Shorts, feeds, or in-app tabs
- website or DNS blocking UI
- parental control or another person's device
- an AI coach, chat, or conversational surface
- leaderboards, competitive streaks, or public sharing
- any screen that claims BlockSocial cannot be disabled or uninstalled
- any screen resembling a system permission dialog, system error, or OS settings page
- a full web dashboard

Freemium is a business hypothesis, not an MVP surface. Do not design monetization.

---

## 5. Platform Design Constraints

This section is the most important constraint set in the brief.

| Area | Android | iOS |
|---|---|---|
| UI framework | Jetpack Compose, Material 3 | SwiftUI, Apple HIG |
| Minimum OS | API 26 | iOS 17 |
| Block surface | custom full-screen overlay rendered by the app | system Screen Time shield |
| Block surface freedom | broad, but must not imitate system UI | severely limited, see section 8 |
| App selection | curated catalog of supported apps with names and icons | opaque system picker, no names or icons available to the app |
| Schedule capacity | effectively unconstrained | hard system limits, see section 8 |
| Open the main app from the block | possible | not officially supported, must not be required |
| Usage-time accuracy | approximate | limited and event-based |

Consequences the designer must accept:

- The Android block screen and the iOS block screen are two different designs. Do not produce one shared mockup.
- On iOS, the app cannot render the selected applications' names or icons in its own lists. Selection is displayed as a count and as system-provided views only.
- On iOS, the shield cannot host arbitrary layout, images, navigation, or input.
- Neither platform may design a flow that depends on the user opening BlockSocial from the block screen.

---

## 6. Screen Inventory

Each screen below is required for the MVP. States listed for each screen are mandatory, not optional variants.

### 6.1 Onboarding

Purpose: explain the product in a few seconds and reach the first rule quickly.

Content: what a soft block is, that the user stays in control, that data stays on the device, that no account is needed.

States: first launch, returning user who has not finished setup.

Constraints: no more than a few steps, skippable where legally possible, no data collection.

### 6.2 Permission Explanation

Purpose: earn the permission before requesting it.

Android surfaces: Accessibility access, Usage Access, notifications when needed.
iOS surfaces: Screen Time authorization, notifications when needed.

Content per permission: four fixed slots in a fixed order — what it enables, what is read, what is never read, how to revoke it.

This once listed a fifth item, what stops working without the permission. Phase 19 dropped it as a slot and kept it as information. Five slots across three cards is fifteen paragraphs on one screen, and the fifth repeated the first from the other side. The consequence of refusing is now stated once by the setup screen, which says outright that either permission can be skipped and that rules are saved anyway, and again by protection health, which states for every degraded state what stops working and what keeps working. The design record in `DESIGN_EXPORT_ANALYSIS.md` section 5 already specified four slots; this section was the outlier.

States: not requested, system sheet open, granted, denied, revoked later.

Constraints: prominent disclosure and affirmative consent are required on Android before the system flow. The screen must never look like the system dialog it precedes. Denial must leave a usable, non-punishing path.

### 6.3 Application Selection

Purpose: choose which applications are restricted.

Android: a curated list of supported applications, showing only those installed. The wording is "supported applications", never "all your apps". A missing application is a normal, explained state.

iOS: the system picker is presented; the result is opaque. The app displays a selection summary such as a count, plus a re-open-picker action.

States: nothing selected, some selected, catalog entry not installed, iOS selection lost after reinstall and needing reselection.

### 6.4 Rule Editor

Purpose: create and edit one restriction rule.

Content: mode (`SCHEDULE`, `ALWAYS`, or `DAILY_LIMIT`), weekdays, start time, end time, daily limit in minutes, bypass policy.

The form changes shape with the mode: weekdays and times belong to `SCHEDULE`, a minutes value belongs to `DAILY_LIMIT`, and `ALWAYS` needs neither. Do not show inert fields for the mode that does not use them.

States: create, edit, overnight interval such as 22:00 to 07:00, daily limit set, daily limit already reached today, disabled rule, validation error, iOS capacity error.

Constraints: the overnight interval must be visually unambiguous. On iOS, a capacity error must be actionable and must say what to remove, never a silent truncation. A daily limit is measured approximately on both platforms, so its remaining time is presented as an estimate, never as an exact countdown.

### 6.5 Dashboard

Purpose: the home surface. Show protection state and today's decisions.

Content: protection health summary, today's interventions, stayed-focused count, bypass count, refusal rate, active rules, current streak.

States: healthy, degraded because a permission is missing or revoked, no rules yet, no events today.

Constraints: protection state is the most prominent element. A degraded state must offer a one-tap path to fix it.

### 6.6 History

Purpose: an honest log of interventions.

Content: per event, the application, time, why the rule was active, and the decision taken.

States: empty, single day, long list, filtered by application.

Constraints: neutral presentation. A bypass entry is recorded with the same visual weight as a stayed-focused entry, differentiated by more than color alone.

### 6.7 Protection Health

Purpose: diagnose why blocking may not be working.

Content: per-requirement status, plain-language explanation, and a repair action. Includes OEM background-restriction guidance on Android.

States: all healthy, one or more failures, platform-specific failure list.

### 6.8 Settings

Purpose: theme, language, data deletion, about, privacy policy.

Constraints: deleting all local data is available and clearly confirmed. No hidden retention.

### 6.9 Block Screen

Android and iOS versions are specified separately in sections 7 and 8.

---

## 7. Android Block Screen

This is the product's signature surface.

Required elements:

- which application was opened
- why the restriction is active right now
- how long the restriction remains active
- primary action: `Stay Focused`
- secondary action: `Open Temporarily`
- optional, low-emphasis: current streak or today's refusal count

Behavior requirements for the designer:

- The screen must be readable and decidable without scrolling on the smallest supported device.
- The primary action must be the easier target. The bypass must be available without hunting, but must not be the default.
- There is always a visible way out. No dead end, no trap, no forced wait before the primary action is usable.
- Repeat display is suppressed for the duration of a granted bypass, so the design must not depend on a nagging pattern.
- The screen must work in dark mode, in landscape, and with a screen reader.

Prohibited:

- imitation of system UI, system errors, or OS branding
- a design that hides, delays, or disguises the exit
- guilt-based copy, countdown pressure, or animated distress
- anything implying the user cannot leave

Bypass duration options are presented per the rule's policy. On Android, a five-minute wall-clock grant is the baseline.

---

## 8. iOS Shield

The iOS block surface is a system Screen Time shield, not an app screen. It is configured, not composed.

The designer works within these limits:

- a system-defined layout with an icon, a title, a subtitle, and a limited number of buttons
- no arbitrary layout, no custom navigation, no scrolling content, no input fields, no images beyond the permitted icon
- no reliable way to open BlockSocial from the shield, so all needed instruction must fit inside the shield text itself

Design deliverable for iOS is therefore copy and configuration, not a screen mockup: shield title, subtitle, icon direction, button labels, and the wording for each state.

System scheduling limits that shape the rule UX:

- a monitored interval cannot be shorter than fifteen minutes
- at most twenty concurrently monitored activities
- at most fifty application tokens in a shield

The iOS bypass duration is unresolved. A five-minute usage-based grant is the preferred candidate and a fifteen-minute wall-clock grant is the fallback. Copy must therefore be written so that the duration is a variable, never a hardcoded promise of five minutes.

---

## 9. Cross-Cutting States

Every screen design must include these states where applicable.

- empty, before any data exists
- loading, where a real delay is possible
- error, with a cause in plain language and a recovery action
- permission missing or revoked, without a crash-like or accusatory presentation
- offline, which is the normal condition and must never be shown as an error

The application is fully offline-capable. No screen may show a connectivity warning.

---

## 10. Tone of Voice

Neutral, plain, supportive, short. The product speaks like a calm assistant, not a coach, a parent, or a drill sergeant.

Rules:

- address the user directly and simply
- state facts, not judgments
- never use guilt, shame, streak-loss threats, or exclamation-driven urgency
- no moralizing about social media
- no fake enthusiasm for a refusal, no disappointment at a bypass

Copy direction, to be finalized during design:

| Situation | Acceptable direction | Prohibited |
|---|---|---|
| Block screen title | name the app and the active rule | "Stop!", "Again?!" |
| Primary action | a calm return, phrased as a choice | "Be strong", "Resist" |
| Secondary action | limited access, with the duration stated | "Give up", "I'm weak" |
| After a bypass | neutral acknowledgement | "You failed again" |
| Streak broken | factual, recoverable | loss-aversion pressure |
| Permission denied | what stops working, how to enable later | warnings implying the app is broken |

Copy is written in English in the repository. Product localization covers Russian and English, so no string is hardcoded in a mockup as final.

---

## 11. Statistics Presentation

Metrics available in the MVP:

- interventions today
- stayed-focused count
- bypass count
- refusal rate
- active rules
- current streak
- per-application breakdown

Presentation rules:

- Never display false precision. Prefer "you declined 12 potential sessions" over "you saved exactly 2 hours 17 minutes".
- Separate measured values from estimated values visually and in wording.
- Hide a metric that a platform cannot measure reliably. Never render it as a zero.
- A streak must never be presented as something the user can lose by accident without the rule having been explained beforehand.
- Charts, if used, must be readable without color as the only channel.

---

## 12. Design System Requirements

The design phase must deliver a token-based system, not one-off screens.

Tokens required:

- color roles for surface, on-surface, primary, on-primary, secondary, outline, success, warning, and a restrained danger role
- spacing scale
- corner radius scale
- elevation or equivalent depth roles
- typography scale mapped to platform text styles
- motion durations and easing

Color direction:

- calm, low-saturation, comfortable at night, since night scrolling is a core scenario
- the danger role is reserved for genuinely destructive actions such as deleting all data, and is never the block screen's dominant color
- semantic state must never be carried by hue alone

Components required:

- rule card
- application row with an installed and a not-installed variant
- protection health item with status and repair action
- statistic tile
- history row
- permission explanation card
- primary, secondary, and destructive buttons
- empty-state block

Platform mapping: Material 3 components on Android, native SwiftUI components on iOS. Do not force one platform's visual language onto the other. Shared tokens, platform-native controls.

---

## 13. Themes

Light and dark themes are both first-class and must be designed together, not derived automatically.

Dark theme is the higher-priority case for the block screen, because interventions frequently occur at night. Contrast must hold in both themes.

---

## 14. Motion

Motion is functional only: state changes, transitions, and confirmation.

- the block screen appears quickly and without theatrics
- no attention-grabbing loops, pulsing, or shaking
- all motion respects Reduce Motion and its Android equivalent
- no motion is required to understand a state

---

## 15. Accessibility

Mandatory, not a later pass.

- TalkBack and VoiceOver support with meaningful labels for every interactive element
- Dynamic Type and large font scales without truncation or overlap, verified on the block screen specifically
- sufficient contrast in both themes
- large touch targets, especially on the block screen
- Reduce Motion respected
- no meaning conveyed by color alone
- a logical focus order, with the primary action reachable first on the block screen

The block screen must remain fully usable with a screen reader active, since Android renders it through an accessibility overlay.

---

## 16. Localization

- product languages are Russian and English
- no user-facing string is hardcoded in the design as final
- layouts must tolerate roughly thirty percent text expansion, which Russian frequently requires
- time formats, weekday names, and first day of week are locale-dependent
- the iOS shield has very little room for text, so its copy must be checked in both languages at the shortest possible length

---

## 17. Prompt Pack for Claude Design

Use one prompt per screen group. Always prepend sections 2 to 5 as context. Always state the platform.

### 17.1 Android Block Screen

```
Design the Android block screen for BlockSocial, a soft-blocking attention app.
Platform: Android, Jetpack Compose, Material 3, dark and light themes.
Context: the screen appears full-screen the moment the user opens a restricted
social app while a rule is active. The user is distracted and decides in seconds.
Required: the app that was opened, why the restriction is active now, how long it
lasts, a primary "stay focused" action, and a secondary "open temporarily" action
with the duration stated.
Constraints: no scrolling, one screen, calm and non-judgmental, never alarm-red as
the dominant color, always a visible way out, must not imitate system UI, must work
with a screen reader and at large font scales.
Deliver: light and dark variants, and the largest-font-scale variant.
```

### 17.2 Onboarding and Permissions

```
Design the onboarding and permission-explanation flow for BlockSocial on Android.
Goal: reach the first working rule in a few minutes.
Required: a short product explanation, then one card per permission that states what
it enables, exactly what is and is not read, and how to revoke it.
Constraints: local-first, no account, no data collection, must not resemble a system
dialog, denial must leave a usable non-punishing path.
Deliver: the happy path, the denied state, and the revoked-later state.
```

### 17.3 Dashboard and Statistics

```
Design the BlockSocial dashboard.
Required: protection health as the most prominent element, then today's interventions,
stayed-focused count, bypass count, refusal rate, active rules, and current streak.
Constraints: no false precision, hide unmeasurable metrics rather than showing zeros,
neutral tone, a bypass is not a failure, one-tap repair when protection is degraded.
Deliver: healthy, degraded, and first-run empty states.
```

### 17.4 Rule Editor

```
Design the BlockSocial rule editor.
Required: mode selection between schedule, always, and daily limit; weekday selection;
start and end time; a daily limit in minutes; and bypass policy.
Constraints: the form shows only the fields the selected mode uses; an overnight interval
such as 22:00 to 07:00 must be visually unambiguous; a daily limit's remaining time is an
estimate, never an exact countdown; validation errors must state how to fix them.
Deliver: create, edit, overnight, daily-limit, limit-reached, and error states.
```

### 17.5 iOS Surfaces

```
Design the iOS surfaces for BlockSocial in SwiftUI following Apple HIG.
Important: the block itself is a system Screen Time shield, not an app screen. Do not
design a custom blocking screen. For the shield, deliver only the title, subtitle, icon
direction, and button labels, within a fixed system layout with no custom layout,
images, scrolling, or input.
App-side screens to design: onboarding, Screen Time authorization explanation, selection
summary presented as a count because app names and icons are unavailable, rule editor,
dashboard, history, settings.
Constraints: never require opening the app from the shield; bypass duration is a variable,
not a fixed promise of five minutes.
```

Reject any output that adds accounts, sync, paywalls, ads, per-feed blocking, or copy that shames the user.

---

## 18. Open Questions Blocked by Spikes

These are unverified. Design may proceed, but no screen may promise them.

| Question | Blocking spike | Design impact |
|---|---|---|
| Is a full-screen accessibility overlay stable enough to host rich UI? | `A-02` | If not, the Android block screen must fall back to a simpler layout |
| Does the curated catalog cover enough applications? | `A-04` | Affects selection wording and the not-installed state |
| Is a five-minute iOS bypass achievable? | `I-05` | Bypass copy and duration options on iOS |
| How do capacity limits surface to the user? | `I-06` | Rule editor error design on iOS |
| Which shield elements are actually configurable in practice? | `I-03`, `I-04` | The entire iOS block copy |

Any design decision that depends on a row above must be marked provisional in the screen catalog.

---

## 19. Deliverables of the Design Phase

1. design tokens for both platforms
2. component library
3. Android screen set including the block screen, in light and dark themes
4. iOS screen set plus shield copy and configuration
5. all mandatory states from section 9
6. accessibility annotations
7. English copy deck with placeholders for localization
8. a screen catalog document referencing this brief

---

## 20. Acceptance Checklist

A design is accepted when:

- it contains nothing from section 4
- Android and iOS block surfaces are designed separately and respect section 5
- the block screen is decidable in seconds, without scrolling, with a visible exit
- tone follows section 10 with no shaming copy anywhere
- statistics follow section 11 with no false precision
- light and dark themes are both complete
- accessibility requirements in section 15 are met, including on the block screen
- Russian text expansion does not break any layout
- every provisional element is marked and traced to a spike in section 18
