package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.GraphicsEnvironment;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;
import seedu.address.model.AddressBook;
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

    @Test
    public void panelUi_observableChangesAndDispose_updateOnlyWhileSubscribed() throws InterruptedException {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        runOnFxThread(() -> {
            Person due = person("Due Client", TODAY, "Call today");
            Person later = person("Later Client", TODAY.plusDays(1), "Call later");
            ObservableList<Person> pending = FXCollections.observableArrayList(due, later);
            SimpleObjectProperty<LocalDate> today = new SimpleObjectProperty<>(TODAY);
            SimpleObjectProperty<Person> selected = new SimpleObjectProperty<>(due);
            PendingFollowUpPanel panel = new PendingFollowUpPanel(pending, today, selected,
                    new AddressBook()::getClientId);
            VBox dueCards = (VBox) findById(panel.getRoot(), "dueTodayCards");
            VBox upcomingCards = (VBox) findById(panel.getRoot(), "upcomingCards");
            VBox overdueCards = (VBox) findById(panel.getRoot(), "overdueCards");
            Label empty = (Label) findById(panel.getRoot(), "emptyMessage");

            assertEquals(1, dueCards.getChildren().size());
            assertEquals(1, upcomingCards.getChildren().size());
            assertFalse(empty.isVisible());
            VBox dueCard = (VBox) dueCards.getChildren().getFirst();
            VBox laterCard = (VBox) upcomingCards.getChildren().getFirst();

            selected.set(later);
            assertSame(dueCard, dueCards.getChildren().getFirst());
            assertSame(laterCard, upcomingCards.getChildren().getFirst());
            assertFalse(((Label) dueCard.getChildren().get(1)).isVisible());
            assertTrue(((Label) laterCard.getChildren().get(1)).isVisible());

            Person sameDay = person("Another Due", TODAY, "Another action");
            pending.add(1, sameDay);
            assertEquals(2, dueCards.getChildren().size());
            pending.setAll(sameDay, due, later);
            VBox firstDueCard = (VBox) dueCards.getChildren().getFirst();
            assertEquals("Another Due", ((Label) firstDueCard.getChildren().getFirst()).getText());

            today.set(TODAY.plusDays(2));
            assertEquals(3, overdueCards.getChildren().size());
            assertTrue(dueCards.getChildren().isEmpty());
            assertTrue(upcomingCards.getChildren().isEmpty());

            String longAction = "x".repeat(200);
            Person replacement = sameDay.withFollowUp(new FollowUp(TODAY.plusDays(4), longAction));
            pending.set(0, replacement);
            pending.clear();
            assertTrue(empty.isVisible());
            pending.add(replacement);
            assertFalse(empty.isVisible());
            assertEquals(1, upcomingCards.getChildren().size());
            VBox replacementCard = (VBox) upcomingCards.getChildren().getFirst();
            Label action = (Label) replacementCard.getChildren().get(3);
            assertEquals(longAction, action.getText());
            assertTrue(action.isWrapText());
            assertTrue(((ScrollPane) panel.getRoot()).isFitToWidth());
            new Scene(panel.getRoot(), 240, 360);
            panel.getRoot().applyCss();
            assertTrue(action.prefHeight(160) > action.prefHeight(1000));

            panel.dispose();
            panel.dispose();
            pending.clear();
            selected.set(null);
            today.set(TODAY.plusDays(5));
            assertSame(replacementCard, upcomingCards.getChildren().getFirst());
            assertEquals(1, upcomingCards.getChildren().size());
        });
    }

    @Test
    public void panelUi_equalValueReplacement_followsSelectedIdentity() throws InterruptedException {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        runOnFxThread(() -> {
            Person original = person("Client", TODAY, "Call client");
            Person replacement = new PersonBuilder(original).build();
            ObservableList<Person> pending = FXCollections.observableArrayList(original);
            SimpleObjectProperty<Person> selected = new SimpleObjectProperty<>(original);
            PendingFollowUpPanel panel = new PendingFollowUpPanel(pending,
                    new SimpleObjectProperty<>(TODAY), selected, new AddressBook()::getClientId);
            VBox cards = (VBox) findById(panel.getRoot(), "dueTodayCards");

            pending.set(0, replacement);
            VBox card = (VBox) cards.getChildren().getFirst();
            assertFalse(card.getStyleClass().contains("pending-card-viewing"));
            selected.set(replacement);
            assertSame(card, cards.getChildren().getFirst());
            assertTrue(card.getStyleClass().contains("pending-card-viewing"));
            assertTrue(((Label) card.getChildren().get(1)).isVisible());

            // Selection may also be published before the corresponding feed replacement.
            selected.set(original);
            assertFalse(card.getStyleClass().contains("pending-card-viewing"));
            pending.set(0, original);
            VBox originalCard = (VBox) cards.getChildren().getFirst();
            assertTrue(originalCard.getStyleClass().contains("pending-card-viewing"));

            panel.dispose();
            selected.set(replacement);
            assertTrue(originalCard.getStyleClass().contains("pending-card-viewing"));
        });
    }

    @Test
    public void panelUi_disposedDuringNotification_ignoresCapturedCallbacks() throws InterruptedException {
        Assumptions.assumeFalse(GraphicsEnvironment.isHeadless());
        runOnFxThread(() -> {
            for (DisposalSource source : DisposalSource.values()) {
                Person person = person("Client", TODAY, "Call client");
                ObservableList<Person> pending = FXCollections.observableArrayList(person);
                SimpleObjectProperty<LocalDate> today = new SimpleObjectProperty<>(TODAY);
                SimpleObjectProperty<Person> selected = new SimpleObjectProperty<>(person);
                AtomicReference<PendingFollowUpPanel> panelReference = new AtomicReference<>();
                Runnable trigger = switch (source) {
                    case LIST -> {
                        pending.addListener((ListChangeListener<Person>) change -> panelReference.get().dispose());
                        yield () -> pending.add(person("Second Client", TODAY, "Second action"));
                    }
                    case DATE -> {
                        today.addListener((ChangeListener<LocalDate>) (observable, oldDate, newDate) ->
                                panelReference.get().dispose());
                        yield () -> today.set(TODAY.plusDays(1));
                    }
                    case SELECTION -> {
                        selected.addListener((InvalidationListener) observable -> panelReference.get().dispose());
                        yield () -> selected.set(null);
                    }
                };
                PendingFollowUpPanel panel = new PendingFollowUpPanel(pending, today, selected,
                    new AddressBook()::getClientId);
                panelReference.set(panel);
                VBox dueCards = (VBox) findById(panel.getRoot(), "dueTodayCards");
                Node originalCard = dueCards.getChildren().getFirst();

                trigger.run();

                assertEquals(1, dueCards.getChildren().size(), source.toString());
                assertSame(originalCard, dueCards.getChildren().getFirst(), source.toString());
                assertTrue(originalCard.getStyleClass().contains("pending-card-viewing"));
                pending.clear();
                today.set(TODAY.plusDays(2));
                selected.set(null);
                assertSame(originalCard, dueCards.getChildren().getFirst(), source.toString());
            }
        });
    }

    private enum DisposalSource {
        LIST, DATE, SELECTION
    }

    private Person person(String name, LocalDate date, String action) {
        return new PersonBuilder().withName(name).withFollowUp(new FollowUp(date, action)).build();
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
            throw new AssertionError("JavaFX pending-panel rendering failed", failure.get());
        }
    }
}
