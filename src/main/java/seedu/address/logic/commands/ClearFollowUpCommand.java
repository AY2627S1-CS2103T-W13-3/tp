package seedu.address.logic.commands;

import static java.util.Objects.requireNonNull;

import seedu.address.commons.util.ToStringBuilder;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.Model;
import seedu.address.model.person.ClientId;
import seedu.address.model.person.Person;

/** Clears a client's pending follow-up while preserving the client and the active view. */
public final class ClearFollowUpCommand extends Command {
    public static final String MESSAGE_CLEARED = "Follow-up for client ID %1$s: %2$s cleared.";
    public static final String MESSAGE_NO_FOLLOW_UP = "This client has no pending follow-up.";

    private final ClientId targetId;

    public ClearFollowUpCommand(ClientId targetId) {
        this.targetId = requireNonNull(targetId);
    }

    @Override
    public CommandResult execute(Model model) throws CommandException {
        requireNonNull(model);
        Person target = ClientIdResolver.requireClient(model, targetId);
        if (target.getFollowUp().isEmpty()) {
            throw new CommandException(MESSAGE_NO_FOLLOW_UP);
        }

        model.setPerson(target, target.withoutFollowUp());
        return new CommandResult(String.format(MESSAGE_CLEARED, targetId, target.getName()));
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof ClearFollowUpCommand otherCommand)) {
            return false;
        }
        return targetId.equals(otherCommand.targetId);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .add("targetId", targetId)
                .toString();
    }
}
