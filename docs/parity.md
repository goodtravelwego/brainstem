# Stimulate parity checklist

Reference: signed-in public web app, Stimulate module, inspected 2026-11-02. Native target: Kotlin / Jetpack Compose.

| Reference area | Android implementation | Verification |
|---|---|---|
| Colors and typography | Reference light/dark colors; bundled Mitr / IBM Plex fonts | Emulator screenshots |
| Preset library | All 11 visible programs; the disabled Sharp & Active preset is excluded | Static extraction from reference source |
| Program detail | Head/step map, timelines, descriptions, evidence, favorites, time, intensity, start/session | Emulator flow + source comparison |
| Session queue | Add/remove, order, per-item durations, intensity, closed-loop, Start Session, Repeat | Emulator flow; dose/timing unit tests |
| Quick Start | 19-point selection, regions, fixed/sweep, presets, logarithmic slider, guides, save | Emulator flow + screenshot |
| Custom builder | Step editing, point masks, frequency, power/duty, reorder, duplicate/delete/save | Emulator creation/editing flow |
| Running session | Countdown, selected points, wave, pause/resume, stop, completion | Emulator pause/completion + engine unit tests |
| Energy summary | Per-point J/cm², average/maximum, intensity and duration | Dose unit tests + emulator screenshot |
| Closed-loop | Simulated check, baseline/measure/after plan, simulated EEG | Plan/dose unit tests; emulator checks impedance fix and enabling/disabling; UI is simulation |
| History | Thai month calendar, daily records, monthly totals, Repeat | Emulator flow + persistence |
| Settings | Easy/Pro, speed, helmet values, theme, profiles/account | Build + emulator screenshots |
| Native lifecycle | Retains ViewModel during rotation; saves on background, restores paused | Implemented; process-death/device coverage remains limited |
| Login | Existing Supabase email endpoints; browser OAuth with PKCE | Compiled; real account flow requires user verification |

## Scope changes

- The bottom navigation bar is removed. The app opens directly to the Stimulate main screen after login; detail screens retain their back buttons. No Train, Evaluate, or Map routes exist.
- The 7-day strip leads to Stimulate History, keeping history available without Map.
- IAPF is not measured or fetched from an excluded Evaluate module. Default preset frequencies are used.
- Native status/navigation bars and adaptive screen sizing replace the website's phone mockup.
- The impedance-check model now follows the reference's ranges, obstruction/noise probabilities, corrections, and same-day caching. The EEG response model remains a deterministic approximation. These values are not measurements.
- Android-local profile addition is supported; full profile cloud editing/photos and clinic remote-management are outside this build.
- Local Android records are not the same browser-local records used by the website. No migration or cloud activity sync is claimed.

## Integrations that need external configuration

- Supabase allowlisted Android callback for Google OAuth: `brainstem://auth/callback`.
- Live helmet BLE service/characteristics and firmware contract, before replacing simulation.
- A backend activity API and migration contract, if shared web/Android session history is required.

Pixel-perfect parity has not been certified. See [the detailed audit](parity-audit-2026-11-02.md) for measured differences, fixes, test evidence, and remaining gaps. Static data for all 11 presets / 23 steps was independently compared with a fresh public-source download; see [the data audit](reference-data-audit.json).
