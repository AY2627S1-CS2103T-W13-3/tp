---
layout: page
title: User Guide
---

PolicyHarbor helps independent insurance agents in Singapore keep client contacts and their next follow-up together. Find a client, review their details, update their contact information and record the next conversation, using typed commands in a desktop workspace.

**Build covered:** this guide describes the v1.5 release candidate, with permanent Client IDs, contact editing and name/phone/tag search. Its window title is **PolicyHarbor v1.5**. The tested candidate was built from [verified source revision `97c10e2a`](https://github.com/AY2627S1-CS2103T-W13-3/tp/commit/97c10e2ad63e20b00db58657d5cd06182de0ae6f). A matching v1.5 JAR has not yet been published on the [releases page](https://github.com/AY2627S1-CS2103T-W13-3/tp/releases). Until it is available, obtain the matching `PolicyHarbor.jar` from the team. The published v1.4 JAR uses different command targeting and does not match this guide.

* Table of Contents
{:toc}

## Quick start

### Install and launch

1. Install Java **25**. On macOS, follow the [SE-EDU Java installation guide](https://se-education.org/guides/tutorials/javaInstallationMac.html) for the prescribed Zulu JDK with JavaFX.
2. Obtain the matching `PolicyHarbor.jar` described above and place it in a **writable folder**, such as a folder you create in Documents. Keep an existing client-data file backed up before replacing an older JAR.
3. Open a terminal and use `cd` to enter that folder. Check `java -version` in the same terminal; it should report Java 25. Then run:

   ```text
   java -jar "PolicyHarbor.jar"
   ```

   Launching from a terminal makes the Java version and any startup error visible; double-clicking may use a different Java installation. If your downloaded JAR has a different filename, substitute that filename inside the quotes.

On a first launch in a folder with no saved client data, the client list is empty. If clients already appear, your saved data has loaded: use their displayed Client IDs and skip adding the sample client if it would duplicate an existing record.

### Try a daily workflow

Use a fresh, separate folder for this example so that it does not change your real clients. Enter each line separately and press **Enter** or click **Run**:

```text
add n/Rachel Lim p/91234567 e/rachel@example.com a/Blk 123 t/health
find n/rac
view 1
edit 1 p/92345678
followup 1 d/2099-12-15 m/Discuss renewal options
followups
```

- In this fresh dataset, the first successful add assigns Rachel **Client ID 1**. In an existing dataset, read the ID from the add feedback or table and substitute it in later commands; deleting all clients does not restart ID allocation.
- The search finds Rachel by part of her name. `view 1` opens her profile; `edit` changes her phone to `92345678` while keeping her ID.
- The follow-up appears in Rachel's profile and the pending summary. The example date is in the future and displays as `UPCOMING`; use the real due date for your own work.

Close the main window, then run the JAR again from the **same folder**. Rachel, her edited phone and her action are restored. The profile starts closed; enter `view 1` to reopen it. You can also close the app through **File → Exit**.

## Understanding the workspace

<span id="reading-the-workspace"></span>

[![PolicyHarbor showing permanent Client IDs, Rachel's selected profile and pending follow-ups](images/Ui.png)](images/Ui.png)

*The workspace in pending mode, populated with fictional clients. The Client ID is the command target; the screenshot contains a larger dataset than the quick-start example. Status badges reflect the capture date, 29 October 2026; live badges change with the current date. Open the image for a full-size view.*

- **Top: command and feedback.** Type a command, then press Enter or use Run. The feedback explains the result or how to correct an error.
- **Left: client table.** It shows Client ID, name, phone and email. `list` shows all clients, `find` shows matches and `followups` shows clients with pending actions, adding due-date and status columns. Columns cannot be sorted by clicking their headers; use the command's displayed order. Narrow tables scroll horizontally.
- **Upper right: client profile.** Use `view ID` to open full contact details, tags and the current action. Clicking or highlighting a table row or summary card does **not** open a profile.
- **Lower right: pending summary.** This always shows every pending action, even when a search hides its client in the table. An empty search result can therefore appear beside a populated summary. Actions are grouped by status as explained under [pending follow-ups](#showing-pending-follow-ups-followups).
- **Bottom: data-file location.** Hover over a shortened path to read the full path.

Drag the main divider to change the table/sidebar widths and the sidebar divider to change the profile/summary heights. Long values wrap; scroll each right-hand panel independently. On a small or highly scaled display, a panel may be shorter than one action card: scroll it or give it more space with the divider.

### Permanent Client IDs

A Client ID identifies one client permanently. It does not change when you search, reorder follow-ups or edit contact details. Read it from the table, profile, summary card or add feedback. Gaps after deletion are normal; IDs are not reused during normal operation.

**An ID is not a row position.** If a search shows only Client ID 8, `delete 1` targets Client ID 1, even if that client is hidden. Always check the ID and, before a change, use `view ID` to confirm the intended client.

`view`, `edit`, `delete` and `followup` resolve IDs against **all saved clients**, including clients hidden by a search or the pending list. IDs use digits `0`–`9` and range from `1` to `2147483647`. Leading zeroes are allowed: `view 001` and `view 1` select the same client. See [backup precautions](#existing-files-client-ids-and-backups) before moving or modifying ID history.

### Keeping the same client profile open

A viewed client is marked `VIEWING` in the table and, when they have an action, the summary. The profile shows their ID, contact details, lowercase tag chips and pending action. With no tags it shows `No tags`; without an action, `Follow-up: None`.

| What you do | What happens to the open profile |
| --- | --- |
| `list`, add another client or change another client's action | The same client stays open. |
| Search, show pending actions or edit details used by a search | The client stays open only if they remain in the displayed table; otherwise the profile closes. |
| Replace the viewed client's action | Their profile updates and follows the same ID, even if the row moves. |
| Clear their action | The profile stays open in full/search mode; it closes in pending mode because the client leaves that table. |
| Delete the viewed client | The profile closes without selecting someone else. |

When a profile closes, later returning to the full list does not reopen it; use `view ID` again. Restarting also clears profile selection. Opening a different client starts at the top; updating the same client's details/action or refreshing the date keeps the reading position as far as the remaining content allows. A failed command or save preserves the previous profile.

### Keyboard workflow and error correction

Successful commands clear the command field. Rejected commands keep the input for correction, mark the field and show **Error** above the message. For example, after `view 0`, correct the retained line to `view 1` for Rachel. Editing removes the field's error style; feedback stays until the next command. Long feedback wraps, scrolls and can be selected/copied.

Enter and Run submit the same command. You can reach Run with **Tab** and activate it with **Space**; typing focus returns to the command field after submission. If the retained text is selected, typing replaces it.

Paste one command line at a time. A paste containing a tab, control character or line separator is rejected as a whole: the previous draft and selection remain, and no command runs. Remove those characters before pasting again. If you press Enter after a rejected paste, the unchanged previous draft is what runs.

## Command overview

<span id="supported-commands"></span>
<span id="command-summary"></span>
<span id="features"></span>

| Task | Format |
| --- | --- |
| [Add a client](#adding-a-client-add) | `add n/NAME p/PHONE_NUMBER e/EMAIL a/ADDRESS [t/TAG]...` |
| [List all clients](#listing-all-clients-list) | `list` |
| [Find clients](#finding-clients-find) | `find KEYWORD [MORE_KEYWORDS]...` or `find n/NAME_PART`, `find p/PHONE_PART`, `find t/TAG` |
| [Open a profile](#selecting-a-client-view) | `view ID` |
| [Edit contact details](#editing-a-clients-details-edit) | `edit ID [n/NAME] [p/PHONE] [e/EMAIL] [a/ADDRESS] [t/TAG]...` |
| [Delete a client](#deleting-a-client-delete) | `delete ID` |
| [Record or replace an action](#recording-or-replacing-a-follow-up-followup) | `followup ID d/DATE m/DESCRIPTION` |
| [Clear an action](#clearing-a-follow-up-followup-id-clear) | `followup ID clear` |
| [Show pending clients](#showing-pending-follow-ups-followups) | `followups` |

### Command format

- Replace `UPPER_CASE` words with your values. Do not type the square brackets: they mark optional fields. `...` means tags or bare search keywords may be repeated as shown.
- Command words are lowercase and case-sensitive. Use ordinary spaces to separate words; extra ordinary spaces around a command are accepted. Tabs, control characters and line separators are rejected.
- Named fields such as `n/` and `p/` may be supplied in any order. Put a space before each prefix. Required fields cannot be omitted or empty; `edit ID t/` is the special case for clearing tags.
- Do not repeat single-valued fields such as `n/` or `d/`; `t/` may repeat. Known prefixes within values can be interpreted as fields, and quotes do not escape them. See the [prefix reference](#prefixes-and-literal-text).
- `list` and `followups` take **no arguments**. For example, `list 123` is rejected with usage feedback.

Standalone `clear`, `help` and `exit` are unsupported and report `Unknown command.`. Clear an action with `followup ID clear`, open Help through the menu/F1 and close the app through the window or File → Exit.

## Managing clients

The reference examples below use Rachel with Client ID 1. They illustrate each command independently; substitute your client's actual ID. In particular, try deletion only with disposable data, as it removes the client used in the other examples.

### Adding a client: `add`

Save a client or prospect's contact details, with optional tags.

**Format:** `add n/NAME p/PHONE_NUMBER e/EMAIL a/ADDRESS [t/TAG]...`

**Example in a fresh dataset:**

```text
add n/Rachel Lim p/91234567 e/rachel@example.com a/Blk 123 t/health
```

**Result:** Rachel is assigned ID 1, with no follow-up yet. Feedback includes her ID and contact details, ending in `Tags: [health]`. With no tags, it ends in `Tags: None`. This is the same add used in the quick start; adding it again is rejected as a duplicate.

Supply name, phone, email and address exactly once. Tags are optional. The [contact field reference](#contact-field-reference) explains accepted values.

A duplicate is rejected when the **name matches and either the phone or email matches** an existing client. Names ignore case, equivalent accented spellings and repeated ordinary spaces; phone comparison ignores allowed formatting, and emails ignore case. Address, tags and follow-up are not part of the check. Two clients may have the same name if both phone and email differ; different names may share contact details. Hidden clients are also checked.

For example, after adding Rachel, `add n/RACHEL  LIM p/91234567 e/other@example.com a/Blk 456` reports `This client already exists in PolicyHarbor.`

Adding preserves the current table mode and open profile. In a search, the new client appears only if they match; in pending mode, they stay hidden until they have an action. To see all clients, use `list`.

### Listing all clients: `list`

Return to all clients in the order they were added.

**Format and example:** `list`

**Result:** `Listed all clients.` The current profile stays open. This leaves search/pending mode without changing data or IDs. Extra arguments are rejected; use `find` to search.

### Finding clients: `find`

Search all clients, including those hidden by the current view. Results appear once per client in the order clients were added.

**Formats and examples:**

| Search | Format | Example and meaning |
| --- | --- | --- |
| Whole name words or a full phone number | `find KEYWORD [MORE_KEYWORDS]...` | `find Rachel Lim` matches either complete name word, ignoring case. `find 92345678` matches those complete phone digits. |
| Part of a name | `find n/NAME_PART` | `find n/rac` matches Rachel. |
| Part of a phone number | `find p/PHONE_PART` | `find p/9234` matches Rachel's edited phone from the quick start. |
| Exact tag | `find t/TAG` | `find t/HEALTH` matches the whole tag `health`, ignoring case. |

**Result:** feedback gives the count, such as `1 client(s) listed!`. Search leaves pending mode. The profile remains open only if the viewed client is among the results; the global summary still shows every pending action.

Bare search uses **either** keyword, not all keywords together: `find Rachel 92345678` returns clients matching either. Partial words do not match in bare mode: `find Rach` does not find Rachel; `find n/Rach` does. Bare phone search requires all stored digits, including any country code and leading zeroes. Separate tokens are not joined into one phone number.

Explicit name search ignores case, equivalent accented spellings and repeated ordinary spaces. Phone search ignores stored `+`, spaces and hyphens, and accepts a fragment of one or more digits with valid phone formatting. `find t/new` does not match `renewal`. Email and address are not searched; quotes and punctuation are literal text.

Use **one** explicit field, without bare text before it. `find n/Rachel p/9234`, repeated fields, empty values or `find` alone produce usage feedback. The prefixes `n/`, `p/` and `t/` remain reserved inside explicit queries.

### Selecting a client: `view`

Open the client's complete profile so you can check their details before acting.

**Format:** `view ID`

**Example:** `view 1`

**Result:** for Rachel, `Showing client ID 1: Rachel Lim`; her profile opens and her row is marked `VIEWING`. If she was hidden, the app first returns to the full list. If she was visible, the current mode and order stay unchanged. Viewing does not change saved data.

Supply one existing [Client ID](#permanent-client-ids). An unknown ID reports, for example, `No client with ID 1.`; this is different from an invalid number such as `0`. A failed view preserves the previous profile and table.

### Editing a client's details: `edit`

Update contact details while retaining the client's **ID and pending follow-up**. The target can be hidden by the current view.

**Format:** `edit ID [n/NAME] [p/PHONE] [e/EMAIL] [a/ADDRESS] [t/TAG]...`

**Example:** `edit 1 p/92345678`

**Result:** Rachel's phone changes; feedback starts `Client ID 1 updated: Rachel Lim`. Omitted fields keep their values. At least one field must be supplied; `edit 1` alone reports `At least one field to edit must be provided.`

**Supplying tags replaces the entire tag set.** For example, `edit 1 a/Blk 456 t/renewal t/priority` changes the address and replaces all previous tags with `priority` and `renewal`. Use a lone empty `t/`, as in `edit 1 t/`, to remove all tags.

The [contact field rules](#contact-field-reference) and duplicate check used for adding also apply to editing. An edit that duplicates another client reports `This person already exists in PolicyHarbor.` The current table mode remains, but search results are checked again: changing a matching field can hide that client and close their profile. Rejected edits keep the previous data.

### Deleting a client: `delete`

Remove an incorrect or unwanted client record and their pending follow-up.

**Format:** `delete ID`

**Example with disposable Rachel data:** `delete 1`

**Result:** Rachel and her action disappear; feedback starts `Deleted client ID 1: Rachel Lim`. If she was viewed, her profile closes. The current table mode remains, and other clients keep their IDs.

**Deletion is saved immediately, with no confirmation or undo.** It also removes the client's current action. Verify the ID with `view ID` first and keep a [backup](#existing-files-client-ids-and-backups) if you may need the record again. Deleting a hidden client works too; the first displayed row is not necessarily Client ID 1.

## Managing follow-ups

Each client can have **one pending action**. An overdue action stays pending until you clear it. The examples use Rachel's actual ID, shown as 1 here, and assume she has not been deleted.

### Recording or replacing a follow-up: `followup`

Record a dated next action, or replace the client's current one.

**Format:** `followup ID d/DATE m/DESCRIPTION`

**Example:** `followup 1 d/2099-12-15 m/Discuss renewal options`

**Result:** for a client without an action, feedback is `Follow-up for client ID 1: Rachel Lim recorded for 2099-12-15.` For a client with an action, it is `Follow-up for client ID 1: Rachel Lim updated to 2099-12-15.` The profile/summary refresh; in pending mode, a changed date can move the row while retaining the selected client.

**Replacing overwrites the previous date and description.** There is no history or undo; copy any details you need before replacing an action.

Use an actual calendar date in `YYYY-MM-DD` format, with a year from `0001` to `9999`. Past, current and future dates are accepted, so you can record overdue work using its original due date.

Descriptions allow multilingual text and emoji and must contain visible text or a symbol. The limit is **1–200 Unicode code points**, after trimming outer ordinary spaces; most letters count once, but joined emoji and accented combinations can count as several. Internal spacing is retained. Controls, line breaks, invalid characters and notes made only of invisible characters/combining marks are rejected.

The prefixes `d/` and `m/` preceded by an ordinary space are reserved even inside descriptions and quotes. For example, `m/Confirm d/m/y format` is rejected as a repeated date field. Write `m/Confirm day/month/year format` instead; `m/Check ref:d/m/y` is also valid because the internal `d/` has no preceding space.

### Clearing a follow-up: `followup ID clear`

Remove the current action after completing it, while keeping the client.

**Format:** `followup ID clear`

**Example:** `followup 1 clear`

**Result:** `Follow-up for client ID 1: Rachel Lim cleared.` Their summary card disappears. In pending mode, the client also leaves the table and their profile closes; in full/search mode, the client remains and their open profile shows no action.

Clearing is saved immediately and has no undo/history. If there is no action, the result is `This client has no pending follow-up.`; no client is deleted.

### Showing pending follow-ups: `followups`

Review clients with actions still to do, in **earliest due-date order**. Equal dates keep the order the clients were added.

**Format and example:** `followups`

**Result:** `Pending follow-ups`, or `There are no pending follow-ups.` if none remain. Extra arguments are rejected. `list` returns to all clients; `find` searches the full dataset.

The global summary groups the same actions by status, retaining due-date order within each group:

| Badge | Meaning |
| --- | --- |
| `OVERDUE` | Due before today. |
| `DUE TODAY` | Due today. |
| `UPCOMING` | Due after today. |

Statuses use the computer's current local date and refresh at startup, when the main window regains focus and once a minute while running. No command is needed to refresh them. Dates remain in `YYYY-MM-DD` format regardless of your computer's date display format.

## Saving and managing data

<span id="saving-the-data"></span>

### Automatic saving and restart

Successful `add`, `edit`, `delete` and singular `followup` changes are saved automatically before appearing in the workspace. There is no save command. `list`, `find`, `view` and `followups` do not rewrite client data.

The file is `data/addressbook.json`, inside the folder **from which you run** `java -jar "PolicyHarbor.jar"`. Running from another folder uses a separate dataset. The footer shows the data-file path. Preferences such as window size/position are stored separately in `preferences.json`.

After restarting from the same folder, contacts, IDs and pending actions return. The app starts with the full client list and no open profile; search/pending mode and profile selection are not restored.

### If saving fails

A failed save keeps the previous client data, table and profile, and retains the rejected command for correction/retry. For a normal write failure, feedback says:

```text
Could not save to data/addressbook.json. No changes were kept. Check that the data folder is writable and has free space, then try again.
```

Restore access to the data folder and free disk space as needed, then submit the retained command again. Do not assume a failed add received an ID or that an unsuccessful edit/delete/action change was saved. The [troubleshooting table](#troubleshooting) covers protected data and symbolic-link errors separately.

### Existing files, Client IDs and backups

**Back up the complete `data/addressbook.json` while the app is closed.** There is no automatic backup or undo. Restore backups while closed, then restart; restoring an older copy replaces newer saved work with that copy's contents.

To transfer data, close the app on both computers. Put the matching JAR in a writable folder on the destination, create its `data` subfolder and copy the complete client file there, backing up any existing destination file first. Launch from the new JAR folder with Java 25.

IDs and allocation history survive normal restarts and deletion, including deleting every client. Copying only contact entries, discarding identity fields or restoring an older backup can lose that history. Never reset IDs or lower their allocation counter as a repair. See [advanced file compatibility](#advanced-data-file-reference) if you maintain saved files directly.

## Help and troubleshooting

The **Help → Help** menu or **F1** opens a window with the online User Guide link. Use **Copy link** or select/copy the link field, then open it in your browser. `Esc` closes Help. Opening Help again restores a minimised Help window and brings it forward. Core client/action commands work offline; opening the online guide needs Internet access.

<span id="faq"></span>
<span id="troubleshooting-and-known-limitations"></span>

### Troubleshooting

| Problem | What to do |
| --- | --- |
| The JAR does not start | Launch from a terminal with `java -version` and `java -jar "PolicyHarbor.jar"`. Confirm Java 25 and the actual JAR filename. Double-clicking may use another Java installation. |
| Startup fails on macOS | Follow the [prescribed JavaFX/JDK setup](https://se-education.org/guides/tutorials/javaInstallationMac.html). Check `java -version` and `which java` in the same terminal before launching. |
| Linux shows `Gdk-CRITICAL` under Wayland | Try `GDK_BACKEND=x11 java -jar "PolicyHarbor.jar"`, as advised in the [course JAR testing instructions](https://nus-cs2103-ay2627-s1.github.io/website/schedule/week11/project.html). |
| A command is rejected | Read the complete feedback, correct the retained line and retry. Use lowercase command words and one line with ordinary spaces. See the [command overview](#command-overview) and [field reference](#contact-field-reference). |
| A new or edited client seems missing | Run `list`: an active search can hide non-matching clients, and pending mode hides clients without actions. IDs have not changed. |
| `No client with ID ...` | Check the table/profile ID, not the row position. The client may have been deleted or belong to another data folder. |
| Changes cannot be saved | Check write access, free space and whether a regular file is blocking the `data` folder. Restore access, then retry. Do not run as administrator just to bypass permissions. |
| Saved-data warning; changes are blocked | Follow [protected-data recovery](#saved-data-warning-and-recovery). Restoring folder access or editing a file does not unlock the running session; restart to reload. |
| Another instance is running | Use or close the existing window before launching in the same folder. One folder supports one instance; separate folders have independent data. |
| The folder cannot be locked | Use a writable folder. `.policyharbor.lock` must be a regular file; it normally remains after closing. Its presence alone does not mean the app is running. Do not delete/replace it while an instance runs; closing/terminating that instance releases the operating-system lock. |
| The data file is a symbolic link | Saving through it is refused. Close the app, back up the link's target, then replace the link with a regular data file and restart. Do not change data paths externally while the app is open. |
| Window settings are invalid or a saved screen was disconnected | Invalid `preferences.json` falls back to default settings and may be replaced; a preference error alone does not block client changes. A missing saved screen causes the window to fit on the primary screen; deleting client data is unnecessary. |
| The workspace feels cramped | Maximise the main window and adjust its dividers. Scroll the table horizontally and the profile/summary vertically. Secondary windows can behave differently across platforms; keep Help in an ordinary window if fullscreen causes trouble. |
| Diagnostic logging is unavailable | The launch terminal reports `Unable to create diagnostic log file; continuing with console logging.` The app keeps running. Check folder write access and that `policyharbor.log.0` is not a directory, then restart to restore file logging. |

### Saved-data warning and recovery

If client data cannot be read or is invalid, the app shows an empty table and a persistent warning. **This does not mean your saved clients were deleted.** `add`, `edit`, `delete` and `followup` are blocked to protect the file; query commands remain available. An ordinary command result does not dismiss the warning.

1. Close PolicyHarbor and make a backup of `data/addressbook.json` if you can access it. Do not delete the only copy of client data to make the warning disappear.
2. If the folder was inaccessible, restore access. Otherwise restore a complete known-good backup, or have the file repaired using the [advanced reference](#advanced-data-file-reference).
3. Restart from the same folder to reload the file. If the client file is absent and that absence can be confirmed, the app starts a new, writable dataset. The app does not partly load a file containing invalid records.

## Known limitations

- One current action per client, with no follow-up history, automatic reminders or notifications. Replacement/clearing has no undo.
- Search supports one explicit field at a time and does not search email/address. Use permanent IDs for changes and `view` to open profiles.
- PolicyHarbor stores contacts and next actions. It does not manage insurance policy, premium or claim records; send messages; import/export CSV; synchronise with cloud services; or create automatic backups. Use the manual backup procedure above.

## Reference information

### Contact field reference

These rules apply to both adding and editing. Ordinary spaces at the outside of field values are removed; internal spacing, spelling and case are retained except that tags become lowercase. There are no additional contact-length limits.

| Field | Accepted values |
| --- | --- |
| Name | Starts with a letter from any language or a digit `0`–`9`. Supports Unicode letters, combining accents, ordinary spaces and `/ - ' @ .`. A name must contain visible content; invisible Hangul fillers alone are rejected. Examples: `Ravi s/o Kumar`, `Sean O'Brien`, `Dr. Lim`. |
| Phone | At least three digits `0`–`9`, optionally beginning with `+`, with single spaces or hyphens between digit groups. For example, `+65 9123 4567`. Display formatting and leading zeroes are retained. |
| Email | One `@`, with ASCII letters/digits and supported punctuation; no spaces. Before `@`, the first/last characters must be letters/digits; `+`, `_`, `-` and periods may occur inside. Periods must separate letters/digits. Domain labels start/end with letters/digits and may contain hyphens; the final label needs two adjacent letters/digits. Stored case is retained. |
| Address | Non-empty, single-line text with visible content, including punctuation, non-English text and emoji. Text made only of spaces/invisible formatting, controls or invalid character sequences is rejected. |
| Tag | One or more English letters or digits, without spaces or punctuation. Tags become lowercase; repeated case variants become one tag. |

Tags have no count limit. They display in lowercase alphabetical order; command feedback uses bracketed tags separated by spaces. Missing required fields, repeated single-valued prefixes and invalid values produce feedback to guide correction. When several values are invalid, correct the reported error and resubmit rather than assuming every problem was listed.

### Prefixes and literal text

Recognised field prefixes must follow an ordinary space. For add/edit, these are `n/`, `p/`, `e/`, `a/` and `t/`; for recording an action, `d/` and `m/`. Quotes are literal characters, not a way to escape a recognised prefix. Avoid putting a space followed by a command's recognised prefix inside a value.

Unrecognised prefixes stay inside the value and must still meet that field's rules: `z/unit` can be address text but is not a valid phone number. Local name forms `s/o` and `d/o` are ordinary name text for add/edit. Explicit search reserves `n/`, `p/` and `t/` and accepts only one field.

### Advanced data-file reference

<span id="editing-the-data-file"></span>

Prefer `add`/`edit`/`followup` to changing saved files directly. If you must edit `data/addressbook.json`, **close the app first and back up the complete file**. The [Developer Guide's saved-data description](DeveloperGuide.html#saved-client-loading) contains the schema and detailed validation rules.

- Save as **UTF-8**. A single leading byte-order mark (BOM), as added by some editors, is accepted. Invalid byte sequences or invalid saved character sequences are rejected, even when a JSON escape is syntactically valid.
- Contact fields, tags, dates and descriptions must be JSON strings. Keep phone values quoted to retain leading zeroes. IDs/counters are numbers; do not convert them to strings.
- Keep property names exact, including `persons`, `tags` and `followUp`. Unknown fields, repeated properties in one object, invalid/duplicate clients, missing required values and invalid identity metadata cause protected startup. Optional missing tags mean no tags; absent/null `followUp` means no action. Valid overdue dates remain supported.
- A legacy file with **no identity metadata** loads valid clients with insertion-order IDs, starting at 1, without rewriting the file. The first successfully saved change writes IDs and the next-ID counter.
- Keep every `clientId` and the root `nextClientId` when copying/backing up. If any identity metadata remains, incomplete/invalid metadata is rejected. If **all** IDs and the counter are removed or set to `null`, the file is treated as legacy and receives a new ID history; discarded IDs and the old counter cannot be reconstructed. Restoring an older backup can also restore older history.
- Do not change paths or saved files externally while PolicyHarbor is running. Symbolic-link save checks do not synchronise with external filesystem changes. Repairing a file during a protected session cannot unlock it; close and restart after repair.
