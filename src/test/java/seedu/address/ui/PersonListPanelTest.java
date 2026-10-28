package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollBar;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.text.Text;
import seedu.address.model.AddressBook;
import seedu.address.model.person.FollowUp;
import seedu.address.model.person.Person;
import seedu.address.testutil.PersonBuilder;

/**
 * Drives a real {@code TableView} to check that rendered rows stay in step with the command list.
 */
public class PersonListPanelTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 12);

    private final ObservableList<Person> source = FXCollections.observableArrayList();
    private final FilteredList<Person> displayed = new FilteredList<>(source);
    private final SimpleObjectProperty<LocalDate> today = new SimpleObjectProperty<>(TODAY);
    private final SimpleBooleanProperty showingFollowUps = new SimpleBooleanProperty(false);
    private final SimpleObjectProperty<Person> selectedPerson = new SimpleObjectProperty<>();

    private final AddressBook identities = new AddressBook();

    private Scene scene;
    private TableView<Person> table;

    @BeforeAll
    public static void startToolkit() {
        try {
            Platform.startup(() -> { });
        } catch (IllegalStateException alreadyStarted) {
            // Another test class started the toolkit.
        }
        Platform.setImplicitExit(false);
    }

    @Test
    public void clientIds_afterDeleteFindAndReorder_remainStable() throws Exception {
        source.setAll(clients(8));
        runOnFx(() -> {
            show(700);
            assertRowsMatchList();

            source.remove(2);
            layout();
            assertRowsMatchList();

            displayed.setPredicate(person -> person.getName().fullName.endsWith("5")
                    || person.getName().fullName.endsWith("7"));
            layout();
            assertEquals(List.of("5", "7"), visibleIndices());
            assertRowsMatchList();

            displayed.setPredicate(null);
            List<Person> reordered = new ArrayList<>(source);
            reordered.sort(Comparator.comparing((Person person) -> person.getName().fullName).reversed());
            source.setAll(reordered);
            layout();
            assertRowsMatchList();
        });
    }

    @Test
    public void largeList_scrollDeleteFilterReorder_rowsStayConsistent() throws Exception {
        source.setAll(clients(200));
        runOnFx(() -> {
            show(700);
            table.scrollTo(150);
            layout();
            assertRowsMatchList();

            for (int i = 0; i < 10; i++) {
                source.remove(150);
                layout();
                assertRowsMatchList();
            }
            source.remove(source.size() - 1);
            layout();
            assertRowsMatchList();

            displayed.setPredicate(person -> person.getName().fullName.contains("1"));
            layout();
            assertRowsMatchList();
            displayed.setPredicate(null);
            layout();
            assertRowsMatchList();

            List<Person> reordered = new ArrayList<>(source);
            Collections.reverse(reordered);
            source.setAll(reordered);
            table.scrollTo(source.size() - 1);
            layout();
            assertRowsMatchList();
        });
    }

    @Test
    public void viewingMarker_followsSelectedClientAcrossRecycling() throws Exception {
        source.setAll(clients(60));
        runOnFx(() -> {
            show(700);
            Person viewed = source.get(1);
            selectedPerson.set(viewed);
            layout();
            assertEquals(List.of(viewed), markedClients());

            table.scrollTo(40);
            layout();
            assertEquals(List.of(), markedClients());
            table.scrollTo(0);
            layout();
            assertEquals(List.of(viewed), markedClients());

            source.remove(viewed);
            layout();
            assertEquals(List.of(), markedClients());
        });
    }

    @Test
    public void modeSwitch_togglesFollowUpColumnsAndStatuses() throws Exception {
        source.setAll(new PersonBuilder().withName("Overdue Client")
                .withFollowUp(new FollowUp(TODAY.minusDays(1), "Call client")).build());
        runOnFx(() -> {
            show(700);
            assertFalse(column("Due").isVisible());
            assertFalse(column("Status").isVisible());

            showingFollowUps.set(true);
            layout();
            assertTrue(column("Due").isVisible());
            assertTrue(column("Status").isVisible());
            assertEquals("OVERDUE", ((Label) cell(row(0), "Status").getGraphic()).getText());

            showingFollowUps.set(false);
            layout();
            assertFalse(column("Status").isVisible());
        });
    }

    @Test
    public void tableSelection_doesNotChangeViewedClient() throws Exception {
        source.setAll(clients(5));
        runOnFx(() -> {
            show(700);
            Person viewed = source.get(0);
            selectedPerson.set(viewed);
            table.getSelectionModel().select(3);
            table.getFocusModel().focus(4);
            layout();
            assertSame(viewed, selectedPerson.get());
            assertEquals(List.of(viewed), markedClients());
        });
    }

    @Test
    public void narrowWidth_columnsDoNotFit_scrollsHorizontally() throws Exception {
        source.setAll(clients(3));
        showingFollowUps.set(true);
        runOnFx(() -> {
            show(300);
            assertSame(TableView.UNCONSTRAINED_RESIZE_POLICY, table.getColumnResizePolicy());
            assertTrue(horizontalScrollBar().isVisible());

            scene.getRoot().resize(700, 400);
            layout();
            assertNotSame(TableView.UNCONSTRAINED_RESIZE_POLICY, table.getColumnResizePolicy());
        });
    }

    @SuppressWarnings("unchecked")
    private void show(double width) {
        identities.setPersons(new ArrayList<>(source));
        source.addListener((ListChangeListener<Person>) change -> identities.setPersons(new ArrayList<>(source)));
        PersonListPanel panel = new PersonListPanel(displayed, today, showingFollowUps, selectedPerson,
                identities::getClientId);
        scene = new Scene(panel.getRoot(), width, 400);
        table = (TableView<Person>) panel.getRoot().lookup("#personTable");
        layout();
    }

    private void layout() {
        scene.getRoot().applyCss();
        scene.getRoot().layout();
    }

    /** Asserts every rendered row shows its aggregate ID and the current list's canonical client. */
    private void assertRowsMatchList() {
        List<TableRow<Person>> rows = filledRows();
        assertFalse(rows.isEmpty());
        for (TableRow<Person> row : rows) {
            int index = row.getIndex();
            assertSame(table.getItems().get(index), row.getItem());
            assertEquals(identities.getClientId(row.getItem()).orElseThrow().toString(),
                    cell(row, "Client ID").getText());
            assertEquals(row.getItem().getName().fullName, nameText(row));
        }
    }

    private List<String> visibleIndices() {
        return filledRows().stream().map(row -> cell(row, "Client ID").getText()).toList();
    }

    private List<Person> markedClients() {
        List<Person> marked = new ArrayList<>();
        for (TableRow<Person> row : filledRows()) {
            boolean rowMarked = row.getStyleClass().contains(PersonListPanel.VIEWED_STYLE_CLASS);
            boolean labelShown = !row.lookupAll(".viewed-marker").isEmpty();
            assertEquals(rowMarked, labelShown, "Row style and VIEWING label must agree");
            if (rowMarked) {
                marked.add(row.getItem());
            }
        }
        return marked;
    }

    @SuppressWarnings("unchecked")
    private List<TableRow<Person>> filledRows() {
        List<TableRow<Person>> rows = new ArrayList<>();
        for (Node node : table.lookupAll(".table-row-cell")) {
            TableRow<Person> row = (TableRow<Person>) node;
            if (!row.isEmpty() && row.isVisible()) {
                rows.add(row);
            }
        }
        rows.sort(Comparator.comparingInt(TableRow::getIndex));
        return rows;
    }

    private TableRow<Person> row(int index) {
        return filledRows().stream().filter(row -> row.getIndex() == index).findFirst().orElseThrow();
    }

    @SuppressWarnings("unchecked")
    private TableCell<Person, Person> cell(TableRow<Person> row, String columnText) {
        return row.getChildrenUnmodifiable().stream()
                .filter(node -> node instanceof TableCell)
                .map(node -> (TableCell<Person, Person>) node)
                .filter(cell -> cell.getTableColumn().getText().equals(columnText))
                .findFirst().orElseThrow();
    }

    private String nameText(TableRow<Person> row) {
        return ((Text) cell(row, "Name").lookup(".client-table-text")).getText();
    }

    private TableColumn<Person, ?> column(String text) {
        return table.getColumns().stream().filter(column -> column.getText().equals(text)).findFirst().orElseThrow();
    }

    private ScrollBar horizontalScrollBar() {
        return table.lookupAll(".scroll-bar").stream()
                .map(node -> (ScrollBar) node)
                .filter(bar -> bar.getOrientation() == Orientation.HORIZONTAL)
                .findFirst().orElseThrow();
    }

    private static List<Person> clients(int count) {
        List<Person> clients = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            clients.add(new PersonBuilder().withName("Client " + i).withPhone(String.valueOf(90000000 + i))
                    .withEmail("client" + i + "@example.com")
                    .withFollowUp(new FollowUp(TODAY.plusDays(i % 3 - 1), "Call client")).build());
        }
        return clients;
    }

    /** Runs {@code action} on the JavaFX thread and rethrows anything it throws, including assertion failures. */
    private static void runOnFx(Runnable action) throws Exception {
        CompletableFuture<Void> done = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                action.run();
                done.complete(null);
            } catch (Throwable t) {
                done.completeExceptionally(t);
            }
        });
        try {
            done.get(30, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            if (e.getCause() instanceof Error error) {
                throw error;
            }
            throw (Exception) e.getCause();
        }
    }
}
