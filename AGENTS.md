# Project Instructions

Canonical instruction source for AI agents working in this repository.

## The product

**BlockSocial Lite** — an Android application that puts a daily time limit on individual applications and shows a full-screen warning when a limit is spent. It is the only product here. Everything lives under `lite/`.

Read `lite/docs/REQUIREMENTS.md` before changing behaviour. It is the specification: 43 numbered requirements, the scope boundary with a reason against every refusal, the deliberate trade-offs, and which test covers what. Where the code and that document disagree, one of the two is a defect — decide which, fix it, and say so.

`lite/README.md` is the short version and the record of what has actually been run.

## Stack

Kotlin, Jetpack Compose, Material 3, `minSdk 26`, `targetSdk 36`, DataStore, coroutines. One Gradle module. No database, no dependency-injection framework, no backend, no network permission at all.

Built on Windows. Verified on an Android emulator driven over `adb`, and on the owner's Poco X7 Pro (HyperOS, Android 15) when they connect it over USB.

## Rules

- Keep solutions simple. Do not over-engineer. This product's whole point is that it is small.
- Do not add unrelated features. Do not change unrelated files.
- Do not perform broad refactors as a side effect of a focused task.
- Find the root cause before fixing. Three failed fixes means the approach is wrong, not the fourth attempt.
- Do not add dependencies without approval.
- Do not write code comments. Make the code self-explanatory through naming and structure. A comment is acceptable only when the user asks for one, or when a non-obvious platform constraint cannot be expressed in code, such as an OEM quirk. Never leave commented-out code, `TODO` notes, or comments restating what the code already says.
- Use explicit state models. Avoid premature abstractions.
- Never block critical system applications.
- Avoid fake precision. A figure the product cannot measure honestly is not shown.
- Include a regression test for every fixed bug.

### Language

The user may write in Russian or English. Agents may reply in the user's language.

All persistent repository content stays in English: documentation, code comments, task files, decision records, and commit messages. User-visible strings are resources, in `values/` and `values-ru/`. Never create translated duplicates of documents or `_RU` and `_EN` suffixes.

### Product boundaries

`lite/docs/REQUIREMENTS.md` section 6 lists what is out of scope and why each was refused. Do not add any of it without explicit approval: history, statistics beyond today's two figures, schedules, always-on blocking, focus sessions, a timed pass after carrying on, accounts, cloud sync, a backend, VPN or DNS filtering, website blocking, parental control of another device, selective blocking of feeds inside an application, an AI coach, social features, monetization, advertising, or third-party analytics.

Notifications are capped at the single one described in R-43. Adding a second is a scope change.

### Privacy

Accessibility access is used only to notice which application came to the front and when. Never read or store screen text, messages, keystrokes, or passwords — `canRetrieveWindowContent` is `false`, and it stays false. Never automate user actions inside third-party applications, prevent uninstalling the app, imitate system permission dialogs, or collect an installed-app inventory beyond the shipped catalog.

## Workflow

1. Read `lite/docs/REQUIREMENTS.md` before changing behaviour.
2. State your assumptions.
3. Modify the minimum number of files.
4. Add or update tests in the same change.
5. Run verification and quote the actual output.
6. Update the English documentation.
7. Report changed files, commands run, results, and remaining risks.

When documents conflict, decide. Pick the option that serves the product, do it, and record the decision and its reason in the document that was wrong, so the conflict is settled rather than carried. Say in the report what was chosen and what was overruled. Never leave a conflict open and waiting for the owner unless the choice is genuinely theirs: scope, money, privacy, or anything with a legal edge.

None of this loosens the honesty rules. Deciding what to build is yours; claiming something works is still earned by running it.

Instruction precedence: the user's current request, then this file, then `lite/docs/REQUIREMENTS.md`, then existing code and tests, then reasonable engineering defaults.

### Git

One focused task per branch or commit. Meaningful commit messages. No secrets, no generated caches, no unrelated formatting sweeps, no destructive history rewriting unless explicitly requested.

## Definition of Done

Complete only when:

- the code is implemented
- tests were actually run
- behaviour was verified on an emulator by the agent, and on the owner's phone when they have it connected
- documentation was updated
- failures are reported honestly

Never claim completion without verification. Use precise language: "compiled", "45 unit tests pass", "verified on an Android 16 emulator", "verified on the owner's Poco X7 Pro over USB", "not reproduced". Never shorten "verified on an emulator" to "verified on a device".

Compilation is not verification. An emulator cannot show how manufacturer firmware freezes background processes, restricts autostart, or delivers accessibility events — the exact behaviours this product depends on. That gap closes only on a real phone.

### Documentation tasks

Complete only when the change covers exactly what was requested with no unrelated edits, affected documents agree with each other, every cross-reference and file path resolves, assumptions and open questions are stated explicitly, the content is in English with no duplicate or translated files, and the report lists the changed files.

Writing a requirement down is not evidence that it holds.
