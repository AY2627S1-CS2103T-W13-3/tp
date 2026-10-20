package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.Test;

import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ModifiableObservableListBase;
import javafx.collections.ObservableList;
import seedu.address.model.AddressBook;
import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.UserPrefs;
import seedu.address.model.person.FollowUp;
import seedu.address.model.person.FollowUpStatus;
import seedu.address.model.person.Person;
import seedu.address.testutil.PersonBuilder;

/** Tests the pending summary without requiring a JavaFX window or the G1 Logic API. */
public class PendingFollowUpProjectionTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 20);

    @Test
    public void constructor_nullSourceOrRecord_rejectsProgrammerMisuse() {
        assertThrows(NullPointerException.class, () -> new PendingFollowUpProjection(null));
        ObservableList<Person> invalidSource = FXCollections.observableArrayList();
        invalidSource.add(null);
        assertThrows(NullPointerException.class, () -> new PendingFollowUpProjection(invalidSource));
    }

    @Test
    public void constructor_noPendingActions_returnsEmptyStableReadOnlyList() {
        ObservableList<Person> source = FXCollections.observableArrayList(new PersonBuilder().build());
        try (PendingFollowUpProjection projection = new PendingFollowUpProjection(source)) {
            ObservableList<Person> pending = projection.getPendingPersons();
            assertSame(pending, projection.getPendingPersons());
            assertTrue(pending.isEmpty());
            assertThrows(UnsupportedOperationException.class, () -> pending.add(person("111", TODAY)));
            assertEquals(1, source.size());
        }
    }

    @Test
    public void constructor_sortsAllStatusPartitionsAndPreservesSameNameReferences() {
        Person upcoming = person("111", TODAY.plusDays(1));
        Person overdue = person("222", TODAY.minusDays(1));
        Person due = person("333", TODAY);
        Person tied = person("444", TODAY);
        Person noAction = new PersonBuilder().withName("Amanda Lee").build();
        ObservableList<Person> source = FXCollections.observableArrayList(upcoming, due, noAction, overdue, tied);
        List<Person> original = List.copyOf(source);
        try (PendingFollowUpProjection projection = new PendingFollowUpProjection(source)) {
            ObservableList<Person> pending = projection.getPendingPersons();
            assertEquals(List.of(overdue, due, tied, upcoming), pending);
            assertSame(overdue, pending.get(0));
            assertSame(due, pending.get(1));
            assertSame(tied, pending.get(2));
            assertSame(upcoming, pending.get(3));
            assertEquals(List.of(FollowUpStatus.OVERDUE, FollowUpStatus.DUE_TODAY,
                    FollowUpStatus.DUE_TODAY, FollowUpStatus.UPCOMING), pending.stream()
                    .map(person -> person.getFollowUp().orElseThrow().getStatus(TODAY)).toList());
            assertEquals(original, source);
            assertThrows(UnsupportedOperationException.class, () -> pending.set(0, upcoming));
            assertThrows(UnsupportedOperationException.class, () -> pending.remove(0));
            assertThrows(UnsupportedOperationException.class, pending::clear);
        }
    }

    @Test
    public void sourceMutations_recordReplaceClearAndDelete_refreshCompleteOrder() {
        Person late = person("111", TODAY.plusDays(2));
        Person early = person("222", TODAY);
        Person noAction = new PersonBuilder().withName("Amanda Lee").build();
        ObservableList<Person> source = FXCollections.observableArrayList();
        try (PendingFollowUpProjection projection = new PendingFollowUpProjection(source)) {
            ObservableList<Person> pending = projection.getPendingPersons();
            source.addAll(late, early, noAction);
            assertEquals(List.of(early, late), pending);

            Person recorded = noAction.withFollowUp(new FollowUp(TODAY, "Call Amanda"));
            source.set(2, recorded);
            assertEquals(List.of(early, recorded, late), pending);
            assertSame(recorded, pending.get(1));

            Person replacement = late.withFollowUp(new FollowUp(TODAY.minusDays(1), "Earlier action"));
            source.set(0, replacement);
            assertEquals(List.of(replacement, early, recorded), pending);
            assertSame(replacement, pending.getFirst());

            source.set(1, early.withoutFollowUp());
            assertEquals(List.of(replacement, recorded), pending);
            source.remove(0);
            assertEquals(List.of(recorded), pending);
            assertSame(pending, projection.getPendingPersons());
            assertEquals(2, source.size());
        }
    }

    @Test
    public void sourceResetAndClear_publishCompleteSnapshotsWithoutReplacingOutputList() {
        ObservableList<Person> source = FXCollections.observableArrayList(person("111", TODAY));
        try (PendingFollowUpProjection projection = new PendingFollowUpProjection(source)) {
            ObservableList<Person> pending = projection.getPendingPersons();
            List<List<Person>> publications = new ArrayList<>();
            pending.addListener((ListChangeListener<Person>) change -> publications.add(List.copyOf(pending)));
            Person upcoming = person("222", TODAY.plusDays(1));
            Person overdue = person("333", TODAY.minusDays(1));
            source.setAll(upcoming, overdue);
            assertEquals(List.of(List.of(overdue, upcoming)), publications);
            assertSame(overdue, pending.getFirst());
            assertSame(upcoming, pending.getLast());

            source.clear();
            assertEquals(List.of(List.of(overdue, upcoming), List.of()), publications);
            assertTrue(pending.isEmpty());
            assertSame(pending, projection.getPendingPersons());
        }
    }

    @Test
    public void sourcePermutation_equalDatesFollowNewSourceOrder() {
        Person first = person("111", TODAY);
        Person second = person("222", TODAY);
        Person overdue = person("333", TODAY.minusDays(1));
        ObservableList<Person> source = FXCollections.observableArrayList(first, second, overdue);
        try (PendingFollowUpProjection projection = new PendingFollowUpProjection(source)) {
            assertEquals(List.of(overdue, first, second), projection.getPendingPersons());
            source.sort(Comparator.comparing((Person person) -> person.getPhone().value).reversed());
            assertEquals(List.of(overdue, second, first), source);
            assertEquals(List.of(overdue, second, first), projection.getPendingPersons());
            assertSame(second, projection.getPendingPersons().get(1));
            assertSame(first, projection.getPendingPersons().get(2));
        }
    }

    @Test
    public void commandViewChanges_doNotFilterOrSortGlobalSummary() {
        Person late = person("111", TODAY.plusDays(1));
        Person early = person("222", TODAY);
        AddressBook book = new AddressBook();
        book.addPerson(late);
        book.addPerson(early);
        Model model = new ModelManager(book, new UserPrefs(), TODAY, false);
        try (PendingFollowUpProjection projection =
                new PendingFollowUpProjection(model.getAddressBook().getPersonList())) {
            ObservableList<Person> pending = projection.getPendingPersons();
            List<List<Person>> publications = new ArrayList<>();
            pending.addListener((ListChangeListener<Person>) change -> publications.add(List.copyOf(pending)));
            model.updateFilteredPersonList(person -> person == late);
            assertEquals(List.of(late), model.getFilteredPersonList());
            assertEquals(List.of(early, late), pending);
            model.showPendingFollowUps();
            assertEquals(List.of(early, late), model.getFilteredPersonList());
            model.updateFilteredPersonList(person -> false);
            assertTrue(model.getFilteredPersonList().isEmpty());
            assertEquals(List.of(early, late), pending);
            assertSame(early, pending.getFirst());
            assertSame(late, pending.getLast());
            assertTrue(publications.isEmpty());
        }
    }

    @Test
    public void close_isIdempotentAndStopsUpdatesWhileOtherProjectionRemainsLive() {
        TrackingPersonList source = new TrackingPersonList();
        Person first = person("111", TODAY);
        source.add(first);
        PendingFollowUpProjection closed = new PendingFollowUpProjection(source);
        ObservableList<Person> snapshot = closed.getPendingPersons();
        List<List<Person>> publications = new ArrayList<>();
        snapshot.addListener((ListChangeListener<Person>) change -> publications.add(List.copyOf(snapshot)));
        try (PendingFollowUpProjection active = new PendingFollowUpProjection(source)) {
            assertTrue(source.hasActiveListeners());
            closed.close();
            closed.close();
            assertTrue(source.hasActiveListeners());
            Person second = person("222", TODAY.minusDays(1));
            source.setAll(second);

            assertEquals(List.of(first), snapshot);
            assertSame(snapshot, closed.getPendingPersons());
            assertTrue(publications.isEmpty());
            assertEquals(List.of(second), active.getPendingPersons());
            assertSame(second, active.getPendingPersons().getFirst());
            assertEquals(List.of(second), source);
        } finally {
            closed.close();
        }
        assertFalse(source.hasActiveListeners());
        assertThrows(UnsupportedOperationException.class, snapshot::clear);
    }

    @Test
    public void repeatedConstructionAndDisposal_leaveNoSourceListeners() {
        TrackingPersonList source = new TrackingPersonList();
        source.add(person("111", TODAY));
        for (int i = 0; i < 5; i++) {
            assertFalse(source.hasActiveListeners());
            try (PendingFollowUpProjection projection = new PendingFollowUpProjection(source)) {
                assertTrue(source.hasActiveListeners());
                assertSame(source.getFirst(), projection.getPendingPersons().getFirst());
            }
            assertFalse(source.hasActiveListeners());
        }
    }

    @Test
    public void constructor_invalidInitialRecord_doesNotAttachListener() {
        TrackingPersonList source = new TrackingPersonList();
        source.add(null);
        assertThrows(NullPointerException.class, () -> new PendingFollowUpProjection(source));
        assertFalse(source.hasActiveListeners());
    }

    private static Person person(String phone, LocalDate date) {
        return new PersonBuilder().withName("Rachel Lim").withPhone(phone)
                .withEmail("client" + phone + "@example.com")
                .withFollowUp(new FollowUp(date, "Call client " + phone)).build();
    }

    /** Exposes JavaFX's actual listener-presence check without reflection or garbage-collection tests. */
    private static class TrackingPersonList extends ModifiableObservableListBase<Person> {
        private final List<Person> persons = new ArrayList<>();

        boolean hasActiveListeners() {
            return hasListeners();
        }

        @Override
        public Person get(int index) {
            return persons.get(index);
        }

        @Override
        public int size() {
            return persons.size();
        }

        @Override
        protected void doAdd(int index, Person person) {
            persons.add(index, person);
        }

        @Override
        protected Person doSet(int index, Person person) {
            return persons.set(index, person);
        }

        @Override
        protected Person doRemove(int index) {
            return persons.remove(index);
        }
    }
}
