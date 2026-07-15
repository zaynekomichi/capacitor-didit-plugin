# capacitor-didit-plugin

Capacitor plugin that wraps the native [Didit](https://didit.me) identity-verification (KYC) SDKs for iOS and Android, so verification (document capture, liveness, NFC passport reading) runs fully in-app — no browser window.

- iOS: [didit-protocol/sdk-ios](https://github.com/didit-protocol/sdk-ios) (`DiditSDK` 4.x)
- Android: [didit-protocol/sdk-android](https://github.com/didit-protocol/sdk-android) (`me.didit:didit-sdk` 4.x)

Requires **Capacitor 8**, iOS 15+, Android minSdk 24 (Capacitor 8 defaults).

## Install

```bash
npm install capacitor-didit-plugin
npx cap sync
```

The plugin is auto-registered by Capacitor on both platforms — do **not** add `registerPlugin` calls in `MainActivity`, custom `CAPBridgeViewController` subclasses, or `packageClassList` patch scripts.

### iOS setup

**Swift Package Manager apps (the default for new Capacitor 8 projects): no dependency steps.** The plugin ships a `Package.swift` that pulls `DiditSDK` automatically — `npx cap sync ios` is enough. Skip to step 2 (Info.plist).

**CocoaPods apps (projects migrated from Capacitor ≤ 7):**

**1. Add the Didit SDK pod to your app's Podfile.** The Didit SDK is not published to the CocoaPods trunk; it is resolved from a podspec URL. In `ios/App/Podfile`, add this line inside the `target 'App' do` block (below `capacitor_pods` — Capacitor rewrites the `capacitor_pods` section on every sync, but lines you add directly in the target block persist):

```ruby
target 'App' do
  capacitor_pods
  # Add your Pods here
  pod 'DiditSDK/All', :podspec => 'https://raw.githubusercontent.com/didit-protocol/sdk-ios/main/DiditSDK.podspec'
end
```

Use the `All` subspec — the plugin's podspec depends on `DiditSDK/All`.

**2. Add usage descriptions to `ios/App/App/Info.plist`:**

```xml
<key>NSCameraUsageDescription</key>
<string>Camera access is required to scan your identity document and verify liveness.</string>
<key>NSMicrophoneUsageDescription</key>
<string>Microphone access is required for liveness video verification.</string>
<key>NSPhotoLibraryUsageDescription</key>
<string>Photo library access lets you upload document images.</string>
```

If you use NFC passport reading, also follow [Didit's NFC setup](https://github.com/didit-protocol/sdk-ios#nfc) (NFC capability, `NFCReaderUsageDescription`, ISO7816 select identifiers).

**3. Run `npx cap sync ios`** (which runs `pod install`).

#### iOS troubleshooting

- **`pod install` fails with "unknown project version" / cannot open `project.pbxproj`** — your project was saved in a new Xcode format (objectVersion 70, Xcode 16.3+/26) that older CocoaPods tooling can't parse. Update the `xcodeproj` gem to ≥ 1.28: `gem install xcodeproj` (or `sudo gem update xcodeproj`), then re-run `pod install`.
- **"Unable to find a specification for `DiditSDK/All`"** — the `pod 'DiditSDK/All', :podspec => …` line is missing from your Podfile (step 1).

### Android setup

The plugin's `build.gradle` declares the Didit Maven repository (`raw.githubusercontent.com/didit-protocol/sdk-android`) and JitPack (needed for some of the SDK's transitive dependencies) and injects them into the consuming build, applies Kotlin itself, and pins the Kotlin/Java toolchain to JDK 21 — your app needs **no** repository or Kotlin configuration in the default Capacitor project layout. A plain Capacitor 8 app builds with no extra steps.

**(Only if your `settings.gradle` centralizes repositories.)** If you use `dependencyResolutionManagement` with `RepositoriesMode.FAIL_ON_PROJECT_REPOS` or `PREFER_SETTINGS`, project-level repositories (including the ones this plugin injects) are ignored — add them centrally instead:

```groovy
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url "https://raw.githubusercontent.com/didit-protocol/sdk-android/main/repository" }
        maven { url "https://jitpack.io" }
    }
}
```

The default Capacitor app template does not use `dependencyResolutionManagement`, so most apps can skip this.

#### Android troubleshooting

- **`Unsupported class file major version 69` (or similar) when Gradle starts** — your default JDK is newer than Capacitor 8's Gradle supports. Build with JDK 21, e.g. `export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"` (Android Studio's bundled JDK) before running Gradle, or install Temurin 21.
- **`Could not find me.didit:didit-sdk`** — your build ignores project-level repositories; see the `dependencyResolutionManagement` note above.
- **`2 files found with path 'META-INF/versions/9/OSGI-INF/MANIFEST.MF'`** — happens when another dependency in your app ships the same multi-release-jar manifest as one of the Didit SDK's transitive dependencies (seen e.g. alongside OneSignal). Add the exclusion to `android/app/build.gradle` inside the `android { }` block:

  ```groovy
  packagingOptions {
      resources {
          excludes += 'META-INF/versions/9/OSGI-INF/MANIFEST.MF'
      }
  }
  ```
- **Out-of-memory / metaspace errors during build** — the Didit SDK pulls in Jetpack Compose; raise the Gradle heap in `android/gradle.properties`: `org.gradle.jvmargs=-Xmx4096m -XX:MaxMetaspaceSize=512m`.
- **Kotlin `jvmTarget` mismatch errors from *other* plugins** — some third-party Kotlin plugins don't pin a jvmTarget and default to your running JDK. Pin every Kotlin module in your app's root `android/build.gradle`:

  ```groovy
  subprojects {
      plugins.withId('org.jetbrains.kotlin.android') {
          project.extensions.getByName('kotlin').jvmToolchain(21)
      }
  }
  ```

  (This plugin already pins its own toolchain; the snippet is only for others that don't.)
- **R8 full mode issues in release builds** — the SDK ships consumer ProGuard rules; if you still hit R8 full-mode problems, set `android.enableR8.fullMode=false` in `gradle.properties`.

## Usage

Create a verification session **from your backend** (Didit `POST /v2/session/`), pass the session token to the app, then:

```typescript
import { DiditVerification } from 'capacitor-didit-plugin';

async function verifyIdentity(sessionToken: string) {
  try {
    const result = await DiditVerification.startVerification({ sessionToken });
    // result.status: 'Approved' | 'Pending' | 'Declined'
    // result.sessionId: reconcile with your backend / webhooks
    return result;
  } catch (err: any) {
    if (err.code === 'CANCELLED') {
      // user closed the flow
    }
    throw err;
  }
}
```

`Pending` means the session completed but the decision isn't final (processing or manual review) — treat the Didit webhook / API status on your backend as the source of truth.

## API

### `startVerification(options: { sessionToken: string }): Promise<DiditVerificationResult>`

Launches the native Didit verification flow and resolves when the user finishes.

```typescript
interface DiditVerificationResult {
  status: 'Approved' | 'Pending' | 'Declined';
  sessionId: string;
}
```

On web the call rejects as unavailable — gate it with `Capacitor.isNativePlatform()` and fall back to Didit's hosted web verification URL.

### Error codes

| `err.code`      | Meaning                                                     |
| --------------- | ----------------------------------------------------------- |
| `MISSING_TOKEN` | `sessionToken` was absent or empty.                         |
| `BUSY`          | A verification flow is already running.                     |
| `UNAVAILABLE`   | No activity / view controller to present from, or platform is web. |
| `CANCELLED`     | The user exited the flow before completing it.              |
| `FAILED`        | The SDK reported an error — see the error message.          |

## Migrating from an in-app (local) plugin

If you previously wired the Didit SDK into your app project directly, remove the old wiring after installing this package:

- **iOS:** delete the local `DiditVerificationPlugin.swift` / bridge files, the custom `ViewController` subclass with `registerPluginInstance`, any `capacitor.config.json` `packageClassList` patch script, and restore `Main.storyboard`'s controller class if you changed it. Keep the `pod 'DiditSDK/All'` Podfile line.
- **Android:** delete the local `DiditVerificationPlugin.kt`, remove `registerPlugin(DiditVerificationPlugin.class)` from `MainActivity`, and remove `me.didit:didit-sdk` plus the Didit Maven repo from your **app** module (the plugin brings both). Your app also no longer needs `apply plugin: 'kotlin-android'` or the Kotlin classpath if nothing else uses Kotlin.
- **JS:** replace imports of the local `src/plugins/DiditVerification` module with `import { DiditVerification } from 'capacitor-didit-plugin'`. The API is identical; cancellations now reject with `code: 'CANCELLED'`.

## License

MIT
