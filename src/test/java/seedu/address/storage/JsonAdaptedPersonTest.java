package seedu.address.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static seedu.address.storage.JsonAdaptedPerson.MISSING_FIELD_MESSAGE_FORMAT;
import static seedu.address.testutil.Assert.assertThrows;
import static seedu.address.testutil.TypicalPersons.BENSON;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import seedu.address.commons.exceptions.IllegalValueException;
import seedu.address.commons.util.JsonUtil;
import seedu.address.model.person.Address;
import seedu.address.model.person.ClientId;
import seedu.address.model.person.Email;
import seedu.address.model.person.Name;
import seedu.address.model.person.Person;
import seedu.address.model.person.Phone;
import seedu.address.model.tag.Tag;

public class JsonAdaptedPersonTest {
    private static final String INVALID_NAME = "R&chel";
    private static final String INVALID_PHONE = "+65--1234";
    private static final String INVALID_ADDRESS = " ";
    private static final String INVALID_EMAIL = "example.com";
    private static final String INVALID_TAG = "#friend";

    private static final String VALID_NAME = BENSON.getName().toString();
    private static final String VALID_PHONE = BENSON.getPhone().toString();
    private static final String VALID_EMAIL = BENSON.getEmail().toString();
    private static final String VALID_ADDRESS = BENSON.getAddress().toString();
    private static final List<JsonAdaptedTag> VALID_TAGS = BENSON.getTags().stream()
            .map(JsonAdaptedTag::new)
            .collect(Collectors.toList());

    @Test
    public void toModelType_validPersonDetails_returnsPerson() throws Exception {
        JsonAdaptedPerson person = new JsonAdaptedPerson(BENSON);
        assertEquals(BENSON, person.toModelType());
    }

    @Test
    public void toModelType_unicodeName_preservesSpelling() throws Exception {
        Person source = new Person(new Name("Jose\u0301 Tan"), new Phone("91234567"),
                new Email("jose@example.com"), new Address("Home"), Set.of());
        assertEquals(source, new JsonAdaptedPerson(source).toModelType());
    }

    @Test
    public void toModelType_internationalPhone_preservesEnteredFormat() throws Exception {
        Person source = new Person(new Name("Rachel Lim"), new Phone("+65 9123 4567"),
                new Email("rachel@example.com"), new Address("Home"), Set.of());
        assertEquals(source, new JsonAdaptedPerson(source).toModelType());
    }

    @Test
    public void toModelType_paddedValues_normalizesLikeCommandInput() throws Exception {
        JsonAdaptedPerson stored = new JsonAdaptedPerson("  Ada  Lovelace  ", " 012345 ",
                " Ada+Work@Example.com ", "  10 Main Street  ",
                List.of(new JsonAdaptedTag(" ACTIVE "), new JsonAdaptedTag("active")));

        Person loaded = stored.toModelType();
        assertEquals("Ada  Lovelace", loaded.getName().fullName);
        assertEquals("012345", loaded.getPhone().value);
        assertEquals("Ada+Work@Example.com", loaded.getEmail().value);
        assertEquals("10 Main Street", loaded.getAddress().value);
        assertEquals(Set.of(new Tag("active")), loaded.getTags());
    }

    @Test
    public void toModelType_nullTagEntry_throwsIllegalValueException() {
        JsonAdaptedPerson stored = new JsonAdaptedPerson(VALID_NAME, VALID_PHONE, VALID_EMAIL,
                VALID_ADDRESS, Arrays.asList((JsonAdaptedTag) null));
        assertThrows(IllegalValueException.class, stored::toModelType);
    }

    @Test
    public void toModelType_nullTagName_throwsIllegalValueException() {
        JsonAdaptedPerson stored = new JsonAdaptedPerson(VALID_NAME, VALID_PHONE, VALID_EMAIL,
                VALID_ADDRESS, List.of(new JsonAdaptedTag((String) null)));
        assertThrows(IllegalValueException.class, Tag.MESSAGE_CONSTRAINTS, stored::toModelType);
    }

    @Test
    public void toModelType_invalidName_throwsIllegalValueException() {
        JsonAdaptedPerson person =
                new JsonAdaptedPerson(INVALID_NAME, VALID_PHONE, VALID_EMAIL, VALID_ADDRESS, VALID_TAGS);
        String expectedMessage = Name.MESSAGE_CONSTRAINTS;
        assertThrows(IllegalValueException.class, expectedMessage, person::toModelType);
    }

