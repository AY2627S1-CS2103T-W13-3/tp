package seedu.address.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.logic.commands.CommandTestUtil.assertCommandFailure;
import static seedu.address.logic.commands.CommandTestUtil.assertCommandSuccess;
import static seedu.address.testutil.TypicalPersons.getTypicalAddressBook;

import org.junit.jupiter.api.Test;

import seedu.address.logic.Messages;
import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.UserPrefs;
import seedu.address.model.person.ClientId;
import seedu.address.model.person.Person;

/**
 * Contains integration tests (interaction with the Model) and unit tests for
 * {@code DeleteCommand}.
 */
public class DeleteCommandTest {

    private Model model = new ModelManager(getTypicalAddressBook(), new UserPrefs());

    @Test
    public void execute_validIndexUnfilteredList_success() {
        Person personToDelete = model.getAddressBook().getPersonById(new ClientId(1)).orElseThrow();
        DeleteCommand deleteCommand = new DeleteCommand(new ClientId(1));

        String expectedMessage = String.format(DeleteCommand.MESSAGE_DELETE_PERSON_SUCCESS,
                new ClientId(1), Messages.format(personToDelete));

        ModelManager expectedModel = new ModelManager(model.getAddressBook(), new UserPrefs());
        expectedModel.deletePerson(personToDelete);

        assertCommandSuccess(deleteCommand, model, expectedMessage, expectedModel);
    }

    @Test
    public void execute_invalidIndexUnfilteredList_throwsCommandException() {
        ClientId unknownId = new ClientId(model.getAddressBook().getPersonList().size() + 1);
        DeleteCommand deleteCommand = new DeleteCommand(unknownId);

        assertCommandFailure(deleteCommand, model, "No client with ID " + unknownId + ".");
    }

    @Test
    public void execute_validIndexFilteredList_success() {
        Person first = model.getAddressBook().getPersonById(new ClientId(1)).orElseThrow();
        model.updateFilteredPersonList(person -> person.getName().equals(first.getName()));

        Person personToDelete = model.getAddressBook().getPersonById(new ClientId(1)).orElseThrow();
        DeleteCommand deleteCommand = new DeleteCommand(new ClientId(1));

        String expectedMessage = String.format(DeleteCommand.MESSAGE_DELETE_PERSON_SUCCESS,
                new ClientId(1), Messages.format(personToDelete));

        Model expectedModel = new ModelManager(model.getAddressBook(), new UserPrefs());
        expectedModel.deletePerson(personToDelete);
        showNoPerson(expectedModel);

        assertCommandSuccess(deleteCommand, model, expectedMessage, expectedModel);
    }

    @Test
    public void execute_hiddenClient_deletesGloballyAndRetainsFilter() throws Exception {
        Person first = model.getAddressBook().getPersonById(new ClientId(1)).orElseThrow();
        model.updateFilteredPersonList(person -> person.getName().equals(first.getName()));
        Person hidden = model.getAddressBook().getPersonById(new ClientId(2)).orElseThrow();
        new DeleteCommand(new ClientId(2)).execute(model);
        assertFalse(model.getAddressBook().getPersonList().contains(hidden));
        assertEquals(1, model.getFilteredPersonList().size());
    }

    @Test
    public void equals() {
        DeleteCommand deleteFirstCommand = new DeleteCommand(new ClientId(1));
        DeleteCommand deleteSecondCommand = new DeleteCommand(new ClientId(2));

        // same object -> returns true
        assertTrue(deleteFirstCommand.equals(deleteFirstCommand));

        // same values -> returns true
        DeleteCommand deleteFirstCommandCopy = new DeleteCommand(new ClientId(1));
        assertTrue(deleteFirstCommand.equals(deleteFirstCommandCopy));

        // different types -> returns false
        assertFalse(deleteFirstCommand.equals(1));

        // null -> returns false
        assertFalse(deleteFirstCommand.equals(null));

        // different person -> returns false
        assertFalse(deleteFirstCommand.equals(deleteSecondCommand));
    }

    @Test
    public void toStringMethod() {
        ClientId targetId = new ClientId(1);
        DeleteCommand deleteCommand = new DeleteCommand(targetId);
        String expected = DeleteCommand.class.getCanonicalName() + "{targetId=" + new ClientId(1) + "}";
        assertEquals(expected, deleteCommand.toString());
    }

    /**
     * Updates {@code model}'s filtered list to show no one.
     */
    private void showNoPerson(Model model) {
        model.updateFilteredPersonList(p -> false);

        assertTrue(model.getFilteredPersonList().isEmpty());
    }
}
