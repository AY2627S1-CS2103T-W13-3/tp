package seedu.address;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.logic.Messages.MESSAGE_INVALID_CHARACTERS;
import static seedu.address.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;

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

import seedu.address.logic.LogicManager;
import seedu.address.logic.commands.FindCommand;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.AddressBook;
import seedu.address.model.ModelManager;
import seedu.address.model.ReadOnlyAddressBook;
import seedu.address.model.UserPrefs;
import seedu.address.model.person.ClientId;
import seedu.address.model.person.FollowUp;
import seedu.address.model.person.Person;
import seedu.address.model.tag.Tag;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;
import seedu.address.testutil.PersonBuilder;

/** Exercises find feedback through the real parser, model and saved-data boundary. */
public class FindInvalidValueFeedbackTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 1);
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-01T00:00:00Z"), ZoneOffset.UTC);
    private static final String PHONE_QUERY_CONSTRAINTS =
            "Phone search queries need at least 1 digit (0-9). An optional leading + and single spaces or hyphens"
                    + " between digit groups are allowed.";

    @TempDir
    public Path testFolder;

    private Person alice;
    private Person bob;
    private AddressBook originalBook;
    private Path file;
    private byte[] originalBytes;
    private ModelManager model;
    private LogicManager logic;

    @BeforeEach
    public void setUp() throws IOException {
        alice = new PersonBuilder().withName("Alice Lim").withPhone("91234567").withTags("health")
                .withFollowUp(new FollowUp(TODAY, "Call Alice")).build();
        bob = new PersonBuilder().withName("Bob Tan").withPhone("81234567").withTags("renewal")
                .withoutFollowUp().build();
        originalBook = new AddressBook();
        originalBook.restorePersons(List.of(alice, bob), List.of(new ClientId(7), new ClientId(12)), 19);
        file = testFolder.resolve("clients.json");
        new JsonAddressBookStorage(file).saveAddressBook(originalBook);
        originalBytes = Files.readAllBytes(file);
        model = new ModelManager(originalBook, new UserPrefs(), TODAY, false);
        model.showPendingFollowUps();
        model.selectPerson(alice);
        StorageManager storage = new StorageManager(new JsonAddressBookStorage(file),
                new JsonUserPrefsStorage(testFolder.resolve("prefs.json"))) {
            @Override
            public void saveAddressBook(ReadOnlyAddressBook addressBook) {
                throw new AssertionError("Find must not save client data");
            }
        };
        logic = new LogicManager(model, storage, CLOCK);
    }

    @Test
    public void find_malformedPhone_reportsQueryConstraintsAndPreservesState() throws Exception {
        for (String query : List.of("9123  4567", "12x", "+", "1--2", "１２３")) {
            assertRejectedSearch("find p/" + query, PHONE_QUERY_CONSTRAINTS);
        }
    }

    @Test
    public void find_invalidTag_reportsTagConstraintsAndPreservesState() throws Exception {
        for (String query : List.of("life--insurance", "has space", "health!", "健康")) {
            assertRejectedSearch("find t/" + query, Tag.MESSAGE_CONSTRAINTS);
        }
    }

    @Test
    public void find_structuralErrors_reportUsageBeforeValueConstraints() throws Exception {
        for (String args : List.of("", "n/", "p/", "t/", "p/   ", "t/   ", "n/Rac p/9",
                "n/Rac n/Bob", "p/9 p/1", "t/health t/renewal", "p/p/9", "t/t/health",
                "p/12x t/health", "t/life-insurance p/9", "Alice p/9")) {
            assertRejectedSearch("find " + args,
                    String.format(MESSAGE_INVALID_COMMAND_FORMAT, FindCommand.MESSAGE_USAGE));
        }
    }

    @Test
    public void find_disallowedCharacters_reportGlobalErrorBeforeValueConstraints() throws Exception {
        assertRejectedSearch("find p/12x\t", MESSAGE_INVALID_CHARACTERS);
        assertRejectedSearch("find t/life-insurance\n", MESSAGE_INVALID_CHARACTERS);
    }

    @Test
    public void find_correctedQueries_preserveMatchingAndSearchAllClients() throws Exception {
        assertRejectedSearch("find p/9123  4567", PHONE_QUERY_CONSTRAINTS);
        for (String command : List.of("find p/9", "find p/+9", "find p/9123 4567", "find n/ALICE   LIM",
                "find t/HEALTH", "find 91234567 Alice")) {
            assertEquals("1 client(s) listed!", logic.execute(command).getFeedbackToUser());
            assertEquals(List.of(alice), model.getFilteredPersonList());
            assertSame(alice, model.selectedPersonProperty().get());
            assertFalse(model.showingFollowUpsProperty().get());
            assertUnchangedClientData();
        }

        for (String command : List.of("find 8123", "find t/heal")) {
            assertEquals("0 client(s) listed!", logic.execute(command).getFeedbackToUser());
            assertTrue(model.getFilteredPersonList().isEmpty());
            assertUnchangedClientData();
        }

        // A search must still reach Bob after an empty result and from pending mode.
        model.showPendingFollowUps();
        assertEquals("1 client(s) listed!", logic.execute("find n/Bob").getFeedbackToUser());
        assertEquals(List.of(bob), model.getFilteredPersonList());
        assertFalse(model.showingFollowUpsProperty().get());
        assertUnchangedClientData();
    }

    private void assertRejectedSearch(String command, String expectedMessage) throws Exception {
        ParseException exception = assertThrows(ParseException.class, () -> logic.execute(command));
        assertEquals(expectedMessage, exception.getMessage());
        assertEquals(List.of(alice), model.getFilteredPersonList());
        assertSame(alice, model.selectedPersonProperty().get());
        assertTrue(model.showingFollowUpsProperty().get());
        assertUnchangedClientData();
    }

    private void assertUnchangedClientData() throws IOException {
        // Aggregate equality includes permanent IDs and the next allocation value.
        assertEquals(originalBook, model.getAddressBook());
        assertFalse(model.hasUnsavedChanges());
        assertArrayEquals(originalBytes, Files.readAllBytes(file));
    }
}
