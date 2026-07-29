# Spike A-01 — Android Launch Detection

**Question.** Does the Accessibility service detect a transition into a target application reliably on `targetSdk 36`?

**Decision.** `GO`, with two mandatory design constraints that this spike discovered and verified, and one unresolved product gap. Evidence is from an emulator only; OEM behaviour remains unverified.

---

## Environment

| Item | Value |
|---|---|
| Host | Windows 11 Home Single Language 10.0.26200, project owner |
| JDK | 17.0.7+8-LTS-224 |
| Gradle | 8.14.4 (wrapper) |
| Android Gradle Plugin | 8.13.2 |
| Kotlin | 2.2.21 |
| `compileSdk` / `targetSdk` | 36 |
| `minSdk` | 26 |
| Device | **emulator**, `sdk_gphone64_x86_64`, Android 16, API 36, `google_apis` x86_64 image rev 7 |
| Emulator | 37.2.1, WHPX acceleration |
| Platform Tools | 37.0.0 (`adb` 1.0.41) |

**No physical device was used.** The owner has never done mobile development and chose the emulator path. Everything below was driven from the host over `adb`; no manual interaction with the device took place.

The Android SDK was empty before this phase. `cmdline-tools` rev 22, `platform-tools`, `platforms;android-36` and `build-tools;36.0.0` were installed by the agent. The emulator and the system image were downloaded manually by the owner after the agent's download proved too slow, and were verified by SHA-1 before extraction. The emulator package needed a hand-written `package.xml`, because a manually placed package carries no SDK metadata.

## Commit

`e775e3074bcf218571f6d568d60f5ea5e8eec98f` on `phase/01-android-launch-detection`.

## Target applications

Instagram and TikTok were the initial hardcoded targets. Neither exists on an emulator image, so they were replaced with two applications that do: `com.google.android.youtube` and `com.android.chrome`. Both are realistic targets for this product. Changing them is a one-line edit of `TARGET_PACKAGES` in `LaunchDetectionService`.

---

## What the spike found

The naive design — treat every `TYPE_WINDOW_STATE_CHANGED` event's `packageName` as the foreground app, and suppress repeats with a time-based debounce — **does not work**. It failed in two distinct ways on the first device run, and both failures are invisible to unit tests written against the assumed model.

### Defect 1: one launch, several detections

A single cold launch of YouTube produced two `TARGET_ENTERED` results three seconds apart:

```
14:56:31.799 package=com.google.android.youtube decision=TARGET_ENTERED
14:56:34.872 package=com.google.android.youtube decision=TARGET_ENTERED
14:56:35.069 package=com.google.android.youtube decision=IGNORED_DEBOUNCED
```

Opening YouTube through a deep link produced three. An application emits several window-state changes while starting, and they are spread further apart than any sane debounce window. A 1000 ms window cannot separate "the app is still opening" from "the user opened the app again". In the product this would mean the block screen appearing two or three times for one launch.

**Cause.** Debounce keyed on elapsed time answers the wrong question. The question is not *how long ago* the app was handled, but *whether the user left it and came back*.

### Defect 2: a real re-entry produced no detection at all

After defining entry as a change of foreground package, pressing home and reopening YouTube produced nothing. The capture explains why:

```
15:04:25.806 package=com.google.android.apps.nexuslauncher decision=IGNORED_SYSTEM
15:04:26.463 package=com.google.android.apps.nexuslauncher decision=IGNORED_SYSTEM
15:04:27.176 package=com.google.android.youtube        decision=TARGET_ENTERED   <-- 1.4 s after home
```

`topResumedActivity` at that moment was the launcher. YouTube was already in the background and still emitting window-state events. The state machine recorded YouTube as foreground again, so the genuine relaunch six seconds later was suppressed as "already foreground".

**Cause.** `TYPE_WINDOW_STATE_CHANGED` plus `event.packageName` is not a foreground signal. It fires for windows of applications that are leaving, and the ordering against the incoming application is not guaranteed.

**What distinguishes them.** Logging `event.className` showed the difference immediately:

```
package=com.google.android.youtube class=android.widget.FrameLayout                              activity=false
package=com.google.android.youtube class=com.google.android.apps.youtube.app.watchwhile.MainActivity activity=true
```

The spurious events are plain views. Real entries carry an activity class.

### The corrected rule

Both constraints are now in `LaunchDetector` and both are mandatory for production:

1. **An event counts only if `packageName` plus `className` resolve to a real activity**, checked with `PackageManager.getActivityInfo`. This needs targeted package visibility through `<queries><package .../></queries>` — the curated-catalog approach of spike A-04. It never needs `QUERY_ALL_PACKAGES`.
2. **An entry is a change of the foreground package.** Any number of further activity windows inside the same application is one visit. Leaving to a system or non-target application ends the visit, so returning is detected again.

A consequence worth recording: with rule 2 in place, the time-based debounce specified for this phase becomes unreachable. It is retained in the code because the phase asked for it, but it no longer decides anything. Production should either drop it or make it per-package, which is the only form that would help the multi-window case below.

---

## Scenario results

All eight scenarios were executed on the final build. `TARGET_ENTERED` counts are what matters.

| # | Scenario | Result | Detections |
|---|---|---|---|
| 1 | Cold launch from the launcher | pass | 1 |
| 2 | Return to a running target | pass | 1 |
| 3 | Deep link into a target | pass | 1 |
| 4 | Unlock with a target already in front | **see gap below** | 0 |
| 5 | Rapid switching between two targets | pass | 3 for 3 switches |
| 6 | Multi-window with two targets | pass | 1 |
| 7 | Launcher, then Settings | pass | 0, both `IGNORED_SYSTEM` |
| 8 | System permission dialog over a target | pass | 1, dialog caused no false detection |

