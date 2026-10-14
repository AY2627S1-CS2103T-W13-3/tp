package seedu.address.ui;

import java.time.LocalDate;

import javafx.beans.InvalidationListener;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.Region;
import seedu.address.logic.commands.FollowUpsCommand;
import seedu.address.model.person.Person;

/**
 * Panel containing the current authoritative numbered client list.
 * It only displays the model's state: clicking or highlighting a row never changes the viewed client.
 */
public class PersonListPanel extends UiPart<Region> {
    public static final String MESSAGE_NO_CLIENTS = "No clients to display.";

    private static final String FXML = "PersonListPanel.fxml";
    private final ReadOnlyObjectProperty<LocalDate> today;
    private final ReadOnlyBooleanProperty showingFollowUps;
    private final ReadOnlyObjectProperty<Person> selectedPerson;

    @FXML
    private ListView<Person> personListView;

    /**
     * Creates a {@code PersonListPanel} that renders {@code personList} in the current list mode and
     * marks the row whose client is {@code selectedPerson}.
     */
    public PersonListPanel(ObservableList<Person> personList, ReadOnlyObjectProperty<LocalDate> today,
                           ReadOnlyBooleanProperty showingFollowUps,
                           ReadOnlyObjectProperty<Person> selectedPerson) {
        super(FXML);
        this.today = today;
        this.showingFollowUps = showingFollowUps;
        this.selectedPerson = selectedPerson;
        personListView.setItems(personList);
        Label emptyListLabel = new Label(getEmptyListMessage(showingFollowUps.get()));
        emptyListLabel.getStyleClass().add("label-bright");
        personListView.setPlaceholder(emptyListLabel);
        personListView.setCellFactory(listView -> new PersonListViewCell());

        InvalidationListener refresh = observable -> {
            emptyListLabel.setText(getEmptyListMessage(showingFollowUps.get()));
            personListView.refresh();
        };
        today.addListener(refresh);
        showingFollowUps.addListener(refresh);
        selectedPerson.addListener(refresh);
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
     * Custom {@code ListCell} that displays the graphics of a {@code Person} using a {@code PersonCard}.
     */
    class PersonListViewCell extends ListCell<Person> {
        @Override
        protected void updateItem(Person person, boolean empty) {
            super.updateItem(person, empty);
            updateGraphic();
        }

        @Override
        public void updateIndex(int index) {
            super.updateIndex(index);
            updateGraphic();
        }

        private void updateGraphic() {
            Person person = getItem();
            setText(null);
            if (isEmpty() || person == null || getIndex() < 0) {
                setGraphic(null);
                return;
            }
            PersonCard card = new PersonCard(person, getIndex() + 1, today.get(), showingFollowUps.get());
            card.setViewed(isViewed(person, selectedPerson.get()));
            setGraphic(card.getRoot());
        }
    }

}
