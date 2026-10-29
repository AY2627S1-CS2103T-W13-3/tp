package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.awt.GraphicsEnvironment;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javafx.event.ActionEvent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DataFormat;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import seedu.address.logic.LogicManager;
import seedu.address.logic.Messages;
import seedu.address.logic.commands.CommandResult;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.ModelManager;
import seedu.address.storage.JsonAddressBookStorage;
import seedu.address.storage.JsonUserPrefsStorage;
import seedu.address.storage.StorageManager;
import seedu.address.testutil.FxTestSupport;

public class CommandPasteTest {

    @TempDir
    public Path temporaryFolder;

    @BeforeAll
    public static void initializeToolkit() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless(), "JavaFX clipboard tests require a graphical session");
        FxTestSupport.initializeToolkit();
    }

    @Test
    public void constructor_nullInputErrorHandler_throwsNullPointerException() throws Exception {
        runOnFxThread(() -> {
            assertThrows(NullPointerException.class, () ->
                    new CommandBox(commandText -> new CommandResult("Submitted."), null));
            return null;
        });
    }

    @Test
    public void paste_invalidClipboard_keepsDraftSelectionAndDoesNotExecute() throws Exception {
        runOnFxThread(() -> {
            AtomicInteger executionCount = new AtomicInteger();
            CommandBox commandBox = new CommandBox(commandText -> {
                executionCount.incrementAndGet();
                return new CommandResult("Submitted.");
            });
            TextField input = (TextField) commandBox.getRoot().lookup("#commandTextField");
            withRestoredClipboard(() -> {
                for (String separator : List.of("\n", "\r\n", "\t", "\u2028", "\u2029")) {
                    input.setText("find Alice");
                    input.selectRange(8, 5);
                    setClipboard("add n/Alice p/111 e/alice@example.com a/First" + separator + "Second");

                    input.paste();

                    assertEquals("find Alice", input.getText(), "Reject the whole paste before JavaFX strips text");
                    assertEquals(8, input.getAnchor());
                    assertEquals(5, input.getCaretPosition());
                    assertTrue(input.getStyleClass().contains(CommandBox.ERROR_STYLE_CLASS));
                    assertEquals(0, executionCount.get(), "A paste must never submit a command to report an error");
                }
            });
            return null;
        });
    }

    @Test
    public void replace_controlCharacters_keepsDraftAndReportsErrorWithoutExecuting() throws Exception {
        runOnFxThread(() -> {
            AtomicInteger executionCount = new AtomicInteger();
            List<String> feedbackMessages = new ArrayList<>();
            CommandBox commandBox = new CommandBox(commandText -> {
                executionCount.incrementAndGet();
                return new CommandResult("Submitted.");
            }, feedbackMessages::add);
            TextField input = (TextField) commandBox.getRoot().lookup("#commandTextField");
            // Native text clipboards can truncate at NUL or change other controls. Exercise the raw
            // replacement boundary directly so these checks do not depend on OS clipboard encoding.
            for (String control : List.of("\u0000", "\u0001", "\u001f", "\u007f", "\u0080", "\u0085", "\u009f")) {
                input.setText("find Alice");
                input.selectRange(10, 5);
                feedbackMessages.clear();

                input.replaceSelection("First" + control + "Second");
                input.replaceText(5, 10, "First" + control + "Second");

                assertEquals("find Alice", input.getText());
                assertEquals(10, input.getAnchor());
                assertEquals(5, input.getCaretPosition());
                assertTrue(input.getStyleClass().contains(CommandBox.ERROR_STYLE_CLASS));
                assertEquals(List.of(Messages.MESSAGE_INVALID_CHARACTERS, Messages.MESSAGE_INVALID_CHARACTERS),
                        feedbackMessages);
                assertEquals(0, executionCount.get());
            }
            input.replaceSelection("Bob");
            assertEquals("find Bob", input.getText());
            assertFalse(input.getStyleClass().contains(CommandBox.ERROR_STYLE_CLASS));
            return null;
        });
    }

    @Test
    public void paste_invalidThenValid_showsFeedbackAndAllowsNormalSubmission() throws Exception {
        runOnFxThread(() -> {
            Path dataFile = temporaryFolder.resolve("clients.json");
            ModelManager model = new ModelManager();
            MainWindow window = new MainWindow(new Stage(), new LogicManager(model, new StorageManager(
                    new JsonAddressBookStorage(dataFile),
                    new JsonUserPrefsStorage(temporaryFolder.resolve("preferences.json")))), dataFile);
            try {
                window.fillInnerParts();
                Scene scene = window.getPrimaryStage().getScene();
                TextField input = (TextField) scene.lookup("#commandTextField");
                TextArea feedback = (TextArea) scene.lookup("#resultDisplay");
                Label feedbackStatus = (Label) scene.lookup("#feedbackStatus");
                Button run = (Button) scene.lookup("#runButton");
                withRestoredClipboard(() -> {
                    setClipboard("add n/Alice p/111 e/alice@example.com a/First\nSecond");
                    input.paste();

                    assertEquals("", input.getText());
                    assertEquals(Messages.MESSAGE_INVALID_CHARACTERS, feedback.getText());
                    assertTrue(feedbackStatus.isVisible());
                    assertTrue(model.getAddressBook().getPersonList().isEmpty());
                    assertFalse(Files.exists(dataFile), "Rejecting a paste must not save any client data");

                    input.fireEvent(new ActionEvent());
                    assertTrue(model.getAddressBook().getPersonList().isEmpty());
                    assertFalse(Files.exists(dataFile));

                    String validCommand = "add n/Alice p/111 e/alice@example.com a/First Second";
                    setClipboard(validCommand);
                    input.paste();
                    assertEquals(validCommand, input.getText());
                    assertFalse(input.getStyleClass().contains(CommandBox.ERROR_STYLE_CLASS));
                    run.fire();

                    assertEquals(1, model.getAddressBook().getPersonList().size());
                    assertEquals("First Second", model.getAddressBook().getPersonList().getFirst().getAddress().value);
                    assertTrue(Files.exists(dataFile));
                    assertEquals("", input.getText());
                    assertFalse(feedbackStatus.isVisible());
                });
            } finally {
                window.getPrimaryStage().close();
            }
            return null;
        });
    }

    @Test
    public void paste_invalidIntoValidDraftThenEnter_submitsOriginalDraftOnce() throws Exception {
        assertInvalidPasteThenSubmitOriginalDraft(false);
    }

    @Test
    public void paste_invalidIntoValidDraftThenRun_submitsOriginalDraftOnce() throws Exception {
        assertInvalidPasteThenSubmitOriginalDraft(true);
    }

    @Test
    public void paste_validUnicodeAndSpaces_replacesSelectionAndRemainsUndoable() throws Exception {
        runOnFxThread(() -> {
            AtomicInteger executionCount = new AtomicInteger();
            CommandBox commandBox = new CommandBox(commandText -> {
                executionCount.incrementAndGet();
                return new CommandResult("Submitted.");
            });
            TextField input = (TextField) commandBox.getRoot().lookup("#commandTextField");
            withRestoredClipboard(() -> {
                input.setText("find Alice");
                input.selectRange(5, 10);
                setClipboard("  李 José 🏠  ");
                input.paste();

                assertEquals("find   李 José 🏠  ", input.getText());
                assertFalse(input.getStyleClass().contains(CommandBox.ERROR_STYLE_CLASS));
                assertEquals(0, executionCount.get());
                input.undo();
                assertEquals("find Alice", input.getText());
                input.redo();
                assertEquals("find   李 José 🏠  ", input.getText());
            });
            return null;
        });
    }

    @Test
    public void paste_platformShortcut_usesSameRejectionAndRecovery() throws Exception {
        runOnFxThread(() -> {
            CommandBox commandBox = new CommandBox(commandText -> new CommandResult("Submitted."));
            new Scene(commandBox.getRoot());
            commandBox.getRoot().applyCss();
            TextField input = (TextField) commandBox.getRoot().lookup("#commandTextField");
            withRestoredClipboard(() -> {
                input.setText("find Alice");
                input.selectRange(10, 5);
                setClipboard("First\tSecond");
                pressPasteShortcut(input);

                assertEquals("find Alice", input.getText());
                assertEquals(10, input.getAnchor());
                assertEquals(5, input.getCaretPosition());
                assertTrue(input.getStyleClass().contains(CommandBox.ERROR_STYLE_CLASS));

                setClipboard("Bob");
                pressPasteShortcut(input);
                assertEquals("find Bob", input.getText());
                assertFalse(input.getStyleClass().contains(CommandBox.ERROR_STYLE_CLASS));
            });
            return null;
        });
    }

    private void assertInvalidPasteThenSubmitOriginalDraft(boolean shouldUseRunButton) throws Exception {
        runOnFxThread(() -> {
            try (DraftSubmissionFixture fixture = new DraftSubmissionFixture()) {
                fixture.prepareDraft();
                withRestoredClipboard(() -> {
                    fixture.pasteInvalidCommand();
                    fixture.assertRejectedPastePreservesDraft();
                    fixture.submit(shouldUseRunButton);
                    fixture.assertOriginalDraftSubmitted();
                });
                fixture.assertSavedDataMatches();
            }
            return null;
        });
    }

    /**
     * Owns the real window and storage used to submit a draft after rejecting a paste.
     */
    private class DraftSubmissionFixture implements AutoCloseable {
        private final Path dataFile = temporaryFolder.resolve("clients.json");
        private final ModelManager model = new ModelManager();
        private final JsonAddressBookStorage addressBookStorage = new JsonAddressBookStorage(dataFile);
        private final List<String> executedCommands = new ArrayList<>();
        private final String originalAddress = "Original draft address";
        private final String originalCommand = "add n/Alice p/111 e/alice@example.com a/" + originalAddress;
        private final MainWindow window = new MainWindow(new Stage(), createRecordingLogic(), dataFile);
        private TextField input;
        private TextArea feedback;
        private Label feedbackStatus;
        private Button run;

        private LogicManager createRecordingLogic() {
            return new LogicManager(model, new StorageManager(addressBookStorage,
                    new JsonUserPrefsStorage(temporaryFolder.resolve("preferences.json")))) {
                @Override
                public CommandResult execute(String commandText) throws CommandException, ParseException {
                    executedCommands.add(commandText);
                    return super.execute(commandText);
                }
            };
        }

        private void prepareDraft() {
            window.fillInnerParts();
            Scene scene = window.getPrimaryStage().getScene();
            input = (TextField) scene.lookup("#commandTextField");
            feedback = (TextArea) scene.lookup("#resultDisplay");
            feedbackStatus = (Label) scene.lookup("#feedbackStatus");
            run = (Button) scene.lookup("#runButton");
            input.setText(originalCommand);
            input.selectRange(originalCommand.length(), originalCommand.indexOf(originalAddress));
        }

        private void pasteInvalidCommand() {
            setClipboard("add n/Bob p/222 e/bob@example.com a/Rejected\nClipboard");
            input.paste();
        }

        private void assertRejectedPastePreservesDraft() {
            assertEquals(originalCommand, input.getText());
            assertEquals(originalCommand.length(), input.getAnchor());
            assertEquals(originalCommand.indexOf(originalAddress), input.getCaretPosition());
            assertEquals(originalAddress, input.getSelectedText());
            assertTrue(input.getStyleClass().contains(CommandBox.ERROR_STYLE_CLASS));
            assertEquals(Messages.MESSAGE_INVALID_CHARACTERS, feedback.getText());
            assertTrue(feedbackStatus.isVisible());
            assertTrue(executedCommands.isEmpty(), "Rejecting a paste must not execute the existing draft");
            assertTrue(model.getAddressBook().getPersonList().isEmpty());
            assertFalse(Files.exists(dataFile), "Rejecting a paste must not save any client data");
        }

        private void submit(boolean shouldUseRunButton) {
            if (shouldUseRunButton) {
                run.fire();
            } else {
                input.fireEvent(new ActionEvent());
            }
        }

        private void assertOriginalDraftSubmitted() {
            assertEquals(List.of(originalCommand), executedCommands);
            assertEquals(1, model.getAddressBook().getPersonList().size());
            assertEquals(originalAddress, model.getAddressBook().getPersonList().getFirst().getAddress().value);
            assertEquals("", input.getText());
            assertFalse(input.getStyleClass().contains(CommandBox.ERROR_STYLE_CLASS));
            assertFalse(feedbackStatus.isVisible());
        }

        private void assertSavedDataMatches() throws Exception {
            assertEquals(model.getAddressBook(), addressBookStorage.readAddressBook().orElseThrow());
        }

        @Override
        public void close() {
            window.getPrimaryStage().close();
        }
    }

    private static void pressPasteShortcut(TextField input) {
        boolean isMac = System.getProperty("os.name").toLowerCase(Locale.ROOT).contains("mac");
        input.fireEvent(new KeyEvent(KeyEvent.KEY_PRESSED, "", "", KeyCode.V, false, !isMac, false, isMac));
        input.fireEvent(new KeyEvent(KeyEvent.KEY_RELEASED, "", "", KeyCode.V, false, !isMac, false, isMac));
    }

    private static void setClipboard(String text) {
        ClipboardContent content = new ClipboardContent();
        content.putString(text);
        Clipboard clipboard = Clipboard.getSystemClipboard();
        assertTrue(clipboard.setContent(content), "Could not set clipboard text");
        String actualText = clipboard.getString();
        assertNotNull(actualText, "Clipboard must supply text before paste is tested");
        // Windows may normalize line endings to CRLF. All other characters must survive the clipboard.
        assertEquals(normalizeLineEndings(text), normalizeLineEndings(actualText),
                "Clipboard changed the input before it could reach the command field");
    }

    private static String normalizeLineEndings(String text) {
        return text.replace("\r\n", "\n").replace('\r', '\n');
    }

    private static void withRestoredClipboard(Runnable action) {
        Clipboard clipboard = Clipboard.getSystemClipboard();
        Map<DataFormat, Object> originalContent = new HashMap<>();
        for (DataFormat format : clipboard.getContentTypes()) {
            Object content = clipboard.getContent(format);
            if (content != null) {
                originalContent.put(format, content);
            }
        }
        try {
            action.run();
        } finally {
            // Restore formats JavaFX can read; native or lazily supplied formats may not round-trip.
            clipboard.setContent(originalContent);
        }
    }

    private static <T> T runOnFxThread(Callable<T> action) throws Exception {
        return FxTestSupport.callOnFx(action, 10);
    }
}
