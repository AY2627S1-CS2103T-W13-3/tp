package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import seedu.address.logic.LogicManager;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.AddressBook;
import seedu.address.model.ModelManager;
import seedu.address.model.ReadOnlyAddressBook;
import seedu.address.model.UserPrefs;
import seedu.address.model.person.ClientId;
import seedu.address.model.person.FollowUp;
import seedu.address.model.person.Person;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;
import seedu.address.testutil.PersonBuilder;

/** Verifies scroll position independently of canonical profile selection. */
public class ClientProfileScrollTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 28);
    private static final Person FIRST = new PersonBuilder().withName("Alice Tan")
            .withPhone("81112222").withEmail("alice@example.com")
            .withFollowUp(new FollowUp(TODAY, "Annual policy review")).build();
    private static final Person SECOND = new PersonBuilder().withName("Bob Lim")
            .withPhone("82223333").withEmail("bob@example.com")
            .withFollowUp(new FollowUp(TODAY, "Annual policy review")).build();

    @TempDir
    public Path folder;

    @BeforeAll
    public static void initializeToolkit() throws Exception {
        FutureTask<Void> initialize = new FutureTask<>(() -> {
            Platform.setImplicitExit(false);
            return null;
        });
        try {
            Platform.startup(initialize);
        } catch (IllegalStateException alreadyStarted) {
            Platform.runLater(initialize);
        }
        initialize.get(10, TimeUnit.SECONDS);
    }

    @Test
    public void differentClient_startsAtTopWithNameVisible() throws Exception {
        onFx(() -> {
            Fixture fixture = new Fixture(FIRST);
            fixture.scrollDown();
            fixture.selected.set(SECOND);
            fixture.layout();

            fixture.assertAtTop();
            assertEquals("Bob Lim", fixture.label("name").getText());
            assertTrue(fixture.label("name").localToScene(fixture.label("name").getBoundsInLocal()).getMinY()
                    >= fixture.scroll.localToScene(fixture.scroll.getBoundsInLocal()).getMinY());
        });
    }

    @Test
    public void sameNameDifferentContact_startsAtTopForSeparateClient() throws Exception {
        onFx(() -> {
            Fixture fixture = new Fixture(FIRST);
            fixture.scrollDown();
            Person sameNameClient = new PersonBuilder(SECOND).withName("Alice Tan").build();
            fixture.selected.set(sameNameClient);
            fixture.layout();

            fixture.assertAtTop();
            assertEquals("82223333", fixture.label("phone").getText());
        });
    }

    @Test
    public void sameClientFollowUpAndDateRefresh_keepReadingPosition() throws Exception {
        onFx(() -> {
            Fixture fixture = new Fixture(FIRST);
            fixture.scrollDown();
            double position = fixture.scroll.getVvalue();
            Person replacement = FIRST.withFollowUp(new FollowUp(TODAY, "Review life coverage"));
            fixture.replace(replacement);
            fixture.layout();
            assertEquals(position, fixture.scroll.getVvalue(), 0.001);
            assertEquals("Review life coverage", fixture.label("followUpAction").getText());

            fixture.today.set(TODAY.plusDays(1));
            fixture.layout();
            assertEquals(position, fixture.scroll.getVvalue(), 0.001);
            assertEquals("OVERDUE", fixture.label("followUpStatus").getText());

            fixture.replace(replacement);
            fixture.replace(new PersonBuilder(replacement).build());
            fixture.layout();
            assertEquals(position, fixture.scroll.getVvalue(), 0.001);
        });
    }

    @Test
    public void equalContactReplacementWithDifferentId_resetsScrollAndRendersNewIdentity() throws Exception {
        onFx(() -> {
            Fixture fixture = new Fixture(FIRST);
            fixture.scrollDown();
            Person equalCopy = new PersonBuilder(FIRST).build();
            fixture.book.restorePersons(List.of(equalCopy), List.of(new ClientId(41)), 42);
            fixture.selected.set(equalCopy);
            fixture.layout();

            fixture.assertAtTop();
            assertEquals("Client ID 41", fixture.label("clientId").getText());
        });
    }

    @Test
    public void sameClientFollowUpCleared_keepsReadingPositionWithinRemainingContent() throws Exception {
        onFx(() -> {
            Fixture fixture = new Fixture(FIRST);
            fixture.scrollDown();
            fixture.replace(FIRST.withoutFollowUp());
            fixture.layout();

            assertTrue(fixture.scroll.getVvalue() > fixture.scroll.getVmin());
            assertEquals("Follow-up: None", fixture.label("followUpNone").getText());
        });
    }

    @Test
    public void initiallyEmptyAndReopenedProfile_startAtTop() throws Exception {
        onFx(() -> {
            Fixture fixture = new Fixture(null);
            fixture.assertAtTop();
            fixture.selected.set(FIRST);
            fixture.layout();
            fixture.assertAtTop();
            fixture.scrollDown();
            fixture.selected.set(null);
            fixture.layout();
            assertTrue(fixture.label("noSelectionMessage").isManaged());
            fixture.selected.set(FIRST);
            fixture.layout();
            fixture.assertAtTop();
        });
    }

    @Test
    public void realCommands_keepPositionOnReplacementAndFailure_resetOnDifferentView() throws Exception {
        onFx(() -> {
            AddressBook book = new AddressBook();
            book.restorePersons(List.of(FIRST, SECOND), List.of(new ClientId(17), new ClientId(41)), 42);
            ModelManager model = new ModelManager(book, new UserPrefs(), TODAY, false);
            FailingStorage storage = new FailingStorage(folder);
            LogicManager logic = new LogicManager(model, storage,
                    Clock.fixed(TODAY.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC));
            logic.execute("followups");
            logic.execute("view 17");
            ClientProfilePanel panel = new ClientProfilePanel(model.selectedPersonProperty(), model.todayProperty(),
                    logic::getClientId);
            ScrollPane scroll = attach(panel);
            scroll.setVvalue(0.75);
            layout(scroll);
            double position = scroll.getVvalue();

            logic.execute("view 17");
            assertEquals(position, scroll.getVvalue(), 0.001);
            logic.execute("followup 17 d/2026-10-29 m/Review life coverage");
            layout(scroll);
            assertNotSame(FIRST, model.selectedPersonProperty().get());
            assertEquals(position, scroll.getVvalue(), 0.001);
            logic.execute("followup 41 d/2026-10-29 m/Review life coverage");
            layout(scroll);
            assertEquals(position, scroll.getVvalue(), 0.001);

            assertThrows(ParseException.class, () -> logic.execute("view 0"));
            storage.shouldFailSave = true;
            assertThrows(CommandException.class, () -> logic.execute("followup 41 clear"));
            layout(scroll);
            assertEquals(position, scroll.getVvalue(), 0.001);
            logic.execute("view 41");
            layout(scroll);
            assertEquals(scroll.getVmin(), scroll.getVvalue(), 0.001);
        });
    }

    private static ScrollPane attach(ClientProfilePanel panel) {
        ScrollPane scroll = (ScrollPane) panel.getRoot();
        new Scene(scroll, 320, 240);
        scroll.resize(320, 240);
        layout(scroll);
        return scroll;
    }

    private static void layout(ScrollPane scroll) {
        scroll.applyCss();
        scroll.layout();
        scroll.layout();
    }

    private static void onFx(CheckedTask task) throws Exception {
        FutureTask<Void> completed = new FutureTask<>(() -> {
            task.run();
            return null;
        });
        Platform.runLater(completed);
        completed.get(10, TimeUnit.SECONDS);
    }

    @FunctionalInterface
    private interface CheckedTask {
        void run() throws Exception;
    }

    private static class Fixture {
        private final SimpleObjectProperty<Person> selected;
        private final SimpleObjectProperty<LocalDate> today = new SimpleObjectProperty<>(TODAY);
        private final ScrollPane scroll;
        private final AddressBook book = new AddressBook();

        Fixture(Person initial) {
            book.restorePersons(List.of(FIRST, SECOND), List.of(new ClientId(17), new ClientId(41)), 42);
            selected = new SimpleObjectProperty<>(initial);
            scroll = attach(new ClientProfilePanel(selected, today, book::getClientId));
        }

        void replace(Person replacement) {
            book.setPerson(selected.get(), replacement);
            selected.set(replacement);
        }

        void layout() {
            ClientProfileScrollTest.layout(scroll);
        }

        void scrollDown() {
            assertTrue(scroll.getContent().getLayoutBounds().getHeight() > scroll.getViewportBounds().getHeight());
            scroll.setVvalue(0.75);
            layout();
            assertTrue(scroll.getVvalue() > 0.5);
        }

        void assertAtTop() {
            assertEquals(scroll.getVmin(), scroll.getVvalue(), 0.001);
        }

        Label label(String id) {
            return (Label) scroll.lookup("#" + id);
        }
    }

    private static class FailingStorage extends StorageManager {
        private boolean shouldFailSave;

        FailingStorage(Path folder) {
            super(new JsonAddressBookStorage(folder.resolve("clients.json")),
                    new JsonUserPrefsStorage(folder.resolve("preferences.json")));
        }

        @Override
        public void saveAddressBook(ReadOnlyAddressBook book) throws IOException {
            if (shouldFailSave) {
                throw new IOException("Synthetic save failure");
            }
            super.saveAddressBook(book);
        }
    }
}
