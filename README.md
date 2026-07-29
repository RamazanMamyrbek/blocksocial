# BlockSocial

Native Android and iOS attention-management application. It interrupts the automatic launch of a social-media application, gives the user a deliberate choice, and records that choice locally.

> System timers measure time. BlockSocial measures decisions.

## Status

Documentation stage. No application source code yet, deliberately.

The central mechanisms are unverified: the Android accessibility overlay on API 36, Google Play acceptance of the Accessibility use case, the Apple Family Controls entitlement, and a predictable bypass on iOS. Feature implementation starts after those checks return results, listed in `docs/TECHNICAL_SPECIFICATION.md` section 8.

## Structure

```text
blocksocial/
├── AGENTS.md          how AI agents work in this repository
├── CLAUDE.md          Claude Code entry point
├── README.md
├── docs/
│   ├── PRODUCT.md                    what we build and why
│   ├── TECHNICAL_SPECIFICATION.md    what must be true
│   ├── ARCHITECTURE.md               how it is built
│   └── PLAN.md                       phases, order, per-phase checklists
└── design/
    ├── UI_UX_BRIEF.md                design input and constraints
    ├── DESIGN_EXPORT_ANALYSIS.md     first design pass, decisions and open items
    └── files/                        the design export itself
```

`docs/PLAN.md` holds 50 phases from git setup to two store submissions.

**Android runs first, all the way to the Play Store, then iOS begins.** Phases 00 to 06 are complete: the Android mechanisms are validated on an emulator and the Play policy package is written. Phases 08 to 27 build and ship Android. Phases 28 to 49 do the same for iOS afterwards.

The one exception is phase 07, which files the Apple Family Controls entitlement request early. It is paperwork, not development, but Apple's answer is slow and can be no, and that answer decides whether iOS happens at all.

What this ordering costs is written down rather than discovered later: the domain contract in phase 08 is frozen from Android evidence alone, and phase 34 is where iOS gets its say and where any resulting Android rework is paid for.

Android uses no physical device at any point. What that costs, and who carries the risk instead, is stated in `docs/TECHNICAL_SPECIFICATION.md` section 9.

## Branch Model

| Branch | Purpose |
|---|---|
| `main` | stable release states only; never receives direct work |
| `dev` | integration branch; every finished phase merges here |
| `phase/<number>-<short-name>` | one branch per phase, created from `dev` |

Branch from the current `dev`, never from `main`. One phase per branch. Merge into `dev` only when that phase's merge conditions in `docs/PLAN.md` are all true. Never rewrite published history.

## Working on a Phase

One phase is one session. Start it with a single instruction:

```text
Начни Phase 4
```

The agent creates the branch, does the work, runs the phase's automated checks, fills in the checkboxes in `docs/PLAN.md`, and reports. You then verify by hand using that phase's manual scenarios and approve the merge into `dev`. You never fill in a checkbox yourself.

If a phase cannot be finished, the agent stops, says where and why, and leaves the remaining checkboxes unchecked. A partially finished phase is never reported as done.

## Stack

| Platform | Stack | Built and tested by |
|---|---|---|
| Android | Kotlin, Jetpack Compose, Material 3, `minSdk 26`, `targetSdk 36` | agent, on an Android emulator |
| iOS | Swift, SwiftUI, iOS 17+, Screen Time frameworks | teammate, on macOS with a real iPhone |

No backend, no accounts, no synchronization. Every feature works offline.

## Working With Agents

Read `AGENTS.md` first. The user may write in Russian or English; everything stored in the repository stays in English.
