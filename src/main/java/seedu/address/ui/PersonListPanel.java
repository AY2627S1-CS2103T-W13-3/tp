package seedu.address.ui;

import java.time.LocalDate;
import java.util.logging.Logger;

import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.layout.Region;
import seedu.address.commons.core.LogsCenter;
import seedu.address.logic.commands.FollowUpsCommand;
import seedu.address.model.person.Person;

/**
 * Panel containing the list of persons.
 */
public class PersonListPanel extends UiPart<Region> {
    public static final String MESSAGE_NO_CLIENTS = "No clients to display.";

    private static final String FXML = "PersonListPanel.fxml";
    private final Logger logger = LogsCenter.getLogger(PersonListPanel.class);
    private final ReadOnlyObjectProperty<LocalDate> today;

    @FXML
    private ListView<Person> personListView;

    /**
     * Creates a {@code PersonListPanel} with the given {@code ObservableList}.
     */
    public PersonListPanel(ObservableList<Person> personList, ReadOnlyObjectProperty<LocalDate> today,
                           ReadOnlyBooleanProperty showingFollowUps) {
        super(FXML);
        this.today = today;
        personListView.setItems(personList);
        Label emptyListLabel = new Label(getEmptyListMessage(showingFollowUps.get()));
        emptyListLabel.getStyleClass().add("label-bright");
        personListView.setPlaceholder(emptyListLabel);
        personListView.setCellFactory(listView -> new PersonListViewCell());
        today.addListener((observable, oldDate, newDate) -> personListView.refresh());
        showingFollowUps.addListener((observable, wasShowingFollowUps, isShowingFollowUps) -> {
            emptyListLabel.setText(getEmptyListMessage(isShowingFollowUps));
            personListView.refresh();
        });
    }

    /** Returns the placeholder text for the active displayed-list mode. */
    static String getEmptyListMessage(boolean showingFollowUps) {
        return showingFollowUps ? FollowUpsCommand.MESSAGE_NO_PENDING : MESSAGE_NO_CLIENTS;
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
            if (isEmpty() || person == null || getIndex() < 0) {
                setGraphic(null);
                setText(null);
            } else {
                setGraphic(new PersonCard(person, getIndex() + 1, today.get()).getRoot());
                setText(null);
            }
        }
    }

}
