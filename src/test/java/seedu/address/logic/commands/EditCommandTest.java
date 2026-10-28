package seedu.address.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.Assert.assertThrows;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import seedu.address.logic.commands.EditCommand.EditPersonDescriptor;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.EditCommandParser;
import seedu.address.model.AddressBook;
import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.UserPrefs;
import seedu.address.model.person.ClientId;
import seedu.address.model.person.FollowUp;
import seedu.address.model.person.Person;
import seedu.address.testutil.EditPersonDescriptorBuilder;
import seedu.address.testutil.PersonBuilder;

/** Tests contact editing against canonical permanent identities. */
public class EditCommandTest {
    private final Person first = new PersonBuilder().withName("Rachel Lim").withPhone("91111111")
            .withEmail("first@example.com").build();
    private final FollowUp action = new FollowUp(LocalDate.of(2026, 10, 5), "Call client");
    private final Person second = new PersonBuilder().withName("Rachel Lim").withPhone("92222222")
            .withEmail("second@example.com").withTags("friend").build().withFollowUp(action);
    private final Person third = new PersonBuilder().withName("Other Client").withPhone("93333333")
            .withEmail("third@example.com").build();
    private final Model model = createModel();

    private Model createModel() {
        AddressBook book = new AddressBook();
        book.restorePersons(List.of(first, second, third),
                List.of(new ClientId(2), new ClientId(17), new ClientId(41)), 42);
        return new ModelManager(book, new UserPrefs());
    }

    @Test
    public void execute_hiddenTarget_preservesFilterIdentityAndFollowUp() throws Exception {
        model.updateFilteredPersonList(person -> person == first);
        model.selectPerson(first);
        new EditCommandParser().parse("17 p/+65 9444-4444 t/renewal").execute(model);
        Person edited = model.getAddressBook().getPersonById(new ClientId(17)).orElseThrow();
        assertEquals("+65 9444-4444", edited.getPhone().value);
        assertEquals(java.util.Set.of(new seedu.address.model.tag.Tag("renewal")), edited.getTags());
        assertEquals(Optional.of(action), edited.getFollowUp());
        assertEquals(second.getName(), edited.getName());
        assertEquals(Optional.of(new ClientId(17)), model.getAddressBook().getClientId(edited));
        assertEquals(42, model.getAddressBook().getNextClientId());
        assertEquals(List.of(first), model.getFilteredPersonList());
        assertSame(first, model.selectedPersonProperty().get());
    }

    @Test
    public void execute_selectedTargetLeavesSearch_clearsSelection() throws Exception {
        model.updateFilteredPersonList(person -> person.getTags().contains(new seedu.address.model.tag.Tag("friend")));
        model.selectPerson(second);
        new EditCommandParser().parse("17 t/").execute(model);
        assertTrue(model.getFilteredPersonList().isEmpty());
        assertEquals(null, model.selectedPersonProperty().get());
        assertTrue(model.getAddressBook().getPersonById(new ClientId(17)).orElseThrow().getTags().isEmpty());
    }

    @Test
    public void execute_identicalEdit_retainsSelectionAndPendingMode() throws Exception {
        model.showPendingFollowUps();
        model.selectPerson(second);
        new EditCommandParser().parse("17 n/Rachel Lim").execute(model);
        Person canonical = model.getAddressBook().getPersonById(new ClientId(17)).orElseThrow();
        assertSame(canonical, model.selectedPersonProperty().get());
        assertEquals(List.of(canonical), model.getFilteredPersonList());
        assertTrue(model.showingFollowUpsProperty().get());
        assertEquals(Optional.of(action), canonical.getFollowUp());
        assertEquals(42, model.getAddressBook().getNextClientId());
    }

    @Test
    public void execute_unchangedNamePhoneOrEmailConflict_normalErrorAndNoChanges() throws Exception {
        for (String fields : List.of("p/9111-1111", "e/first@example.com")) {
            AddressBook before = new AddressBook(model.getAddressBook());
            EditCommand command = new EditCommandParser().parse("17 " + fields);
            assertThrows(CommandException.class, EditCommand.MESSAGE_DUPLICATE_PERSON, () -> command.execute(model));
            assertEquals(before, model.getAddressBook());
        }
    }

    @Test
    public void execute_unknownId_doesNotFallBackToVisibleRow() throws Exception {
        model.updateFilteredPersonList(person -> person == second);
        EditCommand command = new EditCommandParser().parse("1 n/Someone Else");
        assertThrows(CommandException.class, "No client with ID 1.", () -> command.execute(model));
        assertEquals(List.of(second), model.getFilteredPersonList());
    }

    @Test
    public void equalsAndToString_useIdAndDescriptor() {
        EditPersonDescriptor descriptor = new EditPersonDescriptorBuilder().withName("New Name").build();
        EditCommand command = new EditCommand(new ClientId(17), descriptor);
        assertEquals(command, new EditCommand(new ClientId(17), new EditPersonDescriptor(descriptor)));
        assertFalse(command.equals(new EditCommand(new ClientId(2), descriptor)));
        assertFalse(command.equals(new EditCommand(new ClientId(17), new EditPersonDescriptor())));
        assertFalse(command.equals(null));
        assertEquals(EditCommand.class.getCanonicalName() + "{clientId=17, editPersonDescriptor="
                + descriptor + "}", command.toString());
    }
}
