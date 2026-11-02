package seedu.address;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

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
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.AddressBook;
import seedu.address.model.ModelManager;
import seedu.address.model.ReadOnlyAddressBook;
import seedu.address.model.UserPrefs;
import seedu.address.model.person.ClientId;
import seedu.address.model.person.Email;
import seedu.address.model.person.Person;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;

/** Checks accepted email spelling and rejected mutations through real command and storage boundaries. */
public class EmailApostropheWorkflowTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 2);
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-11-02T00:00:00Z"), ZoneOffset.UTC);
    private static final String EMAIL = "sean.o'brien@example.com";

    @TempDir
    public Path testFolder;

    private JsonAddressBookStorage storage;
    private ModelManager model;
    private LogicManager logic;

    @BeforeEach
    public void setUp() throws Exception {
        storage = new JsonAddressBookStorage(testFolder.resolve("clients.json"));
        storage.saveAddressBook(new AddressBook());
        model = new ModelManager(new AddressBook(), new UserPrefs(), TODAY, false);
        logic = createLogic(storage);
    }

    @Test
    public void addAndEdit_apostropheEmail_preservesExactValueAcrossRestart() throws Exception {
        logic.execute("add n/Sean O'Brien p/91234567 e/" + EMAIL + " a/Blk 123");
        assertSavedEmail(EMAIL);
        logic.execute("edit 1 e/Sean.O'Brien@Example.COM");
        assertSavedEmail("Sean.O'Brien@Example.COM");
        logic.execute("edit 1 e/" + EMAIL);
        assertSavedEmail(EMAIL);
        assertEquals(2, model.getAddressBook().getNextClientId());
    }

    @Test
    public void invalidEmail_preservesDataSelectionIdsAndSavedBytes() throws Exception {
        logic.execute("add n/Sean O'Brien p/91234567 e/sean@example.com a/Blk 123");
        logic.execute("view 1");
        AddressBook original = new AddressBook(model.getAddressBook());
        Person selected = model.selectedPersonProperty().get();
        byte[] bytes = Files.readAllBytes(storage.getAddressBookFilePath());
        for (String email : List.of("sean.'brien@example.com", "sean..brien@example.com",
                "sean.o'brien@-example.com", "sean.o'brien@example.c")) {
            for (String command : List.of("edit 1 e/" + email,
                    "add n/Another Client p/81234567 e/" + email + " a/Blk 456")) {
                ParseException exception = assertThrows(ParseException.class, () -> logic.execute(command));
                assertEquals(Email.MESSAGE_CONSTRAINTS, exception.getMessage());
                assertPreserved(original, selected, bytes);
            }
        }
        logic.execute("add n/Next Client p/81234567 e/next@example.com a/Blk 456");
        assertEquals("Next Client", model.getAddressBook().getPersonById(new ClientId(2))
                .orElseThrow().getName().fullName);
    }

    @Test
    public void failedApostropheSave_preservesOriginalThenRetryKeepsId() throws Exception {
        logic.execute("add n/Sean O'Brien p/91234567 e/sean@example.com a/Blk 123");
        logic.execute("view 1");
        AddressBook original = new AddressBook(model.getAddressBook());
        Person selected = model.selectedPersonProperty().get();
        byte[] bytes = Files.readAllBytes(storage.getAddressBookFilePath());
        JsonAddressBookStorage failingStorage = new JsonAddressBookStorage(storage.getAddressBookFilePath()) {
            @Override
            public void saveAddressBook(ReadOnlyAddressBook addressBook) throws IOException {
                throw new IOException("Injected save failure");
            }
        };
        LogicManager failingLogic = createLogic(failingStorage);
        CommandException exception =
                assertThrows(CommandException.class, () -> failingLogic.execute("edit 1 e/" + EMAIL));
        assertEquals(String.format(LogicManager.MESSAGE_SAVE_FAILED, storage.getAddressBookFilePath()),
                exception.getMessage());
        assertPreserved(original, selected, bytes);
        logic.execute("edit 1 e/" + EMAIL);
        assertSavedEmail(EMAIL);
        assertEquals(2, model.getAddressBook().getNextClientId());
    }

    private LogicManager createLogic(JsonAddressBookStorage addressBookStorage) {
        return new LogicManager(model, new StorageManager(addressBookStorage,
                new JsonUserPrefsStorage(testFolder.resolve("prefs.json"))), CLOCK);
    }

    private void assertSavedEmail(String expected) throws Exception {
        ReadOnlyAddressBook reloaded = storage.readAddressBook().orElseThrow();
        assertEquals(expected, reloaded.getPersonById(new ClientId(1)).orElseThrow().getEmail().value);
        assertEquals(model.getAddressBook(), reloaded);
    }

    private void assertPreserved(AddressBook original, Person selected, byte[] bytes) throws IOException {
        assertEquals(original, model.getAddressBook());
        assertEquals(List.of(selected), model.getFilteredPersonList());
        assertSame(selected, model.selectedPersonProperty().get());
        assertFalse(model.hasUnsavedChanges());
        assertArrayEquals(bytes, Files.readAllBytes(storage.getAddressBookFilePath()));
    }
}
