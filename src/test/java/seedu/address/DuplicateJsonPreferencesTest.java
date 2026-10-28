package seedu.address;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.commons.core.GuiSettings;
import seedu.address.model.UserPrefs;
import seedu.address.storage.JsonUserPrefsStorage;

/** Retains the legacy last-value preference behavior without weakening client-data validation. */
public class DuplicateJsonPreferencesTest {
    @TempDir
    public Path testFolder;

    @Test
    public void duplicatePreferences_startupRetainsLastValuesAndSavesThem() throws Exception {
        String settings = "{\"windowWidth\":1000,\"windowHeight\":720,"
                + "\"windowCoordinates\":{\"x\":25,\"y\":35}}";
        String[] preferenceFiles = {
            "{\"guiSettings\":{\"windowWidth\":800},\"guiSettings\":" + settings + "}",
            "{\"guiSettings\":" + settings.replace("1000", "800,\"windowWidth\":1000") + "}"
        };
        Path file = testFolder.resolve("preferences.json");
        JsonUserPrefsStorage storage = new JsonUserPrefsStorage(file);
        GuiSettings expected = new GuiSettings(1000, 720, 25, 35);
        for (String json : preferenceFiles) {
            Files.writeString(file, json);
            UserPrefs preferences = new MainApp().initPrefs(storage);
            assertEquals(expected, preferences.getGuiSettings());
            assertEquals(expected, storage.readUserPrefs().orElseThrow().getGuiSettings());
            assertEquals(expected, new MainApp().initPrefs(storage).getGuiSettings());
        }
    }
}
