package seedu.address.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.List;

import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.AddressBook;
import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.UserPrefs;
import seedu.address.model.person.Person;

/**
 * Shares fixed-date fixtures and rejected-command preservation checks for follow-up commands.
 */
final class FollowUpCommandTestUtil {
    private FollowUpCommandTestUtil() {
    }

    static Model createModel(LocalDate today, Person... persons) {
        AddressBook data = new AddressBook();
        for (Person person : persons) {
            data.addPerson(person);
        }
        return new ModelManager(data, new UserPrefs(), today, false);
    }

    static void assertFailureUnchanged(Command command, Model model, String message) {
        List<Person> originalPersons = List.copyOf(model.getAddressBook().getPersonList());
        List<Person> originalRows = List.copyOf(model.getFilteredPersonList());
        boolean wasShowingFollowUps = model.showingFollowUpsProperty().get();
        boolean hadUnsavedChanges = model.hasUnsavedChanges();
        LocalDate originalToday = model.getToday();

        CommandException error = assertThrows(CommandException.class, () -> command.execute(model));

        assertEquals(message, error.getMessage());
        assertEquals(originalPersons, model.getAddressBook().getPersonList());
        assertEquals(originalRows, model.getFilteredPersonList());
        assertEquals(wasShowingFollowUps, model.showingFollowUpsProperty().get());
        assertEquals(originalToday, model.getToday());
        assertEquals(hadUnsavedChanges, model.hasUnsavedChanges());
    }
}