Scenario 7, verbatim:

```
package=com.android.settings  class=com.android.settings.homepage.SettingsHomepageActivity  decision=IGNORED_SYSTEM
package=com.google.android.apps.nexuslauncher class=...NexusLauncherActivity                decision=IGNORED_SYSTEM
```

The launcher package was never hardcoded. It is resolved at runtime from an `ACTION_MAIN` + `CATEGORY_HOME` query, and `com.google.android.apps.nexuslauncher` was classified correctly without being named anywhere in the source.

### No detection storms

Ten consecutive launch-and-home cycles:

```
TARGET_ENTERED count       : 10
IGNORED_NOT_AN_ACTIVITY    : 9
IGNORED_ALREADY_FOREGROUND : 10
IGNORED_SYSTEM             : 20
total events               : 49
```

Ten launches, ten detections, from 49 raw events. Nineteen of the suppressed events would have been false blocks under the naive rule.

### Latency

Delivery latency is measured as `SystemClock.uptimeMillis() - event.eventTime` at the moment the decision is taken.

```
n=10  min=106  max=140  mean=119.3   (milliseconds)
```

Around 120 ms on an emulator with software-assisted virtualisation. Comfortably fast enough for a block screen. This figure is not representative of physical hardware.

---

## Automated checks

Run by the agent on commit `e775e30`.

| Check | Result |
|---|---|
| `./gradlew :a01:assembleDebug` | `BUILD SUCCESSFUL` |
| `./gradlew :a01:test` | `tests=13 failures=0 errors=0 skipped=0` |
| `./gradlew :a01:lint` | `0 errors, 6 warnings` |
| Merged manifest scan | `absent: SYSTEM_ALERT_WINDOW`, `absent: QUERY_ALL_PACKAGES`, `uses-permission entries: (none)` |

The merged manifest declares **no permissions at all**. The six lint warnings are accepted for throwaway code: `isAccessibilityTool` applies from API 31, newer Gradle, AGP and Kotlin exist, `allowBackup` is deprecated, and there is no application icon.

Three unit tests are regressions for what the device found: `a splash screen and the main activity are one visit even seconds apart`, `a non-activity window while backgrounding does not start a visit`, and `two separate visits to the same target are two detections`. Each one fails against the pre-fix implementation.

### Privacy properties

- `accessibilityEventTypes` is `typeWindowStateChanged` only; `canRetrieveWindowContent="false"`; `isAccessibilityTool="false"`.
- The service reads `event.packageName`, `event.className` and `event.eventTime`. It never touches `event.text` or `event.source`, and dispatches no action into any application.
- `className` is a component name, not user content. It is used solely to ask `PackageManager` whether the window is an activity.
- All logging is inside `if (BuildConfig.DEBUG)`.

---

## Unresolved gap: unlocking into an open target

Scenario 4 produces **no detection**. Locking and unlocking with YouTube in front yields only:

```
package=com.google.android.youtube class=...MainActivity decision=IGNORED_ALREADY_FOREGROUND
```

The keyguard produced no window-state event, so the visit never ended and the unlock does not read as a new entry.

Under the naive rule this case did fire, but only by accident, as a side effect of the bug in defect 2.

This is a product decision, not only a technical one. Picking up the phone and finding Instagram already open is exactly the automatic behaviour this product exists to interrupt. The likely fix is to end the current visit on `ACTION_SCREEN_OFF` or `ACTION_USER_PRESENT` rather than relying on accessibility events. It must be designed and verified in the production detection phase, phase 13.

## Findings for production

1. **Both corrected rules are mandatory.** Neither is optional hardening; without them the block screen either repeats or never appears.
2. **Targeted package visibility is required** for the activity check, reinforcing the curated-catalog decision that spike A-04 will test.
3. **Multi-window is unproven under load.** Scenario 6 passed with one detection, but before the fix, alternating windows produced alternating detections. A per-package debounce map would be the guard if multi-window must be supported.
4. **The launcher must stay dynamically resolved.** Hardcoding launcher package names would have failed on this image.
5. **AGP resolved Build Tools 35.0.0** by default despite `compileSdk 36` and 36.0.0 being installed. Pin `buildToolsVersion` in production if the exact version matters.

## Limits of this evidence

- Emulator only. Stock Android 16 with Google APIs. **No OEM was tested**, and OEM divergence in accessibility event delivery is exactly risk `R-04`. Nothing here reduces that risk.
- Latency figures come from a virtualised device and say nothing about physical hardware.
- No reboot, no permission revocation, no long-running or low-memory behaviour was tested. Those belong to phases 04 and 21.
- Two of eight scenarios were shaped by the emulator: the long-press power gesture opened the assistant rather than a power menu, so scenario 8 was retested with a genuine system permission dialog instead, and split-screen was entered through `am start --windowingMode 6` rather than by gesture.

Physical-device confirmation of these eight scenarios remains required and is carried by the Android beta, phase 25.

## Defects

| # | Defect | Status |
|---|---|---|
| 1 | One launch produced several detections | fixed, regression test added |
| 2 | Return to a target produced no detection | fixed, regression test added |
| 3 | Unlocking into an open target produces no detection | **open**, assigned to phase 13 |
