package seedu.address.model.person;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.logic.commands.CommandTestUtil.VALID_ADDRESS_BOB;
import static seedu.address.logic.commands.CommandTestUtil.VALID_TAG_HUSBAND;
import static seedu.address.testutil.Assert.assertThrows;
import static seedu.address.testutil.TypicalPersons.ALICE;
import static seedu.address.testutil.TypicalPersons.BOB;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

import org.junit.jupiter.api.Test;

import seedu.address.model.person.exceptions.DuplicatePersonException;
import seedu.address.model.person.exceptions.PersonNotFoundException;
import seedu.address.testutil.PersonBuilder;

public class UniquePersonListTest {

    private final UniquePersonList uniquePersonList = new UniquePersonList();

    @Test
    public void contains_nullPerson_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> uniquePersonList.contains(null));
    }

    @Test
    public void contains_personNotInList_returnsFalse() {
        assertFalse(uniquePersonList.contains(ALICE));
    }

    @Test
    public void contains_personInList_returnsTrue() {
        uniquePersonList.add(ALICE);
        assertTrue(uniquePersonList.contains(ALICE));
    }

    @Test
    public void contains_personWithSameIdentityFieldsInList_returnsTrue() {
        uniquePersonList.add(ALICE);
        Person editedAlice = new PersonBuilder(ALICE).withAddress(VALID_ADDRESS_BOB).withTags(VALID_TAG_HUSBAND)
                .build();
        assertTrue(uniquePersonList.contains(editedAlice));
    }

    @Test
    public void add_nullPerson_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> uniquePersonList.add(null));
    }

    @Test
    public void add_duplicatePerson_throwsDuplicatePersonException() {
        uniquePersonList.add(ALICE);
        assertThrows(DuplicatePersonException.class, () -> uniquePersonList.add(ALICE));
    }

    @Test
    public void setPerson_nullTargetPerson_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> uniquePersonList.setPerson(null, ALICE));
    }

    @Test
    public void setPerson_nullEditedPerson_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> uniquePersonList.setPerson(ALICE, null));
    }

    @Test
    public void setPerson_targetPersonNotInList_throwsPersonNotFoundException() {
        assertThrows(PersonNotFoundException.class, () -> uniquePersonList.setPerson(ALICE, ALICE));
    }

    @Test
    public void setPerson_editedPersonIsSamePerson_success() {
        uniquePersonList.add(ALICE);
        uniquePersonList.setPerson(ALICE, ALICE);
        UniquePersonList expectedUniquePersonList = new UniquePersonList();
        expectedUniquePersonList.add(ALICE);
        assertEquals(expectedUniquePersonList, uniquePersonList);
    }

    @Test
    public void setPerson_editedPersonHasSameIdentity_success() {
        uniquePersonList.add(ALICE);
        Person editedAlice = new PersonBuilder(ALICE).withAddress(VALID_ADDRESS_BOB).withTags(VALID_TAG_HUSBAND)
                .build();
        uniquePersonList.setPerson(ALICE, editedAlice);
        UniquePersonList expectedUniquePersonList = new UniquePersonList();
        expectedUniquePersonList.add(editedAlice);
        assertEquals(expectedUniquePersonList, uniquePersonList);
    }

    @Test
    public void setPerson_editedPersonHasDifferentIdentity_success() {
        uniquePersonList.add(ALICE);
        uniquePersonList.setPerson(ALICE, BOB);
        UniquePersonList expectedUniquePersonList = new UniquePersonList();
        expectedUniquePersonList.add(BOB);
        assertEquals(expectedUniquePersonList, uniquePersonList);
    }

    @Test
    public void setPerson_editedPersonHasNonUniqueIdentity_throwsDuplicatePersonException() {
        uniquePersonList.add(ALICE);
        uniquePersonList.add(BOB);
        assertThrows(DuplicatePersonException.class, () -> uniquePersonList.setPerson(ALICE, BOB));
    }

    @Test
    public void remove_nullPerson_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> uniquePersonList.remove(null));
    }

    @Test
    public void remove_personDoesNotExist_throwsPersonNotFoundException() {
        assertThrows(PersonNotFoundException.class, () -> uniquePersonList.remove(ALICE));
    }

    @Test
    public void remove_existingPerson_removesPerson() {
        uniquePersonList.add(ALICE);
        uniquePersonList.remove(ALICE);
        UniquePersonList expectedUniquePersonList = new UniquePersonList();
        assertEquals(expectedUniquePersonList, uniquePersonList);
    }

    @Test
    public void setPersons_nullUniquePersonList_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> uniquePersonList.setPersons((UniquePersonList) null));
    }

    @Test
    public void setPersons_uniquePersonList_replacesOwnListWithProvidedUniquePersonList() {
        uniquePersonList.add(ALICE);
        UniquePersonList expectedUniquePersonList = new UniquePersonList();
        expectedUniquePersonList.add(BOB);
        uniquePersonList.setPersons(expectedUniquePersonList);
        assertEquals(expectedUniquePersonList, uniquePersonList);
    }

    @Test
    public void setPersons_nullList_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> uniquePersonList.setPersons((List<Person>) null));
    }

    @Test
    public void setPersons_list_replacesOwnListWithProvidedList() {
        uniquePersonList.add(ALICE);
        List<Person> personList = List.of(BOB);
        uniquePersonList.setPersons(personList);
        UniquePersonList expectedUniquePersonList = new UniquePersonList();
        expectedUniquePersonList.add(BOB);
        assertEquals(expectedUniquePersonList, uniquePersonList);
    }

    @Test
    public void setPersons_listWithDuplicatePersons_throwsDuplicatePersonException() {
        List<Person> listWithDuplicatePersons = List.of(ALICE, ALICE);
        assertThrows(DuplicatePersonException.class, () -> uniquePersonList.setPersons(listWithDuplicatePersons));
    }

    @Test
    public void asUnmodifiableObservableList_modifyList_throwsUnsupportedOperationException() {
        assertThrows(UnsupportedOperationException.class, ()
            -> uniquePersonList.asUnmodifiableObservableList().remove(0));
    }

    @Test
    public void toStringMethod() {
        assertEquals(uniquePersonList.asUnmodifiableObservableList().toString(), uniquePersonList.toString());
    }

    @Test
    public void add_sameNameDifferentContacts_acceptsBothRecords() {
        Person otherAlice = new PersonBuilder(ALICE).withPhone("000").withEmail("other@example.com").build();
        uniquePersonList.add(ALICE);
        uniquePersonList.add(otherAlice);
        assertEquals(List.of(ALICE, otherAlice), uniquePersonList.asUnmodifiableObservableList());
    }

    @Test
    public void add_normalizedNameAndEmailConflict_rejectsWithoutChanges() {
        uniquePersonList.add(ALICE);
        Person duplicate = new PersonBuilder(ALICE).withName("ALICE   PAULINE").withPhone("000")
                .withEmail(ALICE.getEmail().value.toUpperCase(Locale.ROOT)).build();
        assertThrows(DuplicatePersonException.class, () -> uniquePersonList.add(duplicate));
        assertEquals(List.of(ALICE), uniquePersonList.asUnmodifiableObservableList());
    }

    @Test
    public void setPerson_matchesTargetAndAnotherRecord_rejectsWithoutChanges() {
        Person first = new PersonBuilder().withName("Rachel Lim").withPhone("111")
                .withEmail("one@example.com").build();
        Person second = new PersonBuilder(first).withPhone("222").withEmail("two@example.com").build();
        Person bridge = new PersonBuilder(first).withEmail("TWO@example.com").build();
        uniquePersonList.add(first);
        uniquePersonList.add(second);
        assertThrows(DuplicatePersonException.class, () -> uniquePersonList.setPerson(first, bridge));
        assertEquals(List.of(first, second), uniquePersonList.asUnmodifiableObservableList());
    }

    @Test
    public void setPersons_normalizedConflict_rejectsWithoutReplacingContents() {
        uniquePersonList.add(BOB);
        Person duplicate = new PersonBuilder(ALICE).withName("ALICE   PAULINE").build();
        assertThrows(DuplicatePersonException.class, () -> uniquePersonList.setPersons(List.of(ALICE, duplicate)));
        assertEquals(List.of(BOB), uniquePersonList.asUnmodifiableObservableList());
    }

    @Test
    public void setPersons_nonTransitiveConflictInAnyOrder_checksEveryPair() {
        Person first = new PersonBuilder().withName("Rachel Lim").withPhone("111")
                .withEmail("one@example.com").build();
        Person last = new PersonBuilder(first).withPhone("222").withEmail("two@example.com").build();
        Person bridge = new PersonBuilder(first).withEmail("two@example.com").build();
        uniquePersonList.setPersons(List.of(first, last));
        for (List<Person> order : List.of(List.of(first, last, bridge), List.of(bridge, first, last),
                List.of(last, bridge, first))) {
            assertThrows(DuplicatePersonException.class, () -> uniquePersonList.setPersons(order));
            assertEquals(List.of(first, last), uniquePersonList.asUnmodifiableObservableList());
        }
    }

    @Test
    public void setPerson_followUpOnlyReplacement_keepsCorrectSameNameRecord() {
        Person otherAlice = new PersonBuilder(ALICE).withPhone("000").withEmail("other@example.com").build();
        uniquePersonList.setPersons(List.of(ALICE, otherAlice));
        Person replacement = otherAlice.withFollowUp(new FollowUp(LocalDate.of(2026, 10, 9), "Call"));
        uniquePersonList.setPerson(new PersonBuilder(otherAlice).build(), replacement);
        assertEquals(List.of(ALICE, replacement), uniquePersonList.asUnmodifiableObservableList());
        uniquePersonList.remove(new PersonBuilder(ALICE).build());
        assertEquals(List.of(replacement), uniquePersonList.asUnmodifiableObservableList());
    }

    @Test
    public void add_differentNameSameContacts_acceptsBothRecords() {
        Person otherName = new PersonBuilder(ALICE).withName("Another Client").build();
        uniquePersonList.add(ALICE);
        uniquePersonList.add(otherName);
        assertEquals(List.of(ALICE, otherName), uniquePersonList.asUnmodifiableObservableList());
    }

}
