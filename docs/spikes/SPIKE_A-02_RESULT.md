# Spike A-02 — Android Accessibility Overlay

**Question.** Does an interactive full-screen `TYPE_ACCESSIBILITY_OVERLAY` work without `SYSTEM_ALERT_WINDOW`, and can it always be removed?

**Decision.** `GO`. The overlay works, is interactive, never covers system surfaces, and could not be made to stick in any tested case. Two items are unresolved: the back key is a free bypass, and screen-reader operation is unconfirmed.

---

## Environment

| Item | Value |
|---|---|
| Host | Windows 11 Home Single Language 10.0.26200 |
| Device | emulator `sdk_gphone64_x86_64`, Android 16, API 36, `google_apis` x86_64 rev 7 |
| Gradle / AGP / Kotlin | 8.14.4 / 8.13.2 / 2.2.21 |
| Compose BOM | 2026.06.01 |
| JDK | 17.0.7 |

Emulator only, no physical device, per `docs/TECHNICAL_SPECIFICATION.md` section 9.

## Commit

`17c4effe6504d9d095c6ff31a6247edffc5172d8` on `phase/02-android-overlay`.

## Design direction

Rendered direction **1b, "Quiet"**. `design/DESIGN_EXPORT_ANALYSIS.md` section 9.6 leaves the block-screen direction formally open as `Q-02`, but recommends 1b as the baseline that must work and marks 1c as provisional on this very spike. 1b is what was built.

The oklch tokens were converted to hex as documented: dark `surface #11171B`, `on-surface #E8ECEF`, `on-surface-variant #A1A9AF`, `outline #50565B`, `primary #6FBEBE`; light `surface #F5F8FA`, `on-surface #1B2127`, `primary #207071`. Primary carries dark text in dark theme and white in light theme, which is what keeps it above 4.5:1 in both.

Layout as specified: app line, 34 sp headline, one supporting sentence, 60 dp primary, 56 dp secondary, monospace footnote, in that order, so a screen-reader user reaches the way out before the statistics.

## What was verified

| # | Scenario | Result |
|---|---|---|
| 1 | Overlay appears full screen over a target | pass |
| 2 | Primary action returns home and removes the overlay | pass, `reason=STAY_FOCUSED` |
| 3 | Secondary action removes the overlay, app usable | pass, `reason=OPEN_TEMPORARILY` |
| 4 | Rotation while shown | **not verified**, see below |
| 5 | Dark theme | pass |
| 6 | System back and system home | home passes; **back is a defect**, see below |
| 7 | Ten triggers, no leak, no stacking | pass |
| 8 | Process killed while shown | pass, device not locked |
| 9 | TalkBack | **partially verified**, see below |
| 10 | Largest font scale | pass |
| 11 | Settings and launcher never covered | pass |

### The window is what it claims to be

```
mAttrs={(0,0)(fillxfill) gr=TOP START ty=ACCESSIBILITY_OVERLAY fmt=TRANSLUCENT
```

As accessibility sees it, with the overlay up over YouTube:

```
AccessibilityWindowInfo[title=BlockSocial pause, type=TYPE_ACCESSIBILITY_OVERLAY,
                        layer=2, bounds=Rect(0, 128 - 1080, 2400), focused=true, active=true]
AccessibilityWindowInfo[title=YouTube,           type=TYPE_APPLICATION, layer=0, focused=false]
```

Layer 2 over the application at layer 0, focused and active.

### Primary and secondary actions

```
overlay dismissed package=com.google.android.youtube reason=STAY_FOCUSED
package=com.google.android.apps.nexuslauncher decision=IGNORED_SYSTEM
```

`GLOBAL_ACTION_HOME` returns to the launcher and the overlay is gone. The secondary action leaves the user in the application, and the detector then reports `IGNORED_ALREADY_FOREGROUND`, so the block does not immediately reappear.

### Ten triggers, nothing left behind

```
TARGET_ENTERED       : 10
overlay dismissed    : 10
  by PACKAGE_CHANGED : 10
  by WATCHDOG        : 0
overlay windows left : 0
```

### System surfaces are never covered

Pressing home while the overlay is shown removes it through the launcher event, before the launcher is visible:

```
package=com.google.android.apps.nexuslauncher decision=IGNORED_SYSTEM
overlay dismissed package=com.google.android.youtube reason=PACKAGE_CHANGED
```

Opening Settings and the launcher directly produced `IGNORED_SYSTEM` throughout and zero overlay windows.

### The device cannot be left locked

Force-stopping the spike process with the overlay on screen dropped the window count from 5 to 0, and home still worked. The thirty-second watchdog is the second net: an early screenshot in this spike came back showing YouTube rather than the overlay precisely because the watchdog had already fired.

### Largest font scale

