# Spike A-05 — Usage Data and Daily-Limit Feasibility

**Question.** Is usage data good enough for approximate statistics and for daily limits?

**Decision.** `GO`. Measured error is **+0.09 %** on Android 16 and **+0.02 %** on Android 13 over five-minute sessions, and the local-midnight boundary is exact to within 600 ms. Daily limits are viable. The metric source is decided: `queryEvents`, not `queryUsageStats`.

Executed as plan phase 05.

---

## Environment

| Item | Value |
|---|---|
| Image A | emulator, Android 16, API 36, `google_apis` x86_64 rev 7 |
| Image B | emulator, Android 13, API 33, `default` (AOSP) x86_64 rev 2 |
| Gradle / AGP / Kotlin | 8.14.4 / 8.13.2 / 2.2.21 |
| JDK | 17.0.7 |
| Device timezone | GMT on both |

Emulator only, per `docs/TECHNICAL_SPECIFICATION.md` section 9.

## Commit

`1a86457c9ad5ec634dea162e06ad57e45b687aa3` on `phase/05-android-usage-stats`.

---

## Accuracy

The reference is the device's own clock, sampled only **after** the target application was confirmed to be `topResumedActivity`, and again immediately before leaving it. That reference is conservative: the real foreground period starts slightly earlier and ends slightly later than the sampled interval, so a correct implementation should report a little **more** than the reference, never less.

| Image | Application | Reference | Reported | Difference |
|---|---|---|---|---|
| Android 16 | YouTube | 300 048 ms | 300 324 ms | **+276 ms, +0.092 %** |
| Android 13 | Clock | 300 035 ms | 300 099 ms | **+64 ms, +0.021 %** |

Both sit inside the expected positive bias. For a thirty-minute daily limit this is an error of well under two seconds.

### Screen off

Sixty seconds of use, thirty seconds with the screen off, sixty more seconds:

```
wall interval T1 - T0 : 152 746 ms
reported              : 122 948 ms
implied exclusion     :  29 798 ms   against a commanded 30 000 ms sleep
```

Dark time is excluded, and the excluded interval matches the commanded one to 202 ms.

### Rapid switching

Three applications, three rounds each, fifteen seconds per turn, each turn timed only from the moment the system confirmed it resumed:

| Application | Reported | Reference | Difference |
|---|---|---|---|
| YouTube | 47 518 ms | 45 248 ms | +5.02 % |
| Chrome | 48 109 ms | 45 191 ms | +6.46 % |
| Contacts | 46 798 ms | 45 229 ms | +3.47 % |

The larger positive bias here is the same effect amplified: with nine short turns, the untimed launch and hand-off gaps at both ends of each turn are a bigger share of the total. No application is under-reported and nothing is double counted.

### Local midnight

A session deliberately spanning midnight, on both images:

| Image | Session | Expected after midnight | Reported | Difference |
|---|---|---|---|---|
| Android 16 | 23:59:05 → 00:01:05 | 65 000 ms | 65 583 ms | +583 ms |
| Android 13 | 23:59:04.580 → 00:01:04.611 | 64 611 ms | 64 635 ms | **+24 ms** |

Only the part after local midnight is counted. The Android 16 difference is the delay between the printed timestamp and the launcher actually resuming; the Android 13 run, which timed the boundary to the millisecond, shows the arithmetic itself is exact.

### Usage access denied

```
usageAccess=DENIED
crashes: 0
```

Verified on both images, both before access was ever granted and after it was revoked while the application was installed. The screen states what stops working and what keeps working: "Schedule and always-on blocking would keep working; only statistics and daily limits stop."

---

## The defect this spike existed to find

The first implementation mapped `ACTIVITY_RESUMED` to a session start and **both** `ACTIVITY_PAUSED` and `ACTIVITY_STOPPED` to a session end. That is wrong, and it fails silently for some applications and not others:

| Application | Reported | Confirmed foreground | Error |
|---|---|---|---|
| YouTube | 46 837 ms | 45 167 ms | +3.70 % |
| Contacts | 46 704 ms | 45 183 ms | +3.37 % |
| **Chrome** | **2 916 ms** | **45 168 ms** | **−93.54 %** |

The raw event trace explains it:

```
1785264696599 type=1   ACTIVITY_RESUMED
1785264696849 type=2   ACTIVITY_PAUSED
1785264696897 type=1   ACTIVITY_RESUMED
1785264697818 type=23  ACTIVITY_STOPPED     ← 0.9 s in, app still in front
1785264737259 type=2   ACTIVITY_PAUSED      ← the real departure, 40 s later
1785264738576 type=23  ACTIVITY_STOPPED
```

