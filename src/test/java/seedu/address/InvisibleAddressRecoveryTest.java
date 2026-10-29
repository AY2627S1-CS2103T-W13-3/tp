package seedu.address;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.Assert.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.commons.exceptions.DataLoadingException;
import seedu.address.logic.LogicManager;
import seedu.address.logic.Messages;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.AddressBook;
import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.UserPrefs;
import seedu.address.model.person.Address;
import seedu.address.model.person.FollowUp;
import seedu.address.model.person.Person;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.Storage;
import seedu.address.storage.StorageManager;
import seedu.address.testutil.PersonBuilder;

/** Checks that shared address validation protects both commands and saved client data. */
public class InvisibleAddressRecoveryTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 29);
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-29T00:00:00Z"), ZoneOffset.UTC);

    @TempDir
    public Path testFolder;

    @TestFactory
    public Stream<DynamicTest> invisibleAddress_addAndEdit_preserveDataIdsAndViewedClient() {
        return Stream.of("\u115f", "\u1160", "\u3164", "\uffa0", "\u2800", "\u0301", "\u093e", "\u20dd",
                " \u115f\u1160\u3164\uffa0\u200b\u0301 ").map(blank ->
                DynamicTest.dynamicTest("Invisible U+" + Integer.toHexString(blank.codePointAt(0)), () -> {
                    Person person = new PersonBuilder().withName("Alice")
                            .withFollowUp(new FollowUp(TODAY, "Call client")).build();
                    AddressBook book = new AddressBook();
                    book.addPerson(person);
                    Model model = new ModelManager(book, new UserPrefs(), TODAY, false);
                    model.showPendingFollowUps();
                    model.selectPerson(person);
                    AddressBook original = new AddressBook(model.getAddressBook());
                    Path file = Files.createTempDirectory(testFolder, "command-").resolve("clients.json");
                    Storage storage = storage(file);
                    storage.saveAddressBook(model.getAddressBook());
                    byte[] originalBytes = Files.readAllBytes(file);
                    LogicManager logic = new LogicManager(model, storage, CLOCK);

                    for (String command : List.of("add n/Bob p/222 e/bob@example.com a/" + blank,
                            "edit 1 a/" + blank)) {
                        assertThrows(ParseException.class, Address.MESSAGE_CONSTRAINTS, () -> logic.execute(command));
                        assertEquals(original, model.getAddressBook());
                        assertEquals(2, model.getAddressBook().getNextClientId());
                        assertSame(person, model.selectedPersonProperty().get());
                        assertEquals(List.of(person), model.getFilteredPersonList());
                        assertTrue(model.showingFollowUpsProperty().get());
                        assertFalse(model.hasUnsavedChanges());
                        assertArrayEquals(originalBytes, Files.readAllBytes(file));
                    }
                }));
    }

    @TestFactory
    public Stream<DynamicTest> escapedInvisibleAddress_blocksWholeFileAndPreservesBytes() {
        return Stream.of("\\u115f", "\\u1160", "\\u3164", "\\uffa0", "\\u2800", "\\u0301", "\\u093e", "\\u20dd",
                " \\u115f\\u1160\\u3164\\uffa0\\u200b\\u0301 ").map(escaped ->
                DynamicTest.dynamicTest(escaped, () -> {
                    Path file = Files.createTempFile(testFolder, "saved-address-", ".json");
                    Files.writeString(file, "{\"persons\":[{\"name\":\"Alice\",\"phone\":\"111\","
                            + "\"email\":\"alice@example.com\",\"address\":\"Blk 1\",\"tags\":[]},"
                            + "{\"name\":\"Bob\",\"phone\":\"222\","
                            + "\"email\":\"bob@example.com\",\"address\":\"" + escaped + "\",\"tags\":[]}]}");
                    byte[] original = Files.readAllBytes(file);
                    Storage storage = storage(file);
                    assertThrows(DataLoadingException.class, () -> storage.readAddressBook());

                    Model model = new MainApp().initModelManager(storage, new UserPrefs());
                    assertTrue(model.isDataLoadingBlocked());
                    assertTrue(model.getAddressBook().getPersonList().isEmpty());
                    assertNull(model.selectedPersonProperty().get());
                    LogicManager logic = new LogicManager(model, storage, CLOCK);
                    logic.execute("list");
                    assertThrows(ParseException.class, Messages.MESSAGE_CHANGES_BLOCKED, () ->
                            logic.execute("add n/Cara p/333 e/cara@example.com a/Blk 3"));
                    assertArrayEquals(original, Files.readAllBytes(file));
                }));
    }

    @Test
    public void visibleMultilingualAddress_addEditAndRestart_preservesTextAndIdentity() throws Exception {
        for (String visible : List.of("Blk \u3164 123", "\u115f\u1161", "新加坡 🏠 #02-01", "Cafe\u0301", "🏠", "-")) {
            Path file = Files.createTempDirectory(testFolder, "visible-").resolve("clients.json");
            Storage storage = storage(file);
            Model model = new ModelManager(new AddressBook(), new UserPrefs(), TODAY, false);
            LogicManager logic = new LogicManager(model, storage, CLOCK);
            logic.execute("add n/Alice p/111 e/alice@example.com a/" + visible);
            assertEquals(visible, model.getAddressBook().getPersonList().getFirst().getAddress().value);
            logic.execute("edit 1 a/" + visible + " #02");

            Model restarted = new MainApp().initModelManager(storage, new UserPrefs());
            assertFalse(restarted.isDataLoadingBlocked());
            assertEquals(model.getAddressBook(), restarted.getAddressBook());
            assertEquals(visible + " #02", restarted.getAddressBook().getPersonList().getFirst().getAddress().value);
            assertEquals(2, restarted.getAddressBook().getNextClientId());
        }
    }

    private Storage storage(Path file) {
        return new StorageManager(new JsonAddressBookStorage(file),
                new JsonUserPrefsStorage(testFolder.resolve("prefs.json")));
    }
}
