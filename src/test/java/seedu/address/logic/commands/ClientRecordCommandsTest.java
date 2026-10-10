package seedu.address.logic.commands;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.Assert.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.logic.LogicManager;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.AddressBook;
import seedu.address.model.ModelManager;
import seedu.address.model.ReadOnlyAddressBook;
import seedu.address.model.UserPrefs;
import seedu.address.model.person.FollowUp;
import seedu.address.model.person.NameContainsKeywordsPredicate;
import seedu.address.model.person.Person;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;
import seedu.address.testutil.PersonBuilder;

/**
 * Exercises client commands through the real parser, model, transaction and temporary-file storage.
 */
public class ClientRecordCommandsTest {
    private static final LocalDate STARTUP_DATE = LocalDate.of(2026, 10, 8);
    private static final Clock COMMAND_CLOCK = Clock.fixed(Instant.parse("2026-10-09T00:00:00Z"), ZoneOffset.UTC);
    private static final String ADD_NEW = "add n/John Tan p/003 e/John@example.com a/Blk 3";

    @TempDir
    public Path temporaryFolder;

    private final Person rachel = new PersonBuilder().withName("Rachel Lim").withPhone("001")
            .withEmail("Rachel@example.com").withAddress("Blk 1").withTags("HEALTH", "active")
            .withFollowUp(new FollowUp(STARTUP_DATE.plusDays(2), "Call Rachel")).build();
    private final Person otherRachel = new PersonBuilder(rachel).withPhone("002").withEmail("other@example.com")
            .withTags().withFollowUp(new FollowUp(STARTUP_DATE.plusDays(1), "Send quotation")).build();
    private ModelManager model;
    private CountingStorage storage;
    private LogicManager logic;

    @BeforeEach
    public void setUp() throws Exception {
        AddressBook addressBook = new AddressBook();
        addressBook.setPersons(List.of(rachel, otherRachel));
        model = new ModelManager(addressBook, new UserPrefs(), STARTUP_DATE, false);
        storage = new CountingStorage(temporaryFolder);
        storage.saveAddressBook(addressBook);
        storage.saveCount = 0;
        logic = new LogicManager(model, storage, COMMAND_CLOCK);
    }

    @Test
    public void execute_addSameNameDifferentContacts_savesAndResetsPendingView() throws Exception {
        model.showPendingFollowUps();
        CommandResult result = logic.execute("add n/Rachel Lim p/004 e/third@example.com a/新加坡 🏠"
                + " t/HEALTH t/health t/2026");
        assertEquals("New client added: Rachel Lim; Phone: 004; Email: third@example.com; Address: 新加坡 🏠; "
                + "Tags: [2026] [health]", result.getFeedbackToUser());
        assertEquals(3, model.getAddressBook().getPersonList().size());
        assertEquals(model.getAddressBook().getPersonList(), model.getFilteredPersonList());
        assertFalse(model.showingFollowUpsProperty().get());
        assertEquals(1, storage.saveCount);
        assertEquals(model.getAddressBook(), storage.readAddressBook().orElseThrow());
        assertEquals(STARTUP_DATE.plusDays(1), model.getToday());
    }

    @Test
    public void execute_addWhileFiltered_resetsToFullInsertionOrder() throws Exception {
        model.updateFilteredPersonList(person -> person.equals(otherRachel));
        assertEquals("New client added: John Tan; Phone: 003; Email: John@example.com; Address: Blk 3; Tags: None",
                logic.execute(ADD_NEW).getFeedbackToUser());
        assertEquals(List.of(rachel, otherRachel), model.getFilteredPersonList().subList(0, 2));
        assertEquals(3, model.getFilteredPersonList().size());
        assertEquals(1, storage.saveCount);
    }

    @Test
    public void execute_addNormalizedDuplicates_rejectsAfterValidationWithoutSaving() throws Exception {
        model.showPendingFollowUps();
        assertRejectedWithoutChanges("add n/RACHEL   LIM p/001 e/new@example.com a/Another address",
                CommandException.class, "This client already exists in Policy Harbour.");
        assertRejectedWithoutChanges("add n/rachel lim p/999 e/RACHEL@EXAMPLE.COM a/Another address",
                CommandException.class, "This client already exists in Policy Harbour.");
        assertRejectedWithoutChanges("add n/Rachel Lim p/001 e/bad a/Blk 1",
                ParseException.class, "Email format is not supported. Example: rachel.lim+work@example.com.");
    }

    @Test
    public void execute_deleteLeadingZeroIndex_usesPendingOrderAndRetainsMode() throws Exception {
        model.showPendingFollowUps();
        assertEquals(List.of(otherRachel, rachel), model.getFilteredPersonList());
        CommandResult result = logic.execute("delete " + "0".repeat(10000) + "1");
        assertEquals("Deleted client: Rachel Lim; Phone: 002; Email: other@example.com; Address: Blk 1; Tags: None",
                result.getFeedbackToUser());
        assertEquals(List.of(rachel), model.getAddressBook().getPersonList());
        assertEquals(List.of(rachel), model.getFilteredPersonList());
        assertTrue(model.showingFollowUpsProperty().get());
        assertEquals(1, storage.saveCount);
        assertEquals(model.getAddressBook(), storage.readAddressBook().orElseThrow());
    }

