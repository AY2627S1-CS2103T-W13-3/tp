package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.GraphicsEnvironment;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
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
    public void profileUi_selectionAndDateChanges_clearOldRenderedValues() throws InterruptedException {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        runOnFxThread(() -> {
            Person first = new PersonBuilder().withName("First Client").withTags("health", "Active")
                    .withFollowUp(new FollowUp(TODAY, "First action")).build();
            SimpleObjectProperty<Person> selected = new SimpleObjectProperty<>(first);
            SimpleObjectProperty<LocalDate> today = new SimpleObjectProperty<>(TODAY);
            ClientProfilePanel panel = new ClientProfilePanel(selected, today);
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

            String longAction = "x".repeat(200);
            selected.set(new PersonBuilder().withName("Second Client").withTags("travel")
                    .withFollowUp(new FollowUp(TODAY.plusDays(5), longAction)).build());
            assertEquals(List.of("travel"), chips.getChildren().stream()
                    .map(node -> ((Label) node).getText()).toList());
            assertEquals("UPCOMING", status.getText());
            assertEquals(longAction, action.getText());
            assertTrue(action.isWrapText());

            selected.set(null);
            assertTrue(chips.getChildren().isEmpty());
            assertEquals("", action.getText());
            assertTrue(noSelection.isVisible());
        });
    }

    private static Node findById(Node node, String id) {
        if (id.equals(node.getId())) {
            return node;
        }
        if (node instanceof ScrollPane scrollPane) {
            return findById(scrollPane.getContent(), id);
        }
        if (node instanceof Parent parent) {
            for (Node child : parent.getChildrenUnmodifiable()) {
                Node match = findById(child, id);
                if (match != null) {
                    return match;
                }
            }
        }
        return null;
    }

    private static void runOnFxThread(Runnable task) throws InterruptedException {
        synchronized (Platform.class) {
            try {
                Platform.startup(() -> { });
            } catch (IllegalStateException alreadyStarted) {
                // Another UI test has initialized the toolkit in this test JVM.
            }
        }
        CountDownLatch completed = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                task.run();
            } catch (Throwable error) {
                failure.set(error);
            } finally {
                completed.countDown();
            }
        });
        assertTrue(completed.await(10, TimeUnit.SECONDS));
        if (failure.get() != null) {
            throw new AssertionError("JavaFX profile rendering failed", failure.get());
        }
    }
}
