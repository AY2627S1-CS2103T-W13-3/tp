package seedu.address.ui;

import java.time.LocalDate;
import java.util.Comparator;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import seedu.address.model.person.FollowUp;
import seedu.address.model.person.FollowUpStatus;
import seedu.address.model.person.Person;

/**
 * A UI component that displays information of a {@code Person}.
 */
public class PersonCard extends UiPart<Region> {

    private static final String FXML = "PersonListCard.fxml";

    /**
     * Note: Certain keywords such as "location" and "resources" are reserved keywords in JavaFX.
     * As a consequence, UI elements' variable names cannot be set to such keywords
     * or an exception will be thrown by JavaFX during runtime.
     *
     * @see <a href="https://github.com/se-edu/addressbook-level4/issues/336">The issue on AddressBook level 4</a>
     */

    public final Person person;

    @FXML
    private HBox cardPane;
    @FXML
    private Label name;
    @FXML
    private Label id;
    @FXML
    private Label phone;
    @FXML
    private Label address;
    @FXML
    private Label email;
    @FXML
    private FlowPane tags;
    @FXML
    private Label followUpNone;
    @FXML
    private VBox followUpDetails;
    @FXML
    private Label followUpDueDate;
    @FXML
    private Label followUpDescription;
    @FXML
    private Label followUpStatus;

    /**
     * Creates a {@code PersonCard} with the given {@code Person} and index to display.
     */
    public PersonCard(Person person, int displayedIndex, LocalDate today) {
        super(FXML);
        this.person = person;
        id.setText(displayedIndex + ". ");
        name.setText(person.getName().fullName);
        phone.setText(person.getPhone().value);
        address.setText(person.getAddress().value);
        email.setText(person.getEmail().value);
        person.getTags().stream()
                .sorted(Comparator.comparing(tag -> tag.tagName))
                .forEach(tag -> tags.getChildren().add(new Label(tag.tagName)));

        FollowUpDisplay followUpDisplay = createFollowUpDisplay(person, today);
        followUpNone.setText(followUpDisplay.noneText());
        followUpNone.setVisible(!followUpDisplay.hasFollowUp());
        followUpNone.setManaged(!followUpDisplay.hasFollowUp());
        followUpDetails.setVisible(followUpDisplay.hasFollowUp());
        followUpDetails.setManaged(followUpDisplay.hasFollowUp());
        followUpDueDate.setText(followUpDisplay.dueText());
        followUpDescription.setText(followUpDisplay.actionText());
        followUpStatus.setText(followUpDisplay.statusText());
        if (followUpDisplay.hasFollowUp()) {
            followUpStatus.getStyleClass().add(followUpDisplay.statusStyleClass());
        }
    }

    /**
     * Builds the text and style values used to render a person's follow-up.
     */
    static FollowUpDisplay createFollowUpDisplay(Person person, LocalDate today) {
        if (person.getFollowUp().isEmpty()) {
            return new FollowUpDisplay(false, "Follow-up: None", "", "", "", "");
        }

        FollowUp followUp = person.getFollowUp().orElseThrow();
        FollowUpStatus status = followUp.getStatus(today);
        String statusStyleClass = switch (status) {
            case OVERDUE -> "follow-up-status-overdue";
            case DUE_TODAY -> "follow-up-status-due-today";
            case UPCOMING -> "follow-up-status-upcoming";
        };
        return new FollowUpDisplay(
                true,
                "",
                "Due: " + followUp.getDueDate(),
                "Action: " + followUp.getDescription(),
                "Status: " + status.getDisplayName(),
                statusStyleClass);
    }

    /** Text and style values for a follow-up section in a person card. */
    record FollowUpDisplay(boolean hasFollowUp, String noneText, String dueText, String actionText,
                           String statusText, String statusStyleClass) {
    }
}
