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

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 7);

    @Test
    public void createFollowUpDisplay_personWithoutFollowUp_showsNone() {
        Person person = new PersonBuilder().withoutFollowUp().build();

        PersonCard.FollowUpDisplay display = PersonCard.createFollowUpDisplay(person, TODAY);

        assertFalse(display.hasFollowUp());
        assertEquals("Follow-up: None", display.noneText());
        assertEquals("", display.dueText());
        assertEquals("", display.actionText());
        assertEquals("", display.statusText());
        assertEquals("", display.statusStyleClass());
    }

    @Test
    public void createFollowUpDisplay_overdueFollowUp_showsExactFieldsAndStatus() {
        Person person = personWithFollowUp(TODAY.minusDays(1), "Call client");

        PersonCard.FollowUpDisplay display = PersonCard.createFollowUpDisplay(person, TODAY);

        assertTrue(display.hasFollowUp());
        assertEquals("Due: 2026-10-06", display.dueText());
        assertEquals("Action: Call client", display.actionText());
        assertEquals("Status: OVERDUE", display.statusText());
        assertEquals("follow-up-status-overdue", display.statusStyleClass());
    }

    @Test
    public void createFollowUpDisplay_dueTodayFollowUp_showsDueTodayStatus() {
        Person person = personWithFollowUp(TODAY, "Send quotation");

        PersonCard.FollowUpDisplay display = PersonCard.createFollowUpDisplay(person, TODAY);

        assertEquals("Due: 2026-10-07", display.dueText());
        assertEquals("Action: Send quotation", display.actionText());
        assertEquals("Status: DUE TODAY", display.statusText());
        assertEquals("follow-up-status-due-today", display.statusStyleClass());
    }

    @Test
    public void createFollowUpDisplay_upcomingFollowUp_showsUpcomingStatus() {
        Person person = personWithFollowUp(TODAY.plusDays(1), "Review policy");

        PersonCard.FollowUpDisplay display = PersonCard.createFollowUpDisplay(person, TODAY);

        assertEquals("Due: 2026-10-08", display.dueText());
        assertEquals("Action: Review policy", display.actionText());
        assertEquals("Status: UPCOMING", display.statusText());
        assertEquals("follow-up-status-upcoming", display.statusStyleClass());
    }

    @Test
    public void createFollowUpDisplay_suppliedDateChanges_recalculatesWithoutChangingFollowUp() {
        Person person = personWithFollowUp(TODAY, "Call client");

        assertEquals("Status: UPCOMING", PersonCard.createFollowUpDisplay(person, TODAY.minusDays(1)).statusText());
        assertEquals("Status: DUE TODAY", PersonCard.createFollowUpDisplay(person, TODAY).statusText());
        assertEquals("Status: OVERDUE", PersonCard.createFollowUpDisplay(person, TODAY.plusDays(1)).statusText());
        assertEquals(TODAY, person.getFollowUp().orElseThrow().getDueDate());
    }

    @Test
    public void createFollowUpDisplay_maximumDescription_preservesCompleteText() {
        String description = "A".repeat(200);
        Person person = personWithFollowUp(TODAY.plusDays(1), description);

        PersonCard.FollowUpDisplay display = PersonCard.createFollowUpDisplay(person, TODAY);

        assertEquals("Action: " + description, display.actionText());
    }

    private Person personWithFollowUp(LocalDate dueDate, String description) {
        return new PersonBuilder().withFollowUp(new FollowUp(dueDate, description)).build();
    }
}
