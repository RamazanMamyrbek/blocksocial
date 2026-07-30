# BlockSocial — Development Plan

Phase-by-phase plan from technical validation to release. Read `AGENTS.md` before executing any phase.

**Android ships first, all the way to the Play Store. iOS starts afterwards, beginning with the Apple entitlement request.**

---

## 1. How This Plan Works

One phase is one AI session. The user starts a phase with a single instruction:

```text
Начни Phase 12
```

The agent then executes the whole phase without further prompting: creates the branch, does the work, runs every automated check listed for that phase, fills in every checkbox in this file as it goes, and finishes with a report. The user fills in nothing. After the report the user verifies the result by hand using the manual scenarios, and only then approves the merge.

If a phase cannot be completed, the agent stops, reports exactly where it stopped and why, and leaves the unfinished checkboxes unchecked. A partially finished phase is never reported as done.

### Git workflow

| Branch | Purpose |
|---|---|
| `main` | stable release states only; never receives direct work |
| `dev` | integration branch; every finished phase merges here |
| `phase/<number>-<short-name>` | one branch per phase, created from `dev` |

```text
main ────────────●───────────────────●──────>   releases only
                 ↑                   ↑
dev  ──●───●───●─┴─●───●───●───●───●─┴──────>   integration
       ↑   ↑   ↑   ↑   ↑
    phase/01 … phase/NN                          one per phase
```

Rules:

- Branch from the current `dev`, never from `main`.
- One phase per branch. Never mix two phases in one branch.
- Commit in small, meaningful steps within the phase.
- Merge into `dev` only when that phase's merge conditions are all true.
- `main` receives a merge only in the release phases, 26 and 49.
- Release tags name their platform: `android-v1.0.0`, `ios-v1.0.0`. One repository releases twice.
- Never rewrite published history.

### Universal Definition of Done

These apply to every phase in addition to its own list.

- Every task checkbox in the phase is checked, or the phase is reported unfinished.
- Every automated check listed for the phase was actually run, and its real output is quoted in the report.
- No unrelated file was modified.
- Documentation listed in the phase was updated.
- Nothing unverified is described as verified. Precise wording is required: "implemented but not built on macOS", "unit tests pass, not yet run on a device", "awaiting teammate report".
- The report lists changed files, commands run, results, and remaining risks.

### Roles

| Role | Does |
|---|---|
| Agent | writes code and documents, runs everything it can run including every Android scenario on the emulator, fills checklists, reports |
| Owner | Windows machine, approves merges, reviews reports and evidence |
| Teammate | macOS and a real iPhone, builds and verifies every iOS phase, returns evidence |

The agent cannot build or run iOS. For every iOS phase the agent prepares the handoff document and the teammate's test checklist itself; the teammate executes it and returns results. The user does not write these checklists.

### Android test environment

**Android verification uses an emulator. No physical Android device is used in any phase.**

The agent drives everything from the host over `adb` — install, enable the accessibility service through `settings put secure`, act with `input keyevent` and `monkey`, read `logcat`. The owner is never asked to operate a phone.

This applies to Android only. iOS is verified by the teammate on a real iPhone, and every mention of a real device in an iOS phase means exactly that.

The cost is real and is not written off: an emulator runs stock Android and cannot expose how Samsung or Xiaomi firmware kills background services, restricts autostart, or delivers accessibility events. That is risk `R-06` in `docs/TECHNICAL_SPECIFICATION.md`. It is carried by the protection health screen in phase 19, which must make a dead service visible, and by the Android beta in phase 24, which must recruit testers on that hardware. Phase 22 is scoped as an API-level matrix, not a device matrix.

Evidence wording is not negotiable: "verified on an Android 16 emulator", never "verified on a device".

### Why Android runs to completion first

The original plan interleaved the platforms and gated both on one combined validation. It now runs Android from validation to the Play Store, then iOS from validation to the App Store.

What this buys: a shipped product sooner, one platform's problems at a time, and real device evidence from the Android beta before any Swift is written.

What it costs, stated so nobody rediscovers it later:

- **The domain contract is written from Android evidence alone.** Phase 07 freezes the fixtures with only Android spikes behind them. iOS gets its say in phase 34, and a fixture iOS cannot satisfy is a contract error that changes both platforms. Budget for Android rework there.
- **iOS platform limits are known only from documentation** until phase 28. The fifteen-minute minimum, the twenty-activity cap and the fifty-token shield are written in `docs/TECHNICAL_SPECIFICATION.md` section 4 from Apple's documentation, not from a device.
- **Apple's answer on the entitlement is not known until the iOS track begins.** Phase 27 files the request, and it is the first iOS phase precisely so that a refusal costs no Swift. But the request can take weeks, and the project runs the whole Android cycle without knowing whether iOS is possible. This was decided deliberately. Phase 27 depends on nothing and can be run early, in parallel with any Android phase, by anyone who wants the answer sooner.

### Phase status legend

Mark the status line of each phase as work proceeds: `not started` → `in progress` → `blocked` → `done`.

---

## 2. Phase Index

| # | Phase | Group | Depends on | Status |
|---|---|---|---|---|
| 00 | Git baseline and repository setup | Foundation | — | done |
| 01 | Android spike: launch detection | Android validation | 00 | done |
| 02 | Android spike: accessibility overlay | Android validation | 01 | done |
| 03 | Android spike: supported-app catalog | Android validation | 00 | done |
| 04 | Android spike: temporary bypass persistence | Android validation | 02 | done |
| 05 | Android spike: usage data and daily-limit feasibility | Android validation | 03 | done |
| 06 | Google Play policy package | Android validation | 01, 02 | done |
| 07 | Domain contract and fixtures | Contract | 04, 05 | done |
| 08 | Android validation gate and specification update | Gate | 01–06, 07 | done |
| 09 | Android: project skeleton and design tokens | Android MVP | 08 | done |
| 10 | Android: domain and rule evaluator | Android MVP | 09 | done |
| 11 | Android: persistence layer | Android MVP | 10 | done |
| 12 | Android: detection service | Android MVP | 11 | done |
| 13 | Android: block screen | Android MVP | 12 | done |
| 14 | Android: temporary bypass | Android MVP | 13 | done |
| 15 | Android: application selection | Android MVP | 11 | done |
| 16 | Android: rule editor | Android MVP | 15 | done |
| 17 | Android: daily limits | Android MVP | 16 | not started |
| 18 | Android: history, statistics, dashboard | Android MVP | 17 | not started |
| 19 | Android: onboarding, permissions, protection health | Android MVP | 18 | not started |
| 20 | Android: recovery and reliability hardening | Android MVP | 19 | not started |
| 21 | Android: accessibility and localization | Android MVP | 20 | not started |
| 22 | Android: API-level matrix and defect fixing | Android MVP | 21 | not started |
| 23 | Google Play submission preparation | Android release | 22 | not started |
| 24 | Android closed beta | Android release | 23 | not started |
| 25 | Android beta feedback and defect resolution | Android release | 24 | not started |
| 26 | Android release candidate and Play submission | Android release | 25 | not started |
| 27 | Apple Family Controls entitlement request | iOS entitlement | — | not started |
| 28 | iOS spike: skeleton, targets, and signing | iOS validation | 00, 27 | not started |
| 29 | iOS spike: authorization and picker | iOS validation | 28 | not started |
| 30 | iOS spike: schedule and shield | iOS validation | 29 | not started |
| 31 | iOS spike: shield actions | iOS validation | 30 | not started |
| 32 | iOS spike: usage-threshold bypass and limits | iOS validation | 31 | not started |
| 33 | iOS spike: capacity limits | iOS validation | 30 | not started |
| 34 | iOS contract conformance and validation gate | Gate | 07, 28–33 | not started |
| 35 | iOS: production skeleton | iOS MVP | 34 | not started |
| 36 | iOS: domain and persistence | iOS MVP | 35 | not started |
| 37 | iOS: authorization and picker | iOS MVP | 36 | not started |
| 38 | iOS: schedule compiler and capacity | iOS MVP | 37 | not started |
| 39 | iOS: shield configuration and actions | iOS MVP | 38 | not started |
| 40 | iOS: temporary bypass | iOS MVP | 39 | not started |
| 41 | iOS: daily limits | iOS MVP | 40 | not started |
| 42 | iOS: application screens | iOS MVP | 41 | not started |
| 43 | iOS: recovery and reliability hardening | iOS MVP | 42 | not started |
| 44 | iOS: accessibility and localization | iOS MVP | 43 | not started |
| 45 | iOS: device and TestFlight verification | iOS MVP | 44 | not started |
| 46 | App Store submission preparation | iOS release | 45 | not started |
| 47 | iOS TestFlight beta | iOS release | 46 | not started |
| 48 | iOS beta feedback and defect resolution | iOS release | 47 | not started |
| 49 | iOS release candidate and App Store submission | iOS release | 48 | not started |

Phases 00 to 06 are complete. The Android track runs 07 to 26 without interruption. The iOS track runs 27 to 49 afterwards, and phase 27 can be pulled forward at any time because it depends on nothing.

### Renumbering map

The plan was reordered after phase 06 so that Android runs to release before iOS begins. Phases 00 to 06 kept their numbers and their content. Everything after was renumbered; nothing was deleted. Older commits, and the spike result documents written before the change, may cite the numbers in the left-hand column.

| Original | Now | Original | Now |
|---|---|---|---|
| 07–12 iOS validation | 28–33 | 13 shared contract | 07 |
| 14 validation gate | 08 | 15–28 Android MVP | 09–22 |
| 29–39 iOS MVP | 35–45 | 40 Play submission | 23 |
| 41 App Store submission | 46 | 42 Android beta | 24 |
| 43 iOS beta | 47 | 44 beta defects | 25 and 48 |
| 45 release | 26 and 49 | — | 27 and 34 are new |

Phases 44 and 45 were single cross-platform phases and became one per platform. Phase 27 extracts the Apple entitlement request out of the original phase 07, and phase 34 is the iOS gate that the original combined gate no longer provides.

---


# Foundation

## Phase 00 — Git Baseline and Repository Setup

**Status:** done

**Goal.** Put the repository under version control with the branch model this plan depends on, so every later phase has a commit to reference.

**Depends on.** Nothing.

**Branch.** `phase/00-git-baseline`

**Out of scope.** Any application code. Any change to documentation content.

### Tasks

- [x] Initialize the git repository
- [x] Verify `.gitignore` covers Android, Xcode, secrets, and local tooling
- [x] Commit the current documentation state as the initial commit on `main`
- [x] Create `dev` from `main`
- [x] Create `phase/00-git-baseline` from `dev` and record the branch model in `README.md`
- [x] Add a short "Working on a phase" section to `README.md` describing the session protocol

**Expected result.** A repository with `main`, `dev`, and one phase branch, an initial commit containing all current documentation, and a README that explains how phases are executed.

### Automated checks

- [x] `git log --oneline` returns the initial commit — agent runs
- [x] `git branch -a` shows `main` and `dev` — agent runs
- [x] `git status` is clean after commit — agent runs
- [x] No file matching `.gitignore` patterns is tracked — agent runs `git ls-files`

### Agent checklist

- [x] No secrets, keystores, or provisioning profiles are committed
- [x] `.claude/` is ignored
- [x] Line endings do not mangle existing Markdown
- [x] Commit message follows the convention in `AGENTS.md`

### Manual scenarios for the user

1. Run `git log` and confirm the initial commit contains the documentation.
2. Run `git branch` and confirm `dev` exists.
3. Open `README.md` and confirm the branch model matches how you want to work.

### Definition of Done

Repository initialized, both long-lived branches exist, working tree clean, README documents the workflow.

### Risks

| Risk | Response |
|---|---|
| Accidentally committing local tooling or secrets | verify `git ls-files` before the first commit |
| Line-ending churn on Windows | `.editorconfig` already sets LF; verify the diff of one Markdown file |

### Documents to update

`README.md`

### Merge into `dev` when

Initial commit exists, `dev` branches from it, checks above pass.

---



# Android Validation



Each validation phase produces `docs/spikes/SPIKE_<ID>_RESULT.md` containing environment, commit hash, steps, evidence, defects, and a decision of `GO`, `CHANGE`, or `STOP`. Code written in these phases is throwaway and lives under `spikes/android/<id>/`. It is never promoted directly; it informs the production phase that replaces it.



## Phase 01 — Android Spike: Launch Detection

**Status:** done — decision `GO`, verified on an Android 16 emulator. Physical-hardware confirmation is not planned during development; the OEM gap is carried by phases 19 and 24.

**Goal.** Prove that an `AccessibilityService` on `targetSdk 36` reliably detects a transition into a target application, with no false or runaway detections.

**Depends on.** Phase 00.

**Branch.** `phase/01-android-launch-detection`

**Out of scope.** Any UI. Room. Hilt. Overlays. The supported-app catalog. Production module structure.

### Tasks

- [x] Create a minimal Gradle project under `spikes/android/a01/`
- [x] Implement one `AccessibilityService` handling `TYPE_WINDOW_STATE_CHANGED`
- [x] Read only `event.packageName` and the timestamp
- [x] Exclude BlockSocial's own package and system packages
- [x] Implement debounce with `lastHandledPackage` and `lastHandledAt`
- [x] Log every transition decision with a reason
- [x] Hardcode two target packages for the test
- [x] Write the result document with the decision

**Expected result.** A logcat trace showing correct detection across every scenario below, and a written decision.

### Automated checks

- [x] `./gradlew :a01:assembleDebug` succeeds — agent runs
- [x] `./gradlew :a01:lint` reports no errors — agent runs
- [x] Manifest contains no `SYSTEM_ALERT_WINDOW` and no `QUERY_ALL_PACKAGES` — agent greps
- [x] Unit test of the debounce state machine passes — agent runs `./gradlew :a01:test`

### Agent checklist

- [x] Service reads no text, no tree content, no keystrokes
- [x] No action is performed inside third-party applications
- [x] Self-package exclusion verified in code
- [x] Result document uses `GO`, `CHANGE`, or `STOP`, not prose
- [x] Logs contain no user content

### Manual scenarios for the user

Install on the emulator, enable the service, then check each case and confirm the log:

1. Cold launch of a target app from the launcher.
2. Return to a target app from recents.
3. Open a target app from a notification deep link.
4. Unlock the screen with a target app already in the foreground.
5. Switch rapidly between two target apps.
6. Split-screen with a target app.
7. Open the launcher, then system Settings — neither may be treated as a target.
8. Trigger a system dialog and confirm it is not treated as a target.

### Definition of Done

All eight scenarios behave correctly on the emulator, no repeated detection storms, latency is subjectively acceptable, result document written with a decision.

### Risks

| Risk | Response |
|---|---|
| OEM delivers events differently | unmeasurable here; record the emulator image and hand the question to phase 24 |
| Event floods on rapid switching | debounce window is tunable and its value is recorded |
| Detection latency feels slow | record measured delay; if unacceptable, decision is `CHANGE` |

### Documents to update

`docs/spikes/SPIKE_A-01_RESULT.md`, `docs/TECHNICAL_SPECIFICATION.md` section 6 if an assumption resolves

### Merge into `dev` when

Result document exists with a decision, checks pass, owner confirms the eight scenarios.

---

## Phase 02 — Android Spike: Accessibility Overlay

**Status:** done — decision `GO`, verified on an Android 16 emulator. Three items stay open: the back key is an unrecorded bypass, rotation could not be exercised on this AVD, and screen-reader operation is unconfirmed.

**Goal.** Prove an interactive full-screen `TYPE_ACCESSIBILITY_OVERLAY` works without `SYSTEM_ALERT_WINDOW`, and that it can always be removed.

**Depends on.** Phase 01.

**Out of scope.** Real rules. Persistence. Final visual design. Bypass logic.

**Branch.** `phase/02-android-overlay`

### Tasks

- [x] Extend the phase 01 spike to show an overlay on detection
- [x] Render the block-screen baseline direction chosen in `design/DESIGN_EXPORT_ANALYSIS.md`, not a placeholder — direction 1b
- [x] Implement two working buttons with distinct outcomes
- [x] Implement `GLOBAL_ACTION_HOME` for the primary action
- [x] Implement a watchdog timeout that removes a stuck overlay
- [x] Remove the overlay on package change
- [x] Test with `ComposeView`; fall back to a View implementation if unstable, and record which — ComposeView, no fallback needed
- [ ] Write the result document with the decision

**Expected result.** An overlay that appears over the target app, accepts touch, and always disappears.

### Automated checks

- [x] `./gradlew :a02:assembleDebug` succeeds — agent runs
- [x] `./gradlew :a02:lint` reports no errors — agent runs
- [x] Manifest contains no `SYSTEM_ALERT_WINDOW` — agent greps
- [x] Instrumented test asserting overlay attach and detach — agent runs on the emulator

### Agent checklist

