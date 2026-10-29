package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javafx.geometry.Rectangle2D;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import seedu.address.commons.core.GuiSettings;
import seedu.address.logic.LogicManager;
import seedu.address.model.AddressBook;
import seedu.address.model.ModelManager;
import seedu.address.model.ReadOnlyAddressBook;
import seedu.address.model.UserPrefs;
import seedu.address.model.person.FollowUp;
import seedu.address.model.person.Person;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;
import seedu.address.testutil.FxTestSupport;
import seedu.address.testutil.FxTestSupport.ThrowingRunnable;
import seedu.address.testutil.PersonBuilder;

/** Checks component wiring through the real window, command executor and temporary storage. */
public class DashboardIntegrationTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 21);

    @TempDir
    public Path folder;

    @BeforeAll
    public static void initializeToolkit() throws Exception {
        FxTestSupport.initializeToolkit();
    }

    @Test
    public void help_minimisedWindow_restoresExistingStage() throws Exception {
        runOnFx(() -> {
            try (Fixture fixture = new Fixture()) {
                fixture.window.handleHelp();
                Stage helpStage = Stage.getWindows().stream()
                        .filter(window -> window.isShowing()
                                && window != fixture.window.getPrimaryStage())
                        .map(window -> (Stage) window)
                        .findFirst()
                        .orElseThrow();
                try {
                    helpStage.setIconified(true);
                    assertTrue(helpStage.isIconified());
                    fixture.window.handleHelp();
                    assertTrue(helpStage.isShowing());
                    assertFalse(helpStage.isIconified());
                } finally {
                    helpStage.hide();
                }
            }
        });
    }

    @Test
    public void commands_filteredTable_keepsGlobalPendingAndCanonicalProfile() throws Exception {
        runOnFx(() -> {
            try (Fixture fixture = new Fixture()) {
                assertEquals(3, fixture.pendingCards());
                assertTrue(fixture.node("#pendingFollowUpPlaceholder").isVisible());
                assertTrue(fixture.node("#pendingFollowUpPlaceholder").isManaged());
                fixture.submit("find Alice");
                assertEquals(1, fixture.table().getItems().size());
                assertEquals(3, fixture.pendingCards());
                fixture.submit("view 1");
                assertEquals("Alice Tan", fixture.profileName());
                assertEquals(1, fixture.node("#pendingFollowUpPlaceholder")
                        .lookupAll(".pending-card-viewing").size());
                fixture.submit("followups");
                assertEquals(List.of("Bob Tan", "Charlie Tan", "Alice Tan"), fixture.tableNames());
                fixture.submit("followup 1 d/2026-10-21 m/Call Alice today");
                assertEquals(List.of("Bob Tan", "Alice Tan", "Charlie Tan"), fixture.tableNames());
                assertEquals("Alice Tan", fixture.profileName());
                assertSame(fixture.table().getItems().get(1), fixture.model.selectedPersonProperty().get());
                fixture.submit("followup 1 clear");
                assertEquals(2, fixture.pendingCards());
                assertNull(fixture.model.selectedPersonProperty().get());
                fixture.submit("find Charlie");
                fixture.submit("delete 3");
                assertEquals(1, fixture.pendingCards());
                assertEquals(List.of("Alice Tan", "Bob Tan"), fixture.logic.getAllPersonList().stream()
                        .map(person -> person.getName().fullName).toList());
            }
        });
    }

    @Test
    public void commands_failedSaveAndParse_preserveViewsAndShowErrorThenRecover() throws Exception {
        runOnFx(() -> {
            try (Fixture fixture = new Fixture()) {
                fixture.submit("view 1");
                byte[] saved = Files.readAllBytes(fixture.data);
                Person selected = fixture.model.selectedPersonProperty().get();
                fixture.shouldFailSave.set(true);
                fixture.submit("delete 1");
                assertEquals("delete 1", fixture.input().getText());
                assertTrue(fixture.node("#feedbackStatus").isVisible());
                assertEquals(3, fixture.pendingCards());
                assertSame(selected, fixture.model.selectedPersonProperty().get());
                assertArrayEquals(saved, Files.readAllBytes(fixture.data));
                fixture.submit("view 999");
                assertEquals("view 999", fixture.input().getText());
                assertTrue(fixture.node("#feedbackStatus").isVisible());
                fixture.shouldFailSave.set(false);
                fixture.submit("delete 1");
                assertEquals("", fixture.input().getText());
                assertFalse(fixture.node("#feedbackStatus").isVisible());
                assertEquals(2, fixture.pendingCards());
                assertEquals(2, new JsonAddressBookStorage(fixture.data).readAddressBook()
                        .orElseThrow().getPersonList().size());
                assertSame(fixture.input(), fixture.window.getRoot().getScene().getFocusOwner());
            }
        });
    }

    @Test
    public void publishedDate_refreshesAllGroupsAndProfileWithoutChangingSelection() throws Exception {
        runOnFx(() -> {
            try (Fixture fixture = new Fixture()) {
                fixture.submit("view 3");
                Person selected = fixture.model.selectedPersonProperty().get();
                assertEquals(1, fixture.node("#dueTodayCards").getChildrenUnmodifiable().size());
                fixture.clock.instant = Instant.parse("2026-10-22T00:00:00Z");
                fixture.logic.refreshToday();
                assertEquals(0, fixture.node("#dueTodayCards").getChildrenUnmodifiable().size());
                assertEquals(2, fixture.node("#overdueCards").getChildrenUnmodifiable().size());
                assertEquals("OVERDUE", ((Label) fixture.node("#followUpStatus")).getText());
                assertSame(selected, fixture.model.selectedPersonProperty().get());
            }
        });
    }

    @Test
    public void close_detachesPendingProjectionAndPanel() throws Exception {
        runOnFx(() -> {
            try (Fixture fixture = new Fixture()) {
                int rendered = fixture.pendingCards();
                fixture.close();
                fixture.logic.execute("followup 1 clear");
                assertEquals(rendered, fixture.pendingCards());
            }
        });
    }

    @Test
    public void layout_smallLogicalScreen_keepsBothPanelsAndCommandControlsAccessible() throws Exception {
        runOnFx(() -> {
            try (Fixture fixture = new Fixture()) {
                fixture.submit("view 1");
                for (double[] size : new double[][] {{853, 480}, {1000, 720}, {1536, 864}}) {
                    Region root = (Region) fixture.window.getRoot().getScene().getRoot();
                    root.resize(size[0], size[1]);
                    root.applyCss();
                    root.layout();
                    assertTrue(fixture.node("#pendingFollowUpPlaceholder").getLayoutBounds().getHeight() > 30);
                    assertTrue(fixture.node("#clientProfilePlaceholder").getLayoutBounds().getHeight() > 30);
                    Node run = fixture.node("#runButton");
                    assertTrue(run.localToScene(run.getBoundsInLocal()).getMaxY() < size[1]);
                    assertTrue(fixture.table().getHeight() > 50);
                }
            }
        });
    }

    @Test
    public void window_unavailableSavedScreen_restoresInsidePrimaryWithoutDiscardingPreferences() throws Exception {
        runOnFx(() -> {
            GuiSettings saved = new GuiSettings(900, 640, 1_000_000, 1_000_000);
            assertTrue(Screen.getScreensForRectangle(1_000_000, 1_000_000, 1, 1).isEmpty());
            try (Fixture fixture = new Fixture(saved)) {
                Stage stage = fixture.window.getPrimaryStage();
                Rectangle2D restored = new Rectangle2D(stage.getX(), stage.getY(),
                        stage.getWidth(), stage.getHeight());
                assertTrue(Screen.getPrimary().getVisualBounds().contains(restored));
                assertEquals(saved, fixture.logic.getGuiSettings());
            }
        });
    }

    private class Fixture implements AutoCloseable {
        private final Path data = folder.resolve("clients.json");
        private final AtomicBoolean shouldFailSave = new AtomicBoolean();
        private final MutableClock clock = new MutableClock();
        private final ModelManager model;
        private final LogicManager logic;
        private final MainWindow window;

        Fixture() throws IOException {
            this(new GuiSettings());
        }

        Fixture(GuiSettings settings) throws IOException {
            AddressBook clients = new AddressBook();
            clients.addPerson(client("Alice Tan", "111", 3));
            clients.addPerson(client("Bob Tan", "222", -1));
            clients.addPerson(client("Charlie Tan", "333", 0));
            UserPrefs preferences = new UserPrefs();
            preferences.setGuiSettings(settings);
            model = new ModelManager(clients, preferences, TODAY, false);
            JsonAddressBookStorage storage = new JsonAddressBookStorage(data) {
                @Override
                public void saveAddressBook(ReadOnlyAddressBook clients) throws IOException {
                    if (shouldFailSave.get()) {
                        throw new IOException("Simulated save failure");
                    }
                    super.saveAddressBook(clients);
                }
            };
            storage.saveAddressBook(clients);
            logic = new LogicManager(model, new StorageManager(storage,
                    new JsonUserPrefsStorage(folder.resolve("preferences.json"))), clock);
            window = new MainWindow(new Stage(), logic, data);
            window.fillInnerParts();
            window.getRoot().getScene().getRoot().applyCss();
            window.getRoot().getScene().getRoot().layout();
        }

        void submit(String command) {
            input().setText(command);
            Button runButton = (Button) node("#runButton");
            runButton.fire();
        }

        TextField input() {
            return (TextField) node("#commandTextField");
        }

        Parent node(String selector) {
            return (Parent) window.getRoot().getScene().getRoot().lookup(selector);
        }

        @SuppressWarnings("unchecked")
        TableView<Person> table() {
            return (TableView<Person>) node("#personTable");
        }

        List<String> tableNames() {
            return table().getItems().stream().map(person -> person.getName().fullName).toList();
        }

        int pendingCards() {
            return node("#pendingFollowUpPlaceholder").lookupAll(".pending-card").size();
        }

        String profileName() {
            return ((Label) node("#clientProfilePlaceholder").lookup("#name")).getText();
        }

        @Override
        public void close() {
            window.getRoot().fireEvent(new WindowEvent(window.getRoot(), WindowEvent.WINDOW_CLOSE_REQUEST));
        }
    }

    private static Person client(String name, String phone, int daysFromToday) {
        return new PersonBuilder().withName(name).withPhone(phone)
                .withFollowUp(new FollowUp(TODAY.plusDays(daysFromToday), "Call " + name)).build();
    }

    private static class MutableClock extends Clock {
        private Instant instant = Instant.parse("2026-10-21T00:00:00Z");

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return Clock.fixed(instant, zone);
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }

    private static void runOnFx(ThrowingRunnable action) throws Exception {
        FxTestSupport.runOnFx(action, 20);
    }
}
