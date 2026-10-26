package seedu.address;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.commons.exceptions.DataLoadingException;
import seedu.address.logic.LogicManager;
import seedu.address.logic.Messages;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.Model;
import seedu.address.model.UserPrefs;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.Storage;
import seedu.address.storage.StorageManager;

/** Exercises malformed saved bytes across the storage, startup, and command boundaries. */
public class SavedDataEncodingRecoveryTest {
    private static final String VALID_JSON = "{\"persons\":[{\"name\":\"Ada\",\"phone\":\"123\","
            + "\"email\":\"ada@example.com\",\"address\":\"新加坡 🏠 \uFFFD\"}]} \n\t";
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-26T12:00:00Z"), ZoneOffset.UTC);

    @TempDir
    public Path folder;

    @Test
    public void malformedSavedAddress_blocksMutationsUntilRepairedWithoutChangingBytes() throws Exception {
        Path file = folder.resolve("data/addressbook.json");
        Files.createDirectories(file.getParent());
        byte[] prefix = VALID_JSON.substring(0, VALID_JSON.indexOf("新加坡"))
                .getBytes(StandardCharsets.UTF_8);
        byte[] suffix = VALID_JSON.substring(VALID_JSON.indexOf("新加坡") + "新加坡".length())
                .getBytes(StandardCharsets.UTF_8);
        byte[] malformed = new byte[prefix.length + 1 + suffix.length];
        System.arraycopy(prefix, 0, malformed, 0, prefix.length);
        malformed[prefix.length] = (byte) 0xC3;
        System.arraycopy(suffix, 0, malformed, prefix.length + 1, suffix.length);
        Files.write(file, malformed);

        Storage storage = storageFor(file);
        assertThrows(DataLoadingException.class, storage::readAddressBook);
        Model protectedModel = new MainApp().initModelManager(storage, new UserPrefs());
        LogicManager protectedLogic = new LogicManager(protectedModel, storage, CLOCK);
        assertTrue(protectedModel.isDataLoadingBlocked());
        assertTrue(protectedLogic.getAllPersonList().isEmpty());
        assertEquals("Listed all clients.", protectedLogic.execute("list").getFeedbackToUser());
        ParseException blocked = assertThrows(ParseException.class, () ->
                protectedLogic.execute("add n/Bob p/456 e/bob@example.com a/Home"));
        assertEquals(Messages.MESSAGE_CHANGES_BLOCKED, blocked.getMessage());
        assertArrayEquals(malformed, Files.readAllBytes(file));

        Files.writeString(file, VALID_JSON, StandardCharsets.UTF_8);
        Model repairedModel = new MainApp().initModelManager(storage, new UserPrefs());
        LogicManager repairedLogic = new LogicManager(repairedModel, storage, CLOCK);
        assertFalse(repairedModel.isDataLoadingBlocked());
        assertEquals("新加坡 🏠 \uFFFD", repairedLogic.getAllPersonList().getFirst().getAddress().value);
        repairedLogic.execute("add n/Bob p/456 e/bob@example.com a/Home");
        assertEquals(2, storage.readAddressBook().orElseThrow().getPersonList().size());
    }

    @Test
    public void escapedUnicodeAndTrailingWhitespace_loadWithoutChangingSavedBytes() throws Exception {
        Path file = folder.resolve("data/addressbook.json");
        Files.createDirectories(file.getParent());
        String json = VALID_JSON.replace("新", "\\u65B0");
        byte[] original = json.getBytes(StandardCharsets.UTF_8);
        Files.write(file, original);

        Storage storage = storageFor(file);
        Model model = new MainApp().initModelManager(storage, new UserPrefs());
        LogicManager logic = new LogicManager(model, storage, CLOCK);
        assertFalse(model.isDataLoadingBlocked());
        assertEquals("新加坡 🏠 \uFFFD", logic.getAllPersonList().getFirst().getAddress().value);
        logic.execute("list");
        assertArrayEquals(original, Files.readAllBytes(file));
    }

    private Storage storageFor(Path file) {
        return new StorageManager(new JsonAddressBookStorage(file),
                new JsonUserPrefsStorage(folder.resolve("preferences.json")));
    }
}
