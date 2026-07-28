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

`docs/PLAN.md` holds 46 phases from git setup to store submission. Its first phases are technical validation: they prove the mechanisms listed in `docs/TECHNICAL_SPECIFICATION.md` section 8 on real devices. Implementation phases follow and are re-scoped at the phase 14 gate.

## Working on a Phase

One phase is one session. Start it with a single instruction:

```text
Начни Phase 4
```

The agent creates the branch, does the work, runs the phase's automated checks, fills in the checkboxes in `docs/PLAN.md`, and reports. You then verify by hand using that phase's manual scenarios and approve the merge into `dev`. `main` receives only release states.

## Stack

| Platform | Stack | Built and tested by |
|---|---|---|
| Android | Kotlin, Jetpack Compose, Material 3, `minSdk 26`, `targetSdk 36` | project owner, on Windows |
| iOS | Swift, SwiftUI, iOS 17+, Screen Time frameworks | teammate, on macOS with a real iPhone |

No backend, no accounts, no synchronization. Every feature works offline.

## Working With Agents

Read `AGENTS.md` first. The user may write in Russian or English; everything stored in the repository stays in English.
