package seedu.address.model;

import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javafx.collections.ObservableList;
import seedu.address.commons.util.ToStringBuilder;
import seedu.address.model.person.ClientId;
import seedu.address.model.person.Person;
import seedu.address.model.person.UniquePersonList;

/**
 * Wraps all data at the address-book level.
 * Duplicates are not allowed (by {@link Person#isDuplicateOf(Person)} comparison).
 */
public class AddressBook implements ReadOnlyAddressBook {

    private final UniquePersonList persons = new UniquePersonList();
    private ClientIdentityRegistry identities = new ClientIdentityRegistry();

    public AddressBook() {}

    /**
     * Creates an AddressBook using the Persons in the {@code toBeCopied}
     */
    public AddressBook(ReadOnlyAddressBook toBeCopied) {
        this();
        resetData(toBeCopied);
    }

    //// list overwrite operations

    /**
     * Replaces the contents of the person list with {@code persons}.
     * {@code persons} must not contain duplicate persons.
     * Retained canonical references keep their IDs; new references receive fresh IDs.
     * Removing records, including every record, never lowers the allocation counter.
     * This is an allocation-oriented bulk replacement, not a refresh or contact-edit API:
     * reconstructed equal-valued records are new references and receive new IDs.
     * Use {@link #setPerson(Person, Person)} for edits and {@link #resetData(ReadOnlyAddressBook)}
     * for an identity-bearing snapshot. Storage restoration must use
     * {@link #restorePersons(List, List, long)} with explicit identity metadata.
     */
    public void setPersons(List<Person> persons) {
        List<Person> replacement = List.copyOf(persons);
        UniquePersonList validated = validatePersons(replacement);
        publishPersons(validated, identities.withPersons(replacement));
    }

    /**
     * Restores a complete contact/identity snapshot, validating all input before publishing.
     * Every record needs a unique ID below the supplied next allocation value.
     * Invalid metadata or duplicate contacts leave the existing book unchanged.
     */
    public void restorePersons(List<Person> persons, List<ClientId> ids, long nextClientId) {
        List<Person> replacement = List.copyOf(persons);
        UniquePersonList validated = validatePersons(replacement);
        ClientIdentityRegistry restored = ClientIdentityRegistry.restore(replacement, ids, nextClientId);
        publishPersons(validated, restored);
    }

    /**
     * Resets the existing data of this {@code AddressBook} with {@code newData}.
     */
    public void resetData(ReadOnlyAddressBook newData) {
        requireNonNull(newData);

        List<Person> replacement = List.copyOf(newData.getPersonList());
        UniquePersonList validated = validatePersons(replacement);
        List<ClientId> ids = replacement.stream().map(person -> newData.getClientId(person)
                .orElseThrow(() -> new IllegalArgumentException("A canonical client must have an ID."))).toList();
        ClientIdentityRegistry restored = ClientIdentityRegistry.restore(replacement, ids, newData.getNextClientId());
        publishPersons(validated, restored);
    }

    //// person-level operations

    /**
     * Returns true if an existing record duplicates {@code person} according to the collection rule.
     */
    public boolean hasPerson(Person person) {
        requireNonNull(person);
        return persons.contains(person);
    }

    /**
     * Adds a person to the address book.
     * The person must not already exist in the address book.
     */
    public void addPerson(Person p) {
        UniquePersonList replacement = copyPersons();
        replacement.add(p);
        publishPersons(replacement, identities.withPersons(replacement.asUnmodifiableObservableList()));
    }

    /**
     * Replaces the given person {@code target} in the list with {@code editedPerson}.
     * {@code target} must exist in the address book.
     * {@code editedPerson} must not duplicate any record other than the replaced target.
     */
    public void setPerson(Person target, Person editedPerson) {
        requireNonNull(editedPerson);

        requireNonNull(target);
        UniquePersonList replacement = copyPersons();
        // List mutations accept equal-valued targets; the registry itself uses canonical references.
        int targetIndex = getPersonList().indexOf(target);
        replacement.setPerson(target, editedPerson);
        Person canonicalTarget = getPersonList().get(targetIndex);
        publishPersons(replacement, identities.withReplacement(canonicalTarget, editedPerson));
    }

    /**
     * Removes {@code key} from this {@code AddressBook}.
     * {@code key} must exist in the address book.
     */
    public void removePerson(Person key) {
        UniquePersonList replacement = copyPersons();
        replacement.remove(key);
        publishPersons(replacement, identities.withPersons(replacement.asUnmodifiableObservableList()));
    }

    //// util methods

    /** Validates contacts independently of the live book and its listeners. */
    private static UniquePersonList validatePersons(List<Person> persons) {
        UniquePersonList validated = new UniquePersonList();
        validated.setPersons(persons);
        return validated;
    }

    private UniquePersonList copyPersons() {
        UniquePersonList copy = new UniquePersonList();
        copy.setPersons(persons);
        return copy;
    }

    /** Publishes metadata first so every list observer can resolve the new canonical records. */
    private void publishPersons(UniquePersonList replacement, ClientIdentityRegistry replacementIdentities) {
        identities = replacementIdentities;
        persons.setPersons(replacement);
    }

    private List<ClientId> getOrderedClientIds() {
        return getPersonList().stream().map(person -> identities.getClientId(person).orElseThrow()).toList();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .add("persons", persons)
                .toString();
    }

    @Override
    public ObservableList<Person> getPersonList() {
        return persons.asUnmodifiableObservableList();
    }

    @Override
    public Optional<ClientId> getClientId(Person canonicalPerson) {
        return identities.getClientId(canonicalPerson);
    }

    @Override
    public Optional<Person> getPersonById(ClientId clientId) {
        return identities.getPersonById(clientId);
    }

    @Override
    public long getNextClientId() {
        return identities.getNextClientId();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof AddressBook otherAddressBook)) {
            return false;
        }

        return persons.equals(otherAddressBook.persons)
                && getOrderedClientIds().equals(otherAddressBook.getOrderedClientIds())
                && getNextClientId() == otherAddressBook.getNextClientId();
    }

    @Override
    public int hashCode() {
        return Objects.hash(persons, getOrderedClientIds(), getNextClientId());
    }
}
