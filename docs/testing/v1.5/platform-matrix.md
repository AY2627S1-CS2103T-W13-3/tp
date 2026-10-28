---
layout: page
title: v1.5 dashboard platform matrix
---

This page records v1.5 dashboard usability evidence: automated layout boundaries and packaged-JAR checks on real desktops. Logical scene sizes in automated tests are not evidence of physical display scaling, font substitution or OS window behaviour; a combination counts as checked only after someone runs the packaged JAR on it.

## Automated layout boundaries

`DashboardLayoutBoundaryTest` composes the real `MainWindow` over temporary storage and lays it out at 853x480, 1536x864 and 1920x1080 logical pixels, which correspond to 1280x720 at 150%, 1920x1080 at 125% and 1920x1080 at 100%.

| Partition | Test | Result |
| --- | --- | --- |
| 1,000 clients; last row's `#` readable, and a five-digit index fits | `table_thousandClients_lastIndexStaysReadable` | Failed before the fix (row 1000 rendered as `1...` at 853x480); passes after fixing the `#` column at 56 px |
| Long unbroken email (125 characters) and address (153 characters) in the profile | `profile_longUnbrokenEmailAndAddress_wrapInsideViewport` | Pass |
| 80 pending cards: summary scrolls to the last card; profile stays visible | `pendingSummary_manyCards_scrollsToLastCardAndKeepsProfile` | Pass |
| 80-character unbroken name and 200-character action in a pending card | `pendingSummary_longNameAndMaximumAction_staysInsideSidebar` | Pass |

Already covered elsewhere, so not repeated: restored window geometry extremes (`DashboardShellTest.getWindowBounds_*`), recycled table rows (`PersonListPanelTest`), narrow-table horizontal scrolling (`PersonListPanelTest.narrowWidth_columnsDoNotFit_scrollsHorizontally`), long tags (`ClientProfilePanelTest`) and long feedback (`ResultDisplayTest`).

The wrapping tests were checked against a regression: turning off wrapping on the profile email and on the pending action makes them fail with an ellipsis-clipped value.

**Run record (27 October 2026):** macOS 26.6.2 (25G83), arm64, Homebrew OpenJDK 25.0.5.0.2. This machine has no Zulu JDK+FX, so the project's x86_64 JavaFX 17.0.7 natives could not load; the run supplied the official arm64 JavaFX 17.0.7 natives through `-Djava.library.path`. Source: uncommitted working tree on top of `81c4d65f`; repeat on the final commit. All four boundary tests and Checkstyle passed. In the full suite, only the three native-input tests in `CommandSubmissionTest` failed, because this terminal cannot send native keyboard/mouse input; they fail identically without this change.

**Observation:** at 853x480 the pending summary's viewport is about 99 px tall, shorter than one 114 px card. Scrolling still reaches every card, and the sidebar divider gives the summary more room. This is not treated as a defect, but screenshots and manual checks at 1280x720/150% should expect it.

**Fixture safety and repeatability.** `DisplayMatrixFixtureTest` invokes the actual generator with an explicit date, reloads all 1,000 records through storage, checks the 29 overdue / 2 due-today / 70 upcoming distribution and long values, and verifies that regeneration refuses to overwrite existing data.

## Composed-candidate record: 28 October 2026

The existing layout changes and documentation from PR #136 are merged. This follow-up adds repeatable fixture generation and a packaged backend check; it does **not** complete the physical display matrix.

