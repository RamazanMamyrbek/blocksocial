# Android Validation Gate

**Verdict: `GO` for Android**, with three constraints that are now mandatory rather than advisory, and one open product question that blocks a single phase rather than the track.

This gate judges Android only. iOS has no evidence yet and no verdict here; phase 34 makes that one on its own evidence.

Produced in plan phase 08 from checks `A-01` through `A-06`.

---

## 1. What the spikes decided

| Check | Question | Decision | Evidence |
|---|---|---|---|
| `A-01` | Does the accessibility service detect a transition into a target application reliably on `targetSdk 36`? | `GO` | `docs/spikes/SPIKE_A-01_RESULT.md` |
| `A-02` | Does an interactive full-screen overlay work without `SYSTEM_ALERT_WINDOW`, and can it always be removed? | `GO` | `docs/spikes/SPIKE_A-02_RESULT.md` |
| `A-03` | Does an app-specific grant suppress repeated blocks correctly, including after reboot? | `GO` | `docs/spikes/SPIKE_A-03_RESULT.md` |
| `A-04` | Does the curated catalog work without `QUERY_ALL_PACKAGES`? | `GO` | `docs/spikes/SPIKE_A-04_RESULT.md` |
| `A-05` | Is usage data good enough for statistics and daily limits? | `GO` | `docs/spikes/SPIKE_A-05_RESULT.md` |
| `A-06` | Is the Play policy package consistent with what the code actually does? | complete | `docs/store/play/README.md` |

Five `GO` decisions, no `CHANGE`, no `STOP`. That is not the same as five clean runs: two of the five found defects severe enough that a naive implementation would have shipped broken, and those are now constraints rather than lessons.

## 2. The questions this gate had to answer

### Is the Android overlay stable on API 36?

**Yes, on an emulator.** A full-screen `TYPE_ACCESSIBILITY_OVERLAY` appears over a third-party application, accepts touch, and could not be made to stick in any tested case. Ten consecutive triggers left zero windows behind. Force-stopping the process removed the overlay and left the device usable. The window reports as `ty=ACCESSIBILITY_OVERLAY`, layer 2 above the application, and no `SYSTEM_ALERT_WINDOW` appears in the merged manifest.

Compose works inside it without a fallback to Views, but only because the overlay host supplies its own lifecycle, view-model store and saved-state registry. Evidence: `docs/spikes/SPIKE_A-02_RESULT.md`.

Two scenarios have no evidence either way: rotation, because the AVD does not rotate at all even with no overlay present, and screen-reader operation, because the overlay's node tree could not be retrieved with `adb` tooling. Both moved to phases 13 and 21.

### Is the Accessibility use case defensible under Play policy?

**The package is complete and consistent; the decision is Google's.** Six documents in `docs/store/play/`, cross-read against the source at commit `5bf53a3`. The service reads four fields of one event type. The merged manifest declares no permissions at all, no `INTERNET`, no `QUERY_ALL_PACKAGES`. `canRetrieveWindowContent` is `false`, so the privacy claim is enforced by the system rather than by discipline. `isAccessibilityTool` is `false`, which is the honest declaration and the one that invites review.

No amount of documentation makes a review outcome certain. Risk `R-01` stays open at full weight.

### Does Android work without `QUERY_ALL_PACKAGES`?

**Yes, and it is verified rather than assumed.** Ten catalog entries resolve through twelve targeted `<package>` declarations that match the twelve package names in `shared/supported-app-catalog/catalog.json` exactly. An installed application outside the catalog is invisible to the code, which has no path that enumerates anything. Uninstalling a catalog application moves it to a normal explained state and back again without error.

Evidence: `docs/spikes/SPIKE_A-04_RESULT.md`. Risk `R-05` is closed.

### Is usage measurement accurate enough for daily limits?

**Yes, by a wide margin.** Measured error against a device-clock reference was **+0.09 %** on Android 16 and **+0.02 %** on Android 13 over five-minute sessions. Screen-off time is excluded to within 202 ms of the commanded interval. The local-midnight boundary is exact: a session spanning midnight contributed 64 635 ms of an expected 64 611 ms to the new day.

Evidence: `docs/spikes/SPIKE_A-05_RESULT.md`.

### Do temporary grants survive process death, reboot and clock manipulation?

**Yes, all three.** A grant stores its expiry on two clocks. Within one boot the monotonic clock decides, so with the device clock moved back an hour — reading fifty-eight minutes before the stored expiry — the grant still expired after sixty real seconds. A reboot is recognised by the monotonic counter running backwards, and only then does the wall clock take over. Killing the process restored the grant from storage on reconnect.

Evidence: `docs/spikes/SPIKE_A-03_RESULT.md`.

## 3. The three constraints that are now mandatory

These are not recommendations. Each was found by a device run, and without it the product ships broken in a way no unit test written against the assumed model would have caught.

