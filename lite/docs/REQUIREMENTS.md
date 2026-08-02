# BlockSocial Lite — Requirements

**Document:** `lite/docs/REQUIREMENTS.md`
**Applies to:** the Android application with package `com.blocksocial.lite`, whose source is everything under `lite/`.
**Status:** the specification the code is written against. Where the code and this document disagree, one of the two is a defect.

---

## 1. What this application is

A daily time limit for individual applications, and a warning when a limit is spent.

That sentence is the whole product. It is written first because every requirement below is either that sentence made precise, or a boundary drawn to keep something else out.

The user picks an application, says how many minutes a day they want to spend in it, and gets a full-screen question when they open it after the time is gone. The question can be answered either way. Nothing is forced, nothing is locked, and nothing is hidden from them.

This is a standalone application with its own package name, its own storage, its own launcher entry and its own release. It shares a git repository with another application, and reuses source from it in the way any two projects share a library, but it is not a configuration, variant, edition or subset of that one, and it has no obligation to stay aligned with it. Its scope is decided here.

## 2. Who it is for

Someone who already knows which application is eating their day and roughly how much of it they want back. They are not looking to be coached, scored, or shown a chart. They want a number, an interruption at the right moment, and the freedom to overrule it.

## 3. Definitions

| Term | Meaning here |
|---|---|
| **Catalog** | The fixed list of applications this product can limit. It is shipped with the application and does not grow on the phone. |
| **Limited application** | A catalog application the user has given a daily limit. |
| **Visit** | One continuous stretch with an application in the foreground. It begins when the application comes to the front and ends when it stops being the front application. |
| **Used today** | The sum of a limited application's visits, from the counting start to now. |
| **Counting start** | Normally local midnight. On the day a limit is created or changed, the moment it was saved. See R-14. |
| **Limit spent** | Used today is greater than or equal to the limit. |
| **Warning** | The full-screen question shown when a limited application is opened with its limit spent. |

## 4. Functional requirements

### Choosing what to limit

- **R-01.** The main screen lists exactly the catalog applications that are installed on this phone. An application in the catalog but not installed is not shown; an application installed but not in the catalog is not shown.
- **R-02.** The list is split into applications that have a limit and applications that do not. Both groups are visible without navigating.
- **R-03.** Every row shows the application's name. For an application with a limit, it also shows minutes used and the limit as one line, plus a bar showing the proportion.
- **R-04.** Tapping a row opens that application's own screen.

### The application screen

- **R-05.** It shows two figures and nothing else that counts as a statistic: **used today** and **left today**.
- **R-06.** When the limit is spent, *left today* reads as nothing left rather than as a negative number or a zero that could be mistaken for a measurement.
- **R-07.** The limit is entered by **typing a number of minutes**. It is not a stepper, a slider or a picker.
- **R-08.** The accepted range is 1 to 720 minutes. A number outside it, or anything that is not a number, is refused with a message naming the range, and the save action stays unavailable. Nothing is silently rounded or clamped.
- **R-09.** The screen offers to remove the limit when there is one.

### Measuring

- **R-10.** Time is measured from Android's usage-event stream, not estimated, not inferred from how long a warning was on screen.
- **R-11.** A visit continues across screens *within* the same application. Moving from a list to a detail screen to a full-screen player is one visit, not three, and does not stop the count.
- **R-12.** A visit ends when another application comes to the front, when the screen turns off, or when the screen the user was actually on leaves the foreground.
- **R-13.** No minute earlier than the device's last boot is ever counted. Time with the phone switched off is not time in an application.
- **R-14.** **Saving a limit starts the count from that moment.** Time spent in the application earlier the same day does not carry into a limit that did not exist yet. From the following local midnight the limit counts the whole day as normal. Changing an existing limit restarts the count the same way.
- **R-15.** Without usage access there is no measurement. The product says so where a figure would otherwise be, and no limit is treated as spent.

### Warning

- **R-16.** Opening a limited application whose limit is spent covers the screen with a warning before the user has meaningfully used it.
- **R-17.** The warning states the measurement that produced it: minutes used, the application, and the limit. A warning that only asserts a conclusion is a defect.
- **R-18.** The warning offers exactly two answers: leave the application, or carry on.
- **R-19.** Leaving returns the user to the home screen.
- **R-20.** Carrying on dismisses the warning for this visit only. **It buys no grace period.** The next visit is warned about again, and so is the one after it, for as long as the limit stays spent.
- **R-21.** The warning is not shown twice within one visit.
- **R-22.** The system's own back gesture dismisses the warning the same way carrying on does. The user is never trapped.

### Permissions

