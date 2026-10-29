package seedu.address.ui;

import static seedu.address.commons.util.CollectionUtil.requireAllNonNull;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import javafx.beans.InvalidationListener;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import seedu.address.logic.commands.FollowUpsCommand;
import seedu.address.model.person.ClientId;
import seedu.address.model.person.FollowUp;
import seedu.address.model.person.FollowUpStatus;
import seedu.address.model.person.Person;

/**
 * Table of the current client list, labelled with permanent IDs supplied through Logic.
 * The model owns the displayed order. The table only displays the model's
 * state: clicking or highlighting a row never changes the viewed client.
 */
public class PersonListPanel extends UiPart<Region> {
    public static final String MESSAGE_NO_CLIENTS = "No clients to display.";

    static final String VIEWED_STYLE_CLASS = "viewed-client";

    private static final String FXML = "PersonListPanel.fxml";
    private static final double CELL_HORIZONTAL_PADDING = 12;

    private final Function<Person, Optional<ClientId>> clientIdLookup;
    private final ReadOnlyObjectProperty<LocalDate> today;
    private final ReadOnlyBooleanProperty showingFollowUps;
    private final ReadOnlyObjectProperty<Person> selectedPerson;

    private Node rowViewport;

    @FXML
    private TableView<Person> personTable;
    @FXML
    private TableColumn<Person, Person> clientIdColumn;
    @FXML
    private TableColumn<Person, Person> nameColumn;
    @FXML
    private TableColumn<Person, Person> phoneColumn;
    @FXML
    private TableColumn<Person, Person> emailColumn;
    @FXML
    private TableColumn<Person, Person> dueColumn;
    @FXML
    private TableColumn<Person, Person> statusColumn;

    /**
     * Creates a {@code PersonListPanel} that renders {@code personList} in the current list mode and
     * marks the row whose client is {@code selectedPerson}.
     */
    public PersonListPanel(ObservableList<Person> personList, ReadOnlyObjectProperty<LocalDate> today,
                           ReadOnlyBooleanProperty showingFollowUps,
                           ReadOnlyObjectProperty<Person> selectedPerson,
                           Function<Person, Optional<ClientId>> clientIdLookup) {
        super(FXML);
        requireAllNonNull(personList, today, showingFollowUps, selectedPerson, clientIdLookup);
        this.clientIdLookup = clientIdLookup;
        this.today = today;
        this.showingFollowUps = showingFollowUps;
        this.selectedPerson = selectedPerson;

        personTable.setItems(personList);
        Label emptyListLabel = new Label();
        emptyListLabel.getStyleClass().add("client-table-placeholder");
        personTable.setPlaceholder(emptyListLabel);
        personTable.setRowFactory(table -> new ClientRow());

        clientIdColumn.setCellFactory(column -> new ClientIdCell());
        nameColumn.setCellFactory(column -> new NameCell());
        phoneColumn.setCellFactory(column -> new TextCell(row -> row.phone()));
        emailColumn.setCellFactory(column -> new TextCell(row -> row.email()));
        dueColumn.setCellFactory(column -> new TextCell(row -> row.dueDate()));
        statusColumn.setCellFactory(column -> new StatusCell());
        for (TableColumn<Person, Person> column
                : List.of(clientIdColumn, nameColumn, phoneColumn, emailColumn, dueColumn, statusColumn)) {
            column.setCellValueFactory(cell -> new ReadOnlyObjectWrapper<>(cell.getValue()));
        }
        dueColumn.visibleProperty().bind(showingFollowUps);
        statusColumn.visibleProperty().bind(showingFollowUps);

        InvalidationListener fitColumns = observable -> updateColumnResizePolicy();
        personTable.skinProperty().addListener((observable, oldSkin, newSkin) -> {
            if (rowViewport != null) {
                rowViewport.layoutBoundsProperty().removeListener(fitColumns);
            }
            rowViewport = personTable.lookup(".clipped-container");
            if (rowViewport != null) {
                rowViewport.layoutBoundsProperty().addListener(fitColumns);
            }
            updateColumnResizePolicy();
        });
        personTable.widthProperty().addListener(fitColumns);
        personTable.getVisibleLeafColumns().addListener(fitColumns);

        InvalidationListener refresh = observable -> {
            emptyListLabel.setText(getEmptyListMessage(showingFollowUps.get()));
            personTable.refresh();
        };
        refresh.invalidated(null);
        today.addListener(refresh);
        showingFollowUps.addListener(refresh);
        selectedPerson.addListener(refresh);
    }

    /** Fits columns to the laid-out row viewport, excluding insets and the vertical scrollbar. */
    private void updateColumnResizePolicy() {
        double availableWidth = rowViewport == null
                ? personTable.getWidth() - personTable.getInsets().getLeft() - personTable.getInsets().getRight()
                : rowViewport.getLayoutBounds().getWidth();
        if (availableWidth <= 0) {
            return;
        }
        double minimumWidth = personTable.getVisibleLeafColumns().stream()
                .mapToDouble(TableColumn::getMinWidth).sum();
        personTable.setColumnResizePolicy(availableWidth < minimumWidth
                ? TableView.UNCONSTRAINED_RESIZE_POLICY : TableView.CONSTRAINED_RESIZE_POLICY);
    }

