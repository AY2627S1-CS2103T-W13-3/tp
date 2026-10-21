package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import seedu.address.model.person.FollowUp;
import seedu.address.model.person.Person;
import seedu.address.testutil.PersonBuilder;

public class ClientTableTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 12);

    @Test
    public void toDisplayedIndex_rowIndex_isOneBased() {
        assertEquals(1, PersonListPanel.toDisplayedIndex(0));
        assertEquals(42, PersonListPanel.toDisplayedIndex(41));
    }

    @Test
    public void fxml_everyColumn_isUnsortable() throws IOException {
        String fxml;
        try (InputStream in = PersonListPanel.class.getResourceAsStream("/view/PersonListPanel.fxml")) {
            fxml = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        Matcher column = Pattern.compile("<TableColumn\\b[^>]*>").matcher(fxml);
        int columns = 0;
        while (column.find()) {
            columns++;
            assertEquals(true, column.group().contains("sortable=\"false\""),
                    "Sorting would make # disagree with command indices: " + column.group());
        }
        assertEquals(6, columns);
    }

    @Test
    public void createRowDisplay_ordinaryRowWithFollowUp_showsContactOnly() {
        Person person = new PersonBuilder().withName("Rachel Lim").withPhone("91234567")
                .withEmail("rachel@example.com").withFollowUp(new FollowUp(TODAY, "Call client")).build();

        PersonListPanel.RowDisplay row = PersonListPanel.createRowDisplay(person, TODAY, false);

        assertEquals(new PersonListPanel.RowDisplay("Rachel Lim", "91234567", "rachel@example.com",
                "", "", ""), row);
    }

    @Test
    public void createRowDisplay_pendingRowWithoutFollowUp_showsNoStatus() {
        Person person = new PersonBuilder().withoutFollowUp().build();

        PersonListPanel.RowDisplay row = PersonListPanel.createRowDisplay(person, TODAY, true);

        assertEquals("", row.dueDate());
        assertEquals("", row.status());
        assertEquals("", row.statusStyleClass());
    }

    @Test
    public void createRowDisplay_overdueFollowUp_showsDateAndStatus() {
        PersonListPanel.RowDisplay row = pendingRow(TODAY.minusDays(1));

        assertEquals("2026-10-11", row.dueDate());
        assertEquals("OVERDUE", row.status());
        assertEquals("follow-up-status-overdue", row.statusStyleClass());
    }

    @Test
    public void createRowDisplay_dueTodayFollowUp_showsDueTodayStatus() {
        PersonListPanel.RowDisplay row = pendingRow(TODAY);

        assertEquals("2026-10-12", row.dueDate());
        assertEquals("DUE TODAY", row.status());
        assertEquals("follow-up-status-due-today", row.statusStyleClass());
    }

    @Test
    public void createRowDisplay_upcomingFollowUp_showsUpcomingStatus() {
        PersonListPanel.RowDisplay row = pendingRow(TODAY.plusDays(1));

        assertEquals("2026-10-13", row.dueDate());
        assertEquals("UPCOMING", row.status());
        assertEquals("follow-up-status-upcoming", row.statusStyleClass());
    }

    @Test
    public void createRowDisplay_suppliedDateChanges_recalculatesWithoutChangingFollowUp() {
        Person person = new PersonBuilder().withFollowUp(new FollowUp(TODAY, "Call client")).build();

        assertEquals("UPCOMING", PersonListPanel.createRowDisplay(person, TODAY.minusDays(1), true).status());
        assertEquals("DUE TODAY", PersonListPanel.createRowDisplay(person, TODAY, true).status());
        assertEquals("OVERDUE", PersonListPanel.createRowDisplay(person, TODAY.plusDays(1), true).status());
        assertEquals(TODAY, person.getFollowUp().orElseThrow().getDueDate());
    }

    @Test
    public void createRowDisplay_longValues_preservesCompleteText() {
        String longName = "Alexandra " + "B".repeat(80);
        Person person = new PersonBuilder().withName(longName).build();

        assertEquals(longName, PersonListPanel.createRowDisplay(person, TODAY, false).name());
    }

    @Test
    public void createRowDisplay_nullArgument_throwsNullPointerException() {
        Person person = new PersonBuilder().build();

        assertThrows(NullPointerException.class, () -> PersonListPanel.createRowDisplay(null, TODAY, false));
        assertThrows(NullPointerException.class, () -> PersonListPanel.createRowDisplay(person, null, true));
    }

    private PersonListPanel.RowDisplay pendingRow(LocalDate dueDate) {
        Person person = new PersonBuilder().withFollowUp(new FollowUp(dueDate, "Call client")).build();
        return PersonListPanel.createRowDisplay(person, TODAY, true);
    }
}
