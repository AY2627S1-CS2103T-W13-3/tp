package seedu.address.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.logic.commands.FollowUpCommandTestUtil.assertFailureUnchanged;
import static seedu.address.testutil.TypicalPersons.ALICE;
import static seedu.address.testutil.TypicalPersons.BENSON;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.address.model.Model;
import seedu.address.model.person.ClientId;
import seedu.address.model.person.FollowUp;
import seedu.address.model.person.FollowUpStatus;
import seedu.address.model.person.Person;

/** Tests mutation commands against the real model with a fixed command date. */
public class SetFollowUpCommandTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 7);
    private static final ClientId FIRST = new ClientId(1);

    @Test
    public void execute_newFollowUp_recordsAndPreservesContact() throws Exception {
        Model model = modelWith(ALICE, BENSON);
        CommandResult result = new SetFollowUpCommand(FIRST, " 2026-10-07 ", " Call  Alice ").execute(model);

        assertEquals("Follow-up for client ID 1: Alice Pauline recorded for 2026-10-07.", result.getFeedbackToUser());
        Person updated = ALICE.withFollowUp(new FollowUp(TODAY, "Call  Alice"));
        assertEquals(List.of(updated, BENSON), model.getAddressBook().getPersonList());
        assertEquals(List.of(updated, BENSON), model.getFilteredPersonList());
        assertTrue(model.hasUnsavedChanges());
        assertFalse(result.isExit());
        assertFalse(result.isShowHelp());
    }

    @Test
    public void execute_newOverdueFollowUp_recordsAndPreservesContact() throws Exception {
        Model model = modelWith(ALICE, BENSON);

        CommandResult result = new SetFollowUpCommand(FIRST, "2026-10-06", "Carry-over call").execute(model);

        Person updated = ALICE.withFollowUp(new FollowUp(TODAY.minusDays(1), "Carry-over call"));
        assertEquals("Follow-up for client ID 1: Alice Pauline recorded for 2026-10-06.", result.getFeedbackToUser());
        assertEquals(List.of(updated, BENSON), model.getAddressBook().getPersonList());
        assertEquals(List.of(updated, BENSON), model.getFilteredPersonList());
        assertEquals(FollowUpStatus.OVERDUE, updated.getFollowUp().orElseThrow().getStatus(model.getToday()));
        assertTrue(model.hasUnsavedChanges());
    }

    @Test
    public void execute_existingFollowUp_replacesWithOverdueActionAndReordersPendingView() throws Exception {
        Person alice = ALICE.withFollowUp(new FollowUp(TODAY.plusDays(2), "Old action"));
        Person benson = BENSON.withFollowUp(new FollowUp(TODAY.plusDays(1), "Call Benson"));
        Model model = modelWith(alice, benson);
        model.showPendingFollowUps();

        CommandResult result = new SetFollowUpCommand(new ClientId(2), "2026-10-06", "Renew policy").execute(model);

        Person updated = BENSON.withFollowUp(new FollowUp(TODAY.minusDays(1), "Renew policy"));
        assertEquals("Follow-up for client ID 2: Benson Meier updated to 2026-10-06.", result.getFeedbackToUser());
        assertEquals(List.of(alice, updated), model.getAddressBook().getPersonList());
        assertEquals(List.of(updated, alice), model.getFilteredPersonList());
        assertEquals(FollowUpStatus.OVERDUE, updated.getFollowUp().orElseThrow().getStatus(model.getToday()));
        assertTrue(model.showingFollowUpsProperty().get());
        assertTrue(model.hasUnsavedChanges());
    }

    @Test
    public void execute_identicalReplacement_succeedsAndMarksDirty() throws Exception {
        Person alice = ALICE.withFollowUp(new FollowUp(TODAY, "Call Alice"));
        Model model = modelWith(alice);

        CommandResult result = new SetFollowUpCommand(FIRST, "2026-10-07", "Call Alice").execute(model);

        assertEquals("Follow-up for client ID 1: Alice Pauline updated to 2026-10-07.", result.getFeedbackToUser());
        assertEquals(List.of(alice), model.getFilteredPersonList());
        assertTrue(model.hasUnsavedChanges());
    }

    @Test
    public void execute_searchView_targetsClientIdAndKeepsFilter() throws Exception {
        Model model = modelWith(ALICE, BENSON);
        model.updateFilteredPersonList(person -> person.equals(BENSON));

        new SetFollowUpCommand(new ClientId(2), "2026-10-08", "Call Benson").execute(model);

        Person updated = BENSON.withFollowUp(new FollowUp(TODAY.plusDays(1), "Call Benson"));
        assertEquals(List.of(ALICE, updated), model.getAddressBook().getPersonList());
        // The exact-person predicate no longer matches after replacement; it must remain active.
        assertTrue(model.getFilteredPersonList().isEmpty());
        assertFalse(model.showingFollowUpsProperty().get());
    }

    @Test
    public void execute_unknownClientId_precedesInvalidDateAndDescription() {
        Model model = modelWith(ALICE, BENSON);
        model.updateFilteredPersonList(person -> person.equals(BENSON));
        assertFailureUnchanged(new SetFollowUpCommand(new ClientId(3), "bad", ""), model,
                "No client with ID 3.");
        assertFailureUnchanged(new SetFollowUpCommand(new ClientId(Integer.MAX_VALUE), "bad", ""), model,
                "No client with ID 2147483647.");
        assertFailureUnchanged(new SetFollowUpCommand(FIRST, "bad", ""), modelWith(),
                "No client with ID 1.");
    }

    @Test
    public void execute_invalidDate_precedesInvalidDescription() {
        for (String date : List.of("2026-02-30", "2026-02-29", "1900-02-29", "2026-2-07",
                "0000-01-01", "10000-01-01", "", "2026-10-07\t")) {
            assertFailureUnchanged(new SetFollowUpCommand(FIRST, date, ""), modelWith(ALICE),
                    FollowUp.MESSAGE_INVALID_DATE);
        }
    }

    @Test
    public void execute_pastDateWithInvalidDescription_reportsDescriptionAndPreservesPendingView() {
        Person alice = ALICE.withFollowUp(new FollowUp(TODAY.minusDays(2), "Overdue"));
        Model model = modelWith(alice);
        model.showPendingFollowUps();
        assertFailureUnchanged(new SetFollowUpCommand(FIRST, "2026-10-06", ""), model,
                FollowUp.MESSAGE_INVALID_DESCRIPTION);
    }

    @Test
    public void execute_earliestDateAndPastLeapDay_recordsOverdueAction() throws Exception {
        for (String date : List.of("0001-01-01", "2024-02-29")) {
            Model model = modelWith(ALICE);

            new SetFollowUpCommand(FIRST, date, "Carry-over action").execute(model);

            FollowUp action = model.getFilteredPersonList().get(0).getFollowUp().orElseThrow();
            assertEquals(LocalDate.parse(date), action.getDueDate());
            assertEquals(FollowUpStatus.OVERDUE, action.getStatus(model.getToday()));
        }
    }

    @Test
    public void execute_invalidDescription_preservesExistingAction() {
        Person alice = ALICE.withFollowUp(new FollowUp(TODAY, "Keep this"));
        for (String description : List.of("", "   ", "x".repeat(201), "Call\nAlice", "\ud800", "\u200b")) {
            assertFailureUnchanged(new SetFollowUpCommand(FIRST, "2026-10-08", description), modelWith(alice),
                    FollowUp.MESSAGE_INVALID_DESCRIPTION);
        }
    }

    @Test
    public void execute_boundaryDescriptionLengths_acceptsAfterTrimming() throws Exception {
        for (String description : List.of("x", "x".repeat(200), "续保电话", "Café", "😀".repeat(200))) {
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

        new SetFollowUpCommand(FIRST, "2026-10-07", "Call").execute(candidate);

        Person updated = ALICE.withFollowUp(new FollowUp(TODAY, "Call"));
        assertEquals(List.of(ALICE), live.getFilteredPersonList());
        assertEquals(TODAY, live.getToday());
        assertFalse(live.hasUnsavedChanges());
        assertEquals(TODAY.plusDays(1), candidate.getToday());
        assertEquals(FollowUpStatus.OVERDUE, updated.getFollowUp().orElseThrow().getStatus(candidate.getToday()));
        assertTrue(candidate.hasUnsavedChanges());
        live.commitFrom(candidate);
        assertEquals(List.of(updated), live.getFilteredPersonList());
        assertEquals(TODAY.plusDays(1), live.getToday());
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
    public void equals_comparesClientIdAndRawValues() {
        SetFollowUpCommand command = new SetFollowUpCommand(FIRST, "2026-10-07", "Call");
        assertEquals(command, command);
        assertEquals(command, new SetFollowUpCommand(FIRST, "2026-10-07", "Call"));
        assertNotEquals(command, new SetFollowUpCommand(new ClientId(2), "2026-10-07", "Call"));
        assertNotEquals(command, new SetFollowUpCommand(FIRST, "2026-10-08", "Call"));
        assertNotEquals(command, new SetFollowUpCommand(FIRST, "2026-10-07", "Renew"));
        assertNotEquals(command, null);
        assertNotEquals(command, "followup");
    }

    @Test
    public void toStringMethod() {
        SetFollowUpCommand command = new SetFollowUpCommand(FIRST, "2026-10-07", "Call");
        assertEquals(SetFollowUpCommand.class.getCanonicalName()
                + "{targetId=" + FIRST + ", dateText=2026-10-07, descriptionText=Call}", command.toString());
    }

    private static Model modelWith(Person... persons) {
        return FollowUpCommandTestUtil.createModel(TODAY, persons);
    }
}
