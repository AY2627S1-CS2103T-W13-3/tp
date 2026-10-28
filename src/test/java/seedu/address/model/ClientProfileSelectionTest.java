package seedu.address.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.collections.ObservableList;
import seedu.address.logic.commands.AddCommand;
import seedu.address.logic.commands.FindCommand;
import seedu.address.logic.commands.ListCommand;
import seedu.address.model.person.ClientContainsKeywordsPredicate;
import seedu.address.model.person.FollowUp;
import seedu.address.model.person.Person;
import seedu.address.model.person.exceptions.DuplicatePersonException;
import seedu.address.model.person.exceptions.PersonNotFoundException;
import seedu.address.testutil.PersonBuilder;

/** Regression evidence for session selection, independently of the future view command and UI. */
public class ClientProfileSelectionTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 12);
    private static final Person RACHEL = new PersonBuilder().withName("Rachel Lim")
            .withPhone("81111111").withEmail("rachel@example.com")
            .withFollowUp(new FollowUp(TODAY.plusDays(2), "Call Rachel")).build();
    private static final Person AMANDA = new PersonBuilder().withName("Amanda Tan")
            .withPhone("82222222").withEmail("amanda@example.com")
            .withFollowUp(new FollowUp(TODAY.plusDays(1), "Call Amanda")).build();
    private static final Person OTHER_AMANDA = new PersonBuilder().withName("Amanda Tan")
            .withPhone("83333333").withEmail("other.amanda@example.com").build();
    private static final Person JOHN = new PersonBuilder().withName("John Lee")
            .withPhone("84444444").withEmail("john@example.com").build();

    @Test
    public void selectPerson_canonicalVisibleRecord_changesOnlySelection() {
        Model model = createModel(RACHEL, AMANDA);
        assertNull(model.selectedPersonProperty().get());

        model.selectPerson(AMANDA);

        assertSame(AMANDA, model.selectedPersonProperty().get());
        assertEquals(List.of(RACHEL, AMANDA), model.getFilteredPersonList());
        assertEquals(List.of(RACHEL, AMANDA), model.getAddressBook().getPersonList());
        assertEquals(TODAY, model.getToday());
        assertFalse(model.hasUnsavedChanges());
    }

    @Test
    public void selectPerson_nullNoncanonicalOrHidden_rejectsWithoutChangingState() {
        Model model = createModel(RACHEL, AMANDA);
        model.selectPerson(AMANDA);
        model.updateFilteredPersonList(person -> person == AMANDA);

        assertThrows(NullPointerException.class, () -> model.selectPerson(null));
        assertThrows(IllegalArgumentException.class, () -> model.selectPerson(new PersonBuilder(AMANDA).build()));
        assertThrows(IllegalArgumentException.class, () -> model.selectPerson(RACHEL));
        assertSame(AMANDA, model.selectedPersonProperty().get());
        assertEquals(List.of(AMANDA), model.getFilteredPersonList());
        assertFalse(model.hasUnsavedChanges());
    }

    @Test
    public void setPerson_selectedPendingRecord_reordersAndTracksReplacement() {
        Model model = createModel(RACHEL, AMANDA, OTHER_AMANDA);
        model.selectPerson(AMANDA);
        model.showPendingFollowUps();
        assertEquals(List.of(AMANDA, RACHEL), model.getFilteredPersonList());

        Person replacement = AMANDA.withFollowUp(new FollowUp(TODAY.plusDays(3), "Send revised quotation"));
        model.setPerson(AMANDA, replacement);

        assertEquals(List.of(RACHEL, replacement), model.getFilteredPersonList());
        assertSame(replacement, model.selectedPersonProperty().get());
        Person changedRachel = RACHEL.withFollowUp(new FollowUp(TODAY.plusDays(4), "Send Rachel a reminder"));
        model.setPerson(RACHEL, changedRachel);
        assertEquals(List.of(replacement, changedRachel), model.getFilteredPersonList());
        assertSame(replacement, model.selectedPersonProperty().get());
    }

    @Test
    public void clearFollowUp_fullAndSearchModes_keepsUpdatedProfile() throws Exception {
        for (boolean isSearchMode : List.of(false, true)) {
            Model model = createModel(RACHEL, AMANDA);
            if (isSearchMode) {
                new FindCommand(new ClientContainsKeywordsPredicate(List.of("Amanda"))).execute(model);
            }
            model.selectPerson(AMANDA);
            Person cleared = new PersonBuilder(AMANDA).withoutFollowUp().build();

            model.setPerson(AMANDA, cleared);

            assertSame(cleared, model.selectedPersonProperty().get());
            assertTrue(model.selectedPersonProperty().get().getFollowUp().isEmpty());
            assertTrue(model.getFilteredPersonList().contains(cleared));
        }
    }

    @Test
    public void clearFollowUp_pendingMode_clearsProfileWithoutReopeningOnList() throws Exception {
        Model model = createModel(RACHEL, AMANDA);
        model.showPendingFollowUps();
        model.selectPerson(AMANDA);
        Person cleared = new PersonBuilder(AMANDA).withoutFollowUp().build();

        model.setPerson(AMANDA, cleared);

        assertNull(model.selectedPersonProperty().get());
        assertEquals(List.of(RACHEL), model.getFilteredPersonList());
        new ListCommand().execute(model);
        assertEquals(List.of(RACHEL, cleared), model.getFilteredPersonList());
        assertNull(model.selectedPersonProperty().get());
    }

    @Test
    public void findAndFollowUps_excludedSelection_clearsAndNeverAutomaticallyReopens() throws Exception {
        Model model = createModel(RACHEL, AMANDA, OTHER_AMANDA);
        model.selectPerson(AMANDA);
        new FindCommand(new ClientContainsKeywordsPredicate(List.of("Amanda"))).execute(model);
        assertSame(AMANDA, model.selectedPersonProperty().get());
        new FindCommand(new ClientContainsKeywordsPredicate(List.of("Rachel"))).execute(model);
        assertNull(model.selectedPersonProperty().get());
        new ListCommand().execute(model);
        assertNull(model.selectedPersonProperty().get());

        model.selectPerson(OTHER_AMANDA);
        model.showPendingFollowUps();
        assertNull(model.selectedPersonProperty().get());
        new ListCommand().execute(model);
        assertNull(model.selectedPersonProperty().get());
    }

    @Test
    public void deletePerson_sameNameDifferentContacts_neverTransfersSelection() {
        Model model = createModel(RACHEL, AMANDA, OTHER_AMANDA);
        model.selectPerson(OTHER_AMANDA);

        model.deletePerson(AMANDA);

        assertSame(OTHER_AMANDA, model.selectedPersonProperty().get());
        assertEquals(List.of(RACHEL, OTHER_AMANDA), model.getFilteredPersonList());
        model.deletePerson(OTHER_AMANDA);
        assertNull(model.selectedPersonProperty().get());
        assertEquals(List.of(RACHEL), model.getFilteredPersonList());
    }

    @Test
    public void addAndList_keepExistingSelectionButNeverSelectNewClient() throws Exception {
        Model model = createModel(RACHEL, AMANDA);
        model.showPendingFollowUps();
        model.selectPerson(AMANDA);

        new AddCommand(JOHN).execute(model);
        assertSame(AMANDA, model.selectedPersonProperty().get());
        assertEquals(List.of(RACHEL, AMANDA, JOHN), model.getFilteredPersonList());
        assertFalse(model.showingFollowUpsProperty().get());

        new ListCommand().execute(model);

        assertSame(AMANDA, model.selectedPersonProperty().get());
        assertEquals(List.of(RACHEL, AMANDA, JOHN), model.getFilteredPersonList());
        assertFalse(model.showingFollowUpsProperty().get());
        Model unselected = createModel(RACHEL);
        new AddCommand(JOHN).execute(unselected);
        assertNull(unselected.selectedPersonProperty().get());
    }

    @Test
    public void forkForCommand_selectionAndMutation_neverPublishUntilCommit() {
        Model live = createModel(RACHEL, AMANDA);
        live.showPendingFollowUps();
        live.selectPerson(AMANDA);
        Model candidate = live.forkForCommand(TODAY.plusDays(1));
        assertSame(AMANDA, candidate.selectedPersonProperty().get());
        assertNotSame(live.selectedPersonProperty(), candidate.selectedPersonProperty());
        assertFalse(candidate.hasUnsavedChanges());

        candidate.selectPerson(RACHEL);
        candidate.deletePerson(AMANDA);
        candidate.updateFilteredPersonList(Model.PREDICATE_SHOW_ALL_PERSONS);

        assertSame(AMANDA, live.selectedPersonProperty().get());
        assertEquals(List.of(AMANDA, RACHEL), live.getFilteredPersonList());
        assertEquals(List.of(RACHEL, AMANDA), live.getAddressBook().getPersonList());
        assertTrue(live.showingFollowUpsProperty().get());
        assertEquals(TODAY, live.getToday());
        assertFalse(live.hasUnsavedChanges());
    }

    @Test
    public void commitFrom_cleanSelection_publishesViewAndRetainsObservableObjects() {
        Model live = createModel(RACHEL, AMANDA);
        ReadOnlyObjectProperty<Person> selection = live.selectedPersonProperty();
        ObservableList<Person> displayed = live.getFilteredPersonList();
        var date = live.todayProperty();
        var mode = live.showingFollowUpsProperty();
        Model candidate = live.forkForCommand(TODAY.plusDays(1));
        candidate.showPendingFollowUps();
        candidate.selectPerson(AMANDA);
        assertFalse(candidate.hasUnsavedChanges());

        live.commitFrom(candidate);

        assertSame(selection, live.selectedPersonProperty());
        assertSame(displayed, live.getFilteredPersonList());
        assertSame(date, live.todayProperty());
        assertSame(mode, live.showingFollowUpsProperty());
        assertSame(AMANDA, selection.get());
        assertEquals(List.of(AMANDA, RACHEL), displayed);
        assertEquals(TODAY.plusDays(1), date.get());
        assertTrue(mode.get());
        assertFalse(live.hasUnsavedChanges());
    }

    @Test
    public void commitFrom_equalValueReplacement_rebindsToActualLiveRecord() {
        Model live = createModel(RACHEL, AMANDA, OTHER_AMANDA);
        live.showPendingFollowUps();
        live.selectPerson(AMANDA);
        Model candidate = live.forkForCommand(TODAY);
        Person identicalReplacement = new PersonBuilder(AMANDA).build();
        candidate.setPerson(AMANDA, identicalReplacement);
        assertSame(identicalReplacement, candidate.selectedPersonProperty().get());
        assertNotSame(AMANDA, identicalReplacement);
        assertEquals(live.getAddressBook(), candidate.getAddressBook());

        live.commitFrom(candidate);

        assertSame(AMANDA, live.getAddressBook().getPersonList().get(1));
        assertSame(live.getAddressBook().getPersonList().get(1), live.selectedPersonProperty().get());
        assertSame(live.getFilteredPersonList().get(0), live.selectedPersonProperty().get());
        assertFalse(live.hasUnsavedChanges());
    }

    @Test
    public void commitFrom_changedReplacementAndDeletion_publishesCanonicalSelectionThenClears() {
        Model live = createModel(RACHEL, AMANDA);
        live.selectPerson(AMANDA);
        Model candidate = live.forkForCommand(TODAY);
        Person replacement = AMANDA.withFollowUp(new FollowUp(TODAY.plusDays(3), "Send quotation"));
        candidate.setPerson(AMANDA, replacement);

        live.commitFrom(candidate);

        assertSame(replacement, live.selectedPersonProperty().get());
        assertSame(live.getFilteredPersonList().get(1), live.selectedPersonProperty().get());
        Model deleted = live.forkForCommand(TODAY);
        deleted.deletePerson(replacement);
        live.commitFrom(deleted);
        assertNull(live.selectedPersonProperty().get());
    }

    @Test
    public void setPerson_invalidReplacement_retainsCanonicalSelectionAndCleanState() {
        Model model = createModel(AMANDA, OTHER_AMANDA);
        model.selectPerson(AMANDA);

        assertThrows(DuplicatePersonException.class, () -> model.setPerson(AMANDA, OTHER_AMANDA));
        assertThrows(PersonNotFoundException.class, () -> model.setPerson(JOHN, RACHEL));
        assertThrows(NullPointerException.class, () -> model.setPerson(AMANDA, null));

        assertSame(AMANDA, model.selectedPersonProperty().get());
        assertEquals(List.of(AMANDA, OTHER_AMANDA), model.getFilteredPersonList());
        assertFalse(model.hasUnsavedChanges());
    }

    @Test
    public void setPerson_equalButDistinctTarget_transfersCanonicalSelection() {
        Model model = createModel(AMANDA, OTHER_AMANDA);
        model.selectPerson(AMANDA);
        Person equalTarget = new PersonBuilder(AMANDA).build();
        Person replacement = AMANDA.withFollowUp(new FollowUp(TODAY.plusDays(3), "Send quotation"));
        assertNotSame(AMANDA, equalTarget);

        model.setPerson(equalTarget, replacement);

        assertSame(replacement, model.selectedPersonProperty().get());
        assertSame(model.getFilteredPersonList().get(0), model.selectedPersonProperty().get());
        assertSame(OTHER_AMANDA, model.getFilteredPersonList().get(1));
    }

    @Test
    public void setAddressBook_removedOrRecreatedSelection_doesNotRediscoverByValueOrName() {
        Model model = createModel(AMANDA, OTHER_AMANDA);
        model.selectPerson(AMANDA);
        AddressBook recreated = new AddressBook();
        Person equalRecord = new PersonBuilder(AMANDA).build();
        recreated.addPerson(equalRecord);
        recreated.addPerson(OTHER_AMANDA);

        model.setAddressBook(recreated);

        assertNull(model.selectedPersonProperty().get());
        assertSame(equalRecord, model.getFilteredPersonList().get(0));
        model.selectPerson(equalRecord);
        AddressBook withoutSelected = new AddressBook();
        withoutSelected.addPerson(OTHER_AMANDA);
        model.setAddressBook(withoutSelected);
        assertNull(model.selectedPersonProperty().get());
        assertEquals(List.of(OTHER_AMANDA), model.getFilteredPersonList());
    }

    @Test
    public void selectedPersonProperty_listener_observesCanonicalDisplayedReplacementAndClear() {
        Model model = createModel(RACHEL, AMANDA);
        ReadOnlyObjectProperty<Person> selection = model.selectedPersonProperty();
        List<Person> observedSelections = new ArrayList<>();
        selection.addListener((observable, previous, current) -> {
            assertSame(selection, observable);
            if (current != null) {
                assertTrue(model.getFilteredPersonList().stream().anyMatch(person -> person == current));
            }
            observedSelections.add(current);
        });
        model.selectPerson(AMANDA);
        Model candidate = model.forkForCommand(TODAY);
        Person replacement = AMANDA.withFollowUp(new FollowUp(TODAY.plusDays(3), "Send quotation"));
        candidate.setPerson(AMANDA, replacement);
        assertEquals(List.of(AMANDA), observedSelections);

        model.commitFrom(candidate);
        model.deletePerson(replacement);

        assertSame(selection, model.selectedPersonProperty());
        assertEquals(3, observedSelections.size());
        assertSame(AMANDA, observedSelections.get(0));
        assertSame(replacement, observedSelections.get(1));
        assertNull(observedSelections.get(2));
    }

    @Test
    public void constructor_loadedEmptyOrBlockedModel_startsWithoutSelection() {
        Model previousSession = createModel(RACHEL, AMANDA);
        previousSession.selectPerson(AMANDA);
        Model loaded = new ModelManager(previousSession.getAddressBook(), new UserPrefs(), TODAY, false);
        Model empty = createModel();
        Model blocked = new ModelManager(new AddressBook(), new UserPrefs(), TODAY, true);

        assertNull(loaded.selectedPersonProperty().get());
        assertNull(empty.selectedPersonProperty().get());
        assertNull(blocked.selectedPersonProperty().get());
        assertEquals(List.of(RACHEL, AMANDA), loaded.getFilteredPersonList());
        assertFalse(loaded.showingFollowUpsProperty().get());
        assertTrue(blocked.isDataLoadingBlocked());
    }

    @Test
    public void equals_differentSelection_distinguishesViewState() {
        Model first = createModel(AMANDA, OTHER_AMANDA);
        Model second = createModel(AMANDA, OTHER_AMANDA);
        assertEquals(first, second);
        first.selectPerson(AMANDA);
        assertNotEquals(first, second);
        second.selectPerson(OTHER_AMANDA);
        assertNotEquals(first, second);
        second.selectPerson(AMANDA);
        assertEquals(first, second);
    }

    private static Model createModel(Person... persons) {
        AddressBook data = new AddressBook();
        for (Person person : persons) {
            data.addPerson(person);
        }
        return new ModelManager(data, new UserPrefs(), TODAY, false);
    }
}