- [x] Overlay does not imitate system UI
- [x] No countdown gate before the primary action becomes usable
- [x] Settings, launcher, phone, and system UI are allowlisted and never covered
- [x] Focus order is title, primary, secondary
- [x] Contrast and touch targets match the brief

### Manual scenarios for the user

1. Open a target app; the overlay appears full screen.
2. Tap the primary action; you return home and the overlay disappears.
3. Tap the secondary action; the overlay disappears and the app is usable.
4. Rotate the device while the overlay is shown.
5. Switch to dark mode and repeat.
6. Press system back and system home with the overlay shown.
7. Trigger the overlay ten times in a row; no leak, no stacking.
8. Kill the app process while the overlay is shown; the device is not left locked.
9. Enable TalkBack and complete the decision using it alone.
10. Set the largest font scale and confirm no truncation.
11. Open Settings and the launcher; no overlay appears.

### Definition of Done

Overlay appears and is removable in all eleven scenarios, no device soft-lock in any case, no second overlay permission requested, result document written.

### Risks

| Risk | Response |
|---|---|
| Overlay unstable with Compose | fall back to a View implementation and record it |
| Overlay covers critical system UI | allowlist plus watchdog; blocking defect if unresolved |
| Device left unusable | this is a `STOP` condition until fixed |

### Documents to update

`docs/spikes/SPIKE_A-02_RESULT.md`, `design/DESIGN_EXPORT_ANALYSIS.md` if the chosen direction proves unbuildable

### Merge into `dev` when

All scenarios pass, no soft-lock observed, decision recorded.

---

## Phase 03 — Android Spike: Supported-App Catalog

**Status:** done — decision `GO`, verified on an Android 16 emulator. One item for production: `<queries>` and the catalog are kept in step by hand and should be generated or test-guarded.

**Goal.** Prove that supported applications can be detected and displayed without `QUERY_ALL_PACKAGES`.

**Depends on.** Phase 00.

**Branch.** `phase/03-android-app-catalog`

**Out of scope.** Selection UI polish. Persistence of the selection. Rules.

### Tasks

- [x] Create the catalog JSON in `shared/supported-app-catalog/catalog.json` with a schema version
- [x] Include at least Instagram, TikTok, YouTube, Facebook, X, Reddit, Snapchat, Telegram, Discord, VK
- [x] Declare exactly those packages through targeted `<queries>`
- [x] Resolve installed entries with `PackageManager`
- [x] Read the label and icon for installed entries
- [x] Render not-installed entries as a normal explained state
- [x] Write the result document with the decision

**Expected result.** A list showing which catalog apps are installed, with correct names and icons, and no broad package-visibility permission.

### Automated checks

- [x] `./gradlew :a03:assembleDebug` succeeds — agent runs
- [x] Merged manifest contains no `QUERY_ALL_PACKAGES` — agent inspects the merged manifest, not just the source
- [x] Catalog JSON parses and validates against its schema — agent runs a unit test
- [x] Unit test covering a missing package returns the not-installed state — agent runs

### Agent checklist

- [x] No installed-app inventory beyond the catalog is read or stored
- [x] Wording is "supported applications", never "all your apps"
- [x] A missing app produces no error and no crash
- [x] Catalog entries carry a stable internal id, not just a package name

### Manual scenarios for the user

1. With several catalog apps installed, confirm each is listed with the correct name and icon.
2. Uninstall one and confirm it moves to the not-installed state.
3. Confirm an installed app absent from the catalog is not listed anywhere.
4. Confirm no permission prompt about app visibility appears.

### Definition of Done

Installed catalog apps detected on the emulator, merged manifest clean, missing apps handled, decision recorded.

### Risks

| Risk | Response |
|---|---|
| Catalog coverage feels too narrow | record which apps testers wanted; catalog grows by release |
| OEM variants use different package names | record variants found; add aliases to the catalog schema |

### Documents to update

`docs/spikes/SPIKE_A-04_RESULT.md`, `docs/ARCHITECTURE.md` if the catalog schema changes

### Merge into `dev` when

Merged manifest verified clean, detection works on a device, decision recorded.

---

## Phase 04 — Android Spike: Temporary Bypass Persistence

**Status:** done — decision `GO`, verified on an Android 16 emulator. Grants are sound; the spike instead exposed that force-stop and app update can leave blocking dead, which moves to phase 19.

**Goal.** Prove that a per-application grant suppresses repeated blocks correctly and survives process death and reboot.

**Depends on.** Phase 02.

**Branch.** `phase/04-android-bypass`

**Out of scope.** Full Room schema. Statistics. UI polish.

### Tasks

- [x] Add minimal persistence for grants with an absolute expiry
- [x] Suppress the overlay for the granted application until expiry
- [x] Keep other restricted applications blocked
- [x] Recompute grant state on service start
- [x] Remove expired grants
- [x] Handle a device clock change without granting infinite access
- [x] Write the result document with the decision

**Expected result.** A grant that applies to exactly one application, expires correctly, and survives restarts.

### Automated checks

- [x] `./gradlew :a04:test` passes, covering expiry boundary, clock change, and second-app isolation — agent runs
- [x] `./gradlew :a04:assembleDebug` succeeds — agent runs

### Agent checklist

- [x] Grants store an absolute expiry, never a countdown in memory
- [x] Expiry evaluated on every detection, not on a timer
- [x] No grant leaks across applications
- [x] Clock moved backwards does not extend a grant

### Manual scenarios for the user

1. Take a five-minute grant, use the app, confirm no repeated block.
2. Switch away and back within the window; still no block.
3. Turn the screen off and on within the window; still no block.
4. Wait for expiry, reopen the app, confirm the block returns.
5. Open a second restricted app during the window; it must still block.
6. Reboot during the window; the remaining grant behaves correctly.
7. Move the device clock backwards during the window; access does not extend.
8. Force-stop the app during the window and reopen it.

### Definition of Done

All eight scenarios pass on the emulator, unit tests green, decision recorded.

### Risks

| Risk | Response |
|---|---|
| Process death loses grant state | state is recomputed from storage, never held only in memory |
| Clock manipulation grants unlimited access | evaluate against stored absolute time and detect backwards jumps |

### Documents to update

`docs/spikes/SPIKE_A-03_RESULT.md`

### Merge into `dev` when

Scenarios pass, tests green, decision recorded.

---

## Phase 05 — Android Spike: Usage Data and Daily-Limit Feasibility

**Status:** done — decision `GO`, measured on Android 16 and Android 13 emulator images. Daily limits are viable; `Q-03` is answered in favour of `queryEvents`.

**Goal.** Determine whether `UsageStatsManager` is accurate enough to drive daily limits and statistics, and decide the source of the time-in-app metric.

**Depends on.** Phase 03.

**Branch.** `phase/05-android-usage-stats`

**Out of scope.** The daily-limit feature itself. Charts. Aggregation jobs.

### Tasks

- [x] Request and verify `PACKAGE_USAGE_STATS` access
- [x] Read per-application totals for today
- [x] Compare measured totals against a stopwatch for a controlled session
- [x] Test the day boundary and the local-midnight reset
- [x] Test with the screen off and with rapid switching
- [x] Handle an empty or partial result without crashing
- [x] Record measured error margin per emulator image and API level
- [x] Decide and record the source of the time-in-app metric
- [x] Write the result document with the decision

**Expected result.** A recorded accuracy margin, a decision on whether daily limits are viable, and a decision on the statistics source.

### Automated checks

- [x] `./gradlew :a05:test` passes, covering empty results, partial results, and day-boundary arithmetic — agent runs
- [x] `./gradlew :a05:assembleDebug` succeeds — agent runs

### Agent checklist

- [x] Blocking never depends on usage data
- [x] Measured and estimated values are distinguished in the output
- [x] Permission-denied path is handled with a clear state
- [x] Error margin is recorded as a number, not as an impression

### Manual scenarios for the user

1. Use a target app for a measured five minutes; compare the reported total.
2. Repeat with the screen turning off mid-session.
3. Switch rapidly between three apps and compare totals.
4. Cross local midnight and confirm the counter resets.
5. Revoke usage access and confirm the app reports the state without crashing.
6. Repeat steps 1 and 4 on a second emulator image at a different API level.

### Definition of Done

Accuracy margin recorded on at least two emulator images, day boundary correct, denial handled, both decisions recorded.

### Risks

| Risk | Response |
|---|---|
| Accuracy too poor for limits | limits ship as explicitly approximate, or the decision becomes `CHANGE` |
| OEM reports nothing | record it; protection health must surface the condition |

### Documents to update

`docs/spikes/SPIKE_A-05_RESULT.md`, `docs/PRODUCT.md` question `Q-03`, `docs/TECHNICAL_SPECIFICATION.md` section 6

### Merge into `dev` when

Two devices measured, decisions recorded, tests green.

---

## Phase 06 — Google Play Policy Package

**Status:** done — package written and cross-read against the code at commit `5bf53a3`. Two gaps recorded rather than smoothed over: ungated logging in two spike modules, and policy text describing storage that production has not built yet. Both assigned to phase 23.

**Goal.** Produce the complete policy package for the Accessibility use case and prove it describes what the code actually does.

**Depends on.** Phases 01 and 02.

**Branch.** `phase/06-play-policy-package`

**Out of scope.** Store listing graphics. Pricing. The actual submission.

### Tasks

- [x] Write the in-app prominent disclosure text
- [x] Write the affirmative consent screen text
- [x] Write the privacy policy draft
- [x] Write the Play listing paragraph describing the Accessibility use case
- [x] Draft the Play Console Accessibility declaration
- [x] Write the demo-video script
- [x] Inventory every piece of data the service touches
- [x] Write the argument that screen content is never stored
- [x] Write the justification for `isAccessibilityTool=false`
- [x] Cross-read the disclosure, the spike code, and the declaration for contradictions

**Expected result.** A policy package in `docs/store/play/` where all three descriptions of the mechanism agree.

### Automated checks

- [x] Every claim in the disclosure is traceable to a line in the spike code — agent lists the mapping
- [x] The data inventory contains no item the code does not touch, and misses none it does — agent verifies against phase 01 source
- [x] All documents are in English and free of placeholders — agent greps for `TBD` and `TODO`

### Agent checklist

- [x] The disclosure does not resemble a system dialog
- [x] No claim is made that cannot be demonstrated in the demo video
- [x] `isAccessibilityTool=false` justification matches the product positioning
- [x] The privacy policy matches the actual data behavior, not the intended one

### Manual scenarios for the user

1. Read the disclosure as a first-time user and judge whether the reason is clear.
2. Compare the disclosure against the Play policy text for Accessibility use.
3. Confirm the demo-video script can actually be filmed with the current spike build.
4. Confirm nothing in the package promises a capability that is still unverified.

### Definition of Done

Package complete, internally consistent, no placeholders, owner has read it end to end.

### Risks

| Risk | Response |
|---|---|
| Play policy interpretation is wrong | this is the earliest possible check; a rejection later is far costlier |
| Documents drift from the code | phase 23 re-verifies the package against the shipping build |

### Documents to update

`docs/store/play/*`, `docs/TECHNICAL_SPECIFICATION.md` section 7 risk `R-01`

### Merge into `dev` when

Package complete and cross-read, no contradictions found.

---



# Contract and Android Gate

## Phase 07 — Domain Contract and Fixtures

**Status:** done — 58 cases across four files, validated by `shared/fixtures-validator` with 26 checks, all green. Four cases marked at risk from Apple limits, all in the bypass corpus.

**Goal.** Freeze the rule evaluator contract and the event contract as shared JSON fixtures, so Android has one definition to implement and iOS has one definition to conform to later.

**Depends on.** Phases 04 and 05.

**Branch.** `phase/07-domain-contract`

**Out of scope.** Any platform implementation. UI. Statistics aggregation.

This phase was originally written to run after both platforms had been validated, so that the contract could be shaped by evidence from each. iOS validation now comes later, which means the contract is written from Android evidence alone and iOS must conform to it in phase 34 rather than help shape it.

That is a real cost, not a neutral reordering. The mitigation is to keep the fixtures free of anything Android-shaped: every case states inputs and one expected domain result, never a mechanism. Where an Apple limit is already known from `docs/TECHNICAL_SPECIFICATION.md` section 4 — the fifteen-minute minimum, the twenty-activity cap, the fifty-token shield — the fixtures must not assume it away. If phase 34 finds a case iOS cannot satisfy, the fixture changes and Android changes with it, in the same phase.

### Tasks

- [x] Create `shared/fixtures/schedule-cases.json`
- [x] Create `shared/fixtures/bypass-cases.json`
- [x] Create `shared/fixtures/daily-limit-cases.json`
- [x] Create `shared/fixtures/rule-priority-cases.json`
- [x] Cover normal interval, overnight interval, selected weekday, disabled rule
- [x] Cover active bypass, expired bypass, bypass on a second app
- [x] Cover limit not reached, limit reached, limit reset at local midnight
- [x] Cover time-zone change and DST transition in both directions
- [x] Carry over the two clock rules proven in spike `A-03`: a clock moved backwards must not extend a grant, and a reboot must not resurrect an expired one
- [x] Carry over the day-boundary arithmetic proven in spike `A-05`, including a 23-hour and a 25-hour day
- [x] Freeze the `BlockEvent` field list and schema version
- [x] Write a fixture schema so malformed cases fail loudly
- [x] Mark every case that a known Apple limit could make unsatisfiable, so phase 34 has a shortlist rather than a search

**Expected result.** A fixture corpus that Android loads now and iOS loads later, with every case carrying inputs and one expected domain result.

### Automated checks

- [x] Every fixture file validates against its schema — agent runs a validation script
- [x] No two cases have the same identifier — agent runs
- [x] Every enumerated business rule in `docs/PRODUCT.md` has at least one case — agent maps them and reports gaps
- [x] No case name, field or comment mentions an Android class, API or package — agent greps

### Agent checklist

- [x] Cases state expected results, not implementation details
- [x] Overnight and DST cases specify the exact local times
- [x] Daily-limit cases include the approximate-measurement tolerance
- [x] Nothing in the corpus assumes a mechanism only Android has
- [x] The cases at risk from Apple limits are marked, and the reason is written next to each

### Manual scenarios for the user

1. Read `schedule-cases.json` and confirm the overnight case matches your expectation.
2. Confirm the DST cases cover both the spring and autumn transitions.
3. Confirm a daily-limit case exists for measurement arriving late.
4. Confirm the marked at-risk cases look like a list you would want phase 34 to check first.

### Definition of Done

All four fixture files exist and validate, every business rule mapped to at least one case, at-risk cases marked, schema committed.

### Risks

| Risk | Response |
|---|---|
| The contract turns out Android-shaped | the at-risk marking and the no-Android-terms check are the guard; phase 34 is where it is proven, and a failure there changes both platforms |
| Fixtures encode a wrong assumption | they are the contract; changing one later requires updating every platform that already implements it |
| Coverage gaps found during implementation | add the case to the fixtures first, then fix the implementations |

### Documents to update

`docs/ARCHITECTURE.md` shared-contract section, `docs/TECHNICAL_SPECIFICATION.md` section 8

### Merge into `dev` when

Fixtures validate, rule coverage mapped with no gaps, at-risk cases marked.

---

## Phase 08 — Android Validation Gate and Specification Update

**Status:** done — verdict `GO` for Android. Five spike decisions, all `GO`; three constraints promoted to mandatory; risk `R-05` closed, `R-04` downgraded, `R-07` added; `Q-02` answered. Gate in `docs/GATE_ANDROID.md`.

**Goal.** Convert the Android validation evidence into decisions, update the specification, and decide whether Android implementation proceeds.

**Depends on.** Phases 01 through 06, and 07.

**Branch.** `phase/08-android-validation-gate`

**Out of scope.** Any implementation. Any new spike. Any iOS verdict, which phase 34 makes on its own evidence.

This gate covers Android only. The original single gate judged both platforms at once; with iOS moved after Android, judging iOS here would mean judging it with no evidence.

### Tasks

- [x] Collect every Android spike result and its decision into one summary table
- [x] Answer: is the Android overlay stable on API 36?
- [x] Answer: is the Accessibility use case defensible under Play policy?
- [x] Answer: does Android work without `QUERY_ALL_PACKAGES`?
- [x] Answer: is usage measurement accurate enough for daily limits?
- [x] Answer: do temporary grants survive process death, reboot and clock manipulation?
- [x] Record what emulator-only evidence does not cover, and who carries it
- [x] Record the current state of the Apple entitlement request from phase 27, as information, not as a verdict
- [x] Fold every `CHANGE` decision into the specification and architecture — no decision was `CHANGE`, but three mandatory constraints from `GO` results were folded in
- [x] Remove resolved items from the unverified-assumptions table
- [x] Downgrade or close resolved risks
- [x] Answer the open questions in `docs/PRODUCT.md` that the Android spikes settled
- [x] Record a `GO`, `CHANGE`, or `STOP` verdict for Android
- [x] Re-scope phases 09 to 26 against what was learned

