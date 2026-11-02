package seedu.address;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;

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
import seedu.address.model.person.Person;
import seedu.address.model.tag.Tag;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;

/** Checks segmented tags through real commands, publication and JSON persistence. */
class TagSeparatorWorkflowTest {
    @TempDir
    private Path testFolder;

    @Test
    void addEditSearchAndReload_normalizesTagsAndRetainsOldAlphanumericTags() throws Exception {
        JsonAddressBookStorage saved = new JsonAddressBookStorage(testFolder.resolve("clients.json"));
        ModelManager model = new ModelManager(new AddressBook(), new UserPrefs(), LocalDate.of(2026, 11, 2), false);
        LogicManager logic = new LogicManager(model, storage(saved));
        logic.execute("add n/Sean Lim p/91234567 e/sean@example.com a/Blk 123 t/LIFE-INSURANCE t/health2");
        Person added = model.getAddressBook().getPersonById(new ClientId(1)).orElseThrow();
        assertEquals(Set.of(new Tag("life-insurance"), new Tag("health2")), added.getTags());
        assertEquals("1 client(s) listed!", logic.execute("find t/life-insurance").getFeedbackToUser());
        assertEquals(List.of(added), model.getFilteredPersonList());
        logic.execute("edit 1 t/HIGH_PRIORITY t/health2");
        Person edited = model.getAddressBook().getPersonById(new ClientId(1)).orElseThrow();
        assertEquals(Set.of(new Tag("high_priority"), new Tag("health2")), edited.getTags());
        assertEquals(2, model.getAddressBook().getNextClientId());

        ModelManager reloaded = new ModelManager(saved.readAddressBook().orElseThrow(), new UserPrefs());
        LogicManager restarted = new LogicManager(reloaded, storage(saved));
        assertEquals(model.getAddressBook(), reloaded.getAddressBook());
        for (String query : List.of("HIGH_PRIORITY", "health2")) {
            assertEquals("1 client(s) listed!", restarted.execute("find t/" + query).getFeedbackToUser());
            assertEquals(List.of(edited), reloaded.getFilteredPersonList());
        }
        assertEquals("0 client(s) listed!", restarted.execute("find t/life-insurance").getFeedbackToUser());
    }

    @Test
    void invalidTagsAndFailedSave_preserveDataSelectionAndIdAllocation() throws Exception {
        JsonAddressBookStorage saved = new JsonAddressBookStorage(testFolder.resolve("clients.json"));
        ModelManager model = new ModelManager(new AddressBook(), new UserPrefs());
        LogicManager logic = new LogicManager(model, storage(saved));
        logic.execute("add n/Sean Lim p/91234567 e/sean@example.com a/Blk 123 t/health2");
        Person original = model.getAddressBook().getPersonById(new ClientId(1)).orElseThrow();
        model.selectPerson(original);
        byte[] bytes = Files.readAllBytes(saved.getAddressBookFilePath());
        ReadOnlyAddressBook originalBook = saved.readAddressBook().orElseThrow();
        for (String command : List.of(
                "add n/Bob Tan p/81234567 e/bob@example.com a/Blk 456 t/life--insurance",
                "edit 1 t/high__priority", "find t/-life")) {
            assertEquals(Tag.MESSAGE_CONSTRAINTS,
                    assertThrows(ParseException.class, () -> logic.execute(command)).getMessage());
            assertUnchanged(model, originalBook, original, bytes, saved.getAddressBookFilePath());
        }
        StorageManager failing = new StorageManager(saved,
                new JsonUserPrefsStorage(testFolder.resolve("prefs.json"))) {
            @Override
            public void saveAddressBook(ReadOnlyAddressBook addressBook) throws IOException {
                throw new IOException("Injected save failure");
            }
        };
        LogicManager failedSaveLogic = new LogicManager(model, failing);
        assertThrows(CommandException.class, () -> failedSaveLogic.execute("edit 1 t/high_priority"));
        assertUnchanged(model, originalBook, original, bytes, saved.getAddressBookFilePath());
        assertThrows(CommandException.class, () -> failedSaveLogic.execute(
                "add n/Bob Tan p/81234567 e/bob@example.com a/Blk 456 t/life-insurance"));
        assertUnchanged(model, originalBook, original, bytes, saved.getAddressBookFilePath());
    }

    private StorageManager storage(JsonAddressBookStorage saved) {
        return new StorageManager(saved, new JsonUserPrefsStorage(testFolder.resolve("prefs.json")));
    }

    private void assertUnchanged(ModelManager model, ReadOnlyAddressBook originalBook, Person original,
            byte[] bytes, Path file) throws IOException {
        assertEquals(originalBook, model.getAddressBook());
        assertEquals(List.of(original), model.getFilteredPersonList());
        assertSame(original, model.selectedPersonProperty().get());
        assertFalse(model.hasUnsavedChanges());
        assertArrayEquals(bytes, Files.readAllBytes(file));
    }
}
