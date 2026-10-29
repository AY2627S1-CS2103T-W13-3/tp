package seedu.address.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.TypicalPersons.ALICE;
import static seedu.address.testutil.TypicalPersons.BENSON;
import static seedu.address.testutil.TypicalPersons.CARL;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.AddressBook;
import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.UserPrefs;
import seedu.address.model.person.ClientId;

/** Regression tests distinguishing permanent IDs from visible row positions. */
public class ClientIdTargetingTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 7);

    @Test
    public void execute_sparseIds_filteredRowNeverBecomesAnId() throws Exception {
        Model model = sparseModel();
        model.updateFilteredPersonList(person -> person == BENSON);
        new ViewCommand(new ClientId(17)).execute(model);
        assertSame(BENSON, model.selectedPersonProperty().get());
        assertEquals(List.of(BENSON), model.getFilteredPersonList());

        CommandException error = assertThrows(CommandException.class, () ->
                new ViewCommand(new ClientId(1)).execute(model));
        assertEquals("No client with ID 1.", error.getMessage());
        assertSame(BENSON, model.selectedPersonProperty().get());
        assertEquals(List.of(BENSON), model.getFilteredPersonList());
    }

    @Test
    public void execute_hiddenView_showsFullListAndSelectsCanonicalClient() throws Exception {
        Model model = sparseModel();
        model.showPendingFollowUps();
        new ViewCommand(new ClientId(41)).execute(model);
        assertSame(CARL, model.selectedPersonProperty().get());
        assertEquals(List.of(ALICE, BENSON, CARL), model.getFilteredPersonList());
        assertFalse(model.showingFollowUpsProperty().get());
        assertFalse(model.hasUnsavedChanges());
    }

    @Test
    public void execute_hiddenMutations_keepSearchAndIdentity() throws Exception {
        Model model = sparseModel();
        model.updateFilteredPersonList(person -> person == BENSON);
        new SetFollowUpCommand(new ClientId(41), "2026-10-06", "Call Carl").execute(model);
        assertEquals(List.of(BENSON), model.getFilteredPersonList());
        assertTrue(model.getAddressBook().getPersonById(new ClientId(41)).orElseThrow().getFollowUp().isPresent());
        new ClearFollowUpCommand(new ClientId(41)).execute(model);
        assertTrue(model.getAddressBook().getPersonById(new ClientId(41)).orElseThrow().getFollowUp().isEmpty());
        new DeleteCommand(new ClientId(2)).execute(model);
        assertTrue(model.getAddressBook().getPersonById(new ClientId(2)).isEmpty());
        assertSame(BENSON, model.getAddressBook().getPersonById(new ClientId(17)).orElseThrow());
        assertEquals(List.of(BENSON), model.getFilteredPersonList());
        assertEquals(42, model.getAddressBook().getNextClientId());
    }

    private Model sparseModel() {
        AddressBook data = new AddressBook();
        data.restorePersons(List.of(ALICE, BENSON, CARL),
                List.of(new ClientId(2), new ClientId(17), new ClientId(41)), 42);
        return new ModelManager(data, new UserPrefs(), TODAY, false);
    }
}
