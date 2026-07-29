# Privacy Policy — Draft

Draft for review. It describes what the code does today, verified in `DATA_INVENTORY.md`, not what the product intends to do. Publication needs a hosted URL and a legal read; neither is part of this phase.

**Last updated:** to be set at publication
**Applies to:** BlockSocial for Android

---

## The short version

BlockSocial keeps everything on your phone. It has no servers, no account, and no internet permission. Nobody, including us, can see which apps you chose, when you opened them, or what you decided.

## What BlockSocial reads, and when

When you switch blocking on, BlockSocial uses Android's Accessibility service. Every time an application comes to the front, the service receives:

- the package name of that application, for example `com.instagram.android`
- the class name of the window, which BlockSocial uses only to check whether the window is a real screen, and then discards
- the time the change happened

That is the entire list. BlockSocial cannot read the text on your screen, your messages, what you type, your passwords, or anything you see inside another application. The permission that would allow that is switched off in BlockSocial's own configuration, so Android enforces the restriction.

BlockSocial does not read events from applications you have not chosen to restrict, beyond noticing that something else came to the front so it can dismiss its own screen.

## What BlockSocial stores, and where

On your phone, in storage only BlockSocial can read:

- the applications you selected
- the rules you created
- temporary openings you granted, as an application name and the times the opening starts and ends
- a history of the choices you made at the pause screen

None of it contains anything from inside another application. A stored temporary opening is one line: an application name and four numbers.

## What BlockSocial sends

Nothing. There is no internet permission in the application, no server to send anything to, no analytics, no crash reporting, and no advertising identifier.

## Usage statistics, if you allow them

Statistics and daily limits need a second, separate Android permission called usage access. It is optional and requested on its own. If you grant it, BlockSocial reads how long applications were in the foreground, on this device, to show you your own totals and to know when a daily limit is reached.

If you decline, blocking by schedule and always-on rules keeps working. Only statistics and daily limits stop. BlockSocial will say so rather than fail quietly.

Blocking never depends on usage statistics.

## Which applications BlockSocial can see

BlockSocial does not list the applications on your phone. It carries its own list of supported applications and asks Android only about those, one by one. Applications outside that list are invisible to it, including to us.

## Children

BlockSocial is not designed for children and is not a parental-control tool. It does not knowingly collect anything from anyone, of any age, because it does not collect anything.

## Your control

- Turn blocking off at any time, in Android Settings or inside BlockSocial.
- Withdraw usage access at any time in Android Settings.
- Clear your history inside the application.
- Uninstall, which removes everything BlockSocial has stored.

There is nothing to request from us, because we hold nothing.

## Changes

If a future version of BlockSocial reads or stores anything not listed here, this policy changes first, and the application says so before the new behaviour starts.

## Contact

To be set at publication.

---

## Notes for the reviewer of this draft, not part of the published text

- Two placeholders remain deliberately: the last-updated date and the contact address. Both are set at publication and cannot be filled honestly now. They are the only ones.
- The claim "no internet permission" must be re-verified against the shipping manifest in phase 24. It is true of every spike today.
- The section on storage lists what production will store. Only temporary openings exist so far, in spike `A-04`. The wording is written for the shipping product because that is what the policy will accompany; phase 24 must confirm it still matches.
- The debug-logging gap recorded in `DATA_INVENTORY.md` section 8 must be closed before this policy is published, because the policy says nothing leaves the device and a release log is a place a package name could leak.
