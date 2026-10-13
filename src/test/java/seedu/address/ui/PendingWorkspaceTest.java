package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import seedu.address.model.person.Person;
import seedu.address.testutil.PersonBuilder;

public class PendingWorkspaceTest {

    @Test
    public void isViewed_sameRecord_true() {
        Person person = new PersonBuilder().build();

        assertTrue(PersonListPanel.isViewed(person, person));
    }

    @Test
    public void isViewed_noSelection_false() {
        assertFalse(PersonListPanel.isViewed(new PersonBuilder().build(), null));
    }

    @Test
    public void isViewed_emptyRow_false() {
        assertFalse(PersonListPanel.isViewed(null, null));
        assertFalse(PersonListPanel.isViewed(null, new PersonBuilder().build()));
    }

    @Test
    public void isViewed_sameNameDifferentClient_false() {
        Person viewed = new PersonBuilder().withName("Alex Tan").withPhone("91234567")
                .withEmail("alex1@example.com").build();
        Person sameName = new PersonBuilder().withName("Alex Tan").withPhone("98765432")
                .withEmail("alex2@example.com").build();

        assertFalse(PersonListPanel.isViewed(sameName, viewed));
    }

    @Test
    public void isViewed_equalValuesDifferentRecord_false() {
        Person viewed = new PersonBuilder().build();
        Person copy = new PersonBuilder(viewed).build();
        assertEquals(viewed, copy);
        assertNotSame(viewed, copy);

        assertFalse(PersonListPanel.isViewed(copy, viewed));
    }
}