**Expected result.** A gate document with evidence-backed answers and an Android verdict, plus an updated specification containing no assumption an Android spike has already settled.

### Automated checks

- [x] Every answer cites a spike result file that exists — agent verifies each path resolves
- [x] The unverified-assumptions table contains nothing an Android spike has decided — agent cross-checks
- [x] No document still describes a mechanism a spike replaced — agent greps for the old terms
- [x] No answer claims anything about iOS behaviour — agent greps the gate document

### Agent checklist

- [x] Every answer cites evidence, never an opinion
- [x] Emulator-only evidence is labelled as such in every answer that rests on it
- [x] Product copy promises nothing the gate did not confirm
- [x] Phase re-scoping is written down, not implied
- [x] The entitlement status is reported exactly as it stands, and a pending request is never written up as an approval

### Manual scenarios for the user

1. Read the gate document and confirm each answer is backed by a file you can open.
2. Confirm the specification no longer lists a settled question as unverified.
3. Confirm every claim resting on emulator evidence says so.
4. Decide and record the Android verdict.

### Definition of Done

Every Android answer recorded with citations, specification and architecture updated, Android verdict recorded, phases 09 to 26 re-scoped.

### Risks

| Risk | Response |
|---|---|
| A `CHANGE` result reshapes several later phases | re-scope explicitly before any implementation starts |
| The gate quietly assumes iOS will work | this phase makes no iOS claim at all; phase 34 does that |
| Emulator evidence read as device evidence | the labelling check is an automated grep, not a habit |

### Documents to update

`docs/GATE_ANDROID.md`, `docs/TECHNICAL_SPECIFICATION.md`, `docs/ARCHITECTURE.md`, `docs/PRODUCT.md`, `docs/PLAN.md`

### Merge into `dev` when

All answers recorded, Android verdict made, documents consistent.

---



# Android MVP

## Phase 09 — Android: Project Skeleton and Design Tokens

**Status:** done — project builds, 16 token tests and 3 Compose instrumented tests green, lint clean, verified on Android 16 and Android 13 emulator images. Two findings: `targetSdk 36` forces edge-to-edge and every screen must handle insets, and three color roles cannot carry text at 4.5:1, recorded as `design/DESIGN_EXPORT_ANALYSIS.md` section 9.7.

**Goal.** Create the production Android project with its module structure, dependency setup, and the design token system, so every later phase has somewhere to put code.

**Depends on.** Phase 08.

**Branch.** `phase/09-android-skeleton`

**Out of scope.** Any feature. Any screen beyond a token preview. Any system service.

### Tasks

- [x] Create the Gradle project under `android/` with a Version Catalog
- [x] Create modules `app`, `core-model`, `core-domain`, `core-data`, `core-ui`
- [x] Create empty feature modules for onboarding, dashboard, app-selection, rules, history, settings
- [x] Configure Hilt
- [x] Configure Compose and Material 3
- [x] Implement color tokens for light and dark from `design/DESIGN_EXPORT_ANALYSIS.md`
- [x] Implement the type scale, spacing scale, radius scale, and motion durations
- [x] Wire Reduce Motion to collapse durations to zero
- [x] Add a debug token-preview screen
- [x] Configure unit test and instrumented test infrastructure

**Expected result.** A project that builds, with a theme that renders correctly in both themes and a preview screen proving it.

### Automated checks

- [x] `./gradlew build` succeeds — agent runs
- [x] `./gradlew test` passes — agent runs
- [x] `./gradlew lint` reports no errors — agent runs
- [x] Every color role resolves in both themes — agent runs a unit test over the token map
- [x] Contrast of every on-color against its surface is at least 4.5:1 — agent runs a computed test

### Agent checklist

- [x] Tokens are converted from the documented `oklch` sources, and the conversion is recorded
- [x] Danger role is defined but referenced nowhere yet
- [x] Elevation is a surface step, not a shadow
- [x] No feature code exists in any module yet
- [x] Module dependencies point one direction only

### Manual scenarios for the user

1. Build and install the debug build; open the token preview.
2. Switch the system theme and confirm both themes look intentional.
3. Enable the largest font scale and confirm the type scale holds.
4. Enable Reduce Motion and confirm animations stop.

### Definition of Done

Project builds, tokens render in both themes, contrast test passes, module graph is acyclic.

### Risks

| Risk | Response |
|---|---|
| Colors drift from the design | the contrast test and the recorded conversion prevent silent drift |
| Over-modularization slows work | modules stay empty until a phase needs them |

### Documents to update

`docs/ARCHITECTURE.md` module section, `design/DESIGN_EXPORT_ANALYSIS.md` token conversion note

### Merge into `dev` when

Build green, token tests pass, preview verified by the owner.

---

## Phase 10 — Android: Domain and Rule Evaluator

**Status:** done — all 58 shared fixture cases pass, each as its own named test, plus 13 supporting tests. The harness was checked by corrupting one expectation per file and confirming exactly four failures. The domain has no Android dependency, and a Gradle check now fails the build if it gains one.

**Goal.** Implement the domain model and the rule evaluator, proven against the shared fixtures.

**Depends on.** Phase 09.

**Branch.** `phase/10-android-domain`

**Out of scope.** Persistence. Android framework dependencies. UI.

### Tasks

- [x] Implement `RestrictedApp`, `RestrictionRule`, `BypassPolicy`, `TemporaryAccessGrant`, `BlockEvent` in `core-model`
- [x] Implement the schedule evaluator including overnight intervals
- [x] Implement bypass evaluation against absolute expiry
- [x] Implement daily-limit evaluation with local-midnight reset
- [x] Implement rule priority: always-on, then schedule, then daily limit
- [x] Implement the fixture loader and run every shared case as a test
- [x] Handle time-zone change and DST explicitly

**Expected result.** A pure Kotlin domain layer with no Android dependency, passing every shared fixture case.

### Automated checks

- [x] `./gradlew :core-domain:test` passes with every fixture case — agent runs
- [x] Fixture count in the test report equals the count in `shared/fixtures/` — agent verifies
- [x] `core-model` and `core-domain` have no Android dependency — agent inspects the dependency graph
- [x] `./gradlew lint` clean — agent runs

### Agent checklist

- [x] No fixture case is skipped or marked as expected-to-fail
- [x] Evaluation is pure: same inputs give the same result
- [x] Clock is injected, never read statically
- [x] Daily-limit tolerance matches the fixture definition

### Manual scenarios for the user

1. Read the test report and confirm every fixture case is present.
2. Change one fixture expectation deliberately and confirm the test fails.

### Definition of Done

Every fixture case passes, domain is framework-free, clock is injectable.

### Risks

| Risk | Response |
|---|---|
| DST handling subtly wrong | fixtures cover both transitions; failures block the phase |
| Priority logic diverges from iOS | both platforms run the same fixtures; phase 36 must match |

### Documents to update

`docs/ARCHITECTURE.md` if the evaluator interface differs from the description

### Merge into `dev` when

All fixture tests green, no Android dependency in the domain modules.

---

## Phase 11 — Android: Persistence Layer

**Status:** done — six tables at schema version 1, exported and committed. 15 unit tests and 14 instrumented tests green on an Android 16 emulator. Survival was verified rather than assumed: data seeded, then read back after a force-stop and again after a reboot, without reseeding. Two stale blocks in `docs/TECHNICAL_SPECIFICATION.md` section 2 were corrected, both reported.

**Goal.** Implement Room and DataStore with a migration policy, so state survives process death and reboot.

**Depends on.** Phase 10.

**Branch.** `phase/11-android-persistence`

**Out of scope.** UI. The accessibility service. Statistics aggregation jobs.

### Tasks

- [x] Define Room entities for restricted apps, rules, grants, block events, usage sessions, aggregates
- [x] Define DAOs with the queries the features need
- [x] Implement repositories over the DAOs
- [x] Configure DataStore for onboarding, theme, language, consent, permission snapshot
- [x] Write the schema version and export the schema
- [x] Add a migration test from version one
- [x] Forbid destructive migration in release builds

**Expected result.** A persistence layer with an exported schema, migration test, and repositories the domain can use.

### Automated checks

- [x] `./gradlew :core-data:test` passes — agent runs
- [x] Room schema is exported to the repository — agent verifies the file exists
- [x] Migration test passes — agent runs
- [x] No `fallbackToDestructiveMigration` in the release configuration — agent greps
- [x] Instrumented DAO tests pass — agent runs on the emulator

### Agent checklist

- [x] Entities carry a schema version
- [x] No user content is stored beyond package identifiers and timestamps
- [x] Repositories expose domain types, not entities
- [x] Indices exist for every query used on a hot path

### Manual scenarios for the user

1. Install, create data, force-stop, reopen; confirm data is intact.
2. Reboot the device and confirm data is intact.
3. Confirm the exported schema file is committed.

### Definition of Done

Tests green, schema exported, migration test present, destructive migration blocked in release.

### Risks

| Risk | Response |
|---|---|
| Schema churn later | schema is exported and migration-tested from the first version |
| Heavy queries on the detection path | indices added now; measured in phase 12 |

### Documents to update

`docs/ARCHITECTURE.md` storage section

### Merge into `dev` when

Tests green, schema exported, owner confirms data survives restart.

---

## Phase 12 — Android: Detection Service

**Status:** done — all five scenarios verified on an Android 16 emulator, 23 unit and 9 instrumented tests green. A device run found that the notification shade produced a second block from an application the user never left; fixed, and the fix answers the open question the gate assigned to this phase. Two checklist items written before the spikes contradicted `docs/GATE_ANDROID.md` and were corrected, both reported.

**Goal.** Implement the production accessibility service using what phase 01 proved, wired to real rules and persistence.

**Depends on.** Phase 11.

**Branch.** `phase/12-android-detection`

**Out of scope.** The block screen UI. Bypass creation. Statistics.

### Tasks

- [x] Implement the accessibility service with the safeguards from phase 01
- [x] Load the supported catalog and evaluate against real rules
- [x] Keep all database work off the callback thread
- [x] Implement the system allowlist for settings, launcher, phone, and system UI
- [x] Implement self-package exclusion
- [x] Restore state from persistence on service start
- [x] Emit a structured decision log with no user content
- [x] Handle service disable and re-enable without crashing

**Expected result.** A service that decides correctly whether a launch should be blocked, without showing anything yet.

### Automated checks

- [x] `./gradlew test` passes, covering the decision state machine — agent runs
- [x] No blocking database call on the callback path — agent inspects and adds a test with a strict-mode assertion
- [x] Instrumented test asserting the allowlist is never blocked — agent runs on the emulator
- [x] `./gradlew lint` clean — agent runs

### Agent checklist

- [x] Only the package name, the window class name and the timestamp are read from the event, and the class name is reduced to a boolean without being stored
- [x] Entry is a change of the foreground package, with no time-based debounce, as `docs/GATE_ANDROID.md` requires
- [x] Decision log lines are categorized and free of user content
- [x] Service survives being disabled and re-enabled

### Manual scenarios for the user

1. Create a rule, open the target app, and confirm the log shows a block decision.
2. Open a non-restricted app and confirm no block decision.
3. Open Settings and the launcher; confirm neither is ever a target.
4. Disable the accessibility service, re-enable it, and confirm decisions resume.
5. Reboot and confirm decisions resume without opening the app.

### Definition of Done

Correct decisions in all five scenarios, no database work on the callback thread, allowlist never violated.

### Risks

| Risk | Response |
|---|---|
| Callback latency from database access | measured; work moved off the callback thread |
| OEM kills the service | surfaced by protection health in phase 19 |

### Documents to update

`docs/ARCHITECTURE.md` Android mechanism section

### Merge into `dev` when

Tests green, five scenarios verified by the owner.

---

## Phase 13 — Android: Block Screen

**Status:** done, with one item carried forward. The block screen appears on a real launch on an Android 16 emulator and every outcome is recorded, including the back key, which spike `A-02` found removed the overlay silently. 250 tests green. **Rotation is still unverified**: this AVD does not rotate either, so the item moves to phase 22 rather than being ticked. TalkBack remains for the owner. The service was seen in `Crashed services` once and could not be reproduced; the error handling that failure class needs was added regardless.

**Goal.** Implement the production block screen using the chosen design direction, driven by real detection.

**Depends on.** Phase 12.

**Branch.** `phase/13-android-block-screen`

**Out of scope.** Bypass grant logic, which is phase 14. Statistics on the screen beyond a simple count.

### Tasks

- [x] Implement the overlay host in the accessibility service
- [x] Build the block screen from the chosen design direction and the phase 09 tokens
- [x] Show the application, the active rule, and the time remaining
- [x] Implement the primary action returning the user home
- [x] Implement the secondary action as a stub that only records intent
- [x] Record a `BlockEvent` for each decision
- [x] Implement the watchdog timeout and removal on package change
- [x] Support light and dark, landscape, and the largest font scale
- [x] Decide and implement what the system back key does, then record it like any other outcome — carried over from spike A-02, where back removed the overlay silently and recorded nothing
- [ ] Verify rotation on an AVD configured to rotate — carried over from spike A-02, where the AVD could not rotate at all
- [x] Decide deliberately whether the status bar and notification shade stay reachable above the block screen — spike A-02 left them reachable by default

**Expected result.** A working block screen that appears on a real launch and records the decision.

### Automated checks

- [x] `./gradlew test` passes, covering event recording for both actions — agent runs
- [x] Compose UI tests for both themes and the largest font scale — agent runs
- [x] Accessibility test asserting every interactive element has a label — agent runs
- [x] Instrumented test asserting the overlay always detaches — agent runs on the emulator

### Agent checklist

- [x] The screen never imitates system UI
- [x] There is no forced wait before the primary action works
- [x] Focus order reaches the primary action first
- [x] Copy contains no shaming language and no hardcoded duration promise
- [x] Every string is a resource, none hardcoded

### Manual scenarios for the user

1. Open a restricted app during an active rule; the block screen appears.
2. Confirm it names the app, the rule, and the time remaining correctly.
3. Tap the primary action; confirm you return home and the event is recorded.
4. Tap the secondary action; confirm the event is recorded.
5. Rotate to landscape and repeat.
6. Switch to dark theme and repeat.
7. Set the largest font scale and confirm nothing truncates.
8. Enable TalkBack and complete the decision using it alone.

### Definition of Done

Eight scenarios pass, events recorded correctly, no truncation at the largest scale, TalkBack usable.

### Risks

| Risk | Response |
|---|---|
| Overlay instability appears only at scale | watchdog plus the phase 22 API-level matrix |
| Design direction does not fit the smallest device | fall back to the simpler direction recorded in phase 02 |

### Documents to update

`design/DESIGN_EXPORT_ANALYSIS.md` if the built screen differs from the design

### Merge into `dev` when

Tests green, eight scenarios verified.

---

## Phase 14 — Android: Temporary Bypass

**Status:** done — the whole grant lifecycle verified on an Android 16 emulator, including a reboot mid-grant that came back as `ACTIVE_AFTER_REBOOT` and expiry that restored the block and cleared the stored grant. 262 tests green. Scenario 4 could not be run on device because only one catalog application is installed on the AVD; it rests on a unit test and the fourteen bypass fixtures. Two checkboxes in this phase were found already ticked from commit `d47a285` and were corrected before the work started.

**Goal.** Implement real bypass grants with repeat-block suppression, using what phase 04 proved.

**Depends on.** Phase 13.

**Branch.** `phase/14-android-bypass`

**Out of scope.** Bypass usage limits per day beyond the policy field. iOS parity.

### Tasks

- [x] Create a `TemporaryAccessGrant` from the secondary action
- [x] Persist the grant with an absolute expiry
- [x] Suppress the block for that application until expiry
- [x] Keep other restricted applications blocked
- [x] Recompute grants on service start and after reboot
- [x] Remove expired grants
- [x] Detect a backwards clock change and refuse to extend a grant
- [x] Record `BYPASSED` with the granted duration

**Expected result.** A bypass that behaves exactly as phase 04 proved, now integrated with real rules and storage.

### Automated checks

- [x] `./gradlew test` passes, covering expiry boundary, second-app isolation, and clock rollback — agent runs
- [x] Fixture cases for bypass all pass — agent runs
- [x] Instrumented test for grant survival across process death — agent runs on the emulator

### Agent checklist

- [x] Expiry is absolute and stored, never a memory countdown
- [x] A grant never leaks to another application
- [x] Recorded duration matches the granted duration
- [x] Suppression is evaluated on every detection

### Manual scenarios for the user

1. Take a grant and use the app; no repeated block.
2. Switch away and back; no block.
3. Wait for expiry, reopen; block returns.
4. Open a second restricted app during the grant; it blocks.
5. Reboot mid-grant; behavior stays correct.
6. Move the clock backwards mid-grant; access does not extend.

### Definition of Done

Six scenarios pass, bypass fixtures green, grants survive reboot.

### Risks

| Risk | Response |
|---|---|
| Grant survives longer than intended | boundary test at expiry plus manual scenario three |
| Clock manipulation | explicit backwards-jump detection with a test |

