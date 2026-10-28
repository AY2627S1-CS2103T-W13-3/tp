package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.Region;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import seedu.address.logic.LogicManager;
import seedu.address.model.AddressBook;
import seedu.address.model.ModelManager;
import seedu.address.model.UserPrefs;
import seedu.address.model.person.ClientId;
import seedu.address.model.person.FollowUp;
import seedu.address.model.person.Person;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;
import seedu.address.testutil.PersonBuilder;

/** Checks rendered IDs against the real aggregate, rather than the current row positions. */
public class ClientIdPresentationTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 29);

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
    public void sparseIds_filterAndPendingSort_showCanonicalIdsOnEverySurface() throws Exception {
        onFx(() -> {
            try (Dashboard dashboard = new Dashboard(List.of(2, 17, 41))) {
                assertEquals("Client ID", dashboard.table().getColumns().getFirst().getText());
                assertEquals(List.of("2", "17", "41"), dashboard.ids());
                dashboard.submit("find Rachel");
                assertEquals(List.of("17"), dashboard.ids());
                assertEquals(3, dashboard.root().lookupAll(".pending-card-id").size());
                assertEquals(List.of("Client ID 17", "Client ID 2", "Client ID 41"), dashboard.root()
                        .lookupAll(".pending-card-id").stream().map(node -> ((Label) node).getText())
                        .sorted().toList());
                dashboard.submit("view 17");
                assertEquals("Client ID 17", dashboard.label("#clientId").getText());
                assertEquals(1, dashboard.root().lookupAll(".pending-card-viewing").size());
                dashboard.submit("followups");
                assertEquals(List.of("41", "17", "2"), dashboard.ids());
                dashboard.submit("followup 17 clear");
                assertEquals(List.of("41", "2"), dashboard.ids());
                assertEquals("", dashboard.label("#clientId").getText());
                assertTrue(dashboard.root().lookupAll(".pending-card-viewing").isEmpty());
            }
        });
    }

    @Test
    public void maximumId_narrowTable_rendersEveryDigitAndAllowsHorizontalScrolling() throws Exception {
        onFx(() -> {
            try (Dashboard dashboard = new Dashboard(List.of(2, 17, Integer.MAX_VALUE))) {
                dashboard.root().resize(640, 600);
                dashboard.layout();
                assertEquals("2147483647", dashboard.ids().getLast());
                TableCell<?, ?> cell = dashboard.idCells().getLast();
                assertEquals("2147483647", ((Text) cell.lookup(".text")).getText());
                Text digits = new Text("2147483647");
                digits.setFont(cell.getFont());
                assertTrue(digits.getLayoutBounds().getWidth()
                        <= cell.getWidth() - cell.snappedLeftInset() - cell.snappedRightInset());
                assertEquals(TableView.UNCONSTRAINED_RESIZE_POLICY, dashboard.table().getColumnResizePolicy());
                dashboard.submit("view 2147483647");
                assertEquals("Client ID 2147483647", dashboard.label("#clientId").getText());
            }
        });
    }

    @Test
    public void contactEdit_sameIdPreservesReadingPosition_differentIdAndReopenReset() throws Exception {
        onFx(() -> {
            try (Dashboard dashboard = new Dashboard(List.of(2, 17, 41))) {
                dashboard.submit("view 17");
                ScrollPane profile = (ScrollPane) dashboard.root().lookup(".client-profile-panel");
                assertTrue(profile.getContent().getLayoutBounds().getHeight()
                        > profile.getViewportBounds().getHeight());
                profile.setVvalue(0.75);
                double position = profile.getVvalue();
                assertTrue(position > 0.5);
                dashboard.submit("edit 17 n/Rachel Wong");
                assertEquals("Client ID 17", dashboard.label("#clientId").getText());
                assertEquals("Rachel Wong", dashboard.label("#name").getText());
                assertEquals(position, profile.getVvalue(), 0.001);
                dashboard.submit("view 2");
                assertEquals(profile.getVmin(), profile.getVvalue(), 0.001);
                profile.setVvalue(0.75);
                dashboard.submit("find Rachel");
                assertEquals("", dashboard.label("#clientId").getText());
                dashboard.submit("view 2");
                assertEquals(profile.getVmin(), profile.getVvalue(), 0.001);
                assertFalse(dashboard.label("#noSelectionMessage").isVisible());
            }
        });
    }

    @Test
    public void nonCanonicalRecords_andRecycledEmptyCells_neverInventIds() throws Exception {
        onFx(() -> {
            Person removed = client("Removed Client", "81112222", 0);
            AddressBook empty = new AddressBook();
            var rows = FXCollections.observableArrayList(removed);
            var today = new SimpleObjectProperty<>(TODAY);
            var selected = new SimpleObjectProperty<>(removed);
            PersonListPanel tablePanel = new PersonListPanel(rows, today, new SimpleBooleanProperty(), selected,
                    empty::getClientId);
            new Scene(tablePanel.getRoot(), 600, 250);
            tablePanel.getRoot().applyCss();
            tablePanel.getRoot().layout();
            for (var node : tablePanel.getRoot().lookupAll(".table-cell")) {
                TableCell<?, ?> cell = (TableCell<?, ?>) node;
                if (cell.getTableColumn().getText().equals("Client ID")) {
                    assertTrue(cell.getText() == null || cell.getText().isEmpty());
                }
            }
            rows.clear();
            tablePanel.getRoot().layout();
            ClientProfilePanel profile = new ClientProfilePanel(selected, today, empty::getClientId);
            new Scene(profile.getRoot(), 320, 250);
            profile.getRoot().applyCss();
            assertEquals("", ((Label) profile.getRoot().lookup("#clientId")).getText());
            PendingFollowUpPanel pending = new PendingFollowUpPanel(FXCollections.observableArrayList(removed),
                    today, selected, empty::getClientId);
            new Scene(pending.getRoot(), 320, 250);
            pending.getRoot().applyCss();
            assertEquals("", ((Label) pending.getRoot().lookup(".pending-card-id")).getText());
            pending.dispose();
        });
    }

    private class Dashboard implements AutoCloseable {
        private final MainWindow window;

        Dashboard(List<Integer> ids) throws Exception {
            List<Person> persons = List.of(client("Alice Tan", "81112222", 1),
                    client("Rachel Lim", "82223333", 0), client("Bob Lim", "83334444", -1));
            AddressBook book = new AddressBook();
            book.restorePersons(persons, ids.stream().map(ClientId::new).toList(), (long) ids.getLast() + 1);
            Path data = folder.resolve("clients.json");
            JsonAddressBookStorage storage = new JsonAddressBookStorage(data);
            storage.saveAddressBook(book);
            LogicManager logic = new LogicManager(new ModelManager(book, new UserPrefs(), TODAY, false),
                    new StorageManager(storage, new JsonUserPrefsStorage(folder.resolve("preferences.json"))),
                    Clock.fixed(TODAY.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC));
            window = new MainWindow(new Stage(), logic, data);
            window.fillInnerParts();
            root().resize(1000, 720);
            layout();
        }

        Region root() {
            return (Region) window.getRoot().getScene().getRoot();
        }

        void layout() {
            root().applyCss();
            root().layout();
            root().layout();
        }

        void submit(String command) {
            TextField input = (TextField) root().lookup("#commandTextField");
            Button run = (Button) root().lookup("#runButton");
            input.setText(command);
            run.fire();
            layout();
        }

        Label label(String selector) {
            return (Label) root().lookup(selector);
        }

        @SuppressWarnings("unchecked")
        TableView<Person> table() {
            return (TableView<Person>) root().lookup("#personTable");
        }

        List<TableCell<?, ?>> idCells() {
            return table().lookupAll(".table-cell").stream()
                    .filter(node -> node instanceof TableCell<?, ?> cell && !cell.isEmpty()
                            && cell.getTableColumn() == table().getColumns().getFirst())
                    .<TableCell<?, ?>>map(node -> (TableCell<?, ?>) node)
                    .sorted(java.util.Comparator.comparingInt(TableCell::getIndex)).toList();
        }

        List<String> ids() {
            return idCells().stream().map(TableCell::getText).toList();
        }

        @Override
        public void close() {
            window.getRoot().fireEvent(new WindowEvent(window.getRoot(), WindowEvent.WINDOW_CLOSE_REQUEST));
        }
    }

    private static Person client(String name, String phone, int dueOffset) {
        return new PersonBuilder().withName(name).withPhone(phone).withEmail(phone + "@example.com")
                .withFollowUp(new FollowUp(TODAY.plusDays(dueOffset), "Call client")).build();
    }

    private static void onFx(CheckedTask action) throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> {
            action.run();
            return null;
        });
        Platform.runLater(task);
        task.get(20, TimeUnit.SECONDS);
    }

    @FunctionalInterface
    private interface CheckedTask {
        void run() throws Exception;
    }
}
