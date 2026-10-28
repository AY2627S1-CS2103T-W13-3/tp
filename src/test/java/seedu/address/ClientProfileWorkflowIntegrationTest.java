package seedu.address;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.testutil.Assert.assertThrows;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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
import seedu.address.testutil.MutableClock;
import seedu.address.testutil.PersonBuilder;

/** Exercises the client profile across real commands, saving, and startup. */
public class ClientProfileWorkflowIntegrationTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 12);

    @TempDir
    public Path testFolder;

    @Test
    public void selectedClient_followsPendingReorderingAndClearsWhenExcluded() throws Exception {
        Person first = person("Rachel Lim", "111", "first@example.com", TODAY.plusDays(2));
        Person second = person("Rachel Lim", "222", "second@example.com", TODAY);
        Model model = model(first, second);
        LogicManager logic = new LogicManager(model, storage(testFolder.resolve("clients.json")), clock());

        assertEquals("Showing client: Rachel Lim", logic.execute("view 02").getFeedbackToUser());
        assertSame(second, model.selectedPersonProperty().get());
        assertFalse(model.hasUnsavedChanges());

        logic.execute("followups");
        assertEquals(List.of(second, first), model.getFilteredPersonList());
        assertSame(second, model.selectedPersonProperty().get());
        logic.execute("followup 1 d/2026-10-15 m/Send policy quotation");
        assertEquals(List.of(first, model.selectedPersonProperty().get()), model.getFilteredPersonList());
        assertSame(model.getFilteredPersonList().get(1), model.selectedPersonProperty().get());
        assertEquals("222", model.selectedPersonProperty().get().getPhone().value);
        assertEquals("Send policy quotation", model.selectedPersonProperty().get().getFollowUp()
                .orElseThrow().getDescription());

        logic.execute("followup 2 clear");
        assertNull(model.selectedPersonProperty().get());
        assertEquals(List.of(first), model.getFilteredPersonList());
        logic.execute("list");
        assertNull(model.selectedPersonProperty().get());

        logic.execute("view 02");
        assertEquals("222", model.selectedPersonProperty().get().getPhone().value);
        logic.execute("find NoMatch");
        assertNull(model.selectedPersonProperty().get());
        logic.execute("list");
        assertNull(model.selectedPersonProperty().get());
    }

    @Test
    public void readOnlyCommands_publishSelectionAndDateWithoutSaving() throws Exception {
        Path file = testFolder.resolve("read-only.json");
        Person first = person("Rachel Lim", "111", "first@example.com", TODAY.plusDays(2));
        Person second = person("Amanda Lee", "222", "second@example.com", TODAY);
        Model model = model(first, second);
        JsonAddressBookStorage noSaveStorage = new JsonAddressBookStorage(file) {
            @Override
            public void saveAddressBook(ReadOnlyAddressBook addressBook) throws IOException {
                throw new AssertionError("Read-only commands must not save client data");
            }
        };
        MutableClock clock = clock();
        LogicManager logic = new LogicManager(model, new StorageManager(noSaveStorage,
                new JsonUserPrefsStorage(testFolder.resolve("prefs.json"))), clock);

        logic.execute("view 2");
        assertSame(second, model.selectedPersonProperty().get());
        clock.setInstant(Instant.parse("2026-10-13T00:00:00Z"));
        logic.execute("followups");
        assertEquals(TODAY.plusDays(1), model.getToday());
        assertSame(second, model.selectedPersonProperty().get());
        logic.execute("list");
        logic.execute("find Amanda");
        assertEquals(List.of(second), model.getFilteredPersonList());
        assertSame(second, model.selectedPersonProperty().get());

        assertThrows(ParseException.class, () -> logic.execute("view 0"));
        assertSame(second, model.selectedPersonProperty().get());
        assertEquals(TODAY.plusDays(1), model.getToday());
        assertFalse(Files.exists(file));
    }

    @Test
    public void clearingFollowUpInFullList_keepsProfileWithoutAction() throws Exception {
        Person client = person("Amanda Lee", "333", "amanda@example.com", TODAY.plusDays(1));
        Model model = model(client);
        LogicManager logic = new LogicManager(model, storage(testFolder.resolve("clear.json")), clock());

        logic.execute("view 1");
        logic.execute("followup 1 clear");

        assertSame(model.getFilteredPersonList().get(0), model.selectedPersonProperty().get());
        assertEquals("Amanda Lee", model.selectedPersonProperty().get().getName().fullName);
        assertTrue(model.selectedPersonProperty().get().getFollowUp().isEmpty());
        assertFalse(model.showingFollowUpsProperty().get());
    }

    @Test
    public void failedSaves_leaveSelectedClientViewDateAndSavedBytesUntouched() throws Exception {
        Path file = testFolder.resolve("failed-save.json");
        Person first = person("Rachel Lim", "111", "first@example.com", TODAY.plusDays(2));
        Person second = person("Rachel Lim", "222", "second@example.com", TODAY);
        Model model = model(first, second);
        new JsonAddressBookStorage(file).saveAddressBook(model.getAddressBook());
        byte[] savedBytes = Files.readAllBytes(file);
        model.showPendingFollowUps();
        model.selectPerson(model.getFilteredPersonList().get(0));
        Person selected = model.selectedPersonProperty().get();
        List<Person> stored = List.copyOf(model.getAddressBook().getPersonList());
        List<Person> displayed = List.copyOf(model.getFilteredPersonList());
        MutableClock clock = clock();
        clock.setInstant(Instant.parse("2026-10-13T00:00:00Z"));
        StorageManager failingStorage = new StorageManager(new JsonAddressBookStorage(file),
                new JsonUserPrefsStorage(testFolder.resolve("prefs.json"))) {
            @Override
            public void saveAddressBook(ReadOnlyAddressBook addressBook) throws IOException {
                throw new IOException("simulated disk failure");
            }
        };
        LogicManager logic = new LogicManager(model, failingStorage, clock);

        for (String command : List.of("add n/Bob Lee p/444 e/bob@example.com a/Street",
                "delete 1", "followup 1 d/2026-10-16 m/Replace action", "followup 1 clear")) {
            assertThrows(CommandException.class, String.format(LogicManager.MESSAGE_SAVE_FAILED, file), () ->
                    logic.execute(command));
            assertEquals(stored, model.getAddressBook().getPersonList());
            assertEquals(displayed, model.getFilteredPersonList());
            assertTrue(model.showingFollowUpsProperty().get());
            assertSame(selected, model.selectedPersonProperty().get());
            assertEquals(TODAY, model.getToday());
            assertArrayEquals(savedBytes, Files.readAllBytes(file));
        }
    }

    @Test
    public void restart_loadsOverdueActionAndResetsSessionSelection() throws Exception {
        Path file = testFolder.resolve("restart.json");
        Person first = person("Rachel Lim", "111", "first@example.com", TODAY.minusDays(1));
        Person second = person("Rachel Lim", "222", "second@example.com", TODAY.plusDays(2));
        Model model = model(first, second);
        StorageManager storage = storage(file);
        new JsonAddressBookStorage(file).saveAddressBook(model.getAddressBook());
        LogicManager logic = new LogicManager(model, storage, clock());
        logic.execute("view 2");
        assertSame(second, model.selectedPersonProperty().get());
        logic.execute("followups");

        Model restarted = new MainApp().initModelManager(storage, new UserPrefs());
        assertFalse(restarted.isDataLoadingBlocked());
        assertNull(restarted.selectedPersonProperty().get());
        assertFalse(restarted.showingFollowUpsProperty().get());
        assertEquals(List.of(first, second), restarted.getFilteredPersonList());
        assertEquals(new FollowUp(TODAY.minusDays(1), "Contact client"),
                restarted.getFilteredPersonList().get(0).getFollowUp().orElseThrow());
        assertEquals("OVERDUE", restarted.getFilteredPersonList().get(0).getFollowUp()
                .orElseThrow().getStatus(TODAY).getDisplayName());
    }

    @Test
    public void manyClients_pendingIndicesAndRejectedCommandsKeepCorrectRecords() throws Exception {
        Path file = testFolder.resolve("many-clients.json");
        Model model = model();
        LogicManager logic = new LogicManager(model, storage(file), clock());

        for (int index = 1; index <= 50; index++) {
            logic.execute("add n/Client " + index + " p/" + (1000 + index)
                    + " e/client" + index + "@example.com a/Test address " + index);
        }
        assertEquals(50, model.getFilteredPersonList().size());
        for (int index = 5; index <= 50; index += 5) {
            int daysUntilDue = 11 - index / 5;
            logic.execute("followup " + index + " d/" + TODAY.plusDays(daysUntilDue)
                    + " m/" + "x".repeat(200));
        }
        logic.execute("followups");
        assertEquals(10, model.getFilteredPersonList().size());
        assertEquals(List.of("Client 50", "Client 45", "Client 40", "Client 35", "Client 30",
                "Client 25", "Client 20", "Client 15", "Client 10", "Client 5"),
                model.getFilteredPersonList().stream().map(person -> person.getName().fullName).toList());
        logic.execute("view 0000000001");
        assertEquals("Client 50", model.selectedPersonProperty().get().getName().fullName);

        List<Person> pendingBeforeFailure = List.copyOf(model.getFilteredPersonList());
        byte[] savedBeforeFailure = Files.readAllBytes(file);
        assertThrows(CommandException.class, FollowUp.MESSAGE_INVALID_DATE, () ->
                logic.execute("followup 1 d/2026-02-30 m/Call"));
        assertThrows(CommandException.class, FollowUp.MESSAGE_INVALID_DESCRIPTION, () ->
                logic.execute("followup 1 d/2026-10-13 m/" + "x".repeat(201)));
        assertThrows(CommandException.class, FollowUp.MESSAGE_INVALID_DESCRIPTION, () ->
                logic.execute("followup 1 d/2026-10-11 m/"));
        assertThrows(CommandException.class, "The client index provided is invalid.", () ->
                logic.execute("followup 11 d/2026-02-30 m/Call"));
        assertEquals(pendingBeforeFailure, model.getFilteredPersonList());
        assertEquals("Client 50", model.selectedPersonProperty().get().getName().fullName);
        assertArrayEquals(savedBeforeFailure, Files.readAllBytes(file));

        logic.execute("delete 1");
        assertEquals(9, model.getFilteredPersonList().size());
        assertNull(model.selectedPersonProperty().get());
        logic.execute("list");
        assertEquals(49, model.getFilteredPersonList().size());
        assertFalse(model.getFilteredPersonList().stream()
                .anyMatch(person -> person.getName().fullName.equals("Client 50")));
    }

    private Model model(Person... people) {
        AddressBook book = new AddressBook();
        for (Person person : people) {
            book.addPerson(person);
        }
        return new ModelManager(book, new UserPrefs(), TODAY, false);
    }

    private Person person(String name, String phone, String email, LocalDate dueDate) {
        return new PersonBuilder().withName(name).withPhone(phone).withEmail(email)
                .withFollowUp(new FollowUp(dueDate, "Contact client")).build();
    }

    private StorageManager storage(Path file) {
        return new StorageManager(new JsonAddressBookStorage(file),
                new JsonUserPrefsStorage(testFolder.resolve("prefs.json")));
    }

    private MutableClock clock() {
        return new MutableClock(Instant.parse("2026-10-12T00:00:00Z"), ZoneOffset.UTC);
    }
}