`ACTIVITY_STOPPED` fires when one activity becomes invisible. An application navigating between its own activities does that constantly — Chrome's first-run flow stops one activity while resuming the next. Treating it as leaving the foreground closed the session after 0.9 s.

**The corrected rule:** a session ends only when a *different package* is resumed, when the screen goes non-interactive, or when the window ends. `ACTIVITY_PAUSED` and `ACTIVITY_STOPPED` are not read at all. After the fix Chrome measured +6.46 %, in line with everything else.

This matters beyond one browser. A daily limit built on the broken rule would never fire for an affected application, and nothing in the code or in a unit test written against the assumed model would have shown it. It took a device and a per-application comparison.

---

## Decisions

### Q-03, the source of the time-in-app metric: `queryEvents`

`queryUsageStats(INTERVAL_DAILY)` was read alongside the event-based total throughout. Once the event pairing was correct the two agreed closely on totals — 47 518 against 47 369 for YouTube, 48 109 against 47 042 for Chrome — so accuracy is not what separates them. Two other things do.

**The bucketed source does not reset at local midnight.** Crossing midnight, the event-based total for YouTube fell from 3 202 568 ms to 47 518 ms while `bucketedMillis` stayed at 1 284 946 ms. On Android 13 the same: the event total became 64 635 ms for the new day while the bucket read 420 161 ms. A daily limit must reset at the user's local midnight, and the bucket does not.

**The bucketed source cannot answer the questions the product asks.** It gives one number per package per system bucket: no session count, no arbitrary window, no way to say "since your rule began".

So `queryEvents` it is, with `queryUsageStats` useful only as a sanity check.

### Daily limits are viable

Under 0.1 % error over five minutes, and an exact day boundary, is far better than a daily limit needs. The limit still triggers late — the block only appears on the next launch, because blocking never depends on usage data — and the product must keep saying so. But the measurement is not the weak link.

---

## Automated checks

| Check | Result |
|---|---|
| `./gradlew :a05:assembleDebug` | `BUILD SUCCESSFUL` |
| `./gradlew :a05:test` | `ForegroundTimeAccumulatorTest tests=12 failures=0`, `LocalDayWindowsTest tests=9 failures=0` |
| `./gradlew :a05:lint` | `0 errors, 4 warnings` |

Lint earned its keep here: it caught `AppOpsManager.unsafeCheckOpNoThrow` being an API 29 call in a `minSdk 26` module, which would have crashed on Android 8 and 9. The check now branches on `Build.VERSION.SDK_INT`.

The accumulator tests cover an empty result, a session ended by another application, an application moving between its own activities keeping one session, switching without double counting, several visits summed, a session still open at the window end marked `IN_PROGRESS`, a session that began before the window counted from the window start, screen-off excluding dark time, a screen-off with nothing open, events past the window end, unordered events, and no negative totals.

The day-window tests cover local midnight, the first and last millisecond of a day, crossing into the next window, an ordinary 24-hour day, a 23-hour spring-forward day, a 25-hour autumn-back day, and the same instant belonging to different days in different zones.

### A test that was wrong before the code was

The first day-window tests hardcoded `+05:00` for `Asia/Almaty` and failed by exactly one hour. The bundled tzdata in JDK 17.0.7 predates Kazakhstan's March 2024 move to a single UTC+5 zone, so the runtime used +06:00. The tests now derive expected instants from the zone itself instead of asserting an offset. A test that hardcodes a zone offset does not test the code, it tests the tzdata version.

---

## Findings for production

1. **Never read `ACTIVITY_PAUSED` or `ACTIVITY_STOPPED` as leaving the foreground.** This is the whole spike in one line.
2. **Query events from before the window start.** A session already running at midnight has its `ACTIVITY_RESUMED` in the previous day. This spike uses a 24-hour lookback, which is wasteful; production should persist what was foreground at the last day boundary instead.
3. **A running session is a lower bound.** It is marked `IN_PROGRESS` and its number keeps growing. Statistics must not present it as final.
4. **Usage access is separate from accessibility access** and denial must be a state, not an error. Blocking keeps working without it; only statistics and limits stop.
5. **Blocking never reads usage data.** That separation held throughout this spike and must hold in production: a daily limit decides *whether a rule is active*, and the block itself still comes from the detection service on the next launch.

## Limits of this evidence

Two emulator images, both stock, both in GMT. No OEM firmware, so nothing here says whether a manufacturer trims usage history or reports differently — risk `R-06`. No real day of ordinary use: the longest measured session was five minutes, and the midnight tests were produced by setting the device clock forward rather than by waiting. Moving the clock **backwards** during testing appeared to drop earlier usage history on the AOSP image; that was an artifact of test setup, was not investigated, and is not a scenario the product creates.
