package seedu.address.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.address.logic.commands.FindCommand;
import seedu.address.logic.parser.FindCommandParser;
import seedu.address.model.AddressBook;
import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.UserPrefs;
import seedu.address.model.person.FollowUp;
import seedu.address.model.person.Person;
import seedu.address.testutil.PersonBuilder;

/** Integration coverage for explicit searches against the complete client list. */
class ClientSearchWorkflowTest {
    @Test
    void execute_explicitSearchAfterPendingMode_matchesFullDatasetAndRetainsVisibleSelection() throws Exception {
        LocalDate today = LocalDate.of(2026, 10, 29);
        Person alice = new PersonBuilder().withName("Alice Lim").withPhone("91234567")
                .withTags("renewal").withoutFollowUp().build();
        Person rachel = new PersonBuilder().withName("Rachel   Lim").withPhone("+65 9234-5678")
                .withTags("renewal").withFollowUp(new FollowUp(today, "Call Rachel")).build();
        Person bob = new PersonBuilder().withName("Bob Tan").withPhone("81234567")
                .withTags("urgent").withoutFollowUp().build();
        AddressBook addressBook = new AddressBook();
        addressBook.addPerson(alice);
        addressBook.addPerson(rachel);
        addressBook.addPerson(bob);
        Model model = new ModelManager(addressBook, new UserPrefs(), today, false);
        model.showPendingFollowUps();
        model.selectPerson(rachel);

        FindCommand findByName = new FindCommandParser().parse("n/rac");
        assertEquals("1 client(s) listed!", findByName.execute(model).getFeedbackToUser());
        assertEquals(List.of(rachel), model.getFilteredPersonList());
        assertSame(rachel, model.selectedPersonProperty().get());

        model.showPendingFollowUps();
        model.selectPerson(rachel);
        assertEquals("1 client(s) listed!", new FindCommandParser().parse("n/alice")
                .execute(model).getFeedbackToUser());
        assertEquals(List.of(alice), model.getFilteredPersonList());
        assertNull(model.selectedPersonProperty().get());
        assertFalse(model.showingFollowUpsProperty().get());
        assertFalse(model.hasUnsavedChanges());

        assertEquals("2 client(s) listed!", new FindCommandParser().parse("t/renewal")
                .execute(model).getFeedbackToUser());
        assertEquals(List.of(alice, rachel), model.getFilteredPersonList());
        assertEquals(List.of(alice, rachel, bob), model.getAddressBook().getPersonList());

        assertEquals("1 client(s) listed!", new FindCommandParser().parse("p/9234")
                .execute(model).getFeedbackToUser());
        assertEquals(List.of(rachel), model.getFilteredPersonList());
        assertNull(model.selectedPersonProperty().get());

        assertEquals("0 client(s) listed!", new FindCommandParser().parse("n/absent")
                .execute(model).getFeedbackToUser());
        assertEquals(List.of(), model.getFilteredPersonList());
        assertNull(model.selectedPersonProperty().get());
        assertFalse(model.hasUnsavedChanges());
    }
}
