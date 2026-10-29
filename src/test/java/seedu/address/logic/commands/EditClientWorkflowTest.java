package seedu.address.logic.commands;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.logic.Logic;
import seedu.address.logic.LogicManager;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.AddressBook;
import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.ReadOnlyAddressBook;
import seedu.address.model.UserPrefs;
import seedu.address.model.person.ClientId;
import seedu.address.model.person.FollowUp;
import seedu.address.model.person.Person;
import seedu.address.model.tag.Tag;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;
import seedu.address.testutil.PersonBuilder;

/** Exercises dispatcher, candidate publication and failed-save boundaries with real aggregates. */
public class EditClientWorkflowTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 7);
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-07T00:00:00Z"), ZoneOffset.UTC);

    @TempDir
    public Path temporaryFolder;

    private final Person first = new PersonBuilder().withName("Alice Tan").withPhone("91111111")
            .withEmail("alice@example.com").build();
    private final FollowUp action = new FollowUp(LocalDate.of(2026, 10, 1), "Send renewal");
    private final Person target = new PersonBuilder().withName("Rachel Lim").withPhone("92222222")
            .withEmail("rachel@example.com").build().withFollowUp(action);
    private final Person last = new PersonBuilder().withName("Last Client").withPhone("93333333")
            .withEmail("last@example.com").build();

    private Model createModel() {
        AddressBook book = new AddressBook();
        book.restorePersons(List.of(first, target, last),
                List.of(new ClientId(2), new ClientId(17), new ClientId(41)), 42);
        return new ModelManager(book, new UserPrefs(), TODAY, false);
    }

    private Logic createLogic(Model model, JsonAddressBookStorage storage) {
        return createLogic(model, storage, CLOCK);
    }

    private Logic createLogic(Model model, JsonAddressBookStorage storage, Clock clock) {
        return new LogicManager(model, new StorageManager(storage,
                new JsonUserPrefsStorage(temporaryFolder.resolve("prefs.json"))), clock);
    }

    @Test
    public void execute_editAllFields_publishesAndPersistsSameIdentity() throws Exception {
        Model model = createModel();
        model.updateFilteredPersonList(person -> person.getName().fullName.contains("Rachel"));
        model.selectPerson(target);
        Path destination = temporaryFolder.resolve("clients.json");
        JsonAddressBookStorage storage = new JsonAddressBookStorage(destination);
        Logic logic = createLogic(model, storage);

        CommandResult result = logic.execute("edit 0017 n/Rachel Tan p/+65 9444-4444 "
                + "e/new@example.com a/Block 12 t/Renewal t/renewal");

        Person edited = model.getAddressBook().getPersonById(new ClientId(17)).orElseThrow();
        assertTrue(result.getFeedbackToUser().startsWith("Client ID 17 updated: Rachel Tan"));
        assertEquals("Rachel Tan", edited.getName().fullName);
        assertEquals("+65 9444-4444", edited.getPhone().value);
        assertEquals("new@example.com", edited.getEmail().value);
        assertEquals("Block 12", edited.getAddress().value);
        assertEquals(java.util.Set.of(new Tag("renewal")), edited.getTags());
        assertEquals(Optional.of(action), edited.getFollowUp());
        assertSame(edited, model.selectedPersonProperty().get());
        assertEquals(List.of(edited), model.getFilteredPersonList());
        assertEquals(42, model.getAddressBook().getNextClientId());
        ReadOnlyAddressBook reloaded = storage.readAddressBook().orElseThrow();
        assertEquals(model.getAddressBook(), reloaded);
        assertEquals(Optional.of(edited), reloaded.getPersonById(new ClientId(17)));
    }

    @Test
    public void execute_editEntersSearch_preservesPredicateAndInsertionOrder() throws Exception {
        Model model = createModel();
        model.updateFilteredPersonList(person -> person.getName().fullName.contains("Alice"));
        createLogic(model, new JsonAddressBookStorage(temporaryFolder.resolve("clients.json")))
                .execute("edit 17 n/Alice Lim");
        Person edited = model.getAddressBook().getPersonById(new ClientId(17)).orElseThrow();
        assertEquals(List.of(first, edited), model.getFilteredPersonList());
    }

    @Test
    public void execute_failedMutations_preserveBytesIdentitiesViewAndSelection() throws Exception {
        for (String command : List.of("edit 17 n/New Name", "delete 17",
                "followup 17 d/2026-10-01 m/New action", "followup 17 d/2026-10-20 m/保单更新",
                "followup 17 clear")) {
            Model model = createModel();
            model.showPendingFollowUps();
            model.selectPerson(target);
            AddressBook original = new AddressBook(model.getAddressBook());
            Path destination = temporaryFolder.resolve("clients.json");
            new JsonAddressBookStorage(destination).saveAddressBook(model.getAddressBook());
            byte[] originalBytes = Files.readAllBytes(destination);
            JsonAddressBookStorage failingStorage = new JsonAddressBookStorage(destination) {
                @Override
                public void saveAddressBook(ReadOnlyAddressBook addressBook) throws IOException {
                    throw new IOException("simulated failure");
                }
            };
            Clock nextDayClock = Clock.fixed(TODAY.plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC),
                    ZoneOffset.UTC);
            Logic logic = createLogic(model, failingStorage, nextDayClock);

            CommandException error = assertThrows(CommandException.class, () -> logic.execute(command));

            assertEquals(String.format(LogicManager.MESSAGE_SAVE_FAILED, destination), error.getMessage());
            assertArrayEquals(originalBytes, Files.readAllBytes(destination));
            assertEquals(original, model.getAddressBook());
            assertEquals(42, model.getAddressBook().getNextClientId());
            assertEquals(Optional.of(new ClientId(17)), model.getAddressBook().getClientId(target));
            assertSame(target, model.selectedPersonProperty().get());
            assertEquals(List.of(target), model.getFilteredPersonList());
            assertTrue(model.showingFollowUpsProperty().get());
            assertEquals(TODAY, model.getToday());
            assertFalse(model.hasUnsavedChanges());
        }
    }
}
