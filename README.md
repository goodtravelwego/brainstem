# BrainSTEM Stimulate — native Android

An Android Studio project written in Kotlin and Jetpack Compose, based on the Stimulate/PBM module at https://brainstem-monitor.netlify.app/app/ inspected on 2 November 2026. The UI is native: no WebView or embedded website. Train, Evaluate, and Map have no screens or navigation destinations.

## Open and run

1. In Android Studio, choose **Open** and select `C:\dev\brainstem` (the folder containing `settings.gradle.kts`).
2. Let Gradle sync. This project uses Gradle 9.4.1, Android Gradle Plugin 9.2.1, Kotlin/Compose compiler 2.2.10, and Android SDK 37. Android Studio's bundled JDK works.
3. Select the `app` configuration and an emulator or Android phone, then Run.
4. The login page opens first. **ใช้แบบไม่ล็อกอิน (เก็บในเครื่องนี้)** opens the complete offline simulator without an account.

Without opening Android Studio: see [install/run instructions](docs/run-without-android-studio.md). For later backend access: see [database setup](docs/database-setup.md).

Minimum Android version: Android 8.0 (API 26). Debug APK: `app/build/outputs/apk/debug/app-debug.apk`.

On this computer `local.properties` already points to the installed SDK. On another computer, let Android Studio generate that file for its own SDK location.

## Implemented Stimulate flows

- Relax, Focus, Learn, My Programs, and Favorites categories; the exact 11 visible preset programs, Thai descriptions, frequency ranges, durations, steps, regions, and evidence text from the reference.
- Expandable program descriptions, program detail head map, selectable steps, duration adjustment, favorites, direct Start, and adding a program to the session.
- Today's Session queue: add/remove, reorder, adjust each duration, intensity, closed-loop toggle, Start Session, and recent-session Repeat.
- Quick Start: all 19 individually selectable 10–20 points, All/Clear, region guide, Fixed/Sweep, logarithmic frequency slider, band/sweep presets, band guide, duration, Easy/Pro intensity, duty cycle, and Save as a custom program.
- Pro program builder: create, edit, duplicate, delete, add/duplicate/reorder/delete steps, region/custom points, fixed/sweep frequencies, duration, power, and duty cycle.
- Session timer, head visualization, frequency wave, Pause/Resume, Stop, automatic completion, chime, delivered-energy calculation by point, and summary.
- Simulated headset impedance check; closed-loop baseline, stimulation blocks of at most 3 minutes, 45-second measurements, final 60-second measurement, and simulated EEG summary.
- Per-profile local history, Thai calendar, daily/monthly totals, Repeat, persisted queue/custom programs/preferences, and interrupted-session restoration in a paused state.
- Easy/Pro modes, ×20/real-time simulation, helmet specifications, light/dark/system themes, profile switching, and sign out.
- Native onboarding/profile editing: real/nickname, birth date and age, sex, user group, handedness, guardian details, online consent, camera/gallery photos cropped to 128×128 JPEG, local persistence, and cloud profile/photo requests.

## Account setup and remaining integrations

The public Supabase URL and **publishable** key already exposed by the reference website are in `app/src/main/assets/backend.json`. Email/password sign-in, sign-up requests, password reset emails, token refresh, and retrieval of existing account profiles use that service. Tokens are encrypted using Android Keystore; no browser credentials are copied into this project.

**Google sign-in requires the backend owner to allow `brainstem://auth/callback` under Supabase → Authentication → URL Configuration → Redirect URLs.** The app uses browser-based Google OAuth with PKCE and handles the callback. Until that URL is configured, Google may return to the website instead of Android. Existing website Google login does not automatically authorize a new Android redirect. Authentication was not tested with a real account/password.

Confirmation/reset email links return to the existing website, whose redirect is already supported. After confirmation/reset, return to Android and sign in with the email/password; the native mail screen explains that handoff. Profile creation/editing uses the reference `app_profile` table and `profile-photos` bucket with the signed-in user's token. Real-account cloud operations, OAuth, and actual camera/gallery providers have not been exercised in automation. Clinic remote assignments and cross-device activity sync are not implemented. Session histories/custom programs remain local to this app installation; the website's browser storage is not imported.

