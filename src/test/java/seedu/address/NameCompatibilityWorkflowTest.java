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
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.logic.LogicManager;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.ModelManager;
import seedu.address.model.ReadOnlyAddressBook;
import seedu.address.model.person.ClientId;
import seedu.address.model.person.Name;
import seedu.address.model.person.Person;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;

/** Exercises exact name spelling, feedback, literal matching and persistence boundaries. */
public class NameCompatibilityWorkflowTest {
    @TempDir
    private Path directory;
    private Path dataFile;
    private ModelManager model;
    private LogicManager logic;
    private FailingStorage storage;

    @BeforeEach
    public void setUp() {
        dataFile = directory.resolve("addressbook.json");
        storage = new FailingStorage(dataFile, directory.resolve("preferences.json"));
        model = new ModelManager();
        logic = new LogicManager(model, storage,
                Clock.fixed(Instant.parse("2026-11-02T04:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    public void execute_apostropheVariants_preservesLiteralSearchAndDuplicatePolicy() throws Exception {
        logic.execute("add n/Sean O’Brien p/91234567 e/sean@example.com a/Blk 123");
        logic.execute("add n/Sean O'Brien p/91234567 e/sean@example.com a/Blk 123");
        Person smart = model.getAddressBook().getPersonById(new ClientId(1)).orElseThrow();
        Person ascii = model.getAddressBook().getPersonById(new ClientId(2)).orElseThrow();
        assertEquals("Sean O’Brien", smart.getName().fullName);
        assertEquals("Sean O'Brien", ascii.getName().fullName);
        assertFalse(smart.isDuplicateOf(ascii));
        for (String query : List.of("find O’Brien", "find n/Sean O’Brien", "find n/sean o’brien")) {
            logic.execute(query);
            assertEquals(List.of(smart), logic.getFilteredPersonList());
        }
        for (String query : List.of("find O'Brien", "find n/Sean O'Brien")) {
            logic.execute(query);
            assertEquals(List.of(ascii), logic.getFilteredPersonList());
        }
        logic.execute("view 2");
        assertRejectedUnchanged("edit 2 n/Sean O’Brien", CommandException.class, null);
        assertRejectedUnchanged("add n/SEAN O’BRIEN p/91234567 e/other@example.com a/Home",
                CommandException.class, null);
        logic.execute("add n/Rachel Lim p/81234567 e/rachel@example.com a/Blk 456");
        logic.execute("view 3");
        logic.execute("followup 3 d/2026-11-03 m/Review rider");
        logic.execute("edit 3 n/Mary O’Neil");
        assertEquals("Mary O’Neil", logic.selectedPersonProperty().get().getName().fullName);
        assertEquals(3, logic.getClientId(logic.selectedPersonProperty().get()).orElseThrow().getValue());
        assertTrue(logic.selectedPersonProperty().get().getFollowUp().isPresent());
        ReadOnlyAddressBook reloaded = storage.readAddressBook().orElseThrow();
        assertEquals(model.getAddressBook(), reloaded);
        ModelManager restarted = new ModelManager(reloaded, model.getUserPrefs());
        assertEquals("Sean O’Brien", restarted.getAddressBook().getPersonById(new ClientId(1))
                .orElseThrow().getName().fullName);
        assertEquals("Mary O’Neil", restarted.getAddressBook().getPersonById(new ClientId(3))
                .orElseThrow().getName().fullName);
    }

    @Test
    public void execute_invalidLeadingPunctuationAndFailedSave_preserveCommittedState() throws Exception {
        logic.execute("add n/Rachel Lim p/91234567 e/rachel@example.com a/Blk 123");
        logic.execute("followup 1 d/2026-11-03 m/Call client");
        logic.execute("followups");
        logic.execute("view 1");
        assertTrue(Name.MESSAGE_CONSTRAINTS.contains("begin with a letter or digit (0-9)"));
        assertRejectedUnchanged("add n/-Sean p/81234567 e/sean@example.com a/Home",
                ParseException.class, Name.MESSAGE_CONSTRAINTS);
        assertRejectedUnchanged("edit 1 n/-Sean", ParseException.class, Name.MESSAGE_CONSTRAINTS);
        assertRejectedUnchanged("edit 1 n/’Sean", ParseException.class, Name.MESSAGE_CONSTRAINTS);
        storage.shouldFailSaves = true;
        assertRejectedUnchanged("edit 1 n/Sean O’Brien", CommandException.class, null);
        storage.shouldFailSaves = false;
        logic.execute("edit 1 n/Sean O’Brien");
        assertEquals("Sean O’Brien", logic.selectedPersonProperty().get().getName().fullName);
        assertEquals(model.getAddressBook(), storage.readAddressBook().orElseThrow());
    }

    private void assertRejectedUnchanged(String command, Class<? extends Exception> type, String message)
            throws Exception {
        byte[] bytes = Files.readAllBytes(dataFile);
        List<Person> persons = List.copyOf(logic.getAllPersonList());
        List<Person> displayed = List.copyOf(logic.getFilteredPersonList());
        Person selection = logic.selectedPersonProperty().get();
        long nextId = model.getAddressBook().getNextClientId();
        boolean isPending = logic.showingFollowUpsProperty().get();
        Exception error = assertThrows(type, () -> logic.execute(command));
        if (message != null) {
            assertEquals(message, error.getMessage());
        }
        assertArrayEquals(bytes, Files.readAllBytes(dataFile));
        assertEquals(persons, logic.getAllPersonList());
        assertEquals(displayed, logic.getFilteredPersonList());
        assertSame(selection, logic.selectedPersonProperty().get());
        assertEquals(nextId, model.getAddressBook().getNextClientId());
        assertEquals(isPending, logic.showingFollowUpsProperty().get());
        assertFalse(model.hasUnsavedChanges());
    }

    private static class FailingStorage extends StorageManager {
        private boolean shouldFailSaves;

        FailingStorage(Path data, Path preferences) {
            super(new JsonAddressBookStorage(data), new JsonUserPrefsStorage(preferences));
        }

        @Override
        public void saveAddressBook(ReadOnlyAddressBook addressBook) throws IOException {
            if (shouldFailSaves) {
                throw new IOException("Injected save failure");
            }
            super.saveAddressBook(addressBook);
        }
    }
}
