package seedu.address.logic.parser;

import seedu.address.commons.core.index.Index;
import seedu.address.logic.parser.exceptions.ParseException;

/** Contains parsing helpers specific to the follow-up command grammar. */
public final class FollowUpParserUtil {

    private FollowUpParserUtil() {}

    /**
     * Parses a follow-up index using the shared ASCII/leading-zero rules and outer ordinary-space trimming.
     *
     * @param value The unvalidated index text.
     * @return The parsed one-based index.
     * @throws ParseException If the value is outside the supported positive integer range.
     */
    public static Index parseIndex(String value) throws ParseException {
        return ParserUtil.parseIndex(value);
    }
}
