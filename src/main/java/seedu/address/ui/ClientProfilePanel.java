package seedu.address.ui;

import static java.util.Objects.requireNonNull;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import seedu.address.model.person.FollowUp;
import seedu.address.model.person.FollowUpStatus;
import seedu.address.model.person.Person;

/** Displays the selected client's complete, read-only profile. */
public class ClientProfilePanel extends UiPart<Region> {

    public static final String MESSAGE_NO_CLIENT_SELECTED = "No client selected.";

    private static final String FXML = "ClientProfilePanel.fxml";
    private static final List<String> STATUS_STYLE_CLASSES = List.of(
            "profile-status-overdue", "profile-status-due-today", "profile-status-upcoming");

    @FXML
    private Label noSelectionMessage;
    @FXML
    private VBox profileDetails;
    @FXML
    private Label name;
    @FXML
    private Label phone;
    @FXML
    private Label email;
    @FXML
    private Label address;
    @FXML
    private Label tagsNone;
    @FXML
    private FlowPane tagChips;
    @FXML
    private Label followUpNone;
    @FXML
    private VBox followUpDetails;
    @FXML
    private Label followUpDate;
    @FXML
    private Label followUpAction;
    @FXML
    private Label followUpStatus;

    /** Creates a panel that follows the live selection and published status date. */
    public ClientProfilePanel(ReadOnlyObjectProperty<Person> selectedPerson,
            ReadOnlyObjectProperty<LocalDate> today) {
        super(FXML);
        requireNonNull(selectedPerson);
        requireNonNull(today);
        selectedPerson.addListener((observable, oldPerson, newPerson) -> render(newPerson, today.get()));
        today.addListener((observable, oldDate, newDate) -> render(selectedPerson.get(), newDate));
        render(selectedPerson.get(), today.get());
    }

    private void render(Person selectedPerson, LocalDate today) {
        ProfileDisplay display = createDisplay(selectedPerson, today);
        noSelectionMessage.setText(display.noSelection());
        noSelectionMessage.setVisible(!display.hasSelection());
        noSelectionMessage.setManaged(!display.hasSelection());
        profileDetails.setVisible(display.hasSelection());
        profileDetails.setManaged(display.hasSelection());

        name.setText(display.name());
        phone.setText(display.phone());
        email.setText(display.email());
        address.setText(display.address());
        tagChips.getChildren().clear();
        display.tags().forEach(tag -> {
            Label chip = new WrappingTagChip(tag);
            chip.getStyleClass().add("profile-tag-chip");
            chip.setWrapText(true);
            chip.setMinWidth(0);
            chip.setMinHeight(Region.USE_PREF_SIZE);
            chip.maxWidthProperty().bind(tagChips.widthProperty());
            tagChips.getChildren().add(chip);
        });
        tagsNone.setVisible(display.hasSelection() && display.tags().isEmpty());
        tagsNone.setManaged(display.hasSelection() && display.tags().isEmpty());

        FollowUpDisplay followUp = display.followUp();
        followUpNone.setText(followUp.none());
        followUpNone.setVisible(display.hasSelection() && !followUp.hasFollowUp());
        followUpNone.setManaged(display.hasSelection() && !followUp.hasFollowUp());
        followUpDetails.setVisible(display.hasSelection() && followUp.hasFollowUp());
        followUpDetails.setManaged(display.hasSelection() && followUp.hasFollowUp());
        followUpDate.setText(followUp.date());
        followUpAction.setText(followUp.action());
        followUpStatus.setText(followUp.status());
        followUpStatus.getStyleClass().removeAll(STATUS_STYLE_CLASSES);
        if (followUp.hasFollowUp()) {
            followUpStatus.getStyleClass().add(followUp.statusStyleClass());
        }
    }

    /** Builds the exact visible values without reading a clock or changing model state. */
    static ProfileDisplay createDisplay(Person person, LocalDate today) {
        requireNonNull(today);
        if (person == null) {
            return new ProfileDisplay(false, MESSAGE_NO_CLIENT_SELECTED, "", "", "", "", List.of(),
                    new FollowUpDisplay(false, "", "", "", "", ""));
        }

        FollowUpDisplay followUpDisplay = person.getFollowUp()
                .map(followUp -> createFollowUpDisplay(followUp, today))
                .orElseGet(() -> new FollowUpDisplay(false, "Follow-up: None", "", "", "", ""));
        return new ProfileDisplay(true, "", person.getName().fullName,
                person.getPhone().value,
                person.getEmail().value,
                person.getAddress().value,
                person.getTags().stream().map(tag -> tag.tagName).sorted(Comparator.naturalOrder()).toList(),
                followUpDisplay);
    }

    private static FollowUpDisplay createFollowUpDisplay(FollowUp followUp, LocalDate today) {
        FollowUpStatus status = followUp.getStatus(today);
        String statusStyleClass = switch (status) {
            case OVERDUE -> "profile-status-overdue";
            case DUE_TODAY -> "profile-status-due-today";
            case UPCOMING -> "profile-status-upcoming";
        };
        return new FollowUpDisplay(true, "",
                followUp.getDueDate().toString(),
                followUp.getDescription(),
                status.getDisplayName(), statusStyleClass);
    }

    /** Lets FlowPane measure a wrapped chip's height using its available width. */
    private static class WrappingTagChip extends Label {
        WrappingTagChip(String tag) {
            super(tag);
        }

        @Override
        protected double computePrefHeight(double width) {
            return super.computePrefHeight(width < 0 ? getMaxWidth() : width);
        }
    }

    /** Complete text and visibility state for one profile refresh. */
    record ProfileDisplay(boolean hasSelection, String noSelection, String name, String phone,
                          String email, String address, List<String> tags, FollowUpDisplay followUp) {
    }

    /** Follow-up text and style for one profile refresh. */
    record FollowUpDisplay(boolean hasFollowUp, String none, String date, String action,
                           String status, String statusStyleClass) {
    }
}
