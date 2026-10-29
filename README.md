# PolicyHarbor

[![Java CI](https://github.com/AY2627S1-CS2103T-W13-3/tp/actions/workflows/gradle.yml/badge.svg?branch=master)](https://github.com/AY2627S1-CS2103T-W13-3/tp/actions/workflows/gradle.yml)

**Clients. Follow-ups. Made simple.**

PolicyHarbor v1.5 is a desktop client and follow-up manager for individual insurance agents who prefer typed commands. Keep client contact details and their next action together, then review what is overdue, due today or upcoming.

![PolicyHarbor showing pending follow-ups and a selected client profile](docs/images/Ui.png)

## What you can do

- Add, list, edit and delete clients, including distinct clients with the same name.
- Find clients by a name fragment, phone-number fragment or exact tag.
- Open a complete client profile with `view ID`, including contact details, sorted tags and the pending action. Permanent Client IDs stay unchanged across searches, edits and follow-up sorting.
- Record, replace or clear one dated follow-up per client.
- Keep an all-client pending summary visible beside searches and the selected profile, grouped with `OVERDUE`, `DUE TODAY` and `UPCOMING` badges.
- Save changes automatically to local JSON and reload them after restarting. Failed saves preserve the previous state; invalid saved files open a protected session.

- Submit commands with Enter or Run and correct rejected input using explicit, scrollable error feedback.

PolicyHarbor supports one pending action per client. Multiple actions, follow-up history and notifications are outside v1.5.

## Get started

Install Java 25; Mac users should use the [prescribed Zulu JDK+FX distribution](https://se-education.org/guides/tutorials/javaInstallationMac.html). Download `PolicyHarbor.jar` from the [team's releases](https://github.com/AY2627S1-CS2103T-W13-3/tp/releases), open a terminal in its folder and run:

```bash
java -jar PolicyHarbor.jar
```

The first launch is empty. See the [User Guide](docs/UserGuide.md#quick-start) for a short walkthrough.

## Documentation and development

- [Product website](https://ay2627s1-cs2103t-w13-3.github.io/tp/)
- [User Guide](docs/UserGuide.md)
- [Developer Guide](docs/DeveloperGuide.md)
- [Development Setup](docs/SettingUp.md)
- [About the Team](docs/AboutUs.md)
- [Issue Tracker](https://github.com/AY2627S1-CS2103T-W13-3/tp/issues)

## Acknowledgements

PolicyHarbor evolves [AddressBook Level 3](https://github.com/se-edu/addressbook-level3), created by the [SE-EDU initiative](https://se-education.org). It retains AB3's Java/JavaFX architecture and test conventions. See the Developer Guide for acknowledgements of AI assistance.

## License

PolicyHarbor is distributed under the [MIT License](LICENSE).
