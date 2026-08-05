# BlockSocial Lite — Requirements

**Document:** `lite/docs/REQUIREMENTS.md`
**Applies to:** the Android application with package `com.blocksocial.lite`, whose source is everything under `lite/`.
**Status:** the specification the code is written against. Where the code and this document disagree, one of the two is a defect.

---

## 1. What this application is

A daily time limit for individual applications, and a warning when a limit is spent.

That sentence is the whole product. It is written first because every requirement below is either that sentence made precise, or a boundary drawn to keep something else out.

The user picks an application, says how many minutes a day they want to spend in it, and gets a full-screen question when they open it after the time is gone. The question can be answered either way. Nothing is forced, nothing is locked, and nothing is hidden from them.

This is a standalone application with its own package name, its own storage, its own launcher entry and its own release. It is the only product in this repository. Its scope is decided here.

## 2. Who it is for

Someone who already knows which application is eating their day and roughly how much of it they want back. They are not looking to be coached, scored, or shown a chart. They want a number, an interruption at the right moment, and the freedom to overrule it.

## 3. Definitions

| Term | Meaning here |
|---|---|
| **Catalog** | The fixed list of applications this product can limit. It is shipped with the application and does not grow on the phone. |
| **Limited application** | A catalog application the user has given a daily limit. |
| **Visit** | One continuous stretch with an application in the foreground. It begins when the application comes to the front and ends when it stops being the front application. |
| **Used today** | The sum of a limited application's visits, from the counting start to now. |
| **Counting start** | Local midnight, always. See R-14. |
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
- **R-14.** **A limit covers the whole day from local midnight**, including time spent before the limit was created or last changed. Creating, changing or removing a limit never resets the count. This is Digital Wellbeing's behaviour and it is deliberate: see section 7.
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

- **R-23.** The application needs accessibility access to notice an application coming to the front, usage access to measure time, and exemption from battery optimisation so the phone does not freeze it. It needs nothing else, and requests nothing else. It shows **at most one notification**, and never a second: a silent, ongoing line saying limits are running, which the user can switch off. See R-43 and R-44.
- **R-24.** When any of them is missing, the main screen says so at the top and offers a route to the relevant system settings screen.
- **R-25.** Each permission is explained in terms of what it enables, what is read, and what is never read, before the user is sent to grant it.
- **R-26.** Revoking usage access takes effect on the very next launch of a limited application, not the one after it.
- **R-38.** The accessibility row reports whether the service is **answering**, not whether its switch is on. A service that Android lists as enabled but that has been stopped by the system reads as *switched on, but not running*, and says what to do about it. Reporting a dead service as working is the worst failure this product can have, because the user stops watching for the thing that is broken.
- **R-39.** If the warning cannot be drawn over the other application, that failure is surfaced on the permissions screen rather than swallowed.
- **R-43.** The application runs a **foreground service** for as long as any limit exists, with an ongoing notification on a channel that asks for **minimum importance**. What that buys, measured on an Android 16 emulator, is no sound, no heads-up, and one collapsed line at the bottom of the shade under *Silent*. What it does not buy is the status-bar icon: Android raises any channel a foreground service posts on to low importance — the channel dump reads `mOriginalImp=1, mImportance=2, mImportanceExplanation=system` — and the icon stays. No application can remove it, and the product does not claim otherwise. This is not a feature and nobody wants it; it is the only mechanism Android offers for keeping a process out of the state where the phone is free to freeze it, and Android will not let a foreground service run without one. The owner's phone proved the need directly: with BlockSocial Lite held on screen in split-screen mode the limits worked, and the moment it was backgrounded the accessibility service stopped being sent events. Battery exemption (R-42) reduced the problem but did not remove it. If the last limit is removed, the service and the notification go away.
- **R-44.** That service can be **switched off by the user**, from the same screen that explains the three permissions. It is **on by default**, because on the firmware this product was reported broken on it is the difference between limits working and limits silently not working. Switching it off is honoured immediately, takes the notification away with it, and the screen states plainly what has been given up rather than presenting the choice as free.
- **R-42.** Exemption from battery optimisation is a **requirement, not a suggestion**, and is reported next to the other two. Without it Xiaomi firmware closes the application after ten minutes in the background: the accessibility switch still reads on, events stop arriving, and limits stop stopping anyone. Measured on a Poco X7 Pro, HyperOS, Android 15 — see the README.
- **R-40.** The application can show, on the phone itself, what it is currently seeing: whether the service is connected, when it last saw a screen change, whether usage access is granted, the minutes it has measured for each application today, and the last thirty foreground changes with the decision taken for each. A limit that does not fire must be explainable by its owner without a cable, a laptop or a debug build.
- **R-41.** That page names only catalog applications. Anything else that comes to the front is recorded as an outcome with no name attached, so the page cannot become an inventory of what is installed.

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
| Any notification other than the one in R-43 | Nothing else here has anything to tell you when you are not looking. |
| Preventing uninstall, hiding the application, resisting removal | R-33. The user is in charge. An attention tool that fights its owner has changed sides. |