At `font_scale 2.0` the headline wraps to two lines, the supporting sentence to four, both buttons grow with `heightIn(min = …)` rather than clipping, and the footnote wraps. Nothing truncates and the whole decision stays on one screen with no scrolling.

## Automated checks

| Check | Result |
|---|---|
| `./gradlew :a02:assembleDebug` | `BUILD SUCCESSFUL` |
| `./gradlew :a02:test` | `tests=5 failures=0 errors=0` |
| `./gradlew :a02:lint` | `0 errors, 5 warnings` |
| `./gradlew :a02:connectedDebugAndroidTest` | `tests=6 failures=0 errors=0`, run on the emulator |
| Merged manifest | `absent: SYSTEM_ALERT_WINDOW`, `absent: QUERY_ALL_PACKAGES` |

The merged manifest contains exactly one permission, `com.blocksocial.spike.a02.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`, generated by androidx.core. It is a signature permission scoped to this package and grants nothing to anyone else.

The six instrumented tests are the attach and detach evidence the phase asks for: `overlayAttachesAndDetaches`, `showingTwiceForTheSamePackageDoesNotStack`, `leavingTheBlockedPackageRemovesTheOverlay`, `stayingInTheBlockedPackageKeepsTheOverlay`, `aStuckOverlayIsRemovedByTheWatchdog`, `switchingBlockedPackageReplacesRatherThanStacks`. They drive the real controller against a fake `WindowManager`, so the Compose view, its lifecycle owners and the watchdog all run on-device while the privileged window type stays out of the way.

## Defects and open items

### 1. The back key is an unrecorded bypass — open

```
overlay dismissed package=com.google.android.youtube reason=BACK_PRESSED
package=com.google.android.youtube decision=IGNORED_ALREADY_FOREGROUND
```

Pressing back removes the overlay and leaves the user inside the application, with no event recorded and no bypass grant created. For a product whose entire value is *recording the decision*, a silent third exit is wrong.

Three options for phase 19, and this is a product decision, not a technical one:

1. Back behaves as `Stay focused`: go home, record a refusal.
2. Back is ignored, leaving only the two stated choices.
3. Back behaves as `Open temporarily` and is recorded as a bypass.

Option 1 matches the tone rule that the block screen is a pause rather than a trap. Option 2 risks reading as a lock.

### 2. Rotation was not verified — open

The scenario could not be executed. The AVD does not rotate at all: with `accelerometer_rotation 0` and `user_rotation 1` the display stayed `cur=1080x2400` **with no overlay present**, and `cmd window set-user-rotation` does not exist on this image. This is an emulator limitation, not overlay behaviour. Rotation must be checked in phase 19 on an AVD configured for it, or accepted as a beta item.

### 3. Screen-reader operation is unconfirmed — open

Confirmed: the overlay window is exposed to accessibility services with `focused=true`, `active=true`, and a real title after this spike added one. Before the fix the title was `null`, which would have made a screen reader announce an untitled window; `WindowManager.LayoutParams.accessibilityTitle` is not in the public SDK, so `setTitle` is used instead and does reach the accessibility window list.

Not confirmed: that TalkBack reads the content and that the decision can be completed with TalkBack alone. TalkBack was installed and enabled alongside the spike service, but `uiautomator dump` kept returning the application's node tree rather than the overlay's, so the overlay's nodes could not be retrieved with adb tooling. Scenario 9 therefore has no evidence either way and belongs to phase 27.

### 4. The status bar is not covered — recorded, not a defect

Overlay bounds start at `y=128`, below the status bar, so the clock and the notification shade stay reachable. This is consistent with the rule against imitating system UI and with leaving the user in control, but it does mean the shade can be pulled down over the block screen. Phase 19 should decide deliberately rather than inherit this by accident.

## Findings for production

1. **Compose works in an accessibility overlay**, with no fallback to Views needed, but only because `OverlayViewHost` supplies a `LifecycleOwner`, a `ViewModelStoreOwner` and a `SavedStateRegistryOwner`. Without those three the `ComposeView` will not compose. First composition cost about 650 ms in one measured case, visible as the delivery latency of the overlay's own window event; subsequent shows were around 110 ms.
2. **The overlay generates its own window-state events** under our package. The self-exclusion rule carried over from A-01 is what stops it from re-triggering itself, and this spike is where that rule earns its place.
3. **Dismissal must be driven by the foreground event, not by the button alone.** Removal on package change is what keeps the overlay off the launcher and off Settings.
4. **Keep the watchdog.** It is the only guard that does not depend on receiving another accessibility event.

## Limits of this evidence

Emulator only, stock Android 16, one screen size, one density. No OEM firmware, no physical hardware, no low-memory or long-running behaviour. Rotation untested. Risks `R-04` and `R-06` are unchanged by this spike.
