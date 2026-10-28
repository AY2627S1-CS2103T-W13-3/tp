package seedu.address.logic;

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
import java.time.ZoneId;
import java.util.List;
import java.util.TimeZone;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.AddressBook;
import seedu.address.model.ModelManager;
import seedu.address.model.ReadOnlyAddressBook;
import seedu.address.model.UserPrefs;
import seedu.address.model.person.FollowUp;
import seedu.address.model.person.FollowUpStatus;
import seedu.address.model.person.Person;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;
import seedu.address.testutil.MutableClock;
import seedu.address.testutil.PersonBuilder;

public class FollowUpWorkflowIntegrationTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 4);

    @TempDir
    public Path testFolder;

    @Test
    public void recordReplaceClearAndRestart_preserveInsertionOrderAndView() throws Exception {
        Path file = testFolder.resolve("book.json");
        Person rachel = new PersonBuilder().withName("Rachel Lim").build();
        Person amanda = new PersonBuilder().withName("Amanda Lee").withPhone("91234567").build();
        Person john = new PersonBuilder().withName("John Tan").withPhone("92345678").build();
        AddressBook book = new AddressBook();
        book.addPerson(rachel);
        book.addPerson(amanda);
        book.addPerson(john);
        ModelManager model = new ModelManager(book, new UserPrefs(), TODAY, false);
        LogicManager logic = new LogicManager(model, storage(file), clock());

        logic.execute("followup 1 d/2026-10-06 m/Call Rachel");
        logic.execute("followup 2 d/2026-10-05 m/Call Amanda");
        logic.execute("followups");
        assertEquals(List.of("Amanda Lee", "Rachel Lim"), names(model.getFilteredPersonList()));
        assertEquals(List.of("Rachel Lim", "Amanda Lee", "John Tan"), names(book.getPersonList()));

        logic.execute("followup 1 d/2026-10-07 m/Updated Amanda");
        assertEquals(List.of("Rachel Lim", "Amanda Lee"), names(model.getFilteredPersonList()));
        assertEquals("Updated Amanda", model.getFilteredPersonList().get(1).getFollowUp()
                .orElseThrow().getDescription());
        logic.execute("followup 1 clear");
        assertEquals(List.of("Amanda Lee"), names(model.getFilteredPersonList()));

        AddressBook loaded = new AddressBook(new JsonAddressBookStorage(file).readAddressBook().orElseThrow());
        assertEquals(List.of("Rachel Lim", "Amanda Lee", "John Tan"), names(loaded.getPersonList()));
        assertTrue(loaded.getPersonList().get(0).getFollowUp().isEmpty());
        assertEquals(new FollowUp(LocalDate.of(2026, 10, 7), "Updated Amanda"),
                loaded.getPersonList().get(1).getFollowUp().orElseThrow());
        ModelManager restarted = new ModelManager(loaded, new UserPrefs(), TODAY, false);
        assertFalse(restarted.showingFollowUpsProperty().get());
        assertEquals(3, restarted.getFilteredPersonList().size());
    }

    @Test
    public void overdueFollowUps_recordAndReplace_preserveSelectionOrderAndRestartData() throws Exception {
        Path file = testFolder.resolve("overdue.json");
        Person rachel = new PersonBuilder().withName("Rachel Lim").withTags("health", "active").build();
        Person amanda = new PersonBuilder().withName("Amanda Lee").withPhone("91234567")
                .withFollowUp(new FollowUp(TODAY, "Call Amanda")).build();
        Person john = new PersonBuilder().withName("John Tan").withPhone("92345678")
                .withFollowUp(new FollowUp(TODAY.plusDays(1), "Call John")).build();
        AddressBook book = new AddressBook();
        book.setPersons(List.of(rachel, amanda, john));
        ModelManager model = new ModelManager(book, new UserPrefs(), TODAY, false);
        LogicManager logic = new LogicManager(model, storage(file), clock());

        logic.execute("find Rachel");
        logic.execute("view 1");
        assertEquals("Follow-up for Rachel Lim recorded for 2026-10-03.",
                logic.execute("followup 1 d/2026-10-03 m/Carry-over call").getFeedbackToUser());
        Person overdueRachel = rachel.withFollowUp(new FollowUp(TODAY.minusDays(1), "Carry-over call"));
        assertEquals(List.of(overdueRachel), model.getFilteredPersonList());
        assertFalse(model.showingFollowUpsProperty().get());
        assertSame(model.getFilteredPersonList().get(0), model.selectedPersonProperty().get());
        assertEquals(List.of(overdueRachel, amanda, john), model.getAddressBook().getPersonList());

        logic.execute("followups");
        assertEquals(List.of(overdueRachel, amanda, john), model.getFilteredPersonList());
        assertEquals(List.of(FollowUpStatus.OVERDUE, FollowUpStatus.DUE_TODAY, FollowUpStatus.UPCOMING),
                model.getFilteredPersonList().stream()
                        .map(person -> person.getFollowUp().orElseThrow().getStatus(model.getToday())).toList());
        logic.execute("view 2");
        assertEquals("Follow-up for Amanda Lee updated to 2026-10-03.",
                logic.execute("followup 2 d/2026-10-03 m/Overdue renewal").getFeedbackToUser());
        Person overdueAmanda = amanda.withFollowUp(new FollowUp(TODAY.minusDays(1), "Overdue renewal"));
        // Equal dates retain full-list insertion order, even when the viewed action is replaced.
        assertEquals(List.of(overdueRachel, overdueAmanda, john), model.getFilteredPersonList());
        assertSame(model.getFilteredPersonList().get(1), model.selectedPersonProperty().get());

        logic.execute("followup 2 d/2026-10-02 m/Earlier carry-over task");
        Person earlierAmanda = amanda.withFollowUp(new FollowUp(TODAY.minusDays(2), "Earlier carry-over task"));
        assertEquals(List.of(earlierAmanda, overdueRachel, john), model.getFilteredPersonList());
        assertSame(model.getFilteredPersonList().get(0), model.selectedPersonProperty().get());
        assertTrue(model.showingFollowUpsProperty().get());
        List<Person> expectedFullList = List.of(overdueRachel, earlierAmanda, john);
        assertEquals(expectedFullList, model.getAddressBook().getPersonList());
        assertFalse(model.hasUnsavedChanges());

        AddressBook loaded = new AddressBook(new JsonAddressBookStorage(file).readAddressBook().orElseThrow());
        assertEquals(expectedFullList, loaded.getPersonList());
        ModelManager restarted = new ModelManager(loaded, new UserPrefs(), TODAY, false);
        assertNull(restarted.selectedPersonProperty().get());
        LogicManager restartedLogic = new LogicManager(restarted, storage(file), clock());
        restartedLogic.execute("followups");
        assertEquals(List.of(earlierAmanda, overdueRachel, john), restarted.getFilteredPersonList());
        assertEquals(FollowUpStatus.OVERDUE,
                restarted.getFilteredPersonList().get(0).getFollowUp().orElseThrow().getStatus(restarted.getToday()));
        assertEquals(FollowUpStatus.OVERDUE,
                restarted.getFilteredPersonList().get(1).getFollowUp().orElseThrow().getStatus(restarted.getToday()));
    }

    @Test
    public void earliestFollowUpDate_recordAndRestart_preservesDateAndOverdueStatus() throws Exception {
        Path file = testFolder.resolve("earliest-date.json");
        Person rachel = new PersonBuilder().withName("Rachel Lim").build();
        AddressBook book = new AddressBook();
        book.addPerson(rachel);
        ModelManager model = new ModelManager(book, new UserPrefs(), TODAY, false);
        LogicManager logic = new LogicManager(model, storage(file), clock());

        assertEquals("Follow-up for Rachel Lim recorded for 0001-01-01.",
                logic.execute("followup 1 d/0001-01-01 m/Carry-over call").getFeedbackToUser());
        Person expected = rachel.withFollowUp(new FollowUp(LocalDate.of(1, 1, 1), "Carry-over call"));
        assertEquals(List.of(expected), model.getAddressBook().getPersonList());
        assertTrue(Files.readString(file).contains("\"0001-01-01\""));

        AddressBook loaded = new AddressBook(new JsonAddressBookStorage(file).readAddressBook().orElseThrow());
        ModelManager restarted = new ModelManager(loaded, new UserPrefs(), TODAY, false);
        LogicManager restartedLogic = new LogicManager(restarted, storage(file), clock());
        restartedLogic.execute("followups");

        assertEquals(List.of(expected), restarted.getFilteredPersonList());
        FollowUp restored = restarted.getFilteredPersonList().get(0).getFollowUp().orElseThrow();
        assertEquals(LocalDate.of(1, 1, 1), restored.getDueDate());
        assertEquals(FollowUpStatus.OVERDUE, restored.getStatus(restarted.getToday()));
    }

    @Test
    public void overdueFollowUp_saveCrossesMidnight_publishesOneSampledDate() throws Exception {
        Path file = testFolder.resolve("midnight.json");
        Person rachel = new PersonBuilder().withName("Rachel Lim").build();
        AddressBook book = new AddressBook();
        book.addPerson(rachel);
        ModelManager model = new ModelManager(book, new UserPrefs(), TODAY.minusDays(1), false);
        int[] samples = {0};
        MutableClock clock = new MutableClock(Instant.parse("2026-10-04T23:59:59Z"), ZoneId.of("UTC")) {
            @Override
            public Instant instant() {
                samples[0]++;
                return super.instant();
            }
        };
        StorageManager storage = new StorageManager(new JsonAddressBookStorage(file),
                new JsonUserPrefsStorage(testFolder.resolve("prefs.json"))) {
            @Override
            public void saveAddressBook(ReadOnlyAddressBook addressBook) throws IOException {
                clock.setInstant(Instant.parse("2026-10-05T00:00:00Z"));
                super.saveAddressBook(addressBook);
            }
        };
        LogicManager logic = new LogicManager(model, storage, clock);

        logic.execute("followup 1 d/2026-10-03 m/Carry-over call");

        assertEquals(1, samples[0]);
        assertEquals(TODAY, model.getToday());
        assertEquals(FollowUpStatus.OVERDUE,
                model.getFilteredPersonList().get(0).getFollowUp().orElseThrow().getStatus(model.getToday()));
        assertEquals(model.getAddressBook(), new JsonAddressBookStorage(file).readAddressBook().orElseThrow());
    }

    @Test
    public void failedSavesAndUnsupportedCommands_preserveLiveDataViewDateAndFile() throws Exception {
        Path file = testFolder.resolve("book.json");
        Person rachel = new PersonBuilder().withName("Rachel Lim")
                .withFollowUp(new FollowUp(TODAY.plusDays(1), "Call")).build();
        AddressBook book = new AddressBook();
        book.addPerson(rachel);
        new JsonAddressBookStorage(file).saveAddressBook(book);
        byte[] original = Files.readAllBytes(file);
        ModelManager model = new ModelManager(book, new UserPrefs(), TODAY, false);
        model.showPendingFollowUps();
        model.selectPerson(rachel);
        MutableClock clock = clock();
        clock.setInstant(Instant.parse("2026-10-05T00:00:00Z"));
        StorageManager failingStorage = new StorageManager(new JsonAddressBookStorage(file),
                new JsonUserPrefsStorage(testFolder.resolve("prefs.json"))) {
            @Override
            public void saveAddressBook(ReadOnlyAddressBook addressBook) throws IOException {
                throw new IOException("simulated disk failure");
            }
        };
        LogicManager logic = new LogicManager(model, failingStorage, clock);
        List<String> unsupportedCommands = List.of("edit 1 n/Updated Rachel", "clear", "help", "exit");
        for (String command : List.of("add n/Bob p/92345678 e/bob@example.com a/Street",
                "delete 1", "edit 1 n/Updated Rachel", "followup 1 d/2026-10-06 m/Replace",
                "followup 1 d/2026-10-03 m/Overdue replacement", "followup 1 clear", "clear", "help", "exit")) {
            if (unsupportedCommands.contains(command)) {
                assertThrows(ParseException.class, "Unknown command.", () -> logic.execute(command));
            } else {
                assertThrows(CommandException.class, String.format(LogicManager.MESSAGE_SAVE_FAILED, file), () ->
                    logic.execute(command));
            }
            assertEquals(TODAY, model.getToday());
            assertTrue(model.showingFollowUpsProperty().get());
            assertSame(rachel, model.selectedPersonProperty().get());
            assertEquals(List.of(rachel), model.getAddressBook().getPersonList());
            assertFalse(model.hasUnsavedChanges());
            assertEquals(List.of("Rachel Lim"), names(model.getFilteredPersonList()));
            assertEquals(new FollowUp(TODAY.plusDays(1), "Call"), model.getFilteredPersonList().get(0)
                    .getFollowUp().orElseThrow());
            assertArrayEquals(original, Files.readAllBytes(file));
        }
        assertThrows(CommandException.class, FollowUp.MESSAGE_INVALID_DATE, () ->
                logic.execute("followup 1 d/2026-02-29 m/Invalid calendar date"));
        assertThrows(CommandException.class, FollowUp.MESSAGE_INVALID_DESCRIPTION, () ->
                logic.execute("followup 1 d/2026-10-03 m/"));
        assertEquals(TODAY, model.getToday());
        assertTrue(model.showingFollowUpsProperty().get());
        assertSame(rachel, model.selectedPersonProperty().get());
        assertEquals(List.of(rachel), model.getAddressBook().getPersonList());
        assertEquals(List.of(rachel), model.getFilteredPersonList());
        assertFalse(model.hasUnsavedChanges());
        assertArrayEquals(original, Files.readAllBytes(file));
    }

    @Test
    public void readOnlyAndFailedCommands_sampleDateWithoutSavingOnlyOnSuccess() throws Exception {
        Path file = testFolder.resolve("book.json");
        ModelManager model = new ModelManager(new AddressBook(), new UserPrefs(), TODAY, false);
        MutableClock clock = clock();
        int[] saveCalls = {0};
        StorageManager storage = new StorageManager(new JsonAddressBookStorage(file),
                new JsonUserPrefsStorage(testFolder.resolve("prefs.json"))) {
            @Override
            public void saveAddressBook(ReadOnlyAddressBook addressBook) throws IOException {
                saveCalls[0]++;
                super.saveAddressBook(addressBook);
            }
        };
        LogicManager logic = new LogicManager(model, storage, clock);
        clock.setInstant(Instant.parse("2026-10-05T00:00:00Z"));
        assertThrows(ParseException.class, () -> logic.execute("unknown"));
        assertThrows(CommandException.class, () -> logic.execute("delete 1"));
        assertEquals(TODAY, model.getToday());
        logic.execute("list");
        logic.execute("followups");
        assertEquals(TODAY.plusDays(1), model.getToday());
        assertEquals(0, saveCalls[0]);
        assertFalse(Files.exists(file));
    }

    @Test
    public void defaultClock_resolvesChangedSystemZoneForNextCommand() throws Exception {
        TimeZone previous = TimeZone.getDefault();
        try {
            ModelManager model = new ModelManager(new AddressBook(), new UserPrefs(), TODAY, false);
            LogicManager logic = new LogicManager(model, storage(testFolder.resolve("zone.json")));
            TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Kiritimati"));
            logic.execute("list");
            assertEquals(LocalDate.now(ZoneId.of("Pacific/Kiritimati")), model.getToday());
            TimeZone.setDefault(TimeZone.getTimeZone("Pacific/Honolulu"));
            logic.execute("list");
            assertEquals(LocalDate.now(ZoneId.of("Pacific/Honolulu")), model.getToday());
        } finally {
            TimeZone.setDefault(previous);
        }
    }

    private StorageManager storage(Path file) {
        return new StorageManager(new JsonAddressBookStorage(file),
                new JsonUserPrefsStorage(testFolder.resolve("prefs.json")));
    }

    private MutableClock clock() {
        return new MutableClock(Instant.parse("2026-10-04T00:00:00Z"), ZoneId.of("UTC"));
    }

    private List<String> names(List<Person> people) {
        return people.stream().map(person -> person.getName().fullName).toList();
    }
}