**One. A window event counts only when `packageName` plus `className` resolve to a real activity.** `TYPE_WINDOW_STATE_CHANGED` with `packageName` alone is not a foreground signal: a backgrounding application keeps emitting events, and pressing home then returning produced no detection at all because the state machine believed the target was still in front. The spurious events carry a plain view class rather than an activity class. Found in `A-01`.

**Two. An entry is a change of the foreground package, not a time-based debounce.** A single cold launch produced two detections three seconds apart, and a deep link produced three, because an application emits several window events while starting and they are spread wider than any sane debounce window. Found in `A-01`.

**Three. `ACTIVITY_PAUSED` and `ACTIVITY_STOPPED` are never read as leaving the foreground.** They fire when one activity becomes invisible, which happens constantly while an application navigates inside itself. Chrome reported 2.9 s of a confirmed 45.2 s because its first-run flow stops one activity while resuming another. A daily limit built on that rule would never fire for an affected application. Found in `A-05`.

All three are now written into `docs/ARCHITECTURE.md`, which previously described the mechanism the spikes replaced.

## 4. What emulator-only evidence does not cover

Every figure above comes from an emulator running stock Android. No physical device was used, by the owner's decision, recorded in `docs/TECHNICAL_SPECIFICATION.md` section 9.

| Not covered | Who carries it |
|---|---|
| How OEM firmware kills background services and restricts autostart | the protection health screen in phase 19, and the Android beta in phase 24 |
| Whether accessibility events are delivered the same way on OEM firmware | phase 24; risk `R-06` |
| Real detection latency on physical hardware | phase 24. The 119 ms mean is a virtualised figure |
| Overlay behaviour under rotation | phase 13, on an AVD configured to rotate |
| Screen-reader operation of the block screen | phase 21 |
| Whether ten catalog entries are enough | phase 24 feedback |

The 2 % measurement tolerance in `shared/fixtures/daily-limit-cases.json` is set twenty times wider than the measured error precisely because the measured error comes from an emulator.

## 5. A defect the spikes found that has nothing to do with blocking

Force-stopping BlockSocial kills the accessibility service and **Android does not rebind it**: `Bound services:{}`, no process, and reopening a restricted application produces no block at all. Separately, reinstalling the application left the service listed as bound with a live process and no event delivery; only clearing and rewriting the accessibility setting revived it.

In both cases blocking stops silently while the system settings screen still shows the service as enabled. This is the strongest argument in the whole validation for the protection health screen, and it is now risk `R-07`. Found in `A-03`, assigned to phases 19 and 20.

## 6. The Apple entitlement, as information only

**Not requested. Not granted.** Phase 27 files the request and it has not run, because iOS was deferred as a whole. Nothing in this gate depends on it, and no document may treat the absence of a refusal as an approval. Risk `R-02` is unchanged.

## 7. Open questions

`Q-03` was answered in phase 05 and is recorded in `docs/PRODUCT.md`.

**`Q-02` is answered by this gate.** The Android block-screen baseline is direction **1b, "Quiet"**. Spike `A-02` built it with the documented tokens and it survived the largest font scale without truncation and both themes. Direction 1c stays a candidate for a later pass; it was always marked provisional on `A-02`, and `A-02` has now returned.

**`Q-01` remains open** and no Android spike touches it. Whether any bypass ends the streak, or a configured number is allowed per day, is a product decision. It blocks phase 18 and nothing before it.

`Q-04` and `Q-05` are untouched: one is iOS, one is not MVP-blocking.

## 8. Re-scoping phases 09 to 26

What the spikes changed about work that has not started yet.

| Phase | Change |
|---|---|
| 09 skeleton | the toolchain is proven: Gradle 8.14.4, AGP 8.13.2, Kotlin 2.2.21, JDK 17, `compileSdk` 36. AGP resolves Build Tools 35.0.0 by default, so pin it if the version matters |
| 11 domain | implements the frozen fixtures from phase 07 rather than inventing rules |
| 12 detection | must carry constraints one and two, must log every decision unconditionally, and must decide what happens when the user unlocks into an already-open restricted application, which currently produces no detection |
| 13 block screen | direction 1b, plus the three items `A-02` left open: the back key, rotation, and whether the notification shade stays reachable |
| 15 app selection | must stop `<queries>` and the catalog drifting apart, by generation or by a failing test |
| 17 daily limits | must carry constraint three, and must query events from before the window start so a session already running at midnight is counted |
| 19 protection health | must detect the two silent-failure modes in section 5, not only a revoked permission |
| 21 accessibility | owns the screen-reader confirmation `A-02` could not produce, and must keep an accessibility title on the overlay window |
| 23 Play submission | must close the ungated-logging gap and re-run the data inventory against production sources |
| 24 Android beta | inherits every row of section 4 |

No phase is added, removed or reordered by this gate.

## 9. What this gate does not claim

It does not claim the product works on any physical Android device. It does not claim Google will approve the use case. It does not claim anything about iOS. And it does not claim that five `GO` decisions mean the mechanisms were straightforward: three of them are `GO` only because a device run contradicted the first implementation and the design changed.
