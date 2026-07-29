# Google Play Policy Package

Everything needed to declare BlockSocial's use of the Accessibility API, written so that the three places the mechanism is described say the same thing. Produced in plan phase 06 as spike `A-06`.

Nothing here is submitted yet. Phase 23 re-verifies the whole package against the shipping build before submission.

## Contents

| File | What it is |
|---|---|
| `DATA_INVENTORY.md` | every field the service touches, with the line of code that touches it |
| `DISCLOSURE_AND_CONSENT.md` | the in-app prominent disclosure and the affirmative consent screen |
| `ACCESSIBILITY_DECLARATION.md` | the Play Console declaration answers, and why `isAccessibilityTool` is `false` |
| `LISTING_COPY.md` | the store listing paragraphs that describe the mechanism |
| `PRIVACY_POLICY.md` | policy draft |
| `DEMO_VIDEO_SCRIPT.md` | a 90-second script filmable against the current spike build |

Read `DATA_INVENTORY.md` first. It is the evidence the other five rest on; if a claim is not supported there, it does not belong in any of them.

## The three descriptions, side by side

Play sees the mechanism described in three places. A reviewer who finds them disagreeing has a reason to reject. They were written to be checkable against each other:

| | Disclosure | Listing | Declaration |
|---|---|---|---|
| What is read | "the name of the app that just came to the front, and the moment it happened" | "the name of the app that just came to the front" | "package name of the foreground application, the class name of its window, and the event timestamp" |
| Why it cannot read the screen | "the permission to read screen content is switched off in the app itself" | "that permission is switched off inside the app, so the system enforces it, not us" | "`android:canRetrieveWindowContent` is `false` in the service configuration" |
| What leaves the device | "Nowhere. BlockSocial has no internet permission." | "BlockSocial has no internet permission." | "No. The application has no `INTERNET` permission and contains no networking code." |
| What it does inside other apps | "It never taps, types, or acts inside another app." | "It never taps, types, or does anything inside another app." | "no gesture is dispatched … nothing is automated inside any third-party application" |

The declaration is the most precise because a reviewer reads it; the disclosure is the plainest because a user reads it. They differ in register, not in substance. The one asymmetry is deliberate: the disclosure does not mention the window class name, because naming an internal detail a user cannot act on would add confusion rather than transparency. The declaration and `DATA_INVENTORY.md` both name it, so it is disclosed where it matters.

## Cross-read result

Every claim in the package was checked against the spike sources at `dev` commit `5bf53a3`. Three things were found and are recorded rather than smoothed over.

**1. The back-key line.** `OverlayViewHost.kt:54` reads a `KeyEvent`. The disclosure says BlockSocial does not read what you type. Both are true — the key event belongs to BlockSocial's own window and the service subscribes to no key event type — but a reviewer grepping the source would find it and wonder. It is named in `DATA_INVENTORY.md` section 2 so that it is disclosed by us rather than discovered.

**2. Ungated logging in two spike modules.** `a03` and `a05` log package names without a debug check. They are throwaway builds that never ship, but the privacy policy claims nothing leaves the device, and a release log is somewhere a package name could leak. Recorded as a gap in `DATA_INVENTORY.md` section 8, assigned to phase 23.

**3. The privacy policy describes storage that does not exist yet.** Rules, selections and event history are written about in the future tense of the shipping app; only temporary grants exist today, in `a04`. The policy accompanies the shipping build, so it is written for it, and the notes at the end of that file say plainly which parts are not yet true. Phase 23 confirms.

No contradiction was found between the three descriptions of the mechanism.

## What this package deliberately does not claim

Collected here because the temptation to write each of these will recur:

- that blocking cannot be bypassed
- that it behaves identically on every manufacturer's firmware
- that individual feeds, Reels or tabs can be blocked
- any figure for time saved
- that BlockSocial is an accessibility tool

The last one is the reason for the whole package. BlockSocial uses an API built for assistive software, for a purpose that is not assistive, and says so.

## Open risk

`R-01` in `docs/TECHNICAL_SPECIFICATION.md`: Play may reject the use case regardless of how well it is documented. This package is the earliest possible attempt to find that out, and it lowers the odds of a rejection for a reason we controlled. It cannot lower them to zero, and no decision here should assume it did.
