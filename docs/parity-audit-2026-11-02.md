# Stimulate comparison — 2 November 2026

**Result: closer to the reference after corrections, but full 1:1 UI/UX parity is not certified.** The bottom menu remains removed, as requested.

The original signed-in Stimulate module was inspected through its rendered DOM, computed styles, visible controls, and public source. The native app was checked through emulator screenshots and an isolated instrumentation build. Its main program data was compared against a fresh download of the public website, not only the earlier local reference.

## What matches exactly in the static data

All **11 visible programs and 23 steps** match the fresh website definitions. This includes IDs, order, categories, descriptions, notes, regions, point coordinates, selected points, fixed/sweep frequencies, power values, durations, evidence text, and references. The disabled Sharp & Active preset remains excluded in both visible libraries.

The machine-readable result is in [reference-data-audit.json](reference-data-audit.json). The PBM source has not changed since the initial download. The full HTML byte hashes differ because the first snapshot was saved as decoded text; the extracted PBM module and parsed data are identical.

## Differences corrected during this check

| Area | Difference found | Correction |
|---|---|---|
| Text layout | Generic line height used 1.50 instead of the reference's 1.55; several fractional font sizes were rounded | Shared labels now support fractional sizes and use 1.55 line height |
| Steppers | Shared 29 dp buttons and 15 sp mono numerals differed from the reference's contextual controls | Normal controls use 32 dp buttons / 18 sp display numerals; queue controls use 24 dp buttons / 12.5 sp mono numerals; narrow sweep controls have their own sizing |
| Holding a stepper | Press-and-hold repetition was absent | Added the reference's 450 ms delay and 90 ms repeat interval; physical long-press review remains advisable |
| Pro / closed-loop switches | Thumb, track, label size, and padding differed | Aligned to the 26×16 track, 12 dp thumb, 12 sp label, and reference padding |
| Fixed-frequency scale | Labels were evenly distributed | Labels now use the reference's 0 / 30 / 45 / 56 / 74 / 100 percent positions |
| Band and sweep choices | Small labels, weak selected border, and a freely wrapping sweep list | Band names/numerals, selected styles, and two-column sweep layout now follow the source |
| Sweep description | Every range was labeled simply “Sweep” | Restored Short / Long / Broad Sweep classification and dwell-time text |
| Descending builder sweeps | Shared Quick Start bounds prevented From exceeding To in the builder | Builder endpoints independently support 1–100 Hz; Quick Start keeps its reference constraints. Emulator regression sets From 13 / To 12 |
| Recent-session Repeat | Four recent unsaved Quick Start records could hide an older valid preset session | Filter invalid entries before selecting the latest three unique sessions; added a regression assertion |
| Headset-check behavior | F4 always started obstructed; successful checks were not remembered | Ported the reference's initial ranges, obstruction/noise probabilities, convergence, correction actions, same-day cache, and automatic approval after a fresh successful check |
| Headset-check diagram | Used the 19-point PBM diagram and two rows of values | Added the dedicated F3/F4/Fz/Cz/Pz + Ref/GND diagram and seven-column values, plus the 50 Hz bar |
| Running controls | Frequency was 40 sp instead of the final CSS's 26 sp; Stop used a solid primary button | Corrected the frequency size, time-label spacing, title alignment, and Stop tint |
| Primary actions | Uniform body text differed from display-font button labels and smaller subtitles | Corrected action typography, secondary text, Quick Start action-bar padding, and Save sizing |
| Session overview | Seven-day active count was absent; title was too small; recent-session choices were vertical | Restored active-day count, 17 sp session title, and horizontally scrolling Repeat choices |
| Program options | All cards used the same 24 dp radius | Program options now use the source's 18 dp radius, padding, and gap |

## Verification

- Debug APK build: **passed**.
- Unit tests: **7 passed**, covering dose, step boundaries, pause, sweep endpoints, proportional duration, closed-loop timing, and the headset gate.
- Android 17 / API 37 instrumentation flow: **passed**. It covers categories, favorites, queue, all/clear selection, sweep presets, first and repeated headset checks, pause/resume/completion, history, descending builder sweep, custom-program creation, Repeat regression, and dark mode.
- Android lint: **0 errors**, 21 non-blocking warnings.
- Twelve current screenshots: [screenshots](screenshots).

Instrumentation now uses `com.brainstem.stimulate.verification`, keeping the normal debug app's saved data separate. It does not log into or modify a real online account.

## Remaining differences and limits

1. **Visuals and motion:** the native layout has not passed an automated pixel-difference comparison at identical logical dimensions. The inspected web phone had a 372×800 CSS-pixel screen; the emulator uses a different logical size and native system insets. Gradients, some borders, inline list evidence badges, head glow geometry, waveform motion, completion sound, and certain layout details still differ or need a matching-size review. Passing interaction tests is not proof of pixel-perfect output.
2. **EEG simulation:** the source's randomized response/noise model has not been fully ported. Android still uses its earlier deterministic EEG response model. The impedance-check model was aligned in this pass, but simulated readings themselves will naturally vary. Neither the inspected website nor this build demonstrates a live helmet connection.
3. **Account flow:** email endpoints compile, but real-account login/signup/reset was not exercised. Google needs `brainstem://auth/callback` allowlisted in Supabase. Password reset finishes on the website instead of a native reset screen.
4. **Profiles:** Android retrieves existing cloud profiles; creating/editing cloud profiles, photos, and full original onboarding remain incomplete. New Android profiles are local.
5. **Shared/excluded modules:** Evaluate-dependent IAPF, clinic assignments managed through the original shared shell, and their related data paths are not reproduced. The original activity records are browser-local; they are not automatically the Android installation's records.

The requested menu removal, direct opening of Stimulate, native system bars, and routing the seven-day strip to Stimulate History are deliberate scope changes rather than accidental omissions.

Do not describe this build as a complete 1:1 copy based on these checks. The verified statement is: **exact preset data, tested main Stimulate flows, and the corrections above; remaining differences explicitly listed.**