**Hardware remains a simulator, matching the reference prototype's default behavior.** No real BLE connection, measured EEG, or helmet control is claimed. The EEG model ports the source's randomized IAPF, frequency-dependent responses, rest decay, noise/stress spikes, light-off sampling, baseline/after averages, and high-beta intensity reduction. A persisted random seed and fixed simulation increments keep interrupted-session replay consistent across UI frame timings. Real time changes the simulation clock; it does not connect hardware.

Evaluate-dependent alpha measurement is outside this project's scope. Presets use their reference default frequencies. The Alpha card is informational; the 7-day strip opens Stimulate history, since Map was excluded.

The requested Stimulate flows are implemented and have undergone a second visual comparison at a 372 dp app width. Native font rasterization, system insets, browser scrollbars, and OS pickers still differ between platforms; no pixel-identical rendering or live hardware/backend validation is claimed. Android system bars replace the website's decorative phone bezel/status bar. There is no bottom navigation menu; the app opens directly to the Stimulate main screen after login. See `docs/parity-audit-2026-11-02.md` for the checked differences, corrections, and external dependencies, and `docs/parity.md` for scope changes.

## Build and check

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
.\gradlew.bat :app:assembleDebug :app:testVerificationUnitTest :app:lintDebug
.\gradlew.bat :app:connectedVerificationAndroidTest
```

Verified on 2–3 November 2026: debug APK build, ten unit tests, lint, and the Android 17/API 37 emulator flow. The tests cover dose, timing, pause, sweeps, scaling, headset checks, and seeded EEG replay. The device flow covers categories, favorite/queue, Quick Start, headset checks, pause/completion, history, builder, Repeat, dark mode, profile validation/edit/persistence, and a complete closed-loop session. It never signs into a real account. Its separate verification application ID preserves the normal debug app's local data.

Twenty-one emulator screenshots are in `docs/screenshots`. The test writes new screenshots to Gradle's additional-test-output directory, under `app/build/outputs/connected_android_test_additional_output`.

The current captures use a 372×848 display at mdpi, with a 372×800 application area inside system insets. A second flow run checks the compact 320×640 application area. Session controls stay visible in a fixed bottom row while the content scrolls; the EEG scope scales with screen width. This layout is covered by explicit Pause/Resume visibility and state assertions.

Run `powershell -ExecutionPolicy Bypass -File tools/package-android.ps1` to build/export the debug APK and Android Studio source ZIP. It exports reviewed tracked source files and excludes local configuration/caches. After an existing build, `-SkipBuild` only exports the artifacts.

## Source guide

- `Models.kt`: program models, regions/bands, timing and dose engine.
- `Repository.kt`: static program loading and local JSON persistence.
- `Auth.kt`: Supabase HTTP calls, PKCE, token encryption and refresh.
- `AppState.kt`: profile-scoped state and app actions.
- `Components.kt` / `ReferenceIcons.kt`: typography, palette, controls, native head diagram, and 28 vectors converted from the reference SVG paths.
- `App.kt`, `ProgramScreens.kt`, `SessionScreens.kt`, `SettingsScreens.kt`, `ProfileScreens.kt`: native screens.
- `CompletionChime.kt`: PCM synthesis of the reference's four-note completion sound.
- `reference/pbm.html`: downloaded public PBM reference, retained for comparison only and never embedded in Android.
- `tools/extract-reference.cjs`: extracts only static program data and public backend configuration from the downloaded reference.

The bundled Mitr, IBM Plex Sans Thai, and IBM Plex Mono fonts are distributed under their included Open Font Licenses in `app/src/main/assets`.

## Repository workflow

Work directly on `main`, as requested by the repository owner, with focused Conventional Commits and relevant local checks before pushing. The repository includes an optional PR template, a Gradle distribution checksum, and GitHub Actions pinned to full commit SHAs. CI builds the APK, runs unit tests/lint, and publishes reports/APK artifacts in the background. API 35 device tests can also be run manually through GitHub Actions. Local SDK paths, credentials, IDE caches, signing material, and built APK/ZIP files are excluded. See [CONTRIBUTING.md](CONTRIBUTING.md).