### Documents to update

None expected; update `docs/ARCHITECTURE.md` if the grant lifecycle differs

### Merge into `dev` when

Tests green, six scenarios verified.

---

## Phase 15 — Android: Application Selection

**Status:** done — the selection screen runs on an Android 16 emulator and reflects real device state: YouTube installed and selected, nine catalog entries shown as normal not-installed rows rather than errors. 7 Compose tests plus the merged-manifest check. The catalog loader moved from `app` to `core-data` so a feature module can reach it. The `<queries>` drift guard was already delivered in phase 12 and is proven to fail on a mismatch. A debug entry point was added because the screen had no host and none of the five scenarios could otherwise be run.

**Goal.** Ship the supported-app selection screen using the catalog proven in phase 03.

**Depends on.** Phase 11.

**Branch.** `phase/15-android-app-selection`

**Out of scope.** Rules. The block screen. Catalog expansion beyond the initial list.

### Tasks

- [x] Load the catalog and resolve installed entries
- [x] Build the selection screen from the design components
- [x] Show installed, selected, and not-installed states distinctly
- [x] Persist the selection
- [x] Explain that the list is supported applications, not all applications
- [x] Handle an app uninstalled after selection
- [x] Handle an empty catalog result without a dead end
- [x] Stop `<queries>` and the catalog from drifting apart, by generating the manifest entries from `catalog.json` or by a test that fails when they disagree — carried over from spike A-04, where twelve manifest lines mirror twelve package names by hand and a mismatch would silently resolve to not-installed on every device

**Expected result.** A selection screen that reflects reality and never implies hidden inspection of the device.

### Automated checks

- [x] `./gradlew test` passes for selection persistence and uninstall handling — agent runs
- [x] Compose UI tests for all three row states — agent runs
- [x] Merged manifest still free of `QUERY_ALL_PACKAGES` — agent verifies
- [x] Accessibility labels present on every row — agent runs

### Agent checklist

- [x] Not-installed rows are not presented as errors
- [x] Copy never says "all your apps"
- [x] Selection survives process death
- [x] Icons load without blocking the main thread

### Manual scenarios for the user

1. Open selection and confirm installed catalog apps appear with correct names and icons.
2. Select three apps, leave, return; the selection persists.
3. Uninstall a selected app and confirm the state updates sensibly.
4. Confirm an installed non-catalog app is not listed.
5. Read the explanation and confirm it is honest about coverage.

### Definition of Done

Five scenarios pass, manifest clean, selection durable.

### Risks

| Risk | Response |
|---|---|
| Users expect apps outside the catalog | copy sets the expectation; requests recorded for later releases |

### Documents to update

`docs/PRODUCT.md` if catalog coverage wording changes

### Merge into `dev` when

Tests green, five scenarios verified.

---

## Phase 16 — Android: Rule Editor

**Status:** done — the editor runs on an Android 16 emulator and states an overnight interval three ways at once: the word, a two-segment bar with midnight labelled at the crossing, and the plain sentence. 300 tests green, lint clean. The twenty schedule fixtures were re-run end to end from rules that had been through storage, not from literals. A debug host was added so the six scenarios can be run at all.

**Goal.** Ship rule creation and editing for schedule and always-on modes, with the daily-limit mode present but not yet enforced.

**Depends on.** Phase 15.

**Branch.** `phase/16-android-rule-editor`

**Out of scope.** Daily-limit enforcement, which is phase 17. Statistics.

### Tasks

- [x] Build the rule editor with mode selection
- [x] Show only the fields the selected mode uses
- [x] Implement weekday selection and time pickers
- [x] Make the overnight interval unambiguous in three ways
- [x] Implement the bypass policy field
- [x] Validate and explain every error in plain language
- [x] Persist rules and reflect them in the evaluator
- [x] Build the rule list with enabled and paused states

**Expected result.** Users can create, edit, pause, and delete rules, and the evaluator honors them immediately.

### Automated checks

- [x] `./gradlew test` passes for validation and persistence — agent runs
- [x] Schedule fixtures still pass end to end from stored rules — agent runs
- [x] Compose UI tests for create, edit, overnight, and error states — agent runs
- [x] Accessibility labels on every control — agent runs

### Agent checklist

- [x] Overnight interval cannot be misread as its inverse
- [x] Validation errors state how to fix them
- [x] A paused rule says it is not blocking
- [x] Editing a rule takes effect without an app restart

### Manual scenarios for the user

1. Create a weekday evening rule and confirm it blocks at the right time.
2. Create an overnight rule crossing midnight and confirm it reads unambiguously.
3. Create an always-on rule and confirm it blocks immediately.
4. Pause a rule and confirm blocking stops.
5. Enter an invalid time range and confirm the error explains the fix.
6. Edit a rule while it is active and confirm the change applies.

### Definition of Done

Six scenarios pass, fixtures still green, errors actionable.

### Risks

| Risk | Response |
|---|---|
| Overnight intervals confuse users | three redundant signals, verified in scenario two |
| Rule changes not applied live | scenario six blocks the phase if it fails |

### Documents to update

`design/UI_UX_BRIEF.md` if the editor differs from the brief

### Merge into `dev` when

Tests green, six scenarios verified.

---

## Phase 17 — Android: Daily Limits

**Status:** not started

**Goal.** Ship the `DAILY_LIMIT` mode using the usage measurement validated in phase 05.

**Depends on.** Phase 16.

**Branch.** `phase/17-android-daily-limits`

**Out of scope.** Charts. Weekly trends. iOS parity.

### Tasks

- [ ] Request and verify usage access with its own explanation
- [ ] Accumulate per-application usage for the current local day
- [ ] Evaluate the limit and mark the application restricted when reached
- [ ] Block on the next launch after the limit is reached
- [ ] Reset the counter at local midnight
- [ ] Show remaining time as an estimate, never an exact countdown
- [ ] Handle usage access denied or revoked without breaking schedule blocking
- [ ] Add a WorkManager job for daily aggregation and cleanup

**Expected result.** Daily limits that trigger reliably, presented honestly as approximate.

### Automated checks

- [ ] `./gradlew test` passes with every daily-limit fixture case — agent runs
- [ ] Midnight reset test across a simulated day boundary — agent runs
- [ ] Test asserting schedule blocking still works with usage access denied — agent runs
- [ ] WorkManager job tested with the test scheduler — agent runs

### Agent checklist

- [ ] The limit is never enforced by polling
- [ ] Remaining time is labeled as approximate everywhere it appears
- [ ] Usage access denial degrades only limits, never schedules
- [ ] Accumulation tolerates partial or empty usage results

### Manual scenarios for the user

1. Set a ten-minute limit, use the app for ten minutes, confirm the next launch blocks.
2. Confirm the remaining time is shown as approximate, not exact.
3. Cross local midnight and confirm the counter resets.
4. Revoke usage access and confirm schedule rules still block.
5. Reboot mid-day and confirm accumulated usage is not lost.
6. Repeat step one on a second emulator image at a different API level.

### Definition of Done

Six scenarios pass on two emulator images, fixtures green, denial path safe.

### Risks

| Risk | Response |
|---|---|
| Limit triggers noticeably late | expected; the UI states it and the margin from phase 05 is documented |
| OEM reports no usage | protection health surfaces it; schedules keep working |

### Documents to update

`docs/PRODUCT.md`, `docs/ARCHITECTURE.md` if accumulation differs from the described design

### Merge into `dev` when

Tests green, six scenarios verified on two devices.

---

## Phase 18 — Android: History, Statistics, Dashboard

**Status:** not started

**Goal.** Ship the surfaces that show the user what they decided, with honest numbers.

**Depends on.** Phase 17.

**Branch.** `phase/18-android-dashboard`

**Out of scope.** Weekly and monthly trends. Export. Charts beyond the design components.

### Tasks

- [ ] Build the history screen with equal visual weight for stayed and bypassed
- [ ] Show why each rule was active on every entry
- [ ] Build the statistics tiles for interventions, stayed, bypassed, refusal rate
- [ ] Mark estimated values distinctly from measured values
- [ ] Hide any metric the platform cannot measure, rather than showing zero
- [ ] Implement the streak using the rule decided in `docs/PRODUCT.md` question `Q-01`
- [ ] Build the dashboard with protection state most prominent
- [ ] Implement empty states for history and statistics

**Expected result.** A dashboard and history that report decisions accurately and never inflate them.

### Automated checks

- [ ] `./gradlew test` passes for aggregation, refusal rate, and streak arithmetic — agent runs
- [ ] Test asserting an unmeasurable metric is absent, not zero — agent runs
- [ ] Compose UI tests for empty, populated, and degraded states — agent runs
- [ ] Accessibility labels on every statistic — agent runs

### Agent checklist

- [ ] A bypass row is the same size and weight as a stayed row
- [ ] Estimated values carry a marker and a tilde
- [ ] The streak rule matches the answered `Q-01`, and the rule is explained in the UI
- [ ] No metric claims precision the data cannot support

### Manual scenarios for the user

1. Produce a mix of stayed and bypassed events; confirm both appear with equal weight.
2. Confirm every entry states why the rule was active.
3. Confirm the refusal rate matches a hand count.
4. Confirm an estimated value is visibly marked as estimated.
5. Confirm the streak follows the documented rule.
6. Open a fresh install and confirm the empty states read well.

### Definition of Done

Six scenarios pass, arithmetic verified by hand against the tests, no fake precision.

### Risks

| Risk | Response |
|---|---|
| Statistics feel judgmental | tone verified in scenario one and against the brief |
| Streak rule still unanswered | phase blocked until `Q-01` is answered |

### Documents to update

`docs/PRODUCT.md` question `Q-01` if it is answered during this phase

### Merge into `dev` when

Tests green, six scenarios verified.

---

## Phase 19 — Android: Onboarding, Permissions, Protection Health

**Status:** not started

**Goal.** Ship the first-run experience and the permission lifecycle, including recovery when a permission is revoked.

**Depends on.** Phase 18.

**Branch.** `phase/19-android-onboarding-permissions`

**Out of scope.** Store assets. Analytics. Any remote content.

### Tasks

- [ ] Build onboarding explaining the product in a few steps
- [ ] Build the permission explanation card with four fixed slots per permission
- [ ] Implement the accessibility permission flow with prominent disclosure and consent
- [ ] Implement the usage access flow separately
- [ ] Request notifications only when first needed
- [ ] Verify permission results on return from system settings
- [ ] Build the protection health screen with per-requirement status and a repair action
- [ ] Add OEM background-restriction guidance
- [ ] Handle revocation at any time without a crash
- [ ] Persist the permission snapshot and surface changes on the dashboard
- [ ] Detect that the accessibility service is enabled in settings but delivering no events, and report it as broken rather than healthy — carried over from spike A-03, where a reinstall left the service listed under `Bound services` with a live process and no event delivery
- [ ] Detect that force-stop has killed the service, and say so — carried over from spike A-03, where `am force-stop` left `Bound services:{}` with no rebind and blocking silently stopped
- [ ] State plainly what a dead service means: rules stay saved, blocking does not run

**Expected result.** A first run that earns permissions honestly and a health screen that explains and repairs any degraded state.

### Automated checks

- [ ] `./gradlew test` passes for the permission state reducer across every transition — agent runs
- [ ] Test asserting rules save but stay inactive without accessibility access — agent runs
- [ ] Compose UI tests for granted, denied, and revoked states — agent runs
- [ ] Accessibility labels on every status item — agent runs

### Agent checklist

- [ ] No screen resembles a system dialog
- [ ] Every permission card states what is read and what is never read
- [ ] Denial leaves a usable path, never a dead end
- [ ] Health items use shape as well as color for status
- [ ] The disclosure text matches the phase 06 policy package exactly

### Manual scenarios for the user

1. Complete onboarding on a fresh install and reach a working first rule.
2. Deny accessibility access and confirm the app stays usable and explains the consequence.
3. Grant it later from protection health and confirm blocking starts.
4. Revoke accessibility access while the app runs; confirm the health screen updates without a crash.
5. Deny usage access and confirm schedules still work while limits are disabled.
6. Read the OEM guidance and confirm it is labelled as written from vendor documentation, not observed.

### Definition of Done

Six scenarios pass, no crash on any revocation, disclosure matches the policy package.

### Risks

| Risk | Response |
|---|---|
| Users refuse permissions | value explained before the request; measured in beta |
| OEM guidance inaccurate | cannot be checked without that hardware; it ships labelled unverified and is corrected from phase 24 reports |

### Documents to update

`docs/store/play/*` if the disclosure text changes

### Merge into `dev` when

Tests green, six scenarios verified on the emulator, OEM guidance labelled unverified.

---

## Phase 20 — Android: Recovery and Reliability Hardening

**Status:** not started

**Goal.** Make the application behave correctly after reboot, process death, permission changes, and time changes.

**Depends on.** Phase 19.

**Branch.** `phase/20-android-recovery`

**Out of scope.** New features. Visual changes.

### Tasks

- [ ] Restore all state after reboot without opening the app
- [ ] Recompute grants and remove expired ones on start
- [ ] Restore service state after process death
- [ ] Handle time-zone change and DST transition correctly at runtime
- [ ] Ensure no overlay loop is possible under rapid switching
- [ ] Verify no permanent foreground service exists
- [ ] Measure and record battery impact over a day of normal use
- [ ] Add regression tests for every defect found during this phase

**Expected result.** An application that survives every interruption without user intervention.

### Automated checks

- [ ] `./gradlew test` passes including new regression tests — agent runs
- [ ] Test asserting no overlay is shown twice for one launch — agent runs
- [ ] Time-zone and DST runtime tests — agent runs
- [ ] Instrumented reboot-recovery test — agent runs on the emulator
- [ ] Battery usage recorded from system statistics — owner records

### Agent checklist

- [ ] No state lives only in memory
- [ ] Nothing polls the foreground application
- [ ] Every defect found gets a test before its fix
- [ ] Logs remain free of user content under stress

### Manual scenarios for the user

1. Reboot and confirm blocking works without opening the app.
2. Force-stop the app and confirm blocking resumes.
3. Change the device time zone and confirm rules follow local time.
4. Switch between two restricted apps twenty times rapidly; confirm no overlay loop.
5. Use the phone normally for a day and check battery attribution.
6. Fill the device with a low-memory workload and confirm recovery.

### Definition of Done

Six scenarios pass, no loop observed, battery impact recorded, regression tests added.

### Risks

| Risk | Response |
|---|---|
| OEM kills the service after reboot | documented per device; protection health surfaces it |
| Battery impact higher than expected | measured now, not at release |

### Documents to update

`docs/TECHNICAL_SPECIFICATION.md` section 5 if measured behavior differs

### Merge into `dev` when

Tests green, six scenarios verified, battery figure recorded.

---

## Phase 21 — Android: Accessibility and Localization

**Status:** not started

**Goal.** Make every screen usable with a screen reader, at the largest font scale, and in both languages.

**Depends on.** Phase 20.

**Branch.** `phase/21-android-a11y-localization`

**Out of scope.** New features. Additional languages beyond Russian and English.

### Tasks

- [ ] Add content descriptions to every interactive element
- [ ] Verify focus order on every screen, starting with the block screen
- [ ] Confirm a screen reader can read and operate the block overlay, and that the decision can be completed with it alone — carried over from spike A-02, where the overlay window was exposed with a title but its node tree could not be retrieved with adb tooling
- [ ] Keep an accessibility title on the overlay window; `WindowManager.LayoutParams.accessibilityTitle` is not public, so `setTitle` is the working route
- [ ] Support Dynamic Type up to the largest scale without truncation
- [ ] Verify contrast in both themes against the token tests
- [ ] Honor Reduce Motion everywhere
- [ ] Ensure no meaning is carried by color alone
- [ ] Extract every user-facing string to resources
- [ ] Provide Russian and English translations
- [ ] Verify layouts with Russian text at the largest scale
- [ ] Localize times, weekdays, and the first day of the week

**Expected result.** A fully accessible, fully localized application with no truncated layout in either language.

### Automated checks

- [ ] Accessibility scanner test over every screen — agent runs
- [ ] Test asserting no hardcoded user-facing string remains — agent runs a lint rule
- [ ] Compose UI tests at the largest font scale in both languages — agent runs
- [ ] Contrast test still green — agent runs

### Agent checklist

- [ ] The block screen is fully operable with TalkBack alone
- [ ] Russian text at the largest scale truncates nowhere
- [ ] Status meaning survives a greyscale screenshot
- [ ] Reduce Motion collapses every duration to zero

### Manual scenarios for the user

1. Navigate the whole app with TalkBack only.
2. Complete a block decision with TalkBack at the largest font scale.
3. Switch the device to Russian and repeat the main flows.
4. Take a greyscale screenshot of protection health and confirm statuses are still distinguishable.
5. Enable Reduce Motion and confirm nothing animates.

### Definition of Done

