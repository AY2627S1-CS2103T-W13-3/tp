package seedu.address;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.commons.core.GuiSettings;
import seedu.address.model.Model;
import seedu.address.model.ReadOnlyUserPrefs;
import seedu.address.model.UserPrefs;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;
import seedu.address.testutil.TypicalPersons;

/** Tests preference fallback independently of valid saved clients. */
public class PreferenceRecoveryTest {
    @TempDir
    public Path testFolder;

    private JsonAddressBookStorage clientsStorage;
    private byte[] originalClients;

    @BeforeEach
    public void setUp() throws IOException {
        clientsStorage = new JsonAddressBookStorage(testFolder.resolve("clients.json"));
        clientsStorage.saveAddressBook(TypicalPersons.getTypicalAddressBook());
        originalClients = Files.readAllBytes(clientsStorage.getAddressBookFilePath());
    }

    @Test
    public void nullGuiSettings_defaultsAllowModelStartupAndRepairPreferences() throws Exception {
        Path file = testFolder.resolve("preferences.json");
        Files.writeString(file, "{\"guiSettings\":null}");
        JsonUserPrefsStorage storage = new JsonUserPrefsStorage(file);
        UserPrefs preferences = new MainApp().initPrefs(storage);

        assertClientStartup(preferences, storage);
        assertEquals(new UserPrefs(), preferences);
        assertEquals(preferences, storage.readUserPrefs().orElseThrow());
        preferences.setGuiSettings(new GuiSettings(1100, 750, 30, 40));
        storage.saveUserPrefs(preferences);
        assertEquals(preferences, storage.readUserPrefs().orElseThrow());
    }

    @Test
    public void failedPreferenceRepair_defaultsStillAllowModelStartupAndRetry() throws Exception {
        Path file = testFolder.resolve("unwritable-preferences.json");
        String original = "{\"guiSettings\":null}";
        Files.writeString(file, original);
        FailingPreferenceStorage storage = new FailingPreferenceStorage(file);
        UserPrefs preferences = new MainApp().initPrefs(storage);

        assertEquals(1, storage.saveAttempts);
        assertClientStartup(preferences, storage);
        assertEquals(new UserPrefs(), preferences);
        assertEquals(original, Files.readString(file));
        storage.shouldFailSave = false;
        storage.saveUserPrefs(preferences);
        assertEquals(preferences, storage.readUserPrefs().orElseThrow());
    }

    @Test
    public void missingAndEmptyPreferences_defaultsRemainUsable() throws Exception {
        Path file = testFolder.resolve("defaults.json");
        JsonUserPrefsStorage storage = new JsonUserPrefsStorage(file);
        MainApp app = new MainApp();
        assertEquals(new UserPrefs(), app.initPrefs(storage));
        assertClientStartup(storage.readUserPrefs().orElseThrow(), storage);

        Files.writeString(file, "{}");
        UserPrefs preferences = app.initPrefs(storage);
        assertEquals(new UserPrefs(), preferences);
        assertClientStartup(preferences, storage);
    }

    @Test
    public void validCustomPreferences_surviveStartupAndRepairPass() throws Exception {
        JsonUserPrefsStorage storage = new JsonUserPrefsStorage(testFolder.resolve("custom.json"));
        UserPrefs expected = new UserPrefs();
        expected.setGuiSettings(new GuiSettings(1200, 800, 100, 200));
        storage.saveUserPrefs(expected);

        UserPrefs preferences = new MainApp().initPrefs(storage);
        assertClientStartup(preferences, storage);
        assertEquals(expected, preferences);
        assertEquals(expected, storage.readUserPrefs().orElseThrow());
    }

    @Test
    public void malformedOrNullRootPreferences_defaultsLeaveClientsUnchanged() throws Exception {
        JsonUserPrefsStorage storage = new JsonUserPrefsStorage(testFolder.resolve("invalid.json"));
        for (String invalid : new String[] {"null", "{invalid}", "{\"guiSettings\":[]}"}) {
            Files.writeString(storage.getUserPrefsFilePath(), invalid);
            UserPrefs preferences = new MainApp().initPrefs(storage);
            assertEquals(new UserPrefs(), preferences);
            assertClientStartup(preferences, storage);
            assertEquals(preferences, storage.readUserPrefs().orElseThrow());
        }
    }

    private void assertClientStartup(UserPrefs preferences, JsonUserPrefsStorage preferenceStorage) throws Exception {
        StorageManager storage = new StorageManager(clientsStorage, preferenceStorage);
        Model model = new MainApp().initModelManager(storage, preferences);
        assertFalse(model.isDataLoadingBlocked());
        assertNotNull(model.getGuiSettings());
        assertEquals(TypicalPersons.getTypicalAddressBook(), model.getAddressBook());
        assertArrayEquals(originalClients, Files.readAllBytes(clientsStorage.getAddressBookFilePath()));
    }

    private static class FailingPreferenceStorage extends JsonUserPrefsStorage {
        private boolean shouldFailSave = true;
        private int saveAttempts;

        FailingPreferenceStorage(Path file) {
            super(file);
        }

        @Override
        public void saveUserPrefs(ReadOnlyUserPrefs preferences) throws IOException {
            saveAttempts++;
            if (shouldFailSave) {
                throw new IOException("Synthetic preference write failure");
            }
            super.saveUserPrefs(preferences);
        }
    }
}
