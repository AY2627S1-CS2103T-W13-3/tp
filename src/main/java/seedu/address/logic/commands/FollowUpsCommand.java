package seedu.address.logic.commands;

import static java.util.Objects.requireNonNull;

import seedu.address.model.Model;

/**
 * Shows all clients with pending follow-ups in due-date order.
 */
public final class FollowUpsCommand extends Command {

    public static final String COMMAND_WORD = "followups";
    public static final String MESSAGE_PENDING = "Pending follow-ups";
    public static final String MESSAGE_NO_PENDING = "There are no pending follow-ups.";

    @Override
    public CommandResult execute(Model model) {
        requireNonNull(model);
        model.showPendingFollowUps();
        return new CommandResult(model.getFilteredPersonList().isEmpty() ? MESSAGE_NO_PENDING : MESSAGE_PENDING);
    }
}
