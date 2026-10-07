package seedu.address.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static seedu.address.logic.Messages.MESSAGE_CHANGES_BLOCKED;
import static seedu.address.logic.Messages.MESSAGE_INVALID_PERSON_DISPLAYED_INDEX;
import static seedu.address.logic.Messages.MESSAGE_UNKNOWN_COMMAND;
import static seedu.address.logic.commands.CommandTestUtil.ADDRESS_DESC_AMY;
import static seedu.address.logic.commands.CommandTestUtil.EMAIL_DESC_AMY;
import static seedu.address.logic.commands.CommandTestUtil.NAME_DESC_AMY;
import static seedu.address.logic.commands.CommandTestUtil.PHONE_DESC_AMY;
import static seedu.address.testutil.Assert.assertThrows;

import java.io.IOException;
import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import seedu.address.logic.commands.AddCommand;
import seedu.address.logic.commands.CommandResult;
import seedu.address.logic.commands.ListCommand;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.AddressBook;
import seedu.address.model.Model;
import seedu.address.model.ModelManager;
import seedu.address.model.ReadOnlyAddressBook;
import seedu.address.model.UserPrefs;
import seedu.address.model.person.FollowUp;
import seedu.address.model.person.FollowUpStatus;
import seedu.address.model.person.Person;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.Storage;
import seedu.address.storage.StorageManager;
import seedu.address.testutil.MutableClock;
import seedu.address.testutil.PersonBuilder;

public class LogicManagerTest {
    private static final IOException DUMMY_IO_EXCEPTION = new IOException("dummy IO exception");
    private static final IOException DUMMY_AD_EXCEPTION = new AccessDeniedException("dummy access denied exception");

    @TempDir
    public Path temporaryFolder;

    private Model model = new ModelManager();
    private Logic logic;
    private Storage storage;

    @BeforeEach
    public void setUp() {
        JsonAddressBookStorage addressBookStorage =
                new JsonAddressBookStorage(temporaryFolder.resolve("addressBook.json"));
        JsonUserPrefsStorage userPrefsStorage = new JsonUserPrefsStorage(temporaryFolder.resolve("userPrefs.json"));
        storage = new StorageManager(addressBookStorage, userPrefsStorage);
        logic = new LogicManager(model, storage);
    }

    @Test
    public void execute_invalidCommandFormat_throwsParseException() {
        String invalidCommand = "uicfhmowqewca";
        assertParseException(invalidCommand, MESSAGE_UNKNOWN_COMMAND);
    }

    @Test
    public void execute_commandExecutionError_throwsCommandException() {
        String deleteCommand = "delete 9";
        assertCommandException(deleteCommand, MESSAGE_INVALID_PERSON_DISPLAYED_INDEX);
    }

    @Test
    public void execute_validCommand_success() throws Exception {
        String listCommand = ListCommand.COMMAND_WORD;
        assertCommandSuccess(listCommand, ListCommand.MESSAGE_SUCCESS, model);
    }

    @Test
    public void execute_storageThrowsIoException_throwsCommandException() {
        assertCommandFailureForExceptionFromStorage(DUMMY_IO_EXCEPTION, LogicManager.MESSAGE_SAVE_FAILED);
    }

    @Test
    public void execute_storageThrowsAdException_throwsCommandException() {
        assertCommandFailureForExceptionFromStorage(DUMMY_AD_EXCEPTION, LogicManager.MESSAGE_SAVE_FAILED);
    }

    @Test
    public void getFilteredPersonList_modifyList_throwsUnsupportedOperationException() {
        assertThrows(UnsupportedOperationException.class, () -> logic.getFilteredPersonList().remove(0));
    }

    @Test
    public void followUpStateProperties_reflectLiveModel() {
        LocalDate today = LocalDate.of(2026, 10, 7);
        ModelManager liveModel = new ModelManager(new AddressBook(), new UserPrefs(), today, true);
        Clock fixedClock = Clock.fixed(Instant.parse("2026-10-07T00:00:00Z"), ZoneOffset.UTC);
        Logic bridgedLogic = new LogicManager(liveModel, storage, fixedClock);

        assertSame(liveModel.todayProperty(), bridgedLogic.todayProperty());
        assertEquals(today, bridgedLogic.todayProperty().get());
        assertSame(liveModel.showingFollowUpsProperty(), bridgedLogic.showingFollowUpsProperty());
        assertFalse(bridgedLogic.showingFollowUpsProperty().get());
        assertTrue(bridgedLogic.isDataLoadingBlocked());

        liveModel.showPendingFollowUps();
        assertTrue(bridgedLogic.showingFollowUpsProperty().get());

        liveModel.commitFrom(liveModel.forkForCommand(today.plusDays(1)));
        assertEquals(today.plusDays(1), bridgedLogic.todayProperty().get());
    }

