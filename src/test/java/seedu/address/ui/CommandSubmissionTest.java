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
import javafx.css.CssMetaData;
import javafx.css.PseudoClass;
import javafx.css.Styleable;
import javafx.geometry.Bounds;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Control;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.robot.Robot;
import javafx.scene.text.Text;
import javafx.stage.Stage;
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
        Runnable initialize = () -> {
            Platform.setImplicitExit(false);
            initialized.countDown();
        };
        try {
            Platform.startup(initialize);
        } catch (IllegalStateException alreadyStarted) {
            // Other component tests may have initialized the shared JavaFX runtime first.
            Platform.runLater(initialize);
        }
        assertTrue(initialized.await(10, TimeUnit.SECONDS), "JavaFX toolkit did not start");
    }

    @Test
    public void initializeToolkit_alreadyRunning_remainsUsable() throws Exception {
        initializeToolkit();
        initializeToolkit();
        assertTrue(runOnFxThread(Platform::isFxApplicationThread));
        assertFalse(runOnFxThread(Platform::isImplicitExit));
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
    public void commandBar_existingHost_keepsControlsReadableAndWithinBounds() throws Exception {
        runOnFxThread(() -> {
            Path dataFile = temporaryFolder.resolve("clients.json");
            LogicManager logic = new LogicManager(new ModelManager(), new StorageManager(
                    new JsonAddressBookStorage(dataFile),
                    new JsonUserPrefsStorage(temporaryFolder.resolve("preferences.json"))), CLOCK);
            MainWindow window = new MainWindow(new Stage(), logic, dataFile);
            try {
                window.fillInnerParts();
                Scene scene = window.getPrimaryStage().getScene();
                Region root = assertInstanceOf(Region.class, scene.getRoot());
                TextField commandTextField = assertInstanceOf(TextField.class, root.lookup("#commandTextField"));
                Button runButton = assertInstanceOf(Button.class, root.lookup("#runButton"));
                Region commandBar = assertInstanceOf(Region.class, commandTextField.getParent());
                commandTextField.setText("find " + "Long command text ".repeat(30));

                for (double width : new double[] {450, 853, 1280, 1920}) {
                    root.resize(width, 720);
                    root.applyCss();
                    root.layout();
                    assertControlsFit(commandBar, commandTextField, runButton);
                    assertReadable(commandTextField);
                    assertTrue(contrastRatio((Color) runButton.getTextFill(),
                            (Color) runButton.getBackground().getFills().getFirst().getFill()) >= 4.5,
                            "The Run label must remain readable in the existing host");
                }

                commandTextField.setText("invalid command");
                runButton.fire();
                root.applyCss();
                assertEquals("invalid command", commandTextField.getText());
                assertEquals(1, errorClassCount(commandTextField));
                assertReadable(commandTextField);
            } finally {
                window.getPrimaryStage().close();
            }
            return null;
        });
    }

    @Test
    public void commandBar_narrowWidth_keepsInputAndRunAvailable() throws Exception {
        runOnFxThread(() -> {
            CommandBoxFixture fixture = new CommandBoxFixture(commandText -> new CommandResult("Submitted."));
            Region root = assertInstanceOf(Region.class, fixture.scene.getRoot());
            fixture.commandTextField.setText("find " + "Long command text ".repeat(30));

            for (double width : new double[] {320, 450, 853}) {
                root.resize(width, root.prefHeight(width));
                root.applyCss();
                root.layout();
                assertControlsFit(root, fixture.commandTextField, fixture.runButton);
                fixture.commandTextField.positionCaret(fixture.commandTextField.getLength());
                assertEquals(fixture.commandTextField.getLength(), fixture.commandTextField.getCaretPosition());
            }
            return null;
        });
    }

    @Test
    public void commandBar_tabAndSpace_submitOnceAndReturnFocus() throws Exception {
        runOnFxThread(() -> {
            AtomicInteger executionCount = new AtomicInteger();
            CommandBoxFixture fixture = new CommandBoxFixture(commandText -> {
                executionCount.incrementAndGet();
                return new CommandResult("Submitted.");
            });
            fixture.commandTextField.setText("list");
            fixture.commandTextField.requestFocus();
            pressKey(fixture.commandTextField, KeyCode.TAB);
            assertSame(fixture.runButton, fixture.scene.getFocusOwner(), "Tab must reach Run from the input");
            pressKey(fixture.runButton, KeyCode.SPACE);
            assertEquals(1, executionCount.get());
            assertEquals("", fixture.commandTextField.getText());
            assertSame(fixture.commandTextField, fixture.scene.getFocusOwner());
            assertFalse(fixture.commandTextField.getAccessibleText().isBlank());
            assertFalse(fixture.commandTextField.getAccessibleHelp().isBlank());
            assertFalse(fixture.runButton.getAccessibleHelp().isBlank());
            return null;
        });
    }

    @Test
    public void commandBar_buttonStates_keepReadableLabelAndVisibleFocus() throws Exception {
        runOnFxThread(() -> {
            CommandBoxFixture fixture = new CommandBoxFixture(commandText -> new CommandResult("Submitted."));
            Region root = assertInstanceOf(Region.class, fixture.scene.getRoot());
            root.resize(450, root.prefHeight(450));
            root.layout();
            double buttonWidth = fixture.runButton.getWidth();

            for (String state : List.of("hover", "pressed", "focused")) {
                PseudoClass pseudoClass = PseudoClass.getPseudoClass(state);
                fixture.runButton.pseudoClassStateChanged(pseudoClass, true);
                root.applyCss();
                root.layout();
                Color buttonBackground = (Color) fixture.runButton.getBackground().getFills().getFirst().getFill();
                assertTrue(contrastRatio((Color) fixture.runButton.getTextFill(), buttonBackground) >= 4.5,
                        "The Run label must stay readable in every interactive state");
                assertEquals(buttonWidth, fixture.runButton.getWidth(), "Interaction must not shift the layout");
                if (state.equals("focused")) {
                    Color focusBorder = (Color) fixture.runButton.getBorder().getStrokes().getFirst().getTopStroke();
                    Color barBackground = (Color) root.getBackground().getFills().getFirst().getFill();
                    assertEquals(1.0, focusBorder.getOpacity(), "The focus indicator must be visible");
                    assertTrue(contrastRatio(focusBorder, barBackground) >= 3,
                            "Keyboard focus must be distinguishable against the command bar");
                }
                fixture.runButton.pseudoClassStateChanged(pseudoClass, false);
            }
            return null;
        });
    }

    @Test
    public void commandBar_inputStates_keepVisibleBoundary() throws Exception {
        runOnFxThread(() -> {
            CommandBoxFixture fixture = new CommandBoxFixture(commandText -> new CommandResult("Submitted."));
            Region root = assertInstanceOf(Region.class, fixture.scene.getRoot());
            PseudoClass focused = PseudoClass.getPseudoClass("focused");
            for (boolean isError : List.of(false, true)) {
                if (isError) {
                    fixture.commandTextField.getStyleClass().add(CommandBox.ERROR_STYLE_CLASS);
                }
                for (boolean isFocused : List.of(false, true)) {
                    fixture.commandTextField.pseudoClassStateChanged(focused, isFocused);
                    root.applyCss();
                    Color border = (Color) fixture.commandTextField.getBorder().getStrokes().getFirst()
                            .getTopStroke();
                    Color background = (Color) root.getBackground().getFills().getFirst().getFill();
                    assertEquals(1.0, border.getOpacity(), "The input boundary must be visible");
                    assertTrue(contrastRatio(border, background) >= 3,
                            "The input boundary must contrast against the command bar in every state");
                    assertReadable(fixture.commandTextField);
                }
            }
            return null;
        });
    }

    @Test
    public void submit_mouseClick_submitsOnceAndRestoresTypingAfterSuccessAndFailure() throws Exception {
        assertShownSubmission(SubmissionMethod.RUN_ACTION);
    }

    @Test
    public void submit_enterKey_submitsOnceAndRestoresTypingAfterSuccessAndFailure() throws Exception {
        assertShownSubmission(SubmissionMethod.ENTER);
    }

    @Test
    public void submit_tabAndSpace_submitsOnceAndRestoresTypingAfterSuccessAndFailure() throws Exception {
        assertShownSubmission(SubmissionMethod.RUN_KEYBOARD);
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
                    assertEquals(String.format(LogicManager.MESSAGE_SAVE_FAILED, dataFile), failures.getLast());
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

    /**
     * Exercises native input in a shown window, including typing again without refocusing after submission.
     * Hosted macOS native desktop checks require explicit opt-in after their test worker aborted in CI.
     */
    private static void assertShownSubmission(SubmissionMethod method) throws Exception {
        boolean isMacActionsRunner = "true".equals(System.getenv("GITHUB_ACTIONS"))
                && "macOS".equals(System.getenv("RUNNER_OS"));
        boolean isNativeUiEnabled = Boolean.parseBoolean(System.getenv("POLICYHARBOR_NATIVE_UI_TESTS"));
        // Only native desktop checks are gated; the remaining JavaFX component tests still run on macOS.
        assumeFalse(isMacActionsRunner && !isNativeUiEnabled,
                "Native desktop tests are opt-in on macOS Actions after SIGABRT; "
                        + "set POLICYHARBOR_NATIVE_UI_TESTS=true on a runner configured for native GUI input");
        List<String> executedTexts = new ArrayList<>();
        CommandBoxFixture fixture = runOnFxThread(() -> new CommandBoxFixture(commandText -> {
            executedTexts.add(commandText);
            switch (commandText) {
                case "parse":
                    throw new ParseException("Invalid command format.");
                case "command":
                    throw new CommandException("Invalid client index.");
                case "save":
                    throw new CommandException("Unable to save changes.", new IOException("Simulated save failure"));
                default:
                    return new CommandResult("Submitted.");
            }
        }));
        Stage stage = runOnFxThread(() -> showCommandWindow(fixture.scene));
        try {
            Robot robot = runOnFxThread(Robot::new);
            awaitFxCondition(stage::isFocused, "The test window must receive native keyboard input");
            AtomicBoolean inputClicked = new AtomicBoolean();
            runOnFxThread(() -> {
                fixture.commandTextField.addEventFilter(MouseEvent.MOUSE_RELEASED, event -> inputClicked.set(true));
                clickControl(robot, fixture.commandTextField);
                return null;
            });
            awaitFxCondition(inputClicked::get, "The initial native click must reach the test input");
            awaitFxCondition(fixture.commandTextField::isFocused, "The test input must receive native focus");
            List<String> expectedTexts = new ArrayList<>();
            for (String input : List.of("list", "parse", "command", "save")) {
                runOnFxThread(() -> {
                    fixture.commandTextField.setText(input);
                    fixture.commandTextField.positionCaret(input.length());
                    fixture.commandTextField.requestFocus();
                    return null;
                });
                awaitFxCondition(fixture.commandTextField::isFocused, "The input must be focused before submission");
                if (method == SubmissionMethod.RUN_KEYBOARD) {
                    runOnFxThread(() -> {
                        robot.keyType(KeyCode.TAB);
                        return null;
                    });
                    awaitFxCondition(fixture.runButton::isFocused, "Tab must reach Run before submitting " + input);
                }
                runOnFxThread(() -> {
                    if (method == SubmissionMethod.RUN_ACTION) {
                        clickControl(robot, fixture.runButton);
                    } else {
                        robot.keyType(method == SubmissionMethod.ENTER ? KeyCode.ENTER : KeyCode.SPACE);
                    }
                    return null;
                });
                expectedTexts.add(input);
                int expectedCount = expectedTexts.size();
                awaitFxCondition(() -> executedTexts.size() >= expectedCount, "Native input must submit the command");
                awaitFxCondition(fixture.commandTextField::isFocused, "Submission must restore actual input focus");
                String retainedText = input.equals("list") ? "" : input;
                runOnFxThread(() -> {
                    assertEquals(expectedTexts, executedTexts, "Each interaction must submit exactly once");
                    assertEquals(retainedText, fixture.commandTextField.getText());
                    assertEquals(input.equals("list") ? 0 : 1, errorClassCount(fixture.commandTextField));
                    return null;
                });
                assertCanTypeAfterSubmission(fixture.commandTextField, robot, retainedText);
                runOnFxThread(() -> {
                    assertEquals(expectedTexts, executedTexts, "Typing must not cause another submission");
                    assertEquals(0, errorClassCount(fixture.commandTextField), "Typing must clear the error style");
                    return null;
                });
            }
        } finally {
            runOnFxThread(() -> {
                stage.close();
                return null;
            });
        }
    }

    /**
     * Shows the test scene above other desktop windows. Must be called on the JavaFX thread.
     */
    private static Stage showCommandWindow(Scene scene) {
        Stage window = new Stage();
        window.setTitle("Command submission regression test");
        window.setScene(scene);
        window.setWidth(600);
        window.setHeight(180);
        window.setAlwaysOnTop(true);
        window.show();
        window.toFront();
        window.requestFocus();
        return window;
    }

    /**
     * Clicks the visible control using native mouse input. Must be called on the JavaFX thread.
     */
    private static void clickControl(Robot robot, Control control) {
        Bounds bounds = control.localToScreen(control.getBoundsInLocal());
        assertTrue(bounds != null, "The control must be visible on screen");
        robot.mouseMove(bounds.getCenterX(), bounds.getCenterY());
        robot.mouseClick(MouseButton.PRIMARY);
    }

    /**
     * Replaces the retained command using only native keyboard input, without requesting focus.
     */
    private static void assertCanTypeAfterSubmission(TextField commandTextField, Robot robot, String retainedText)
            throws Exception {
        runOnFxThread(() -> {
            KeyCode shortcut = System.getProperty("os.name").startsWith("Mac") ? KeyCode.META : KeyCode.CONTROL;
            robot.keyPress(shortcut);
            robot.keyType(KeyCode.A);
            robot.keyRelease(shortcut);
            return null;
        });
        awaitFxCondition(() -> commandTextField.getSelectedText().equals(retainedText),
                "The native select-all shortcut must reach the input after submission");
        runOnFxThread(() -> {
            robot.keyType(KeyCode.X);
            return null;
        });
        awaitFxCondition(() -> commandTextField.getText().equals("x"),
                "Typing after submission must reach the input without clicking or requesting focus");
    }

    /**
     * Waits outside the JavaFX thread while polling observable UI state on that thread.
     */
    private static void awaitFxCondition(Callable<Boolean> condition, String message) throws Exception {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < deadline) {
            if (runOnFxThread(condition)) {
                return;
            }
            TimeUnit.MILLISECONDS.sleep(20);
        }
        assertTrue(runOnFxThread(condition), message);
    }

    private static long errorClassCount(TextField commandTextField) {
        return commandTextField.getStyleClass().stream().filter(CommandBox.ERROR_STYLE_CLASS::equals).count();
    }

    private static void assertControlsFit(Region commandBar, TextField commandTextField, Button runButton) {
        assertTrue(commandTextField.getWidth() >= 100, "The command field must retain usable editing space");
        assertTrue(commandTextField.getBoundsInParent().getMaxX() <= runButton.getLayoutX(),
                "The input and Run button must not overlap");
        assertTrue(runButton.getBoundsInParent().getMaxX() <= commandBar.getWidth(),
                "Run must remain inside the command bar");
        assertTrue(runButton.getBoundsInParent().getMaxY() <= commandBar.getHeight(),
                "Run must fit the command bar height");
        Text label = assertInstanceOf(Text.class, runButton.lookup(".text"));
        Bounds labelBounds = runButton.sceneToLocal(label.localToScene(label.getBoundsInLocal()));
        assertTrue(runButton.getLayoutBounds().contains(labelBounds), "The Run label must not be clipped");
    }

    private static void assertReadable(TextField commandTextField) {
        Color background = (Color) commandTextField.getBackground().getFills().getLast().getFill();
        assertTrue(contrastRatio(textFieldColor(commandTextField, "-fx-text-fill"), background) >= 4.5,
                "Command text must remain readable in normal and error states");
        assertTrue(contrastRatio(textFieldColor(commandTextField, "-fx-prompt-text-fill"), background) >= 4.5,
                "The command prompt must remain readable");
    }

    @SuppressWarnings("unchecked")
    private static Color textFieldColor(TextField commandTextField, String property) {
        CssMetaData<Styleable, ?> metadata = (CssMetaData<Styleable, ?>) commandTextField.getCssMetaData().stream()
                .filter(cssProperty -> cssProperty.getProperty().equals(property)).findFirst().orElseThrow();
        return assertInstanceOf(Color.class, metadata.getStyleableProperty(commandTextField).getValue());
    }

    private static double contrastRatio(Color first, Color second) {
        double firstLuminance = relativeLuminance(first);
        double secondLuminance = relativeLuminance(second);
        return (Math.max(firstLuminance, secondLuminance) + 0.05)
                / (Math.min(firstLuminance, secondLuminance) + 0.05);
    }

    private static double relativeLuminance(Color color) {
        return 0.2126 * linearChannel(color.getRed()) + 0.7152 * linearChannel(color.getGreen())
                + 0.0722 * linearChannel(color.getBlue());
    }

    private static double linearChannel(double channel) {
        return channel <= 0.04045 ? channel / 12.92 : Math.pow((channel + 0.055) / 1.055, 2.4);
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