- **R-23.** The application needs accessibility access to notice an application coming to the front, and usage access to measure time. It needs nothing else, and requests nothing else.
- **R-24.** When either is missing, the main screen says so at the top and offers a route to the relevant system settings screen.
- **R-25.** Each permission is explained in terms of what it enables, what is read, and what is never read, before the user is sent to grant it.
- **R-26.** Revoking usage access takes effect on the very next launch of a limited application, not the one after it.

### Storage

- **R-27.** The only thing stored is, per application, the limit in minutes and the moment it was set.
- **R-28.** **No history is kept.** Not of warnings shown, not of answers given, not of time measured. Today's figures are recomputed from the system's own usage data every time they are needed, and yesterday's are simply gone.

## 5. Non-functional requirements

- **R-29.** Everything works offline. The application has no network permission, and no data leaves the phone.
- **R-30.** Screen content is never read. `canRetrieveWindowContent` is `false`, so the system enforces this rather than the code promising it.
- **R-31.** Messages, keystrokes, passwords and notification contents are never read or stored, in any circumstances.
- **R-32.** No action is ever automated inside another application.
- **R-33.** The application can be uninstalled by the ordinary means at any time, and nothing resists it.
- **R-34.** The interface is available in English and Russian. Every user-visible string is a resource.
- **R-35.** The layout survives the largest system font scale without losing a control off-screen.
- **R-36.** `minSdk 26`, `targetSdk 36`, phone form factor.
- **R-37.** A warning appears within roughly a second of the application coming to the front.

## 6. Out of scope

Listed because each one was considered and refused, not because nobody thought of it.

| Not in this product | Why |
|---|---|
| History of warnings and answers | R-28. Nothing in the product needs it, so keeping it would be collecting for its own sake. |
| Statistics, streaks, refusal rates, "hours saved" | The two figures on the application screen are the entire information architecture. Everything else is a scoreboard, and a scoreboard is a different product. |
| Schedules, always-on blocking, focus sessions | This is a daily limit. Another rule type is another product. |
| A timed pass after carrying on | R-20. A limit that stops asking after one refusal is not a limit. |
| Blocking parts of an application — feeds, reels, tabs | Cannot be done without reading screen content, which R-30 forbids outright. |
| Accounts, cloud sync, a backend | R-29. |
| Website or network filtering, VPN, DNS | Different mechanism, different privacy story, different product. |
| Controlling another person's phone | Not what this is for. |
| Advertising, analytics, monetisation | R-29 leaves nowhere for them to send anything. |
| Preventing uninstall, hiding the application, resisting removal | R-33. The user is in charge. An attention tool that fights its owner has changed sides. |

Nothing here is a promise that these will arrive later. They are refused, and reopening one means reopening this document.

## 7. Deliberate trade-offs

Two places where a reasonable person would choose differently, recorded with the reasoning so the choice can be revisited rather than rediscovered.

**Entering an application is decided by the package that raised the window, without checking that the window is a recognisable activity.** The stricter check was tried and it cost a real, reported failure: on some firmware the return into an application reports a window whose class does not resolve to an activity, and the warning never appeared. The looser rule can in principle warn about an application appearing in a floating or picture-in-picture window while the user is looking at something else. A warning at a slightly wrong moment is a nuisance; a limit that silently stops enforcing is the product not working. The nuisance was chosen.

**The measurement is cached and refreshed when the foreground leaves a limited application, rather than read fresh on every window change.** A figure can therefore be a few seconds stale. Reading usage events on every window change would put a system query on the path that has to produce a warning within a second. The one thing not allowed to be stale is permission: that is re-checked at the moment of the decision, per R-26.

## 8. How each requirement is checked

| Requirements | Checked by |
|---|---|
| R-10 to R-14 | `ForegroundSessionsTest` — including a visit that moves between screens, a visit interrupted by a shutdown, and a limit set part-way through the day |
| R-15, R-16, R-20, R-21, R-26 | `DetectionPipelineTest` — including the warning returning on every fresh entry and the measurement being read only when a visit begins |
| R-05, R-06 | `DailyLimitTest` |
| R-07, R-08 | `TypedMinutesTest` |
| R-01 to R-04, R-17 to R-19, R-22 to R-25 | Driven over `adb` on an emulator; the current results are in `../README.md` |
| R-29 to R-33 | The manifest, `accessibility_service_config.xml`, and the absence of any network dependency |
| R-34, R-35 | String resources in `values/` and `values-ru/`; font scaling by inspection |

Nothing in this document has been verified on a physical phone. Manufacturer firmware alters both mechanisms this product stands on — how accessibility services are kept alive, and what the usage-event stream contains — and an emulator cannot show that.
