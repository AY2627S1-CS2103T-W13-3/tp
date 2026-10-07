package seedu.address.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.Assert.assertThrows;
import static seedu.address.testutil.TypicalPersons.ALICE;
import static seedu.address.testutil.TypicalPersons.BENSON;
import static seedu.address.testutil.TypicalPersons.CARL;
import static seedu.address.testutil.TypicalPersons.DANIEL;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;

import javafx.collections.ObservableList;
import seedu.address.model.AddressBook;
import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.UserPrefs;
import seedu.address.model.person.FollowUp;
import seedu.address.model.person.Person;
import seedu.address.testutil.AddressBookBuilder;

public class FollowUpsCommandTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 4);
    private final FollowUpsCommand command = new FollowUpsCommand();

    @Test
    public void execute_nullModel_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> command.execute(null));
    }

    @Test
    public void execute_afterSearch_showsAllPendingInStableDueDateOrder() {
        Person later = ALICE.withFollowUp(new FollowUp(TODAY.plusDays(2), "Call Alice"));
        Person overdue = BENSON.withFollowUp(new FollowUp(TODAY.minusDays(1), "Call Benson"));
        Person tied = DANIEL.withFollowUp(new FollowUp(TODAY.minusDays(1), "Call Daniel"));
        Person dueToday = CARL.withFollowUp(new FollowUp(TODAY, "Call Carl"));
        AddressBook data = new AddressBookBuilder().withPerson(later).withPerson(overdue)
                .withPerson(dueToday).withPerson(tied).build();
        Model model = new ModelManager(data, new UserPrefs(), TODAY, false);
        model.updateFilteredPersonList(person -> person.equals(later));
        ObservableList<Person> displayed = model.getFilteredPersonList();

        assertEquals(new CommandResult("Pending follow-ups"), command.execute(model));

        assertEquals(List.of(overdue, tied, dueToday, later), displayed);
        assertSame(displayed, model.getFilteredPersonList());
        assertEquals(data, model.getAddressBook());
        assertTrue(model.showingFollowUpsProperty().get());
        assertFalse(model.hasUnsavedChanges());
        assertEquals(TODAY, model.getToday());
        assertEquals(new CommandResult("Pending follow-ups"), command.execute(model));
        assertEquals(List.of(overdue, tied, dueToday, later), displayed);
        assertFalse(model.hasUnsavedChanges());
    }

    @Test
    public void execute_clientsWithoutFollowUps_excludesThemFromPendingList() {
        Person pending = BENSON.withFollowUp(new FollowUp(TODAY, "Call Benson"));
        AddressBook data = new AddressBookBuilder().withPerson(ALICE).withPerson(pending).build();
        Model model = new ModelManager(data, new UserPrefs(), TODAY, false);

        assertEquals(new CommandResult("Pending follow-ups"), command.execute(model));
        assertEquals(List.of(pending), model.getFilteredPersonList());
        assertEquals(data, model.getAddressBook());
        assertFalse(model.hasUnsavedChanges());
    }

    @Test
    public void execute_noPendingFollowUps_showsEmptyPendingView() {
        AddressBook data = new AddressBookBuilder().withPerson(ALICE).build();
        Model model = new ModelManager(data, new UserPrefs(), TODAY, false);

        assertEquals(new CommandResult("There are no pending follow-ups."), command.execute(model));
        assertTrue(model.getFilteredPersonList().isEmpty());
        assertTrue(model.showingFollowUpsProperty().get());
        assertEquals(data, model.getAddressBook());
        assertFalse(model.hasUnsavedChanges());
    }

    @Test
    public void execute_emptyBlockedSession_allowsReadOnlyQuery() {
        Model model = new ModelManager(new AddressBook(), new UserPrefs(), TODAY, true);

        assertEquals(new CommandResult("There are no pending follow-ups."), command.execute(model));
        assertTrue(model.getFilteredPersonList().isEmpty());
        assertTrue(model.showingFollowUpsProperty().get());
        assertTrue(model.isDataLoadingBlocked());
        assertFalse(model.hasUnsavedChanges());
    }

    @Test
    public void execute_candidate_keepsLiveViewUntilPublication() {
        Person pending = BENSON.withFollowUp(new FollowUp(TODAY, "Call Benson"));
        AddressBook data = new AddressBookBuilder().withPerson(ALICE).withPerson(pending).build();
        Model live = new ModelManager(data, new UserPrefs(), TODAY, false);
        Model candidate = live.forkForCommand(TODAY.plusDays(1));

        command.execute(candidate);

        assertEquals(List.of(pending), candidate.getFilteredPersonList());
        assertFalse(candidate.hasUnsavedChanges());
        assertEquals(List.of(ALICE, pending), live.getFilteredPersonList());
        assertFalse(live.showingFollowUpsProperty().get());
        assertEquals(TODAY, live.getToday());
        live.commitFrom(candidate);
        assertEquals(List.of(pending), live.getFilteredPersonList());
        assertTrue(live.showingFollowUpsProperty().get());
        assertEquals(TODAY.plusDays(1), live.getToday());
        assertEquals(data, live.getAddressBook());
        assertFalse(live.hasUnsavedChanges());
    }
}
