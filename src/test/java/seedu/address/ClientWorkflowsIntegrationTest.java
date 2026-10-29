package seedu.address;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.logic.LogicManager;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.AddressBook;
import seedu.address.model.ModelManager;
import seedu.address.model.ReadOnlyAddressBook;
import seedu.address.model.ReadOnlyUserPrefs;
import seedu.address.model.UserPrefs;
import seedu.address.model.person.ClientId;
import seedu.address.model.person.FollowUp;
import seedu.address.model.person.Person;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.Storage;
import seedu.address.storage.StorageManager;
import seedu.address.testutil.PersonBuilder;

/** Cross-feature checks for add retry and the search-to-edit workflow. */
class ClientWorkflowsIntegrationTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 29);
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-29T00:00:00Z"), ZoneOffset.UTC);

    @TempDir
    private Path testFolder;

    @Test
    void add_failedSaveThenRetry_assignsOneIdOnlyAfterSuccessfulSave() throws Exception {
        Path dataFile = testFolder.resolve("addressbook.json");
        JsonAddressBookStorage addressBookStorage = new JsonAddressBookStorage(dataFile);
        addressBookStorage.saveAddressBook(new AddressBook());
        byte[] originalBytes = Files.readAllBytes(dataFile);
        StorageManager storage = new StorageManager(addressBookStorage,
                new JsonUserPrefsStorage(testFolder.resolve("prefs.json"))) {
            private boolean failNextSave = true;

            @Override
            public void saveAddressBook(ReadOnlyAddressBook addressBook) throws IOException {
                if (failNextSave) {
                    failNextSave = false;
                    throw new IOException("Injected save failure");
                }
                super.saveAddressBook(addressBook);
            }
        };
        ModelManager model = new ModelManager(addressBookStorage.readAddressBook().orElseThrow(),
                new UserPrefs(), TODAY, false);
        LogicManager logic = new LogicManager(model, storage, CLOCK);
        String command = "add n/Retry Client p/91234567 e/retry@example.com a/Somewhere";

        CommandException saveError = assertThrows(CommandException.class, () -> logic.execute(command));
        assertEquals(String.format(LogicManager.MESSAGE_SAVE_FAILED, storage.getAddressBookFilePath()),
                saveError.getMessage());
        assertEquals(List.of(), model.getAddressBook().getPersonList());
        assertEquals(1, model.getAddressBook().getNextClientId());
        assertFalse(model.hasUnsavedChanges());
        assertArrayEquals(originalBytes, Files.readAllBytes(dataFile));

        assertEquals("Client ID 1 added: Retry Client; Phone: 91234567; Email: retry@example.com; "
                + "Address: Somewhere; Tags: None", logic.execute(command).getFeedbackToUser());
        Person added = model.getAddressBook().getPersonList().get(0);
        assertEquals(1, model.getAddressBook().getClientId(added).orElseThrow().getValue());
        assertEquals(2, model.getAddressBook().getNextClientId());
        assertEquals(2, addressBookStorage.readAddressBook().orElseThrow().getNextClientId());
    }

    @Test
    void searchThenEdit_preservesTargetIdAndFollowUp() throws Exception {
        Person alice = new PersonBuilder().withName("Alice Lim").withPhone("81234567")
                .withEmail("alice@example.com").build();
        Person rachel = new PersonBuilder().withName("Rachel Lim").withPhone("92345678")
                .withEmail("rachel@example.com").withFollowUp(new FollowUp(TODAY, "Call Rachel")).build();
        AddressBook addressBook = new AddressBook();
        addressBook.restorePersons(List.of(alice, rachel), List.of(new ClientId(2), new ClientId(17)), 18);
        ModelManager model = new ModelManager(addressBook, new UserPrefs(), TODAY, false);
        LogicManager logic = new LogicManager(model, new MemoryStorage(), CLOCK);

        assertEquals("1 client(s) listed!", logic.execute("find n/rac").getFeedbackToUser());
        String feedback = logic.execute("edit 17 p/+65 9234 5678").getFeedbackToUser();
        assertTrue(feedback.startsWith("Client ID 17 updated: Rachel Lim"));
        assertTrue(feedback.contains("Phone: +65 9234 5678"));

        Person edited = model.getAddressBook().getPersonById(new ClientId(17)).orElseThrow();
        assertEquals(17, model.getAddressBook().getClientId(edited).orElseThrow().getValue());
        assertEquals("+65 9234 5678", edited.getPhone().value);
        assertEquals(rachel.getFollowUp(), edited.getFollowUp());
        assertEquals(List.of(edited), model.getFilteredPersonList());
    }

    @Test
    void addDuringTagSearch_showsMatchingClientAndKeepsNonMatchHidden() throws Exception {
        Person existing = new PersonBuilder().withName("Existing Renewal Client").withPhone("44445555")
                .withEmail("existing-renewal@example.com").withTags("renewal").build();
        AddressBook addressBook = new AddressBook();
        addressBook.addPerson(existing);
        ModelManager model = new ModelManager(addressBook, new UserPrefs(), TODAY, false);
        LogicManager logic = new LogicManager(model, new MemoryStorage(), CLOCK);

        assertEquals("1 client(s) listed!", logic.execute("find t/renewal").getFeedbackToUser());
        model.selectPerson(model.getFilteredPersonList().get(0));
        Person matching = new PersonBuilder().withName("New Renewal Client").withPhone("55556666")
                .withEmail("new-renewal@example.com").withAddress("Somewhere").withTags("renewal").build();
        logic.execute("add n/New Renewal Client p/55556666 e/new-renewal@example.com a/Somewhere t/renewal");

        assertEquals(List.of(existing, matching), model.getFilteredPersonList());
        Person canonical = model.getAddressBook().getPersonById(new ClientId(2)).orElseThrow();
        assertEquals(matching, canonical);
        assertEquals(2, model.getAddressBook().getClientId(canonical).orElseThrow().getValue());
        assertTrue(model.getAddressBook().getClientId(matching).isEmpty());
        assertSame(existing, model.selectedPersonProperty().get());

        logic.execute("add n/Other Client p/66667777 e/other@example.com a/Somewhere t/urgent");
        assertEquals(List.of(existing, matching), model.getFilteredPersonList());
        assertEquals(3, model.getAddressBook().getPersonList().size());
        assertSame(existing, model.selectedPersonProperty().get());
    }

    private static class MemoryStorage implements Storage {
        @Override
        public Path getUserPrefsFilePath() {
            return Path.of("test-prefs.json");
        }

        @Override
        public Optional<UserPrefs> readUserPrefs() {
            return Optional.empty();
        }

        @Override
        public void saveUserPrefs(ReadOnlyUserPrefs userPrefs) {
            // These workflows only persist client changes.
        }

        @Override
        public Path getAddressBookFilePath() {
            return Path.of("test-addressbook.json");
        }

        @Override
        public Optional<ReadOnlyAddressBook> readAddressBook() {
            return Optional.empty();
        }

        @Override
        public void saveAddressBook(ReadOnlyAddressBook addressBook) throws IOException {
            // This workflow checks model publication rather than disk failure.
        }
    }
}
