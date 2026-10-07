package seedu.address.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    public void failedSaves_preserveLiveDataViewDateAndFile() throws Exception {
        Path file = testFolder.resolve("book.json");
        Person rachel = new PersonBuilder().withName("Rachel Lim")
                .withFollowUp(new FollowUp(TODAY.plusDays(1), "Call")).build();
        AddressBook book = new AddressBook();
        book.addPerson(rachel);
        new JsonAddressBookStorage(file).saveAddressBook(book);
        byte[] original = Files.readAllBytes(file);
        ModelManager model = new ModelManager(book, new UserPrefs(), TODAY, false);
        model.showPendingFollowUps();
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
        for (String command : List.of("add n/Bob p/92345678 e/bob@example.com a/Street",
                "delete 1", "edit 1 n/Updated Rachel", "followup 1 d/2026-10-06 m/Replace",
                "followup 1 clear", "clear")) {
            assertThrows(CommandException.class, LogicManager.MESSAGE_SAVE_FAILED, () -> logic.execute(command));
            assertEquals(TODAY, model.getToday());
            assertTrue(model.showingFollowUpsProperty().get());
            assertEquals(List.of("Rachel Lim"), names(model.getFilteredPersonList()));
            assertEquals(new FollowUp(TODAY.plusDays(1), "Call"), model.getFilteredPersonList().get(0)
                    .getFollowUp().orElseThrow());
            assertTrue(java.util.Arrays.equals(original, Files.readAllBytes(file)));
        }
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
