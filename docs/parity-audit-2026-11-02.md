# Stimulate comparison — 2 November 2026

**Result: the requested native Stimulate flows are implemented, with the second comparison and corrections below completed.** The bottom menu remains removed, as requested. Real-account authentication/cloud/provider checks require the backend owner or a real device and are listed separately. Native rendering is not a pixel-identical web renderer.

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
| Account styling | Account screens reused Stimulate's coral action color and dimmed module wordmark | Restored the shared account blue, full-color wordmark, compact native inputs, and lighter guest action |
| Simulated battery | Session drain truncated elapsed five-minute units | Uses the reference's nearest-unit rounding with a minimum drain of one |
| Small-screen session controls | EEG measurement content placed Resume/Stop below the visible area on a 320×640 display | Controls stay in a fixed bottom row; only session content scrolls. The EEG chart now uses the source's 640:300 aspect ratio and the head scales to 70% of screen width |

## Second visual comparison and corrections

The comparison emulator uses **372×848 at mdpi**, giving a **372×800 app area** after Android's top/bottom insets. The reference iframe was sized to a 372 CSS-pixel body; its desktop scrollbar sometimes requires a 387-pixel viewport. Comparisons use the content area, not the website's bottom menu or Android system bars. Screenshots are retained in [comparison](comparison) and [screenshots](screenshots). Browser JPEG captures and native PNGs are not suitable for a strict zero-pixel-difference assertion; geometry was also inspected through source DOM bounds and native semantic bounds.

| Area | Correction in this pass |
|---|---|
| Palette | Corrected page background to source E9EFEC / 0A1110 rather than the separate app-bezel background |
| Shared geometry | Corrected header position, card borders/radii/padding/gaps, fractional note sizing, battery, and grouped seven-day dots |
| Reference icons | Converted all 28 source SVG icons to Android vector resources, including previously missing edit/copy/delete/bulb/clock icons |
| Home | Corrected small info icons, program subtitles, evidence colors, Alpha card dash/spacing, Pro new-program action sizing, and FAB shadow |
| Weekly total | Matched per-day minute rounding before summing the week; added a regression covering small sessions, older sessions, and future dates |
| Quick Start | Restored its white selected mode tab, grouped All/Clear controls, 20 sp counter, head-card gradient, 48 dp frequency controls, logarithmic range track, sweep bar, and divided option rows in source order |
| Number layout | Constrained large frequency/time/Alpha line boxes to the source height; Android's natural font metrics had expanded the frequency number to 92 px despite a 58 sp requested line height |
| Builder | Recreated expanded/collapsed step cards, number markers, inline tools, compact frequency controls, band-color chips, duration/power columns, duty row, and dashed Add Step |
| Builder save | Avoided overwriting a newer draft with a stale captured object when the name field reports an edit; device assertions now cover add/duplicate/up/down/delete/duration/save |
| Program detail | Corrected title/subtitle grouping, selected step rows, region captions from the exact source data, multi-step timelines/gradients, and About/reference disclosure geometry |
| Settings | Recreated small section headings, feature chips, 2-column helmet fields and native numeric inputs |
| Profiles | Changed profile switching to a bottom sheet with account row, per-profile avatar colors, selected border, and edit/new-user actions |
| Running | Restored light/dark radial backgrounds, dark control colors, fixed control positions, timeline outline, continuous head/EEG animation time, and the source wave's doubled SVG viewport scale |
| Summary | Corrected completion block, tile sizing, Alpha/status/band chips, and 300:84 chart geometry with normal-range shade, dashed zero line, endpoint markers, and labels inside the plot |
| History | Corrected calendar spacing/type/dots, record cards, and the single shared monthly-total container |

After the 2 November comparison, the original reference was left in Easy mode with Quick Start restored to Fixed 10 Hz, all points, 10 minutes. The reference simulation test added one one-minute **local simulated session** and drained its simulated battery by one; it did not write a cloud profile or control a helmet.

## Verification

Follow-up on **3 November 2026**, addressing the two marked browser comments:

- Program info now has the reference's filled ink/white selected state. Description text uses ink, a 4 dp top margin, 12.5 sp / 1.45 typography, and an inline evidence badge. The test opens/closes it and verifies that the app remains on Home.
- Builder up/down/copy/delete controls use the exact vector paths, 30×30 dp containers and 9 dp corners. Copy/delete icons are 15 dp; arrows are 16 dp. A disabled control fades as a whole to 30%, including its background. Delete is disabled for one step, enabled after duplication, and disabled again after deleting back to one step; device assertions cover those transitions.
- The same pass corrected the builder's 130 dp mode selector and 38 dp / 22 sp fixed-frequency and duty controls. Compact duration controls distribute their available width, preserving the full button width.
- The original browser draft, **Alpha Calm (copy)**, was preserved. A separate temporary reference tab was used to inspect info behavior, then closed.

