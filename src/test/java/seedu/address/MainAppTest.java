package seedu.address;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.Assert.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.logic.LogicManager;
import seedu.address.logic.Messages;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.AddressBook;
import seedu.address.model.Model;
import seedu.address.model.UserPrefs;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.Storage;
import seedu.address.storage.StorageManager;
import seedu.address.testutil.TypicalPersons;

public class MainAppTest {
    @TempDir
    public Path testFolder;

    @Test
    public void missingFile_startsEmptyAndWritable() throws Exception {
        Storage storage = storage(testFolder.resolve("missing.json"));
        Model model = new MainApp().initModelManager(storage, new UserPrefs());
        assertTrue(model.getAddressBook().getPersonList().isEmpty());
        assertFalse(model.isDataLoadingBlocked());
        new LogicManager(model, storage).execute("list");
        assertFalse(Files.exists(storage.getAddressBookFilePath()));
    }

    @Test
    public void invalidFile_blocksChangesWithoutOverwritingIt() throws Exception {
        Path file = testFolder.resolve("invalid.json");
        String original = "{not valid JSON}";
        Files.writeString(file, original);
        Storage storage = storage(file);
        Model model = new MainApp().initModelManager(storage, new UserPrefs());
        assertTrue(model.getAddressBook().getPersonList().isEmpty());
        assertTrue(model.isDataLoadingBlocked());
        LogicManager logic = new LogicManager(model, storage);
        logic.execute("list");
        logic.execute("followups");
        assertTrue(logic.isDataLoadingBlocked());
        assertThrows(ParseException.class, Messages.MESSAGE_CHANGES_BLOCKED, () -> logic.execute("followup 1 clear"));
        assertEquals(original, Files.readString(file));
    }

    @Test
    public void repairedFile_restartsWithLoadedDataAndNoBlock() throws Exception {
        Path file = testFolder.resolve("repaired.json");
        Files.writeString(file, "bad data");
        Storage storage = storage(file);
        MainApp app = new MainApp();
        assertTrue(app.initModelManager(storage, new UserPrefs()).isDataLoadingBlocked());
        AddressBook book = TypicalPersons.getTypicalAddressBook();
        new JsonAddressBookStorage(file).saveAddressBook(book);
        Model repaired = app.initModelManager(storage, new UserPrefs());
        assertFalse(repaired.isDataLoadingBlocked());
        assertEquals(book, repaired.getAddressBook());
    }

    private Storage storage(Path file) {
        return new StorageManager(new JsonAddressBookStorage(file),
                new JsonUserPrefsStorage(testFolder.resolve("prefs.json")));
    }
}
