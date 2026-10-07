package seedu.address.logic.parser;

import static seedu.address.logic.Messages.MESSAGE_CHANGES_BLOCKED;
import static seedu.address.logic.Messages.MESSAGE_EMPTY_COMMAND;
import static seedu.address.logic.Messages.MESSAGE_INVALID_CHARACTERS;
import static seedu.address.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;
import static seedu.address.logic.Messages.MESSAGE_UNKNOWN_COMMAND;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import seedu.address.logic.commands.AddCommand;
import seedu.address.logic.commands.ClearCommand;
import seedu.address.logic.commands.Command;
import seedu.address.logic.commands.DeleteCommand;
import seedu.address.logic.commands.EditCommand;
import seedu.address.logic.commands.ExitCommand;
import seedu.address.logic.commands.FindCommand;
import seedu.address.logic.commands.FollowUpsCommand;
import seedu.address.logic.commands.HelpCommand;
import seedu.address.logic.commands.ListCommand;
import seedu.address.logic.parser.exceptions.ParseException;

/**
 * Parses user input.
 */
public class AddressBookParser {

    /**
     * Used for initial separation of command word and args.
     */
    private static final Pattern BASIC_COMMAND_FORMAT = Pattern.compile("(?<commandWord>\\S+)(?<arguments>.*)");

    /**
     * Parses user input into command for execution.
     *
     * @param userInput full user input string
     * @return the command based on the user input
     * @throws ParseException if the user input does not conform to the expected format
     */
    public Command parseCommand(String userInput) throws ParseException {
        return parseCommand(userInput, false);
    }

    /**
     * Parses user input while applying the startup loading protection gate.
     *
     * @param userInput Full user input string.
     * @param isDataLoadingBlocked Whether client-data mutations are blocked.
     * @return The command based on the user input.
     * @throws ParseException If the user input does not conform to the expected format.
     */
    public Command parseCommand(String userInput, boolean isDataLoadingBlocked) throws ParseException {
        if (containsDisallowedCharacters(userInput)) {
            throw new ParseException(MESSAGE_INVALID_CHARACTERS);
        }

        String trimmedInput = trimOrdinarySpaces(userInput);
        if (trimmedInput.isEmpty()) {
            throw new ParseException(MESSAGE_EMPTY_COMMAND);
        }

        final Matcher matcher = BASIC_COMMAND_FORMAT.matcher(trimmedInput);
        if (!matcher.matches()) {
            throw new ParseException(String.format(MESSAGE_INVALID_COMMAND_FORMAT, HelpCommand.MESSAGE_USAGE));
        }

        final String commandWord = matcher.group("commandWord");
        final String arguments = matcher.group("arguments");

        if (isDataLoadingBlocked && isMutationCommand(commandWord)) {
            throw new ParseException(MESSAGE_CHANGES_BLOCKED);
        }

        return switch (commandWord) {
            case AddCommand.COMMAND_WORD -> new AddCommandParser().parse(arguments);
            case EditCommand.COMMAND_WORD -> new EditCommandParser().parse(arguments);
            case DeleteCommand.COMMAND_WORD -> new DeleteCommandParser().parse(arguments);
            case ClearCommand.COMMAND_WORD -> new ClearCommand();
            case FollowUpCommandParser.COMMAND_WORD -> new FollowUpCommandParser().parse(arguments);
            case FollowUpsCommand.COMMAND_WORD -> new FollowUpsCommand();
            case FindCommand.COMMAND_WORD -> new FindCommandParser().parse(arguments);
            case ListCommand.COMMAND_WORD -> new ListCommand();
            case ExitCommand.COMMAND_WORD -> new ExitCommand();
            case HelpCommand.COMMAND_WORD -> new HelpCommand();
            default -> throw new ParseException(MESSAGE_UNKNOWN_COMMAND);
        };
    }

    private static boolean containsDisallowedCharacters(String input) {
        return input.codePoints().anyMatch(character -> character <= 0x001f
                || (character >= 0x007f && character <= 0x009f)
                || character == 0x2028 || character == 0x2029);
    }

    private static String trimOrdinarySpaces(String input) {
        int first = 0;
        int last = input.length();
        while (first < last && input.charAt(first) == ' ') {
            first++;
        }
        while (last > first && input.charAt(last - 1) == ' ') {
            last--;
        }
        return input.substring(first, last);
    }

    private static boolean isMutationCommand(String commandWord) {
        return commandWord.equals(AddCommand.COMMAND_WORD)
                || commandWord.equals(EditCommand.COMMAND_WORD)
                || commandWord.equals(DeleteCommand.COMMAND_WORD)
                || commandWord.equals(ClearCommand.COMMAND_WORD)
                || commandWord.equals(FollowUpCommandParser.COMMAND_WORD);
    }

}
