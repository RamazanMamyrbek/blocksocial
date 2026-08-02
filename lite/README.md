# BlockSocial Lite

A daily time limit for individual applications, and a warning when a limit is spent. That is the whole product.

`docs/REQUIREMENTS.md` is the specification; this file is the short version and the record of what has actually been run.

This is its own application — package `com.blocksocial.lite`, its own storage, its own launcher entry, its own release, and its own scope decided in its own requirements document. It happens to share a git repository with the application in `../android` and reuses source from it the way any two projects share a library, but it is not a variant, edition or subset of that one and owes it no alignment. Both install on the same phone at once.

## What it does

1. Lists the supported applications that are installed on the phone, split into those with a limit and those without.
2. Lets you type a daily limit in minutes for any of them.
3. Measures how long each was in the foreground today.
4. When you open one whose limit is spent, it covers the screen with the figures and one question.
5. It asks again on the next entry, and the one after that. Answering buys no grace period.

Tapping an application shows the two figures that matter: used today and left today.

**A limit covers the whole day from midnight**, including time already spent before you set it — the same rule Digital Wellbeing uses. Creating, changing or removing a limit never resets the count.

## What it deliberately does not do

No history. No statistics beyond those two figures. No streaks, no refusal rates, no "hours saved". No schedules, no always-on blocking, no focus sessions. No timed pass after carrying on. No onboarding carousel — just the two permissions and what each one is for.

Nothing is stored except the limit, one number per application. There is no record of when you were warned or what you chose, because nothing here needs one. The diagnostics page keeps the last thirty foreground changes in memory only; closing the process forgets them.

`docs/REQUIREMENTS.md` section 6 lists what is out of scope and why each was refused.

## How it is built

One Gradle module. DataStore for two values per application, no database. Dependencies are constructed in a `Container` held by `Application`; there is no dependency-injection framework. Neither Room nor Hilt earns its place in an application with one screen of state, and both would have been the largest thing in this source tree. `../AGENTS.md` names a different stack for the application it describes; this one is recorded here so the difference is deliberate rather than discovered.

The parts taken from `../android` are the ones that were expensive to get right: foreground-session accumulation, the usage reader, the transition tracker, the never-blocked system package list, and the overlay host. They carry the fixes those files earned — counting no minute earlier than the last boot among them — plus the ones found here and not yet carried back.

## Privacy

The accessibility service reads the package name of whatever came to the front and the moment it happened. It never reads screen content: `canRetrieveWindowContent` is `false` in `accessibility_service_config.xml`, so the system enforces that rather than the code promising it. Usage access supplies foreground durations for the catalog applications only. Nothing leaves the phone; there is no network permission at all.

## Building

```
cd lite
./gradlew :app:assembleDebug
./gradlew :app:test
```

The release build is signed with the same hand-testing keystore as the full application, resolved from `lite/keystore.properties`. Both that file and the `.jks` are untracked.

```
./gradlew :app:assembleRelease
```

## What has been verified

On an Android 16 emulator (`blocksocial_a01`), with the **signed release APK**:

| | |
|---|---|
| A one-minute limit saved after six minutes already spent in YouTube today | list reads 6 of 1 — limit spent, immediately, with no reset |
| Opening YouTube with the limit spent | warning |
| "Carry on anyway", leave, return | warning again, every time |
| Crossing the limit while inside, leaving and returning within a second | warning |
| "Leave it for today" | warning gone, launcher in front |
| Application screen | used today 6 min, left today "Nothing left" |
| Typing a limit | accepted; out of range refused with the range named and the save button unavailable |
| Accessibility service stopped underneath a running app | main screen says limits are not running, and clears when it returns |
| "What the app is seeing" | service WORKING, connect time, last screen change, minutes measured, recent decisions |
| Crashes | none |

43 unit tests, no failures. Lint clean. Nothing here has been run on a physical phone; manufacturer firmware alters both mechanisms this product stands on, and an emulator cannot show that.

## Notifications

There are none, and there is no notification permission. The switch for this application in the system notification settings is therefore inert on some phones — greyed out and unresponsive. That is the expected consequence of R-28 and does not affect limits.

## Verifying it by hand, from the host

No phone is touched. Everything is driven over `adb`:

```
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell appops set com.blocksocial.lite android:get_usage_stats allow
adb shell settings put secure enabled_accessibility_services \
  com.blocksocial.lite/com.blocksocial.lite.detection.LimitAccessibilityService
adb shell settings put secure accessibility_enabled 1
adb shell monkey -p com.google.android.youtube -c android.intent.category.LAUNCHER 1
adb shell dumpsys accessibility | grep -c "BlockSocial Lite warning"
```

The last line prints `1` while the warning is on screen.