    @Test
    public void execute_dataLoadingBlocked_mutationCommandsBlocked() {
        Model blockedModel = new ModelManager(new AddressBook(), new UserPrefs(),
                LocalDate.of(2026, 10, 7), true);
        Logic blockedLogic = new LogicManager(blockedModel, storage);
        String addCommand = AddCommand.COMMAND_WORD + NAME_DESC_AMY + PHONE_DESC_AMY
                + EMAIL_DESC_AMY + ADDRESS_DESC_AMY;
        assertThrows(ParseException.class, MESSAGE_CHANGES_BLOCKED, () -> blockedLogic.execute(addCommand));
        assertThrows(ParseException.class, MESSAGE_CHANGES_BLOCKED, () -> blockedLogic.execute("clear"));
        assertThrows(ParseException.class, MESSAGE_CHANGES_BLOCKED, () -> blockedLogic.execute("delete 1"));
        assertThrows(ParseException.class, MESSAGE_CHANGES_BLOCKED, () ->
                blockedLogic.execute("edit 1 " + NAME_DESC_AMY));
        assertThrows(ParseException.class, MESSAGE_CHANGES_BLOCKED, () -> blockedLogic.execute("followup 1 clear"));
    }

    @Test
    public void execute_dataLoadingBlocked_readOnlyCommandSucceeds() throws Exception {
        Model blockedModel = new ModelManager(new AddressBook(), new UserPrefs(),
                LocalDate.of(2026, 10, 7), true);
        Logic blockedLogic = new LogicManager(blockedModel, storage);
        CommandResult result = blockedLogic.execute(ListCommand.COMMAND_WORD);
        assertEquals(ListCommand.MESSAGE_SUCCESS, result.getFeedbackToUser());
    }

    @Test
    public void execute_followUpIndexOutOfRange_precedesInvalidDate() {
        assertCommandException("followup 999 d/bad m/", MESSAGE_INVALID_PERSON_DISPLAYED_INDEX);
    }

    @Test
    public void execute_afterMidnightInvalidCommand_keepsDate() {
        LocalDate startupDate = LocalDate.of(2026, 10, 7);
        Model liveModel = new ModelManager(new AddressBook(), new UserPrefs(), startupDate, false);
        Clock nextDayClock = Clock.fixed(Instant.parse("2026-10-08T00:00:00Z"), ZoneOffset.UTC);
        Logic nextDayLogic = new LogicManager(liveModel, storage, nextDayClock);
        assertThrows(ParseException.class, () -> nextDayLogic.execute("invalid"));
        assertEquals(startupDate, liveModel.getToday());
    }

    @Test
    public void refreshToday_idleAcrossLocalMidnight_updatesStatusesWithoutSaving() {
        LocalDate startupDate = LocalDate.of(2026, 10, 7);
        Person dueToday = new PersonBuilder().withFollowUp(new FollowUp(startupDate, "Call client")).build();
        Person dueTomorrow = new PersonBuilder().withName("Tomorrow")
                .withFollowUp(new FollowUp(startupDate.plusDays(1), "Send quotation")).build();
        AddressBook addressBook = new AddressBook();
        addressBook.addPerson(dueToday);
        addressBook.addPerson(dueTomorrow);
        Model liveModel = new ModelManager(addressBook, new UserPrefs(), startupDate, false);
        MutableClock clock = new MutableClock(Instant.parse("2026-10-07T15:59:59Z"),
                ZoneId.of("Asia/Singapore"));
        Logic idleLogic = new LogicManager(liveModel, storage, clock);
        List<LocalDate> dateChanges = new ArrayList<>();
        idleLogic.todayProperty().addListener((observable, oldDate, newDate) -> dateChanges.add(newDate));
        idleLogic.refreshToday();
        assertTrue(dateChanges.isEmpty());
        assertEquals(FollowUpStatus.DUE_TODAY, dueToday.getFollowUp().orElseThrow().getStatus(liveModel.getToday()));
        assertEquals(FollowUpStatus.UPCOMING, dueTomorrow.getFollowUp().orElseThrow().getStatus(liveModel.getToday()));
        clock.setInstant(Instant.parse("2026-10-07T16:00:00Z"));
        idleLogic.refreshToday();
        assertEquals(LocalDate.of(2026, 10, 8), idleLogic.todayProperty().get());
        assertEquals(List.of(LocalDate.of(2026, 10, 8)), dateChanges);
        assertEquals(FollowUpStatus.OVERDUE, dueToday.getFollowUp().orElseThrow().getStatus(liveModel.getToday()));
        assertEquals(FollowUpStatus.DUE_TODAY, dueTomorrow.getFollowUp().orElseThrow().getStatus(liveModel.getToday()));
        idleLogic.refreshToday();
        assertEquals(1, dateChanges.size());
        assertFalse(liveModel.hasUnsavedChanges());
        assertEquals(addressBook, liveModel.getAddressBook());
        assertFalse(Files.exists(storage.getAddressBookFilePath()));
    }

