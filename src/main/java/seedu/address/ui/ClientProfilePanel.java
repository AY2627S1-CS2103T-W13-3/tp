package seedu.address.ui;

import static java.util.Objects.requireNonNull;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import javafx.beans.InvalidationListener;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import seedu.address.model.person.ClientId;
import seedu.address.model.person.FollowUp;
import seedu.address.model.person.FollowUpStatus;
import seedu.address.model.person.Person;

/** Displays the selected client's complete, read-only profile. */
public class ClientProfilePanel extends UiPart<Region> {

    public static final String MESSAGE_NO_CLIENT_SELECTED = "No client selected.";

    private static final String FXML = "ClientProfilePanel.fxml";
    private static final List<String> STATUS_STYLE_CLASSES = List.of(
            "profile-status-overdue", "profile-status-due-today", "profile-status-upcoming");

    private final Function<Person, Optional<ClientId>> clientIdLookup;
    private Optional<ClientId> renderedClientId;

    @FXML
    private Label noSelectionMessage;
    @FXML
    private VBox profileDetails;

    @FXML
    private Label clientId;
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
            ReadOnlyObjectProperty<LocalDate> today, Function<Person, Optional<ClientId>> clientIdLookup) {
        super(FXML);
        requireNonNull(selectedPerson);
        requireNonNull(today);
        this.clientIdLookup = requireNonNull(clientIdLookup);
        renderedClientId = lookupId(selectedPerson.get());
        // Cache the ID: the previous immutable record may already have left the aggregate.
        selectedPerson.addListener((InvalidationListener) observable -> {
            Person newPerson = selectedPerson.get();
            Optional<ClientId> nextId = lookupId(newPerson);
            boolean isSameClient = renderedClientId.isPresent() && renderedClientId.equals(nextId);
            renderedClientId = nextId;
            render(newPerson, today.get());
            if (!isSameClient) {
                ScrollPane scroll = (ScrollPane) getRoot();
                scroll.setVvalue(scroll.getVmin());
            }
        });
        today.addListener((observable, oldDate, newDate) -> render(selectedPerson.get(), newDate));
        render(selectedPerson.get(), today.get());
    }

    private Optional<ClientId> lookupId(Person person) {
        return person == null ? Optional.empty() : clientIdLookup.apply(person);
    }

    private void render(Person selectedPerson, LocalDate today) {
        ProfileDisplay display = createDisplay(selectedPerson, today);
        noSelectionMessage.setText(display.noSelection());
        setShown(noSelectionMessage, !display.hasSelection());
        setShown(profileDetails, display.hasSelection());
        renderContactDetails(selectedPerson, display);
        renderTags(display);
        renderFollowUp(display);
    }

    /** Updates contact values without changing the selection or its scroll position. */
    private void renderContactDetails(Person selectedPerson, ProfileDisplay display) {
        clientId.setText(lookupId(selectedPerson).map(id -> "Client ID " + id).orElse(""));
        name.setText(display.name());
        phone.setText(display.phone());
        email.setText(display.email());
        address.setText(display.address());
    }

    private void renderTags(ProfileDisplay display) {
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
        setShown(tagsNone, display.hasSelection() && display.tags().isEmpty());
    }

    private void renderFollowUp(ProfileDisplay display) {
        FollowUpDisplay followUp = display.followUp();
        followUpNone.setText(followUp.none());
        setShown(followUpNone, display.hasSelection() && !followUp.hasFollowUp());
        setShown(followUpDetails, display.hasSelection() && followUp.hasFollowUp());
        followUpDate.setText(followUp.date());
        followUpAction.setText(followUp.action());
        followUpStatus.setText(followUp.status());
        followUpStatus.getStyleClass().removeAll(STATUS_STYLE_CLASSES);
        if (followUp.hasFollowUp()) {
            followUpStatus.getStyleClass().add(followUp.statusStyleClass());
        }
    }

    /** Keeps layout participation in sync with visibility. */
    private static void setShown(Node node, boolean isShown) {
        node.setVisible(isShown);
        node.setManaged(isShown);
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
