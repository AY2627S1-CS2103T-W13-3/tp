package seedu.address.logic.parser;

import static seedu.address.commons.util.StringUtil.trimOrdinarySpaces;
import static seedu.address.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;

import seedu.address.logic.commands.ViewCommand;
import seedu.address.logic.parser.exceptions.ParseException;

/**
 * Parses a single displayed-list index for the view command.
 */
public final class ViewCommandParser implements Parser<ViewCommand> {
    /**
     * Checks the one-token shape before delegating index syntax to the shared parser.
     * Global raw-character validation is performed by {@link AddressBookParser}.
     *
     * @throws ParseException if the argument shape or index is invalid.
     */
    @Override
    public ViewCommand parse(String args) throws ParseException {
        String trimmedArgs = trimOrdinarySpaces(args);
        if (trimmedArgs.isEmpty() || trimmedArgs.contains(" ")) {
            throw new ParseException(String.format(MESSAGE_INVALID_COMMAND_FORMAT, ViewCommand.MESSAGE_USAGE));
        }
        return new ViewCommand(ParserUtil.parseIndex(trimmedArgs));
    }
}
