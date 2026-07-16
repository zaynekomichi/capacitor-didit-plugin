# Changelog

## 0.1.1 (2026-07-16)

- Android: launch the Didit verification UI via the SDK state flow — `DiditSdk.startVerification` only prepares the session on Android, so the plugin now collects `DiditSdk.state` and calls `launchVerificationUI` when the session is `Ready`. Without this the verification screen never appeared.
- Android: reject immediately with code `FAILED` when session preparation errors before the UI launches (bad token, network failure) instead of hanging forever.
- Android: clear the busy guard on every settle path so a failed or dropped session can't permanently lock `startVerification` with `BUSY`.

## 0.1.0 (2026-07-15)

Initial release.

- `startVerification({ sessionToken })` launching the native Didit verification flow on iOS (DiditSDK 4.x via CocoaPods podspec URL or SPM) and Android (`me.didit:didit-sdk` 4.1.0).
- Typed results (`Approved` / `Pending` / `Declined` + `sessionId`) and error codes (`MISSING_TOKEN`, `BUSY`, `UNAVAILABLE`, `CANCELLED`, `FAILED`).
- Plugin ships its own Kotlin toolchain config and Didit/JitPack Maven repositories — no app-level Kotlin or repository setup needed.
