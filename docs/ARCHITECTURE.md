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

### The shared contract

Frozen in phase 07. It lives in `shared/fixtures/` and is data, not code, so neither platform owns it.

| File | What it pins down |
|---|---|
| `schedule-cases.json` | when a schedule rule is active |
| `bypass-cases.json` | when a temporary grant suppresses a block |
| `daily-limit-cases.json` | when a daily limit has been reached |
| `rule-priority-cases.json` | which rule wins when several apply, and the reserved focus-session slot |
| `block-event-contract.json` | the frozen `BlockEvent` field list |
| `business-rules.json` | the rules from `docs/PRODUCT.md` the corpus must cover |
| `SCHEMA.md` | what a case means, and the interval decisions the contract makes |

`shared/fixtures-validator` is a small Kotlin build whose tests are the schema: it fails on a malformed case, a duplicate identifier, a business rule with no case, a local-time note that disagrees with its own instant, and any platform or mechanism name inside a case. It is wired into the Android build as `:shared-fixtures`, so `gradlew test` from `android/` runs it with everything else and the contract cannot rot unnoticed. It keeps its own settings file and still builds standalone, which is what the iOS track will use.

Three properties matter more than the case count:

- **A case states inputs and one expected result, never a mechanism.** A platform that reaches the right answer by a different internal route conforms.
- **Every instant carries an explicit offset**, and a human-readable local-time note that the validator recomputes and checks. Ambiguous time is how a cross-platform contract rots.
- **Cases a known Apple limit could make unsatisfiable are marked** with the limit that threatens them. That marking is the shortlist phase 34 works through; a failure there is a contract error and changes both platforms, not an iOS workaround.

The contract was written from Android evidence alone, because iOS validation now runs after Android ships. That is recorded here so the marking is read as a debt, not as decoration.

---

## Android

### Blocking mechanism

```text
Accessibility event (TYPE_WINDOW_STATE_CHANGED)
   → packageName, className, eventTime
   → exclude BlockSocial and system packages
   → require className to resolve to a real activity
   → require the foreground package to have changed
   → check the supported catalog
   → evaluate the rule
   → evaluate active bypass grants
   → show the overlay
```

The service consumes the package name, the window class name and the transition timestamp. The class name is passed to `PackageManager` and reduced to a boolean; it is never stored or displayed. The service never inspects the accessibility tree, never reads text, and never performs actions inside other applications.

**Two rules are mandatory, and both were established by spike `A-01` after the obvious implementation failed on a device.**

*An event counts only when `packageName` plus `className` resolve to a real activity.* `TYPE_WINDOW_STATE_CHANGED` with `packageName` alone is not a foreground signal: an application that is going into the background keeps emitting window events, and those events carry a plain view class rather than an activity class. Without this check, pressing home and returning to a restricted application produces no block at all, because the state machine still believes the application is in front. The check needs only the targeted package visibility the catalog already declares.

*An entry is a change of the foreground package, not a time-based debounce.* An application emits several window events while starting, spread wider than any workable debounce window: one cold launch produced two blocks, a deep link produced three. Any number of further activity windows inside the same application is one visit. Leaving to the launcher or to another application ends the visit, so a genuine return is detected again. BlockSocial's own package does not end a visit, because the block screen belongs to us.

*A window that only covers the current application does not end the visit; the screen going off does.* System UI is on the allowlist, so the first version treated the notification shade as leaving the application — and phase 12 measured the result on a device: glancing at a notification and dismissing it produced a second block from an application the user had never left. Package identity cannot separate that from the case that must still block, because the lock screen is the same package. The signal that separates them is the screen: `ACTION_SCREEN_OFF` ends the visit, so unlocking into an application that was already open is a fresh entry and is blocked, while an overlay drawn over it is not. This is the decision the Android gate left open for this phase.

