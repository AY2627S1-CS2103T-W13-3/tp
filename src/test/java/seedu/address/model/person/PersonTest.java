package seedu.address.model.person;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.logic.commands.CommandTestUtil.VALID_ADDRESS_BOB;
import static seedu.address.logic.commands.CommandTestUtil.VALID_EMAIL_BOB;
import static seedu.address.logic.commands.CommandTestUtil.VALID_NAME_BOB;
import static seedu.address.logic.commands.CommandTestUtil.VALID_PHONE_BOB;
import static seedu.address.logic.commands.CommandTestUtil.VALID_TAG_HUSBAND;
import static seedu.address.testutil.Assert.assertThrows;
import static seedu.address.testutil.TypicalPersons.ALICE;
import static seedu.address.testutil.TypicalPersons.BOB;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import seedu.address.model.AddressBook;
import seedu.address.model.tag.Tag;
import seedu.address.testutil.PersonBuilder;

public class PersonTest {

    @Test
    public void constructor_legacySignature_hasNoFollowUp() {
        assertEquals(Optional.empty(), ALICE.getFollowUp());
    }

    @Test
    public void constructor_nullFollowUpOptional_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> new Person(ALICE.getName(), ALICE.getPhone(),
                ALICE.getEmail(), ALICE.getAddress(), ALICE.getTags(), null));
    }

    @Test
    public void withFollowUp_addReplaceClear_preservesContactAndSource() {
        FollowUp first = new FollowUp(LocalDate.of(2026, 10, 5), "Call client");
        FollowUp replacement = new FollowUp(LocalDate.of(2026, 10, 6), "Send quotation");
        Person withFirst = ALICE.withFollowUp(first);
        Person withReplacement = withFirst.withFollowUp(replacement);
        Person cleared = withReplacement.withoutFollowUp();

        assertEquals(Optional.empty(), ALICE.getFollowUp());
        assertEquals(Optional.of(first), withFirst.getFollowUp());
        assertEquals(Optional.of(replacement), withReplacement.getFollowUp());
        assertEquals(ALICE, withFirst.withoutFollowUp());
        assertEquals(ALICE, cleared);
        assertEquals(ALICE, ALICE.withoutFollowUp());
        assertThrows(NullPointerException.class, () -> ALICE.withFollowUp(null));
    }

    @Test
    public void constructor_mutableTags_defensivelyCopiesTags() {
        Set<Tag> tags = new HashSet<>(ALICE.getTags());
        Person person = new Person(ALICE.getName(), ALICE.getPhone(), ALICE.getEmail(), ALICE.getAddress(),
                tags, Optional.of(new FollowUp(LocalDate.of(2026, 10, 5), "Call client")));
        tags.clear();
        assertEquals(ALICE.getTags(), person.getTags());
        assertThrows(UnsupportedOperationException.class, () -> person.getTags().clear());
    }

    @Test
    public void equals_followUpChangesValueButNotIdentity() {
        FollowUp action = new FollowUp(LocalDate.of(2026, 10, 5), "Call client");
        Person first = ALICE.withFollowUp(action);
        Person equalCopy = ALICE.withFollowUp(new FollowUp(LocalDate.of(2026, 10, 5), "Call client"));
        Person changedDate = ALICE.withFollowUp(new FollowUp(LocalDate.of(2026, 10, 6), "Call client"));
        Person changedDescription = ALICE.withFollowUp(new FollowUp(LocalDate.of(2026, 10, 5), "Send quotation"));

        assertEquals(first, equalCopy);
        assertEquals(first.hashCode(), equalCopy.hashCode());
        assertFalse(first.equals(ALICE));
        assertFalse(ALICE.equals(first));
        assertFalse(first.equals(changedDate));
        assertFalse(first.equals(changedDescription));
        assertTrue(ALICE.isSamePerson(first));
        assertTrue(first.isSamePerson(ALICE));
        assertTrue(first.isSamePerson(changedDate));
    }

    @Test
    public void personBuilder_copiesAndClearsFollowUp() {
        FollowUp action = new FollowUp(LocalDate.of(2026, 10, 5), "Call client");
        Person original = ALICE.withFollowUp(action);
        assertEquals(original, new PersonBuilder(original).build());
        assertEquals(original, new PersonBuilder(ALICE).withFollowUp(action).build());
        assertEquals(ALICE, new PersonBuilder(original).withoutFollowUp().build());
        assertEquals(Optional.empty(), new PersonBuilder().build().getFollowUp());
    }

    @Test
    public void addressBook_copyRetainsFollowUpAndRemainsIndependent() {
        Person original = ALICE.withFollowUp(new FollowUp(LocalDate.of(2026, 10, 5), "Call client"));
        AddressBook source = new AddressBook();
        source.addPerson(original);
        AddressBook copy = new AddressBook(source);
        assertEquals(original, copy.getPersonList().getFirst());
        copy.setPerson(original, original.withoutFollowUp());
        assertEquals(original, source.getPersonList().getFirst());
        assertEquals(ALICE, copy.getPersonList().getFirst());
    }

    @Test
    public void asObservableList_modifyList_throwsUnsupportedOperationException() {
        Person person = new PersonBuilder().build();
        assertThrows(UnsupportedOperationException.class, () -> person.getTags().remove(0));
    }

    @Test
    public void isSamePerson() {
        // same object -> returns true
        assertTrue(ALICE.isSamePerson(ALICE));

        // null -> returns false
        assertFalse(ALICE.isSamePerson(null));

        // same name, all other attributes different -> returns true
        Person editedAlice = new PersonBuilder(ALICE).withPhone(VALID_PHONE_BOB).withEmail(VALID_EMAIL_BOB)
                .withAddress(VALID_ADDRESS_BOB).withTags(VALID_TAG_HUSBAND).build();
        assertTrue(ALICE.isSamePerson(editedAlice));

        // different name, all other attributes same -> returns false
        editedAlice = new PersonBuilder(ALICE).withName(VALID_NAME_BOB).build();
        assertFalse(ALICE.isSamePerson(editedAlice));

        // name differs in case, all other attributes same -> returns false
        Person editedBob = new PersonBuilder(BOB).withName(VALID_NAME_BOB.toLowerCase()).build();
        assertFalse(BOB.isSamePerson(editedBob));

        // name has trailing spaces, all other attributes same -> returns false
        String nameWithTrailingSpaces = VALID_NAME_BOB + " ";
        editedBob = new PersonBuilder(BOB).withName(nameWithTrailingSpaces).build();
        assertFalse(BOB.isSamePerson(editedBob));
    }

    @Test
    public void equals() {
        // same values -> returns true
        Person aliceCopy = new PersonBuilder(ALICE).build();
        assertTrue(ALICE.equals(aliceCopy));

        // same object -> returns true
        assertTrue(ALICE.equals(ALICE));

        // null -> returns false
        assertFalse(ALICE.equals(null));

        // different type -> returns false
        assertFalse(ALICE.equals(5));

        // different person -> returns false
        assertFalse(ALICE.equals(BOB));

        // different name -> returns false
        Person editedAlice = new PersonBuilder(ALICE).withName(VALID_NAME_BOB).build();
        assertFalse(ALICE.equals(editedAlice));

        // different phone -> returns false
        editedAlice = new PersonBuilder(ALICE).withPhone(VALID_PHONE_BOB).build();
        assertFalse(ALICE.equals(editedAlice));

        // different email -> returns false
        editedAlice = new PersonBuilder(ALICE).withEmail(VALID_EMAIL_BOB).build();
        assertFalse(ALICE.equals(editedAlice));

        // different address -> returns false
        editedAlice = new PersonBuilder(ALICE).withAddress(VALID_ADDRESS_BOB).build();
        assertFalse(ALICE.equals(editedAlice));

        // different tags -> returns false
        editedAlice = new PersonBuilder(ALICE).withTags(VALID_TAG_HUSBAND).build();
        assertFalse(ALICE.equals(editedAlice));
    }

    @Test
    public void toStringMethod() {
        String expected = Person.class.getCanonicalName() + "{name=" + ALICE.getName() + ", phone=" + ALICE.getPhone()
                + ", email=" + ALICE.getEmail() + ", address=" + ALICE.getAddress() + ", tags=" + ALICE.getTags() + "}";
        assertEquals(expected, ALICE.toString());
    }
}
