package seedu.address.ui;

import static java.util.Objects.requireNonNull;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import javafx.beans.InvalidationListener;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import seedu.address.model.person.ClientId;
import seedu.address.model.person.FollowUp;
import seedu.address.model.person.FollowUpStatus;
import seedu.address.model.person.Person;

/**
 * Read-only summary of every client's pending follow-up, labelled with permanent Client IDs.
 * The input list supplies due-date order independently of the filtered table.
 */
public class PendingFollowUpPanel extends UiPart<Region> {

    private static final String FXML = "PendingFollowUpPanel.fxml";

    private final Function<Person, Optional<ClientId>> clientIdLookup;
    private final ObservableList<Person> pendingPersons;
    private final ReadOnlyObjectProperty<LocalDate> today;
    private final ReadOnlyObjectProperty<Person> selectedPerson;
    private final ListChangeListener<Person> pendingListener;
    private final ChangeListener<LocalDate> dateListener;
    private final InvalidationListener selectionListener;
    private final Map<Person, CardNodes> renderedCards = new IdentityHashMap<>();
    private Person renderedSelection;
    private boolean disposed;

    @FXML
    private Label emptyMessage;
    @FXML
    private VBox overdueGroup;
    @FXML
    private VBox dueTodayGroup;
    @FXML
    private VBox upcomingGroup;
    @FXML
    private VBox overdueCards;
    @FXML
    private VBox dueTodayCards;
    @FXML
    private VBox upcomingCards;

    /**
     * Creates a panel from the already-sorted pending feed and published date.
     * The selected record is used only for an identity-based visual marker.
     */
    public PendingFollowUpPanel(ObservableList<Person> pendingPersons,
            ReadOnlyObjectProperty<LocalDate> today,
            ReadOnlyObjectProperty<Person> selectedPerson, Function<Person, Optional<ClientId>> clientIdLookup) {
        super(FXML);
        this.clientIdLookup = requireNonNull(clientIdLookup);
        this.pendingPersons = requireNonNull(pendingPersons);
        this.today = requireNonNull(today);
        this.selectedPerson = requireNonNull(selectedPerson);

        // Validate and render before registering listeners, so bad initial data
        // cannot leave a partially constructed panel subscribed to the feed.
        render(pendingPersons, today.get(), selectedPerson.get());
        pendingListener = change -> refresh();
        dateListener = (observable, oldDate, newDate) -> refresh();
        // ChangeListener suppresses equal-valued replacements, but our marker follows identity.
        selectionListener = observable -> {
            if (!disposed) {
                updateSelection(this.selectedPerson.get());
            }
        };
        pendingPersons.addListener(pendingListener);
        today.addListener(dateListener);
        selectedPerson.addListener(selectionListener);
    }

    /** Stops observing external state when the dashboard removes this panel. */
    public void dispose() {
        if (disposed) {
            return;
        }
        // A notification already in progress may still invoke a removed listener.
        disposed = true;
        pendingPersons.removeListener(pendingListener);
        today.removeListener(dateListener);
        selectedPerson.removeListener(selectionListener);
        renderedCards.clear();
        renderedSelection = null;
    }

    private void refresh() {
        if (!disposed) {
            render(pendingPersons, today.get(), selectedPerson.get());
        }
    }

    private void render(List<Person> pendingPersons, LocalDate today, Person selectedPerson) {
        PendingDisplay display = createDisplay(pendingPersons, today, selectedPerson);
        renderedCards.clear();
        showGroup(overdueGroup, overdueCards, display.overdue());
        showGroup(dueTodayGroup, dueTodayCards, display.dueToday());
        showGroup(upcomingGroup, upcomingCards, display.upcoming());
        emptyMessage.setVisible(display.isEmpty());
        emptyMessage.setManaged(display.isEmpty());
        renderedSelection = selectedPerson;
    }

    private void showGroup(VBox group, VBox cards, List<PendingCard> cardDisplays) {
        group.setVisible(!cardDisplays.isEmpty());
        group.setManaged(!cardDisplays.isEmpty());
        cards.getChildren().setAll(cardDisplays.stream().map(this::createCard).toList());
    }

