package seedu.address.logic.commands;

import static java.util.Objects.requireNonNull;

import java.util.List;

import seedu.address.commons.core.index.Index;
import seedu.address.commons.util.ToStringBuilder;
import seedu.address.logic.Messages;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.Model;
import seedu.address.model.person.Person;

/** Clears a client's pending follow-up while preserving the client and the active view. */
public final class ClearFollowUpCommand extends Command {
    public static final String MESSAGE_CLEARED = "Follow-up for %1$s cleared.";
    public static final String MESSAGE_NO_FOLLOW_UP = "This client has no pending follow-up.";

    private final Index targetIndex;

    public ClearFollowUpCommand(Index targetIndex) {
        this.targetIndex = requireNonNull(targetIndex);
    }

    @Override
    public CommandResult execute(Model model) throws CommandException {
        requireNonNull(model);
        List<Person> displayed = model.getFilteredPersonList();
        if (targetIndex.getZeroBased() >= displayed.size()) {
            throw new CommandException(Messages.MESSAGE_INVALID_PERSON_DISPLAYED_INDEX);
        }
        Person target = displayed.get(targetIndex.getZeroBased());
        if (target.getFollowUp().isEmpty()) {
            throw new CommandException(MESSAGE_NO_FOLLOW_UP);
        }

        model.setPerson(target, target.withoutFollowUp());
        return new CommandResult(String.format(MESSAGE_CLEARED, target.getName()));
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof ClearFollowUpCommand otherCommand)) {
            return false;
        }
        return targetIndex.equals(otherCommand.targetIndex);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .add("targetIndex", targetIndex)
                .toString();
    }
}
