---
layout: page
title: User Guide
---

AddressBook Level 3 (AB3) is a **desktop application for managing contacts, optimized for use through a Command Line Interface (CLI)** while retaining the benefits of a Graphical User Interface (GUI). If you type quickly, AB3 can help you manage contacts faster than traditional GUI applications.

* Table of Contents
{:toc}

--------------------------------------------------------------------------------------------------------------------

## Quick start

1. Ensure that Java `25` or later is installed on your computer.<br>
   **Mac users:** Ensure you have the precise JDK version prescribed [here](https://se-education.org/guides/tutorials/javaInstallationMac.html).

1. Download the latest `.jar` file from [here](https://github.com/se-edu/addressbook-level3/releases).

1. Copy the file to the folder you want to use as the _home folder_ for your AddressBook.

1. Open a terminal, `cd` to the folder containing the JAR file, and run `java -jar addressbook.jar`.<br>
   A GUI similar to the one below should appear in a few seconds. Note how the app contains some sample data.<br>
   ![Ui](images/Ui.png)

1. Type a command in the command box and press Enter to execute it. For example, type **`list`** and press Enter to show the full client list.<br>
   Some example commands you can try:

   * `list` : Lists all contacts.

   * `add n/John Doe p/98765432 e/johnd@example.com a/John street, block 123, #01-01` : Adds a contact named `John Doe` to the Address Book.

   * `delete 3` : Deletes the 3rd contact shown in the current list.

   * `view 1` : Selects the first client shown, if the list is non-empty.

   Close the application using the window's close button.

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
  For example, `list 123` is interpreted as `list`. Indexed commands such as `view` require their stated arguments.

* If you are using a PDF version of this document, be careful when copying and pasting commands that span multiple lines as space characters surrounding line-breaks may be omitted when copied over to the application.
</div>

### Supported commands

Command words are case-sensitive. The supported words are `add`, `delete`, `view`, `find`, `list`, `followup` and `followups`.

Typed `edit`, standalone `clear`, `help` and `exit` are unsupported and return exactly `Unknown command.`, even when saved data could not be loaded. Use `followup INDEX clear` to clear one client's follow-up; this remains supported. Close the application with the window's close button. The GUI Help menu and `F1` shortcut remain separate from typed commands.

Use one command line and ordinary spaces. Tabs, control characters and line separators are rejected before command recognition or argument validation. Blank input returns `Enter a command.`. For example, `VIEW 1` returns `Unknown command.` because `view` must be lowercase.


### Adding a client: `add`

Adds a client with their contact details and optional tags. A successful add returns to the full client list in insertion order; the new client has no follow-up yet.

Format: `add n/NAME p/PHONE_NUMBER e/EMAIL a/ADDRESS [t/TAG]...`

Supply each required prefix exactly once. The prefixes may appear in any order and must be preceded by an ordinary space. Tags may appear repeatedly and have no count limit. Outer ordinary spaces are removed from field values.

| Field | Accepted values |
| --- | --- |
| Name | Non-blank English letters, digits and ordinary spaces. Case and internal spaces are preserved. |
| Phone | At least three digits `0`–`9`. Leading zeroes are retained. |
| Email | Alphanumeric groups separated by single `+`, `_`, `.` or `-` before one `@`; domain labels contain alphanumeric groups separated by single hyphens and labels separated by dots. The final label must contain two adjacent letters or digits. Stored case is preserved. |
| Address | Non-empty single-line text, including punctuation, non-English text and emoji. |
| Tag | One or more English letters or digits. Tags are stored in lowercase; repeated case variants become one tag. |

Use one command line and ordinary spaces. Tabs, control characters and line separators are rejected. There are no additional contact-length limits.

A client is a duplicate when **their name matches and either their phone or email matches** an existing client. Names are compared ignoring English case and repeated ordinary spaces; phones match exactly, and emails ignore case. Address, tags and follow-up do not affect this check. Two clients may share a name if both their phone and email differ. Different names may share contact details. Duplicate checks include clients hidden by the current filter.

Examples, entered into a list without these clients:

* `add n/Rachel Lim p/001 e/Rachel@example.com a/Blk 123 t/HEALTH t/active t/health` adds Rachel and displays `Tags: [active] [health]`.
* `add n/Rachel Lim p/002 e/other@example.com a/新加坡 🏠` adds a second Rachel because both contact details differ.
* `add n/RACHEL  LIM p/001 e/new@example.com a/Another address` fails with `This client already exists in Policy Harbour.` because the first Rachel has the same normalized name and phone.

Success text includes the name, phone, email, address and sorted lowercase bracketed tags separated by spaces. Without tags, it ends in `Tags: None`. For example:

```text
New client added: Rachel Lim; Phone: 001; Email: Rachel@example.com; Address: Blk 123; Tags: [active] [health]
```

A missing required prefix or extra text before the first prefix produces:

```text
Invalid command format!
Usage: add n/NAME p/PHONE_NUMBER e/EMAIL a/ADDRESS [t/TAG]...
```

These shape errors take priority over repeated required prefixes. After shape and repeated-prefix checks, values are checked in this order: name, phone, email, address, then tags. Duplicate checking comes after validation. An unrecognized prefix inside a field remains literal text: for example, `z/unit` inside an address is allowed, while the same text inside a phone makes the phone invalid.

Changes are saved before they appear in the list. If saving fails, the previous data and list remain and the result says `Changes could not be saved. No changes were kept. Try the command again.` If saved data could not be loaded at startup, adding clients is blocked to protect that file.

### Listing all persons: `list`

Shows a list of all persons in the address book.

Format: `list`

### Selecting a client: `view`

Selects the client at an index in the **currently displayed list** and returns `Showing client: NAME`, using the stored name without a trailing period. For Rachel Lim, the feedback is `Showing client: Rachel Lim`.

Format: `view INDEX`

Supply exactly one index containing digits `0`–`9`, with a value from `1` to `2147483647`. Leading zeroes are allowed, including long runs of zeroes: `view 02` and `view 0002` both mean `view 2`. The index must refer to an existing displayed row.

The command keeps client data, list mode, row order and numbering unchanged and does not save. After `find`, use search-result indices; after `followups`, use pending-list indices rather than full-list positions. Two same-name clients remain separate records even though their success feedback uses the same name.

Examples:

* `list` then `view 2` selects the second client in the full list, if present.
* `find Rachel` then `view 1` selects the first matching Rachel, if present.
* `followups` then `view 01` selects the first displayed pending client, if present.

| Input problem | Exact result |
| --- | --- |
| Missing index or extra tokens, such as `view 0 extra` | `Invalid command format!` followed by a newline and `Usage: view INDEX` |
| One invalid index token, such as `0`, `-1`, `1.5`, `abc` or `2147483648` | `Index must be a positive integer from 1 to 2147483647.` |
| Valid index beyond the displayed list | `The client index provided is invalid.` |

Missing or extra tokens take priority over index syntax; valid syntax is checked before the displayed-list range. A failed view preserves the previous selection, list and displayed date. In a failed-load session, `view` is still allowed: a valid index produces the range error against the empty list rather than a mutation-blocked message.

**Integration status:** this branch implements the command and selected-client state. The dedicated profile panel is pending integration, so selection currently produces the feedback above without displaying a full client profile. The later profile panel will display the selected record's details.

### Locating clients by name: `find`

Searches all clients' names, including clients hidden by a prior search or pending list, and displays matches in their original insertion order. It leaves pending mode and returns `COUNT client(s) listed!`, for example `2 client(s) listed!` or `0 client(s) listed!`.

Format: `find KEYWORD [MORE_KEYWORDS]...`

* Matching is case-insensitive: `rachel` matches `Rachel`.
* Only whole words match: `Rach` does not match `Rachel`.
* A client matching any keyword appears once (an OR search). Keyword order and repeated keywords do not change the result order.
* Only names are searched; phone, email, address and tags are not searched.
* Separate keywords with ordinary spaces. Multiple ordinary spaces are allowed. Quotes and punctuation are literal: `find "Rachel"` and `find Rachel,` do not match the name `Rachel Lim`.

For a dataset containing Rachel Lim and Amanda Lee, `find rachel AMANDA` returns both in insertion order. `find Lim` returns Rachel Lim; `find Rach` returns no matches unless another client has that exact name word.

A missing keyword produces exactly these two lines:

```text
Invalid command format!
Usage: find KEYWORD [MORE_KEYWORDS]...
```

Search does not change client data or save it. The selected client remains selected only while present in the results; searching them out clears selection. A later search does not automatically select them again.

### Deleting a client: `delete`

Deletes the client at an index in the **currently displayed list**. After `find`, use the search-result indices; after `followups`, use the pending list's due-date order. Deleting a client also removes their follow-up. The active list mode remains unchanged and its rows are renumbered.

Format: `delete INDEX`

Supply exactly one index, using digits `0`–`9`, from `1` to `2147483647`. Leading zeroes are allowed: `delete 02` means `delete 2`. The index must also refer to an existing displayed row.

Examples:

* `list` followed by `delete 2` deletes the second client in the full list.
* `find Rachel` followed by `delete 1` deletes the first Rachel shown, preserving any other same-name client.
* `followups` followed by `delete 01` deletes the first pending client shown, even if that client occupies another position in the full list.

Success text uses the deleted client's contact details and sorted tags, without their follow-up. For example:

```text
Deleted client: Rachel Lim; Phone: 001; Email: Rachel@example.com; Address: Blk 123; Tags: [active] [health]
```

| Input problem | Result |
| --- | --- |
| Missing index or extra tokens, such as `delete 0 extra` | `Invalid command format!` followed by a newline and `Usage: delete INDEX` |
| One invalid index token, such as `0`, `-1`, `abc`, `1.5` or `2147483648` | `Index must be a positive integer from 1 to 2147483647.` |
| Valid index beyond the displayed list | `The client index provided is invalid.` |

Shape errors take priority over index syntax. A rejected delete leaves data, the current list and date unchanged. A save failure also keeps the previous state and reports `Changes could not be saved. No changes were kept. Try the command again.` Deletion is blocked if saved data could not be loaded at startup.

### Keeping the same client profile open `[planned; pending integration]`

The `view` command, selection model and dedicated profile panel are implemented, but the main window still awaits panel integration. You can use `view INDEX` to select a client and receive confirmation feedback; the following describes the visible profile once it is connected to the workspace.

The profile will show the selected client's name, phone, email, address and tags. Tags appear in lowercase alphabetical order, or as `None` when there are no tags. A pending follow-up will show its date, full action text and `OVERDUE`, `DUE TODAY` or `UPCOMING` status. With no follow-up it will show `Follow-up: None`; with no selected client it will show `No client selected.`

Your open profile will follow the client, even when their row number changes. Two clients with the same name will have separate profiles. Only a successful `view` command will open a profile; adding a client or highlighting a row will not open one automatically.

| Change | Planned result for your open profile |
| --- | --- |
| Show the full list or add a client | Keep the same client open. |
| Search or show pending follow-ups | Keep the client open only if they remain in the displayed list; otherwise close the profile. |
| Record or replace that client's follow-up | Update their profile and keep it open, even if the pending list moves their row. |
| Clear that client's follow-up in the full list or search results | Keep their profile open without a follow-up. |
| Clear that client's follow-up in the pending list | Close their profile because the client leaves the pending list. |
| Delete the open client | Close their profile; do not open the client who inherits their row number. |
| Delete another client or change another client's follow-up | Keep the same client open despite any renumbering. |

Once a profile closes because its client leaves the list, returning to the full list will not reopen it. You will need another successful `view` command. Reopening the application will also start without an open profile. A failed command or failed save must leave the previous profile unchanged; this requires the later command and profile integration checks.

### Saving the data

AddressBook automatically saves data after every command. You do not need to save manually.

### Editing the data file

AddressBook data is saved automatically as a JSON file `[JAR file location]/data/addressbook.json`. Advanced users are welcome to update data directly by editing that data file.

<div markdown="span" class="alert alert-warning">:exclamation: **Caution:**
If your changes make the data file invalid, AddressBook starts with an empty address book at the next run. The invalid file remains on disk until you run a command (AddressBook saves after every command). Still, we recommend backing up the file before editing it.<br>
Furthermore, certain edits can cause the AddressBook to behave in unexpected ways (e.g., if a value entered is outside of the acceptable range). Therefore, edit the data file only if you are confident that you can update it correctly.
</div>

### Archiving data files `[coming in v2.0]`

_Details coming soon ..._

--------------------------------------------------------------------------------------------------------------------

## FAQ

**Q**: How do I transfer my data to another computer?<br>
**A**: Install the app on the other computer and overwrite the data file it creates with the data file from your previous AddressBook home folder.

--------------------------------------------------------------------------------------------------------------------

## Known issues

1. **When using multiple screens**, if you move the application to a secondary screen, and later switch to using only the primary screen, the GUI will open off-screen. The remedy is to delete the `preferences.json` file created by the application before running the application again.
2. **If you minimize the Help Window** and then use the `Help` menu or the keyboard shortcut `F1` again, the original Help Window will remain minimized, and no new Help Window will appear. The remedy is to manually restore the minimized Help Window.

--------------------------------------------------------------------------------------------------------------------

## Command summary

Action | Format, Examples
--------|------------------
**Add** | `add n/NAME p/PHONE_NUMBER e/EMAIL a/ADDRESS [t/TAG]...` <br> e.g., `add n/James Ho p/22224444 e/jamesho@example.com a/123, Clementi Rd, 1234665 t/friend t/colleague`
**Delete** | `delete INDEX`<br> e.g., `delete 3`
**View** | `view INDEX`<br> e.g., `view 02` (selection implemented; profile panel pending integration)
**Find** | `find KEYWORD [MORE_KEYWORDS]...`<br> e.g., `find James Jake`
**List** | `list`
**Record or replace follow-up** | `followup INDEX d/DATE m/DESCRIPTION`
**Clear follow-up** | `followup INDEX clear`
**Pending follow-ups** | `followups`

Typed `edit`, standalone `clear`, `help` and `exit` return `Unknown command.`. Use the window's close button to exit.
