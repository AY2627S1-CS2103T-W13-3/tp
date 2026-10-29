package seedu.address.logic;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.model.AddressBook;
import seedu.address.model.ModelManager;
import seedu.address.model.ReadOnlyAddressBook;
import seedu.address.model.UserPrefs;
import seedu.address.model.person.FollowUp;
import seedu.address.model.person.Person;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;
import seedu.address.testutil.PersonBuilder;

/** Exercises phone search through the real parser, command, model and saved-data boundary. */
public class FindPhoneWorkflowIntegrationTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 28);
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-28T00:00:00Z"), ZoneOffset.UTC);

    @TempDir
    public Path testFolder;

    private Person alice;
    private Person bob;
    private Person carol;
    private Person numericName;
    private List<Person> allClients;
    private Path file;
    private byte[] originalBytes;
    private ModelManager model;
    private LogicManager logic;

    @BeforeEach
    public void setUp() throws IOException {
        alice = new PersonBuilder().withName("Alice Lim").withPhone("00123456").withTags("renewal").build();
        bob = new PersonBuilder().withName("Bob Baker").withPhone("81234562")
                .withFollowUp(new FollowUp(TODAY, "Call Bob")).build();
        carol = new PersonBuilder().withName("Carol Lim").withPhone("81234562").build();
        numericName = new PersonBuilder().withName("81234562 Agent").withPhone("90909090").build();
        allClients = List.of(alice, bob, carol, numericName);
        AddressBook book = new AddressBook();
        book.setPersons(allClients);
        file = testFolder.resolve("clients.json");
        new JsonAddressBookStorage(file).saveAddressBook(book);
        originalBytes = Files.readAllBytes(file);
        model = new ModelManager(book, new UserPrefs(), TODAY.minusDays(1), false);
        StorageManager storage = new StorageManager(new JsonAddressBookStorage(file),
                new JsonUserPrefsStorage(testFolder.resolve("prefs.json"))) {
            @Override
            public void saveAddressBook(ReadOnlyAddressBook addressBook) {
                throw new AssertionError("Search must not save client data");
            }
        };
        logic = new LogicManager(model, storage, CLOCK);
    }

    @Test
    public void find_fullPhoneOrName_returnsEveryMatchOnceInInsertionOrder() throws Exception {
        assertEquals("3 client(s) listed!", logic.execute("find 81234562").getFeedbackToUser());
        assertEquals(List.of(bob, carol, numericName), model.getFilteredPersonList());

        assertEquals("4 client(s) listed!",
                logic.execute("find 81234562 aLiCe Bob 81234562").getFeedbackToUser());
        assertEquals(allClients, model.getFilteredPersonList());
        assertUnchangedClientData();
    }

    @Test
    public void find_phoneKeywords_requireExactDigitsAndRetainLeadingZeroes() throws Exception {
        assertEquals("1 client(s) listed!", logic.execute("find 00123456").getFeedbackToUser());
        assertEquals(List.of(alice), model.getFilteredPersonList());
        for (String keyword : List.of("8123", "1234562", "081234562", "123456", "000123456",
                "+81234562", "8123-4562", "8123 4562", "81234562,", "\"81234562\"")) {
            assertEquals("0 client(s) listed!", logic.execute("find " + keyword).getFeedbackToUser());
            assertEquals(List.of(), model.getFilteredPersonList());
        }
        assertUnchangedClientData();
    }

    @Test
    public void find_explicitFields_matchPartialNamePhoneAndExactTag() throws Exception {
        assertEquals("1 client(s) listed!", logic.execute("find n/lice").getFeedbackToUser());
        assertEquals(List.of(alice), model.getFilteredPersonList());

        assertEquals("1 client(s) listed!", logic.execute("find p/0123").getFeedbackToUser());
        assertEquals(List.of(alice), model.getFilteredPersonList());

        assertEquals("1 client(s) listed!", logic.execute("find t/ReNeWaL").getFeedbackToUser());
        assertEquals(List.of(alice), model.getFilteredPersonList());

        assertEquals("0 client(s) listed!", logic.execute("find t/new").getFeedbackToUser());
        assertEquals(List.of(), model.getFilteredPersonList());
        assertUnchangedClientData();
    }

    @Test
    public void find_afterPendingOrSearch_reconcilesSelectionAndSearchesAllClients() throws Exception {
        model.showPendingFollowUps();
        model.selectPerson(bob);
        assertEquals(List.of(bob), model.getFilteredPersonList());

        logic.execute("find 81234562");

        assertEquals(List.of(bob, carol, numericName), model.getFilteredPersonList());
        assertFalse(model.showingFollowUpsProperty().get());
        assertSame(bob, model.selectedPersonProperty().get());
        logic.execute("find 00123456");
        assertEquals(List.of(alice), model.getFilteredPersonList());
        assertNull(model.selectedPersonProperty().get());
        logic.execute("find 81234562");
        assertEquals(List.of(bob, carol, numericName), model.getFilteredPersonList());
        assertNull(model.selectedPersonProperty().get());
        assertUnchangedClientData();
    }

    private void assertUnchangedClientData() throws IOException {
        assertEquals(allClients, model.getAddressBook().getPersonList());
        assertFalse(model.hasUnsavedChanges());
        assertEquals(TODAY, model.getToday());
        assertArrayEquals(originalBytes, Files.readAllBytes(file));
    }
}
