package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import seedu.address.model.person.FollowUp;
import seedu.address.model.person.Person;
import seedu.address.testutil.PersonBuilder;

public class FollowUpPersonCardTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 12);

    @Test
    public void createFollowUpDisplay_ordinaryRowWithFollowUp_showsNoFollowUpDetails() {
        Person person = personWithFollowUp(TODAY, "Call client");

        PersonCard.FollowUpDisplay display = PersonCard.createFollowUpDisplay(person, TODAY, false);

        assertFalse(display.hasFollowUp());
        assertEquals("", display.dueText());
        assertEquals("", display.actionText());
        assertEquals("", display.statusText());
        assertEquals("", display.statusStyleClass());
    }

    @Test
    public void createFollowUpDisplay_ordinaryRowWithoutFollowUp_showsNoNoneSummary() {
        Person person = new PersonBuilder().withoutFollowUp().build();

        PersonCard.FollowUpDisplay display = PersonCard.createFollowUpDisplay(person, TODAY, false);

        assertFalse(display.hasFollowUp());
        assertEquals("", display.dueText());
    }

    @Test
    public void createFollowUpDisplay_pendingRowWithoutFollowUp_showsNothing() {
        Person person = new PersonBuilder().withoutFollowUp().build();

        assertFalse(PersonCard.createFollowUpDisplay(person, TODAY, true).hasFollowUp());
    }

    @Test
    public void createFollowUpDisplay_overdueFollowUp_showsExactFieldsAndStatus() {
        Person person = personWithFollowUp(TODAY.minusDays(1), "Call client");

        PersonCard.FollowUpDisplay display = PersonCard.createFollowUpDisplay(person, TODAY, true);

        assertTrue(display.hasFollowUp());
        assertEquals("Due: 2026-10-11", display.dueText());
        assertEquals("Action: Call client", display.actionText());
        assertEquals("OVERDUE", display.statusText());
        assertEquals("follow-up-status-overdue", display.statusStyleClass());
    }

    @Test
    public void createFollowUpDisplay_dueTodayFollowUp_showsDueTodayStatus() {
        Person person = personWithFollowUp(TODAY, "Send quotation");

        PersonCard.FollowUpDisplay display = PersonCard.createFollowUpDisplay(person, TODAY, true);

        assertEquals("Due: 2026-10-12", display.dueText());
        assertEquals("Action: Send quotation", display.actionText());
        assertEquals("DUE TODAY", display.statusText());
        assertEquals("follow-up-status-due-today", display.statusStyleClass());
    }

    @Test
    public void createFollowUpDisplay_upcomingFollowUp_showsUpcomingStatus() {
        Person person = personWithFollowUp(TODAY.plusDays(1), "Review policy");

        PersonCard.FollowUpDisplay display = PersonCard.createFollowUpDisplay(person, TODAY, true);

        assertEquals("Due: 2026-10-13", display.dueText());
        assertEquals("Action: Review policy", display.actionText());
        assertEquals("UPCOMING", display.statusText());
        assertEquals("follow-up-status-upcoming", display.statusStyleClass());
    }

    @Test
    public void createFollowUpDisplay_suppliedDateChanges_recalculatesWithoutChangingFollowUp() {
        Person person = personWithFollowUp(TODAY, "Call client");

        assertEquals("UPCOMING", PersonCard.createFollowUpDisplay(person, TODAY.minusDays(1), true).statusText());
        assertEquals("DUE TODAY", PersonCard.createFollowUpDisplay(person, TODAY, true).statusText());
        assertEquals("OVERDUE", PersonCard.createFollowUpDisplay(person, TODAY.plusDays(1), true).statusText());
        assertEquals(TODAY, person.getFollowUp().orElseThrow().getDueDate());
    }

    @Test
    public void createFollowUpDisplay_maximumDescription_preservesCompleteText() {
        String description = "A".repeat(200);
        Person person = personWithFollowUp(TODAY.plusDays(1), description);

        PersonCard.FollowUpDisplay display = PersonCard.createFollowUpDisplay(person, TODAY, true);

        assertEquals("Action: " + description, display.actionText());
    }

    private Person personWithFollowUp(LocalDate dueDate, String description) {
        return new PersonBuilder().withFollowUp(new FollowUp(dueDate, description)).build();
    }
}
