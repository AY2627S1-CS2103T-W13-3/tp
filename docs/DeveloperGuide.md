---
layout: page
title: Developer Guide
---
* Table of Contents
{:toc}

--------------------------------------------------------------------------------------------------------------------

## **Acknowledgements**

* PolicyHarbor adapts [AddressBook Level 3](https://github.com/se-edu/addressbook-level3), retaining its Java/JavaFX architecture and test conventions.
* Ayush Jain used OpenAI Codex to assist with the client-record validation, duplicate detection, tag formatting, add/delete parsing and messages, associated tests, and the related User/Developer Guide sections in this increment.

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

**`Main`** (consisting of classes [`Main`](https://github.com/se-edu/addressbook-level3/tree/master/src/main/java/seedu/address/Main.java) and [`MainApp`](https://github.com/se-edu/addressbook-level3/tree/master/src/main/java/seedu/address/MainApp.java)) is in charge of the app launch and shut down.

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

The **API** of this component is specified in [`Ui.java`](https://github.com/se-edu/addressbook-level3/tree/master/src/main/java/seedu/address/ui/Ui.java)

![Structure of the UI Component](images/UiClassDiagram.png)

The UI consists of a `MainWindow` and its parts, such as `CommandBox`, `ResultDisplay`, `PersonListPanel`, and `StatusBarFooter`. All of these, including `MainWindow`, inherit from the abstract `UiPart` class, which captures common behavior among classes that represent visible GUI parts.

The `UI` component uses the JavaFX UI framework. The layouts of these UI parts are defined in matching `.fxml` files in `src/main/resources/view`. For example, [`MainWindow.fxml`](https://github.com/se-edu/addressbook-level3/tree/master/src/main/resources/view/MainWindow.fxml) specifies the layout of [`MainWindow`](https://github.com/se-edu/addressbook-level3/tree/master/src/main/java/seedu/address/ui/MainWindow.java).

The `UI` component,

* executes user commands using the `Logic` component.
* listens for changes to `Model` data so that the UI can be updated with the modified data.
* keeps a reference to the `Logic` component, because the `UI` relies on the `Logic` to execute commands.
* depends on some classes in the `Model` component because it displays `Person` objects from the model.

### Logic component

**API** : [`Logic.java`](https://github.com/se-edu/addressbook-level3/tree/master/src/main/java/seedu/address/logic/Logic.java)

Here's a (partial) class diagram of the `Logic` component:

<img src="images/LogicClassDiagram.png" width="550"/>

The sequence diagram below illustrates the interactions within the `Logic` component, taking `execute("delete 1")` API call as an example.

![Interactions Inside the Logic Component for the `delete 1` Command](images/DeleteSequenceDiagram.png)

<div markdown="span" class="alert alert-info">:information_source: **Note:** The lifeline for `DeleteCommandParser` should end at the destroy marker (X), but due to a limitation of PlantUML, it continues to the end of the diagram.
</div>

How the `Logic` component works:

1. When `Logic` is called upon to execute a command, the command is passed to an `AddressBookParser` object, which in turn creates a parser that matches the command (e.g., `DeleteCommandParser`) and uses it to parse the command.
1. This results in a `Command` object (more precisely, an object of one of its subclasses e.g., `DeleteCommand`) which is executed by the `LogicManager`.
1. The command can communicate with the `Model` when it is executed (e.g. to delete a person).<br>
   Note that although this is shown as a single step in the diagram above for simplicity, the code can require several interactions between the command object and the `Model` to complete the operation.
1. The result of the command execution is encapsulated as a `CommandResult` object which is returned from `Logic`.

Here are the other classes in `Logic` (omitted from the class diagram above) that are used for parsing a user command:

<img src="images/ParserClasses.png" width="600"/>

How the parsing works:

* When called upon to parse a user command, the `AddressBookParser` class creates an `XYZCommandParser` (`XYZ` is a placeholder for the specific command name, e.g., `AddCommandParser`). The parser uses the other classes shown above to parse the user command and create an `XYZCommand` object (e.g., `AddCommand`). The `AddressBookParser` returns that object as a `Command` object.
* All `XYZCommandParser` classes, such as `AddCommandParser` and `DeleteCommandParser`, implement the `Parser` interface so they can be treated similarly where appropriate, for example during testing.

### Model component

**API** : [`Model.java`](https://github.com/se-edu/addressbook-level3/tree/master/src/main/java/seedu/address/model/Model.java)

<img src="images/ModelClassDiagram.png" width="450" />

The `Model` component,

* stores the address book data i.e., all `Person` objects (which are contained in a `UniquePersonList` object).
* stores the `Person` objects selected by the current filter, such as search results, in a separate _filtered_ list. It exposes this list as an unmodifiable `ObservableList<Person>` that the UI can observe and bind to, so the UI updates when the list changes.
* stores a `UserPrefs` object that represents the user’s preferences (currently, just the GUI settings). This is exposed to the outside as a `ReadOnlyUserPrefs` object.
* does not depend on any of the other three components (as the `Model` represents data entities of the domain, they should make sense on their own without depending on other components)

<div markdown="span" class="alert alert-info">:information_source: **Note:** The alternative, arguably more object-oriented, design below keeps a unique list of tags in `AddressBook`, and each `Person` references tags from that list. This lets `AddressBook` maintain one `Tag` object per unique tag instead of each `Person` holding its own `Tag` objects.<br>

<img src="images/BetterModelClassDiagram.png" width="450" />

</div>

### Storage component

**API** : [`Storage.java`](https://github.com/se-edu/addressbook-level3/tree/master/src/main/java/seedu/address/storage/Storage.java)

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

`AddressBook` delegates to this collection path. The existing model, command candidates and storage conversion therefore reuse the same rejection rule. Storage adapter alignment and profile selection belong to their separate packages; this increment introduces no persistent ID or profile API.

**Alternatives considered:** name-only rejection prevents legitimate same-name clients. Redefining `Person.equals`/`hashCode` around duplicate rejection would violate equality's transitivity. Both are unsuitable. `Person.isSamePerson` retains exact stored-name matching for legacy callers; it is neither the duplicate predicate nor a stable record-identity key. `equals`/`hashCode` still include all stored fields and follow-up.

`Tag` normalizes validated ASCII values with `Locale.ROOT`, so sets deduplicate case variants. `Tag.formatTags(Set<Tag>)` is the shared display API: sorted bracketed lowercase values separated by one space, or `None`. `Messages.format(Person)` uses it and emits contact details only; add/delete feedback does not append follow-up information.

### Client command parsing and publication

`AddCommandParser` retains the existing tokenizer. It checks required prefixes and preamble before repeated required prefixes, then validates name, phone, email, address and tags in that order. Unrecognized prefixes remain part of the current field value. The model's whole-address-book duplicate check follows parsing, even when the client is hidden by a filter. Successful addition resets the view to the full insertion-order list.

`DeleteCommandParser` checks for exactly one ordinary-space-separated token before calling `ParserUtil.parseIndex`. The shared helper accepts ASCII digits, trims outer U+0020, removes leading zeroes before bounded conversion and rejects values outside `1..2147483647` without leaking `NumberFormatException`. `FollowUpParserUtil` delegates to it. The future view parser can consume the same helper. Delete resolves an index against `Model.getFilteredPersonList`, retaining the active mode and deleting the full record with its action.

The merged `LogicManager` executes against a candidate, saves dirty data, then publishes the candidate. Parsing, duplicate, index and save failures preserve the live data, list mode/order and date; successful read-only commands do not save. This increment tests that existing transaction boundary with client commands instead of introducing another execution path. UI selection and excluded legacy command routes are outside this package.

Tests cover domain partitions, exact messages, validation precedence, normalized duplicates, the non-transitive conflict example, safe replacements, tag normalization, index boundaries and many leading zeroes. `ClientRecordCommandsTest` exercises the real dispatcher/model/Logic and temporary-file storage, including same-name round trips, pending/filter indices, follow-up targeting and injected save failures. Parser tests cover syntax separately from command displayed-range checks.

### \[Proposed\] Undo/redo feature

#### Proposed Implementation

The proposed undo/redo mechanism is facilitated by `VersionedAddressBook`. It extends `AddressBook` with an undo/redo history, stored internally as an `addressBookStateList` and `currentStatePointer`. Additionally, it implements the following operations:

* `VersionedAddressBook#commit()` — Saves the current address book state in its history.
* `VersionedAddressBook#undo()` — Restores the previous address book state from its history.
* `VersionedAddressBook#redo()` — Restores a previously undone address book state from its history.

These operations are exposed in the `Model` interface as `Model#commitAddressBook()`, `Model#undoAddressBook()` and `Model#redoAddressBook()` respectively.

Given below is an example usage scenario and how the undo/redo mechanism behaves at each step.

Step 1. The user launches the application for the first time. The `VersionedAddressBook` will be initialized with the initial address book state, and the `currentStatePointer` pointing to that single address book state.

![UndoRedoState0](images/UndoRedoState0.png)

Step 2. The user executes `delete 5` command to delete the 5th person in the address book. The `delete` command calls `Model#commitAddressBook()`, causing the modified state of the address book after the `delete 5` command executes to be saved in the `addressBookStateList`, and the `currentStatePointer` is shifted to the newly inserted address book state.

![UndoRedoState1](images/UndoRedoState1.png)

Step 3. The user executes `add n/David …​` to add a new person. The `add` command also calls `Model#commitAddressBook()`, causing another modified address book state to be saved into the `addressBookStateList`.

![UndoRedoState2](images/UndoRedoState2.png)

<div markdown="span" class="alert alert-info">:information_source: **Note:** If a command fails its execution, it will not call `Model#commitAddressBook()`, so the address book state will not be saved into the `addressBookStateList`.

</div>

Step 4. The user now decides that adding the person was a mistake, and decides to undo that action by executing the `undo` command. The `undo` command will call `Model#undoAddressBook()`, which will shift the `currentStatePointer` once to the left, pointing it to the previous address book state, and restores the address book to that state.

![UndoRedoState3](images/UndoRedoState3.png)

<div markdown="span" class="alert alert-info">:information_source: **Note:** If the `currentStatePointer` is at index 0, pointing to the initial AddressBook state, then there are no previous AddressBook states to restore. The `undo` command uses `Model#canUndoAddressBook()` to check if this is the case. If so, it will return an error to the user rather
than attempting to perform the undo.

</div>

The following sequence diagram shows how an undo operation goes through the `Logic` component:

![UndoSequenceDiagram](images/UndoSequenceDiagram-Logic.png)

<div markdown="span" class="alert alert-info">:information_source: **Note:** The lifeline for `UndoCommand` should end at the destroy marker (X), but due to a limitation of PlantUML, it continues to the end of the diagram.

</div>

Similarly, how an undo operation goes through the `Model` component is shown below:

![UndoSequenceDiagram](images/UndoSequenceDiagram-Model.png)

The `redo` command does the opposite — it calls `Model#redoAddressBook()`, which shifts the `currentStatePointer` once to the right, pointing to the previously undone state, and restores the address book to that state.

<div markdown="span" class="alert alert-info">:information_source: **Note:** If the `currentStatePointer` is at index `addressBookStateList.size() - 1`, pointing to the latest address book state, then there are no undone AddressBook states to restore. The `redo` command uses `Model#canRedoAddressBook()` to check if this is the case. If so, it will return an error to the user rather than attempting to perform the redo.

</div>

Step 5. The user then decides to execute the command `list`. Commands that do not modify the address book, such as `list`, will usually not call `Model#commitAddressBook()`, `Model#undoAddressBook()` or `Model#redoAddressBook()`. Thus, the `addressBookStateList` remains unchanged.

![UndoRedoState4](images/UndoRedoState4.png)

Step 6. The user executes `clear`, which calls `Model#commitAddressBook()`. Since the `currentStatePointer` is not pointing at the end of the `addressBookStateList`, all address book states after the `currentStatePointer` will be purged. Reason: It no longer makes sense to redo the `add n/David …​` command. This is the behavior that most modern desktop applications follow.

![UndoRedoState5](images/UndoRedoState5.png)

The following activity diagram summarizes what happens when a user executes a new command:

<img src="images/CommitActivityDiagram.png" width="250" />

#### Design considerations

**Aspect: How undo & redo execute:**

* **Alternative 1 (current choice):** Saves the entire address book.
  * Pros: Easy to implement.
  * Cons: May have performance issues in terms of memory usage.

* **Alternative 2:** Individual command knows how to undo/redo by
  itself.
  * Pros: Will use less memory (e.g. for `delete`, just save the person being deleted).
  * Cons: We must ensure that the implementation of each individual command is correct.

_{more aspects and alternatives to be added}_

### \[Proposed\] Data archiving

_{Explain here how the data archiving feature will be implemented}_

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
* uses a personal computer to manage client details and follow ups
* needs to review which client follow ups are due, upcoming, or overdue
* types quickly and prefers typed commands to mouse interactions 

**Value proposition**: Policy Harbour helps individual insurance agents keep client contact details and one pending next action per client organised in a keyboard driven desktop app, making follow ups easier to review and manage.

### User stories

Priorities: High (must have) - `* * *`, Medium (nice to have) - `* *`, Low (unlikely to have) - `*`

| Priority | As a …​                           | I want to …​                                                | So that I can…​                                   |
| -------- | --------------------------------- | ----------------------------------------------------------- | ------------------------------------------------- |
| `* * *`  | insurance agent                   | add a client or prospect with their contact details         | keep their information in one place               |
| `* * *`  | insurance agent                   | view my client list                                         | see the clients and prospects I manage            |
| `* * *`  | insurance agent                   | view a selected client's complete profile                   | review relevant context before contacting them    |
| `* * *`  | insurance agent                   | delete an incorrect client record                           | prevent inaccurate records from causing confusion |
| `* * *`  | insurance agent                   | access my saved information after restarting the app        | retain my work between sessions                   |
| `* * *`  | insurance agent                   | record a follow up and its due date                         | remember a promised action                        |
| `* * *`  | insurance agent                   | see follow ups that are due soon                            | plan my upcoming work                             |
| `* * *`  | insurance agent                   | see overdue follow ups                                      | address missed commitments promptly               |
| `* * *`  | insurance agent                   | mark a completed follow up as complete                      | know it no longer needs attention                 |
| `* * *`  | insurance agent                   | receive a clear explanation when I enter an invalid command | correct it without risking my records             |
| `* *`    | insurance agent                   | archive former or inactive clients                          | keep them out of my active portfolio              |
| `* *`    | insurance agent                   | filter clients using relevant criteria                      | focus on the records I need                       |
| `* *`    | insurance agent                   | identify clients I have not contacted recently              | avoid overlooking long-term relationships         |
| `* *`    | insurance agent                   | reschedule a client's follow up                             | reflect an agreed change to our plans             |
| `* *`    | insurance agent                   | record client preferences and circumstances                 | provide personalised, continuous service          |
| `* *`    | insurance agent                   | identify client records with incomplete information         | know which details need attention                 |
| `* *`    | insurance agent switching systems | import existing client contact information                  | avoid re-entering records manually                |
| `* *`    | insurance agent                   | identify clients with approaching policy related dates      | prepare for relevant upcoming events              |
| `* *`    | first time user                   | view concise usage guidance                                 | begin using the app without extensive training    |
| `*`      | potential user                    | explore realistic sample client records                     | understand how the app could support my work      |
| `*`      | insurance agent                   | categorise clients with tags                                | group related records                             |

### Use cases

(For all use cases below, the **System** is the `PolicyHabour` and the **Actor** is the `Insurance Agent `, unless specified otherwise)

**Use case: UC01 - Retrieve Client Profile**

**MSS**

1.  Agent chooses to view all client. 
2. System displays client list with index for each client.  
3. Agent selects the desired client from the list.  
4. System displays the client’s full contact details and pending follow-ups, if any. 

    Use case ends.

**Use case: UC02 - Add Client Profile**

**MSS**

1.  User chooses to add new client profile.  
2. System requests for details of new client.  
3. User enters the requested details.  
4. System request for confirmation.  
5. User confirms it. 
6. System saves new client record and displays the updated client list. 

    Use case ends.

**Extensions**

* 3a. Sysem detects an error in the entered data.  

   * 3a1. System request for correct data.  
   * 3a2. User enters new data. 

   Steps 3a1-3a2 are repeated until the data entered are correct.

   Use case resumes from step 4.


* a. At any time, User chooses to cancel the record. 

    * a1. System requests to confirm the cancellation.  
    * a2. User confirms the cancellation. 


      Use case ends. 

**Use case: UC03 - Delete Client Record**

**MSS**

1.  User performs Retrieve Client Profile (UC01) to select desired client.
2. User request to delete the desired client.
3. System deletes the desired client. record and any associated pending follow-up and saves the changes. 
4. System displays the updated client list.  

   User case ends 

**Use case: UC04 - Record Client Follow-Up**

**MSS**

1.  User performs Retrieve Client Profile (UC01) to select desired client
2. User chooses to add new Client Follow-up for desired client
3. System request for details of new followup 
4. User enters requested details 
5. System request for confirmation
6. User confirms it 
7. System records and saves the follow-up and displays the updated desired client information

    Use case ends.

**Extensions**

* 4a. Sysem detects an error in the entered data.  

   * 4a1. System request for correct data.  
   * 4a2. User enters new data. 

   Steps 4a1-4a2 are repeated until the   data entered are correct.

   Use case resumes from step 5.


* a. At any time, User chooses to cancel the record. 

    * a1. System requests to confirm the cancellation.  
    * a2. User confirms the cancellation. 


      Use case ends. 

**Use case: UC05 - Clear Client Follow-Up**

**MSS**

1.  User performs Retrieve Client Profile (UC01) to select desired client
2. User chooses the respective client follow-up to deleted 
3. System deletes the respective client follow-up and saves the changes 
4. System displays updated desired client information 

    Use case ends.

### Non-Functional Requirements

1. Should work on any _mainstream OS_ as long as it has Java `25` or above installed.

2. Should be able to hold up to 1000 persons without noticeable sluggishness in performance for typical usage.

3. A user with above-average typing speed for regular English text (i.e. not code or system administration commands) should be able to accomplish most tasks faster using commands than using the mouse.

4. Should respond to typical user commands such as adding, editing, deleting, searching, filtering, and viewing client records within 1 second under normal operating conditions.

5. Should preserve all successfully saved client information between application sessions, such that closing and reopening the application does not cause data loss.

6. Invalid commands or invalid user input should not modify existing client data and should result in a clear error message explaining how the input can be corrected.

7. Should be usable without an Internet connection for all core client-management functions.

8. Client data should be stored locally on the user's device and should not require a remote server for normal operation.

9. Should be distributable as a single executable JAR file without requiring a separate installation process.

10. The user interface should remain usable at common laptop screen resolutions, including `1280 × 720` and above.

11. Commonly used commands and command formats should remain consistent throughout the application to reduce the amount of relearning required from the user.

12. The application should be designed primarily for keyboard-based interaction, while still allowing mouse interaction where appropriate.

_{More to be added}_

### Glossary

* **Client record**: The contact details stored for a client or prospective client
* **Follow up**: The next action an agent plans to take for a client, with a due date and description
* **Pending**: A follow up that has been recorded and not yet cleared. It stays pending after its due date passes
* **Index**: The row number of a client in the list currently displayed.
* **Overdue**: A pending follow up whose due date is before today
* **Due today**: A pending follow up whose due date is today
* **Upcoming**: A pending follow up whose due date is after today


--------------------------------------------------------------------------------------------------------------------

## **Appendix: Instructions for manual testing**

Given below are instructions to test the app manually.

<div markdown="span" class="alert alert-info">:information_source: **Note:** These instructions only provide a starting point for testers to work on;
testers are expected to do more *exploratory* testing.

</div>

### Launch and shutdown

1. Initial launch

   1. Download the JAR file and copy it into an empty folder.

   1. Double-click the JAR file.<br>
      Expected: The GUI opens with a set of sample contacts. The window size may not be optimal.

1. Saving window preferences

   1. Resize the window to an optimal size. Move the window to a different location. Close the window.

   1. Relaunch the app by double-clicking the JAR file.<br>
       Expected: The most recent window size and location are retained.

1. _{ more test cases …​ }_

### Client creation, duplicates and deletion

Run each scenario in a fresh writable test folder with an empty client dataset and Java 25. Use synthetic data. Inspect `list` before each indexed command; preceding deletions change row numbers. These are manual procedures to run against the built JAR, not a claim that GUI testing has already been performed.

1. Contact normalization and tags:
   1. Enter `add n/  Rachel  Lim  p/001 e/Rachel@example.com a/新加坡 🏠 t/HEALTH t/health t/active`.
   1. Expected: the client is added, the stored name retains the two internal spaces, phone remains `001`, email retains case and address retains Unicode. Feedback ends in `Tags: [active] [health]`.
   1. Close and relaunch. Expected: the same stored contact values remain.
1. Duplicate partition (new folder):
   1. Add `add n/Rachel Lim p/001 e/Rachel@example.com a/Blk 1`.
   1. Add `add n/Rachel Lim p/002 e/other@example.com a/Blk 2`. Expected: both clients exist.
   1. Try `add n/RACHEL  LIM p/001 e/new@example.com a/Blk 3`. Expected: `This client already exists in Policy Harbour.`; list unchanged.
   1. Try `add n/rachel lim p/003 e/RACHEL@EXAMPLE.COM a/Blk 3`. Expected: the same duplicate error; list unchanged.
   1. Add `add n/John Tan p/001 e/Rachel@example.com a/Blk 3`. Expected: allowed because the name differs.
1. Validation order (new folder):
   1. Try `add n/Bad& n/Rachel p/12 e/bad`. Expected: format error and the exact add usage because address is missing, before any repeated-prefix or field error.
   1. Try `add a/ t/! e/bad p/12 n/Bad&`. Expected: the name constraint, before phone/email/address/tag errors.
   1. Correct the name, phone, email and address in turn. Expected: each remaining constraint is reported in that order; no client is added until all values are valid.
1. Displayed indices and delete errors (new folder):
   1. Add the two Rachel records from the duplicate scenario. Enter `find Rachel`, then `delete 01`.
   1. Expected: only the first displayed Rachel is removed; the other record remains and feedback includes `Tags: None`.
   1. Try `delete 0 extra`. Expected: `Invalid command format!` plus `Usage: delete INDEX` on the next line.
   1. Try `delete 0` and `delete 2147483648`. Expected: `Index must be a positive integer from 1 to 2147483647.`
   1. Try `delete 2147483647`. Expected: `The client index provided is invalid.` All rejected cases preserve the list and data.
1. Pending-order targeting (new folder):
   1. Add the two Rachel records. Record a follow-up for row 1 at device-today + 2 days and row 2 at device-today + 1 day using `followup INDEX d/DATE m/ACTION`.
   1. Enter `followups`, then `delete 1`. Expected: the second-added Rachel (earlier due date) is removed with its action; pending mode remains active with the other Rachel.

Injected I/O failure and non-transitive replacement conflicts are automated in `ClientRecordCommandsTest` and `UniquePersonListTest`. A save failure must leave both the displayed state and saved bytes unchanged. The profile, UI layout and startup warning require their owners' separate manual checks.

### Saving data

1. Dealing with missing/corrupted data files

   1. _{Explain how to simulate missing or corrupted data files and state the expected behavior.}_

1. _{ more test cases …​ }_
