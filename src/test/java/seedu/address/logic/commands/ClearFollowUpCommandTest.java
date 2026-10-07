package seedu.address.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.TypicalPersons.ALICE;
import static seedu.address.testutil.TypicalPersons.BENSON;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.address.commons.core.index.Index;
import seedu.address.logic.Messages;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.AddressBook;
import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.UserPrefs;
import seedu.address.model.person.FollowUp;
import seedu.address.model.person.Person;

/** Tests clearing an action without deleting the client or resetting the view. */
public class ClearFollowUpCommandTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 7);
    private static final Index FIRST = Index.fromOneBased(1);
    private static final Person ALICE_PENDING = ALICE.withFollowUp(new FollowUp(TODAY, "Call Alice"));
    private static final Person BENSON_OVERDUE = BENSON.withFollowUp(new FollowUp(TODAY.minusDays(1), "Call Benson"));

    @Test
    public void execute_existingFollowUp_clearsOnlyAction() throws Exception {
        Model model = modelWith(ALICE_PENDING, BENSON);

        CommandResult result = new ClearFollowUpCommand(FIRST).execute(model);

        assertEquals("Follow-up for Alice Pauline cleared.", result.getFeedbackToUser());
        assertEquals(List.of(ALICE, BENSON), model.getAddressBook().getPersonList());
        assertEquals(List.of(ALICE, BENSON), model.getFilteredPersonList());
        assertTrue(model.hasUnsavedChanges());
        assertFalse(result.isExit());
        assertFalse(result.isShowHelp());
    }

    @Test
    public void execute_pendingView_clearsDisplayedOverdueClientAndRemovesRow() throws Exception {
        Model model = modelWith(ALICE_PENDING, BENSON_OVERDUE);
        model.showPendingFollowUps();

        CommandResult result = new ClearFollowUpCommand(FIRST).execute(model);

        assertEquals("Follow-up for Benson Meier cleared.", result.getFeedbackToUser());
        assertEquals(List.of(ALICE_PENDING, BENSON), model.getAddressBook().getPersonList());
        assertEquals(List.of(ALICE_PENDING), model.getFilteredPersonList());
        assertTrue(model.showingFollowUpsProperty().get());
        new ClearFollowUpCommand(FIRST).execute(model);
        assertTrue(model.getFilteredPersonList().isEmpty());
        assertEquals(List.of(ALICE, BENSON), model.getAddressBook().getPersonList());
        assertTrue(model.showingFollowUpsProperty().get());
    }

    @Test
    public void execute_searchView_preservesFilterAndOtherClients() throws Exception {
        Model model = modelWith(ALICE_PENDING, BENSON_OVERDUE);
        model.updateFilteredPersonList(person -> person.getName().equals(BENSON.getName()));

        new ClearFollowUpCommand(FIRST).execute(model);

        assertEquals(List.of(BENSON), model.getFilteredPersonList());
        assertEquals(List.of(ALICE_PENDING, BENSON), model.getAddressBook().getPersonList());
        assertFalse(model.showingFollowUpsProperty().get());
    }

    @Test
    public void execute_noFollowUp_failsWithoutMutation() {
        Model model = modelWith(ALICE, BENSON_OVERDUE);
        model.updateFilteredPersonList(person -> person.getName().equals(ALICE.getName()));
        assertFailureUnchanged(new ClearFollowUpCommand(FIRST), model, "This client has no pending follow-up.");
    }

    @Test
    public void execute_invalidDisplayedIndex_failsBeforePresenceCheck() {
        Model model = modelWith(ALICE_PENDING, BENSON);
        model.showPendingFollowUps();
        assertFailureUnchanged(new ClearFollowUpCommand(Index.fromOneBased(2)), model,
                Messages.MESSAGE_INVALID_PERSON_DISPLAYED_INDEX);
        assertFailureUnchanged(new ClearFollowUpCommand(Index.fromOneBased(Integer.MAX_VALUE)), model,
                Messages.MESSAGE_INVALID_PERSON_DISPLAYED_INDEX);
        assertFailureUnchanged(new ClearFollowUpCommand(FIRST), modelWith(),
                Messages.MESSAGE_INVALID_PERSON_DISPLAYED_INDEX);
    }

    @Test
    public void execute_candidate_keepsLiveActionUntilCommit() throws Exception {
        Model live = modelWith(ALICE_PENDING);
        live.showPendingFollowUps();
        Model candidate = live.forkForCommand(TODAY.plusDays(1));

        new ClearFollowUpCommand(FIRST).execute(candidate);

        assertEquals(List.of(ALICE_PENDING), live.getFilteredPersonList());
        assertEquals(TODAY, live.getToday());
        assertFalse(live.hasUnsavedChanges());
        assertTrue(candidate.getFilteredPersonList().isEmpty());
        assertTrue(candidate.hasUnsavedChanges());
        live.commitFrom(candidate);
        assertEquals(List.of(ALICE), live.getAddressBook().getPersonList());
        assertTrue(live.getFilteredPersonList().isEmpty());
    }

    @Test
    public void constructor_nullIndex_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new ClearFollowUpCommand(null));
        assertThrows(NullPointerException.class, () -> new ClearFollowUpCommand(FIRST).execute(null));
    }

    @Test
    public void equals_comparesIndex() {
        ClearFollowUpCommand command = new ClearFollowUpCommand(FIRST);
        assertEquals(command, command);
        assertEquals(command, new ClearFollowUpCommand(FIRST));
        assertNotEquals(command, new ClearFollowUpCommand(Index.fromOneBased(2)));
        assertNotEquals(command, null);
        assertNotEquals(command, "clear");
    }

    @Test
    public void toStringMethod() {
        assertEquals(ClearFollowUpCommand.class.getCanonicalName() + "{targetIndex=" + FIRST + "}",
                new ClearFollowUpCommand(FIRST).toString());
    }

    private static Model modelWith(Person... persons) {
        AddressBook data = new AddressBook();
        for (Person person : persons) {
            data.addPerson(person);
        }
        return new ModelManager(data, new UserPrefs(), TODAY, false);
    }

    private static void assertFailureUnchanged(Command command, Model model, String message) {
        List<Person> before = List.copyOf(model.getAddressBook().getPersonList());
        List<Person> displayed = List.copyOf(model.getFilteredPersonList());
        boolean pending = model.showingFollowUpsProperty().get();
        boolean dirty = model.hasUnsavedChanges();
        LocalDate today = model.getToday();
        CommandException error = assertThrows(CommandException.class, () -> command.execute(model));
        assertEquals(message, error.getMessage());
        assertEquals(before, model.getAddressBook().getPersonList());
        assertEquals(displayed, model.getFilteredPersonList());
        assertEquals(pending, model.showingFollowUpsProperty().get());
        assertEquals(today, model.getToday());
        assertEquals(dirty, model.hasUnsavedChanges());
    }
}
