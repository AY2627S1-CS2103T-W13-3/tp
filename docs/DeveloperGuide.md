---
layout: page
title: Developer Guide
---
* Table of Contents
{:toc}

--------------------------------------------------------------------------------------------------------------------

PolicyHarbor helps individual insurance agents manage contact records and one pending next action per client. This guide is for developers: start with component responsibilities, then follow the identity, command-publication and persistence invariants before changing the product. Exact command syntax is in the [User Guide](UserGuide.html).

**Version baseline:** this guide describes the verified v1.5 release-candidate implementation at [`01ef0f17`](https://github.com/AY2627S1-CS2103T-W13-3/tp/tree/01ef0f17bf9631bbe50c95e2f47b5592244f3574). At this audit the latest published JAR is v1.4, and the candidate still displays that title/version label. This is not documentation of that older binary. Before publishing v1.5, align the final tag, uploaded JAR, guides and testing record; repeat packaged checks if the implementation changes.

## **Acknowledgements**

* [AddressBook Level 3 (AB3)](https://github.com/se-edu/addressbook-level3) is the starting codebase. PolicyHarbor retains `seedu.address`, the Java/JavaFX/FXML component-manager structure, common utilities and test/build conventions. Client identity, follow-ups, transactions, validation and dashboard behavior substantially adapt that foundation.
* The [AB3 Developer Guide](https://se-education.org/addressbook-level3/DeveloperGuide.html) and [User Guide](https://se-education.org/addressbook-level3/UserGuide.html) supplied documentation structure and retained architecture explanations. The architecture, component, class/parser and deletion diagrams under `docs/diagrams` derive from AB3 diagrams and have been adapted to PolicyHarbor's implemented relationships and execution flow. The project website retains the AB3 Jekyll/Minima foundation.
* The homepage bundles a subset of [Newsreader, by the Newsreader Project Authors](https://github.com/productiontype/Newsreader), for headings in `docs/assets/landing/Newsreader.woff2`. Its SIL Open Font License 1.1 notice is retained in [`OFL.txt`](assets/landing/OFL.txt). The font is a reused asset, separate from the application implementation.
* All team members used ChatGPT for general assistance during the project.
* Somaaditya Samal used OpenAI Codex to audit his RepoSense-attributed code, refactor migration and panel lifecycle code, and improve related tests for issue #241.

--------------------------------------------------------------------------------------------------------------------

## **Setting up, getting started**

Refer to [_Setting up and getting started_](SettingUp.html) for the prescribed JDK/JavaFX and IDE setup. Keep synthetic test folders separate from real client data. Production entry points are `Main`/`MainApp`; test fixtures live in test sources and are not packaged commands.

--------------------------------------------------------------------------------------------------------------------

## **Design**

<div markdown="span" class="alert alert-primary">

:bulb: **Tip:** The `.puml` files used to create diagrams are in `docs/diagrams`. Refer to the [_PlantUML Tutorial_ at se-edu/guides](https://se-education.org/guides/tutorials/plantUml.html) to learn how to create and edit diagrams.
</div>

Class diagrams use solid associations, dashed dependencies, hollow triangles for generalization/realization and filled diamonds for composition. `<<interface>>` labels distinguish interfaces; italic class names identify abstract classes. Sequence diagrams use solid call arrows and dashed returns, with `alt` frames for alternative outcomes. On a narrow screen, scroll a figure horizontally or open its full-size image.

### Architecture

<figure class="uml-diagram" tabindex="0" aria-label="High-level component dependencies">
  <img src="images/ArchitectureDiagram.png" alt="High-level component dependencies" />
  <figcaption>High-level component dependencies. <a href="images/ArchitectureDiagram.png">Open full-size image</a>.</figcaption>
</figure>

PolicyHarbor is an offline Java/JavaFX client and next-action manager. Components separate presentation, command orchestration, domain state and local persistence; the diagram shows selected dependencies, not class inheritance.

**Main components of the architecture**

**`Main`** (consisting of classes [`Main`](https://github.com/AY2627S1-CS2103T-W13-3/tp/blob/master/src/main/java/seedu/address/Main.java) and [`MainApp`](https://github.com/AY2627S1-CS2103T-W13-3/tp/blob/master/src/main/java/seedu/address/MainApp.java)) is in charge of the app launch and shut down.

* At launch, `MainApp` acquires working-folder ownership, loads/recovers preferences, constructs Storage, loads Model, then connects Logic and UI. Invalid client loading creates a protected model; it does not silently start a writable replacement dataset.
* At startup/focus and each minute, it refreshes the shared date used for action statuses. On shutdown it stops the timer, saves window preferences and releases instance ownership; UI closure disposes its observers and Help window.

The bulk of the app's work is done by the following four components:

* [**`UI`**](#ui-component): The UI of the App.
* [**`Logic`**](#logic-component): The command executor.
* [**`Model`**](#model-component): Holds the data of the App in memory.
* [**`Storage`**](#storage-component): Reads data from, and writes data to, the hard disk.

[**`Commons`**](#common-classes) represents a collection of classes used by multiple other components.

**How the architecture components interact with each other**

Commands cross the UI → Logic → candidate Model → Storage boundary. The [transaction sequence](#save-before-publish-boundary) shows when changes become live; component/class diagrams below describe responsibilities without repeating that flow.

Each of the four main components (also shown in the diagram above),

* defines its _API_ in an `interface` with the same name as the Component.
* provides its functionality through a concrete `{Component Name}Manager` class that implements the corresponding API interface.

For example, the `Logic` component defines its API in `Logic.java` and implements it in `LogicManager.java`. Other components interact with a component through its interface rather than its concrete class, preventing them from coupling to that component's implementation, as illustrated in the following partial class diagram.

<figure class="uml-diagram" tabindex="0" aria-label="Component interfaces and manager implementations">
  <img src="images/ComponentManagers.png" alt="Component interfaces and manager implementations" />
  <figcaption>Component interfaces and manager implementations. <a href="images/ComponentManagers.png">Open full-size image</a>.</figcaption>
</figure>

The sections below give more details of each component.

### UI component

The **API** of this component is specified in [`Ui.java`](https://github.com/AY2627S1-CS2103T-W13-3/tp/blob/master/src/main/java/seedu/address/ui/Ui.java)

<figure class="uml-diagram" tabindex="0" aria-label="UI composition and shared UiPart base class">
  <img src="images/UiClassDiagram.png" alt="UI composition and shared UiPart base class" />
  <figcaption>UI composition and shared UiPart base class. <a href="images/UiClassDiagram.png">Open full-size image</a>.</figcaption>
</figure>

The UI consists of a `MainWindow` and its parts, including `CommandBox`, `ResultDisplay`, `PersonListPanel`, `ClientProfilePanel`, `PendingFollowUpPanel` and `StatusBarFooter`. All of these, including `MainWindow`, inherit from the abstract `UiPart` class, which captures common behavior among classes that represent visible GUI parts.

The `UI` component uses the JavaFX UI framework. The layouts of these UI parts are defined in matching `.fxml` files in `src/main/resources/view`. For example, [`MainWindow.fxml`](https://github.com/AY2627S1-CS2103T-W13-3/tp/blob/master/src/main/resources/view/MainWindow.fxml) specifies the layout of [`MainWindow`](https://github.com/AY2627S1-CS2103T-W13-3/tp/blob/master/src/main/java/seedu/address/ui/MainWindow.java).

The `UI` component,

* executes user commands using the `Logic` component.
* listens for changes to `Model` data so that the UI can be updated with the modified data.
* keeps a reference to the `Logic` component, because the `UI` relies on the `Logic` to execute commands.
* depends on some classes in the `Model` component because it displays `Person` objects from the model.

### Logic component

**API** : [`Logic.java`](https://github.com/AY2627S1-CS2103T-W13-3/tp/blob/master/src/main/java/seedu/address/logic/Logic.java)

Here's a (partial) class diagram of the `Logic` component:

<figure class="uml-diagram" tabindex="0" aria-label="Logic, command and parser structure">
  <img src="images/LogicClassDiagram.png" alt="Logic, command and parser structure" />
  <figcaption>Logic, command and parser structure. <a href="images/LogicClassDiagram.png">Open full-size image</a>.</figcaption>
</figure>

`LogicManager` dispatches commands, runs them on isolated model candidates, saves dirty client data and publishes only on success. Read-only commands publish view/selection/date without saving. See the [save-before-publish boundary](#save-before-publish-boundary) for the execution sequence and failure contract. `XYZCommand` is a representative concrete command, not a source class.

Parser delegation is shown separately to keep the command diagram readable:

<figure class="uml-diagram" tabindex="0" aria-label="Selected parsers and shared parsing utilities">
  <img src="images/ParserClasses.png" alt="Selected parsers and shared parsing utilities" />
  <figcaption>Selected parsers and shared parsing utilities. <a href="images/ParserClasses.png">Open full-size image</a>.</figcaption>
</figure>

This partial diagram focuses on follow-up, view and search parsing. The shown parsers implement `Parser<T extends Command>` with their respective command result types. `ViewCommandParser` and `FollowUpCommandParser` share `ClientIdParserUtil.parseClientId()`. Add, edit and delete parsers and low-level tokenizer details are omitted to keep this view focused.

How the parsing works:

* When called upon to parse a user command, the `AddressBookParser` class creates an `XYZCommandParser` (`XYZ` is a placeholder for the specific command name, e.g., `AddCommandParser`). The parser uses the other classes shown above to parse the user command and create an `XYZCommand` object (e.g., `AddCommand`). The `AddressBookParser` returns that object as a `Command` object.
* All `XYZCommandParser` classes, such as `AddCommandParser` and `DeleteCommandParser`, implement the `Parser` interface so they can be treated similarly where appropriate, for example during testing.

### Model component

**API** : [`Model.java`](https://github.com/AY2627S1-CS2103T-W13-3/tp/blob/master/src/main/java/seedu/address/model/Model.java)

<figure class="uml-diagram" tabindex="0" aria-label="Model ownership, permanent IDs and the optional follow-up">
  <img src="images/ModelClassDiagram.png" alt="Model ownership, permanent IDs and the optional follow-up" />
  <figcaption>Model ownership, permanent IDs and the optional follow-up. <a href="images/ModelClassDiagram.png">Open full-size image</a>.</figcaption>
</figure>

The `Model` component,

* owns client records through `AddressBook`/`UniquePersonList`, with aggregate-owned permanent identity metadata independent of contact equality.
* derives one displayed list for full, search or pending mode and exposes a stable unmodifiable `ObservableList<Person>`.
* exposes read-only properties for the current date, pending-list mode and selected canonical client; these are session state and are not serialized.
* stores a `UserPrefs` object that represents the user’s preferences (currently, just the GUI settings). This is exposed to the outside as a `ReadOnlyUserPrefs` object.
* does not depend on any of the other three components (as the `Model` represents data entities of the domain, they should make sense on their own without depending on other components)

### Storage component

**API** : [`Storage.java`](https://github.com/AY2627S1-CS2103T-W13-3/tp/blob/master/src/main/java/seedu/address/storage/Storage.java)

<figure class="uml-diagram" tabindex="0" aria-label="JSON storage and follow-up adapters">
  <img src="images/StorageClassDiagram.png" alt="JSON storage and follow-up adapters" />
  <figcaption>JSON storage and follow-up adapters. <a href="images/StorageClassDiagram.png">Open full-size image</a>.</figcaption>
</figure>

The `Storage` component,

* can save both client data and user preference data in JSON format, and read them back into corresponding objects.
* is implemented by `StorageManager`, which delegates the actual JSON file access to `JsonAddressBookStorage` and `JsonUserPrefsStorage` (one class per data file).
* depends on some classes in the `Model` component (because the `Storage` component's job is to save/retrieve objects that belong to the `Model`)

### Common classes

The [`seedu.address.commons`](https://github.com/AY2627S1-CS2103T-W13-3/tp/tree/master/src/main/java/seedu/address/commons) package supplies shared exceptions, JSON/file/string utilities, GUI settings, logging and process locking. Domain rules stay in Model; common utilities do not own client state.

--------------------------------------------------------------------------------------------------------------------

## **Implementation**

### Permanent client identity

A table position changes after searching, deleting or sorting pending work. It cannot safely identify a client. [`ClientId`](https://github.com/AY2627S1-CS2103T-W13-3/tp/blob/master/src/main/java/seedu/address/model/person/ClientId.java) is instead a permanent positive integer, independent of contact-value equality. Commands resolve it against the **complete** address book through `ClientIdResolver.requireClient`, including clients hidden by a filter.

[`AddressBook`](https://github.com/AY2627S1-CS2103T-W13-3/tp/blob/master/src/main/java/seedu/address/model/AddressBook.java) owns both `UniquePersonList` and `ClientIdentityRegistry`. The registry associates canonical `Person` references with IDs and maintains a monotonic `nextClientId`. A canonical record is the actual immutable instance in the owning book; an equal-valued copy is not an identity key. Metadata is installed before list notifications, so observers can resolve newly published records.

| Aggregate operation | Identity consequence |
| --- | --- |
| `addPerson` | Allocates the next ID; the command obtains it from the aggregate for feedback. |
| `setPerson(target, replacement)` | Transfers the canonical target's ID to the immutable replacement, retaining insertion position. Use this for contact/follow-up changes. |
| `removePerson` | Removes the association without lowering the allocation counter. |
| `setPersons` | Allocation-oriented bulk replacement: retained references keep IDs; reconstructed equal-valued records receive new ones. It is not an edit or snapshot API. |
| `resetData` / `restorePersons` | Copies an identity-bearing snapshot / restores a fully validated saved snapshot, including its counter. |

IDs range from 1 to 2147483647. The `long` counter can reach the exhausted sentinel 2147483648; allocation then raises `ClientIdExhaustedException`, which addition translates into a command error. Deleting records, including every record, does not release old IDs. A failed command candidate consumes no **live** ID. Normal save/restart retains gaps and the counter; externally removing all identity history is the [legacy compatibility limitation](#permanent-identity-in-json).

### Save-before-publish boundary

The central invariant is that a command must not publish client-data changes that could not be saved. [`LogicManager.execute`](https://github.com/AY2627S1-CS2103T-W13-3/tp/blob/master/src/main/java/seedu/address/logic/LogicManager.java) owns this boundary:

```text
command = parse(rawInput, loadingGate)
candidate = liveModel.forkForCommand(one sampled LocalDate)
result = command.execute(candidate)
if candidate.hasUnsavedChanges():
    storage.saveAddressBook(candidate.getAddressBook())
liveModel.commitFrom(candidate)
return result
```

The candidate has its own aggregate/identity registry, observable list and session properties; immutable `Person` values may be shared. Parse or command failures publish nothing. A save failure becomes a `CommandException` with recovery guidance and discards the candidate, preserving live data, list mode/order, selected client and command date. The atomic storage writer preserves the destination on its reported write/move failure. Read-only commands publish view/date changes without saving client data.

<figure class="uml-diagram" tabindex="0" aria-label="Permanent-ID deletion and save-before-publish">
  <img src="images/DeleteSequenceDiagram.png" alt="Permanent-ID deletion and save-before-publish" />
  <figcaption>Successful ID resolution followed by the two storage outcomes for deletion. Parser internals are omitted. <a href="images/DeleteSequenceDiagram.png">Open full-size image</a>.</figcaption>
</figure>

Deletion removes the identified client and their optional action. It retains the active list mode and clears selection only when necessary. The diagram assumes parsing and ID resolution succeed; it does not imply that deletion is committed before saving. An [alternative component-level sequence](images/ArchitectureSequenceDiagram.png) abstracts the same boundary.

**Trade-off:** candidate isolation avoids trying to roll back every observable UI effect after an I/O failure. It copies state and revalidates collections rather than optimizing for very large datasets. Performance targets are [requirements to measure](#non-functional-requirements), not a claim that copying is free.

### Client-record validation and duplicate rejection

Contact values (`Name`, `Phone`, `Email`, `Address`) and `Person` are immutable. Parsers and JSON adapters reconstruct the same domain values, so saved data cannot bypass validation. Unicode names, addresses and follow-up notes must contain valid, persistable text; unpaired UTF-16 surrogates are rejected even when produced by syntactically valid JSON escapes. Email validation and ordinary-space trimming use iterative scans, avoiding recursive-regex/large-input failure modes. The [UG's input reference](UserGuide.html) gives exact user-facing constraints.

[`Person.isDuplicateOf`](https://github.com/AY2627S1-CS2103T-W13-3/tp/blob/master/src/main/java/seedu/address/model/person/Person.java) applies **normalized-name equality AND (same phone digits OR case-insensitive email equality)**. Name comparison uses NFC composition, collapsed ordinary-space runs and `equalsIgnoreCase`; stored spelling is retained. Phone formatting is retained for display but ignored for this duplicate comparison. Address, tags and follow-up are excluded.

This relation is symmetric but **not transitive**: for one name, A `(111, one@example.com)` conflicts with B `(111, two@example.com)`, and B with C `(222, two@example.com)`, although A and C may coexist. `UniquePersonList` therefore checks every relevant pair, excluding only the replaced slot during an edit. It cannot safely use this relation as `equals`/`hashCode`. Full value equality still includes all stored fields and the action; `isSamePerson`'s legacy stored-name comparison is not permanent identity. Name-only rejection was also unsuitable because real clients can share names.

`Tag` lowercases validated ASCII values with `Locale.ROOT`; sets remove case variants. `Tag.formatTags` supplies consistent sorted output to contact feedback, while the profile renders sorted chips. Duplicate checking covers the whole book, irrespective of the displayed filter.

<span id="client-command-parsing-and-publication"></span>

<span id="view-execution-and-error-precedence"></span>

<span id="dispatcher-and-loading-protection"></span>

### View parsing, client search and supported command routing

[`AddressBookParser`](https://github.com/AY2627S1-CS2103T-W13-3/tp/blob/master/src/main/java/seedu/address/logic/parser/AddressBookParser.java) first rejects raw control characters and line/paragraph separators, then dispatches case-sensitive commands. Only `add`, `delete`, `edit`, `view`, `find`, `list`, `followup` and `followups` are typed routes. Legacy command classes are not proof that `clear`, `help` or `exit` is accepted at the command box; Help/window closure are GUI actions.

Global character validation precedes the failed-load mutation gate. In a protected session, `add`, `delete`, `edit` and singular `followup` are rejected before their command-specific parsers; queries remain available. Otherwise each parser checks its own shape/prefixes and values. `list`/`followups` reject arguments. Shared `ClientIdParserUtil` validates a bounded ASCII numeric ID (leading zeroes allowed); `ClientIdResolver` distinguishes a valid-but-absent ID during execution. A malformed command never changes the model merely because its prefix was recognized.

#### Bare and explicit client search

`FindCommandParser` constructs a predicate over all clients, replacing any earlier full/search/pending view. Bare keywords use whole-name-word **OR** exact-full-phone matching; explicit `n/`, `p/` or `t/` accepts exactly one field. `ClientSearchPredicate` implements normalized case-insensitive name substrings, phone-digit substrings ignoring display separators, and exact case-insensitive tags. Bare phone keywords retain leading zeroes and are not concatenated. Prefix mixing, quoted text and ordinary-space tokenization follow the shared grammar, rather than a general query language.

`FindCommand` accepts `Predicate<Person>`, keeping search policy outside the command. Search preserves insertion order, emits each client once and retains a selected profile only while its canonical record is visible. `ViewCommand` resolves IDs globally: viewing a hidden client first restores the full list; viewing an already visible client retains the current mode. Neither operation saves client data.

### Contact editing

[`EditCommandParser`](https://github.com/AY2627S1-CS2103T-W13-3/tp/blob/master/src/main/java/seedu/address/logic/parser/EditCommandParser.java) requires an ID and at least one supplied field. Omitted fields remain unchanged; supplied tags replace the complete set, with a lone empty `t/` clearing it. Contact validation occurs during parsing, before execution resolves the ID.

`EditCommand` creates an immutable replacement retaining the optional follow-up. `Model.setPerson` checks the full collection for duplicates, transfers the ID through the aggregate, and re-evaluates the active filter. Changing a search field can hide the edited client and clear selection; a failed save publishes none of these changes. This reuses the same replacement path as follow-up changes rather than assigning identity in individual commands.

### Follow-up state and date handling

`Person` contains **zero or one** immutable `FollowUp` with `dueDate : LocalDate` and `description : String`. Setting/replacing creates a new `Person` preserving contacts/tags/ID; clearing removes the optional value. There is no completion flag, notification scheduler or action history.

`FollowUpCommandParser` distinguishes `followup ID clear` from date/description input. The set command resolves the global ID before validating the raw date and description; date errors precede description errors. Past dates are allowed for overdue carry-over work. Calendar parsing is strict, with years 0001–9999. Notes retain Unicode spelling/internal spacing, trim outer ordinary spaces, and require 1–200 Unicode code points with visible content. The tokenizer recognizes space-prefixed `d/` and `m/` even inside a note; quotes do not escape them. These are deliberate grammar limits, documented in the UG.

`ModelManager.showPendingFollowUps()` includes every client with an action and stably sorts by due date. Ties retain full-list insertion order. `FollowUp.getStatus(today)` derives `OVERDUE`, `DUE_TODAY` or `UPCOMING` from the supplied date; the value does not read the device clock. Neither status nor selection is serialized.

`LogicManager` samples its clock once per command candidate and publishes that date only on success. Separately, `MainApp` refreshes the live date on startup, regained window focus and a one-minute timer. An idle window therefore updates across midnight; a failed command itself cannot advance the date, although an independent refresh can. ISO dates avoid dependence on the operating system's display format; the local clock/time zone still determine what counts as today.

#### A follow-up through the system

For `followup 17 d/2099-12-15 m/Discuss renewal`, dispatch creates a `SetFollowUpCommand` with raw values. The command resolves ID 17 on its candidate, validates and replaces the client while transferring identity. Storage serializes the action's date/description with the contact and identity metadata. Successful publication updates the table, profile and global summary; all use the same published date for status. Failed execution or saving retains the previous action and views.

<span id="selection-transition-evidence"></span>

### Client-profile selection model

The profile is model-owned session state, independent of JavaFX row highlighting. [`Model.selectedPersonProperty()`](https://github.com/AY2627S1-CS2103T-W13-3/tp/blob/master/src/main/java/seedu/address/model/Model.java) is a stable read-only property whose nullable value must be a canonical **visible** record. An equal-valued copy is insufficient. Selection changes do not mark client data dirty and are not persisted.

| Transition | Selection consequence |
| --- | --- |
| Reorder pending rows / replace selected client's immutable value | Follows the same ID while visible. |
| Filter out or delete selected client | Clears; returning to `list` does not reopen it. |
| Clear selected client's action | Clears in pending mode; remains in full/search mode if visible. |
| Add another client | Preserves the current mode/eligible selection; does not select the addition. |
| Restart | Starts without selection. |

#### Candidate copying and selection publication

`forkForCommand(today)` copies data, view state and selection into isolated observables. `commitFrom(candidate)` retains the live observable objects and maps the candidate's selected **Client ID** back to the actual committed record. This matters when an identical follow-up replacement creates an equal-valued new object: an equal aggregate snapshot need not reset the live list, so publishing the candidate reference directly would leave selection outside it. Canonical reconciliation handles either retained or replaced live data.

Profile scroll continuity is presentation state. `ClientProfilePanel` caches the rendered ID before a replacement removes the old record from the registry. The same client keeps its reading position across edits/action replacement; a different client (even with the same name) starts at the top. Model membership remains authoritative.

### Integrated dashboard and data ownership

`MainWindow.fillInnerParts()` composes the command/feedback bar, permanent-ID table, profile and grouped pending panel. It rejects repeated initialization. `Dashboard.css` owns host geometry/branding; component FXML/styles own their contents. Split panes allocate space without modifying model state.

`PersonListPanel` observes only `Logic.getFilteredPersonList()`, uses `Logic.getClientId` for labels and identity for the `VIEWING` marker, and enables due/status columns in pending mode. Rows clear recycled graphics. The table is unsortable by header; insufficient column width enables horizontal scrolling. Highlighting a row does not select a profile.

`PendingFollowUpProjection` instead consumes `Logic.getAllPersonList()`, the complete unmodifiable committed feed. It retains canonical references and stably sorts actions by due date, independent of search/pending filters. `PendingFollowUpPanel` groups this projection using the published date and observes selected-client changes for markers. There is no second client store or panel clock. `MainWindow` closes the projection and disposes both `ClientProfilePanel` and `PendingFollowUpPanel` when hidden. Cleanup is idempotent and panel callbacks guard queued notifications. The profile renders its initial display before subscribing, so failed construction leaves no listeners attached. `ClientProfileLifecycleTest` covers window cleanup, disposal during notification and failed construction.

### Command submission and feedback

Enter and Run use one `CommandBox` submission path; Run is not a default button that could also fire on Enter. Errors retain exact input and restore typing focus; success clears it. `MainWindow` explicitly chooses normal/error feedback, rather than classifying message wording. The selectable, scrollable `ResultDisplay` does not steal command focus. A separate persistent saved-data warning cannot be cleared by ordinary command feedback.

#### Client profile panel

`ClientProfilePanel` observes selected-client/date properties and wraps contacts, sorted tag chips and action text in its own scroll pane. The pending panel scrolls independently. Help is a separate F1/menu window with copy-link feedback and Escape closure; reopening restores an iconified Help window. The footer shows the real data path with a full-path tooltip. These controls do not change storage ownership.

### Saved client loading

[`JsonSerializableAddressBook`](https://github.com/AY2627S1-CS2103T-W13-3/tp/blob/master/src/main/java/seedu/address/storage/JsonSerializableAddressBook.java) represents the aggregate snapshot; `JsonAdaptedPerson`, `JsonAdaptedTag` and `JsonAdaptedFollowUp` reconstruct domain values. All contacts and identities are validated before `AddressBook.restorePersons` publishes them. Missing optional tags/actions mean an empty set/no action; required values, duplicates and invalid identity associations fail the **whole** load, without accepting an earlier valid subset.

#### Permanent identity in JSON

Only persistent contact/action/identity data is stored:

```json
{
  "nextClientId": 42,
  "persons": [
    {"clientId": 17, "name": "Rachel Lim", "phone": "00123",
     "email": "rachel@example.com", "address": "Blk 123", "tags": [],
     "followUp": {"dueDate": "2020-01-01", "description": "Call client"}}
  ]
}
```

Serialization asks the aggregate for each canonical record's ID; it never infers IDs from display order. Adapters require integral JSON numbers in the ID/counter ranges. Restoration rejects repeated IDs and a counter not greater than every allocated ID. Empty books retain their counter, including exhaustion.

A file is legacy only if **every** client ID and the root counter are absent or null. Loading assigns insertion-order IDs 1..N with counter N+1, without rewriting; the first successful mutation writes the current schema. Once any identity value is non-null, incomplete/null metadata is invalid throughout. Rejecting partial metadata avoids silently assigning new identities to established records.

**Compatibility limitation:** externally removing/nullifying *all* identity history makes a migrated file indistinguishable from legacy input. Former gaps and the high-water counter cannot be recovered without a complete backup. Normal operations do not clear this metadata.

#### Protected startup and safe replacement

Client reads use strict UTF-8 (one initial BOM is accepted), reject a null root/trailing content, duplicate object properties, unknown client-schema fields and non-string saved text. Domain validation then rejects invalid values, including malformed UTF-16 from JSON escapes. A genuinely absent file means a new empty book. If absence cannot be established because access is denied, loading is attempted so an inaccessible existing file cannot be mistaken for a first launch.

`JsonAddressBookStorage` translates load failures to `DataLoadingException`. `MainApp.initModelManager` creates an empty mutation-blocked model and the UI keeps a persistent warning. Queries remain usable; command dispatch blocks mutations. Repairing permissions/JSON during that session does not unlock it: restore a valid complete backup, then restart and reload before modifying clients.

For saves, [`JsonAddressBookStorage`](https://github.com/AY2627S1-CS2103T-W13-3/tp/blob/master/src/main/java/seedu/address/storage/JsonAddressBookStorage.java) serializes a candidate, writes a sibling temporary file and atomically replaces the destination. Unsupported atomic replacement is a save failure, not a fallback to partial writing; failures attempt temporary-file cleanup. A symbolic-link destination is rejected before writing and again before replacement. These checks are best effort against external filesystem changes: a late path swap can replace the link itself, but the atomic move does not write through to its target. The instance lock coordinates PolicyHarbor processes, not external editors. Atomic replacement is not a backup/history service or a guarantee against hardware failure.

### Preference recovery

Preferences have a different recovery policy from client records. `JsonUserPrefsStorage` rejects explicitly null `guiSettings` and malformed/mapping failures; `MainApp.initPrefs` falls back to `UserPrefs` defaults and attempts to save the recovery. Missing settings retain defaults. Client loading proceeds independently, so invalid preferences alone do not block client mutations. Preference compatibility still ignores unknown properties and accepts the last repeated property; strict client-reader options are not imposed on the shared mapper.

### Application instance ownership

Before preferences/client loading, `MainApp.init()` acquires an OS file lock through `AppInstanceLock` on `.policyharbor.lock` in the working folder. A competing launch or lock I/O failure leaves services uninitialized; startup explains the error and exits without preference writes. Initialization failure, normal shutdown or process termination releases ownership.

The marker is retained: deleting it while an owner holds the original file could let another process lock a replacement. An in-process registry rejects duplicate acquisitions before opening another descriptor, whose closure can release existing OS locks on some systems. Separate working folders remain independent. This prevents simultaneous PolicyHarbor writers to one working folder, without introducing a server or remote authentication.

### Diagnostic log fallback

`JsonUtil.readJsonFile` logs only path and failure category, excluding Jackson messages/throwables that can contain raw client data. It retains the cause in `DataLoadingException` for callers; that does not make the cause safe to print in ordinary diagnostics. Command submission logs outcome/category without raw input. These safeguards limit accidental disclosure in local diagnostic files; they do not encrypt the user's local client data.

`LogsCenter` installs console logging before opening rotating `policyharbor.log` files. A file-handler `IOException` produces a generic warning and leaves console logging available; other initialization failures propagate. This fallback does not make client/preference writes succeed in an unwritable folder.

--------------------------------------------------------------------------------------------------------------------

<span id="automated-evidence"></span>

<span id="layout-boundaries-and-acceptance-tools"></span>

<span id="v15-display-matrix-checks"></span>

## **Documentation, logging, testing, dev-ops**

Use the [documentation](Documentation.html), [testing](Testing.html), [logging](Logging.html) and [DevOps](DevOps.html) guides for tool setup. For implementation work, run focused tests first, then the configured `./gradlew check coverage` and `.github/run-checks.sh` checks. Assertions check developer invariants; user-input validation uses normal exceptions and must work with or without `-ea`.

| Boundary to verify | Representative source/test entry points |
| --- | --- |
| Domain validity, duplicate relation and allocation | `PersonTest`, `AddressTest`, `EmailTest`, `AddressBookTest`, `ClientIdentityRegistryTest` |
| Dispatch, search/edit grammar and global ID targeting | `AddressBookParserTest`, `FindCommandParserTest`, `EditCommandParserTest`, `ClientIdTargetingTest` |
| Save-before-publish, canonical selection and dates | `LogicManagerTest`, `ClientProfileSelectionTest`, `ClientProfileWorkflowIntegrationTest`, `FollowUpWorkflowIntegrationTest` |
| Identity migration/round trips and whole-file recovery | `ClientIdMigrationTest`, `ClientIdRoundTripTest`, `ClientIdLoadingProtectionTest`, `SavedTextTypeRecoveryTest`, `InaccessibleDataRecoveryTest` |
| Strict JSON/Unicode and private diagnostics | `JsonDuplicatePropertyTest`, `SavedDataEncodingRecoveryTest`, `SavedAddressRecoveryTest`, `JsonDiagnosticPrivacyTest`, `LoadingDiagnosticPrivacyTest` |
| Atomic I/O, instance ownership and diagnostic fallback | `JsonAddressBookStorageTest`, `StorageDirectoryRecoveryTest`, `AppInstanceLockTest`, `MainAppInstanceTest`, `LogsCenterTest` |
| Observable dashboard, input and real geometry | `DashboardIntegrationTest`, `DashboardLayoutBoundaryTest`, `CommandSubmissionTest`, `ClientProfileScrollTest`, `HelpAndFooterTest` |

Test classes live under [`src/test/java/seedu/address`](https://github.com/AY2627S1-CS2103T-W13-3/tp/tree/master/src/test/java/seedu/address). Storage tests inject write/move failures; integration tests assert saved bytes and live state, rather than just error text. Fixed/injected dates establish midnight and time-zone behavior reproducibly. Native focus, font substitution and physical display scaling still need packaged-app checks.

`DisplayMatrixFixture` is an **IDE/test-source tool**, not a packaged command. It writes 1,000 synthetic records to a fresh folder and refuses overwrite. Launch the JAR from that folder, reach `view 1000`, inspect long contact values and scroll to the final pending card. `StatusBarFooterLongPathFixture` separately exercises a roughly 200-character path/tooltip, which the app's fixed relative data path cannot configure through preferences. Logical scene-size tests establish geometry, not physical scaling or throughput. Record actual OS/JDK/resolution/scaling results and pending combinations in the [platform matrix](testing/v1.5/platform-matrix.html).

The repository's Java CI workflow configures Java 25 Zulu with JavaFX on Linux, macOS and Windows, `check coverage`, and Linux repository checks. Inspect the actual workflow/check status on the release commit; configuration is not evidence that CI ran. For release evidence, identify the exact source revision, JAR checksum/runtime and tests executed. A previous revision's test record does not by itself verify the final distributed JAR.

--------------------------------------------------------------------------------------------------------------------

## **Appendix: Requirements**

### Product scope

**Target users** are individual insurance agents who keep their own client/prospect contact records and dated next actions on a personal computer, type quickly and prefer commands. **Value proposition:** keep contact context and one pending action per client together, with stable targeting and a chronological overview of overdue, due-today and upcoming work.

### User stories

The following stories are implemented in the verified candidate; they are not a statement that the published v1.4 JAR has these newer capabilities. In each row, read: **As an insurance agent, I want to [capability], so that I can [benefit].**

| Priority | Capability | Benefit |
| --- | --- | --- |
| High | add a client/prospect with contact details | keep their information in one place |
| High | list clients and search by name, phone or tag | locate the record using the information I remember |
| High | target clients by permanent IDs across filtering/reordering | act on the intended record consistently |
| High | edit contact details while preserving ID and follow-up | keep information current without recreating the client |
| High | open a complete client profile | review context before making contact |
| High | delete a client and their pending action | remove incorrect records without leaving orphan work |
| High | reload saved records, IDs and actions after restarting | retain my work between sessions |
| High | record or replace one dated next action | remember an agreed commitment |
| High | review pending work chronologically and by status | prioritize follow-ups even while searching clients |
| High | clear a completed action | remove work that no longer needs attention |
| High | receive actionable errors while rejected changes preserve data | correct mistakes and retry safely |
| Medium | categorize clients using tags | retain useful context and find a category |

#### Recorded future requirements — not implemented

The existing backlog records the following aspirations. They are **not commands, release commitments or proposed implementations**. Each row uses the same insurance-agent role and benefit format above.

| Capability | Benefit |
| --- | --- |
| retain multiple actions and follow-up history | review earlier commitments |
| receive follow-up notifications | be reminded without opening the pending view |
| archive inactive clients | retain context without treating them as active work |
| import existing client records | avoid re-entering a dataset manually |
| combine multiple search fields | narrow a large result set using several criteria |

### Use cases

**System:** PolicyHarbor. **Actor:** individual insurance agent. Unless an extension states otherwise, the app has loaded valid data. Commands are single submissions; the system does not prompt for separate fields or confirmation. These use cases cover distinct interaction patterns; routine add/delete/list syntax is in the UG and manual appendix.

#### UC01 — Retrieve a client hidden by the current view

**Preconditions:** the client exists with a known permanent ID; the table is filtered or in pending mode and hides that client.

**Main success scenario**

1. Agent requests the client's profile using their permanent ID.
2. System resolves the ID against the complete client collection, restores the full list and opens the correct profile.

Use case ends. Saved data remains unchanged.

**Extensions**

* 1a. The request has invalid syntax or an invalid ID value.
  * 1a1. System explains the format/ID constraint and retains the current view and profile.
  * Use case ends.
* 2a. No client has that valid ID.
  * 2a1. System reports the missing ID and retains the current view and profile.
  * Use case ends.

#### UC02 — Edit a client while retaining their identity

**Preconditions:** the client exists; client mutations are not blocked by startup recovery. The agent has the permanent ID and at least one replacement field.

**Main success scenario**

1. Agent submits contact changes for that ID.
2. System validates the supplied fields, resolves the client and checks other records for duplicates.
3. System saves the replacement with the same ID and pending action, then publishes it and success feedback.

Use case ends. The active filter is retained; the profile follows the replacement only while it remains visible.

**Extensions**

* 1a. Startup recovery blocks mutations.
  * 1a1. System explains that saved data must be repaired/restored and reloaded; no changes are accepted.
  * Use case ends.
* 2a. Fields/ID are invalid, the ID is absent or the replacement duplicates another client.
  * 2a1. System explains the relevant error and retains data, filter and selection.
  * 2a2. Agent corrects the retained request.
  * Use case resumes at step 1.
* 3a. Storage cannot save the replacement.
  * 3a1. System gives recovery guidance and keeps the previous live record, ID, action, view and saved file.
  * 3a2. Agent addresses the filesystem problem and retries.
  * Use case resumes at step 1.

#### UC03 — Record, replace or clear a next action

**Preconditions:** the client exists with a known ID; mutations are allowed. A replacement/clear request concerns the client's single pending action.

**Main success scenario**

1. Agent submits a date and action description for that ID.
2. System resolves the client and validates the date and description.
3. System saves the new action, replacing any previous one, then publishes the updated client and pending views.

Use case ends. The ID/contacts are retained; pending order and status reflect the new date, and any visible selected profile follows the same client.

**Extensions**

* 1a. Agent instead requests that the action be cleared.
  * 1a1. System resolves the client and confirms an action exists.
  * 1a2. System saves/publishes its removal. The client leaves pending mode; an open profile clears there but remains in full/search mode while visible.
  * Use case ends.
* 1a1a. There is no pending action.
  * 1a1a1. System explains this and makes no changes.
  * Use case ends.
* 2a. Input/ID is invalid or the ID is absent.
  * 2a1. System explains the error; the previous action, data and view remain unchanged.
  * Use case ends.
* 3a. Saving the new/replacement action fails.
  * 3a1. System gives recovery guidance and preserves the previous action, data, view and saved file; no tentative reorder is published.
  * Use case ends.
* 1a2a. Saving removal fails.
  * 1a2a1. System gives recovery guidance and preserves the previous action, data, view and saved file; no tentative removal is published.
  * Use case ends.

### Non-Functional Requirements

These are acceptance criteria. Platform, timing and physical-display targets require recorded measurements; stating a target does not mark it verified.

| ID | Quality constraint / acceptance criterion | Evidence boundary |
| --- | --- | --- |
| NFR1 | Core operations work on Windows, Linux and macOS using Java 25 and the prescribed JavaFX runtime (Zulu JDK+FX on macOS). | Run the packaged scenarios on each supported OS/runtime; logical JavaFX tests alone do not prove portability. |
| NFR2 | Capacity target: at 1,000 synthetic clients, all clients and pending cards remain reachable by scrolling or permanent ID. | The layout fixture/tests exercise this capacity; retain physical display checks. |
| NFR3 | Responsiveness target: after warm-up, 95% of 30 representative add/edit/delete/search/view/follow-up commands complete from submission to visible feedback within 1 second at 1,000 clients. | Record hardware, runtime, local disk, dataset and timings. No such benchmark is claimed here; 1,000-record rendering is not timing evidence. |
| NFR4 | A tester can complete add/search/view/edit/action/clear/delete using the keyboard without refocusing with a pointer after each submission. | Packaged Enter/Tab/Space/error-correction checks; this replaces an unmeasured faster-than-mouse claim. |
| NFR5 | Successful mutations survive close/restart with identical contacts, IDs, counter and actions. | Compare restored values; selection/list mode are intentionally session-only. |
| NFR6 | Rejected input and reported save failures leave live client data, IDs, list/selection and existing saved bytes unchanged. | Fault-injection and workflow tests; independent date refresh can still occur. |
| NFR7 | Invalid/inaccessible existing client data is preserved and blocks mutations until a valid reload. | Recovery tests include unchanged bytes and repair/restart. |
| NFR8 | Core client/action operations work with networking disabled, with data on local storage and no required remote server. | Help's external UG link is supplementary. Local storage is not encrypted by the app. |
| NFR9 | Distribution is one executable JAR of at most 100 MB, launched from a writable working folder without a separate installer. | Measure the final uploaded asset; a candidate measurement does not certify a later JAR. |
| NFR10 | Command box/feedback, client table and both sidebar views remain accessible at 1280×720/150% and 1920×1080/100% or 125%; overflowing content remains reachable. | Record actual OS/resolution/scaling in the platform matrix. Logical geometry is supporting evidence. |
| NFR11 | Equivalent operations use consistent permanent-ID syntax, shared domain validation and explicit corrective feedback; retained failed input remains editable. | Parser/feedback tests and packaged scenarios. |
| NFR12 | Closing the main window closes Help, stops date refresh and releases process ownership so the same folder can be reopened. | Lifecycle tests and second-process/restart checks. |

### Glossary

* **Client record:** contact details/tags and, optionally, one pending action for a client or prospect.
* **Client ID:** permanent positive integer allocated by the address book; unrelated to row position and not reused after normal deletion.
* **Canonical record:** the actual immutable `Person` instance registered in its owning address book, rather than an equal-valued copy.
* **Follow-up:** next action with a due date and description.
* **Pending:** recorded and not cleared, including actions whose dates have passed.
* **Overdue / due today / upcoming:** due date is before / equal to / after the published local date.
* **Protected startup:** empty query-capable session that blocks client mutations after a saved-data loading failure.

--------------------------------------------------------------------------------------------------------------------

## **Appendix: Instructions for manual testing**

These are independent packaged-app procedures, not a statement that every OS/scaling combination has passed. Use **only disposable synthetic data**. Record the JAR checksum, matching commit, Java/OS and physical display settings. Run each lettered scenario in its own fresh writable working folder; copy the same `PolicyHarbor.jar` there and launch from that folder with Java 25 (`java -jar PolicyHarbor.jar`). On macOS use the prescribed Zulu JDK+FX setup from [SettingUp](SettingUp.html). The title may still show v1.4; verify the artifact, not its window label.

**Two-client fixture:** whenever requested below, start an empty folder, launch, and enter these lines separately. Expect Rachel ID 1 with lowercase `health`/`active` tags, Amanda ID 2, and no selected profile. Repeated tag case variants collapse.

```text
add n/Rachel Lim p/001 e/rachel@example.com a/Blk 123 t/HEALTH t/health t/active
add n/Amanda Lee p/002 e/amanda@example.com a/Blk 456
```

<span id="startup-persistence-and-basic-workflow"></span>

<span id="concurrent-launches-and-restart"></span>

### A. Launch and initial setup

1. Launch in a fresh folder. Expect an empty table, `No client selected.`, an empty pending summary and no saved-data warning. Run `list` and `followups`; neither creates client records.
2. Add the two-client fixture, close the main window and restart in the same folder. Expect both records/IDs, no selected profile and the full list. Move/resize the window, close/restart and check restored window preferences.
3. While that instance is open, launch the JAR from a second terminal in the **same** folder. Expect an already-running error, no second working window and unchanged saved client bytes. Close the first normally and reopen: the retained `.policyharbor.lock` marker must not block restart. Never remove a marker while an owner is running.
4. Launch in a different fresh working folder while the first is open. Expect an independent empty dataset. Close all test windows afterwards, including Help if open.

<span id="search-and-validation"></span>

<span id="editing-while-preserving-identity"></span>

### B. Basic client workflow

**Start:** fresh two-client fixture. Submit each row in order.

| Commands / action | Essential expected result |
| --- | --- |
| `list`, then `find lim AMANDA Lim` | Both clients once, insertion order; `2 client(s) listed!`. |
| `find n/rac`, then `find p/01`, then `find t/HEALTH` | Each finds only Rachel (ID 1): partial name/digits and exact case-insensitive tag. |
| `find 001`, then `find 01` | Exact bare-phone search finds Rachel for `001`, no match for `01`. |
| `view 1` | Restores the full list from the empty result and opens Rachel; `Showing client ID 1: Rachel Lim`. |
| `edit 1 p/+65 9123 4567 t/RENEWAL t/priority` | Same ID/profile; new phone and replacement lowercase tags. |
| `edit 1 t/` | Tags cleared; contacts retained. |
| `find p/9123`, then `edit 1 p/888` | Rachel leaves results and profile closes; `view 1` restores the full list and shows phone `888`. |
| `delete 2`, then add `add n/Ben Tan p/003 e/ben@example.com a/Blk 789` | Amanda removed; Ben receives ID **3**, not 2. Rachel remains ID 1. |

Repeat a same-name check in a separate fresh folder: add two Rachel Lim records with different phone **and** email values, then view/delete by the displayed IDs. Only the intended record is affected. Conversely, adding `n/RACHEL  LIM` with Rachel's existing phone or case-variant email is rejected as a duplicate, with existing records unchanged.

<span id="recording-and-replacing-overdue-carry-over-work"></span>

### C. Follow-up workflow

**Start:** fresh two-client fixture. The fixed future dates below assume the computer date is before December 2099; otherwise substitute two valid dates after today consistently.

```text
followup 1 d/2099-12-15 m/Discuss renewal
followup 2 d/2099-12-14 m/Call Amanda
followups
view 1
followup 1 d/2099-12-13 m/Confirm renewal
```

Expect IDs 2 then 1 before replacement, then 1 before 2 afterwards. Rachel's profile/`VIEWING` marker follows ID 1, and replacement leaves only the new note. The global summary contains both actions even after `find Amanda`; Rachel's hidden profile clears.

1. Enter `list`, then replace the two dates with **actual yesterday and today** using `followup ID d/YYYY-MM-DD m/ACTION`. Expect `OVERDUE` and `DUE TODAY` respectively in profile/pending views; future work is `UPCOMING`. ISO input works regardless of the OS's displayed date format.
2. Give both clients the same date and enter `followups`; expect insertion order (IDs 1, 2). Reopen ID 1 and enter `followup 1 clear`. It leaves the pending table/summary and the profile clears. `view 1` restores the full list and shows `Follow-up: None`.
3. Enter `list`, `view 2`, then `followup 2 clear`; the selected profile remains with no action. Repeat `followup 2 clear`; expect a no-pending-action error with no change.
4. Record `followup 1 d/2099-12-15 m/续保电话 👩‍💻`, then restart. Expect preserved Unicode spelling/action and no session selection. Glyph appearance must be checked on each platform.

For midnight behavior, use a disposable test environment whose local date can be controlled: leave a due-today action open across midnight (or change the test clock), wait for the minute refresh or refocus the window. Expect consistent updated statuses. Fixed-clock automated tests cover this boundary when changing the host date is unsuitable; record whether the real packaged case was exercised.

<span id="profile-transitions-and-permanent-client-ids"></span>

<span id="integrated-dashboard-checks"></span>

<span id="integrated-v14-dashboard-checks"></span>

### D. Permanent IDs and profile selection

**Start:** fresh two-client fixture; record actions for ID 1 on `2099-12-15` and ID 2 on `2099-12-14`, then `followups`, `view 2`.

| Action | Essential expected result |
| --- | --- |
| Replace ID 2's action with `2099-12-16` | Amanda moves after Rachel, retaining ID 2 and the open profile/marker. |
| Click/highlight Rachel's row | Amanda stays viewed; row highlighting does not change model selection. |
| `find Rachel`, then `list` | Amanda's hidden profile clears and does not reopen automatically. Global summary keeps both actions. |
| `view 2`, then `delete 1` | Amanda stays viewed even though the preceding record disappears. Rachel's action disappears from the summary too. |
| `delete 2` | Amanda/action removed; profile and pending summary empty. |

This tests record identity across changing row positions; it does not assume IDs are consecutive after deletion.

<span id="reserved-prefixes-in-follow-up-descriptions"></span>

<span id="save-failures-and-automated-boundaries"></span>

### E. Persistence and failed operations

**Start:** fresh two-client fixture. Enter `followup 1 d/2099-12-15 m/Discuss renewal`, `edit 1 a/Blk 789`, then `view 1`. Copy `data/addressbook.json` as a baseline before rejected operations.

1. Submit `edit 1`, `view 0`, `list extra`, `find n/Rachel p/001`, an invalid date (`followup 1 d/2026-02-29 m/Call`), and a repeated reserved prefix (`followup 1 d/2099-12-15 m/Confirm d/m/y format`). Each must show a corrective error, retain editable input and preserve data/list/profile and saved bytes. `help`, `exit` and standalone `clear` must report `Unknown command.`; F1/window closure remain GUI actions.
2. Close/restart. Expect edited contacts, ID 1, ID 2 and the action retained, with full list/no selected profile. Queries (`list`, `find Rachel`, `view 1`, `followups`) must not rewrite client data.
3. For a portable failed-save check, close the app, back up the valid file, then start it again. **While it remains open**, move the disposable `data` directory to `data-backup` and put a regular file named `data` in its place. Try `delete 1`. Expect save-failure guidance with `No changes were kept.`, and both clients, action, current view and profile retained. Compare the JSON in `data-backup` with its baseline.
4. Remove only that obstruction file and restore `data-backup` to `data`. Retry `delete 1`; expect success, Rachel/action removed and Amanda still ID 2. Restart and add a distinct client: expect ID 3. Do not use this procedure with real data or modify the active lock marker.

Automated storage fault injection covers additional write/atomic-move failures without relying on OS permission behavior.

<span id="window-help-and-long-content"></span>

### F. Interface, keyboard and long content

**Start:** fresh two-client fixture; set one action and `view 1`.

1. Submit valid `list` with Enter and Run. Each activation must produce one result and clear the field. Submit an invalid command by each route, then correct the retained input without clicking the field. Tab to Run and activate with Space; verify single submission and restored typing focus.
2. Press F1 from the command box. Expect Help with **Copy link** focused. Activate it and paste into a disposable editor; expect `https://ay2627s1-cs2103t-w13-3.github.io/tp/UserGuide.html`. Manual selection/copy must also work. Minimize Help and invoke F1/menu again: it must restore. Escape closes it; reopening clears previous copy feedback. If clipboard rejection can be induced on the platform, expect the manual-copy fallback; otherwise record this case as pending.
3. Use a long address and a 200-code-point action. Resize/drag both split dividers: contact/action text wraps, profile/summary scroll independently and table columns scroll horizontally rather than disappearing. Feedback must remain readable, selectable and scrollable, including a multiline error's final line. Check empty/recycled rows have no stale badge/marker.
4. Exercise scrolling with many synthetic records using the [test-source fixture procedure](#documentation-logging-testing-dev-ops). At the physical settings in NFR10, verify `view 1000`, the last table ID, long profile fields and the final pending card. Record untested OS/scaling combinations, rather than treating logical scene tests as completed physical checks.
5. Open Help, close the main window and verify all application windows close. Relaunch with `java -ea -jar PolicyHarbor.jar`; repeat valid input and one rejection. Assertions must not alter normal validation behavior.

<span id="loading-protection-and-saved-data-compatibility"></span>

<span id="inaccessible-client-data-at-startup"></span>

<span id="permanent-id-persistence-and-recovery-checks"></span>

### G. Important recovery

**Start:** separate disposable folder, app closed. Create a `data` directory and save the [current identity JSON example](#permanent-identity-in-json) as `data/addressbook.json`; preserve a complete backup outside `data`.

1. Launch and `view 17`: Rachel loads with ID 17 and an overdue action (assuming today is after 2020-01-01). Queries must leave the file byte-identical. Delete 17, close/restart and add a distinct client: expect ID **42**. Restart and confirm it remains 42.
2. Close, restore the example, then make `nextClientId` equal to 17. Launch: expect an empty table/profile/summary and a persistent saved-data warning. Queries stay available; `view 17` reports no such loaded client. `add`, `edit`, `delete` and `followup` must be blocked and the invalid bytes preserved. Command success/error feedback cannot dismiss the warning.
3. Restore the full valid backup while the protected app is open. A mutation must still be blocked. Close/restart: `view 17` works again and a valid mutation saves normally. Repeat with malformed JSON in another disposable copy to cover parsing rather than identity validation.
4. For legacy migration, close and use a disposable copy with **all** client IDs and `nextClientId` removed. Loading assigns insertion-order ID 1 without rewriting; the first successful mutation writes identity metadata. Do not erase all IDs in the only copy of real data: that also discards former allocation history.
5. In a fresh fixture with valid client data, close, write `{"guiSettings":null}` to `preferences.json`, and relaunch. Expect default window preferences and the same clients without a client-data warning; a normal mutation still saves. Client-file bytes must not change merely because preferences were repaired.

For inaccessible-file behavior, use the permission-aware automated regression or a disposable OS account/folder that can actually deny access. Restoring access in a protected session must not enable writes before restart. Record platforms/accounts where this restriction cannot be exercised.
