package seedu.address.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.logic.LogicManager;
import seedu.address.logic.Messages;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.AddressBook;
import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.UserPrefs;
import seedu.address.model.person.ClientId;
import seedu.address.model.person.FollowUp;
import seedu.address.model.person.Person;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;
import seedu.address.testutil.PersonBuilder;

/** Integration checks for ID-aware add feedback and view-mode retention. */
class ClientAddWorkflowTest {
    @TempDir
    private Path testFolder;

    @Test
    void execute_addsFinalAvailableIdThenReportsExhaustion() throws Exception {
        Person existing = new PersonBuilder().withName("Existing Client").withPhone("11111111")
                .withEmail("existing@example.com").build();
        AddressBook addressBook = new AddressBook();
        addressBook.restorePersons(List.of(existing), List.of(new ClientId(1)), Integer.MAX_VALUE);
        Model model = new ModelManager(addressBook, new UserPrefs(), LocalDate.of(2026, 10, 29), false);
        Person lastClient = new PersonBuilder().withName("Last Client").withPhone("22222222")
                .withEmail("last@example.com").build();

        CommandResult result = new AddCommand(lastClient).execute(model);
        assertEquals(String.format(AddCommand.MESSAGE_SUCCESS, Integer.MAX_VALUE, Messages.format(lastClient)),
                result.getFeedbackToUser());
        assertEquals(Integer.MAX_VALUE,
                model.getAddressBook().getClientId(lastClient).orElseThrow().getValue());

        Person rejected = new PersonBuilder().withName("Rejected Client").withPhone("33333333")
                .withEmail("rejected@example.com").build();
        CommandException error = assertThrows(CommandException.class, () -> new AddCommand(rejected).execute(model));
        assertEquals(AddCommand.MESSAGE_CLIENT_ID_EXHAUSTED, error.getMessage());
        assertFalse(model.getAddressBook().getPersonList().contains(rejected));
    }

    @Test
    void execute_addsMatchingClientWithoutLosingSearchOrSelection() throws Exception {
        Person alice = new PersonBuilder().withName("Alice Lim").withPhone("44444444")
                .withEmail("alice@example.com").build();
        AddressBook addressBook = new AddressBook();
        addressBook.addPerson(alice);
        Model model = new ModelManager(addressBook, new UserPrefs());
        model.updateFilteredPersonList(person -> person.getName().fullName.endsWith("Lim"));
        model.selectPerson(alice);
        Person rachel = new PersonBuilder().withName("Rachel Lim").withPhone("55555555")
                .withEmail("rachel@example.com").build();

        new AddCommand(rachel).execute(model);

        assertEquals(List.of(alice, rachel), model.getFilteredPersonList());
        assertSame(alice, model.selectedPersonProperty().get());

        Person bob = new PersonBuilder().withName("Bob Tan").withPhone("55555556")
                .withEmail("bob@example.com").build();
        new AddCommand(bob).execute(model);
        assertEquals(List.of(alice, rachel), model.getFilteredPersonList());
        assertSame(alice, model.selectedPersonProperty().get());
    }

    @Test
    void execute_addsActionlessClientWithoutShowingItInPendingMode() throws Exception {
        LocalDate today = LocalDate.of(2026, 10, 29);
        Person withFollowUp = new PersonBuilder().withName("Follow Up Client").withPhone("66666666")
                .withEmail("follow@example.com").withFollowUp(new FollowUp(today, "Call client")).build();
        AddressBook addressBook = new AddressBook();
        addressBook.addPerson(withFollowUp);
        Model model = new ModelManager(addressBook, new UserPrefs(), today, false);
        model.showPendingFollowUps();
        model.selectPerson(withFollowUp);
        Person withoutFollowUp = new PersonBuilder().withName("No Action Client").withPhone("77777777")
                .withEmail("noaction@example.com").withoutFollowUp().build();

        new AddCommand(withoutFollowUp).execute(model);

        assertEquals(List.of(withFollowUp), model.getFilteredPersonList());
        assertSame(withFollowUp, model.selectedPersonProperty().get());
        assertEquals(2, model.getAddressBook().getPersonList().size());
    }

    @Test
    void execute_duplicateDoesNotConsumeIdAndDistinctSameNameClientIsAllowed() throws Exception {
        Person first = new PersonBuilder().withName("Jordan Lee").withPhone("88810000")
                .withEmail("jordan@example.com").build();
        AddressBook addressBook = new AddressBook();
        addressBook.addPerson(first);
        Model model = new ModelManager(addressBook, new UserPrefs());
        Person duplicate = new PersonBuilder(first).withAddress("Different Address").build();

        CommandException duplicateError = assertThrows(CommandException.class,
                () -> new AddCommand(duplicate).execute(model));
        assertEquals(AddCommand.MESSAGE_DUPLICATE_PERSON, duplicateError.getMessage());
        assertEquals(2, model.getAddressBook().getNextClientId());
        assertEquals(List.of(first), model.getAddressBook().getPersonList());

        Person distinct = new PersonBuilder(first).withPhone("88810001")
                .withEmail("jordan.other@example.com").build();
        new AddCommand(distinct).execute(model);
        assertEquals(2, model.getAddressBook().getClientId(distinct).orElseThrow().getValue());
    }

    @Test
    void execute_addAfterDeletingHighestIdAndRestart_doesNotReuseDeletedId() throws Exception {
        Person retained = new PersonBuilder().withName("Retained Client").withPhone("10101010")
                .withEmail("retained@example.com").build();
        Person deleted = new PersonBuilder().withName("Deleted Client").withPhone("20202020")
                .withEmail("deleted@example.com").build();
        AddressBook initial = new AddressBook();
        initial.restorePersons(List.of(retained, deleted), List.of(new ClientId(2), new ClientId(41)), 42);
        Path dataFile = testFolder.resolve("addressbook.json");
        JsonAddressBookStorage addressBookStorage = new JsonAddressBookStorage(dataFile);
        addressBookStorage.saveAddressBook(initial);

        StorageManager storage = new StorageManager(addressBookStorage,
                new JsonUserPrefsStorage(testFolder.resolve("prefs.json")));
        Model model = new ModelManager(addressBookStorage.readAddressBook().orElseThrow(),
                new UserPrefs(), LocalDate.of(2026, 10, 29), false);
        LogicManager logic = new LogicManager(model, storage);
        String deleteFeedback = logic.execute("delete 41").getFeedbackToUser();
        assertTrue(deleteFeedback.startsWith("Deleted client ID 41: Deleted Client"));

        Model restartedModel = new ModelManager(addressBookStorage.readAddressBook().orElseThrow(),
                new UserPrefs(), LocalDate.of(2026, 10, 29), false);
        LogicManager restartedLogic = new LogicManager(restartedModel, storage);
        String addFeedback = restartedLogic.execute(
                "add n/New Client p/30303030 e/new@example.com a/Somewhere").getFeedbackToUser();

        assertTrue(addFeedback.startsWith("Client ID 42 added: New Client"));
        Person added = restartedModel.getAddressBook().getPersonById(new ClientId(42)).orElseThrow();
        assertEquals(42, restartedModel.getAddressBook().getClientId(added).orElseThrow().getValue());
        assertEquals(43, restartedModel.getAddressBook().getNextClientId());
    }
}
