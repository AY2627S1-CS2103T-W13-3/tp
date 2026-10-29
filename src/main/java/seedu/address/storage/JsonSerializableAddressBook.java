package seedu.address.storage;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.LongNode;

import seedu.address.commons.exceptions.IllegalValueException;
import seedu.address.model.AddressBook;
import seedu.address.model.ReadOnlyAddressBook;
import seedu.address.model.person.ClientId;
import seedu.address.model.person.Person;
import seedu.address.model.person.exceptions.DuplicatePersonException;

/**
 * An Immutable AddressBook that is serializable to JSON format.
 */
@JsonRootName(value = "addressbook")
class JsonSerializableAddressBook {

    public static final String MESSAGE_DUPLICATE_PERSON = "Persons list contains duplicate person(s).";
    private static final long MAX_NEXT_CLIENT_ID = (long) Integer.MAX_VALUE + 1;

    private final List<JsonAdaptedPerson> persons;
    private final JsonNode nextClientId;

    /**
     * Constructs a {@code JsonSerializableAddressBook} with the given persons.
     */
    @JsonCreator
    public JsonSerializableAddressBook(@JsonProperty("persons") List<JsonAdaptedPerson> persons,
            @JsonProperty("nextClientId") JsonNode nextClientId) {
        this.nextClientId = nextClientId == null ? null : nextClientId.deepCopy();
        this.persons = persons == null ? null : new ArrayList<>(persons);
    }

    /** Creates legacy, ID-free test data without inventing metadata. */
    public JsonSerializableAddressBook(List<JsonAdaptedPerson> persons) {
        this(persons, null);
    }

    /**
     * Converts a given {@code ReadOnlyAddressBook} into this class for Jackson use.
     *
     * @param source future changes to this will not affect the created {@code JsonSerializableAddressBook}.
     */
    public JsonSerializableAddressBook(ReadOnlyAddressBook source) {
        persons = new ArrayList<>();
        nextClientId = LongNode.valueOf(source.getNextClientId());
        for (Person person : source.getPersonList()) {
            ClientId id = source.getClientId(person).orElseThrow(() ->
                    new IllegalArgumentException("Canonical client has no permanent ID."));
            persons.add(new JsonAdaptedPerson(person, id));
        }
    }

    /**
     * Converts this address book into the model's {@code AddressBook} object.
     *
     * @throws IllegalValueException if there were any data constraints violated.
     */
    public AddressBook toModelType() throws IllegalValueException {
        List<Person> modelPersons = readPersons();
        boolean hasIdentityMetadata = hasIdentityMetadata();
        long counter = hasIdentityMetadata ? readCounter() : modelPersons.size() + 1L;
        List<ClientId> ids = readClientIds(hasIdentityMetadata);
        AddressBook addressBook = new AddressBook();
        try {
            addressBook.restorePersons(modelPersons, ids, counter);
        } catch (DuplicatePersonException e) {
            throw new IllegalValueException(MESSAGE_DUPLICATE_PERSON, e);
        } catch (IllegalArgumentException e) {
            throw new IllegalValueException("Invalid client identity metadata.", e);
        }
        return addressBook;
    }

    private List<Person> readPersons() throws IllegalValueException {
        if (persons == null) {
            throw new IllegalValueException("Persons list is missing.");
        }
        List<Person> modelPersons = new ArrayList<>();
        for (JsonAdaptedPerson jsonAdaptedPerson : persons) {
            if (jsonAdaptedPerson == null) {
                throw new IllegalValueException("Persons list contains a missing person.");
            }
            modelPersons.add(jsonAdaptedPerson.toModelType());
        }
        return modelPersons;
    }

    private boolean hasIdentityMetadata() {
        // The shared migration contract treats all-absent/all-null metadata as legacy.
        // Once all identity history is removed externally, this file cannot reveal its former IDs.
        // Any remaining non-null metadata selects strict validation; never downgrade a partial file.
        return nextClientId != null && !nextClientId.isNull()
                || persons.stream().anyMatch(JsonAdaptedPerson::hasClientId);
    }

    private List<ClientId> readClientIds(boolean hasIdentityMetadata) throws IllegalValueException {
        List<ClientId> ids = new ArrayList<>();
        for (int index = 0; index < persons.size(); index++) {
            ids.add(hasIdentityMetadata ? persons.get(index).toClientId() : new ClientId(index + 1));
        }
        return ids;
    }

    private long readCounter() throws IllegalValueException {
        if (nextClientId == null || !nextClientId.isIntegralNumber() || !nextClientId.canConvertToLong()
                || nextClientId.longValue() < 1 || nextClientId.longValue() > MAX_NEXT_CLIENT_ID) {
            throw new IllegalValueException("Next client ID must be an integer from 1 to 2147483648.");
        }
        return nextClientId.longValue();
    }

}
