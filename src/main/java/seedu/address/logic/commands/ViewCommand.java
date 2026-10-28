package seedu.address.logic.commands;

import static java.util.Objects.requireNonNull;

import seedu.address.commons.util.ToStringBuilder;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.Model;
import seedu.address.model.person.ClientId;
import seedu.address.model.person.Person;

/**
 * Selects a client using its permanent ID, without changing client data.
 */
public final class ViewCommand extends Command {
    public static final String COMMAND_WORD = "view";
    public static final String MESSAGE_USAGE = "Usage: view ID";
    public static final String MESSAGE_SUCCESS = "Showing client ID %1$s: %2$s";

    private final ClientId targetId;

    public ViewCommand(ClientId targetId) {
        this.targetId = requireNonNull(targetId);
    }

    @Override
    public CommandResult execute(Model model) throws CommandException {
        requireNonNull(model);
        Person person = ClientIdResolver.requireClient(model, targetId);
        if (model.getFilteredPersonList().stream().noneMatch(displayed -> displayed == person)) {
            model.updateFilteredPersonList(Model.PREDICATE_SHOW_ALL_PERSONS);
        }
        model.selectPerson(person);
        return new CommandResult(String.format(MESSAGE_SUCCESS, targetId, person.getName()));
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        return other instanceof ViewCommand otherViewCommand && targetId.equals(otherViewCommand.targetId);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).add("targetId", targetId).toString();
    }
}
