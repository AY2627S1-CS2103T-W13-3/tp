package seedu.address.logic;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.AddressBook;
import seedu.address.model.ModelManager;
import seedu.address.model.UserPrefs;
import seedu.address.model.person.Person;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;

/** Checks that rejected query arguments cannot publish changes to the dashboard or saved clients. */
public class UnexpectedArgumentsIntegrationTest {
    @TempDir
    public Path temporaryFolder;

    @Test
    public void execute_unexpectedQueryArguments_preservesDataSelectionAndListMode() throws Exception {
        LocalDate today = LocalDate.of(2026, 10, 29);
        ModelManager model = new ModelManager(new AddressBook(), new UserPrefs(), today, false);
        Path file = temporaryFolder.resolve("clients.json");
        Logic logic = new LogicManager(model, new StorageManager(new JsonAddressBookStorage(file),
                new JsonUserPrefsStorage(temporaryFolder.resolve("prefs.json"))),
                Clock.fixed(Instant.parse("2026-10-29T00:00:00Z"), ZoneOffset.UTC));
        logic.execute("add n/Rachel Lim p/91234567 e/rachel@example.com a/Blk 1");
        logic.execute("add n/Aisyah Rahman p/92345678 e/aisyah@example.com a/Blk 2");
        logic.execute("followup 1 d/2026-10-29 m/Discuss renewal");

        for (String mode : List.of("list", "find Rachel", "followups")) {
            logic.execute(mode);
            logic.execute("view 1");
            AddressBook savedModel = new AddressBook(model.getAddressBook());
            List<Person> displayed = List.copyOf(model.getFilteredPersonList());
            Person selected = model.selectedPersonProperty().get();
            boolean isShowingFollowUps = model.showingFollowUpsProperty().get();
            byte[] savedBytes = Files.readAllBytes(file);
            for (String command : List.of("list words", "list 123", "followups words", "followups n/Rachel")) {
                ParseException error = assertThrows(ParseException.class, () -> logic.execute(command));
                String commandWord = command.substring(0, command.indexOf(' '));
                assertEquals("Invalid command format!\nUsage: " + commandWord + " (no arguments)",
                        error.getMessage());
                assertEquals(savedModel, model.getAddressBook());
                assertEquals(displayed, model.getFilteredPersonList());
                assertSame(selected, model.selectedPersonProperty().get());
                assertEquals(isShowingFollowUps, model.showingFollowUpsProperty().get());
                assertEquals(today, model.getToday());
                assertArrayEquals(savedBytes, Files.readAllBytes(file));
            }
        }
    }
}
