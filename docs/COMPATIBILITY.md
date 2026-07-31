# BlockSocial — Android Compatibility

**Document:** `COMPATIBILITY.md`
**Covers:** the Android application only. iOS has its own verification path.
**Last updated by:** phase 22.

---

## 1. What this document is for

To say precisely what has been verified, on what, and — at greater length — what has not. A reader should be able to finish it and know exactly which claims are backed by evidence and which are open questions carried into the beta.

## 2. The one thing to read if you read nothing else

**No physical Android device has been used at any point in this project.** Everything below was verified on emulator images running stock Android. An emulator cannot show how a manufacturer's firmware terminates background services, restricts autostart, or delivers accessibility events, and those are exactly the behaviours BlockSocial depends on. Nothing in this document is evidence about a Samsung, Xiaomi, Huawei, Oppo, Vivo or OnePlus phone.

That is a deliberate choice recorded in `docs/TECHNICAL_SPECIFICATION.md` section 9, not an oversight. The gap is carried by two things: the protection health screen, which must make a dead service visible rather than let it fail silently, and the Android beta in phase 24, which must recruit testers on the hardware named in section 6.

## 3. Support window

| | |
|---|---|
| `minSdk` | 26 — Android 8.0 |
| `targetSdk` | 36 — Android 16 |
| Form factor verified | phone only |
| Languages verified | English and Russian |

`minSdk 26` is the floor the code compiles against. It is **not** a level that has been run. See section 5.

## 4. What was verified, and on what

Two emulator images, both stock Android, both run from the same machine over `adb`.

| Image | API | Android | Kind |
|---|---|---|---|
| `blocksocial_a01` | 36 | 16 | Google APIs, x86_64 |
| `blocksocial_a33` | 33 | 13 | default, x86_64 |

**A third image, Android 10 (API 29), is missing.** The plan called for it. The system image would not install: `sdkmanager` failed twice with `Unexpected end of ZLIB input stream` partway through unzipping `system.img`. That is a download failure in this environment, not a finding about the product, and it leaves everything from `minSdk 26` to API 32 compiled but unexecuted. Section 5.2 says what that costs.

### Automated suites

`gradlew connectedDebugAndroidTest` runs against every attached device, so one invocation with both images booted produced the matrix.

| | API 33 | API 36 |
|---|---|---|
| Instrumented tests | 116 | 116 |
| Failures | 0 | 0 |

Unit tests and lint are platform-independent and run once: **400 tests, 0 failures, lint clean on every module.** `gradlew build` completes clean.

### Checked by hand, driven over `adb`

| Check | API 33 | API 36 |
|---|---|---|
| Onboarding from a fresh install, skip, finish setup | yes | yes |
| Application shell, tabs, back stack | yes | yes |
| Protection health reports each requirement and its repair action | yes | yes |
| Accessibility service binds and reports "Blocking is running" | yes | yes |
| Accessibility events delivered and classified, ~100 ms | yes | yes |
| `POST_NOTIFICATIONS` requested at runtime, and only once protection has failed | yes | yes |
| Application selection, rule creation, block screen, decision recorded in history | no target app on this image | yes |
| Reboot recovery without opening the application | not run | yes |
| Process death and rebind | not run | yes |
| Time-zone change at runtime | not run | yes |
| Twenty rapid switches with no overlay loop | not run | yes |
| TalkBack focuses the block overlay and completes the decision | not run | yes |
| Russian at the largest font scale | not run | yes |
| Greyscale legibility of protection health | not run | yes |

The API 33 image is a `default` system image with no Google applications, so none of the ten catalog applications is installed on it. Everything that needs a real restricted application to launch — the block screen itself and everything downstream of it — could only be exercised on API 36, where YouTube is present. What API 33 does show is that the detection service binds, receives window events and classifies them at the same latency, which is the mechanism those scenarios sit on top of.

### Defects found

Phases 19, 20 and 21 each found defects by running the product, and each fixed one with a regression test; they are recorded in `docs/PLAN.md` under their own phases. **This phase found none.** Both images behaved identically wherever both were exercised, and no defect was opened, so the list of open critical or major defects is empty.

## 5. What was not verified

Grouped by why, because the reasons differ and so do the responses.

### 5.1 No physical hardware, ever

