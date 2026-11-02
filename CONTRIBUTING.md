# Contributing

This repository contains the native Kotlin / Jetpack Compose BrainSTEM Stimulate app. Preserve the requested scope: no Train, Evaluate, Map, or bottom Stimulate menu. The main screen opens directly after authentication.

## Git workflow

1. Start from an up-to-date `main` and create a short-lived branch such as `feat/native-stimulate-parity`, `fix/session-timer`, or `docs/android-setup`.
2. Keep commits focused and use Conventional Commits: `feat:`, `fix:`, `test:`, `docs:`, `ci:`, or `chore:`. Explain why a non-obvious behavior changed in the body.
3. Stage explicit files and review `git diff --cached` and `git diff --cached --check` before committing. Do not commit unrelated changes or generated output.
4. Push the branch and open a pull request. State the problem, resulting behavior, validation, screenshots for visible changes, and any remaining external configuration.
5. Require passing CI before merge. Prefer squash merge for a single focused feature; preserve separate commits when they form useful reviewable units. Never force-push `main`, rewrite shared history, or merge failing checks.
6. Use `git pull --ff-only` for routine updates. If conflicts occur, resolve them on the feature branch and rerun relevant checks. Do not discard another person's changes.

For a multi-contributor repository, configure GitHub branch protection to require pull requests and the `Android checks` status check, with reviews appropriate to the team's size. Avoid rules that prevent the sole maintainer from completing routine work.

## Build and validation

Use Android Studio's bundled JDK, or JDK 17 or newer compatible with Gradle 9.4.1. Install Android SDK platform 37.0 and Build Tools 36.0.0. Keep Gradle, AGP, Kotlin, dependencies, and GitHub Actions pinned to reviewed versions.

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
