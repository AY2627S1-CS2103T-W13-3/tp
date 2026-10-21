package seedu.address.logic;

import java.time.LocalDate;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.collections.ObservableList;
import seedu.address.commons.core.GuiSettings;
import seedu.address.logic.commands.CommandResult;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.person.Person;

/**
 * API of the Logic component
 */
public interface Logic {
    /**
     * Executes the command and returns the result.
     * @param commandText The command as entered by the user.
     * @return the result of the command execution.
     * @throws CommandException If an error occurs during command execution.
     * @throws ParseException If an error occurs during parsing.
     */
    CommandResult execute(String commandText) throws CommandException, ParseException;

    /** Returns an unmodifiable view of the filtered list of persons */
    ObservableList<Person> getFilteredPersonList();

    /**
     * Returns the stable, unmodifiable observable list of all committed clients in insertion order.
     * Filtering and pending-list ordering do not affect this feed. Successful data changes are
     * published only after saving; rejected commands and failed saves leave it unchanged.
     * Elements are the canonical immutable records, including distinct clients with the same name.
     */
    ObservableList<Person> getAllPersonList();

    /** Returns the date snapshot used for displayed follow-up statuses. */
    ReadOnlyObjectProperty<LocalDate> todayProperty();

    /** Refreshes the shared status date from the current local date. */
    void refreshToday();

    /** Returns whether the pending follow-ups view is showing. */
    ReadOnlyBooleanProperty showingFollowUpsProperty();

    /** Returns the live, read-only selected-client property; null means no selection. */
    ReadOnlyObjectProperty<Person> selectedPersonProperty();

    /** Returns whether invalid saved data has blocked client changes. */
    boolean isDataLoadingBlocked();

    /**
     * Returns the user prefs' GUI settings.
     */
    GuiSettings getGuiSettings();

    /**
     * Set the user prefs' GUI settings.
     */
    void setGuiSettings(GuiSettings guiSettings);
}
