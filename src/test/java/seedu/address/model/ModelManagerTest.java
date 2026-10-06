package seedu.address.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.model.Model.PREDICATE_SHOW_ALL_PERSONS;
import static seedu.address.testutil.Assert.assertThrows;
import static seedu.address.testutil.TypicalPersons.ALICE;
import static seedu.address.testutil.TypicalPersons.BENSON;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import seedu.address.commons.core.GuiSettings;
import seedu.address.model.person.FollowUp;
import seedu.address.model.person.NameContainsKeywordsPredicate;
import seedu.address.model.person.Person;
import seedu.address.model.person.exceptions.DuplicatePersonException;
import seedu.address.model.person.exceptions.PersonNotFoundException;
import seedu.address.testutil.AddressBookBuilder;
import seedu.address.testutil.PersonBuilder;

public class ModelManagerTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 4);

    private ModelManager modelManager = new ModelManager();

    @Test
    public void constructor() {
        assertEquals(new UserPrefs(), modelManager.getUserPrefs());
        assertEquals(new GuiSettings(), modelManager.getGuiSettings());
        assertEquals(new AddressBook(), new AddressBook(modelManager.getAddressBook()));
    }

    @Test
    public void constructor_validUserPrefs_copiesUserPrefs() {
        UserPrefs userPrefs = new UserPrefs();
        userPrefs.setGuiSettings(new GuiSettings(1, 2, 3, 4));
        modelManager = new ModelManager(new AddressBook(), userPrefs);
        assertEquals(userPrefs, modelManager.getUserPrefs());

        // Modifying userPrefs should not modify modelManager's userPrefs
        UserPrefs oldUserPrefs = new UserPrefs(userPrefs);
        userPrefs.setGuiSettings(new GuiSettings(5, 6, 7, 8));
        assertEquals(oldUserPrefs, modelManager.getUserPrefs());
    }

    @Test
    public void setGuiSettings_nullGuiSettings_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> modelManager.setGuiSettings(null));
    }

    @Test
    public void setGuiSettings_validGuiSettings_setsGuiSettings() {
        GuiSettings guiSettings = new GuiSettings(1, 2, 3, 4);
        modelManager.setGuiSettings(guiSettings);
        assertEquals(guiSettings, modelManager.getGuiSettings());
    }

    @Test
    public void hasPerson_nullPerson_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> modelManager.hasPerson(null));
    }

    @Test
    public void hasPerson_personNotInAddressBook_returnsFalse() {
        assertFalse(modelManager.hasPerson(ALICE));
    }

    @Test
    public void hasPerson_personInAddressBook_returnsTrue() {
        modelManager.addPerson(ALICE);
        assertTrue(modelManager.hasPerson(ALICE));
    }

    @Test
    public void getFilteredPersonList_modifyList_throwsUnsupportedOperationException() {
        assertThrows(UnsupportedOperationException.class, () -> modelManager.getFilteredPersonList().remove(0));
    }

    @Test
    public void constructor_explicitState_exposesDateAndLoadingBlock() {
        Model model = new ModelManager(new AddressBook(), new UserPrefs(), TODAY, true);
        assertEquals(TODAY, model.getToday());
        assertEquals(TODAY, model.todayProperty().get());
        assertTrue(model.isDataLoadingBlocked());
        assertFalse(model.showingFollowUpsProperty().get());
        assertFalse(model.hasUnsavedChanges());
    }

    @Test
    public void showPendingFollowUps_afterSearch_sortsAllPendingAndPreservesSourceOrder() {
        Person late = withFollowUp(ALICE, 2);
        Person early = withFollowUp(BENSON, -1);
        Person tied = new PersonBuilder(withFollowUp(ALICE, -1)).withName("Cara").build();
        Person noFollowUp = new PersonBuilder().withName("David").build();
        Model model = modelWith(late, early, noFollowUp, tied);
        model.updateFilteredPersonList(person -> person.equals(late));

        model.showPendingFollowUps();

        assertEquals(List.of(early, tied, late), model.getFilteredPersonList());
        assertEquals(List.of(late, early, noFollowUp, tied), model.getAddressBook().getPersonList());
        assertTrue(model.showingFollowUpsProperty().get());
        assertFalse(model.hasUnsavedChanges());

        model.updateFilteredPersonList(person -> !person.equals(noFollowUp));
        assertEquals(List.of(late, early, tied), model.getFilteredPersonList());
        assertFalse(model.showingFollowUpsProperty().get());
        model.updateFilteredPersonList(PREDICATE_SHOW_ALL_PERSONS);
        assertEquals(List.of(late, early, noFollowUp, tied), model.getFilteredPersonList());
    }

    @Test
    public void setPerson_pendingMode_reordersThenClearsUsingDisplayedIndices() {
        Person late = withFollowUp(ALICE, 2);
        Person early = withFollowUp(BENSON, 1);
        Model model = modelWith(late, early);
        model.showPendingFollowUps();
        Person replacement = withFollowUp(early, 3);

        model.setPerson(model.getFilteredPersonList().get(0), replacement);
        assertEquals(List.of(late, replacement), model.getFilteredPersonList());
        assertTrue(model.showingFollowUpsProperty().get());

        Person cleared = new PersonBuilder(late).withoutFollowUp().build();
        model.setPerson(model.getFilteredPersonList().get(0), cleared);
        assertEquals(List.of(replacement), model.getFilteredPersonList());
        assertEquals(List.of(cleared, replacement), model.getAddressBook().getPersonList());
        model.deletePerson(model.getFilteredPersonList().get(0));
        assertTrue(model.getFilteredPersonList().isEmpty());
        assertEquals(List.of(cleared), model.getAddressBook().getPersonList());
        assertTrue(model.showingFollowUpsProperty().get());
    }

    @Test
    public void pendingTies_replacementDeletionAndRestart_preserveInsertionOrder() {
        Person first = withFollowUp(ALICE, 0);
        Person second = withFollowUp(BENSON, 0);
        Person third = new PersonBuilder(first).withName("Cara").build();
        Model model = modelWith(first, second, third);
        model.showPendingFollowUps();
        Person replacement = new PersonBuilder(first).withPhone("99998888").build();
        model.setPerson(first, replacement);
        assertEquals(List.of(replacement, second, third), model.getFilteredPersonList());
        model.deletePerson(second);
        assertEquals(List.of(replacement, third), model.getFilteredPersonList());

        Model restarted = new ModelManager(model.getAddressBook(), model.getUserPrefs(), TODAY, false);
        restarted.showPendingFollowUps();
        assertEquals(List.of(replacement, third), restarted.getFilteredPersonList());
    }

    @Test
    public void mutations_searchMode_reapplyPredicateAndAddResetsView() {
        Model model = modelWith(ALICE, BENSON);
        model.updateFilteredPersonList(person -> person.getName().fullName.startsWith("Alice"));
        Person renamed = new PersonBuilder(ALICE).withName("Zelda").build();
        model.setPerson(ALICE, renamed);
        assertTrue(model.getFilteredPersonList().isEmpty());
        model.deletePerson(BENSON);
        assertTrue(model.getFilteredPersonList().isEmpty());
        model.addPerson(BENSON);
        assertEquals(List.of(renamed, BENSON), model.getFilteredPersonList());
        assertFalse(model.showingFollowUpsProperty().get());

        model.showPendingFollowUps();
        model.addPerson(ALICE);
        assertEquals(List.of(renamed, BENSON, ALICE), model.getFilteredPersonList());
        assertFalse(model.showingFollowUpsProperty().get());
    }

    @Test
    public void forkForCommand_pendingView_copiesStateWithoutSharingMutableObjects() {
        Person late = withFollowUp(ALICE, 2);
        Person early = withFollowUp(BENSON, 1);
        Model live = new ModelManager(modelWith(late, early).getAddressBook(), new UserPrefs(), TODAY, true);
        live.showPendingFollowUps();
        live.setPerson(late, late);
        Model candidate = live.forkForCommand(TODAY.plusDays(1));

        assertEquals(List.of(early, late), candidate.getFilteredPersonList());
        assertTrue(candidate.showingFollowUpsProperty().get());
        assertTrue(candidate.isDataLoadingBlocked());
        assertEquals(TODAY.plusDays(1), candidate.getToday());
        assertFalse(candidate.hasUnsavedChanges());
        assertNotSame(live.getAddressBook(), candidate.getAddressBook());
        assertNotSame(live.getAddressBook().getPersonList(), candidate.getAddressBook().getPersonList());
        assertNotSame(live.getFilteredPersonList(), candidate.getFilteredPersonList());
        assertNotSame(live.getUserPrefs(), candidate.getUserPrefs());
        assertNotSame(live.todayProperty(), candidate.todayProperty());
        assertNotSame(live.showingFollowUpsProperty(), candidate.showingFollowUpsProperty());

        candidate.deletePerson(early);
        candidate.updateFilteredPersonList(PREDICATE_SHOW_ALL_PERSONS);
        candidate.setGuiSettings(new GuiSettings(1, 2, 3, 4));
        assertEquals(List.of(late, early), live.getAddressBook().getPersonList());
        assertEquals(List.of(early, late), live.getFilteredPersonList());
        assertTrue(live.showingFollowUpsProperty().get());
        assertEquals(TODAY, live.getToday());
        assertEquals(new GuiSettings(), live.getGuiSettings());

        live.deletePerson(late);
        assertEquals(List.of(late), candidate.getFilteredPersonList());
    }

    @Test
    public void forkForCommand_searchView_preservesPredicate() {
        Model live = modelWith(ALICE, BENSON);
        live.updateFilteredPersonList(person -> person.getName().fullName.startsWith("Alice"));
        Model candidate = live.forkForCommand(TODAY);
        assertEquals(List.of(ALICE), candidate.getFilteredPersonList());
        candidate.setPerson(ALICE, new PersonBuilder(ALICE).withName("Zelda").build());
        assertTrue(candidate.getFilteredPersonList().isEmpty());
        assertEquals(List.of(ALICE), live.getFilteredPersonList());
        assertFalse(candidate.showingFollowUpsProperty().get());
    }

    @Test
    public void hasUnsavedChanges_successfulDataMutations_marksDirtyEvenWhenEqual() {
        Model original = modelWith(ALICE);
        Model candidate = original.forkForCommand(TODAY);
        candidate.setPerson(ALICE, new PersonBuilder(ALICE).build());
        assertTrue(candidate.hasUnsavedChanges());

        candidate = original.forkForCommand(TODAY);
        candidate.addPerson(BENSON);
        assertTrue(candidate.hasUnsavedChanges());

        candidate = original.forkForCommand(TODAY);
        candidate.deletePerson(ALICE);
        assertTrue(candidate.hasUnsavedChanges());

        candidate = original.forkForCommand(TODAY);
        candidate.setAddressBook(original.getAddressBook());
        assertTrue(candidate.hasUnsavedChanges());

        candidate = modelWith().forkForCommand(TODAY);
        candidate.setAddressBook(new AddressBook());
        assertTrue(candidate.hasUnsavedChanges());
    }

    @Test
    public void hasUnsavedChanges_queriesAndPreferences_remainsClean() {
        Model candidate = modelWith(ALICE).forkForCommand(TODAY);
        candidate.showPendingFollowUps();
        candidate.updateFilteredPersonList(PREDICATE_SHOW_ALL_PERSONS);
        candidate.hasPerson(ALICE);
        candidate.getAddressBook();
        candidate.getFilteredPersonList();
        candidate.getToday();
        candidate.todayProperty();
        candidate.showingFollowUpsProperty();
        candidate.isDataLoadingBlocked();
        candidate.getUserPrefs();
        candidate.setGuiSettings(new GuiSettings(1, 2, 3, 4));
        assertFalse(candidate.hasUnsavedChanges());
    }

    @Test
    public void hasUnsavedChanges_rejectedMutations_remainsClean() {
        Model candidate = modelWith(ALICE, BENSON).forkForCommand(TODAY);
        Person missing = new PersonBuilder().withName("Missing").build();
        assertThrows(DuplicatePersonException.class, () -> candidate.addPerson(ALICE));
        assertFalse(candidate.hasUnsavedChanges());
        assertThrows(DuplicatePersonException.class, () -> candidate.setPerson(ALICE, BENSON));
        assertFalse(candidate.hasUnsavedChanges());
        assertThrows(PersonNotFoundException.class, () -> candidate.deletePerson(missing));
        assertFalse(candidate.hasUnsavedChanges());
        assertThrows(PersonNotFoundException.class, () -> candidate.setPerson(missing, ALICE));
        assertFalse(candidate.hasUnsavedChanges());
        assertThrows(NullPointerException.class, () -> candidate.setAddressBook(null));
        assertFalse(candidate.hasUnsavedChanges());
        assertEquals(List.of(ALICE, BENSON), candidate.getFilteredPersonList());
    }

    @Test
    public void commitFrom_changedCandidate_publishesOnceThroughStableObservableObjects() {
        Person late = withFollowUp(ALICE, 2);
        Person early = withFollowUp(BENSON, 1);
        Model live = modelWith(late, early);
        ObservableList<Person> displayed = live.getFilteredPersonList();
        ReadOnlyObjectProperty<LocalDate> date = live.todayProperty();
        ReadOnlyBooleanProperty pending = live.showingFollowUpsProperty();
        List<List<Person>> displayedChanges = new ArrayList<>();
        List<LocalDate> datesAtPublication = new ArrayList<>();
        List<Boolean> modesAtPublication = new ArrayList<>();
        displayed.addListener((ListChangeListener<Person>) change -> {
            displayedChanges.add(List.copyOf(change.getList()));
            datesAtPublication.add(date.get());
            modesAtPublication.add(pending.get());
        });
        Model candidate = live.forkForCommand(TODAY.plusDays(1));
        Person replacement = withFollowUp(late, 0);
        candidate.setPerson(late, replacement);
        candidate.showPendingFollowUps();
        candidate.setGuiSettings(new GuiSettings(1, 2, 3, 4));

        live.commitFrom(candidate);

        assertSame(displayed, live.getFilteredPersonList());
        assertSame(date, live.todayProperty());
        assertSame(pending, live.showingFollowUpsProperty());
        assertEquals(List.of(List.of(replacement, early)), displayedChanges);
        assertEquals(List.of(candidate.getToday()), datesAtPublication);
        assertEquals(List.of(candidate.showingFollowUpsProperty().get()), modesAtPublication);
        assertEquals(List.of(replacement, early), live.getAddressBook().getPersonList());
        assertEquals(TODAY.plusDays(1), date.get());
        assertTrue(pending.get());
        assertEquals(candidate.getGuiSettings(), live.getGuiSettings());
        candidate.deletePerson(early);
        candidate.setGuiSettings(new GuiSettings(5, 6, 7, 8));
        assertEquals(List.of(replacement, early), displayed);
        assertEquals(new GuiSettings(1, 2, 3, 4), live.getGuiSettings());
    }

    @Test
    public void commitFrom_readOnlyCandidate_publishesViewAndDateWithoutReplacingData() {
        Person late = withFollowUp(ALICE, 2);
        Person early = withFollowUp(BENSON, 1);
        Model live = modelWith(late, early);
        List<List<Person>> dataChanges = new ArrayList<>();
        live.getAddressBook().getPersonList().addListener((ListChangeListener<Person>) change ->
                dataChanges.add(List.copyOf(change.getList())));
        Model candidate = live.forkForCommand(TODAY.plusDays(1));
        candidate.showPendingFollowUps();
        assertFalse(candidate.hasUnsavedChanges());

        live.commitFrom(candidate);

        assertEquals(List.of(early, late), live.getFilteredPersonList());
        assertTrue(live.showingFollowUpsProperty().get());
        assertEquals(TODAY.plusDays(1), live.getToday());
        assertTrue(dataChanges.isEmpty());
        assertFalse(live.hasUnsavedChanges());

        Model searchCandidate = live.forkForCommand(TODAY.plusDays(2));
        searchCandidate.updateFilteredPersonList(person -> person.equals(late));
        live.commitFrom(searchCandidate);
        assertEquals(List.of(late), live.getFilteredPersonList());
        assertFalse(live.showingFollowUpsProperty().get());
        assertEquals(TODAY.plusDays(2), live.getToday());
        assertTrue(dataChanges.isEmpty());
    }

    @Test
    public void displayedList_modeAndDataChanges_retainsUnmodifiableObject() {
        Model model = modelWith(withFollowUp(ALICE, 1), BENSON);
        ObservableList<Person> displayed = model.getFilteredPersonList();
        model.showPendingFollowUps();
        assertSame(displayed, model.getFilteredPersonList());
        assertThrows(UnsupportedOperationException.class, () -> displayed.add(BENSON));
        model.setAddressBook(modelWith(ALICE).getAddressBook());
        assertSame(displayed, model.getFilteredPersonList());
        assertTrue(displayed.isEmpty());
        assertTrue(model.showingFollowUpsProperty().get());
        model.updateFilteredPersonList(PREDICATE_SHOW_ALL_PERSONS);
        assertSame(displayed, model.getFilteredPersonList());
        assertEquals(List.of(ALICE), displayed);
    }

    private static Person withFollowUp(Person person, int daysFromToday) {
        return new PersonBuilder(person)
                .withFollowUp(new FollowUp(TODAY.plusDays(daysFromToday), "Call client"))
                .build();
    }

    private static ModelManager modelWith(Person... persons) {
        AddressBook addressBook = new AddressBook();
        for (Person person : persons) {
            addressBook.addPerson(person);
        }
        return new ModelManager(addressBook, new UserPrefs(), TODAY, false);
    }

    @Test
    public void equals() {
        AddressBook addressBook = new AddressBookBuilder().withPerson(ALICE).withPerson(BENSON).build();
        AddressBook differentAddressBook = new AddressBook();
        UserPrefs userPrefs = new UserPrefs();

        // same values -> returns true
        modelManager = new ModelManager(addressBook, userPrefs);
        ModelManager modelManagerCopy = new ModelManager(addressBook, userPrefs);
        assertTrue(modelManager.equals(modelManagerCopy));

        // same object -> returns true
        assertTrue(modelManager.equals(modelManager));

        // null -> returns false
        assertFalse(modelManager.equals(null));

        // different types -> returns false
        assertFalse(modelManager.equals(5));

        // different addressBook -> returns false
        assertFalse(modelManager.equals(new ModelManager(differentAddressBook, userPrefs)));

        // different filteredList -> returns false
        String[] keywords = ALICE.getName().fullName.split("\\s+");
        modelManager.updateFilteredPersonList(new NameContainsKeywordsPredicate(List.of(keywords)));
        assertFalse(modelManager.equals(new ModelManager(addressBook, userPrefs)));

        // resets modelManager to initial state for upcoming tests
        modelManager.updateFilteredPersonList(PREDICATE_SHOW_ALL_PERSONS);

        // different userPrefs -> returns false
        UserPrefs differentUserPrefs = new UserPrefs();
        differentUserPrefs.setGuiSettings(new GuiSettings(1, 2, 3, 4));
        assertFalse(modelManager.equals(new ModelManager(addressBook, differentUserPrefs)));
    }
}
