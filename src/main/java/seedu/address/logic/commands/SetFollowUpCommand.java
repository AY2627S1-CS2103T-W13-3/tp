package seedu.address.logic.commands;

import static java.util.Objects.requireNonNull;
import static seedu.address.commons.util.CollectionUtil.requireAllNonNull;

import java.time.LocalDate;
import java.util.List;

import seedu.address.commons.core.index.Index;
import seedu.address.commons.util.ToStringBuilder;
import seedu.address.logic.Messages;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.Model;
import seedu.address.model.person.FollowUp;
import seedu.address.model.person.Person;

/** Records or replaces the pending follow-up of a client in the displayed list. */
public final class SetFollowUpCommand extends Command {
    public static final String MESSAGE_USAGE = "Usage: followup INDEX d/DATE m/DESCRIPTION\n"
            + "Or: followup INDEX clear";
    public static final String MESSAGE_RECORDED = "Follow-up for %1$s recorded for %2$s.";
    public static final String MESSAGE_UPDATED = "Follow-up for %1$s updated to %2$s.";
    public static final String MESSAGE_PAST_DATE = "Follow-up date cannot be in the past.";

    private final Index targetIndex;
    private final String dateText;
    private final String descriptionText;

    /**
     * Keeps raw values so execution can report an invalid displayed index before invalid field values.
     */
    public SetFollowUpCommand(Index targetIndex, String dateText, String descriptionText) {
        requireAllNonNull(targetIndex, dateText, descriptionText);
        this.targetIndex = targetIndex;
        this.dateText = dateText;
        this.descriptionText = descriptionText;
    }

    @Override
    public CommandResult execute(Model model) throws CommandException {
        requireNonNull(model);
        List<Person> displayed = model.getFilteredPersonList();
        if (targetIndex.getZeroBased() >= displayed.size()) {
            throw new CommandException(Messages.MESSAGE_INVALID_PERSON_DISPLAYED_INDEX);
        }
        Person target = displayed.get(targetIndex.getZeroBased());
        FollowUp followUp;
        try {
            LocalDate dueDate = FollowUp.parseDate(dateText);
            if (dueDate.isBefore(model.getToday())) {
                throw new CommandException(MESSAGE_PAST_DATE);
            }
            followUp = new FollowUp(dueDate, descriptionText);
        } catch (IllegalArgumentException e) {
            throw new CommandException(e.getMessage(), e);
        }

        model.setPerson(target, target.withFollowUp(followUp));
        String message = target.getFollowUp().isPresent() ? MESSAGE_UPDATED : MESSAGE_RECORDED;
        return new CommandResult(String.format(message, target.getName(), followUp.getDueDate()));
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof SetFollowUpCommand otherCommand)) {
            return false;
        }
        return targetIndex.equals(otherCommand.targetIndex)
                && dateText.equals(otherCommand.dateText)
                && descriptionText.equals(otherCommand.descriptionText);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .add("targetIndex", targetIndex)
                .add("dateText", dateText)
                .add("descriptionText", descriptionText)
                .toString();
    }
}
