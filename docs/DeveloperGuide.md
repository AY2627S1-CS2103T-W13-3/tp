---
layout: page
title: Developer Guide
---
* Table of Contents
{:toc}

--------------------------------------------------------------------------------------------------------------------

## **Acknowledgements**

* PolicyHarbor adapts [AddressBook Level 3](https://github.com/se-edu/addressbook-level3), retaining its Java/JavaFX architecture and test conventions.
* All team members used ChatGPT for general assistance during the project.

--------------------------------------------------------------------------------------------------------------------

## **Setting up, getting started**

Refer to the guide [_Setting up and getting started_](SettingUp.md).

--------------------------------------------------------------------------------------------------------------------

## **Design**

<div markdown="span" class="alert alert-primary">

:bulb: **Tip:** The `.puml` files used to create diagrams are in `docs/diagrams`. Refer to the [_PlantUML Tutorial_ at se-edu/guides](https://se-education.org/guides/tutorials/plantUml.html) to learn how to create and edit diagrams.
</div>

### Architecture

<img src="images/ArchitectureDiagram.png" width="280" />

The _**Architecture Diagram**_ given above explains the high-level design of the App.

The following provides a quick overview of the main components and their interactions.

**Main components of the architecture**

**`Main`** (consisting of classes [`Main`](https://github.com/AY2627S1-CS2103T-W13-3/tp/blob/master/src/main/java/seedu/address/Main.java) and [`MainApp`](https://github.com/AY2627S1-CS2103T-W13-3/tp/blob/master/src/main/java/seedu/address/MainApp.java)) is in charge of the app launch and shut down.

* At app launch, it initializes the other components in the correct sequence, and connects them up with each other.
* At shut down, it shuts down the other components and invokes cleanup methods where necessary.

The bulk of the app's work is done by the following four components:

* [**`UI`**](#ui-component): The UI of the App.
* [**`Logic`**](#logic-component): The command executor.
* [**`Model`**](#model-component): Holds the data of the App in memory.
* [**`Storage`**](#storage-component): Reads data from, and writes data to, the hard disk.

[**`Commons`**](#common-classes) represents a collection of classes used by multiple other components.

**How the architecture components interact with each other**

The _Sequence Diagram_ below shows how the components interact with each other for the scenario where the user issues the command `delete 1`.

<img src="images/ArchitectureSequenceDiagram.png" width="574" />

Each of the four main components (also shown in the diagram above),

* defines its _API_ in an `interface` with the same name as the Component.
* provides its functionality through a concrete `{Component Name}Manager` class that implements the corresponding API interface.

For example, the `Logic` component defines its API in `Logic.java` and implements it in `LogicManager.java`. Other components interact with a component through its interface rather than its concrete class, preventing them from coupling to that component's implementation, as illustrated in the following partial class diagram.

<img src="images/ComponentManagers.png" width="300" />

The sections below give more details of each component.

### UI component

The **API** of this component is specified in [`Ui.java`](https://github.com/AY2627S1-CS2103T-W13-3/tp/blob/master/src/main/java/seedu/address/ui/Ui.java)

![Structure of the UI Component](images/UiClassDiagram.png)

The UI consists of a `MainWindow` and its parts, including `CommandBox`, `ResultDisplay`, `PersonListPanel`, `ClientProfilePanel` and `StatusBarFooter`. All of these, including `MainWindow`, inherit from the abstract `UiPart` class, which captures common behavior among classes that represent visible GUI parts.

The `UI` component uses the JavaFX UI framework. The layouts of these UI parts are defined in matching `.fxml` files in `src/main/resources/view`. For example, [`MainWindow.fxml`](https://github.com/AY2627S1-CS2103T-W13-3/tp/blob/master/src/main/resources/view/MainWindow.fxml) specifies the layout of [`MainWindow`](https://github.com/AY2627S1-CS2103T-W13-3/tp/blob/master/src/main/java/seedu/address/ui/MainWindow.java).

The `UI` component,

* executes user commands using the `Logic` component.
* listens for changes to `Model` data so that the UI can be updated with the modified data.
* keeps a reference to the `Logic` component, because the `UI` relies on the `Logic` to execute commands.
* depends on some classes in the `Model` component because it displays `Person` objects from the model.

### Logic component

**API** : [`Logic.java`](https://github.com/AY2627S1-CS2103T-W13-3/tp/blob/master/src/main/java/seedu/address/logic/Logic.java)

Here's a (partial) class diagram of the `Logic` component:

<img src="images/LogicClassDiagram.png" width="550"/>

The sequence diagram below illustrates the interactions within the `Logic` component, taking `execute("delete 1")` API call as an example.

![Interactions Inside the Logic Component for the `delete 1` Command](images/DeleteSequenceDiagram.png)

<div markdown="span" class="alert alert-info">:information_source: **Note:** The lifeline for `DeleteCommandParser` should end at the destroy marker (X), but due to a limitation of PlantUML, it continues to the end of the diagram.
</div>

How the `Logic` component works:

1. When `Logic` is called upon to execute a command, the command is passed to an `AddressBookParser` object, which in turn creates a parser that matches the command (e.g., `DeleteCommandParser`) and uses it to parse the command.
1. `LogicManager` samples a command date and forks an isolated model candidate. The parsed command executes against that candidate.
1. Dirty client data is saved before the candidate is published with `Model.commitFrom`. A parse, command or save failure discards the candidate and leaves the live state unchanged.
1. A successful read-only command publishes list/selection/date changes without saving. Its `CommandResult` supplies the feedback text returned to the UI.

Here are the other classes in `Logic` (omitted from the class diagram above) that are used for parsing a user command:

<img src="images/ParserClasses.png" width="600"/>

How the parsing works:

* When called upon to parse a user command, the `AddressBookParser` class creates an `XYZCommandParser` (`XYZ` is a placeholder for the specific command name, e.g., `AddCommandParser`). The parser uses the other classes shown above to parse the user command and create an `XYZCommand` object (e.g., `AddCommand`). The `AddressBookParser` returns that object as a `Command` object.
* All `XYZCommandParser` classes, such as `AddCommandParser` and `DeleteCommandParser`, implement the `Parser` interface so they can be treated similarly where appropriate, for example during testing.

### Model component

**API** : [`Model.java`](https://github.com/AY2627S1-CS2103T-W13-3/tp/blob/master/src/main/java/seedu/address/model/Model.java)

<img src="images/ModelClassDiagram.png" width="450" />

The `Model` component,

* stores the address book data i.e., all `Person` objects (which are contained in a `UniquePersonList` object).
* derives one displayed list for full, search or pending mode and exposes a stable unmodifiable `ObservableList<Person>`.
* exposes read-only properties for the current date, pending-list mode and selected canonical client; these are session state and are not serialized.
* stores a `UserPrefs` object that represents the user’s preferences (currently, just the GUI settings). This is exposed to the outside as a `ReadOnlyUserPrefs` object.
* does not depend on any of the other three components (as the `Model` represents data entities of the domain, they should make sense on their own without depending on other components)

### Storage component

**API** : [`Storage.java`](https://github.com/AY2627S1-CS2103T-W13-3/tp/blob/master/src/main/java/seedu/address/storage/Storage.java)

<img src="images/StorageClassDiagram.png" width="550" />

The `Storage` component,

* can save both address book data and user preference data in JSON format, and read them back into corresponding objects.
* is implemented by `StorageManager`, which delegates the actual JSON file access to `JsonAddressBookStorage` and `JsonUserPrefsStorage` (one class per data file).
* depends on some classes in the `Model` component (because the `Storage` component's job is to save/retrieve objects that belong to the `Model`)

### Common classes

Classes used by multiple components are in the `seedu.address.commons` package.

--------------------------------------------------------------------------------------------------------------------

## **Implementation**

This section describes some noteworthy details on how certain features are implemented.

### Client-record validation and duplicate rejection

Contact values remain immutable `Name`, `Phone`, `Email` and `Address` objects inside `Person`. Name and address constructors trim outer U+0020 spaces, retaining internal spaces and case. Shared parsers trim only U+0020 for all contact fields. Names, phones and tags use ASCII validation; addresses accept Unicode single-line text but reject C0/C1 controls and U+2028/U+2029. Email keeps the existing grammar, including the adjacent alphanumeric pair in the final domain label, and preserves stored case.

`Person.isDuplicateOf` implements normalized-name equality **AND** (exact-phone equality **OR** case-insensitive-email equality). Names are normalized only for this comparison: lowercase with `Locale.ROOT` and collapse ordinary-space runs. Stored names remain unchanged. Address, tags and follow-up do not participate.

The relation is symmetric but **not transitive**. For a common name, record A `(111, one@example.com)` duplicates B `(111, two@example.com)`, and B duplicates C `(222, two@example.com)`, while A and C are allowed together. Consequently:

* `UniquePersonList.contains` checks for a duplicate against every existing record.
* `setPersons` checks every pair before replacing the list, so ordering cannot hide a conflict.
* `setPerson` resolves the target by full value equality, excludes only that full-list slot and checks the replacement against every other record before updating. Matching the old target does not bypass those checks.
* Follow-up-only replacements retain the contact fields and remain valid. Two same-name clients can coexist and be replaced or deleted independently.

`AddressBook` delegates to this collection path. Model mutations, command candidates and storage loading reuse the same rejection rule. No persistent client ID is introduced; selection tracks canonical immutable record instances.

**Alternatives considered:** name-only rejection prevents legitimate same-name clients. Redefining `Person.equals`/`hashCode` around duplicate rejection would violate equality's transitivity. Both are unsuitable. `Person.isSamePerson` retains exact stored-name matching for legacy callers; it is neither the duplicate predicate nor a stable record-identity key. `equals`/`hashCode` still include all stored fields and follow-up.

`Tag` normalizes validated ASCII values with `Locale.ROOT`, so sets deduplicate case variants. `Tag.formatTags(Set<Tag>)` is the shared display API: sorted bracketed lowercase values separated by one space, or `None`. `Messages.format(Person)` uses it and emits contact details only; add/delete feedback does not append follow-up information.

### Client command parsing and publication

`AddCommandParser` retains the existing tokenizer. It checks required prefixes and preamble before repeated required prefixes, then validates name, phone, email, address and tags in that order. Unrecognized prefixes remain part of the current field value. The model's whole-address-book duplicate check follows parsing, even when the client is hidden by a filter. Successful addition resets the view to the full insertion-order list.

`DeleteCommandParser` checks for exactly one ordinary-space-separated token before calling `ParserUtil.parseIndex`. The shared helper accepts ASCII digits, trims outer U+0020, removes leading zeroes before bounded conversion and rejects values outside `1..2147483647` without leaking `NumberFormatException`. `FollowUpParserUtil` and `ViewCommandParser` use the same helper. Delete resolves an index against `Model.getFilteredPersonList`, retaining the active mode and deleting the full record with its action.

`LogicManager` executes against a candidate, saves dirty data, then publishes the candidate. Parsing, duplicate, index and save failures preserve live data, list mode/order, selection and date; successful read-only commands do not save.

Tests cover domain partitions, exact messages, validation precedence, normalized duplicates, the non-transitive conflict example, safe replacements, tag normalization, index boundaries and many leading zeroes. `ClientRecordCommandsTest` exercises the real dispatcher/model/Logic and temporary-file storage, including same-name round trips, pending/filter indices, follow-up targeting and injected save failures. Parser tests cover syntax separately from command displayed-range checks.

### Saved client loading

`JsonAdaptedPerson` validates saved contact fields using the domain types after removing outer ordinary spaces, matching command input. `JsonAdaptedTag` validates tags before `Tag` converts them to lowercase; collecting them into a set removes case variants. Missing tags become an empty set. Optional follow-ups, including overdue dates, use the existing follow-up adapter.

`JsonSerializableAddressBook` converts every record before installing the list through `AddressBook.setPersons`. That central collection operation checks every pair with `Person.isDuplicateOf`, including the non-transitive case. Missing entries, invalid values and duplicates become loading failures. `JsonAddressBookStorage` translates them to `DataLoadingException`; `MainApp` then starts with an empty, mutation-blocked model while leaving the saved file unchanged. A repaired file loads on the next startup.

A saved record uses the following JSON shape. Tags are strings, not objects; omit `followUp` (or use `null`) when no action is pending. Do not add session selection or a computed status to the file.

```json
{
  "persons": [{
    "name": "Rachel Lim",
    "phone": "91234567",
    "email": "rachel@example.com",
    "address": "Blk 123",
    "tags": ["active", "health"],
    "followUp": {
      "dueDate": "2099-12-15",
      "description": "Discuss renewal options"
    }
  }]
}
```

### View parsing, name search and supported command routing

`ViewCommand`, `ViewCommandParser`, `FindCommand` and `AddressBookParser` use the shared index helper and model selection API. `Logic.selectedPersonProperty()` delegates to the live model, allowing the workspace and profile to observe the same selection.

#### View execution and error precedence

`ViewCommandParser` trims outer ordinary spaces (U+0020) and requires exactly one token. Missing or additional tokens produce `Invalid command format!` followed by a newline and `Usage: view INDEX`, before any index syntax check. It then calls `ParserUtil.parseIndex`, accepting ASCII digits with arbitrarily many leading zeroes and a numeric value in `1..2147483647`. Invalid syntax propagates the shared message unchanged: `Index must be a positive integer from 1 to 2147483647.`. It does not duplicate shared validation or wrap every parsing error as a usage error.

At execution, `ViewCommand` obtains `Model.getFilteredPersonList()` and checks the target against its current size. An index beyond the displayed list produces `The client index provided is invalid.`. Otherwise it passes the exact displayed `Person` instance to `Model.selectPerson` and returns an ordinary `CommandResult` containing `Showing client: NAME`. The name is feedback, not a selection identity key. No command-result field, callback or UI event is added.

The current list is authoritative in full, search and pending modes. Selection does not change data, order, mode or the dirty flag. Through the existing `LogicManager` execution path, the command selects on an isolated candidate; successful publication transfers selection and the sampled date without saving client data. A failed parse or range check publishes nothing. The command never samples a date itself.

#### Name-search behavior

`FindCommandParser` reports the exact usage `Usage: find KEYWORD [MORE_KEYWORDS]...` for empty arguments. After global validation, it trims only outer U+0020 and splits keywords on runs of ordinary spaces. Quotes and punctuation remain literal tokens. `NameContainsKeywordsPredicate` retains whole-word, case-insensitive OR matching against names; it adds no substring, tag or contact search.

`FindCommand` applies that predicate to the full dataset with `updateFilteredPersonList`, leaving pending mode and restoring insertion-order results. Each client appears once regardless of repeated keywords. It uses the existing shared count message, `%1$d client(s) listed!`. The model retains selection only if its exact record remains visible; search does not explicitly select any record or mark data dirty.

#### Dispatcher and loading protection

`AddressBookParser` rejects C0/C1 controls and U+2028/U+2029 on raw input, trims ordinary spaces, and rejects an empty line with `Enter a command.`. Command words are case-sensitive. Its supported routes are `add`, `delete`, `view`, `find`, `list`, `followup` and `followups`.

The mutation gate contains only `add`, `delete` and singular `followup`. In a failed-load session these routes report the existing changes-blocked diagnostic before command-specific parsing. Queries remain available, including normal view argument validation and the range error against an empty displayed list. Typed `edit`, standalone `clear`, `help` and `exit` reach the unknown-command result, `Unknown command.`, in both normal and blocked sessions. Global raw-character errors still have priority. `followup INDEX clear` remains a supported mutation.

Legacy command classes, their direct unit tests, the GUI Help menu and the window close handler remain available for their separate uses; removal applies to typed dispatch. Compatibility tests assert parse rejection for unsupported routes and retain failed-save/file-preservation checks for supported mutations.

#### Automated evidence

`ViewCommandParserTest` covers shape-before-syntax precedence, ASCII index boundaries and long leading-zero runs. `ViewCommandTest` uses date `2026-10-12` and two permitted same-name clients to verify exact displayed-record selection in full/search/pending modes, unchanged list/data/mode, failure preservation, and selection/date publication through Logic without a storage write.

`FindCommandParserTest`, `FindCommandTest` and `AddressBookParserTest` cover literal usage/result text, ordinary-space splitting, whole-word OR matching, insertion order, repeated keywords, all-data search after filtering/pending mode, selection reconciliation and global validation. Dispatcher tests reject all four unsupported routes in normal and blocked sessions and retain singular follow-up clearing. `LogicManagerTest` and `FollowUpWorkflowIntegrationTest` provide compatibility evidence for blocked-session rejection and preservation of state and saved bytes.

Command tests alone do not certify rendered profile behavior. The workspace and profile sections below describe the integrated UI and its separate checks.

### Client-profile selection model

`Model` and `ModelManager` own session selection. The integrated `view` command, Logic bridge and profile pane use this state instead of JavaFX row highlighting.

`Model.selectedPersonProperty()` returns one stable `ReadOnlyObjectProperty<Person>`. Its nullable value is the selected canonical record, with `null` meaning no selection. `Model.selectPerson(Person)` accepts a non-null object that is actually present in the current displayed list. An equal-valued copy is not a canonical member. Selection changes view state without marking client data dirty; the property does not expose a writable UI setter.

Selection follows the exact record, never its name, duplicate relation or displayed index. Immutable `Person` instances can be shared between model copies. When `setPerson(target, replacement)` replaces the selected record, the model tracks the replacement and retains it as the selection only if it remains visible. Filtering out or deleting the selected record clears selection. Clearing its follow-up retains selection in full/search mode but clears it in pending mode, where the record is no longer visible. Returning to the full list does not recreate a cleared selection. Addition reveals the full list without selecting the new record. Constructors start with no selection, including when data loading is blocked; selection is not stored in JSON or preferences.

#### Candidate copying and selection publication

`forkForCommand(today)` copies selection along with data and view state into a clean candidate. The candidate has its own observable list and properties, so selecting, replacing, filtering or deleting a record there does not change the live model. Discarding the candidate preserves live selection and the published date. Selection-only operations remain clean; an accepted data replacement remains dirty even when its stored values are unchanged.

After the caller has successfully saved any dirty candidate, `commitFrom(candidate)` publishes its data, mode, date and selection while retaining the live observable objects. It maps the selected candidate record's full-list position to the corresponding committed live record. This position is used only within that publication; it is not a persistent ID or a displayed row number.

This mapping matters for an identical follow-up replacement. The candidate may contain a new `Person` object with equal values, while publication skips resetting the value-equal live address book. Publishing the candidate object directly would leave selection pointing outside the live list. Mapping to the committed live record retains a canonical visible selection whether the data was reset or retained.

#### Selection transition evidence

`ClientProfileSelectionTest` covers model-level selection transitions using a fixed date of `2026-10-12`: same-name records, pending reordering, filter exclusion, follow-up clearing, deletion, candidate isolation and equal-value replacement. Canonical-object assertions distinguish correct record tracking from value equality. The existing model and pending-list suites provide regression coverage for the authoritative displayed list and stable due-date ordering.

These tests establish the model boundary. `ClientProfileWorkflowIntegrationTest` adds real-command checks for pending reordering, filter exclusion, full-list and pending-list clearing, failed-save preservation, restart, and a 50-client command sequence with rejected input and saved-byte checks. The manual testing appendix covers visual profile updates and window resizing.

### Client workspace and pending-list rendering

`MainWindow` places `PersonListPanel` and `ClientProfilePanel` in the two placeholders of a horizontal `SplitPane`. It passes the filtered list, `todayProperty()`, `showingFollowUpsProperty()` and `selectedPersonProperty()` to the list, and the selected-person and date properties to the profile. The UI only reads these values and never samples the clock.

`PersonListPanel` keeps one panel-level listener on the date, mode and selection properties. A change refreshes the visible cells and the empty-list text. Each cell rebuilds its `PersonCard` from `updateItem` and `updateIndex`. A reused cell that moves therefore gets the right number, and an empty cell clears its graphic, so no stale badge or highlight remains.

`PersonCard(person, index, today, showingFollowUps)` renders an ordinary row (index, name, phone and email) or a pending row, which adds the due date, action and textual status badge. The three-argument constructor renders an ordinary row. `createFollowUpDisplay` is a pure function from person, date and mode to the row text and style, so the row content is unit-tested without starting JavaFX.

The viewed row comes from model selection only. `PersonListPanel.isViewed` compares object identity, so a same-name or value-equal record is never highlighted by mistake. Selection in the JavaFX `ListView` is styled like an ordinary row and never writes back to the model.

A failed load shows `Messages.MESSAGE_LOAD_WARNING` in a separate `loadWarning` label that is visible for the whole session. Command results replace only the result display. `MainWindow` logs only whether a command succeeded or failed, never the feedback text, because feedback contains contact and action details.

### Client profile panel

`ClientProfilePanel` receives Logic's read-only selected-client and published-date properties. It listens to both and renders the immutable `Person` without selecting, saving or sampling the device clock. A selected client displays name and labeled contact fields; `Tag.formatTags` supplies the sorted lowercase tags. The follow-up status comes from `FollowUp.getStatus(today)`, so a published date change updates the status even when the selected record does not change.

The panel's FXML contains a scrollable, read-only layout with wrapping address and action labels. Its scoped CSS is loaded by that FXML; shared workspace styles remain in `DarkTheme.css`. On deselection the panel hides all client fields, clears their text and shows `No client selected.`. When a selected client's action clears, it hides the date, action and status and shows `Follow-up: None`.

`ClientProfilePanelTest` checks exact profile text, sorted tags, the no-selection and no-action states, and overdue/due-today/upcoming values from a fixed date. `ClientProfileWorkflowIntegrationTest` checks the selection and saved-data boundaries through real commands and storage. These tests do not prove visual wrapping or window composition; inspect the rendered pane at a narrow window width with long address and action values.

### Follow-up state and date handling

`Person` contains an optional immutable `FollowUp` with a `LocalDate` due date and an action description. Recording or replacing an action creates a new `Person` that preserves every contact field and tag; clearing removes the optional action. There is no action history or completion flag.

`FollowUpCommandParser` recognizes either `followup INDEX clear` or one date and one description prefix. Commands resolve the index against the current displayed list before validating a new date/action. New actions cannot be dated before the candidate's sampled date. Stored overdue actions remain valid: JSON conversion checks calendar/description validity without imposing a today-based restriction.

`showPendingFollowUps()` includes every client with an action and sorts by due date. Stable sorting preserves full-list insertion order on equal dates. Chronological order naturally groups overdue, due-today and upcoming actions. `FollowUpStatus` derives those labels from the due date and published date; neither status nor the selected client is stored in JSON.

`LogicManager` samples its injected clock once per command candidate. The date is published only when that command succeeds. Separately, `MainApp` refreshes the live date on startup, window focus and a one-minute timer, so statuses update across midnight without a command. A failed command does not itself advance the date; independent focus/timer refreshes can still occur. `MainAppTest` and `LogicManagerTest` cover these two date paths.

### Save-before-publish boundary

`LogicManager.execute` follows this sequence:

1. Parse against the startup loading gate.
2. Fork an isolated candidate with the sampled date.
3. Execute the command against the candidate.
4. Save the candidate only when its data is dirty.
5. Publish its data, preferences, mode, date and selection to the live model.

A save failure becomes `CommandException` with `MESSAGE_SAVE_FAILED`; no candidate state is published. `JsonAddressBookStorage` writes a sibling temporary file, then replaces the destination with an atomic move. On failure it attempts to remove the temporary file and preserves the existing destination. Unsupported atomic replacement is reported as a save failure, rather than falling back to a partial write. See `JsonAddressBookStorageTest` for write/move failure coverage.

--------------------------------------------------------------------------------------------------------------------

## **Documentation, logging, testing, dev-ops**

* [Documentation guide](Documentation.md)
* [Testing guide](Testing.md)
* [Logging guide](Logging.md)
* [DevOps guide](DevOps.md)

--------------------------------------------------------------------------------------------------------------------

## **Appendix: Requirements**

### Product scope

**Target user profile**:

* is an individual insurance agent who manages their own client and prospect records
* personally keeps track of client contact details and the next action they need to take for each client
* uses a personal computer to manage client details and follow-ups
* needs to review which client follow-ups are due, upcoming, or overdue
* types quickly and prefers typed commands to mouse interactions

**Value proposition**: PolicyHarbor helps individual insurance agents keep client contact details and one pending next action per client organised in a keyboard driven desktop app, making follow-ups easier to review and manage.

### User stories

These stories are delivered in v1.3. The MVP deliberately limits each client to one pending action.

| Priority | As an insurance agent, I want to... | So that I can... |
| --- | --- | --- |
| High | add a client or prospect with contact details | keep their information in one place |
| High | list clients and search by name | find the record I need |
| High | view a client's complete profile | review context before making contact |
| High | delete an incorrect client record | remove inaccurate information |
| High | reload saved data after restarting | retain my work between sessions |
| High | record or replace a dated next action | remember an agreed commitment |
| High | review overdue, due-today and upcoming actions | prioritize follow-ups |
| High | clear a completed action | remove work that no longer needs attention |
| High | receive useful errors without losing data | correct mistakes safely |
| Medium | categorize clients with tags | keep relevant context in the profile |

Backlog ideas, outside v1.3: multiple actions/history, notifications, client archiving, import and richer filtering. They are not advertised as available commands.

### Use cases

The actor is an individual insurance agent and the system is PolicyHarbor. Each command is entered in one line; the app does not prompt for individual fields or confirmation.

**UC01 — Retrieve a client profile**

1. The agent lists all clients or searches their names.
2. The system displays matching clients with current row numbers.
3. The agent enters `view INDEX`.
4. The system opens that client's complete profile without changing saved data.

Extension: invalid syntax or a missing displayed index produces an error and preserves the current profile.

**UC02 — Add a client**

1. The agent submits `add` with contact details and optional tags.
2. The system validates the fields and checks the full dataset for duplicates.
3. The system saves the new record, then displays the full list and success feedback.

Extensions: invalid/duplicate data produces an error without changes. A save failure preserves the previous data and view. The agent can correct and resubmit the retained command text.

**UC03 — Delete a client**

1. The agent identifies the client in the displayed list.
2. The agent enters `delete INDEX`; opening a profile first is optional.
3. The system saves the removal of the client and their follow-up, then refreshes the current list. If that client was viewed, the profile closes.

Extensions: an invalid index or save failure leaves the original state intact.

**UC04 — Record or replace a follow-up**

1. The agent enters `followup INDEX d/DATE m/DESCRIPTION` for a displayed client.
2. The system validates the index, date and description.
3. The system saves the action and updates the list/profile, reordering pending rows when necessary.

Extensions: invalid input or saving failure preserves the previous action and view. Only one action is stored, so an accepted replacement overwrites the earlier action.

**UC05 — Clear a follow-up**

1. The agent enters `followup INDEX clear` for a displayed client.
2. The system saves removal of the pending action.
3. In pending mode the client leaves the list and their open profile closes; in full/search mode the client remains and their profile shows no action.

Extensions: an absent action, invalid index or save failure produces an error and preserves the original state.

### Non-Functional Requirements

1. Should work on Windows, Linux and macOS with Java `25`; macOS uses the prescribed Zulu JDK+FX distribution.

2. Target: hold 1000 clients without noticeable sluggishness in typical usage. The automated stress scenario uses 50 clients; it is not a benchmark proving this target.

3. A user with above-average typing speed for regular English text (i.e. not code or system administration commands) should be able to accomplish most tasks faster using commands than using the mouse.

4. Should respond to typical user commands such as adding, deleting, searching, viewing client records and updating follow-ups within 1 second under normal operating conditions.

5. Should preserve all successfully saved client information between application sessions, such that closing and reopening the application does not cause data loss.

6. Invalid commands or invalid user input should not modify existing client data and should result in a clear error message explaining how the input can be corrected.

7. Should be usable without an Internet connection for all core client-management functions.

8. Client data should be stored locally on the user's device and should not require a remote server for normal operation.

9. Should be distributable as a single executable JAR file without requiring a separate installation process.

10. The user interface should remain usable at common laptop screen resolutions, including `1280 × 720` at 150% scaling, and work well at `1920 × 1080` at 100%/125% scaling. These are manual verification targets.

11. Commonly used commands and command formats should remain consistent throughout the application to reduce the amount of relearning required from the user.

12. The application should be designed primarily for keyboard-based interaction, while still allowing mouse interaction where appropriate.

### Glossary

* **Client record**: The contact details stored for a client or prospective client
* **Follow up**: The next action an agent plans to take for a client, with a due date and description
* **Pending**: A follow-up that has been recorded and not yet cleared. It stays pending after its due date passes
* **Index**: The row number of a client in the list currently displayed.
* **Overdue**: A pending follow-up whose due date is before today
* **Due today**: A pending follow-up whose due date is today
* **Upcoming**: A pending follow-up whose due date is after today

--------------------------------------------------------------------------------------------------------------------

## **Appendix: Instructions for manual testing**

Test the packaged JAR in a fresh writable folder with Java 25 (the prescribed Zulu JDK+FX on Mac). Use synthetic clients and repeat each independent scenario from the specified starting state. These are procedures, not a claim that every platform/scaling combination has been tested.

### Startup, persistence and basic workflow

1. Launch `java -jar addressbook.jar` from the JAR's folder. Expect an empty list, `No client selected.` and no loading warning.
2. Enter these commands separately:

   ```text
   add n/Rachel Lim p/001 e/rachel@example.com a/Blk 123 t/HEALTH t/health t/active
   add n/Amanda Lee p/002 e/amanda@example.com a/Blk 456
   view 1
   ```

   Expect Rachel's contact fields, `Tags: [active] [health]` and `Follow-up: None` in the profile. Only Rachel's row is marked `VIEWING`.
3. Record Rachel's action for today + 2 days and Amanda's for today + 1 day using `followup INDEX d/DATE m/ACTION`. Use actual `YYYY-MM-DD` dates.
4. Enter `followups`, then `view 1`. Expect Amanda before Rachel, the correct profile, and continuous numbering. Replace Amanda's action with today's date to verify the `DUE TODAY` badge.
5. Close/relaunch from the same folder. Expect all contacts/actions in full insertion order and no profile selection. Resize/move the window, close/relaunch and verify window preferences.

### Profile transitions and displayed indices

From the two-client dataset above:

| Action | Expected result |
| --- | --- |
| `followups`, `view 1`, then replace row 1's action with a date later than Rachel's | Amanda moves after Rachel; her profile and `VIEWING` marker follow the record. |
| `followup 2 clear` after that reorder | Amanda leaves pending mode and her profile closes. |
| `list`, `view 2` | Amanda can be viewed again and shows `Follow-up: None`. |
| `find Rachel` | Only Rachel remains; Amanda's profile closes. |
| `list` | Both clients return; no profile reopens automatically. |
| `view 1`, then `delete 2` | Rachel remains viewed; deleting her neighbor does not change selection. |
| `delete 1` | Rachel and her action disappear and the profile clears. |

For same-name targeting, start fresh and add two Rachel Lim clients with different phone/email values. View/delete each displayed row and verify that only its record is affected.

### Search and validation

Start fresh with Rachel and Amanda from the basic workflow.

| Input | Expected result |
| --- | --- |
| `find lim AMANDA Lim` | `2 client(s) listed!`, Rachel then Amanda, each once. |
| `find Rach`, `find "Rachel"`, `find Rachel,`, `find rachel@example.com` | Each returns `0 client(s) listed!`; only whole name words match. |
| `find` | `Invalid command format!` then `Usage: find KEYWORD [MORE_KEYWORDS]...`. |
| `view 0 extra`, `delete 0 extra` | Format error and the corresponding usage, before index syntax. |
| `view 0`, `view 2147483648`, `delete 0` | `Index must be a positive integer from 1 to 2147483647.` |
| `view 2147483647` | `The client index provided is invalid.` |
| `edit 1 n/Changed`, `clear`, `help`, `exit` | `Unknown command.`; no client data changes and the window stays open. |
| `add n/RACHEL  LIM p/001 e/new@example.com a/Blk 9` | Duplicate error despite name case/spacing differences. |
| `add n/Rachel Lim p/003 e/RACHEL@EXAMPLE.COM a/Blk 9` | Duplicate error because email matching ignores case. |
| `add n/Rachel Lim p/003 e/other@example.com a/Blk 9` | Allowed: both phone and email differ. |
| `add a/ t/! e/bad p/12 n/Bad&` | Name constraint first; no record is added. |

After each rejected command, verify that the input stays editable/focused and the client data, list and selected profile are unchanged. On success the command box clears. Date preservation is checked per command; the independent focus/minute refresh can update displayed statuses.

### Loading protection and saved-data compatibility

Close the app before editing its JSON; back up the file first.

1. Make one saved follow-up date earlier than today, retaining a valid ISO date and action. Restart and enter `followups`. Expect that action to load as `OVERDUE`.
2. Remove a record's optional `followUp` or `tags` field and restart. Expect no action/no tags for that client; legacy contact-only records load.
3. In a separate folder, corrupt the JSON or put an invalid/duplicate client in it. Restart. Expect an empty list and a persistent saved-data warning.
4. Enter `list`, `find Rachel` and `view 1`. Queries remain available; a valid view index gives the displayed-range error. The warning stays visible.
5. Try `add`, `delete` or `followup`. Expect the changes-blocked diagnostic before command-specific parsing. Confirm the invalid file's bytes are unchanged.
6. Close the app, restore the valid backup and relaunch. Expect normal writable operation.

### Window, help and long content

- Test full/search/pending modes, scrolling and empty lists. Ordinary rows show no action fields; reused/empty rows show no stale badge or `VIEWING` marker.
- Click/highlight another row while a profile is open. The viewed client must remain unchanged until a successful `view` command.
- Use a long address and an action up to 200 characters. Narrow the split panes and verify wrapping/scrolling and access to the command box and feedback.
- Check at 1920×1080 with 100%/125% scaling and at 1280×720 with 150% scaling on available platforms. Record any untested combinations.
- Open Help from the menu or F1. Its link/copy button must point to PolicyHarbor's User Guide, rather than AB3's guide.

### Save failures and automated boundaries

`JsonAddressBookStorageTest` injects write/move failures. `ClientProfileWorkflowIntegrationTest` checks failed add/delete/action replacement/clearing without changing saved bytes, selection, list mode/order or date. `ClientRecordCommandsTest` covers additional duplicate and index partitions; `ClientProfileSelectionTest` covers canonical identity and equal-value publication.

For an optional manual failure check, keep the app open and make only its test `data` directory unwritable. Attempt a mutation. Expect `Changes could not be saved. No changes were kept. Try the command again.` with the previous data/list/profile retained. Restore directory permissions before continuing. Use disposable data; automated fault injection is the reproducible evidence where platform permissions differ.