Five scenarios pass, scanner reports no critical issues, no hardcoded strings remain.

### Risks

| Risk | Response |
|---|---|
| Russian expansion breaks the block screen | tested at the largest scale in scenario two |
| Screen reader on an overlay behaves oddly | this is the block surface's own mechanism; blocking defect if it fails |

### Documents to update

`docs/TECHNICAL_SPECIFICATION.md` section 5 if a constraint changes

### Merge into `dev` when

Tests green, five scenarios verified.

---

## Phase 22 — Android: API-Level Matrix and Defect Fixing

**Status:** not started

**Goal.** Verify the full application across every API level an emulator can provide, fix what it exposes, and state in writing what remains unverified because no physical device is used.

**Depends on.** Phase 21.

**Branch.** `phase/22-android-api-matrix`

**Out of scope.** New features. Store submission. Any claim about OEM firmware behaviour.

This phase was originally a three-brand physical device matrix. It is not, because Android verification uses an emulator only. What it cannot cover does not disappear; it moves to the protection health screen and to the beta in phase 24, and this phase's job includes saying so precisely.

### Tasks

- [ ] Create emulator images for Android 10, 13, and 16
- [ ] Run the full manual suite on each image
- [ ] Record every defect with API level and reproduction steps
- [ ] Add a regression test for every defect fixed
- [ ] Write the OEM background-restriction guidance from vendor documentation, marked as unverified
- [ ] Produce a compatibility summary that separates what was tested from what was not
- [ ] List the OEM behaviours the beta must confirm, and hand that list to phase 24

**Expected result.** A written compatibility summary covering three API levels, a defect list with no critical or major items remaining, and an explicit statement of the OEM gap.

### Automated checks

- [ ] Full unit and instrumented suites pass on every emulator image — agent runs
- [ ] Every fixed defect has a regression test — agent verifies the mapping
- [ ] `./gradlew build` clean — agent runs

### Agent checklist

- [ ] Every defect is recorded before it is fixed
- [ ] No defect is closed without a test
- [ ] The compatibility summary names API levels, and never implies device coverage
- [ ] OEM guidance is labelled as written from documentation, not observation
- [ ] The list handed to phase 24 is concrete enough for a tester to execute

### Manual scenarios for the user

1. Read the compatibility summary and confirm it claims nothing about physical hardware.
2. Confirm the OEM gap is stated plainly enough to accept.

### Definition of Done

Three API levels covered on emulator images, defects recorded and fixed, no critical or major defects open, compatibility summary written, OEM gap documented and handed to phase 24.

### Risks

| Risk | Response |
|---|---|
| The summary reads as if devices were tested | the agent checklist forbids it; the owner checks it in scenario one |
| OEM defects surface only in beta | expected and accepted; protection health must fail visibly rather than silently |
| Emulator passes hide a real defect | fix what beta finds; do not claim universal support in store copy |

### Documents to update

`docs/COMPATIBILITY.md`, protection health guidance strings, phase 24 tester recruitment criteria

### Merge into `dev` when

API-level matrix complete, no critical or major defects open, OEM gap written down.

---



# Android Release

Android reaches the Play Store here, before any iOS work begins.

## Phase 23 — Google Play Submission Preparation

**Status:** not started

**Goal.** Prepare everything Google Play requires and prove it matches the shipping build.

**Depends on.** Phase 22.

**Branch.** `phase/23-play-submission-prep`

**Out of scope.** Actual submission, which happens in phase 26. iOS.

### Tasks

- [ ] Re-verify the phase 06 policy package against the shipping build
- [ ] Close the ungated-logging gap: confirm no release build logs a package name — carried over from phase 06, where `a03` and `a05` logged package names outside a `BuildConfig.DEBUG` check while the privacy policy claims nothing leaves the device
- [ ] Confirm the privacy policy's storage section matches what production actually stores — carried over from phase 06, where it was written for rules, selections and history that only phases 11 and 18 build
- [ ] Re-run the `DATA_INVENTORY.md` traceability against production sources, since every line number in it points at spike code
- [ ] Finalize the store listing text
- [ ] Produce screenshots for the required device sizes
- [ ] Record the Accessibility demo video from the script
- [ ] Complete the Data Safety declaration
- [ ] Complete the Accessibility declaration
- [ ] Publish the privacy policy at a stable URL
- [ ] Configure the release build with signing and shrinking
- [ ] Produce a signed release artifact and verify it installs
- [ ] Verify the release build contains no debug logging of user data

**Expected result.** A submission-ready package where the listing, the declarations, and the binary agree.

### Automated checks

- [ ] `./gradlew bundleRelease` succeeds — agent runs
- [ ] Release build contains no debug logging of user content — agent greps the mapping and log calls
- [ ] Merged release manifest contains no forbidden permission — agent verifies
- [ ] Every Data Safety answer maps to an actual code behavior — agent lists the mapping

### Agent checklist

- [ ] Every listing claim is true of the shipping build
- [ ] The demo video shows the real mechanism
- [ ] No unverified capability is advertised
- [ ] Signing keys are not committed

### Manual scenarios for the user

1. Install the signed release artifact on a clean device and complete the main flow.
2. Watch the demo video and confirm it matches the app.
3. Read the Data Safety answers and confirm each is true.
4. Confirm the privacy policy URL resolves.

### Definition of Done

Signed artifact installs and works, all declarations complete and accurate, video recorded, policy published.

### Risks

| Risk | Response |
|---|---|
| Declaration contradicts behavior | the mapping check exists to catch this before review |
| Review rejects the Accessibility use | the package was designed for this in phase 06 |

### Documents to update

`docs/store/play/*`

### Merge into `dev` when

Signed artifact verified, declarations complete and mapped.

---

## Phase 24 — Android Closed Beta

**Status:** not started

**Goal.** Put the Android build in front of real users and collect structured feedback. This is also the **first contact with physical hardware and OEM firmware**, because development uses an emulator only.

**Depends on.** Phase 23.

**Branch.** `phase/24-android-beta`

**Out of scope.** iOS. New features. Public release.

### Tasks

- [ ] Configure the closed testing track
- [ ] Recruit testers on at least Samsung and Xiaomi or Redmi hardware, plus one Pixel or AOSP-like device
- [ ] Execute the OEM behaviour list handed over by phase 22
- [ ] Confirm on each brand: the service survives a reboot, survives background restriction, and protection health reports the truth when it does not
- [ ] Write the tester instructions and the feedback form
- [ ] Instrument the early metrics locally, with no third-party SDK
- [ ] Collect onboarding completion, permission grant, and first-rule rates
- [ ] Collect intervention counts, refusal rate, and bypass rate
- [ ] Collect permission revocations and blocking failures
- [ ] Triage every report into defect, limitation, or product feedback

**Expected result.** A structured feedback set with metrics from real usage.

### Automated checks

- [ ] Metrics are computed locally and no network call exists — agent greps for network usage
- [ ] Metric arithmetic tested — agent runs
- [ ] Beta build is the same commit as the verified release candidate — agent verifies the hash

### Agent checklist

- [ ] No analytics SDK is introduced
- [ ] No user content leaves the device
- [ ] Tester instructions state clearly what is being tested
- [ ] Every report is triaged, none silently dropped
- [ ] The phase 22 OEM list is fully executed, and every item is marked confirmed or failed
- [ ] Any OEM defect found is recorded as a defect, never as an acceptable quirk

### Manual scenarios for the user

1. Join the closed track yourself and confirm the install path works.
2. Read the tester instructions as a newcomer and judge their clarity.
3. Review the triaged report list and confirm the categories make sense.

### Definition of Done

Testers on at least three brands, metrics collected, every report triaged.

### Risks

| Risk | Response |
|---|---|
| Too few testers to be meaningful | state the sample size honestly; do not generalize |
| Privacy concern from testers | local-only metrics are verifiable in the build |

### Documents to update

`docs/BETA_REPORT.md`

### Merge into `dev` when

Feedback collected and triaged, metrics recorded.

---

## Phase 25 — Android Beta Feedback and Defect Resolution

**Status:** not started

**Goal.** Fix what the Android beta exposed and decide honestly what ships as a stated limitation.

**Depends on.** Phase 24.

**Branch.** `phase/25-android-beta-defects`

**Out of scope.** New features. Scope growth from feature requests. iOS.

This is where OEM reality arrives. Every defect that emulator testing could not have found lands in this phase, and the honest outcome for some of them is a documented limitation rather than a fix.

### Tasks

- [ ] Rank every triaged defect by severity
- [ ] Fix every critical and major defect
- [ ] Add a regression test for every fix
- [ ] Separate OEM-specific defects from general ones, and say which manufacturer each affects
- [ ] Convert unfixable issues into documented limitations with honest copy
- [ ] Correct the OEM background-restriction guidance from what testers actually had to do
- [ ] Re-run the full manual suite on the emulator images after fixes
- [ ] Update product copy where the beta showed it misleads
- [ ] Record feature requests in the post-MVP backlog without implementing them

**Expected result.** An Android build with no critical or major defects and no undocumented limitation.

### Automated checks

- [ ] Full Android suite green — agent runs
- [ ] Every fix has a regression test — agent verifies the mapping
- [ ] Fixture suite still passes — agent runs
- [ ] No new dependency was added — agent diffs the dependency files

### Agent checklist

- [ ] No feature request is implemented under the guise of a fix
- [ ] Every limitation is stated in the UI or the store listing, not hidden
- [ ] An OEM defect is recorded as a defect, never softened into a quirk
- [ ] The protection health screen surfaces every failure mode the beta found

### Manual scenarios for the user

1. Re-run the main flows after the fixes.
2. Confirm every known limitation is stated somewhere the user will see it.
3. Confirm no beta feature request silently entered the build.
4. Read the corrected OEM guidance and confirm it matches what testers reported.

### Definition of Done

No critical or major defects open, regression tests added, limitations documented, OEM guidance corrected from real reports.

### Risks

| Risk | Response |
|---|---|
| Scope growth from beta requests | requests go to the post-MVP backlog, not into this phase |
| An OEM defect has no fix | document it plainly in the listing and in protection health; do not ship a silent failure |
| Emulator suite passes while devices fail | the beta is the only device evidence this project has; trust it over the emulator |

### Documents to update

`docs/BETA_REPORT_ANDROID.md`, `docs/PRODUCT.md` if copy changes, `docs/COMPATIBILITY.md`, post-MVP backlog in this file

### Merge into `dev` when

Android suite green, no critical or major defects open.

---

## Phase 26 — Android Release Candidate and Play Submission

**Status:** not started

**Goal.** Cut the Android release, merge to `main`, and submit to Google Play.

**Depends on.** Phase 25.

**Branch.** `phase/26-android-release-candidate`

**Out of scope.** Post-release features. Monetization. iOS, which reaches its own release in phase 49.

This is the first time `main` receives anything since the documentation baseline. The tag names the platform, because iOS will tag the same branch later: `android-v1.0.0`, not `v1.0.0`.

### Tasks

- [ ] Freeze `dev` and cut the release candidate
- [ ] Set the version name and build number
- [ ] Produce the final signed Android artifact
- [ ] Re-run the full manual suite against the exact release build, not an approximation
- [ ] Verify the store declarations still match the final binary
- [ ] Re-run the `docs/store/play/DATA_INVENTORY.md` traceability against the release sources
- [ ] Write the release notes
- [ ] Tag `android-v1.0.0` and merge to `main`
- [ ] Submit to Google Play
- [ ] Record the submission date and the review outcome

**Expected result.** A tagged Android release on `main`, submitted to Google Play, with declarations that match the binary.

### Automated checks

- [ ] `./gradlew bundleRelease` succeeds and the artifact is signed — agent runs
- [ ] Full Android suite green against the release configuration — agent runs
- [ ] The release build logs no package name and no user content — agent greps the release sources and inspects the build
- [ ] Every store declaration still maps to a real behavior — agent re-runs the mapping check
- [ ] Version name and build number are consistent across the build files and the documents — agent verifies

### Agent checklist

- [ ] The tag points at the exact commit submitted to Play
- [ ] The tag is platform-scoped, so the iOS release can tag the same repository later
- [ ] Release notes claim nothing unverified, and say plainly which manufacturers were tested
- [ ] `main` receives the merge only after the artifact is verified
- [ ] No secret is present in any committed file

### Manual scenarios for the user

1. Install the exact release artifact on a clean device and complete every main flow.
2. Read the release notes and confirm every claim is true.
3. Confirm the git tag matches what was submitted.
4. Confirm the listing does not promise behaviour the beta did not demonstrate.

### Definition of Done

Artifact verified against the release build, declarations re-checked, `android-v1.0.0` tagged, `main` updated, submission made and recorded.

### Risks

| Risk | Response |
|---|---|
| Play review rejects the submission | the policy package was prepared in phases 06 and 23 precisely for this; a rejection produces a new phase, not a patch |
| A last-minute change breaks something | the full suite runs against the exact release build |
| `main` diverges once iOS also releases | platform-scoped tags and one shared `main`; iOS merges through `dev` like everything else |

### Documents to update

`docs/RELEASE_NOTES.md`, `README.md` status section, `docs/store/play/*`

### Merge into `dev` and then `main` when

Artifact verified, declarations accurate, owner has signed off.

---



# Apple Entitlement

The iOS track opens with paperwork, not code, so a refusal costs no Swift.

## Phase 27 — Apple Family Controls Entitlement Request

**Status:** not started

**Goal.** File the Family Controls distribution entitlement request with Apple, before any Swift is written, so no iOS work proceeds on the assumption of an approval that may not come.

**Depends on.** Nothing technical. It needs an Apple Developer account and a description of the product.

**Branch.** `phase/27-apple-entitlement-request`

**Out of scope.** Any Swift. Any Xcode project. Any iOS design. This phase writes a request and files it.

This is the first iOS phase, and it is deliberately first: everything after it becomes wasted work if Apple refuses. It runs after Android has shipped, which was the owner's decision.

The cost of that decision, recorded here so it is never a surprise: **Apple's answer will not be known until the iOS track begins.** The request can take weeks and it can be refused. A refusal discovered here costs nothing already built, because no iOS work will have started — but it also means the project spends the entire Android cycle without knowing whether iOS is possible at all. That is an accepted trade, not an oversight. Anyone who wants the answer sooner can run this phase at any time: nothing in the Android track depends on it, and it can be started in parallel with any Android phase.

### Tasks

- [ ] Confirm an Apple Developer account exists, or record that obtaining one is the first blocker
- [ ] Reserve the bundle identifier and create the App ID
- [ ] Write the entitlement request describing what BlockSocial does with Family Controls and why
- [ ] State in the request that the app is for the device owner's own use, never for controlling another person's device
- [ ] Reuse the mechanism description already cross-read in `docs/store/play/`, so both stores are told the same thing
- [ ] Submit the request
- [ ] Record the submission date, the exact text submitted, and the reference number
- [ ] Set a reminder to chase it if no answer arrives

**Expected result.** A submitted request with its date and text recorded, or a written statement of what blocks submission.

### Automated checks

- [ ] The submitted text contains no claim absent from `docs/PRODUCT.md` — agent cross-reads
- [ ] The request does not describe BlockSocial as parental control — agent greps the submitted text
- [ ] The mechanism description matches the one in `docs/store/play/ACCESSIBILITY_DECLARATION.md` in substance — agent compares
- [ ] The recorded date and reference are present, not left blank — agent verifies

### Agent checklist

- [ ] The request describes the real mechanism, in the same terms as the Play package
- [ ] It never claims the app assists users with disabilities
- [ ] The record states plainly that the entitlement is not granted until Apple says so
- [ ] No later phase treats a pending request as an approval

### Manual scenarios for the user

1. Read the request text before it is sent and confirm it describes the product you want to build.
2. Submit it, or have the teammate submit it from the developer account.
3. Record the answer here when it arrives, whatever it is.

### Definition of Done

Request submitted with date and text recorded, or the blocker to submitting it written down.

### Risks

| Risk | Response |
|---|---|
| Apple refuses the entitlement | risk `R-02`. No iOS work has been done yet, so nothing built is lost; the decision becomes whether iOS ships at all |
| The answer arrives slowly and phases 28 onward stall | this phase can be run early, in parallel with any Android phase, precisely to avoid that; it sits here only because iOS was deferred as a whole |
| No Apple Developer account yet | record it as the blocker; it costs money and time |

### Documents to update

`docs/ios/ENTITLEMENT_REQUEST.md`, `docs/TECHNICAL_SPECIFICATION.md` risk `R-02`

### Merge into `dev` when

The request is submitted and recorded, or the blocker is documented.

---


# iOS Validation



Every iOS phase ends with a handoff document in `docs/handoffs/HANDOFF_<phase>.md` written by the agent, containing commit hash, Xcode and iOS versions, affected targets, exact build steps, expected behavior, numbered test cases, required evidence, and known limitations. The teammate executes it and returns a report. The agent never claims an iOS phase works before that report arrives.



