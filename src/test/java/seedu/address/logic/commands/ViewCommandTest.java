package seedu.address.logic.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.Assert.assertThrows;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.commons.core.index.Index;
import seedu.address.logic.LogicManager;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.AddressBook;
import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.ReadOnlyAddressBook;
import seedu.address.model.UserPrefs;
import seedu.address.model.person.FollowUp;
import seedu.address.model.person.Person;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;
import seedu.address.testutil.PersonBuilder;

/** Tests view selection against the authoritative list and the real command publication path. */
public class ViewCommandTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 12);
    private static final Clock CLOCK = Clock.fixed(TODAY.atStartOfDay().toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    @TempDir
    public Path temporaryFolder;

    private final Person first = new PersonBuilder().withName("Rachel Lim").withPhone("123")
            .withEmail("first@example.com").withFollowUp(new FollowUp(TODAY.plusDays(2), "Call Rachel")).build();
    private final Person second = new PersonBuilder().withName("Rachel Lim").withPhone("456")
            .withEmail("second@example.com").withFollowUp(new FollowUp(TODAY, "Send quotation")).build();

    @Test
    public void execute_fullSearchAndPendingLists_selectsExactDisplayedRecord() throws Exception {
        Model model = createModel(false);
        assertView(model, 2, second);
        model.updateFilteredPersonList(person -> person == second);
        assertView(model, 1, second);
        model.showPendingFollowUps();
        assertEquals(List.of(second, first), model.getFilteredPersonList());
        assertView(model, 1, second);
        assertView(model, 2, first);
    }

    @Test
    public void execute_invalidDisplayedIndex_preservesSelectionAndState() throws Exception {
        Model model = createModel(false);
        model.updateFilteredPersonList(person -> person == second);
        assertView(model, 1, second);
        Model before = model.forkForCommand(TODAY);
        assertThrows(CommandException.class, "The client index provided is invalid.", () ->
                new ViewCommand(Index.fromOneBased(2)).execute(model));
        assertEquals(before, model);
        assertSame(second, model.selectedPersonProperty().get());
    }

    @Test
    public void execute_emptyBlockedSession_reportsRangeError() {
        Model model = createModel(true);
        assertThrows(CommandException.class, "The client index provided is invalid.", () ->
                new ViewCommand(Index.fromOneBased(1)).execute(model));
        assertNull(model.selectedPersonProperty().get());
        assertFalse(model.hasUnsavedChanges());
    }

    @Test
    public void execute_throughLogic_publishesSelectionAndDateWithoutSaving() throws Exception {
        Model model = createModel(false);
        model.updateToday(TODAY.minusDays(1));
        model.showPendingFollowUps();
        LogicManager logic = createLogic(model);
        assertEquals("Showing client: Rachel Lim", logic.execute("view 02").getFeedbackToUser());
        assertSame(first, logic.selectedPersonProperty().get());
        assertEquals(TODAY, model.getToday());
        assertEquals(List.of(second, first), model.getFilteredPersonList());
        assertTrue(model.showingFollowUpsProperty().get());
        assertFalse(model.hasUnsavedChanges());
    }

    @Test
    public void execute_throughLogic_failuresPreservePreviousSelectionDateAndList() throws Exception {
        Model model = createModel(false);
        model.showPendingFollowUps();
        model.selectPerson(second);
        model.updateToday(TODAY.minusDays(1));
        Model before = model.forkForCommand(model.getToday());
        LogicManager logic = createLogic(model);
        for (String input : List.of("view", "view 0 extra")) {
            assertThrows(ParseException.class, "Invalid command format!\nUsage: view INDEX", () ->
                    logic.execute(input));
            assertEquals(before, model);
        }
        assertThrows(ParseException.class, "Index must be a positive integer from 1 to 2147483647.", () ->
                logic.execute("view 0"));
        assertThrows(CommandException.class, "The client index provided is invalid.", () -> logic.execute("view 3"));
        assertEquals(before, model);
        assertSame(second, model.selectedPersonProperty().get());
    }

    @Test
    public void equalsAndToString() {
        ViewCommand command = new ViewCommand(Index.fromOneBased(1));
        assertEquals(command, command);
        assertEquals(command, new ViewCommand(Index.fromOneBased(1)));
        assertFalse(command.equals(new ViewCommand(Index.fromOneBased(2))));
        assertFalse(command.equals(null));
        assertFalse(command.equals(new ListCommand()));
        assertEquals(ViewCommand.class.getCanonicalName() + "{targetIndex=" + Index.fromOneBased(1) + "}",
                command.toString());
    }

    private Model createModel(boolean blocked) {
        AddressBook addressBook = new AddressBook();
        if (!blocked) {
            addressBook.addPerson(first);
            addressBook.addPerson(second);
        }
        return new ModelManager(addressBook, new UserPrefs(), TODAY, blocked);
    }

    private void assertView(Model model, int index, Person expected) throws Exception {
        List<Person> displayed = List.copyOf(model.getFilteredPersonList());
        List<Person> stored = List.copyOf(model.getAddressBook().getPersonList());
        boolean pending = model.showingFollowUpsProperty().get();
        assertEquals("Showing client: Rachel Lim",
                new ViewCommand(Index.fromOneBased(index)).execute(model).getFeedbackToUser());
        assertSame(expected, model.selectedPersonProperty().get());
        assertEquals(displayed, model.getFilteredPersonList());
        assertEquals(stored, model.getAddressBook().getPersonList());
        assertEquals(pending, model.showingFollowUpsProperty().get());
        assertEquals(TODAY, model.getToday());
        assertFalse(model.hasUnsavedChanges());
    }

    private LogicManager createLogic(Model model) {
        JsonAddressBookStorage storage = new JsonAddressBookStorage(temporaryFolder.resolve("clients.json")) {
            @Override
            public void saveAddressBook(ReadOnlyAddressBook data) throws IOException {
                throw new AssertionError("View must not save client data");
            }
        };
        return new LogicManager(model, new StorageManager(storage,
                new JsonUserPrefsStorage(temporaryFolder.resolve("prefs.json"))), CLOCK);
    }
}
