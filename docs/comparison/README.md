# Visual comparison evidence

Reference: the signed-in Stimulate module at `https://brainstem-monitor.netlify.app/app/`, inspected on 2 November 2026.

The native captures in [../screenshots](../screenshots) use a 372×848 mdpi emulator, with a 372×800 app area after 24-pixel system insets at the top and bottom. The browser iframe body was set to 372 CSS pixels wide. Scrollable reference pages use a 387×800 browser viewport because the desktop scrollbar occupies 15 pixels; shorter pages use 372×800. A few reference JPEGs were scaled by the browser capture during viewport transitions, so use the inspected DOM geometry, not those images, for numeric measurements.

[quick-start-side-by-side.png](quick-start-side-by-side.png) compares a fresh reference Quick Start at left with native Kotlin at right. It crops the scrollbar and Android system insets; it does not resize either app. The original web menu is intentionally absent in Android. The button bar remains available at the bottom.

Two Quick Start references are retained: `reference-quick-fixed-fresh.jpg` was captured immediately after opening a new Stimulate iframe; `reference-quick-fixed.jpg` followed an earlier visit to Program detail. The website reuses SVG gradient IDs, and the second navigation order can suppress its point glows. The native drawing retains the fresh module's glow and selected-point outlines without sharing SVG IDs.

Other references cover Home, Program, Builder, Fixed/Sweep, running, completion, and Easy/Pro settings. Reference Pro screenshots show Pro controls; native Easy captures intentionally show Easy controls. Native screens also include dark settings, history, headset check, profile editing, baseline, and the closed-loop summary. The same native flow also passed with a 320×640 app area; its temporary captures remain under ignored `app/build/compact-comparison`.

The review compares data, layout, controls, and behavior. Browser JPEG compression, font rasterization, fractional pixel rounding, scrollbars, OS bars, and native OS pickers prevent a meaningful universal zero-pixel-difference assertion. See [the audit](../parity-audit-2026-11-02.md) for implemented corrections and external account/device checks.

Follow-up on 3 November: `reference-info-expanded.jpg` and `reference-builder-alpha-copy.jpg` record the two marked areas. Native captures `20-program-info-expanded.png` and `21-builder-alpha-copy.png` reproduce those states, including the filled info button and fully faded single-step delete button. The reference info capture includes an existing queue; compare the marked row itself rather than its absolute vertical position.

The user subsequently requested a spacing trial for the evidence badge. Current native capture `20-program-info-expanded.png` puts it on its own line with 6 dp above it. The inline badge in `marked-controls-side-by-side.png` records the earlier reference-matching version and is superseded by this deliberate spacing change.

The later Duplicate & Edit comment is recorded by native captures `22-program-actions.png` and `23-custom-program-actions.png` in `../screenshots`. The preset action spans the content width, includes the source copy icon and uses `.btn.sm` geometry. Custom actions use the matching edit/copy/trash icons and Delete color. The About panel's missing icon/caption/value columns above the action were corrected in the same pass.
