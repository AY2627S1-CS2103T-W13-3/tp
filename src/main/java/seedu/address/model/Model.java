package seedu.address.model;

import java.time.LocalDate;
import java.util.function.Predicate;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.collections.ObservableList;
import seedu.address.commons.core.GuiSettings;
import seedu.address.model.person.Person;

/**
 * The API of the Model component.
 */
public interface Model {
    /** {@code Predicate} that always evaluates to true */
    Predicate<Person> PREDICATE_SHOW_ALL_PERSONS = unused -> true;

    /**
     * Returns the user prefs.
     */
    ReadOnlyUserPrefs getUserPrefs();

    /**
     * Returns the user prefs' GUI settings.
     */
    GuiSettings getGuiSettings();

    /**
     * Sets the user prefs' GUI settings.
     */
    void setGuiSettings(GuiSettings guiSettings);

    /**
     * Replaces address book data with the data in {@code addressBook}.
     */
    void setAddressBook(ReadOnlyAddressBook addressBook);

    /** Returns the AddressBook */
    ReadOnlyAddressBook getAddressBook();

    /**
     * Returns true if a person with the same identity as {@code person} exists in the address book.
     */
    boolean hasPerson(Person person);

    /**
     * Deletes the given person.
     * The person must exist in the address book.
     */
    void deletePerson(Person target);

    /**
     * Adds the given person.
     * {@code person} must not already exist in the address book.
     */
    void addPerson(Person person);

    /**
     * Replaces the given person {@code target} with {@code editedPerson}.
     * {@code target} must exist in the address book.
     * The person identity of {@code editedPerson} must not be the same as another existing person in the address book.
     */
    void setPerson(Person target, Person editedPerson);

    /** Shows all pending follow-ups in due-date order, retaining insertion order for ties. */
    void showPendingFollowUps();

    /** Returns the date snapshot used for this model's command and displayed statuses. */
    LocalDate getToday();

    /** Returns the stable, read-only date property. */
    ReadOnlyObjectProperty<LocalDate> todayProperty();

    /** Returns whether the displayed list is in pending-follow-up mode. */
    ReadOnlyBooleanProperty showingFollowUpsProperty();

    /** Returns whether startup loading failed and client mutations must be blocked. */
    boolean isDataLoadingBlocked();

    /**
     * Copies data, preferences and view state into an isolated, initially clean command candidate.
     * The caller supplies the command's sampled date. Discard the candidate on failure.
     */
    Model forkForCommand(LocalDate today);

    /** Returns whether a successful client-data mutation has occurred since creation/publication. */
    boolean hasUnsavedChanges();

    /**
     * Publishes a validated candidate produced by {@link #forkForCommand(LocalDate)}.
     * The caller must save dirty candidates successfully before calling this method.
     * Performs no file I/O and retains the live observable list and properties.
     */
    void commitFrom(Model candidate);

    /** Returns the stable, unmodifiable displayed list, authoritative for command indices. */
    ObservableList<Person> getFilteredPersonList();

    /**
     * Updates the filter of the filtered person list to filter by the given {@code predicate}.
     * @throws NullPointerException if {@code predicate} is null.
     */
    void updateFilteredPersonList(Predicate<Person> predicate);
}
