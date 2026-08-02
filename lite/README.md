# BlockSocial Lite

A second, deliberately smaller application built from the same parts as the one in `../android`. It does one thing: a daily time limit per application, and a warning when that limit is spent.

It installs alongside the full application rather than replacing it — different `applicationId` (`com.blocksocial.lite`), different data, different launcher entry. Both can sit on the same phone at the same time.

## What it does

1. Lists the supported applications that are installed on the phone.
2. Lets you set a daily limit in minutes on any of them.
3. Measures how long each of them was in the foreground today.
4. When you open one whose limit is spent, it covers the screen and asks whether you are sure you want to carry on.
5. It asks again on the next entry, and the one after that. There is no grace period and nothing is remembered between entries.

Tapping an application shows the two figures that matter: minutes used today, and minutes left.

## What it deliberately does not do

No history. No statistics beyond today's two figures. No streaks, no refusal rates, no "hours saved". No schedules, no always-on blocking, no focus sessions. No timed bypass grant. No onboarding carousel — just the two permissions and what each one is for.

Nothing is stored except the limits themselves. There is no record of when you were warned or what you chose, because nothing in the product needs one.

## How it differs structurally from `../android`

| | `../android` | here |
|---|---|---|
| Modules | eleven | one |
| Storage | Room and DataStore | DataStore only, one integer per application |
| Dependency injection | Hilt | a `Container` built in `Application` |
| Rules | always-on, schedule, daily limit | daily limit |
| After a warning | a five-minute bypass grant | nothing; the next entry asks again |

Hilt and Room were dropped rather than copied. Neither earns its place in an application with one screen of state and one integer per application to persist, and both cost build time and indirection that would be the largest thing in this source tree. That is a deviation from the stack named in `../AGENTS.md`, which describes the full application; it is recorded here rather than left to be discovered.

The parts that were copied nearly verbatim are the ones that were expensive to get right: `usage/ForegroundSessions.kt`, `usage/UsageStatsReader.kt`, `detection/ForegroundTransitionTracker.kt`, `detection/SystemPackages.kt` and `warning/OverlayViewHost.kt`. They carry the fixes those files earned, including counting no minute earlier than the last boot and treating `MOVE_TO_BACKGROUND` as the end of a visit.

## Privacy

Unchanged from the full application, and narrower. The accessibility service reads the package name of whatever came to the front and the moment it happened. It never reads screen content — `canRetrieveWindowContent` is `false` in `accessibility_service_config.xml`, so the system enforces that rather than the code. Usage access supplies foreground durations for the catalog applications only. Nothing leaves the phone; there is no network permission at all.

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

On an Android 16 emulator (`blocksocial_a01`), with the **signed release APK**, YouTube restricted to five minutes a day:

| | |
|---|---|
| Under the limit | 0 of 5 minutes shown on the list, no warning on entry |
| Five minutes held in the foreground | measured 6, warning on the next entry |
| Warning contents | "About 6 minutes in YouTube today, against a limit of 5." |
| "Carry on anyway", then leave and return | warning again — three times in a row, debug and release |
| "Leave it for today" | warning gone, launcher in front |
| Application screen after the limit | used today 8 min, left today "Nothing left" |
| Crashes | none |

24 unit tests, no failures. Lint clean. Nothing here has been run on a physical phone, and the note in `../docs/COMPATIBILITY.md` about manufacturer firmware applies to this application unchanged — it uses the same two Android mechanisms.

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
