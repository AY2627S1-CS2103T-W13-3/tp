package seedu.address;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.Assert.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javafx.animation.Animation;
import javafx.animation.Timeline;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.ActionEvent;
import javafx.util.Duration;
import seedu.address.logic.LogicManager;
import seedu.address.logic.Messages;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.AddressBook;
import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.UserPrefs;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.Storage;
import seedu.address.storage.StorageManager;
import seedu.address.testutil.MutableClock;
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
    public void bomEncodedFiles_loadClientsAndPreferencesWithoutChangingClientBytes() throws Exception {
        Path file = testFolder.resolve("bom-clients.json");
        Storage storage = storage(file);
        storage.saveAddressBook(TypicalPersons.getTypicalAddressBook());
        String original = "\ufeff" + Files.readString(file);
        Files.writeString(file, original);
        Path preferencesFile = testFolder.resolve("bom-preferences.json");
        Files.writeString(preferencesFile, "\ufeff{\"guiSettings\":{\"windowWidth\":1234,\"windowHeight\":789}}");
        MainApp app = new MainApp();
        UserPrefs preferences = app.initPrefs(new JsonUserPrefsStorage(preferencesFile));
        assertEquals(1234, preferences.getGuiSettings().getWindowWidth());
        Model model = app.initModelManager(storage, preferences);
        assertFalse(model.isDataLoadingBlocked());
        assertEquals(TypicalPersons.getTypicalAddressBook(), model.getAddressBook());
        assertEquals(original, Files.readString(file));
        new LogicManager(model, storage).execute("add n/New Client p/123 e/new@example.com a/Street");
        Model restarted = app.initModelManager(storage, preferences);
        assertFalse(restarted.isDataLoadingBlocked());
        assertEquals(model.getAddressBook(), restarted.getAddressBook());
    }

    @Test
    public void trailingJsonContent_blocksChangesAndPreservesFile() throws Exception {
        Path file = testFolder.resolve("trailing.json");
        for (String trailing : new String[] {"x", "{\"persons\":[]}", "[1,2]", "{"}) {
            String original = "{\"persons\":[]}" + trailing;
            Files.writeString(file, original);
            Storage storage = storage(file);
            Model model = new MainApp().initModelManager(storage, new UserPrefs());
            assertTrue(model.isDataLoadingBlocked());
            LogicManager logic = new LogicManager(model, storage);
            assertThrows(ParseException.class, Messages.MESSAGE_CHANGES_BLOCKED, () ->
                    logic.execute("add n/Ada p/123 e/ada@example.com a/Street"));
            assertEquals(original, Files.readString(file));
        }
    }

    @Test
    public void longEmail_addAndRestart_preservesSavedClient() throws Exception {
        Path file = testFolder.resolve("long-email.json");
        Storage storage = storage(file);
        Model model = new MainApp().initModelManager(storage, new UserPrefs());
        String email = "a.".repeat(20000) + "a@example.com";
        new LogicManager(model, storage).execute("add n/Ada p/123 e/" + email + " a/Street");

        Model restarted = new MainApp().initModelManager(storage, new UserPrefs());
        assertFalse(restarted.isDataLoadingBlocked());
        assertEquals(model.getAddressBook(), restarted.getAddressBook());
        assertEquals(email, restarted.getAddressBook().getPersonList().get(0).getEmail().value);
    }

    @Test
    public void nullJsonClientFile_blocksChangesWithoutOverwritingIt() throws Exception {
        Path file = testFolder.resolve("null-client.json");
        Files.writeString(file, "null");
        Storage storage = storage(file);

        Model model = new MainApp().initModelManager(storage, new UserPrefs());
        assertTrue(model.isDataLoadingBlocked());
        assertTrue(model.getAddressBook().getPersonList().isEmpty());
        LogicManager logic = new LogicManager(model, storage);
        assertThrows(ParseException.class, Messages.MESSAGE_CHANGES_BLOCKED, () ->
                logic.execute("add n/Ada p/123 e/ada@example.com a/Street"));
        assertEquals("null", Files.readString(file));
    }

    @Test
    public void nullJsonPreferencesFile_usesDefaultsAndRepairsFile() throws Exception {
        Path file = testFolder.resolve("null-preferences.json");
        Files.writeString(file, "null");

        UserPrefs preferences = new MainApp().initPrefs(new JsonUserPrefsStorage(file));

        assertEquals(new UserPrefs(), preferences);
        assertFalse(Files.readString(file).equals("null"));
    }

    @Test
    public void invalidSavedClient_blocksStartupWithoutReplacingOtherRecords() throws Exception {
        Path file = testFolder.resolve("invalid-client.json");
        String original = "{\"persons\":[{\"name\":\"Ada\",\"phone\":\"12345\","
                + "\"email\":\"ada@example.com\",\"address\":\"First Street\"},"
                + "{\"name\":\"Bob\",\"phone\":\"1\",\"email\":\"bob@example.com\","
                + "\"address\":\"Second Street\"}]}";
        Files.writeString(file, original);
        Storage storage = storage(file);

        Model model = new MainApp().initModelManager(storage, new UserPrefs());
        assertTrue(model.isDataLoadingBlocked());
        assertTrue(model.getAddressBook().getPersonList().isEmpty());
        assertNull(model.selectedPersonProperty().get());
        LogicManager logic = new LogicManager(model, storage);
        assertThrows(ParseException.class, Messages.MESSAGE_CHANGES_BLOCKED, () -> logic.execute("delete 1"));
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

    @Test
    public void focusRefresh_updatesDateWithoutSaving() {
        Path file = testFolder.resolve("focus.json");
        Model model = new ModelManager(new AddressBook(), new UserPrefs(), LocalDate.of(2026, 10, 7), false);
        MutableClock clock = new MutableClock(Instant.parse("2026-10-07T00:00:00Z"), ZoneOffset.UTC);
        LogicManager logic = new LogicManager(model, storage(file), clock);
        SimpleBooleanProperty focused = new SimpleBooleanProperty(false);
        MainApp.refreshDateOnFocus(focused, logic);

        clock.setInstant(Instant.parse("2026-10-08T00:00:00Z"));
        assertEquals(LocalDate.of(2026, 10, 7), model.getToday());
        focused.set(true);
        assertEquals(LocalDate.of(2026, 10, 8), model.getToday());
        assertFalse(Files.exists(file));
    }

    @Test
    public void periodicRefresh_isScheduledAndUpdatesDateWithoutSaving() {
        Path file = testFolder.resolve("periodic.json");
        Model model = new ModelManager(new AddressBook(), new UserPrefs(), LocalDate.of(2026, 10, 7), false);
        MutableClock clock = new MutableClock(Instant.parse("2026-10-07T00:00:00Z"), ZoneOffset.UTC);
        LogicManager logic = new LogicManager(model, storage(file), clock);
        Timeline timeline = MainApp.createDateRefreshTimeline(logic);
        assertEquals(Animation.INDEFINITE, timeline.getCycleCount());
        assertEquals(Duration.minutes(1), timeline.getKeyFrames().get(0).getTime());

        clock.setInstant(Instant.parse("2026-10-08T00:00:00Z"));
        timeline.getKeyFrames().get(0).getOnFinished().handle(new ActionEvent());
        assertEquals(LocalDate.of(2026, 10, 8), model.getToday());
        assertFalse(Files.exists(file));
    }

    private Storage storage(Path file) {
        return new StorageManager(new JsonAddressBookStorage(file),
                new JsonUserPrefsStorage(testFolder.resolve("prefs.json")));
    }
}
