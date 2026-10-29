package seedu.address.logic.commands;

import static java.util.Objects.requireNonNull;
import static seedu.address.commons.util.CollectionUtil.requireAllNonNull;

import java.time.LocalDate;

import seedu.address.commons.util.ToStringBuilder;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.Model;
import seedu.address.model.person.ClientId;
import seedu.address.model.person.FollowUp;
import seedu.address.model.person.Person;

/** Records or replaces the pending follow-up of a client identified by permanent ID. */
public final class SetFollowUpCommand extends Command {
    public static final String MESSAGE_USAGE = "Usage: followup ID d/DATE m/DESCRIPTION\n"
            + "Or: followup ID clear";
    public static final String MESSAGE_RECORDED = "Follow-up for client ID %1$s: %2$s recorded for %3$s.";
    public static final String MESSAGE_UPDATED = "Follow-up for client ID %1$s: %2$s updated to %3$s.";

    private final ClientId targetId;
    private final String dateText;
    private final String descriptionText;

    /**
     * Keeps raw values so execution can report an unknown client ID before invalid field values.
     */
    public SetFollowUpCommand(ClientId targetId, String dateText, String descriptionText) {
        requireAllNonNull(targetId, dateText, descriptionText);
        this.targetId = targetId;
        this.dateText = dateText;
        this.descriptionText = descriptionText;
    }

    @Override
    public CommandResult execute(Model model) throws CommandException {
        requireNonNull(model);
        Person target = ClientIdResolver.requireClient(model, targetId);
        FollowUp followUp;
        try {
            LocalDate dueDate = FollowUp.parseDate(dateText);
            followUp = new FollowUp(dueDate, descriptionText);
        } catch (IllegalArgumentException e) {
            throw new CommandException(e.getMessage(), e);
        }

        model.setPerson(target, target.withFollowUp(followUp));
        String message = target.getFollowUp().isPresent() ? MESSAGE_UPDATED : MESSAGE_RECORDED;
        return new CommandResult(String.format(message, targetId, target.getName(), followUp.getDueDate()));
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof SetFollowUpCommand otherCommand)) {
            return false;
        }
        return targetId.equals(otherCommand.targetId)
                && dateText.equals(otherCommand.dateText)
                && descriptionText.equals(otherCommand.descriptionText);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .add("targetId", targetId)
                .add("dateText", dateText)
                .add("descriptionText", descriptionText)
                .toString();
    }
}