    @Test
    public void toModelType_unpairedSurrogateName_throwsIllegalValueException() {
        JsonAdaptedPerson person = new JsonAdaptedPerson("Jose\uD800", VALID_PHONE, VALID_EMAIL,
                VALID_ADDRESS, VALID_TAGS);
        assertThrows(IllegalValueException.class, Name.MESSAGE_CONSTRAINTS, person::toModelType);
    }

    @Test
    public void toModelType_nullName_throwsIllegalValueException() {
        JsonAdaptedPerson person = new JsonAdaptedPerson(null, VALID_PHONE, VALID_EMAIL, VALID_ADDRESS, VALID_TAGS);
        String expectedMessage = String.format(MISSING_FIELD_MESSAGE_FORMAT, Name.class.getSimpleName());
        assertThrows(IllegalValueException.class, expectedMessage, person::toModelType);
    }

    @Test
    public void toModelType_invalidPhone_throwsIllegalValueException() {
        JsonAdaptedPerson person =
                new JsonAdaptedPerson(VALID_NAME, INVALID_PHONE, VALID_EMAIL, VALID_ADDRESS, VALID_TAGS);
        String expectedMessage = Phone.MESSAGE_CONSTRAINTS;
        assertThrows(IllegalValueException.class, expectedMessage, person::toModelType);
    }

    @Test
    public void toModelType_nullPhone_throwsIllegalValueException() {
        JsonAdaptedPerson person = new JsonAdaptedPerson(VALID_NAME, null, VALID_EMAIL, VALID_ADDRESS, VALID_TAGS);
        String expectedMessage = String.format(MISSING_FIELD_MESSAGE_FORMAT, Phone.class.getSimpleName());
        assertThrows(IllegalValueException.class, expectedMessage, person::toModelType);
    }

    @Test
    public void toModelType_invalidEmail_throwsIllegalValueException() {
        JsonAdaptedPerson person =
                new JsonAdaptedPerson(VALID_NAME, VALID_PHONE, INVALID_EMAIL, VALID_ADDRESS, VALID_TAGS);
        String expectedMessage = Email.MESSAGE_CONSTRAINTS;
        assertThrows(IllegalValueException.class, expectedMessage, person::toModelType);
    }

    @Test
    public void toModelType_nullEmail_throwsIllegalValueException() {
        JsonAdaptedPerson person = new JsonAdaptedPerson(VALID_NAME, VALID_PHONE, null, VALID_ADDRESS, VALID_TAGS);
        String expectedMessage = String.format(MISSING_FIELD_MESSAGE_FORMAT, Email.class.getSimpleName());
        assertThrows(IllegalValueException.class, expectedMessage, person::toModelType);
    }

    @Test
    public void toModelType_invalidAddress_throwsIllegalValueException() {
        JsonAdaptedPerson person =
                new JsonAdaptedPerson(VALID_NAME, VALID_PHONE, VALID_EMAIL, INVALID_ADDRESS, VALID_TAGS);
        String expectedMessage = Address.MESSAGE_CONSTRAINTS;
        assertThrows(IllegalValueException.class, expectedMessage, person::toModelType);
    }

    @Test
    public void toModelType_nullAddress_throwsIllegalValueException() {
        JsonAdaptedPerson person = new JsonAdaptedPerson(VALID_NAME, VALID_PHONE, VALID_EMAIL, null, VALID_TAGS);
        String expectedMessage = String.format(MISSING_FIELD_MESSAGE_FORMAT, Address.class.getSimpleName());
        assertThrows(IllegalValueException.class, expectedMessage, person::toModelType);
    }

    @Test
    public void toModelType_invalidTags_throwsIllegalValueException() {
        List<JsonAdaptedTag> invalidTags = new ArrayList<>(VALID_TAGS);
        invalidTags.add(new JsonAdaptedTag(INVALID_TAG));
        JsonAdaptedPerson person =
                new JsonAdaptedPerson(VALID_NAME, VALID_PHONE, VALID_EMAIL, VALID_ADDRESS, invalidTags);
        assertThrows(IllegalValueException.class, person::toModelType);
    }

    @Test
    public void toModelType_identityMetadata_preservesContactConversionAndRejectsRawIdCoercion() throws Exception {
        JsonAdaptedPerson identified = new JsonAdaptedPerson(BENSON,
                new ClientId(17));
        assertEquals(BENSON, identified.toModelType());
        assertEquals(17, identified.toClientId().getValue());
        for (String invalid : List.of("\"17\"", "17.5", "true", "0", "2147483648")) {
            JsonAdaptedPerson adapted = JsonUtil.fromJsonString(
                    "{\"clientId\":" + invalid + "}", JsonAdaptedPerson.class);
            assertThrows(IllegalValueException.class, adapted::toClientId);
        }
    }

}
