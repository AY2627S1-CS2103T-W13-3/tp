package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.JavaFxTestUtil.findById;
import static seedu.address.testutil.JavaFxTestUtil.runOnFxThread;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import seedu.address.model.AddressBook;
import seedu.address.model.person.FollowUp;
import seedu.address.model.person.Person;
import seedu.address.testutil.JavaFxTestUtil;
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
        assertEquals("001", display.phone());
        assertEquals("Rachel@example.com", display.email());
        assertEquals("Blk 123, Main Street", display.address());
        assertEquals(List.of("2026", "active", "health"), display.tags());
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

        assertEquals(List.of(), due.tags());
        assertTrue(due.followUp().hasFollowUp());
        assertEquals("", due.followUp().none());
        assertEquals("2026-10-12", due.followUp().date());
        assertEquals("Call client", due.followUp().action());
        assertEquals("DUE TODAY", due.followUp().status());
        assertEquals("OVERDUE", overdue.followUp().status());
        assertEquals("UPCOMING", upcoming.followUp().status());
        assertEquals("profile-status-due-today", due.followUp().statusStyleClass());
        assertEquals("profile-status-overdue", overdue.followUp().statusStyleClass());
        assertEquals("profile-status-upcoming", upcoming.followUp().statusStyleClass());
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
        assertEquals(List.of(), deselected.tags());
    }

    @Test
    public void createDisplay_replacedActionAndLongText_keepCompleteAction() {
        String longAction = "x".repeat(200);
        Person original = new PersonBuilder().withFollowUp(new FollowUp(TODAY, "Old action")).build();
        Person replaced = original.withFollowUp(new FollowUp(TODAY.plusDays(1), longAction));

        ClientProfilePanel.ProfileDisplay display = ClientProfilePanel.createDisplay(replaced, TODAY);

        assertEquals(longAction, display.followUp().action());
        assertEquals("2026-10-13", display.followUp().date());
        assertEquals("UPCOMING", display.followUp().status());
    }

    @Test
    public void createDisplay_nullPublishedDate_rejected() {
        assertThrows(NullPointerException.class, () -> ClientProfilePanel.createDisplay(null, null));
    }

    @Test
    public void profileUi_selectionAndDateChanges_clearOldRenderedValues() throws Exception {
        JavaFxTestUtil.initializeToolkit();
        runOnFxThread(() -> {
            Person first = new PersonBuilder().withName("First Client").withTags("health", "Active")
                    .withFollowUp(new FollowUp(TODAY, "First action")).build();
            SimpleObjectProperty<Person> selected = new SimpleObjectProperty<>(first);
            SimpleObjectProperty<LocalDate> today = new SimpleObjectProperty<>(TODAY);
            ClientProfilePanel panel = new ClientProfilePanel(selected, today, new AddressBook()::getClientId);
            FlowPane chips = (FlowPane) findById(panel.getRoot(), "tagChips");
            Label status = (Label) findById(panel.getRoot(), "followUpStatus");
            Label action = (Label) findById(panel.getRoot(), "followUpAction");
            Label noSelection = (Label) findById(panel.getRoot(), "noSelectionMessage");

            assertEquals(List.of("active", "health"), chips.getChildren().stream()
                    .map(node -> ((Label) node).getText()).toList());
            assertEquals("DUE TODAY", status.getText());

            today.set(TODAY.plusDays(1));
            assertEquals("OVERDUE", status.getText());
            assertTrue(status.getStyleClass().contains("profile-status-overdue"));
            assertFalse(status.getStyleClass().contains("profile-status-due-today"));

            String longAction = "续保电话 👩\u200d💻 " + "x".repeat(191);
            selected.set(new PersonBuilder().withName("Second Client").withTags("travel")
                    .withFollowUp(new FollowUp(TODAY.plusDays(5), longAction)).build());
            assertEquals(List.of("travel"), chips.getChildren().stream()
                    .map(node -> ((Label) node).getText()).toList());
            assertEquals("UPCOMING", status.getText());
            assertEquals(longAction, action.getText());
            assertTrue(action.isWrapText());
            new Scene(panel.getRoot(), 240, 360);
            panel.getRoot().applyCss();
            assertTrue(action.prefHeight(160) > action.prefHeight(1000));

            selected.set(null);
            assertTrue(chips.getChildren().isEmpty());
            assertEquals("", action.getText());
            assertTrue(noSelection.isVisible());
        });
    }

    @Test
    public void profileUi_longTag_staysInsideNarrowViewport() throws Exception {
        JavaFxTestUtil.initializeToolkit();
        runOnFxThread(() -> {
            String longTag = "a".repeat(80);
            Person person = new PersonBuilder().withTags(longTag, "short").build();
            ClientProfilePanel panel = new ClientProfilePanel(new SimpleObjectProperty<>(person),
                    new SimpleObjectProperty<>(TODAY), new AddressBook()::getClientId);
            ScrollPane scroll = (ScrollPane) panel.getRoot();
            new Scene(scroll, 240, 360);
            scroll.resize(240, 360);
            scroll.applyCss();
            scroll.layout();
            scroll.layout();
            FlowPane chips = (FlowPane) findById(scroll, "tagChips");
            Label chip = (Label) chips.getChildren().getFirst();

            assertEquals(longTag, chip.getText());
            assertTrue(scroll.getContent().getLayoutBounds().getWidth() <= scroll.getViewportBounds().getWidth() + 1,
                    "Content width " + scroll.getContent().getLayoutBounds().getWidth()
                            + " exceeds viewport " + scroll.getViewportBounds().getWidth());
            assertTrue(chip.getWidth() <= chips.getWidth() + 1);
            assertTrue(chip.getHeight() >= chip.prefHeight(chip.getWidth()) - 1);
            assertTrue(chip.prefHeight(120) > chip.prefHeight(1000));
            Node nextChip = chips.getChildren().get(1);
            assertTrue(nextChip.getBoundsInParent().getMinY() >= chip.getBoundsInParent().getMaxY(),
                    "Long chip " + chip.getBoundsInParent() + "; next chip " + nextChip.getBoundsInParent());
            assertTrue(nextChip.getBoundsInParent().getMaxY() <= chips.getHeight() + 1);

            scroll.resize(360, 360);
            scroll.layout();
            assertTrue(chip.getWidth() <= chips.getWidth() + 1);
            assertEquals(longTag, chip.getText());
        });
    }

}
