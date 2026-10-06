package seedu.address.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.Assert.assertThrows;
import static seedu.address.testutil.TypicalPersons.ALICE;
import static seedu.address.testutil.TypicalPersons.BENSON;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.address.commons.core.index.Index;
import seedu.address.logic.commands.AddCommand;
import seedu.address.logic.commands.DeleteCommand;
import seedu.address.logic.commands.FindCommand;
import seedu.address.logic.commands.ListCommand;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.person.FollowUp;
import seedu.address.model.person.NameContainsKeywordsPredicate;
import seedu.address.model.person.Person;
import seedu.address.testutil.AddressBookBuilder;

/**
 * Checks the model foundation with existing commands, before follow-up command routing is added.
 */
public class FollowUpListIntegrationTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 4);
    private static final Person LATE = ALICE.withFollowUp(new FollowUp(TODAY.plusDays(2), "Call Alice"));
    private static final Person EARLY = BENSON.withFollowUp(new FollowUp(TODAY, "Call Benson"));

    @Test
    public void delete_pendingCandidate_targetsDisplayedRowAndPublishesOnlyOnCommit() throws Exception {
        Model live = createModel();
        live.showPendingFollowUps();
        Model candidate = live.forkForCommand(TODAY.plusDays(1));

        new DeleteCommand(Index.fromOneBased(1)).execute(candidate);

        assertEquals(List.of(LATE), candidate.getFilteredPersonList());
        assertTrue(candidate.hasUnsavedChanges());
        assertEquals(List.of(EARLY, LATE), live.getFilteredPersonList());
        assertEquals(TODAY, live.getToday());
        live.commitFrom(candidate);
        assertEquals(List.of(LATE), live.getFilteredPersonList());
        assertEquals(List.of(LATE), live.getAddressBook().getPersonList());
        assertTrue(live.showingFollowUpsProperty().get());
    }

    @Test
    public void existingCommands_switchViewsAndKeepFailedCandidateIsolated() throws Exception {
        Model live = createModel();
        live.showPendingFollowUps();
        Model failed = live.forkForCommand(TODAY.plusDays(1));
        assertThrows(CommandException.class, () -> new DeleteCommand(Index.fromOneBased(3)).execute(failed));
        assertFalse(failed.hasUnsavedChanges());
        assertEquals(List.of(EARLY, LATE), live.getFilteredPersonList());
        assertEquals(TODAY, live.getToday());

        Model candidate = live.forkForCommand(TODAY.plusDays(1));
        new ListCommand().execute(candidate);
        assertEquals(List.of(LATE, EARLY), candidate.getFilteredPersonList());
        assertFalse(candidate.showingFollowUpsProperty().get());
        assertFalse(candidate.hasUnsavedChanges());
        candidate.showPendingFollowUps();
        new FindCommand(new NameContainsKeywordsPredicate(List.of("Alice"))).execute(candidate);
        assertEquals(List.of(LATE), candidate.getFilteredPersonList());
        assertFalse(candidate.showingFollowUpsProperty().get());
        assertFalse(candidate.hasUnsavedChanges());

        candidate.showPendingFollowUps();
        new DeleteCommand(Index.fromOneBased(1)).execute(candidate);
        new AddCommand(EARLY).execute(candidate);
        assertEquals(List.of(LATE, EARLY), candidate.getFilteredPersonList());
        assertFalse(candidate.showingFollowUpsProperty().get());
        assertTrue(candidate.hasUnsavedChanges());
    }

    private static Model createModel() {
        AddressBook data = new AddressBookBuilder().withPerson(LATE).withPerson(EARLY).build();
        return new ModelManager(data, new UserPrefs(), TODAY, false);
    }
}
