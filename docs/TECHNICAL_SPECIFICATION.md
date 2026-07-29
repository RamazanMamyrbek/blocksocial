# Technical Specification

What must be true for BlockSocial to work. `ARCHITECTURE.md` describes how it is built; this document describes the requirements, constraints, and limits it must satisfy.

---

## 1. Platforms

### Android

| Item | Value |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose, Material 3 |
| `minSdk` | 26 |
| `targetSdk` / `compileSdk` | 36 |
| Build | Gradle Kotlin DSL, Version Catalog |

API 36 is the starting target because from August 31, 2026 new Play submissions must target Android 16 or higher.

### iOS

| Item | Value |
|---|---|
| Language | Swift |
| UI | SwiftUI |
| Minimum OS | iOS 17 |
| Persistence | SwiftData |
| Frameworks | FamilyControls, ManagedSettings, ManagedSettingsUI, DeviceActivity |
| Authorization | Family Controls `.individual` |

iOS 17 is chosen to use SwiftData. Older versions are not an MVP objective.

Android is built on Windows and verified on an emulator, never on a physical device; see section 9. iOS is built and tested by a teammate on macOS with a real iPhone.

---

## 2. Domain Model

### RestrictedApp

```text
id
platformIdentifier
displayName
iconReference
isEnabled
createdAt
```

Android uses a package name. iOS uses an opaque `ApplicationToken`, never a bundle ID.

### RestrictionRule

```text
id
restrictedAppId
mode            SCHEDULE | ALWAYS | DAILY_LIMIT
daysOfWeek
startLocalTime
endLocalTime
dailyLimitMinutes
isEnabled
bypassPolicy
createdAt
updatedAt
```

`DAILY_LIMIT` is an MVP mode. It is implemented after `SCHEDULE` and `ALWAYS` because it requires usage measurement that must be proven first: check `A-05` on Android and DeviceActivity thresholds on iOS, which are the same mechanism the iOS bypass depends on in check `I-05`. If either measurement proves unreliable, the limit is surfaced honestly as approximate rather than dropped or reported as exact.

`daysOfWeek`, `startLocalTime`, and `endLocalTime` are unused when the mode is `DAILY_LIMIT`; `dailyLimitMinutes` is unused for the other two modes.

### BypassPolicy

```text
type            NONE | TEMPORARY | ONE_LAUNCH
durationMinutes
maximumUsesPerDay
```

`ONE_LAUNCH` is used only where the platform makes it predictable.

### TemporaryAccessGrant

```text
id
restrictedAppId
grantedAt
expiresAt
sourceBlockEventId
status
```

### BlockEvent

```text
id
restrictedAppRef
occurredAt
zone
primaryReason           ALWAYS | FOCUS_SESSION | SCHEDULE | DAILY_LIMIT
allReasons              every reason that applied, in priority order
userAction              STAYED_FOCUSED | BYPASSED | DISMISSED_BY_SYSTEM | UNKNOWN
bypassDurationMinutes
platform
eventSchemaVersion
```

Both platforms must produce semantically identical events for the same situation.

Frozen in phase 07 as `shared/fixtures/block-event-contract.json`, which is the authoritative version; this table is the summary. Two fields changed there and the reasons are recorded in that file: `triggerReason` became `primaryReason` plus `allReasons`, because `docs/PRODUCT.md` requires the user to see one reason while the record keeps all of them, and one field cannot carry both. `zone` was added so a later reader can group events into the days the user actually lived through, even after travelling.

---

## 3. Permissions

### Android

| Permission | Purpose | Consequence if denied |
|---|---|---|
| Accessibility access | detect which application came to the front | blocking does not run; rules save but stay inactive |
| Usage access | approximate usage statistics and daily limits | statistics stop updating and daily limits cannot trigger; schedule and always-on blocking still work |
| Notifications | tell the user a temporary opening ended | optional, requested only when needed |

Required flow: user enables blocking, the app shows its own explanation, the user opens system settings, the app verifies the result. Usage access is requested separately.

Not used: `SYSTEM_ALERT_WINDOW`, `QUERY_ALL_PACKAGES`.

### iOS

| Permission | Purpose |
|---|---|
| Screen Time authorization, `.individual` | apply and remove system shields |
| Notifications | optional, same purpose as Android |

Family Controls is a privileged entitlement. It must be present on the main app and on every Screen Time extension, each with its own App ID and provisioning profile, and a distribution entitlement must be granted by Apple.

---

## 4. Platform Constraints

### Android

- The Accessibility service needs only the package name and a transition timestamp. It must not read screen text, messages, keystrokes, or passwords, and must not automate actions inside third-party applications.
- The block surface is a `TYPE_ACCESSIBILITY_OVERLAY` hosted by the service. No second overlay permission is requested.
- Installed-app visibility is restricted from Android 11. The app declares specific packages through `<queries>` and presents a curated catalog of supported applications. It never claims to support every installed app.
- `UsageStatsManager` gives aggregated, device-dependent results. It feeds statistics and daily-limit evaluation, but it is never the immediate blocker: the block itself always comes from the accessibility service on the next launch. A daily limit therefore triggers slightly late, and the UI says so instead of implying an exact cutoff.
- No permanent foreground service. Deferrable work runs through WorkManager.

