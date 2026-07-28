# Project Instructions

Canonical instruction source for AI agents working in this repository.

## Goal

BlockSocial is a native Android and iOS attention-management application. It interrupts the automatic launch of a social-media application, gives the user a deliberate choice between `Stay Focused` and `Open Temporarily`, and records that choice locally.

Whole applications are blocked, never individual feeds or tabs. Everything is local-first and works offline. The user always keeps control.

## Stack

- **Android:** Kotlin, Jetpack Compose, Material 3, `minSdk 26`, `targetSdk 36`, Room, DataStore, WorkManager, Hilt
- **iOS:** Swift, SwiftUI, iOS 17+, SwiftData, FamilyControls, ManagedSettings, DeviceActivity, App Groups
- **Backend:** none
- **Testing:** JUnit, Turbine, Compose UI Test on Android; XCTest or Swift Testing on iOS

Android is built and tested by the project owner on Windows. iOS is built and tested by a teammate on macOS with a real iPhone.

## Current State

The repository contains documentation only. No application source code exists yet, and that is deliberate.

The plan in `docs/PLAN.md` opens with technical validation: the checks in `docs/TECHNICAL_SPECIFICATION.md` section 8. Those checks are real work and do involve minimal, throwaway code. Feature implementation is what waits for their results.

Do not generate the Android or iOS application until a phase is explicitly approved.

## Rules

- Keep solutions simple. Do not over-engineer.
- Do not add unrelated features. Do not change unrelated files.
- Do not perform broad refactors as a side effect of a focused task.
- Find the root cause before fixing.
- Do not add dependencies without approval.
- Do not write code comments. Make the code self-explanatory through naming and structure. A comment is acceptable only when the user asks for one, or when a non-obvious platform constraint cannot be expressed in code, such as an OEM quirk or an Apple API limit. Never leave commented-out code, `TODO` notes, or comments restating what the code already says.
- Use explicit state models. Avoid premature abstractions.
- Make schedule registration idempotent. Handle permission revocation without crashing.
- Never block critical system applications.
- Avoid fake precision in statistics.
- Keep debug logging free of sensitive data.
- Include a regression test for every fixed bug.

### Language

The user may write in Russian or English. Agents may reply in the user's language.

All persistent repository content stays in English: documentation, code comments, task files, decision records, and commit messages. Never create translated duplicates or `_RU` and `_EN` suffixes.

### Product boundaries

Do not add without explicit approval: manual focus sessions, a backend, accounts, cloud synchronization, VPN or DNS filtering, website blocking, parental control of another device, selective blocking of Reels or feeds, an AI coach, social features, monetization, advertising, or third-party analytics.

Daily limits are inside the MVP. See `docs/PRODUCT.md` for the full scope boundary.

### Privacy

Accessibility access is used only for the declared blocking feature. Never read or store messages, screen text, keystrokes, or passwords. Never automate user actions inside third-party applications, prevent uninstalling BlockSocial, imitate system permission dialogs, collect an installed-app inventory beyond the approved catalog strategy, or transmit iOS activity tokens.

## Workflow

1. Read the relevant documents before writing anything: `docs/PRODUCT.md` for product questions, `docs/TECHNICAL_SPECIFICATION.md` for requirements and constraints, `docs/ARCHITECTURE.md` for structure.
2. State your assumptions.
3. Work on one approved phase at a time. Do not start the next phase without approval.
4. Modify the minimum number of files.
5. Add or update tests in the same change.
6. Run verification and quote the actual output.
7. Update the English documentation.
8. Report changed files, commands run, results, and remaining risks.

When documents conflict, report the conflict. Do not silently pick whichever is convenient.

Instruction precedence: the user's current request, then this file, then the documents under `docs/`, then existing code and tests, then reasonable engineering defaults.

### Plan phases

Every phase in `docs/PLAN.md` states six things:

1. **Goal** — what this phase makes true
2. **Tasks** — the concrete work, with an owner
3. **Testing** — how the result is checked
4. **Completion criteria** — the observable condition that ends the phase
5. **Risks** — what can go wrong here and the response
6. **Out of scope** — what this phase deliberately does not touch

A phase small enough to debug is a phase whose failure points to one cause. If a phase cannot state its completion criteria as something observable, it is too vague to start.

### iOS handoffs

iOS code is written without local Xcode access. Every handoff must include the commit hash, expected Xcode and iOS versions, affected targets, exact build steps, expected behavior, numbered test cases, required screenshots or recordings, log-export instructions, and known limitations.

Keep iOS changes small. One system mechanism per handoff. Never accumulate a queue of unverified Swift.

### Git

One focused task per branch or commit. Meaningful commit messages. No secrets, no generated caches, no unrelated formatting sweeps, no destructive history rewriting unless explicitly requested.

## Definition of Done

Two kinds of task, two different bars. Apply the one that matches the work.

### Implementation tasks and technical spikes

Complete only when:

- the code is implemented
- tests were actually run
- success criteria were checked
- Android behavior was verified by the owner on a real device
- iOS behavior was verified by the teammate when affected
- documentation was updated
- failures are reported honestly

Never claim completion without verification. Use precise language: "implemented but not built on macOS", "compiled on Android", "verified on a Samsung device", "awaiting entitlement", "check failed, fallback required".

Compilation is not verification. For iOS, nothing is confirmed until the teammate returns evidence from Xcode or a real device.

A spike additionally ends with a written result: environment, commit hash, steps, evidence, defects, and a `GO`, `CHANGE`, or `STOP` decision.

### Documentation tasks

Complete only when:

- the change covers exactly what was requested, with no unrelated edits
- affected documents agree with each other, and any conflict found is reported rather than silently resolved
- every cross-reference and file path resolves
- assumptions and open questions are stated explicitly, and nothing unverified is presented as confirmed
- the content is in English, with no duplicate or translated files
- the report lists the changed files

Do not claim that a documentation task verified behavior. Writing a requirement down is not evidence that it holds.
