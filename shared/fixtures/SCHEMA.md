# Fixture Corpus Schema

The contract every platform implements. A case states inputs and one expected domain result. It never states a mechanism.

Enforced by `shared/fixtures-validator`, which fails loudly on anything this document forbids. The validator is the schema; this file is what it means.

---

## Files

| File | Kind | What it pins down |
|---|---|---|
| `schedule-cases.json` | `schedule` | when a schedule rule is active |
| `bypass-cases.json` | `bypass` | when a temporary grant suppresses a block |
| `daily-limit-cases.json` | `daily-limit` | when a daily limit has been reached |
| `rule-priority-cases.json` | `rule-priority` | which rule wins when several apply |
| `block-event-contract.json` | `block-event` | the frozen `BlockEvent` field list |
| `business-rules.json` | — | the rules from `docs/PRODUCT.md` that the corpus must cover |

## Envelope

Every case file:

```json
{
  "contractVersion": 1,
  "kind": "schedule",
  "cases": [ ... ]
}
```

`contractVersion` is identical in every file. Changing the shape of a case raises it, in every file, in one commit.

## A case

```json
{
  "id": "schedule-weekday-inside-interval",
  "title": "Inside a weekday evening interval",
  "covers": ["BR-03"],
  "given": { },
  "when": { },
  "expect": { },
  "atRisk": { "platform": "ios", "reason": "..." }
}
```

| Field | Required | Rule |
|---|---|---|
| `id` | yes | unique across **all** files, lower-kebab-case |
| `title` | yes | a sentence a person can read |
| `covers` | yes | at least one business-rule id from `business-rules.json` |
| `given` | yes | the world before the question |
| `when` | yes | the question |
| `expect` | yes | one domain result |
| `atRisk` | no | present only when a known platform limit could make this case unsatisfiable |

## Time

Two rules, because ambiguous time is how contracts rot.

1. **An instant is always written with an explicit offset**, for example `2026-10-25T02:30:00+02:00`. Never a bare local time, because on a daylight-saving day a bare local time is ambiguous or does not exist.
2. **`localTimeNote` states what that instant reads as on the wall clock** in the case's zone, for example `"2026-10-25 02:30 (first pass, CEST)"`. The validator recomputes it and fails if the note disagrees. The note exists so a person can read the file; the check exists so the note can be trusted.

`zone` is an IANA identifier.

## Interval semantics, decided here

These were genuinely ambiguous. The corpus decides them, and both platforms follow.

| Question | Decision |
|---|---|
| Is the start inclusive? | yes |
| Is the end inclusive? | no. An interval is `[start, end)` |
| Which weekday owns an overnight interval? | the day it **starts**. `22:00-07:00` on Monday runs from Monday 22:00 to Tuesday 07:00 |
| A start time that does not exist, on a spring-forward day? | the interval begins at the first instant that does exist |
| A time that occurs twice, on an autumn-back day? | both passes are inside the interval |
| Does a daily limit reset in the device's current zone? | yes. Changing zone changes the day window immediately |

## `given` by kind

### schedule

```json
"rule": {
  "mode": "SCHEDULE",
  "enabled": true,
  "daysOfWeek": ["MONDAY"],
  "startLocalTime": "22:00",
  "endLocalTime": "07:00"
}
```

`daysOfWeek` uses `MONDAY` … `SUNDAY`. Times are `HH:mm`, 24-hour.

### bypass

```json
"grant": {
  "forApp": "app-a",
  "grantedAtWallClock": "2026-07-27T20:00:00+05:00",
  "grantedAtMonotonicMillis": 5000000,
  "durationMinutes": 5
}
```

`grant` may be `null`, meaning no grant exists.

**Two clocks, and why.** `grantedAtWallClock` is the device's own clock, which a user can change. `grantedAtMonotonicMillis` is a counter that only moves forward while the device is running and restarts at zero when it boots; every platform has one. A grant that trusts only the wall clock is defeated by moving the clock back, which spike `A-03` demonstrated on a device. A grant that trusts only the monotonic counter is defeated by a reboot. The contract therefore carries both, and the case expectations encode which one decides.

`forApp` is an opaque handle, never a package name or a bundle identifier. Cases use `app-a`, `app-b`.

### daily-limit

```json
"rule": { "mode": "DAILY_LIMIT", "enabled": true, "dailyLimitMinutes": 30 },
"usageSessions": [
  { "forApp": "app-a", "from": "...", "to": "..." }
]
```

A session with `to` omitted is still running at the moment of the question.

### rule-priority

```json
"rules": [ { "id": "r1", ... }, { "id": "r2", ... } ],
"grant": null,
"usageSessions": [ ]
```

## `when`

```json
{
  "zone": "Europe/Berlin",
  "instant": "2026-10-25T02:30:00+02:00",
  "localTimeNote": "2026-10-25 02:30 (first pass, CEST)",
  "forApp": "app-a",
  "monotonicMillis": 5060000
}
```

`zone` and `instant` are always present. `localTimeNote` is required wherever local time is the point of the case. `forApp` and `monotonicMillis` appear where the kind needs them.

## `expect`

| Kind | Fields |
|---|---|
| schedule | `restrictionActive` |
| bypass | `suppressesBlock`, and `evaluation` as a diagnostic |
| daily-limit | `measuredMinutesToday`, `restrictionActive`, sometimes `blockPresentedImmediately` |
| rule-priority | `restrictionActive`, `primaryReason`, `allReasons` |

**`suppressesBlock` is what conformance is measured on.** `evaluation` is a five-value classification a platform should be able to report for debugging: `ACTIVE`, `EXPIRED`, `ACTIVE_AFTER_REBOOT`, `EXPIRED_AFTER_REBOOT`, `DISCARDED_CLOCK_MOVED_BEFORE_GRANT`. A platform that reaches the right `suppressesBlock` by a different internal route still conforms.

`primaryReason` and each entry of `allReasons` is one of `ALWAYS`, `FOCUS_SESSION`, `SCHEDULE`, `DAILY_LIMIT`. `FOCUS_SESSION` is reserved and unused in the MVP; it exists so the priority order does not have to change when it ships.

## `atRisk`

```json
"atRisk": {
  "platform": "ios",
  "reason": "The iOS bypass is expected to be built on usage thresholds rather than a stored grant, so reboot semantics may not map."
}
```

Present only where a limit already documented in `docs/TECHNICAL_SPECIFICATION.md` section 4 could make the case unsatisfiable. It is a shortlist for phase 34, not a prediction. A case marked at risk is still binding until phase 34 says otherwise, and if it changes there, it changes for both platforms.

`atRisk` is the **only** place in the corpus where a platform may be named, because naming the platform is the entire point of the field. The validator exempts it from the platform check and still forbids mechanism names inside it: an at-risk note states a limit, not an implementation.

## What may not appear anywhere in the corpus

No platform class, API, package or framework name. No `com.` or `org.` identifiers. No mechanism words such as accessibility service, shield, usage statistics manager, elapsed realtime, or system uptime. A case describes what must be true, not how a platform learns it.

The validator greps for these and fails.
