---
layout: page
title: User Guide
---

This guide describes the PolicyHarbor v1.5 client-management workflows. The published v1.4 JAR still uses displayed row indices and does not support the `edit` command. Use a v1.5-compatible build when following the permanent Client ID instructions below.

* Table of Contents
{:toc}

--------------------------------------------------------------------------------------------------------------------

## Quick start

1. Ensure that Java `25` is installed on your computer.<br>
   **Mac users:** Ensure you have the precise JDK version prescribed [here](https://se-education.org/guides/tutorials/javaInstallationMac.html).

1. Download `PolicyHarbor.jar` from the v1.5 (when published) or a later release on the [team's releases page](https://github.com/AY2627S1-CS2103T-W13-3/tp/releases). Until then, use a JAR built from the current `master` source. **Do not use the v1.4 JAR with this guide:** its command targeting and supported features differ.

1. Copy the file to the folder where you want PolicyHarbor to store its data.

1. Open a terminal, `cd` to the folder containing the JAR file, and run `java -jar PolicyHarbor.jar`.<br>
   The client list is empty on first launch. Add a client to get started.

1. Enter each command below separately, pressing Enter or clicking **Run** after each one:

   ```text
   add n/Rachel Lim p/91234567 e/rachel@example.com a/Blk 123 t/health
   view 1
   followup 1 d/2099-12-15 m/Discuss renewal options
   followups
   ```

   Rachel's contact details appear in the profile pane. Her action appears in the pending summary with an `UPCOMING` badge. The example uses a future date; for your own actions use their due date in `YYYY-MM-DD` format, including a past date for overdue work.

1. Close the window and run the JAR again from the same folder. Rachel and her follow-up are restored; enter `view 1` to reopen her profile.

1. Refer to the [Features](#features) section below for details of each command.

--------------------------------------------------------------------------------------------------------------------

## Features

<div markdown="block" class="alert alert-info">

**:information_source: Notes about the command format:**<br>

* Words in `UPPER_CASE` are the parameters to be supplied by the user.<br>
  For example, in `add n/NAME`, replace `NAME` with a value such as `John Doe`.

* Items in square brackets are optional.<br>
  For example, `n/NAME [t/TAG]` can be used as `n/John Doe t/friend` or as `n/John Doe`.

* Items followed by `…`​ can appear zero or more times.<br>
  For example, `[t/TAG]…​` may be omitted, or written as `t/friend` or `t/friend t/family`.

* Parameters can be in any order.<br>
  For example, if the command specifies `n/NAME p/PHONE_NUMBER`, `p/PHONE_NUMBER n/NAME` is also acceptable.

* Extraneous ordinary text after `list` and `followups` is ignored.<br>
  For example, `list 123` is interpreted as `list`. Client-ID commands such as `view` require their stated arguments.

* If you are using a PDF version of this document, be careful when copying and pasting commands that span multiple lines as space characters surrounding line-breaks may be omitted when copied over to the application.

</div>

### Reading the workspace

[![Pending follow-ups and the selected client profile](images/Ui.png)](images/Ui.png)

*PolicyHarbor workspace with fictional clients. Open the image to enlarge it.*

The light dashboard keeps the command field, **Run** button and feedback strip at the top. Enter and Run submit the same current text once. A successful command clears the input; a rejected command keeps it for correction, marks the field and shows **Error** above the complete message. Editing clears the field's error style; the feedback remains until the next command. Long feedback wraps and can be scrolled, selected and copied.

* **Left: the client table.** The `Client ID` column shows each client's permanent ID. Use that ID with `view`, `edit`, `delete` and `followup`, even after searching or reordering the list. The table shows all clients after `list`, matches after `find`, and clients with follow-ups after `followups`. Columns cannot be sorted independently of command order. Narrow tables scroll horizontally.
* **Upper right: the selected client profile.** Use `view ID` to open a client's complete details. Long fields and tags wrap; scroll within the profile to read everything.
* **Lower right: all pending follow-ups.** This summary always includes every client's action, even when the left table is filtered. It groups actions under `OVERDUE`, `DUE TODAY` and `UPCOMING`, with due-date order and insertion-order ties within each group. Use the client's permanent ID shown in the workspace when entering commands; a position in this summary is not an ID.

Drag the main divider to change the table/sidebar widths. Drag the divider between the profile and pending summary to change their heights. Each right-hand panel scrolls independently.

On a small or highly scaled screen, such as 1280x720 at 150% scaling, a right-hand panel can be shorter than one follow-up card. Scroll within it or drag the divider between the panels to give it more room; every card and the whole profile remain reachable.

Long values without spaces, such as a long email address, wrap onto extra lines in the table, profile and pending summary instead of being cut off.

The `Client ID` column shows permanent IDs, which may contain gaps after deletions and do not change with filtering or due-date ordering.

The table shows Client ID, name, phone and email. In `followups` mode it also shows due date and status; read the full action in the sidebar or selected profile.

| Badge | Meaning |
| --- | --- |
| `OVERDUE` | The due date is before today. |
| `DUE TODAY` | The due date is today. |
| `UPCOMING` | The due date is after today. |

Follow-up statuses refresh automatically using the current date when PolicyHarbor starts, when the main window regains focus and once every minute while the application is running. You do not need to enter a command just to refresh the statuses.

In `followups` mode the table is ordered by due date, with insertion-order ties. Client IDs stay unchanged when rows move. The selected canonical client is marked `VIEWING` in the table and, if they have an action, in the summary. Clicking or highlighting either view does not select a profile or execute a command.

An empty table says `No clients to display.` or, in `followups` mode, `There are no pending follow-ups.`. An empty global summary has its own empty message. Finding no matches can leave an empty table alongside a populated summary; this is expected.

The footer identifies the local data file. When its path is shortened, hover over it to read the full path. The Help menu or `F1` opens the guide link, which can be selected or copied with **Copy link**; `Esc` closes Help.

If saved data could not be loaded at startup, a warning stays above the workspace for the whole session, whatever command you run. Close the app and restore a valid saved copy to clear it. An ordinary command error does not dismiss that warning.

### Keyboard workflow and error correction

Suppose Alice Morgan has Client ID `17` and Bob Chen has Client ID `42`. To review Alice's next action, enter each command separately:

1. `find Alice` narrows the table. If several clients match, identify Alice by her contact details and check the `Client ID` column.
2. `view 17` opens Alice's profile so you can confirm the intended client before changing a follow-up.
3. `followup 17 d/2099-12-15 m/Call about renewal` sets Alice's action. Replace the example date with the action's due date in `YYYY-MM-DD` format; past dates are allowed for overdue work.
4. `find Bob` switches the table to Bob and clears Alice's profile. Alice's action remains in the global pending summary.
5. `followups` shows pending clients in due-date order. Alice's ID remains `17` regardless of her row position.

If you submit `view 0` with Enter, the rejected line remains in the command field. Correct its ID to `17` and press Enter to open Alice without clicking the field again. If Alice is not visible in the current list, `view 17` returns to the full list to show her.
You can also reach **Run** with Tab and activate it with Space. After a rejected Run submission, typing focus returns to the field; if the retained line is selected, type the corrected command to replace it.

### Supported commands

Command words are case-sensitive. The supported words are `add`, `delete`, `edit`, `view`, `find`, `list`, `followup` and `followups`.

Standalone `clear`, `help` and `exit` are unsupported and return exactly `Unknown command.`, even when saved data could not be loaded. Use `followup ID clear` to clear one client's follow-up; this remains supported. Close the application with the window's close button. The GUI Help menu and `F1` shortcut remain separate from typed commands.

Use one command line and ordinary spaces. Tabs, control characters and line separators are rejected before command recognition or argument validation. Blank input returns `Enter a command.`. For example, `VIEW 1` returns `Unknown command.` because `view` must be lowercase.

If pasted text contains a tab, control character or line separator, the whole paste is rejected immediately with an error in the feedback area. Your existing command text and selection stay unchanged, and no command runs. Pressing `Enter` or clicking `Run` still submits your unchanged draft. To use the copied text, remove its invalid characters and paste again before submitting.

### Adding a client: `add`

Adds a client with their contact details and optional tags. A successful add keeps the current list mode and search filter; the new client has no follow-up yet. A matching client appears in search results, while a non-matching client stays hidden. An actionless client stays out of the pending list. The selected client remains selected if still visible.

Format: `add n/NAME p/PHONE_NUMBER e/EMAIL a/ADDRESS [t/TAG]...`

Supply each required prefix exactly once. The prefixes may appear in any order and must be preceded by an ordinary space. Tags may appear repeatedly and have no count limit. Outer ordinary spaces are removed from field values.

| Field | Accepted values |
| --- | --- |
| Name | Starts with a letter or digit; supports letters from any language, ASCII digits, combining accents, ordinary spaces and `/ - ' @ .`. Original spelling and punctuation are preserved. |
| Phone | At least three digits `0`–`9`, with an optional leading `+` and single spaces or hyphens between digit groups, such as `+65 9123 4567`. The entered format and leading zeroes are retained. |
| Email | Before one `@`, ASCII letters and digits may be joined by `+`, `_`, `.` or `-`. Consecutive `+`, `_` and `-` are accepted, but periods must separate alphanumeric characters. Domain labels may contain consecutive hyphens (including `xn--` Punycode labels), but must start and end with a letter or digit. The final label must contain two adjacent letters or digits. Stored case is preserved. |
| Address | Non-empty single-line text, including punctuation, non-English text and emoji. |
| Tag | One or more English letters or digits. Tags are stored in lowercase; repeated case variants become one tag. |

Use one command line and ordinary spaces. Tabs, control characters and line separators are rejected. There are no additional contact-length limits. Outer ordinary spaces (U+0020) are trimmed without changing the characters between them.
For example, `n/  Alice  Tan  ` stores `Alice  Tan`, retaining the two spaces between the name words.
Long internal space runs do not add a new name or address length restriction; the same field rules still apply.

Address input and recovery:

* An address must include text beyond spaces and invisible formatting characters. Non-breaking spaces, full-width spaces or zero-width spaces alone are rejected.
* Non-English text and emoji are supported. An address containing an invalid Unicode character sequence is rejected; correct the address and submit it again.
* When editing the saved JSON directly, use complete characters. An incomplete Unicode escape pair, such as a lone `\uD800` in an address, causes the whole client file to be rejected at startup, even if the JSON syntax is otherwise correct.
* In that case, the app shows the saved-data warning and blocks client changes to protect the file. Close the app, back up the file, correct the affected address or restore a valid backup, then restart. Editing the file while the app is open does not clear the warning.

Names such as `Ravi s/o Kumar`, `Priya d/o Raj`, `Mary-Ann Lim`, `Sean O'Brien`, `Tan Ah Kow @ Tan Ah Kau` and `Dr. Lim` can be entered as written. For example: `add n/Ravi s/o Kumar p/91110010 e/ravi@example.com a/Blk 1`.

A client is a duplicate when **their name matches and either their phone or email matches** an existing client. Names are compared ignoring case, Unicode composition and repeated ordinary spaces; phones are compared by digits (so `+65 9123 4567` and `65-9123-4567` match), and emails ignore case. Address, tags and follow-up do not affect this check. Two clients may share a name if both their phone and email differ. Different names may share contact details. Duplicate checks include clients hidden by the current filter.

Examples, entered into a list without these clients:

* `add n/Rachel Lim p/001 e/Rachel@example.com a/Blk 123 t/HEALTH t/active t/health` adds Rachel and displays `Tags: [active] [health]`.
* `add n/Rachel Lim p/002 e/other@example.com a/新加坡 🏠` adds a second Rachel because both contact details differ.
* `add n/RACHEL  LIM p/001 e/new@example.com a/Another address` fails with `This client already exists in PolicyHarbor.` because the first Rachel has the same normalized name and phone.

Success text includes the new client's permanent ID, name, phone, email, address and sorted lowercase bracketed tags separated by spaces. Without tags, it ends in `Tags: None`. IDs identify clients rather than displayed rows, so gaps can remain after deletions. For example:

```text
Client ID 1 added: Rachel Lim; Phone: 001; Email: Rachel@example.com; Address: Blk 123; Tags: [active] [health]
```

If the client ID range is exhausted, the add is rejected with `No more clients can be added because the client ID limit has been reached.` The next ID is not consumed when a duplicate is rejected or a save fails.

A missing required prefix or extra text before the first prefix produces:

```text
Invalid command format!
Usage: add n/NAME p/PHONE_NUMBER e/EMAIL a/ADDRESS [t/TAG]...
```

If several fields are invalid, correct the reported error and submit the command again. Fields are checked in the order name, phone, email, address, then tags. An unrecognized prefix inside a field remains literal text: for example, `z/unit` inside an address is allowed, while the same text inside a phone makes the phone invalid.

Changes are saved before they appear in the list. If saving fails, the previous data and list remain and the result says `Could not save to data/addressbook.json. No changes were kept. Check that the data folder is writable and has free space, then try again.` If saved data could not be loaded at startup, adding clients is blocked to protect that file.

### Listing all clients: `list`

Shows all clients in insertion order and returns `Listed all clients.`. The open profile stays selected if its client is in the list.

Format: `list`

### Selecting a client: `view`

Selects a client using their **permanent Client ID**, not the row's position in the current list. For example, if Rachel Lim has Client ID `17`, the feedback is `Showing client ID 17: Rachel Lim`.

Format: `view ID`

Supply exactly one ID containing ASCII digits `0`–`9`, with a value from `1` to `2147483647`. Leading zeroes are allowed: `view 017` and `view 17` target the same client. Read the ID from the `Client ID` column or the feedback from `add`. IDs are permanent, so deleting clients, filtering results or sorting follow-ups does not renumber the remaining IDs.

`view` resolves the ID against **all saved clients**, including clients hidden by the current search or pending list. If the client is hidden, the app returns to the full list and opens their profile. If already visible, the current list mode and order remain unchanged. Viewing never saves or modifies client data.

For example, given Alice (ID 2) and Rachel (ID 17):

* `find Rachel` shows Rachel as the first and only result; `view 17` opens Rachel, **not** `view 1`.
* `followups` can reorder the visible rows; `view 17` still opens Rachel.
* `view 2` opens Alice even if the current filter hides her; the full list becomes visible.

| Input problem | Exact result |
| --- | --- |
| Missing ID or extra tokens, such as `view 0 extra` | `Invalid command format!` followed by a newline and `Usage: view ID` |
| Invalid ID token, such as `0`, `-1`, `1.5`, `abc` or `2147483648` | `Client ID must be a positive integer from 1 to 2147483647.` |
| Valid ID that does not exist, such as `view 1` when only IDs 2 and 17 exist | `No client with ID 1.` |

Missing or extra tokens take priority over ID syntax. A failed view preserves the previous selection, list and displayed date. In a failed-load session, `view` is still allowed, but an unknown ID reports `No client with ID 1.` (for example, when entering `view 1`) because no clients were loaded.

On success, `view` opens the selected client's full profile in the right pane and marks the corresponding row `VIEWING`.

[![Full client list with Rachel Lim's contact details and follow-up](images/ClientProfile.png)](images/ClientProfile.png)

### Finding clients: `find`

Searches all clients, including those hidden by a prior search or pending list, and displays matches in insertion order. Each client appears once. Search leaves pending mode and returns `COUNT client(s) listed!`, for example `2 client(s) listed!` or `0 client(s) listed!`.

Use either the existing bare search or one explicit field:

```text
find KEYWORD [MORE_KEYWORDS]...
find n/NAME_PART
find p/PHONE_PART
find t/TAG
```

* Bare keywords retain the existing behavior: each keyword matches a complete name word or a complete phone number's digits. Matching is OR-based, so `find Rachel 81234567` returns clients matching either keyword. Repeated keywords do not duplicate results. Leading zeroes are significant; `find 00123456` matches the full number, while `find 0012` does not.
* `find n/rac` matches a case-insensitive name substring, including partial words. Unicode composed and decomposed spellings match, and runs of ordinary spaces are collapsed. For example, `find n/rachel lim` matches `Rachel   Lim`.
* `find p/9123` matches a phone-number digit substring. The search ignores a stored number's allowed display formatting (`+`, spaces and hyphens) and retains leading zeroes. A partial query may contain fewer than three digits.
* `find t/renewal` matches the exact tag, ignoring case. It does not match a tag substring such as `new`.
* Email and address are not searched. Bare numeric keywords may also match a whole name word. Quotes and punctuation remain literal within values.
* Use exactly one explicit field. Do not combine explicit fields or put bare keywords before one; for example, `find n/Rachel p/9123` is invalid. Known `n/`, `p/` and `t/` prefixes are reserved in explicit queries.

For a dataset containing Rachel Lim and Amanda Lee, `find rachel AMANDA` returns both in insertion order. `find Lim` matches the complete name word `Lim`; `find Rach` returns no matches in bare mode, while `find n/rach` finds Rachel.

If Bob Baker's phone number is `81234562`, `find 81234562` finds Bob and any other clients with that full number or name word. `find p/234` matches the digit substring in Bob's phone.

Empty values, repeated or mixed explicit fields, and text before an explicit field produce the usage error:

```text
Invalid command format!
Usage: find KEYWORD [MORE_KEYWORDS]... | find n/NAME_PART | find p/PHONE_PART | find t/TAG
```

Search does not change client data or save it. The selected client remains selected only while present in the results; searching them out clears selection. A later search does not automatically select them again.

For example, search for Rachel with `find n/rac`, then use `view 17` and `edit 17 p/+65 9234 5678`. The ID continues to target Rachel even if a later filter hides her; editing preserves her ID and pending follow-up.

### Editing a client's details: `edit`

Changes one or more contact fields for an existing client, identified by their permanent Client ID. The client's ID and pending follow-up are preserved.

Format: `edit ID [n/NAME] [p/PHONE] [e/EMAIL] [a/ADDRESS] [t/TAG]...`

At least one field must be provided. Omitted fields keep their existing values. The same name, phone, email, address and tag validation rules used by `add` apply. Each of `n/`, `p/`, `e/` and `a/` may appear only once; repeat `t/` to supply multiple tags. Supplying tags replaces the client's **entire** tag set instead of adding to it; use only `t/` with an empty value to clear all tags.

For example, if Rachel's Client ID is `17`:

* `edit 17 p/+65 9123 4567` changes only her phone number.
* `edit 17 a/Blk 456 t/renewal t/priority` updates her address and replaces all her tags with `priority` and `renewal`.
* `edit 17 t/` removes all her tags.

A successful edit reports `Client ID 17 updated: ` followed by the client's updated contact details and tags. `edit 17` without a field reports `At least one field to edit must be provided.`; an unknown ID reports `No client with ID 17.` when ID 17 does not exist. An edit that would duplicate another client is rejected with `This person already exists in PolicyHarbor.` Invalid input or a failed save leaves client data unchanged.

Editing retains the current list mode. Search results are re-evaluated, so a client may disappear from a filtered list when their details no longer match. The edited client's permanent ID does not change.

### Deleting a client: `delete`

Deletes the client identified by their **permanent Client ID**, even if that client is hidden by the current search or pending list. Deleting a client also removes their follow-up. The active list mode remains unchanged; displayed rows may change, but other clients keep their IDs.

Format: `delete ID`

Supply exactly one ID containing ASCII digits `0`–`9`, with a value from `1` to `2147483647`. Leading zeroes are allowed: `delete 017` targets Client ID 17. The ID must belong to an existing client; it is not a position in the displayed list.

Examples:

* `list` followed by `delete 17` deletes the client whose ID is 17, regardless of their row position.
* `find Rachel` followed by `delete 17` deletes Rachel only if her ID is 17. Do not use `delete 1` merely because she appears first.
* `followups` followed by `delete 2` deletes Client ID 2 even if that client is currently hidden. Check the ID before deleting.

Success text includes the deleted client's ID, contact details and sorted tags, without their follow-up. For example:

```text
Deleted client ID 17: Rachel Lim; Phone: 001; Email: Rachel@example.com; Address: Blk 123; Tags: [active] [health]
```

| Input problem | Exact result |
| --- | --- |
| Missing ID or extra tokens, such as `delete 0 extra` | `Invalid command format!` followed by a newline and `Usage: delete ID` |
| Invalid ID token, such as `0`, `-1`, `abc`, `1.5` or `2147483648` | `Client ID must be a positive integer from 1 to 2147483647.` |
| Valid ID not found in the saved client list | `No client with ID 17.` (if ID 17 does not exist) |

Shape errors take priority over ID syntax. A rejected delete leaves data, the current list and date unchanged. A save failure also keeps the previous state and reports `Could not save to data/addressbook.json. No changes were kept. Check that the data folder is writable and has free space, then try again.` Deletion is blocked if saved data could not be loaded at startup.

### Keeping the same client profile open

Use `view ID` to select a client by permanent Client ID. If that client is currently hidden, the full list opens first. The right pane then updates with that client's details.

Opening a different client starts their profile at the top, so their name and contact details are visible first.
This also applies to separate clients who share a name and to reopening a profile after it closes.
Viewing the same client again, changing their follow-up or refreshing its status keeps your reading position, as far as the remaining content allows.

The profile shows the selected client's name, phone, email, address and tags. Tags appear as chips in lowercase alphabetical order, or as `No tags` when there are no tags. A pending follow-up shows its date, full action text and `OVERDUE`, `DUE TODAY` or `UPCOMING` status. With no follow-up it shows `Follow-up: None`; with no selected client it shows `No client selected.`

Your open profile will follow the client, even when their row position changes. Two clients with the same name will have separate profiles. Only a successful `view` command will open a profile; adding a client or highlighting a row will not open one automatically.

| Change | Result for your open profile |
| --- | --- |
| Show the full list or add a client | Keep the same client open. |
| Search or show pending follow-ups | Keep the client open only if they remain in the displayed list; otherwise close the profile. |
| Record or replace that client's follow-up | Update their profile and keep it open, even if the pending list moves their row. |
| Clear that client's follow-up in the full list or search results | Keep their profile open without a follow-up. |
| Clear that client's follow-up in the pending list | Close their profile because the client leaves the pending list. |
| Delete the open client | Close their profile; do not open another client automatically. |
| Delete another client or change another client's follow-up | Keep the same client open despite any row reordering. |

Once a profile closes because its client leaves the list, returning to the full list will not reopen it. You will need another successful `view` command. Reopening the application also starts without an open profile. A failed command or failed save leaves the previous profile unchanged.

### Recording or replacing a follow-up: `followup`

Records one pending action for a client identified by their **permanent Client ID**, or replaces that client's existing action. The ID is resolved against all saved clients, including hidden clients.

Format: `followup ID d/DATE m/DESCRIPTION`

Use an existing calendar date in `YYYY-MM-DD` format, with a year from `0001` to `9999`. A new or replacement follow-up may be due in the past, today or in the future. Use the original due date to record overdue carry-over work; a date before today displays as `OVERDUE`. An already saved follow-up can also become overdue as time passes.

Descriptions support multilingual text, accents, punctuation and emoji, such as `followup 17 d/2099-12-15 m/续保电话`. They must contain 1–200 Unicode code points after outer ordinary spaces are removed. A letter with a combining accent or a joined emoji can contain several code points. Original spelling and internal spaces are preserved. Notes must include visible text or a symbol; whitespace, formatting characters or combining marks alone do not form a valid note. Controls, line breaks and invalid or unassigned Unicode characters are rejected. The existing reserved-prefix rules for `d/` and `m/` still apply.

Within a description, `d/` and `m/` immediately preceded by an ordinary space are reserved command prefixes. They cause a duplicate-field error, even when the description is wrapped in quotation marks; quotes do not escape prefixes. For example, `followup 17 d/2099-12-15 m/Confirm d/m/y format` is rejected. Use `followup 17 d/2099-12-15 m/Confirm day/month/year format` instead. Slash text without that preceding space, such as `ref:d/m/y`, is allowed.

For example, `followup 17 d/2099-12-15 m/Discuss renewal options` records Rachel's action if her ID is 17, regardless of her position in the table. Success reports `Follow-up for client ID 17: Rachel Lim recorded for 2099-12-15.`, or `Follow-up for client ID 17: Rachel Lim updated to 2099-12-15.` when replacing an action. The list and profile refresh immediately. In pending mode, changing the due date may move the client to another row without changing their ID.

An unknown ID produces `No client with ID 17.` when ID 17 does not exist, before validating the new date or description in command execution. Invalid input and failed saves leave the previous follow-up and list unchanged.

### Clearing a follow-up: `followup ID clear`

Removes the pending action from the client identified by a permanent ID. For Rachel (ID 17), success reports `Follow-up for client ID 17: Rachel Lim cleared.` If the client has no pending action, the result is `This client has no pending follow-up.` If the ID does not exist, the result is `No client with ID 17.`. In pending mode, a cleared client disappears from the list; in the full list or search results, the client remains visible. A failed save preserves the action.

### Showing pending follow-ups: `followups`

Shows only clients with pending actions, ordered by ascending due date. Clients with the same due date retain their original insertion order. The feedback is `Pending follow-ups`, or `There are no pending follow-ups.` if none remain. Use the **permanent Client IDs** shown in the table for `view`, `edit`, `delete` and singular `followup` commands. Positions in this reordered list are not command targets. `list` returns to the full list, and `find` searches all clients even if they were hidden in pending mode.

### Saving the data

PolicyHarbor automatically saves successful client and follow-up changes. `list`, `find`, `view` and `followups` do not rewrite the data file. You do not need a save command. The selected profile is session-only.

In v1.5, if a change cannot be saved because the data folder is unavailable, the change is not kept in the app or the saved file. Restore access to the folder, then submit the same command again. The app will report whether that retry succeeds.

If `data/addressbook.json` is a symbolic link, PolicyHarbor refuses to save through it. When the link is present at either save check, the link and its target remain unchanged, and the app explains the problem. Do not change the data path externally while the app is running; concurrent path changes are not synchronized. Close the app, replace the link with a regular data file, then retry the command. Back up the target before replacing the link.

Close the app before copying `data/addressbook.json` to a backup location. Restore a backup while the app is closed, then restart from the JAR folder.

### Editing the data file

Client data is saved automatically as `data/addressbook.json` relative to the folder from which you run `java -jar PolicyHarbor.jar`. Advanced users can update it directly by editing that file.

Save this file as UTF-8 when editing it in another program. UTF-8 with a leading byte-order mark (BOM), as offered by some editors, is also accepted. In v1.5, an invalid byte sequence cannot be safely interpreted as a client's name, address or follow-up, so PolicyHarbor refuses to load the file and protects it from changes.

If you see the saved-data warning after editing the file, close PolicyHarbor and back up the file before repairing its encoding or restoring a known-good copy. Restart the app to load the repaired file; a command in the protected session cannot repair it.

<div markdown="span" class="alert alert-warning">:exclamation: **Caution:**
If the data file contains an invalid client or duplicate clients, PolicyHarbor starts with an empty displayed list and blocks client changes. It does not overwrite or partly load the invalid file. Correct the file or restore a backup, then restart the app. Back up the file before editing it.<br>
Repeated JSON property names within the same object are also rejected, even if their values match. For example, keep only one `persons` property in the file's outer object and one `description` property in each follow-up object. Compare the repeated values with your backup before choosing which value to keep; then restart the app. Different client objects may each have their own `name`, `phone` and other properties.<br>
Names, addresses, phone numbers, email addresses and tags follow the same validation rules used when adding a client. Outer ordinary spaces are removed; tags are stored in lowercase without duplicates. Missing tags mean no tags. Existing valid overdue follow-ups are preserved. Unrecognised field names are rejected so that misspellings such as `tag` or `followup` cannot silently discard data. Use `tags` and `followUp` exactly as shown in the Developer Guide.
</div>

--------------------------------------------------------------------------------------------------------------------

## FAQ

**Q**: How do I transfer my data to another computer?<br>
**A**: Close the app on both computers. Place the JAR in a writable folder on the new computer and create a `data` subfolder. Copy the old `data/addressbook.json` into it, backing up any existing destination file first. Run the JAR from the new folder with Java 25.

--------------------------------------------------------------------------------------------------------------------

## Troubleshooting and known limitations

* **Cannot save changes:** the error identifies the destination file. No changes from the failed command are kept. Check that its `data` folder is writable, that a file is not blocking the folder, and that the disk has free space. Restore access before retrying the retained command. If moving the app, close it first and copy both the JAR and its data to a writable folder; do not run with administrator privileges just to bypass permissions.

* **If another PolicyHarbor window is already running in this folder:** use that window, or close it before launching again. Only one instance can use a folder at a time; a second launch stops before loading or changing client data and preferences. Separate folders can hold independent datasets.
* **If PolicyHarbor cannot lock the folder:** run it from a writable folder and check that `.policyharbor.lock` is a regular file. This marker normally remains after the app closes; its presence alone does not prevent startup. Do not delete or replace it while the app is running. Closing the app or terminating its process releases the operating-system lock.
* **If the app does not start on macOS:** follow the [SE-EDU setup guide](https://se-education.org/guides/tutorials/javaInstallationMac.html) to install and activate Zulu Java 25 with JavaFX. Check `java -version` and `which java` in the same terminal, then run `java -jar PolicyHarbor.jar`. A different active Java installation may cause JavaFX loading errors.
* **Saved-data warning:** close the app and back up `data/addressbook.json` before repairing invalid data or restoring a valid backup. If startup could not access the data folder, restore its access permissions instead of replacing valid client data, then restart to reload.
  The app stays protected for the current session even after access returns; query commands cannot remove the warning.
  Only a confirmed missing data file starts an empty, writable client list.
* **Diagnostic log unavailable:** if `Unable to create diagnostic log file; continuing with console logging.` appears in the launch terminal, PolicyHarbor continues logging there. Check that the launch folder is writable and that `policyharbor.log.0` is not a directory, then restart to restore file logging. Client-data and preference writes still require their own writable files and folders.
* **MVP scope:** one pending follow-up per client. Follow-up history, notifications and archiving are not provided. Contact details can be updated with `edit`.


* **Window restoration:** PolicyHarbor restores the window on its saved screen when that screen is available. If the saved screen is unavailable, startup fits the window inside the primary screen's usable area. You do not need to delete `preferences.json` when disconnecting a secondary screen.
* **If the workspace feels cramped on a small or highly scaled display:** maximize the window, then drag the main divider and the sidebar divider towards the area you are reading. The command box and feedback stay at the top of the window; the table and both right-hand panels scroll to show the rest.
* **Help window:** use the Help menu or `F1` to open the Help window. If a minimised Help window does not reappear, restore it from your operating system's dock or taskbar.

--------------------------------------------------------------------------------------------------------------------

## Command summary

Action | Format, Examples
-------- | ------------------
**Add** | `add n/NAME p/PHONE_NUMBER e/EMAIL a/ADDRESS [t/TAG]...` <br> e.g., `add n/James Ho p/22224444 e/jamesho@example.com a/123, Clementi Rd, 1234665 t/friend t/colleague`
**Delete** | `delete ID`<br> e.g., `delete 17`
**Edit** | `edit ID [n/NAME] [p/PHONE] [e/EMAIL] [a/ADDRESS] [t/TAG]...`<br> e.g., `edit 17 p/+65 9123 4567`
**View** | `view ID`<br> e.g., `view 017`
**Find** | `find KEYWORD [MORE_KEYWORDS]...` or `find n/NAME_PART`, `find p/PHONE_PART`, `find t/TAG`<br> e.g., `find James 81234562`, `find n/jam`, `find p/8123`, `find t/renewal`
**List** | `list`
**Record or replace follow-up** | `followup ID d/DATE m/DESCRIPTION`
**Clear follow-up** | `followup ID clear`
**Pending follow-ups** | `followups`

Standalone `clear`, `help` and `exit` return `Unknown command.`. Use the window's close button to exit.
