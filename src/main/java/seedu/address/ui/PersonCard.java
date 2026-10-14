package seedu.address.ui;

import java.time.LocalDate;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import seedu.address.model.person.FollowUp;
import seedu.address.model.person.FollowUpStatus;
import seedu.address.model.person.Person;

/**
 * A UI component that displays one numbered row of the current client list.
 * Full/search rows show only contact essentials; pending rows add the follow-up details.
 */
public class PersonCard extends UiPart<Region> {

    static final String VIEWED_STYLE_CLASS = "viewed-client";

    private static final String FXML = "PersonListCard.fxml";
    private static final FollowUpDisplay NO_FOLLOW_UP = new FollowUpDisplay(false, "", "", "", "");

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
    private Label email;
    @FXML
    private Label viewedMarker;
    @FXML
    private VBox followUpDetails;
    @FXML
    private Label followUpDueDate;
    @FXML
    private Label followUpDescription;
    @FXML
    private Label followUpStatus;

    /**
     * Creates an ordinary (full/search) row for {@code person}.
     */
    public PersonCard(Person person, int displayedIndex, LocalDate today) {
        this(person, displayedIndex, today, false);
    }

    /**
     * Creates a row for {@code person}; pending rows also show the follow-up date, action and status.
     */
    public PersonCard(Person person, int displayedIndex, LocalDate today, boolean showingFollowUps) {
        super(FXML);
        this.person = person;
        id.setText(displayedIndex + ". ");
        name.setText(person.getName().fullName);
        phone.setText(person.getPhone().value);
        email.setText(person.getEmail().value);

        FollowUpDisplay followUpDisplay = createFollowUpDisplay(person, today, showingFollowUps);
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
     * Marks this row as the client currently open in the profile panel.
     */
    public void setViewed(boolean isViewed) {
        viewedMarker.setVisible(isViewed);
        viewedMarker.setManaged(isViewed);
        cardPane.getStyleClass().remove(VIEWED_STYLE_CLASS);
        if (isViewed) {
            cardPane.getStyleClass().add(VIEWED_STYLE_CLASS);
        }
    }

    /**
     * Builds the follow-up text and style for a row. Only pending rows show follow-up details.
     */
    static FollowUpDisplay createFollowUpDisplay(Person person, LocalDate today, boolean showingFollowUps) {
        if (!showingFollowUps || person.getFollowUp().isEmpty()) {
            return NO_FOLLOW_UP;
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
                "Due: " + followUp.getDueDate(),
                "Action: " + followUp.getDescription(),
                status.getDisplayName(),
                statusStyleClass);
    }

    /** Text and style values for the follow-up section of a pending row. */
    record FollowUpDisplay(boolean hasFollowUp, String dueText, String actionText,
                           String statusText, String statusStyleClass) {
    }
}
