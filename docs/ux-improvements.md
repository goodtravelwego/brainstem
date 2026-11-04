# UX/UI improvement branch

Branch: `codex/ux-ui-improvements`, based on `main` at `7e34726`.

These changes intentionally improve the native Stimulate experience beyond the reference website's layout. The preset data, simulation model, and Stimulate-only scope remain the basis of the app. This branch does not claim pixel equality with the website.

## Changes

- The active profile, guest/local-storage context, and simulation speed are visible on the main screens and during sessions. Simulated battery information is labelled Demo. Settings distinguish a fast 20× demo from normal 1× simulation.
- Leaving an edited builder draft offers Save changes, Discard changes, or Keep editing. Drafts are stored locally per profile and recovered after an interrupted app session. Reverting a change does not trigger a false warning.
- Program Start and + Session actions stay visible beneath the scrolling detail. About is expandable. Home reserves space for Quick Start rather than covering library rows, and the empty session's Choose a program button scrolls to the library.
- Quick Start shows duration near the top and initially collapses the manual point map. Custom opens point editing. Saving asks for a program name and confirms the result. Sweep, duty cycle, power, and closed-loop controls have additional explanations.
- Early termination says Session Ended / Stopped Early instead of Complete. Sessions shorter than five seconds explain that they were not saved. Android Back pauses a running session and never resumes an already paused session.
- Home has an explicit History action. Tapping a history card reopens its saved results; Repeat remains a separate action. New records include the profile name, planned duration, and simulation speed. Older local records remain readable.
- Login validates missing/invalid email and missing password beside the relevant input and focuses the first invalid field. Input text is 16 sp; the previously enlarged login labels are retained.

## Local verification

The verification build uses a separate application ID and isolated guest data. Tests do not log into a real account, operate real hardware, or alter the normal debug app's data.

- Ten JVM tests cover session calculations, timing, pause, sweeps, scaling, and seeded EEG replay.
- Nine Android tests cover the full Stimulate flow, activity/session restoration, draft protection and recovery, profile isolation, named Quick Start saving, visible actions, login validation, saved-result navigation, and pause/early-end behaviour.
- Standard checks use a 372 dp display width / 800 dp app area. The full Stimulate flow and six UX tests also pass with a 320 dp display width / 640 dp app area.
- Build and lint checks run locally. The six existing lint warnings concern available dependency/tooling updates; there are no lint errors.

Reviewed screenshots are in [ux-screenshots](ux-screenshots). Historical reference-comparison screenshots remain in `screenshots`.

## Remaining external work

Google OAuth still requires the backend owner to allow `brainstem://auth/callback`. Real-account cloud operations and camera/gallery providers have not been exercised. Helmet/EEG data remain simulated. This UX pass does not constitute a full translation, TalkBack, or large-font accessibility audit.
