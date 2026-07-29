# Prominent Disclosure and Affirmative Consent

Two screens, in this order, before the system Accessibility settings are ever opened. Every factual claim below is backed by `DATA_INVENTORY.md`.

---

## Placement rules

1. The disclosure appears **before** any request, not after, and not buried in onboarding.
2. It is BlockSocial's own screen. It uses the product's typography, its own colours, and the word BlockSocial. It carries no system iconography, no Android robot, no "Android is requesting", and no lookalike system button styling. A user must never mistake it for a dialog from the operating system.
3. The user leaves this screen only by choosing. There is no automatic dismissal, no timeout, and no way to reach the settings deep link without passing through the consent action.
4. Declining is a normal outcome. The application continues to work, explains what is unavailable, and offers the choice again later. It never repeats the request unprompted.
5. A line under the buttons states that the next screen belongs to Android, so the hand-off is not mistaken for part of BlockSocial.

---

## Screen 1 — Prominent disclosure

**Title**

> How BlockSocial blocks an app

**Body**

> To interrupt an app the moment you open it, BlockSocial uses Android's Accessibility service.
>
> Here is exactly what that means.
>
> **What BlockSocial reads.** Only the name of the app that just came to the front, and the moment it happened. That is the whole list.
>
> **What BlockSocial never reads.** Not the text on your screen. Not your messages. Not what you type. Not your passwords. The permission to read screen content is switched off in the app itself, so this is not a promise about our intentions — it is a setting the system enforces.
>
> **What BlockSocial does.** When you open an app you chose to restrict, it shows you a full-screen pause with two options. If you choose to stay focused, it presses the home button for you. It never taps, types, or acts inside another app.
>
> **Where it goes.** Nowhere. BlockSocial has no internet permission. Nothing about your apps or your choices leaves this phone.

**Buttons**

- Primary: `Continue`
- Secondary: `Not now`

**Footnote under the buttons**

> The next screen is Android's own, not ours.

---

## Screen 2 — Affirmative consent

Shown only after `Continue`. This is the screen that records consent.

**Title**

> Turn on blocking?

**Body**

> BlockSocial will use the Accessibility service to notice when a restricted app opens, and to show you the pause screen.
>
> It reads the name of the app in front and nothing else. You can turn this off at any time in Android Settings, or from the Protection screen inside BlockSocial.

**Buttons**

- Primary: `I agree, open Settings`
- Secondary: `Cancel`

Pressing `I agree, open Settings` records the consent locally with a timestamp, then opens `Settings.ACTION_ACCESSIBILITY_SETTINGS`. Pressing `Cancel` records nothing and returns.

**Footnote under the buttons**

> The next screen is Android's own, not ours. Find BlockSocial in the list and switch it on.

---

## If the user declines

> Blocking is off. Your rules are saved and nothing is lost — they simply will not run until you turn blocking on.
>
> Statistics and daily limits need a separate permission and are also unavailable.

With one action to reopen the disclosure. No nagging, no repeat prompt on next launch, no badge.

---

## Copy rules that apply to every version of this text

- Say "the name of the app in front", not "app usage" — the second is vaguer and sounds larger.
- Never write "we do not collect personal data" as a bare claim. Name the specific things not read, because that is checkable.
- Never claim a capability that has not been proven. Blocking on OEM firmware is unverified until beta, so no copy anywhere promises that BlockSocial cannot be escaped, or that it works identically on every phone.
- The word "block" describes what the user asked for. Do not write "protect you from", "guard", or "lock".
- Russian and English must say the same thing. If a sentence cannot be translated without weakening it, change the English.
