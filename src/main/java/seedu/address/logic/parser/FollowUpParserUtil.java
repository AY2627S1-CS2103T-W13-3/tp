package seedu.address.logic.parser;

import static java.util.Objects.requireNonNull;

import seedu.address.commons.core.index.Index;
import seedu.address.logic.parser.exceptions.ParseException;

/** Contains parsing helpers specific to the follow-up command grammar. */
public final class FollowUpParserUtil {

    private FollowUpParserUtil() {}

    /**
     * Parses a follow-up command index, accepting ASCII digits and leading zeroes only.
     *
     * @param value The unvalidated index text.
     * @return The parsed one-based index.
     * @throws ParseException If the value is outside the supported positive integer range.
     */
    public static Index parseIndex(String value) throws ParseException {
        requireNonNull(value);
        if (!value.chars().allMatch(character -> character >= '0' && character <= '9')) {
            throw new ParseException(ParserUtil.MESSAGE_INVALID_INDEX);
        }

        String significantDigits = value.replaceFirst("^0+", "");
        if (significantDigits.isEmpty() || significantDigits.length() > 10
                || (significantDigits.length() == 10 && significantDigits.compareTo("2147483647") > 0)) {
            throw new ParseException(ParserUtil.MESSAGE_INVALID_INDEX);
        }

        return Index.fromOneBased(Integer.parseInt(significantDigits));
    }
}
