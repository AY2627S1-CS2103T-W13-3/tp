package seedu.address.model;

import static java.util.Objects.requireNonNull;
import static seedu.address.commons.util.CollectionUtil.requireAllNonNull;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;
import java.util.logging.Logger;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import seedu.address.commons.core.GuiSettings;
import seedu.address.commons.core.LogsCenter;
import seedu.address.model.person.Person;

/**
 * Represents the in-memory model of the address book data.
 */
public class ModelManager implements Model {
    private static final Logger logger = LogsCenter.getLogger(ModelManager.class);

    private final AddressBook addressBook;
    private final UserPrefs userPrefs;
    private final ObservableList<Person> displayedPersons = FXCollections.observableArrayList();
    private final ObservableList<Person> readOnlyDisplayedPersons =
            FXCollections.unmodifiableObservableList(displayedPersons);
    private final ReadOnlyObjectWrapper<LocalDate> today = new ReadOnlyObjectWrapper<>();
    private final ReadOnlyBooleanWrapper showingFollowUps = new ReadOnlyBooleanWrapper(false);
    private final boolean dataLoadingBlocked;
    private Predicate<Person> activePredicate = PREDICATE_SHOW_ALL_PERSONS;
    private boolean unsavedChanges;

    /**
     * Initializes a ModelManager with the given addressBook and userPrefs.
     */
    public ModelManager(ReadOnlyAddressBook addressBook, ReadOnlyUserPrefs userPrefs) {
        this(addressBook, userPrefs, LocalDate.now(), false);
    }

    /**
     * Initializes an ordinary full-list view with the supplied startup date and loading state.
     */
    public ModelManager(ReadOnlyAddressBook addressBook, ReadOnlyUserPrefs userPrefs,
            LocalDate today, boolean isDataLoadingBlocked) {
        requireAllNonNull(addressBook, userPrefs, today);
        logger.fine("Initializing model");
        this.addressBook = new AddressBook(addressBook);
        this.userPrefs = new UserPrefs(userPrefs);
        this.today.set(today);
        dataLoadingBlocked = isDataLoadingBlocked;
        refreshDisplayedPersons();
    }

    public ModelManager() {
        this(new AddressBook(), new UserPrefs());
    }

    //=========== UserPrefs ==================================================================================

    @Override
    public ReadOnlyUserPrefs getUserPrefs() {
        return userPrefs;
    }

    @Override
    public GuiSettings getGuiSettings() {
        return userPrefs.getGuiSettings();
    }

    @Override
    public void setGuiSettings(GuiSettings guiSettings) {
        requireNonNull(guiSettings);
        userPrefs.setGuiSettings(guiSettings);
    }

    //=========== AddressBook ================================================================================

    @Override
    public void setAddressBook(ReadOnlyAddressBook addressBook) {
        this.addressBook.resetData(addressBook);
        unsavedChanges = true;
        refreshDisplayedPersons();
    }

    @Override
    public ReadOnlyAddressBook getAddressBook() {
        return addressBook;
    }

    @Override
    public boolean hasPerson(Person person) {
        requireNonNull(person);
        return addressBook.hasPerson(person);
    }

    @Override
    public void deletePerson(Person target) {
        addressBook.removePerson(target);
        unsavedChanges = true;
        refreshDisplayedPersons();
    }

    @Override
    public void addPerson(Person person) {
        addressBook.addPerson(person);
        unsavedChanges = true;
        updateFilteredPersonList(PREDICATE_SHOW_ALL_PERSONS);
    }

    @Override
    public void setPerson(Person target, Person editedPerson) {
        requireAllNonNull(target, editedPerson);

        addressBook.setPerson(target, editedPerson);
        unsavedChanges = true;
        refreshDisplayedPersons();
    }

    //=========== Filtered Person List Accessors =============================================================

    /**
     * Returns the stable, unmodifiable list of persons currently displayed.
     * This list is maintained separately from the address book; model mutations and view changes
     * must explicitly refresh its contents through {@link #refreshDisplayedPersons()}.
     */
    @Override
    public ObservableList<Person> getFilteredPersonList() {
        return readOnlyDisplayedPersons;
    }

    @Override
    public void updateFilteredPersonList(Predicate<Person> predicate) {
        requireNonNull(predicate);
        activePredicate = predicate;
        showingFollowUps.set(false);
        refreshDisplayedPersons();
    }

    @Override
    public void showPendingFollowUps() {
        activePredicate = PREDICATE_SHOW_ALL_PERSONS;
        showingFollowUps.set(true);
        refreshDisplayedPersons();
    }

    @Override
    public LocalDate getToday() {
        return today.get();
    }

    @Override
    public ReadOnlyObjectProperty<LocalDate> todayProperty() {
        return today.getReadOnlyProperty();
    }

    @Override
    public ReadOnlyBooleanProperty showingFollowUpsProperty() {
        return showingFollowUps.getReadOnlyProperty();
    }

    @Override
    public boolean isDataLoadingBlocked() {
        return dataLoadingBlocked;
    }

    @Override
    public Model forkForCommand(LocalDate commandToday) {
        ModelManager candidate = new ModelManager(addressBook, userPrefs, commandToday, dataLoadingBlocked);
        candidate.activePredicate = activePredicate;
        candidate.showingFollowUps.set(showingFollowUps.get());
        candidate.refreshDisplayedPersons();
        return candidate;
    }

    @Override
    public boolean hasUnsavedChanges() {
        return unsavedChanges;
    }

    @Override
    public void commitFrom(Model candidate) {
        // Candidates originate from forkForCommand; their data is already validated.
        ModelManager commandModel = (ModelManager) requireNonNull(candidate);
        if (!addressBook.equals(commandModel.addressBook)) {
            addressBook.resetData(commandModel.addressBook);
        }
        userPrefs.setGuiSettings(commandModel.getGuiSettings());
        activePredicate = commandModel.activePredicate;
        showingFollowUps.set(commandModel.showingFollowUps.get());
        today.set(commandModel.getToday());
        unsavedChanges = false;
        refreshDisplayedPersons();
    }

    /** Rebuilds the derived view once without changing the full list's insertion order. */
    private void refreshDisplayedPersons() {
        List<Person> visible = new ArrayList<>();
        for (Person person : addressBook.getPersonList()) {
            if (showingFollowUps.get() ? person.getFollowUp().isPresent() : activePredicate.test(person)) {
                visible.add(person);
            }
        }
        if (showingFollowUps.get()) {
            visible.sort(Comparator.comparing(person -> person.getFollowUp().orElseThrow().getDueDate()));
        }
        displayedPersons.setAll(visible);
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof ModelManager otherModelManager)) {
            return false;
        }

        return addressBook.equals(otherModelManager.addressBook)
                && userPrefs.equals(otherModelManager.userPrefs)
                && displayedPersons.equals(otherModelManager.displayedPersons)
                && showingFollowUps.get() == otherModelManager.showingFollowUps.get()
                && getToday().equals(otherModelManager.getToday())
                && dataLoadingBlocked == otherModelManager.dataLoadingBlocked;
    }

}
