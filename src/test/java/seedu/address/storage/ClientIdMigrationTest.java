package seedu.address.storage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import seedu.address.commons.util.JsonUtil;
import seedu.address.logic.LogicManager;
import seedu.address.model.AddressBook;
import seedu.address.model.ModelManager;
import seedu.address.model.ReadOnlyAddressBook;
import seedu.address.model.UserPrefs;
import seedu.address.model.person.FollowUp;

/** Verifies legacy migration and non-contiguous persisted identity. */
public class ClientIdMigrationTest {
    @TempDir
    public Path folder;

    @Test
    public void legacy_loadAssignsInsertionOrderIdsWithoutRewriting() throws Exception {
        Path file = copyFixture("legacy.json");
        byte[] original = Files.readAllBytes(file);
        ReadOnlyAddressBook book = new JsonAddressBookStorage(file).readAddressBook().orElseThrow();
        assertEquals(List.of(1, 2, 3), book.getPersonList().stream()
                .map(person -> book.getClientId(person).orElseThrow().getValue()).toList());
        assertEquals(4, book.getNextClientId());
        assertReadOnlyCommandsPreserveBytes(file, book, original);
    }

    @Test
    public void newFormat_loadPreservesGapsAndCounterWithoutRewriting() throws Exception {
        Path file = copyFixture("gapped.json");
        byte[] original = Files.readAllBytes(file);
        ReadOnlyAddressBook book = new JsonAddressBookStorage(file).readAddressBook().orElseThrow();
        assertEquals(List.of(2, 17, 41), book.getPersonList().stream()
                .map(person -> book.getClientId(person).orElseThrow().getValue()).toList());
        assertEquals(42, book.getNextClientId());
        assertReadOnlyCommandsPreserveBytes(file, book, original);
    }

    @Test
    public void nullMetadata_remainsLegacy() throws Exception {
        Path file = folder.resolve("null-metadata.json");
        String legacy = Files.readString(Path.of("src/test/data/ClientIdMigrationTest/legacy.json"));
        Files.writeString(file, legacy.replace("\"persons\":", "\"nextClientId\":null,\"persons\":")
                .replace("\"name\":", "\"clientId\":null,\"name\":"));
        ReadOnlyAddressBook book = new JsonAddressBookStorage(file).readAddressBook().orElseThrow();
        assertEquals(4, book.getNextClientId());
        assertEquals(1, book.getClientId(book.getPersonList().getFirst()).orElseThrow().getValue());
    }

    @Test
    public void externallyNullifiedHistory_legacyResetIsExplicitAndBackupRestoresOriginalIds() throws Exception {
        Path file = copyFixture("gapped.json");
        byte[] backup = Files.readAllBytes(file);
        var json = (ObjectNode) JsonUtil.fromJsonString(Files.readString(file), JsonNode.class);
        json.putNull("nextClientId");
        for (var person : json.get("persons")) {
            ObjectNode client = (ObjectNode) person;
            client.putNull("clientId");
        }
        Files.writeString(file, json.toString());
        byte[] resetFile = Files.readAllBytes(file);
        JsonAddressBookStorage storage = new JsonAddressBookStorage(file);
        var book = new AddressBook(storage.readAddressBook().orElseThrow());
        assertEquals(List.of(1, 2, 3), book.getPersonList().stream()
                .map(person -> book.getClientId(person).orElseThrow().getValue()).toList());
        assertEquals(4, book.getNextClientId());
        assertReadOnlyCommandsPreserveBytes(file, book, resetFile);
        var target = book.getPersonList().getFirst();
        book.setPerson(target, target.withFollowUp(new FollowUp(
                LocalDate.of(2020, 1, 1), "Call client")));
        storage.saveAddressBook(book);
        assertEquals(book, new AddressBook(storage.readAddressBook().orElseThrow()));
        Files.write(file, backup);
        var restored = storage.readAddressBook().orElseThrow();
        assertEquals(List.of(2, 17, 41), restored.getPersonList().stream()
                .map(person -> restored.getClientId(person).orElseThrow().getValue()).toList());
        assertEquals(42, restored.getNextClientId());
        assertArrayEquals(backup, Files.readAllBytes(file));
    }

    @Test
    public void bomNewFormat_readsWithoutRewritingSignature() throws Exception {
        Path file = copyFixture("gapped.json");
        Files.writeString(file, "\ufeff" + Files.readString(file));
        byte[] original = Files.readAllBytes(file);
        var book = new JsonAddressBookStorage(file).readAddressBook().orElseThrow();
        assertEquals(42, book.getNextClientId());
        assertArrayEquals(original, Files.readAllBytes(file));
    }

    private void assertReadOnlyCommandsPreserveBytes(Path file, ReadOnlyAddressBook book, byte[] original)
            throws Exception {
        Storage storage = new StorageManager(new JsonAddressBookStorage(file),
                new JsonUserPrefsStorage(folder.resolve("prefs.json")));
        var model = new ModelManager(book, new UserPrefs());
        var logic = new LogicManager(model, storage);
        for (String command : List.of("list", "find Rachel", "followups")) {
            logic.execute(command);
            assertArrayEquals(original, Files.readAllBytes(file));
            assertEquals(book, model.getAddressBook());
        }
    }

    private Path copyFixture(String name) throws Exception {
        return Files.copy(Path.of("src/test/data/ClientIdMigrationTest", name), folder.resolve(name));
    }
}