Nothing here is a promise that these will arrive later. They are refused, and reopening one means reopening this document.

## 7. Deliberate trade-offs

Places where a reasonable person would choose differently, recorded with the reasoning so the choice can be revisited rather than rediscovered.

**A limit counts the whole day, and touching the limit does not reset it (R-14).** The opposite was tried: the count started from the moment the limit was saved. It was built to answer a real complaint — setting a five-minute limit at noon and being stopped on the spot feels like the product inventing a reason. But it made the counter reset every time the number was edited, which turns a daily limit into a stopwatch you can restart by opening the settings screen, and a limit you can reset by tapping twice is not a limit. Digital Wellbeing counts the whole day, the owner asked for Digital Wellbeing's behaviour, and that is what this now does. The original complaint is answered differently: the measurement was simply wrong at the time — see the defects in the README — and the warning now states the figure that produced it, so an immediate stop is checkable rather than arbitrary.

**Entering an application is decided by the package that raised the window, without checking that the window is a recognisable activity.** The stricter check was tried and it cost a real, reported failure: on some firmware the return into an application reports a window whose class does not resolve to an activity, and the warning never appeared. The looser rule can in principle warn about an application appearing in a floating or picture-in-picture window while the user is looking at something else. A warning at a slightly wrong moment is a nuisance; a limit that silently stops enforcing is the product not working. The nuisance was chosen.

**The measurement is read fresh at the moment of the decision, not from a cache.** It was cached at first, refreshed asynchronously when the foreground left a limited application. That saved a system query on a rare code path and cost a whole class of missed warnings: cross the limit while inside the application, leave, and come back before the refresh lands, and the decision is taken against a reading from before the crossing. The query runs only when a visit to a limited application begins — once per launch, not once per window change — so paying for it is cheap and being wrong about it is not.

## 8. How each requirement is checked

| Requirements | Checked by |
|---|---|
| R-10 to R-14 | `ForegroundSessionsTest` — including a visit that moves between screens, a visit interrupted by a shutdown, and a limit set part-way through the day |
| R-15, R-16, R-20, R-21, R-26 | `DetectionPipelineTest` — including the warning returning on every fresh entry and the measurement being read only when a visit begins — and `UsageTodayTest` for the difference between no measurement and a measured zero |
| R-05, R-06 | `DailyLimitTest` |
| R-07, R-08 | `TypedMinutesTest` |
| R-38, R-39 | `ServiceStateTest` |
| R-40, R-41 | `DecisionLogTest` — the last thirty changes, newest first, and a name kept only for catalog applications — and `anApplicationOutsideTheCatalogIsNeverGivenAName` in `DetectionPipelineTest` |
| R-43, R-44 | `GuardDecisionTest` — the guard runs only while a limit exists and the switch is on, and the switch starts on |
| R-01 to R-04, R-17 to R-19, R-22 to R-25 | Driven over `adb` on an emulator; the current results are in `../README.md` |
| R-29 to R-33 | The manifest, `accessibility_service_config.xml`, and the absence of any network dependency |
| R-34, R-35 | String resources in `values/` and `values-ru/`; font scaling by inspection |

Most of this has been verified on an emulator only. What a physical phone has actually shown — a Poco X7 Pro on HyperOS, Android 15 — is listed in `../README.md`, and it is what R-42, R-43 and R-38 exist for. Manufacturer firmware alters both mechanisms this product stands on — how accessibility services are kept alive, and what the usage-event stream contains — so an emulator result is evidence about Android, not about that phone.
