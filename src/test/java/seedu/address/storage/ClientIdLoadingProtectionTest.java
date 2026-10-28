package seedu.address.storage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.MainApp;
import seedu.address.commons.core.LogsCenter;
import seedu.address.commons.exceptions.DataLoadingException;
import seedu.address.logic.LogicManager;
import seedu.address.logic.Messages;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.Model;
import seedu.address.model.ReadOnlyUserPrefs;
import seedu.address.model.UserPrefs;

/** Exercises the actual protected-startup boundary for corrupt identity metadata. */
public class ClientIdLoadingProtectionTest {
    private static final String CLIENT = "{\"clientId\":17,\"name\":\"Rachel\",\"phone\":\"222\","
            + "\"email\":\"rachel@example.com\",\"address\":\"Home\"}";

    @TempDir
    public Path folder;

    @Test
    public void invalidMetadata_rejectsWholeFileWithoutCoercionOrRenumbering() throws Exception {
        for (String invalid : List.of("0", "-1", "2147483648", "9223372036854775808", "17.0", "17.5",
                "\"17\"", "true", "[]", "{}", "null")) {
            assertProtected("{\"nextClientId\":18,\"persons\":[" + CLIENT.replace("17", invalid) + "]}");
        }
        for (String invalid : List.of("0", "-1", "17", "2147483649", "9223372036854775808", "18.0", "18.5",
                "\"18\"", "false", "[]", "{}", "null")) {
            assertProtected("{\"nextClientId\":" + invalid + ",\"persons\":[" + CLIENT + "]}");
        }
        assertProtected("{\"persons\":[" + CLIENT + "]}");
        assertProtected("{\"nextClientId\":18,\"persons\":[" + CLIENT.replace("\"clientId\":17,", "") + "]}");
        assertProtected("{\"nextClientId\":18,\"persons\":[" + CLIENT + ","
                + CLIENT.replace("Rachel", "Bob").replace("rachel@", "bob@") + "]}");
        assertProtected("{\"nextClientId\":18,\"persons\":[" + CLIENT + ","
                + CLIENT.replace("\"clientId\":17,", "").replace("Rachel", "Bob") + "]}");
        assertProtected("{\"nextClientId\":18,\"persons\":[" + CLIENT.replace("clientId", "clientID") + "]}");
        assertProtected("{\"nextClientId\":18,\"persons\":[" + CLIENT + "],\"unknown\":true}");
        assertProtected("{\"nextClientId\":18,\"nextClientId\":19,\"persons\":[" + CLIENT + "]}");
    }

    @Test
    public void partiallyNullifiedHistory_neverDowngradesAndPreservesOriginalBytes() throws Exception {
        assertProtected("{\"nextClientId\":null,\"persons\":[" + CLIENT + "]}");
        assertProtected("{\"nextClientId\":18,\"persons\":["
                + CLIENT.replace("\"clientId\":17", "\"clientId\":null") + "]}");
        String other = CLIENT.replace("17", "null").replace("Rachel", "Bob").replace("rachel@", "bob@");
        assertProtected("{\"nextClientId\":null,\"persons\":[" + CLIENT + "," + other + "]}");
    }

    @Test
    public void invalidIdentity_diagnosticsExcludeRawMetadataAndContactValues() throws Exception {
        List<String> records = new ArrayList<>();
        Handler capture = new Handler() {
            @Override
            public void publish(LogRecord record) {
                records.add(new SimpleFormatter().format(record));
            }

            @Override
            public void flush() { }

            @Override
            public void close() { }
        };
        var logger = LogsCenter.getLogger(ClientIdLoadingProtectionTest.class).getParent();
        logger.addHandler(capture);
        try {
            assertProtected("{\"nextClientId\":18,\"persons\":["
                    + CLIENT.replace("17", "\"PRIVATE_ID_CANARY\"") + "]}");
            assertTrue(records.stream().noneMatch(record -> record.contains("PRIVATE_ID_CANARY")
                    || record.contains("rachel@example.com")));
        } finally {
            logger.removeHandler(capture);
        }
    }

    private void assertProtected(String json) throws Exception {
        Path file = folder.resolve("clients.json");
        Files.writeString(file, json);
        byte[] original = Files.readAllBytes(file);
        JsonAddressBookStorage clients = new JsonAddressBookStorage(file);
        assertThrows(DataLoadingException.class, clients::readAddressBook);
        Storage storage = new StorageManager(clients, new JsonUserPrefsStorage(folder.resolve("prefs.json")));
        // MainApp's package-private startup seam is outside this exclusively owned test package.
        var startup = MainApp.class.getDeclaredMethod("initModelManager", Storage.class, ReadOnlyUserPrefs.class);
        startup.setAccessible(true);
        Model model = (Model) startup.invoke(new MainApp(), storage, new UserPrefs());
        assertTrue(model.isDataLoadingBlocked());
        assertTrue(model.getAddressBook().getPersonList().isEmpty());
        LogicManager logic = new LogicManager(model, storage);
        for (String command : List.of("list", "find Rachel", "followups")) {
            logic.execute(command);
        }
        ParseException error = assertThrows(ParseException.class, () ->
                logic.execute("add n/New p/333 e/new@example.com a/Home"));
        assertEquals(Messages.MESSAGE_CHANGES_BLOCKED, error.getMessage());
        assertArrayEquals(original, Files.readAllBytes(file));
    }
}