## Phase 28 — iOS Spike: Skeleton, Targets, and Signing

**Status:** not started

**Goal.** Get all five targets building and signing on the teammate's machine.

**Depends on.** Phases 00 and 27. Phase 27 filed the Family Controls entitlement request; its outcome is read here, not requested here.

**Branch.** `phase/28-ios-skeleton`

**Out of scope.** Any product feature. UI beyond a placeholder. Persistence. Requesting the entitlement, which phase 27 already did.

### Tasks

- [ ] Read the current entitlement status recorded in phase 27 and state it plainly before anything else
- [ ] Create the Xcode project with the main app and three extension targets
- [ ] Create the test target
- [ ] Configure the App Group across all targets
- [ ] Add the Family Controls capability to every target that needs it
- [ ] Write the handoff document with build steps and test cases
- [ ] Record the toolchain in `ios/BUILD_ENVIRONMENT.md` from the teammate's report

**Expected result.** All targets build and sign on a real device, or a precise blocker list, with the entitlement status stated rather than assumed.

### Automated checks

Agent-side, since the agent cannot build iOS:

- [ ] Project file references every target and the App Group consistently — agent inspects the project file
- [ ] No target is missing the capability it needs — agent checks each entitlements file
- [ ] Handoff document contains every required field — agent verifies against `AGENTS.md`

Teammate-side, listed in the handoff:

- [ ] `xcodebuild -scheme BlockSocialApp -destination 'generic/platform=iOS' build` succeeds
- [ ] Each extension target builds
- [ ] The app launches on a real iPhone
- [ ] Provisioning resolves for every target

### Agent checklist

- [ ] The entitlement status from phase 27 is stated at the top of the result, granted or not
- [ ] Bundle identifiers follow one consistent scheme
- [ ] App Group identifier is identical across targets
- [ ] Extensions contain no networking and no heavy dependencies
- [ ] Handoff test cases are numbered and unambiguous
- [ ] No provisioning profile or certificate is committed

### Manual scenarios for the teammate

The agent writes these into the handoff; the teammate executes them.

1. Open the project in Xcode and confirm five targets are present.
2. Build each target; record any signing error verbatim.
3. Run the app on a real iPhone and confirm it launches.
4. Confirm the App Group appears in every target's capabilities.
5. Return the Xcode version, iOS version, build log, and a screenshot.

### Definition of Done

Teammate reports all targets building and signing, or a precise blocker list; entitlement status from phase 27 recorded; `ios/BUILD_ENVIRONMENT.md` filled from the report.

### Risks

| Risk | Response |
|---|---|
| The entitlement was never granted | phase 27 should have surfaced this long ago; if it is still open here, stop and decide whether iOS proceeds at all before writing more Swift |
| Provisioning complexity across four App IDs | one target at a time; the handoff isolates the failing one |

### Documents to update

`docs/handoffs/HANDOFF_28.md`, `ios/BUILD_ENVIRONMENT.md`, `docs/spikes/SPIKE_I-01_RESULT.md`

### Merge into `dev` when

Teammate report received and recorded, entitlement status stated.

---

## Phase 29 — iOS Spike: Authorization and Picker

**Status:** not started

**Goal.** Prove `.individual` authorization and `FamilyActivityPicker` behave correctly across approve, deny, revoke, restart, and reinstall.

**Depends on.** Phase 28.

**Branch.** `phase/29-ios-authorization-picker`

**Out of scope.** Rules. Shields. Schedules. Real UI design.

### Tasks

- [ ] Implement `.individual` authorization with all three states handled
- [ ] Recheck authorization on launch and on foreground entry
- [ ] Present `FamilyActivityPicker` and store the selection locally
- [ ] Persist the selection to the App Group with a schema version
- [ ] Display the selection as a count, never as invented names
- [ ] Handle a selection lost after reinstall as a recovery state
- [ ] Write the handoff document with test cases

**Expected result.** Authorization and selection that survive restart, degrade honestly after revocation, and never fabricate application names.

### Automated checks

Agent-side:

- [ ] Selection encoding and decoding round-trips — unit test written for the teammate to run
- [ ] No code path attempts to read a bundle identifier from a token — agent greps
- [ ] Handoff document complete — agent verifies

Teammate-side:

- [ ] Unit tests pass in Xcode
- [ ] App builds and runs on a real iPhone

### Agent checklist

- [ ] Tokens are never logged, transmitted, or persisted outside the App Group
- [ ] Denial leaves a usable, non-punishing path
- [ ] Revocation is detected on foreground entry, not only at launch
- [ ] The UI never implies the app can see which apps were chosen

### Manual scenarios for the teammate

1. First launch: approve authorization; confirm the state shown.
2. Fresh install: deny authorization; confirm the app stays usable and explains what stops working.
3. Approve, select seven applications, confirm the count displayed.
4. Force-quit and relaunch; confirm the selection persists.
5. Revoke Screen Time authorization in Settings; return to the app; confirm the state updates without a crash.
6. Reinstall the app; confirm the selection-lost recovery state appears.
7. Return screenshots of every state.

### Definition of Done

Teammate confirms all seven scenarios, no crash on revocation, no fabricated app names, evidence returned.

### Risks

| Risk | Response |
|---|---|
| Selection silently lost on reinstall | expected on iOS; must surface as a recovery state, not an error |
| Authorization state stale | recheck on foreground entry |

### Documents to update

`docs/handoffs/HANDOFF_08.md`, `docs/spikes/SPIKE_I-02_RESULT.md`

### Merge into `dev` when

Teammate report received, all scenarios pass, decision recorded.

---

## Phase 30 — iOS Spike: Schedule and Shield

**Status:** not started

**Goal.** Prove a shield appears and disappears on schedule on a real iPhone without the main app running.

**Depends on.** Phase 29.

**Branch.** `phase/30-ios-schedule-shield`

**Out of scope.** Multiple rules. Capacity handling. Bypass. Custom shield actions.

### Tasks

- [ ] Register one daily `DeviceActivitySchedule`
- [ ] Apply the shield at interval start from the monitor extension
- [ ] Remove the shield at interval end
- [ ] Provide custom shield title and subtitle through `ShieldConfiguration`
- [ ] Make schedule registration idempotent
- [ ] Implement stop-monitoring and confirm no stale shield remains
- [ ] Write the handoff document with test cases

**Expected result.** A shield that appears and disappears on time, driven entirely by the system.

### Automated checks

Agent-side:

- [ ] Registration path is idempotent by construction — agent documents the reasoning and the test
- [ ] Extension contains no networking and no heavy dependency — agent inspects
- [ ] Handoff complete — agent verifies

Teammate-side:

- [ ] All targets build
- [ ] Unit tests pass

### Agent checklist

- [ ] The App Group payload is versioned
- [ ] Repeated registration does not duplicate activities
- [ ] Shield copy contains no hardcoded duration promise
- [ ] Stopping monitoring always clears the store

### Manual scenarios for the teammate

1. Set a schedule starting two minutes ahead; confirm the shield appears on time.
2. Confirm the shield's custom text is shown.
3. Force-quit the main app; confirm the shield still applies.
4. Lock the phone across the interval start; confirm behavior.
5. Reboot the device mid-interval; confirm the shield persists.
6. Change the device time zone; record what happens.
7. Overwrite the schedule; confirm no duplicate activity.
8. Stop monitoring; confirm no shield remains.
9. Return a screen recording of the shield appearing.

### Definition of Done

Teammate confirms the shield appears and disappears with the main app closed, no stale shield after stop, evidence returned.

### Risks

| Risk | Response |
|---|---|
| Interval start unreliable after reboot | record precisely; this shapes the product promise |
| Time-zone change breaks the schedule | record behavior; handle in phase 38 |

### Documents to update

`docs/handoffs/HANDOFF_09.md`, `docs/spikes/SPIKE_I-03_RESULT.md`

### Merge into `dev` when

Teammate report received, shield verified on a real device, decision recorded.

---

## Phase 31 — iOS Spike: Shield Actions

**Status:** not started

**Goal.** Prove shield buttons work, change the settings store, and record events, without private APIs and without launching the parent app.

**Depends on.** Phase 30.

**Branch.** `phase/31-ios-shield-actions`

**Out of scope.** Timed bypass. Daily limits. Statistics UI.

### Tasks

- [ ] Implement the `ShieldAction` extension with primary and secondary buttons
- [ ] Handle `.close` and `.defer` responses
- [ ] Modify `ManagedSettingsStore` from the action
- [ ] Append an event to the App Group queue
- [ ] Drain the queue into SwiftData when the main app next opens
- [ ] Write the handoff document with test cases

**Expected result.** Shield buttons that produce the intended outcome and leave a recorded event.

### Automated checks

Agent-side:

- [ ] No private API is referenced — agent greps for known private symbols
- [ ] No code path attempts to launch the containing app — agent greps
- [ ] Queue encoding round-trips — unit test written
- [ ] Handoff complete — agent verifies

Teammate-side:

- [ ] Targets build; unit tests pass

### Agent checklist

- [ ] Extension stays minimal and allocates little
- [ ] Event queue is append-only and versioned
- [ ] Draining is idempotent; a replayed queue does not duplicate events
- [ ] No user content is written to the queue

### Manual scenarios for the teammate

1. Tap the primary shield button; confirm the intended outcome.
2. Tap the secondary button; confirm the intended outcome.
3. Reopen the target app immediately after each action.
4. Open the main app and confirm both events appear.
5. Repeat five times and confirm no duplicated or lost events.
6. Return screenshots of the event list.

### Definition of Done

Teammate confirms both buttons work and events arrive intact, no private APIs, evidence returned.

### Risks

| Risk | Response |
|---|---|
| Extension terminated before writing the event | keep the write minimal; measure loss rate over five runs |
| Action cannot achieve the intended outcome | record precisely; the UX must not depend on it |

### Documents to update

`docs/handoffs/HANDOFF_10.md`, `docs/spikes/SPIKE_I-04_RESULT.md`

### Merge into `dev` when

Teammate report received, both actions verified, decision recorded.

---

## Phase 32 — iOS Spike: Usage-Threshold Bypass and Limits

**Status:** not started

**Goal.** Prove that removing a shield, registering a usage threshold, and reapplying the shield when it fires is predictable across ten consecutive runs. This mechanism carries both the bypass and daily limits.

**Depends on.** Phase 31.

**Branch.** `phase/32-ios-usage-threshold`

**Out of scope.** Final bypass UI. The daily-limit feature. Statistics.

### Tasks

- [ ] Remove the shield from the shield action
- [ ] Register a five-minute usage threshold for the target application
- [ ] Reapply the shield when the threshold event fires
- [ ] Measure actual elapsed usage against the threshold
- [ ] Run the full cycle ten consecutive times and record every result
- [ ] If unreliable, implement and measure the fifteen-minute wall-clock fallback
- [ ] Record which variant the product will ship
- [ ] Write the handoff document with test cases

**Expected result.** A recorded reliability figure over ten runs and a decision on the shipping bypass semantics.

### Automated checks

Agent-side:

- [ ] No `Timer`, `sleep`, or long-lived extension process is used — agent greps
- [ ] Duration is a configurable value, never a hardcoded literal in copy — agent greps
- [ ] Handoff complete with a results table for ten runs — agent verifies

Teammate-side:

- [ ] Targets build; unit tests pass

### Agent checklist

- [ ] The fallback path is implemented, not merely described
- [ ] Copy references the duration as a variable
- [ ] Threshold registration is idempotent
- [ ] The result table records each run individually, not an average

### Manual scenarios for the teammate

1. Trigger a bypass and use the app continuously for five minutes; record when the shield returns.
2. Repeat with pauses in the middle.
3. Repeat while switching to other apps.
4. Repeat with the screen turning off.
5. Repeat after force-quitting the target app.
6. Trigger a second bypass immediately after the first.
7. Complete ten full runs and fill the results table.
8. Return a screen recording of at least two complete runs.

### Definition of Done

Ten runs recorded individually, a shipping variant chosen, `docs/PRODUCT.md` question `Q-04` answered, evidence returned.

### Risks

| Risk | Response |
|---|---|
| Threshold fires late or not at all | fall back to fifteen-minute wall-clock; the product promise changes accordingly |
| Behavior differs between iOS versions | record the tested version; retest in phase 45 |

### Documents to update

`docs/handoffs/HANDOFF_11.md`, `docs/spikes/SPIKE_I-05_RESULT.md`, `docs/PRODUCT.md` question `Q-04`, `docs/ARCHITECTURE.md` bypass section

### Merge into `dev` when

Ten runs recorded, decision made, documents updated.

---

## Phase 33 — iOS Spike: Capacity Limits

**Status:** not started

**Goal.** Prove that Apple's twenty-activity and fifty-token limits are detected before saving and never leave a stale shield.

**Depends on.** Phase 30.

**Branch.** `phase/33-ios-capacity`

**Out of scope.** Final rule-editor UI. Bypass. Statistics.

### Tasks

- [ ] Register activities up to and past twenty; record the failure mode
- [ ] Add tokens up to and past fifty; record the failure mode
- [ ] Implement grouping of identical intervals
- [ ] Implement capacity validation before saving
- [ ] Implement safe replacement of an existing activity
- [ ] Implement stale-shield cleanup
- [ ] Draft the user-facing capacity error copy
- [ ] Write the handoff document with test cases

**Expected result.** Capacity errors surfaced before saving, with copy that names what to remove, and no stale shield in any case.

### Automated checks

Agent-side:

- [ ] Validation runs before any write to the store — agent inspects the call order
- [ ] No path silently truncates a selection — agent greps
- [ ] Unit tests cover the boundary at nineteen, twenty, and twenty-one activities — written for the teammate

Teammate-side:

- [ ] Targets build; unit tests pass

### Agent checklist

- [ ] Error copy names the specific rules to disable
- [ ] Grouping reduces activity count measurably; the reduction is recorded
- [ ] Replacement never leaves an orphaned activity
- [ ] Daily limits are counted against the same budget

### Manual scenarios for the teammate

1. Create rules until the limit is reached; confirm the error appears before saving.
2. Confirm the error names which rules to disable.
3. Confirm nothing was saved when the error appeared.
4. Disable one rule and confirm the new one saves.
5. Replace an existing rule and confirm no duplicate activity.
6. Force a stale shield and confirm cleanup removes it.
7. Return screenshots of the error state.

### Definition of Done

Boundaries measured, validation happens before saving, no stale shield observed, copy drafted, evidence returned.

### Risks

| Risk | Response |
|---|---|
| Real limits differ from documentation | record measured values; they override the specification |
| Grouping insufficient for realistic rule sets | introduce a product-level rule limit and state it in the UI |

### Documents to update

`docs/handoffs/HANDOFF_12.md`, `docs/spikes/SPIKE_I-06_RESULT.md`, `docs/TECHNICAL_SPECIFICATION.md` section 4

### Merge into `dev` when

Teammate report received, boundaries recorded, no stale shield.

---



# iOS Contract and Gate

## Phase 34 — iOS Contract Conformance and Validation Gate

**Status:** not started

**Goal.** Prove the shared fixtures are satisfiable on iOS, convert the iOS spike evidence into decisions, and decide whether iOS implementation proceeds.

**Depends on.** Phases 07 and 28 through 33.

**Branch.** `phase/34-ios-validation-gate`

**Out of scope.** Any iOS product implementation. Any change to Android behaviour that is not forced by a fixture failure.

This phase carries the debt created by building Android first. The fixtures in phase 07 were written from Android evidence, and this is where iOS gets its say. A fixture iOS cannot satisfy is not an iOS problem to work around: it is a contract error, and fixing it changes both platforms.

### Tasks

- [ ] Run the shared fixtures against the iOS domain implementation from the spikes
- [ ] Start with the cases phase 07 marked as at risk from Apple limits
- [ ] Record every fixture that fails, with the Apple limit or behaviour that causes it
- [ ] For each failure decide: change the fixture and Android with it, or accept a documented per-platform difference
- [ ] Collect every iOS spike result and its decision into one summary table
- [ ] Answer: is the Family Controls entitlement granted?
- [ ] Answer: is the iOS bypass verified, or is the fallback confirmed?
- [ ] Answer: do iOS schedules and the App Group work on a real device?
- [ ] Answer: are capacity limits detected before saving?
- [ ] Answer: is the Windows to Mac handoff reproducible?
- [ ] Update `docs/ARCHITECTURE.md` where the platforms genuinely diverge
- [ ] Record a `GO`, `CHANGE`, or `STOP` verdict for iOS
- [ ] Re-scope phases 35 to 49 against what was learned

**Expected result.** A fixture conformance report, an iOS gate document with evidence-backed answers, and an iOS verdict.

### Automated checks

- [ ] Every shared fixture case has a recorded iOS result — agent verifies the mapping is complete
- [ ] Every fixture change made here is applied to Android in the same phase — agent runs the Android suite
- [ ] Every answer cites a spike result file that exists — agent verifies each path resolves
- [ ] No documented platform difference contradicts `docs/PRODUCT.md` — agent cross-reads

