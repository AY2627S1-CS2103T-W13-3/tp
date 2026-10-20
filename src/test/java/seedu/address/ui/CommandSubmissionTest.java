package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.awt.GraphicsEnvironment;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import seedu.address.logic.commands.CommandResult;

public class CommandSubmissionTest {

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

    /**
     * Runs a check on the JavaFX application thread and propagates failures to JUnit.
     */
    private static <T> T runOnFxThread(Callable<T> action) throws Exception {
        FutureTask<T> task = new FutureTask<>(action);
        Platform.runLater(task);
        return task.get(10, TimeUnit.SECONDS);
    }
}
