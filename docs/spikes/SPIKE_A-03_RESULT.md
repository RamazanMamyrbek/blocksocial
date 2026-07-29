# Spike A-03 — Temporary Bypass Persistence

**Question.** Does an app-specific grant suppress repeated blocks correctly, including after reboot?

**Decision.** `GO`. A grant applies to exactly one application, expires on schedule, survives process death and reboot, and cannot be extended by moving the device clock. One defect was found that has nothing to do with grants and everything to do with whether blocking runs at all — see [Force-stop kills protection](#force-stop-kills-protection-and-android-does-not-bring-it-back).

Executed as plan phase 04.

---

## Environment

| Item | Value |
|---|---|
| Device | emulator `sdk_gphone64_x86_64`, Android 16, API 36 |
| Gradle / AGP / Kotlin | 8.14.4 / 8.13.2 / 2.2.21 |
| JDK | 17.0.7 |
| Grant duration | 60 s, raised to 600 s for the reboot scenario only |

Emulator only, per `docs/TECHNICAL_SPECIFICATION.md` section 9.

## Commit

`a0c3ee428e836accea1b0120d4249a48263eadec` on `phase/04-android-bypass`.

## The time model

A grant that stores only a wall-clock expiry is trivially defeated: move the clock back an hour and the grant appears to have an hour left. A grant that stores only elapsed realtime is defeated by a reboot, because that clock restarts at zero.

So a grant stores **both**, and which one decides depends on whether the device rebooted:

```kotlin
val rebooted = now.elapsedRealtimeMillis < grant.grantedAtElapsedRealtimeMillis
```

Elapsed realtime only ever increases within one boot, so a value below the one recorded at grant time can only mean the clock restarted. No boot id is needed.

| Situation | Clock that decides | Why |
|---|---|---|
| Same boot | elapsed realtime | monotonic; a clock change moves nothing |
| After reboot | wall clock | elapsed realtime is meaningless across boots |
| After reboot, wall clock earlier than the grant itself | neither, grant discarded | the state is not trustworthy, so it is not trusted |

Five explicit evaluations rather than a boolean: `ACTIVE`, `EXPIRED`, `ACTIVE_AFTER_REBOOT`, `EXPIRED_AFTER_REBOOT`, `DISCARDED_CLOCK_MOVED_BEFORE_GRANT`. Only the two active ones suppress a block, and the log names which one applied, so a wrong decision on a device is readable rather than guessed at.

Grants are held in memory for evaluation and written through to storage. Nothing reads from disk inside an accessibility callback, and nothing depends on a timer: expiry is evaluated on each detection, and an evaluation that no longer suppresses deletes the grant on the spot.

## What was verified on the device

| # | Scenario | Result |
|---|---|---|
| 1 | Take a grant, use the app, no repeated block | pass |
| 2 | Switch away and back inside the window | pass |
| 3 | Screen off and on inside the window | pass |
| 4 | Wait for expiry, reopen, block returns | pass |
| 5 | A second restricted app during the window still blocks | pass |
| 6 | Reboot during the window | pass, both directions |
| 7 | Clock moved backwards during the window | pass |
| 8 | Process death during the window | pass |

### One grant, one application

```
package=com.google.android.youtube decision=TARGET_ENTERED
package=com.google.android.youtube grant=null blocking
grant created package=com.google.android.youtube expiresAtWallClock=1785259890998 expiresAtElapsedRealtime=9622659
overlay dismissed package=com.google.android.youtube reason=OPEN_TEMPORARILY
package=com.google.android.youtube decision=IGNORED_ALREADY_FOREGROUND
package=com.google.android.apps.nexuslauncher decision=IGNORED_SYSTEM
package=com.google.android.youtube decision=TARGET_ENTERED
package=com.google.android.youtube suppressedBy=ACTIVE
package=com.android.chrome decision=TARGET_ENTERED
package=com.android.chrome grant=null blocking
```

Leaving to the launcher and returning is suppressed. Chrome, in the same window, is not: the grant did not leak.

### The clock test, which is the one that matters

A grant was taken, then the device clock was moved back one hour.

```
grant created  expiresAtWallClock=1785260283753  expiresAtElapsedRealtime=10015920
clock moved back one hour, wall clock now 1785256626

inside the real 60 s:
  package=com.google.android.youtube suppressedBy=ACTIVE

after the real 60 s, wall clock still an hour behind (1785256707):
  package=com.google.android.youtube grant=EXPIRED blocking
```

At the moment of the second check the wall clock read roughly **1 576 000 ms before** the stored expiry. A wall-clock implementation would have handed out another fifty-nine minutes. The monotonic clock refused.

### Reboot

```
elapsedRealtime before reboot: 10216.76 s
elapsedRealtime after reboot :    34.54 s

connected ... activeGrants=[com.google.android.youtube] wallClock=1785260560555 elapsedRealtime=66570
package=com.google.android.youtube suppressedBy=ACTIVE_AFTER_REBOOT
```

The service came back on its own, reloaded the grant from storage, detected the reset and fell through to the wall clock. Moving the wall clock forward past the stored expiry then produced the other half:

```
clock moved forward 20 minutes to 1785261931 (grant wall expiry was 1785261075)
package=com.google.android.youtube grant=EXPIRED_AFTER_REBOOT blocking
```

### Process death

Killing the process, rather than force-stopping it, is what a low-memory kill looks like:

```
pid=18021 → kill -9
connected ... activeGrants=[com.google.android.youtube] wallClock=1785260084068 elapsedRealtime=9815730
package=com.google.android.youtube suppressedBy=ACTIVE
```

State was recomputed from storage, not remembered. The stored form is both clocks, visible on disk:

```xml
<string name="com.google.android.youtube">1785259923002|1785259983002|9654664|9714664</string>
```

## Automated checks

| Check | Result |
|---|---|
| `./gradlew :a04:assembleDebug` | `BUILD SUCCESSFUL` |
| `./gradlew :a04:test` | `GrantEvaluatorTest tests=12 failures=0`, `GrantRepositoryTest tests=8 failures=0` |
| `./gradlew :a04:lint` | `0 errors, 8 warnings` |
| Merged manifest | `absent: SYSTEM_ALERT_WINDOW`, `absent: QUERY_ALL_PACKAGES` |

The twenty unit tests cover the expiry boundary to the millisecond, a clock moved back by a year, a clock moved forward, both reboot outcomes, a clock set before the grant, two applications holding independent grants, re-granting the same application, purge on load, and reload after process death.

## Defects and findings

### Force-stop kills protection, and Android does not bring it back

Force-stopping BlockSocial during the window left:

```
NO PROCESS
Bound services:{}
```

The accessibility service was gone and the system never rebound it. Reopening a restricted application produced no block at all — not because the grant applied, but because nothing was watching. The grant file was still on disk and correct.

This is not a bypass defect. It is a protection-availability defect, and it is the strongest argument yet for the protection health screen in phase 20: the application must be able to tell the user that blocking is not running, because the device gives no other signal. Phase 21 must decide how the service is brought back.

### An updated app can be enabled but dead

Reinstalling the APK left the service listed under `Bound services` with a live process, yet no events were delivered. Writing `enabled_accessibility_services` with the value it already held did not rebind it; only clearing the setting and writing it again did. An update can therefore leave a user enabled-but-unprotected, with the system settings screen showing the service as on.

Both findings belong to the same question and are carried to phase 20.

### The spike's own observability

The first device runs produced almost empty logs because only the blocking branch logged its decision. That cost several inconclusive runs and is the reason every decision is now logged unconditionally. A production detection service should assume the same: if a decision is not logged, a wrong decision is invisible.

## Limits of this evidence

Emulator only. Stock Android 16. No OEM firmware, so nothing here says how a manufacturer's task killer behaves, which is exactly the case the force-stop finding suggests will matter. The reboot scenario used a ten-minute grant because an emulator reboot takes longer than sixty seconds; the shipped constant is sixty seconds and the product value is five minutes. Grant durations, the copy around them, and what a bypass records as an event are product decisions and are not part of this spike.
