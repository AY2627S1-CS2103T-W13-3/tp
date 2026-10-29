package seedu.address;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.commons.exceptions.DataLoadingException;
import seedu.address.logic.LogicManager;
import seedu.address.logic.Messages;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.UserPrefs;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;

/** Ensures saved text cannot be silently reconstructed from another JSON scalar type. */
public class SavedTextTypeRecoveryTest {
    private static final String VALID = "{\"nextClientId\":2,\"persons\":[{\"clientId\":1,\"name\":\"Rachel\","
            + "\"phone\":\"00123\",\"email\":\"rachel@example.com\",\"address\":\"Home\",\"tags\":[\"renewal\"],"
            + "\"followUp\":{\"dueDate\":\"2020-01-01\",\"description\":\"Call client\"}}]}";

    @TempDir
    public Path folder;

    @Test
    public void wrongTypes_protectWholeFileAndBlockWrites_thenRecoverAfterRepair() throws Exception {
        for (String field : List.of("name", "phone", "email", "address", "dueDate", "description")) {
            for (String value : List.of("true", "false", "123", "123.5", "[]", "{}", "null")) {
                String invalid = VALID.replaceAll("\"" + field + "\":\"[^\"]*\"", "\"" + field + "\":" + value);
                assertProtected(invalid);
            }
        }
        for (String value : List.of("true", "123", "{}", "[]", "null")) {
            assertProtected(VALID.replace("[\"renewal\"]", "[" + value + "]"));
        }
    }

    private void assertProtected(String invalid) throws Exception {
        Path file = folder.resolve("clients.json");
        Files.writeString(file, invalid);
        byte[] original = Files.readAllBytes(file);
        JsonAddressBookStorage clients = new JsonAddressBookStorage(file);
        assertThrows(DataLoadingException.class, clients::readAddressBook);
        StorageManager storage = new StorageManager(clients, new JsonUserPrefsStorage(folder.resolve("prefs.json")));
        var model = new MainApp().initModelManager(storage, new UserPrefs());
        assertTrue(model.isDataLoadingBlocked());
        assertTrue(model.getAddressBook().getPersonList().isEmpty());
        LogicManager logic = new LogicManager(model, storage);
        logic.execute("list");
        ParseException error = assertThrows(ParseException.class, () ->
                logic.execute("add n/Bob p/222 e/bob@example.com a/Home"));
        assertEquals(Messages.MESSAGE_CHANGES_BLOCKED, error.getMessage());
        assertArrayEquals(original, Files.readAllBytes(file));
        Files.writeString(file, VALID);
        var repaired = new MainApp().initModelManager(storage, new UserPrefs());
        assertFalse(repaired.isDataLoadingBlocked());
        assertEquals("00123", repaired.getAddressBook().getPersonList().getFirst().getPhone().value);
        new LogicManager(repaired, storage).execute("add n/Bob p/222 e/bob@example.com a/Home");
        assertEquals(repaired.getAddressBook(), clients.readAddressBook().orElseThrow());
    }
}
