package seedu.address.storage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.model.ReadOnlyAddressBook;

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
        var model = new seedu.address.model.ModelManager(book, new seedu.address.model.UserPrefs());
        var logic = new seedu.address.logic.LogicManager(model, storage);
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