### iOS

Apple's limits are hard and shape the product:

| Limit | Value |
|---|---|
| Minimum monitored interval | 15 minutes |
| Maximum event-monitoring interval | 1 week |
| Concurrently monitored activities | 20 |
| Application tokens in a shield | 50 |

Consequences: schedules must be compiled and grouped, capacity must be validated before saving, and a rule that exceeds capacity must be rejected with an actionable message. A selection is never silently truncated.

The shield is a system surface. It offers a fixed layout with an icon, a title, a subtitle, and a limited number of buttons. It cannot host arbitrary layout, navigation, scrolling, or input. There is no supported way to launch the main app from a shield action, so no flow may depend on it.

Tokens returned by `FamilyActivityPicker` are opaque. The app must not expect bundle IDs, attempt to de-anonymize them, transmit them, or treat them as cross-device identifiers.

---

## 5. Non-Functional Requirements

**Reliability.** No crash after permission revocation. No overlay loop. Idempotent schedule registration. Predictable recovery after reboot and process death.

**Performance.** No database work inside accessibility callbacks. Minimal iOS extensions. Cached statistics aggregates.

**Battery.** No foreground-app polling, no VPN, no permanent foreground service.

**Offline.** Every MVP feature works with no network. No screen shows a connectivity state.

**Privacy.** The app does not collect messages, screen text, passwords, browser history, contacts, location, or advertising identifiers. No third-party analytics SDK before a privacy review. Release logs contain no application names, tokens, or user content.

**Accessibility.** TalkBack and VoiceOver, Dynamic Type and large font scales without truncation, sufficient contrast in both themes, large touch targets, Reduce Motion respected, no meaning carried by color alone. The block screen must be fully operable with a screen reader.

**Localization.** Russian and English. No hardcoded user-facing strings. Layouts tolerate about thirty percent text expansion.

---

## 6. Unverified Assumptions

None of the following may be presented as a guaranteed capability until a spike proves it.

| Assumption | Status |
|---|---|
| The accessibility overlay is stable on API 36 across OEMs | unverified, and cannot be verified before beta: no physical device is used |
| Accessibility events are delivered the same way on OEM firmware as on stock Android | unverified, and cannot be verified before beta |
| Google Play accepts the Accessibility use case | unverified |
| The curated catalog covers enough applications | unverified |
| Apple grants the Family Controls distribution entitlement | unverified |
| A five-minute bypass is achievable on iOS | unverified, fallback is fifteen minutes |
| Screen Time extensions behave correctly through TestFlight | unverified |
| Schedule capacity has a workable UX on iOS | unverified |
| Usage measurement is accurate enough to drive daily limits on Android | **verified** on emulator images for API 33 and 36 by spike `A-05`: error under 0.1 percent over five minutes, local-midnight boundary exact |
| Usage measurement is accurate enough to drive daily limits on iOS | unverified |

---

## 7. Risks That Can Stop the Project

| ID | Risk | Impact | Mitigation |
|---|---|---|---|
| R-01 | Google Play rejects the Accessibility use case | critical | the full package is written and cross-read against the code in `docs/store/play/`: prominent disclosure, affirmative consent, Play Console declaration, listing copy, privacy policy, demo-video script, and a data inventory traced line by line. `isAccessibilityTool=false`, one subscribed event type, `canRetrieveWindowContent=false`, no tree inspection, no `INTERNET`, no `QUERY_ALL_PACKAGES`. Phase 23 re-verifies against the shipping build. The residual risk is a review decision and cannot be removed by documentation. |
| R-02 | Apple does not grant the Family Controls entitlement | critical for iOS, not for the project | the request is filed in phase 27, which opens the iOS track before any Swift is written, so a refusal costs no iOS work. The consequence is accepted deliberately: the answer is not known during the Android cycle at all. Phase 27 depends on nothing and can be pulled forward at any time. A pending request is never recorded as an approval |
| R-03 | The iOS bypass cannot be made predictable | high | real-device proof; fallback to a fifteen-minute wall-clock grant; keep the duration a variable in all copy |
| R-04 | The Android overlay is unstable on API 36 | high | prove the overlay before building UI on it; keep a simpler fallback design; emulator proof only, so OEM instability surfaces first in beta |
| R-06 | OEM firmware breaks detection or kills the service, and nothing catches it before release | high | accepted deliberately: development uses an emulator only; the protection health screen must surface a dead service, and the Android beta must recruit Samsung and Xiaomi testers |
| R-05 | App selection turns out to require `QUERY_ALL_PACKAGES` | high | curated catalog plus targeted `<queries>`; the product promises supported applications only |

