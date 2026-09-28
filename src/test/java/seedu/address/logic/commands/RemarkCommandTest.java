package seedu.address.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static seedu.address.logic.commands.CommandTestUtil.assertCommandFailure;
import static seedu.address.logic.commands.CommandTestUtil.assertCommandSuccess;
import static seedu.address.logic.commands.CommandTestUtil.showPersonAtIndex;
import static seedu.address.testutil.TypicalIndexes.INDEX_FIRST_PERSON;
import static seedu.address.testutil.TypicalIndexes.INDEX_SECOND_PERSON;
import static seedu.address.testutil.TypicalPersons.getTypicalAddressBook;

import org.junit.jupiter.api.Test;

import seedu.address.commons.core.index.Index;
import seedu.address.logic.Messages;
import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.UserPrefs;
import seedu.address.model.person.Person;
import seedu.address.model.person.Remark;
import seedu.address.testutil.PersonBuilder;

public class RemarkCommandTest {
    private final Model model = new ModelManager(getTypicalAddressBook(), new UserPrefs());

    @Test
    public void execute_addRemark_success() {
        assertRemarkSuccess("Likes coffee");
    }

    @Test
    public void execute_replaceExistingRemark_success() {
        Person original = model.getFilteredPersonList().get(0);
        model.setPerson(original, new PersonBuilder(original).withRemark("Likes coffee").build());
        assertRemarkSuccess("Likes tea");
    }

    @Test
    public void execute_clearExistingRemark_success() {
        Person original = model.getFilteredPersonList().get(0);
        model.setPerson(original, new PersonBuilder(original).withRemark("Likes coffee").build());
        assertRemarkSuccess("");
    }

    @Test
    public void execute_filteredList_updatesDisplayedPerson() {
        showPersonAtIndex(model, INDEX_SECOND_PERSON);
        assertRemarkSuccess("Likes tea");
    }

    @Test
    public void execute_invalidIndexUnfilteredList_failure() {
        Index invalidIndex = Index.fromOneBased(model.getFilteredPersonList().size() + 1);
        assertCommandFailure(new RemarkCommand(invalidIndex, new Remark("coffee")), model,
                Messages.MESSAGE_INVALID_PERSON_DISPLAYED_INDEX);
    }

    @Test
    public void execute_invalidIndexFilteredList_failure() {
        showPersonAtIndex(model, INDEX_FIRST_PERSON);
        assertCommandFailure(new RemarkCommand(INDEX_SECOND_PERSON, new Remark("coffee")), model,
                Messages.MESSAGE_INVALID_PERSON_DISPLAYED_INDEX);
    }

    @Test
    public void equals() {
        RemarkCommand command = new RemarkCommand(INDEX_FIRST_PERSON, new Remark("coffee"));
        assertEquals(command, command);
        assertEquals(command, new RemarkCommand(INDEX_FIRST_PERSON, new Remark("coffee")));
        assertFalse(command.equals(null));
        assertFalse(command.equals(1));
        assertFalse(command.equals(new RemarkCommand(INDEX_SECOND_PERSON, new Remark("coffee"))));
        assertFalse(command.equals(new RemarkCommand(INDEX_FIRST_PERSON, new Remark("tea"))));
    }

    private void assertRemarkSuccess(String value) {
        Person original = model.getFilteredPersonList().get(0);
        Person edited = new PersonBuilder(original).withRemark(value).build();
        Model expectedModel = new ModelManager(model.getAddressBook(), new UserPrefs());
        expectedModel.setPerson(original, edited);
        String message = value.isEmpty() ? RemarkCommand.MESSAGE_DELETE_REMARK_SUCCESS
                : RemarkCommand.MESSAGE_ADD_REMARK_SUCCESS;
        assertCommandSuccess(new RemarkCommand(INDEX_FIRST_PERSON, new Remark(value)), model,
                String.format(message, Messages.format(edited)), expectedModel);
    }
}
