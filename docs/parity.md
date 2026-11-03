# Stimulate parity checklist

Reference: signed-in public web app, Stimulate module, inspected 2026-11-02. Native target: Kotlin / Jetpack Compose.

| Reference area | Android implementation | Verification |
|---|---|---|
| Colors and typography | Reference light/dark colors; bundled Mitr / IBM Plex fonts | 372 dp comparison + emulator screenshots |
| Preset library | All 11 visible programs; the disabled Sharp & Active preset is excluded | Static extraction from reference source |
| Program detail | Head/step map, timelines, descriptions, evidence, favorites, time, intensity, start/session | Emulator flow + source comparison |
| Session queue | Add/remove, order, per-item durations, intensity, closed-loop, Start Session, Repeat | Emulator flow; dose/timing unit tests |
| Quick Start | 19-point selection, regions, fixed/sweep, presets, logarithmic slider, guides, save | Emulator flow + screenshot |
| Custom builder | Step editing, point masks, frequency, power/duty, reorder, duplicate/delete/save | Device assertions for create/add/duplicate/reorder/delete/duration/save |
| Running session | Countdown, selected points, wave, pause/resume, stop, completion | Emulator pause/completion + engine unit tests |
| Energy summary | Per-point J/cm², average/maximum, intensity and duration | Dose unit tests + emulator screenshot |
| Closed-loop | Source response/noise model, light-off sampling, baseline/after averages, stress-triggered level reduction | Plan/dose and seeded replay tests; complete emulator session and EEG summary |
| History | Thai month calendar, daily records, monthly totals, Repeat | Emulator flow + persistence |
| Settings | Easy/Pro, speed, helmet values, theme, profiles/account | Build + emulator screenshots |
| Native lifecycle | Retains ViewModel during rotation; saves on background, restores paused | Device assertions for activity recreation and preferences reload, including continued dose/EEG; actual OS process-kill coverage remains limited |
| Login | Existing Supabase email endpoints; browser OAuth with PKCE | Compiled; real account flow requires user verification |
| Profiles | Full native form, birth/age, guardian details, consent, camera/gallery, local/cloud save | Local validation/edit/persistence emulator flow; cloud and image providers need real-account/device verification |

## Scope changes

- The bottom navigation bar is removed. The app opens directly to the Stimulate main screen after login; detail screens retain their back buttons. No Train, Evaluate, or Map routes exist.
- Per the requested spacing trial, expanded program descriptions place the evidence badge on its own line, 6 dp below the text, instead of the reference's inline badge.
- The 7-day strip leads to Stimulate History, keeping history available without Map.
- IAPF is not measured or fetched from an excluded Evaluate module. Default preset frequencies are used.
- Native status/navigation bars and adaptive screen sizing replace the website's phone mockup.
- The impedance and EEG response/noise models follow the reference's formulas. Fixed simulation increments and a persisted random seed support consistent session restoration. Random values differ between sessions and platforms. These values are not measurements.
- Profile creation/editing/photos are implemented. Clinic remote-management remains outside this build.
- Confirmation/reset email links use the existing website; users return to Android to sign in afterward. A direct native handoff needs backend-owner configuration.
- Local Android records are not the same browser-local records used by the website. No migration or cloud activity sync is claimed.

## Integrations that need external configuration

- Supabase allowlisted Android callback for Google OAuth: `brainstem://auth/callback`.
- Live helmet BLE service/characteristics and firmware contract, before replacing simulation.
- A backend activity API and migration contract, if shared web/Android session history is required.

The requested native Stimulate implementation and the visual corrections found in the second comparison are delivered. Native font rasterization, OS providers/system insets, browser scrollbar behavior, and randomized EEG readings differ by platform. Real-account verification and backend-owner configuration are separate external dependencies. See [the detailed audit](parity-audit-2026-11-02.md) for comparison evidence and those boundaries. Static data for all 11 presets / 23 steps was independently compared with a fresh public-source download; see [the data audit](reference-data-audit.json).

For backend configuration, see [database setup](database-setup.md). For APK/command-line testing, see [run without Android Studio](run-without-android-studio.md).
