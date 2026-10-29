package seedu.address.model;

import static java.util.Objects.requireNonNull;
import static seedu.address.commons.util.AppUtil.checkArgument;
import static seedu.address.commons.util.CollectionUtil.requireAllNonNull;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
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
import seedu.address.model.person.ClientId;
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
    private final ReadOnlyObjectWrapper<Person> selectedPerson = new ReadOnlyObjectWrapper<>();
    private final boolean isDataLoadingBlocked;
    private Predicate<Person> activePredicate = PREDICATE_SHOW_ALL_PERSONS;
    private boolean hasUnsavedChanges;

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
        this.isDataLoadingBlocked = isDataLoadingBlocked;
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
        Optional<ClientId> selectedId = getSelectedClientId();
        this.addressBook.resetData(addressBook);
        hasUnsavedChanges = true;
        refreshDisplayedPersons(resolveSelection(selectedId));
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
        hasUnsavedChanges = true;
        refreshDisplayedPersons();
    }

    @Override
    public void addPerson(Person person) {
        addressBook.addPerson(person);
        hasUnsavedChanges = true;
        refreshDisplayedPersons();
    }

    @Override
    public void setPerson(Person target, Person editedPerson) {
        requireAllNonNull(target, editedPerson);
        Optional<ClientId> selectedId = getSelectedClientId();
        addressBook.setPerson(target, editedPerson);
        hasUnsavedChanges = true;
        refreshDisplayedPersons(resolveSelection(selectedId));
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
    public void updateToday(LocalDate today) {
        requireNonNull(today);
        if (!today.equals(this.today.get())) {
            this.today.set(today);
        }
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
    public ReadOnlyObjectProperty<Person> selectedPersonProperty() {
        return selectedPerson.getReadOnlyProperty();
    }

    @Override
    public void selectPerson(Person person) {
        requireNonNull(person);
        checkArgument(containsSameInstance(displayedPersons, person),
                "Selected client must be a canonical member of the displayed list.");
        selectedPerson.set(person);
    }

    @Override
    public boolean isDataLoadingBlocked() {
        return isDataLoadingBlocked;
    }

    @Override
    public Model forkForCommand(LocalDate commandToday) {
        ModelManager candidate = new ModelManager(addressBook, userPrefs, commandToday, isDataLoadingBlocked);
        candidate.copyListModeFrom(this);
        // The aggregate copy retains IDs and counter while isolating its mutable ledger.
        candidate.refreshDisplayedPersons(candidate.resolveSelection(getSelectedClientId()));
        return candidate;
    }

    @Override
    public boolean hasUnsavedChanges() {
        return hasUnsavedChanges;
    }

    @Override
    public void commitFrom(Model candidate) {
        // Candidates originate from forkForCommand; their data is already validated.
        ModelManager commandModel = (ModelManager) requireNonNull(candidate);
        Optional<ClientId> selectedId = commandModel.getSelectedClientId();
        if (!addressBook.equals(commandModel.addressBook)) {
            addressBook.resetData(commandModel.addressBook);
        }
        userPrefs.setGuiSettings(commandModel.getGuiSettings());
        copyListModeFrom(commandModel);
        today.set(commandModel.getToday());
        hasUnsavedChanges = false;
        // An equal-valued replacement may skip resetData. Bind to the actual committed instance.
        refreshDisplayedPersons(resolveSelection(selectedId));
        assert !hasUnsavedChanges() : "A published command must leave the live model clean.";
    }

    /**
     * Rebuilds the derived view once without changing the full list's insertion order.
     */
    private void refreshDisplayedPersons() {
        refreshDisplayedPersons(selectedPerson.get());
    }

    /**
     * Rebuilds the view, retaining the requested canonical selection only while it remains visible.
     */
    private void refreshDisplayedPersons(Person selection) {
        List<Person> visible = createDisplayedSnapshot();
        displayedPersons.setAll(visible);
        selectedPerson.set(containsSameInstance(visible, selection) ? selection : null);
        // Check settled state only, after both the displayed list and selection have been refreshed.
        assert selectedPerson.get() == null || addressBook.getClientId(selectedPerson.get()).isPresent()
                : "The selected client must have a canonical committed identity.";
    }

    /** Computes the derived order without publishing list or selection changes. */
    private List<Person> createDisplayedSnapshot() {
        List<Person> visible = new ArrayList<>();
        for (Person person : addressBook.getPersonList()) {
            if (showingFollowUps.get() ? person.getFollowUp().isPresent() : activePredicate.test(person)) {
                visible.add(person);
            }
        }
        if (showingFollowUps.get()) {
            visible.sort(Comparator.comparing(person -> person.getFollowUp().orElseThrow().getDueDate()));
        }
        return visible;
    }

    /** Copies list mode only; the command date and canonical selection are reconciled separately. */
    private void copyListModeFrom(ModelManager source) {
        activePredicate = source.activePredicate;
        showingFollowUps.set(source.showingFollowUps.get());
    }

    /** Tests canonical instance membership; Person value equality is not record identity. */
    private static boolean containsSameInstance(List<Person> persons, Person target) {
        return persons.stream().anyMatch(person -> person == target);
    }

    /** Returns permanent identity for the selected canonical client, without allocating an ID. */
    private Optional<ClientId> getSelectedClientId() {
        return selectedPerson.get() == null ? Optional.empty() : addressBook.getClientId(selectedPerson.get());
    }

    /** Rebinds a selection to this aggregate's canonical instance; visibility is checked on refresh. */
    private Person resolveSelection(Optional<ClientId> selectedId) {
        return selectedId.flatMap(addressBook::getPersonById).orElse(null);
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
                && Objects.equals(selectedPerson.get(), otherModelManager.selectedPerson.get())
                && isDataLoadingBlocked == otherModelManager.isDataLoadingBlocked;
    }

}