### Agent checklist

- [ ] A fixture failure is treated as a contract question, never as an iOS workaround
- [ ] Any change to a fixture is followed through to Android before this phase closes
- [ ] A pending entitlement is reported as pending, never as granted
- [ ] Every accepted platform difference is written where a user would see its effect, not only in an internal document

### Manual scenarios for the user

1. Read the conformance report and confirm you agree with each accept-the-difference decision.
2. Confirm any fixture that changed was also fixed on Android.
3. Decide and record the iOS verdict.

### Definition of Done

Every fixture has an iOS result, every failure resolved by a written decision, Android still green against the final fixtures, iOS verdict recorded, phases 35 to 49 re-scoped.

### Risks

| Risk | Response |
|---|---|
| Many fixtures fail on iOS | this is the cost of writing the contract from one platform; budget for Android rework in this phase rather than pretending it will not happen |
| A difference is accepted silently | every accepted difference must appear in product copy or the store listing, and the check is manual scenario three |
| The entitlement is still not granted | `STOP` on iOS is a legitimate verdict; Android already shipped and does not wait |

### Documents to update

`docs/GATE_IOS.md`, `docs/ARCHITECTURE.md`, `docs/PRODUCT.md`, `shared/fixtures/*`, `docs/PLAN.md`

### Merge into `dev` when

Conformance report complete, Android green against the final fixtures, iOS verdict recorded.

---



# iOS MVP



Every phase below produces a handoff document and a teammate test checklist written by the agent. No iOS phase is done before the teammate's report arrives.



## Phase 35 — iOS: Production Skeleton

**Status:** not started

**Goal.** Turn the validated spike project into the production project structure with tokens and shared infrastructure.

**Depends on.** Phase 34.

**Branch.** `phase/35-ios-skeleton`

**Out of scope.** Features. Screens beyond a token preview.

### Tasks

- [ ] Create the production Xcode project with all five targets
- [ ] Configure the App Group and versioned payload types
- [ ] Implement design tokens for light and dark from the design analysis
- [ ] Implement the type scale mapped to Dynamic Type
- [ ] Add a token preview screen
- [ ] Configure the test target and the fixture loader
- [ ] Write the handoff document

**Expected result.** A production project that builds on the teammate's machine and renders the tokens correctly.

### Automated checks

Agent-side:

- [ ] Every target references the same App Group — agent inspects
- [ ] Token values match the documented conversions — agent verifies against the design analysis
- [ ] Handoff complete — agent verifies

Teammate-side:

- [ ] All targets build
- [ ] Unit tests pass
- [ ] Token preview renders in both themes on the emulator

### Agent checklist

- [ ] Extensions carry no unnecessary dependency
- [ ] Payload types are versioned from the start
- [ ] No color is hardcoded outside the token layer

### Manual scenarios for the teammate

1. Build every target and report any error verbatim.
2. Run the app and open the token preview.
3. Switch between light and dark and confirm both look intentional.
4. Set the largest Dynamic Type size and confirm the scale holds.
5. Return screenshots of both themes.

### Definition of Done

Teammate confirms all targets build and tokens render correctly in both themes.

### Risks

| Risk | Response |
|---|---|
| Token conversion differs from Android | both derive from the same documented source; compared in phase 34 |

### Documents to update

`docs/handoffs/HANDOFF_29.md`, `ios/BUILD_ENVIRONMENT.md`

### Merge into `dev` when

Teammate report received and positive.

---

## Phase 36 — iOS: Domain and Persistence

**Status:** not started

**Goal.** Implement the Swift domain and rule evaluator against the same shared fixtures, plus SwiftData persistence.

**Depends on.** Phase 35.

**Branch.** `phase/36-ios-domain-persistence`

**Out of scope.** Screen Time integration. UI.

### Tasks

- [ ] Implement the domain model matching the specification
- [ ] Implement the schedule evaluator including overnight intervals
- [ ] Implement bypass and daily-limit evaluation
- [ ] Implement rule priority identical to Android
- [ ] Load and run every shared fixture case as a test
- [ ] Implement SwiftData models for rules, history, statistics, permission snapshots
- [ ] Implement versioned App Group payload encoding and decoding
- [ ] Write the handoff document

**Expected result.** A Swift domain that produces identical results to Android for every fixture case.

### Automated checks

Agent-side:

- [ ] Fixture count referenced in the test file equals the count in `shared/fixtures/` — agent verifies
- [ ] Domain code has no Screen Time or UI dependency — agent inspects imports
- [ ] Handoff complete — agent verifies

Teammate-side:

- [ ] All unit tests pass, with the fixture case count reported
- [ ] Targets build

### Agent checklist

- [ ] No fixture case is skipped
- [ ] Clock is injected, never read statically
- [ ] Payload encoding round-trips with a version field
- [ ] Priority order matches the Android implementation exactly

### Manual scenarios for the teammate

1. Run the unit test suite and report the passing case count.
2. Confirm the count matches the number in `shared/fixtures/`.
3. Report any failing case with its identifier.

### Definition of Done

Every fixture case passes on the teammate's machine, and the count matches Android's.

### Risks

| Risk | Response |
|---|---|
| Divergence from Android semantics | the shared fixtures are the arbiter; a mismatch blocks the phase |

### Documents to update

`docs/handoffs/HANDOFF_30.md`

### Merge into `dev` when

Teammate reports all fixture cases passing.

---

## Phase 37 — iOS: Authorization and Picker

**Status:** not started

**Goal.** Ship the production authorization flow and application selection.

**Depends on.** Phase 36.

**Branch.** `phase/37-ios-authorization-picker`

**Out of scope.** Rules. Shields. Statistics.

### Tasks

- [ ] Implement the authorization explanation screen before the system prompt
- [ ] Implement `.individual` authorization with all states handled
- [ ] Recheck status on launch and foreground entry
- [ ] Present the picker and persist the selection to the App Group
- [ ] Display the selection as a count with an honest explanation
- [ ] Implement the reselection recovery flow after reinstall
- [ ] Write the handoff document

**Expected result.** A production selection flow that never fabricates application names and recovers from revocation.

### Automated checks

Agent-side:

- [ ] No token is logged or transmitted — agent greps
- [ ] Selection encoding round-trip test written — agent verifies
- [ ] Handoff complete — agent verifies

Teammate-side:

- [ ] Unit tests pass; targets build

### Agent checklist

- [ ] The explanation screen does not resemble the system prompt
- [ ] Denial leaves a usable path
- [ ] The count is the only claim made about the selection

### Manual scenarios for the teammate

1. Fresh install, approve authorization, select apps, confirm the count.
2. Fresh install, deny, confirm the app stays usable and explains the consequence.
3. Revoke in Settings, return, confirm the state updates without a crash.
4. Reinstall and confirm the reselection flow appears.
5. Return screenshots of every state.

### Definition of Done

Teammate confirms all five scenarios and returns screenshots.

### Risks

| Risk | Response |
|---|---|
| Users confused by the opaque selection | the explanation is verified in scenario one |

### Documents to update

`docs/handoffs/HANDOFF_31.md`

### Merge into `dev` when

Teammate report received and positive.

---

## Phase 38 — iOS: Schedule Compiler and Capacity

**Status:** not started

**Goal.** Ship schedule compilation with grouping, capacity validation, and safe replacement.

**Depends on.** Phase 37.

**Branch.** `phase/38-ios-schedule-compiler`

**Out of scope.** Shield appearance. Bypass. Daily limits.

### Tasks

- [ ] Compile rules into DeviceActivity schedules with grouping
- [ ] Validate capacity before any write to the store
- [ ] Implement the capacity error copy from phase 33
- [ ] Implement safe replacement of existing activities
- [ ] Implement stale-activity cleanup
- [ ] Make registration idempotent
- [ ] Handle time-zone change
- [ ] Write the handoff document

**Expected result.** Rules that register reliably and fail loudly, never silently, when capacity is exceeded.

### Automated checks

Agent-side:

- [ ] Validation precedes every store write — agent inspects call order
- [ ] Boundary unit tests at nineteen, twenty, and twenty-one activities written — agent verifies
- [ ] No truncation path exists — agent greps
- [ ] Handoff complete — agent verifies

Teammate-side:

- [ ] Unit tests pass; targets build

### Agent checklist

- [ ] Grouping reduction is measured and recorded
- [ ] The error names the specific rules to disable
- [ ] Replacement leaves no orphan
- [ ] Daily limits share the same budget accounting

### Manual scenarios for the teammate

1. Create rules until capacity is reached; confirm the error appears before saving.
2. Confirm nothing was saved when the error appeared.
3. Disable one rule and confirm the new one saves.
4. Change the device time zone and confirm schedules follow local time.
5. Return screenshots of the error state.

### Definition of Done

Teammate confirms all five scenarios, no silent truncation, no orphaned activity.

### Risks

| Risk | Response |
|---|---|
| Realistic rule sets exceed capacity | a stated product limit, surfaced honestly in the UI |

### Documents to update

`docs/handoffs/HANDOFF_32.md`, `docs/TECHNICAL_SPECIFICATION.md` section 4 if measured limits differ

### Merge into `dev` when

Teammate report received and positive.

---

## Phase 39 — iOS: Shield Configuration and Actions

**Status:** not started

**Goal.** Ship the production shield appearance, copy, and button behavior in both languages.

**Depends on.** Phase 38.

**Branch.** `phase/39-ios-shield`

**Out of scope.** Bypass timing. Daily limits.

### Tasks

- [ ] Implement `ShieldConfiguration` with production title, subtitle, and icon
- [ ] Write the shield copy in English and Russian, verified at the shortest possible length
- [ ] Implement `ShieldAction` for both buttons
- [ ] Record events to the App Group queue
- [ ] Drain the queue into SwiftData on next app open, idempotently
- [ ] Ensure no flow requires opening the app from the shield
- [ ] Write the handoff document

**Expected result.** A shield that reads well in both languages and records every decision.

### Automated checks

Agent-side:

- [ ] No private API referenced — agent greps
- [ ] No code path attempts to launch the containing app — agent greps
- [ ] Queue draining is idempotent — unit test written
- [ ] Russian copy fits the documented length budget — agent measures character counts
- [ ] Handoff complete — agent verifies

Teammate-side:

- [ ] Unit tests pass; targets build

### Agent checklist

- [ ] Copy contains no shaming language and no hardcoded duration
- [ ] Both languages verified at the shortest layout
- [ ] Events carry no user content

### Manual scenarios for the teammate

1. Trigger the shield and confirm the custom text appears.
2. Switch the device to Russian and confirm the shield reads correctly.
3. Tap each button and confirm the intended outcome.
4. Open the app and confirm both events appear in history.
5. Repeat five times and confirm no lost or duplicated events.
6. Return screenshots in both languages.

### Definition of Done

Teammate confirms all six scenarios in both languages, no event loss over five runs.

### Risks

| Risk | Response |
|---|---|
| Russian copy truncated by the system layout | measured before handoff, verified in scenario two |
| Extension terminated before writing an event | loss rate measured in scenario five |

### Documents to update

`docs/handoffs/HANDOFF_33.md`, `design/UI_UX_BRIEF.md` shield copy section

### Merge into `dev` when

Teammate report received, both languages verified.

---

## Phase 40 — iOS: Temporary Bypass

**Status:** not started

**Goal.** Ship the bypass in whichever form phase 32 proved, with honest copy about its duration.

**Depends on.** Phase 39.

**Branch.** `phase/40-ios-bypass`

**Out of scope.** Daily limits. Statistics.

### Tasks

- [ ] Implement the bypass using the variant chosen in phase 32
- [ ] Remove the shield and register the threshold or interval
- [ ] Reapply the shield when it fires
- [ ] Record `BYPASSED` with the actual granted duration
- [ ] Ensure the copy states the real duration, not an aspirational one
- [ ] Handle a second bypass immediately after the first
- [ ] Write the handoff document

**Expected result.** A bypass that behaves as measured, described honestly in the UI.

### Automated checks

Agent-side:

- [ ] Duration is read from configuration, never hardcoded in copy — agent greps
- [ ] No timer or sleep is used — agent greps
- [ ] Handoff includes a ten-run results table — agent verifies

Teammate-side:

- [ ] Unit tests pass; targets build

### Agent checklist

- [ ] The shipped duration matches the phase 32 decision exactly
- [ ] Copy never promises five minutes if the fallback shipped
- [ ] Repeated bypasses do not stack thresholds

### Manual scenarios for the teammate

1. Take a bypass and use the app; confirm the shield returns at the expected point.
2. Repeat with pauses and app switching.
3. Take a second bypass immediately after the first.
4. Reboot mid-bypass and confirm behavior.
5. Complete ten runs and fill the results table.
6. Return a screen recording of two runs.

### Definition of Done

Ten runs recorded, behavior matches the phase 32 measurement, copy matches reality.

### Risks

| Risk | Response |
|---|---|
| Production behavior differs from the spike | ten runs re-measured here, not assumed |

### Documents to update

`docs/handoffs/HANDOFF_34.md`, `docs/PRODUCT.md` if the shipped duration differs

### Merge into `dev` when

Teammate report received, ten runs recorded.

---

## Phase 41 — iOS: Daily Limits

**Status:** not started

**Goal.** Ship `DAILY_LIMIT` on iOS using the same threshold mechanism the bypass uses.

**Depends on.** Phase 40.

**Branch.** `phase/41-ios-daily-limits`

**Out of scope.** Charts. Weekly trends.

### Tasks

- [ ] Register a usage threshold per limited application
- [ ] Apply the shield when the threshold fires
- [ ] Reset at local midnight
- [ ] Count limit activities against the capacity budget
- [ ] Show remaining time as an estimate
- [ ] Handle authorization revoked mid-day
- [ ] Write the handoff document

**Expected result.** Daily limits that trigger through the system and are described as approximate.

### Automated checks

Agent-side:

- [ ] Daily-limit fixture cases referenced in the test suite — agent verifies
- [ ] Capacity accounting includes limit activities — agent inspects
- [ ] Handoff complete — agent verifies

Teammate-side:

- [ ] Unit tests pass; targets build

### Agent checklist

- [ ] Remaining time is labeled approximate everywhere
- [ ] Midnight reset uses local time
- [ ] A limit rule is rejected when capacity is full, with a clear error

### Manual scenarios for the teammate

1. Set a ten-minute limit and use the app; confirm the shield appears.
2. Confirm remaining time is shown as approximate.
3. Cross local midnight and confirm the reset.
4. Fill capacity and confirm a new limit rule is rejected clearly.
5. Revoke authorization mid-day and confirm no crash.
6. Return screenshots and a recording of step one.

### Definition of Done

Teammate confirms all six scenarios, fixtures pass, capacity accounting correct.

### Risks

| Risk | Response |
|---|---|
| Threshold accuracy poor | stated as approximate; margin recorded from real runs |
| Limits exhaust the activity budget | product limit stated in the UI |

### Documents to update

`docs/handoffs/HANDOFF_35.md`

### Merge into `dev` when

Teammate report received and positive.

---

## Phase 42 — iOS: Application Screens

**Status:** not started

**Goal.** Ship onboarding, rules, dashboard, history, and settings in the main app.

**Depends on.** Phase 41.

**Branch.** `phase/42-ios-screens`

**Out of scope.** New mechanisms. Store assets.

### Tasks

- [ ] Build onboarding matching the product explanation
- [ ] Build the rule editor with all three modes and capacity errors
- [ ] Build the dashboard with protection state most prominent
- [ ] Build history with equal weight for stayed and bypassed
- [ ] Build statistics with measured and estimated values distinguished
- [ ] Build settings including delete-all-data
- [ ] Implement every empty and degraded state
- [ ] Write the handoff document

**Expected result.** A complete main application matching the Android feature set within iOS constraints.

### Automated checks

Agent-side:

- [ ] Every screen has a corresponding view test written — agent verifies
- [ ] No metric is rendered as zero when unmeasurable — agent inspects
- [ ] Handoff complete — agent verifies

Teammate-side:

- [ ] Unit and view tests pass; targets build

### Agent checklist

- [ ] Statistics follow the same honesty rules as Android
- [ ] The streak rule matches the answered `Q-01`
- [ ] Delete-all-data is confirmed and complete
- [ ] No screen promises an unverified capability

### Manual scenarios for the teammate

1. Complete onboarding on a fresh install and reach a working rule.
2. Create one rule of each mode.
3. Produce stayed and bypassed events; confirm both appear in history.
4. Confirm the refusal rate matches a hand count.
5. Delete all data and confirm everything is cleared.
6. Return screenshots of every screen.

### Definition of Done

Teammate confirms all six scenarios and returns a full screenshot set.

### Risks

| Risk | Response |
|---|---|
| Feature parity gaps with Android | differences documented in `docs/ARCHITECTURE.md`, never hidden |

### Documents to update