    @Test
    public void refreshToday_clockJumps_usesCurrentDate() {
        LocalDate startupDate = LocalDate.of(2026, 10, 7);
        Model liveModel = new ModelManager(new AddressBook(), new UserPrefs(), startupDate, false);
        MutableClock clock = new MutableClock(Instant.parse("2026-10-07T00:00:00Z"), ZoneOffset.UTC);
        Logic timedLogic = new LogicManager(liveModel, storage, clock);
        clock.setInstant(Instant.parse("2026-10-12T00:00:00Z"));
        timedLogic.refreshToday();
        assertEquals(LocalDate.of(2026, 10, 12), liveModel.getToday());
        clock.setInstant(Instant.parse("2026-10-06T00:00:00Z"));
        timedLogic.refreshToday();
        assertEquals(LocalDate.of(2026, 10, 6), liveModel.getToday());
    }

    /**
     * Executes the command and confirms that
     * - no exceptions are thrown <br>
     * - the feedback message is equal to {@code expectedMessage} <br>
     * - the internal model manager state is the same as that in {@code expectedModel} <br>
     * @see #assertCommandFailure(String, Class, String, Model)
     */
    private void assertCommandSuccess(String inputCommand, String expectedMessage,
            Model expectedModel) throws CommandException, ParseException {
        CommandResult result = logic.execute(inputCommand);
        assertEquals(expectedMessage, result.getFeedbackToUser());
        assertEquals(expectedModel, model);
    }

    /**
     * Executes the command, confirms that a ParseException is thrown and that the result message is correct.
     * @see #assertCommandFailure(String, Class, String, Model)
     */
    private void assertParseException(String inputCommand, String expectedMessage) {
        assertCommandFailure(inputCommand, ParseException.class, expectedMessage);
    }

    /**
     * Executes the command, confirms that a CommandException is thrown and that the result message is correct.
     * @see #assertCommandFailure(String, Class, String, Model)
     */
    private void assertCommandException(String inputCommand, String expectedMessage) {
        assertCommandFailure(inputCommand, CommandException.class, expectedMessage);
    }

    /**
     * Executes the command, confirms that the exception is thrown and that the result message is correct.
     * @see #assertCommandFailure(String, Class, String, Model)
     */
    private void assertCommandFailure(String inputCommand, Class<? extends Throwable> expectedException,
            String expectedMessage) {
        Model expectedModel = new ModelManager(model.getAddressBook(), new UserPrefs());
        assertCommandFailure(inputCommand, expectedException, expectedMessage, expectedModel);
    }

    /**
     * Executes the command and confirms that
     * - the {@code expectedException} is thrown <br>
     * - the resulting error message is equal to {@code expectedMessage} <br>
     * - the internal model manager state is the same as that in {@code expectedModel} <br>
     * @see #assertCommandSuccess(String, String, Model)
     */
    private void assertCommandFailure(String inputCommand, Class<? extends Throwable> expectedException,
            String expectedMessage, Model expectedModel) {
        assertThrows(expectedException, expectedMessage, () -> logic.execute(inputCommand));
        assertEquals(expectedModel, model);
    }

    /**
     * Tests the Logic component's handling of an {@code IOException} thrown by the Storage component.
     *
     * @param e the exception to be thrown by the Storage component
     * @param expectedMessage the message expected inside exception thrown by the Logic component
     */
    private void assertCommandFailureForExceptionFromStorage(IOException e, String expectedMessage) {
        Path prefPath = temporaryFolder.resolve("ExceptionUserPrefs.json");

        // Inject LogicManager with a JsonAddressBookStorage that throws the IOException e when saving
        JsonAddressBookStorage addressBookStorage = new JsonAddressBookStorage(prefPath) {
            @Override
            public void saveAddressBook(ReadOnlyAddressBook addressBook) throws IOException {
                throw e;
            }
        };

        JsonUserPrefsStorage userPrefsStorage =
                new JsonUserPrefsStorage(temporaryFolder.resolve("ExceptionUserPrefs.json"));
        StorageManager storage = new StorageManager(addressBookStorage, userPrefsStorage);

        logic = new LogicManager(model, storage);

        // Triggers the saveAddressBook method by executing an add command
        String addCommand = AddCommand.COMMAND_WORD + NAME_DESC_AMY + PHONE_DESC_AMY
                + EMAIL_DESC_AMY + ADDRESS_DESC_AMY;
        assertCommandFailure(addCommand, CommandException.class, expectedMessage, model);
    }
}