    private VBox createCard(PendingCard card) {
        Label name = wrappingCardLabel(card.name(), "pending-card-name");

        Label viewing = new Label("VIEWING");
        viewing.getStyleClass().add("pending-viewing-marker");
        viewing.setVisible(card.isViewing());
        viewing.setManaged(card.isViewing());

        Label date = new Label("Due " + card.date());
        date.getStyleClass().add("pending-card-date");

        Label action = wrappingCardLabel(card.action(), "pending-card-action");

        Label status = new Label(card.status());
        status.getStyleClass().addAll("pending-card-status", card.statusStyleClass());

        Label clientId = wrappingCardLabel(
                clientIdLookup.apply(card.person()).map(id -> "Client ID " + id).orElse(""), "pending-card-id");

        VBox container = new VBox(6, name, viewing, date, action, status, clientId);
        container.getStyleClass().add("pending-card");
        if (card.isViewing()) {
            container.getStyleClass().add("pending-card-viewing");
        }
        renderedCards.put(card.person(), new CardNodes(container, viewing));
        return container;
    }

    /** Creates a card label that can wrap long names, descriptions and IDs. */
    private static Label wrappingCardLabel(String text, String styleClass) {
        Label label = new Label(text);
        label.getStyleClass().add(styleClass);
        label.setWrapText(true);
        label.setMaxWidth(Double.MAX_VALUE);
        return label;
    }

    private void updateSelection(Person newPerson) {
        if (renderedSelection == newPerson) {
            return;
        }
        setViewing(renderedSelection, false);
        setViewing(newPerson, true);
        renderedSelection = newPerson;
    }

    private void setViewing(Person person, boolean isViewing) {
        CardNodes nodes = renderedCards.get(person);
        if (nodes == null) {
            return;
        }
        nodes.viewing().setVisible(isViewing);
        nodes.viewing().setManaged(isViewing);
        if (isViewing) {
            if (!nodes.container().getStyleClass().contains("pending-card-viewing")) {
                nodes.container().getStyleClass().add("pending-card-viewing");
            }
        } else {
            nodes.container().getStyleClass().remove("pending-card-viewing");
        }
    }

    /** Builds complete visible values without sampling a clock or mutating the feed. */
    static PendingDisplay createDisplay(List<Person> pendingPersons, LocalDate today, Person selectedPerson) {
        requireNonNull(pendingPersons);
        requireNonNull(today);
        List<PendingCard> overdue = new ArrayList<>();
        List<PendingCard> dueToday = new ArrayList<>();
        List<PendingCard> upcoming = new ArrayList<>();

        for (Person person : pendingPersons) {
            requireNonNull(person, "Pending feed must not contain null clients");
            FollowUp followUp = person.getFollowUp().orElseThrow(() ->
                    new IllegalArgumentException("Pending feed contains a client without a follow-up"));
            FollowUpStatus status = followUp.getStatus(today);
            PendingCard card = new PendingCard(person, person.getName().fullName,
                    followUp.getDueDate().toString(), followUp.getDescription(), status.getDisplayName(),
                    statusStyleClass(status), person == selectedPerson);
            switch (status) {
                case OVERDUE -> overdue.add(card);
                case DUE_TODAY -> dueToday.add(card);
                case UPCOMING -> upcoming.add(card);
                default -> throw new AssertionError("Unsupported follow-up status");
            }
        }
        return new PendingDisplay(List.copyOf(overdue), List.copyOf(dueToday), List.copyOf(upcoming));
    }

    private static String statusStyleClass(FollowUpStatus status) {
        return switch (status) {
            case OVERDUE -> "pending-status-overdue";
            case DUE_TODAY -> "pending-status-due-today";
            case UPCOMING -> "pending-status-upcoming";
        };
    }

    /** The three fixed visual groups, each retaining its input order. */
    record PendingDisplay(List<PendingCard> overdue, List<PendingCard> dueToday, List<PendingCard> upcoming) {
        boolean isEmpty() {
            return overdue.isEmpty() && dueToday.isEmpty() && upcoming.isEmpty();
        }
    }

    /** Full text and identity marker for one pending card. */
    record PendingCard(Person person, String name, String date, String action, String status,
                       String statusStyleClass, boolean isViewing) {
    }

    private record CardNodes(VBox container, Label viewing) {
    }
}