    @Test
    public void execute_deleteFilteredIndex_keepsOtherSameNameRecord() throws Exception {
        model.updateFilteredPersonList(person -> person.equals(otherRachel));
        logic.execute("delete 01");
        assertEquals(List.of(rachel), model.getAddressBook().getPersonList());
        assertTrue(model.getFilteredPersonList().isEmpty());
        assertEquals(1, storage.saveCount);
    }

    @Test
    public void execute_deleteErrors_preservesDataViewDateAndBytes() throws Exception {
        model.showPendingFollowUps();
        assertRejectedWithoutChanges("delete 0 extra", ParseException.class,
                "Invalid command format!\nUsage: delete INDEX");
        assertRejectedWithoutChanges("delete 2147483648", ParseException.class,
                "Index must be a positive integer from 1 to 2147483647.");
        assertRejectedWithoutChanges("delete 2147483647", CommandException.class,
                "The client index provided is invalid.");
        assertRejectedWithoutChanges("delete", ParseException.class, "Invalid command format!\nUsage: delete INDEX");
    }

    @Test
    public void execute_saveFailureOnAddAndDelete_retainsLiveStateAndSavedBytes() throws Exception {
        model.showPendingFollowUps();
        storage.shouldFail = true;
        assertRejectedWithoutChanges(ADD_NEW, CommandException.class,
                "Changes could not be saved. No changes were kept. Try the command again.");
        assertRejectedWithoutChanges("delete 1", CommandException.class,
                "Changes could not be saved. No changes were kept. Try the command again.");
    }

    @Test
    public void execute_followUpAfterSameNameAddition_targetsDisplayedRecord() throws Exception {
        model.showPendingFollowUps();
        logic.execute("followup 0001 d/2026-10-11 m/Send revised quotation");
        assertEquals(List.of(rachel, otherRachel.withFollowUp(
                new FollowUp(LocalDate.of(2026, 10, 11), "Send revised quotation"))), model.getFilteredPersonList());
        assertEquals(rachel, model.getAddressBook().getPersonList().getFirst());
        assertEquals(model.getAddressBook(), storage.readAddressBook().orElseThrow());
    }

    @Test
    public void execute_rawControlCharacters_precedesAddShapeAndPreservesState() throws Exception {
        assertRejectedWithoutChanges("add n/Rachel\n", ParseException.class,
                "Use one command line and ordinary spaces. Control characters are not allowed.");
    }

    @Test
    public void execute_nameSearch_reportsClientCountAndDoesNotSave() throws Exception {
        assertEquals("2 client(s) listed!", logic.execute("find Rachel").getFeedbackToUser());
        assertEquals(List.of(rachel, otherRachel), model.getFilteredPersonList());
        assertEquals(0, storage.saveCount);
    }

    @Test
    public void execute_addAfterSearch_checksWholeAddressBookForDuplicates() throws Exception {
        model.updateFilteredPersonList(new NameContainsKeywordsPredicate(List.of("Nobody")));
        assertRejectedWithoutChanges("add n/Rachel Lim p/001 e/new@example.com a/Another address",
                CommandException.class, "This client already exists in Policy Harbour.");
    }

    private void assertRejectedWithoutChanges(String input, Class<? extends Throwable> exception, String message)
            throws Exception {
        AddressBook original = new AddressBook(model.getAddressBook());
        List<Person> originalRows = new ArrayList<>(model.getFilteredPersonList());
        LocalDate originalToday = model.getToday();
        boolean originalMode = model.showingFollowUpsProperty().get();
        byte[] originalBytes = Files.readAllBytes(storage.getAddressBookFilePath());
        int originalSaves = storage.saveCount;
        assertThrows(exception, message, () -> logic.execute(input));
        assertEquals(original, model.getAddressBook());
        assertEquals(originalRows, model.getFilteredPersonList());
        assertEquals(originalToday, model.getToday());
        assertEquals(originalMode, model.showingFollowUpsProperty().get());
        assertEquals(originalSaves, storage.saveCount);
        assertFalse(model.hasUnsavedChanges());
        assertArrayEquals(originalBytes, Files.readAllBytes(storage.getAddressBookFilePath()));
    }

    /**
     * Counts completed real-file saves and injects a failure before writing when requested.
     */
    private static class CountingStorage extends StorageManager {
        private int saveCount;
        private boolean shouldFail;

        CountingStorage(Path directory) {
            super(new JsonAddressBookStorage(directory.resolve("clients.json")),
                    new JsonUserPrefsStorage(directory.resolve("preferences.json")));
        }

        @Override
        public void saveAddressBook(ReadOnlyAddressBook addressBook) throws IOException {
            if (shouldFail) {
                throw new IOException("Injected save failure");
            }
            super.saveAddressBook(addressBook);
            saveCount++;
        }
    }
}
