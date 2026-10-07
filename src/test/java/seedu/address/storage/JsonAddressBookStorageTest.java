package seedu.address.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.Assert.assertThrows;
import static seedu.address.testutil.TypicalPersons.ALICE;
import static seedu.address.testutil.TypicalPersons.HOON;
import static seedu.address.testutil.TypicalPersons.IDA;
import static seedu.address.testutil.TypicalPersons.getTypicalAddressBook;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.commons.exceptions.DataLoadingException;
import seedu.address.model.AddressBook;
import seedu.address.model.ReadOnlyAddressBook;
import seedu.address.model.person.FollowUp;
import seedu.address.model.person.Person;

public class JsonAddressBookStorageTest {
    private static final Path TEST_DATA_FOLDER = Paths.get("src", "test", "data", "JsonAddressBookStorageTest");

    @TempDir
    public Path testFolder;

    @Test
    public void readAddressBook_nullFilePath_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> readAddressBook(null));
    }

    private java.util.Optional<ReadOnlyAddressBook> readAddressBook(String filePath) throws Exception {
        return new JsonAddressBookStorage(Paths.get(filePath)).readAddressBook(addToTestDataPathIfNotNull(filePath));
    }

    private Path addToTestDataPathIfNotNull(String prefsFileInTestDataFolder) {
        return prefsFileInTestDataFolder != null
                ? TEST_DATA_FOLDER.resolve(prefsFileInTestDataFolder)
                : null;
    }

    @Test
    public void read_missingFile_emptyResult() throws Exception {
        assertFalse(readAddressBook("NonExistentFile.json").isPresent());
    }

    @Test
    public void read_notJsonFormat_exceptionThrown() {
        assertThrows(DataLoadingException.class, () -> readAddressBook("notJsonFormatAddressBook.json"));
    }

    @Test
    public void readAddressBook_invalidPersonAddressBook_throwDataLoadingException() {
        assertThrows(DataLoadingException.class, () -> readAddressBook("invalidPersonAddressBook.json"));
    }

    @Test
    public void readAddressBook_invalidAndValidPersonAddressBook_throwDataLoadingException() {
        assertThrows(DataLoadingException.class, () -> readAddressBook("invalidAndValidPersonAddressBook.json"));
    }

    @Test
    public void readAndSaveAddressBook_allInOrder_success() throws Exception {
        Path filePath = testFolder.resolve("TempAddressBook.json");
        AddressBook original = getTypicalAddressBook();
        JsonAddressBookStorage jsonAddressBookStorage = new JsonAddressBookStorage(filePath);

        // Save in new file and read back
        jsonAddressBookStorage.saveAddressBook(original, filePath);
        ReadOnlyAddressBook readBack = jsonAddressBookStorage.readAddressBook(filePath).get();
        assertEquals(original, new AddressBook(readBack));

        // Modify data, overwrite existing file, and read back
        original.addPerson(HOON);
        original.removePerson(ALICE);
        jsonAddressBookStorage.saveAddressBook(original, filePath);
        readBack = jsonAddressBookStorage.readAddressBook(filePath).get();
        assertEquals(original, new AddressBook(readBack));

        // Save and read without specifying file path
        original.addPerson(IDA);
        jsonAddressBookStorage.saveAddressBook(original); // file path not specified
        readBack = jsonAddressBookStorage.readAddressBook().get(); // file path not specified
        assertEquals(original, new AddressBook(readBack));

    }

    @Test
    public void saveAddressBook_nullAddressBook_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> saveAddressBook(null, "SomeFile.json"));
    }

    /**
     * Saves {@code addressBook} at the specified {@code filePath}.
     */
    private void saveAddressBook(ReadOnlyAddressBook addressBook, String filePath) {
        try {
            new JsonAddressBookStorage(Paths.get(filePath))
                    .saveAddressBook(addressBook, addToTestDataPathIfNotNull(filePath));
        } catch (IOException ioe) {
            throw new AssertionError("There should not be an error writing to the file.", ioe);
        }
    }

    @Test
    public void saveAddressBook_nullFilePath_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> saveAddressBook(new AddressBook(), null));
    }

    @Test
    public void followUp_roundTripAndLegacyData() throws Exception {
        Path file = testFolder.resolve("followups.json");
        AddressBook book = new AddressBook();
        Person overdue = ALICE.withFollowUp(new FollowUp(LocalDate.of(2020, 1, 2), "Call  client"));
        book.addPerson(overdue);
        book.addPerson(HOON);
        JsonAddressBookStorage storage = new JsonAddressBookStorage(file);
        storage.saveAddressBook(book);

        String json = Files.readString(file);
        assertTrue(json.contains("\"dueDate\" : \"2020-01-02\""));
        assertTrue(json.contains("\"description\" : \"Call  client\""));
        assertTrue(json.contains("\"followUp\" : null"));
        assertEquals(book, new AddressBook(storage.readAddressBook().orElseThrow()));
        assertTrue(HOON.getFollowUp().isEmpty());
        Path legacyFile = testFolder.resolve("legacy.json");
        Files.writeString(legacyFile, "{\"persons\":[{\"name\":\"Legacy Client\","
                + "\"phone\":\"91234567\",\"email\":\"legacy@example.com\","
                + "\"address\":\"Old Street\",\"tags\":[]}]}");
        Person legacy = new JsonAddressBookStorage(legacyFile).readAddressBook().orElseThrow()
                .getPersonList().get(0);
        assertTrue(legacy.getFollowUp().isEmpty());
    }

    @Test
    public void malformedFollowUp_rejectsWholeFile() throws Exception {
        String person = "{\"name\":\"Alice Pauline\",\"phone\":\"94351253\","
                + "\"email\":\"alice@example.com\",\"address\":\"123, Jurong West Ave 6, #08-111\","
                + "\"tags\":[],\"followUp\":%s}";
        String[] invalid = {
            "{}", "{\"dueDate\":\"2026-10-07\"}",
            "{\"description\":\"Call\"}",
            "{\"dueDate\":\"2026-02-30\",\"description\":\"Call\"}",
            "{\"dueDate\":\"2026-10-07\",\"description\":\"   \"}",
            "[{\"dueDate\":\"2026-10-07\",\"description\":\"Call\"}]"
        };
        Path file = testFolder.resolve("invalid-followup.json");
        JsonAddressBookStorage storage = new JsonAddressBookStorage(file);
        for (String followUp : invalid) {
            Files.writeString(file, "{\"persons\":[" + String.format(person, "null") + ","
                    + String.format(person.replace("Alice Pauline", "Bob Pauline"), followUp) + "]}");
            assertThrows(DataLoadingException.class, storage::readAddressBook);
        }
    }

    @Test
    public void failedWriteAndMove_preserveOriginalBytes() throws Exception {
        Path file = testFolder.resolve("book.json");
        byte[] original = "original data".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        AddressBook book = getTypicalAddressBook();
        for (int failure = 0; failure < 3; failure++) {
            Files.write(file, original);
            int failureKind = failure;
            JsonAddressBookStorage storage = new JsonAddressBookStorage(file) {
                @Override
                void writeTemporaryFile(Path temporaryFile, String json) throws IOException {
                    if (failureKind == 0) {
                        Files.writeString(temporaryFile, "partial");
                        throw new IOException("write failed");
                    }
                    super.writeTemporaryFile(temporaryFile, json);
                }

                @Override
                void replaceFile(Path temporaryFile, Path destination) throws IOException {
                    if (failureKind == 1) {
                        throw new IOException("move failed");
                    }
                    throw new AtomicMoveNotSupportedException(temporaryFile.toString(), destination.toString(),
                            "atomic replacement unsupported");
                }
            };
            assertThrows(IOException.class, () -> storage.saveAddressBook(book));
            assertTrue(java.util.Arrays.equals(original, Files.readAllBytes(file)));
            try (java.util.stream.Stream<Path> files = Files.list(testFolder)) {
                assertEquals(1, files.count());
            }
        }
    }
}
