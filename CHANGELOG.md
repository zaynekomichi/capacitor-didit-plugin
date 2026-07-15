# Changelog

## 0.1.0 (2026-07-15)

Initial release.

- `startVerification({ sessionToken })` launching the native Didit verification flow on iOS (DiditSDK 4.x via CocoaPods podspec URL or SPM) and Android (`me.didit:didit-sdk` 4.1.0).
- Typed results (`Approved` / `Pending` / `Declined` + `sessionId`) and error codes (`MISSING_TOKEN`, `BUSY`, `UNAVAILABLE`, `CANCELLED`, `FAILED`).
- Plugin ships its own Kotlin toolchain config and Didit/JitPack Maven repositories — no app-level Kotlin or repository setup needed.
