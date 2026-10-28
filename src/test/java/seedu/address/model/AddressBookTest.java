package seedu.address.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.logic.commands.CommandTestUtil.VALID_ADDRESS_BOB;
import static seedu.address.logic.commands.CommandTestUtil.VALID_TAG_HUSBAND;
import static seedu.address.testutil.Assert.assertThrows;
import static seedu.address.testutil.TypicalPersons.ALICE;
import static seedu.address.testutil.TypicalPersons.BOB;
import static seedu.address.testutil.TypicalPersons.CARL;
import static seedu.address.testutil.TypicalPersons.getTypicalAddressBook;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import seedu.address.model.exceptions.ClientIdExhaustedException;
import seedu.address.model.person.ClientId;
import seedu.address.model.person.FollowUp;
import seedu.address.model.person.Person;
import seedu.address.model.person.exceptions.DuplicatePersonException;
import seedu.address.model.person.exceptions.PersonNotFoundException;
import seedu.address.testutil.PersonBuilder;

public class AddressBookTest {

    private final AddressBook addressBook = new AddressBook();

    @Test
    public void constructor() {
        assertEquals(List.of(), addressBook.getPersonList());
    }

    @Test
    public void resetData_null_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> addressBook.resetData(null));
    }

    @Test
    public void resetData_withValidReadOnlyAddressBook_replacesData() {
        AddressBook newData = getTypicalAddressBook();
        addressBook.resetData(newData);
        assertEquals(newData, addressBook);
    }

    @Test
    public void resetData_withDuplicatePersons_throwsDuplicatePersonException() {
        // Two persons with the same identity fields
        Person editedAlice = new PersonBuilder(ALICE).withAddress(VALID_ADDRESS_BOB).withTags(VALID_TAG_HUSBAND)
                .build();
        List<Person> newPersons = List.of(ALICE, editedAlice);
        AddressBookStub newData = new AddressBookStub(newPersons);

        assertThrows(DuplicatePersonException.class, () -> addressBook.resetData(newData));
    }

    @Test
    public void hasPerson_nullPerson_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> addressBook.hasPerson(null));
    }

    @Test
    public void hasPerson_personNotInAddressBook_returnsFalse() {
        assertFalse(addressBook.hasPerson(ALICE));
    }

    @Test
    public void hasPerson_personInAddressBook_returnsTrue() {
        addressBook.addPerson(ALICE);
        assertTrue(addressBook.hasPerson(ALICE));
    }

    @Test
    public void hasPerson_personWithSameIdentityFieldsInAddressBook_returnsTrue() {
        addressBook.addPerson(ALICE);
        Person editedAlice = new PersonBuilder(ALICE).withAddress(VALID_ADDRESS_BOB).withTags(VALID_TAG_HUSBAND)
                .build();
        assertTrue(addressBook.hasPerson(editedAlice));
    }

    @Test
    public void getPersonList_modifyList_throwsUnsupportedOperationException() {
        assertThrows(UnsupportedOperationException.class, () -> addressBook.getPersonList().remove(0));
    }

    @Test
    public void toStringMethod() {
        String expected = AddressBook.class.getCanonicalName() + "{persons=" + addressBook.getPersonList() + "}";
        assertEquals(expected, addressBook.toString());
    }

    /**
     * A stub ReadOnlyAddressBook whose persons list can violate interface constraints.
     */
    private static class AddressBookStub implements ReadOnlyAddressBook {
        private final ObservableList<Person> persons = FXCollections.observableArrayList();
        private final Map<Person, ClientId> ids = new IdentityHashMap<>();
        private final Map<ClientId, Person> records = new HashMap<>();

        AddressBookStub(Collection<Person> persons) {
            this.persons.setAll(persons);
            for (Person person : persons) {
                ClientId id = new ClientId(ids.size() + 1);
                ids.put(person, id);
                records.put(id, person);
            }
        }

        @Override
        public Optional<ClientId> getClientId(Person person) {
            return Optional.ofNullable(ids.get(person));
        }

        @Override
        public Optional<Person> getPersonById(ClientId id) {
            return Optional.ofNullable(records.get(id));
        }

        @Override
        public long getNextClientId() {
            return ids.size() + 1L;
        }

        @Override
        public ObservableList<Person> getPersonList() {
            return persons;
        }
    }

    @Test
    public void resetData_sameNameDifferentContacts_acceptsBoth() {
        Person otherAlice = new PersonBuilder(ALICE).withPhone("000").withEmail("other@example.com").build();
        addressBook.resetData(new AddressBookStub(List.of(ALICE, otherAlice)));
        assertEquals(List.of(ALICE, otherAlice), addressBook.getPersonList());
        assertEquals(addressBook, new AddressBook(addressBook));
    }

    @Test
    public void resetData_normalizedDuplicate_rejectsWithoutChanges() {
        addressBook.addPerson(ALICE);
        Person duplicate = new PersonBuilder(ALICE).withName("ALICE   PAULINE").build();
        assertThrows(DuplicatePersonException.class, ()
            -> addressBook.resetData(new AddressBookStub(List.of(ALICE, duplicate))));
        assertEquals(List.of(ALICE), addressBook.getPersonList());
    }


    @Test
    public void addPerson_deletionAndReaddition_neverReusesId() {
        addressBook.addPerson(ALICE);
        addressBook.addPerson(BOB);
        addressBook.addPerson(CARL);
        assertEquals(List.of(1, 2, 3), clientIds(addressBook));
        addressBook.removePerson(CARL);
        addressBook.addPerson(CARL);
        assertEquals(List.of(1, 2, 4), clientIds(addressBook));
        addressBook.setPersons(List.of());
        assertEquals(5L, addressBook.getNextClientId());
        addressBook.addPerson(ALICE);
        assertEquals(List.of(5), clientIds(addressBook));
    }

    @Test
    public void getClientId_equalCopyAndUnknownId_doNotResolveOrAllocate() {
        addressBook.addPerson(ALICE);
        assertEquals(Optional.of(new ClientId(1)), addressBook.getClientId(ALICE));
        assertTrue(addressBook.getClientId(new PersonBuilder(ALICE).build()).isEmpty());
        assertTrue(addressBook.getPersonById(new ClientId(2)).isEmpty());
        assertSame(ALICE, addressBook.getPersonById(new ClientId(1)).orElseThrow());
        assertEquals(2L, addressBook.getNextClientId());
        assertThrows(NullPointerException.class, () -> addressBook.getClientId(null));
        assertThrows(NullPointerException.class, () -> addressBook.getPersonById(null));
    }

    @Test
    public void addPerson_duplicate_rejectsWithoutConsumingId() {
        addressBook.addPerson(ALICE);
        AddressBook before = new AddressBook(addressBook);
        assertThrows(DuplicatePersonException.class, () -> addressBook.addPerson(new PersonBuilder(ALICE).build()));
        assertEquals(before, addressBook);
        addressBook.addPerson(BOB);
        assertEquals(List.of(1, 2), clientIds(addressBook));
    }

    @Test
    public void setPersons_reorderingAndNewReferences_retainsOnlyCanonicalIds() {
        addressBook.setPersons(List.of(ALICE, BOB));
        addressBook.setPersons(List.of(BOB, CARL, ALICE));
        assertEquals(List.of(2, 3, 1), clientIds(addressBook));
        Person equalAlice = new PersonBuilder(ALICE).build();
        addressBook.setPersons(List.of(equalAlice, BOB));
        assertEquals(List.of(4, 2), clientIds(addressBook));
        assertTrue(addressBook.getClientId(ALICE).isEmpty());
        assertTrue(addressBook.getPersonById(new ClientId(3)).isEmpty());
        assertEquals(5L, addressBook.getNextClientId());
    }

    @Test
    public void setPerson_equalTargetAndFollowUpReplacement_preservesId() {
        addressBook.restorePersons(List.of(ALICE, BOB), List.of(new ClientId(2), new ClientId(17)), 42);
        Person replacement = ALICE.withFollowUp(new FollowUp(LocalDate.of(2026, 10, 1), "Call Alice"));
        addressBook.setPerson(new PersonBuilder(ALICE).build(), replacement);
        assertEquals(List.of(2, 17), clientIds(addressBook));
        assertTrue(addressBook.getClientId(ALICE).isEmpty());
        assertSame(replacement, addressBook.getPersonById(new ClientId(2)).orElseThrow());
        addressBook.setPerson(replacement, replacement.withoutFollowUp());
        assertEquals(List.of(2, 17), clientIds(addressBook));
        assertEquals(42L, addressBook.getNextClientId());
    }

    @Test
    public void removePerson_equalTarget_removesCanonicalIdentity() {
        addressBook.setPersons(List.of(ALICE, BOB));
        addressBook.removePerson(new PersonBuilder(ALICE).build());
        assertEquals(List.of(2), clientIds(addressBook));
        assertTrue(addressBook.getClientId(ALICE).isEmpty());
        assertTrue(addressBook.getPersonById(new ClientId(1)).isEmpty());
        assertEquals(3L, addressBook.getNextClientId());
    }

    @Test
    public void mutations_invalidTargetOrReplacement_leaveListAndIdentityUnchanged() {
        addressBook.setPersons(List.of(ALICE, BOB));
        AddressBook before = new AddressBook(addressBook);
        assertThrows(PersonNotFoundException.class, () -> addressBook.setPerson(CARL, ALICE));
        assertThrows(PersonNotFoundException.class, () -> addressBook.removePerson(CARL));
        assertThrows(DuplicatePersonException.class, () -> addressBook.setPerson(ALICE, BOB));
        assertThrows(DuplicatePersonException.class, () -> addressBook.setPersons(List.of(ALICE, ALICE)));
        assertThrows(NullPointerException.class, () -> addressBook.setPersons(Arrays.asList(ALICE, null)));
        assertThrows(NullPointerException.class, () -> addressBook.addPerson(null));
        assertThrows(NullPointerException.class, () -> addressBook.setPerson(null, ALICE));
        assertThrows(NullPointerException.class, () -> addressBook.setPerson(ALICE, null));
        assertThrows(NullPointerException.class, () -> addressBook.removePerson(null));
        assertEquals(before, addressBook);
    }

    @Test
    public void copyAndReset_gappedIdentitiesAndCounter_areIndependentAndExact() {
        addressBook.restorePersons(List.of(ALICE, BOB), List.of(new ClientId(2), new ClientId(17)), 42);
        AddressBook copy = new AddressBook(addressBook);
        assertEquals(addressBook, copy);
        assertSame(ALICE, copy.getPersonById(new ClientId(2)).orElseThrow());
        copy.removePerson(BOB);
        copy.addPerson(CARL);
        assertEquals(List.of(2, 42), clientIds(copy));
        assertEquals(List.of(2, 17), clientIds(addressBook));
        assertEquals(42L, addressBook.getNextClientId());
        addressBook.resetData(copy);
        addressBook.resetData(addressBook);
        assertEquals(copy, addressBook);
        assertEquals(43L, addressBook.getNextClientId());
        copy.setPersons(List.of());
        assertEquals(List.of(2, 42), clientIds(addressBook));
    }

    @Test
    public void restorePersons_validEmptyCounter_preservesHighWaterMark() {
        addressBook.restorePersons(List.of(), List.of(), 18);
        addressBook.addPerson(ALICE);
        assertEquals(List.of(18), clientIds(addressBook));
        assertEquals(19L, addressBook.getNextClientId());
    }

    @Test
    public void restorePersons_invalidMetadata_rejectsBeforeAnyPublication() {
        addressBook.addPerson(CARL);
        AddressBook before = new AddressBook(addressBook);
        List<List<Person>> publications = new ArrayList<>();
        addressBook.getPersonList().addListener((ListChangeListener<Person>) change ->
                publications.add(List.copyOf(addressBook.getPersonList())));
        assertThrows(IllegalArgumentException.class, () -> addressBook.restorePersons(
                List.of(ALICE, BOB), List.of(new ClientId(2)), 18));
        assertThrows(IllegalArgumentException.class, () -> addressBook.restorePersons(
                List.of(ALICE, BOB), List.of(new ClientId(2), new ClientId(2)), 18));
        for (long counter : List.of(0L, -1L, 17L, 2147483649L, Long.MAX_VALUE)) {
            assertThrows(IllegalArgumentException.class, () -> addressBook.restorePersons(
                    List.of(ALICE), List.of(new ClientId(17)), counter));
        }
        assertThrows(DuplicatePersonException.class, () -> addressBook.restorePersons(
                List.of(ALICE, new PersonBuilder(ALICE).build()), List.of(new ClientId(2), new ClientId(17)), 18));
        assertThrows(NullPointerException.class, () -> addressBook.restorePersons(null, List.of(), 1));
        assertThrows(NullPointerException.class, () -> addressBook.restorePersons(List.of(), null, 1));
        assertThrows(NullPointerException.class, () -> addressBook.restorePersons(
                List.of(ALICE), Arrays.asList((ClientId) null), 18));
        assertEquals(before, addressBook);
        assertTrue(publications.isEmpty());
    }

    @Test
    public void addPerson_lastIdAndExhaustion_neverOverflowOrLoseExistingRecords() {
        addressBook.restorePersons(List.of(ALICE), List.of(new ClientId(17)), Integer.MAX_VALUE);
        addressBook.addPerson(BOB);
        assertEquals(List.of(17, Integer.MAX_VALUE), clientIds(addressBook));
        assertEquals(2147483648L, addressBook.getNextClientId());
        AddressBook before = new AddressBook(addressBook);
        assertThrows(ClientIdExhaustedException.class, () -> addressBook.addPerson(CARL));
        assertEquals(before, addressBook);
        assertThrows(DuplicatePersonException.class, () -> addressBook.addPerson(ALICE));
        Person replacement = new PersonBuilder(ALICE).withAddress("New home").build();
        addressBook.setPerson(ALICE, replacement);
        addressBook.removePerson(BOB);
        addressBook.setPersons(List.of(replacement));
        assertEquals(List.of(17), clientIds(addressBook));
        assertEquals(2147483648L, addressBook.getNextClientId());
        before = new AddressBook(addressBook);
        assertThrows(ClientIdExhaustedException.class, () -> addressBook.setPersons(List.of(replacement, CARL)));
        assertEquals(before, addressBook);
        addressBook.removePerson(replacement);
        AddressBook emptyCopy = new AddressBook(addressBook);
        assertThrows(ClientIdExhaustedException.class, () -> emptyCopy.addPerson(CARL));
        assertEquals(2147483648L, emptyCopy.getNextClientId());
    }

    @Test
    public void observableChanges_publishIdentityBeforeListNotifications() {
        ObservableList<Person> view = addressBook.getPersonList();
        List<List<Integer>> publishedIds = new ArrayList<>();
        List<Boolean> consistentLookups = new ArrayList<>();
        view.addListener((ListChangeListener<Person>) change -> {
            publishedIds.add(clientIds(addressBook));
            consistentLookups.add(view.stream().allMatch(person -> addressBook.getClientId(person)
                    .flatMap(addressBook::getPersonById).orElse(null) == person));
        });
        addressBook.addPerson(ALICE);
        addressBook.addPerson(BOB);
        Person replacement = new PersonBuilder(ALICE).withAddress("New home").build();
        addressBook.setPerson(ALICE, replacement);
        addressBook.removePerson(BOB);
        addressBook.setPersons(List.of(CARL, replacement));
        addressBook.restorePersons(List.of(ALICE), List.of(new ClientId(17)), 42);
        addressBook.resetData(new AddressBook());
        assertSame(view, addressBook.getPersonList());
        assertEquals(List.of(List.of(1), List.of(1, 2), List.of(1, 2), List.of(1),
                List.of(3, 1), List.of(17), List.of()), publishedIds);
        assertEquals(List.of(true, true, true, true, true, true, true), consistentLookups);
    }

    @Test
    public void equals_identityMetadata_participatesInEqualityAndHashing() {
        addressBook.restorePersons(List.of(ALICE), List.of(new ClientId(17)), 42);
        AddressBook sameValues = new AddressBook();
        sameValues.restorePersons(List.of(new PersonBuilder(ALICE).build()), List.of(new ClientId(17)), 42);
        assertEquals(addressBook, sameValues);
        assertEquals(addressBook.hashCode(), sameValues.hashCode());
        sameValues.restorePersons(List.of(ALICE), List.of(new ClientId(2)), 42);
        assertNotEquals(addressBook, sameValues);
        sameValues.restorePersons(List.of(ALICE), List.of(new ClientId(17)), 43);
        assertNotEquals(addressBook, sameValues);
        assertFalse(addressBook.equals(null));
        assertFalse(addressBook.equals("book"));
        assertEquals(addressBook, addressBook);
    }

    private static List<Integer> clientIds(ReadOnlyAddressBook book) {
        return book.getPersonList().stream().map(person -> book.getClientId(person).orElseThrow().getValue()).toList();
    }

}
