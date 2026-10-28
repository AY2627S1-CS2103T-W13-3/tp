package seedu.address.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.Assert.assertThrows;
import static seedu.address.testutil.TypicalPersons.ALICE;
import static seedu.address.testutil.TypicalPersons.BOB;
import static seedu.address.testutil.TypicalPersons.CARL;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.address.model.exceptions.ClientIdExhaustedException;
import seedu.address.model.person.ClientId;
import seedu.address.model.person.Person;
import seedu.address.testutil.PersonBuilder;

/** Tests immutable registry snapshots independently of observable-list publication. */
public class ClientIdentityRegistryTest {
    @Test
    public void withPersons_retainedReferences_preservesIdsWithoutMutatingPreviousSnapshot() {
        ClientIdentityRegistry first = new ClientIdentityRegistry().withPersons(List.of(ALICE, BOB));
        ClientIdentityRegistry next = first.withPersons(List.of(BOB, CARL));
        assertEquals(new ClientId(1), first.getClientId(ALICE).orElseThrow());
        assertEquals(new ClientId(2), next.getClientId(BOB).orElseThrow());
        assertEquals(new ClientId(3), next.getClientId(CARL).orElseThrow());
        assertTrue(next.getClientId(ALICE).isEmpty());
        assertTrue(next.getPersonById(new ClientId(1)).isEmpty());
        assertEquals(3L, first.getNextClientId());
        assertEquals(4L, next.getNextClientId());
    }

    @Test
    public void restore_mutableInputCollections_areDefensivelyCopied() {
        List<Person> persons = new ArrayList<>(List.of(ALICE));
        List<ClientId> ids = new ArrayList<>(List.of(new ClientId(17)));
        ClientIdentityRegistry registry = ClientIdentityRegistry.restore(persons, ids, 42);
        persons.set(0, BOB);
        ids.set(0, new ClientId(2));
        assertSame(ALICE, registry.getPersonById(new ClientId(17)).orElseThrow());
        assertTrue(registry.getClientId(BOB).isEmpty());
        assertTrue(registry.getClientId(new PersonBuilder(ALICE).build()).isEmpty());
        assertEquals(42L, registry.getNextClientId());
    }

    @Test
    public void withReplacement_canonicalTarget_transfersIdWithoutAllocating() {
        ClientIdentityRegistry registry = new ClientIdentityRegistry().withPersons(List.of(ALICE, BOB));
        Person replacement = new PersonBuilder(ALICE).withAddress("New home").build();
        ClientIdentityRegistry changed = registry.withReplacement(ALICE, replacement);
        assertTrue(changed.getClientId(ALICE).isEmpty());
        assertSame(replacement, changed.getPersonById(new ClientId(1)).orElseThrow());
        assertSame(ALICE, registry.getPersonById(new ClientId(1)).orElseThrow());
        assertEquals(3L, changed.getNextClientId());
        assertThrows(IllegalArgumentException.class, () -> registry.withReplacement(CARL, replacement));
        assertThrows(IllegalArgumentException.class, () -> registry.withReplacement(ALICE, BOB));
    }

    @Test
    public void withPersons_exhaustion_failureDoesNotMutateSourceSnapshot() {
        ClientIdentityRegistry registry = ClientIdentityRegistry.restore(
                List.of(ALICE), List.of(new ClientId(17)), 2147483648L);
        assertThrows(ClientIdExhaustedException.class, () -> registry.withPersons(List.of(ALICE, BOB)));
        assertEquals(new ClientId(17), registry.getClientId(ALICE).orElseThrow());
        assertEquals(2147483648L, registry.withPersons(List.of()).getNextClientId());
    }

    @Test
    public void restore_invalidAssociations_rejects() {
        assertThrows(IllegalArgumentException.class, () -> ClientIdentityRegistry.restore(
                List.of(ALICE, ALICE), List.of(new ClientId(1), new ClientId(2)), 3));
        assertThrows(IllegalArgumentException.class, () -> ClientIdentityRegistry.restore(
                List.of(ALICE, BOB), List.of(new ClientId(1), new ClientId(1)), 3));
        assertThrows(IllegalArgumentException.class, () -> ClientIdentityRegistry.restore(
                List.of(ALICE), List.of(new ClientId(17)), 17));
        assertThrows(IllegalArgumentException.class, () -> ClientIdentityRegistry.restore(
                List.of(ALICE), List.of(), 18));
        assertThrows(IllegalArgumentException.class, () -> ClientIdentityRegistry.restore(List.of(), List.of(), 0));
    }
}