`docs/handoffs/HANDOFF_36.md`, `docs/ARCHITECTURE.md` divergence table

### Merge into `dev` when

Teammate report received and positive.

---

## Phase 43 — iOS: Recovery and Reliability Hardening

**Status:** not started

**Goal.** Make the iOS application survive reboot, revocation, time-zone change, reinstall, and low memory.

**Depends on.** Phase 42.

**Branch.** `phase/43-ios-recovery`

**Out of scope.** New features. Visual changes.

### Tasks

- [ ] Verify schedules survive reboot
- [ ] Handle authorization revoked at any moment
- [ ] Handle stale tokens and payload version mismatch
- [ ] Handle time-zone change at runtime
- [ ] Verify extensions behave under memory pressure
- [ ] Ensure the App Group queue never loses events silently
- [ ] Add a regression test for every defect found
- [ ] Write the handoff document

**Expected result.** An application with no unhandled recovery path.

### Automated checks

Agent-side:

- [ ] Payload version mismatch test written — agent verifies
- [ ] Queue-drain idempotency test written — agent verifies
- [ ] Handoff complete — agent verifies

Teammate-side:

- [ ] All tests pass; targets build

### Agent checklist

- [ ] No recovery path ends in a dead end
- [ ] Version mismatch degrades gracefully, never crashes
- [ ] Every defect gets a test before its fix

### Manual scenarios for the teammate

1. Reboot and confirm shields still apply on schedule.
2. Revoke authorization mid-interval and confirm graceful degradation.
3. Change the time zone and confirm schedules follow local time.
4. Reinstall and confirm the reselection flow.
5. Run a memory-heavy app alongside and confirm extensions still fire.
6. Return logs and a recording of steps one and two.

### Definition of Done

Teammate confirms all six scenarios, regression tests added for every defect.

### Risks

| Risk | Response |
|---|---|
| Extension terminated under memory pressure | measured in scenario five; behavior documented honestly |

### Documents to update

`docs/handoffs/HANDOFF_37.md`

### Merge into `dev` when

Teammate report received, no unhandled recovery path.

---

## Phase 44 — iOS: Accessibility and Localization

**Status:** not started

**Goal.** Make every iOS surface usable with VoiceOver, at the largest Dynamic Type size, in both languages.

**Depends on.** Phase 43.

**Branch.** `phase/44-ios-a11y-localization`

**Out of scope.** New features. Additional languages.

### Tasks

- [ ] Add accessibility labels and traits to every element
- [ ] Verify focus order on every screen
- [ ] Support Dynamic Type to the largest size
- [ ] Honor Reduce Motion
- [ ] Ensure no meaning is carried by color alone
- [ ] Localize every string to Russian and English
- [ ] Verify shield copy in both languages at the shortest layout
- [ ] Compare token rendering against Android for consistency
- [ ] Write the handoff document

**Expected result.** A fully accessible, fully localized iOS application consistent with Android.

### Automated checks

Agent-side:

- [ ] No hardcoded user-facing string remains — agent greps
- [ ] Russian string lengths fit documented budgets — agent measures
- [ ] Handoff complete — agent verifies

Teammate-side:

- [ ] Accessibility audit in Xcode reports no critical issues
- [ ] Tests pass; targets build

### Agent checklist

- [ ] Shield copy verified in both languages
- [ ] Status meaning survives greyscale
- [ ] Reduce Motion respected everywhere

### Manual scenarios for the teammate

1. Navigate the whole app with VoiceOver only.
2. Set the largest Dynamic Type size and check every screen.
3. Switch the device to Russian and repeat the main flows.
4. Confirm the shield reads correctly in Russian.
5. Enable Reduce Motion and confirm nothing animates.
6. Return screenshots at the largest size in both languages.

### Definition of Done

Teammate confirms all six scenarios, audit clean, no hardcoded strings.

### Risks

| Risk | Response |
|---|---|
| Shield text truncated in Russian | measured before handoff, verified in scenario four |

### Documents to update

`docs/handoffs/HANDOFF_38.md`

### Merge into `dev` when

Teammate report received and positive.

---

## Phase 45 — iOS: Device and TestFlight Verification

**Status:** not started

**Goal.** Verify the complete application on a real device and through TestFlight, which is the first realistic distribution test.

**Depends on.** Phase 44.

**Branch.** `phase/45-ios-testflight-verification`

**Out of scope.** External beta testers, which is phase 47. Store listing.

### Tasks

- [ ] Prepare a TestFlight build with the distribution entitlement
- [ ] Verify every extension works in the TestFlight build, not only in development
- [ ] Run the full manual suite on a real iPhone
- [ ] Cover reboot, revoked authorization, time-zone change, and low memory
- [ ] Record every defect with iOS version and reproduction steps
- [ ] Add a regression test for every fix
- [ ] Write the handoff document and a compatibility summary

**Expected result.** A TestFlight build in which every mechanism works, with defects recorded and fixed.

### Automated checks

Agent-side:

- [ ] Every defect from the report has a corresponding fix and test — agent verifies the mapping
- [ ] Handoff complete — agent verifies

Teammate-side:

- [ ] Archive and upload succeed
- [ ] TestFlight build installs and runs
- [ ] Extensions fire in the TestFlight build
- [ ] Full test suite passes

### Agent checklist

- [ ] Development-only behavior is not mistaken for TestFlight behavior
- [ ] Entitlement status is confirmed, not assumed
- [ ] Every defect is recorded before it is fixed

### Manual scenarios for the teammate

1. Install from TestFlight on a clean device and complete onboarding.
2. Verify schedule blocking, bypass, and daily limits all work.
3. Reboot and confirm behavior persists.
4. Revoke authorization and confirm graceful degradation.
5. Report the iOS version and device model with every result.
6. Return a full screen recording of the main flow.

### Definition of Done

TestFlight build verified end to end on a real device, no critical or major defects open, compatibility summary written.

### Risks

| Risk | Response |
|---|---|
| Extensions behave differently under TestFlight | this phase exists precisely to catch that before submission |
| Entitlement still pending | phase blocked; the Android track continues independently |

### Documents to update

`docs/handoffs/HANDOFF_39.md`, `docs/COMPATIBILITY.md`

### Merge into `dev` when

TestFlight verification complete, no critical or major defects open.

---



# iOS Release

## Phase 46 — App Store Submission Preparation

**Status:** not started

**Goal.** Prepare everything the App Store requires, including entitlement documentation.

**Depends on.** Phase 45.

**Branch.** `phase/46-appstore-submission-prep`

**Out of scope.** Actual submission, which happens in phase 49. Android.

### Tasks

- [ ] Finalize the App Store listing text
- [ ] Produce screenshots for the required device sizes
- [ ] Complete the privacy nutrition labels
- [ ] Document the Family Controls entitlement usage for review
- [ ] Prepare reviewer notes explaining the Screen Time mechanism
- [ ] Verify the release configuration and archive
- [ ] Verify no debug logging of user content in the release build
- [ ] Write the handoff document for the teammate to archive and validate

**Expected result.** A submission-ready iOS package with reviewer notes that explain a mechanism reviewers may not expect.

### Automated checks

Agent-side:

- [ ] Every nutrition-label answer maps to an actual code behavior — agent lists the mapping
- [ ] No token or user content is logged in release — agent greps
- [ ] Handoff complete — agent verifies

Teammate-side:

- [ ] Archive validates against App Store Connect
- [ ] Release build runs on a real device

### Agent checklist

- [ ] Reviewer notes describe the real mechanism, including its limits
- [ ] No claim of five-minute behavior unless it was proven
- [ ] Entitlement documentation matches what Apple granted

### Manual scenarios for the teammate

1. Archive the app and validate it against App Store Connect.
2. Install the release build and complete the main flow.
3. Read the reviewer notes and confirm they match the app's behavior.
4. Return the validation output.

### Definition of Done

Archive validates, listing and labels complete and accurate, reviewer notes written.

### Risks

| Risk | Response |
|---|---|
| Reviewer unfamiliar with the Screen Time flow | reviewer notes exist for exactly this |
| Nutrition labels contradict behavior | mapping check before submission |

### Documents to update

`docs/store/appstore/*`, `docs/handoffs/HANDOFF_41.md`

### Merge into `dev` when

Archive validated by the teammate, declarations complete.

---

## Phase 47 — iOS TestFlight Beta

**Status:** not started

**Goal.** Run an external TestFlight beta and collect the same structured feedback.

**Depends on.** Phase 46.

**Branch.** `phase/47-ios-beta`

**Out of scope.** Android. New features. Public release.

### Tasks

- [ ] Configure external TestFlight testing
- [ ] Write the tester instructions and the feedback form
- [ ] Collect the same metric set as Android, computed locally
- [ ] Collect iOS-specific issues: authorization, shield behavior, bypass timing, capacity
- [ ] Record device models and iOS versions for every report
- [ ] Triage every report into defect, limitation, or product feedback
- [ ] Write the handoff document for the teammate to manage the build

**Expected result.** A structured iOS feedback set covering real devices and iOS versions.

### Automated checks

Agent-side:

- [ ] No network call for metrics — agent greps
- [ ] Metric definitions identical to Android — agent compares
- [ ] Handoff complete — agent verifies

Teammate-side:

- [ ] TestFlight build distributed successfully
- [ ] Build matches the verified commit

### Agent checklist

- [ ] Bypass timing feedback is collected specifically, since it is the least certain mechanism
- [ ] Capacity errors are reported with the rule count that triggered them
- [ ] No analytics SDK is introduced

### Manual scenarios for the teammate

1. Distribute the build and confirm testers can install it.
2. Collect at least one report per major iOS version in the test group.
3. Return the aggregated feedback with device and version for each entry.

### Definition of Done

External beta run, feedback collected across multiple devices and iOS versions, every report triaged.

### Risks

| Risk | Response |
|---|---|
| Beta review delays distribution | start the phase as soon as phase 46 completes |
| Bypass behaves differently in the wild | this is the main thing the beta exists to find |

### Documents to update

`docs/BETA_REPORT.md`, `docs/handoffs/HANDOFF_43.md`

### Merge into `dev` when

Feedback collected and triaged.

---

## Phase 48 — iOS Beta Feedback and Defect Resolution

**Status:** not started

**Goal.** Fix what the TestFlight beta exposed and decide honestly what ships as a stated limitation.

**Depends on.** Phase 47.

**Branch.** `phase/48-ios-beta-defects`

**Out of scope.** New features. Scope growth from feature requests. Android behaviour, unless a shared fixture forces it.

### Tasks

- [ ] Rank every triaged defect by severity
- [ ] Fix every critical and major defect
- [ ] Add a regression test for every fix
- [ ] Convert unfixable issues into documented limitations with honest copy
- [ ] Record where iOS genuinely behaves differently from the shipped Android app, and make sure the copy says so
- [ ] Re-run the shared fixtures on both platforms after fixes
- [ ] Re-run the full manual suite through the teammate against a TestFlight build
- [ ] Update product copy where the beta showed it misleads
- [ ] Record feature requests in the post-MVP backlog without implementing them

**Expected result.** An iOS build with no critical or major defects, and no difference from Android that a user could hit without being told.

### Automated checks

- [ ] Shared fixtures pass on both platforms — agent runs Android, teammate runs iOS
- [ ] Every fix has a regression test — agent verifies the mapping
- [ ] The Android suite is still green after any fixture change — agent runs
- [ ] No new dependency was added — agent diffs the dependency files

### Agent checklist

- [ ] No feature request is implemented under the guise of a fix
- [ ] Every limitation is stated in the UI or the store listing, not hidden
- [ ] Any fixture changed for iOS is followed through to Android in this phase
- [ ] Android is re-verified after any shared change, not assumed unaffected

### Manual scenarios for the user

1. Read the list of platform differences and confirm each one is stated where a user will meet it.
2. Confirm no beta feature request silently entered the build.
3. Confirm Android was re-run after any shared change.

### Definition of Done

No critical or major defects open, regression tests added, limitations documented, both platforms green against the shared fixtures.

### Risks

| Risk | Response |
|---|---|
| An iOS fix changes shared behaviour | Android runs again in this phase; a shared change is never one-sided |
| Differences accumulate quietly | they are listed explicitly and each is checked against the copy |
| Scope growth from beta requests | requests go to the post-MVP backlog |

### Documents to update

`docs/BETA_REPORT_IOS.md`, `docs/PRODUCT.md` if copy changes, `docs/ARCHITECTURE.md` divergence table, post-MVP backlog in this file

### Merge into `dev` when

Both suites green, no critical or major defects open.

---

## Phase 49 — iOS Release Candidate and App Store Submission

**Status:** not started

**Goal.** Cut the iOS release, merge to `main`, and submit to the App Store.

**Depends on.** Phase 48.

**Branch.** `phase/49-ios-release-candidate`

**Out of scope.** Post-release features. Monetization. Android, which released in phase 26.

Android is already live by this point. Anything shipped here must not change Android behaviour without a deliberate Android release of its own.

### Tasks

- [ ] Freeze `dev` and cut the release candidate
- [ ] Set the version name and build number, aligned with the shipped Android version where it makes sense
- [ ] Produce and validate the final iOS archive through the teammate
- [ ] Re-run the full manual suite through the teammate against the exact release build
- [ ] Verify the App Store declarations still match the final binary
- [ ] Confirm the shipped Android app is unaffected, and if it is affected, say what the Android release plan is
- [ ] Write the release notes
- [ ] Tag `ios-v1.0.0` and merge to `main`
- [ ] Submit to the App Store
- [ ] Record the submission date and the review outcome

**Expected result.** A tagged iOS release on `main`, submitted to the App Store, with declarations that match the binary and no unannounced change to the live Android app.

### Automated checks

- [ ] Android suite green at the release commit — agent runs
- [ ] Shared fixtures pass on both platforms — agent runs Android, teammate runs iOS
- [ ] Every store declaration still maps to a real behavior — agent re-runs the mapping check
- [ ] Version numbers are consistent across the build files and the documents — agent verifies
- [ ] iOS archive validates against App Store Connect — teammate runs

### Agent checklist

- [ ] The tag points at the exact commit submitted to the App Store
- [ ] The tag is platform-scoped and does not collide with the Android tag
- [ ] Release notes claim nothing unverified
- [ ] Any change to shared code since the Android release is listed, with its Android impact stated
- [ ] `main` receives the merge only after the archive is verified
- [ ] No secret, certificate or provisioning profile is present in any committed file

### Manual scenarios for the user

1. Confirm the teammate installed the exact archive and completed every main flow.
2. Read the release notes and confirm every claim is true.
3. Confirm the git tag matches what was submitted.
4. Confirm the live Android app is either untouched or has its own release scheduled.

### Definition of Done

Archive verified, declarations re-checked, `ios-v1.0.0` tagged, `main` updated, submission made and recorded, Android impact stated.

### Risks

| Risk | Response |
|---|---|
| App Store review rejects the submission | the policy work in phase 46 exists for this; a rejection produces a new phase, not a patch |
| Shared code changed since the Android release | listed explicitly, with an Android release scheduled if needed |
| Two platforms on one `main` | platform-scoped tags, and every release passes through `dev` first |

### Documents to update

`docs/RELEASE_NOTES.md`, `README.md` status section, `docs/store/*`

### Merge into `dev` and then `main` when

Archive verified, declarations accurate, owner and teammate have both signed off.

---

# Post-MVP Backlog

Not planned as phases. Each requires explicit approval and its own plan before any work starts.

| Item | Why it waits |
|---|---|
| Manual focus sessions | the priority model reserves the slot; the MVP has nothing in it |
| Monetization: payments, subscriptions | freemium is an untested assumption |
| Backend | the product is local-first by design |
| Accounts | no feature requires identity |
| Cloud synchronization | requires a backend and a privacy review |
| AI coach | outside the product's stated purpose |
| VPN or DNS filtering | rejected in favor of soft blocking |
| Selective blocking of Reels, Shorts, feeds, or tabs | not achievable reliably in a store-approved way |

Feature requests collected during beta are recorded here without commitment.

---

# Cross-Cutting Risks

| Risk | Where it is addressed |
|---|---|
| Google Play rejects the Accessibility use case | phases 06, 23, 26 |
| Apple withholds the Family Controls entitlement | phases 27, 28, 34 |
| The iOS bypass cannot be made predictable | phases 32, 40, 47 |
| The Android overlay is unstable on API 36 | phases 02, 13, 22 |
| App selection requires broad package visibility | phases 03, 15, 23 |
| Usage measurement too poor for daily limits | phases 05, 17, 41 |
| OEM background termination | unmeasurable on an emulator; surfaced by protection health in phase 19, confirmed by beta testers in phase 24 |
| The domain contract is written from Android evidence alone | phase 07 marks the cases at risk; phase 34 proves them on iOS and pays for any that fail |
| iOS verification cadence too slow | one mechanism per handoff, every iOS phase |
| Scope growth from beta feedback | phases 25 and 48 route requests to the backlog |
