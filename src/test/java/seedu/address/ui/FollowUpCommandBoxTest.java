package seedu.address.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import seedu.address.logic.commands.CommandResult;
import seedu.address.logic.commands.exceptions.CommandException;
import seedu.address.logic.parser.exceptions.ParseException;

public class FollowUpCommandBoxTest {

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
