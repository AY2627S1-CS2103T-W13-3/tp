package seedu.address.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static seedu.address.logic.commands.CommandTestUtil.showPersonAtIndex;
import static seedu.address.testutil.TypicalIndexes.INDEX_FIRST_PERSON;
import static seedu.address.testutil.TypicalPersons.getTypicalAddressBook;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.UserPrefs;

public class CountCommandTest {

    private Model model;

    @BeforeEach
    public void setUp() {
        model = new ModelManager(getTypicalAddressBook(), new UserPrefs());
    }

    @Test
    public void execute_multiplePersons_showsPluralCount() {
        int personCount = model.getFilteredPersonList().size();

        CommandResult result = new CountCommand().execute(model);

        assertEquals(personCount + " persons listed.", result.getFeedbackToUser());
    }

    @Test
    public void execute_onePerson_showsSingularCount() {
        showPersonAtIndex(model, INDEX_FIRST_PERSON);

        CommandResult result = new CountCommand().execute(model);

        assertEquals("1 person listed.", result.getFeedbackToUser());
    }

    @Test
    public void execute_noPersons_showsZeroCount() {
        model.updateFilteredPersonList(unused -> false);

        CommandResult result = new CountCommand().execute(model);

        assertEquals("0 persons listed.", result.getFeedbackToUser());
    }
}
