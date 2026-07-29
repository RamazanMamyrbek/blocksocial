# Data Inventory and Code Traceability

Every piece of data the accessibility service touches, with the line of code that touches it. Nothing in the policy package may claim anything this table does not support.

Verified against the spike sources at `dev` commit `5bf53a3`. Line numbers refer to that state.

---

## 1. What the accessibility service reads

The service subscribes to one event type and reads four fields of it. There are no others.

| Field | Purpose | Code |
|---|---|---|
| `event.eventType` | discard everything that is not a window change | `a04/.../BlockOverlayService.kt:53` |
| `event.packageName` | identify which application came to the front | `a04/.../BlockOverlayService.kt:59` |
| `event.className` | ask `PackageManager` whether this window is a real activity | `a04/.../BlockOverlayService.kt:62`, used at `:148` |
| `event.eventTime` | debounce repeated events | `a04/.../BlockOverlayService.kt:63` |

The same four, and only these four, appear in `a01/.../LaunchDetectionService.kt:27,31,32,33` and `a02/.../BlockOverlayService.kt:42,47,50,51`.

`event.className` is never stored and never shown. It is passed straight to `packageManager.getActivityInfo` and reduced to a boolean. This exists because spike `A-01` proved that `packageName` alone is not a reliable foreground signal: a backgrounding application keeps emitting window events, and the ones to ignore carry a plain view class rather than an activity class.

## 2. What the service does not read

Each of these is an absence that can be checked, not a promise.

| Not read | How it is prevented |
|---|---|
| Screen text, labels, field contents | `android:canRetrieveWindowContent="false"` in `accessibility_service_config.xml` |
| The view hierarchy | no call to `getRootInActiveWindow` or `AccessibilityNodeInfo` exists in any source file |
| The window list | no call to `getWindows` exists |
| Every other event type | `android:accessibilityEventTypes="typeWindowStateChanged"` |
| Keystrokes and input in other applications | no key or text event type is subscribed; see the note below |

**The one nuance about keys.** `OverlayViewHost.kt:54` reads `keyCode` and `action` from a `KeyEvent`. That event is delivered to BlockSocial's own block screen, so that the system back button can be handled on our own window. It is not an accessibility event, it carries no input from any other application, and it cannot: the service subscribes to no key event type. The disclosure says "we do not read what you type"; that statement survives this line, and the line is named here so no reviewer has to discover it.

## 3. What the service does

| Action | Purpose | Code |
|---|---|---|
| `performGlobalAction(GLOBAL_ACTION_HOME)` | return the user to the home screen when they choose to stay focused | `a04/.../BlockOverlayService.kt:118` |
| Add a `TYPE_ACCESSIBILITY_OVERLAY` window | show the block screen | `a04/.../BlockOverlayController.kt` |

No gesture is dispatched, no button is pressed, no text is entered, and nothing is automated inside any third-party application. `dispatchGesture` appears nowhere in the sources.

## 4. Package visibility

The application does not enumerate installed applications. It asks `PackageManager` about package names that already appear in its own catalog, and it can only see those because they are declared one by one:

```xml
<queries>
    <package android:name="com.instagram.android" />
    ...
</queries>
```

`a03/src/main/AndroidManifest.xml` declares twelve packages, one per catalog entry. `QUERY_ALL_PACKAGES` is absent from every merged manifest, verified in spikes `A-02`, `A-03` and `A-04`.

Reads: `getApplicationInfo`, `getApplicationLabel`, `getApplicationIcon`, `getActivityInfo` — `a03/.../PackageManagerAppLookup.kt:10,13`, `a03/.../CatalogActivity.kt:32`, `a04/.../BlockOverlayService.kt:139,148`.

## 5. Usage statistics, a separate permission

`PACKAGE_USAGE_STATS` is declared only in `a05/src/main/AndroidManifest.xml:5` and is requested separately from accessibility access. It powers statistics and daily limits, never blocking.

Read per usage event: package name, timestamp, event type — `a05/.../UsageStatsReader.kt:20,22`. Denial is a state, not an error: `a05/.../UsageAccess.kt`.

## 6. What is stored on the device

| Stored | Contents | Where |
|---|---|---|
| Temporary access grants | package name plus four timestamps | `SharedPreferences("temporary_access_grants", MODE_PRIVATE)`, `a04/.../GrantStorage.kt:15` |

A stored grant looks exactly like this, and this is the whole record:

```xml
<string name="com.google.android.youtube">1785259923002|1785259983002|9654664|9714664</string>
```

Production adds the user's selected applications, their rules, and a local event history. All of it stays in the application's private storage. None of it contains screen content.

## 7. What leaves the device

Nothing. There is no `INTERNET` permission in any manifest, and no networking API — no `HttpURLConnection`, no `OkHttp`, no `Retrofit`, no socket — appears in any source file. There is no analytics SDK, no crash reporter, and no advertising identifier.

## 8. Logging

Accessibility service logging is compiled behind a debug check: `a01/.../LaunchDetectionService.kt:64`, `a02/.../BlockOverlayService.kt:125`, `a04/.../BlockOverlayService.kt:173`.

**Known gap, to be closed before submission.** The spike activities in `a03` and `a05` call `Log.i` without a debug gate (`a03/.../CatalogActivity.kt:23`, `a05/.../UsageActivity.kt:38,48,57,67,80`), and those lines include package names. These are throwaway spike builds and never ship. Phase 24 must verify that no release build logs a package name; the specification already requires release logs to contain no application names. This gap is recorded here rather than glossed over, because the privacy policy must describe actual behaviour, not intended behaviour.

## 9. Declared data types for the Play Data safety form

| Question | Answer | Basis |
|---|---|---|
| Does the app collect or share user data? | No | section 7 |
| Is data transmitted off device? | No | section 7 |
| Is data encrypted in transit? | Not applicable, nothing is transmitted | section 7 |
| Can users request deletion? | Yes, uninstalling removes all of it; the app also offers clearing history | section 6 |
| Are app activity or app info collected? | Stored locally only, not collected in the Play sense | section 6 |