The service logs every decision, not only the ones that block. Spike `A-01` lost several inconclusive device runs to logging only the blocking branch: a decision that is not logged makes a wrong decision invisible. Logging is confined to debug builds and carries no window class and no screen content.

**Nothing on the callback path touches storage.** The service keeps an immutable snapshot of what the decision needs — which packages map to which selected application, their rules, and their grants — and replaces it from a coroutine whenever the database changes. The callback reads that snapshot and calls the pure evaluator, so the only cross-process work it does is the activity check that constraint one requires. This is structural rather than careful: the class that makes the decision has no repository to call. An instrumented test runs the path fifty times on the main thread under a strict-mode policy that kills the process on a disk read.

The snapshot is rebuilt from persistence when the service connects, so protection resumes after a reboot without the user opening BlockSocial. Verified on an Android 16 emulator.

### Block surface

A full-screen `TYPE_ACCESSIBILITY_OVERLAY` hosted by the accessibility service. No separate overlay permission is requested. The main application UI is Compose, and the overlay uses `ComposeView`: spike `A-02` proved it stable on an emulator, so the View-based fallback is not needed.

Compose in a window that is not an activity needs its own `LifecycleOwner`, `ViewModelStoreOwner` and `SavedStateRegistryOwner`, supplied by the overlay host. Without those three the content does not compose at all. The window also carries an accessibility title, set through `setTitle`, because `WindowManager.LayoutParams.accessibilityTitle` is not in the public SDK and an untitled window is announced as untitled.

The design baseline is direction **1b, "Quiet"** from `design/DESIGN_EXPORT_ANALYSIS.md`, confirmed in the Android gate.

Actions:

- `Stay Focused` — perform a global home action or dismiss safely, record `STAYED_FOCUSED`.
- `Open Temporarily` — create a `TemporaryAccessGrant`, remove the overlay, record `BYPASSED`, and suppress further overlays for that application until the grant expires.

Recovery guarantees: the overlay can always be removed, a watchdog timeout prevents a stuck overlay, and the overlay is removed on a package change. Settings, the launcher, the phone app, and system UI are on a permanent allowlist.

**Every way out is a recorded outcome.** Spike `A-02` found that the system back key removed the overlay silently and recorded nothing, which is a bypass the history would never show. Back is now treated as `Stay Focused`: the user chose to leave, so the event says so. The watchdog and the foreground moving elsewhere record `DISMISSED_BY_SYSTEM`, which the statistics never count as either a refusal or a bypass. A test walks every dismissal path and asserts the window count returns to zero and the outcome is reported.

The status bar and the notification shade stay reachable above the block screen. This is deliberate: the product is a pause, not a cage, and trapping the user under a full-screen window would be both hostile and hard to defend under Play policy. Because system UI does not end a visit, reaching the shade does not produce a second block.

The screen carries the whole decision without scrolling at the largest font scale and in a landscape-shaped viewport, both verified on an Android 16 emulator. Rotation itself is still unverified: neither the `A-02` AVD nor this one rotates, so the display never leaves `rotation 0`. It stays open and moves to phase 22, where the API-level matrix needs an AVD that rotates anyway.

### Application selection

A curated JSON catalog at `shared/supported-app-catalog/catalog.json`, with every package declared through targeted `<queries>`. `PackageManager` decides which catalog entries are installed. Missing entries render as a normal, explained state rather than an error.

An entry carries a stable `id` and a **list** of package names, not one. Rules and stored selections reference the id, so a package rename does not orphan user data, and the list covers applications that ship under more than one package: TikTok and Telegram already do. The displayed name for an installed application is the label the device reports; the catalog's own name is a fallback for entries that are not installed and therefore have no device label.

The manifest `<queries>` and the catalog must not drift apart. A catalog entry with no matching manifest declaration resolves to not-installed on every device, silently and with no error, so the two are kept in step by generation or by a failing test rather than by care.

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

