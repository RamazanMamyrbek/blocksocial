@AGENTS.md

# Claude Code Rules

- The product is **BlockSocial Lite**, and it is the only one. Everything lives under `lite/`.
- Read `lite/docs/REQUIREMENTS.md` before changing behaviour, and `lite/README.md` for what has actually been run
- Never claim completion without running the relevant tests
- Never present an unverified assumption as a confirmed capability
- Driving the emulator UI with `uiautomator` temporarily disables other accessibility services, including this one. Use `adb exec-out screencap` and fixed coordinates when testing blocking, or the service will look broken when it is not.
