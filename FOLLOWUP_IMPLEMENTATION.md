# PolicyHarbor follow-up feature: shared agent contract

**Author:** Prepared for Ayush Jain and the PolicyHarbor team with Codex
**Date:** 2026-10-06
**Status:** Revised implementation contract; domain foundation implemented in PR #44, remaining packages planned
**Reviewers:** Ayush Jain, Josthan Wong, Gwen Lim, Kin Chong, Somaaditya Samal
**Contract version:** 2
**Inspected baseline:** upstream `master` at `4132ebeeff409e6e81f7ce87b1cc4316aa9ba874`
**Foundation PR:** [#44 - Add follow-up domain foundation](https://github.com/AY2627S1-CS2103T-W13-3/tp/pull/44) (check its live merge status before starting dependent work)
**Source:** `CS2103T-W13-3.pdf`, especially sections 2, 7, 8 and 9 (pages 2-3 and 9-13), and the requested four-feature screenshot.

Version 2 removes the proposed `FollowUpMessages` class. Command wording belongs to the commands that implement it; generic errors reuse existing AB3 message owners. The domain API, user-visible wording, five-person split and overall dependency chain remain unchanged. `FollowUp.parseDate()` and `FollowUpStatus.getDisplayName()` remain part of the agreed foundation. Do not recreate the removed message class from an older copy of this document.

Read this entire document before implementing a work package. It is intended to be shared with each person's coding agent without requiring access to the original chat or PDF. Paths below are relative to the repository root. Names are tentative allocations, not claims about expertise or availability.

## Context

The deliverable is one working follow-up workflow: **record, replace/reschedule, clear, and list pending follow-ups**. These are four user behaviours, not four independent implementations. Recording and replacing use the same syntax and almost identical code. Clearing removes the pending action; it does not create a completed record.

At the inspected upstream baseline, the application is still the AB3 implementation internally: there are no follow-up classes, commands, parsers, JSON fields or UI controls. PR #44 now implements the domain foundation; the remaining sections below describe subsequent work. Recheck upstream and PR #44 before implementation rather than assuming the foundation has merged. Existing client creation, listing, name search and deletion provide a useful starting point. Keep the `seedu.address` packages and `Person`/`AddressBook` names for this increment to avoid a repository-wide rename.

The source MVP also specifies a separate `view` command and profile panel. Those do not exist yet. This proposal delivers the four requested behaviours through the existing list, with follow-up details shown there. It is a follow-up increment, **not completion of the entire MVP or a pixel-perfect implementation of the mockup**. Profile integration remains an explicit follow-on task. The PDF's precise command rules govern this increment; broader Developer Guide stories do not authorize adding history, archive, notifications or interactive multi-step prompts.

### What exists and what must change

| Existing code | Finding | Required work |
| --- | --- | --- |
| `src/main/java/seedu/address/model/person/Person.java` | Immutable contact fields and tags; no follow-up. | Add one optional immutable follow-up, preserve it in copies, include it in value equality. |
| `logic/commands/EditCommand.java` under the same Java root | Constructs a new `Person` from contact fields. | Preserve follow-up when legacy edit remains available. Otherwise editing a contact silently loses it. |
| `logic/parser/AddressBookParser.java`, `CliSyntax.java` | No `followup` or `followups` route or `d/`, `m/` prefixes. | Add routes and dedicated syntax validation. |
| `model/ModelManager.java` | A `FilteredList` only; no pending mode, date context or sorting. | Add pending mode and a stable displayed-list API that all indexed commands share. |
| `logic/LogicManager.java` | Executes on the live model, then saves after every command, including `list`. | Execute against a private candidate model; save mutations before publishing. Read-only commands must not write. |
| `storage/JsonAdaptedPerson.java` | Serializes contact fields only. | Add optional nested follow-up data and validate on loading. |
| `storage/JsonAddressBookStorage.java` | Creates/writes the target directly. | Write a temporary file and atomically replace the target so failed saving does not destroy old data. |
| `MainApp.java` | Uses sample data for a missing file and empty data after a load error, without a write block. | Distinguish empty first launch from invalid data; block mutations after failed loading. |
| `ui/PersonCard.java`, `PersonListPanel.java` | Contact cards only; no follow-up labels or custom empty-list text. | Render due date, action and status; refresh on successful commands and index changes. |
| `ui/CommandBox.java` | Keeps failed input, clears successful input, but ignores empty input. | Preserve the useful behaviour; surface the MVP's empty-command error. |
| `src/test/java/seedu/address/logic/LogicManagerTest.java` | Save-error test expects the in-memory add to remain. | Replace that expectation with unchanged live data. |
| `build.gradle` | Java 25, JavaFX 17.0.7, JUnit 5. | Use the existing toolchain. Do not introduce another UI framework or dependency. |

## Five-person work allocation

Every person implements production code **and** tests. Do not assign one person only documentation or testing. Changes to common files have exactly one owner; other people request a change from that owner.

| Person | Work package | Concrete deliverable | Primary dependencies |
| --- | --- | --- | --- |
| **Ayush** | Domain types and mutations | `FollowUp`, status and domain validation, `Person` extension, record/replace and clear commands; preserve data through legacy edit. | Gwen's date/model API for command execution. |
| **Josthan** | Command parsing and routing | Both command routes; syntax, prefixes, index validation, error precedence and blocked-load gate. | Ayush's constructors/messages and Gwen's list-command constructor. |
| **Gwen** | Pending list and model state | Pending filtering, date sorting, stable row indices, per-command candidate models and publishing, `FollowUpsCommand`. | Ayush's immutable follow-up types. |
| **Somaaditya** | Persistence and execution integration | JSON round trip, atomic save, startup load block, `LogicManager` transaction orchestration and read-only behaviour. | Gwen's candidate model API; Josthan's dispatcher overload. |
| **Kin Chong** | UI and executable demonstration | Follow-up fields/status in cards, empty-state messages, load warning, refresh behaviour, keyboard flow, focused UI tests and demo checklist. | Ayush's types and Somaaditya's read-only Logic API. |

Gwen and Somaaditya have the most integration-sensitive packages. Reserve joint review time for their boundary; redistribute tests/review effort if needed, but do not give two agents ownership of the same implementation file.

### Exact ownership

In this table, `main/` means `src/main/java/seedu/address/` and `test/` means `src/test/java/seedu/address/`. `(new)` denotes a proposed file that does not exist at the baseline.

| Owner | Production files | Tests / supporting files |
| --- | --- | --- |
| Ayush | `main/model/person/FollowUp.java` (new), `FollowUpStatus.java` (new); `main/model/person/Person.java`; `main/logic/commands/SetFollowUpCommand.java`, `ClearFollowUpCommand.java` (new); `main/logic/commands/EditCommand.java` | Corresponding new domain and command tests; `test/model/person/PersonTest.java`; `test/logic/commands/EditCommandTest.java`; `test/testutil/PersonBuilder.java`; `test/testutil/FollowUpTestData.java` (new). This document. |
| Josthan | `main/logic/parser/FollowUpCommandParser.java`, `FollowUpParserUtil.java` (new); `main/logic/parser/AddressBookParser.java`, `CliSyntax.java`, `ParserUtil.java`; `main/logic/Messages.java` | Corresponding parser tests, including `AddressBookParserTest.java`, `ParserUtilTest.java`, `AddCommandParserTest.java`, `EditCommandParserTest.java`, `ArgumentTokenizerTest.java`; new `test/logic/MessagesTest.java` for ordered duplicate diagnostics. |
| Gwen | `main/model/Model.java`, `ModelManager.java`; `main/logic/commands/FollowUpsCommand.java` (new), `ListCommand.java` | `test/model/ModelManagerTest.java`; `test/logic/commands/ListCommandTest.java`; new `FollowUpsCommandTest.java`, `FollowUpListIntegrationTest.java`; interface-method additions to the nested `ModelStub` in `test/logic/commands/AddCommandTest.java`. |
| Somaaditya | `main/storage/JsonAdaptedFollowUp.java` (new), `JsonAdaptedPerson.java`, `JsonAddressBookStorage.java`; `main/logic/Logic.java`, `LogicManager.java`; `main/MainApp.java` | Matching storage/logic tests; new `test/MainAppTest.java` if startup testing needs it; new `test/logic/FollowUpWorkflowIntegrationTest.java`; new fixtures under `src/test/data/FollowUpStorageTest/`. |
| Kin Chong | `main/ui/PersonCard.java`, `PersonListPanel.java`, `MainWindow.java`, `CommandBox.java`; `src/main/resources/view/PersonListCard.fxml`, `MainWindow.fxml`, `DarkTheme.css` | New UI tests under `test/ui/` with distinct class names; follow-up sections in `docs/UserGuide.md` and `docs/DeveloperGuide.md` after integration. |

All other files are outside these write allowlists. If another file genuinely needs changing, agree and record its sole owner here before editing it. In particular, only Josthan changes `Messages.java` and `ParserUtil.java`; do not independently change `CommandResult.java`, `AddressBook.java`, `UniquePersonList.java`, shared JSON configuration, Gradle, README or the UI mockup. Existing commands should pick up the displayed list through their existing model call.

Keep new test fixtures inside each owner's files; do not all add constants to `TypicalPersons.java` or `CommandTestUtil.java`. Only Ayush adds shared follow-up fixture helpers. Preserve every unrelated person's edits.

## Functional Requirements

- FR-1: A client MUST have zero or one pending follow-up. Its date and description MUST be present together. Clearing MUST remove only that action, leaving contact information unchanged.
- FR-2: `followup INDEX d/DATE m/DESCRIPTION` MUST record an action when absent and replace both fields when present. An identical replacement MUST succeed with the updated message. There MUST NOT be separate reschedule syntax.
- FR-3: `followup INDEX clear` MUST clear an existing action and MUST fail when none exists. It MUST NOT delete the client or retain completion history.
- FR-4: `followups` MUST select all clients with pending actions from the full dataset, independent of the previous filter. It MUST sort ascending by due date, breaking ties by full-list insertion order, and number rows from 1. It MUST include overdue and future actions. Ordinary trailing text MUST be ignored.
- FR-5: Every indexed operation MUST resolve its target against the currently displayed list before mutation. Follow-up changes MUST retain the active list/filter. Replacement MUST immediately re-sort pending mode; clearing MUST remove that row there. `list` and `find` MUST leave pending mode and restore insertion-order results. `add` MUST still show the full list. Deletion MUST remove the client's action too.
- FR-6: New/replacement dates MUST use exactly four-digit `YYYY-MM-DD`, year 0001-9999, valid Gregorian dates, and be today or later. Description MUST contain 1-200 ASCII printable characters (U+0020-U+007E) after trimming outer ordinary spaces; retain case and internal spaces. Prefix order MAY vary; repeated `d/` or `m/` MUST be rejected even when values match.
- FR-7: Status MUST be derived as `OVERDUE`, `DUE TODAY`, or `UPCOMING` against one sampled device-local date per command. Labels MUST refresh at startup and after each successful command, including read-only commands, but MUST remain unchanged after failed commands. Past stored dates MUST remain valid and pending. There MUST NOT be a midnight timer.
- FR-8: Shared input rules and the validation precedence below MUST apply. A failure MUST keep command text for correction and keep data, list mode, row order and displayed date unchanged. A success MUST clear the command box and retain keyboard focus.
- FR-9: Every successful data-mutating command MUST save before the live model changes or a success message appears. Read-only commands MUST NOT save. Follow-up changes MUST round-trip across restarts. Successful startup MUST show the full list in saved insertion order, not the previous session's filter.
- FR-10: Malformed/unreadable saved data MUST produce an empty, mutation-blocked session with a persistent warning. No command or close operation MUST overwrite that file. A missing file MUST instead produce a normal, empty, writable session. Restarting with valid data MUST clear the block.
- FR-11: Pending rows MUST show index, name, phone, email, due date, description and a textual status. Empty pending mode MUST show `There are no pending follow-ups.`; an empty ordinary list MUST show `No clients to display.`. The increment MUST expose follow-up details in ordinary client cards too, so recording an action is visibly useful before invoking `followups`.

## Non-Functional Requirements

- NFR-1: Failed parse, validation, execution or save attempts MUST cause zero live client/list/date changes and zero changes to the previously saved file. Use injected failure tests, not permission assumptions that differ across operating systems.
- NFR-2: Existing public contact constructors MUST remain source-compatible. Existing contact-only JSON fixtures MUST load with no follow-up. The full regression suite and Checkstyle MUST pass under the repository's Java 25 toolchain.
- NFR-3: All four behaviours MUST work with zero network requests. Status MUST be conveyed by visible text, not only colour. A 200-character description MUST wrap or remain inspectable without covering neighbouring fields.
- NFR-4: Sorting MUST retain the source insertion order and MUST NOT mutate the stored full list. Date tests MUST use a fixed/injected clock, not the day on which tests happen to run.
- NFR-5: Follow-up paths MUST introduce no new logging of full command text, client contact details or descriptions. The existing raw command logging in `LogicManager` and `AddressBookParser` MUST be removed or replaced with command-name-only logs by their owners.

No invented throughput or large-dataset performance target is added for this increment. Use the existing Developer Guide NFRs when assessing the whole product.

## Data Models

| Entity / field | Type | Contract |
| --- | --- | --- |
| `Person.followUp` | `Optional<FollowUp>` | Non-null optional. Empty means no pending action. Never a collection. |
| `FollowUp.dueDate` | `LocalDate` | Non-null, year 1-9999. Past dates allowed in the domain/storage layer. |
| `FollowUp.description` | `String` | Trim ordinary spaces; 1-200 printable ASCII characters. |
| `FollowUpStatus` | enum | `OVERDUE`, `DUE_TODAY`, `UPCOMING`; display labels defined below. Computed, never persisted. |
| Model full list | existing `AddressBook` | Retains insertion order and contact identity rules. |
| Model displayed list | `ObservableList<Person>` exposed read-only | Derived from full list plus active filter/mode; authoritative for indices. |
| Model today | read-only observable `LocalDate` to consumers | Snapshot used for display/status, published only at startup/success. |
| Model pending mode | read-only observable boolean | True only for the `followups` view. False for full/search views. |
| Model loading block | boolean | Session state, not JSON data. Set from startup loading outcome. |
| Candidate dirty state | boolean | Tracks attempted successful model mutations even when values equal old values. Starts false for each candidate. |

## API Contracts

These are **Java signature contracts**, not copy-ready class implementations. Use the existing package architecture and Java naming conventions. There is no HTTP API and no TypeScript implementation in this desktop feature. Do not invent different class names or parallel APIs in individual agent sessions.

### 1. Domain foundation - Ayush

```java
// package seedu.address.model.person
public final class FollowUp {
    private final LocalDate dueDate;
    private final String description;

    public FollowUp(LocalDate dueDate, String description);
    public static LocalDate parseDate(String text);
    public LocalDate getDueDate();
    public String getDescription();
    public FollowUpStatus getStatus(LocalDate today);
    // Override equals, hashCode and toString using both fields.
}

public enum FollowUpStatus {
    OVERDUE, DUE_TODAY, UPCOMING;
    public String getDisplayName();
}

// Add to existing Person; retain existing five-argument constructor.
public Person(Name name, Phone phone, Email email, Address address,
        Set<Tag> tags, Optional<FollowUp> followUp);
public Optional<FollowUp> getFollowUp();
public Person withFollowUp(FollowUp followUp);
public Person withoutFollowUp();
```

`FollowUp` has no setter, client reference, id, completed flag, stored status or clock. `parseDate` trims only U+0020 at the ends, checks `[0-9]{4}-[0-9]{2}-[0-9]{2}`, then strictly validates the date and year. It throws `IllegalArgumentException` with the exact invalid-date message. The constructor validates the LocalDate year and normalized description and throws `IllegalArgumentException` with the appropriate constraint message; null arguments throw `NullPointerException` for programmer misuse. It does **not** reject past dates. Do not use a forgiving date parser that turns 30 February into another date.

`getStatus(today)` compares the two dates. Display names are exactly `OVERDUE`, `DUE TODAY`, `UPCOMING`. Domain validation is reused by JSON loading and commands. The future-date restriction belongs only to the mutation command.

The old `Person` constructor delegates to the new one with `Optional.empty()`. `withFollowUp`/`withoutFollowUp` return new persons retaining every contact field and tag. `equals` and `hashCode` include the optional follow-up; `isSamePerson` does **not** include it. Do not change contact duplicate rules in this increment. The `PersonBuilder` copy constructor and legacy `EditCommand.createEditedPerson` must retain it. Add `PersonBuilder.withFollowUp(FollowUp)` and `withoutFollowUp()`.

### Message ownership across later PRs

**Do not create `FollowUpMessages.java`.** This table defines the shared *contract*, not a new central class. Introduce each constant when its owning behaviour is implemented. Reuse existing generic constants and update their wording in place; do not introduce synonymous follow-up-only index/input errors.

| Owner / implementation stage | Constant location | Exact value |
| --- | --- | --- |
| Ayush / foundation PR 1 | `FollowUp.MESSAGE_INVALID_DATE` | `Dates must exist and use YYYY-MM-DD format, with a year from 0001 to 9999.` |
| Ayush / foundation PR 1 | `FollowUp.MESSAGE_INVALID_DESCRIPTION` | `Follow-up descriptions must contain 1 to 200 printable English characters after trimming.` |
| Ayush / command PR 4 | `SetFollowUpCommand.MESSAGE_RECORDED` | `Follow-up for %1$s recorded for %2$s.` |
| Ayush / command PR 4 | `SetFollowUpCommand.MESSAGE_UPDATED` | `Follow-up for %1$s updated to %2$s.` |
| Ayush / command PR 4 | `SetFollowUpCommand.MESSAGE_PAST_DATE` | `Follow-up date cannot be in the past.` |
| Ayush / command PR 4 | `ClearFollowUpCommand.MESSAGE_CLEARED` | `Follow-up for %1$s cleared.` |
| Ayush / command PR 4 | `ClearFollowUpCommand.MESSAGE_NO_FOLLOW_UP` | `This client has no pending follow-up.` |
| Gwen / query PR 5 | `FollowUpsCommand.MESSAGE_PENDING` | `Pending follow-ups` |
| Gwen / query PR 5 | `FollowUpsCommand.MESSAGE_NO_PENDING` | `There are no pending follow-ups.` |
| Gwen / query PR 5 | Existing `ListCommand.MESSAGE_SUCCESS` | `Listed all clients.` |
| Josthan / parser PR 6 | Existing `ParserUtil.MESSAGE_INVALID_INDEX` | `Index must be a positive integer from 1 to 2147483647.` |
| Josthan / parser PR 6 | Existing `Messages.MESSAGE_INVALID_PERSON_DISPLAYED_INDEX` | `The client index provided is invalid.` |
| Josthan / parser PR 6 | Existing `Messages.MESSAGE_DUPLICATE_FIELDS` | `Multiple values specified for the following single-valued field(s): ` (ends in one space) |
| Josthan / parser PR 6 | `Messages.MESSAGE_INVALID_CHARACTERS` | `Use one command line and ordinary spaces. Control characters are not allowed.` |
| Josthan / parser PR 6 | `Messages.MESSAGE_EMPTY_COMMAND` | `Enter a command.` |
| Josthan / parser PR 6 | `Messages.MESSAGE_LOAD_WARNING` | `Saved data could not be loaded. Client changes are blocked to protect your saved data. Close Policy Harbour and restore a valid saved copy before trying again.` |
| Josthan / parser PR 6 | `Messages.MESSAGE_CHANGES_BLOCKED` | `Client changes are blocked because saved data could not be loaded. Close Policy Harbour and restore valid saved data.` |
| Somaaditya / persistence PR 7 | `LogicManager.MESSAGE_SAVE_FAILED` | `Changes could not be saved. No changes were kept. Try the command again.` |
| Kin Chong / UI PR 8 | `PersonListPanel.MESSAGE_NO_CLIENTS` | `No clients to display.` |

`Messages` is the existing cross-command message owner, so the shared load-block diagnostics belong there. Josthan introduces the load diagnostics with the dispatch gate; Somaaditya and Kin Chong consume them rather than editing that same file. Save failures stay with LogicManager's existing save-error handling. Startup reuses `ListCommand.MESSAGE_SUCCESS`; there is no separate `MESSAGE_STARTUP` constant. The UI reuses `FollowUpsCommand.MESSAGE_NO_PENDING` for the pending empty state.

The domain class MUST NOT import the logic package. Keep both validation messages directly on `FollowUp`; commands and JSON adapters consume its validation, without alias constants in another class. Generic constant identifiers retain AB3's names for compatibility, even where their wording changes from person to client.

`SetFollowUpCommand.MESSAGE_USAGE` contains both supported singular-command forms, exactly these two lines:

```text
Usage: followup INDEX d/DATE m/DESCRIPTION
Or: followup INDEX clear
```

Josthan constructs format errors with the **existing** `Messages.MESSAGE_INVALID_COMMAND_FORMAT` and `String.format(..., SetFollowUpCommand.MESSAGE_USAGE)`. The result is exactly three lines, without extra examples or blank lines:

```text
Invalid command format!
Usage: followup INDEX d/DATE m/DESCRIPTION
Or: followup INDEX clear
```

The two diagnostics containing `Policy Harbour` intentionally match the supplied PDF. Resolve any product-spelling change centrally with the team; agents must not each normalize messages differently. Domain parsing and enum display labels are deliberately unchanged in version 2.

### 2. Parsing - Josthan

```java
// package seedu.address.logic.parser
public final class FollowUpCommandParser implements Parser<Command> {
    public static final String COMMAND_WORD = "followup";
    public Command parse(String args) throws ParseException;
}

public final class FollowUpParserUtil {
    public static Index parseIndex(String value) throws ParseException;
}

// Add to CliSyntax:
public static final Prefix PREFIX_FOLLOW_UP_DATE = new Prefix("d/");
public static final Prefix PREFIX_FOLLOW_UP_DESCRIPTION = new Prefix("m/");

// Preserve AddressBookParser.parseCommand(String), delegating with false.
public Command parseCommand(String input, boolean isDataLoadingBlocked)
        throws ParseException;
```

Global dispatch first checks the **raw input** for U+0000-U+001F, U+007F-U+009F, U+2028 and U+2029. Reject rather than trimming those away. Then trim ordinary spaces, reject empty input, recognize the case-sensitive command word, and apply the loading block before invoking the command-specific parser. Preserve other existing routes this iteration. Block `add`, `delete`, `followup` and the still-present legacy mutators `edit` and `clear`; permit read-only routes. An unknown word still produces existing `Unknown command.` before a load-block error.

The `followups` route returns `new FollowUpsCommand()` and ignores ordinary trailing text after global checks. It requires no separate parser class. The singular `followup` route uses `FollowUpCommandParser`.

Recognize only `d/` and `m/` for follow-up arguments, using existing `ArgumentTokenizer`. Prefixes require a preceding ordinary space. Other prefix-like text is part of the value and goes through value validation. There is no quoting/escaping. `m/clear` is description text, not the clear operation.

Validate in this exact order:

1. Complete **shape**: either one index token before both present prefixes, or exactly two tokens `INDEX clear` without prefixes. Missing prefixes, a missing index or extra preamble/clear tokens produce the format message. Empty *present* prefix values pass shape validation.
2. Repeated prefixes: report each duplicate once, in `d/, m/` order with comma-space separation. Update the existing `Messages.getErrorMessageForDuplicatePrefixes` to preserve the supplied prefix order, remove duplicates without an unordered set, and join with comma-space. Then reuse `argMultimap.verifyNoDuplicatePrefixesFor(PREFIX_FOLLOW_UP_DATE, PREFIX_FOLLOW_UP_DESCRIPTION)`. Add literal expected-output tests for one and multiple duplicates. Update directly affected legacy parser tests to match the order their production parser supplies; do not create a second formatter.
3. Index syntax: ASCII digits only, numeric value 1-2147483647; leading zeroes allowed. Reject overflow with the specified message, not a leaked `NumberFormatException`. Strip leading zeroes before bounded parsing so arbitrarily many leading zeroes do not falsely overflow a valid index.
4. Construct the command with the parsed `Index` and **raw date/description strings**. Date validation is deliberately deferred until execution, because displayed-list range errors have higher priority.

Keep global `ParserUtil.parseIndex` behaviour for legacy commands in this increment, but update its existing `MESSAGE_INVALID_INDEX` wording in place. `FollowUpParserUtil.parseIndex` uses that same constant while enforcing the new command's exact leading-zero and range rules. Similarly, update `Messages.MESSAGE_INVALID_PERSON_DISPLAYED_INDEX` in place, and have both follow-up commands use it. Run the full parser/command regression suite because shared wording changes affect legacy consumers too. Do not add a parallel `MESSAGE_INDEX_OUT_OF_RANGE` or a second index-message constant. In PR 4, commands already refer to the existing shared constant; PR 6 changes its wording when dispatch integration lands.

### 3. Mutation commands - Ayush

```java
// package seedu.address.logic.commands
public final class SetFollowUpCommand extends Command {
    public SetFollowUpCommand(Index targetIndex, String dateText, String descriptionText);
    public CommandResult execute(Model model) throws CommandException;
    // Override equals and toString as in existing command classes.
}
public final class ClearFollowUpCommand extends Command {
    public ClearFollowUpCommand(Index targetIndex);
    public CommandResult execute(Model model) throws CommandException;
}
```

Both operate on the supplied model, which will be a private candidate when called through `LogicManager`. Neither writes files nor calls `updateFilteredPersonList(PREDICATE_SHOW_ALL_PERSONS)`.

`SetFollowUpCommand.execute` checks displayed range first, captures that `Person`, parses the date, rejects a past date against `model.getToday()`, then validates description/constructs the immutable `FollowUp`. Replace through `model.setPerson(target, target.withFollowUp(value))`. Choose recorded/updated using the **old** person's optional action. Format dates as `YYYY-MM-DD`. Replacing identical values still calls `setPerson` and requests persistence.

`ClearFollowUpCommand.execute` checks displayed range, then presence, then calls `model.setPerson(target, target.withoutFollowUp())`. Missing follow-up is an error. Never reuse `DeleteCommand` to clear an action.

### 4. Model and pending query - Gwen

Preserve every existing `Model` method. Add these APIs and implement them in `ModelManager`:

```java
public interface Model {
    // Existing methods remain, including getFilteredPersonList().
    void showPendingFollowUps();
    LocalDate getToday();
    ReadOnlyObjectProperty<LocalDate> todayProperty();
    ReadOnlyBooleanProperty showingFollowUpsProperty();
    boolean isDataLoadingBlocked();
    Model forkForCommand(LocalDate today);
    boolean hasUnsavedChanges();
    void commitFrom(Model candidate);
}

// Keep existing ModelManager constructors as compatibility overloads.
public ModelManager(ReadOnlyAddressBook data, ReadOnlyUserPrefs prefs,
        LocalDate today, boolean isDataLoadingBlocked);

// package seedu.address.logic.commands
public final class FollowUpsCommand extends Command {
    public static final String COMMAND_WORD = "followups";
    public FollowUpsCommand();
    public CommandResult execute(Model model);
}
```

Keep one stable, unmodifiable observable displayed-list object for callers. Its contents may be rebuilt when data/mode changes; do not replace the object that the UI is observing. A small internal backing list populated from the full list is sufficient. Pending results are selected in full-list order, then stably sorted by due date. Do not sort `AddressBook.getPersonList()` in place or sort independently in the UI. Tests must prove equal-date ordering survives replacement, deletion and restart.

`updateFilteredPersonList(predicate)` sets ordinary/search mode, resets pending sorting, and applies the predicate to the full list. `showPendingFollowUps()` clears the previous search by selecting pending persons from all data. `setPerson` and `deletePerson` recompute the existing view without resetting its mode. `addPerson` retains its existing full-list behaviour. Rebuild row numbering in the UI from the same list indices.

`forkForCommand(today)` returns an isolated `ModelManager` with copied `AddressBook`, preferences, active predicate/mode and loading-block state, but the supplied date and `hasUnsavedChanges() == false`. Immutable persons may be shared; backing observable lists and mutable preferences must not be shared. Date sampling is outside this method. Mutating APIs set the candidate dirty flag even for an identical accepted replacement. Query methods never do.

`commitFrom(candidate)` publishes the candidate's validated data, filter/mode and date into the existing live model and its stable observable objects. It does no validation or file I/O. Do not publish partial intermediate list states when rebuilding; update the derived displayed list once using `setAll` after the internal state is ready. Read-only success also publishes its view/date. If no live data changed, avoid unnecessary full data replacement. There is no live-state rollback: failed candidates are simply discarded.

This is a single-threaded command protocol, not a general transaction framework. Future profile selection state would also need to be copied/published, but there is no profile state at the inspected baseline. Do not introduce a stored client id just for sorting or row numbering.

`FollowUpsCommand` invokes `showPendingFollowUps()` and returns its own `MESSAGE_PENDING` or `MESSAGE_NO_PENDING` based on the resulting list. It performs no save.

### 5. Storage, startup and Logic bridge - Somaaditya

Add nested optional JSON to each existing person object; keep the existing `persons` root and contact/tag format:

```json
{
  "name": "Rachel Lim",
  "phone": "91234567",
  "email": "rachel@example.com",
  "address": "21 Clementi Road",
  "tags": [],
  "followUp": {
    "dueDate": "2026-10-05",
    "description": "Send updated quotation"
  }
}
```

A missing `followUp` property or JSON `null` means no action. Serialize no action as `"followUp": null` for a deterministic shape with the current Jackson configuration. The non-null value must be **one object**, never an array. Both fields must be present and valid; an empty object or one missing field fails the entire load. Due dates in the past remain valid. Status, dirty flag, view mode and today are not persisted.

```java
// package seedu.address.storage; package-private like JsonAdaptedPerson
class JsonAdaptedFollowUp {
    @JsonCreator
    public JsonAdaptedFollowUp(@JsonProperty("dueDate") String dueDate,
            @JsonProperty("description") String description);
    public JsonAdaptedFollowUp(FollowUp source);
    public FollowUp toModelType() throws IllegalValueException;
}

// Add nullable JsonAdaptedFollowUp as the final JsonAdaptedPerson creator argument.
// Keep the old five-argument Java overload for existing test callers;
// exactly one constructor is annotated @JsonCreator.

// Add overload; preserve the existing two-argument LogicManager constructor.
public LogicManager(Model model, Storage storage, Clock clock);

// Add to Logic; LogicManager delegates these to its live Model.
ReadOnlyObjectProperty<LocalDate> todayProperty();
ReadOnlyBooleanProperty showingFollowUpsProperty();
boolean isDataLoadingBlocked();
```

Use the domain validators and convert invalid values to `IllegalValueException` so existing `DataLoadingException` handling covers the whole dataset. Do not silently drop an invalid action. Do not disable validation to accept old data; legacy JSON simply has an empty optional action. Keep the existing address-book file path this iteration.

Execution algorithm:

1. Call `addressBookParser.parseCommand(text, model.isDataLoadingBlocked())`.
2. Sample one date. For production, resolve the device's current zone for **each** invocation; a cached `Clock.systemDefaultZone()` can retain an old zone after a system-zone change. For tests, use the injected clock consistently. No command/UI/storage class samples time independently.
3. Create `Model candidate = model.forkForCommand(today)`.
4. Execute the parsed command on that candidate. On failure, discard it.
5. If candidate is dirty, save its address book. On any `IOException`, discard it and throw `CommandException(LogicManager.MESSAGE_SAVE_FAILED, cause)`.
6. Publish with `model.commitFrom(candidate)` only after saving succeeds, or immediately for read-only success; then return the command result.

Atomic disk save: serialize the whole candidate, write a temporary file in the target's directory, close it, then atomically move it over the target. Do not truncate/create the destination before the operation. If atomic replacement is unsupported or the move fails, report failure and leave the old file untouched; do not silently fall back to a truncating write. Remove temporary files best-effort without masking the original error or turning an already successful replacement into a reported failure. Test failed write and failed move separately. No backup/recovery subsystem is required, and power-loss durability beyond this file-operation contract is not claimed.

At startup, use empty data for a missing file, with loading block false. On `DataLoadingException`, use empty data with loading block true and retain the file. Both initialize a full-list view and sampled startup date. Make the startup factory testable, e.g. package-visible `initModelManager`, without changing the application's public launch flow. The UI shows `ListCommand.MESSAGE_SUCCESS` on valid startup; invalid startup also displays a separate persistent warning. Existing preferences saving remains separate from client data.

### 6. UI - Kin Chong

```java
// Update the existing call sites within the UI owner's files.
public PersonCard(Person person, int displayedIndex, LocalDate today);
public PersonListPanel(ObservableList<Person> persons,
        ReadOnlyObjectProperty<LocalDate> today,
        ReadOnlyBooleanProperty showingFollowUps);
```

`MainWindow` passes the three Logic values to `PersonListPanel`. Each rendered card derives status from `FollowUp.getStatus(today)` and uses its display name. Show no-action text `Follow-up: None` on ordinary client cards; pending mode contains only clients with actions. Retain existing contact information this iteration; the separate profile redesign is out of scope.

The panel listens for date and mode changes, updates its empty placeholder, and refreshes cells. Also ensure cell index changes rebuild the displayed number: JavaFX may move/reuse cells after sorting without a different person value. Do not rely only on `updateItem` to keep indices correct. Do not register persistent property listeners separately on every short-lived card; keep refresh listeners at panel level.

Use a separate warning label in `MainWindow.fxml` for failed loading, displaying `Messages.MESSAGE_LOAD_WARNING`. Bind its visibility/managed state to the startup block; later result messages must not hide it. Route empty input through Logic instead of returning early in `CommandBox`. Keep failed text, clear only after returned success, and preserve focus.

The screenshot is visual guidance, not authority over behaviour. In particular, `followups` replaces the current numbered list; do not implement an independently indexed second pending list while commands still target the first list. No edit to `docs/images/Ui.png` is needed for this increment.

## Acceptance Criteria

Each owner adds tests for their boundary. Somaaditya's workflow integration tests cross the parser/model/storage boundary; Kin Chong supplies the GUI checks. These criteria cover the full increment. PR #44 tests the domain foundation only; later packages must supply the remaining evidence.

### AC-1: Record and replace (FR-1, FR-2, FR-6)

**Given** a fixed date of 2026-10-04 and a visible client without an action, **When** `followup 1 d/2026-10-05 m/Send quotation` succeeds, **Then** exactly one action exists and the recorded message appears. **When** the command is repeated with a different date/description, **Then** both values replace the old action; identical replacement also succeeds with the updated message. Owner: Ayush; integration: Somaaditya.

### AC-2: Clear without deleting (FR-1, FR-3, FR-5)

**Given** a client with a pending action, **When** `followup 1 clear` succeeds, **Then** the client remains and the action is absent. In pending mode the row disappears; in ordinary/search mode the client remains. A second clear in the full list produces the no-follow-up error. Owner: Ayush/Gwen.

### AC-3: Query, ordering and view switching (FR-4, FR-5; NFR-4)

**Given** clients inserted A, B, C, D, with B and D on an earlier equal date, A on a later date, and C with no action, **When** `followups` runs after a search, **Then** the visible order is B, D, A, indexed 1-3. `followups anything` behaves identically. **When** `list` or `find` runs, **Then** full/search insertion order returns. Owner: Gwen.

### AC-4: Current indices and immediate resort (FR-2, FR-3, FR-5)

**Given** pending order B, D, A, **When** `followup 1` replaces B's date with a later date, **Then** B is changed, not full-list item A, and the new list is re-sorted/re-numbered. Subsequent indexed clear/delete acts on the new visible row. Deletion removes the whole client and action; add resets to the full list. Owner: Gwen/Somaaditya; visible indices: Kin Chong.

### AC-5: Date and description boundaries (FR-6, FR-8)

**Given** a fixed date, **When** date/description boundaries are tested, **Then** today and a valid future leap date are accepted; yesterday, `0000-01-01`, `10000-01-01`, `2026-02-30`, `2026-1-01`, slash-formatted dates, and non-ASCII date digits fail appropriately. Descriptions of 1 and 200 valid characters succeed; blank, 201 characters, emoji and non-ASCII letters fail. Internal spaces and punctuation are preserved. Owner: Ayush.

### AC-6: Grammar and error priority (FR-6, FR-8)

**Given** several simultaneously invalid fields, **When** input is processed, **Then** the first error follows the prescribed order. Test `followup 999 d/bad m/` for range before date; `followup 0 d/2026-10-05 d/2026-10-06 m/x` for duplicate before index; missing `m/` for shape before duplicate; `followup 1 clear extra` for format; `followup 1.5 clear` for index syntax. Accept `01` and long leading-zero indices representing 1; reject 0, signs and values above 2147483647. Both repeated prefixes produce exactly `d/, m/`. Owner: Josthan/Ayush.

### AC-7: Shared input and focus (FR-8, FR-11)

**Given** a command box, **When** empty/control-character input or uppercase `FOLLOWUP` is entered, **Then** the correct error appears, previous data/view remain unchanged, and the input is retained. **When** valid input succeeds, **Then** the box clears and retains keyboard focus. Test control characters before unknown-command and blocked-load checks. Owner: Josthan/Kin Chong.

### AC-8: Persistence and backward compatibility (FR-9; NFR-2)

**Given** contact-only legacy JSON and valid new JSON, **When** each is loaded, **Then** old contacts have no action and new actions retain exact fields and insertion order. **When** record, replace and clear are saved and the app restarts, **Then** each result survives and the full list is shown. Past saved dates load successfully. Owner: Somaaditya.

### AC-9: Failed save is a no-op (FR-8, FR-9; NFR-1)

**Given** a saved dataset and active pending/search view, **When** add, delete, record, replace or clear encounters injected save failure, **Then** live contacts, action values, list mode/order, displayed date and original file bytes are unchanged. No success feedback is emitted; command input remains. Test errors during temporary write and final move, plus unsupported atomic replacement. Owner: Somaaditya/Gwen.

### AC-10: Invalid loading protects the file (FR-10; NFR-1)

**Given** invalid JSON, a missing follow-up field, an impossible stored date, an invalid description or an array of actions, **When** startup occurs, **Then** nothing is partially loaded, the warning persists across `list`/`followups`, and mutation commands are blocked even with invalid arguments. Unknown command/control-character errors take their higher priority. File bytes stay unchanged. A missing file instead gives an empty writable app; restarting with valid data clears the block. Owner: Somaaditya/Josthan/Kin Chong.

### AC-11: Status refresh without saving (FR-7, FR-9; NFR-4)

**Given** overdue, today and future dates, **When** startup or a successful command refreshes the sampled date, **Then** all labels use the same date. After advancing a mutable test clock, a failed command leaves labels unchanged and a successful `list` refreshes them. Read-only success and failed commands each make zero client-data save calls. Verify a changed device zone is used at the next production date sample. Owner: Somaaditya/Gwen/Kin Chong.

### AC-12: Visible complete workflow (FR-11; NFR-3)

**Given** a desktop launch with network disconnected, **When** the four behaviours are performed using typed commands, **Then** every pending row shows all required fields, textual statuses are readable, long descriptions do not overlap, empty-list messages are correct, and row numbers follow reordering. Owner: Kin Chong.

### AC-13: Compatibility, immutable copies and logging (FR-1, FR-5; NFR-2, NFR-5)

**Given** a person with an action, **When** legacy edit, `PersonBuilder` copying, model copying and JSON round trips occur, **Then** the action is retained unless explicitly cleared/deleted. Follow-up changes affect `equals`/`hashCode` but not contact identity. **When** commands run, **Then** production logs contain no raw command descriptions/contact fields from the touched command-logging paths. Existing tests and Checkstyle pass. Owner: Ayush/Somaaditya/Josthan.

## Edge Cases

- EC-1: An overdue action can be loaded and listed, but replacement requires a new date of today or later. Rejecting all past dates in the domain constructor breaks this distinction.
- EC-2: Equal dates must retain full-list order, including after deleting and re-adding a client. Sorting the live source list destroys this invariant.
- EC-3: `followup 1 d/2026-10-05 m/clear` records the word clear. Recognized `d/` or `m/` inside free text starts another parameter; unrecognized `p/` stays ordinary description text. Quote marks provide no escape mechanism.
- EC-4: Invalid raw control characters are rejected before tokenizer trimming can erase them. Non-ASCII printable description text reaches description validation rather than being silently removed.
- EC-5: A successful identical replacement is still a mutation/save attempt. Comparing old/new address books for inequality is insufficient to decide whether to save; use the explicit dirty flag.
- EC-6: A read-only query after a failed load must not overwrite the damaged file with an empty dataset. This is a real risk in the current unconditional-save LogicManager.
- EC-7: Disk full, denied access, temporary-file failure, unsupported atomic replacement and final move failure must preserve old data and report the single save-failure message.
- EC-8: Refreshing the date before a command that later fails would change labels on failure. Only the candidate receives the new date until successful publication.
- EC-9: Deleting the only pending client yields the pending empty state; clearing their action keeps the client accessible through `list`.
- EC-10: Missing/null follow-up is valid legacy data; a non-null partial object is invalid. Never convert a corrupt partial object into an empty optional.
- EC-11: Multiple application instances and external file edits while running are outside the MVP. Do not add file watching or concurrent merge logic.

## Out of Scope

- OS-1: Multiple actions per client, completion history, notifications, reminders, recurrence, separate reschedule/complete commands, calendar integration, cloud sync and policy management. These exceed the supplied follow-up MVP.
- OS-2: A new `Client` class, root package rename, UUIDs, database, new framework, build-tool upgrades or full visual redesign. These are unnecessary shared-file changes.
- OS-3: Full client profile / `view` command. The baseline has neither; this increment renders follow-up details in existing cards. Before claiming the **whole MVP** is complete, a later profile task must implement selected-client identity preservation, immediate profile refresh on follow-up change, and clearing when that client leaves the view. That task must use the same displayed-list API and copy/publish selection state in transactions.
- OS-4: Replacing all legacy add/name/phone/email/tag/duplicate rules or deleting legacy `edit`, `help`, `clear`, `exit` routes. Those are separate MVP-alignment tasks. Preserve follow-ups through legacy edit and block all existing mutators on failed loading now. Do not claim full section-3/section-9 contact-validation compliance until the contact rules are aligned.
- OS-5: Silently editing the source MVP or deleting existing tests to make a branch pass. Any intentional contract change must be recorded here and agreed by the affected owners.

## Merge sequence and shared foundation

File ownership reduces textual conflicts; it cannot guarantee zero semantic conflicts. A common compiled baseline plus integration tests is mandatory. No agent should create competing versions of the same shared class.

1. **Agree on this contract and names.** Each person takes one package. Keep course contributions under that person's Git author name/email. Create a separate issue/PR per package according to team workflow. Agree the calendar deadline as a team; the dependency stages below do not by themselves establish that the whole feature fits into the remaining time.
2. **Land small foundation PRs before parallel consumers depend on them.** Ayush first lands the immutable domain types with their validation messages, backward-compatible `Person` and builder changes. No future command, generic input, storage or startup constants belong in this foundation PR. Gwen then lands the working Model APIs/candidate/display-list support. Somaaditya lands the Logic getter bridge/clock constructor against those APIs. Each owner edits only their files. All three foundations must compile and pass relevant regression tests; do not merge `UnsupportedOperationException` placeholders or failing stubs.
3. **Start parallel implementation from the common foundation.** Ayush completes mutation commands; Josthan develops parsing tests/implementation; Gwen completes pending command/tests; Somaaditya completes persistence/execution; Kin Chong builds the UI. Before foundation merges, people can design tests and work privately, but must not duplicate missing shared production classes in their branch.
4. **Merge producers before routers.** Domain/model foundation -> mutation and pending commands -> parser routes -> persistence/execution/startup integration -> UI integration. Independent tests/UI work can be developed earlier; compile-ready PRs must include or wait for their actual dependencies. In particular, dispatcher imports must not be merged before the imported command classes exist.
5. **Integrate and demonstrate.** Everyone syncs to the integrated upstream head and runs their focused tests. Somaaditya runs the workflow suite; Kin Chong executes the visible demo. All five review the acceptance criteria and fix their owned files. Reserve the final work session for failures at boundaries instead of scheduling five last-minute PRs.

Suggested branches: `codex/followup-domain-commands`, `codex/followup-parser`, `codex/followup-list-model`, `codex/followup-storage`, `codex/followup-ui`. Split foundation work into smaller branches/PRs when needed. Create feature branches from the team's current `upstream/master` after required foundations merge. Do not branch five implementations from an old unmerged README branch. Do not force-push another person's branch.

Suggested review pairs: Gwen reviews Ayush's model assumptions; Ayush reviews Josthan's parser/command boundary; Somaaditya reviews Gwen's candidate state; Gwen reviews Somaaditya's save/publish behaviour; Josthan reviews Kin Chong's displayed indices against parser semantics. Review does not transfer file ownership.

### Exact PR flow

The numbers below are **plan steps**, not GitHub PR numbers. Foundation step 1 is GitHub PR #44. Every step uses its own feature branch in the author's fork and targets team `master`, with an assigned issue, milestone and teammate review.

| Plan PR | Owner | Scope | Required predecessors |
| --- | --- | --- | --- |
| 1 | Ayush | Domain values/status/validation, optional `Person` field, copy/edit preservation and tests; no central message class | Current upstream baseline |
| 2 | Gwen | Model APIs, displayed-list state, candidate copying/publishing and tests | 1 |
| 3 | Somaaditya | Logic read-only property bridge and clock constructor; retain existing execution temporarily | 2 |
| 4 | Ayush | Record/replace/clear commands and their success/usage messages | 3 |
| 5 | Gwen | Pending-list command and messages; update existing `ListCommand.MESSAGE_SUCCESS` wording | 3 |
| 6 | Josthan | Parser/routes, shared input/index/duplicate diagnostics, load-block diagnostics and gate | 4 and 5 |
| 7 | Somaaditya | JSON, atomic saving, candidate execution and startup integration | 6 |
| 8 | Kin Chong | UI, empty states, persistent load warning, keyboard flow and demonstration | Start UI work after 3; shared diagnostics require 6; complete and merge after 7 |

Merge order: **1 -> 2 -> 3 -> {4, 5} -> 6 -> 7 -> 8**. Steps 4 and 5 may merge in either order. Storage serialization work and UI construction can proceed in parallel with command/parser work after their domain/API prerequisites are available. Missing command/message references are dependencies to wait for, not permission to add duplicate stubs in another owner's files.

### Package completion checks

| Owner | Minimum evidence in their PR |
| --- | --- |
| Ayush | Value validation/immutability, constructor compatibility, record vs replace messages, clear/no-action behaviour, copy/edit preservation. |
| Josthan | Both routes, exact messages, prefix ordering, leading zeroes/overflow, global input checks, load-block precedence, no raw input logs. |
| Gwen | Sorting/ties, search reset, reordering after changes, current indices, isolated candidates, unchanged live state on discarded candidate, stable observable list. |
| Somaaditya | Legacy/new JSON tests, overdue reload, failed-load protection, failed-write/move tests, no read-only saves, integrated successful-save-before-publish flow. |
| Kin Chong | Screenshot/demo of the actual feature, visible status/date/description, empty states, reindexed rows, persistent warning, input focus/retention, long description layout. |

Run relevant JUnit tests while developing, then the existing repository checks at integration:

```bash
./gradlew check coverage
.github/run-checks.sh
git diff --check
```

Do not infer success merely because the Markdown plan is validated. The commands above must run against implemented application code; record actual failures or missing Java/JavaFX tooling in PRs. Do not disable Checkstyle or tests.

### Deterministic integration demo

Use a fixed clock with today = 2026-10-04 in tests; for a live demo use dates relative to the device's day. Start with three valid clients in order Rachel, Amanda, John.

1. Record Rachel for today+2 and Amanda for today+1. Both return recorded messages.
2. Run `followups`: Amanda is 1, Rachel is 2, John is absent.
3. Replace row 1 with today+3: Rachel becomes 1, Amanda becomes 2. Confirm Amanda changed.
4. Clear row 1: Rachel's action disappears; her client still appears in `list`.
5. Restart: Amanda's replacement persists, Rachel still has no action, and the full list is restored.
6. Load a fixture containing an overdue action: it appears as `OVERDUE`; it is not silently cleared.
7. Inject a save failure while replacing Amanda: old data, display and file remain unchanged, with the exact failure message.

## Copy-paste instructions for each coding agent

Replace the owner name with exactly one of the five names above; the agent then uses that owner's write allowlist.

```text
Implement my work package in FOLLOWUP_IMPLEMENTATION.md. My owner name is
[OWNER NAME]. Read the complete contract and applicable repository instructions
first. Check the current commit and whether the foundation/dependency PRs have
merged. Report any mismatch with the contract before choosing a different API.

You are not alone in this codebase. Do not revert others' work. Edit only my
owned files, including my tests. Reuse the exact shared classes, method
signatures, JSON fields, command grammar and messages. Do not create a second
FollowUp type, change the data model, or implement another person's package.

If a needed shared API is absent, identify its owner and dependency; continue
independent work, but do not add a competing stub or merge uncompilable code.
Use fixed dates in tests. Cover the acceptance criteria assigned to me and
regressions touched by my change. Report changed files, tests actually run,
remaining dependency blockers and any proposed contract change.

Do not commit, push or open a PR unless I separately authorize that action.
```

Validation note: the version-2 document is checked for structural completeness and consistent message ownership. The structure validator has a desktop-inapplicable HTTP endpoint warning. PR #44 validates only the implemented domain foundation; the command, parser, model, persistence and UI acceptance criteria above remain work for later PRs. Do not treat foundation test results as evidence that the full workflow is implemented.

This document is the shared planning artifact. Once the team accepts it, update its status/version centrally and distribute the same revision to all five agents. Future user instructions can change the plan; record agreed interface changes before dependent implementations drift apart.
