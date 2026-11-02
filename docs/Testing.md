---
layout: page
title: Testing guide
---

## Run automated checks

Use Java 25; on Mac select the [prescribed Zulu JDK+FX](https://se-education.org/guides/tutorials/javaInstallationMac.html) for both Gradle and the IDE.

```bash
./gradlew check coverage
```

On Windows use `gradlew.bat`. `check` runs the JUnit suite and Checkstyle. Reports are under `build/reports/tests/test`, `build/reports/checkstyle` and `build/reports/jacoco`. On macOS/Linux also run `.github/run-checks.sh` and `git diff --check`.

During development, run a focused class with:

```bash
./gradlew test --tests 'seedu.address.logic.commands.ViewCommandTest'
```

A focused run does not establish whole-suite coverage. Run the full suite before reporting overall results. Use `clean` when stale build outputs are a concern; it is not required for every test run.

IntelliJ can run a class, package or `src/test/java` through its JUnit runner. Ensure its configured JDK matches Gradle's.

## Assertions during development

Gradle explicitly enables assertions for `Test` and `JavaExec` tasks, including `test`, `run` and `runShadow`. An assertion failure signals an internal invariant violation; investigate it rather than disabling assertions to make a check pass. Assertions must have no side effects and must not be the only validation for user input, external files or recoverable errors.

For a direct IntelliJ application launch, enable `-ea` in its VM options as described in [Setting up](SettingUp.html#setting-up-the-project-on-your-computer). Build the packaged application with `./gradlew shadowJar`. From the project root, run each command below separately, closing the application between runs:

```bash
java -ea -jar build/libs/PolicyHarbor.jar
java -jar build/libs/PolicyHarbor.jar
```

For isolated manual checks, copy the built JAR into a fresh writable folder with fictional data, then run that copy. The packaged app stores its data/preferences/logs/lock beside the physical JAR; passing the original build JAR's absolute path from a different working folder is not isolation. Both modes must preserve normal validation and save/load protection. The Gradle daemon's JVM arguments or a task dry run alone do not prove assertions are active in the application JVM.

## Test responsibilities

| Level | What it checks | Examples |
| --- | --- | --- |
| Unit | Domain constraints, parsers and pure display values | `FollowUpTest`, `ViewCommandParserTest`, `ClientProfilePanelTest` |
| Integration | Model transitions and cooperating components | `ClientProfileSelectionTest`, `LogicManagerTest`, `JsonAddressBookStorageTest` |
| Workflow | Real command routing, persistence, failure preservation and restart | `ClientRecordCommandsTest`, `ClientProfileWorkflowIntegrationTest` |

Tests use fixed dates and temporary data folders. Storage failure tests inject write/move failures rather than relying on OS permissions. Keep successful and rejected commands' data/view assertions together.

Pure display-function tests do not prove FXML binding, focus, wrapping or scaling. Follow the Developer Guide's [manual testing appendix](DeveloperGuide.html#appendix-instructions-for-manual-testing) against the actual JAR before a release, and record the tested platform/runtime and remaining gaps.

## Published v1.6 verification

The [published v1.6 release](https://github.com/AY2627S1-CS2103T-W13-3/tp/releases/tag/v1.6) uses [source tag `v1.6`](https://github.com/AY2627S1-CS2103T-W13-3/tp/tree/v1.6) at `dda85cd74d709880b744d1b5d9485390d0cd0f07`. Its [PolicyHarbor.jar](https://github.com/AY2627S1-CS2103T-W13-3/tp/releases/download/v1.6/PolicyHarbor.jar) is **12,592,618 bytes**, with SHA-256:

```text
f70a19f2a6c82c5006656c0a62513e43ad163f2be04f6e97611fe80d3c58bc71
```

**Uploaded-asset check (3 November 2026):** the official download passed ZIP integrity and executable-manifest checks and matched GitHub's asset digest. In a fresh writable folder on macOS with Zulu JDK+FX 25.0.3, the downloaded JAR opened the native **PolicyHarbor v1.6** window with an empty client list and closed normally. It was byte-for-byte identical to the clean build at `7412c0175a868ae4e16b47431d40010e16c5b55a` and the earlier candidate documented below. Only `docs/UserGuide.md` changed between that build revision and the release tag; application sources, bundled resources and build inputs are identical. [Java CI](https://github.com/AY2627S1-CS2103T-W13-3/tp/actions/runs/41481917391) and [Pages deployment](https://github.com/AY2627S1-CS2103T-W13-3/tp/actions/runs/41481917337) passed for the tagged revision.

**Native macOS checks on the identical pre-upload JAR (3 November 2026):** with Zulu JDK+FX 25.0.3, the exact course filename `[CS2103T-W13-3][PolicyHarbor].jar` launched from a different working folder. Enter added a synthetic client; Run recorded a follow-up; viewing, editing an apostrophe-containing email and searching an uppercase tag succeeded. An invalid phone edit preserved the command draft, selected profile and saved bytes. Normal exit and an assertions-enabled restart retained the Client ID, contact details, tag and follow-up. The caller folder stayed empty; application files remained beside the JAR.

These are focused macOS checks. Native Windows/Linux GUI checks, physical display/scaling combinations, clipboard-write rejection and Firefox website validation remain unverified. The known mixed-script rendering and extreme JSON-nesting limitations remain documented in the [User Guide](UserGuide.html#known-limitations); neither release publication nor green CI establishes their resolution. The older records below retain their original scope.

## v1.6 candidate documentation verification

On 2 November 2026, the guide audit used source `e185a9cc568a0cbde2225cc7e3ff3c200c39f568` on macOS with Zulu JDK+FX 25.0.3. `check coverage shadowJar` passed all **901 tests**, with no failures or skips, and both Checkstyle tasks. Independent source reviews covered every UG/DG section; 49 isolated command/storage assertions checked guide examples, validation, feedback and restart behaviour. All nine referenced diagram images contain PlantUML source matching the committed diagrams.

The current `images/Ui.png` is a 2880 × 1714 scene capture of `MainWindow` loaded from that candidate JAR with ten fictional clients, pending mode and Rachel's profile selected. It was captured on 2 November using the real JAR-folder path; no interface elements were composited. The candidate JAR's SHA-256 is `f70a19f2a6c82c5006656c0a62513e43ad163f2be04f6e97611fe80d3c58bc71`.

These checks cover the candidate and documentation. The [published v1.6 verification](#published-v16-verification) above records the subsequent uploaded-asset check and remaining platform/browser gaps; the older release records below remain historical evidence.

## Published v1.5 verification

The [published v1.5 release](https://github.com/AY2627S1-CS2103T-W13-3/tp/releases/tag/v1.5) uses [source tag `v1.5`](https://github.com/AY2627S1-CS2103T-W13-3/tp/tree/v1.5) at `cb060cf3460df83499e9eea4ab9865d9ebe664c3`. Its [PolicyHarbor.jar](https://github.com/AY2627S1-CS2103T-W13-3/tp/releases/download/v1.5/PolicyHarbor.jar) is **12,587,362 bytes**, with SHA-256:

```text
1ff50ee4666c1b9177a481f58c805d6971f850a27776512b396c4298cf9e5d8c
```

**Uploaded-asset check (29 October 2026):** the official download passed ZIP integrity and executable-manifest checks, matched GitHub's asset digest, and was byte-for-byte identical to the separately built artifact from the tagged source. In a fresh folder on macOS 26.6.2 arm64 with Zulu JDK+FX 25.0.3, `java -jar PolicyHarbor.jar` started and displayed the native window title `PolicyHarbor v1.5`. The published binary was not rebuilt or replaced during this check.

The earlier comprehensive QA used source `97c10e2ad63e20b00db58657d5cd06182de0ae6f`, a **12,587,304-byte** JAR before the final invisible-only address fix in PR #249. That different candidate has SHA-256:

```text
f2624a0707b078aa636e3b062a93659f0f84d4e33fd87f3186548020b60e1f82
```

Retain its results as historical evidence, not as the final release checksum. This uploaded-asset check repeated startup and identity verification; it does not claim a new complete GUI regression, Windows/Linux desktop verification or completion of the physical display matrix. The alternate-working-directory file-location concern remained in that published binary; v1.6 uses the physical JAR folder.

## v1.5 dashboard layout and platform matrix

`DashboardLayoutBoundaryTest` lays out the real window at the logical sizes of the release display matrix and checks a 1,000-client table, long unbroken profile values and a full pending summary. `DisplayMatrixFixture` writes the matching 1,000-client data file for packaged-JAR checks. Run `./gradlew displayMatrixFixture --args="/absolute/path/to/fresh-folder yyyy-MM-dd"`; the folder must not already contain a client-data file. `DisplayMatrixFixtureTest` checks round-trip loading, the reference-date distribution and refusal to overwrite existing data. Use `./gradlew packagedBackendCheck --args="/absolute/path/to/another-fresh-folder"` for a self-checking 1,000-client workflow and backend timings against the built JAR. This does not exercise keyboard/mouse input or rendering. Record automated results and every packaged OS/resolution/scaling check in the [v1.5 platform matrix](testing/v1.5/platform-matrix.html); combinations nobody has run stay pending.

The [28 October composed-candidate record](testing/v1.5/platform-matrix.html#composed-candidate-record-28-october-2026) identifies the exact source and JAR, 624 passing tests, three locally excluded native-input tests, packaged backend timings and limited ordinary/`-ea` startup observations. Physical resolution/scaling and desktop interaction remain pending; #135 is not complete.

## v1.4 integrated dashboard

`DashboardIntegrationTest` exercises the real window, Logic and temporary JSON storage: filtered indices versus the global summary, canonical selection after pending reordering, clear/delete, failed-save preservation, explicit error recovery, published-date refresh, observer cleanup and logical layout sizes.

The shown-window focus test in `ResultDisplayTest` and the shown-window repeated-overflow test in `ErrorFeedbackLayoutTest` need an interactive desktop. The macOS Actions profile skips these two tests unless `POLICYHARBOR_NATIVE_UI_TESTS=true`: the hosted worker aborts with SIGABRT during native window interaction. Scene-level focus, input retention, layout, feedback selection and clipboard regressions still run on every CI platform. The offscreen overflow test retains scrollbar, complete-text scrolling, selection, focus and compact-recovery assertions at all three logical sizes, including on macOS Actions. A local check using that profile must report the two skipped tests explicitly. Native packaged-app interaction and the real OS/resolution/scaling matrix are separate evidence; logical scene-size checks do not prove display scaling.

The three Robot-based command-submission tests were retired after reproducible desktop-activation/input failures. `CommandSubmissionTest` retains scene-level Enter/Run/Tab-Space routing, logical focus, rejected-input retention and failed-save rollback/retry checks. These do not replace native keyboard/mouse coverage. Before release, use the packaged JAR to verify Enter, mouse Run and Tab/Space separately with a successful command, a parse error, an invalid client ID and a save failure in disposable data. Check exactly-once submission, clearing after success, retained input/data after failure, and typing immediately afterwards without clicking or requesting focus again. Record the JAR checksum, platform/runtime and actual outcome; historical automated counts/exclusions in the platform matrix remain records of those earlier runs.

The Help restoration regression checks `HelpWindow.focus()` on an unshown stage: it must clear a pre-set iconified state, retain the same stage and remain safe when repeated. It runs on every CI platform without a skip or desktop opt-in. A shown stage's iconified state is controlled by the OS window manager, so this regression does not assert synchronous native minimise/restore transitions. For desktop verification, open Help, minimise it, then reopen it through the Help menu and F1 separately; allow the desktop to settle and confirm that the same window is restored and focused. The component regression does not prove that native interaction. See the [JavaFX Stage iconification contract](https://openjfx.io/javadoc/17/javafx.graphics/javafx/stage/Stage.html#iconifiedProperty()).

See the [Developer Guide's dashboard procedure](DeveloperGuide.html#integrated-v14-dashboard-checks) for manual acceptance and the [v1.4 release notes](releases/v1.4.html) for the user-visible increment.

### v1.4 release GUI verification record

Evidence is recorded by source revision; checks on an earlier packaged app do not establish the same coverage for the final JAR.

- **Integration baseline `ffda0137`:** packaged checks passed on macOS arm64 with Zulu 25+FX for ordinary and assertion-enabled startup, Enter/Run, typing after a rejected command, recovery, filtered table with the global pending summary, F1/Escape and save/restart. The product screenshots were captured from this packaged app.
- **Pre-release audit `9aced71d345fdbd6c9a5de59b81a121b86d36d0a` (21 October 2026):** fresh startup, add via Enter, follow-up via Run, profile selection, filtered table versus global summary, invalid-index input/focus retention, typing recovery, pending mode, F1/Escape and save/restart passed. Restart with `-ea` preserved saved clients/actions and reset selection; validation and recovery also passed. Saved bytes remained unchanged across restart and read-only commands.
- **Final v1.4 `28ed61635311779743324517bcdce154102cce39` (21 October 2026):** fresh packaged startup and `list` passed. The broader GUI scenarios above, assertion-enabled launch and save/restart were not repeated against this final JAR.

The pre-release audit and final smoke check used macOS 26.6.2 (25G83), arm64, with Zulu JDK+FX 25.0.3+9-LTS (Zulu25.34+17-CA). Physical resolution/scaling was not recorded, so these runs do not establish the display matrix below.

The [published v1.4 release](https://github.com/AY2627S1-CS2103T-W13-3/tp/releases/tag/v1.4) identifies the final source revision above. Its `PolicyHarbor.jar` is 12,558,262 bytes with SHA-256:

```text
7ac65543efedbd13b34664dbff2ba091b96fa79a36705cf9be1f90b4358435b2
```

GitHub's reported asset digest matches the locally built JAR used for the final smoke check. Downloading and relaunching the uploaded asset was not separately verified.

On that final source, `check coverage shadowJar` passed locally using the committed macOS Actions test profile through a temporary init script: 576 tests, 572 passed and the four native desktop tests present at that revision skipped. Checkstyle, coverage generation and JAR packaging passed. [CI run 39600670010](https://github.com/AY2627S1-CS2103T-W13-3/tp/actions/runs/39600670010) passed on Linux, macOS and Windows for the same source revision. These automated checks are separate from packaged desktop verification.

The evidence table scrolls horizontally when needed; keyboard users can focus it and use the arrow keys.

<div role="region" aria-label="Release verification evidence" tabindex="0" style="max-width: 100%; overflow-x: auto;" markdown="1">

| Release check | Evidence/status |
| --- | --- |
| Missing saved screen | `DashboardIntegrationTest.window_unavailableSavedScreen_restoresInsidePrimaryWithoutDiscardingPreferences` verifies the real window constructor with unavailable saved coordinates, primary-screen bounds and retained preferences. Physical monitor unplug/reconnect remains untested. |
| Invalid client-data startup | `DashboardStartupIntegrationTest` loads malformed JSON through MainApp, opens the composed UI, submits a successful query and rejected mutation, and checks the persistent warning, empty table/summary and unchanged saved bytes. |
| Broader GUI scenarios on the final JAR | Pending repetition of Enter/Run, input/focus recovery, filtered/global views, F1/Escape, assertion-enabled launch and save/restart on `28ed6163`; only fresh startup and `list` were checked on this artifact. |
| Uploaded asset download/relaunch | Pending; the reported digest matches the locally tested JAR, but no separate download/relaunch check was performed. |
| Windows packaged GUI | Pending; platform CI does not replace running the packaged JAR. |
| Linux packaged GUI | Pending; virtual-display component CI does not replace a real desktop check. |
| 1920x1080 at 100% and 125%, on each supported OS | Physical display/scaling checks pending. |
| 1280x720 at 150%, on each supported OS | Physical display/scaling checks pending; the 853x480 logical scene test is automated evidence only. |
| Clipboard rejection | Pending; record whether the environment can exercise a rejected clipboard write. |

</div>

When adding verification evidence, record the exact source commit, JAR SHA-256, OS/JDK, actual resolution/scaling, date, result and evidence for each exercised case. Keep unavailable combinations explicitly pending rather than treating publication, a screenshot or green CI as proof.
