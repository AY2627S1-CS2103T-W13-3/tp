package seedu.address.storage;

import static seedu.address.commons.util.StringUtil.trimOrdinarySpaces;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.node.IntNode;

import seedu.address.commons.exceptions.IllegalValueException;
import seedu.address.model.person.Address;
import seedu.address.model.person.ClientId;
import seedu.address.model.person.Email;
import seedu.address.model.person.FollowUp;
import seedu.address.model.person.Name;
import seedu.address.model.person.Person;
import seedu.address.model.person.Phone;
import seedu.address.model.tag.Tag;

/**
 * Jackson-friendly version of {@link Person}.
 */
class JsonAdaptedPerson {

    public static final String MISSING_FIELD_MESSAGE_FORMAT = "Person's %s field is missing!";

    private final JsonNode clientId;
    private final String name;
    private final String phone;
    private final String email;
    private final String address;
    private final List<JsonAdaptedTag> tags = new ArrayList<>();
    private final JsonAdaptedFollowUp followUp;

    /**
     * Constructs a {@code JsonAdaptedPerson} with the given person details.
     */
    @JsonCreator
    public JsonAdaptedPerson(
            @JsonProperty("name") @JsonDeserialize(using = StoredTextDeserializer.class) String name,
            @JsonProperty("phone") @JsonDeserialize(using = StoredTextDeserializer.class) String phone,
            @JsonProperty("email") @JsonDeserialize(using = StoredTextDeserializer.class) String email,
            @JsonProperty("address") @JsonDeserialize(using = StoredTextDeserializer.class) String address,
            @JsonProperty("tags") List<JsonAdaptedTag> tags,
            @JsonProperty("followUp") JsonAdaptedFollowUp followUp,
            @JsonProperty("clientId") JsonNode clientId) {
        this.clientId = clientId == null ? null : clientId.deepCopy();
        this.name = name;
        this.phone = phone;
        this.email = email;
        this.address = address;
        this.followUp = followUp;
        if (tags != null) {
            this.tags.addAll(tags);
        }
    }

    /** Keeps existing callers and follow-up test data source-compatible. */
    public JsonAdaptedPerson(String name, String phone, String email, String address,
            List<JsonAdaptedTag> tags, JsonAdaptedFollowUp followUp) {
        this(name, phone, email, address, tags, followUp, null);
    }

    /** Keeps existing callers and contact-only test data source-compatible. */
    public JsonAdaptedPerson(String name, String phone, String email, String address, List<JsonAdaptedTag> tags) {
        this(name, phone, email, address, tags, null);
    }

    /**
     * Converts a given {@code Person} into this class for Jackson use.
     */
    public JsonAdaptedPerson(Person source) {
        this(source, null);
    }

    /** Snapshots contact values and the aggregate-owned permanent identity. */
    public JsonAdaptedPerson(Person source, ClientId id) {
        clientId = id == null ? null : IntNode.valueOf(id.getValue());
        name = source.getName().fullName;
        phone = source.getPhone().value;
        email = source.getEmail().value;
        address = source.getAddress().value;
        followUp = source.getFollowUp().map(JsonAdaptedFollowUp::new).orElse(null);
        tags.addAll(source.getTags().stream()
                .map(JsonAdaptedTag::new)
                .collect(Collectors.toList()));
    }

    /** A non-null value selects strict new-format validation, even when it is invalid. */
    boolean hasClientId() {
        return clientId != null && !clientId.isNull();
    }

    /** Validates the raw numeric type before constructing the domain ID. */
    ClientId toClientId() throws IllegalValueException {
        if (!hasClientId() || !clientId.isIntegralNumber() || !clientId.canConvertToInt()
                || clientId.intValue() < 1) {
            throw new IllegalValueException("Client ID must be an integer from 1 to 2147483647.");
        }
        return new ClientId(clientId.intValue());
    }

    /**
     * Converts this Jackson-friendly adapted person object into the model's {@code Person} object.
     *
     * @throws IllegalValueException if there were any data constraints violated in the adapted person.
     */
    public Person toModelType() throws IllegalValueException {
        if (name == null) {
            throw new IllegalValueException(String.format(MISSING_FIELD_MESSAGE_FORMAT, Name.class.getSimpleName()));
        }
        if (!Name.isValidName(name)) {
            throw new IllegalValueException(Name.MESSAGE_CONSTRAINTS);
        }
        final Name modelName = new Name(name);

        if (phone == null) {
            throw new IllegalValueException(String.format(MISSING_FIELD_MESSAGE_FORMAT, Phone.class.getSimpleName()));
        }
        String trimmedPhone = trimOrdinarySpaces(phone);
        if (!Phone.isValidPhone(trimmedPhone)) {
            throw new IllegalValueException(Phone.MESSAGE_CONSTRAINTS);
        }
        final Phone modelPhone = new Phone(trimmedPhone);

        if (email == null) {
            throw new IllegalValueException(String.format(MISSING_FIELD_MESSAGE_FORMAT, Email.class.getSimpleName()));
        }
        String trimmedEmail = trimOrdinarySpaces(email);
        if (!Email.isValidEmail(trimmedEmail)) {
            throw new IllegalValueException(Email.MESSAGE_CONSTRAINTS);
        }
        final Email modelEmail = new Email(trimmedEmail);

        if (address == null) {
            throw new IllegalValueException(String.format(MISSING_FIELD_MESSAGE_FORMAT, Address.class.getSimpleName()));
        }
        if (!Address.isValidAddress(address)) {
            throw new IllegalValueException(Address.MESSAGE_CONSTRAINTS);
        }
        final Address modelAddress = new Address(address);

        final Set<Tag> modelTags = new HashSet<>();
        for (JsonAdaptedTag tag : tags) {
            if (tag == null) {
                throw new IllegalValueException(Tag.MESSAGE_CONSTRAINTS);
            }
            modelTags.add(tag.toModelType());
        }
        Optional<FollowUp> modelFollowUp = followUp == null
                ? Optional.empty() : Optional.of(followUp.toModelType());
        return new Person(modelName, modelPhone, modelEmail, modelAddress, modelTags, modelFollowUp);
    }

}
