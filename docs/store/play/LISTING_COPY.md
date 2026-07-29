# Play Listing Copy

Only the parts that describe the mechanism. Graphics, screenshots and pricing are out of scope for this phase.

---

## Short description, 80 characters maximum

> A pause between the tap and the feed. You decide, and BlockSocial remembers.

78 characters.

---

## Full description, mechanism section

This paragraph must appear in the listing, because Play policy requires the Accessibility use to be described where users can see it before installing.

> **How blocking works, and what BlockSocial can see**
>
> BlockSocial uses Android's Accessibility service to notice the moment you open an app you chose to restrict. It reads one thing: the name of the app that just came to the front. It cannot read the text on your screen — that permission is switched off inside the app, so the system enforces it, not us.
>
> When a restricted app opens, BlockSocial shows a full-screen pause with two options: stay focused, or open the app for a short time. It never taps, types, or does anything inside another app.
>
> BlockSocial has no internet permission. Your apps, your rules and your choices never leave your phone. There is no account and no server.
>
> You can turn blocking off at any time, in Android Settings or inside BlockSocial.

---

## Full description, product section

> You do not decide to open Instagram. Your thumb does, and twenty minutes later you close it annoyed.
>
> Screen-time reports tell you about that afterwards. BlockSocial interrupts it as it happens.
>
> Choose the apps you want to be deliberate about. Set the hours, or a daily limit. When you open one of them, a full screen appears naming the rule you set, and asks what you want to do. Stay focused, and you go back to what you were doing. Open it anyway, and you get a few minutes without being asked again.
>
> Either answer is fine. BlockSocial counts both, and shows you the pattern rather than a score.
>
> - Whole apps, not feeds or tabs
> - Weekday schedules, always-on, and daily limits
> - A temporary opening, so a rule is never a trap
> - Your history of choices, kept on your phone
> - Works offline, because it never needed a network
>
> BlockSocial does not shame you, does not gamify quitting, and cannot stop a determined person from opening an app. That is not a bug. It is a tool for the moment your attention slips, not a lock.

---

## Claims deliberately not made

These would be easy to write and are not defensible today.

| Not written | Why |
|---|---|
| "Cannot be bypassed", "unbreakable" | untrue, and the product is a soft blocker by design |
| "Works on every Android phone" | OEM firmware behaviour is unverified until beta, risk `R-06` |
| "Blocks Reels and Shorts" | selective in-app blocking is a non-goal in `docs/PRODUCT.md` |
| "Saves you two hours a day" | the product rule against false precision |
| "Parental control" | explicitly not a target user |
| "Private and secure" as a bare slogan | replaced by the specific list of what is and is not read |

## Wording that must stay consistent across the three descriptions

The listing, the in-app disclosure, and the Play Console declaration all say the same three things in the same terms:

1. It reads **the name of the app that came to the front**, and the time.
2. It **cannot read screen content**, and the reason given is always the same: the setting is off in the service configuration.
3. **Nothing leaves the phone**, because there is no internet permission.

If any of the three ever changes, all three change together.
