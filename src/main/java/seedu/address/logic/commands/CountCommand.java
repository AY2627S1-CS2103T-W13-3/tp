package seedu.address.logic.commands;

import static java.util.Objects.requireNonNull;

import seedu.address.model.Model;

/**
 * Shows the number of persons in the currently displayed list.
 */
public class CountCommand extends Command {

    public static final String COMMAND_WORD = "count";

    public static final String MESSAGE_SUCCESS = "%1$d person%2$s listed.";

    @Override
    public CommandResult execute(Model model) {
        requireNonNull(model);

        int personCount = model.getFilteredPersonList().size();
        String pluralSuffix = personCount == 1 ? "" : "s";
        return new CommandResult(String.format(MESSAGE_SUCCESS, personCount, pluralSuffix));
    }
}
