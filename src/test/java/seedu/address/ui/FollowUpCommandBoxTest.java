package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import seedu.address.logic.commands.CommandResult;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.exceptions.ParseException;

public class FollowUpCommandBoxTest {

    @Test
    public void executeCommand_nullExecutor_throwsNullPointerException() {
        NullPointerException exception = assertThrows(NullPointerException.class, () ->
                CommandBox.executeCommand(null, "list"));

        assertEquals("commandExecutor", exception.getMessage());
    }

    @Test
    public void executeCommand_nullInput_rejectsBeforeExecution() {
        AtomicInteger executionCount = new AtomicInteger();

        NullPointerException exception = assertThrows(NullPointerException.class, () ->
                CommandBox.executeCommand(commandText -> {
                    executionCount.incrementAndGet();
                    return new CommandResult("Listed all clients.");
                }, null));

        assertEquals("commandText", exception.getMessage());
        assertEquals(0, executionCount.get());
    }

    @Test
    public void executeCommand_whitespaceInput_passesUnchangedExactlyOnce() throws Exception {
        AtomicInteger executionCount = new AtomicInteger();
        AtomicReference<String> executedText = new AtomicReference<>();
        String input = "  \t  ";
        CommandResult expected = new CommandResult("Enter a command.");

        CommandResult actual = CommandBox.executeCommand(commandText -> {
            executionCount.incrementAndGet();
            executedText.set(commandText);
            return expected;
        }, input);

        assertEquals(1, executionCount.get());
        assertEquals(input, executedText.get());
        assertSame(expected, actual);
    }

    @Test
    public void executeCommand_unexpectedFailure_propagatesUnchanged() {
        IllegalStateException expected = new IllegalStateException("Executor unavailable");

        IllegalStateException actual = assertThrows(IllegalStateException.class, () ->
                CommandBox.executeCommand(commandText -> {
                    throw expected;
                }, "list"));

        assertSame(expected, actual);
    }

    @Test
    public void executeCommand_emptyInput_reachesCommandExecutor() throws Exception {
        AtomicReference<String> executedText = new AtomicReference<>();
        CommandResult expected = new CommandResult("Enter a command.");

        CommandResult actual = CommandBox.executeCommand(commandText -> {
            executedText.set(commandText);
            return expected;
        }, "");

        assertEquals("", executedText.get());
        assertSame(expected, actual);
    }

    @Test
    public void executeCommand_validInput_returnsSuccessfulResult() throws Exception {
        AtomicReference<String> executedText = new AtomicReference<>();
        CommandResult expected = new CommandResult("Listed all clients.");

        CommandResult actual = CommandBox.executeCommand(commandText -> {
            executedText.set(commandText);
            return expected;
        }, "list");

        assertEquals("list", executedText.get());
        assertSame(expected, actual);
    }

    @Test
    public void executeCommand_executionFailure_propagatesToUiHandler() {
        CommandException expected = new CommandException("Changes could not be saved.");

        CommandException actual = assertThrows(CommandException.class, () ->
                CommandBox.executeCommand(commandText -> {
                    throw expected;
                }, "followup 1 clear"));

        assertSame(expected, actual);
    }

    @Test
    public void executeCommand_parseFailure_propagatesToUiHandler() {
        ParseException expected = new ParseException("Invalid command");

        ParseException actual = assertThrows(ParseException.class, () ->
                CommandBox.executeCommand(commandText -> {
                    throw expected;
                }, "invalid"));

        assertSame(expected, actual);
    }
}
