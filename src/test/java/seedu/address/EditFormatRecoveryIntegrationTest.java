package seedu.address;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javafx.collections.ListChangeListener;
import seedu.address.logic.LogicManager;
import seedu.address.logic.Messages;
import seedu.address.logic.commands.EditCommand;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.ModelManager;
import seedu.address.model.person.Person;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.Storage;
import seedu.address.storage.StorageManager;

/** Checks that edit-format feedback preserves committed state and supports correction. */
public class EditFormatRecoveryIntegrationTest {
    @TempDir
    private Path directory;

    @Test
    public void execute_invalidEditInFilteredView_preservesStateAndAllowsCorrection() throws Exception {
        assertFormatRecovery(false);
    }

    @Test
    public void execute_invalidEditInPendingView_preservesStateAndAllowsCorrection() throws Exception {
        assertFormatRecovery(true);
    }

    private void assertFormatRecovery(boolean isPendingView) throws Exception {
        Path dataFile = directory.resolve("addressbook.json");
        Storage storage = new StorageManager(new JsonAddressBookStorage(dataFile),
                new JsonUserPrefsStorage(directory.resolve("preferences.json")));
        ModelManager model = new ModelManager();
        LogicManager logic = new LogicManager(model, storage,
                Clock.fixed(Instant.parse("2026-11-01T12:00:00Z"), ZoneOffset.UTC));
        logic.execute("add n/Rachel Lim p/91234567 e/rachel@example.com a/Blk 123 t/health");
        logic.execute("add n/Alice Tan p/81234567 e/alice@example.com a/Blk 456");
        logic.execute("followup 1 d/2026-11-02 m/Review hospital rider");
        logic.execute(isPendingView ? "followups" : "find n/Rachel");
        logic.execute("view 1");
        byte[] savedBytes = Files.readAllBytes(dataFile);
        List<Person> fullFeed = List.copyOf(logic.getAllPersonList());
        List<Person> displayed = List.copyOf(logic.getFilteredPersonList());
        Person selected = logic.selectedPersonProperty().get();
        long nextId = model.getAddressBook().getNextClientId();
        AtomicInteger notifications = new AtomicInteger();
        logic.getAllPersonList().addListener((ListChangeListener<Person>) change -> notifications.incrementAndGet());
        logic.getFilteredPersonList().addListener(
                (ListChangeListener<Person>) change -> notifications.incrementAndGet());
        logic.selectedPersonProperty().addListener(observable -> notifications.incrementAndGet());

        for (String command : List.of("edit 1 pp/92345678", "edit 1 phone/92345678", "edit 1 extra p/92345678")) {
            ParseException error = assertThrows(ParseException.class, () -> logic.execute(command));
            assertEquals(String.format(Messages.MESSAGE_INVALID_COMMAND_FORMAT, EditCommand.MESSAGE_USAGE),
                    error.getMessage());
            assertArrayEquals(savedBytes, Files.readAllBytes(dataFile));
            assertEquals(fullFeed, logic.getAllPersonList());
            assertEquals(displayed, logic.getFilteredPersonList());
            assertSame(selected, logic.selectedPersonProperty().get());
            assertEquals(isPendingView, logic.showingFollowUpsProperty().get());
            assertEquals(nextId, model.getAddressBook().getNextClientId());
            assertFalse(model.hasUnsavedChanges());
            assertEquals(0, notifications.get());
        }

        logic.execute("edit 001 p/92345678 a/Home pp/unit");
        Person corrected = logic.selectedPersonProperty().get();
        assertEquals("92345678", corrected.getPhone().value);
        assertEquals("Home pp/unit", corrected.getAddress().value);
        assertEquals(selected.getFollowUp(), corrected.getFollowUp());
        assertEquals(1, logic.getClientId(corrected).orElseThrow().getValue());
        assertEquals(isPendingView, logic.showingFollowUpsProperty().get());
        assertEquals(nextId, model.getAddressBook().getNextClientId());
        assertEquals(List.of(corrected), logic.getFilteredPersonList());
        assertEquals(model.getAddressBook(), storage.readAddressBook().orElseThrow());
        assertFalse(model.hasUnsavedChanges());
    }
}
