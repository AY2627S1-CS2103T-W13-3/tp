package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import seedu.address.model.person.FollowUp;
import seedu.address.model.person.Person;
import seedu.address.testutil.PersonBuilder;

public class ClientProfilePanelTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 12);

    @Test
    public void createDisplay_noSelection_showsOnlyEmptyMessage() {
        ClientProfilePanel.ProfileDisplay display = ClientProfilePanel.createDisplay(null, TODAY);

        assertFalse(display.hasSelection());
        assertEquals("No client selected.", display.noSelection());
        assertEquals("", display.name());
        assertEquals("", display.phone());
        assertEquals("", display.followUp().none());
    }

    @Test
    public void createDisplay_selectedWithoutAction_showsContactAndNone() {
        Person person = new PersonBuilder().withName("Rachel Lim").withPhone("001")
                .withEmail("Rachel@example.com").withAddress("Blk 123, Main Street")
                .withTags("health", "Active", "2026").build();

        ClientProfilePanel.ProfileDisplay display = ClientProfilePanel.createDisplay(person, TODAY);

        assertTrue(display.hasSelection());
        assertEquals("", display.noSelection());
        assertEquals("Rachel Lim", display.name());
        assertEquals("Phone: 001", display.phone());
        assertEquals("Email: Rachel@example.com", display.email());
        assertEquals("Address: Blk 123, Main Street", display.address());
        assertEquals("Tags: [2026] [active] [health]", display.tags());
        assertFalse(display.followUp().hasFollowUp());
        assertEquals("Follow-up: None", display.followUp().none());
        assertEquals("", display.followUp().date());
        assertEquals("", display.followUp().action());
        assertEquals("", display.followUp().status());
    }

    @Test
    public void createDisplay_selectedWithAction_usesSuppliedDateForStatus() {
        Person person = new PersonBuilder().withFollowUp(new FollowUp(TODAY, "Call client")).build();

        ClientProfilePanel.ProfileDisplay due = ClientProfilePanel.createDisplay(person, TODAY);
        ClientProfilePanel.ProfileDisplay overdue = ClientProfilePanel.createDisplay(person, TODAY.plusDays(1));
        ClientProfilePanel.ProfileDisplay upcoming = ClientProfilePanel.createDisplay(person, TODAY.minusDays(1));

        assertEquals("Tags: None", due.tags());
        assertTrue(due.followUp().hasFollowUp());
        assertEquals("", due.followUp().none());
        assertEquals("Follow-up date: 2026-10-12", due.followUp().date());
        assertEquals("Follow-up action: Call client", due.followUp().action());
        assertEquals("Status: DUE TODAY", due.followUp().status());
        assertEquals("Status: OVERDUE", overdue.followUp().status());
        assertEquals("Status: UPCOMING", upcoming.followUp().status());
        assertEquals(TODAY, person.getFollowUp().orElseThrow().getDueDate());
    }

    @Test
    public void createDisplay_replacementAndClearing_doNotRetainOldFields() {
        Person withAction = new PersonBuilder().withFollowUp(new FollowUp(TODAY, "Call client")).build();
        Person withoutAction = withAction.withoutFollowUp();

        ClientProfilePanel.ProfileDisplay cleared = ClientProfilePanel.createDisplay(withoutAction, TODAY);
        ClientProfilePanel.ProfileDisplay deselected = ClientProfilePanel.createDisplay(null, TODAY);

        assertEquals("Follow-up: None", cleared.followUp().none());
        assertEquals("", cleared.followUp().date());
        assertEquals("", cleared.followUp().action());
        assertEquals("", cleared.followUp().status());
        assertFalse(deselected.hasSelection());
        assertEquals("", deselected.name());
        assertEquals("", deselected.address());
    }
}
