package seedu.address.model;

import static java.util.Objects.requireNonNull;
import static seedu.address.commons.util.AppUtil.checkArgument;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import seedu.address.model.exceptions.ClientIdExhaustedException;
import seedu.address.model.person.ClientId;
import seedu.address.model.person.Person;

/**
 * Immutable identity metadata for an address book's canonical records.
 * Person references, rather than contact equality, distinguish ledger entries.
 */
final class ClientIdentityRegistry {
    private static final long EXHAUSTED_COUNTER = (long) Integer.MAX_VALUE + 1;

    private final Map<Person, ClientId> clientIds;
    private final Map<ClientId, Person> personsById;
    private final long nextClientId;

    ClientIdentityRegistry() {
        this(new IdentityHashMap<>(), new HashMap<>(), 1);
    }

    private ClientIdentityRegistry(Map<Person, ClientId> clientIds, Map<ClientId, Person> personsById,
            long nextClientId) {
        this.clientIds = clientIds;
        this.personsById = personsById;
        this.nextClientId = nextClientId;
    }

    /** Validates and snapshots restored metadata without mutating any existing registry. */
    static ClientIdentityRegistry restore(List<Person> persons, List<ClientId> ids, long nextClientId) {
        requireNonNull(persons);
        requireNonNull(ids);
        checkArgument(persons.size() == ids.size(), "Each client must have exactly one ID.");
        checkArgument(nextClientId >= 1 && nextClientId <= EXHAUSTED_COUNTER,
                "The next client ID is outside the supported range.");
        Map<Person, ClientId> clientIds = new IdentityHashMap<>();
        Map<ClientId, Person> personsById = new HashMap<>();
        for (int i = 0; i < persons.size(); i++) {
            Person person = requireNonNull(persons.get(i));
            ClientId id = requireNonNull(ids.get(i));
            checkArgument(id.getValue() < nextClientId, "The next client ID must exceed every allocated ID.");
            checkArgument(!clientIds.containsKey(person), "A canonical client cannot have multiple IDs.");
            checkArgument(!personsById.containsKey(id), "Client IDs must be unique.");
            clientIds.put(person, id);
            personsById.put(id, person);
        }
        return new ClientIdentityRegistry(clientIds, personsById, nextClientId);
    }

    /** Retains canonical identities and allocates only for new references, in supplied list order. */
    ClientIdentityRegistry withPersons(List<Person> persons) {
        requireNonNull(persons);
        List<ClientId> ids = new ArrayList<>();
        long next = nextClientId;
        for (Person person : persons) {
            requireNonNull(person);
            ClientId id = clientIds.get(person);
            if (id == null) {
                if (next == EXHAUSTED_COUNTER) {
                    throw new ClientIdExhaustedException();
                }
                id = new ClientId((int) next);
                next++;
            }
            ids.add(id);
        }
        return restore(persons, ids, next);
    }

    /** Transfers a canonical target's ID to its immutable replacement without allocating. */
    ClientIdentityRegistry withReplacement(Person target, Person replacement) {
        requireNonNull(target);
        requireNonNull(replacement);
        ClientId id = clientIds.get(target);
        checkArgument(id != null, "The replacement target must be canonical.");
        checkArgument(target == replacement || !clientIds.containsKey(replacement),
                "The replacement already belongs to another client.");
        Map<Person, ClientId> changedIds = new IdentityHashMap<>(clientIds);
        Map<ClientId, Person> changedPersons = new HashMap<>(personsById);
        changedIds.remove(target);
        changedIds.put(replacement, id);
        changedPersons.put(id, replacement);
        return new ClientIdentityRegistry(changedIds, changedPersons, nextClientId);
    }

    Optional<ClientId> getClientId(Person canonicalPerson) {
        return Optional.ofNullable(clientIds.get(requireNonNull(canonicalPerson)));
    }

    Optional<Person> getPersonById(ClientId id) {
        return Optional.ofNullable(personsById.get(requireNonNull(id)));
    }

    long getNextClientId() {
        return nextClientId;
    }
}
