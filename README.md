# BlockSocial Lite

A daily time limit for individual applications on Android, and a warning when a limit is spent. That is the whole product.

You pick an application, type how many minutes a day you want to spend in it, and get a full-screen question when you open it after the time is gone. The question can be answered either way. Nothing is forced, nothing is locked, and nothing is hidden from you.

Everything lives in [`lite/`](lite/). Start with:

- [`lite/README.md`](lite/README.md) — what it does, how it is built, and what has actually been run on a phone
- [`lite/docs/REQUIREMENTS.md`](lite/docs/REQUIREMENTS.md) — the specification, the scope boundary, and the reasoning behind every refusal

```
cd lite
./gradlew :app:assembleDebug
./gradlew :app:test
```

## History

This repository previously held a larger application — schedules, always-on blocking, timed bypass grants, an event history, a statistics tab, eleven Gradle modules — together with its plan, specification and design documents. It was replaced by the smaller one after the larger one turned out to be more machinery than its owner wanted to use.

Those files are gone from the working tree, not from the repository. Every commit up to and including `33c2226` still contains them, so `git log` and `git show` reach the whole of it.
