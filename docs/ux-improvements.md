# UX/UI presentation improvements

Branch: `codex/ux-ui-improvements`, based on `main` at `7e34726`.

This branch changes the presentation of the existing Stimulate features. It does not add product features, navigation destinations, dialogs, persistence, or backend requirements.

## Retained changes

- Reserve space for the existing Quick Start action so it does not cover Home's program rows.
- Keep the existing Start and + Session buttons visible below the scrolling program details. About content remains visible in its original flow.
- Move the existing Quick Start duration control above the point map. Point selection and Save retain their original behaviour.
- Use 16 sp input text and larger existing login error text; retain the earlier requested label and evidence-badge spacing changes.
- Fade disabled action text, label the simulated battery Demo, and clarify the existing simulation-speed labels.
- Use Stopped Early and a neutral icon for the existing early-stop summary. The session-ending and history-saving rules are unchanged.

The added draft saving/recovery, naming dialog, result reopening, profile bar, navigation shortcuts, extra area controls, and new summary flow have been removed. State, data models, persistence, authentication, and profile logic match the baseline.

## Local verification

Build, ten JVM tests, the three existing Android device tests, and lint run locally using the isolated verification application. The full device flow is also checked at a compact 320 dp width. No real account or hardware is used. The six existing lint warnings concern dependency/tooling updates.

Current presentation screenshots are in [ux-screenshots](ux-screenshots). Historical reference screenshots remain in `screenshots`.
