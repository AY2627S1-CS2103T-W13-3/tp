package seedu.address;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.logic.LogicManager;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.AddressBook;
import seedu.address.model.Model;
import seedu.address.model.UserPrefs;
import seedu.address.model.person.Person;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.Storage;
import seedu.address.storage.StorageManager;

/** Tests a real, recoverable filesystem failure rather than an injected storage exception. */
public class StorageDirectoryRecoveryTest {
    private static final String ADD_BOB = "add n/Bob p/456 e/bob@example.com a/Home";
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-26T12:00:00Z"), ZoneOffset.UTC);

    @TempDir
    public Path folder;

    @Test
    public void dataDirectoryReplacedByFile_failedSaveRetainsStateAndRetrySucceeds() throws Exception {
        Path dataDirectory = folder.resolve("data");
        Files.createDirectories(dataDirectory);
        Path file = dataDirectory.resolve("addressbook.json");
        Files.writeString(file, "{\"persons\":[{\"name\":\"Ada\",\"phone\":\"123\","
                + "\"email\":\"ada@example.com\",\"address\":\"Street\"}]}", StandardCharsets.UTF_8);
        byte[] original = Files.readAllBytes(file);
        Storage storage = new StorageManager(new JsonAddressBookStorage(file),
                new JsonUserPrefsStorage(folder.resolve("preferences.json")));
        Model model = new MainApp().initModelManager(storage, new UserPrefs());
        LogicManager logic = new LogicManager(model, storage, CLOCK);
        logic.execute("view 1");
        AddressBook before = new AddressBook(model.getAddressBook());
        List<Person> displayedBefore = List.copyOf(model.getFilteredPersonList());
        Person selectedBefore = model.selectedPersonProperty().get();

        Path backupDirectory = folder.resolve("saved-data");
        Files.move(dataDirectory, backupDirectory);
        Files.writeString(dataDirectory, "A file is blocking the data directory.");
        try {
            assertEquals(LogicManager.MESSAGE_SAVE_FAILED,
                    assertThrows(CommandException.class, () -> logic.execute(ADD_BOB)).getMessage());
            assertEquals(before, model.getAddressBook());
            assertEquals(displayedBefore, model.getFilteredPersonList());
            assertSame(selectedBefore, model.selectedPersonProperty().get());
            assertArrayEquals(original, Files.readAllBytes(backupDirectory.resolve("addressbook.json")));
        } finally {
            Files.delete(dataDirectory);
            Files.move(backupDirectory, dataDirectory);
        }

        logic.execute(ADD_BOB);
        assertEquals(2, storage.readAddressBook().orElseThrow().getPersonList().size());
        assertEquals(2, model.getFilteredPersonList().size());
        assertSame(selectedBefore, model.selectedPersonProperty().get());
    }
}
