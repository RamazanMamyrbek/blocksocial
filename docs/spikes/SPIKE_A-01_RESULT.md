# Spike A-01 — Android Launch Detection

**Question.** Does the Accessibility service detect a transition into a target application reliably on `targetSdk 36`?

**Decision.** `PENDING` — automated checks pass; the decision requires device evidence that only the owner can produce. See [Outstanding](#outstanding).

---

## Environment

| Item | Value |
|---|---|
| Machine | Windows 11 Home Single Language 10.0.26200, project owner |
| JDK | 17.0.7+8-LTS-224 (`C:\Program Files\Java\jdk-17`) |
| Gradle | 8.14.4 (wrapper, `bin` distribution) |
| Android Gradle Plugin | 8.13.2 |
| Kotlin | 2.2.21 |
| `compileSdk` / `targetSdk` | 36 (SDK Platform 36, `Pkg.Revision=2`) |
| `minSdk` | 26 |
| Build Tools | 36.0.0 installed; AGP resolved its own default of 35.0.0 |
| Platform Tools | 37.0.0 (`adb` 1.0.41) |
| Device | not yet run |

The Android SDK was empty before this phase. `cmdline-tools` rev 22, `platform-tools`, `platforms;android-36` and `build-tools;36.0.0` were installed into the existing `C:\Users\ramam\AppData\Local\Android\Sdk`. Android Studio on this machine is 2022.3 and was not used; the build runs from the Gradle wrapper.

## Commit

`6ea0534cfa884928f06ee536ab76fa15f8b6129b` on `phase/01-android-launch-detection`.

## What was built

`spikes/android/` is a standalone Gradle build hosting one module, `:a01`. Later Android spikes add sibling modules to the same build.

| File | Role |
|---|---|
| `LaunchDetector.kt` | pure Kotlin transition state machine, no Android imports, unit tested |
| `LaunchDetectionService.kt` | `AccessibilityService`, reads `event.packageName` and `event.eventTime` only |
| `SystemPackages.kt` | critical-package exclusion set plus launcher packages resolved at runtime |
| `OpenAccessibilitySettingsActivity.kt` | launcher entry that opens Accessibility settings and finishes |
| `accessibility_service_config.xml` | `typeWindowStateChanged` only, `canRetrieveWindowContent="false"`, `isAccessibilityTool="false"` |

### Decision model

`LaunchDetector.onForegroundPackageChanged` returns exactly one `TransitionDecision`:

| Decision | Condition |
|---|---|
| `IGNORED_UNKNOWN_PACKAGE` | package name null or blank |
| `IGNORED_SELF` | the spike's own package; does **not** reset the debounce window |
| `IGNORED_SYSTEM` | launcher, Settings, SystemUI, dialer, permission controller; resets the window |
| `IGNORED_NOT_TARGET` | any other package; resets the window |
| `IGNORED_DEBOUNCED` | same target package again within `debounceWindowMillis` |
| `TARGET_ENTERED` | a target package entered outside the window |

Two behaviours are deliberate and are the ones the device run must confirm:

1. **Leaving a target resets the window.** Otherwise returning from the launcher inside one second would be silently swallowed, breaking scenario 2.
2. **The spike's own package does not reset the window.** In phase 02 the block overlay belongs to us, so a target → BlockSocial → target sequence must not re-trigger.

Debounce window: `1000 ms`, a single constant in `LaunchDetectionService`. Target packages are hardcoded there as `com.instagram.android` and `com.zhiliaoapp.musically`.

## Automated evidence

All four automated checks were run by the agent on the commit above.

### `./gradlew :a01:assembleDebug`

```
BUILD SUCCESSFUL in 4m 43s
37 actionable tasks: 37 executed
```

Output: `a01/build/outputs/apk/debug/a01-debug.apk`, 859 426 bytes.

### `./gradlew :a01:test`

```
suite=com.blocksocial.spike.a01.LaunchDetectorTest tests=10 failures=0 errors=0 skipped=0 time=0.071s
```

| Test | Covers |
|---|---|
| entering a target application is detected | happy path |
| repeated events for the same target inside the window are debounced | event flood |
| the same target is detected again once the window has passed | window expiry |
| leaving to a non-target allows immediate re-entry to be detected | scenario 2 |
| switching between two targets is detected each time | scenario 5 |
| own package is ignored and does not reset the debounce window | phase 02 precondition |
| system packages are ignored | scenario 7 |
| packages outside the target set are ignored | scenario 7 |
| missing package names are ignored | null safety |
| an event flood on one target produces a single detection | 50 events → 1 detection, 49 suppressed |

### `./gradlew :a01:lint`

```
0 errors, 6 warnings
```

The check requires no errors. The six warnings are accepted for throwaway spike code:

| Warning | Why accepted |
|---|---|
| `isAccessibilityTool` only used on API 31+ | intentional; it is the R-01 mitigation and is ignored below API 31 |
| newer Gradle 8.14.5 available | version pinned deliberately |
| newer AGP 9.3.1 available | AGP 8.13.2 chosen for stability on this spike |
| newer Kotlin 2.4.10 available | Kotlin 2.2.21 chosen to match AGP 8.13 |
| `allowBackup` deprecated | spike stores no data |
| missing application icon | spike has no UI |

### Forbidden permissions

Scanned the **merged** manifest at `a01/build/intermediates/merged_manifest/debug/processDebugMainManifest/AndroidManifest.xml`:

```
absent: SYSTEM_ALERT_WINDOW
absent: QUERY_ALL_PACKAGES
all uses-permission entries: (none)
```

The merged manifest declares no permissions at all. Launcher resolution uses a targeted `<queries>` element for `ACTION_MAIN` + `CATEGORY_HOME`, which is the direction spike A-04 will extend.

### Privacy properties verified in code

- `accessibilityEventTypes` is `typeWindowStateChanged` only.
- `canRetrieveWindowContent="false"`.
- The service reads `event.packageName` and `event.eventTime`; it never touches `event.text`, `event.source`, or `event.className`.
- No action is dispatched into any third-party application.
- Logging is wrapped in `if (BuildConfig.DEBUG)`.

## Outstanding

The Definition of Done requires all eight scenarios on at least one real device. **No device was connected** (`adb devices` returned an empty list), so none of them has been executed and no latency figure exists yet.

### How to run

```
adb install -r spikes/android/a01/build/outputs/apk/debug/a01-debug.apk
adb logcat -c
adb logcat -s SpikeA01:I
```

Open the installed app once — it goes straight to Accessibility settings — and enable **BlockSocial Spike A-01 launch detection**. The connection line appears immediately:

```
I SpikeA01: connected targets=[com.instagram.android, com.zhiliaoapp.musically] debounceMillis=1000
```

Every subsequent window transition logs one line:

```
I SpikeA01: package=<pkg> decision=<DECISION> deliveryLatencyMillis=<n>
```

If neither Instagram nor TikTok is installed, change `TARGET_PACKAGES` in `LaunchDetectionService.kt` and rebuild.

### Scenarios to confirm

| # | Scenario | Expected |
|---|---|---|
| 1 | Cold launch of a target app from the launcher | one `TARGET_ENTERED` |
| 2 | Return to a target app from recents | one `TARGET_ENTERED` |
| 3 | Open a target app from a notification deep link | one `TARGET_ENTERED` |
| 4 | Unlock with a target app already in the foreground | recorded either way; note which |
| 5 | Switch rapidly between two target apps | one `TARGET_ENTERED` per switch, no storm |
| 6 | Split-screen with a target app | recorded; note the observed behaviour |
| 7 | Open the launcher, then Settings | `IGNORED_SYSTEM`, never `TARGET_ENTERED` |
| 8 | Trigger a system dialog | `IGNORED_SYSTEM` or `IGNORED_NOT_TARGET`, never `TARGET_ENTERED` |

Scenarios 4 and 6 have no single correct answer yet; the spike records what the platform actually does so the product can decide.

Also needed to close this spike: device manufacturer, model and Android version, the observed `deliveryLatencyMillis` range, and confirmation that no detection storm occurs.

## Findings for production

1. **The service is not restricted with `android:packageNames`.** Restricting it to the target packages would be a real privacy improvement, but it would stop delivery of launcher and Settings events, and the reset-on-leave rule depends on seeing them. Production must choose between the narrower event surface and this reset behaviour, or find another way to detect leaving a target.
2. **AGP resolved Build Tools 35.0.0 by default** even with `compileSdk 36` and 36.0.0 installed. Harmless here; the production build should pin `buildToolsVersion` explicitly if the exact version matters.
3. **The debounce window is a single constant.** If device evidence shows 1000 ms is wrong, the value changes in one place and the unit tests take the new value as a constructor argument.

## Defects

None found so far. The section stays open until the device run completes.
