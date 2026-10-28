package seedu.address.logic.parser;

import seedu.address.logic.parser.exceptions.ParseException;
import seedu.address.model.person.ClientId;

/** Checked permanent-ID parsing using the shared bounded numeric parser. */
public final class ClientIdParserUtil {
    public static final String MESSAGE_INVALID_CLIENT_ID =
            "Client ID must be a positive integer from 1 to 2147483647.";

    private ClientIdParserUtil() {
    }

    /** Parses an ID, accepting leading zeros and trimming outer ordinary spaces. */
    public static ClientId parseClientId(String text) throws ParseException {
        try {
            return new ClientId(ParserUtil.parseIndex(text).getOneBased());
        } catch (ParseException e) {
            throw new ParseException(MESSAGE_INVALID_CLIENT_ID, e);
        }
    }
}
