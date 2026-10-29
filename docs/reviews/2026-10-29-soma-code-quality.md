# Somaaditya Samal: attributed-code quality audit

Date: 29 October 2026 (Singapore). Team baseline: `01ef0f17bf9631bbe50c95e2f47b5592244f3574`.
Issue: [#241](https://github.com/AY2627S1-CS2103T-W13-3/tp/issues/241).

## Assessment and sources

The reviewed contributions have strong validation and persistence tests. This change fixes a verified profile-observer lifecycle defect, improves migration readability and boolean naming, and removes duplicated test infrastructure. No further blocking defect was identified in the reviewed contribution paths. This is evidence for human review, not a predicted grade or a certification of the whole application.

The current [grading criteria](https://nus-cs2103-ay2627-s1.github.io/website/admin/tp-grading.html) assess the code claimed by each student through manual inspection and automated analysis. Relevant criteria are suitable logging, exceptions, assertions and defensive coding; Java standards beyond Checkstyle; reasonable abstraction levels; and low duplication, especially in tests. Testing assessment includes the quality of automated tests and bugs found by others.

The [individual testing expectations](https://nus-cs2103-ay2627-s1.github.io/website/admin/tp-expectations.html#individual-expectations-on-testing) require evidence of automated testing and explicitly set no minimum coverage percentage. The [Week 11 instructions](https://nus-cs2103-ay2627-s1.github.io/website/admin/tp-w11.html#2-fix-alpha-test-bugs-fine-tune-features-improve-code-quality) recommend improving coverage where useful, then addressing remaining quality problems. Therefore test selection here follows behavior and risk rather than an invented grading threshold.

Manual review also used the [basic and intermediate Java standard](https://se-education.org/guides/conventions/java/intermediate.html), the course's [code-quality guidance](https://nus-cs2103-ay2627-s1.github.io/website/se-book-adapted/chapters/codeQuality.html#slap-hard), [error-handling guidance](https://nus-cs2103-ay2627-s1.github.io/website/se-book-adapted/chapters/errorHandling.html), and the repository's Checkstyle configuration. Naming, explicit imports, braces, null contracts, abstraction, nesting, mutability, duplication and exception boundaries were checked separately from automated style checks.

## Attribution and scope

The [RepoSense dashboard](https://nus-cs2103-ay2627-s1.github.io/tp-dashboard/?search=SomaadityaSamal) report was generated **29 October, 19:20 Singapore**. The [report metadata](https://nus-cs2103-ay2627-s1.github.io/tp-dashboard/summary.json) and [team authorship data](https://nus-cs2103-ay2627-s1.github.io/tp-dashboard/AY2627S1-CS2103T-W13-3_tp_master/authorship.json) identify `SomaadityaSamal`: **1,078 functional-code lines, 2,314 test-code lines, 121 documentation lines and 2 other lines** across 62 files. These are dashboard counts, including attributed blank/comment lines; they are not executable coverage counts.

The 29 functional files and the contributions in 25 test files were reviewed, with callers and existing integration tests used to check crossed boundaries. Six documentation files and two other files appear in the inventory for attribution context; this is not a complete documentation or release audit. The current UG/DG were consulted for storage, identity, validation, profile and pending-panel behavior. The only guide behavior update in this PR is the profile lifecycle description, plus acknowledgement of this Codex-assisted work.

Git history and current baseline blame were cross-checked using the verified author email `somaadityasamal@gmail.com`. The dashboard snapshot differs slightly from current Git blame in seven files, shown below. Preserve these differences rather than editing ownership markers or treating historical commits as present-day attribution. For example, the current email validator has been rewritten by another contributor; historical email work was checked through its retained tests and integration paths without claiming the replacement implementation.

## Findings and resolutions

| Priority | Finding and consequence | Resolution and evidence |
| --- | --- | --- |
| P2 | `ClientProfilePanel` installed anonymous listeners that window cleanup could not remove. After `WINDOW_HIDDEN`, later model edits and date changes still updated the closed profile and retained the panel through model properties. Initial render failure could also leave a partially constructed panel subscribed. | Store/removal of selection and date observers; idempotent disposal; guards for callbacks captured during notification; render before subscription. `MainWindow` owns/disposes the profile alongside the pending panel. `ClientProfileLifecycleTest` reproduces hidden-window updates and checks both notification sources and failed construction. The window regression failed on the baseline with expected `Rachel Lim`, actual `Amanda Lee`, before the production fix. |
| Recommended | `JsonSerializableAddressBook.toModelType` mixed conversion loops, legacy detection, numeric checks, ID allocation and aggregate restoration in one method, making migration precedence harder to inspect. | Extract person conversion, identity-format detection and ID reading. Keep contact validation before counter validation, counter validation before individual IDs, and existing exception translation. Name the exhausted-counter bound. Existing migration/protection/round-trip suites remain applicable; new tests pin precedence, the lower counter boundary and defensive list copying. |
| Recommended | Both owned panel suites contained almost identical recursive lookup and toolkit/thread dispatch code. Maintaining separate startup/timeout behavior invites divergence. | Reuse existing `JavaFxTestUtil` toolkit/dispatch functions and one shared node lookup. Pure display tests remain independent of graphical-session availability. Scenario assertions and test cases were retained. |
| Standards | Boolean fields/locals `disposed` and `previousWasDigit` lack the recommended boolean prefix used by the course standard. | Rename to `isDisposed` and `wasPreviousDigit`; preserve all phone validation and disposal behavior. |
| Recommended | Protected-startup tests invoked a package-private seam through reflection, and several storage tests used verbose qualified class names and boolean array comparisons. | Introduce a test-only package bridge without widening production access, use explicit imports, and use `assertArrayEquals` for useful byte-diff diagnostics. |

## Review by contribution area

| Area | Checks and existing evidence | Outcome |
| --- | --- | --- |
| JSON adapters and migration | Required/null fields; wrong scalar types before coercion; unknown/repeated fields; invalid UTF-8/BOM/trailing roots; whole-file rejection; legacy/all-null versus partially identified files; gapped IDs; integer bounds/exhaustion; missing/duplicate IDs; overdue actions; stable round trips. `JsonAdaptedPersonTest`, `JsonSerializableAddressBookTest`, `ClientIdLoadingProtectionTest`, `ClientIdMigrationTest`, `ClientIdRoundTripTest`, and saved-text/encoding recovery integration tests cover these boundaries. | Preserve the accepted contract. Entirely removing/nullifying all identity history is indistinguishable from a legacy file and resets IDs; existing backup/reset tests and guides explicitly document this limitation. |
| Saving and transaction publication | Sibling temporary file, atomic replacement, write/move/unsupported-atomic failures, symlink rejection and late swap, original-byte preservation, cleanup, no publication before save, retry without consuming IDs. Storage, directory-recovery and profile/follow-up workflow tests check live data, selection, date, mode and disk state together. | No change to atomicity or failure-state semantics. The two symlink checks remain best effort with external writers, as documented. No fallback to partial writes was introduced. |
| Startup and status date | Missing file versus malformed/inaccessible data; empty mutation-blocked startup; repaired-file recovery; read-only commands; focus/periodic refresh; one sampled date per command; system-zone changes. `MainAppTest`, recovery suites, `LogicManagerTest` and `FollowUpWorkflowIntegrationTest` provide evidence. | Existing defenses remain suitable. Some native application startup/shutdown paths are not exercised by in-process unit coverage. |
| Names, phone, duplicate matching and search | ASCII digit boundaries, optional plus/separators, long inputs, leading zeroes, original formatting, Unicode letters/marks, invalid surrogates, canonical composition and caseless duplicate symmetry; preserve distinction between duplicate relation, value equality and canonical identity. Domain, parser, duplicate-command and restart tests cross-check these rules. | No new restrictions or normalization policy. Boolean naming correction only. The name/phone/person source files are fully line/branch covered in the measured run. |
| Profile and pending panels | Empty state; full contact/action text; status classes; long tag wrapping; immutable display snapshots; canonical/equal-value selection; permanent ID lookup and scroll continuity; group order; disposal and notifications. Existing rendering/scroll/dashboard suites exercise FXML and logical geometry. | Fix the profile lifecycle gap. Pending-panel identity mapping and its existing guarded disposal remain intact. CSS/FXML already separate presentation from model mutation; no new styling framework or layout rewrite. |
| Logging, exceptions, assertions and defense | Logic logs command types rather than raw command text; loading errors use safe diagnostics; adapters translate validation failures; storage translates to loading errors; `requireNonNull`, immutable values, copies/unmodifiable lists and validation protect boundaries. Pending grouping rejects invalid feeds and asserts unsupported status via `AssertionError`; the attributed packaged acceptance checker also uses enabled Java assertions for developer invariants. | Existing evidence is purposeful. No token log statements, redundant catch blocks or assertions used as user-input validation were added. Diagnostics/privacy regressions are included in the full suite. |
| Tests and source organization | Fixed clocks and temporary files; real parser/model/storage paths; state preservation after failure; focused doubles at I/O boundaries; shared helpers; no broad mocking or disabled checks. Field-by-field adapter validation is explicit rather than hidden behind a generic reflection-based validator. | Retain scenario-specific assertions and integration sequences. Six added tests target distinct boundaries; no getter-only tests or coverage exclusions. |

## Validation and coverage

Environment: macOS arm64, Temurin Java 25.0.3, repository Gradle wrapper and Checkstyle 14.1.0. Commands ran against this branch unless noted.

| Check | Result |
| --- | --- |
| Baseline `./gradlew check coverage` | 821 tests: 818 passed, 3 failed, 0 skipped. All three failures were native `CommandSubmissionTest` keyboard/mouse focus cases before any production edits. |
| Failing lifecycle regression before fix | Observed expected old profile name versus actual edited name after window cleanup. |
| Affected storage/panel/phone suites during implementation | Passed after the lifecycle fix and migration/test refactoring. The subsequent full run also includes the final six added tests. |
| Final `./gradlew check coverage` | 827 tests: 824 passed, the same 3 native focus cases failed, 0 skipped. Checkstyle main/test reports contain zero violations. The complete Gradle command is **not green** locally. |
| `./gradlew coverage -x test` | Passed; generated the XML/HTML report from the preceding complete 827-test execution, without replacing it with focused-suite execution data. |
| Repository scripts and whitespace checks | Passed against the staged final files before commit; `git diff --cached --check` also passed. |
| Packaged backend acceptance | Passed: `./gradlew packagedBackendCheck` loaded production classes from `build/libs/PolicyHarbor.jar`, with assertions enabled and 1,000 fictional clients in a fresh temporary folder. Filter, permanent-ID lookup, rejected input, follow-up reorder/clear, deletion and reload checks passed. It is not a physical desktop-scaling check. |

The three local failures are `submit_tabAndSpace_submitsOnceAndRestoresTypingAfterSuccessAndFailure`, `submit_mouseClick_submitsOnceAndRestoresTypingAfterSuccessAndFailure`, and `submit_enterKey_submitsOnceAndRestoresTypingAfterSuccessAndFailure`. They report a native window not receiving keyboard input or typing after submission not reaching the input. The same cases fail on the untouched baseline. Their cause is not proven to be exclusively environmental; they remain a desktop verification gap. No tests were skipped, filtered out of the full run, or weakened to hide them. GitHub checks are reported separately in the PR for its exact head.

Coverage below measures **whole source files containing attributed contributions**, including other authors' code. It is not a personal coverage percentage. Interfaces and CSS/FXML do not have comparable JaCoCo executable counters. The report comes from the full test execution with the native failures disclosed above.

| Source file (under `src/main/java`) | Baseline lines | Final lines | Final branches |
| --- | --- | --- | --- |
| `seedu/address/MainApp.java` | 35/89 (39.3%) | 35/89 (39.3%) | 3/14 (21.4%) |
| `seedu/address/commons/core/LogsCenter.java` | 26/29 (89.7%) | 26/29 (89.7%) | n/a |
| `seedu/address/commons/util/FileUtil.java` | 4/14 (28.6%) | 4/14 (28.6%) | 2/6 (33.3%) |
| `seedu/address/commons/util/JsonUtil.java` | 37/40 (92.5%) | 37/40 (92.5%) | 6/6 (100.0%) |
| `seedu/address/logic/LogicManager.java` | 38/38 (100.0%) | 38/38 (100.0%) | 2/2 (100.0%) |
| `seedu/address/logic/Messages.java` | 17/17 (100.0%) | 17/17 (100.0%) | 3/4 (75.0%) |
| `seedu/address/logic/commands/AddCommand.java` | 23/23 (100.0%) | 23/23 (100.0%) | 6/6 (100.0%) |
| `seedu/address/logic/commands/ClearCommand.java` | 4/4 (100.0%) | 4/4 (100.0%) | n/a |
| `seedu/address/logic/commands/EditCommand.java` | 72/73 (98.6%) | 72/73 (98.6%) | 26/28 (92.9%) |
| `seedu/address/logic/commands/ExitCommand.java` | 2/2 (100.0%) | 2/2 (100.0%) | n/a |
| `seedu/address/model/person/Name.java` | 38/38 (100.0%) | 38/38 (100.0%) | 54/54 (100.0%) |
| `seedu/address/model/person/NameContainsKeywordsPredicate.java` | 13/13 (100.0%) | 13/13 (100.0%) | 4/4 (100.0%) |
| `seedu/address/model/person/Person.java` | 49/49 (100.0%) | 49/49 (100.0%) | 30/30 (100.0%) |
| `seedu/address/model/person/Phone.java` | 32/32 (100.0%) | 32/32 (100.0%) | 32/32 (100.0%) |
| `seedu/address/storage/JsonAdaptedFollowUp.java` | 13/13 (100.0%) | 13/13 (100.0%) | 4/4 (100.0%) |
| `seedu/address/storage/JsonAdaptedPerson.java` | 64/64 (100.0%) | 64/64 (100.0%) | 40/40 (100.0%) |
| `seedu/address/storage/JsonAdaptedTag.java` | 13/13 (100.0%) | 13/13 (100.0%) | 4/4 (100.0%) |
| `seedu/address/storage/JsonAddressBookStorage.java` | 37/39 (94.9%) | 37/39 (94.9%) | 4/4 (100.0%) |
| `seedu/address/storage/JsonSerializableAddressBook.java` | 45/46 (97.8%) | 45/46 (97.8%) | 33/34 (97.1%) |
| `seedu/address/storage/StoredTextDeserializer.java` | 5/5 (100.0%) | 5/5 (100.0%) | 2/2 (100.0%) |
| `seedu/address/storage/SymbolicLinkSaveException.java` | 2/2 (100.0%) | 2/2 (100.0%) | n/a |
| `seedu/address/ui/ClientProfilePanel.java` | 85/85 (100.0%) | 98/98 (100.0%) | 37/37 (100.0%) |
| `seedu/address/ui/MainWindow.java` | 107/120 (89.2%) | 109/122 (89.3%) | 29/48 (60.4%) |
| `seedu/address/ui/PendingFollowUpPanel.java` | 107/109 (98.2%) | 107/109 (98.2%) | 34/37 (91.9%) |

The full repository has 2,181/2,359 executable lines covered (92.5%) and 952/1,031 branches covered (92.3%) in this execution. The baseline had 2,166/2,344 lines (92.4%) and 945/1,023 branches (92.4%). The small branch-percentage movement is not a regression finding: behavior, changed denominators and uncovered branches matter more than the percentage.

Remaining gaps were considered explicitly. Storage cleanup after a second I/O error and serialization of an inconsistent custom `ReadOnlyAddressBook` are exceptional/internal paths; do not add production seams solely to raise coverage. The pending panel's unsupported-enum/default path cannot be triggered with the current enum; redundant/no-op selection/style guards account for other branches. `FileUtil`'s older directory/write helpers and much of `MainApp`'s native launch/stop code are shared/inherited areas, so their full-file percentages must not be misrepresented as uncovered personal business logic.

## Freeze footprint and remaining verification

The live [v1.6 freeze policy](https://nus-cs2103-ay2627-s1.github.io/website/admin/tp-w12.html#1-fix-any-latent-bugs) starts at the v1.5 deadline plus ten hours: **Friday, 30 October 2026, 10:00 Singapore**. The per-member allowance is the greater of 15% of final attributed functional LoC or 150 lines. Code-file changes under `src/main`, including comments/blank lines, count; tests and documentation do not. The calculation uses final dashboard attribution. This PR changes five production Java files: **86 added and 47 removed lines**, 133 combined diff lines as a conservative review aid. This is not the dashboard calculation and says nothing about the member's other post-freeze changes or eventual merge time. No feature, command grammar or saved schema was added.

Manual follow-up: on an interactive desktop, run the three native submission scenarios (Enter, Tab/Space and mouse) and verify typing returns to the command input after successful and failed commands. Retain the existing physical Windows/Linux/macOS resolution/scaling release matrix; logical JavaFX layout tests and packaged backend checks cannot certify it. A complete rendered website/documentation audit and PE rehearsal were outside this code-quality review. Course marks and final freeze allowance remain decisions based on the final submitted product and dashboard.

## Attributed-file inventory

Counts refer to the pre-change dashboard snapshot and baseline Git blame, not authorship transferred by this PR. A file with one credited import/message line is included for completeness without treating the whole file as this student's implementation.

| File | RepoSense category | RepoSense lines | Current baseline Git lines |
| --- | --- | ---: | ---: |
| `README.md` | docs | 2 | 2 |
| `build.gradle` | other | 1 | 1 |
| `docs/AboutUs.md` | docs | 3 | 3 |
| `docs/DevOps.md` | docs | 2 | 2 |
| `docs/DeveloperGuide.md` | docs | 57 | 70 |
| `docs/UserGuide.md` | docs | 43 | 50 |
| `docs/_sass/minima/_base.scss` | other | 1 | 1 |
| `docs/diagrams/StorageClassDiagram.puml` | docs | 14 | 14 |
| `src/main/java/seedu/address/MainApp.java` | functional-code | 39 | 39 |
| `src/main/java/seedu/address/commons/core/LogsCenter.java` | functional-code | 1 | 1 |
| `src/main/java/seedu/address/commons/util/FileUtil.java` | functional-code | 1 | 1 |
| `src/main/java/seedu/address/commons/util/JsonUtil.java` | functional-code | 4 | 4 |
| `src/main/java/seedu/address/logic/Logic.java` | functional-code | 16 | 16 |
| `src/main/java/seedu/address/logic/LogicManager.java` | functional-code | 63 | 65 |
| `src/main/java/seedu/address/logic/Messages.java` | functional-code | 2 | 2 |
| `src/main/java/seedu/address/logic/commands/AddCommand.java` | functional-code | 1 | 1 |
| `src/main/java/seedu/address/logic/commands/ClearCommand.java` | functional-code | 1 | 1 |
| `src/main/java/seedu/address/logic/commands/EditCommand.java` | functional-code | 1 | 1 |
| `src/main/java/seedu/address/logic/commands/ExitCommand.java` | functional-code | 1 | 1 |
| `src/main/java/seedu/address/model/person/Name.java` | functional-code | 22 | 22 |
| `src/main/java/seedu/address/model/person/NameContainsKeywordsPredicate.java` | functional-code | 4 | 4 |
| `src/main/java/seedu/address/model/person/Person.java` | functional-code | 9 | 9 |
| `src/main/java/seedu/address/model/person/Phone.java` | functional-code | 24 | 24 |
| `src/main/java/seedu/address/storage/JsonAdaptedFollowUp.java` | functional-code | 39 | 39 |
| `src/main/java/seedu/address/storage/JsonAdaptedPerson.java` | functional-code | 64 | 64 |
| `src/main/java/seedu/address/storage/JsonAdaptedTag.java` | functional-code | 5 | 7 |
| `src/main/java/seedu/address/storage/JsonAddressBookStorage.java` | functional-code | 38 | 38 |
| `src/main/java/seedu/address/storage/JsonSerializableAddressBook.java` | functional-code | 60 | 62 |
| `src/main/java/seedu/address/storage/StoredTextDeserializer.java` | functional-code | 24 | 24 |
| `src/main/java/seedu/address/storage/SymbolicLinkSaveException.java` | functional-code | 11 | 11 |
| `src/main/java/seedu/address/ui/ClientProfilePanel.java` | functional-code | 147 | 147 |
| `src/main/java/seedu/address/ui/MainWindow.java` | functional-code | 3 | 3 |
| `src/main/java/seedu/address/ui/PendingFollowUpPanel.java` | functional-code | 213 | 213 |
| `src/main/resources/view/ClientProfilePanel.css` | functional-code | 85 | 85 |
| `src/main/resources/view/ClientProfilePanel.fxml` | functional-code | 62 | 62 |
| `src/main/resources/view/PendingFollowUpPanel.css` | functional-code | 102 | 102 |
| `src/main/resources/view/PendingFollowUpPanel.fxml` | functional-code | 36 | 36 |
| `src/test/java/seedu/address/ClientProfileWorkflowIntegrationTest.java` | test-code | 244 | 244 |
| `src/test/java/seedu/address/MainAppTest.java` | test-code | 163 | 163 |
| `src/test/java/seedu/address/PackagedBackendCheck.java` | test-code | 17 | 17 |
| `src/test/java/seedu/address/SavedDataEncodingRecoveryTest.java` | test-code | 99 | 99 |
| `src/test/java/seedu/address/SavedTextTypeRecoveryTest.java` | test-code | 70 | 70 |
| `src/test/java/seedu/address/StorageDirectoryRecoveryTest.java` | test-code | 74 | 74 |
| `src/test/java/seedu/address/commons/util/FileUtilUtf8Test.java` | test-code | 43 | 43 |
| `src/test/java/seedu/address/logic/FollowUpWorkflowIntegrationTest.java` | test-code | 160 | 159 |
| `src/test/java/seedu/address/logic/LogicManagerTest.java` | test-code | 87 | 90 |
| `src/test/java/seedu/address/logic/commands/ClientRecordCommandsTest.java` | test-code | 16 | 16 |
| `src/test/java/seedu/address/logic/parser/AddCommandParserTest.java` | test-code | 2 | 2 |
| `src/test/java/seedu/address/logic/parser/ParserUtilTest.java` | test-code | 1 | 1 |
| `src/test/java/seedu/address/model/person/EmailTest.java` | test-code | 14 | 14 |
| `src/test/java/seedu/address/model/person/NameContainsKeywordsPredicateTest.java` | test-code | 6 | 6 |
| `src/test/java/seedu/address/model/person/NameTest.java` | test-code | 13 | 13 |
| `src/test/java/seedu/address/model/person/PersonTest.java` | test-code | 40 | 40 |
| `src/test/java/seedu/address/model/person/PhoneTest.java` | test-code | 14 | 14 |
| `src/test/java/seedu/address/storage/ClientIdLoadingProtectionTest.java` | test-code | 116 | 116 |
| `src/test/java/seedu/address/storage/ClientIdMigrationTest.java` | test-code | 120 | 120 |
| `src/test/java/seedu/address/storage/ClientIdRoundTripTest.java` | test-code | 115 | 115 |
| `src/test/java/seedu/address/storage/JsonAdaptedPersonTest.java` | test-code | 67 | 67 |
| `src/test/java/seedu/address/storage/JsonAddressBookStorageTest.java` | test-code | 203 | 203 |
| `src/test/java/seedu/address/storage/JsonSerializableAddressBookTest.java` | test-code | 59 | 59 |
| `src/test/java/seedu/address/ui/ClientProfilePanelTest.java` | test-code | 241 | 241 |
| `src/test/java/seedu/address/ui/PendingFollowUpPanelTest.java` | test-code | 330 | 330 |