Secondary risks with known mitigations: OEM background termination, handled by a protection health screen and honest positioning; duplicate accessibility events, handled by debounce and a state machine; overlay covering critical system UI, handled by a system allowlist, a watchdog timeout, and removal on package change.

Risks that no amount of code removes: store review decisions, OEM differences, future platform changes, and user willingness to grant permissions. These need early evidence, not more classes.

---

## 8. Validation Before Development

Feature implementation does not begin until that platform's central mechanisms are proven: on an Android emulator for Android, on a real iPhone for iOS. The checks themselves are early phases of `PLAN.md` and do involve minimal, throwaway code.

The two platforms are validated at different times. Android validation, checks `A-01` through `A-06`, is complete. iOS validation, checks `I-01` through `I-07`, runs after Android has shipped. The consequence is that the shared domain contract is written from Android evidence alone and iOS conforms to it afterwards; where that conflicts with an Apple limit, the contract changes and Android changes with it.

Each check produces a short result document with an environment, a commit hash, evidence, and a `GO`, `CHANGE`, or `STOP` decision. Code that compiles is not a passed check.

### Android

| ID | Question |
|---|---|
| A-01 | Does the Accessibility service detect a transition into a target application reliably on `targetSdk 36`? |
| A-02 | Does an interactive accessibility overlay work without `SYSTEM_ALERT_WINDOW`? |
| A-03 | Does an app-specific grant suppress repeated blocks correctly, including after reboot? |
| A-04 | Does the curated catalog work without `QUERY_ALL_PACKAGES`? |
| A-05 | Is usage data good enough for approximate statistics and for daily limits, across emulator images for Android 10, 13, and 16? OEM variation stays unmeasured until beta. |
| A-06 | Is the Play policy package consistent with what the code actually does? |

### iOS

| ID | Question |
|---|---|
| I-01 | Do all targets build and sign, with the entitlement requested? |
| I-02 | Do authorization and the picker persist correctly across restart and reinstall? |
| I-03 | Does the shield appear and disappear on schedule without the main app running? |
| I-04 | Do shield actions work without private APIs and without launching the parent app? |
| I-05 | Is a usage-threshold bypass predictable across ten consecutive runs? The same threshold mechanism carries daily limits. |
| I-06 | Are capacity limits detected before saving, leaving no stale shield? |
| I-07 | Can the teammate build a commit and return a structured report with no verbal explanation? |

### Cross-platform

Shared fixtures exist for the rule evaluator, frozen in phase 07 as `shared/fixtures/`, and both platforms must produce identical domain results against them. The corpus covers normal interval, overnight interval, selected weekday, disabled rule, active bypass, expired bypass, a bypass that does not leak to a second application, daily limit not yet reached, daily limit reached, daily limit reset at local midnight, time-zone change, and both daylight-saving transitions, together with the two clock rules proven in check `A-03` and the day-boundary arithmetic proven in check `A-05`.

`shared/fixtures-validator` enforces the shape and reports coverage gaps against the business rules in `docs/PRODUCT.md`. Four cases are marked as at risk from Apple's fifteen-minute minimum monitored interval and from the assumption that a monotonic counter resets at boot; check `I-05` and the iOS gate resolve them, and a case that cannot be satisfied changes the contract for both platforms rather than being worked around on one.

### Go decision

Android proceeds when the overlay is stable, no loops exist, the catalog works, service state restores, the policy package is ready, and critical system apps are never blocked.

iOS proceeds when the entitlement is available or its process is confirmed, all targets sign, the picker works, the shield appears, a bypass or its fallback is proven, a TestFlight build runs, and extensions read the App Group payload.

---

## 9. Testing

**Unit.** Schedule evaluator, overnight intervals, DST, rule priority, bypass expiry, daily-limit accumulation and midnight reset, statistics, permission state reducers. Both platforms run against the shared fixtures.

**Android.** Emulator only. No physical Android device is used in any development phase.

Every Android scenario is driven from the host over `adb`, with no manual interaction: `adb install`, `settings put secure` to enable the accessibility service, `input keyevent` and `monkey` to act, `logcat` to read the result. Coverage across API levels comes from separate emulator images; Android 10, 13, and 16 remain the target levels.

This is a deliberate limitation, not an oversight. An emulator runs stock Android, so it cannot show how Samsung, Xiaomi, or Huawei firmware terminates background services, restricts autostart, or delivers accessibility events. Those differences are risks `R-04` and `R-06`. They are carried by the Android beta, which must recruit testers on at least Samsung and Xiaomi hardware, and by the protection health screen, which must make a dead service visible to the user rather than silently failing.

Never describe emulator evidence as device evidence.

**iOS device.** A real iPhone on iOS 17 or later, both a development build and TestFlight, covering reboot, revoked authorization, time-zone change, and low memory.

---

## 10. Definition of Done

A feature is complete when requirements are documented, code is implemented, unit tests pass, platform tests pass, Android is verified on an emulator, iOS is verified by the teammate when affected, permissions are tested, documentation is updated, no critical or major defects remain, and a reproducible commit exists.
