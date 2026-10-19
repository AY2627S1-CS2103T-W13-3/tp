package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.address.model.person.FollowUp;
import seedu.address.model.person.Person;
import seedu.address.testutil.PersonBuilder;

public class PendingFollowUpPanelTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 19);

    @Test
    public void createDisplay_emptyList_showsEmptyState() {
        PendingFollowUpPanel.PendingDisplay display = PendingFollowUpPanel.createDisplay(List.of(), TODAY, null);

        assertTrue(display.isEmpty());
        assertTrue(display.overdue().isEmpty());
        assertTrue(display.dueToday().isEmpty());
        assertTrue(display.upcoming().isEmpty());
    }

    @Test
    public void createDisplay_threeStatuses_preservesInputOrderWithinGroups() {
        Person old = person("Old", TODAY.minusDays(2), "Call old client");
        Person recent = person("Recent", TODAY.minusDays(1), "Call recent client");
        Person due = person("Due", TODAY, "Call today");
        Person later = person("Later", TODAY.plusDays(1), "Call later");
        List<Person> pending = List.of(old, recent, due, later);

        PendingFollowUpPanel.PendingDisplay display = PendingFollowUpPanel.createDisplay(pending, TODAY, due);

        assertEquals(List.of("Old", "Recent"), display.overdue().stream().map(card -> card.name()).toList());
        assertEquals("Due", display.dueToday().getFirst().name());
        assertEquals("2026-10-19", display.dueToday().getFirst().date());
        assertEquals("Call today", display.dueToday().getFirst().action());
        assertEquals("DUE TODAY", display.dueToday().getFirst().status());
        assertTrue(display.dueToday().getFirst().isViewing());
        assertEquals("Later", display.upcoming().getFirst().name());
        assertFalse(display.upcoming().getFirst().isViewing());
        assertEquals("pending-status-overdue", display.overdue().getFirst().statusStyleClass());
        assertEquals("pending-status-due-today", display.dueToday().getFirst().statusStyleClass());
        assertEquals("pending-status-upcoming", display.upcoming().getFirst().statusStyleClass());
    }

    @Test
    public void createDisplay_sameNames_marksOnlySelectedIdentity() {
        Person first = new PersonBuilder().withName("Alex Lee").withPhone("111")
                .withEmail("first@example.com").withFollowUp(new FollowUp(TODAY, "First action")).build();
        Person second = new PersonBuilder().withName("Alex Lee").withPhone("222")
                .withEmail("second@example.com").withFollowUp(new FollowUp(TODAY, "Second action")).build();
        Person equalCopy = new PersonBuilder().withName("Alex Lee").withPhone("222")
                .withEmail("second@example.com").withFollowUp(new FollowUp(TODAY, "Second action")).build();

        PendingFollowUpPanel.PendingDisplay selected = PendingFollowUpPanel.createDisplay(
                List.of(first, second), TODAY, second);
        PendingFollowUpPanel.PendingDisplay copied = PendingFollowUpPanel.createDisplay(
                List.of(first, second), TODAY, equalCopy);

        assertEquals(2, selected.dueToday().size());
        assertFalse(selected.dueToday().get(0).isViewing());
        assertTrue(selected.dueToday().get(1).isViewing());
        assertFalse(copied.dueToday().get(1).isViewing());
    }

    @Test
    public void createDisplay_dateAndListChanges_recomputeEveryCard() {
        String longAction = "x".repeat(200);
        Person original = person("Rachel", TODAY, "Old action");
        Person replacement = original.withFollowUp(new FollowUp(TODAY.plusDays(2), longAction));
        List<Person> pending = new ArrayList<>(List.of(original));

        PendingFollowUpPanel.PendingDisplay due = PendingFollowUpPanel.createDisplay(pending, TODAY, original);
        pending.set(0, replacement);
        PendingFollowUpPanel.PendingDisplay upcoming = PendingFollowUpPanel.createDisplay(pending, TODAY, replacement);
        PendingFollowUpPanel.PendingDisplay overdue = PendingFollowUpPanel.createDisplay(
                pending, TODAY.plusDays(3), replacement);
        pending.clear();
        PendingFollowUpPanel.PendingDisplay empty = PendingFollowUpPanel.createDisplay(pending, TODAY, null);

        assertEquals("Old action", due.dueToday().getFirst().action());
        assertEquals(longAction, upcoming.upcoming().getFirst().action());
        assertTrue(upcoming.dueToday().isEmpty());
        assertEquals("OVERDUE", overdue.overdue().getFirst().status());
        assertTrue(empty.isEmpty());
    }

    @Test
    public void createDisplay_reorderedTie_usesFeedOrderWithoutAddingIndices() {
        Person first = person("First", TODAY.plusDays(1), "First action");
        Person second = person("Second", TODAY.plusDays(1), "Second action");
        List<Person> pending = new ArrayList<>(List.of(first, second));

        PendingFollowUpPanel.PendingDisplay before = PendingFollowUpPanel.createDisplay(pending, TODAY, null);
        pending.set(0, second);
        pending.set(1, first);
        PendingFollowUpPanel.PendingDisplay after = PendingFollowUpPanel.createDisplay(pending, TODAY, null);

        assertEquals(List.of("First", "Second"), before.upcoming().stream().map(card -> card.name()).toList());
        assertEquals(List.of("Second", "First"), after.upcoming().stream().map(card -> card.name()).toList());
    }

    @Test
    public void createDisplay_invalidDependenciesOrMissingAction_rejected() {
        Person noAction = new PersonBuilder().build();
        List<Person> invalidPending = List.of(noAction);

        assertThrows(NullPointerException.class, () -> PendingFollowUpPanel.createDisplay(null, TODAY, null));
        assertThrows(NullPointerException.class, () -> PendingFollowUpPanel.createDisplay(List.of(), null, null));
        assertThrows(IllegalArgumentException.class, () ->
                PendingFollowUpPanel.createDisplay(invalidPending, TODAY, null));
    }

    private Person person(String name, LocalDate date, String action) {
        return new PersonBuilder().withName(name).withFollowUp(new FollowUp(date, action)).build();
    }
}
