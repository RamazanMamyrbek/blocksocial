# Demo Video Script

Play asks for a short recording showing the accessibility feature in use. This script is written to be filmable **today**, against the `a04` spike build, so that nothing in it is a promise about software that does not exist yet.

**Length:** about 90 seconds. No narration is required; on-screen captions are enough, and captions survive translation better than a voice track.

---

## What differs between this recording and the shipping app

State this in the video description rather than hiding it.

| In the spike | In the shipping app |
|---|---|
| Restricted applications are YouTube and Chrome, fixed in code | the user picks from the supported-app catalog |
| The rule text is a fixed example | the real rule the user created |
| A temporary opening lasts 60 seconds | five minutes |
| No onboarding, no settings, no statistics | present |

Everything the video claims about the **mechanism** is identical in both.

---

## Shot list

**1. The disclosure, 0:00–0:12**

Screen recording of the prominent disclosure text from `DISCLOSURE_AND_CONSENT.md`. Hold long enough to read the two lists: what is read, what is never read.

> Caption: BlockSocial explains the Accessibility use before asking for anything.

The spike has no disclosure screen. Film this against a static render of the approved copy and say so in the description, or defer this shot until phase 20 builds the real screen. Do not film a mockup and present it as the running app.

**2. Turning the service on, 0:12–0:25**

Tap through to Android's Accessibility settings, find BlockSocial in the list, switch it on, accept the system's own confirmation.

> Caption: The switch lives in Android Settings. It can be turned off there at any time.

**3. The interruption, 0:25–0:40**

Return home. Open YouTube from the launcher. The block screen appears over it.

> Caption: Opening a restricted app shows a full-screen pause.

Let the screen sit still for four seconds so a reviewer can read it: the app name, the rule, and the two options.

**4. Staying focused, 0:40–0:50**

Tap `Stay focused`. The device returns to the home screen.

> Caption: "Stay focused" presses home. BlockSocial does nothing inside the other app.

**5. Opening anyway, 0:50–1:05**

Open YouTube again. When the pause appears, tap `Open for 60 seconds`. YouTube opens normally and is fully usable. Scroll it briefly to show it is not restricted.

> Caption: The other choice is always available. A rule is not a trap.

**6. The opening holds, 1:05–1:15**

Press home, then open YouTube again within the window. No pause appears.

> Caption: Inside the temporary opening, BlockSocial stays out of the way.

**7. The opening ends, 1:15–1:30**

Wait for the window to pass, then open YouTube once more. The pause appears again.

> Caption: When the time is up, the pause comes back.

---

## What must be visible in the recording

- The whole flow is one continuous take from shot 2 onward. Cuts invite the suspicion that something was staged between them.
- Android's own Accessibility settings screen, unedited, so the reviewer sees the service being enabled through the system and not through a lookalike.
- The block screen covering a real third-party application, not a simulator or a mock.
- No point where BlockSocial types, taps, or scrolls inside YouTube.

## What must not appear

- No claim, caption or otherwise, that BlockSocial cannot be bypassed.
- No suggestion that it reads or reacts to anything on the screen. Nothing in the recording should show BlockSocial responding to content, only to the app being opened.
- No personal data on screen: sign out of accounts, clear notifications, and use a clean device profile.
- No OEM-specific setup steps, because that behaviour is unverified until beta.

## Recording notes

Record at the device's native resolution with the emulator or phone in portrait. Turn off the developer overlay for pointer locations, because a visible touch indicator during shot 4 makes it look as if BlockSocial performed the tap.
