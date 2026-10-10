package seedu.address.logic.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static seedu.address.testutil.Assert.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.address.logic.parser.exceptions.ParseException;

/**
 * Verifies follow-up indices share the client index contract.
 */
public class FollowUpParserUtilTest {

    @Test
    public void parseIndex_validBoundariesAndLeadingZeroes_matchSharedParser() throws Exception {
        for (String valid : List.of("1", "0002", "0002147483647", "0".repeat(10000) + "1")) {
            assertEquals(ParserUtil.parseIndex(valid), FollowUpParserUtil.parseIndex(valid));
        }
    }

    @Test
    public void parseIndex_invalidValues_preservesSharedError() {
        for (String invalid : List.of("", "0", "-1", "+1", "1.5", "1 2", "2147483648", "١", "1\t")) {
            assertThrows(ParseException.class, "Index must be a positive integer from 1 to 2147483647.", ()
                -> FollowUpParserUtil.parseIndex(invalid));
        }
        assertThrows(NullPointerException.class, () -> FollowUpParserUtil.parseIndex(null));
    }
}
