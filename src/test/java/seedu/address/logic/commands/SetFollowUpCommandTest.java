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

/** Tests mutation commands against the real model with a fixed command date. */
public class SetFollowUpCommandTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 7);
    private static final Index FIRST = Index.fromOneBased(1);

    @Test
    public void execute_newFollowUp_recordsAndPreservesContact() throws Exception {
        Model model = modelWith(ALICE, BENSON);
        CommandResult result = new SetFollowUpCommand(FIRST, " 2026-10-07 ", " Call  Alice ").execute(model);

        assertEquals("Follow-up for Alice Pauline recorded for 2026-10-07.", result.getFeedbackToUser());
        Person updated = ALICE.withFollowUp(new FollowUp(TODAY, "Call  Alice"));
        assertEquals(List.of(updated, BENSON), model.getAddressBook().getPersonList());
        assertEquals(List.of(updated, BENSON), model.getFilteredPersonList());
        assertTrue(model.hasUnsavedChanges());
        assertFalse(result.isExit());
        assertFalse(result.isShowHelp());
    }

    @Test
    public void execute_existingFollowUp_replacesBothFieldsAndReordersPendingView() throws Exception {
        Person alice = ALICE.withFollowUp(new FollowUp(TODAY.plusDays(2), "Old action"));
        Person benson = BENSON.withFollowUp(new FollowUp(TODAY.plusDays(1), "Call Benson"));
        Model model = modelWith(alice, benson);
        model.showPendingFollowUps();

        CommandResult result = new SetFollowUpCommand(FIRST, "2026-10-10", "Renew policy").execute(model);

        Person updated = BENSON.withFollowUp(new FollowUp(TODAY.plusDays(3), "Renew policy"));
        assertEquals("Follow-up for Benson Meier updated to 2026-10-10.", result.getFeedbackToUser());
        assertEquals(List.of(alice, updated), model.getAddressBook().getPersonList());
        assertEquals(List.of(alice, updated), model.getFilteredPersonList());
        assertTrue(model.showingFollowUpsProperty().get());
        assertTrue(model.hasUnsavedChanges());
    }

    @Test
    public void execute_identicalReplacement_succeedsAndMarksDirty() throws Exception {
        Person alice = ALICE.withFollowUp(new FollowUp(TODAY, "Call Alice"));
        Model model = modelWith(alice);

        CommandResult result = new SetFollowUpCommand(FIRST, "2026-10-07", "Call Alice").execute(model);

        assertEquals("Follow-up for Alice Pauline updated to 2026-10-07.", result.getFeedbackToUser());
        assertEquals(List.of(alice), model.getFilteredPersonList());
        assertTrue(model.hasUnsavedChanges());
    }

    @Test
    public void execute_searchView_targetsDisplayedRowAndKeepsFilter() throws Exception {
        Model model = modelWith(ALICE, BENSON);
        model.updateFilteredPersonList(person -> person.equals(BENSON));

        new SetFollowUpCommand(FIRST, "2026-10-08", "Call Benson").execute(model);

        Person updated = BENSON.withFollowUp(new FollowUp(TODAY.plusDays(1), "Call Benson"));
        assertEquals(List.of(ALICE, updated), model.getAddressBook().getPersonList());
        // The exact-person predicate no longer matches after replacement; it must remain active.
        assertTrue(model.getFilteredPersonList().isEmpty());
        assertFalse(model.showingFollowUpsProperty().get());
    }

    @Test
    public void execute_invalidDisplayedIndex_precedesInvalidDateAndDescription() {
        Model model = modelWith(ALICE, BENSON);
        model.updateFilteredPersonList(person -> person.equals(BENSON));
        assertFailureUnchanged(new SetFollowUpCommand(Index.fromOneBased(2), "bad", ""), model,
                Messages.MESSAGE_INVALID_PERSON_DISPLAYED_INDEX);
        assertFailureUnchanged(new SetFollowUpCommand(Index.fromOneBased(Integer.MAX_VALUE), "bad", ""), model,
                Messages.MESSAGE_INVALID_PERSON_DISPLAYED_INDEX);
        assertFailureUnchanged(new SetFollowUpCommand(FIRST, "bad", ""), modelWith(),
                Messages.MESSAGE_INVALID_PERSON_DISPLAYED_INDEX);
    }

    @Test
    public void execute_invalidDate_precedesInvalidDescription() {
        for (String date : List.of("2026-02-30", "2026-2-07", "0000-01-01", "", "2026-10-07\t")) {
            assertFailureUnchanged(new SetFollowUpCommand(FIRST, date, ""), modelWith(ALICE),
                    FollowUp.MESSAGE_INVALID_DATE);
        }
    }

    @Test
    public void execute_pastDate_precedesInvalidDescriptionAndPreservesPendingView() {
        Person alice = ALICE.withFollowUp(new FollowUp(TODAY.minusDays(2), "Overdue"));
        Model model = modelWith(alice);
        model.showPendingFollowUps();
        assertFailureUnchanged(new SetFollowUpCommand(FIRST, "2026-10-06", ""), model,
                "Follow-up date cannot be in the past.");
    }

    @Test
    public void execute_invalidDescription_preservesExistingAction() {
        Person alice = ALICE.withFollowUp(new FollowUp(TODAY, "Keep this"));
        for (String description : List.of("", "   ", "x".repeat(201), "Call\nAlice", "Café")) {
            assertFailureUnchanged(new SetFollowUpCommand(FIRST, "2026-10-08", description), modelWith(alice),
                    FollowUp.MESSAGE_INVALID_DESCRIPTION);
        }
    }

    @Test
    public void execute_boundaryDescriptionLengths_acceptsAfterTrimming() throws Exception {
        for (String description : List.of("x", "x".repeat(200))) {
            Model model = modelWith(ALICE);
            new SetFollowUpCommand(FIRST, "9999-12-31", " " + description + " ").execute(model);
            FollowUp action = model.getFilteredPersonList().get(0).getFollowUp().orElseThrow();
            assertEquals(description, action.getDescription());
            assertEquals(LocalDate.of(9999, 12, 31), action.getDueDate());
        }
    }

    @Test
    public void execute_candidate_usesSampledDateAndKeepsLiveStateUntilCommit() throws Exception {
        Model live = modelWith(ALICE);
        Model candidate = live.forkForCommand(TODAY.plusDays(1));
        assertFailureUnchanged(new SetFollowUpCommand(FIRST, "2026-10-07", "Call"), candidate,
                "Follow-up date cannot be in the past.");

        new SetFollowUpCommand(FIRST, "2026-10-08", "Call").execute(candidate);

        assertEquals(List.of(ALICE), live.getFilteredPersonList());
        assertEquals(TODAY, live.getToday());
        assertFalse(live.hasUnsavedChanges());
        assertTrue(candidate.hasUnsavedChanges());
        live.commitFrom(candidate);
        assertEquals(List.of(ALICE.withFollowUp(new FollowUp(TODAY.plusDays(1), "Call"))),
                live.getFilteredPersonList());
    }

    @Test
    public void constructor_nullArguments_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new SetFollowUpCommand(null, "2026-10-07", "Call"));
        assertThrows(NullPointerException.class, () -> new SetFollowUpCommand(FIRST, null, "Call"));
        assertThrows(NullPointerException.class, () -> new SetFollowUpCommand(FIRST, "2026-10-07", null));
        assertThrows(NullPointerException.class, () ->
                new SetFollowUpCommand(FIRST, "2026-10-07", "Call").execute(null));
    }

    @Test
    public void equals_comparesIndexAndRawValues() {
        SetFollowUpCommand command = new SetFollowUpCommand(FIRST, "2026-10-07", "Call");
        assertEquals(command, command);
        assertEquals(command, new SetFollowUpCommand(FIRST, "2026-10-07", "Call"));
        assertNotEquals(command, new SetFollowUpCommand(Index.fromOneBased(2), "2026-10-07", "Call"));
        assertNotEquals(command, new SetFollowUpCommand(FIRST, "2026-10-08", "Call"));
        assertNotEquals(command, new SetFollowUpCommand(FIRST, "2026-10-07", "Renew"));
        assertNotEquals(command, null);
        assertNotEquals(command, "followup");
    }

    @Test
    public void toStringMethod() {
        SetFollowUpCommand command = new SetFollowUpCommand(FIRST, "2026-10-07", "Call");
        assertEquals(SetFollowUpCommand.class.getCanonicalName()
                + "{targetIndex=" + FIRST + ", dateText=2026-10-07, descriptionText=Call}", command.toString());
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
        LocalDate today = model.getToday();
        boolean dirty = model.hasUnsavedChanges();
        CommandException error = assertThrows(CommandException.class, () -> command.execute(model));
        assertEquals(message, error.getMessage());
        assertEquals(before, model.getAddressBook().getPersonList());
        assertEquals(displayed, model.getFilteredPersonList());
        assertEquals(pending, model.showingFollowUpsProperty().get());
        assertEquals(today, model.getToday());
        assertEquals(dirty, model.hasUnsavedChanges());
    }
}