- **Source:** [fb0a548cc69a61a9c99110c099ff1b4137d74e41](https://github.com/wkc75/tp/commit/fb0a548cc69a61a9c99110c099ff1b4137d74e41), a separate composed candidate containing the fixes for #130–#134 and these verification tools. This is not a merged release candidate on team master.
- **Artifact:** `PolicyHarbor.jar`, SHA-256 `dc4b250ee964a6640ba86d6c78d782f248fe6fe6e0a8e01d08a9b1b7827a7008`.
- **Environment:** macOS 26.6.2 (25G83), arm64; Zulu JDK+FX 25.0.3+9-LTS (Zulu25.34+17-CA); reference date 2026-10-28. Physical resolution/scaling was not verified.
- **Automated checks:** 624 tests passed, plus Checkstyle, coverage generation, the repository checks and packaging. Three native-input methods in `CommandSubmissionTest` were excluded locally: `submit_enterKey_submitsOnceAndRestoresTypingAfterSuccessAndFailure`, `submit_mouseClick_submitsOnceAndRestoresTypingAfterSuccessAndFailure`, and `submit_tabAndSpace_submitsOnceAndRestoresTypingAfterSuccessAndFailure`. All three failed the unchanged baseline because the initial native click did not reach the input. The exclusion is local; the committed CI configuration is unchanged.
- **Packaged backend:** `packagedBackendCheck` passed against production classes from this JAR with assertions enabled and 1,000 fictional clients. Filtering/index mapping, long-profile data, invalid-index rejection, follow-up reorder/clear/delete and save/reload passed. Selection was empty after reload.
- **Startup smoke only:** both ordinary `java -jar` and `java -ea -jar` launches loaded the fixture and reached UI startup without a fatal exception. The assertion-enabled launch left the data file byte-identical (SHA-256 `054d808d606109ae21510acf2be2337c3d504e47b36ab7905fc789e3cb0ef125`). The session could not inspect or interact with the Java window, so these are log/process observations, not proof of visual layout, Enter/Run, error focus recovery or scrolling. Each launch was stopped after the smoke check.

Single-run backend observations, measured inside `packagedBackendCheck` (including disk persistence for mutations):

| Operation | Time (ms) |
| --- | ---: |
| Load and initialise the 1,000-client model | 266.868 |
| `find Client` | 113.319 |
| `view 999` in the filtered list | 128.456 |
| `list` | 118.641 |
| `view 1` for the long-profile client | 110.783 |
| `followups` | 110.903 |
| Replace the first action and reorder it to the end | 238.268 |
| Clear that action | 219.444 |
| Delete the first remaining pending client | 220.864 |

These times exclude rendering and scrolling and are not a performance guarantee. All nine physical OS/resolution/scaling rows below remain **Pending**. Complete native input and visual acceptance on the final merged JAR before closing #135; CI and logical layout tests do not replace those checks. Screenshot claimants should use the fixture and narrow-viewport observation above, then record their actual platform and artifact.

## Packaged-JAR display matrix

### Setup

1. Build the candidate with `./gradlew shadowJar` and record its SHA-256 (`shasum -a 256 build/libs/PolicyHarbor.jar`, or `certutil -hashfile build\libs\PolicyHarbor.jar SHA256` on Windows) and the source commit.
2. Generate fictional data with `./gradlew displayMatrixFixture --args="/absolute/path/to/fresh-folder 2026-10-28"` (Windows: `gradlew.bat`). Replace the path and date with the folder and reference date for your check. Alternatively, run `DisplayMatrixFixture` (in `src/test/java/seedu/address/ui`) from the IDE with those arguments. It writes `data/addressbook.json` with 1,000 fictional clients and refuses to overwrite an existing file. Follow-up dates are relative to a reference date, which defaults to today and is printed. The app classifies follow-ups against the date it runs on, so generate the folder on the day of the check and record the printed date in the row. A second argument (`yyyy-MM-dd`) recreates the exact file another checker used.
3. Copy the JAR into that folder and launch it from a terminal there with `java -jar PolicyHarbor.jar`. Run `java -version` first and record the JDK.
4. Set the physical resolution and OS scaling for the row being checked.

### Separate backend acceptance and timing

Run `./gradlew packagedBackendCheck --args="/absolute/path/to/another-fresh-folder"` (Windows: `gradlew.bat`). This builds the JAR, loads production classes from that JAR ahead of test outputs, generates 1,000 fictional clients and checks filter/index mapping, long profile data, rejected input, follow-up reorder/clear/delete and save/reload. Assertions must be enabled; the task enables them and the checker refuses to run without them. The fixture refuses to replace existing client data.

The output identifies the loaded JAR and reports backend command times, including persistence for mutations. These are single-run observations, not a performance guarantee. They exclude rendering, scrolling, keyboard/mouse submission and physical display scaling; none of the desktop matrix rows can be marked passed from this command.

### Scenarios for each row

- **A. Table:** `list`, scroll to the end. Row `1000` is fully readable; `view 1000` opens Client 1000.
- **B. Long values:** `view 1`. The long name, phone, email, address and tags wrap inside the profile without an ellipsis, and the profile scrolls to its follow-up.
- **C. Pending summary:** scroll the summary to its last upcoming card, then drag the sidebar divider both ways. Both panels stay usable.
- **D. Commands:** Enter and Run each submit once; an invalid command keeps its text, shows **Error**, and typing continues without refocusing; F1 opens Help and Esc closes it.
- **E. Follow-ups:** `followups`, replace one action's date (the row reorders), clear one, delete one.
- **F. Restart:** close and relaunch in the same folder. Data persists; selection resets. Repeat the launch with `java -ea -jar PolicyHarbor.jar`.

### Results

| OS / version | JDK | Physical resolution / scaling | JAR SHA-256 / source | Date / checker | Scenarios | Result |
| --- | --- | --- | --- | --- | --- | --- |
| Windows | | 1920x1080 / 100% | | | | Pending |
| Windows | | 1920x1080 / 125% | | | | Pending |
| Windows | | 1280x720 / 150% | | | | Pending |
| macOS | | 1920x1080 / 100% | | | | Pending |
| macOS | | 1920x1080 / 125% | | | | Pending |
| macOS | | 1280x720 / 150% | | | | Pending |
| Linux | | 1920x1080 / 100% | | | | Pending |
| Linux | | 1920x1080 / 125% | | | | Pending |
| Linux | | 1280x720 / 150% | | | | Pending |

Fill a row only with checks actually performed, naming the checker. On platforms without a matching display, record the closest combination actually used in a new row, leaving the original pending. A peer checker may supply evidence for a platform the team lacks.
