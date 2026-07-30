# Product

BlockSocial is a native Android and iOS application that interrupts the automatic launch of a social-media application, gives the user a deliberate choice, and records that choice locally.

---

## Problem

People open social-media applications without deciding to. The sequence is automatic: pick up the phone, open Instagram or TikTok, land in the feed, lose twenty minutes, close the app with a feeling of irritation. The loss is not only time. The habit removes the moment of choice entirely.

Existing tools fail in one of two ways. Hard blockers are too rigid, so users disable them and stop. System timers report time after the fact, which changes nothing at the moment the app is opened.

Neither addresses the actual failure: the tap happens before any decision is made.

## Target Users

People aged roughly 16 to 40 who notice they open social apps automatically, want to reduce that, and are not willing to delete the apps.

Three priority segments:

- **Students** — distraction during study, procrastination, short-form video before exams.
- **Knowledge workers** — interrupted deep work, checking social media between tasks, lost rhythm.
- **Night scrollers** — long phone use before sleep, disrupted sleep schedule.

Not a target: parents controlling a child's device, employers controlling staff devices.

## Core Value

BlockSocial inserts a pause between the impulsive tap and entering the app, then records what the user decided.

> System timers measure time. BlockSocial measures decisions.

Three mechanisms combined:

1. **A moment of awareness** — the intervention lands exactly when the app is opened, not in a weekly report.
2. **Flexible restriction** — the user may decline entry or consciously take limited access.
3. **Honest statistics** — not only screen time, but interventions, refusals, and bypasses.

## MVP

1. Onboarding
2. Permission explanation and request
3. Selection of supported applications
4. Weekday schedules and an always-on mode
5. Daily limits per application
6. Soft block with two actions
7. Temporary bypass
8. Intervention history
9. Basic daily statistics
10. Local storage
11. Protection health screen
12. Correct behavior when permissions are revoked
13. Light and dark themes

Everything works offline.

Daily limits are part of the MVP, not a later addition. They are built after schedule blocking works, because they depend on usage measurement that is not yet proven: `UsageStatsManager` on Android and DeviceActivity thresholds on iOS. Sequencing them second is a build order, not a reduction of scope.

## Non-Goals

Outside the product entirely:

- parental control or managing another person's device
- surveillance, MDM, antivirus, VPN, ad blocking
- HTTPS interception
- a guarantee that the device owner cannot reach an application
- a replacement for medical or psychological care

Deferred, requires explicit approval to add:

- manual focus sessions started on demand
- backend, accounts, cloud synchronization
- VPN or DNS filtering, website blocking
- selective blocking of Reels, Shorts, feeds, or in-app tabs
- AI coach, social features, competitions
- monetization: payments, subscriptions, advertising, third-party analytics
- web dashboard

This list is exhaustive. Anything not on it and not in the non-goals above belongs to the MVP.

The product blocks whole applications. Selective blocking inside an application is not technically achievable in a reliable, store-approved way, and promising it would be dishonest.

## Main User Flows

### First launch

1. User opens BlockSocial and sees a short explanation.
2. User sees why each permission is needed, in the product's own words.
3. User grants permissions.
4. User selects supported applications.
5. User creates the first rule.
6. Dashboard appears; protection is active.

### Intervention

1. User opens Instagram.
2. BlockSocial detects that a rule is active.
3. The soft block appears, naming the app, the rule, and the time remaining.
4. User chooses `Stay Focused` or `Open Temporarily`.
5. The decision is recorded.
6. Statistics update.

### Review

1. User opens the dashboard.
2. User sees interventions, refusals, bypasses, refusal rate, active rules, streak.
3. User adjusts rules.

## Business Rules

A restriction is active when its rule is enabled, no bypass is active for that application, and the rule's own condition holds:

- **always-on** — always
- **schedule** — the current weekday is selected and local time falls inside the interval
- **daily limit** — measured usage of that application today has reached the limit

An interval such as `22:00–07:00` crosses midnight. Schedules are stored in local time; bypass grants store an absolute expiry. A daily limit resets at local midnight.

Usage measurement is approximate on both platforms, so a daily limit triggers slightly late rather than exactly at the limit. The product states this rather than implying a precise cutoff.

Rule priority: always-on, then schedule, then daily limit. When several apply, the user sees one clear reason while all reasons are preserved in the event record. A manual focus session, if it ships later, sits directly below always-on; the priority model reserves that slot but the MVP has nothing in it.

After a bypass is granted, the block must not appear again for that application until the grant expires.

BlockSocial never blocks itself, system settings, the launcher, the phone app, or critical system UI.

Statistics never show false precision. Prefer "you declined 12 potential sessions" over "you saved exactly 2 hours 17 minutes". A metric a platform cannot measure is hidden, not shown as zero.

## Tone

The interface never shames, accuses, or scores the user as a failure. A bypass is a legitimate outcome. No guilt copy, no urgency pressure, no aggressive red as the dominant color, no moralizing about social media.

The block screen is a pause, not an alarm.

## Open Questions

| ID | Question | Blocks |
|---|---|---|
| Q-01 | ~~Streak rule~~ | **answered** |
| Q-02 | ~~Which Android block-screen direction is the baseline~~ | **answered** |
| Q-03 | ~~Source of the approximate time-in-app metric~~ | **answered** |
| Q-04 | Default bypass duration on iOS, which depends on what the platform can actually deliver | iOS copy |
| Q-05 | Business model. Freemium is an untested assumption. | not MVP-blocking |

Questions are answered in this file when decided.

**Q-02, answered.** The Android block-screen baseline is direction **1b, "Quiet"**: the application line, a 34 sp headline naming the rule, one supporting sentence, a full-width primary and secondary button, and a monospace footnote. Spike `A-02` built it with the documented tokens and it held in both themes and at the largest font scale without truncation, with the whole decision on one screen and no scrolling. Direction 1c, the structural one, was always marked provisional on that spike; it stays a candidate for a later design pass rather than the baseline. Evidence is in `docs/spikes/SPIKE_A-02_RESULT.md` and `docs/GATE_ANDROID.md`.

**Q-03, answered.** Time in an application comes from Android usage statistics, read as events rather than as daily buckets, not from the length of bypass grants. Spike `A-05` measured the error at under 0.1 percent over a five-minute session on two emulator images, and showed that the bucketed source does not reset at local midnight and so cannot carry a daily limit. Evidence is in `docs/spikes/SPIKE_A-05_RESULT.md`. A session still running is shown as in progress rather than as a final number.

**Q-01, answered.** A day counts towards the streak when the number of bypasses that day is **at or below a configured maximum**, default two. A bypass inside the allowance does not break the streak; exceeding it means the day is not counted, and the streak starts again the next day. The streak explains its own rule inline, before it can be lost, and never appears in a warning colour.

This resolves the conflict recorded in `design/DESIGN_EXPORT_ANALYSIS.md` section 9.1 in favour of this document. The design export's option 2e states the opposite — that opening an app ends the streak — and that copy must be replaced when the dashboard is designed. The stricter rule was rejected because it turns a legitimate bypass into a failure, which contradicts the principle that a bypass is a legitimate outcome and the rule that the interface never scores the user as a failure.

**No open question now blocks Android work.** `Q-04` is iOS and `Q-05` is not MVP-blocking.
