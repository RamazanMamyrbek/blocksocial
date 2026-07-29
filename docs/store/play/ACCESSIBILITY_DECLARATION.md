# Play Console Accessibility Declaration

The answers to give in the Play Console when declaring `BIND_ACCESSIBILITY_SERVICE`, plus the reasoning behind the two settings a reviewer is most likely to question.

---

## Declaration answers

**Which permissions or APIs does your app use that require a declaration?**

> `BIND_ACCESSIBILITY_SERVICE`.

**What is the core functionality of your app?**

> BlockSocial interrupts the automatic opening of a social-media application. When the user opens an application they chose to restrict, BlockSocial shows a full-screen pause offering two choices: return to what they were doing, or open the application for a short, fixed period. It records which choice was made, on the device, and shows the user their own pattern over time.

**Why does that functionality require the Accessibility API?**

> The feature has to act at the moment an application comes to the foreground. Android offers no other way for an application to learn that a different application has just been opened, in time to react. Usage statistics report the past, not the present, and are explicitly not used for blocking. Overlay permissions grant a window but no signal about what is in front.
>
> BlockSocial therefore subscribes to a single accessibility event type, `typeWindowStateChanged`, and reads only the package name and timestamp of the application that came to the front.

**What data does the accessibility service access?**

> The package name of the foreground application, the class name of its window, and the event timestamp. The class name is used only to ask `PackageManager` whether the window belongs to an activity, and is discarded immediately; it is never stored or displayed.
>
> The service cannot read screen content. `android:canRetrieveWindowContent` is `false` in the service configuration, so the restriction is enforced by the system rather than by our own discipline. The service does not read text, form fields, messages, notifications, or keystrokes, and subscribes to no event type that would carry them.

**Is the data transmitted off the device?**

> No. The application has no `INTERNET` permission and contains no networking code. There is no backend, no account, no analytics SDK and no crash reporter.

**How is the user informed?**

> A prominent disclosure screen appears before any request, naming what is read and what is not, followed by a separate affirmative consent screen. Neither screen resembles a system dialog. Declining is a supported outcome and the application keeps working without blocking. Full text in `DISCLOSURE_AND_CONSENT.md`.

**Can users disable it?**

> Yes, from Android Settings at any time, and from the Protection screen inside BlockSocial, which also detects when the service has stopped running and says so.

---

## Why `isAccessibilityTool` is `false`

`android:isAccessibilityTool="false"` is set in every service configuration.

The flag declares whether an application exists to assist users with disabilities. BlockSocial does not. It is an attention-management tool for users who can see and operate their phone normally, and it uses the Accessibility API for a purpose the platform allows but did not design the API for.

Declaring `true` would be a misrepresentation. It would place BlockSocial among screen readers and switch-access tools, exempt it from exactly the review it should receive, and mislead users who filter that list looking for assistive software. The honest declaration is the one that invites scrutiny, and this package exists to make that scrutiny cheap.

The consequence is accepted: the use case must be declared and approved, and it can be rejected. That is risk `R-01` in `docs/TECHNICAL_SPECIFICATION.md`.

## Why the service subscribes to only one event type

`android:accessibilityEventTypes="typeWindowStateChanged"` is the narrowest subscription that answers "which application is now in front". Subscribing to more would deliver text, focus and content-change events the product has no use for, and would make the privacy claim harder to verify. The narrow subscription is a design decision, not an optimisation.

## What a reviewer can verify without our help

| Claim | Where a reviewer sees it |
|---|---|
| One event type only | `accessibility_service_config.xml`, `accessibilityEventTypes` |
| Cannot read screen content | same file, `canRetrieveWindowContent="false"` |
| Not presented as an assistive tool | same file, `isAccessibilityTool="false"` |
| No broad package visibility | merged manifest, no `QUERY_ALL_PACKAGES` |
| Nothing leaves the device | merged manifest, no `INTERNET` permission |
| Usage access is separate and optional | `PACKAGE_USAGE_STATS` declared on its own, requested in its own flow |