    /** Returns the placeholder text for the active displayed-list mode. */
    static String getEmptyListMessage(boolean showingFollowUps) {
        return showingFollowUps ? FollowUpsCommand.MESSAGE_NO_PENDING : MESSAGE_NO_CLIENTS;
    }

    /**
     * Returns whether {@code rowPerson} is the viewed client. Compares record identity, not names or
     * field values, so two clients sharing a name are never both marked.
     */
    static boolean isViewed(Person rowPerson, Person selectedPerson) {
        return rowPerson != null && rowPerson == selectedPerson;
    }

    /**
     * Builds the text and status style for one row. Due date and status are blank outside pending mode
     * and for clients without a follow-up.
     */
    static RowDisplay createRowDisplay(Person person, LocalDate today, boolean showingFollowUps) {
        requireAllNonNull(person, today);
        String name = person.getName().fullName;
        String phone = person.getPhone().value;
        String email = person.getEmail().value;
        Optional<FollowUp> pendingFollowUp = person.getFollowUp();
        if (!showingFollowUps || pendingFollowUp.isEmpty()) {
            return new RowDisplay(name, phone, email, "", "", "");
        }

        FollowUp followUp = pendingFollowUp.orElseThrow();
        FollowUpStatus status = followUp.getStatus(today);
        String statusStyleClass = switch (status) {
            case OVERDUE -> "follow-up-status-overdue";
            case DUE_TODAY -> "follow-up-status-due-today";
            case UPCOMING -> "follow-up-status-upcoming";
        };
        return new RowDisplay(name, phone, email, followUp.getDueDate().toString(),
                status.getDisplayName(), statusStyleClass);
    }

    /** Text and style values for one table row. */
    record RowDisplay(String name, String phone, String email, String dueDate,
                      String status, String statusStyleClass) {
    }

    /** Highlights the viewed client's row. Table selection is styled like an ordinary row. */
    private class ClientRow extends TableRow<Person> {
        @Override
        protected void updateItem(Person person, boolean empty) {
            super.updateItem(person, empty);
            getStyleClass().remove(VIEWED_STYLE_CLASS);
            if (!empty && isViewed(person, selectedPerson.get())) {
                getStyleClass().add(VIEWED_STYLE_CLASS);
            }
        }
    }

    /**
     * Base cell that renders a filled row from its {@link RowDisplay} and clears itself when empty,
     * so a recycled cell never keeps an old client's text, marker or status.
     */
    private abstract class ClientCell extends TableCell<Person, Person> {
        @Override
        protected void updateItem(Person person, boolean empty) {
            super.updateItem(person, empty);
            setText(null);
            if (empty || person == null) {
                setGraphic(null);
                return;
            }
            render(person, createRowDisplay(person, today.get(), showingFollowUps.get()));
        }

        abstract void render(Person person, RowDisplay row);

        /** Returns a text node that wraps within this cell's column, so long values grow the row height. */
        Text wrappingText(String value) {
            Text text = new Text(value);
            text.getStyleClass().add("client-table-text");
            text.wrappingWidthProperty().bind(getTableColumn().widthProperty().subtract(CELL_HORIZONTAL_PADDING));
            return text;
        }
    }

    private class ClientIdCell extends ClientCell {
        @Override
        void render(Person person, RowDisplay row) {
            setGraphic(null);
            setText(clientIdLookup.apply(person).map(ClientId::toString).orElse(""));
        }
    }

    private class NameCell extends ClientCell {
        @Override
        void render(Person person, RowDisplay row) {
            VBox box = new VBox(2, wrappingText(row.name()));
            if (isViewed(person, selectedPerson.get())) {
                Label marker = new Label("VIEWING");
                marker.getStyleClass().add("viewed-marker");
                box.getChildren().add(marker);
            }
            setGraphic(box);
        }
    }

    private class TextCell extends ClientCell {
        private final Function<RowDisplay, String> field;

        TextCell(Function<RowDisplay, String> field) {
            this.field = field;
        }

        @Override
        void render(Person person, RowDisplay row) {
            setGraphic(wrappingText(field.apply(row)));
        }
    }

    private class StatusCell extends ClientCell {
        @Override
        void render(Person person, RowDisplay row) {
            if (row.status().isEmpty()) {
                setGraphic(null);
                return;
            }
            Label badge = new Label(row.status());
            badge.getStyleClass().addAll("follow-up-status", row.statusStyleClass());
            badge.setMinWidth(Region.USE_PREF_SIZE);
            setGraphic(badge);
        }
    }
}
