package seedu.address.logic.commands;

import static java.util.Objects.requireNonNull;
import static seedu.address.logic.parser.CliSyntax.PREFIX_ADDRESS;
import static seedu.address.logic.parser.CliSyntax.PREFIX_EMAIL;
import static seedu.address.logic.parser.CliSyntax.PREFIX_NAME;
import static seedu.address.logic.parser.CliSyntax.PREFIX_PHONE;
import static seedu.address.logic.parser.CliSyntax.PREFIX_TAG;

import seedu.address.commons.util.ToStringBuilder;
import seedu.address.logic.Messages;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.Model;
import seedu.address.model.exceptions.ClientIdExhaustedException;
import seedu.address.model.person.ClientId;
import seedu.address.model.person.Person;

/**
 * Adds a person to the address book.
 */
public class AddCommand extends Command {

    public static final String COMMAND_WORD = "add";

    public static final String MESSAGE_USAGE = "Usage: add "
            + PREFIX_NAME + "NAME " + PREFIX_PHONE + "PHONE_NUMBER " + PREFIX_EMAIL + "EMAIL "
            + PREFIX_ADDRESS + "ADDRESS [" + PREFIX_TAG + "TAG]...";

    public static final String MESSAGE_SUCCESS = "Client ID %1$s added: %2$s";
    public static final String MESSAGE_DUPLICATE_PERSON = "This client already exists in PolicyHarbor.";
    public static final String MESSAGE_CLIENT_ID_EXHAUSTED =
            "No more clients can be added because the client ID limit has been reached.";

    private final Person toAdd;

    /**
     * Creates an AddCommand to add the specified {@code Person}
     */
    public AddCommand(Person person) {
        requireNonNull(person);
        toAdd = person;
    }

    @Override
    public CommandResult execute(Model model) throws CommandException {
        requireNonNull(model);

        if (model.hasPerson(toAdd)) {
            throw new CommandException(MESSAGE_DUPLICATE_PERSON);
        }

        try {
            model.addPerson(toAdd);
        } catch (ClientIdExhaustedException e) {
            throw new CommandException(MESSAGE_CLIENT_ID_EXHAUSTED, e);
        }
        ClientId clientId = model.getAddressBook().getClientId(toAdd)
                .orElseThrow(() -> new IllegalStateException("Added client has no assigned ID."));
        return new CommandResult(String.format(MESSAGE_SUCCESS, clientId, Messages.format(toAdd)));
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof AddCommand otherAddCommand)) {
            return false;
        }

        return toAdd.equals(otherAddCommand.toAdd);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .add("toAdd", toAdd)
                .toString();
    }
}
