package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.awt.GraphicsEnvironment;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javafx.application.Platform;
import javafx.scene.control.TextField;
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

    /**
     * Runs a check on the JavaFX application thread and propagates failures to JUnit.
     */
    private static <T> T runOnFxThread(Callable<T> action) throws Exception {
        FutureTask<T> task = new FutureTask<>(action);
        Platform.runLater(task);
        return task.get(10, TimeUnit.SECONDS);
    }
}
