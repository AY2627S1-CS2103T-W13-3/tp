package seedu.address.logic.commands;

import static java.util.Objects.requireNonNull;

import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.Model;
import seedu.address.model.person.ClientId;
import seedu.address.model.person.Person;

/** Resolves permanent client IDs against the entire saved collection. */
final class ClientIdResolver {
    private ClientIdResolver() {
    }

    static Person requireClient(Model model, ClientId id) throws CommandException {
        requireNonNull(model);
        requireNonNull(id);
        return model.getAddressBook().getPersonById(id)
                .orElseThrow(() -> new CommandException("No client with ID " + id + "."));
    }
}
