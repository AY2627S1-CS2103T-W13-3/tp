package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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

    private static Person person(String phone, LocalDate date) {
        return new PersonBuilder().withName("Rachel Lim").withPhone(phone)
                .withEmail("client" + phone + "@example.com")
                .withFollowUp(new FollowUp(date, "Call client " + phone)).build();
    }
}