Dependencies point down five layers and never sideways: `core-model` (0), `core-domain` (1), `core-data` and `core-ui` (2), the feature modules (3), `app` (4). A Gradle task, `checkModuleGraph`, fails the build on any project dependency that points at its own layer or above, and it runs as part of `check`. Gradle already rejects a cycle; this catches the flatter mistake of a core module reaching into a feature.

`core-model` and `core-domain` are plain Kotlin/JVM modules with no Android dependency. That is not tidiness: it makes the rule evaluator testable against the shared fixtures without an emulator, and it makes an accidental platform call a compile error rather than a review comment.

System services stay in `app` until a separate platform module is justified. The module split is filled in after one vertical slice works end to end: detect a launch, show the block, record the decision, persist it, display it in history.

### Design system

`core-ui` holds the tokens and nothing else so far: color for both themes, the type scale, spacing, radius, and motion. Values come from `design/DESIGN_EXPORT_ANALYSIS.md`, converted from the documented `oklch()` sources and re-derived by a test on every build, so a hex cannot drift from the design without failing. Section 12 of that document records the conversion; section 9.7 records which roles the measured contrast does **not** allow as text.

There is no dynamic color. The palette is fixed, because a block screen that recolors itself to the user's wallpaper is not the product's design.

Elevation is a step in the surface ramp, never a shadow.

`targetSdk 36` means edge-to-edge is enforced, so every activity handles window insets explicitly. Verified on an Android 16 emulator, where the unhandled case drew the title under the status bar and the last row under the gesture pill.

### Domain

`core-model` holds the state, `core-domain` holds the decisions, and neither touches the framework. One entry point answers the only question the rest of the application asks:

```text
RuleEvaluator.evaluate(rules, grant, usageSessions, forApp, at) → RestrictionDecision
```

It returns `restrictionActive`, the `primaryReason` shown to the user, and `allReasons` in priority order for the event record. Three evaluators sit beneath it — schedule, bypass, daily limit — each usable alone, each a pure function.

**The clock is a parameter, never a reading.** `DeviceTime` carries the wall clock, the monotonic counter and the zone together, so no evaluator can consult the system on its own and no test has to manipulate one. That is what makes the daylight-saving and clock-tampering cases expressible at all.

An active grant is checked before any rule, and it short-circuits: a suppressed block reports no reasons rather than reasons that were overridden.

**A rule is a sealed type, not a mode flag.** A schedule rule cannot exist without its days and times, and a daily-limit rule cannot exist without its limit, because the compiler will not build one.

Two interval decisions are not what a naive implementation produces, and both come from `shared/fixtures/SCHEMA.md`:

- A start time that falls in a spring-forward gap begins at **the first instant that exists**. `java.time` would instead shift the local time forward by the length of the gap, which starts the interval an hour late. The evaluator asks the zone rules for the transition and uses its instant.
- An overnight interval belongs to the day it **starts**, so the evaluator tests today and yesterday as candidate start days rather than only the current date.

Reaching a daily limit does not present a block. The restriction turns active the moment measurement crosses the limit, and the user meets it the next time they open the application — the corpus states this and the detection layer in phase 12 depends on it.

`BlockEvent` enforces its own contract at construction: `primaryReason` must appear in `allReasons`, `allReasons` must be in priority order, and a bypass duration exists exactly when the action was a bypass. A malformed event cannot be built, so it cannot be stored.

Two Gradle checks keep this honest, both wired into `check`: `checkDomainIsFrameworkFree` fails if either domain module gains an Android dependency or plugin, and `checkModuleGraph` fails on a sideways or upward module dependency. The whole test-runtime classpath of `core-domain` is `core-model`, the Kotlin standard library, JUnit and a JSON parser.

Every case in `shared/fixtures/` runs as its own test, so the report names the cases rather than counting them. The iOS evaluator in phase 36 implements the same contract against the same files.

### Storage

