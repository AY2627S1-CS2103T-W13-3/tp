package seedu.address;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.commons.core.LogsCenter;
import seedu.address.logic.LogicManager;
import seedu.address.logic.Messages;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.Model;
import seedu.address.model.UserPrefs;
import seedu.address.model.person.Person;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.Storage;
import seedu.address.storage.StorageManager;

/** Verifies private diagnostics across real preference and client startup recovery. */
public class LoadingDiagnosticPrivacyTest {
    private static final List<String> PRIVATE_VALUES = List.of("Private Canary", "87654321",
            "private7284@example.com", "7284 Canary Street");
    private static final String CLIENT_JSON = "{\"name\":\"Private Canary\",\"phone\":\"87654321\","
            + "\"email\":\"private7284@example.com\",\"address\":\"7284 Canary Street\"}";

    @TempDir
    public Path testFolder;

    private Logger baseLogger;
    private CapturingHandler handler;

    @BeforeEach
    public void setUp() {
        // Capture the shared parent so downstream loggers cannot bypass the privacy assertions.
        baseLogger = LogsCenter.getLogger(LoadingDiagnosticPrivacyTest.class).getParent();
        handler = new CapturingHandler();
        baseLogger.addHandler(handler);
    }

    @AfterEach
    public void tearDown() {
        baseLogger.removeHandler(handler);
        handler.close();
    }

    @Test
    public void malformedClientJson_startupAndCommands_preserveFileWithoutLoggingClientContents() throws Exception {
        assertProtectedStartup("{\"persons\":[" + CLIENT_JSON + ",PRIVATE_INVALID_TOKEN]}");
    }

    @Test
    public void wrongClientType_startupAndCommands_preserveFileWithoutPublishingPartialData() throws Exception {
        assertProtectedStartup("{\"persons\":[" + CLIENT_JSON + ","
                + "{\"name\":{\"value\":\"Private Canary\"}}]}");
    }

    @Test
    public void invalidPreferences_startup_recoversWithoutLoggingContentsOrChangingValidClients() throws Exception {
        Path clients = testFolder.resolve("clients.json");
        Files.writeString(clients, "{\"persons\":[" + CLIENT_JSON + "]}");
        byte[] originalClients = Files.readAllBytes(clients);
        Path preferencesFile = testFolder.resolve("preferences.json");
        Files.writeString(preferencesFile, "{\"guiSettings\":{\"windowWidth\":\"Private Canary\"}}");
        JsonUserPrefsStorage preferencesStorage = new JsonUserPrefsStorage(preferencesFile);
        Storage storage = new StorageManager(new JsonAddressBookStorage(clients), preferencesStorage);
        MainApp app = new MainApp();

        UserPrefs preferences = app.initPrefs(preferencesStorage);
        Model model = app.initModelManager(storage, preferences);

        assertEquals(new UserPrefs(), preferences);
        assertEquals(preferences, preferencesStorage.readUserPrefs().orElseThrow());
        assertFalse(model.isDataLoadingBlocked());
        assertEquals(1, model.getAddressBook().getPersonList().size());
        Person loaded = model.getAddressBook().getPersonList().get(0);
        assertEquals(PRIVATE_VALUES.get(0), loaded.getName().fullName);
        assertEquals(PRIVATE_VALUES.get(1), loaded.getPhone().value);
        assertEquals(PRIVATE_VALUES.get(2), loaded.getEmail().value);
        assertEquals(PRIVATE_VALUES.get(3), loaded.getAddress().value);
        new LogicManager(model, storage).execute("find Private");
        assertEquals(List.of(loaded), model.getFilteredPersonList());
        assertArrayEquals(originalClients, Files.readAllBytes(clients));
        assertWarning("JsonUtil", preferencesFile);
        assertWarning("MainApp", preferencesFile);
        assertPrivateDiagnostics();
    }

    private void assertProtectedStartup(String json) throws Exception {
        Path clients = testFolder.resolve("clients.json");
        Files.writeString(clients, json);
        byte[] original = Files.readAllBytes(clients);
        Storage storage = new StorageManager(new JsonAddressBookStorage(clients),
                new JsonUserPrefsStorage(testFolder.resolve("preferences.json")));
        Model model = new MainApp().initModelManager(storage, new UserPrefs());
        LogicManager logic = new LogicManager(model, storage);

        assertTrue(model.isDataLoadingBlocked());
        assertTrue(model.getAddressBook().getPersonList().isEmpty());
        assertNull(model.selectedPersonProperty().get());
        logic.execute("list");
        logic.execute("find Private");
        logic.execute("followups");
        for (String command : List.of("add n/New Client p/123 e/new@example.com a/Street",
                "delete 1", "followup 1 clear")) {
            ParseException failure = assertThrows(ParseException.class, () -> logic.execute(command));
            assertEquals(Messages.MESSAGE_CHANGES_BLOCKED, failure.getMessage());
            assertArrayEquals(original, Files.readAllBytes(clients));
            assertTrue(model.getAddressBook().getPersonList().isEmpty());
        }
        assertTrue(logic.isDataLoadingBlocked());
        assertTrue(logic.getAllPersonList().isEmpty());
        assertTrue(logic.getFilteredPersonList().isEmpty());
        assertNull(logic.selectedPersonProperty().get());
        assertWarning("JsonUtil", clients);
        assertWarning("MainApp", clients);
        assertPrivateDiagnostics();
    }

    private void assertWarning(String loggerName, Path file) {
        assertTrue(handler.records.stream().anyMatch(record -> record.getLoggerName().endsWith("." + loggerName)
                && record.getLevel().equals(Level.WARNING) && record.getMessage().contains(file.toString())),
                "Expected a useful path warning from " + loggerName);
    }

    private void assertPrivateDiagnostics() {
        for (LogRecord record : handler.records) {
            assertNull(record.getThrown(), "Startup diagnostics must not attach untrusted exception details");
            for (String privateValue : PRIVATE_VALUES) {
                assertFalse(record.getMessage().contains(privateValue), "Log message exposes client contents");
                assertFalse(new SimpleFormatter().format(record).contains(privateValue),
                        "Formatted startup diagnostic exposes client contents");
            }
        }
    }

    private static class CapturingHandler extends Handler {
        private final List<LogRecord> records = new ArrayList<>();

        @Override
        public void publish(LogRecord record) {
            records.add(record);
        }

        @Override
        public void flush() {
        }

        @Override
        public void close() {
        }
    }
}
