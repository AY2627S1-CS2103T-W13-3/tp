package seedu.address;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static seedu.address.testutil.Assert.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.logic.LogicManager;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.person.FollowUp;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;

/** Regression checks for long internal space runs through real commands and saved data. */
public class LongWhitespaceCommandTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 10, 28);
    private static final Clock CLOCK = Clock.fixed(TODAY.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC);

    @TempDir
    public Path testFolder;

    @Test
    public void add_longInternalSpaces_preservesNameAndSavedRoundTrip() {
        String name = "Alice" + " ".repeat(120_000) + "Tan";
        // A generous budget distinguishes linear handling from repeated suffix-regex scans.
        assertTimeout(Duration.ofSeconds(10), () -> {
            Model model = new ModelManager();
            StorageManager storage = storage();
            LogicManager logic = new LogicManager(model, storage, CLOCK);
            logic.execute("add n/" + name + " p/123 e/alice@example.com a/Street");

            assertEquals(name, logic.getAllPersonList().get(0).getName().fullName);
            assertEquals(model.getAddressBook(), storage.readAddressBook().orElseThrow());
            logic.execute("find Tan");
            assertEquals(1, logic.getFilteredPersonList().size());
        });
    }

    @Test
    public void followup_longInternalSpaces_rejectsPromptlyAndPreservesSavedClient() throws Exception {
        Model model = new ModelManager();
        StorageManager storage = storage();
        LogicManager logic = new LogicManager(model, storage, CLOCK);
        logic.execute("add n/Alice Tan p/123 e/alice@example.com a/Street");
        byte[] saved = Files.readAllBytes(storage.getAddressBookFilePath());
        String description = "Call" + " ".repeat(180_000) + "client";
        String command = "followup 1 d/" + TODAY.plusDays(1) + " m/" + description;

        assertTimeout(Duration.ofSeconds(10), () -> assertThrows(CommandException.class,
                FollowUp.MESSAGE_INVALID_DESCRIPTION, () -> logic.execute(command)));

        assertFalse(logic.getAllPersonList().get(0).getFollowUp().isPresent());
        assertArrayEquals(saved, Files.readAllBytes(storage.getAddressBookFilePath()));
    }

    private StorageManager storage() {
        return new StorageManager(new JsonAddressBookStorage(testFolder.resolve("clients.json")),
                new JsonUserPrefsStorage(testFolder.resolve("preferences.json")));
    }
}
