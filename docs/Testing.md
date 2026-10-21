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

For a direct IntelliJ application launch, enable `-ea` in its VM options as described in [Setting up](SettingUp.md#setting-up-the-project-on-your-computer). Build the packaged application with `./gradlew shadowJar`. From the project root, run each command below separately, closing the application between runs:

```bash
java -ea -jar build/libs/PolicyHarbor.jar
java -jar build/libs/PolicyHarbor.jar
```

For isolated manual checks, use a fresh writable folder with fictional data and specify the absolute path to the built JAR. If you copy the JAR into that folder, use `PolicyHarbor.jar` as the path instead. Both modes must preserve normal validation and save/load protection. The Gradle daemon's JVM arguments or a task dry run alone do not prove assertions are active in the application JVM.

## Test responsibilities

| Level | What it checks | Examples |
| --- | --- | --- |
| Unit | Domain constraints, parsers and pure display values | `FollowUpTest`, `ViewCommandParserTest`, `ClientProfilePanelTest` |
| Integration | Model transitions and cooperating components | `ClientProfileSelectionTest`, `LogicManagerTest`, `JsonAddressBookStorageTest` |
| Workflow | Real command routing, persistence, failure preservation and restart | `ClientRecordCommandsTest`, `ClientProfileWorkflowIntegrationTest` |

Tests use fixed dates and temporary data folders. Storage failure tests inject write/move failures rather than relying on OS permissions. Keep successful and rejected commands' data/view assertions together.

Pure display-function tests do not prove FXML binding, focus, wrapping or scaling. Follow the Developer Guide's [manual testing appendix](DeveloperGuide.md#appendix-instructions-for-manual-testing) against the actual JAR before a release, and record the tested platform/runtime and remaining gaps.

## v1.4 integrated dashboard

`DashboardIntegrationTest` exercises the real window, Logic and temporary JSON storage: filtered indices versus the global summary, canonical selection after pending reordering, clear/delete, failed-save preservation, explicit error recovery, published-date refresh, observer cleanup and logical layout sizes.

The native mouse/keyboard tests in `CommandSubmissionTest` need an interactive desktop. Its macOS Actions profile skips those three tests unless `POLICYHARBOR_NATIVE_UI_TESTS=true`; the remaining JavaFX component tests still run. A local check using that profile must report the three skipped tests explicitly. Native packaged-app interaction and the real OS/resolution/scaling matrix are separate evidence; logical scene-size checks do not prove display scaling.

See the [Developer Guide's dashboard procedure](DeveloperGuide.md#integrated-v14-dashboard-checks) for manual acceptance and the [v1.4 release notes](releases/v1.4.md) for the user-visible increment.

### v1.4 release GUI verification record

The packaged-app checks recorded for integration revision `ffda0137` passed on the available macOS arm64 desktop with Zulu 25+FX: ordinary and assertion-enabled startup, Enter/Run, typing after a rejected command, recovery, filtered table with the global pending summary, F1/Escape and save/restart. The product screenshots were captured from that packaged app. Recheck the final release commit/JAR after all prerequisite and review changes land.

| Release check | Evidence/status |
| --- | --- |
| Missing saved screen | `DashboardIntegrationTest.window_unavailableSavedScreen_restoresInsidePrimaryWithoutDiscardingPreferences` verifies the real window constructor with unavailable saved coordinates, primary-screen bounds and retained preferences. Physical monitor unplug/reconnect remains untested. |
| Invalid client-data startup | `DashboardStartupIntegrationTest` loads malformed JSON through MainApp, opens the composed UI, submits a successful query and rejected mutation, and checks the persistent warning, empty table/summary and unchanged saved bytes. |
| Windows packaged GUI | Pending before release; platform CI does not replace running the packaged JAR. |
| Linux packaged GUI | Pending before release; virtual-display component CI does not replace a real desktop check. |
| 1920x1080 at 100% and 125%, on each supported OS | Physical display/scaling checks pending. |
| 1280x720 at 150%, on each supported OS | Physical display/scaling checks pending; the 853x480 logical scene test is automated evidence only. |
| Clipboard rejection | Pending; record whether the environment can exercise a rejected clipboard write. |

Before publication, the release operator records the exact source commit, JAR SHA-256, OS/JDK, actual resolution/scaling, date, result and evidence for each exercised case. Keep unavailable combinations explicitly pending rather than treating a screenshot or green CI as proof.
