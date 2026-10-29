package seedu.address.logic.parser;

import static seedu.address.commons.util.StringUtil.trimOrdinarySpaces;
import static seedu.address.logic.Messages.MESSAGE_INVALID_COMMAND_FORMAT;

import java.util.List;

import seedu.address.logic.commands.FindCommand;
import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.person.ClientContainsKeywordsPredicate;
import seedu.address.model.person.ClientSearchPredicate;
import seedu.address.model.person.ClientSearchPredicate.Field;
import seedu.address.model.person.Phone;
import seedu.address.model.tag.Tag;

/**
 * Parses input arguments and creates a new FindCommand object
 */
public class FindCommandParser implements Parser<FindCommand> {

    /**
     * Parses the given {@code String} of arguments in the context of the FindCommand
     * and returns a FindCommand object for execution.
     * Bare keywords are separated by ordinary spaces; explicit name values may contain spaces.
     * Quotes and punctuation remain literal within a value.
     * Global raw-character validation is performed by {@link AddressBookParser}.
     * @throws ParseException if the user input does not conform to the expected format
     */
    public FindCommand parse(String args) throws ParseException {
        String trimmedArgs = trimOrdinarySpaces(args);
        if (trimmedArgs.isEmpty()) {
            throw new ParseException(
                    String.format(MESSAGE_INVALID_COMMAND_FORMAT, FindCommand.MESSAGE_USAGE));
        }

        Field field = getExplicitFieldAtStart(trimmedArgs);
        if (field != null) {
            String query = trimOrdinarySpaces(trimmedArgs.substring(2));
            if (query.isEmpty() || containsAnotherExplicitField(query)) {
                throw createInvalidSearchFormatException();
            }
            validateQuery(field, query);
            return new FindCommand(new ClientSearchPredicate(field, query));
        }

        if (containsExplicitFieldAfterText(trimmedArgs)) {
            throw createInvalidSearchFormatException();
        }

        String[] keywords = trimmedArgs.split(" +");

        return new FindCommand(new ClientContainsKeywordsPredicate(List.of(keywords)));
    }

    private Field getExplicitFieldAtStart(String args) {
        if (args.startsWith("n/")) {
            return Field.NAME;
        } else if (args.startsWith("p/")) {
            return Field.PHONE;
        } else if (args.startsWith("t/")) {
            return Field.TAG;
        }
        return null;
    }

    private boolean containsAnotherExplicitField(String query) {
        return containsExplicitFieldAfterText(query) || query.startsWith("n/")
                || query.startsWith("p/") || query.startsWith("t/");
    }

    private boolean containsExplicitFieldAfterText(String args) {
        for (int index = 0; index + 2 < args.length(); index++) {
            if (args.charAt(index) == ' ' && isReservedPrefix(args, index + 1)) {
                return true;
            }
        }
        return false;
    }

    private boolean isReservedPrefix(String args, int index) {
        assert index >= 0 && index < args.length() - 1 : "A prefix must have two available characters.";
        char prefix = args.charAt(index);
        return args.charAt(index + 1) == '/' && (prefix == 'n' || prefix == 'p' || prefix == 't');
    }

    private void validateQuery(Field field, String query) throws ParseException {
        if (field == Field.PHONE && !Phone.isValidPhoneQuery(query)) {
            throw createInvalidSearchFormatException();
        }
        if (field == Field.TAG && !Tag.isValidTagName(query)) {
            throw createInvalidSearchFormatException();
        }
    }

    private ParseException createInvalidSearchFormatException() {
        return new ParseException(String.format(MESSAGE_INVALID_COMMAND_FORMAT, FindCommand.MESSAGE_USAGE));
    }

}