Room holds restricted apps, rules, temporary grants, block events, usage sessions, and statistics aggregates. DataStore holds onboarding state, language, theme, consent versions, debug flags, and the last permission snapshot. Destructive production migrations are prohibited.

Six tables, schema version 1, exported to `android/core-data/schemas/` and committed. The export is not decoration: a migration test opens the database at the exported schema and validates it against the entities, so an entity that drifts from the committed schema fails the build rather than a user's install.

**Destructive migration is forbidden by a check, not by discipline.** `checkNoDestructiveMigration` fails the build if `fallbackToDestructiveMigration` appears anywhere in shipped source. Without a migration, Room refuses to open a changed schema and says so; it never silently drops what the user recorded.

Everything is stored as primitives — strings, integers, epoch milliseconds — so the schema is readable without a converter registry, and a test asserts that no column outside an approved list exists. That list is the enforcement point for the privacy rule: nothing is stored beyond catalog identifiers, enumerated decisions, and timestamps.

Keys and indices follow the questions the application actually asks:

| Table | Key | Index | Asked by |
|---|---|---|---|
| `restriction_rule` | rule id | `appCatalogId` + `enabled` | every detection, for one application |
| `temporary_access_grant` | **`appCatalogId`** | — | every detection, for one application |
| `usage_session` | generated | `appCatalogId` + `fromEpochMillis` | the daily-limit day window |
| `block_event` | event id | `occurredAtEpochMillis` | history, newest first |

A grant is keyed by its application rather than by a grant id, so an application cannot hold two grants at once and re-granting is idempotent by construction. That is the storage half of the rule that a block must not reappear until the grant expires.

Rules reference the catalog identifier, never a package name, and deleting an application cascades to its rules. Repositories return domain types; entities do not leave `core-data`.

Both survival guarantees were verified on an Android 16 emulator by seeding the database and preferences, then reading them back after a force-stop and again after a reboot, without reseeding.

### Writes that must outlive the service

A write the user's record depends on — a block event, a taken grant, a cleared grant — runs on an application-scoped writer, `DurableWrites`, not on the accessibility service's own scope. The service scope is cancelled in `onUnbind`, and Android unbinds the service at times of its own choosing. Phase 19 caught a decision being lost exactly that way: the block screen was dismissed because the service was stopping, and the record of that dismissal was cancelled a moment later. Reads into memory, such as refreshing usage, stay on the service scope, because they are worthless once the service is gone.

### The application shell

One launcher entry, `MainActivity`. It decides between setup and the shell from `onboardingCompleted`, and hosts every screen.

Navigation is a hand-written state machine: a sealed `Destination`, a `ShellNavigator` holding a back stack, and a saver that encodes it so rotation and process death do not lose the user's place. Switching a tab clears the stack; opening a screen pushes onto it; `Protection` and a rule list belong to the tab they were opened from. No navigation library is used, and none is needed for five destinations without deep links.

Screens are content, not frames. A screen never applies `safeDrawingPadding`, a background, or its own outer scroll — the shell owns those. Two frames exist because two kinds of screen exist: `ScrollingScreen` for content that must be scrolled by its host, and `ScrollsItselfScreen` for content that already contains a `LazyColumn`. Nesting one inside the other crashes Compose with an infinity-height error, so a test renders every screen inside the real shell.

### Background work

The accessibility service is the runtime mechanism. WorkManager handles only deferrable work: daily aggregation, old-event cleanup, consistency checks, and statistics preparation. There is no permanent foreground service and no polling.

Daily limits read accumulated usage from `UsageStatsManager` and mark the application as restricted once the limit is reached; the block itself still comes from the accessibility service on the next launch. The limit is never enforced by polling.

