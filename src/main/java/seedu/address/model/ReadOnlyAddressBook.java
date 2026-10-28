package seedu.address.model;

import java.util.Optional;

import javafx.collections.ObservableList;
import seedu.address.model.person.ClientId;
import seedu.address.model.person.Person;

/**
 * Unmodifiable view of an address book
 */
public interface ReadOnlyAddressBook {

    /**
     * Returns an unmodifiable view of the persons list.
     * This list will not contain any duplicate persons.
     */
    ObservableList<Person> getPersonList();

    /**
     * Returns a canonical record's permanent ID, or empty for an absent/equal-valued copy.
     * The argument must be non-null. This lookup does not allocate an ID.
     */
    Optional<ClientId> getClientId(Person canonicalPerson);

    /** Returns the canonical record for a non-null ID, or empty if that ID is absent. */
    Optional<Person> getPersonById(ClientId clientId);

    /**
     * Returns the next allocation value in {@code 1..2147483648}.
     * The last value indicates exhaustion and is not a valid client ID.
     */
    long getNextClientId();
}
