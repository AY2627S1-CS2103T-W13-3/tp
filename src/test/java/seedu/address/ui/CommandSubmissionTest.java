package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import seedu.address.commons.core.LogsCenter;
import seedu.address.logic.LogicManager;
import seedu.address.logic.Messages;
import seedu.address.logic.commands.CommandResult;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.AddressBook;
import seedu.address.model.ModelManager;
import seedu.address.model.ReadOnlyAddressBook;
import seedu.address.model.UserPrefs;
import seedu.address.model.person.Person;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;
import seedu.address.testutil.PersonBuilder;

public class CommandSubmissionTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 7);
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-07T00:00:00Z"), ZoneOffset.UTC);

    @TempDir
    public Path temporaryFolder;

    @BeforeAll
    public static void initializeToolkit() throws InterruptedException {
        assumeFalse(GraphicsEnvironment.isHeadless(), "JavaFX control tests require a graphical session");
        CountDownLatch initialized = new CountDownLatch(1);
        Platform.startup(() -> {
            Platform.setImplicitExit(false);
            initialized.countDown();
        });
        assertTrue(initialized.await(10, TimeUnit.SECONDS), "JavaFX toolkit did not start");
    }

    @Test
    public void constructor_nullExecutor_throwsNullPointerException() throws Exception {
        NullPointerException exception = runOnFxThread(() ->
                assertThrows(NullPointerException.class, () -> new CommandBox(null)));

        assertEquals("commandExecutor", exception.getMessage());
    }

    @Test
    public void constructor_validExecutor_loadsEditableFieldWithoutExecuting() throws Exception {
        AtomicInteger executionCount = new AtomicInteger();

        runOnFxThread(() -> {
            CommandBox commandBox = new CommandBox(commandText -> {
                executionCount.incrementAndGet();
                return new CommandResult("Listed all clients.");
            });
            TextField commandTextField = assertInstanceOf(TextField.class,
                    commandBox.getRoot().lookup("#commandTextField"));

            commandTextField.setText("list");
            assertTrue(commandTextField.isEditable());
            assertEquals("list", commandTextField.getText());
            return null;
        });

        assertEquals(0, executionCount.get());
    }

    @Test
    public void submit_enterAndRun_executeIdenticalTextExactlyOnce() throws Exception {
        runOnFxThread(() -> {
            List<String> executedTexts = new ArrayList<>();
            List<String> expectedTexts = new ArrayList<>();
            CommandBox commandBox = new CommandBox(commandText -> {
                executedTexts.add(commandText);
                return new CommandResult("Command submitted.");
            });
            new Scene(commandBox.getRoot());
            commandBox.getRoot().applyCss();
            TextField commandTextField = assertInstanceOf(TextField.class,
                    commandBox.getRoot().lookup("#commandTextField"));
            Button runButton = assertInstanceOf(Button.class, commandBox.getRoot().lookup("#runButton"));

            for (String input : List.of("list", "", "  \t  ", "  followup 1 clear  ", "invalid command")) {
                commandTextField.setText(input);
                String currentText = commandTextField.getText();
                commandTextField.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.ENTER,
                        false, false, false, false));
                commandTextField.fireEvent(new KeyEvent(KeyEvent.KEY_RELEASED, "", "", KeyCode.ENTER,
                        false, false, false, false));
                expectedTexts.add(currentText);
                assertEquals(expectedTexts, executedTexts, "Enter must submit the current text exactly once");

                commandTextField.setText(input);
                assertEquals(currentText, commandTextField.getText());
                runButton.fire();
                expectedTexts.add(currentText);
                assertEquals(expectedTexts, executedTexts, "Run must submit the same text exactly once");
            }
            return null;
        });
    }

    @Test
    public void runButton_isAccessibleAndDoesNotSubmitAsDefaultButton() throws Exception {
        runOnFxThread(() -> {
            CommandBox commandBox = new CommandBox(commandText -> new CommandResult("Command submitted."));
            Button runButton = assertInstanceOf(Button.class, commandBox.getRoot().lookup("#runButton"));

            assertFalse(runButton.isDefaultButton(), "Enter in the command field must not also activate Run");
            assertTrue(runButton.isFocusTraversable(), "Run must be reachable using the keyboard");
            assertFalse(runButton.getText().isBlank(), "Run must have a visible label");
            assertFalse(runButton.getAccessibleText().isBlank(), "Run must have an accessible label");
            return null;
        });
    }

    @Test
    public void submit_rejectedCommands_retainInputStateAndFocusThenRecover() throws Exception {
        runOnFxThread(() -> {
            for (SubmissionMethod method : SubmissionMethod.values()) {
                AddressBook addressBook = new AddressBook();
                Person person = new PersonBuilder().build();
                addressBook.addPerson(person);
                ModelManager model = new ModelManager(addressBook, new UserPrefs(), TODAY, false);
                model.selectPerson(person);
                Path dataFile = temporaryFolder.resolve(method.name() + ".json");
                LogicManager logic = new LogicManager(model, new StorageManager(
                        new JsonAddressBookStorage(dataFile),
                        new JsonUserPrefsStorage(temporaryFolder.resolve("preferences.json"))), CLOCK);
                List<String> failures = new ArrayList<>();
                AtomicInteger executionCount = new AtomicInteger();
                CommandBoxFixture fixture = new CommandBoxFixture(commandText -> {
                    executionCount.incrementAndGet();
                    try {
                        return logic.execute(commandText);
                    } catch (CommandException | ParseException e) {
                        failures.add(e.getMessage());
                        throw e;
                    }
                });
                List<String> inputs = List.of("", "   ", "invalid command", "delete 9", "followup 999 d/bad m/");
                List<String> expectedMessages = List.of("Enter a command.", "Enter a command.",
                        Messages.MESSAGE_UNKNOWN_COMMAND, Messages.MESSAGE_INVALID_PERSON_DISPLAYED_INDEX,
                        Messages.MESSAGE_INVALID_PERSON_DISPLAYED_INDEX);

                for (int i = 0; i < inputs.size(); i++) {
                    fixture.commandTextField.setText(inputs.get(i));
                    for (int attempt = 0; attempt < 2; attempt++) {
                        fixture.submit(method);
                        assertEquals(inputs.get(i), fixture.commandTextField.getText());
                        assertEquals(expectedMessages.get(i), failures.getLast());
                        assertEquals(1, errorClassCount(fixture.commandTextField));
                        assertSame(fixture.commandTextField, fixture.scene.getFocusOwner());
                        assertEquals(addressBook, model.getAddressBook());
                        assertEquals(List.of(person), model.getFilteredPersonList());
                        assertSame(person, model.selectedPersonProperty().get());
                        assertEquals(TODAY, model.getToday());
                        assertFalse(Files.exists(dataFile));
                    }
                }
                assertEquals(inputs.size() * 2, executionCount.get());

                fixture.commandTextField.setText("list");
                assertEquals(0, errorClassCount(fixture.commandTextField), "Editing must reset the error style");
                fixture.submit(method);
                assertEquals("", fixture.commandTextField.getText());
                assertEquals(0, errorClassCount(fixture.commandTextField));
                assertSame(fixture.commandTextField, fixture.scene.getFocusOwner());
                assertEquals(inputs.size() * 2 + 1, executionCount.get());
            }
            return null;
        });
    }

    @Test
    public void submit_saveFailure_retainsInputAndCommittedDataThenRetries() throws Exception {
        runOnFxThread(() -> {
            for (SubmissionMethod method : SubmissionMethod.values()) {
                AddressBook addressBook = new AddressBook();
                Person person = new PersonBuilder().build();
                addressBook.addPerson(person);
                ModelManager model = new ModelManager(addressBook, new UserPrefs(), TODAY, false);
                model.selectPerson(person);
                Path dataFile = temporaryFolder.resolve(method.name() + ".json");
                new JsonAddressBookStorage(dataFile).saveAddressBook(addressBook);
                byte[] savedData = Files.readAllBytes(dataFile);
                AtomicBoolean failSave = new AtomicBoolean(true);
                AtomicInteger saveCount = new AtomicInteger();
                JsonAddressBookStorage storage = new JsonAddressBookStorage(dataFile) {
                    @Override
                    public void saveAddressBook(ReadOnlyAddressBook data) throws IOException {
                        saveCount.incrementAndGet();
                        if (failSave.get()) {
                            throw new IOException("Simulated save failure");
                        }
                        super.saveAddressBook(data);
                    }
                };
                LogicManager logic = new LogicManager(model, new StorageManager(storage,
                        new JsonUserPrefsStorage(temporaryFolder.resolve("preferences.json"))), CLOCK);
                List<String> failures = new ArrayList<>();
                CommandBoxFixture fixture = new CommandBoxFixture(commandText -> {
                    try {
                        return logic.execute(commandText);
                    } catch (CommandException e) {
                        failures.add(e.getMessage());
                        throw e;
                    }
                });
                fixture.commandTextField.setText("delete 1");

                for (int attempt = 0; attempt < 2; attempt++) {
                    fixture.submit(method);
                    assertEquals("delete 1", fixture.commandTextField.getText());
                    assertEquals(LogicManager.MESSAGE_SAVE_FAILED, failures.getLast());
                    assertEquals(1, errorClassCount(fixture.commandTextField));
                    assertSame(fixture.commandTextField, fixture.scene.getFocusOwner());
                    assertEquals(addressBook, model.getAddressBook());
                    assertEquals(List.of(person), model.getFilteredPersonList());
                    assertSame(person, model.selectedPersonProperty().get());
                    assertEquals(TODAY, model.getToday());
                    assertArrayEquals(savedData, Files.readAllBytes(dataFile));
                    assertEquals(attempt + 1, saveCount.get());
                }

                failSave.set(false);
                fixture.submit(method);
                assertEquals("", fixture.commandTextField.getText());
                assertEquals(0, errorClassCount(fixture.commandTextField));
                assertSame(fixture.commandTextField, fixture.scene.getFocusOwner());
                assertTrue(model.getAddressBook().getPersonList().isEmpty());
                assertTrue(new JsonAddressBookStorage(dataFile).readAddressBook().orElseThrow()
                        .getPersonList().isEmpty());
                assertEquals(3, saveCount.get());
            }
            return null;
        });
    }

    @Test
    public void submit_unexpectedFailure_propagatesWithoutClearingInput() throws Exception {
        runOnFxThread(() -> {
            IllegalStateException expected = new IllegalStateException("Executor unavailable");
            CommandBoxFixture fixture = new CommandBoxFixture(commandText -> {
                throw expected;
            });
            fixture.commandTextField.setText("list");

            RuntimeException exception = assertThrows(RuntimeException.class, () ->
                    fixture.submit(SubmissionMethod.RUN_ACTION));
            Throwable cause = exception;
            while (cause.getCause() != null) {
                cause = cause.getCause();
            }
            assertSame(expected, cause);
            assertEquals("list", fixture.commandTextField.getText());
            assertSame(fixture.commandTextField, fixture.scene.getFocusOwner());
            return null;
        });
    }

    @Test
    public void submit_diagnostics_recordOutcomesWithoutPrivateData() throws Exception {
        runOnFxThread(() -> {
            String privateDetails = "PRIVATE_CLIENT_COMMAND_AND_ERROR";
            CommandBoxFixture fixture = new CommandBoxFixture(commandText -> {
                if (commandText.startsWith("parse")) {
                    throw new ParseException(privateDetails);
                }
                if (commandText.startsWith("save")) {
                    throw new CommandException(privateDetails, new IOException(privateDetails));
                }
                return new CommandResult(privateDetails);
            });
            Logger logger = LogsCenter.getLogger(CommandBox.class);
            List<LogRecord> records = new ArrayList<>();
            Handler handler = new Handler() {
                @Override
                public void publish(LogRecord record) {
                    records.add(record);
                }

                @Override
                public void flush() {}

                @Override
                public void close() {}
            };
            Level previousLevel = logger.getLevel();
            boolean usedParentHandlers = logger.getUseParentHandlers();
            logger.setLevel(Level.FINE);
            logger.setUseParentHandlers(false);
            logger.addHandler(handler);
            try {
                for (String prefix : List.of("parse", "save", "list")) {
                    fixture.commandTextField.setText(prefix + " " + privateDetails);
                    fixture.submit(SubmissionMethod.RUN_ACTION);
                }
                assertEquals(3, records.size(), "Every submission must record an outcome at diagnostic level");
                assertTrue(records.get(0).getMessage().contains("ParseException"));
                assertTrue(records.get(1).getMessage().contains("CommandException"));
                for (LogRecord record : records) {
                    assertFalse(record.getMessage().contains(privateDetails));
                    assertNull(record.getThrown(), "Exception details can contain client data");
                    assertNull(record.getParameters(), "Raw command values must not be logged as parameters");
                }
            } finally {
                logger.removeHandler(handler);
                logger.setLevel(previousLevel);
                logger.setUseParentHandlers(usedParentHandlers);
            }
            return null;
        });
    }

    private static long errorClassCount(TextField commandTextField) {
        return commandTextField.getStyleClass().stream().filter(CommandBox.ERROR_STYLE_CLASS::equals).count();
    }

    private static void pressKey(Control control, KeyCode keyCode) {
        control.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", keyCode, false, false, false, false));
        control.fireEvent(new KeyEvent(KeyEvent.KEY_RELEASED, "", "", keyCode, false, false, false, false));
    }

    private enum SubmissionMethod {
        ENTER,
        RUN_ACTION,
        RUN_KEYBOARD
    }

    /**
     * Loads the real command controls into a scene for submission and logical focus checks.
     */
    private static class CommandBoxFixture {
        private final Scene scene;
        private final TextField commandTextField;
        private final Button runButton;

        CommandBoxFixture(CommandBox.CommandExecutor commandExecutor) {
            CommandBox commandBox = new CommandBox(commandExecutor);
            scene = new Scene(commandBox.getRoot());
            commandBox.getRoot().applyCss();
            commandTextField = assertInstanceOf(TextField.class, commandBox.getRoot().lookup("#commandTextField"));
            runButton = assertInstanceOf(Button.class, commandBox.getRoot().lookup("#runButton"));
        }

        void submit(SubmissionMethod method) {
            switch (method) {
                case ENTER:
                    commandTextField.requestFocus();
                    pressKey(commandTextField, KeyCode.ENTER);
                    break;
                case RUN_ACTION:
                    runButton.requestFocus();
                    runButton.fire();
                    break;
                case RUN_KEYBOARD:
                    runButton.requestFocus();
                    pressKey(runButton, KeyCode.SPACE);
                    break;
                default:
                    throw new AssertionError("Unknown submission method");
            }
        }
    }

    /**
     * Runs a check on the JavaFX application thread and propagates failures to JUnit.
     */
    private static <T> T runOnFxThread(Callable<T> action) throws Exception {
        FutureTask<T> task = new FutureTask<>(action);
        Platform.runLater(task);
        return task.get(10, TimeUnit.SECONDS);
    }
}
