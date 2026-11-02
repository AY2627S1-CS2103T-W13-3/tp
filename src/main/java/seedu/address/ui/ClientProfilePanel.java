package seedu.address.ui;

import static java.util.Objects.requireNonNull;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Function;

import javafx.beans.InvalidationListener;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXML;
import javafx.scene.AccessibleRole;
import javafx.scene.Node;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
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
    private static final KeyCombination COPY_SHORTCUT =
            new KeyCodeCombination(KeyCode.C, KeyCombination.SHORTCUT_DOWN);
    private static final List<String> STATUS_STYLE_CLASSES = List.of(
            "profile-status-overdue", "profile-status-due-today", "profile-status-upcoming");

    private final Function<Person, Optional<ClientId>> clientIdLookup;
    private final BiConsumer<String, Boolean> copyFeedback;
    private final Function<String, Boolean> clipboardWriter;
    private final ReadOnlyObjectProperty<Person> selectedPerson;
    private final ReadOnlyObjectProperty<LocalDate> today;
    private final InvalidationListener selectionListener;
    private final ChangeListener<LocalDate> dateListener;
    private Optional<ClientId> renderedClientId;
    private boolean isDisposed;

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
        this(selectedPerson, today, clientIdLookup, (message, isError) -> { });
    }

    /** Reports copy results through the dashboard without changing the command draft. */
    public ClientProfilePanel(ReadOnlyObjectProperty<Person> selectedPerson,
            ReadOnlyObjectProperty<LocalDate> today, Function<Person, Optional<ClientId>> clientIdLookup,
            BiConsumer<String, Boolean> copyFeedback) {
        this(selectedPerson, today, clientIdLookup, copyFeedback, ClientProfilePanel::writeClipboard);
    }

    /** Allows clipboard rejection to be tested independently of the desktop clipboard. */
    ClientProfilePanel(ReadOnlyObjectProperty<Person> selectedPerson,
            ReadOnlyObjectProperty<LocalDate> today, Function<Person, Optional<ClientId>> clientIdLookup,
            BiConsumer<String, Boolean> copyFeedback, Function<String, Boolean> clipboardWriter) {
        super(FXML);
        this.selectedPerson = requireNonNull(selectedPerson);
        this.today = requireNonNull(today);
        this.clientIdLookup = requireNonNull(clientIdLookup);
        this.copyFeedback = requireNonNull(copyFeedback);
        this.clipboardWriter = requireNonNull(clipboardWriter);
        configureCopy(phone, "phone", person -> Optional.of(person.getPhone().value));
        configureCopy(email, "email", person -> Optional.of(person.getEmail().value));
        configureCopy(address, "address", person -> Optional.of(person.getAddress().value));
        configureCopy(followUpAction, "follow-up action",
                person -> person.getFollowUp().map(FollowUp::getDescription));
        renderedClientId = lookupId(selectedPerson.get());
        render(selectedPerson.get(), today.get());
        selectionListener = observable -> refreshSelection();
        dateListener = (observable, oldDate, newDate) -> {
            if (!isDisposed) {
                render(selectedPerson.get(), newDate);
            }
        };
        selectedPerson.addListener(selectionListener);
        today.addListener(dateListener);
    }

    /** Uses the current model value for every activation; rendered labels never supply clipboard content. */
    private void configureCopy(Label label, String field, Function<Person, Optional<String>> valueLookup) {
        MenuItem copy = new MenuItem("Copy " + field);
        copy.setOnAction(event -> copyField(field, valueLookup));
        label.setContextMenu(new ContextMenu(copy));
        label.setFocusTraversable(true);
        label.setAccessibleRole(AccessibleRole.BUTTON);
        label.setAccessibleText("Copy " + field + ". Press Enter, Space or the copy shortcut.");
        label.getStyleClass().add("profile-copyable");
        label.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER || event.getCode() == KeyCode.SPACE
                    || COPY_SHORTCUT.match(event)) {
                copyField(field, valueLookup);
                event.consume();
            }
        });
    }

    private void copyField(String field, Function<Person, Optional<String>> valueLookup) {
        Optional<String> value = isDisposed ? Optional.empty()
                : Optional.ofNullable(selectedPerson.get()).flatMap(valueLookup).filter(text -> !text.isBlank());
        if (value.isEmpty()) {
            copyFeedback.accept("No " + field + " to copy.", true);
            return;
        }
        boolean copied;
        try {
            copied = Boolean.TRUE.equals(clipboardWriter.apply(value.get()));
        } catch (IllegalStateException | SecurityException exception) {
            copied = false;
        }
        copyFeedback.accept(copied ? "Copied " + field + "." : "Could not copy " + field + ".", !copied);
    }

    private static boolean writeClipboard(String value) {
        ClipboardContent content = new ClipboardContent();
        content.putString(value);
        return Clipboard.getSystemClipboard().setContent(content);
    }

    /** Releases external observers; callbacks already in progress also stop refreshing the panel. */
    public void dispose() {
        if (isDisposed) {
            return;
        }
        isDisposed = true;
        selectedPerson.removeListener(selectionListener);
        today.removeListener(dateListener);
    }

    private void refreshSelection() {
        if (isDisposed) {
            return;
        }
        Person newPerson = selectedPerson.get();
        Optional<ClientId> nextId = lookupId(newPerson);
        // Cache the ID: the previous immutable record may already have left the aggregate.
        boolean isSameClient = renderedClientId.isPresent() && renderedClientId.equals(nextId);
        renderedClientId = nextId;
        render(newPerson, today.get());
        if (!isSameClient) {
            ScrollPane scroll = (ScrollPane) getRoot();
            scroll.setVvalue(scroll.getVmin());
        }
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
