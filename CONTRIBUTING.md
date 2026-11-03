# Contributing

This repository contains the native Kotlin / Jetpack Compose BrainSTEM Stimulate app. Preserve the requested scope: no Train, Evaluate, Map, or bottom Stimulate menu. The main screen opens directly after authentication.

## Git workflow

1. Work directly on `main`, as requested by the repository owner. Run `git pull --ff-only` before starting. Do not create additional branches unless explicitly requested.
2. Keep commits focused and use Conventional Commits: `feat:`, `fix:`, `test:`, `docs:`, `ci:`, or `chore:`. Explain why a non-obvious behavior changed in the body.
3. Stage explicit files and review `git diff --cached` and `git diff --cached --check` before committing. Do not commit unrelated changes or generated output.
4. Run relevant local checks before pushing `main`. Record resulting behavior, validation, screenshots for visible changes, and any remaining external configuration. A pull request is optional when explicitly requested.
5. Keep CI enabled as background verification. Report failures accurately and fix relevant defects; do not wait for a hosted emulator between routine local UI iterations. Never force-push `main` or rewrite shared history.
6. If an update cannot fast-forward, inspect and preserve both sets of changes, resolve conflicts, and rerun relevant checks. Do not discard another person's changes.

Use local Gradle builds and the Android emulator for the normal feedback loop. Keep commits focused so each change remains reviewable and reversible on `main`.

## Build and validation

Use Android Studio's bundled JDK. The committed daemon criteria request Java 25; CI explicitly installs Temurin 25. Java/Kotlin bytecode targets Java 17. Install Android SDK platform 37.0 and Build Tools 36.0.0. Keep Gradle, AGP, Kotlin, dependencies, and GitHub Actions pinned to reviewed versions.

```powershell
.\gradlew.bat :app:assembleDebug :app:testVerificationUnitTest :app:lintDebug :app:lintVerification
.\gradlew.bat :app:connectedVerificationAndroidTest
git diff --check
```

Device tests use a separate verification application ID so normal debug-app data is preserved. Never use real online accounts or patient data in automated tests. Add tests for meaningful behavior changes; do not add tests that merely restate trivial layout implementation.

For UI parity changes, compare the same screen and state with the reference, document deliberate scope changes, and capture current emulator screenshots. Passing a build or smoke test does not establish pixel-perfect parity.

## Code and data

- Keep UI, state, persistence, authentication, and simulation responsibilities separate. Use explicit data models, bounded input validation, accessible control descriptions, and lifecycle-aware work.
- Follow Kotlin's official formatting conventions. Keep source text UTF-8 and use LF in cross-platform source files.
- Preserve exact preset data unless the reference changes or the requested behavior explicitly changes. Run `node tools/audit-reference.cjs` when reviewing a newly downloaded reference snapshot.
- Retain bundled font licenses and reference-source provenance.
- Commit source, tests, build definitions, and documentation. Keep `.gradle`, `.kotlin`, IDE state, local SDK paths, APKs/AABs, signing files, ZIP exports, logs, and user data out of Git. CI artifacts or releases hold distributable builds.
- Only public client configuration may be bundled. The Supabase publishable key is public by design; service-role keys, personal access tokens, passwords, signing keys, OAuth secrets, and session data must never be committed.
- Use GitHub repository secrets for release signing/CI credentials if release automation is added. Do not print credentials in logs or place them in shell arguments.

## Pull request checklist

- [ ] Focused scope and clear before/after behavior.
- [ ] Relevant build, lint, unit, and device checks pass.
- [ ] Visible changes have current screenshots and comparison notes.
- [ ] No credentials, personal data, local paths, generated builds, or unrelated files are staged.
- [ ] Documentation and external setup instructions reflect the final implementation.