Usage is derived from `queryEvents`, not from `queryUsageStats`. The bucketed source agrees on totals but does not reset at local midnight and cannot report sessions or an arbitrary window, so it cannot carry a daily limit. A session ends only when a **different** package is resumed, when the screen goes non-interactive, or when the day window ends. `ACTIVITY_PAUSED` and `ACTIVITY_STOPPED` are never read: they fire when a single activity becomes invisible, which happens whenever an application navigates inside itself, and reading them under-reported one browser by 93 percent in spike `A-05`. Events are queried from before the window start, so a session already running at midnight is counted from midnight rather than lost.

### Recovery

After reboot, rules remain in Room, grants are recomputed, expired grants are removed, and the dashboard reports the resulting health state. After process death, service state is restored from persistence; overlay logic never depends on an Activity being alive.

A grant stores its expiry on **two** clocks: the device wall clock and a monotonic counter that restarts at boot. Within one boot the monotonic clock decides, so moving the device clock changes nothing — spike `A-03` moved it back an hour and the grant still expired on time. A reboot is recognised by the monotonic counter running backwards, and only then does the wall clock take over; a wall clock earlier than the grant's own creation makes the grant untrustworthy and it is discarded. Expiry is evaluated on every detection, never on a timer, and no grant state is read from disk inside an accessibility callback. A grant that evaluates as spent is deleted at the moment it is noticed, and the service sweeps spent grants once when it connects, so storage never accumulates dead ones.

Taking a bypass writes the grant to storage and applies it to the in-memory snapshot in the same breath. The write is what survives a reboot; the in-memory copy is what closes the gap between the user's tap and the database notifying the service, so the application cannot block itself again a moment after granting access.

Verified end to end on an Android 16 emulator: after taking a five-minute grant, leaving and returning reported `grant=ACTIVE` with no block; a reboot mid-grant restored it as `ACTIVE_AFTER_REBOOT`, the wall clock correctly taking over from the reset monotonic counter; and after expiry the same launch reported `EXPIRED_AFTER_REBOOT`, blocked, and cleared the stored grant.

Protection health reports both, as distinct states rather than one vague failure, and each says what stops working and what survives. Phase 19 added a probe for the silent case: opening the health screen is itself a window change, so a live service must report it within a grace period; a service that does not answer its own probe is called broken rather than healthy.

The probe ignores a service that reconnected after the probe began. Android unbinds and rebinds the accessibility service on its own, and the heartbeat forgets the last event on disconnect, so a rebind inside the probe window used to leave a perfectly healthy service looking silent. That false accusation was observed on an Android 16 emulator once a permission request was hung off it. A probe older than the current connection is now treated as stale and answered with silence about it, not with an accusation; the next probe judges the new connection.

One correction to the record. Phase 19 tried to reproduce the force-stop case on an Android 16 emulator and could not: `am force-stop` **cleared** `enabled_accessibility_services` instead of leaving the service listed as enabled. The platform therefore reported the service as turned off, which is the honest outcome and better than what `A-03` saw. The enabled-but-dead state is still implemented and unit tested, because `A-03` observed it and older platforms may still behave that way, but it has not been seen on a device since. Treat it as defensive rather than confirmed.

Two failure modes are not recoverable from inside the application and must therefore be **reported** rather than repaired. Force-stopping BlockSocial kills the accessibility service and Android does not rebind it. Reinstalling can leave the service listed as bound, with a live process, delivering no events. In both cases blocking stops while the system settings screen still shows the service as enabled, so protection health must detect a service that is enabled but silent, not merely a permission that was revoked.

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

Android is built on Windows and verified on an Android emulator. No physical Android device is used; OEM behaviour is therefore unmeasured until beta, as recorded in `docs/TECHNICAL_SPECIFICATION.md` section 9.

iOS is written without local Xcode access and built by the teammate on macOS. Each handoff carries a commit hash, Xcode and iOS versions, affected targets, exact build steps, expected behavior, numbered test cases, required screenshots or recordings, log-export instructions, and known limitations. One system mechanism per handoff; unverified Swift never accumulates.

The teammate records the exact toolchain in `ios/BUILD_ENVIRONMENT.md` once that directory exists.
