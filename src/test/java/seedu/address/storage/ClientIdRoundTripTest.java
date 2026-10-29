package seedu.address.storage;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.model.AddressBook;
import seedu.address.model.person.ClientId;
import seedu.address.model.person.FollowUp;
import seedu.address.testutil.PersonBuilder;

/** Verifies durable identity history using the real atomic storage writer. */
public class ClientIdRoundTripTest {
    @TempDir
    public Path folder;

    @Test
    public void gapsAndEmptyBook_restartNeverReusesDeletedIds() throws Exception {
        Path file = Files.copy(Path.of("src/test/data/ClientIdMigrationTest/gapped.json"),
                folder.resolve("clients.json"));
        JsonAddressBookStorage storage = new JsonAddressBookStorage(file);
        AddressBook book = new AddressBook(storage.readAddressBook().orElseThrow());
        book.removePerson(book.getPersonById(new ClientId(41)).orElseThrow());
        storage.saveAddressBook(book);
        book = new AddressBook(storage.readAddressBook().orElseThrow());
        var next = new PersonBuilder().withName("Next").build();
        book.addPerson(next);
        assertEquals(42, book.getClientId(next).orElseThrow().getValue());
        for (var person : List.copyOf(book.getPersonList())) {
            book.removePerson(person);
        }
        storage.saveAddressBook(book);
        book = new AddressBook(storage.readAddressBook().orElseThrow());
        assertEquals(43, book.getNextClientId());
        book.addPerson(next);
        assertEquals(43, book.getClientId(next).orElseThrow().getValue());
        storage.saveAddressBook(book);
        assertEquals(book, new AddressBook(storage.readAddressBook().orElseThrow()));
    }

    @Test
    public void lastAllocatedId_roundTripPreservesExhaustionWithoutOverflow() throws Exception {
        AddressBook book = new AddressBook();
        var person = new PersonBuilder().build();
        book.restorePersons(List.of(person), List.of(new ClientId(Integer.MAX_VALUE)), 2147483648L);
        JsonAddressBookStorage storage = new JsonAddressBookStorage(folder.resolve("last.json"));
        storage.saveAddressBook(book);
        assertEquals(book, new AddressBook(storage.readAddressBook().orElseThrow()));
    }

    @Test
    public void emptyNewFormat_preservesCounterAndExhaustedSentinel() throws Exception {
        for (long counter : List.of(18L, 2147483648L)) {
            Path file = folder.resolve("empty.json");
            Files.writeString(file, "{\"persons\":[],\"nextClientId\":" + counter + "}");
            JsonAddressBookStorage storage = new JsonAddressBookStorage(file);
            var book = storage.readAddressBook().orElseThrow();
            assertEquals(counter, book.getNextClientId());
            storage.saveAddressBook(book);
            assertEquals(counter, storage.readAddressBook().orElseThrow().getNextClientId());
        }
    }

    @Test
    public void failedCandidateSave_preservesBytesAndLiveCounter_retryUsesSameId() throws Exception {
        Path file = Files.copy(Path.of("src/test/data/ClientIdMigrationTest/gapped.json"),
                folder.resolve("clients.json"));
        JsonAddressBookStorage storage = new JsonAddressBookStorage(file);
        AddressBook live = new AddressBook(storage.readAddressBook().orElseThrow());
        byte[] saved = Files.readAllBytes(file);
        AddressBook candidate = new AddressBook(live);
        var added = new PersonBuilder().withName("New").build();
        candidate.addPerson(added);
        JsonAddressBookStorage failing = new JsonAddressBookStorage(file) {
            @Override
            void replaceFile(Path temporaryFile, Path destination) throws IOException {
                throw new IOException("Injected replacement failure");
            }
        };
        assertThrows(IOException.class, () -> failing.saveAddressBook(candidate));
        assertEquals(42, live.getNextClientId());
        assertTrue(live.getClientId(added).isEmpty());
        assertArrayEquals(saved, Files.readAllBytes(file));
        AddressBook retry = new AddressBook(live);
        retry.addPerson(added);
        assertEquals(42, retry.getClientId(added).orElseThrow().getValue());
        storage.saveAddressBook(retry);
        assertEquals(retry, new AddressBook(storage.readAddressBook().orElseThrow()));
    }

    @Test
    public void legacy_firstMutationWritesNewSchemaAndPreservesUnicodeFollowUp() throws Exception {
        Path file = Files.copy(Path.of("src/test/data/ClientIdMigrationTest/legacy.json"),
                folder.resolve("legacy.json"));
        JsonAddressBookStorage storage = new JsonAddressBookStorage(file);
        AddressBook book = new AddressBook(storage.readAddressBook().orElseThrow());
        var old = book.getPersonList().getFirst();
        var changed = old.withFollowUp(new FollowUp(
                LocalDate.of(2020, 1, 1), "联系客户 🏠"));
        book.setPerson(old, changed);
        storage.saveAddressBook(book);
        assertEquals(book, new AddressBook(storage.readAddressBook().orElseThrow()));
        assertEquals(1, book.getClientId(changed).orElseThrow().getValue());
        assertTrue(Files.readString(file).contains("nextClientId"));
        assertTrue(Files.readString(file).contains("clientId"));
    }
}
