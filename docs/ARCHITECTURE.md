# Architecture

How BlockSocial is built. `TECHNICAL_SPECIFICATION.md` defines what must be true; this document defines the structure that makes it true.

---

## System Overview

Two native applications with no shared executable code.

```text
Shared product contract
├── Android implementation
│   ├── Kotlin domain
│   ├── Android platform adapters
│   └── Room
└── iOS implementation
    ├── Swift domain
    ├── Screen Time adapters
    └── SwiftData
```

What is shared is a contract, not a runtime: terminology, domain models, business rules, event semantics, UX flows, localization, test fixtures, and design tokens. Each platform implements that contract with its own mechanism.

There is no backend, no account system, and no synchronization. Every device is independent and every feature works offline.

The rule evaluator is the one piece of logic that must behave identically on both platforms. It is written twice, once per language, and both implementations are tested against the same shared fixtures.

---

## Android

### Blocking mechanism

```text
Accessibility event (TYPE_WINDOW_STATE_CHANGED)
   → packageName
   → exclude BlockSocial and system packages
   → check the supported catalog
   → evaluate the rule
   → evaluate active bypass grants
   → show the overlay
```

The service consumes only the package name and the transition timestamp. It never inspects the accessibility tree beyond that, never reads text, and never performs actions inside other applications.

Required safeguards against duplicate and runaway events: `lastHandledPackage`, `lastHandledAt`, a debounce window, an `overlayVisible` guard, self-package exclusion, and revalidation of the foreground target before showing the overlay.

### Block surface

A full-screen `TYPE_ACCESSIBILITY_OVERLAY` hosted by the accessibility service. No separate overlay permission is requested. The main application UI is Compose; the overlay uses `ComposeView` if it proves stable, otherwise a small View-based implementation.

Actions:

- `Stay Focused` — perform a global home action or dismiss safely, record `STAYED_FOCUSED`.
- `Open Temporarily` — create a `TemporaryAccessGrant`, remove the overlay, record `BYPASSED`, and suppress further overlays for that application until the grant expires.

Recovery guarantees: the overlay can always be removed, a watchdog timeout prevents a stuck overlay, and the overlay is removed on a package change. Settings, the launcher, the phone app, and system UI are on a permanent allowlist.

### Application selection

A curated JSON catalog of supported package names, with those packages declared through targeted `<queries>`. `PackageManager` decides which catalog entries are installed. Missing entries render as a normal, explained state rather than an error.

### Modules

```text
android/
├── app
├── core-model
├── core-domain
├── core-data
├── core-ui
├── feature-onboarding
├── feature-dashboard
├── feature-app-selection
├── feature-rules
├── feature-history
└── feature-settings
```

System services stay in `app` until a separate platform module is justified. The module split is filled in after one vertical slice works end to end: detect a launch, show the block, record the decision, persist it, display it in history.

### Storage

Room holds restricted apps, rules, temporary grants, block events, usage sessions, and statistics aggregates. DataStore holds onboarding state, language, theme, consent versions, debug flags, and the last permission snapshot. Destructive production migrations are prohibited.

### Background work

The accessibility service is the runtime mechanism. WorkManager handles only deferrable work: daily aggregation, old-event cleanup, consistency checks, and statistics preparation. There is no permanent foreground service and no polling.

Daily limits read accumulated usage from `UsageStatsManager` and mark the application as restricted once the limit is reached; the block itself still comes from the accessibility service on the next launch. The limit is never enforced by polling.

### Recovery

After reboot, rules remain in Room, grants are recomputed, expired grants are removed, and the dashboard reports the resulting health state. After process death, service state is restored from persistence; overlay logic never depends on an Activity being alive.

### Stack

Coroutines and Flow, Hilt, Room, DataStore, WorkManager, `java.time`. Tests: JUnit, kotlinx-coroutines-test, Turbine, AndroidX Test, Compose UI Test. Versions pinned through a Version Catalog.

---

## iOS

### Blocking mechanism

There is no launch interception. Restrictions are registered in advance and the system applies a shield.

