package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javafx.application.Platform;
import javafx.geometry.Bounds;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.skin.VirtualFlow;
import javafx.scene.layout.Region;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import seedu.address.logic.LogicManager;
import seedu.address.model.AddressBook;
import seedu.address.model.ModelManager;
import seedu.address.model.UserPrefs;
import seedu.address.model.person.FollowUp;
import seedu.address.model.person.Person;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;
import seedu.address.testutil.PersonBuilder;

/**
 * Checks dashboard layout partitions not covered by {@link DashboardIntegrationTest}: many records,
 * long unbroken values and a full pending summary, at the logical sizes of the release display matrix.
 * Logical scene sizes are not evidence of physical display scaling; see docs/testing/v1.5/platform-matrix.md.
 */
public class DashboardLayoutBoundaryTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 21);

    /** 1280x720 at 150%, 1920x1080 at 125% and 1920x1080 at 100%, in logical pixels. */
    private static final double[][] LOGICAL_SIZES = {{853, 480}, {1536, 864}, {1920, 1080}};

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
    public void table_thousandClients_lastIndexStaysReadable() throws Exception {
        AddressBook clients = DisplayMatrixFixture.clients(TODAY);
        assertEquals(1000, clients.getPersonList().size());
        runOnFx(() -> {
            try (Dashboard dashboard = new Dashboard(clients)) {
                for (double[] size : LOGICAL_SIZES) {
                    dashboard.resize(size);
                    // TableView.scrollTo places rows by estimated height, so a taller wrapped last row can
                    // miss the viewport on another platform's fonts; scroll to the end as a user would.
                    VirtualFlow<?> rows = (VirtualFlow<?>) dashboard.table().lookup(".virtual-flow");
                    rows.setPosition(1);
                    dashboard.layout();
                    TableCell<?, ?> cell = dashboard.indexCell(999);
                    assertEquals("1000", ((Text) cell.lookup(".text")).getText(), "at " + size[0] + "x" + size[1]);

                    Text fiveDigits = new Text("99999");
                    fiveDigits.setFont(cell.getFont());
                    double available = cell.getWidth() - cell.snappedLeftInset() - cell.snappedRightInset();
                    assertTrue(fiveDigits.getLayoutBounds().getWidth() <= available,
                            "Five-digit index needs " + fiveDigits.getLayoutBounds().getWidth() + " of " + available);
                }
            }
        });
    }

    @Test
    public void profile_longUnbrokenEmailAndAddress_wrapInsideViewport() throws Exception {
        String email = "a".repeat(70) + "@" + "b".repeat(50) + ".com";
        String address = "Blk" + "9".repeat(150);
        AddressBook clients = new AddressBook();
        clients.addPerson(new PersonBuilder().withEmail(email).withAddress(address).build());
        runOnFx(() -> {
            try (Dashboard dashboard = new Dashboard(clients)) {
                dashboard.submit("view 1");
                for (double[] size : LOGICAL_SIZES) {
                    dashboard.resize(size);
                    ScrollPane profile = dashboard.scrollPane("#clientProfilePlaceholder");
                    assertContentFitsViewportWidth(profile);
                    assertCompleteText(email, (Label) profile.lookup("#email"));
                    assertCompleteText(address, (Label) profile.lookup("#address"));
                }
            }
        });
    }

    @Test
    public void pendingSummary_manyCards_scrollsToLastCardAndKeepsProfile() throws Exception {
        AddressBook clients = new AddressBook();
        for (int i = 1; i <= 80; i++) {
            clients.addPerson(new PersonBuilder().withName("Client " + i).withPhone(String.format("9%07d", i))
                    .withFollowUp(new FollowUp(TODAY.plusDays(i), "Call client " + i)).build());
        }
        runOnFx(() -> {
            try (Dashboard dashboard = new Dashboard(clients)) {
                dashboard.submit("view 1");
                for (double[] size : LOGICAL_SIZES) {
                    dashboard.resize(size);
                    ScrollPane pending = dashboard.scrollPane("#pendingFollowUpPlaceholder");
                    assertEquals(80, pending.lookupAll(".pending-card").size());
                    assertTrue(pending.getContent().getLayoutBounds().getHeight() > pending.getViewportBounds()
                            .getHeight(), "80 cards should need scrolling");

                    pending.setVvalue(pending.getVmax());
                    dashboard.layout();
                    Node lastCard = pending.lookupAll(".pending-card").stream().reduce((first, second) -> second)
                            .orElseThrow();
                    assertBottomInsideViewport(lastCard, pending);
                    assertTrue(dashboard.node("#clientProfilePlaceholder").getLayoutBounds().getHeight() > 30);
                    pending.setVvalue(pending.getVmin());
                }
            }
        });
    }

    @Test
    public void pendingSummary_longNameAndMaximumAction_staysInsideSidebar() throws Exception {
        String name = "Abcdefghij".repeat(8);
        String action = "x".repeat(200);
        AddressBook clients = new AddressBook();
        clients.addPerson(new PersonBuilder().withName(name).withFollowUp(new FollowUp(TODAY, action)).build());
        runOnFx(() -> {
            try (Dashboard dashboard = new Dashboard(clients)) {
                for (double[] size : LOGICAL_SIZES) {
                    dashboard.resize(size);
                    ScrollPane pending = dashboard.scrollPane("#pendingFollowUpPlaceholder");
                    assertContentFitsViewportWidth(pending);
                    assertCompleteText(name, (Label) pending.lookup(".pending-card-name"));
                    assertCompleteText(action, (Label) pending.lookup(".pending-card-action"));
                }
            }
        });
    }

    private static void assertContentFitsViewportWidth(ScrollPane scrollPane) {
        double content = scrollPane.getContent().getLayoutBounds().getWidth();
        double viewport = scrollPane.getViewportBounds().getWidth();
        assertTrue(content <= viewport + 1, "Content width " + content + " exceeds viewport " + viewport);
    }

    /** Asserts that a label renders its whole value, rather than an ellipsis-clipped prefix. */
    private static void assertCompleteText(String expected, Label label) {
        assertEquals(expected, label.getText());
        assertEquals(expected, ((Text) label.lookup(".text")).getText());
    }

    /** Asserts that scrolling reached {@code node}'s end; a short pane may still be shorter than the whole card. */
    private static void assertBottomInsideViewport(Node node, ScrollPane scrollPane) {
        Bounds nodeBounds = node.localToScene(node.getBoundsInLocal());
        Bounds viewport = scrollPane.localToScene(scrollPane.getBoundsInLocal());
        assertTrue(nodeBounds.getMaxY() >= viewport.getMinY() && nodeBounds.getMaxY() <= viewport.getMaxY() + 1,
                "Card end " + nodeBounds.getMaxY() + " is outside viewport " + viewport);
    }

    /** A real main window over temporary storage, laid out at logical sizes without being shown. */
    private class Dashboard implements AutoCloseable {
        private final MainWindow window;

        Dashboard(AddressBook clients) throws IOException {
            Path data = folder.resolve("clients.json");
            JsonAddressBookStorage storage = new JsonAddressBookStorage(data);
            storage.saveAddressBook(clients);
            LogicManager logic = new LogicManager(new ModelManager(clients, new UserPrefs(), TODAY, false),
                    new StorageManager(storage, new JsonUserPrefsStorage(folder.resolve("preferences.json"))),
                    Clock.fixed(TODAY.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC));
            window = new MainWindow(new Stage(), logic, data);
            window.fillInnerParts();
            layout();
        }

        void submit(String command) {
            TextField input = (TextField) node("#commandTextField");
            input.setText(command);
            Button run = (Button) node("#runButton");
            run.fire();
        }

        void resize(double[] size) {
            root().resize(size[0], size[1]);
            layout();
        }

        void layout() {
            root().applyCss();
            root().layout();
            root().applyCss();
            root().layout();
        }

        Region root() {
            return (Region) window.getRoot().getScene().getRoot();
        }

        Parent node(String selector) {
            return (Parent) root().lookup(selector);
        }

        ScrollPane scrollPane(String placeholder) {
            return (ScrollPane) ((Parent) node(placeholder)).getChildrenUnmodifiable().getFirst();
        }

        @SuppressWarnings("unchecked")
        TableView<Person> table() {
            return (TableView<Person>) node("#personTable");
        }

        /** Returns the rendered index cell of {@code rowIndex}. */
        TableCell<?, ?> indexCell(int rowIndex) {
            return table().lookupAll(".table-cell").stream()
                    .filter(node -> node instanceof TableCell<?, ?> cell && cell.getIndex() == rowIndex
                            && cell.getTableColumn() == table().getColumns().getFirst() && cell.getText() != null)
                    .map(node -> (TableCell<?, ?>) node)
                    .findFirst().orElseThrow(() -> new AssertionError("Row " + rowIndex + " is not rendered"));
        }

        @Override
        public void close() {
            window.getRoot().fireEvent(new WindowEvent(window.getRoot(), WindowEvent.WINDOW_CLOSE_REQUEST));
        }
    }

    private static void runOnFx(ThrowingRunnable action) throws Exception {
        FutureTask<Void> task = new FutureTask<>(() -> {
            action.run();
            return null;
        });
        Platform.runLater(task);
        task.get(60, TimeUnit.SECONDS);
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
