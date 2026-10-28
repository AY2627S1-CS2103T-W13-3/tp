package seedu.address.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static seedu.address.testutil.Assert.assertThrows;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.address.commons.exceptions.IllegalValueException;
import seedu.address.commons.util.JsonUtil;
import seedu.address.model.AddressBook;
import seedu.address.testutil.TypicalPersons;

public class JsonSerializableAddressBookTest {

    private static final Path TEST_DATA_FOLDER = Paths.get("src", "test", "data", "JsonSerializableAddressBookTest");
    private static final Path TYPICAL_PERSONS_FILE = TEST_DATA_FOLDER.resolve("typicalPersonsAddressBook.json");
    private static final Path INVALID_PERSON_FILE = TEST_DATA_FOLDER.resolve("invalidPersonAddressBook.json");
    private static final Path DUPLICATE_PERSON_FILE = TEST_DATA_FOLDER.resolve("duplicatePersonAddressBook.json");

    @Test
    public void toModelType_typicalPersonsFile_success() throws Exception {
        JsonSerializableAddressBook dataFromFile = JsonUtil.readJsonFile(TYPICAL_PERSONS_FILE,
                JsonSerializableAddressBook.class).get();
        AddressBook addressBookFromFile = dataFromFile.toModelType();
        AddressBook typicalPersonsAddressBook = TypicalPersons.getTypicalAddressBook();
        assertEquals(addressBookFromFile, typicalPersonsAddressBook);
    }

    @Test
    public void toModelType_invalidPersonFile_throwsIllegalValueException() throws Exception {
        JsonSerializableAddressBook dataFromFile = JsonUtil.readJsonFile(INVALID_PERSON_FILE,
                JsonSerializableAddressBook.class).get();
        assertThrows(IllegalValueException.class, dataFromFile::toModelType);
    }

    @Test
    public void toModelType_duplicatePersons_throwsIllegalValueException() throws Exception {
        JsonSerializableAddressBook dataFromFile = JsonUtil.readJsonFile(DUPLICATE_PERSON_FILE,
                JsonSerializableAddressBook.class).get();
        assertThrows(IllegalValueException.class, JsonSerializableAddressBook.MESSAGE_DUPLICATE_PERSON,
                dataFromFile::toModelType);
    }

    @Test
    public void toModelType_normalizedDuplicateEmail_rejectsWholeBook() {
        JsonAdaptedPerson first = new JsonAdaptedPerson("Ada  Lovelace", "12345", "ADA@example.com",
                "First Street", null);
        JsonAdaptedPerson duplicate = new JsonAdaptedPerson("ada lovelace", "67890", "ada@EXAMPLE.com",
                "Second Street", null);
        JsonSerializableAddressBook stored = new JsonSerializableAddressBook(List.of(first, duplicate));

        assertThrows(IllegalValueException.class, JsonSerializableAddressBook.MESSAGE_DUPLICATE_PERSON,
                stored::toModelType);
    }

    @Test
    public void toModelType_normalizedDuplicatePhoneWithDifferentEmails_rejectsWholeBook() {
        JsonAdaptedPerson first = new JsonAdaptedPerson("Ada  Lovelace", "12345", "ada1@example.com",
                "First Street", null);
        JsonAdaptedPerson duplicate = new JsonAdaptedPerson("ada lovelace", " 12345 ", "ada2@example.com",
                "Second Street", null);
        JsonSerializableAddressBook stored = new JsonSerializableAddressBook(List.of(first, duplicate));

        assertThrows(IllegalValueException.class, JsonSerializableAddressBook.MESSAGE_DUPLICATE_PERSON,
                stored::toModelType);
    }

    @Test
    public void toModelType_sameNameDifferentContacts_keepsBothClients() throws Exception {
        JsonAdaptedPerson first = new JsonAdaptedPerson("Ada Lovelace", "12345", "ada1@example.com",
                "First Street", null);
        JsonAdaptedPerson second = new JsonAdaptedPerson("ada lovelace", "67890", "ada2@example.com",
                "Second Street", null);

        AddressBook loaded = new JsonSerializableAddressBook(List.of(first, second)).toModelType();
        assertEquals(2, loaded.getPersonList().size());
    }

    @Test
    public void toModelType_nullPersonEntry_throwsIllegalValueException() {
        JsonSerializableAddressBook stored = new JsonSerializableAddressBook(
                Arrays.asList((JsonAdaptedPerson) null));
        assertThrows(IllegalValueException.class, stored::toModelType);
    }

    @Test
    public void toModelType_newFormatGap_preservesIdsAndCounter() throws Exception {
        String json = "{\"nextClientId\":18,\"persons\":["
                + "{\"clientId\":2,\"name\":\"Ada\",\"phone\":\"111\","
                + "\"email\":\"ada@example.com\",\"address\":\"Home\"},"
                + "{\"clientId\":17,\"name\":\"Bob\",\"phone\":\"222\","
                + "\"email\":\"bob@example.com\",\"address\":\"Home\"}]}";
        AddressBook book = JsonUtil.fromJsonString(json, JsonSerializableAddressBook.class).toModelType();
        assertEquals(18, book.getNextClientId());
        assertEquals(List.of(2, 17), book.getPersonList().stream()
                .map(person -> book.getClientId(person).orElseThrow().getValue()).toList());
        String saved = JsonUtil.toJsonString(new JsonSerializableAddressBook(book));
        assertEquals(book, JsonUtil.fromJsonString(saved, JsonSerializableAddressBook.class).toModelType());
    }

}
