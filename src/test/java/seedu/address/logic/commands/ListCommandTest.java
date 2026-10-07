package seedu.address.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static seedu.address.logic.commands.CommandTestUtil.assertCommandSuccess;
import static seedu.address.logic.commands.CommandTestUtil.showPersonAtIndex;
import static seedu.address.testutil.TypicalIndexes.INDEX_FIRST_PERSON;
import static seedu.address.testutil.TypicalPersons.getTypicalAddressBook;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import seedu.address.model.AddressBook;
import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.UserPrefs;
import seedu.address.model.person.FollowUp;
import seedu.address.model.person.Person;
import seedu.address.testutil.AddressBookBuilder;
import seedu.address.testutil.PersonBuilder;

/**
 * Contains integration tests (interaction with the Model) and unit tests for ListCommand.
 */
public class ListCommandTest {

    private Model model;
    private Model expectedModel;

    @BeforeEach
    public void setUp() {
        model = new ModelManager(getTypicalAddressBook(), new UserPrefs());
        expectedModel = new ModelManager(model.getAddressBook(), new UserPrefs());
    }

    @Test
    public void execute_listIsNotFiltered_showsSameList() {
        assertCommandSuccess(new ListCommand(), model, ListCommand.MESSAGE_SUCCESS, expectedModel);
    }

    @Test
    public void execute_listIsFiltered_showsEverything() {
        showPersonAtIndex(model, INDEX_FIRST_PERSON);
        assertCommandSuccess(new ListCommand(), model, ListCommand.MESSAGE_SUCCESS, expectedModel);
    }
    @Test
    public void execute_pendingView_restoresInsertionOrderWithoutMutatingData() {
        LocalDate today = LocalDate.of(2026, 10, 4);
        Person later = new PersonBuilder().withName("Alice")
                .withFollowUp(new FollowUp(today.plusDays(2), "Call Alice")).build();
        Person earlier = new PersonBuilder().withName("Benson")
                .withFollowUp(new FollowUp(today, "Call Benson")).build();
        AddressBook data = new AddressBookBuilder().withPerson(later).withPerson(earlier).build();
        Model pendingModel = new ModelManager(data, new UserPrefs(), today, false);
        new FollowUpsCommand().execute(pendingModel);
        assertEquals(List.of(earlier, later), pendingModel.getFilteredPersonList());

        assertEquals(new CommandResult("Listed all clients."), new ListCommand().execute(pendingModel));

        assertEquals(List.of(later, earlier), pendingModel.getFilteredPersonList());
        assertFalse(pendingModel.showingFollowUpsProperty().get());
        assertFalse(pendingModel.hasUnsavedChanges());
        assertEquals(data, pendingModel.getAddressBook());
        assertEquals(today, pendingModel.getToday());
    }
}
