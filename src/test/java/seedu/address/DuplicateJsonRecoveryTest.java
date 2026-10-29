package seedu.address;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.Assert.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.logic.LogicManager;
import seedu.address.logic.Messages;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.Model;
import seedu.address.model.UserPrefs;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.Storage;
import seedu.address.storage.StorageManager;

/** Exercises duplicate-key recovery through actual startup, command and storage paths. */
public class DuplicateJsonRecoveryTest {
    private static final String ADA = "{\"name\":\"Ada\",\"phone\":\"123\","
            + "\"email\":\"ada@example.com\",\"address\":\"Street\","
            + "\"followUp\":{\"dueDate\":\"2099-12-15\",\"description\":\"Call Ada\"}}";
    private static final String VALID = "{\"persons\":[" + ADA + "]}";

    @TempDir
    public Path testFolder;

    @Test
    public void duplicateRootProperty_blocksStartupAndRecoversAfterRepair() throws Exception {
        assertProtectedRecovery("{\"persons\":[" + ADA + "],\"persons\":[]}");
    }

    @Test
    public void duplicateContactProperty_blocksStartupAndRecoversAfterRepair() throws Exception {
        assertProtectedRecovery(VALID.replace("\"name\":\"Ada\"", "\"name\":\"Lost Ada\",\"name\":\"Ada\""));
    }

    @Test
    public void duplicateActionProperty_blocksStartupAndRecoversAfterRepair() throws Exception {
        assertProtectedRecovery(VALID.replace("\"description\":\"Call Ada\"", "\"description\":\"Lost action\","
                + "\"description\":\"Call Ada\""));
    }

    @Test
    public void duplicateFollowUpEndingInNull_blocksStartupAndRecoversAfterRepair() throws Exception {
        assertProtectedRecovery(VALID.replace("\"description\":\"Call Ada\"}",
                "\"description\":\"Call Ada\"},\"followUp\":null"));
    }

    private void assertProtectedRecovery(String invalidJson) throws Exception {
        Path file = testFolder.resolve("clients.json");
        Storage storage = new StorageManager(new JsonAddressBookStorage(file),
                new JsonUserPrefsStorage(testFolder.resolve("preferences.json")));
        Files.writeString(file, invalidJson);
        byte[] original = Files.readAllBytes(file);
        Model model = new MainApp().initModelManager(storage, new UserPrefs());
        assertTrue(model.isDataLoadingBlocked());
        assertTrue(model.getAddressBook().getPersonList().isEmpty());
        assertNull(model.selectedPersonProperty().get());
        LogicManager logic = new LogicManager(model, storage);
        logic.execute("list");
        logic.execute("find Ada");
        logic.execute("followups");
        assertThrows(CommandException.class, "No client with ID 1.", () ->
                logic.execute("view 1"));
        for (String command : new String[] {"add n/Bob p/456 e/bob@example.com a/Office",
            "delete 1", "followup 1 clear", "edit 1 n/Updated Client"}) {
            assertThrows(ParseException.class, Messages.MESSAGE_CHANGES_BLOCKED, () -> logic.execute(command));
        }
        assertTrue(model.isDataLoadingBlocked());
        assertArrayEquals(original, Files.readAllBytes(file));

        Files.writeString(file, VALID);
        Model restarted = new MainApp().initModelManager(storage, new UserPrefs());
        assertFalse(restarted.isDataLoadingBlocked());
        assertEquals("Ada", restarted.getAddressBook().getPersonList().get(0).getName().fullName);
        assertEquals("Call Ada", restarted.getAddressBook().getPersonList().get(0)
                .getFollowUp().orElseThrow().getDescription());
        new LogicManager(restarted, storage).execute("add n/Bob p/456 e/bob@example.com a/Office");
        Model saved = new MainApp().initModelManager(storage, new UserPrefs());
        assertFalse(saved.isDataLoadingBlocked());
        assertEquals(2, saved.getAddressBook().getPersonList().size());
    }
}
