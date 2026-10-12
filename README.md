# TapGet Android UI Prototype (Phase 2)

A starter Android Studio project for the approved TapGet brand direction and the first simulated-download UI prototype.

## Included
- Kotlin + Jetpack Compose, Material 3
- Downloads, Queue, Completed, Download Details, Add Download dialog, Settings
- Light/dark theme toggle
- Search on the Downloads screen
- Simulated transfer progress only (no actual network download engine)
- TapGet Android launcher resources from the supplied brand package

## Open/build
1. Open this folder in Android Studio (Ladybug or newer; recent stable recommended).
2. Allow Gradle sync and install the required Android SDK (compile SDK 35).
3. Run the `app` configuration on an emulator or Android device.

If your local Android Studio selects a newer compatible Gradle/Android Gradle Plugin, allow it to update the project.
No Gradle wrapper is included; Android Studio can import the project and use its configured Gradle installation.

## Install brand resources
The supplied adaptive icon resources are copied to `app/src/main/res/`. Confirm the app icon in the manifest/build and test adaptive masks on device.
Use `app/src/main/assets/brand/tapget-logo-primary.png` on light surfaces and `app/src/main/assets/brand/tapget-logo-reversed.png` on dark surfaces. The logo PNGs are included under `app/src/main/assets/brand/`.

## Current limitations
- No actual downloading, background service, retry engine, queue persistence, file storage, permissions flow, or notifications.
- Demo rows and progress are in-memory sample data; they reset when the app process restarts.
- This is an initial runnable prototype scaffold, not a verified APK. Build and UI acceptance testing are still required.

## GitHub Actions: CI and signed release APK

Two workflows are included under `.github/workflows/`:

- `ci.yml` builds the debug APK and runs local unit-test tasks on pushes, pull requests, or manual dispatch. The debug APK is available as the `tapget-debug-apk` workflow artifact.
- `build-signed-apk.yml` builds a signed universal APK plus four ABI-specific APKs (`armeabi-v7a`, `arm64-v8a`, `x86`, and `x86_64`) when manually dispatched or when a `v*` tag is pushed. It verifies every APK signature and uploads all APKs as a workflow artifact.
- When a `v*` tag is pushed, the same workflow also creates/updates a GitHub Release with generated release notes and attaches all signed APKs. A manual workflow run uploads artifacts but does not publish a GitHub Release.

### Configure signing secrets before running the signed workflow

Create a release keystore locally and keep it private. Do **not** commit the keystore or passwords to the repository. Base64-encode the keystore file, then add these repository secrets in **Settings → Secrets and variables → Actions**:

- `ANDROID_KEYSTORE_BASE64` — the keystore file encoded as one Base64 string
- `ANDROID_KEYSTORE_PASSWORD` — keystore password
- `ANDROID_KEY_ALIAS` — key alias
- `ANDROID_KEY_PASSWORD` — key password

For example, on Linux/macOS, encode the file with `base64 -w 0 tapget-release.jks` (on macOS, use `base64 tapget-release.jks` and remove any line breaks before saving). Store the output directly as a GitHub secret; never paste it into source files or commit it.

Then open **Actions → Build signed APKs and publish GitHub Release → Run workflow** for a test build. To publish a release, push a version tag such as `v0.1.0`; GitHub Actions will build and verify all APKs, then attach them to the GitHub Release. Ensure repository Actions settings allow the workflow to create releases; if your organization restricts workflow permissions, grant the workflow write access to repository contents. Keep secure backups of the keystore and passwords: updates published under the same Android application ID must be signed with the same key.

**Note:** These workflows have been added to the project source but have not been executed in GitHub Actions from this environment. A successful build/signature verification is not yet claimed.