- Debug APK build: **passed**.
- Unit tests: **10 passed**, covering dose, step boundaries, pause, sweeps, proportional duration, closed-loop timing, headset gating, seeded replay, and frequency-dependent light-off EEG sampling.
- Android 17 / API 37 instrumentation flow: **passed**. It covers categories, favorites, queue, all/clear selection, sweep presets, first and repeated headset checks, pause/resume/completion, history, descending builder sweep, custom-program creation, Repeat regression, and dark mode.
- Local completion check on 3 November adds two lifecycle regressions: activity recreation retains the ViewModel/session and pauses it; a fresh ViewModel restores the saved profile, queue, custom programs, favorites, helmet settings, session plan, seed, elapsed time, dose and EEG. Resuming the restored session produces the same subsequent dose/EEG as the original. This tests preferences reload and activity recreation, not an operating-system process kill. The flow fixture explicitly resets speed, helmet settings and closed-loop state so the tests can run together. **All three device tests passed together locally in 43.6 seconds.**
- Android lint: **0 errors, 6 warnings** after the 3 November editor cleanup. All remaining lint warnings recommend newer Gradle/dependency versions; the 20 source-level lint warnings were corrected.
- Twenty-three current screenshots: [screenshots](screenshots), including native profile editing, baseline measurement, closed-loop summary, and program action buttons.

Instrumentation now uses `com.brainstem.stimulate.verification`, keeping the normal debug app's saved data separate. It does not log into or modify a real online account.

## Platform boundaries and external validation

- **Native rendering:** the app is Kotlin/Compose. Android font rasterization, rounding, system bars, date picker, photo provider, and responsive behavior differ from browser rendering. The source desktop scrollbar also affects viewport-relative run-head sizing. The second pass inspected representative same-width layouts and corrected the concrete differences above; it does not establish byte-for-byte pixel equality across operating systems. The intentional bottom-menu removal also changes bottom anchoring.
- **Simulator:** both the inspected web module and Android use simulated helmet/EEG behavior. Android retains fixed 100 ms engine increments and a persisted seed for reproducible restoration. Random readings and frame timing differ between separate sessions/platforms. No live helmet connection is claimed.
- **Authentication:** email endpoints, Google PKCE, token refresh and encrypted token storage are implemented. No real account credentials were used in verification. Google needs the backend owner to allow `brainstem://auth/callback`; the requester has no Supabase administration access. Email confirmation/reset uses the configured HTTPS `emailRedirect`, followed by Android email/password sign-in.
- **Profiles:** full fields, local validation/edit/persistence, avatar conversion, consent, cloud profile requests and private photo requests are implemented. Local tests pass. Actual cloud writes, Google return, refresh across a real sign-in, and camera/gallery providers require real-account/device validation. [Database setup](database-setup.md) explains the existing-project owner change and supplies an unexecuted new-project starter schema with ownership policies.
- **Excluded/shared modules:** Train, Evaluate, Map and clinic remote management are outside the requested implementation. The Alpha card explains default-frequency use, and the seven-day strip opens Stimulate History. Browser-local history is not imported and cross-device activity synchronization is not added.

Build success and passing interaction tests are reported as build/behavior evidence, not as proof of universal pixel equality or a verified backend integration.

## Android Studio diagnostics follow-up — 3 November

The IDE showed red `Literals must be surrounded by whitespace` errors for compressed conditional expressions inside Kotlin string templates, even though the Gradle compiler accepted those expressions. Spaced the expressions in Run/Summary, the session queue and the builder. Android Studio's **Project Errors** pane then reported **No errors found by the IDE**. The open `SessionScreens.kt` file has no yellow code warnings; its remaining `eyeoff` spelling hint is an internal icon identifier.

The cleanup also gives the session revision a meaningful role in reading the displayed step, uses window-container dimensions for responsive sizing, moves default dimensions inside composables, puts optional modifiers first, uses the equivalent Android KTX preference/URI/bitmap helpers, declares the existing no-backup policy for older Android versions, and removes redundant annotations/string templates. Debug/verification builds, ten unit tests and both lint variants pass locally; all three device tests passed again together in 53.9 seconds. Quick Start, paused running and profile-form captures were visually checked after the modifier changes. No dependency versions or reference data were changed.

## Marked Duplicate & Edit follow-up — 3 November

The marked reference control uses `.btn.sm` with `flex:1`; the native implementation incorrectly reused a small outlined pill. Preset programs now have a full-width, borderless surface-2 button with a 16 dp copy vector, 13 sp medium text, 12 dp corners and 12/8 dp horizontal/vertical padding. Its line box follows the reference's 20.15 px line height, giving approximately 36 dp overall height rather than Android's natural 38 dp. Custom-program Edit/Duplicate/Delete use the same icon-button styling at their content widths, with the source danger color on Delete.

The About section above the marked control also now includes its missing pulse/target/bulb icons, 18/58/flexible column widths with 6 dp gaps, medium value text, 10 dp vertical padding, and source paragraph/evidence line heights.

Device assertions check that Duplicate & Edit spans the 372 dp screen's content width between 16 dp insets and has the reference height. Preset and custom duplication open unsaved copies with new IDs and preserved steps; Edit retains the original program ID. The screenshots are [preset actions](screenshots/22-program-actions.png) and [custom actions](screenshots/23-custom-program-actions.png). The existing Easy/Pro visibility and save/cancel behavior are retained.

## Git delivery

The implementation is merged into `main`, PR #1 is merged, and the feature branch is removed. Per the owner's 3 November instruction, all development, builds and tests run locally; completed work is pushed directly to `main`. The GitHub Actions workflow was removed and the running hosted job was canceled. Focused Conventional Commits, ignored local/build/credential files, explicit line endings, contribution guidelines, an optional PR template and the verified Gradle distribution checksum remain. Java 25 matches the committed daemon criteria. No shared history was rewritten.
