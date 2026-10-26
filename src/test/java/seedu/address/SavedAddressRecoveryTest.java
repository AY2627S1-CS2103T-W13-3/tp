package seedu.address;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.Assert.assertThrows;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.commons.exceptions.DataLoadingException;
import seedu.address.logic.LogicManager;
import seedu.address.logic.Messages;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.Model;
import seedu.address.model.UserPrefs;
import seedu.address.model.person.Address;
import seedu.address.model.person.FollowUp;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.Storage;
import seedu.address.storage.StorageManager;

public class SavedAddressRecoveryTest {
    @TempDir
    public Path testFolder;

    @TestFactory
    public Stream<DynamicTest> escapedUnpairedSurrogates_blockLoadingAndPreserveWholeFile() {
        return Stream.of("\\uD800", "\\uDC00", "\\uDC00\\uD800", "\\uD800A", "\\uD800\\uD800",
                "A\\uDC00B", "\\uD83C\\uDFE0\\uD800")
                .map(escapedAddress -> DynamicTest.dynamicTest(escapedAddress, () -> {
                    Path file = Files.createTempFile(testFolder, "invalid-address-", ".json");
                    byte[] original = savedClients(escapedAddress).getBytes(StandardCharsets.UTF_8);
                    Files.write(file, original);
                    Storage storage = storage(file);

                    assertThrows(DataLoadingException.class, () -> storage.readAddressBook());
                    Model model = new MainApp().initModelManager(storage, new UserPrefs());
                    assertTrue(model.isDataLoadingBlocked());
                    assertTrue(model.getAddressBook().getPersonList().isEmpty());
                    assertNull(model.selectedPersonProperty().get());
                    LogicManager logic = new LogicManager(model, storage);
                    logic.execute("list");
                    logic.execute("find Alice");
                    logic.execute("followups");
                    assertThrows(CommandException.class, Messages.MESSAGE_INVALID_PERSON_DISPLAYED_INDEX, () ->
                            logic.execute("view 1"));
                    for (String command : new String[] {"add n/Cara p/333 e/cara@example.com a/Blk 3",
                        "delete 1", "followup 1 clear"}) {
                        assertThrows(ParseException.class, Messages.MESSAGE_CHANGES_BLOCKED, () ->
                                logic.execute(command));
                    }
                    assertTrue(logic.isDataLoadingBlocked());
                    assertTrue(model.getAddressBook().getPersonList().isEmpty());
                    assertArrayEquals(original, Files.readAllBytes(file));
                }));
    }

    @Test
    public void repairedSavedAddress_requiresRestartThenAllowsSaving() throws Exception {
        Path file = testFolder.resolve("repaired-address.json");
        Files.writeString(file, savedClients("\\uD800"));
        Storage storage = storage(file);
        MainApp app = new MainApp();
        Model blocked = app.initModelManager(storage, new UserPrefs());
        assertTrue(blocked.isDataLoadingBlocked());

        Files.writeString(file, savedClients("新加坡 🏠"));
        LogicManager blockedLogic = new LogicManager(blocked, storage);
        blockedLogic.execute("list");
        assertTrue(blockedLogic.isDataLoadingBlocked());
        assertThrows(ParseException.class, Messages.MESSAGE_CHANGES_BLOCKED, () ->
                blockedLogic.execute("add n/Cara p/333 e/cara@example.com a/Blk 3"));

        Model repaired = app.initModelManager(storage, new UserPrefs());
        assertFalse(repaired.isDataLoadingBlocked());
        assertEquals(2, repaired.getAddressBook().getPersonList().size());
        assertEquals("新加坡 🏠", repaired.getAddressBook().getPersonList().get(1).getAddress().value);
        LogicManager logic = new LogicManager(repaired, storage);
        logic.execute("add n/Cara p/333 e/cara@example.com a/Blk 3");
        logic.execute("followup 3 d/2099-12-15 m/Call client");

        Model restarted = app.initModelManager(storage, new UserPrefs());
        assertFalse(restarted.isDataLoadingBlocked());
        assertEquals(repaired.getAddressBook(), restarted.getAddressBook());
        assertEquals(new FollowUp(LocalDate.of(2099, 12, 15), "Call client"),
                restarted.getAddressBook().getPersonList().get(2).getFollowUp().orElseThrow());
    }

    @TestFactory
    public Stream<DynamicTest> validUnicode_addSaveAndRestart_preservesAddress() {
        return Stream.of("新加坡 🏠 #02-01", "Blk \uFFFD", "\uD800\uDC00", "\uDBFF\uDFFF")
                .map(address -> DynamicTest.dynamicTest(address, () -> {
                    Path file = Files.createTempDirectory(testFolder, "unicode-").resolve("clients.json");
                    Storage storage = storage(file);
                    Model model = new MainApp().initModelManager(storage, new UserPrefs());
                    new LogicManager(model, storage).execute("add n/Alice p/111 e/alice@example.com a/" + address);

                    Model restarted = new MainApp().initModelManager(storage, new UserPrefs());
                    assertFalse(restarted.isDataLoadingBlocked());
                    assertEquals(1, restarted.getAddressBook().getPersonList().size());
                    assertEquals(address, restarted.getAddressBook().getPersonList().get(0).getAddress().value);
                    assertEquals(model.getAddressBook(), restarted.getAddressBook());
                }));
    }

    @Test
    public void add_unpairedSurrogate_rejectsWithoutChangingSavedDataOrSelection() throws Exception {
        Path file = testFolder.resolve("command-address.json");
        Files.writeString(file, savedClients("Second Street"));
        byte[] original = Files.readAllBytes(file);
        Storage storage = storage(file);
        Model model = new MainApp().initModelManager(storage, new UserPrefs());
        LogicManager logic = new LogicManager(model, storage);
        logic.execute("find Alice");
        logic.execute("view 1");

        assertThrows(ParseException.class, Address.MESSAGE_CONSTRAINTS, () ->
                logic.execute("add n/Cara p/333 e/cara@example.com a/\uD800"));

        assertEquals(2, model.getAddressBook().getPersonList().size());
        assertEquals(1, model.getFilteredPersonList().size());
        assertEquals("Alice", model.selectedPersonProperty().get().getName().fullName);
        assertArrayEquals(original, Files.readAllBytes(file));
    }

    private String savedClients(String secondAddressJson) {
        return "{\"persons\":[{\"name\":\"Alice\",\"phone\":\"111\",\"email\":\"alice@example.com\","
                + "\"address\":\"First Street\",\"tags\":[]},{\"name\":\"Bob\",\"phone\":\"222\","
                + "\"email\":\"bob@example.com\",\"address\":\"" + secondAddressJson + "\",\"tags\":[]}]}";
    }

    private Storage storage(Path file) {
        return new StorageManager(new JsonAddressBookStorage(file),
                new JsonUserPrefsStorage(testFolder.resolve("prefs.json")));
    }
}
