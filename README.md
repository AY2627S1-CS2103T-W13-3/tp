# PolicyHarbor

[![Java CI](https://github.com/AY2627S1-CS2103T-W13-3/tp/actions/workflows/gradle.yml/badge.svg?branch=master)](https://github.com/AY2627S1-CS2103T-W13-3/tp/actions/workflows/gradle.yml)

**Clients. Follow-ups. Made simple.**

PolicyHarbor is a desktop application being developed for individual insurance agents to manage client contact details and keep track of their next follow-up. It combines typed commands with a graphical interface, helping agents who prefer the keyboard retrieve client information and review pending actions in one place.

Whether the next step is sending a quotation, discussing a policy renewal or arranging an annual review, PolicyHarbor is designed to keep the client and the action together.

## Intended interface

![PolicyHarbor UI mockup showing the command box, client list, client profile and pending follow-ups](docs/images/Ui.png)

*Mockup of the intended product. PolicyHarbor is under active development; the interface and planned features below do not describe a completed release.*

## Planned core features

The first release focuses on the everyday client follow-up workflow:

| Feature | What it will let you do |
| --- | --- |
| Add clients | Record a client or prospect's name, phone number, email and address. |
| List clients | Review all clients, including those without a pending follow-up. |
| View a profile | See a client's full contact details and next action. |
| Manage follow-ups | Record or replace one pending action per client, with a due date and short description, or clear it when it is no longer needed. |
| Review pending actions | List follow-ups by due date, with overdue, due-today and upcoming status labels. |
| Delete clients | Remove a client record and its associated follow-up. |
| Save automatically | Keep client records and pending follow-ups locally between sessions. |

Name search and client tags are optional additions to the initial scope. The MVP focuses on one next action per client; multiple follow-ups, follow-up history and notifications are outside that scope.

## Documentation and development

PolicyHarbor is built with Java and JavaFX. The documentation is being updated alongside the application as the planned workflow is implemented.

- [User Guide](docs/UserGuide.md)
- [Developer Guide](docs/DeveloperGuide.md)
- [Development Setup](docs/SettingUp.md)
- [About the Team](docs/AboutUs.md)
- [Issue Tracker](https://github.com/AY2627S1-CS2103T-W13-3/tp/issues)

## Acknowledgements

This project is based on the AddressBook-Level3 project created by the [SE-EDU initiative](https://se-education.org).

## License

PolicyHarbor is distributed under the [MIT License](LICENSE).
