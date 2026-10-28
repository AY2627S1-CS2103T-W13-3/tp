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
     * Replaces client data, permanent IDs and the allocation counter with the supplied aggregate state.
     * Reconciles selection by ID and retains the active display mode and predicate.
     */
    void setAddressBook(ReadOnlyAddressBook addressBook);

    /** Returns the AddressBook */
    ReadOnlyAddressBook getAddressBook();

    /**
     * Returns true if an existing record duplicates {@code person} according to
     * {@link Person#isDuplicateOf(Person)}. This is not a selection identity check.
     */
    boolean hasPerson(Person person);

    /**
     * Deletes the given canonical client resolved globally by permanent ID.
     * The person must exist in the address book.
     */
    void deletePerson(Person target);

    /**
     * Adds the given person using the aggregate's next permanent ID, retaining the active display mode.
     * {@code person} must not already exist in the address book.
     */
    void addPerson(Person person);

    /**
     * Replaces the given person {@code target} with {@code editedPerson}, retaining its permanent ID.
     * Re-evaluates the active predicate and retains selection only while the client remains visible.
     * {@code target} must exist in the address book.
     * {@code editedPerson} must not duplicate any record other than the replaced target.
     */
    void setPerson(Person target, Person editedPerson);

    /** Shows all pending follow-ups in due-date order, retaining insertion order for ties. */
    void showPendingFollowUps();

    /** Returns the date snapshot used for this model's command and displayed statuses. */
    LocalDate getToday();

    /** Updates the status date without changing client data or the displayed list. */
    void updateToday(LocalDate today);

    /** Returns the stable, read-only date property. */
    ReadOnlyObjectProperty<LocalDate> todayProperty();

    /** Returns whether the displayed list is in pending-follow-up mode. */
    ReadOnlyBooleanProperty showingFollowUpsProperty();

    /**
     * Returns the stable, read-only selected-client property; null means no selection.
     */
    ReadOnlyObjectProperty<Person> selectedPersonProperty();

    /**
     * Selects the exact canonical record in the current displayed list, without marking data dirty.
     * Only the view command explicitly selects a client; it resolves permanent IDs globally and
     * restores the full list when necessary before selecting a hidden client. Other operations
     * reconcile the selected ID to the committed canonical instance or clear it.
     *
     * @throws NullPointerException if {@code person} is null.
     * @throws IllegalArgumentException if that instance is not in the current displayed list.
     */
    void selectPerson(Person person);

    /** Returns whether startup loading failed and client mutations must be blocked. */
    boolean isDataLoadingBlocked();

    /**
     * Copies data, preferences and view state, including selection, into an isolated, initially clean candidate.
     * The aggregate copy preserves IDs/counter with an independent identity ledger.
     * The caller supplies the command's sampled date. Discard the candidate on failure.
     */
    Model forkForCommand(LocalDate today);

    /** Returns whether a successful client-data mutation has occurred since creation/publication. */
    boolean hasUnsavedChanges();

    /**
     * Publishes a validated candidate produced by {@link #forkForCommand(LocalDate)}.
     * The caller must save dirty candidates successfully before calling this method.
     * Performs no file I/O and retains the live observable list and properties.
     * Publishes identity metadata even when contact values compare equal, and resolves
     * candidate selection by permanent ID rather than displayed or insertion-order offsets.
     */
    void commitFrom(Model candidate);

    /** Returns the stable, unmodifiable display list; command targets use permanent IDs, not row positions. */
    ObservableList<Person> getFilteredPersonList();

    /**
     * Updates the filter of the filtered person list to filter by the given {@code predicate}.
     * @throws NullPointerException if {@code predicate} is null.
     */
    void updateFilteredPersonList(Predicate<Person> predicate);
}