Covered in section 2 and section 6. This is the largest gap and the only one with a plan attached.

### 5.2 API levels between the floor and the lowest tested image

`minSdk` is 26. The lowest image run is stated in section 4. Levels below it compile and are within the declared support window, but they have not been executed. Two platform behaviours the application depends on changed in that range and were read from documentation rather than observed:

- `POST_NOTIFICATIONS` is a runtime permission from API 33 only. Below it the application takes the branch that opens notification settings instead, and that branch has been executed only on images at or above the lowest tested level unless section 4 says otherwise.
- Accessibility service rebinding after a force-stop, and whether `enabled_accessibility_services` is cleared, is platform behaviour that has been observed on Android 16 and is not assumed to hold below it.

### 5.3 Things an emulator cannot measure honestly

- **Battery.** Phase 20 read `dumpsys batterystats`, `dumpsys power` and `dumpsys alarm`. The useful findings are structural: no wakelock is held by BlockSocial code and no alarm is scheduled. The CPU figures disagree between the per-process and per-uid counters, which is ordinary on an emulator, so **no battery percentage is quoted anywhere in this project**. A real figure needs real hardware over a real day and belongs to the beta.
- **Thermal and memory pressure** as a real phone experiences them.
- **Real-world event timing.** Emulator window transitions are not phone window transitions.

### 5.4 Screen shapes and inputs

Tablets, foldables, desktop windowing, external keyboards, and any display other than the single phone-sized emulator screen. The application has no tablet layout and does not claim one.

### 5.5 Store behaviour

Nothing here says anything about Google Play review. That is phase 23.

## 6. The list for the beta

These are the questions an emulator cannot answer. A tester with the right hardware can answer each of them in a few minutes, and each is written so the answer is yes or no rather than an impression.

Recruit at minimum: **Samsung One UI**, **Xiaomi HyperOS or MIUI**. Desirable: Huawei EMUI, Oppo ColorOS, Vivo Funtouch, OnePlus OxygenOS.

| # | What the tester does | What to report |
|---|---|---|
| 1 | Turn on blocking, then leave the phone untouched and locked overnight. In the morning open a restricted application. | Did the pause screen appear? If not, what does the Protection screen say? |
| 2 | Turn on blocking, then reboot the phone. Without opening BlockSocial, open a restricted application. | Did the pause screen appear? |
| 3 | Open Protection and read the accessibility row after a full day of normal use. | Did it ever say anything other than "Working"? Which wording, and when? |
| 4 | Find the manufacturer's battery or autostart settings and check what BlockSocial is set to. | The exact screen name and the exact setting value, before any change. |
| 5 | Set that battery setting to unrestricted, then repeat check 1. | Did the outcome change? |
| 6 | Open a restricted application, meet the pause, and use the phone's own gesture navigation to leave and return several times. | Did the pause appear more than once for one launch, or fail to appear on a genuine relaunch? |
| 7 | Turn on TalkBack and complete one decision on the pause screen using TalkBack alone. | Could the decision be completed? What did TalkBack read out? |
| 8 | Check the phone's battery screen after a day. | Where does BlockSocial appear, and with what figure? |
| 9 | Update BlockSocial from the store while blocking is on, then open a restricted application. | Did blocking still work, and what did Protection say? |
| 10 | Force-stop BlockSocial from the phone's application settings, then open Protection. | Does it say blocking stopped, and does the repair action get it running again? |

Checks 1 to 5 carry risk `R-06`, check 6 carries `R-04`, and checks 9 and 10 carry `R-07`. Nothing in this project has evidence for any of them on manufacturer firmware.

The wording of the on-screen OEM guidance already reflects that: `health_oem_body` tells the user that some manufacturers stop background services and how to allow BlockSocial to run, and it says outright that the guidance is written from vendor documentation and **has not been observed on a device by us**. That sentence must survive translation and review; it is the honest label on an unverified claim.

## 7. How to reproduce any of this

Every check in section 4 is driven from a host machine over `adb`; none requires holding a phone.

```
gradlew check                          # unit tests and lint on every module
gradlew connectedDebugAndroidTest      # instrumented tests on every attached emulator
```

`connectedDebugAndroidTest` runs against **all** attached devices, so booting two emulator images and running it once produces the matrix in one pass. That is how section 4 was produced.
