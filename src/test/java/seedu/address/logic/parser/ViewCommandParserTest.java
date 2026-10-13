package seedu.address.logic.parser;

import static seedu.address.logic.parser.CommandParserTestUtil.assertParseFailure;
import static seedu.address.logic.parser.CommandParserTestUtil.assertParseSuccess;

import java.util.List;

import org.junit.jupiter.api.Test;

import seedu.address.commons.core.index.Index;
import seedu.address.logic.commands.ViewCommand;

/** Tests view argument shape before shared index validation. */
public class ViewCommandParserTest {
    private final ViewCommandParser parser = new ViewCommandParser();

    @Test
    public void parse_validIndex_returnsViewCommand() {
        for (String input : List.of("2", " 02 ", "0".repeat(1000) + "2")) {
            assertParseSuccess(parser, input, new ViewCommand(Index.fromOneBased(2)));
        }
        assertParseSuccess(parser, "2147483647", new ViewCommand(Index.fromOneBased(Integer.MAX_VALUE)));
    }

    @Test
    public void parse_missingOrExtraTokens_reportsUsageBeforeSyntax() {
        for (String input : List.of("", "   ", "1 extra", "0 extra", "abc 2", "1  2")) {
            assertParseFailure(parser, input, "Invalid command format!\nUsage: view INDEX");
        }
    }

    @Test
    public void parse_invalidSingleToken_preservesSharedIndexError() {
        for (String input : List.of("0", "000", "-1", "+1", "1.5", "abc", "2147483648",
                "9".repeat(1000), "١", "１", "1\u00a02")) {
            assertParseFailure(parser, input, "Index must be a positive integer from 1 to 2147483647.");
        }
    }
}