```text
Rules → schedule compiler → DeviceActivity schedules
                          → ManagedSettingsStore
                          → system shield on the target apps
```

`DeviceActivityMonitor` receives interval start and end callbacks and threshold events, and applies or removes shields. The main application does not need to be running.

Daily limits use the same usage-threshold callback as the bypass: a threshold is registered for the limit, and the shield is applied when it fires. Both features therefore stand or fall on the same mechanism, which is why it is checked early.

### Targets

```text
ios/
├── BlockSocialApp
├── BlockSocialDeviceActivityMonitor
├── BlockSocialShieldConfiguration
├── BlockSocialShieldAction
└── BlockSocialTests
```

| Target | Responsibility |
|---|---|
| Main app | onboarding, authorization, picker, rules, dashboard, history, settings |
| DeviceActivityMonitor | interval start and end, usage thresholds, applying and removing shields |
| ShieldConfiguration | shield appearance and localized text |
| ShieldAction | button handling, store changes, minimal event recording |

Extensions stay thin: no networking, no heavy dependency injection, no analytics, no large assets.

### Schedule compiler

Apple's limits force compilation rather than a direct mapping from rules to activities. The compiler groups identical intervals, groups applications, validates capacity before saving, replaces existing activities safely, and rejects excess rules with an actionable error naming what to remove.

Daily limits draw on the same twenty-activity budget as schedules, so capacity validation counts both. A user who fills the budget with schedules cannot then add a limit, and the error must say which of the two to give up.

### Bypass

The preferred design: the shield action removes the shield, registers a five-minute usage threshold, and the monitor reapplies the shield when the threshold fires. The fallback, if that proves unreliable, is a fifteen-minute wall-clock grant, which is the shortest valid interval Apple allows.

No timer, sleep, or long-lived extension process is used. Until this is proven on a real device, the duration stays a variable in code and in copy.

### Storage

SwiftData holds rule metadata, history, statistics, and permission snapshots. An App Group holds what the extensions need: the encoded selection, compact rules, a rule-to-activity mapping, bypass state, and an event queue the main app drains. Every payload carries a schema version.

---

## Where the Platforms Diverge

| Area | Android | iOS |
|---|---|---|
| Launch detection | accessibility events | none; shields are pre-applied |
| Block surface | custom full-screen overlay | system Screen Time shield |
| Block surface freedom | broad, must not imitate system UI | fixed layout, copy and configuration only |
| App selection | package names from a curated catalog | opaque tokens from the system picker |
| App names and icons | available | unavailable; selection shown as a count |
| Scheduling | custom evaluator | DeviceActivity, hard capacity limits |
| Bypass | app-controlled wall-clock grant | requires proof; fallback is longer |
| Usage statistics | `UsageStatsManager`, approximate | DeviceActivity events, limited |
| Daily limits | limit evaluated from usage data, blocked on the next launch | usage threshold fires, shield applied by the system |
| Open the app from the block | possible | not supported, never required |
| Background stability | OEM-dependent | system-managed, extension-limited |

Must match across platforms: terminology, schedule semantics, the meaning of `Stay Focused`, event history, core metrics, privacy rules, local-first behavior, and mode names.

May differ: block surface appearance, bypass options, usage accuracy, selection mechanism, permission flow, recovery UX, rule capacity, and technical logs.

Metrics a platform cannot measure reliably are hidden on that platform rather than shown as zero.

---

## Logging

Android uses categorized Logcat; iOS uses OSLog. Debug builds include the commit hash and build number. Release logs exclude application names, tokens, and user content. On failure, the teammate exports the Xcode console log.

---

## Build and Delivery

Android is built on Windows by the project owner and tested on real devices from several manufacturers.

iOS is written without local Xcode access and built by the teammate on macOS. Each handoff carries a commit hash, Xcode and iOS versions, affected targets, exact build steps, expected behavior, numbered test cases, required screenshots or recordings, log-export instructions, and known limitations. One system mechanism per handoff; unverified Swift never accumulates.

The teammate records the exact toolchain in `ios/BUILD_ENVIRONMENT.md` once that directory exists.
