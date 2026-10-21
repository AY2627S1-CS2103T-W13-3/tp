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
